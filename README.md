# LumiFlow

Sistema web para gestão e acompanhamento do fluxo de produção de uma vidraçaria. Desenvolvido como Projeto Integrador Extensionista, o LumiFlow centraliza informações operacionais e apoia o controle das etapas produtivas.

> Status: MVP em desenvolvimento.

---

## Sumário

- [Visão geral](#visão-geral)
- [Funcionalidades](#funcionalidades)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Pré-requisitos](#pré-requisitos)
- [Configuração](#configuração)
- [Como executar](#como-executar)
- [Testes](#testes)
- [Segurança e perfis de acesso](#segurança-e-perfis-de-acesso)
- [Banco de dados](#banco-de-dados)
- [Implantação com Docker](#implantação-com-docker)
- [Licença](#licença)

---

## Visão geral

O LumiFlow reúne em um único ambiente o cadastro de recursos produtivos, a definição de roteiros, o acompanhamento de ordens e o controle de estoque de vidro. A interface é renderizada no servidor com Thymeleaf e possui autenticação e autorização baseadas em perfis de acesso.

O objetivo é oferecer uma operação mais organizada para administradores, supervisores e operadores, com rastreabilidade dos principais dados do processo produtivo.

---

## Funcionalidades

### Gestão operacional

- Cadastro e manutenção de produtos, setores, máquinas e operadores.
- Configuração de etapas por setor.
- Definição de roteiros de produção por produto.
- Consulta de ordens de produção por setor.
- Lançamento e acompanhamento da produção.

### Controle de vidro

- Cadastro de tipos de chapa de vidro.
- Registro de entradas, saídas, perdas e ajustes.
- Consulta de saldo atual, histórico de movimentações e alertas de estoque mínimo.

### Administração e segurança

- Cadastro, edição e exclusão de usuários.
- Autenticação por login e senha, com hash BCrypt.
- Controle de sessão, proteção CSRF e navegação protegida.
- Permissões por perfil de acesso.
- Migrações de banco de dados gerenciadas pelo Flyway.

---

## Tecnologias

| Camada | Tecnologias |
| --- | --- |
| Back-end | Java 21, Spring Boot 3.5 |
| Web | Spring MVC, Thymeleaf |
| Persistência | Spring Data JPA, Hibernate |
| Segurança | Spring Security, BCrypt |
| Banco de dados | PostgreSQL |
| Migrações | Flyway |
| Mapeamento | MapStruct |
| Produtividade | Lombok |
| Front-end | HTML5, CSS3, JavaScript |
| Build e testes | Maven, Spring Boot Test |

---

## Arquitetura

O projeto adota uma arquitetura em camadas para separar responsabilidades e facilitar a manutenção:

```text
Controller → Service → Repository → Banco de dados
                 ↕
             DTO / Mapper / Entity
```

| Camada | Responsabilidade |
| --- | --- |
| Controller | Recebe requisições HTTP, valida entradas e prepara as respostas ou telas. |
| Service | Concentra regras de negócio, validações e orquestra os fluxos da aplicação. |
| Repository | Realiza consultas e operações de persistência com JPA. |
| DTO e Mapper | Isolam a camada web e fazem a conversão entre dados de entrada/saída e entidades. |
| Security | Define autenticação, autorização e regras de acesso às rotas. |

---

## Estrutura do projeto

```text
LumiFlow/
├── .mvn/                       # Maven Wrapper
├── src/
│   ├── main/
│   │   ├── java/br/com/lumiflow/
│   │   │   ├── config/          # Configurações da aplicação
│   │   │   ├── controller/      # Controladores web
│   │   │   ├── dto/             # Objetos de transferência de dados
│   │   │   ├── exception/       # Exceções e tratamento global
│   │   │   ├── mapper/          # Conversores MapStruct
│   │   │   ├── model/           # Entidades e enums
│   │   │   ├── repository/      # Acesso a dados
│   │   │   ├── security/        # Configuração de segurança
│   │   │   └── service/         # Regras de negócio
│   │   └── resources/
│   │       ├── db/migration/    # Scripts do Flyway
│   │       ├── static/          # CSS, JavaScript e imagens
│   │       ├── templates/       # Telas Thymeleaf
│   │       └── application*.properties
│   └── test/                    # Testes automatizados
├── Dockerfile
├── pom.xml
├── mvnw
└── mvnw.cmd
```

---

## Pré-requisitos

- JDK 21 ou superior.
- PostgreSQL.
- Git (opcional).
- Docker (opcional, para implantação em contêiner).

O Maven Wrapper já está incluído; não é necessário instalar o Maven globalmente.

---

## Configuração

A aplicação obtém as credenciais de banco de dados por variáveis de ambiente:

| Variável | Descrição | Exemplo |
| --- | --- | --- |
| `PGHOST` | Host do PostgreSQL | `localhost` |
| `PGPORT` | Porta do PostgreSQL | `5432` |
| `PGDATABASE` | Nome do banco | `lumiflow` |
| `PGUSER` | Usuário do banco | `postgres` |
| `PGPASSWORD` | Senha do banco | `sua_senha` |

Exemplo no PowerShell:

```powershell
$env:PGHOST = "localhost"
$env:PGPORT = "5432"
$env:PGDATABASE = "lumiflow"
$env:PGUSER = "postgres"
$env:PGPASSWORD = "sua_senha"
```

> Não versione credenciais reais. Prefira configurar as variáveis no ambiente de execução ou no provedor de hospedagem.

---

## Como executar

Clone o repositório e acesse a pasta do projeto:

```bash
git clone <url-do-repositorio>
cd LumiFlow
```

Após configurar as variáveis de ambiente, inicie a aplicação:

```powershell
.\mvnw.cmd spring-boot:run
```

O sistema estará disponível em `http://localhost:8080/login`.

---

## Testes

Execute a suíte de testes com:

```powershell
.\mvnw.cmd test
```

---

## Segurança e perfis de acesso

As senhas são armazenadas com BCrypt. As rotas são protegidas pelo Spring Security, e formulários utilizam proteção CSRF.

| Perfil | Acesso principal |
| --- | --- |
| `SUPORTE` | Administração de usuários, cadastros e demais recursos do sistema. |
| `PCP_SUPERVISOR` | Cadastros operacionais e criação de ordens de produção. |
| `GESTAO` | Consulta de dashboard, ordens e relatórios. |
| `OPERADOR` | Consulta de ordens e operações permitidas no fluxo produtivo. |

---

## Banco de dados

O PostgreSQL é gerenciado por migrações do Flyway, executadas na inicialização da aplicação. A criação e evolução do schema devem ser feitas por novos scripts em `src/main/resources/db/migration`.

O Hibernate é configurado com `spring.jpa.hibernate.ddl-auto=validate`, garantindo que o modelo da aplicação seja validado sem alterar automaticamente a estrutura do banco.

---

## Implantação com Docker

O projeto inclui um `Dockerfile` com build em múltiplos estágios e execução com o perfil `prod`.

```bash
docker build -t lumiflow .
docker run --rm -p 8080:8080 \
  -e PGHOST=host \
  -e PGPORT=5432 \
  -e PGDATABASE=lumiflow \
  -e PGUSER=usuario \
  -e PGPASSWORD=senha \
  lumiflow
```

---

## Licença

Este projeto é distribuído sob a licença MIT. Consulte [LICENSE](LICENSE) para mais informações.
