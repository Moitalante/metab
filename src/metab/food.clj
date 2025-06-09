(ns metab.food
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.string :as str]
            [metab.translations :as t]
            [clojure.tools.logging :as log]))

(def ^:private nutritionix-app-id "1bcc5746")
(def ^:private nutritionix-api-key "40f29da9257857eaccb43a4dd60fd84d")
(def ^:private nutritionix-food-api-url "https://trackapi.nutritionix.com/v2/natural/nutrients")

(defn- call-nutritionix-food-api [query-en quantidade-g nome-original-pt]
  (try
    (let [query-nutritionix (str quantidade-g "g " query-en)
          _ (log/info "FOOD: Query para Nutritionix Food API:" query-nutritionix)
          headers {"x-app-id" nutritionix-app-id, "x-app-key" nutritionix-api-key}
          body-req (json/generate-string {:query query-nutritionix})
          resp (client/post nutritionix-food-api-url
                            {:headers headers, :content-type :json, :body body-req, :as :json,
                             :throw-exceptions false, :conn-timeout 8000, :socket-timeout 15000})
          status (:status resp)
          body-resp (:body resp)
          food-info (get-in body-resp [:foods 0])]
      (log/debug "FOOD: Resposta Nutritionix Food Status:" status "Body:" (pr-str body-resp))
      (cond
        (not= status 200) {:erro (str "Nutritionix Food status " status), :detalhes (pr-str body-resp)}
        (nil? food-info) {:erro "Nao encontrado na Nutritionix Food", :detalhes (pr-str body-resp)}
        :else
        (let [kcal_total (get food-info :nf_calories 0.0)
              nome-en-api (get food-info :food_name query-en)
              trad-en-pt-result (t/traduzir nome-en-api "en" "pt")
              nome-pt-final (if-let [traduzido (:texto trad-en-pt-result)]
                              traduzido
                              (do (log/warn "FOOD: Erro ao traduzir EN->PT para" nome-en-api ". Usando nome original.")
                                  nome-original-pt))]
          (log/info "FOOD: Detalhes API - " nome-pt-final " (" nome-en-api "), Kcal Total:" kcal_total "para" quantidade-g "g")
          {:nome_pt nome-pt-final, :quantidade_g quantidade-g, :kcal_consumidas (float kcal_total)})))
    (catch Exception e
      (log/error e "FOOD: Excecao ao buscar alimento na Nutritionix.")
      {:erro (.getMessage e)})))

(defn obter_info_alimento [nome-alimento-pt quantidade-g]
  (log/info "OBTER_INFO_ALIMENTO: Recebido: nome:" (pr-str nome-alimento-pt) ", qtd:" (pr-str quantidade-g))
  (if (or (str/blank? (str nome-alimento-pt)) (not (and (number? quantidade-g) (pos? quantidade-g))))
    {:erro "Nome do alimento e quantidade (positiva) sao obrigatorios."}
    (let [trad-pt-en-result (t/traduzir nome-alimento-pt "pt" "en")]
      (if-let [nome-en (:texto trad-pt-en-result)]
        (call-nutritionix-food-api nome-en quantidade-g nome-alimento-pt)
        (or (:erro trad-pt-en-result) {:erro "Falha desconhecida na traducao PT->EN"})))))