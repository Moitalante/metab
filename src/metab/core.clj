(ns metab.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [metab.handler :refer [app]] ; Suas rotas Compojure
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

;; --- MIDDLEWARE DE LOGGING (para vermos o que está acontecendo) ---
(defn logging-middleware [handler id-log]
  (fn [request]
    (println (str "\n>>> REQUISICAO (" id-log "):") request)
    (let [response (handler request)]
      (println (str "\n<<< RESPOSTA (" id-log "):")
               (if (:body response)
                 ;; Mostra tipo e tamanho do body para não poluir com HTML grande
                 (assoc response :body (str (type (:body response))
                                            ", tamanho: "
                                            (count (str (:body response)))
                                            " caracteres"))
                 response))
      response)))

(def final-app
  (-> app ; Suas rotas Compojure de metab.handler
      (logging-middleware "ANTES_DE_WRAP_DEFAULTS") ; Vê o que 'app' retorna
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      (logging-middleware "NO_TOPO_DA_PILHA"))) ; Vê a requisição original

(defn -main []
  (println "Servidor Metab iniciado na porta 3000...")
  (run-jetty final-app {:port 3000}))