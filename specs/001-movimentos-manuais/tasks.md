# Tarefas de implementação — Movimentos Manuais

Referências: `AGENTS.md`, `constitution.md`, `README.md`, `specs/001-movimentos-manuais/spec.md` e `specs/001-movimentos-manuais/plan.md`.

Referência do exercício: `document/references/enunciado-movimentos-manuais.md` (requisitos e modelo relacional) e `document/references/enunciado-movimentos-manuais.pdf` (apoio visual à disposição da tela).

Executar na ordem abaixo, dentro do escopo autorizado. Incluir em cada tarefa testes unitários de sucesso, erro e limites aplicáveis às regras e aos casos de uso, com asserções sobre resultados e efeitos esperados. Manter evidências distinguíveis de testes unitários e de integração e marcar a conclusão somente após as validações aplicáveis.

Os vínculos RF, RN e CA remetem a `specs/001-movimentos-manuais/spec.md`; os intervalos são inclusivos. Cada vínculo identifica a contribuição da tarefa dentro de seu escopo. Critérios compartilhados são atendidos pelo conjunto das tarefas envolvidas. T001 e T002 são de preparação técnica e indicam sua base de governança e planejamento.

## 1. Preparação

- [x] **T001 — Inicializar o backend.** Criar a aplicação Spring Boot e habilitar o JAR executável. Ajustar os perfis local e de testes conforme o plano; configurar Surefire para `*Test` e Failsafe para `*IT`, sem sobreposição. Separar os dados JaCoCo dos grupos e gerar relatórios unitário, de integração e consolidado, conforme os caminhos do plano. Verificar 100% de linhas no consolidado em `verify`, preservando as exclusões existentes.

  **Base técnica:** `AGENTS.md`, `constitution.md` e `specs/001-movimentos-manuais/plan.md`, seções 1, 6 e 7.
- [x] **T002 — Inicializar o frontend.** Criar o workspace Angular nas pastas existentes, com as tecnologias definidas no plano e lockfile. Configurar componentes standalone, Reactive Forms, HttpClient, URL da API por ambiente, proxy local e execução dos testes.

  **Base técnica:** `AGENTS.md`, `constitution.md` e `specs/001-movimentos-manuais/plan.md`, seções 1, 2, 5 e 7.

## 2. Backend

- [x] **T003 — Implementar domínio e portas.** Criar modelos com campos diretos, identificadores compostos sem aninhamento e contratos de consulta e inclusão, incluindo a projeção de movimentos com descrição do produto. Aplicar as validações de negócio previstas na especificação nos modelos, com compartilhamento interno das regras repetidas, preservando precisão decimal e independência de Spring, JPA e HTTP, conforme a seção 2.1 do plano.

  **Vínculos:** `RF-INC-05`, `RF-INC-06`, `RF-INC-11`, `RF-INC-12`; `CA-03`, `CA-06`, `CA-07`, `CA-12`, `CA-13`.
- [x] **T004 — Implementar persistência.** Criar entidades JPA, chaves compostas e mapeadores. Adicionar a restrição única por período/lançamento em nova migração, preservando a existente. Implementar a equivalência temporal de `SMALLDATETIME`; preparar dados demonstrativos exclusivos do ambiente local e fixtures isoladas para testes.

  **Vínculos:** `RF-CON-02`; `RF-INC-05`, `RF-INC-06`, `RF-INC-11`, `RF-INC-12`; `RN-02`, `RN-03`, `RN-05`; `CA-04`, `CA-05`, `CA-06`, `CA-12`, `CA-13`.
- [x] **T005 — Implementar consultas.** Criar casos de uso e adaptadores para produtos, COSIFs por produto e movimentos. Usar projeção com descrição do produto, paginação de seis registros e ordenação determinística conforme o plano, sem tipos Spring nas portas. Preservar a página solicitada e os totais reais quando não houver conteúdo, inclusive em páginas além dos resultados com deslocamento superior a `Integer.MAX_VALUE`, conforme a seção 4.3 do plano.

  **Vínculos:** `RF-CON-02` a `RF-CON-05`; `RF-INC-05`; `RF-API-01`; `CA-01`, `CA-02`, `CA-03`, `CA-11`.
- [x] **T006 — Implementar inclusão concorrente.** Validar o vínculo produto/COSIF, calcular o próximo lançamento e verificar seu limite numérico. Gerar data/hora com `Clock` em UTC e usuário `TESTE`. Garantir gravação atômica e inserção sem sobrescrita por `persist`/`flush`, com transações `REQUIRES_NEW` e até três tentativas no total. Repetir somente colisões de período/lançamento, incluindo a chave completa de movimentos reportada pelo H2 conforme a seção 3 do plano. Cobrir deterministicamente a disputa pelo mesmo máximo no mesmo produto/COSIF, em períodos vazio e preenchido, preservando os dados dos dois movimentos.

  **Vínculos:** `RF-INC-05`, `RF-INC-06`; `RN-01` a `RN-05`; `CA-03`, `CA-04`, `CA-05`, `CA-06`.
- [ ] **T007 — Expor os contratos REST.** Implementar os quatro endpoints e os campos e tipos exatos dos DTOs, da paginação e dos erros definidos no plano, conforme RF-API-01. Aplicar Bean Validation, rejeitar tipos incompatíveis sem coerção e usar `ProblemDetail` em pt-BR. Validar `descricao` conforme RF-INC-12, rejeitando texto vazio ou formado somente por espaços com `400` associado a `descricao`; preservar os espaços de textos válidos e o limite de 50 caracteres. Transportar valor e lançamento como strings; validar o texto completo de `valor` com ponto decimal, sinal e limites antes de converter para `BigDecimal` ou normalizar, rejeitando os formatos inválidos com `400` associado a `valor`. Aplicar os retornos `200`, `201`, `400`, `404`, `409` e `500` previstos.

  **Vínculos:** `RF-CON-02` a `RF-CON-05`; `RF-INC-05`, `RF-INC-06`, `RF-INC-11`, `RF-INC-12`; `RF-API-01`; `CA-02`, `CA-03`, `CA-05`, `CA-06`, `CA-07`, `CA-11`, `CA-12`, `CA-13`.
- [ ] **T008 — Validar a integração do backend.** Adicionar testes REST/JPA/Flyway para RF-API-01, DTOs, erros, validações, precisão, paginação, equivalência temporal e rollback. Conferir coleções vazias, produto inexistente, par inválido, página inválida e página além dos resultados com totais reais. Testar descrição vazia ou formada somente por espaços, erro associado a `descricao` e ausência de gravação; conferir preservação do texto válido e limite de 50 caracteres incluindo espaços. Para `valor`, testar os formatos do contrato, incluindo rejeição de vírgula no JSON, milhar, formatos mistos, notação científica, zero e limites excedidos; verificar sinal, precisão no limite de `NUMERIC(18,2)` e ausência de gravação nas rejeições. Verificar numeração com lacunas, períodos distintos e inclusões concorrentes no mesmo período vazio ou preenchido, inclusive entre produtos diferentes; testar também o esgotamento das tentativas.

  **Vínculos:** `RF-CON-02` a `RF-CON-05`; `RF-INC-05`, `RF-INC-06`, `RF-INC-11`, `RF-INC-12`; `RF-API-01`; `RN-01` a `RN-05`; `CA-01` a `CA-07`, `CA-11` a `CA-13`.

## 3. Frontend

- [ ] **T009 — Implementar a consulta visual.** Reproduzir o layout de referência e o grid com as sete colunas, carregamento inicial, paginação e estados de carregamento, erro e ausência de registros. Consumir os campos dos DTOs e os metadados de paginação definidos no plano.

  **Vínculos:** `RF-CON-01` a `RF-CON-05`; `RF-API-01`; `CA-01`, `CA-02`, `CA-11`.
- [ ] **T010 — Implementar o formulário.** Aplicar os estados e ações Novo, Limpar e Incluir conforme a especificação. Implementar validadores, seleções nulas com `-- Selecione --` e dependência Produto/COSIF com `switchMap`. Para Descrição, cumprir RF-INC-12: rejeitar texto vazio ou formado somente por espaços, informar erro e impedir inclusão, preservando o texto informado e o limite de 50 caracteres. Durante o carregamento ou após erro, manter Cosif sem seleção e desabilitado e impedir inclusão. Exibir carregamento e, em falha, erro e Tentar novamente para consultar apenas os COSIFs do produto atual, preservando os demais dados. Manter o fluxo operacional após falhas e descartar consultas obsoletas, inclusive ao limpar o formulário. Validar o texto de Valor conforme RF-INC-11, aceitando vírgula ou ponto decimal e rejeitando milhar, formatos mistos, notação científica, zero e limites excedidos antes de normalizar. Informar erro no campo e preservar o texto inválido para correção. Converter entradas válidas para strings JSON com ponto e duas casas e formatar o campo em pt-BR sem milhar, preservando sinal e precisão sem ponto flutuante ou arredondamento; preservar lançamento como texto.

  **Vínculos:** `RF-INC-01`, `RF-INC-02`, `RF-INC-04`, `RF-INC-05`, `RF-INC-07`, `RF-INC-08`, `RF-INC-10`, `RF-INC-11`, `RF-INC-12`; `CA-03`, `CA-06`, `CA-07`, `CA-08`, `CA-10`, `CA-12`, `CA-13`.
- [ ] **T011 — Integrar o fluxo de inclusão.** Enquanto o POST estiver pendente, bloquear todos os campos e Novo, Limpar e Incluir, impedindo novos envios. Em falha confirmada, restaurar o modo de inclusão com valores e seleções preservados e Incluir condicionado à validade dos dados. Após sucesso, limpar e desabilitar os campos e consultar a primeira página. Se essa consulta falhar, permitir repetir somente o GET, sem reenviar o movimento persistido.

  **Vínculos:** `RF-CON-01`; `RF-INC-02`, `RF-INC-03`, `RF-INC-04`, `RF-INC-09`; `RF-API-01`; `CA-05`, `CA-06`, `CA-09`, `CA-11`.
- [ ] **T012 — Validar o frontend.** Cobrir com testes unitários os cenários de sucesso, erro e limites dos comportamentos de CA-01 a CA-13 aplicáveis à interface, incluindo descrição vazia ou formada somente por espaços, bloqueio da inclusão, mensagem no campo, preservação do texto e limite de 50 caracteres. Incluir os exemplos monetários aceitos e rejeitados, sinal, zero e limites de dígitos e casas. Verificar preservação exata de `9999999999999999,99`, conversão para o JSON com ponto e duas casas, formatação do campo sem milhar e preservação da entrada inválida. Cobrir paginação, transições do formulário, bloqueio dos campos e ações durante o POST, restauração da edição com valores e seleções preservados em falha confirmada e falhas de envio ou reconsulta. Testar carregamento, erro e nova tentativa de COSIFs para o produto atual, preservação dos demais dados, sucesso sem seleção automática, troca de produto após falha, respostas fora de ordem e limpeza durante a consulta. Conferir consumo de coleções vazias, metadados de paginação e mensagens de erros por campo no formato contratado.

  **Vínculos:** `RF-CON-01` a `RF-CON-05`; `RF-INC-01` a `RF-INC-05`, `RF-INC-07` a `RF-INC-12`; `RF-API-01`; `CA-01` a `CA-03`, `CA-05` a `CA-13`.

## 4. Validação final

- [ ] **T013 — Validar a aplicação e registrar a execução.** Executar `./mvnw clean verify` ou o equivalente Windows, testes Angular sem observação e build de produção. Conferir os resultados de execução e os relatórios de cobertura separados de `*Test` e `*IT`, o consolidado com 100% de linhas e os cenários unitários de sucesso, erro e limites das regras. Confirmar CA-01 a CA-13 com a aplicação iniciada e verificar a persistência após reinício. Atualizar o README com os comandos confirmados, as versões do frontend, o resumo dos testes por grupo e os caminhos das evidências. Distinguir no README a representação SQL de `SMALLDATETIME` por domínio H2 sobre `TIMESTAMP(0)` da equivalência comportamental de `DAT_MOVIMENTO`. Registrar os complementos efetivamente implementados para arredondamento ao minuto e faixa de datas, com as evidências dos testes de arredondamento e limites, incluindo virada de dia; declarar equivalência somente após confirmar esses resultados.

  **Vínculos:** `RF-CON-01` a `RF-CON-05`; `RF-INC-01` a `RF-INC-12`; `RF-API-01`; `RN-01` a `RN-05`; `CA-01` a `CA-13`.
