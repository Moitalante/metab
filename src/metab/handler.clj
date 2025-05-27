(ns metab.handler
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.util.response :refer [redirect]]
            [clojure.string :as str]
            [metab.view :as view]
            [metab.db :as db]
            [metab.met :as met]
            [metab.food :as food]))

(defn html-response
  [html-body]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body html-body})

(defn- pagina-erro [mensagem status-code]
  {:status status-code
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (view/pagina-de-erro mensagem)})

(defn- parse-float [s default]
  (try (Float/parseFloat (str/trim (str s)))
       (catch Exception _ default)))

(defroutes app
  (GET "/" []
    (try
      (let [usuario (db/carregar-usuario)]
        (html-response (view/pagina-principal usuario)))
      (catch Exception e
        (println "!!! EXCECAO NO HANDLER GET / !!!:" (.getMessage e))
        (pagina-erro (str "Erro ao carregar pagina principal: " (.getMessage e)) 500))))

  (GET "/usuario" []
    (html-response (view/pagina-usuario (db/carregar-usuario))))

  (POST "/usuario" {params :params}
    (let [{:keys [nome peso altura]} params
          _ (println "--- DEBUG: /usuario ---")
          _ (println "Parâmetros Recebidos:" params)
          usuario-data {:nome   (str/trim (str nome))
                        :peso   (parse-float peso 70.0)
                        :altura (parse-float altura 170.0)}]
      (db/salvar-usuario usuario-data)
      (redirect "/usuario")))

  (POST "/adicionar" {params :params}
    (let [{:keys [data tipo nome quantidade peso]} params
          _ (println "--- DEBUG: /adicionar ---")
          _ (println "Parâmetros Recebidos:" params)
          usuario (db/carregar-usuario)]
      (if (or (str/blank? (str data)) (str/blank? (str tipo)) (str/blank? (str nome)) (str/blank? (str quantidade)))
        (pagina-erro "Data, Tipo, Nome e Quantidade sao obrigatorios." 400)
        (try
          (let [qtd (parse-float quantidade 0.0)
                tipo-str (name tipo)
                peso-val (if (and (= tipo-str "exercicio") (not (str/blank? (str peso))))
                           (parse-float peso (:peso usuario 78.0))
                           (:peso usuario 78.0))

                info (case tipo-str
                       "alimento"  (let [food-data (food/buscar-kcal (str nome))]
                                     {:nome (:nome food-data)
                                      :kcal (* (/ qtd 100) (:kcal food-data))})
                       "exercicio" {:nome (str nome)
                                    :kcal (met/calcular-kcal (str nome) qtd peso-val)}
                       {:nome (str nome) :kcal 0})

                kcal (:kcal info)
                nome-final (:nome info)
                registro-base {:tipo tipo-str :nome nome-final :quantidade qtd :kcal kcal}
                registro-final (if (= "exercicio" tipo-str)
                                 (assoc registro-base :peso peso-val)
                                 registro-base)]
            (db/adicionar-registro (str data) registro-final)
            (redirect "/"))
          (catch Exception e
            (println "Erro no handler /adicionar (catch):" (.getMessage e) "Params:" params)
            (pagina-erro (str "Ocorreu um erro ao processar. Detalhes: " (.getMessage e)) 500)))))

    ;; --- ROTA DE RESUMO (APENAS A VERSÃO ANTIGA) ---
    (GET "/resumo/:dias" [dias]
      (try
        (let [num-dias (Integer/parseInt dias)]
          (html-response (view/pagina-resumo (db/calcular-resumo num-dias))))
        (catch NumberFormatException _
          (pagina-erro "Numero de dias invalido." 400))))
    ;; REMOVIDA a rota /resumo/:inicio/:fim

    (route/not-found (pagina-erro "Pagina nao encontrada." 404))))