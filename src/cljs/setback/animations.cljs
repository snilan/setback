(ns setback.animations
  (:refer-clojure :exclude [val]) 
  (:use
    [setback.shared.cards :only (Card numbers suits sort-cards)]
    [jayq.core :only ($ bind inner val append add-class)])
  (:require
    [monet.canvas :as monet]))

(def canvas (.get ($ :#canvas) 0))
;;(set! (.-height canvas) 1000)
;;(set! (.-width canvas) 1000)

(def ctx (monet/get-context canvas "2d"))

(defn get-image [suit number]
  (let [img (js/Image.)
        path (str "images/" (name suit) number ".png")]
    (set! (.-src img) path)
    (add-class ($ img) :ui-state-default)
    img)) 

(def images 
  (into {}
    (for [s suits n numbers]
      [(Card. s n) (get-image s n)])))

(.log js/console images)

(defn attr [e a]
  (.getAttribute e a))

(defn draw-hand [cards]
  (.log js/console "Drawing hand...")
  (.log js/console "sorted cards:")
  (.log js/console (pr-str (sort-cards cards)))
  (doseq [card (sort-cards cards)]
    (let [img (images card)]
      (add-class ($ img) :your-card)
      (append ($ :#your_hand) img))) 
  (.sortable ($ :#your_hand)))
  
(defn draw-other-players [players]
  nil)

(defn draw-board [cards-on-table]
  (let [width (attr canvas "width")
        height (attr canvas "height")]
    (-> ctx
      (monet/fill-style "#493")
      (monet/circle {:x (- width 50) :y (- height 50) :r (/ width 2)}))))

(draw-board nil)

