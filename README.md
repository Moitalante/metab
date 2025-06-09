# Calculadora Metabólica

## 1. Descrição do Projeto

O projeto **Calculadora de Calorias** é uma aplicação desenvolvida na linguagem Clojure que permite o monitoramento de consumo e gasto calórico. O objetivo é fornecer uma ferramenta para que usuários possam registrar seus hábitos alimentares e rotinas de exercício, visando um estilo de vida mais saudável.

A aplicação foi desenvolvida para cumprir os requisitos da disciplina de Programação Funcional, incluindo o uso de um banco de dados em memória (`atom`), comunicação com APIs externas e uma arquitetura que separa o back-end da interface com o usuário.

## 2. Arquitetura

O sistema segue um modelo cliente-servidor, composto por duas partes independentes que se comunicam via HTTP/JSON:

* **Back-end (API):** Um servidor de API desenvolvido com Ring e Compojure. Ele é responsável por toda a lógica de negócio, processamento de dados e gerenciamento do estado da aplicação. Ele não possui interface visual própria.

* **Front-end (Cliente de Terminal):** Uma aplicação de linha de comando que funciona como o cliente da API. É responsável por toda a interação com o usuário, coletando dados e exibindo os resultados obtidos através de requisições ao back-end.

## 3. Funcionalidades Principais

* **Gerenciamento de Perfil:** Permite cadastrar e consultar os dados pessoais do usuário (altura, peso, idade e sexo).
* **Registro de Alimentos:** Registra o consumo de um alimento, com data e quantidade em gramas. As calorias são obtidas através de uma consulta à API externa da Nutritionix.
* **Registro de Exercícios:** Registra a realização de uma atividade física, com data e duração. O gasto calórico é calculado com base no valor MET, também obtido via API externa (Nutritionix Exercise).
* **Tradução Dinâmica:** Utiliza um dicionário interno de termos comuns e uma API externa (MyMemory) como fallback para traduzir termos entre português e inglês, facilitando a comunicação com as APIs externas.
* **Consulta de Dados:** Permite ao usuário consultar o extrato de transações e o saldo final de calorias para um determinado período de datas.

## 4. Estrutura do Projeto e Explicação dos Arquivos

O código do projeto está organizado da seguinte forma:

### Back-end

* **`project.clj`**: Define o projeto, suas dependências (bibliotecas externas) e configurações para o Leiningen.
* **`src/metab/server.clj`**: Ponto de entrada do servidor. É responsável por iniciar o servidor web (Jetty) e aplicar os middlewares essenciais ao handler da API.
* **`src/metab/handler.clj`**: O roteador da API. Define todos os endpoints HTTP (ex: `POST /usuario`, `GET /resumo`) e orquestra as chamadas para a lógica de negócio.
* **`src/metab/db.clj`**: Gerencia o estado da aplicação. Utiliza um `atom` para manter os dados em memória e também salva/carrega o estado de arquivos JSON para persistência.
* **`src/metab/translations.clj`**: Módulo de serviço que implementa uma tradução híbrida: primeiro consulta um mapa interno de termos confiáveis e, se necessário, recorre à API externa (MyMemory).
* **`src/metab/food.clj`**: Módulo de serviço para se comunicar com a API da Nutritionix para buscar informações de alimentos.
* **`src/metab/exercise.clj`**: Módulo de serviço para interagir com a API de Exercícios da Nutritionix para buscar valores MET e calcular o gasto calórico.

### Front-end

* **`src/metab/client.clj`**: Este é o front-end completo da aplicação. É um cliente de linha de comando que exibe um menu para o usuário, coleta dados e faz chamadas HTTP para a API do back-end.

## 5. Fluxo de Uso do Cliente

Ao executar o cliente, um menu com as seguintes opções é exibido:

1.  **Registrar Alimento:** Solicita o nome, a quantidade (g) e a data do alimento. O sistema chama o back-end, que traduz o termo para inglês, busca o melhor resultado na API da Nutritionix e salva o registro com as calorias calculadas.

2.  **Registrar Exercicio:** O usuário informa o nome do exercício, a duração em minutos e a data. O sistema utiliza o peso salvo no perfil, chama o back-end que, por sua vez, usa a API da Nutritionix para obter o valor MET e calcula as calorias gastas antes de salvar.

3.  **Ver Saldo por Periodo:** Solicita uma data de início e uma de fim e exibe um resumo do balanço calórico (consumidas - gastas) para o período especificado.

4.  **Ver Extrato por Periodo:** Solicita um período de datas e retorna uma lista detalhada de todas as transações (alimentos e exercícios) registradas naquelas datas.

5.  **Cadastrar/Atualizar Perfil:** Abre o formulário para o usuário inserir ou atualizar seus dados pessoais.

6.  **Ver Perfil Atual:** Exibe os dados do perfil do usuário que estão salvos no momento.

7.  **Sair:** Encerra a aplicação cliente.

## 6. Tecnologias Utilizadas

* Linguagem Principal: Clojure
* Servidor Web: Ring & Jetty
* Roteamento: Compojure
* Gerenciamento de Estado: Atom
* Cliente HTTP: clj-http
* Manipulação de JSON: Cheshire
* Gerenciamento do Projeto: Leiningen
* APIs Externas: Nutritionix, MyMemory

## 7. Como Executar o Projeto

A aplicação requer que o servidor e o cliente sejam executados em dois terminais separados.

**Pré-requisitos:**
* [Leiningen](https://leiningen.org/) instalado.
* Java JDK.

**Configuração:**

1.  **Clonar o Repositório:**
    ----terminal
    git clone [https://github.com/Moitalante/metab.git](https://github.com/Moitalante/metab.git)
    cd metab
    code .
    ----

2.  **Chaves de API:**
    As chaves da API Nutritionix já estão inseridas no código. Elas estão nos arquivos `src/metab/food.clj` e `src/metab/exercise.clj` com os seguintes valores:

    ----clojure
    (def ^:private nutritionix-app-id "1bcc5746")
    (def ^:private nutritionix-api-key "40f29da9257857eaccb43a4dd60fd84d")
    ----

3.  **Instalar as Dependências:**
    ----terminal
    lein deps
    ----

4.  **Executar a Aplicação:**

    * **Terminal 1 (Iniciar o Servidor Back-end):**
        ----
        lein run -m metab.server
        ----
        O servidor será iniciado na porta 3000. Deixe este terminal rodando.

    * **Terminal 2 (Iniciar o Cliente Front-end):**
        Abra um novo terminal, navegue até a mesma pasta do projeto e execute:
        ----
        lein cliente
        ----
        O menu interativo da aplicação aparecerá neste terminal para você usar.