(ns metab.db
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]))

;; --- O ESTADO DA APLICAÇÃO AGORA VIVE NESTE ATOM ---
(def banco-de-dados
  (atom {;; Valores numéricos começam como nil
         :usuario {:nome "" :peso nil :altura nil :idade nil :sexo "Nao informado"}
         :alimentos []
         :exercicios []}))

;; Reinicia o banco de dados para o estado inicial (útil para testes)
(defn resetar-banco! []
  (reset! banco-de-dados {:usuario {:nome "" :peso nil :altura nil :idade nil :sexo "Nao informado"}
                          :alimentos []
                          :exercicios []}))

;; --- Funções para Usuário (interagem com o atom) ---
(defn carregar-usuario []
  (:usuario @banco-de-dados))

(defn salvar-usuario [usuario-data]
  (swap! banco-de-dados assoc :usuario usuario-data)
  (log/info "Usuario salvo:" usuario-data)
  usuario-data)

;; --- Funções para Alimentos ---
(defn ler-alimentos []
  (:alimentos @banco-de-dados))

(defn adicionar-alimento
  "Adiciona um alimento ao estado. Espera um mapa com os dados do alimento."
  [alimento-map]
  (let [alimento-com-id (assoc alimento-map :id (System/currentTimeMillis))]
    (swap! banco-de-dados update :alimentos conj alimento-com-id)
    (log/info "Alimento adicionado:" alimento-com-id)
    alimento-com-id))

;; --- Funções para Exercícios ---
(defn ler-exercicios []
  (:exercicios @banco-de-dados))

(defn adicionar-exercicio
  "Adiciona um exercício ao estado. Espera um mapa com os dados do exercício."
  [exercicio-map]
  (let [exercicio-com-id (assoc exercicio-map :id (System/currentTimeMillis))]
    (swap! banco-de-dados update :exercicios conj exercicio-com-id)
    (log/info "Exercicio adicionado:" exercicio-com-id)
    exercicio-com-id))

;; --- Função de Resumo (lê do atom) ---
(defn calcular-resumo [dias]
  (let [alimentos (ler-alimentos)
        exercicios (ler-exercicios)
        num-dias (if (and (number? dias) (pos? dias)) dias 7)

        filtra-por-data (fn [col]
                          (let [datas (->> col (map :data) distinct sort (take-last num-dias))]
                            (filter #(some #{(:data %)} datas) col)))

        alimentos-filtrados (filtra-por-data alimentos)
        exercicios-filtrados (filtra-por-data exercicios)

        kcal-consumidas (->> alimentos-filtrados (map :kcal_consumidas) (reduce + 0.0))
        kcal-gastas     (->> exercicios-filtrados (map :kcal_gastas) (reduce + 0.0))]
    (log/info "Calculando resumo para os ultimos" num-dias "dias de atividade.")
    {:kcal-consumidas kcal-consumidas
     :kcal-gastas kcal-gastas
     :diferenca (- kcal-consumidas kcal-gastas)}))