(ns metab.view
  (:require [hiccup.page :refer [html5]]))

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
       }
       window.onload = function() {
           if (typeof togglePesoField === 'function') {
               togglePesoField();
           }
       };"]
    [:style "body { font-family: sans-serif; }
              label { display: inline-block; width: 180px; margin-bottom: 5px; }
              input, select { padding: 5px; margin-bottom: 10px; width: 200px; }
              button { padding: 10px 15px; cursor: pointer; }
              nav a { margin-right: 15px; }
              .error { color: red; }
              .summary p { font-size: 1.1em; }"]]
   [:body
    [:nav
     [:a {:href "/"} "Registrar"]
     [:a {:href "/usuario"} "Usuario"]
     [:a {:href "/resumo/7"} "Resumo (7 dias)"]] ; <-- Link de exemplo do intervalo removido
    [:hr]
    [:h1 title]
    content]))

(defn pagina-principal [usuario]
  (layout "Registro Metabolico"
          [:h2 "Adicionar Registro"]
          [:form {:method "post" :action "/adicionar"}
           [:label {:for "data"} "Data (yyyy-mm-dd):"] [:br]
           [:input {:type "date" :id "data" :name "data" :required true}] [:br]

           [:label {:for "tipo"} "Tipo:"] [:br]
           [:select {:id "tipo" :name "tipo" :onchange "togglePesoField()"}
            [:option {:value "alimento"} "Alimento"]
            [:option {:value "exercicio"} "Exercicio"]] [:br]

           [:label {:for "nome"} "Nome:"] [:br]
           [:input {:type "text" :id "nome" :name "nome" :required true}] [:br]

           [:label {:for "quantidade"} "Quantidade (g ou min):"] [:br]
           [:input {:type "number" :id "quantidade" :name "quantidade" :step "any" :required true}] [:br]

           [:div {:id "peso-field" :style "display:none;"}
            [:label {:for "peso"} "Peso (kg):"] [:br]
            [:input {:type "number" :id "peso" :name "peso" :step "any" :value (:peso usuario 70.0)}] [:br]]

           [:button {:type "submit"} "Enviar"]]))

(defn pagina-usuario [usuario]
  (layout "Perfil do Usuario"
          [:h2 "Seus Dados"]
          [:form {:method "post" :action "/usuario"}
           [:label {:for "nome"} "Nome:"] [:br]
           [:input {:type "text" :id "nome" :name "nome" :value (:nome usuario)}] [:br]

           [:label {:for "peso"} "Peso (kg):"] [:br]
           [:input {:type "number" :id "peso" :name "peso" :step "any" :value (:peso usuario)}] [:br]

           [:label {:for "altura"} "Altura (cm):"] [:br]
           [:input {:type "number" :id "altura" :name "altura" :step "any" :value (:altura usuario)}] [:br]

           [:button {:type "submit"} "Salvar"]]))

;; --- PÁGINA DE RESUMO REVERTIDA ---
(defn pagina-resumo [resumo]
  (layout "Resumo (Ultimos dias)" ; Título mais genérico
          [:div.summary
           [:p (str "Kcal consumidas: " (format "%.1f" (:kcal-consumidas resumo)))]
           [:p (str "Kcal gastas: " (format "%.1f" (:kcal-gastas resumo)))]
           [:p (str "Diferenca: " (format "%.1f" (:diferenca resumo)))]]))

(defn pagina-de-erro [mensagem]
  (layout "Erro"
          [:h2.error "Ocorreu um Erro"]
          [:p.error mensagem]
          [:a {:href "/"} "Voltar para a pagina principal"]))