(ns metab.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [metab.handler :refer [app]]
            [ring.middleware.params :refer [wrap-params]]))

(defn -main []
  (run-jetty (wrap-params app) {:port 3000}))