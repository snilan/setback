(ns setback.core
  (:refer-clojure :exclude [val])
  (:use 
    [cljs.reader :only [read-string]]
    [jayq.core :only [$ bind inner val]])
  (:require 
    [goog.events :as events]
    [clojure.browser.repl :as repl]))

;; set up the REPL

;; (repl/connect "http://localhost:9000/repl")

(def socket (new js/window.WebSocket "ws://localhost:8080"))

(def table (atom {}))
(def players (atom []))

(def msgbox ($ :#message_box))
(def game ($ :#joingame))
(def namebox ($ :#name))


(defn make-event [etype data]
  {:type etype
   :data data})

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
        (.send socket (pr-str (make-event :game-state :garbage)))))

(defn read-msg [msg]
  (js/alert (.-data msg))
  (read-string (.-data msg)))

(defn add-player [data]
  data)

(defn remove-player [data]
  data)

(defn make-move [data]
  data)

(defn hand-over [data]
  data)

(defn new-hand [data]
  data)
 
(defn update-table [data]
  (inner msgbox (pr-str data)))

(set! (.-onmessage socket) #(-> % read-msg update-table))

(set! (.-onclose socket) (fn [] 
                           (js/alert "socket closed")))

(defn listen [element event callback]
  (events/listen element (name event) callback))

