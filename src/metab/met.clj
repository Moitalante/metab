(ns metab.met
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def met-db
  (json/parse-string (slurp (io/resource "public/met.json")) true))


(defn calcular-kcal [nome minutos peso]
  (let [met (get met-db (str/lower-case nome) 8)]
    (* 0.0175 met peso minutos)))
