(defproject metab "0.1.0-SNAPSHOT"
  :description "Simulador metabólico web"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [ring "1.9.6"]
                 [compojure "1.6.2"]
                 [hiccup "1.0.5"]
                 [clj-http "3.12.3"]
                 [cheshire "5.11.0"]]
  :main metab.core
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler metab.handler/app})
