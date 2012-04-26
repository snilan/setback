(ns setback.shared.cards)

(defrecord Card [suit number])

(def suits [:clubs :diamonds :spades :hearts])
(def numbers (range 2 15))

(defn point-val [card-num]
   (cond 
     (= (card-num 10)) 10 
     (> card-num 10) (- card-num 10)
     :else 0))


(defn vector-sort
  [v1 v2]
  (cond
    (and (empty? v1) (empty? v2))
      0
    (empty? v1)
      -1
    (empty? v2)
      1
    :default
    (let [cmp (apply compare (map first [v1 v2]))]
      (if (zero? cmp)
        (vector-sort (rest v1) (rest v2))
        cmp))))



;; first sort by suit, then number
;; seems to be working
(defn sort-cards [cards]
  (sort-by (fn [c]
            (vector 
              ((into {} (map-indexed #(vector %2 %1) suits)) (:suit c)) 
              (- (:number c)))) 
           vector-sort
           cards))


