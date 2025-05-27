(ns metab.db
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]))
  ;; REMOVIDAS importações de java.time

(def db-path "data/dados.json")
(def user-db-path "data/usuario.json")

(.mkdirs (io/file "data"))

;; --- Funções para Registros ---

(defn carregar []
  (let [f (io/file db-path)]
    (if (and (.exists f) (> (.length f) 0))
      (try
        (json/parse-string (slurp f) true)
        (catch Exception e
          (println "ERRO ao ler/parsear dados.json:" (.getMessage e))
          []))
      [])))

(defn salvar [db]
  (spit db-path (json/generate-string db {:pretty true})))

(defn adicionar-registro [data registro]
  (let [db (carregar)
        novo-registro (assoc registro :data data)
        atualizado (conj db novo-registro)]
    (salvar atualizado)))

;; --- Funções para Usuário ---

(defn carregar-usuario []
  (let [f (io/file user-db-path)
        defaults {:nome "" :peso 70.0 :altura 170.0}]
    (if (and (.exists f) (> (.length f) 0))
      (try
        (json/parse-string (slurp f) true)
        (catch Exception e
          (println "ERRO ao ler/parsear usuario.json:" (.getMessage e))
          defaults))
      defaults)))

(defn salvar-usuario [usuario-data]
  (spit user-db-path (json/generate-string usuario-data {:pretty true})))

;; --- Função de Resumo (APENAS A VERSÃO ANTIGA) ---
(defn calcular-resumo [dias]
  (let [registros (carregar)
        ;; Garante que 'dias' seja um número positivo
        num-dias (if (and (number? dias) (pos? dias)) dias 7) ; Padrão 7 se inválido
        datas (->> registros (map :data) distinct sort (take-last num-dias))
        filtrados (filter #(some #{(:data %)} datas) registros)
        kcal-consumidas (->> filtrados (filter #(= "alimento" (:tipo %))) (map :kcal) (reduce + 0.0))
        kcal-gastas     (->> filtrados (filter #(= "exercicio" (:tipo %))) (map :kcal) (reduce + 0.0))]
    {:kcal-consumidas kcal-consumidas
     :kcal-gastas kcal-gastas
     :diferenca (- kcal-consumidas kcal-gastas)}))
