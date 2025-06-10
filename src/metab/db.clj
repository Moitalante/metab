(ns metab.db
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [java-time.api :as jt]))

(def db-dir-path "data")
(def alimentos-db-path (str db-dir-path "/alimentos.json"))
(def exercicios-db-path (str db-dir-path "/exercicios.json"))
(def usuario-db-path (str db-dir-path "/usuario.json"))

;; Funções Auxiliares de Arquivo
(defn- garantir-diretorio-db! []
  (let [db-dir (io/file db-dir-path)]
    (when-not (.exists db-dir) (.mkdirs db-dir))))

(defn- ler-do-arquivo [path-arquivo default-value]
  (garantir-diretorio-db!)
  (let [f (io/file path-arquivo)]
    (if (and (.exists f) (> (.length f) 0))
      (try
        (json/parse-string (slurp f) true)
        (catch Exception e
          (log/error e "ERRO ao ler/parsear" path-arquivo)
          default-value))
      default-value)))

(defn- salvar-no-arquivo [path-arquivo dados]
  (garantir-diretorio-db!)
  (try
    (spit path-arquivo (json/generate-string dados {:pretty true}))
    true
    (catch Exception e
      (log/error e "AVISO: Falha ao salvar arquivo" path-arquivo)
      false)))

;;Atoms para Estado em Memória
(defonce alimentos-atom (atom (ler-do-arquivo alimentos-db-path [])))
(defonce exercicios-atom (atom (ler-do-arquivo exercicios-db-path [])))
(defonce usuario-atom (atom (ler-do-arquivo usuario-db-path {:nome nil, :peso nil, :altura nil, :idade nil, :sexo nil})))

;;Persistência
(defn salvar-alimentos! [] (salvar-no-arquivo alimentos-db-path @alimentos-atom))
(defn salvar-exercicios! [] (salvar-no-arquivo exercicios-db-path @exercicios-atom))
(defn salvar-usuario! [] (salvar-no-arquivo usuario-db-path @usuario-atom))

;;Usuário
(defn salvar-usuario [usuario-data]
  (reset! usuario-atom usuario-data)
  (salvar-usuario!)
  @usuario-atom)

(defn carregar-usuario [] @usuario-atom)

;;Alimentos
(defn adicionar-alimento [alimento-map]
  (let [alimento-com-id (assoc alimento-map :id (System/currentTimeMillis))]
    (swap! alimentos-atom conj alimento-com-id)
    (salvar-alimentos!)
    alimento-com-id))

;;Exercícios
(defn adicionar-exercicio [exercicio-map]
  (let [exercicio-com-id (assoc exercicio-map :id (System/currentTimeMillis))]
    (swap! exercicios-atom conj exercicio-com-id)
    (salvar-exercicios!)
    exercicio-com-id))

(defn- parse-date [date-str] (try (jt/local-date "yyyy-MM-dd" date-str) (catch Exception _ nil)))

(defn obter-extrato-por-periodo [data-inicio-str data-fim-str]
  (log/info "Buscando extrato entre" data-inicio-str "e" data-fim-str)
  (if-let [inicio (parse-date data-inicio-str)]
    (if-let [fim (parse-date data-fim-str)]
      (if (jt/after? inicio fim)
        {:erro "A data de inicio nao pode ser posterior a data de fim."}
        (let [filtra-intervalo (fn [reg]
                                 (when-let [data-reg (parse-date (:data reg))]
                                   (not (or (jt/before? data-reg inicio) (jt/after? data-reg fim)))))
              alimentos-filtrados (filter filtra-intervalo @alimentos-atom)
              exercicios-filtrados (filter filtra-intervalo @exercicios-atom)

              map-alimento (fn [a] (assoc a :tipo "alimento"))
              map-exercicio (fn [e] (assoc e :tipo "exercicio"))]
          {:transacoes (sort-by :data (concat (map map-alimento alimentos-filtrados)
                                              (map map-exercicio exercicios-filtrados)))}))
      {:erro "Formato da data de fim invalido. Use yyyy-MM-dd."})
    {:erro "Formato da data de inicio invalido. Use yyyy-MM-dd."}))

(defn calcular-saldo-por-periodo [data-inicio-str data-fim-str]
  (let [extrato (obter-extrato-por-periodo data-inicio-str data-fim-str)]
    (if (:erro extrato)
      extrato
      (let [transacoes (:transacoes extrato)
            kcal-consumidas (->> transacoes (filter #(= "alimento" (:tipo %))) (map :kcal_consumidas) (reduce + 0.0))
            kcal-gastas     (->> transacoes (filter #(= "exercicio" (:tipo %))) (map :kcal_gastas) (reduce + 0.0))]
        {:periodo {:inicio data-inicio-str, :fim data-fim-str}
         :kcal_consumidas (float kcal-consumidas)
         :kcal_gastas (float kcal-gastas)
         :saldo_calorico (float (- kcal-consumidas kcal-gastas))}))))