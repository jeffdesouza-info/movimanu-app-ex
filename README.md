# movimanu-app-ex

Sistema de Movimentos Manuais com API REST em Java 21, Spring Boot, Spring Cloud, DDD e persistência H2, além de um frontend Angular para consulta e inclusão.

O backend já possui inicialização Spring Boot, JAR executável, perfis local e de testes, modelos de domínio, persistência JPA, casos de uso de consulta e inclusão concorrente e os quatro endpoints REST, com validações e erros em `ProblemDetail`. A validação integrada do backend está concluída, com execução separada de testes unitários e de integração. O frontend Angular já possui consulta automática de movimentos, grid de sete colunas, paginação e estados de carregamento, erro e ausência de registros. O formulário interativo carrega produtos/COSIFs, valida os campos e integra a inclusão ao backend, com bloqueio durante o envio, recuperação em falha e atualização do grid após sucesso. A validação unitária do frontend está concluída, com 28 testes aprovados em três arquivos. A validação final está concluída, incluindo conferência manual da aplicação em execução e persistência após reinício.

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
            |       |-- manual-movement-form.validators.ts
            |       |-- manual-movement-form.validators.spec.ts
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

Na validação final da T013, em 16/10/2026, o Codex executou `.\mvnw.cmd clean verify`, com `BUILD SUCCESS` e código de saída zero. O log e os XMLs Surefire/Failsafe fornecidos foram conferidos e confirmam os resultados abaixo, sem sobreposição entre os grupos. Os relatórios unitários e de integração registram a JVM em `America/Sao_Paulo`. Testes e build não foram executados novamente nesta atualização documental.

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

O consolidado corresponde à união das linhas cobertas e das duas sessões registradas nos relatórios por grupo, com o mesmo conjunto de classes e as exclusões de configuração e inicialização. A exigência de 100% de linhas aplica-se ao consolidado; o log Maven da T013 confirma a aprovação da verificação configurada.

Na validação temporal da T007, o Codex reportou `clean verify` aprovado com a JVM em `UTC` e em `America/Sao_Paulo`, incluindo as regressões SQL/JPA/projeção/REST. A T008 preservou o código de produção e complementou os testes REST existentes. A validação integrada do backend está concluída. A consulta visual, o fluxo de inclusão e a validação unitária do frontend também estão concluídos. A T013 reconfirmou os resultados automatizados e concluiu a validação com a aplicação iniciada e banco persistente, conforme o registro de validação final abaixo.

Na execução da T013, o Flyway avisou que o H2 `2.4.240` é mais recente que a versão verificada, `2.3.232`. A validação das migrações e os testes passaram, mantendo-se as versões gerenciadas pelo projeto.

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

Use terminais separados para backend e frontend, mantendo os processos em primeiro plano e encerrando-os com `Ctrl+C` ao terminar. Uma instância já ativa ocupa sua porta: confira os processos associados a `8080` e `4200` antes de iniciar outras instâncias. No Windows, encerre também a instância Java que usa o JAR antes de executar `clean verify`, para liberar o arquivo de `target` para exclusão.

### Ambientes

`src/environments/environment.ts` e `src/environments/environment.production.ts` definem `apiBaseUrl: '/api/v1'`. O build de produção substitui o arquivo de desenvolvimento pelo de produção. O proxy local se aplica ao servidor de desenvolvimento.

### Consulta visual implementada

`ManualMovementsPageComponent`, em `src/app/features/manual-movements/`, apresenta o formulário e o grid com Mês, Ano, Código do Produto, Descrição do Produto, Nr Lançamento, Descrição do Movimento e Valor, nessa ordem. No desktop, Mês/Ano e Produto/Cosif ocupam duas colunas; Valor, Descrição e os botões Limpar, Novo e Incluir ficam na coluna esquerda, com as ações abaixo da descrição. Os estilos também preveem disposição em uma coluna para telas pequenas e rolagem horizontal do grid quando necessária.

`ManualMovementsApiService.listPage` consulta `GET /api/v1/movimentos-manuais?page=0` ao carregar a tela. As próximas consultas usam a página solicitada, mantendo a ordenação recebida da API. O componente consome `content`, `page`, `size`, `totalElements` e `totalPages`; a navegação oferece Primeira, Anterior e Próxima e respeita os limites informados. O serviço e os modelos mantêm códigos, valor e lançamento como textos. No grid, o valor troca o ponto decimal por vírgula por operação textual, preservando sinal e precisão.

O grid indica carregamento, ausência de movimentos e falha de consulta. Em erro, exibe `detail` quando informado pela API ou uma mensagem genérica em pt-BR; Tentar novamente repete somente o GET da página solicitada. O fluxo usa `switchMap` para descartar consultas anteriores e encerra a assinatura ao destruir o componente.

Uma página vazia com total de registros maior que zero recebe mensagem própria, sem intervalo invertido no resumo. Os metadados retornados permanecem intactos; somente o número apresentado no indicador é limitado ao total de páginas. Primeira e Anterior continuam disponíveis quando a página recebida é maior que zero, inclusive com `totalPages=1`; Próxima fica indisponível além do limite.

### Formulário interativo implementado

O formulário usa Reactive Forms e inicia com os textos vazios, Produto/Cosif em `-- Selecione --` com valor nulo e os campos desabilitados. Novo fica disponível; ao acioná-lo, a tela entra no modo de inclusão, habilita os campos e consulta `GET /api/v1/produtos`. Cosif permanece desabilitado até a consulta do produto selecionado terminar. Produtos exibem sua descrição; COSIFs exibem `codigoCosif - (codigoClassificacao)`. A consulta de produtos apresenta carregamento e, em falha, mensagem com nova tentativa.

A seleção de Produto consulta `GET /api/v1/produtos/{codigoProduto}/cosifs`. A troca de produto ou uma nova tentativa limpa as opções e a seleção de Cosif, indica carregamento e desabilita esse campo. `switchMap` cancela consultas obsoletas; o tratamento de falhas dentro do fluxo permite novas trocas e tentativas. Em falha, Cosif permanece vazio e desabilitado, e Tentar novamente consulta somente o produto atual, preservando os demais dados. Em sucesso, as opções ficam disponíveis sem seleção automática. Incluir depende também do estado carregado e de um COSIF presente nas opções recebidas.

Limpar esvazia os textos e retorna ambas as seleções a nulo, mantendo o modo de inclusão. A ação cancela a consulta de COSIFs pendente, limpa suas opções e mensagens e mantém Cosif indisponível até uma nova consulta. O reset publica o valor nulo de Produto para que `distinctUntilChanged` reconheça a limpeza e permita selecionar novamente o mesmo código, tanto após uma consulta concluída quanto durante uma consulta pendente.

Os validadores em `manual-movement-form.validators.ts` exigem mês inteiro de 1 a 12 com até dois dígitos e ano inteiro de 1 a 9999 com até quatro dígitos. Descrição é obrigatória, deve conter ao menos um caractere diferente de espaço e admite até 50 caracteres, incluindo os espaços preservados. Os erros são exibidos junto aos campos, e Incluir permanece indisponível com dados inválidos.

Valor é texto decimal simples, diferente de zero, com sinal negativo opcional, até 16 dígitos inteiros e até duas casas após vírgula ou ponto. Milhar, formatos mistos, notação científica, espaços e símbolos monetários são rejeitados. A formatação ao sair do campo usa vírgula e duas casas, sem milhar; entradas inválidas permanecem disponíveis para correção. A conversão para o contrato da API usa somente operações textuais, como `150,5` → `"150.50"`, `-150,00` → `"-150.00"` e `9999999999999999,99` → `"9999999999999999.99"`, sem ponto flutuante ou arredondamento.

### Fluxo de inclusão implementado

Com os dados válidos, Incluir chama `ManualMovementsApiService.createMovement` e envia `POST /api/v1/movimentos-manuais` com os seis campos contratados: mês/ano como números, códigos e descrição como textos e valor textual com ponto e duas casas. A descrição conserva o texto informado.

Ao iniciar o POST, o componente entra no modo `sending` e desabilita Mês, Ano, Produto, Cosif, Valor, Descrição e os botões Novo, Limpar e Incluir, preservando valores e seleções. As ações também verificam o modo da tela para impedir alterações e novo envio enquanto a requisição estiver pendente. A interface indica a inclusão em andamento.

Em falha do POST, o componente retorna ao modo de inclusão e restaura a edição sem limpar os dados ou as seleções. Cosif respeita o estado de sua consulta, e Incluir é recalculado pela validade dos dados e pela seleção de um COSIF carregado. A interface apresenta `detail` da API ou uma mensagem genérica em pt-BR e associa `errors[].field` e `errors[].message` aos seis campos conhecidos. As mensagens do envio anterior são limpas quando os dados do formulário são alterados.

Após confirmação de sucesso, o formulário fica vazio e desabilitado, Produto/Cosif retornam à seleção nula e a tela volta ao modo de consulta. O reset publica explicitamente o valor nulo de Produto para atualizar `distinctUntilChanged`, permitindo iniciar uma nova inclusão com o mesmo produto. O componente consulta a primeira página do grid; se esse GET falhar, Tentar novamente repete somente essa consulta, sem reenviar o movimento já incluído.

### Testes e build do frontend

Na execução final da T013, os logs fornecidos confirmam `npm test` aprovado com **três arquivos e 28 testes** e `npm run build` concluído, ambos com código de saída zero e sem avisos nas saídas aprovadas. O terminal registrou Node.js `24.21.0` e npm `11.19.0`. Os comandos foram executados em `frontend/movimanu-web`. A instalação por `npm ci` na T012 havia registrado 296 pacotes pelo lockfile.

A T012 complementou somente os dois arquivos de testes de validadores e do componente, preservando as regressões anteriores, acrescentando seis testes do componente e ampliando os casos dos validadores e da restauração após falha do POST. A T013 reaproveitou essa suíte. A suíte não configura relatório persistido próprio de testes; a coleta da T013 preservou as saídas do terminal em arquivos de log. Testes e build não foram executados novamente nesta atualização documental. As tentativas inicialmente bloqueadas por acesso a diretórios foram separadas das execuções aprovadas.

| Arquivo, relativo ao workspace | Testes nos fontes revisados | Cenários |
| --- | --- | --- |
| `src/app/app.spec.ts` | 2 | Criação da aplicação e renderização do título da página de movimentos. |
| `src/app/features/manual-movements/manual-movement-form.validators.spec.ts` | 4 | Limites de mês/ano, descrição com conteúdo e espaços preservados, limite total de tamanho, formatos monetários aceitos/rejeitados, representações de zero, equivalência entre vírgula e ponto e conversão/formatação textual exata. |
| `src/app/features/manual-movements/manual-movements-page.component.spec.ts` | 22 | Consulta e paginação, estados do formulário, produtos/COSIFs e coleções vazias, Limpar, validações e mensagens nos campos, POST com payload exato, bloqueio durante envio, preservação dos dados e erros por campo após falha, reset após sucesso, repetição somente do GET e inclusões consecutivas do mesmo produto. |

Os complementos da T012 verificam:

- Descrição vazia ou formada somente por espaços, mensagem no campo, indicação de invalidade, preservação do texto e bloqueio do POST; aceitação de 50 caracteres e rejeição de 51, contando espaços.
- Formatação monetária sem milhar ao sair do campo, preservação da entrada inválida e envio exato de `9999999999999999,99` como `"9999999999999999.99"`. Os validadores também conferem `1,23` e `1.23` como equivalentes e rejeitam diferentes representações de zero, além dos formatos e limites já cobertos.
- Coleções vazias de produtos e COSIFs com inclusão indisponível, rejeição de COSIF sem seleção ou fora das opções carregadas e troca de produto após falha da consulta, preservando os demais dados e aguardando seleção explícita após o sucesso.
- Restauração das ações após falha confirmada do POST e recálculo de Incluir conforme a validade dos dados: valor zero bloqueia o envio e a correção para valor negativo válido permite continuar.

As regressões reaproveitadas conferem carregamento, erro e nova tentativa de COSIFs do produto atual, cancelamento de consultas obsoletas e limpeza durante a consulta. Limpar permite selecionar novamente o mesmo produto tanto após uma consulta concluída quanto pendente. Duas inclusões consecutivas do mesmo produto verificam formulário vazio e desabilitado após cada sucesso, novo GET de COSIFs no segundo ciclo e habilitação sem seleção automática. Os testes de envio mantêm as verificações de bloqueio dos seis campos e dos três botões, ausência de POST adicional, restauração com valores e seleções preservados e nova tentativa somente do GET após sucesso. A consulta e a paginação continuam cobertas, incluindo metadados e páginas vazias além dos resultados.

A suíte exercita o componente e seus contratos HTTP com requisições simuladas. A revisão dos fontes confirmou as asserções e o total de 28 testes; o log da T013 confirmou a nova execução aprovada. A integração com o backend em execução também foi conferida manualmente, conforme a seção de validação final.

A conferência visual da T009 registrou o layout desktop e o estado vazio da consulta. Na T013, capturas e relatos da interação em `http://localhost:4200` confirmaram a renderização Angular e os fluxos com o backend real, incluindo seleção, validação, limpeza, inclusão, ordenação, paginação e recuperação de falhas.

A saída de build informada fica em `dist/movimanu-web/browser/`, relativa ao workspace, correspondendo a `frontend/movimanu-web/dist/movimanu-web/browser/` a partir da raiz do repositório. Os diretórios `dist/`, `node_modules/` e `.angular/cache/` são ignorados pelo Git.

As tarefas de implementação e validação do frontend estão concluídas. O fechamento da T013 combina as evidências automatizadas, HTTP e manuais registradas a seguir.

Referências técnicas: [configuração do workspace Angular](https://angular.dev/reference/configs/workspace-config), [build](https://angular.dev/cli/build) e [proxy de desenvolvimento](https://angular.dev/tools/cli/serve).

## Validação final — T013

Concluída em **16/10/2026**, considerando o horário de `America/Sao_Paulo`. O fechamento combina execução automatizada pelo Codex, revisão dos logs/XMLs/respostas HTTP e conferência manual realizada pelo responsável pelo projeto no Chrome. **T001 a T013 estão concluídas.** A publicação e a escolha do provedor de cloud continuam fora desta validação local.

### Aplicação iniciada e persistência

O backend iniciou no perfil `local`, porta `8080`, a partir de `backend/movimento-manual-service`, com `H2_URL` absoluta apontando para o banco em arquivo `.t013-evidence/h2/movimanu`, na raiz do projeto. As duas inicializações usaram o mesmo diretório de trabalho e a mesma URL. Esse banco isolado foi usado também na conferência manual; o banco padrão não foi usado nessa verificação.

Os GETs de produtos e dos COSIFs de `0001` retornaram `200`, com dois produtos e dois COSIFs. A consulta inicial de movimentos estava vazia. O POST de `T013-validacao-20261016`, mês 10/ano 2026, produto `0001`, COSIF `10000000001` e valor textual `"321.47"`, retornou `201`. Após reiniciar o backend, o GET retornou `200` com o mesmo movimento. A revisão comparou independentemente as respostas e confirmou os **dez campos idênticos**, incluindo lançamento `"1"`, usuário `TESTE` e data `2026-10-16T21:51:00Z`.

O frontend iniciou por `npm start`, com compilação de desenvolvimento concluída e proxy operacional. O HTML e o proxy responderam `200`; a renderização foi confirmada posteriormente pelas capturas da interface em uso. Para reencontrar os registros dessa conferência, mantenha a mesma `H2_URL`; a URL padrão depende do diretório de trabalho.

### Conferência manual e critérios de aceite

Foram conferidos o bloqueio inicial, Novo, seleções pendentes, troca de Produto, preservação dos demais campos e Limpar. As validações de mês, ano, valor e descrição exibiram mensagens junto aos campos e impediram inclusão inválida. Após sucesso, o formulário ficou vazio e desabilitado, somente Novo ficou habilitado e o grid foi atualizado.

Com sete registros, foram observados seis na primeira página e um na segunda, dezembro/2026 antes de outubro/2026 e janeiro/2026 antes de dezembro/2025. Os lançamentos de janeiro/2026 seguiram 1, 2 e 3 entre os produtos A e B. O limite `9999999999999999,99` foi incluído e exibido sem perda de precisão.

O modo Offline do DevTools simulou falha de consulta de COSIFs e falha de envio, com mensagens, dados preservados e recuperação. A repetição de COSIFs disponibilizou as opções sem seleção automática. Com latência de 10.000 ms, download/upload de 1.000 kbit/s, foram observados os seis campos e os três botões bloqueados, com indicação de inclusão em andamento. A recuperação do envio produziu uma única inclusão bem-sucedida (`201`), seguida de um GET (`200`), com `T013-manual-07`, lançamento 4 de janeiro/2026, valor `10,00` e **oito movimentos** no total. Ao terminar, a limitação de rede foi removida.

Os critérios foram confirmados pelo conjunto de métodos abaixo; os cenários de integração e contrato indicados como automatizados não foram todos repetidos manualmente.

| Critério | Evidência de atendimento |
| --- | --- |
| CA-01 — Consulta | Tela renderizada, sete colunas e ordem entre anos conferidas manualmente; testes de componente e consulta aprovados. |
| CA-02 — Paginação | Navegação manual com seis itens e um item; testes de componente e integração cobrem metadados e páginas além dos resultados. |
| CA-03 — Seleções | Produto/COSIF, rótulos, ausência de seleção automática e troca conferidos na tela; rejeição de par sem vínculo coberta pelas suítes. |
| CA-04 — Numeração | Primeiro lançamento, lacunas, períodos e concorrência cobertos por testes; sequência entre produtos também observada manualmente. |
| CA-05 — Persistência | POST, dez campos iguais após reinício e atualização da consulta confirmados por HTTP; reset e bloqueio após sucesso conferidos na tela. |
| CA-06 — Validação | Rejeições automatizadas e mensagens/bloqueio manual para mês 0/13, ano 0/não inteiro, valor inválido e descrição sem conteúdo. |
| CA-07 — Limites e sinal | Meses 1/12 e valores positivos/negativos incluídos; meses 0/13 e zero rejeitados na interface, complementando os testes de limites. |
| CA-08 — Limpeza | Campos e seleções limpos manualmente, mantendo inclusão e Cosif aguardando Produto; regressões de limpeza aprovadas. |
| CA-09 — Envio | Falha de rede, preservação e bloqueio durante POST conferidos manualmente; falha confirmada da API e restauração também cobertas por testes. |
| CA-10 — COSIFs | Erro, nova tentativa e recuperação sem seleção automática conferidos manualmente; carregamento e descarte de consultas obsoletas cobertos por testes. |
| CA-11 — REST | Suítes de integração conferem DTOs, erros, página inválida e além dos resultados; GETs/POST e proxy também verificados com a aplicação iniciada. |
| CA-12 — Valor | Formatos e payload textual exato cobertos por testes; sinal, zero, formato inválido, conversão ponto/vírgula e valor máximo conferidos na interface. |
| CA-13 — Descrição | Rejeição de vazio/espaços conferida manualmente; preservação de espaços válidos e limite total de 50 caracteres confirmados pelas suítes frontend/backend. |

### Evidências recebidas e caminhos

A coleta foi fornecida em `t013-evidence.zip` e `t013-evidence-complement.zip`; capturas e relatos da conferência manual foram recebidos separadamente. O `RESUMO.md` do complemento foi produzido antes dessa conferência manual e conserva o status daquela coleta. O fechamento acima incorpora as evidências manuais posteriores.

Os relatórios XML mantêm os caminhos de `backend/movimento-manual-service/target/` já indicados na seção do backend. Os seguintes arquivos de coleta são relativos à raiz do projeto:

| Evidência | Arquivo |
| --- | --- |
| Execução Maven | `.t013-evidence/maven-clean-verify.log` |
| Testes Angular | `.t013-evidence/frontend-npm-test-escalated.log` |
| Build de produção | `.t013-evidence/frontend-npm-build-escalated.log` |
| Configuração e reinício do backend | `.t013-evidence/backend-run-config.txt` e `.t013-evidence/backend-after-restart.stdout.log` |
| POST e comparação após reinício | `.t013-evidence/http-post-response.json` e `.t013-evidence/http-persistence-comparison.json` |
| Servidor de desenvolvimento aprovado | `.t013-evidence/frontend-dev-server-complement-retry.stdout.log` |

No ZIP complementar, logs e respostas foram agrupados em `evidence/backend/` e `evidence/frontend/`; os XMLs preservam a estrutura do módulo. As tentativas anteriores com falha de compilação por acesso a diretórios estão separadas em `evidence/frontend/previous-attempts/`. As capturas de paginação, falha de COSIF, falha de envio, POST pendente e estado final documentam a etapa manual.

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

A equivalência temporal descrita corresponde ao comportamento implementado no adaptador e aos cenários de teste acima; o domínio SQL sobre `TIMESTAMP(0)` continua sendo apenas a representação do tipo no H2. A execução da T013 reconfirmou os quatro testes unitários do conversor e os testes de persistência, consulta e REST. Os XMLs aprovados e a conferência das evidências confirmam arredondamento ao minuto, faixa de datas, virada de dia/ano e leitura em UTC; a preservação do movimento após reinício foi verificada separadamente no H2 persistente.

Referências técnicas: [TIMESTAMP no H2](https://h2database.com/html/datatypes.html) e [SMALLDATETIME no SQL Server](https://learn.microsoft.com/en-us/sql/t-sql/data-types/smalldatetime-transact-sql).
