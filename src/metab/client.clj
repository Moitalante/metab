;; src/metab/client.clj
(ns metab.client
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]) 
  (:gen-class))

(def api-base-url "http://localhost:3000")


(defn- post-data [endpoint data]
  (try (client/post (str api-base-url endpoint) {:content-type :json, :body (json/generate-string data), :as :json, :throw-exceptions false})
       (catch Exception e {:status 500 :body {:erro (.getMessage e)}})))

(defn- get-data [endpoint params]
  (try (client/get (str api-base-url endpoint) {:query-params params, :as :json, :throw-exceptions false})
       (catch Exception e {:status 500 :body {:erro (.getMessage e)}})))

(defn- tratar-resposta [response]
  (println "\n--- Resposta do Servidor ---")
  (if (>= (:status response) 400) (println "ERRO:" (:status response)) (println "SUCESSO:" (:status response)))
  (pprint (:body response))
  (println "--------------------------"))

(defn- prompt [mensagem] (print (str mensagem " ")) (flush) (read-line))

(defn registrar-usuario []
  (println "\n Cadastro de Perfil")
  (let [nome (prompt "Nome:"), peso (prompt "Peso (kg):"), altura (prompt "Altura (cm):")
        idade (prompt "Idade:"), sexo (prompt "Sexo (Masculino/Feminino):")
        data {:nome nome, :peso peso, :altura altura, :idade idade, :sexo sexo}]
    (tratar-resposta (post-data "/usuario" data))))

(defn registrar-alimento []
  (println "\n-- Registrar Alimento --")
  (let [nome (prompt "Nome do alimento:"), quantidade (prompt "Quantidade (g):")
        data (prompt "Data (AAAA-MM-DD):"), data-map {:nome nome, :quantidade_g quantidade, :data data}]
    (tratar-resposta (post-data "/alimentos" data-map))))

(defn registrar-exercicio []
  (println "\n-- Registrar Exercicio --")
  (println "(O seu peso sera pego do seu perfil cadastrado)")
  (let [nome (prompt "Nome do exercicio:"), duracao (prompt "Duracao (minutos):")
        data (prompt "Data (AAAA-MM-DD):"), data-map {:nome nome, :duracao_min duracao, :data data}]
    (tratar-resposta (post-data "/exercicios" data-map))))

(defn ver-resumo []
  (println "\n-- Ver Saldo por Periodo --")
  (let [inicio (prompt "Data de Inicio (AAAA-MM-DD):"), fim (prompt "Data de Fim (AAAA-MM-DD):")]
    (tratar-resposta (get-data "/saldo" {:inicio inicio, :fim fim}))))

(defn ver-extrato []
  (println "\n-- Ver Extrato por Periodo --")
  (let [inicio (prompt "Data de Inicio (AAAA-MM-DD):"), fim (prompt "Data de Fim (AAAA-MM-DD):")]
    (tratar-resposta (get-data "/extrato" {:inicio inicio, :fim fim}))))

(defn exibir-menu []
  (println "\n -> Calculadora Metabolica <-")
  (println "1. Registrar Alimento")
  (println "2. Registrar Exercicio")
  (println "3. Ver Saldo por Periodo")
  (println "4. Ver Extrato por Periodo")
  (println "5. Cadastrar/Atualizar Perfil")
  (println "6. Ver Perfil Atual")
  (println "7. Sair")
  (prompt "Escolha uma opcao:"))



(defn- perfil-valido? [usuario]
  (and (not (str/blank? (:nome usuario)))
       (some? (:peso usuario))))

(declare ciclo-principal)

(defn verificar-e-configurar-perfil []
  (println "Verificando perfil de usuario no servidor...")
  (let [usuario-atual (:body (get-data "/usuario" {}))]
    (if (perfil-valido? usuario-atual)
      (println "Bem-vindo(a) de volta," (or (:nome usuario-atual) "Usuario") "!")
      (do
        (println "\n*** ATENCAO: Perfil incompleto. Por favor, cadastre seus dados.")
        (registrar-usuario)
        (verificar-e-configurar-perfil))))) 

(defn ciclo-principal []
  (let [opcao (exibir-menu)]
    (condp = opcao
      "1" (registrar-alimento)
      "2" (registrar-exercicio)
      "3" (ver-resumo)
      "4" (ver-extrato)
      "5" (registrar-usuario)
      "6" (tratar-resposta (get-data "/usuario" {}))
      "7" (println "Saindo...")
      (println "Opcao invalida, tente novamente."))
    (when (not= opcao "7")
      (ciclo-principal))))


(defn -main [& _]
  (verificar-e-configurar-perfil) 
  (ciclo-principal))             