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

Prefixo: `/api/v1`. A API usa DTOs próprios, sem expor entidades JPA.

| Método e recurso | Contrato |
| --- | --- |
| `GET /produtos` | Códigos e descrições para seleção. |
| `GET /produtos/{codigoProduto}/cosifs` | COSIFs do produto, com código e classificação. |
| `GET /movimentos-manuais?page=0` | Página de seis registros, com as informações do grid, página atual, total de registros e total de páginas. |
| `POST /movimentos-manuais` | Recebe mês, ano, produto, COSIF, descrição e valor; responde `201` com o movimento persistido. |

- Consultar o grid por projeção com a descrição do produto. Ordenar por ano, mês e lançamento decrescentes, seguidos de produto e COSIF crescentes, garantindo desempate determinístico.
- Transportar valor monetário e número do lançamento como strings no JSON. O valor usa ponto decimal e duas casas, como `"-150.00"`; validar antes de normalizar, rejeitando excesso de casas sem arredondamento silencioso.
- Aplicar Bean Validation nos DTOs e as regras pertinentes no domínio/caso de uso, conforme RF-INC-06. Padronizar erros com `ProblemDetail`, mensagens em pt-BR e indicação dos campos: `400` para entrada inválida, `404` para recurso consultado inexistente e `409` para conflito de inclusão.

## 5. Interface e comunicação

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

- Cobrir CA-01 a CA-10 com testes unitários de domínio/aplicação e do frontend; incluir limites, precisão decimal, estados do formulário, bloqueio dos campos e ações durante o POST, restauração da edição com valores e seleções preservados em falha confirmada, carregamento e falha de COSIFs, nova tentativa para o produto atual sem alterar os demais dados e respostas fora de ordem.
- Adicionar testes de integração REST/JPA/Flyway, incluindo concorrência no mesmo período vazio e preenchido, com produtos distintos; verificar sequência, rollback, paginação e equivalência temporal.
- Configurar Maven Failsafe para executar `*IT` em `verify`, integrando a cobertura ao JaCoCo e preservando o limite de 100% de linhas e as exclusões existentes.
- Na implementação, executar `./mvnw clean verify`, testes Angular sem modo de observação e build de produção. Validar a aplicação iniciada com H2 persistente e registrar no README os comandos confirmados e as versões do frontend.

Referências técnicas: [compatibilidade Angular](https://angular.dev/reference/versions), [Spring Cloud 2025.1.3](https://spring.io/blog/2026/08/20/spring-cloud-2025-1-3-has-been-released/) e [SMALLDATETIME](https://learn.microsoft.com/en-us/sql/t-sql/data-types/smalldatetime-transact-sql).
