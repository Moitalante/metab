(ns metab.food
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;; --- Chaves Nutritionix ---
(def nutritionix-app-id "1bcc5746")
(def nutritionix-api-key "40f29da9257857eaccb43a4dd60fd84d")

;; --- Carregar Mapas de Tradução ---

;; Mapa EN -> PT (o que você chamou de traducao.json)
(defn- carregar-en-pt []
  (try
    (-> (io/resource "public/traducao.json") ; Mantém seu caminho atual
        (slurp)
        (json/parse-string true))
    (catch Exception e
      (println "AVISO: Não foi possível carregar 'public/traducao.json'." (.getMessage e))
      {})))

;; Mapa PT -> EN (NOVO)
(defn- carregar-pt-en []
  (try
    (-> (io/resource "public/pt-en.json") ; Procura em 'resources/pt-en.json'
        (slurp)
        (json/parse-string true))
    (catch Exception e
      (println "AVISO: Não foi possível carregar 'pt-en.json'." (.getMessage e))
      {})))

(def en-pt-map (carregar-en-pt))
(def pt-en-map (carregar-pt-en))

;; --- Função para buscar termo em inglês ---
(defn- buscar-ingles [texto-pt]
  (let [chave (-> texto-pt (str/trim) (str/lower-case) keyword)]
    (get pt-en-map chave texto-pt))) ; Se não achar, usa o próprio texto PT

;; --- Função para buscar termo em português ---
(defn- buscar-portugues [texto-en]
  (let [chave (-> texto-en (str/trim) (str/lower-case) keyword)]
    (get en-pt-map chave texto-en))) ; Se não achar, usa o texto EN

;; --- Função Principal Atualizada ---
(defn buscar-kcal
  "Busca calorias usando PT->EN antes e EN->PT depois."
  [nome-alimento-pt]
  (if (or (nil? nutritionix-app-id) (nil? nutritionix-api-key))
    (do
      (println "ERRO: Chaves da API Nutritionix não configuradas!")
      {:nome nome-alimento-pt :kcal 0.0})
    (try
      ;; 1. Converte PT para EN (ou usa original se não encontrar)
      (let [nome-en-busca (buscar-ingles nome-alimento-pt)
            _ (println "Buscando na Nutritionix por:" nome-en-busca) ; Log de debug

            url "https://trackapi.nutritionix.com/v2/natural/nutrients"
            headers {"x-app-id" nutritionix-app-id
                     "x-app-key" nutritionix-api-key
                     "Content-Type" "application/json"}
            body (json/generate-string {:query nome-en-busca}) ; <-- Usa o nome em Inglês
            resp (client/post url {:headers headers :body body :as :json})
            food-info (get-in resp [:body :foods 0])]

        (if food-info
          (let [nome-en-api (get food-info :food_name nome-alimento-pt)
                kcal-val (get food-info :nf_calories 0.0)]
            ;; 2. Traduz de volta para PT (ou usa original) para salvar/mostrar
            ;;    Usar o nome original digitado é mais simples:
            {:nome nome-alimento-pt ; <-- Retorna o nome que o usuário digitou
             :kcal kcal-val})
          ;; Se não achou na Nutritionix
          {:nome nome-alimento-pt :kcal 0.0}))
      (catch Exception e
        (println "Erro ao buscar na Nutritionix:" (.getMessage e))
        {:nome nome-alimento-pt :kcal 0.0}))))