(ns setback.core
  (:refer-clojure :exclude [val])
  (:use 
    [cljs.reader :only [read-string]]
    [jayq.core :only [$ bind inner val]]
    [setback.shared.cards :only [Card]])
  (:require 
    [setback.animations :as anim]
    [setback.shared.events :as events] 
    [clojure.browser.repl :as repl]))


(def socket (new js/window.WebSocket "ws://localhost:8080"))

(def table (atom {}))
(def players (atom []))

(def msgbox ($ :#message_box))
(def game ($ :#joingame))
(def namebox ($ :#name))

(bind ($ :#joinbutton) :click
      (fn [e]
        (let [msg (pr-str (events/make-event :join (val game)))]
          (.send socket msg))))

(bind namebox :change
      (fn [e]
        (let [msg (pr-str (events/make-event :name-change (val namebox)))]
          (js/alert msg)
          (.send socket msg)))) 

(bind ($ :#check_state) :click
      (fn [e]
        (.send socket (pr-str (events/make-event :game-state nil)))))

(bind ($ :#leave) :click
      (fn [e]
        (.send socket (pr-str (events/make-event :leave nil)))))

(defn read-msg [msg]
  (.log js/console (.-data msg))
  (read-string (.-data msg)))

(defn update-table [msg]
  (inner msgbox (pr-str msg))
  (let [event (:event msg)
        data (:data msg)]
    (events/trigger event data)))


(events/react-to 
  :error
  (fn [data]
    (inner msgbox data)))

(events/react-to 
  :bet
  (fn [data]
    (if-let [{:keys [pid amount]} data]
      (inner msgbox (str pid " bet " amount)))))

(events/react-to
  :new-hand
  (fn [data]
    (.log js/console (pr-str data))
    (let [cards 
          (map #(Card. (:suit %) (:number %)) data)]
    (anim/draw-hand cards))))

;;(repl/connect "http://localhost:9000/repl")

(set! (.-onmessage socket) #(-> % read-msg update-table))
(set! (.-onclose socket) (fn [] nil))

