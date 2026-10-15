# Diretrizes para agentes — Movimentos Manuais

## Fluxo de trabalho

- Leia `constitution.md` na raiz, o `README.md`, `document/references/enunciado-movimentos-manuais.md` e os artefatos SDD da tarefa antes de atuar.
- Consulte `document/references/enunciado-movimentos-manuais.pdf` para apoio visual à disposição da tela; o modelo relacional está descrito no enunciado em Markdown.
- Siga a sequência `spec.md` (requisitos e aceite), `plan.md` (solução técnica) e `tasks.md` (execução). Implemente somente tarefas com escopo definido e autorizado.
- É proibido registrar referências a versões Git em qualquer artefato gerado ou atualizado, incluindo hashes, identificadores de commits, tags ou links vinculados a uma revisão do repositório. Referencie documentos do projeto somente por seus caminhos.
- Respeite o modo solicitado: planejamento, documentação ou implementação. Esclareça lacunas e conflitos que afetem requisitos antes de implementar o comportamento envolvido.
- Preserve alterações existentes do usuário e limite o trabalho ao escopo solicitado. Commits, push, pull requests e deploy exigem autorização explícita.
- Ao concluir, informe arquivos alterados, validações executadas, resultados e impedimentos. Documentação e respostas devem ser em português brasileiro.

## Organização dos arquivos

- Backend: `backend/movimento-manual-service`, com pacote-base `com.antlia.movimanu` em `src/main/java`.
- Use `domain` para o modelo e regras de negócio; `application/port` e `application/usecase` para portas e casos de uso; `infrastructure` para persistência, configuração e cloud; `interfaces/rest` para endpoints.
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
- Preserve a exigência de 100% de cobertura de linhas no relatório consolidado do backend, com as exclusões existentes de configuração e inicialização. Gere também relatórios JaCoCo exclusivos dos testes unitários e de integração, com dados de execução separados e as mesmas exclusões, conforme `specs/001-movimentos-manuais/plan.md`.
- Configure Maven Surefire para os testes `*Test` e Maven Failsafe para os `*IT`, mantendo a execução dos grupos separada até a consolidação da cobertura em `verify`. O POM atual ainda exige a configuração de Failsafe. Confirme por grupo os testes executados, falhos e ignorados, os caminhos dos relatórios e a verificação dos 100% de linhas.
- A base atual ainda não tem inicialização Java nem workspace Angular. Valide comandos de execução e de frontend quando essas estruturas forem implementadas; o build estrutural não comprova funcionamento da aplicação.
