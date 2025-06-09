(ns metab.handler
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.util.http-response :as resp]
            [metab.db :as db]
            [metab.food :as food]
            [metab.exercise :as exercise]))

(defn- to-float [x] (try (Float/parseFloat (str x)) (catch Exception _ nil)))
(defn- to-int [x] (try (Integer/parseInt (str x)) (catch Exception _ nil)))

(defn- calcular-kcal-exercicio [met peso-kg duracao-min]
  (float (* 0.0175 met peso-kg duracao-min)))

(defroutes api-routes
  (POST "/usuario" {body :body}
    (let [usuario-data {:nome (:nome body), :peso (to-float (:peso body)),
                        :altura (to-int (:altura body)), :idade (to-int (:idade body)), :sexo (:sexo body)}]
      (resp/ok (db/salvar-usuario usuario-data))))
  (GET "/usuario" [] (resp/ok (db/carregar-usuario)))

  (POST "/alimentos" {body :body}
    (let [nome (:nome body), quantidade (to-float (:quantidade_g body)), data (:data body)]
      (if (or (nil? nome) (nil? quantidade) (<= quantidade 0) (nil? data))
        (resp/bad-request {:erro "Nome, quantidade_g (positiva) e data sao obrigatorios."})
        (let [info (food/obter_info_alimento nome quantidade)]
          (if (:erro info)
            (resp/bad-request info)
            (resp/created "/alimentos" (db/adicionar-alimento (assoc info :data data))))))))

  (POST "/exercicios" {body :body}
    (let [nome (:nome body), duracao (to-float (:duracao_min body)), data (:data body)
          usuario (db/carregar-usuario)]
      (if (or (nil? nome) (nil? duracao) (<= duracao 0) (nil? data))
        (resp/bad-request {:erro "Nome, duracao_min (positiva) e data sao obrigatorios."})
        (if-let [peso (:peso usuario)]
          (let [info-exercicio (exercise/obter-info-exercicio nome)]
            (if (:erro info-exercicio)
              (resp/bad-request info-exercicio)
              (let [met (:met info-exercicio)
                    calorias-gastas (calcular-kcal-exercicio met peso duracao)
                    exercicio-para-db {:nome_pt (:nome_pt info-exercicio)
                                       :duracao_min duracao
                                       :peso_kg peso
                                       :kcal_gastas calorias-gastas
                                       :data data}]
                (resp/created "/exercicios" (db/adicionar-exercicio exercicio-para-db)))))
          (resp/bad-request {:erro "Peso do usuario nao definido. Cadastre o perfil primeiro."})))))

  (GET "/extrato" [inicio fim]
    (let [extrato (db/obter-extrato-por-periodo inicio fim)]
      (if (:erro extrato) (resp/bad-request extrato) (resp/ok extrato))))
  (GET "/saldo" [inicio fim]
    (let [saldo (db/calcular-saldo-por-periodo inicio fim)]
      (if (:erro saldo) (resp/bad-request saldo) (resp/ok saldo))))

  (route/not-found (resp/not-found {:erro "Endpoint nao encontrado."})))