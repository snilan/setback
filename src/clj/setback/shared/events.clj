(ns setback.shared.events)

(def event-types 
  #{
    :join
    :leave
    :error
    :new-hand
    :bet
    })
    
(defn make-event [event data]
  {:event event
   :data data})


; The simplest pub/sub model ever.
; Not very necessary, but it helps to organize my code.

(def reactions (atom {}))

(defn react-to [k fun]
  (swap! reactions update-in [k] conj fun))

(defn trigger [k data]
  (let [funs (@reactions k)]
   (for [f funs]
    (f data)))) 


