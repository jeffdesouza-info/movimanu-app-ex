# Constituição do projeto — Movimentos Manuais

## 1. Desenvolvimento orientado por especificações

- O projeto adota Spec-Driven Development: requisitos e critérios de aceite orientam o planejamento, as tarefas, a implementação e sua validação.
- Regras de negócio e decisões pendentes devem ser esclarecidas e registradas antes da implementação correspondente. Mudanças nestes princípios exigem decisão explícita do responsável pelo projeto e atualização dos artefatos afetados.
- É proibido registrar referências a versões Git nos artefatos do projeto. Documentos do projeto devem ser referenciados somente por seus caminhos.

## 2. Arquitetura e projeto de código

- A API REST utiliza Java 21 e Spring Boot, com arquitetura de microsserviços orientada a DDD. As fronteiras dos serviços seguem os contextos de negócio definidos no planejamento.
- A organização hexagonal separa domínio, aplicação, interfaces e infraestrutura. As regras de negócio permanecem independentes de HTTP, persistência e provedores de cloud.
- SOLID é obrigatório: responsabilidade única, aberto/fechado, substituição de Liskov, segregação de interfaces e inversão de dependência. As soluções devem priorizar simplicidade, coesão e baixo acoplamento.

## 3. Tecnologias e ambientes

- A persistência utiliza Spring Data JPA e Hibernate, inicialmente com H2, e evolução do esquema por migrações Flyway.
- O frontend utiliza Angular 8 ou superior. A versão deve ser estável, compatível com as ferramentas adotadas e registrada no planejamento antes da implementação.
- O projeto utiliza componentes Spring Cloud conforme as necessidades especificadas. A escolha entre OCI e AWS permanece pendente; configurações de ambiente devem permitir essa definição posterior.

## 4. Fidelidade às referências do teste

- Use `document/references/enunciado-movimentos-manuais.md` como referência do exercício e `document/references/enunciado-movimentos-manuais.pdf` como apoio visual à disposição da tela.
- O modelo relacional descrito no enunciado é a referência para `PRODUTO`, `PRODUTO_COSIF` e `MOVIMENTO_MANUAL`. Devem ser respeitados colunas, cardinalidades, chaves primárias e estrangeiras, inclusive compostas, nulabilidade, tipos, tamanhos e precisão.
- Adaptações de tipos para H2, incluindo `SMALLDATETIME`, exigem documentação e validação da equivalência de comportamento. Valores monetários devem preservar a precisão decimal definida no modelo.
- A disposição da tela descrita no enunciado orienta campos, ações e listagem da interface. Comportamentos não descritos nas referências devem ser definidos na especificação funcional.

## 5. Qualidade verificável

- Toda lógica implementada deve ser coberta por testes unitários, incluindo cenários de sucesso, erro e condições de limite. A cobertura de linhas deve ser acompanhada da verificação efetiva das regras de negócio.
- Testes de integração devem complementar os unitários na validação de persistência e contratos REST. A conclusão de uma tarefa depende do atendimento aos critérios de aceite e das validações aplicáveis.
