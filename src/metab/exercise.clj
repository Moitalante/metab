(ns metab.exercise
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.string :as str]
            [metab.translations :as t]
            [clojure.tools.logging :as log]))

(def ^:private nutritionix-app-id "1bcc5746")
(def ^:private nutritionix-api-key "40f29da9257857eaccb43a4dd60fd84d")
(def ^:private nutritionix-exercise-api-url "https://trackapi.nutritionix.com/v2/natural/exercise")

(defn- call-nutritionix-for-met [query-ingles]
  (log/info "EXERCISE: Buscando MET na Nutritionix para:" query-ingles)
  (try
    (let [payload {:query query-ingles}
          response (client/post nutritionix-exercise-api-url
                                {:headers {"Content-Type" "application/json", "x-app-id" nutritionix-app-id, "x-app-key" nutritionix-api-key}
                                 :body (json/generate-string payload), :as :json, :throw-exceptions false
                                 :conn-timeout 8000, :socket-timeout 15000})
          status (:status response), body (:body response)]
      (log/debug "EXERCISE: Resposta Nutritionix MET Status:" status "Body:" body)
      (cond
        (not= status 200) {:erro (str "Erro Nutritionix (Status " status "): " (or (:message body) (pr-str body)))}
        (empty? (:exercises body)) {:erro (str "Nenhum exercicio encontrado para: " query-ingles)}
        :else
        (let [exercise-data (first (:exercises body))]
          (if-let [met-valor (:met exercise-data)]
            {:sucesso true, :nome_en (:name exercise-data), :met met-valor}
            {:erro (str "MET nao encontrado para: " query-ingles)}))))
    (catch Exception e {:erro (str "Excecao Nutritionix: " (.getMessage e))})))

(defn obter-info-exercicio
  "Função principal deste namespace. Busca a info de um exercício (nome e MET)."
  [nome-exercicio-pt]
  (log/info "OBTER_INFO_EXERCICIO: Recebido:" nome-exercicio-pt)
  (if (str/blank? nome-exercicio-pt)
    {:erro "Nome do exercicio nao pode ser vazio."}
    (let [trad-pt-en-result (t/traduzir nome-exercicio-pt "pt" "en")]
      (if-let [nome-en (:texto trad-pt-en-result)]
        (let [res-nutritionix (call-nutritionix-for-met nome-en)]
          (if (:sucesso res-nutritionix)
            (let [nome-en-api (:nome_en res-nutritionix)
                  met-valor (:met res-nutritionix)
                  trad-en-pt-result (t/traduzir nome-en-api "en" "pt")
                  nome-pt-final (if-let [t (:texto trad-en-pt-result)] t nome-en-api)]
              {:nome_pt nome-pt-final, :met met-valor})
            res-nutritionix))
        (or (:erro trad-pt-en-result) {:erro "Falha desconhecida na traducao"})))))