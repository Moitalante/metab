(ns metab.food
  (:require [clj-http.client :as client]
            [cheshire.core :as json]))

(def app-id "1bcc5746")
(def api-key "40f29da9257857eaccb43a4dd60fd84d")

(defn buscar-kcal [nome]
  (let [url "https://trackapi.nutritionix.com/v2/natural/nutrients"
        headers {"x-app-id" app-id
                 "x-app-key" api-key
                 "Content-Type" "application/json"}
        body (json/generate-string {:query nome})
        resp (client/post url {:headers headers :body body :as :json})]
    (get-in resp [:body :foods 0 :nf_calories] 0.0)))
