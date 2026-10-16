# Plano técnico — Movimentos Manuais

Base: `spec.md` desta funcionalidade, `constitution.md` e `AGENTS.md`. Destino: `specs/001-movimentos-manuais/plan.md`.

Referência do exercício: `document/references/enunciado-movimentos-manuais.md` (requisitos e modelo relacional) e `document/references/enunciado-movimentos-manuais.pdf` (apoio visual à disposição da tela).

## 1. Tecnologias e inicialização

- Preservar Java 21, Spring Boot 4.1.1, Spring Cloud 2025.1.3 e Maven Wrapper 3.9.16. Criar a inicialização do serviço e habilitar o empacotamento executável.
- Inicializar o frontend com Angular e CLI 22, Node.js 24 (mínimo 24.15.0), TypeScript 6.0.x e RxJS 7. Fixar as versões resolvidas no projeto e versionar o lockfile.
- Manter H2 em arquivo no perfil `local`; usar H2 isolado em memória nos testes. Flyway controla o esquema; Hibernate apenas o valida.

## 2. Organização e responsabilidades

- Implementar a funcionalidade no único contexto `movimento-manual-service`, incluindo a consulta de produtos/COSIFs usados como referência.
- Em `domain`, manter modelos, identificadores e regras sem dependências de Spring, JPA ou HTTP. Em `application/port` e `application/usecase`, definir contratos e casos de uso de consulta e inclusão.
- Em `infrastructure`, implementar persistência JPA, mapeamento domínio/entidades, transações e configuração. Em `interfaces/rest`, concentrar controllers, DTOs, validação de entrada e tratamento de erros. Tipos Spring, inclusive paginação, não atravessam as portas.
- No frontend, usar componentes standalone, Reactive Forms e serviços com HttpClient em `features/manual-movements`, respeitando as pastas existentes.

### 2.1. Modelagem do domínio e contratos

- Usar campos diretos: `String` para códigos e descrições, `int` para mês e ano, `Long` para lançamento, `BigDecimal` para valor e `Instant` para data/hora. Não criar tipos apenas para encapsular campos individuais.
- Manter os modelos `Product`, `ProductCosif`, `ManualMovement` e `MovementDraft`. Os identificadores `ProductCosifId` e `ManualMovementId` têm componentes diretos, sem identificadores aninhados; `MovementDraft` recebe diretamente os seis dados de inclusão.
- Validar nos construtores dos modelos; compartilhar as regras repetidas no auxiliar interno `ModelValidation`, sem dependências de frameworks. Códigos de produto e COSIF são obrigatórios, com comprimentos máximos de quatro e onze caracteres, sem exigir comprimento exato, converter para número ou completar códigos. Preservar zeros à esquerda e os espaços de descrições válidas.
- No domínio, rejeitar valor nulo, zero, escala original maior que dois ou valor absoluto acima de `9999999999999999.99`. Após essas verificações, normalizar para duas casas sem arredondamento ou ponto flutuante. A validação textual de entrada permanece na fronteira REST, conforme a seção 4.2.
- Manter `ProductQueryPort`, `ProductCosifQueryPort`, `ManualMovementQueryPort` e `ManualMovementInclusionPort`. Receber códigos como `String`, mês/ano e página como `int`; a consulta do maior lançamento retorna `Optional<Long>` e a inclusão recebe `ManualMovement`.
- A consulta paginada retorna `MovementPage<ManualMovementProjection>`. Definir a projeção em `application/projection`, com os dados necessários à resposta de movimentos, inclusive a descrição do produto, usando tipos Java sem formatação REST. Manter `MovementPage<T>` com tamanho fixo de seis e os metadados contratados, sem criar um tipo específico para envolver apenas o número da página.

### 2.2. Consultas

- Implementar `ListProductsUseCase`, `ListProductCosifsUseCase` e `FindManualMovementsUseCase` como classes de aplicação independentes de frameworks. Conectá-las às portas por `QueryUseCaseConfiguration` na infraestrutura.
- Na consulta de COSIFs, verificar a existência do produto pela porta própria antes de consultar os vínculos. Produto inexistente gera `ProductNotFoundException` na aplicação; produto existente sem COSIFs retorna lista vazia. O tratamento HTTP fica na interface REST.
- Implementar os três adaptadores JPA de consulta em `infrastructure/persistence/adapter`, com repositórios Spring Data em `repository` e transações de leitura na infraestrutura. Listar produtos por código crescente e COSIFs do produto por código COSIF crescente.
- Consultar `ManualMovementEntity` por JPQL com `join fetch` dos relacionamentos para produto/COSIF e produto. No adaptador, usar `ManualMovementMapper.toProjection` para construir `ManualMovementProjection`, incluindo a descrição do produto sem consultas adicionais por linha e preservando a conversão temporal do atributo da entidade. Aplicar a ordenação da seção 4.1 e a paginação da seção 4.3. Consultar o maior lançamento por mês e ano, independentemente de produto/COSIF; manter as portas e a projeção independentes de frameworks.

## 3. Persistência e inclusão concorrente

- Mapear as chaves compostas de `PRODUTO_COSIF` e `MOVIMENTO_MANUAL` com `@EmbeddedId`, preservando os relacionamentos do modelo. Tratar códigos como texto, valores com `BigDecimal` e o número do lançamento com `Long`.
- Preservar `V1__create_manual_movement_schema.sql` e adicionar, em `V2__unique_period_launch_number.sql`, a restrição `UK_MOVIMENTO_MANUAL_PERIODO_LANCAMENTO` sobre `(DAT_ANO, DAT_MES, NUM_LANCAMENTO)`, complementar à chave primária existente. A unicidade é independente do produto/COSIF e fornece a restrição de banco necessária a RN-02, cujo fluxo concorrente é implementado na inclusão.
- Implementar `IncludeManualMovementUseCase` sem dependências de frameworks. Validar o vínculo por `ProductCosifQueryPort.existsById`; vínculo inexistente gera `ProductCosifNotFoundException`. Consultar o maior lançamento somente por mês/ano, iniciar em um quando ausente e verificar o limite `999999999999999999` antes do incremento.
- Definir `TransactionExecutionPort` com operação Java `Supplier<T>`, implementada por `SpringTransactionExecutionAdapter` com `TransactionTemplate` e `REQUIRES_NEW`. Cada tentativa executa validação do vínculo, nova leitura do máximo e gravação em uma transação própria; manter a repetição fora dessa fronteira. Conectar o caso de uso às portas por `InclusionUseCaseConfiguration`.
- No `JpaManualMovementInclusionAdapter`, executar somente inserção por `EntityManager.persist` seguido de `flush`, sem usar `merge` ou gravação que possa atualizar uma entidade existente. A colisão deve ser detectada antes de retornar da tentativa; desfazer a transação inteira antes de repetir.
- Identificar a colisão por `UK_MOVIMENTO_MANUAL_PERIODO_LANCAMENTO` ou seu índice H2. No mesmo produto/COSIF, reconhecer também o nome que o H2 reporta para a chave completa: `PK_MOVIMENTO_MANUAL INDEX PRIMARY_KEY_<n>`, com os prefixos de esquema aplicáveis. A chave completa duplicada também viola a unicidade de período/lançamento. Restringir essa equivalência à chave de movimentos; não repetir outras violações de integridade ou falhas de persistência.
- Traduzir somente essas colisões para `ManualMovementSequenceCollisionException`; o caso de uso repete com nova transação e nova leitura, até três tentativas no total. Esgotamento ou limite numérico gera `ManualMovementSequenceConflictException`, cujo tratamento HTTP de conflito pertence à interface REST. O controle fica no banco e funciona também em períodos ainda vazios.
- Preservar regressões determinísticas com duas inclusões do mesmo período e produto/COSIF, em períodos vazio e preenchido. Fazer ambas lerem o mesmo máximo e aguardar o commit da primeira antes da segunda gravação; verificar lançamentos distintos, preservação das descrições/valores, colisão, rollback e nova leitura. Manter também os cenários concorrentes entre produtos diferentes.
- Gerar os dados automáticos no backend; configurar `Clock.systemUTC()` na infraestrutura, obter o instante da gravação pelo relógio injetado e preencher o usuário fixo `TESTE`. Na infraestrutura, usar `SmallDateTimeConverter` para converter `Instant` e `LocalDateTime` em UTC, normalizar ao minuto e validar a faixa de `1900-01-01` a `2079-06-06` antes e depois do arredondamento. Arredondar para baixo os segundos inferiores a `29.999` e para cima a partir desse limiar, mantendo segundos em `00`; rejeitar resultados acima de `2079-06-06T23:59:00Z`. Aplicar o conversor ao atributo JPA e a normalização no mapeador de movimentos. O domínio H2 sobre `TIMESTAMP(0)` representa o tipo SQL; a equivalência comportamental depende desse complemento e dos testes de limiar, faixa, UTC e virada de dia.
- Manter `movementAt` como `Instant` na entidade JPA, com `SmallDateTimeConverter` aplicado e conversão explícita UTC para a coluna sem fuso. Preservar a ausência da configuração global `hibernate.jdbc.time_zone=UTC` neste mapeamento, sem compensação fixa de horas. Na leitura, construir a projeção a partir da entidade convertida e formatar o instante em UTC na REST. Preservar a regressão em que o literal SQL `2026-01-01 12:00:00` corresponde a `2026-01-01T12:00:00Z` na entidade, na projeção e na REST, com validação nas JVMs `UTC` e `America/Sao_Paulo`. Configurar a formatação SQL em `spring.jpa.properties.hibernate.format_sql`.
- Carregar dados demonstrativos de produtos/COSIFs somente no perfil `local`, pela inicialização SQL configurada em `application-local.yml` com `classpath:db/local/demo-data.sql`. Inserir apenas registros ausentes, preservando a criação/evolução do esquema pelo Flyway. Usar fixtures próprias em `src/test/resources/fixtures/persistence-fixtures.sql` e banco em memória separado no teste da carga local.

## 4. Contrato REST

Prefixo: `/api/v1`. Sucessos usam `application/json`; erros usam `application/problem+json`. Usar DTOs próprios, sem expor entidades JPA.

### 4.1. Recursos e respostas

| Método e recurso | Resposta |
| --- | --- |
| `GET /produtos` | `200` com `ProdutoResponse[]`; sem produtos, retornar `[]`. |
| `GET /produtos/{codigoProduto}/cosifs` | `200` com `CosifResponse[]`; produto existente sem COSIFs retorna `[]`; produto inexistente retorna `404`. |
| `GET /movimentos-manuais?page=0` | `200` com `PaginaMovimentosResponse`, inclusive quando vazia ou além dos resultados; `page` inválido retorna `400`. |
| `POST /movimentos-manuais` | Recebe `IncluirMovimentoRequest`; após persistência bem-sucedida, retorna `201` com `MovimentoResponse`. Par inválido, inclusive produto inexistente, retorna `400`. |

- Verificar a existência em `PRODUTO` separadamente da consulta de COSIFs: lista vazia de vínculos não comprova inexistência do produto.
- Consultar o grid por projeção com a descrição do produto. Ordenar por ano, mês e lançamento decrescentes, seguidos de produto e COSIF crescentes, garantindo desempate determinístico.
- Conflito de numeração, incluindo limite da sequência ou esgotamento das três tentativas por colisão, retorna `409`. Falha inesperada de infraestrutura ou persistência retorna `500`.

### 4.2. DTOs e formatos

Os nomes abaixo são exatos e usam `camelCase`. Inteiros são números JSON; códigos, descrição, valor e lançamento são textos. Todos os campos de entrada são obrigatórios e não nulos.

| DTO | Campos JSON |
| --- | --- |
| `ProdutoResponse` | `codigoProduto: string`, `descricaoProduto: string ou null`. |
| `CosifResponse` | `codigoCosif: string`, `codigoClassificacao: string ou null`. O produto é identificado no caminho do GET. |
| `IncluirMovimentoRequest` | `mes: inteiro`, `ano: inteiro`, `codigoProduto: string`, `codigoCosif: string`, `descricao: string`, `valor: string`. |
| `MovimentoResponse` | Os seis campos de `IncluirMovimentoRequest`, acrescidos de `descricaoProduto: string ou null`, `numeroLancamento: string`, `dataMovimento: string` e `codigoUsuario: string`. Usado tanto no POST quanto nos itens da consulta. |
| `PaginaMovimentosResponse` | `content: MovimentoResponse[]`, `page: inteiro`, `size: inteiro`, `totalElements: inteiro`, `totalPages: inteiro`. |

- Preservar códigos como texto, inclusive zeros à esquerda. Nas respostas, manter todos os campos declarados, usando `null` somente nas descrições/classificações que aceitam nulo no esquema; listas vazias são `[]`, nunca `null`.
- No JSON de entrada, `valor` usa somente ponto decimal: sinal negativo opcional no início, de um a 16 dígitos inteiros e, opcionalmente, ponto seguido de uma ou duas casas. Aceitar, por exemplo, `"150"`, `"150.5"` e `"-150.00"`. A aceitação de vírgula em RF-INC-11 é exclusiva do formulário, que a converte antes do envio. Rejeitar vírgula, separadores de milhar, formatos mistos, notação científica, espaços e símbolos monetários com `400` associado a `valor`. Validar o texto completo, o limite de dígitos e o valor diferente de zero antes de converter para `BigDecimal` ou normalizar; não arredondar, remover caracteres inválidos nem usar ponto flutuante. Na saída, usar ponto e sempre duas casas, como `"150.50"`.
- `numeroLancamento` é texto decimal sem casas, entre `"1"` e `"999999999999999999"`. `dataMovimento` usa ISO 8601 em UTC, com segundos `00` após a equivalência temporal prevista, por exemplo `"2026-10-15T16:54:00Z"`. `codigoUsuario` é `"TESTE"`.
- Aplicar Bean Validation e as regras de RF-INC-06, sem coerção silenciosa de tipos JSON: não converter textos para mês/ano, truncar números decimais ou converter valores/códigos numéricos em texto. JSON malformado, tipos incompatíveis e propriedades não previstas no DTO de entrada retornam `400`; dados automáticos pertencem somente à resposta.
- Validar `descricao` conforme RF-INC-12: rejeitar ausência, nulo, texto vazio, texto formado somente por espaços ou com mais de 50 caracteres. Retornar `400` com `errors[].field` igual a `descricao` e mensagem em pt-BR, sem persistir. Usar a verificação de conteúdo apenas para validar; preservar o texto recebido e contar seus espaços no limite de tamanho.

### 4.3. Paginação

- `page` é o único parâmetro de paginação, com índice inicial zero e padrão `0` quando omitido. Se informado, deve representar um inteiro não negativo; vazio, não inteiro ou negativo retorna `400`, associado ao campo `page`.
- O tamanho da página é fixo: `size=6`, independentemente da quantidade de itens retornada. Não expor parâmetros para alterar tamanho ou ordenação.
- `totalElements` é a quantidade total da consulta e `totalPages` é o arredondamento para cima de `totalElements / 6`; ambos são zero quando não há registros.
- Página além dos resultados retorna `content=[]`, mantendo `page` solicitado, `size=6` e os totais reais, sem redirecionar para outra página.
- No adaptador, calcular o deslocamento com `long` e consultar o total antes de aplicar a paginação JPA. Se o deslocamento for maior ou igual ao total, retornar a página vazia sem executar a busca paginada. Esse comportamento também se aplica a páginas além dos resultados cujo deslocamento exceda `Integer.MAX_VALUE`, sem impor um limite adicional ao parâmetro `page`. Preservar testes unitários e de integração com sete movimentos e páginas `357913942` e `Integer.MAX_VALUE`.

Exemplo: com sete movimentos, solicitar `page=2` retorna `200` com `{"content":[],"page":2,"size":6,"totalElements":7,"totalPages":2}`.

### 4.4. Erros

Usar `ProblemDetail` com os campos abaixo sempre presentes. A extensão `errors` fica no primeiro nível do JSON, sem objeto intermediário `properties`.

| Campo | Tipo e conteúdo |
| --- | --- |
| `type` | String com `"about:blank"`. |
| `title` | Resumo em pt-BR: `Requisição inválida` (`400`), `Recurso não encontrado` (`404`), `Conflito de inclusão` (`409`) ou `Erro interno` (`500`). |
| `status` | Inteiro igual ao status HTTP. |
| `detail` | Explicação em pt-BR; em `500`, usar mensagem genérica, sem detalhes internos de infraestrutura. |
| `instance` | String com o caminho requisitado, incluindo `/api/v1`, sem query string. |
| `errors` | Lista de objetos com `field: string` e `message: string` em pt-BR; usar `[]` quando não houver erro associado a campo ou parâmetro. |

Os valores de `field` correspondem aos nomes de entrada ou parâmetros, como `mes`, `codigoProduto`, `codigoCosif` e `page`. No POST, produto inexistente é associado a `codigoProduto`; ausência do vínculo produto/COSIF é associada a `codigoCosif`. Preservar o campo identificável nos erros de desserialização de `mes`, `ano`, `codigoProduto`, `codigoCosif`, `descricao` e `valor`, incluindo números fora da faixa de `int` em mês/ano. JSON ilegível sem campo identificável e erros globais usam `errors=[]`.

## 5. Interface e comunicação

- Tipar os serviços HTTP com os DTOs contratados; consumir `content`, `page`, `size`, `totalElements` e `totalPages` na consulta. Tratar `200` com coleção vazia como ausência de dados e mapear `errors[].field` e `errors[].message` aos campos correspondentes, exibindo `detail` nos erros gerais.
- Modelar explicitamente os estados de consulta, inclusão e envio. Derivar deles a habilitação do formulário e das ações, conforme RF-INC-01 a RF-INC-04 e RF-INC-08 a RF-INC-10. Combinar o modo da tela com o estado da consulta de COSIFs ao habilitar campos e ações.
- Aplicar ao campo Descrição um validador de conteúdo conforme RF-INC-12, além da obrigatoriedade e do limite de 50 caracteres. Rejeitar texto formado somente por espaços, exibir erro no campo e manter Incluir indisponível. Preservar o texto digitado para correção e envio, sem aparar espaços automaticamente; aplicar a mesma regra de conteúdo e tamanho da API.
- Representar `-- Selecione --` por valor nulo. Na troca de produto ou nova tentativa, limpar a seleção/opções de Cosif, indicar carregamento e desabilitar a combo-box. Usar `switchMap` para ignorar respostas ou erros de consultas anteriores; ao limpar o formulário ou remover o produto, cancelar a consulta anterior e limpar as opções e o erro de Cosif.
- Em falha do GET de COSIFs, encerrar a indicação de carregamento, manter Cosif sem seleção e desabilitado e apresentar mensagem com a ação Tentar novamente. Essa ação consulta somente os COSIFs do produto atual, sem POST nem reset dos demais dados; fica disponível apenas no modo de inclusão, com produto selecionado e sem consulta de COSIFs pendente.
- Tratar as falhas dentro do fluxo da consulta de COSIFs, mantendo operacionais as próximas trocas de produto e novas tentativas. Em sucesso, disponibilizar opções sem seleção automática e habilitar Cosif somente quando o modo da tela permitir. Impedir inclusão durante o carregamento, após erro ou sem seleção válida, mesmo com o controle Cosif desabilitado.
- Manter Valor como texto e validar a entrada completa conforme RF-INC-11, antes de qualquer conversão ou formatação. Tratar vírgula ou ponto como separador decimal, nunca como agrupamento de milhares; `1.234` e `1,234` são inválidos por excederem duas casas. Impedir inclusão com entrada inválida e informar o problema no campo, preservando o texto para correção.
- Para o JSON, substituir a vírgula por ponto somente após validar e completar a fração para duas casas por operações textuais: `150` → `"150.00"`, `150,5` → `"150.50"` e `-150,00` → `"-150.00"`. Preservar sinal e todos os dígitos, inclusive no limite `9999999999999999,99`, sem `Number`, `parseFloat` ou arredondamento. Ao formatar o campo em pt-BR, usar vírgula e duas casas, sem agrupamento de milhares. Preservar também o lançamento como texto.
- Ao iniciar o POST, entrar no estado de envio e desabilitar todos os campos e os botões Novo, Limpar e Incluir, preservando valores e seleções. Manter o bloqueio enquanto o POST estiver pendente e impedir novos envios.
- Em falha confirmada da inclusão, retornar ao estado de inclusão e restaurar a edição e as ações sem limpar valores ou seleções. Recalcular a disponibilidade de Incluir pela validade do formulário.
- Após confirmação de sucesso, aplicar o reset previsto e reconsultar a primeira página. Uma falha nessa reconsulta deve permitir repetir somente o GET, sem reenviar a inclusão já concluída.
- Configurar a URL da API por ambiente e proxy no desenvolvimento local. Reproduzir o layout de referência e apresentar carregamento, erros e ausência de registros.

## 6. Ambientes e Spring Cloud

- Desabilitar o Config Client no perfil `local`; no perfil `cloud`, preservar a configuração externa opcional existente. Circuit breaker será usado quando houver chamadas externas que o justifiquem.
- Manter configurações independentes de OCI/AWS, cuja escolha permanece pendente, sem acrescentar dependência de serviços cloud à execução local.

## 7. Validação e conclusão

- Para cada regra e caso de uso, criar testes unitários de sucesso, erro e limites aplicáveis, com asserções sobre resultados e efeitos esperados. No backend, isolar as dependências por dublês, sem iniciar contexto Spring, banco ou rede. As validações de entrada também devem ter testes unitários próprios; os testes REST complementam essa verificação. Preservar os testes unitários de Bean Validation do DTO, valor textual, desserialização, parsing de página e tratamento de erros. Cobrir `mes` e `ano` acima e abaixo da faixa de `int`, exigindo HTTP `400`, campo correto e ausência de persistência nos testes REST.
- Cobrir CA-01 a CA-13 com testes unitários de domínio/aplicação e do frontend; incluir descrição vazia ou formada somente por espaços, conteúdo válido com espaços preservados e limite de 50 caracteres, além de limites, sinal, zero, precisão decimal e formatos monetários válidos e inválidos. Verificar a conversão exata do texto para o JSON e a formatação do campo sem milhar. Cobrir estados do formulário, bloqueio dos campos e ações durante o POST, restauração da edição com valores e seleções preservados em falha confirmada, carregamento e falha de COSIFs, nova tentativa para o produto atual sem alterar os demais dados e respostas fora de ordem.
- Adicionar testes de integração REST/JPA/Flyway para todos os casos de RF-API-01, nomes e tipos dos DTOs, metadados de páginas vazias ou além dos resultados e formato dos erros. Conferir rejeição de descrição vazia ou formada somente por espaços com `400` associado a `descricao`, sem gravação, e preservação de espaços em descrições válidas dentro do limite. Verificar `valor` textual com ponto, rejeição de vírgula e demais formatos inválidos, preservação de sinal e precisão até o limite de `NUMERIC(18,2)` e ausência de gravação em caso de erro. Incluir concorrência no mesmo período vazio e preenchido, tanto com o mesmo produto/COSIF quanto com produtos distintos; verificar sequência, rollback, paginação e equivalência temporal.
- Separar a execução do backend: Maven Surefire executa somente `*Test` na fase `test`; Maven Failsafe executa somente `*IT` nas fases de integração e verifica seus resultados em `verify`. Confirmar que cada teste pertence a um único grupo e registrar por grupo as quantidades executadas, falhas e ignoradas.
- Configurar o JaCoCo com arquivos de execução distintos para unitários e integração. Gerar relatórios HTML e XML exclusivos de cada grupo e um consolidado pela união dos dados da mesma execução. Preservar as exclusões existentes de configuração e inicialização em todos os relatórios e na verificação do limite.

Os caminhos abaixo são relativos ao módulo `backend/movimento-manual-service`. Cada diretório de relatórios deve conter `index.html` e `jacoco.xml`.

| Grupo | Dados JaCoCo | Diretório de relatórios | Finalidade |
| --- | --- | --- | --- |
| Unitários | `target/jacoco-unit.exec` | `target/site/jacoco-unit` | Evidenciar somente a cobertura dos `*Test`. |
| Integração | `target/jacoco-integration.exec` | `target/site/jacoco-integration` | Evidenciar somente a cobertura dos `*IT`. |
| Consolidado | `target/jacoco.exec` | `target/site/jacoco` | Verificar a exigência de 100% de linhas. |

- Executar a verificação de 100% de linhas em `verify`, após consolidar os dados. A meta aplica-se ao consolidado do backend; os relatórios por grupo demonstram a contribuição de cada suíte. A conclusão exige também os cenários unitários de sucesso, erro e limites das regras. Gerar as evidências em build limpo, sem reutilizar dados de execução anteriores.
- Na implementação, executar `./mvnw clean verify`, testes Angular sem modo de observação e build de produção. Conferir os resultados Surefire/Failsafe, os relatórios unitário, de integração e consolidado e a verificação dos 100% de linhas. Registrar separadamente o resultado dos testes do frontend. Validar a aplicação iniciada com H2 persistente e registrar no README os comandos confirmados, as versões do frontend, o resumo dos testes por grupo e os caminhos das evidências.

Referências técnicas: [compatibilidade Angular](https://angular.dev/reference/versions), [Spring Cloud 2025.1.3](https://spring.io/blog/2026/08/20/spring-cloud-2025-1-3-has-been-released/), [SMALLDATETIME](https://learn.microsoft.com/en-us/sql/t-sql/data-types/smalldatetime-transact-sql) e [ProblemDetail](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html).
