(ns metab.met
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def met-db
  (try
    (json/parse-string (slurp (io/resource "public/met.json")) true)
    (catch Exception e
      (println "ERRO ao carregar/parsear public/met.json:" (.getMessage e))
      {}))) ; Retorna mapa vazio em caso de erro para evitar NullPointerException depois

(defn calcular-kcal [nome minutos peso]
  (let [nome-kw (-> nome str/trim str/lower-case keyword) ; Converte para keyword
        met (get met-db nome-kw 8)] ; Busca usando a keyword
    (if (= met 8) ; Log se estiver usando o MET padrão (ajuda a debugar)
        (println (str "AVISO: MET não encontrado para '" nome "', usando padrão 8.")))
    (* 0.0175 met peso minutos)))