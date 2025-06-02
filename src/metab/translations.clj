;; src/metab/translations.clj
(ns metab.translations
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.tools.logging :as log])) ; Adicionado para logging

(def ^:private mymemory-api-url "https://api.mymemory.translated.net/get")

(defn traduzir
  "Traduz o texto de um idioma de origem para um idioma de destino usando a MyMemory API.
   Retorna um mapa com :texto-traduzido em caso de sucesso, ou :erro em caso de falha."
  [texto lang-de lang-para]
  (log/info (str "TRADUCAO: Solicitando traducao de '" texto "' de " lang-de " para " lang-para))
  (if (str/blank? texto)
    (do (log/warn "TRADUCAO: Texto para traducao vazio.")
        {:erro "Texto para traducao nao pode ser vazio."})
    (try
      (let [langpair (str lang-de "|" lang-para)
            ;; Adicionando um email no parâmetro 'de' pode aumentar os limites da MyMemory
            ;; Pode ser um email genérico ou o do usuário da aplicação se você tiver.
            ; de-param "seu-email-para-mymemory@example.com" 
            response (client/get mymemory-api-url
                                 {:query-params {"q" texto
                                                 "langpair" langpair
                                                 ;; "de" de-param ; Descomente se quiser usar um email
                                                 }
                                  :throw-exceptions false
                                  :as :json
                                  :conn-timeout 8000  ; Aumentado
                                  :socket-timeout 15000}) ; Aumentado
            status (:status response)
            body (:body response)]
        (log/debug "TRADUCAO: Resposta da API MyMemory Status:" status "Body:" body)
        (cond
          (not= status 200)
          (do (log/error "TRADUCAO: API MyMemory retornou status" status "Body:" body)
              {:erro (str "API de Traducao MyMemory retornou status " status) :detalhes body})

          (or (nil? body) (nil? (:responseData body)) (nil? (get-in body [:responseData :translatedText])))
          (do (log/warn "TRADUCAO: Resposta da API MyMemory inesperada. Body:" body)
              {:erro "Resposta da API de Traducao MyMemory inesperada ou sem texto traduzido." :detalhes body})

          :else
          (let [texto-traduzido (get-in body [:responseData :translatedText])]
            (if (str/blank? texto-traduzido)
              (do (log/warn "TRADUCAO: API MyMemory retornou texto traduzido vazio. Body:" body)
                  {:erro "API de Traducao retornou texto traduzido vazio." :detalhes body})
              (do (log/info "TRADUCAO: '" texto "' -> '" texto-traduzido "'")
                  {:texto-traduzido texto-traduzido})))))
      (catch Exception e
        (log/error e "TRADUCAO: Excecao ao chamar API de Traducao.")
        {:erro (str "Excecao ao chamar API de Traducao: " (.getMessage e))}))))