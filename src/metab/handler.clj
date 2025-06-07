(ns metab.handler
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.util.response :refer [redirect]]
            [clojure.string :as str]
            [metab.view :as view]
            [metab.db :as db]
            [metab.exercise :as exercise]
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

(defn- parse-float [s]
  (let [s-clean (str/trim (str s))]
    (when-not (str/blank? s-clean)
      (try (Float/parseFloat s-clean) (catch NumberFormatException _ nil)))))

(defn- parse-int [s]
  (let [s-clean (str/trim (str s))]
    (when-not (str/blank? s-clean)
      (try (Integer/parseInt s-clean) (catch NumberFormatException _ nil)))))

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
    (let [{:keys [nome peso altura idade sexo]} params
          usuario-data {:nome   (str/trim (str nome))
                        :peso   (parse-float peso)
                        :altura (parse-int altura)
                        :idade  (parse-int idade)
                        :sexo   (or sexo "Nao informado")}]
      (db/salvar-usuario usuario-data)
      (redirect "/usuario")))

  (POST "/adicionar" {params :params}
    (log/info "HANDLER: Recebido POST /adicionar com params:" params)
    (let [{:keys [data tipo nome quantidade peso]} params
          usuario (db/carregar-usuario)]

      (if (or (str/blank? (str data)) (str/blank? (str tipo)) (str/blank? (str nome)) (str/blank? (str quantidade)))
        (pagina-erro "Data, Tipo, Nome e Quantidade sao obrigatorios." 400)
        (try
          (let [qtd (parse-float quantidade)
                tipo-str (name tipo)
                nome-str (str/trim (str nome))
                data-str (str data)]

            (if (or (nil? qtd) (not (pos? qtd)))
              (pagina-erro "A quantidade deve ser um numero positivo." 400)

              (cond
                (= tipo-str "alimento")
                (let [alimento-info (food/buscar-info-alimento-nutritionix nome-str qtd)]
                  (if (:erro alimento-info)
                    (pagina-erro (str "Erro ao buscar info do alimento: " (:erro alimento-info)) 400)
                    (let [alimento-para-db (assoc alimento-info :data data-str)]
                      (if (db/adicionar-alimento alimento-para-db)
                        (redirect "/")
                        (pagina-erro "Nao foi possivel salvar o alimento no DB." 500)))))

                (= tipo-str "exercicio")
                (let [peso-final (or (parse-float peso) (:peso usuario))]
                  (if (nil? peso-final)
                    (pagina-erro "Peso do usuario nao definido. Por favor, cadastre seu peso no Perfil." 400)
                    (let [exercicio-info (exercise/logar-exercicio-e-calcular-kcal nome-str qtd peso-final data-str)]
                      (if (:erro exercicio-info)
                        (pagina-erro (str "Erro ao logar exercicio: " (:erro exercicio-info)) 400)
                        (if (db/adicionar-exercicio exercicio-info)
                          (redirect "/")
                          (pagina-erro "Nao foi possivel salvar o exercicio no DB." 500))))))
                :else
                (pagina-erro "Tipo de registro desconhecido." 400))))

          (catch Exception e
            (log/error e "HANDLER: Erro GRAVE no /adicionar. Params:" params)
            (pagina-erro (str "Ocorreu um erro critico ao processar. Detalhes: " (.getMessage e)) 500)))))

    (GET "/resumo/:dias" [dias]
      (try
        (let [num-dias (parse-int dias)]
          (if (or (nil? num-dias) (neg? num-dias))
            (pagina-erro "Numero de dias deve ser um numero inteiro e positivo." 400)
            (html-response (view/pagina-resumo (db/calcular-resumo num-dias)))))
        (catch Exception e
          (log/error e "HANDLER: Erro no /resumo/:dias. Dias:" dias)
          (pagina-erro (str "Erro ao gerar resumo: " (.getMessage e)) 500))))

    (route/not-found (pagina-erro "Pagina nao encontrada." 404)))