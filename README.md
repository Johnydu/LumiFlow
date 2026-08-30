# LumiFlow

Sistema web para gestão do fluxo de produção de uma vidraçaria. O projeto centraliza os cadastros operacionais, os roteiros de fabricação, as ordens de produção e o controle de estoque de vidro.

> Status: MVP funcional em evolução.

## Funcionalidades atuais

- Dashboard com indicadores de ordens e resumo por setor.
- Cadastro de produtos, setores, etapas, máquinas, operadores e usuários.
- Definição de roteiros de produção por produto.
- Criação, consulta e acompanhamento de ordens de produção por setor.
- Lançamentos de produção e relatório de tempo.
- Controle de chapas de vidro: cadastro, entradas, saídas, perdas, ajustes, saldos e histórico.
- Autenticação de usuários e controle de acesso por perfil.

## Tecnologias

| Camada | Tecnologias |
| --- | --- |
| Back-end | Java 21, Spring Boot 3.5 |
| Web | Spring MVC, Thymeleaf, HTML, CSS e JavaScript |
| Dados | Spring Data JPA, Hibernate e PostgreSQL |
| Migrações | Flyway |
| Segurança | Spring Security e BCrypt |
| Build e testes | Maven e Spring Boot Test |

## Estrutura

```text
src/main/
├── java/br/com/lumiflow/
│   ├── controller/  # Rotas e telas
│   ├── service/     # Regras de negócio
│   ├── repository/  # Persistência
│   ├── entity/      # Modelo de dados
│   ├── dto/         # Dados de entrada e saída
│   ├── mapper/      # Conversão com MapStruct
│   └── security/    # Autenticação e autorização
└── resources/
    ├── db/migration/ # Migrações Flyway
    ├── static/       # CSS, JavaScript e imagens
    └── templates/    # Telas Thymeleaf
```

## Pré-requisitos

- JDK 21 ou superior.
- PostgreSQL.
- Docker (opcional).

O Maven Wrapper já está incluído, portanto não é preciso instalar o Maven globalmente.

## Configuração local

O perfil padrão é `dev` e usa `src/main/resources/application-dev.properties`, arquivo ignorado pelo Git. Para executar localmente, crie esse arquivo com a configuração do seu PostgreSQL. O perfil `prod`, usado no contêiner, lê a conexão por variáveis de ambiente.

| Variável | Descrição | Exemplo seguro |
| --- | --- | --- |
| `PGHOST` | Host do PostgreSQL | `localhost` |
| `PGPORT` | Porta do PostgreSQL | `5432` |
| `PGDATABASE` | Nome do banco | `lumiflow` |
| `PGUSER` | Usuário do banco | `postgres` |
| `PGPASSWORD` | Senha do banco | `defina_localmente` |

Exemplo no PowerShell:

```powershell
$env:PGHOST = "localhost"
$env:PGPORT = "5432"
$env:PGDATABASE = "lumiflow"
$env:PGUSER = "postgres"
$env:PGPASSWORD = "defina_uma_senha_local"
```

Nunca inclua valores reais de `PGPASSWORD`, tokens, chaves privadas ou arquivos `.env` em commits. Em hospedagem, cadastre os valores no gerenciador de variáveis da plataforma.

## Como executar

Após configurar o banco e as variáveis necessárias, execute:

```powershell
.\mvnw.cmd spring-boot:run
```

Acesse `http://localhost:8080/login`.

## Testes

```powershell
.\mvnw.cmd test
```

## Banco de dados

O schema é criado e evoluído pelas migrações em `src/main/resources/db/migration`. Em produção, o Hibernate usa `ddl-auto=validate`: ele valida o modelo sem alterar o banco automaticamente.

## Segurança

- Senhas são armazenadas com hash BCrypt; nunca em texto puro.
- Spring Security exige autenticação para as rotas da aplicação e aplica autorização conforme o perfil.
- Os formulários usam proteção CSRF.
- A sessão de produção usa cookies `HttpOnly`, `Secure` e `SameSite=Strict`.
- O perfil `prod` usa variáveis de ambiente para a conexão de banco; não há credenciais de banco no repositório.

Perfis definidos atualmente: `SUPORTE`, `PCP_SUPERVISOR`, `GESTAO` e `OPERADOR`.

## Docker

```bash
docker build -t lumiflow .
cp .env.example .env
# Edite somente o arquivo .env local, sem versioná-lo.
docker run --rm -p 8080:8080 --env-file .env lumiflow
```

Evite colocar a senha diretamente no histórico do terminal ou em comandos compartilhados. Para ambientes reais, prefira o mecanismo de secrets da plataforma de deploy.

## Licença

Distribuído sob a licença MIT. Consulte [LICENSE](LICENSE).
