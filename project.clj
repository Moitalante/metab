(defproject metab "0.1.0-SNAPSHOT"
  :description "Calculadora Metabólica (API e Cliente Terminal)"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.logging "1.3.0"]
                 [ring/ring-core "1.9.6"]
                 [ring/ring-defaults "0.4.0"]
                 [ring/ring-jetty-adapter "1.9.6"]
                 [ring/ring-json "0.5.1"]
                 [compojure "1.7.0"]
                 [metosin/ring-http-response "0.9.3"]
                 [clj-http "3.12.3"]
                 [cheshire "5.11.0"]
                 [clojure.java-time "1.4.2"]]
  :main metab.server
  :aot [metab.server]

  :aliases {"cliente" ["run" "-m" "metab.client"]})