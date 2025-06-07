(ns metab.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [metab.handler :refer [app]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]])
  (:gen-class))

(def final-app
  (wrap-defaults app
                 (assoc-in site-defaults [:security :anti-forgery] false)))

(defn -main []
  (println "Servidor Metab iniciado na porta 3000...")
  (run-jetty final-app {:port 3000}))