;; src/metab/handler.clj
(ns metab.handler
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.util.response :refer [redirect]]
            [clojure.string :as str]
            [metab.view :as view]
            [metab.db :as db]
            [metab.exercise :as exercise] ; Mudou de metab.met
            [metab.food :as food]
            [clojure.tools.logging :as log]))

(defn html-response [html-body]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body html-body})

(defn- pagina-erro [mensagem status-code]
  {:status status-code
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (view/pagina-de-erro mensagem)})

(defn- parse-float [s default-value]
  (try (Float/parseFloat (str/trim (str s))) (catch Exception _ default-value)))

(defroutes app
  (GET "/" []
    (try
      (let [usuario (db/carregar-usuario)]
        (html-response (view/pagina-principal usuario)))
      (catch Exception e
        (log/error e "HANDLER: EXCECAO NO GET /")
        (pagina-erro (str "Erro ao carregar pagina principal: " (.getMessage e)) 500))))

  (GET "/usuario" []
    (html-response (view/pagina-usuario (db/carregar-usuario))))

  (POST "/usuario" {params :params}
    (log/info "HANDLER: Recebido POST /usuario com params:" params)
    (let [{:keys [nome peso altura]} params
          usuario-data {:nome   (str/trim (str nome))
                        :peso   (parse-float peso 70.0)
                        :altura (parse-float altura 170.0)}]
      (if (db/salvar-usuario usuario-data)
        (redirect "/usuario")
        (pagina-erro "Nao foi possivel salvar dados do usuario." 500))))

  (POST "/adicionar" {params :params}
    (log/info "HANDLER: Recebido POST /adicionar com params:" params)
    (let [{:keys [data tipo nome quantidade peso]} params
          usuario (db/carregar-usuario)]

      (if (or (str/blank? (str data)) (str/blank? (str tipo)) (str/blank? (str nome)) (str/blank? (str quantidade)))
        (pagina-erro "Data, Tipo, Nome e Quantidade sao obrigatorios." 400)
        (try
          (let [qtd (parse-float quantidade 0.0)
                tipo-str (name tipo)
                nome-str (str/trim (str nome))
                data-str (str data)] ; Garantir que data é string

            (log/info "HANDLER: Adicionando" tipo-str ":" nome-str "Qtd:" qtd "Data:" data-str)

            (cond
              (= tipo-str "alimento")
              (let [alimento-info-api (food/buscar-info-alimento-nutritionix nome-str qtd)]
                (if (:erro alimento-info-api)
                  (pagina-erro (str "Erro ao buscar info do alimento: " (:erro alimento-info-api)) 400)
                  (let [alimento-para-db (assoc alimento-info-api :data data-str)]
                    (if (db/adicionar-alimento alimento-para-db)
                      (redirect "/")
                      (pagina-erro "Nao foi possivel salvar o alimento." 500)))))

              (= tipo-str "exercicio")
              (let [peso-final (parse-float peso (:peso usuario 78.0))
                    exercicio-info-api (exercise/logar-exercicio-e-calcular-kcal nome-str qtd peso-final data-str)]
                (if (:erro exercicio-info-api)
                  (pagina-erro (str "Erro ao logar exercicio: " (:erro exercicio-info-api)) 400)
                  (do
                    ;; logar-exercicio-e-calcular-kcal já retorna o mapa formatado para o db
                    ;; e db/adicionar-exercicio foi chamado dentro dele no "Projeto API"
                    ;; Agora, logar-exercicio-e-calcular-kcal retorna o mapa, e nós adicionamos ao DB
                    (if (db/adicionar-exercicio exercicio-info-api)
                      (redirect "/")
                      (pagina-erro "Nao foi possivel salvar o exercicio." 500)))))
              :else
              (pagina-erro "Tipo de registro desconhecido." 400)))

          (catch Exception e
            (log/error e "HANDLER: Erro GRAVE no /adicionar. Params:" params)
            (pagina-erro (str "Ocorreu um erro critico ao processar. Detalhes: " (.getMessage e)) 500)))))

    (GET "/resumo/:dias" [dias]
      (try
        (let [num-dias (Integer/parseInt dias)]
          (if (neg? num-dias)
            (pagina-erro "Numero de dias deve ser positivo." 400)
            (html-response (view/pagina-resumo (db/calcular-resumo num-dias)))))
        (catch NumberFormatException _
          (pagina-erro "Numero de dias invalido. Use um numero." 400))
        (catch Exception e
          (log/error e "HANDLER: Erro no /resumo/:dias. Dias:" dias)
          (pagina-erro (str "Erro ao gerar resumo: " (.getMessage e)) 500))))

    (route/not-found (pagina-erro "Pagina nao encontrada." 404))))