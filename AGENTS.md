# Diretrizes para agentes — Movimentos Manuais

## Fluxo de trabalho

- Leia `constitution.md` na raiz, o `README.md`, `document/references/enunciado-movimentos-manuais.md` e os artefatos SDD da tarefa antes de atuar.
- Consulte `document/references/enunciado-movimentos-manuais.pdf` para apoio visual à disposição da tela; o modelo relacional está descrito no enunciado em Markdown.
- Identifique os artefatos SDD aplicáveis ao escopo autorizado e siga a sequência `spec.md` (requisitos e aceite), `plan.md` (solução técnica) e `tasks.md` (execução). Em trabalho transversal, considere as especificações afetadas. Implemente somente tarefas com escopo definido e autorizado.
- Mantenha este `AGENTS.md` como governança geral do projeto, independente de especificações individuais. Referencie artefatos SDD por seu papel e nome genérico; os caminhos concretos pertencem ao contexto da tarefa e aos documentos da especificação correspondente.
- É proibido registrar referências a versões Git em qualquer artefato gerado ou atualizado, incluindo hashes, identificadores de commits, tags ou links vinculados a uma revisão do repositório. Referencie documentos do projeto somente por seus caminhos.
- Respeite o modo solicitado: planejamento, documentação ou implementação. Esclareça lacunas e conflitos que afetem requisitos antes de implementar o comportamento envolvido.
- Preserve alterações existentes do usuário e limite o trabalho ao escopo solicitado. Commits, push, pull requests e deploy exigem autorização explícita.
- Ao concluir, informe arquivos alterados, validações executadas, resultados e impedimentos. Documentação e respostas devem ser em português brasileiro.

## Papel normativo dos documentos

| Documento | Responsabilidade |
| --- | --- |
| `constitution.md` | Define os princípios obrigatórios de arquitetura, qualidade e fidelidade ao exercício que orientam os demais artefatos. |
| `AGENTS.md` | Define como o agente deve atuar: modos, autorização, organização, edição, validação e prestação de contas, respeitando a constituição e o escopo aprovado. |
| `document/references/enunciado-movimentos-manuais.md` | Registra a referência limpa do exercício, incluindo requisitos de origem e modelo relacional. As decisões funcionais aprovadas são consolidadas na especificação. |
| `document/references/enunciado-movimentos-manuais.pdf` | Oferece apoio visual à disposição da tela, em conjunto com o enunciado em Markdown e a especificação. |
| `spec.md` da especificação aplicável | Define o comportamento esperado por requisitos funcionais (RF), regras de negócio (RN) e critérios de aceite (CA); é a fonte dos identificadores usados nas tarefas. |
| `plan.md` da especificação aplicável | Define a solução técnica, as tecnologias, os contratos REST referenciados na especificação e a estratégia de testes e cobertura para atender aos requisitos. |
| `tasks.md` da especificação aplicável | Organiza a execução, o escopo e o estado das tarefas e seus vínculos com RF, RN e CA, conforme a especificação e o plano. |
| `README.md` | Documenta preparação, execução e validação confirmadas da aplicação, incluindo comandos e evidências. Deve refletir o comportamento aprovado. |

- O plano detalha a especificação, e as tarefas executam a solução aprovada. Instruções operacionais e tarefas devem respeitar os princípios, requisitos, critérios de aceite e contratos definidos nos documentos responsáveis. Esclareça divergências que os afetem antes de implementar o trecho envolvido.
- Em cada tarefa, cite os identificadores RF, RN e CA pertinentes, sem reproduzir seus textos, resolvendo-os na especificação indicada no contexto da tarefa. O vínculo indica a contribuição da tarefa dentro de seu escopo; um critério compartilhado pode depender de várias tarefas. Para preparação técnica sem vínculo funcional direto, indique a base de governança e as seções do plano.

## Organização dos arquivos

- Backend: `backend/movimento-manual-service`, com pacote-base `com.antlia.movimanu` em `src/main/java`.
- Use `domain` para o modelo e regras de negócio; `application/port` e `application/usecase` para portas e casos de uso; `infrastructure` para persistência, configuração e cloud; `interfaces/rest` para endpoints.
- Siga a modelagem definida no plano aplicável. Use campos diretos com tipos Java usuais; mantenha as validações nos construtores ou fábricas dos modelos e compartilhe regras repetidas em auxiliares internos simples. Não introduza tipos apenas para encapsular campos individuais nem abstrações sem responsabilidade necessária ao escopo aprovado.
- Recursos e migrações ficam em `src/main/resources`; testes, em `src/test/java/com/antlia/movimanu/unit` e `integration`.
- Frontend: `frontend/movimanu-web/src/app`, com `core` para serviços gerais, `shared` para elementos reutilizáveis e `features/manual-movements` para a funcionalidade.

## Convenções de edição

- Siga `.editorconfig`: UTF-8, CRLF, quebra final, sem espaços ao fim das linhas; quatro espaços em Java e dois em XML, YAML, SQL e Markdown.
- Use pacotes em minúsculas, tipos Java em `PascalCase` e métodos e atributos em `camelCase`, com nomes que expressem sua responsabilidade.
- Nomeie migrações Flyway como `V<numero>__<descricao_snake_case>.sql`; preserve migrações já aplicadas e faça evoluções em novos scripts.
- Não versione credenciais, arquivos `.env`, dados locais do H2 ou saídas de build. Quando autorizado a criar commits, use Conventional Commits.

## Validação

- No backend, use JUnit Jupiter na versão gerenciada pelo Spring Boot, sem sobrescrever essa dependência. Mantenha testes unitários como `*Test` e testes de integração com Spring Boot Test como `*IT`. Inclua testes nas alterações de comportamento.
- Cubra cada regra e caso de uso com testes unitários de sucesso, erro e limites aplicáveis, com asserções sobre resultados e efeitos esperados. Execute-os de forma isolada, usando dublês das dependências quando necessário, sem banco, rede ou contexto Spring. Esses cenários unitários são obrigatórios mesmo quando as mesmas linhas são exercitadas por testes de integração.
- Execute, na raiz, `.\mvnw.cmd test` durante o desenvolvimento e `.\mvnw.cmd clean verify` para a validação final. Em Linux/macOS, use `./mvnw`.
- Preserve a exigência de 100% de cobertura de linhas no relatório consolidado do backend, com as exclusões existentes de configuração e inicialização. Gere também relatórios JaCoCo exclusivos dos testes unitários e de integração, com dados de execução separados e as mesmas exclusões, conforme o `plan.md` aplicável ao escopo da tarefa.
- Mantenha Maven Surefire para os testes `*Test` e Maven Failsafe para os `*IT`, já configurados no POM, sem sobreposição e com execução separada até a consolidação da cobertura em `verify`. Confirme por grupo os testes executados, falhos e ignorados, os caminhos dos relatórios e a verificação dos 100% de linhas.
- No frontend, execute os comandos em `frontend/movimanu-web`. Use `npm ci` para instalar as dependências a partir do lockfile, `npm test` para testes sem observação e `npm run build` para o build de produção. Use `npm start` para iniciar o servidor de desenvolvimento com o proxy local. Confira as versões de Node.js e npm resolvidas pelo terminal, respeitando o plano e o `package.json`; preserve as versões fixadas e o `package-lock.json`.
- O backend já possui inicialização Spring Boot, JAR executável, perfis local e de testes, modelos de domínio, portas e projeção de consulta independentes de frameworks. A infraestrutura de persistência contém entidades JPA, chaves compostas, mapeadores, restrição única por período/lançamento, conversão temporal e dados demonstrativos exclusivos do perfil local, com testes de persistência. As consultas de produtos, COSIFs e movimentos já possuem casos de uso e adaptadores JPA, com projeção, ordenação e paginação testadas. A inclusão concorrente está implementada com validação do vínculo, sequência por período, relógio UTC, usuário fixo, inserção sem sobrescrita e até três tentativas em transações independentes, com testes determinísticos.
- Os quatro endpoints REST em `/api/v1` estão implementados com DTOs próprios, Bean Validation, validação textual de valor, desserialização sem coerção e `ProblemDetail` em pt-BR. Erros de tipo, inclusive números fora da faixa de `int`, preservam o campo identificável. A consulta carrega as entidades com os relacionamentos necessários e monta a projeção no mapeador, preservando a conversão temporal UTC. A validação integrada do backend está concluída, com testes REST/JPA/Flyway, testes unitários próprios das validações de entrada e cobertura consolidada de 100% de linhas.
- O frontend Angular possui consulta inicial de movimentos, grid de sete colunas, paginação pelos metadados da API, estados de carregamento, erro e ausência de registros e nova tentativa somente do GET. Páginas vazias além dos resultados preservam os metadados e permitem retornar à primeira página ou à anterior, sem apresentar intervalos invertidos. O layout desktop e o estado vazio foram conferidos em captura da aplicação em execução.
- O formulário interativo usa Reactive Forms com modos de consulta, inclusão e envio, ações Novo/Limpar, seleções nulas e consulta de produtos/COSIFs. O fluxo de COSIFs trata carregamento, erro, nova tentativa e cancelamento de consultas obsoletas; após Limpar, permite selecionar novamente o mesmo produto. Período, descrição e valor são validados, preservando espaços válidos e precisão monetária por operações textuais. O POST está integrado: bloqueia todos os campos e Novo/Limpar/Incluir durante o envio e restaura a edição com os dados preservados em falha confirmada, exibindo erros gerais e por campo. Após sucesso, limpa e desabilita o formulário, publica a seleção nula de Produto e consulta a primeira página; a nova tentativa dessa consulta repete somente o GET. A complementação dos testes do frontend e a validação final da aplicação permanecem nas tarefas seguintes.
