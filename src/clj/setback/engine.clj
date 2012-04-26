(ns setback.engine
  (:use setback.shared.cards
        setback.shared.events)
  (:import [setback.shared.cards Card]))

(def games (atom {}))
(def player-info (atom {}))

(def num-cards 6)
(def num-players 3)

; hierarchy of a game
; game -> hand -> play -> turn

; two stages of a game
; {:betting, :playing} 

(defn new-game []
  {:cards-on-table []
   :players []
   :dealer 0
   :trump-player nil
   :stage :betting
   :num-players num-players
   :trump nil
   :goal 21})

(defn suit? [card suit]
  (= (:suit card) suit))

(def deck 
  (for [suit suits n numbers]
    (Card. suit n)))

;; I decided to set apart player information
;; from the player in the context of a single game
;; Both are identifiable by their pid 


(defn find-player-pos [game pid]
  (->> (:players game) 
    (keep-indexed #(if (= pid (:pid %2)) %1)) 
    first))

(defn find-player [game pid]
  (first (filter #(= (:pid %) pid) (:players game))))

(defn num-players? [game-id]
  (if (get @games game-id)
    (count (get-in @games [game-id :players]))
    0))

(defn game-is-full? [game-id]
  (= (num-players? game-id) num-players))


(defn hide-cards [game pid]
  (update-in
    (update-in game #(assoc % :you (dissoc % :pid (find-player game pid))))
    [:players] (fn [x]
                 (map #(dissoc % :cards :pid)
                 (dissoc (:players game) (find-player-pos game pid))))))

;; Seems to work well
(defn deal-cards [game]
  (let [shuffled (shuffle deck)]
    (update-in game [:players]
      #(first (reduce (fn [[players cards-left] player]
                 [(conj players
                        (assoc player :cards (into #{} (take num-cards cards-left))))
                  (drop num-cards cards-left)])
               [nil shuffled] %)))))
               
(defn- beats? [card1 card2 trump dealt]
  (assert (some #(or (suit? % trump) (suit? % dealt)) [card1 card2]))
  (cond
    (and (suit? card1 trump) (not (suit? card2 trump)))
      true
    (and (not (suit? card1 trump)) (suit? card2 trump))
      false
    (and (suit? card1 dealt) (not (suit? card2 dealt)))
      true
    (and (not (suit? card1 dealt)) (suit? card2 dealt))
     false
    (> (:number card1) (:number card2))
     true
    (< (:number card1) (:number card2))
     false))

(defn who-takes-hand? [hand trump]
  (let [suit-dealt (-> hand first second :suit)]
    (first 
      (reduce (fn [[p card] [winner wcard]]
                (if (beats? card wcard trump suit-dealt)
                  [p card]
                  [winner wcard])) 
              (first hand) (rest hand)))))

(defn has-card? [player card]
  (contains? (:cards player) card))

(defn dealer? [game player]
  (= (nth (:players game) (:dealer game)) player))

(defn hand-over? [game]
  (every? empty? (map :cards (:players game))))

(defn game-over? [game]
  (some #(>= (:points %) (:goal game)) (:players game)))

(defn next-to-play? [game player]
  (let [offset (count (:cards-on-table game))]
   (= player
     (nth (:players game) (mod (+ (:dealer game) offset) (:num-players game)))))) 

(defn next-to-bet? [game player]
  (let [players (:players game)
        dealer (:dealer game)
        num-players (:num-players game)
        clist (take num-players (drop (inc dealer) (cycle players)))]
    (= 
      (first (comp not nil?) clist) 
      player)))

(defn valid-move? [game player card] 
  (and
    (= (:stage game) :playing)
    (next-to-play? game player) 
    (has-card? player card)
    (if (dealer? player)
      true
      (let [dealt-suit (-> game :cards-on-table first second :suit)
            trump (:trump game)]
        (or 
          (suit? card trump)
          (suit? card dealt-suit)
          (not-any? #(= (:suit %) dealt-suit) (:cards player)))))))

(def min-bet 2)
(def max-bet 4)

(defn valid-bet? [game player bet]
 (and
  (= (:stage game) :betting)
  (next-to-bet? player)
  (let [maxb (max (keep :bet (:players game)))]
    (if (dealer? player)
      (if (zero? maxb)
        (<= min-bet bet max-bet)
        (or (zero? bet) (>= bet maxb)))
      (or 
        (zero? bet)
        (and (> bet maxb) (<= bet max-bet)))))))


(defn make-move [game player card]
  (when (and game player card) 
    (let [card (Card. (:suit card) (:number card))]
      (if (valid-move? game player card)
        (let [pos (find-player-pos game player)]
          (update-in  
            (update-in game [:cards-on-table] conj [player card])
            [:players pos :cards] disj card))))))


(defn make-bet [game player bet]
  (if (valid-bet? game player bet) 
    (let [pos (find-player-pos game player)]
      (assoc-in game [:players pos :bet] bet))))



;; functions specific to updating in-memory databases
(defn add-player-to-store 
 "Adds player information to in-memory database" 
  [pid ch]
  (swap! player-info assoc pid
         {:name "Anonymous"
          :pid pid
          :private-channel ch
          :game-id nil}))

(defn should-know?
  "Filter which clients should receive this message"
  [pid msg]
  (let [event (:event msg)
        src (:src msg)]
    (if-not (and (= event :error) (not= src pid))
     true))) 

(defn hide-info 
  "Hides cards of other players"
  [pid msg]
  (let [event (:event msg)
        data (:data msg)]
    (if (= event :new-hand)
      (let [cards 
              (set (map #(into {} %) (:cards (find-player data pid))))]
        (make-event event cards))
      msg)))

(defn make-new-game [game-id ch]
  (swap! games assoc game-id (new-game)))

(defn add-game-info 
  "Hides personal information about player and adds game vars"
  [player]
  (merge
    (dissoc player :private-channel :game-id)
    {:points 0 :cards [] :bet nil :cards-taken []}))

(defn add-player-to-game [pid game-id]
  (swap! player-info assoc-in [pid :game-id] game-id) 
  (let [p (@player-info pid)]
    (swap! games update-in [game-id :players] conj (add-game-info p))))

(defn change-name [pid new-name]
  (if-let [game-id (:game-id (get @player-info pid))]
    (if-let [pos (find-player-pos (get @games game-id) pid)]
      (swap! games assoc-in [game-id :players pos :name] new-name)))
  (swap! player-info assoc-in [pid :name] new-name))



;;  Each single player entity needs to keep track of tha single hand and moves by other players, including:
;;  - Whether or not they have any more trump cards
;;  - Points accumulated
;;  - In fact, we need all cards throw
;;  And possibly:
;;  - Whether or not they bet  
;;  - The suit they ae playing most easily (losing their low cards from)

;;  Actually, we can represent opponent players simply as a list of the cards they have thrown, and whether or not they are trump player
;;  opponents [
;;             {:trump false
;;              :total-score 15
;;              :cards-thrown [(Card. :spades 8) (Card. :hearts seven)  
