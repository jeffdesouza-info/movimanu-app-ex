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

No H2, `SMALLDATETIME` é representado por um domínio SQL sobre `TIMESTAMP(0)`, definido em `backend/movimento-manual-service/src/main/resources/db/migration/V1__create_manual_movement_schema.sql`. A precisão `(0)` indica ausência de casas fracionárias nos segundos; essa representação ainda admite segundos diferentes de `00`.

A equivalência comportamental de `DAT_MOVIMENTO` depende dos complementos previstos em `specs/001-movimentos-manuais/plan.md`: arredondamento ao minuto no adaptador, segundos em `00`, respeito à faixa de datas de `1900-01-01` a `2079-06-06` e testes de arredondamento e limites, incluindo virada de dia. Na etapa estrutural, essa equivalência permanece pendente de implementação e validação; sua confirmação e as evidências devem ser registradas no README ao concluir T013 de `specs/001-movimentos-manuais/tasks.md`.

Referências técnicas: [TIMESTAMP no H2](https://h2database.com/html/datatypes.html) e [SMALLDATETIME no SQL Server](https://learn.microsoft.com/en-us/sql/t-sql/data-types/smalldatetime-transact-sql).
