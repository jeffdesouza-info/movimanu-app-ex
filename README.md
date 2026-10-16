# movimanu-app-ex

Base do sistema de Movimentos Manuais, preparada para uma API REST em Java 21 com Spring Boot, Spring Cloud, DDD e persistência H2, além de um frontend Angular.

O backend já possui inicialização Spring Boot, JAR executável, perfis local e de testes, modelos de domínio e contratos de consulta e inclusão, com execução separada de testes unitários e de integração. O workspace Angular está inicializado com componentes standalone, Reactive Forms, HttpClient, ambientes, proxy local e testes iniciais. As funcionalidades de consulta e inclusão de movimentos permanecem pendentes.

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
        |-- angular.json
        |-- package.json
        |-- package-lock.json
        |-- proxy.conf.json
        |-- public/
        `-- src/
            |-- main.ts
            |-- app/
            |   |-- app.config.ts
            |   |-- app.ts
            |   |-- app.spec.ts
            |   |-- core/
            |   |-- shared/
            |   `-- features/manual-movements/
            `-- environments/
                |-- environment.ts
                `-- environment.production.ts
```

O serviço de movimentos manuais representa o primeiro bounded context. A organização interna separa domínio, aplicação, adaptadores de infraestrutura e interface REST. Novos microsserviços devem ser adicionados como módulos irmãos em `backend`.

## Domínio e contratos

Os modelos em `domain/model` usam campos diretos: `String` para códigos e descrições, `int` para mês e ano, `Long` para lançamento, `BigDecimal` para valor e `Instant` para data/hora. Os identificadores compostos têm componentes diretos e `MovementDraft` recebe os seis dados de inclusão.

As validações ficam nos construtores dos modelos, com apoio do auxiliar interno `ModelValidation`. Os códigos preservam zeros à esquerda e respeitam comprimentos máximos de quatro e onze caracteres. O valor valida a escala original e os limites antes de ser normalizado para duas casas, sem arredondamento.

As quatro portas em `application/port` definem consulta de produtos, COSIFs, movimentos e inclusão. A consulta de movimentos recebe a página como `int` e retorna `MovementPage<ManualMovementProjection>`; a projeção em `application/projection` inclui a descrição do produto. Domínio e contratos permanecem independentes de Spring, JPA e HTTP. Os casos de uso, adaptadores de persistência e endpoints ainda serão implementados.

## Pré-requisitos

- Java 21.
- Nenhuma instalação global de Maven é necessária; o Maven Wrapper usa Maven 3.9.16.
- Node.js 24.x, no mínimo 24.15.0, e npm 11.19.0, conforme `engines` e `packageManager` do frontend.
- Os scripts npm utilizam o Angular CLI instalado no workspace; não é necessária instalação global do CLI.

## Comandos do backend

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

Na validação reportada da conclusão do domínio e das portas, `.\mvnw.cmd test` passou e `.\mvnw.cmd clean verify` concluiu com sucesso. Os resultados abaixo foram informados pelo Codex; testes e build não foram executados novamente nesta atualização documental.

| Grupo | Executados | Falhas | Erros | Ignorados |
| --- | --- | --- | --- | --- |
| Unitários — Surefire (`*Test`) | 22 | 0 | 0 | 0 |
| Integração — Failsafe (`*IT`) | 1 | 0 | 0 | 0 |

Os testes unitários verificam modelos, validações, projeção e paginação, além da declaração do ponto de entrada Spring Boot. O teste de integração confirma a inicialização com H2 em memória e a existência da tabela `PRODUTO` após a migração.

Na preparação inicial do backend, o JAR executável iniciou no perfil `local`, com H2 persistente e aplicação da migração `V1__create_manual_movement_schema.sql`.

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

Na execução reportada, o JaCoCo consolidado registrou **90 de 90 linhas elegíveis cobertas (100%)**, com aprovação da verificação configurada e preservação das exclusões existentes de configuração e inicialização. A cobertura abrange o código implementado até esta etapa. Persistência JPA, casos de uso, contratos REST e comportamentos funcionais da interface permanecem nas próximas tarefas.

Na validação inicial do backend, o Flyway avisou que a versão resolvida do H2 era mais nova que a versão com a qual havia sido verificado. A migração e os testes passaram, mantendo-se as versões gerenciadas pelo projeto.

## Frontend Angular

O workspace fica em `frontend/movimanu-web`. As dependências diretas estão fixadas em `package.json`, com resolução registrada em `package-lock.json`.

| Tecnologia | Versão utilizada na validação |
| --- | --- |
| Node.js | 24.21.0 |
| npm | 11.19.0 |
| Angular e Angular CLI | 22.2.3 |
| TypeScript | 6.0.3 |
| RxJS | 7.8.2 |
| Vitest | 4.1.12 |

### Preparação e comandos

Em uma cópia nova, a partir da raiz do repositório, entre no workspace, confira as ferramentas selecionadas pelo terminal e instale as dependências pelo lockfile:

```powershell
Set-Location .\frontend\movimanu-web
node --version
npm --version
npm ci
```

No Windows, `where.exe node` e `where.exe npm` permitem conferir os caminhos encontrados. O terminal do VSCode/Codex deve resolver as versões previstas para o projeto.

Execute os testes sem observação e o build de produção:

```powershell
npm test
npm run build
```

O script `test` já contém `--watch=false`; `npm run build` usa a configuração `production` definida como padrão em `angular.json`.

Para iniciar o servidor de desenvolvimento:

```powershell
npm start
```

A aplicação é servida em `http://localhost:4200`. O proxy configurado em `proxy.conf.json` e associado ao alvo `serve` encaminha `/api/v1/**` para `http://localhost:8080`. Inicie o backend separadamente para usar esse encaminhamento.

### Ambientes e validação inicial

`src/environments/environment.ts` e `src/environments/environment.production.ts` definem `apiBaseUrl: '/api/v1'`. O build de produção substitui o arquivo de desenvolvimento pelo de produção. O proxy local se aplica ao servidor de desenvolvimento.

Na execução técnica reportada, `npm test` passou com **um arquivo e dois testes**, `npm run build` passou e o servidor de desenvolvimento iniciou com o proxy configurado, sendo encerrado após a conferência. Node.js 24.21.0 e npm 11.19.0 também foram confirmados em terminal após o ajuste de prioridade do PATH.

Os testes de `src/app/app.spec.ts` verificam a criação do componente inicial e a renderização do título. A execução usa o alvo `@angular/build:unit-test`, com Vitest. Esses testes validam a base inicial; os comportamentos de consulta e inclusão ainda serão implementados e testados.

O diretório de saída esperado é `dist/movimanu-web/`, relativo ao workspace, com os arquivos de navegador em `browser/`. A partir da raiz do repositório, isso corresponde a `frontend/movimanu-web/dist/movimanu-web/browser/`. `angular.json` usa `@angular/build:application` sem sobrescrever `outputPath`; a localização efetivamente gerada deve ser conferida no resumo do build. Os diretórios `dist/`, `node_modules/` e `.angular/cache/` são ignorados pelo Git.

Os resultados acima registram a execução técnica informada e a configuração publicada, sem nova execução de testes ou build nesta atualização documental. Não foi informado caminho de relatório de testes do frontend salvo em arquivo.

Referências técnicas: [configuração do workspace Angular](https://angular.dev/reference/configs/workspace-config), [build](https://angular.dev/cli/build) e [proxy de desenvolvimento](https://angular.dev/tools/cli/serve).

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
