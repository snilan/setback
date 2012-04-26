(ns setback.core
  (:use lamina.core
        aleph.http
        setback.engine
        setback.shared.events
        (ring.middleware resource file-info)
        (hiccup core page))
  (:require [cljs.repl :as repl]
            [cljs.repl.browser :as browser]))


(defn process-message [pid msg]
  (let [event (:event msg)
        player (get @player-info pid)
        game (get @games (:game-id player))]
    (when (map? msg)
      (case event
        :make-move 
          (make-move game pid (:data msg))

        :name-change 
          (do
            (change-name pid (:data msg))
            (make-event event (@player-info pid)))

        :game-state 
          (do (println "game: " game) game)

        :bet 
          (make-bet game pid (:data msg))

        :chat 
          (make-event event (str (get-name pid) ": " (:data msg)))

        (make-event :error :unknown-event)))))

(defn record-receive [msg]
  (do
    (println "Sent by " (:src msg) ":" msg)
    msg))

(defn record-send [msg pid]
  (do
    (println "Sent to " pid ":" msg)
    msg))

(defn join-game [ch pid game-id]
  (if (game-is-full? game-id)
    (enqueue ch (str (make-event :error "Room is already full")))
    (let [game-ch (named-channel game-id (partial make-new-game game-id))] ;; calls make-new-game if channel doesn't exist
      (siphon
        (filter* (comp not nil?)
                 (map* (partial process-message pid)
                       (map* record-receive
                             (map* #(assoc % :src pid)
                                   (filter* map? (map* read-string ch))))))
        game-ch)

      (siphon
        (map* #(record-send % pid) 
        (map* 
          (comp str (partial hide-info pid))
          (filter* (partial should-know? pid) game-ch)))
        ch)
      (add-player-to-game pid game-id)
      (enqueue game-ch (str (make-event :join pid)))
      (when (game-is-full? game-id)
        (swap! games update-in [game-id] deal-cards)
        (enqueue game-ch (make-event :new-hand (@games game-id)))))))
      

(defn wait-for-join [ch pid]
  (receive ch
           (fn [message]
             (let [msg (read-string message)]
               (println "found:" msg)
               (if (and (map? msg)
                        (= :join (:event msg))
                        (:data msg))
                 (let [response (join-game ch pid (-> msg :data keyword))]
                   (if (= :error (:event response))
                     (wait-for-join ch pid)))
                 (wait-for-join ch pid))))))

(defn game-handler [channel request]
  (let [pid (gensym "player")]
    (add-player-to-store pid channel)
    (wait-for-join channel pid)))

(defn dashboard []
  [:div#dashboard
   [:select#card-choice]])

(defn page []
  (html5
   [:head
    [:title "Setback"]
    [:meta {:charset "UTF-8"}]]
   [:body
    [:input#check_state {:type "button" :value "Check game state"}]
    [:br]
    [:input#joinbutton {:type "button" :value "Join Game"}]
    [:input#joingame {:type "text"}]
    [:label {:for "name"} "Enter name:"]
    [:input#name {:type "text" :value "Anonymous"}]
    [:br] 
    [:div#message_box]
    [:canvas#canvas {:height 200 :width 200}]
    [:div#your_hand {:height 200 :width 300}]
    [:input#chat_input {:type "text"}]
    [:div#chat_box {:height 200 :width 400}]
    [:input#leave {:type "button" :value "Leave Game"}]
    (include-js "/js/jquery.js")
    (include-js "/js/jquery-ui.js")
    (include-js "/js/client.js")]))


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

