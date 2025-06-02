(defproject metab "0.1.0-SNAPSHOT"
  :description "Simulador metabólico web"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.logging "1.3.0"] ; <--- Adicionado/Verifique
                 [ring "1.9.6"] ; Ou a versão que você tinha
                 [ring/ring-defaults "0.4.0"]
                 [compojure "1.6.2"] ; Para compojure.core
                 [hiccup "1.0.5"]
                 [clj-http "3.12.3"]
                 [cheshire "5.11.0"]] ; Usando uma versão um pouco mais nova
  :main metab.core
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler metab.core/final-app})