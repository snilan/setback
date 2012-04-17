(ns setback.core
  (:use lamina.core
        aleph.http
        setback.engine
        (ring.middleware resource file-info)
        (hiccup core page))
  (:require [cljs.repl :as repl]
            [cljs.repl.browser :as browser]))

(def receiver (channel))

(def event-types
  #{
    :join
    :make-move
    :leave})

(defn make-event [etype data]
  {:type etype
   :data data})

(defn process-event [pid event]
  (let [event-type (:type event)
        player (get @players pid)
        game (get @games (:game-id player))]
    (when (map? event)
      (case event-type
        :make-move (make-move game pid (:data event))
        :name-change (change-name pid (:data event))
        :game-state (do (println "game: " game) game)
        nil))))

(defn join-game [channel pid game-id]
  (if (= num-players (num-players? game-id))
    (enqueue channel (str (make-event :error "Room is already full")))
    (let [game-channel (named-channel game-id)]
      (siphon
        (map* str 
              (filter* identity
                       (map* (partial process-event pid)
                             (map* read-string channel))))
        game-channel)
      (siphon game-channel channel) 
      (if-not (get @games game-id)
        (swap! games assoc game-id (new-game)))
      (swap! players assoc-in [pid :game-id] game-id) 
      (swap! games update-in [game-id :players] conj (get @players pid)) 
      (if (= (num-players? game-id) num-players)
        (swap! games update-in [game-id] deal-cards))
      (@games game-id))))

(defn wait-for-join [ch pid]
  (receive ch
           (fn [message]
             (println "received:" message)
             (let [msg (read-string message)]
               (if (and (map? msg)
                        (= :join (:type msg))
                        (:data msg))
                 (let [response (join-game ch pid (-> msg :data keyword))]
                   (println "response:" response)
                   (if (not= :error (:type response))
                     (enqueue ch (str response)) 
                     (wait-for-join ch pid)))
                 (wait-for-join ch pid))))))

(defn game-handler [channel request]
  (let [pid (gensym "player")]
    (add-player pid)
    (wait-for-join channel pid)))

(defn dashboard []
  [:div#dashboard
   [:select#card-choice]])

(defn page []
  (html5
   [:head]
   [:body
    [:input#check_state {:type "button" :value "Check game state"}]
    [:br]
    [:input#joinbutton {:type "button" :value "Join Game"}]
    [:input#joingame {:type "text"}]
    [:label {:for "name"} "Enter name:"]
    [:input#name {:type "text" :value "Anonymous"}]
    [:br] 
    [:div#message_box]
    [:div#players
     (for [i (range 3)]
       [:div.player {:width 200 :height 200}])]
    (include-js "/js/jquery.js")    
    (include-js "/js/app.js")]))


(defn sync-app [request]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (page)})

(def wrapped-sync-app
  (-> sync-app
      (wrap-resource "public")
      (wrap-file-info)))

(defn app [channel request]
  ;;(println "request: " request)
  (if (:websocket request)
    (game-handler channel request)
    (enqueue channel (wrapped-sync-app request))))

(defn start-repl []
  (repl/repl (browser/repl-env)))

(defn -main [& args]
  (start-http-server app {:port 8080 :websocket true}))

