(ns metab.handler
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.util.response :refer [redirect]]
            [clojure.string :as str]
            [metab.view :as view]
            [metab.db :as db]
            [metab.met :as met]
            [metab.food :as food]))

(defroutes app
  (GET "/" [] (view/pagina-principal))

  (POST "/adicionar" {params :params}
    (let [{:strs [data tipo nome quantidade peso]} params]
      (try
        (let [qtd (Float/parseFloat (str/trim quantidade))
              peso (if peso (Float/parseFloat (str/trim peso)) 78)
              kcal (case (str/lower-case tipo)
                     "alimento" (* (/ qtd 100) (food/buscar-kcal nome))
                     "exercicio" (met/calcular-kcal nome qtd peso)
                     0)]
          (db/adicionar-registro data {:tipo tipo :nome nome :quantidade qtd :peso peso :kcal kcal})
          (redirect "/"))
        (catch Exception _
          {:status 400
           :headers {"Content-Type" "text/html"}
           :body "<h2>Erro: Preencha todos os campos corretamente.</h2><a href='/'>Voltar</a>"}))))


  (GET "/resumo/:dias" [dias]
    (view/pagina-resumo (db/calcular-resumo (Integer/parseInt dias))))

  (route/not-found "Página não encontrada"))
