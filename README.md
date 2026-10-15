# movimanu-app-ex

Base do sistema de Movimentos Manuais, preparada para uma API REST em Java 21 com Spring Boot, Spring Cloud, DDD e persistência H2, além de um frontend Angular.

O backend já possui inicialização Spring Boot, JAR executável, perfis local e de testes e execução separada de testes unitários e de integração. As funcionalidades de consulta e inclusão de movimentos e o workspace Angular permanecem pendentes.

## Estrutura

```text
.
|-- backend/
|   `-- movimento-manual-service/
|       `-- src/
|           |-- main/
|           |   |-- java/com/antlia/movimanu/
|           |   |   |-- MovimanuApplication.java
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

No PowerShell, a partir da raiz, execute os testes unitários:

```powershell
.\mvnw.cmd test
```

Para executar testes unitários e de integração, gerar os relatórios de cobertura, verificar o limite configurado e empacotar o JAR executável:

```powershell
.\mvnw.cmd clean verify
```

O reempacotamento executável já está habilitado. Para iniciar o backend com o perfil local, mantendo a raiz como diretório de trabalho:

```powershell
java -jar .\backend\movimento-manual-service\target\movimento-manual-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

Em Linux/macOS, use `./mvnw test`, `./mvnw clean verify` e barras `/` no caminho do JAR.

## Validação do backend

Na validação de inicialização reportada, `.\mvnw.cmd test` passou e `.\mvnw.cmd clean verify` terminou com **BUILD SUCCESS**. O JAR executável iniciou no perfil `local`, com H2 persistente e aplicação da migração `V1__create_manual_movement_schema.sql`. O teste de integração confirmou a inicialização com H2 em memória e o esquema migrado. Estes resultados registram a execução técnica informada, sem nova execução nesta atualização documental.

| Grupo | Executados | Falhas | Ignorados |
| --- | --- | --- | --- |
| Unitários — Surefire (`*Test`) | 1 | 0 | 0 |
| Integração — Failsafe (`*IT`) | 1 | 0 | 0 |

O teste unitário verifica a declaração do ponto de entrada Spring Boot; o teste de integração valida a inicialização com banco em memória e a existência da tabela `PRODUTO` após a migração.

Os caminhos abaixo são relativos a `backend/movimento-manual-service`:

| Evidência | Caminho |
| --- | --- |
| Execução dos testes unitários | `target/surefire-reports/` |
| Execução dos testes de integração | `target/failsafe-reports/` |
| Dados de cobertura unitária | `target/jacoco-unit.exec` |
| Dados de cobertura de integração | `target/jacoco-integration.exec` |
| Dados de cobertura consolidados | `target/jacoco.exec` |
| Relatórios unitários HTML e XML | `target/site/jacoco-unit/index.html` e `target/site/jacoco-unit/jacoco.xml` |
| Relatórios de integração HTML e XML | `target/site/jacoco-integration/index.html` e `target/site/jacoco-integration/jacoco.xml` |
| Relatórios consolidados HTML e XML | `target/site/jacoco/index.html` e `target/site/jacoco/jacoco.xml` |
| Log da inicialização local reportada | `target/run-smoke.stdout.log` |

Os relatórios JaCoCo foram gerados e a verificação do limite de 100% de linhas passou, mas foram avaliadas **zero classes e zero linhas**. A única classe de produção existente, `MovimanuApplication`, está abrangida pela exclusão preexistente `*Application`. Portanto, ainda não há classes elegíveis para medir cobertura funcional; esse resultado não comprova cobertura funcional de 100%. A exigência de 100% no consolidado e as exclusões existentes permanecem mantidas para a evolução do backend.

Durante a execução reportada, o Flyway avisou que a versão resolvida do H2 era mais nova que a versão com a qual havia sido verificado. A migração e os testes passaram, mantendo-se as versões gerenciadas pelo projeto.

## Publicação em repositório Maven

A publicação não foi validada nesta etapa. Para publicar os artefatos, informe um repositório Maven aprovado pela organização:

```powershell
.\mvnw.cmd clean deploy "-DaltDeploymentRepository=releases::default::https://repositorio.example/repository/releases"
```

A escolha do provedor de cloud, OCI ou AWS, permanece pendente.

## Banco de dados local

O perfil padrão `local` usa um banco H2 persistente em `./data/movimanu`, relativo ao diretório de trabalho. O esquema é criado pelo Flyway, integrado pelo starter do Spring Boot, a partir de `V1__create_manual_movement_schema.sql`; o Hibernate está configurado somente para validá-lo.

Com o backend iniciado no perfil `local`, o console H2 está configurado em `http://localhost:8080/h2-console`, com os seguintes dados padrão:

- JDBC URL: `jdbc:h2:file:./data/movimanu;DB_CLOSE_ON_EXIT=FALSE`
- usuário: `sa`
- senha: vazia

Credenciais podem ser sobrescritas com `H2_USER` e `H2_PASSWORD`. Os perfis `local` e `test` desativam o Config Client. O perfil `test`, definido em `src/test/resources/application-test.yml` do backend, usa H2 isolado em memória e mantém o console desabilitado.

O perfil `cloud` espera `DB_URL`, `DB_USER` e `DB_PASSWORD`, sem vincular o projeto antecipadamente a AWS ou OCI.

## Modelo relacional

- `PRODUTO`: chave primária `COD_PRODUTO`.
- `PRODUTO_COSIF`: chave primária composta por `COD_PRODUTO` e `COD_COSIF`; `COD_PRODUTO` referencia `PRODUTO`.
- `MOVIMENTO_MANUAL`: chave primária composta por mês, ano, número do lançamento, produto e COSIF; o par produto/COSIF referencia `PRODUTO_COSIF`.

No H2, `SMALLDATETIME` é representado por um domínio SQL sobre `TIMESTAMP(0)`, definido em `backend/movimento-manual-service/src/main/resources/db/migration/V1__create_manual_movement_schema.sql`. A precisão `(0)` indica ausência de casas fracionárias nos segundos; essa representação ainda admite segundos diferentes de `00`.

A equivalência comportamental de `DAT_MOVIMENTO` depende dos complementos previstos no `plan.md` da especificação aplicável: arredondamento ao minuto no adaptador, segundos em `00`, respeito à faixa de datas de `1900-01-01` a `2079-06-06` e testes de arredondamento e limites, incluindo virada de dia. Esses complementos permanecem pendentes de implementação e validação; sua confirmação e as evidências devem ser registradas no README ao concluir a validação final da aplicação.

Referências técnicas: [TIMESTAMP no H2](https://h2database.com/html/datatypes.html) e [SMALLDATETIME no SQL Server](https://learn.microsoft.com/en-us/sql/t-sql/data-types/smalldatetime-transact-sql).
