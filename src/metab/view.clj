(ns metab.view
  (:require [hiccup.page :refer [html5]]))

(defn layout [title & content]
  (html5
   [:head
    [:title title]
    [:meta {:charset "UTF-8"}]
    [:script
     "function togglePesoField() {
         var tipoSelect = document.getElementById('tipo');
         if (!tipoSelect) return; // Evita erro em páginas sem o seletor
         var tipo = tipoSelect.value;
         var pesoField = document.getElementById('peso-field');
         if (!pesoField) return;
         if (tipo === 'exercicio') {
           pesoField.style.display = 'block';
         } else {
           pesoField.style.display = 'none';
         }
       }
       window.onload = function() {
           // Verifica se a função existe antes de chamar
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
     [:a {:href "/resumo/7"} "Resumo (7 dias)"]]
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
            [:label {:for "peso"} "Peso (kg) (opcional se ja cadastrado):"] [:br]
            [:input {:type "number" :id "peso" :name "peso" :step "any" :placeholder (:peso usuario "Nao definido")}] [:br]]

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

           [:label {:for "idade"} "Idade:"] [:br]
           [:input {:type "number" :id "idade" :name "idade" :value (:idade usuario)}] [:br]

           [:label {:for "sexo"} "Sexo Biologico:"] [:br]
           [:select {:id "sexo" :name "sexo"}
            [:option {:value "Feminino" :selected (= "Feminino" (:sexo usuario))} "Feminino"]
            [:option {:value "Masculino" :selected (= "Masculino" (:sexo usuario))} "Masculino"]
            [:option {:value "Nao informado" :selected (not (#{"Feminino" "Masculino"} (:sexo usuario)))} "Nao informado"]]
           [:br]

           [:button {:type "submit"} "Salvar"]]))

(defn pagina-resumo [resumo]
  (layout "Resumo (Ultimos dias)"
          [:div.summary
           [:p (str "Kcal consumidas: " (format "%.1f" (:kcal-consumidas resumo)))]
           [:p (str "Kcal gastas: " (format "%.1f" (:kcal-gastas resumo)))]
           [:p (str "Diferenca: " (format "%.1f" (:diferenca resumo)))]]))

(defn pagina-de-erro [mensagem]
  (layout "Erro"
          [:h2.error "Ocorreu um Erro"]
          [:p.error mensagem]
          [:a {:href "/"} "Voltar para a pagina principal"]))