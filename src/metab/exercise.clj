(ns metab.exercise
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.string :as str]
            [metab.translations :as t]
            [clojure.tools.logging :as log]))

(def ^:private nutritionix-app-id "1bcc5746")
(def ^:private nutritionix-api-key "40f29da9257857eaccb43a4dd60fd84d")
(def ^:private nutritionix-exercise-api-url "https://trackapi.nutritionix.com/v2/natural/exercise")

(defn- call-nutritionix-exercise-api [exercise-query-ingles peso-kg duracao-min]
  (log/info "EXERCISE: Chamando Nutritionix Exercise API com query:" exercise-query-ingles "peso:" peso-kg "duracao:" duracao-min)
  (if (or (str/blank? nutritionix-app-id) (str/blank? nutritionix-api-key)
          (= "1bcc5746" nutritionix-app-id)
          (= "40f29da9257857eaccb43a4dd60fd84d" nutritionix-api-key))
    (do (log/error "EXERCISE: Chaves da API Nutritionix (exercicio) nao configuradas.")
        {:erro "Chaves da API Nutritionix (exercicio) nao configuradas."})
    (try
      (let [query-completa (str exercise-query-ingles " for " duracao-min " minutes")
            payload {:query query-completa :weight_kg peso-kg}
            response (client/post nutritionix-exercise-api-url
                                  {:headers {"Content-Type" "application/json"
                                             "x-app-id" nutritionix-app-id
                                             "x-app-key" nutritionix-api-key}
                                   :body (json/generate-string payload)
                                   :throw-exceptions false
                                   :as :json
                                   :conn-timeout 8000
                                   :socket-timeout 15000})
            status (:status response)
            body (:body response)]
        (log/debug "EXERCISE: Resposta Nutritionix Status:" status "Body:" body)
        (cond
          (not= status 200)
          (do (log/error "EXERCISE: Erro Nutritionix (Status " status "): " body)
              {:erro (str "Erro Nutritionix Exercicio (Status " status "): " (or (:message body) (pr-str body)))})

          (or (nil? body) (empty? (:exercises body)))
          (do (log/warn "EXERCISE: Nenhum exercicio encontrado para '" query-completa "'. Body:" body)
              {:erro (str "Nenhum exercicio encontrado para: " query-completa) :resposta_api body})
          :else
          (let [exercise-data (first (:exercises body))]
            (if (:met exercise-data)
              {:sucesso true :dados-exercicio exercise-data}
              (do (log/warn "EXERCISE: MET nao encontrado para '" query-completa "'. Data:" exercise-data)
                  {:erro (str "MET nao encontrado para: " query-completa) :resposta_api body})))))
      (catch Exception e
        (log/error e "EXERCISE: Excecao ao chamar Nutritionix Exercise API.")
        {:erro (str "Excecao Nutritionix Exercicio: " (.getMessage e))}))))

(defn- fetch-info-exercicio
  [query-em-portugues peso-kg duracao-min]
  (if (str/blank? query-em-portugues)
    {:erro "Nome do exercicio para busca de MET nao pode ser vazio."}
    (let [trad-pt-en-result (t/traduzir query-em-portugues "pt" "en")]
      (if (:erro trad-pt-en-result)
        (assoc trad-pt-en-result :etapa "traducao pt->en exercicio")
        (let [query-ingles (:texto-traduzido trad-pt-en-result)
              res-nutritionix (call-nutritionix-exercise-api query-ingles peso-kg duracao-min)]
          (if (or (:erro res-nutritionix) (not (:sucesso res-nutritionix)))
            (assoc res-nutritionix :etapa "chamada nutritionix exercicio")
            (let [nome-ingles-api (get-in res-nutritionix [:dados-exercicio :name])
                  met (get-in res-nutritionix [:dados-exercicio :met])
                  calorias-direto-api (get-in res-nutritionix [:dados-exercicio :nf_calories])]
              (if (and nome-ingles-api (or met calorias-direto-api))
                (let [trad-en-pt-result (t/traduzir nome-ingles-api "en" "pt")
                      nome-pt-final (if (:erro trad-en-pt-result)
                                      (do (log/warn "EXERCISE: Erro ao traduzir EN->PT para" nome-ingles-api ". Usando nome original PT.")
                                          query-em-portugues)
                                      (:texto-traduzido trad-en-pt-result))]
                  (log/info "EXERCISE: Detalhes API - " nome-pt-final " (" nome-ingles-api "), MET:" met ", Kcal (API):" calorias-direto-api)
                  {:nome_pt nome-pt-final
                   :nome_en_api nome-ingles-api
                   :met met
                   :calorias_direto_api calorias-direto-api})
                (do (log/warn "EXERCISE: Nome Ingles ou MET/Calorias nao encontrado na Nutritionix. Detalhes:" res-nutritionix)
                    {:erro "Nome Ingles ou MET/Calorias nao encontrado na Nutritionix." :etapa "processar nutritionix exercicio"}))))))))

  (defn- calcular-kcal-com-met [met peso-kg duracao-min]
    (if (or (nil? met) (nil? peso-kg) (nil? duracao-min) (<= met 0.0) (<= peso-kg 0.0) (<= duracao-min 0.0))
      0.0
      (float (* 0.0175 met peso-kg duracao-min))))

  (defn logar-exercicio-e-calcular-kcal
    [nome-exercicio-pt duracao-min peso-kg data-exercicio]
    (log/info "EXERCISE: Logando exercicio - " nome-exercicio-pt ", Dur:" duracao-min ", Peso:" peso-kg ", Data:" data-exercicio)
    (if (or (str/blank? nome-exercicio-pt) (nil? duracao-min) (<= duracao-min 0)
            (nil? peso-kg) (<= peso-kg 0) (str/blank? data-exercicio))
      {:erro "Nome, duracao, peso e data do exercicio sao obrigatorios e validos."}
      (let [info-exercicio (fetch-info-exercicio nome-exercicio-pt peso-kg duracao-min)]
        (if (:erro info-exercicio)
          (assoc info-exercicio :etapa "logar_exercicio_busca_info")
          (let [met-valor (:met info-exercicio)
                calorias-api (:calorias_direto_api info-exercicio)
                calorias (if (and calorias-api (> calorias-api 0))
                           (do (log/info "EXERCISE: Usando calorias diretas da API:" calorias-api)
                               calorias-api)
                           (do (log/info "EXERCISE: Calculando calorias com MET:" met-valor)
                               (calcular-kcal-com-met met-valor peso-kg duracao-min)))]
            (log/info "EXERCISE: Calorias calculadas/obtidas:" calorias)
            {:nome_pt (:nome_pt info-exercicio)
             :duracao_min duracao-min
             :peso_kg peso-kg
             :kcal_gastas (float calorias)
             :data data-exercicio
             :met_usado met-valor
             :nome_en_api (:nome_en_api info-exercicio)})))))