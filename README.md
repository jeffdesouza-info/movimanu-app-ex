# movimanu-app-ex

Base do sistema de Movimentos Manuais, preparada para uma API REST em Java 21 com Spring Boot, Spring Cloud, DDD e persistência H2, além de um frontend Angular.

Esta etapa contém somente estrutura, dependências e configuração. Ainda não há código Java nem Angular.

## Estrutura

```text
.
|-- backend/
|   `-- movimento-manual-service/
|       `-- src/
|           |-- main/
|           |   |-- java/com/antlia/movimanu/
|           |   |   |-- domain/
|           |   |   |-- application/
|           |   |   |-- infrastructure/
|           |   |   `-- interfaces/
|           |   `-- resources/
|           |       |-- db/migration/
|           |       |-- application.yml
|           |       |-- application-local.yml
|           |       `-- application-cloud.yml
|           `-- test/java/com/antlia/movimanu/
|               |-- unit/
|               `-- integration/
`-- frontend/
    `-- movimanu-web/
        `-- src/app/
            |-- core/
            |-- shared/
            `-- features/manual-movements/
```

O serviço de movimentos manuais representa o primeiro bounded context. A organização interna separa domínio, aplicação, adaptadores de infraestrutura e interface REST. Novos microsserviços devem ser adicionados como módulos irmãos em `backend`.

## Pré-requisitos

- Java 21
- Nenhuma instalação global de Maven é necessária; o Maven Wrapper usa Maven 3.9.16.

## Comandos

No PowerShell, a partir da raiz:

```powershell
.\mvnw.cmd clean verify
```

O build inicial valida a estrutura e empacota um JAR ainda sem aplicação executável. Quando a classe de inicialização for criada, remova `spring-boot.repackage.skip` ou defina-a como `false` para gerar o executable JAR:

```powershell
.\mvnw.cmd clean verify -Dspring-boot.repackage.skip=false
```

Para publicar os artefatos em um repositório Maven, informe o destino aprovado pela organização, pois AWS ou OCI ainda não foi escolhido:

```powershell
.\mvnw.cmd clean deploy "-DaltDeploymentRepository=releases::default::https://repositorio.example/repository/releases"
```

## Banco de dados local

O perfil padrão `local` usa um banco H2 persistente em `./data/movimanu`. O esquema é criado pelo Flyway a partir de `V1__create_manual_movement_schema.sql`.

Depois que a aplicação Java for implementada e iniciada, o console estará disponível em `http://localhost:8080/h2-console`, com os seguintes dados padrão:

- JDBC URL: `jdbc:h2:file:./data/movimanu;DB_CLOSE_ON_EXIT=FALSE`
- usuário: `sa`
- senha: vazia

Credenciais podem ser sobrescritas com `H2_USER` e `H2_PASSWORD`. O perfil `cloud` espera `DB_URL`, `DB_USER` e `DB_PASSWORD`, sem vincular o projeto antecipadamente a AWS ou OCI.

## Modelo relacional

- `PRODUTO`: chave primária `COD_PRODUTO`.
- `PRODUTO_COSIF`: chave primária composta por `COD_PRODUTO` e `COD_COSIF`; `COD_PRODUTO` referencia `PRODUTO`.
- `MOVIMENTO_MANUAL`: chave primária composta por mês, ano, número do lançamento, produto e COSIF; o par produto/COSIF referencia `PRODUTO_COSIF`.

O tipo `SMALLDATETIME` do modelo original é preservado no H2 por meio de um domínio SQL sobre `TIMESTAMP(0)`.
