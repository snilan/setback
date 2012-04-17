(ns setback.engine
  (:use setback.crossover.cards))

(def games (atom {}))
(def players (atom {}))


(defn point-val [card-num]
   (cond 
     (= (card-num 10)) 10 
     (> card-num 10) (- card-num 10)
     :else 0))

(defn suit? [card suit]
  (= (:suit card) suit))

(def deck 
  (for [suit suits n numbers]
    (Card. suit n)))

(defn add-player [pid]
  (swap! players assoc pid
         {:name "Anonymous"
          :pid pid
          :points 0
          :bet nil
          :game-id nil
          :cards nil}))

(defn new-game []
  {:cards-on-table []
   :players []
   :dealer 0
   :num-players num-players
   :trump nil
   :goal 21})

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

(defn change-name [pid new-name]
  (if-let [game-id (:game-id (get @players pid))]
    (if-let [pos (find-player-pos (get @games game-id) pid)]
      (swap! games assoc-in [game-id :players pos :name] new-name)))
  (swap! players assoc-in [pid :name] new-name))

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

(defn next-to-go? [game player]
  (let [offset (count (:cards-on-table game))]
   (= player
     (nth (:players game) (mod (+ (:dealer game) offset) (:num-players game)))))) 

(defn valid-move? [game player card] 
  (and
    (next-to-go? game player) 
    (has-card? player card)
    (if (dealer? player)
      true
      (let [dealt-suit (-> game :cards-on-table first second :suit)
            trump (:trump game)]
        (or 
          (suit? card trump)
          (suit? card dealt-suit)
          (not-any? #(= (:suit %) dealt-suit) (:cards player)))))))


(defn make-move [game player card]
  (when (and game player card) 
    (let [card (Card. (:suit card) (:number card))]
      (if (valid-move? game player card)
        (let [pos (find-player-pos game player)]
          (update-in  
            (update-in game [:cards-on-table] conj [player card])
            [:players pos :cards] disj card))))))


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

