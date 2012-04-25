(ns setback.core
  (:refer-clojure :exclude [val])
  (:use 
    [cljs.reader :only [read-string]]
    [jayq.core :only [$ bind inner val]]
    [setback.shared.cards :only [Card]]
    [setback.shared.events :only [react-to trigger make-event]])
  (:require 
    [goog.events :as events]
    [setback.animations :as anim]
    [clojure.browser.repl :as repl]))


(def socket (new js/window.WebSocket "ws://localhost:8080"))

(def table (atom {}))
(def players (atom []))

(def msgbox ($ :#message_box))
(def game ($ :#joingame))
(def namebox ($ :#name))

(bind ($ :#joinbutton) :click
      (fn [e]
        (let [msg (pr-str (make-event :join (val game)))]
          (.send socket msg))))

(bind namebox :change
      (fn [e]
        (let [msg (pr-str (make-event :name-change (val namebox)))]
          (js/alert msg)
          (.send socket msg)))) 

(bind ($ :#check_state) :click
      (fn [e]
        (.send socket (pr-str (make-event :game-state nil)))))

(bind ($ :#leave) :click
      (fn [e]
        (.send socket (pr-str (make-event :leave nil)))))

(defn read-msg [msg]
  (js/alert (.-data msg))
  (read-string (.-data msg)))

(defn update-table [msg]
  (js/alert (pr-str msg))
  (inner msgbox (pr-str msg))
  (let [event (:event msg)
        data (:data msg)]
    (trigger event data)))


(react-to 
  :error
  (fn [data]
    (inner msgbox data)))

(react-to 
  :bet
  (fn [data]
    (if-let [{:keys [pid amount]} data]
      (inner msgbox (str pid " bet " amount)))))

(react-to
  :new-hand
  (fn [data]
    (if-let [cards (:cards data)]
      (anim/draw-hand cards))))

;;(repl/connect "http://localhost:9000/repl")

(set! (.-onmessage socket) #(-> % read-msg))
(set! (.-onclose socket) (fn [] nil))

