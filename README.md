# LumiFlow

Sistema web para gestão e acompanhamento do fluxo de produção de uma vidraçaria. O projeto foi desenvolvido como parte de um Projeto Integrador Exten# LumiFlow

Sistema web para gestão e acompanhamento do fluxo de produção de uma vidraçaria. O projeto foi desenvolvido como parte de um Projeto Integrador Extensionista e centraliza o controle de usuários, setores, máquinas e demais informações relacionadas à operação.

> Status: MVP em desenvolvimento

---

# Sumário

* Visão Geral
* Funcionalidades
* Roadmap de Versões
* Tecnologias
* Estrutura do Projeto
* Arquitetura
* Pré-requisitos
* Configuração
* Como Executar
* Segurança e Perfis de Acesso
* Banco de Dados
* Licença

---

# Visão Geral

O LumiFlow foi criado para auxiliar no gerenciamento do fluxo produtivo de uma vidraçaria, permitindo acompanhar recursos, processos e informações operacionais em um único ambiente.

A aplicação possui autenticação, controle de permissões e interface web renderizada no servidor através do Thymeleaf, oferecendo uma experiência simples e centralizada para operadores, supervisores e administradores.

---

# Funcionalidades

## Implementadas

### Autenticação e Segurança

* Login de usuários
* Controle de sessão
* Senhas criptografadas com BCrypt
* Controle de acesso por perfil

### Gestão de Usuários

* Cadastro de usuários
* Edição de usuários
* Exclusão de usuários
* Controle de nível de acesso
* Associação de usuários aos setores

### Gestão de Setores

* Cadastro de setores
* Edição de setores
* Exclusão de setores

### Gestão de Máquinas

* Cadastro de máquinas
* Edição de máquinas
* Exclusão de máquinas
* Associação com setores
* Filtro por setor

### Sistema

* Dashboard inicial
* Navegação protegida
* Migrações automáticas com Flyway

---

# Roadmap de Versões

| Versão | Status             | Funcionalidades                  |
| ------ | ------------------ | -------------------------------- |
| v1.0.0 | Em desenvolvimento | Usuários, Setores e Máquinas     |
| v1.1.0 | Planejada          | Produtos                         |
| v1.2.0 | Planejada          | Roteiros de Produção             |
| v1.3.0 | Planejada          | Ordens de Produção               |
| v1.4.0 | Planejada          | Lançamentos de Produção          |
| v1.5.0 | Planejada          | Dashboard Operacional            |
| v1.6.0 | Planejada          | Relatórios Gerenciais            |
| v2.0.0 | Futuro             | Melhorias gerais e novos módulos |

---

# Tecnologias

| Camada         | Tecnologias             |
| -------------- | ----------------------- |
| Back-end       | Java 21, Spring Boot    |
| MVC            | Spring MVC              |
| Persistência   | Spring Data JPA         |
| Segurança      | Spring Security, BCrypt |
| Interface      | Thymeleaf               |
| Front-end      | HTML5, CSS3, JavaScript |
| Banco de Dados | MySQL                   |
| Migrações      | Flyway                  |
| Mapeamento     | MapStruct               |
| Produtividade  | Lombok                  |
| Build          | Maven                   |
| Testes         | Spring Boot Test        |

---

# Estrutura do Projeto

```text
LumiFlow/
├── .mvn/
├── src/
│   ├── main/
│   │   ├── java/br/com/lumiflow/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   │   ├── maquina/
│   │   │   │   ├── nivelacesso/
│   │   │   │   ├── setor/
│   │   │   │   └── usuario/
│   │   │   ├── entity/
│   │   │   │   └── enums/
│   │   │   ├── exception/
│   │   │   ├── mapper/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   ├── service/
│   │   │   └── LumiflowApplication.java
│   │   └── resources/
│   │       ├── db/migration/
│   │       ├── static/
│   │       │   ├── css/
│   │       │   └── js/
│   │       ├── templates/
│   │       │   ├── fragments/
│   │       │   ├── dashboard/
│   │       │   ├── maquina/
│   │       │   ├── setor/
│   │       │   └── usuario/
│   │       └── application.properties
│   └── test/
├── pom.xml
├── mvnw
├── mvnw.cmd
└── LICENSE
```

---

# Arquitetura

O projeto segue uma arquitetura em camadas:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Banco de Dados

Controller
    ↓
DTO
↕
Mapper
↕
Entity
```

## Responsabilidades

### Controller

Responsável por:

* Receber requisições HTTP
* Validar entradas
* Preparar dados para as telas
* Controlar redirecionamentos

### Service

Responsável por:

* Regras de negócio
* Validações
* Fluxos da aplicação
* Integração entre camadas

### Repository

Responsável por:

* Operações de banco de dados
* Consultas JPA
* Persistência das entidades

### DTO

Responsável por:

* Transferência segura de dados
* Isolamento da camada web

### Mapper

Responsável por:

* Conversão Entity ↔ DTO
* Redução de código repetitivo

### Security

Responsável por:

* Autenticação
* Autorização
* Controle de permissões

---

# Pré-requisitos

* JDK 21 ou superior
* MySQL
* Maven (opcional, pois o projeto utiliza Maven Wrapper)
* Git (opcional)

---

# Configuração

Defina as variáveis de ambiente antes de iniciar a aplicação:

| Variável          | Exemplo                              |
| ----------------- | ------------------------------------ |
| DB_MYSQL_URL      | jdbc:mysql://localhost:3306/lumiflow |
| DB_MYSQL_USERNAME | root                                 |
| DB_MYSQL_PASSWORD | sua_senha                            |

Exemplo PowerShell:

```powershell
$env:DB_MYSQL_URL="jdbc:mysql://localhost:3306/lumiflow"
$env:DB_MYSQL_USERNAME="root"
$env:DB_MYSQL_PASSWORD="sua_senha"
```

---

# Como Executar

Clone o repositório:

```bash
git clone <repositorio>
```

Acesse a pasta:

```bash
cd LumiFlow
```

Execute a aplicação:

```powershell
.\mvnw.cmd spring-boot:run
```

Acesse:

```text
http://localhost:8080/login
```

---

# Executando Testes

```powershell
.\mvnw.cmd test
```

---

# Segurança e Perfis de Acesso

As senhas são armazenadas utilizando BCrypt.

Perfis atualmente suportados:

| Perfil         | Responsabilidade                  |
| -------------- | --------------------------------- |
| SUPORTE        | Administração completa do sistema |
| PCP_SUPERVISOR | Cadastros operacionais e ordens   |
| GESTAO         | Dashboard e relatórios            |
| OPERADOR       | Produção e lançamentos            |

---

# Banco de Dados

As migrações são executadas automaticamente pelo Flyway.

Arquivos atuais:

```text
V1__schema_inicial.sql
V2__dados_iniciais.sql
```

O projeto utiliza:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Garantindo que alterações estruturais sejam controladas exclusivamente pelas migrações.

---

# Licença

Este projeto é distribuído sob a licença MIT.

Consulte o arquivo LICENSE para mais informações.
sionista e centraliza o controle de usuários, setores, máquinas e demais informações relacionadas à operação.

> Status: MVP em desenvolvimento

---

# Sumário

* Visão Geral
* Funcionalidades
* Roadmap de Versões
* Tecnologias
* Estrutura do Projeto
* Arquitetura
* Pré-requisitos
* Configuração
* Como Executar
* Segurança e Perfis de Acesso
* Banco de Dados
* Licença

---

# Visão Geral

O LumiFlow foi criado para auxiliar no gerenciamento do fluxo produtivo de uma vidraçaria, permitindo acompanhar recursos, processos e informações operacionais em um único ambiente.

A aplicação possui autenticação, controle de permissões e interface web renderizada no servidor através do Thymeleaf, oferecendo uma experiência simples e centralizada para operadores, supervisores e administradores.

---

# Funcionalidades

## Implementadas

### Autenticação e Segurança

* Login de usuários
* Controle de sessão
* Senhas criptografadas com BCrypt
* Controle de acesso por perfil

### Gestão de Usuários

* Cadastro de usuários
* Edição de usuários
* Exclusão de usuários
* Controle de nível de acesso
* Associação de usuários aos setores

### Gestão de Setores

* Cadastro de setores
* Edição de setores
* Exclusão de setores

### Gestão de Máquinas

* Cadastro de máquinas
* Edição de máquinas
* Exclusão de máquinas
* Associação com setores
* Filtro por setor

### Sistema

* Dashboard inicial
* Navegação protegida
* Migrações automáticas com Flyway

---

# Roadmap de Versões

| Versão | Status             | Funcionalidades                  |
| ------ | ------------------ | -------------------------------- |
| v1.0.0 | Em desenvolvimento | Usuários, Setores e Máquinas     |
| v1.1.0 | Planejada          | Produtos                         |
| v1.2.0 | Planejada          | Roteiros de Produção             |
| v1.3.0 | Planejada          | Ordens de Produção               |
| v1.4.0 | Planejada          | Lançamentos de Produção          |
| v1.5.0 | Planejada          | Dashboard Operacional            |
| v1.6.0 | Planejada          | Relatórios Gerenciais            |
| v2.0.0 | Futuro             | Melhorias gerais e novos módulos |

---

# Tecnologias

| Camada         | Tecnologias             |
| -------------- | ----------------------- |
| Back-end       | Java 21, Spring Boot    |
| MVC            | Spring MVC              |
| Persistência   | Spring Data JPA         |
| Segurança      | Spring Security, BCrypt |
| Interface      | Thymeleaf               |
| Front-end      | HTML5, CSS3, JavaScript |
| Banco de Dados | MySQL                   |
| Migrações      | Flyway                  |
| Mapeamento     | MapStruct               |
| Produtividade  | Lombok                  |
| Build          | Maven                   |
| Testes         | Spring Boot Test        |

---

# Estrutura do Projeto

```text
LumiFlow/
├── .mvn/
├── src/
│   ├── main/
│   │   ├── java/br/com/lumiflow/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   │   ├── maquina/
│   │   │   │   ├── nivelacesso/
│   │   │   │   ├── setor/
│   │   │   │   └── usuario/
│   │   │   ├── entity/
│   │   │   │   └── enums/
│   │   │   ├── exception/
│   │   │   ├── mapper/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   ├── service/
│   │   │   └── LumiflowApplication.java
│   │   └── resources/
│   │       ├── db/migration/
│   │       ├── static/
│   │       │   ├── css/
│   │       │   └── js/
│   │       ├── templates/
│   │       │   ├── fragments/
│   │       │   ├── dashboard/
│   │       │   ├── maquina/
│   │       │   ├── setor/
│   │       │   └── usuario/
│   │       └── application.properties
│   └── test/
├── pom.xml
├── mvnw
├── mvnw.cmd
└── LICENSE
```

---

# Arquitetura

O projeto segue uma arquitetura em camadas:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Banco de Dados

Controller
    ↓
DTO
↕
Mapper
↕
Entity
```

## Responsabilidades

### Controller

Responsável por:

* Receber requisições HTTP
* Validar entradas
* Preparar dados para as telas
* Controlar redirecionamentos

### Service

Responsável por:

* Regras de negócio
* Validações
* Fluxos da aplicação
* Integração entre camadas

### Repository

Responsável por:

* Operações de banco de dados
* Consultas JPA
* Persistência das entidades

### DTO

Responsável por:

* Transferência segura de dados
* Isolamento da camada web

### Mapper

Responsável por:

* Conversão Entity ↔ DTO
* Redução de código repetitivo

### Security

Responsável por:

* Autenticação
* Autorização
* Controle de permissões

---

# Pré-requisitos

* JDK 21 ou superior
* MySQL
* Maven (opcional, pois o projeto utiliza Maven Wrapper)
* Git (opcional)

---

# Configuração

Defina as variáveis de ambiente antes de iniciar a aplicação:

| Variável          | Exemplo                              |
| ----------------- | ------------------------------------ |
| DB_MYSQL_URL      | jdbc:mysql://localhost:3306/lumiflow |
| DB_MYSQL_USERNAME | root                                 |
| DB_MYSQL_PASSWORD | sua_senha                            |

Exemplo PowerShell:

```powershell
$env:DB_MYSQL_URL="jdbc:mysql://localhost:3306/lumiflow"
$env:DB_MYSQL_USERNAME="root"
$env:DB_MYSQL_PASSWORD="sua_senha"
```

---

# Como Executar

Clone o repositório:

```bash
git clone <repositorio>
```

Acesse a pasta:

```bash
cd LumiFlow
```

Execute a aplicação:

```powershell
.\mvnw.cmd spring-boot:run
```

Acesse:

```text
http://localhost:8080/login
```

---

# Executando Testes

```powershell
.\mvnw.cmd test
```

---

# Segurança e Perfis de Acesso

As senhas são armazenadas utilizando BCrypt.

Perfis atualmente suportados:

| Perfil         | Responsabilidade                  |
| -------------- | --------------------------------- |
| SUPORTE        | Administração completa do sistema |
| PCP_SUPERVISOR | Cadastros operacionais e ordens   |
| GESTAO         | Dashboard e relatórios            |
| OPERADOR       | Produção e lançamentos            |

---

# Banco de Dados

As migrações são executadas automaticamente pelo Flyway.

Arquivos atuais:

```text
V1__schema_inicial.sql
V2__dados_iniciais.sql
```

O projeto utiliza:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Garantindo que alterações estruturais sejam controladas exclusivamente pelas migrações.

---

# Licença

Este projeto é distribuído sob a licença MIT.

Consulte o arquivo LICENSE para mais informações.
