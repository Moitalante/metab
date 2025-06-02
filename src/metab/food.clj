;; src/metab/food.clj
(ns metab.food
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.string :as str]
            [metab.translations :as t]
            [clojure.tools.logging :as log]))

;; --- Chaves Nutritionix ---
(def nutritionix-app-id "1bcc5746")
(def nutritionix-api-key "40f29da9257857eaccb43a4dd60fd84d")

(defn- call-nutritionix-food-api [query-en quantidade-g nome-original-pt]
  (log/info "CALL_NUTRITIONIX_FOOD_API - Recebido: query-en:" (pr-str query-en) ", quantidade-g:" (pr-str quantidade-g) ", nome-original-pt:" (pr-str nome-original-pt))

  ;; Validação robusta dos inputs da função interna
  (if (or (str/blank? (str query-en))       ; Garante que query-en não é nil ou vazia
          (nil? quantidade-g)
          (not (number? quantidade-g))
          (not (pos? quantidade-g)))    ; Garante que quantidade-g é um número positivo
    (do
      (log/error "CALL_NUTRITIONIX_FOOD_API: Input invalido. query-en:" query-en "quantidade-g:" quantidade-g)
      {:erro "Input invalido para chamada interna da API Nutritionix (query ou quantidade)."
       :detalhes {:query-en query-en :quantidade-g quantidade-g}})
    (try
      (let [query-nutritionix (str quantidade-g "g " query-en)
            _ (log/info "FOOD: Query formatada para Nutritionix Food API:" query-nutritionix)
            url "https://trackapi.nutritionix.com/v2/natural/nutrients"
            headers {"x-app-id" nutritionix-app-id
                     "x-app-key" nutritionix-api-key
                     "Content-Type" "application/json"}
            body-req (json/generate-string {:query query-nutritionix}) ; Corpo da requisição
            resp (client/post url {:headers headers
                                   :body body-req ; Envia a string JSON
                                   :as :json
                                   :throw-exceptions false
                                   :conn-timeout 8000
                                   :socket-timeout 15000})
            status (:status resp)
            body-resp (:body resp)
            food-info (get-in body-resp [:foods 0])]
        (log/debug "FOOD: Resposta Nutritionix Food Status:" status "Body:" (pr-str body-resp))
        (cond
          (not= status 200)
          (do (log/error "FOOD: Erro na API Nutritionix Food status" status ":" (pr-str body-resp))
              {:erro (str "Nutritionix Food status " status) :detalhes (pr-str body-resp)})

          (nil? food-info)
          (do (log/warn "FOOD: Alimento '" query-nutritionix "' nao encontrado na Nutritionix. Resposta:" (pr-str body-resp))
              {:erro "Nao encontrado Nutritionix Food" :detalhes (pr-str body-resp)})
          :else
          (let [nome-en-api (get food-info :food_name query-en) ; Fallback para query-en se não houver :food_name
                kcal_total (get food-info :nf_calories 0.0)
                trad-en-pt-result (t/traduzir nome-en-api "en" "pt")
                nome-pt-final (if (:erro trad-en-pt-result)
                                (do (log/warn "FOOD: Erro ao traduzir EN->PT para" nome-en-api ". Usando nome original PT.")
                                    nome-original-pt) ; Fallback para o nome original PT
                                (:texto-traduzido trad-en-pt-result))]
            (log/info "FOOD: Detalhes API - " nome-pt-final " (" nome-en-api "), Kcal Total:" kcal_total "para" quantidade-g "g")
            {:nome_pt nome-pt-final
             :quantidade_g quantidade-g
             :kcal_consumidas kcal_total})))
      (catch Exception e
        (log/error e "FOOD: Excecao DENTRO de call-nutritionix-food-api. Query-en:" query-en)
        {:erro (str "Excecao interna em call-nutritionix: " (.getMessage e))}))))

(defn buscar-info-alimento-nutritionix
  "Busca info na Nutritionix. Retorna {:nome_pt ..., :kcal_consumidas ...} ou {:erro ...}"
  [nome-alimento-pt quantidade-g]
  (log/info "BUSCAR_INFO_ALIMENTO_NUTRITIONIX - Recebido: nome-alimento-pt:" (pr-str nome-alimento-pt) ", quantidade-g:" (pr-str quantidade-g))
  (if (or (str/blank? (str nome-alimento-pt))
          (nil? quantidade-g)
          (not (number? quantidade-g))
          (not (pos? quantidade-g)))
    (do (log/error "BUSCAR_INFO_ALIMENTO_NUTRITIONIX: Inputs invalidos.")
        {:erro "Nome do alimento e quantidade (positiva) sao obrigatorios."
         :detalhes {:nome-alimento-pt nome-alimento-pt :quantidade-g quantidade-g}})
    (if (or (str/blank? nutritionix-app-id) (str/blank? nutritionix-api-key)
            (= "SUA_APP_ID_AQUI" nutritionix-app-id)
            (= "SUA_API_KEY_AQUI" nutritionix-api-key))
      (do (log/error "FOOD: Chaves da API Nutritionix (alimento) nao configuradas.")
          {:erro "Chaves API Nutritionix (alimento) nao configuradas."})
      (let [trad-pt-en-result (t/traduzir nome-alimento-pt "pt" "en")]
        (if (:erro trad-pt-en-result)
          (do (log/warn "FOOD: Erro ao traduzir PT->EN para" nome-alimento-pt ":" (:erro trad-pt-en-result) ". Tentando com nome original PT.")
              (call-nutritionix-food-api nome-alimento-pt quantidade-g nome-alimento-pt))
          (call-nutritionix-food-api (:texto-traduzido trad-pt-en-result) quantidade-g nome-alimento-pt))))))