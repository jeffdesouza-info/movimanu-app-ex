# movimanu-app-ex

Sistema de Movimentos Manuais com API REST em Java 21, Spring Boot, Spring Cloud, DDD e persistência H2, além de um frontend Angular em desenvolvimento.

O backend já possui inicialização Spring Boot, JAR executável, perfis local e de testes, modelos de domínio, persistência JPA, casos de uso de consulta e inclusão concorrente e os quatro endpoints REST, com validações e erros em `ProblemDetail`. A validação integrada do backend está concluída, com execução separada de testes unitários e de integração. O frontend Angular já possui consulta automática de movimentos, grid de sete colunas, paginação e estados de carregamento, erro e ausência de registros. O formulário permanece no estado visual inicial; suas interações, o fluxo de inclusão e a validação final da aplicação pertencem às próximas tarefas.

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
|           |       |-- db/local/demo-data.sql
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
            |       |-- manual-movements-api.service.ts
            |       |-- manual-movements.models.ts
            |       |-- manual-movements-page.component.ts
            |       |-- manual-movements-page.component.html
            |       |-- manual-movements-page.component.css
            |       `-- manual-movements-page.component.spec.ts
            `-- environments/
                |-- environment.ts
                `-- environment.production.ts
```

O serviço de movimentos manuais representa o primeiro bounded context. A organização interna separa domínio, aplicação, adaptadores de infraestrutura e interface REST. Novos microsserviços devem ser adicionados como módulos irmãos em `backend`.

## Domínio e contratos

Os modelos em `domain/model` usam campos diretos: `String` para códigos e descrições, `int` para mês e ano, `Long` para lançamento, `BigDecimal` para valor e `Instant` para data/hora. Os identificadores compostos têm componentes diretos e `MovementDraft` recebe os seis dados de inclusão.

As validações ficam nos construtores dos modelos, com apoio do auxiliar interno `ModelValidation`. Os códigos preservam zeros à esquerda e respeitam comprimentos máximos de quatro e onze caracteres. O valor valida a escala original e os limites antes de ser normalizado para duas casas, sem arredondamento.

As portas em `application/port` definem consulta de produtos, COSIFs, movimentos, inclusão e execução de operações em nova transação. A consulta de movimentos recebe a página como `int` e retorna `MovementPage<ManualMovementProjection>`; a projeção em `application/projection` inclui a descrição do produto. Domínio e contratos permanecem independentes de Spring, JPA e HTTP. Os casos de uso e adaptadores de consulta e inclusão estão implementados e são acessados pela interface REST, que usa DTOs próprios.

## Consultas implementadas

| Caso de uso | Comportamento |
| --- | --- |
| `ListProductsUseCase` | Lista produtos em ordem crescente de código; retorna lista vazia quando não há registros. |
| `ListProductCosifsUseCase` | Verifica a existência do produto antes de consultar seus COSIFs. Produto inexistente gera `ProductNotFoundException`; produto existente sem vínculos retorna lista vazia. Os COSIFs são ordenados por código crescente. |
| `FindManualMovementsUseCase` | Rejeita página negativa e consulta movimentos com descrição do produto, seis registros por página e os totais reais. |

Os adaptadores e repositórios ficam em `infrastructure/persistence/adapter` e `repository`. A configuração `QueryUseCaseConfiguration` conecta os casos de uso às portas. As dependências de Spring/JPA permanecem na infraestrutura.

A consulta JPQL de movimentos carrega `ManualMovementEntity` com `join fetch` para o produto/COSIF e o produto. O adaptador usa `ManualMovementMapper.toProjection` para construir `ManualMovementProjection`, incluindo a descrição do produto sem consultas adicionais por linha e preservando a conversão temporal aplicada à entidade. A ordenação é por ano, mês e lançamento decrescentes, seguidos de produto e COSIF crescentes. A consulta do maior lançamento filtra somente mês e ano, independentemente do produto/COSIF.

O adaptador calcula o deslocamento com `long` e consulta o total antes da busca paginada. Para página além dos resultados, retorna conteúdo vazio com a página solicitada, tamanho seis e totais reais, sem executar a busca paginada. Os testes cobrem sete movimentos com `page=2`, `357913942` e `Integer.MAX_VALUE`; os totais permanecem sete elementos e duas páginas.

## Inclusão concorrente

`IncludeManualMovementUseCase` recebe `MovementDraft`, valida o vínculo produto/COSIF e calcula o maior lançamento do mês/ano acrescido de um, iniciando em um quando o período está vazio. A sequência independe de produto/COSIF; o limite `999999999999999999` é verificado antes do incremento. Vínculo inexistente gera `ProductCosifNotFoundException`.

`TransactionExecutionPort` mantém o contrato transacional independente de frameworks. Na infraestrutura, `SpringTransactionExecutionAdapter` usa `TransactionTemplate` com `REQUIRES_NEW`: cada tentativa repete validação do vínculo, leitura do máximo e gravação em uma transação própria. `InclusionUseCaseConfiguration` fornece `Clock.systemUTC()`; data/hora e usuário `TESTE` são gerados no backend, com normalização temporal pelo mapeador e conversor.

`JpaManualMovementInclusionAdapter` grava por `EntityManager.persist` seguido de `flush`, garantindo somente inserção, sem atualizar um movimento existente por `merge`. A repetição ocorre após o rollback e reconhece `UK_MOVIMENTO_MANUAL_PERIODO_LANCAMENTO`, seu índice H2 e o formato `PK_MOVIMENTO_MANUAL INDEX PRIMARY_KEY_<n>`, com os prefixos de esquema reportados pelo H2. A colisão da chave completa também implica repetição de período/lançamento; outras chaves e erros de persistência não acionam novas tentativas.

O caso de uso captura `ManualMovementSequenceCollisionException` e tenta novamente com nova transação e nova leitura, até três tentativas no total. Esgotamento ou limite da sequência gera `ManualMovementSequenceConflictException`, traduzida pela interface REST em HTTP `409`. Outros erros inesperados de persistência recebem HTTP `500` com mensagem genérica.

## Persistência JPA

A infraestrutura em `infrastructure/persistence` contém `ProductEntity`, `ProductCosifEntity` e `ManualMovementEntity`, os identificadores compostos `ProductCosifEntityId` e `ManualMovementEntityId`, três mapeadores e `SmallDateTimeConverter`. As anotações JPA ficam na infraestrutura; os modelos de domínio mantêm campos diretos e independência de frameworks.

A migração `V2__unique_period_launch_number.sql` adiciona `UK_MOVIMENTO_MANUAL_PERIODO_LANCAMENTO` sobre `(DAT_ANO, DAT_MES, NUM_LANCAMENTO)`, complementando a chave primária e impedindo a repetição por período inclusive entre produtos diferentes. A migração V1 foi preservada. A geração da sequência, as transações e as novas tentativas estão implementadas no fluxo de inclusão descrito acima.

## API REST implementada

`ManualMovementController`, em `interfaces/rest`, expõe os recursos abaixo. Sucessos usam `application/json`; erros usam `application/problem+json`.

| Método e caminho | Comportamento |
| --- | --- |
| `GET /api/v1/produtos` | `200` com produtos ordenados por código; sem registros, retorna `[]`. |
| `GET /api/v1/produtos/{codigoProduto}/cosifs` | `200` com COSIFs do produto; produto existente sem vínculos retorna `[]`; produto inexistente retorna `404`. |
| `GET /api/v1/movimentos-manuais?page=0` | `200` com `content`, `page`, `size=6`, `totalElements` e `totalPages`. A página começa em zero; além dos resultados, mantém a página solicitada e os totais reais, com conteúdo vazio. |
| `POST /api/v1/movimentos-manuais` | `201` com o movimento persistido, incluindo descrição do produto, lançamento, data/hora e usuário gerados pelo backend. |

O POST recebe exatamente os seis campos abaixo; todos são obrigatórios e não nulos:

```json
{
  "mes": 10,
  "ano": 2026,
  "codigoProduto": "0001",
  "codigoCosif": "10000000001",
  "descricao": "Ajuste manual",
  "valor": "150.50"
}
```

`mes` é inteiro de 1 a 12; `ano`, inteiro de 1 a 9999. Códigos são textos com até quatro e onze caracteres, preservando zeros à esquerda. A descrição deve conter ao menos um caractere diferente de espaço e ter até 50 caracteres, contando e preservando os espaços recebidos.

`valor` é texto com sinal negativo opcional, de um a 16 dígitos inteiros e, opcionalmente, ponto seguido de uma ou duas casas. Zero, vírgula, milhar, notação científica, espaços e formatos mistos são rejeitados antes da conversão para `BigDecimal`. A resposta usa ponto e duas casas, sem perda de precisão. A aceitação de vírgula no formulário será tratada no frontend.

`ProdutoResponse` contém `codigoProduto` e `descricaoProduto`; `CosifResponse`, `codigoCosif` e `codigoClassificacao`. `MovimentoResponse`, usado no POST e nos itens da consulta, contém os seis campos da entrada mais `descricaoProduto`, `numeroLancamento`, `dataMovimento` e `codigoUsuario`. Valor e lançamento são strings; a data usa ISO 8601 em UTC com segundos `00`, e o usuário é `TESTE`. Descrições de produto e classificações podem ser `null`; listas vazias são `[]`.

Bean Validation e `IncluirMovimentoRequestDeserializer` rejeitam tipos JSON incompatíveis, propriedades não previstas e números fora da faixa de `int`, sem coerção silenciosa. `PageParameterParser` adota página zero quando omitida e rejeita parâmetro vazio, negativo, não inteiro ou fora da faixa de `int` com `400` associado a `page`.

`RestExceptionHandler` retorna `ProblemDetail` com `type="about:blank"`, `title`, `status`, `detail`, `instance` e a extensão `errors` no primeiro nível. `instance` contém o caminho com `/api/v1`, sem query string. Cada erro de campo contém `field` e `message` em pt-BR; tipos incompatíveis e estouro de `int` em `mes` ou `ano` preservam o campo. Erros globais ou sem campo identificável usam `errors=[]`.

Entradas inválidas retornam `400`; no POST, produto inexistente é associado a `codigoProduto`, e COSIF sem vínculo, a `codigoCosif`. Produto inexistente na consulta de COSIFs retorna `404`. Conflitos de sequência retornam `409`; falhas inesperadas retornam `500`, sem expor detalhes internos.

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

Na validação final da T008, o Codex reportou sucesso em `.\mvnw.cmd test` e `.\mvnw.cmd clean verify`. Os XMLs Surefire/Failsafe fornecidos foram conferidos e confirmam os resultados abaixo, sem sobreposição entre os grupos. Os relatórios de integração registram a JVM em `America/Sao_Paulo`. Testes e build não foram executados novamente nesta atualização documental.

| Grupo | Suítes | Executados | Falhas | Erros | Ignorados |
| --- | --- | --- | --- | --- | --- |
| Unitários — Surefire (`*Test`) | 26 | 119 | 0 | 0 | 0 |
| Integração — Failsafe (`*IT`) | 6 | 74 | 0 | 0 | 0 |

O resumo Failsafe confirma 74 execuções concluídas, sem falhas, erros, ignorados, instabilidades ou timeout. Os logs registram a validação das duas migrações Flyway e sua aplicação nos bancos inicializados durante os testes.

Os testes unitários verificam modelos, validações, projeção, paginação, identificadores JPA, mapeadores, conversor temporal, casos de uso e adaptadores de consulta e inclusão, além da declaração do ponto de entrada Spring Boot. Na inclusão, cobrem primeiro lançamento, continuidade após o máximo, vínculo inválido, limite numérico, repetição restrita a colisões, esgotamento das três tentativas e propagação de outros erros. O teste do adaptador verifica a ordem `persist`/`flush` e a ausência de chamada a `merge`.

A interface REST possui testes unitários próprios em `src/test/java/com/antlia/movimanu/unit/interfaces/rest`: validação do valor textual, Bean Validation do DTO, desserialização sem coerção, parsing de `page` e tratamento de erros. Os limites `2147483648` e `-2147483649` são exercitados em `mes` e `ano`, verificando a identificação do campo no erro.

Os testes de integração verificam o esquema migrado, os relacionamentos persistidos, a conversão temporal, a restrição única entre produtos diferentes, a carga demonstrativa local e as consultas. `src/test/java/com/antlia/movimanu/integration/JpaQueryAdaptersIT.java`, relativo ao módulo backend, cobre produto sem COSIFs versus inexistente, coleções vazias, projeção, ordenação, páginas normais e além dos resultados, páginas com deslocamento alto e maior lançamento por período. Também verifica que o literal SQL `2026-01-01 12:00:00` é lido como `2026-01-01T12:00:00Z` na entidade e na projeção.

`src/test/java/com/antlia/movimanu/integration/ManualMovementRestIT.java` possui 56 execuções e cobre os quatro endpoints, campos e tipos exatos dos DTOs, paginação, respostas de erro, precisão decimal, dados automáticos e preservação dos espaços de descrições válidas. Verifica descrição com exatamente 50 caracteres na resposta e no banco, formatos aceitos de valor inteiro ou com uma ou duas casas, sinal negativo e precisão até o limite de `NUMERIC(18,2)`. Com sete movimentos, as páginas `357913942` e `Integer.MAX_VALUE` retornam conteúdo vazio e preservam a página solicitada e os totais reais.

As rejeições REST incluem ausência de dados, tipos incompatíveis, propriedades desconhecidas, descrição inválida e formatos monetários inválidos, incluindo texto vazio e sinal positivo explícito. Os quatro casos de estouro de `int` em mês/ano verificam HTTP `400`, campo correto e ausência de persistência. As asserções de `ProblemDetail` verificam títulos em pt-BR, status, caminho sem query string e estrutura dos erros por campo. A leitura REST da data também espera `2026-01-01T12:00:00Z` para o mesmo literal SQL.

O esgotamento das três tentativas é exercitado por colisões controladas na porta de inclusão, mantendo o caso de uso e a fronteira transacional reais. O teste exige HTTP `409`, exatamente três chamadas de inclusão e nenhuma alteração na quantidade de movimentos. Os testes JPA de colisão real, rollback e concorrência foram preservados.

`src/test/java/com/antlia/movimanu/integration/JpaManualMovementInclusionIT.java` acrescenta oito testes de inclusão. Eles verificam numeração, lacunas, períodos distintos, vínculo inválido, limite de 18 dígitos, dados automáticos e concorrência entre produtos diferentes. Duas regressões determinísticas usam o mesmo produto/COSIF em períodos vazio e preenchido: ambas as operações leem o mesmo máximo, a segunda gravação aguarda o commit da primeira e as asserções conferem a colisão identificada, nova leitura, ausência de gravação adicional na tentativa rejeitada e preservação das descrições e valores nos dois lançamentos finais. O perfil de teste mantém o banco em memória sem a carga demonstrativa.

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

Os XMLs JaCoCo fornecidos e conferidos apresentam a seguinte cobertura de linhas:

| Grupo | Linhas cobertas | Linhas descobertas | Cobertura |
| --- | --- | --- | --- |
| Unitários | 472/551 | 79 | 85,66% |
| Integração | 489/551 | 62 | 88,75% |
| Consolidado | **551/551** | **0** | **100%** |

O consolidado corresponde à união das linhas cobertas e das duas sessões registradas nos relatórios por grupo, com o mesmo conjunto de classes e as exclusões de configuração e inicialização. A exigência de 100% de linhas aplica-se ao consolidado; o Codex reportou aprovação da verificação configurada.

Na validação temporal da T007, o Codex reportou `clean verify` aprovado com a JVM em `UTC` e em `America/Sao_Paulo`, incluindo as regressões SQL/JPA/projeção/REST. A T008 preservou o código de produção e complementou os testes REST existentes. A validação integrada do backend está concluída. A consulta visual do frontend também está implementada; o formulário interativo, o fluxo de inclusão e a validação final da aplicação permanecem nas tarefas seguintes.

Na execução atual, o Flyway avisou que o H2 `2.4.240` é mais recente que a versão verificada, `2.3.232`. A validação das migrações e os testes passaram, mantendo-se as versões gerenciadas pelo projeto.

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

### Ambientes

`src/environments/environment.ts` e `src/environments/environment.production.ts` definem `apiBaseUrl: '/api/v1'`. O build de produção substitui o arquivo de desenvolvimento pelo de produção. O proxy local se aplica ao servidor de desenvolvimento.

### Consulta visual implementada

`ManualMovementsPageComponent`, em `src/app/features/manual-movements/`, apresenta o formulário no estado visual inicial e o grid com Mês, Ano, Código do Produto, Descrição do Produto, Nr Lançamento, Descrição do Movimento e Valor, nessa ordem. No desktop, Mês/Ano e Produto/Cosif ocupam duas colunas; Valor, Descrição e os botões Limpar, Novo e Incluir ficam na coluna esquerda, com as ações abaixo da descrição. Os estilos também preveem disposição em uma coluna para telas pequenas e rolagem horizontal do grid quando necessária.

`ManualMovementsApiService.listPage` consulta `GET /api/v1/movimentos-manuais?page=0` ao carregar a tela. As próximas consultas usam a página solicitada, mantendo a ordenação recebida da API. O componente consome `content`, `page`, `size`, `totalElements` e `totalPages`; a navegação oferece Primeira, Anterior e Próxima e respeita os limites informados. O serviço e os modelos mantêm códigos, valor e lançamento como textos. No grid, o valor troca o ponto decimal por vírgula por operação textual, preservando sinal e precisão.

O grid indica carregamento, ausência de movimentos e falha de consulta. Em erro, exibe `detail` quando informado pela API ou uma mensagem genérica em pt-BR; Tentar novamente repete somente o GET da página solicitada. O fluxo usa `switchMap` para descartar consultas anteriores e encerra a assinatura com o componente.

Uma página vazia com total de registros maior que zero recebe mensagem própria, sem intervalo invertido no resumo. Os metadados retornados permanecem intactos; somente o número apresentado no indicador é limitado ao total de páginas. Primeira e Anterior continuam disponíveis quando a página recebida é maior que zero, inclusive com `totalPages=1`; Próxima fica indisponível além do limite.

Campos e botões do formulário permanecem desabilitados nesta etapa, com textos vazios e as combo-boxes em `-- Selecione --`. A interação de Novo, Limpar e Incluir, as consultas de produtos/COSIFs e as validações do formulário pertencem à próxima etapa; o POST e o fluxo após a inclusão também permanecem pendentes.

### Validação da T009

Na execução final reportada pelo Codex, `npm test` passou com **dois arquivos e oito testes**, e `npm run build` passou. Foram informados Node.js `24.21.0` e npm `11.19.0`. Testes e build não foram executados novamente nesta revisão ou atualização documental; não foi fornecido relatório persistido da execução dos testes do frontend.

| Arquivo, relativo ao workspace | Testes nos fontes revisados | Cenários |
| --- | --- | --- |
| `src/app/app.spec.ts` | 2 | Criação da aplicação e renderização do título da página de movimentos. |
| `src/app/features/manual-movements/manual-movements-page.component.spec.ts` | 6 | GET inicial, carregamento, sete colunas, preservação da ordem recebida, seis registros e o restante na página seguinte, navegação, coleção vazia, erro com nova tentativa e páginas vazias além dos resultados, inclusive com `totalPages=1`. |

A revisão dos fontes confirmou os oito testes e os ajustes de paginação. Os cálculos do resumo e da navegação foram conferidos isoladamente, incluindo páginas normais e vazias além dos resultados, com preservação dos metadados.

A captura fornecida da aplicação em `http://localhost:4200` confirmou, em desktop e com a consulta vazia, a disposição dos campos e botões, os cabeçalhos das sete colunas sem sobreposição e a indicação de ausência de movimentos. A captura não demonstra navegação com registros, carregamento, erros ou o layout em telas pequenas; esses cenários permanecem sujeitos à validação final da aplicação, complementando os testes existentes.

A saída de build informada fica em `dist/movimanu-web/`, relativa ao workspace, com arquivos de navegador em `browser/` conforme a configuração do projeto. A partir da raiz do repositório, o caminho corresponde a `frontend/movimanu-web/dist/movimanu-web/browser/`. Os diretórios `dist/`, `node_modules/` e `.angular/cache/` são ignorados pelo Git.

A T009 está concluída. O formulário interativo, a integração do POST, a complementação dos testes do frontend e a validação final da aplicação permanecem pendentes.

Referências técnicas: [configuração do workspace Angular](https://angular.dev/reference/configs/workspace-config), [build](https://angular.dev/cli/build) e [proxy de desenvolvimento](https://angular.dev/tools/cli/serve).

## Publicação em repositório Maven

A publicação não foi validada nesta etapa. Para publicar os artefatos, informe um repositório Maven aprovado pela organização:

```powershell
.\mvnw.cmd clean deploy "-DaltDeploymentRepository=releases::default::https://repositorio.example/repository/releases"
```

A escolha do provedor de cloud, OCI ou AWS, permanece pendente.

## Banco de dados local

O perfil padrão `local` usa um banco H2 persistente em `./data/movimanu`, relativo ao diretório de trabalho. A URL pode ser sobrescrita por `H2_URL`. O Flyway cria o esquema pela migração `V1__create_manual_movement_schema.sql` e aplica a restrição única pela `V2__unique_period_launch_number.sql`; o Hibernate está configurado somente para validar o esquema.

Somente o perfil `local` habilita `spring.sql.init.mode=always` com `classpath:db/local/demo-data.sql`. O script insere os produtos demonstrativos `0001` e `0002` e três vínculos COSIF apenas quando ausentes, usando `WHERE NOT EXISTS`. Os testes usam fixtures próprias em `src/test/resources/fixtures/persistence-fixtures.sql` do backend; o teste da carga local usa outro banco em memória para evitar dados no arquivo local.

Com o backend iniciado no perfil `local`, o console H2 está configurado em `http://localhost:8080/h2-console`, com os seguintes dados padrão:

- JDBC URL: `jdbc:h2:file:./data/movimanu;DB_CLOSE_ON_EXIT=FALSE`
- usuário: `sa`
- senha: vazia

Credenciais podem ser sobrescritas com `H2_USER` e `H2_PASSWORD`. Os perfis `local` e `test` desativam o Config Client. O perfil `test`, definido em `src/test/resources/application-test.yml` do backend, usa H2 isolado em memória e mantém o console desabilitado.

O perfil `cloud` espera `DB_URL`, `DB_USER` e `DB_PASSWORD`, sem vincular o projeto antecipadamente a AWS ou OCI.

## Modelo relacional

- `PRODUTO`: chave primária `COD_PRODUTO`.
- `PRODUTO_COSIF`: chave primária composta por `COD_PRODUTO` e `COD_COSIF`; `COD_PRODUTO` referencia `PRODUTO`.
- `MOVIMENTO_MANUAL`: chave primária composta por mês, ano, número do lançamento, produto e COSIF; o par produto/COSIF referencia `PRODUTO_COSIF`. A V2 acrescenta unicidade por ano, mês e lançamento.

No H2, `SMALLDATETIME` é representado por um domínio SQL sobre `TIMESTAMP(0)`, definido em `backend/movimento-manual-service/src/main/resources/db/migration/V1__create_manual_movement_schema.sql`. A precisão `(0)` indica ausência de casas fracionárias nos segundos; essa representação ainda admite segundos diferentes de `00`.

O comportamento temporal é complementado na infraestrutura por `SmallDateTimeConverter`, aplicado ao atributo `movementAt` da entidade, mantido como `Instant`. O conversor faz a conversão explícita entre `Instant` e `LocalDateTime` em UTC, arredonda ao minuto com segundos em `00` e verifica a faixa de `1900-01-01` a `2079-06-06` antes e depois do arredondamento. O mapeador de movimentos também usa essa normalização ao criar a entidade.

Como a coluna SQL não armazena fuso, a configuração global `hibernate.jdbc.time_zone=UTC` foi removida para preservar a conversão explícita do atributo neste mapeamento. A consulta monta a projeção a partir da entidade convertida, e a REST formata o `Instant` em UTC, sem compensação fixa de horas. `format_sql` está configurado em `spring.jpa.properties.hibernate.format_sql`.

A implementação arredonda para baixo quando os segundos são inferiores a `29.999` e para cima a partir de `29.999`, inclusive em viradas de dia ou ano. O último minuto representável é `2079-06-06T23:59:00Z`; entradas cujo arredondamento ultrapassaria a faixa são rejeitadas.

As evidências de teste abaixo usam caminhos relativos a `backend/movimento-manual-service`. Esses testes integram a execução final de `clean verify` reportada acima.

| Evidência | Cenários verificados |
| --- | --- |
| `src/test/java/com/antlia/movimanu/unit/infrastructure/persistence/converter/SmallDateTimeConverterTest.java` | Valores imediatamente abaixo e no limiar de `29.999` segundos, virada de ano, limites mínimo/máximo, rejeição fora da faixa e após arredondamento, conversão nos dois sentidos em UTC e nulos. |
| `src/test/java/com/antlia/movimanu/integration/JpaPersistenceIT.java` | Gravação e releitura do horário arredondado, relacionamentos compostos, valor decimal e rejeição de lançamento repetido no período entre produtos diferentes. |
| `src/test/java/com/antlia/movimanu/integration/JpaQueryAdaptersIT.java` e `ManualMovementRestIT.java` no mesmo diretório | Leitura do literal SQL `2026-01-01 12:00:00` como `2026-01-01T12:00:00Z` na entidade, na projeção e na resposta REST. |

A equivalência temporal descrita corresponde ao comportamento implementado no adaptador e aos cenários de teste acima; o domínio SQL sobre `TIMESTAMP(0)` continua sendo apenas a representação do tipo no H2. A validação final da aplicação deverá reconfirmar esses testes e a execução com banco persistente.

Referências técnicas: [TIMESTAMP no H2](https://h2database.com/html/datatypes.html) e [SMALLDATETIME no SQL Server](https://learn.microsoft.com/en-us/sql/t-sql/data-types/smalldatetime-transact-sql).
