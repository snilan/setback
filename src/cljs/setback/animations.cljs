(ns setback.animations
  (:refer-clojure :exclude [val]) 
  (:use 
    [jayq.core :only ($ bind val)])
  (:require 
    [monet.canvas :as canvas]))


(def canvas ($ "#canvas"))

(defn get-image [suit number]
  (let [img (js/Image.)
        path (str (name suit) number ".png")]
    (set! (.-source img) path))) 


(def images nil)

