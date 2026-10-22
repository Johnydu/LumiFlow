# LumiFlow

Sistema web para gestão do fluxo de produção de uma vidraçaria. O LumiFlow centraliza o planejamento, a execução e o acompanhamento da produção, além de oferecer controle de estoque de chapas de vidro, rastreabilidade de movimentações e gestão de usuários por perfil.

> **Status:** MVP funcional em evolução.

## Sumário

- [Visão geral](#visão-geral)
- [Funcionalidades](#funcionalidades)
- [Primeiros passos](#primeiros-passos)
- [Arquitetura](#arquitetura)
- [Tecnologias](#tecnologias)
- [Configuração local](#configuração-local)
- [Primeiro acesso](#primeiro-acesso)
- [Testes](#testes)
- [Banco de dados e migrações](#banco-de-dados-e-migrações)
- [Execução com Docker](#execução-com-docker)
- [Deploy no Railway](#deploy-no-railway)
- [Segurança](#segurança)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Contribuição](#contribuição)
- [Solução de problemas](#solução-de-problemas)
- [Licença](#licença)

## Visão geral

O LumiFlow organiza as principais etapas do processo produtivo de uma vidraçaria:

1. Cadastro da estrutura operacional e dos produtos.
2. Definição do roteiro de fabricação de cada produto.
3. Criação e distribuição das ordens de produção.
4. Acompanhamento das ordens por setor.
5. Registro de produção, refugos e retrabalhos.
6. Consulta de indicadores e histórico de lançamentos.
7. Controle do estoque e da movimentação de chapas de vidro.

A interface é renderizada no servidor com Thymeleaf. As regras de negócio e a consistência das operações ficam na camada de serviços.

## Funcionalidades

### Planejamento e produção

- Dashboard com indicadores de produção e resumo por setor.
- Cadastro de produtos, setores, etapas, máquinas e operadores.
- Roteiros de fabricação reutilizáveis (duplicar, vincular produtos). Cada ordem guarda o roteiro com que foi criada; um roteiro já usado em ordens não pode ter a estrutura alterada nem ser excluído (duplique-o para criar uma nova versão).
- Criação, consulta e acompanhamento de ordens de produção.
- Visualização das ordens por setor.
- Liberação e divisão de etapas em variantes.
- Lançamento de produção por ordem e setor.
- Registro de refugos (com variante, quando aplicável) e de retrabalhos.
- Histórico de lançamentos.

> A tela de relatório de tempo ainda é um protótipo visual, sem dados reais.

### Estoque de vidro

- Cadastro de tipos de chapa de vidro, com estoque mínimo.
- Registro de entradas e saídas (consumo).
- Consulta de saldos, com alerta de estoque mínimo.
- Histórico de movimentações com filtros.

### Administração e acesso

- Login personalizado e controle de acesso por perfil, com hierarquia de funções.
- Cadastro e manutenção de usuários (somente `ADMIN`).
- Validação de dados com Bean Validation.
- Mensagens de operação em português do Brasil.

## Primeiros passos

Resumo do caminho mais curto para rodar localmente. Cada etapa é detalhada nas seções seguintes.

1. Instale JDK 21 e PostgreSQL, e crie o banco `lumiflow`.
2. Crie `src/main/resources/application-dev.properties` (modelo em [Configuração local](#configuração-local)).
3. Defina as variáveis `PG*` na sessão do terminal.
4. Execute `.\mvnw.cmd spring-boot:run`.
5. Crie o primeiro usuário `ADMIN` (veja [Primeiro acesso](#primeiro-acesso)).
6. Acesse `http://localhost:8080/login`.

## Arquitetura

```text
Navegador
   |
   v
Controllers (rotas, formulários e respostas)
   |
   v
Services (regras de negócio e transações)
   |
   v
Repositories (acesso a dados)
   |
   v
PostgreSQL
```

- **Controllers:** recebem requisições, validam DTOs e delegam aos services.
- **Services:** concentram regras de negócio, validações de contexto e transações.
- **Repositories:** encapsulam consultas e persistência com Spring Data JPA.
- **Entities:** modelo persistido.
- **DTOs:** dados de entrada e saída de cada operação (records).
- **Mappers:** conversões entre entidades e DTOs (MapStruct).
- **Security:** autenticação, autorização, hierarquia de perfis e sessão.
- **Templates e recursos estáticos:** Thymeleaf, HTML, CSS e JavaScript.

## Tecnologias

| Camada | Tecnologias |
| --- | --- |
| Linguagem | Java 21 |
| Framework | Spring Boot 3.5.16 |
| Web | Spring MVC, Thymeleaf, HTML, CSS e JavaScript |
| Persistência | Spring Data JPA e Hibernate |
| Banco de dados | PostgreSQL 14 ou superior (recomendado) |
| Migrações | Flyway |
| Segurança | Spring Security e BCrypt |
| Mapeamento | MapStruct e Lombok |
| Build | Maven Wrapper |
| Testes | Spring Boot Test e Spring Security Test |
| Empacotamento | Docker e Eclipse Temurin JRE 21 |

## Configuração local

### Pré-requisitos

- JDK 21.
- PostgreSQL 14 ou superior.
- Docker (opcional, apenas para execução em contêiner).

O Maven Wrapper já está no projeto; não é preciso instalar o Maven.

### 1. Criar o banco

```sql
CREATE DATABASE lumiflow;
```

O usuário da aplicação precisa de permissão para conectar e executar as migrações do Flyway.

### 2. Criar o perfil de desenvolvimento

O perfil padrão é `dev` (definido em `application.properties`). Ele lê `src/main/resources/application-dev.properties`, arquivo **ignorado pelo Git**. Crie-o a partir do modelo abaixo (ou copie `application-dev.properties.example`, se existir no repositório):

```properties
spring.datasource.url=jdbc:postgresql://${PGHOST:localhost}:${PGPORT:5432}/${PGDATABASE:lumiflow}
spring.datasource.username=${PGUSER:postgres}
spring.datasource.password=${PGPASSWORD}

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.flyway.enabled=true
spring.thymeleaf.cache=false
```

### 3. Definir as variáveis de ambiente

| Variável | Descrição | Exemplo |
| --- | --- | --- |
| `PGHOST` | Host do PostgreSQL | `localhost` |
| `PGPORT` | Porta do PostgreSQL | `5432` |
| `PGDATABASE` | Nome do banco | `lumiflow` |
| `PGUSER` | Usuário do banco | `postgres` |
| `PGPASSWORD` | Senha do banco | definida apenas localmente |

> **Importante:** o Spring Boot **não lê o arquivo `.env`**. Ele serve apenas para `docker run --env-file` (veja [Execução com Docker](#execução-com-docker)). Para rodar com o Maven, defina as variáveis na sessão do terminal.

PowerShell:

```powershell
$env:PGHOST = "localhost"
$env:PGPORT = "5432"
$env:PGDATABASE = "lumiflow"
$env:PGUSER = "postgres"
$env:PGPASSWORD = "defina_uma_senha_local"
```

Linux/macOS:

```bash
export PGHOST=localhost PGPORT=5432 PGDATABASE=lumiflow PGUSER=postgres
export PGPASSWORD='defina_uma_senha_local'
```

### 4. Executar

```powershell
.\mvnw.cmd spring-boot:run
```

Em Linux/macOS, use `./mvnw spring-boot:run`. A aplicação ficará em `http://localhost:8080/login`.

Para outro perfil: `.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=prod`.

### Segredos

Nunca versione arquivos `.env`, senhas, tokens, chaves privadas, dumps com dados reais ou logs com dados pessoais. Em ambientes compartilhados e de produção, use o gerenciador de variáveis da plataforma.

## Primeiro acesso

As migrações criam apenas os níveis de acesso; **nenhum usuário é criado automaticamente**. Crie o primeiro `ADMIN` diretamente no banco.

**1. Gere um hash BCrypt** da senha inicial (mínimo de 8 caracteres, com maiúscula, minúscula e número). Duas opções:

```bash
# Apache htpasswd
htpasswd -bnBC 10 "" 'SuaSenha123' | tr -d ':\n'

# Python
python -c "import bcrypt; print(bcrypt.hashpw(b'SuaSenha123', bcrypt.gensalt(10)).decode())"
```

**2. Insira o usuário**, substituindo o hash:

```sql
INSERT INTO usuario (nome, login, senha, nivel_acesso_id)
SELECT 'ADMINISTRADOR', 'admin', '<HASH_BCRYPT>', id
FROM nivel_acesso
WHERE descricao = 'ADMIN';
```

**3. Entre com `admin`, altere a senha pela tela de usuários e não reutilize a senha inicial.**

Nunca grave a senha ou o hash real em arquivos versionados. Use uma senha diferente em cada ambiente.

## Testes

```powershell
.\mvnw.cmd test
```

Estado atual da cobertura: existe apenas o teste `contextLoads()`, que sobe o contexto completo da aplicação e, por isso, **exige um PostgreSQL acessível e o perfil configurado**. Testes de regras de negócio e de acesso negado ainda estão pendentes.

Para gerar o pacote sem executar os testes:

```powershell
.\mvnw.cmd clean package -DskipTests
```

## Banco de dados e migrações

As migrações ficam em `src/main/resources/db/migration/` e são executadas pelo Flyway em ordem de versão. As atuais cobrem: tabelas (V1), triggers de auditoria (V2), índices (V3) e o roteiro gravado em cada ordem de produção (V4). Como o Railway já tem o histórico do Flyway aplicado, mudanças estruturais entram sempre como nova migração (V5, V6...), sem editar as existentes.

O Hibernate usa `ddl-auto=validate`: valida o modelo contra o banco, sem alterar o schema. Toda mudança estrutural deve ser feita por uma nova migração.

Regras para novas migrações:

- Não edite migrações já aplicadas em ambientes compartilhados.
- Use o padrão `V<versão>__<descricao>.sql`.
- Avalie índices, chaves estrangeiras e impacto sobre dados existentes.
- Teste em uma cópia do banco e faça backup antes de mudanças destrutivas.

## Execução com Docker

```powershell
docker build -t lumiflow .
Copy-Item .env.example .env
# Edite somente o .env local, sem versioná-lo.
docker run --rm -p 8080:8080 --env-file .env lumiflow
```

O `Dockerfile` tem duas etapas: build com Maven/JDK 21 e execução do JAR em imagem JRE 21 Alpine, com usuário não-root (`appuser`) e porta `8080`. A imagem ativa o perfil `prod`.

Se o PostgreSQL estiver fora do contêiner, não use `localhost` em `PGHOST`: use o endereço da máquina, o nome do serviço na rede Docker ou o host da plataforma.

## Deploy no Railway

O projeto inclui `railway.toml` (build por Dockerfile, uma réplica). Para produção:

- use o perfil `prod` (já ativo na imagem Docker), com verificação de saúde em `/actuator/health`;
- no serviço da aplicação, defina `PG_URL` (URL JDBC, ex.: `jdbc:postgresql://<host>:<porta>/<banco>`), `PG_USERNAME` e `PG_PASSWORD`, referenciando os valores do serviço PostgreSQL do Railway;
- mantenha `spring.jpa.hibernate.ddl-auto=validate` e deixe o Flyway aplicar as migrações;
- garanta HTTPS e monitore logs e falhas de migração;
- mantenha backups do banco.

O limite de falhas de login por IP depende do IP real do cliente. Atrás do proxy do Railway ele vem no `X-Forwarded-For`, e o Tomcat só o aceita de proxies "internos"; o `application-prod.properties` já inclui as faixas privadas e a `100.64.0.0/10` (CGNAT) em `server.tomcat.remoteip.internal-proxies`. Após o deploy, force uma falha de login e confira no log (`Falha de login: ... ip=`) que o IP é o seu e não o do proxy; se for do proxy, ajuste essa propriedade.

O perfil `prod` habilita cache de templates e de recursos estáticos, reduz o nível de log, define cookies de sessão `HttpOnly`, `Secure` e `SameSite=Strict`, e expõe somente o endpoint de saúde do Actuator. O container usa o fuso `America/Sao_Paulo` e dimensiona o heap conforme a memória disponível.

## Segurança

### Controles implementados

- Spring Security protege as rotas; a autorização é feita no backend.
- Senhas armazenadas com BCrypt.
- CSRF habilitado para operações que alteram estado.
- Logout invalida a sessão e remove o cookie `JSESSIONID`.
- Sessões registradas: ao excluir um usuário ou alterar login, perfil, setor ou senha, as sessões ativas dele são encerradas.
- Logs sem quebra de linha injetada por entrada do usuário.
- Cookies de sessão seguros em produção.
- DTOs e Bean Validation nas entradas.
- Contêiner executado sem privilégios de administrador.
- Credenciais de banco fornecidas por variáveis de ambiente.

### Perfis e hierarquia

| Perfil | Escopo geral |
| --- | --- |
| `ADMIN` | Acesso administrativo completo, incluindo usuários |
| `SUPERVISOR` | Supervisão ampla (herda `PCP` e `GESTAO`) |
| `PCP` | Cadastros produtivos, roteiros e criação de ordens |
| `GESTAO` | Liberação e divisão de ordens, visão gerencial |
| `OPERADOR` | Lançamentos no chão de fábrica |

```text
ADMIN > SUPERVISOR
SUPERVISOR > PCP
SUPERVISOR > GESTAO
PCP > OPERADOR
GESTAO > OPERADOR
```

Ocultar botões ou menus na interface não substitui a autorização em controllers e services.

### Diretrizes para alterações

- Valide o vínculo entre ordem, etapa, setor, máquina, operador e usuário autenticado (proteção contra IDOR).
- Não confie em IDs enviados pelo navegador sem confirmar pertencimento e permissão.
- Não receba entidades JPA diretamente em operações de escrita.
- Não use `innerHTML` com dados persistidos ou informados pelo usuário.
- Envie o token CSRF em chamadas AJAX que alteram dados.
- Revise as permissões sempre que criar uma rota de escrita.

### Reporte de vulnerabilidades

Não abra issues públicas para falhas de segurança. Reporte de forma privada ao mantenedor (por exemplo, pelo recurso *Security Advisories* do GitHub).

## Estrutura do projeto

```text
.
├── src/main/java/br/com/lumiflow/
│   ├── config/          # Configurações e chaves de mensagens
│   ├── controller/      # Controllers MVC e endpoints
│   ├── dto/             # Objetos de entrada e saída (records)
│   ├── entity/          # Entidades JPA e enums
│   ├── exception/       # Exceções de negócio e handler global
│   ├── mapper/          # Conversões entre entidades e DTOs
│   ├── repository/      # Repositórios Spring Data
│   ├── security/        # Autenticação e autorização
│   ├── service/         # Regras de negócio
│   └── validation/      # Validadores de login e senha
├── src/main/resources/
│   ├── db/migration/    # Migrações Flyway
│   ├── static/          # CSS, JavaScript e imagens
│   ├── templates/       # Templates Thymeleaf
│   ├── application*.properties
│   └── messages.properties
├── src/test/            # Testes automatizados
├── Dockerfile
├── railway.toml
├── .env.example         # Modelo de variáveis (uso com Docker)
├── mvnw / mvnw.cmd
└── pom.xml
```

## Contribuição

Fluxo de branches: `main` (produção), `develop` (homologação) e `feature/*` (novas funcionalidades). Toda alteração deve passar por Pull Request antes de chegar à `main`.

Antes de abrir o PR:

1. execute os testes e a compilação completa;
2. verifique manualmente o fluxo alterado com um usuário de menor privilégio;
3. confira o diff para garantir que nenhum segredo foi incluído;
4. crie uma migração Flyway para qualquer mudança de schema;
5. mantenha controllers finos, com a regra de negócio nos services.

## Solução de problemas

**Erro de conexão com o PostgreSQL:** confirme que o serviço está ativo, que host, porta, banco, usuário e senha estão corretos e que as variáveis foram definidas **na mesma sessão** em que o Maven é executado (o `.env` não é lido pelo Spring).

**Falha de migração Flyway:** leia a mensagem no log, consulte `flyway_schema_history` e não edite migrações já aplicadas; corrija com uma nova migração.

**Não consigo entrar:** nenhum usuário é criado automaticamente. Siga [Primeiro acesso](#primeiro-acesso).

**Acesso negado (403):** confirme o perfil do usuário e a regra correspondente em `SecurityConfig` e nas anotações `@PreAuthorize` dos controllers.

**Página sem estilos ou scripts:** verifique o perfil ativo e os caminhos `/css/**`, `/js/**` e `/images/**`; em produção, verifique também o cache do navegador e do proxy.

## Licença

Distribuído sob a licença MIT. Consulte o arquivo [LICENSE](LICENSE).
