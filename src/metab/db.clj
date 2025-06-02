;; src/metab/db.clj
(ns metab.db
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]))

(def db-dir-path "data")
(def user-db-path (str db-dir-path "/usuario.json"))
(def alimentos-db-path (str db-dir-path "/alimentos.json"))
(def exercicios-db-path (str db-dir-path "/exercicios.json"))

(defn- garantir-diretorio-db! []
  (let [db-dir (io/file db-dir-path)]
    (when-not (.exists db-dir)
      (try
        (.mkdirs db-dir)
        (log/info (str "Diretorio '" db-dir-path "' criado."))
        (catch Exception e
          (log/error e (str "Falha ao criar diretorio '" db-dir-path "'.")))))))

(defn- ler-lista-do-arquivo [path-arquivo]
  (garantir-diretorio-db!)
  (let [f (io/file path-arquivo)]
    (if (and (.exists f) (> (.length f) 0))
      (try
        (json/parse-string (slurp f) true)
        (catch Exception e
          (log/error e (str "ERRO ao ler/parsear " path-arquivo "."))
          []))
      [])))

(defn- salvar-lista-no-arquivo [path-arquivo dados-lista]
  (garantir-diretorio-db!)
  (try
    (io/make-parents path-arquivo)
    (spit path-arquivo (json/generate-string dados-lista {:pretty true}))
    true ; Indica sucesso
    (catch Exception e
      (log/error e (str "AVISO: Falha ao salvar o arquivo " path-arquivo "."))
      false))) ; Indica falha

;; --- Funções para Usuário ---
(defn carregar-usuario []
  (let [f (io/file user-db-path)
        defaults {:nome "" :peso 70.0 :altura 170.0}]
    (if (and (.exists f) (> (.length f) 0))
      (try
        (json/parse-string (slurp f) true)
        (catch Exception e
          (log/error e "ERRO ao ler/parsear usuario.json.")
          defaults))
      defaults)))

(defn salvar-usuario [usuario-data]
  (salvar-lista-no-arquivo user-db-path usuario-data))

;; --- Funções para Alimentos ---
(defn ler-alimentos [] (ler-lista-do-arquivo alimentos-db-path))
(defn salvar-alimentos [alimentos] (salvar-lista-no-arquivo alimentos-db-path alimentos))

(defn adicionar-alimento
  "Espera um mapa com :nome_pt, :quantidade_g, :kcal_consumidas, :data"
  [alimento-map]
  (let [alimentos-atuais (ler-alimentos)
        alimento-com-id (assoc alimento-map :id (System/currentTimeMillis))]
    (when (salvar-alimentos (conj alimentos-atuais alimento-com-id))
      alimento-com-id))) ; Retorna o alimento salvo com ID se o salvamento for bem-sucedido

;; --- Funções para Exercícios ---
(defn ler-exercicios [] (ler-lista-do-arquivo exercicios-db-path))
(defn salvar-exercicios [exercicios] (salvar-lista-no-arquivo exercicios-db-path exercicios))

(defn adicionar-exercicio
  "Espera um mapa com :nome_pt, :duracao_min, :peso_kg, :kcal_gastas, :data, opcionalmente :met_usado, :nome_en_api"
  [exercicio-map]
  (let [exercicios-atuais (ler-exercicios)
        exercicio-com-id (assoc exercicio-map :id (System/currentTimeMillis))]
    (when (salvar-exercicios (conj exercicios-atuais exercicio-com-id))
      exercicio-com-id))) ; Retorna o exercício salvo com ID

;; --- Função de Resumo ATUALIZADA ---
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
    {:kcal-consumidas kcal-consumidas
     :kcal-gastas kcal-gastas
     :diferenca (- kcal-consumidas kcal-gastas)}))