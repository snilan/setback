(defproject setback "0.1.0-SNAPSHOT"
  :description "Experimental drawing board"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [aleph "0.2.1-beta2"]
                 [ring "1.1.0-beta2"]
                 [hiccup "1.0.0-beta1"]
                 [org.clojure/clojurescript "0.0-1006"]
                 [monet "0.1.0-SNAPSHOT"]
                 [jayq "0.1.0-alpha2"]]
  :source-path "src/clj"
  :extra-classpath-dirs ["src/cljs"]
  :plugins [[lein-cljsbuild "0.1.8"]]
  :cljsbuild {:crossovers [setback.crossover]
              :crossover-path "src/cljs/"
              :crossover-jar false
              :builds
              [{:source-path "src/cljs"
                :compiler {:output-to "resources/public/js/app.js"
                           :pretty-print true
                           :optimizations :simple}}]}
  :ring {:handler setback.core/app}
  :main setback.core)
