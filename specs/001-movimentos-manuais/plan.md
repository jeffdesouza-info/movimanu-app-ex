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

## 3. Persistência e inclusão concorrente

- Mapear as chaves compostas de `PRODUTO_COSIF` e `MOVIMENTO_MANUAL` com `@EmbeddedId`, preservando os relacionamentos do modelo. Tratar códigos como texto, valores com `BigDecimal` e o número do lançamento com `Long`.
- Em nova migração, adicionar unicidade em `(DAT_ANO, DAT_MES, NUM_LANCAMENTO)`, complementar à chave primária existente. Essa restrição materializa RN-02 inclusive entre produtos diferentes, sem criar outra tabela.
- Cada tentativa de inclusão executa em uma transação: validar o vínculo produto/COSIF, consultar o maior lançamento do período, calcular o próximo e persistir. Verificar o limite de 18 dígitos antes do incremento.
- Em colisão nessa restrição única, desfazer a tentativa inteira e repetir com nova transação e nova leitura, até três tentativas no total. Outros erros não recebem essa repetição; esgotamento retorna conflito. O controle fica no banco e funciona também em períodos ainda vazios.
- Gerar os dados automáticos no backend; obter o instante da gravação por `Clock` em UTC. No adaptador, reproduzir o arredondamento ao minuto e a faixa de datas de `SMALLDATETIME`; o alias atual sobre `TIMESTAMP(0)` exige esse complemento e testes de equivalência.
- Disponibilizar dados demonstrativos de produtos/COSIFs apenas no ambiente local e fixtures próprias nos testes, respeitando o esquema.

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
- Na entrada, `valor` usa ponto decimal e até duas casas, sem separador de milhar ou notação científica; aceitar, por exemplo, `"150"`, `"150.5"` e `"-150.00"`. Na saída, usar sempre duas casas, como `"150.50"`. Validar o limite de 16 dígitos inteiros, a precisão e o valor diferente de zero antes de normalizar, sem arredondamento silencioso nem ponto flutuante.
- `numeroLancamento` é texto decimal sem casas, entre `"1"` e `"999999999999999999"`. `dataMovimento` usa ISO 8601 em UTC, com segundos `00` após a equivalência temporal prevista, por exemplo `"2026-10-15T16:54:00Z"`. `codigoUsuario` é `"TESTE"`.
- Aplicar Bean Validation e as regras de RF-INC-06, sem coerção silenciosa de tipos JSON: não converter textos para mês/ano, truncar números decimais ou converter valores/códigos numéricos em texto. JSON malformado, tipos incompatíveis e propriedades não previstas no DTO de entrada retornam `400`; dados automáticos pertencem somente à resposta.

### 4.3. Paginação

- `page` é o único parâmetro de paginação, com índice inicial zero e padrão `0` quando omitido. Se informado, deve representar um inteiro não negativo; vazio, não inteiro ou negativo retorna `400`, associado ao campo `page`.
- O tamanho da página é fixo: `size=6`, independentemente da quantidade de itens retornada. Não expor parâmetros para alterar tamanho ou ordenação.
- `totalElements` é a quantidade total da consulta e `totalPages` é o arredondamento para cima de `totalElements / 6`; ambos são zero quando não há registros.
- Página além dos resultados retorna `content=[]`, mantendo `page` solicitado, `size=6` e os totais reais, sem redirecionar para outra página.

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

Os valores de `field` correspondem aos nomes de entrada ou parâmetros, como `mes`, `codigoProduto`, `codigoCosif` e `page`. No POST, produto inexistente é associado a `codigoProduto`; ausência do vínculo produto/COSIF é associada a `codigoCosif`. JSON ilegível sem campo identificável e erros globais usam `errors=[]`.

## 5. Interface e comunicação

- Tipar os serviços HTTP com os DTOs contratados; consumir `content`, `page`, `size`, `totalElements` e `totalPages` na consulta. Tratar `200` com coleção vazia como ausência de dados e mapear `errors[].field` e `errors[].message` aos campos correspondentes, exibindo `detail` nos erros gerais.
- Modelar explicitamente os estados de consulta, inclusão e envio. Derivar deles a habilitação do formulário e das ações, conforme RF-INC-01 a RF-INC-04 e RF-INC-08 a RF-INC-10. Combinar o modo da tela com o estado da consulta de COSIFs ao habilitar campos e ações.
- Representar `-- Selecione --` por valor nulo. Na troca de produto ou nova tentativa, limpar a seleção/opções de Cosif, indicar carregamento e desabilitar a combo-box. Usar `switchMap` para ignorar respostas ou erros de consultas anteriores; ao limpar o formulário ou remover o produto, cancelar a consulta anterior e limpar as opções e o erro de Cosif.
- Em falha do GET de COSIFs, encerrar a indicação de carregamento, manter Cosif sem seleção e desabilitado e apresentar mensagem com a ação Tentar novamente. Essa ação consulta somente os COSIFs do produto atual, sem POST nem reset dos demais dados; fica disponível apenas no modo de inclusão, com produto selecionado e sem consulta de COSIFs pendente.
- Tratar as falhas dentro do fluxo da consulta de COSIFs, mantendo operacionais as próximas trocas de produto e novas tentativas. Em sucesso, disponibilizar opções sem seleção automática e habilitar Cosif somente quando o modo da tela permitir. Impedir inclusão durante o carregamento, após erro ou sem seleção válida, mesmo com o controle Cosif desabilitado.
- Manter o valor como texto no formulário; converter vírgula/ponto e formatar em pt-BR sem passar por `Number` ou `parseFloat`. Preservar também o lançamento como texto.
- Ao iniciar o POST, entrar no estado de envio e desabilitar todos os campos e os botões Novo, Limpar e Incluir, preservando valores e seleções. Manter o bloqueio enquanto o POST estiver pendente e impedir novos envios.
- Em falha confirmada da inclusão, retornar ao estado de inclusão e restaurar a edição e as ações sem limpar valores ou seleções. Recalcular a disponibilidade de Incluir pela validade do formulário.
- Após confirmação de sucesso, aplicar o reset previsto e reconsultar a primeira página. Uma falha nessa reconsulta deve permitir repetir somente o GET, sem reenviar a inclusão já concluída.
- Configurar a URL da API por ambiente e proxy no desenvolvimento local. Reproduzir o layout de referência e apresentar carregamento, erros e ausência de registros.

## 6. Ambientes e Spring Cloud

- Desabilitar o Config Client no perfil `local`; no perfil `cloud`, preservar a configuração externa opcional existente. Circuit breaker será usado quando houver chamadas externas que o justifiquem.
- Manter configurações independentes de OCI/AWS, cuja escolha permanece pendente, sem acrescentar dependência de serviços cloud à execução local.

## 7. Validação e conclusão

- Cobrir CA-01 a CA-11 com testes unitários de domínio/aplicação e do frontend; incluir limites, precisão decimal, estados do formulário, bloqueio dos campos e ações durante o POST, restauração da edição com valores e seleções preservados em falha confirmada, carregamento e falha de COSIFs, nova tentativa para o produto atual sem alterar os demais dados e respostas fora de ordem.
- Adicionar testes de integração REST/JPA/Flyway para todos os casos de RF-API-01, nomes e tipos dos DTOs, metadados de páginas vazias ou além dos resultados e formato dos erros. Incluir concorrência no mesmo período vazio e preenchido, com produtos distintos; verificar sequência, rollback, paginação e equivalência temporal.
- Configurar Maven Failsafe para executar `*IT` em `verify`, integrando a cobertura ao JaCoCo e preservando o limite de 100% de linhas e as exclusões existentes.
- Na implementação, executar `./mvnw clean verify`, testes Angular sem modo de observação e build de produção. Validar a aplicação iniciada com H2 persistente e registrar no README os comandos confirmados e as versões do frontend.

Referências técnicas: [compatibilidade Angular](https://angular.dev/reference/versions), [Spring Cloud 2025.1.3](https://spring.io/blog/2026/08/20/spring-cloud-2025-1-3-has-been-released/), [SMALLDATETIME](https://learn.microsoft.com/en-us/sql/t-sql/data-types/smalldatetime-transact-sql) e [ProblemDetail](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html).
