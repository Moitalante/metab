# Calculadora Metabólica - Projeto de Programação Funcional

## Sobre o Projeto

Este é o meu projeto para a disciplina de Programação Funcional (T300 - 2025.1). A proposta foi criar uma **Calculadora Metabólica**, uma aplicação web completa desenvolvida em Clojure para permitir o registro e acompanhamento do consumo de alimentos e do gasto de calorias com exercícios físicos.

A aplicação foi construída com um backend em Clojure que lida com toda a lógica de negócio e um frontend simples, também em Clojure com Hiccup, para a interação com o usuário.

## Principais Funcionalidades

O sistema permite que o usuário realize as seguintes ações:

- **Gerenciar Perfil:** Cadastrar e consultar dados pessoais como nome, peso, altura, idade e sexo.
- **Registrar Alimentos:** Inserir um alimento consumido e a quantidade em gramas. O sistema se conecta à API da Nutritionix para buscar as calorias e salva o registro.
- **Registrar Exercícios:** Inserir um exercício realizado e a duração em minutos. A aplicação busca o valor MET (equivalente metabólico) na API de Exercícios da Nutritionix, calcula o gasto calórico com base no peso do usuário e salva o registro.
- **Tradução Dinâmica:** Para melhorar a experiência, o sistema utiliza a API MyMemory para traduzir os nomes dos alimentos e exercícios (que são buscados em inglês) para o português.
- **Visualizar Resumo:** Apresenta um resumo simples do saldo calórico dos últimos 7 dias.

## Tecnologias

Para construir este projeto, utilizei o seguinte ecossistema:

- **Linguagem:** Clojure
- **Servidor Web:** Ring com o adaptador Jetty
- **Roteamento:** Compojure
- **Frontend (Views):** Hiccup
- **Cliente HTTP:** clj-http
- **Manipulação de JSON:** Cheshire
- **Gerenciamento do Projeto:** Leiningen
- **APIs Externas:** Nutritionix (Alimentos e Exercícios) e MyMemory (Tradução)

## Como Rodar o Projeto

Para rodar este projeto na sua máquina, siga os passos abaixo.

**Pré-requisitos:**
* Ter o [Leiningen](https://leiningen.org/) instalado.
* Ter uma JDK (Java Development Kit) instalada.

**Configuração:**

1.  **Clonar o Repositório**
    Primeiro, clone o projeto para a sua máquina e entre na pasta criada:
    ```bash
    git clone [https://github.com/Moitalante/metab.git](https://github.com/Moitalante/metab.git)
    cd metab
    ```

2.  **Configurar Chaves de API**
    O sistema usa a API da Nutritionix, então você vai precisar das suas próprias chaves.
    
    Abra os arquivos `src/metab/food.clj` e `src/metab/exercise.clj` e coloque suas chaves nas seguintes linhas, substituindo os placeholders:
    ```clojure
    (def nutritionix-app-id "SUA_CHAVE_NUTRITIONIX_APP_ID_AQUI")
    (def nutritionix-api-key "SUA_CHAVE_NUTRITIONIX_API_KEY_AQUI")
    ```

3.  **Instalar as Dependências**
    Com o Leiningen, este passo é simples. No terminal, dentro da pasta do projeto, rode:
    ```bash
    lein deps
    ```

4.  **Iniciar o Servidor**
    Finalmente, para iniciar a aplicação:
    ```bash
    lein run
    ```
    Pronto! O servidor estará rodando em `http://localhost:3000`.

## Estrutura do Código

Para quem for explorar o código, o projeto está organizado da seguinte forma:

- **`project.clj`**: Arquivo de configuração do Leiningen, com todas as dependências do projeto.
- **`src/metab/core.clj`**: Ponto de entrada da aplicação. Ele configura os middlewares do Ring e inicia o servidor Jetty.
- **`src/metab/db.clj`**: Lida com o estado da aplicação. Nesta versão, ele gerencia um `atom` em memória que funciona como nosso banco de dados.
- **`src/metab/view.clj`**: Responsável por todo o HTML. Gera as páginas usando Hiccup.
- **`src/metab/handler.clj`**: O "coração" da aplicação web. Define as rotas (`/`, `/usuario`, etc.) e orquestra as chamadas para os outros módulos.
- **`src/metab/translations.clj`**: Contém a lógica para chamar a API MyMemory e fazer as traduções.
- **`src/metab/food.clj`**: Contém a lógica para buscar os dados nutricionais de alimentos na API da Nutritionix.
- **`src/metab/exercise.clj`**: Contém a lógica para buscar os METs de exercícios na API da Nutritionix e calcular as calorias gastas.