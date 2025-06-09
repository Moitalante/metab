(ns metab.translations
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))

(def ^:private mapa-traducoes-confiaveis
  "Mapa para traduções específicas que a API externa erra ou que queremos garantir."
  {:musculacao "weightlifting" 
   :feijao "beans"              
   :peito-de-frango "chicken breast"
   :maca "apple"
   :caminhada "walking"
   :corrida "running"
   :natacao "swimming"
   })

(def ^:private mymemory-api-url "https://api.mymemory.translated.net/get")

(defn- traduzir-via-api [texto lang-de lang-para]
  (log/info "TRADUCAO API: Solicitando '" texto "' de" lang-de "para" lang-para)
  (try
    (let [response (client/get mymemory-api-url
                               {:query-params {"q" texto, "langpair" (str lang-de "|" lang-para)}
                                :as :json, :throw-exceptions false, :conn-timeout 8000, :socket-timeout 15000})
          traducao (get-in (:body response) [:responseData :translatedText])]
      (if (and (= 200 (:status response)) (not (str/blank? traducao)))
        (let [texto-original-lower (str/lower-case texto)
              texto-traduzido-lower (str/lower-case traducao)]
          (if (= texto-original-lower texto-traduzido-lower)
            {:erro (str "Nenhuma traducao especifica encontrada para '" texto "' na API.")}
            (do (log/info "TRADUCAO API: Sucesso '" texto "' -> '" traducao "'")
                {:texto traducao})))
        (do (log/warn "TRADUCAO API: Falha." (:body response))
            {:erro "Falha na API de traducao"})))
    (catch Exception e
      (log/error e "TRADUCAO API: Excecao.")
      {:erro (.getMessage e)})))

(defn traduzir
  "Traduz texto. Primeiro checa o mapa de traduções confiáveis. Se não encontrar, usa a API externa."
  [texto lang-de lang-para]
  (if (and (= lang-de "pt") (= lang-para "en"))
    (let [chave (-> texto str/trim str/lower-case (str/replace #" " "-") keyword)
          traducao-local (get mapa-traducoes-confiaveis chave)]
      (if traducao-local
        (do (log/info "TRADUCAO INTERNA: Usando '" traducao-local "' para '" texto "'")
            {:texto traducao-local})
        (do (log/warn "TRADUCAO INTERNA: Nao encontrado para '" texto "'. Usando API...")
            (traduzir-via-api texto lang-de lang-para))))
    (traduzir-via-api texto lang-de lang-para)))