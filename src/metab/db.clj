(ns metab.db
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]))

(def db-path "data/dados.json")

(defn carregar []
  (if (.exists (io/file db-path))
    (json/parse-string (slurp db-path) true)
    []))

(defn salvar [db]
  (spit db-path (json/generate-string db {:pretty true})))

(defn adicionar-registro [data registro]
  (let [db (carregar)
        novo-registro (assoc registro :data data)
        atualizado (conj db novo-registro)]
    (salvar atualizado)))

(defn calcular-resumo [dias]
  (let [registros (carregar)
        datas (->> registros (map :data) distinct sort (take-last dias))
        filtrados (filter #(some #{(:data %)} datas) registros)
        kcal-consumidas (->> filtrados (filter #(= "alimento" (:tipo %))) (map :kcal) (reduce + 0))
        kcal-gastas    (->> filtrados (filter #(= "exercicio" (:tipo %))) (map :kcal) (reduce + 0))]
    {:kcal-consumidas kcal-consumidas
     :kcal-gastas kcal-gastas
     :diferenca (- kcal-consumidas kcal-gastas)}))