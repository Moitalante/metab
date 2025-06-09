(ns metab.server
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [metab.handler :refer [api-routes]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]])
  (:gen-class))

(def app
  (-> #'api-routes
      (wrap-json-body {:keywords? true :json-nested true})
      wrap-json-response
      (wrap-defaults api-defaults)))

(defn -main [& _]
  (println "Servidor da API Metab iniciado na porta 3000...")
  (run-jetty #'app {:port 3000 :join? false}))