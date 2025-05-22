(ns metab.view
  (:require
   [hiccup.page :refer [html5]]
   [hiccup.form :refer [form-to label text-field submit-button drop-down]]))

(defn layout [title & content]
  (html5
   [:head
    [:title title]
    [:meta {:charset "UTF-8"}]
    [:script
     "function togglePesoField() {
        var tipo = document.getElementById('tipo').value;
        var pesoField = document.getElementById('peso-field');
        if (tipo === 'exercicio') {
          pesoField.style.display = 'block';
        } else {
          pesoField.style.display = 'none';
        }
      }"]]
   [:body
    [:h1 title]
    content]))

(defn pagina-principal []
  (layout "Registro Metabólico"
          (form-to [:post "/adicionar"]
                   (label "data" "Data (yyyy-mm-dd):") [:br]
                   (text-field {:name "data"} "") [:br]

                   (label "tipo" "Tipo:") [:br]
                   (drop-down {:id "tipo" :name "tipo" :onchange "togglePesoField()"} ["alimento" "exercicio"]) [:br]

                   (label "nome" "Nome:") [:br]
                   (text-field {:name "nome"} "") [:br]

                   (label "quantidade" "Quantidade (g ou min):") [:br]
                   (text-field {:name "quantidade"} "") [:br]

                   [:div {:id "peso-field" :style "display:none;"}
                    (label "peso" "Peso (kg):") [:br]
                    (text-field {:name "peso"} "") [:br]]

                   (submit-button "Enviar"))))

(defn pagina-resumo [resumo]
  (layout "Resumo"
          [:p (str "Kcal consumidas: " (:kcal-consumidas resumo))]
          [:p (str "Kcal gastas: " (:kcal-gastas resumo))]
          [:p (str "Diferença: " (:diferenca resumo))]))
