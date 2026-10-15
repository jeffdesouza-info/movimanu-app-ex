# Tarefas de implementação — Movimentos Manuais

Referências: `AGENTS.md`, `constitution.md`, `README.md`, `spec.md` e `plan.md`.

Referência do exercício: `document/references/enunciado-movimentos-manuais.md` (requisitos e modelo relacional) e `document/references/enunciado-movimentos-manuais.pdf` (apoio visual à disposição da tela).

Executar na ordem abaixo, dentro do escopo autorizado. Incluir os testes unitários pertinentes em cada tarefa e marcar sua conclusão somente após as validações aplicáveis.

## 1. Preparação

- [ ] **T001 — Inicializar o backend.** Criar a aplicação Spring Boot e habilitar o JAR executável. Ajustar os perfis local e de testes conforme o plano; configurar Maven Failsafe e sua integração com JaCoCo, preservando o limite de 100% de cobertura de linhas e as exclusões existentes.
- [ ] **T002 — Inicializar o frontend.** Criar o workspace Angular nas pastas existentes, com as tecnologias definidas no plano e lockfile. Configurar componentes standalone, Reactive Forms, HttpClient, URL da API por ambiente, proxy local e execução dos testes.

## 2. Backend

- [ ] **T003 — Implementar domínio e portas.** Criar modelos, identificadores e contratos de consulta e inclusão. Aplicar as validações de negócio previstas na especificação, preservando precisão decimal e independência de Spring, JPA e HTTP.
- [ ] **T004 — Implementar persistência.** Criar entidades JPA, chaves compostas e mapeadores. Adicionar a restrição única por período/lançamento em nova migração, preservando a existente. Implementar a equivalência temporal de `SMALLDATETIME`; preparar dados demonstrativos exclusivos do ambiente local e fixtures isoladas para testes.
- [ ] **T005 — Implementar consultas.** Criar casos de uso e adaptadores para produtos, COSIFs por produto e movimentos. Usar projeção com descrição do produto, paginação de seis registros e ordenação determinística conforme o plano, sem tipos Spring nas portas.
- [ ] **T006 — Implementar inclusão concorrente.** Validar o vínculo produto/COSIF, calcular o próximo lançamento e verificar seu limite numérico. Gerar data/hora com `Clock` em UTC e usuário `TESTE`. Garantir gravação atômica e repetir somente colisões da restrição única prevista, com transações independentes e até três tentativas no total.
- [ ] **T007 — Expor os contratos REST.** Implementar os quatro endpoints do plano, DTOs, Bean Validation e erros com `ProblemDetail` em pt-BR. Transportar valor e lançamento como strings; rejeitar precisão inválida antes da normalização e aplicar os retornos `201`, `400`, `404` e `409` previstos.
- [ ] **T008 — Validar a integração do backend.** Adicionar testes REST/JPA/Flyway para contratos, validações, precisão, paginação, equivalência temporal e rollback. Verificar numeração com lacunas, períodos distintos e inclusões concorrentes no mesmo período vazio ou preenchido, inclusive entre produtos diferentes; testar também o esgotamento das tentativas.

## 3. Frontend

- [ ] **T009 — Implementar a consulta visual.** Reproduzir o layout de referência e o grid com as sete colunas, carregamento inicial, paginação e estados de carregamento, erro e ausência de registros.
- [ ] **T010 — Implementar o formulário.** Aplicar os estados e ações Novo, Limpar e Incluir conforme a especificação. Implementar validadores, seleções nulas com `-- Selecione --` e dependência Produto/COSIF com `switchMap`. Tratar e formatar valores em pt-BR sem conversão para ponto flutuante; preservar lançamento como texto.
- [ ] **T011 — Integrar o fluxo de inclusão.** Bloquear envios simultâneos e preservar dados em falhas de inclusão. Após confirmação, limpar e desabilitar os campos e consultar a primeira página. Se essa consulta falhar, permitir repetir somente o GET, sem reenviar o movimento persistido.
- [ ] **T012 — Validar o frontend.** Cobrir os comportamentos de CA-01 a CA-08 aplicáveis à interface, incluindo limites, valores negativos e zero, precisão, paginação, transições do formulário, respostas COSIF fora de ordem e falhas de envio ou reconsulta.

## 4. Validação final

- [ ] **T013 — Validar a aplicação e registrar a execução.** Executar `./mvnw clean verify` ou o equivalente Windows, testes Angular sem observação e build de produção. Confirmar a execução dos testes `*IT`, a cobertura exigida e CA-01 a CA-08 com a aplicação iniciada. Verificar a persistência após reinício e atualizar o README com os comandos confirmados e as versões do frontend.
