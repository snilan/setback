(ns setback.crossover.cards)


(defrecord Card [suit number])

(def suits [:clubs :diamonds :spades :hearts])
(def numbers (range 2 15))
(def num-cards 6)
(def num-players 3)

(defn point-val [card-num]
   (cond 
     (= (card-num 10)) 10 
     (> card-num 10) (- card-num 10)
     :else 0))




