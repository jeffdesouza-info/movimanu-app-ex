# Enunciado de referência - Movimentos Manuais

Exercício técnico de desenvolvimento Java e Angular. Este documento reescreve as exigências do material fornecido em uma apresentação neutra, com os campos e o modelo de dados descritos em texto.

Uso no projeto: este enunciado registra a referência do exercício. Os esclarecimentos aprovados e os critérios de aceite ficam em `specs/001-movimentos-manuais/spec.md`; as soluções técnicas, em `specs/001-movimentos-manuais/plan.md`; a execução, em `specs/001-movimentos-manuais/tasks.md`.

## 1. Objetivo

Construir uma aplicação em camadas para consultar e incluir movimentos manuais associados a um mês e ano, respeitando as regras de negócio, a interface de referência e o modelo relacional descritos a seguir.

## 2. Tecnologias e organização

| Camada ou aspecto | Exigência do exercício |
| --- | --- |
| Apresentação | Angular 8 ou superior. |
| Aplicação | Java 8 ou superior com Spring Boot, expondo uma API REST. |
| Organização da API | Desejável estruturar um microsserviço orientado a DDD. |
| Banco de dados | H2, Oracle ou SQL Server. |
| Acesso a dados | Desejável utilizar ORM, como Hibernate. |
| Diferenciais | Testes unitários e utilização de componentes Spring Cloud. |

Variações da estrutura em camadas são admitidas pelo exercício. As escolhas concretas e os compromissos adicionais adotados neste projeto estão em `constitution.md` e `specs/001-movimentos-manuais/plan.md`.

## 3. Entrega e avaliação

Entregar o código-fonte em arquivo ou em repositório próprio, preferencialmente GitHub ou Bitbucket.

A avaliação considera:

- Atendimento aos requisitos de negócio.
- Aderência às tecnologias solicitadas.
- Legibilidade, organização e facilidade de manutenção do código.
- Aplicação dos princípios SOLID.

## 4. Interface de referência

Título da tela: **Movimentos Manuais**. A disposição dos campos segue duas colunas:

| Linha | Coluna esquerda | Coluna direita |
| --- | --- | --- |
| 1 | Mês: campo de texto. | Ano: campo de texto. |
| 2 | Produto: combo-box. | Cosif: combo-box. |
| 3 | Valor: campo de texto. | Sem campo. |
| 4 | Descrição: área de texto. | Sem campo. |

As combo-boxes apresentam a opção `-- Selecione --`. Abaixo da descrição ficam os botões **Limpar**, **Novo** e **Incluir**, nessa ordem. O grid fica abaixo do formulário e contém, nesta ordem: **Mês**, **Ano**, **Código do Produto**, **Descrição do Produto**, **Nr Lançamento**, **Descrição** e **Valor**.

Essa descrição registra a disposição funcional do protótipo; os rótulos e a origem dos dados do grid estão detalhados em `specs/001-movimentos-manuais/spec.md`.

## 5. Consulta e inclusão

1. Preencher o grid com o resultado de uma consulta automática ao banco de dados.
2. Ao acionar **Novo**, habilitar os campos para digitação de um novo lançamento.
3. Permitir digitar mês, ano, valor e descrição do movimento.
4. Carregar Produto a partir de `PRODUTO`, exibindo `DES_PRODUTO`. Carregar os COSIFs correspondentes ao produto a partir de `PRODUTO_COSIF`, exibindo `COD_COSIF - (COD_CLASSIFICACAO)`.
5. Gerar `NUM_LANCAMENTO` a partir da sequência de lançamentos do mesmo mês e ano, acrescentando um ao último número do período.
6. Preencher `DAT_MOVIMENTO` automaticamente com a data e hora da gravação.
7. Preencher `COD_USUARIO` automaticamente com o valor fixo `TESTE`.
8. Após incluir o movimento, atualizar o grid e desabilitar os campos da tela.

## 6. Modelo relacional

Preservar os tipos, tamanhos, precisão, nulabilidade, chaves e relacionamentos abaixo. `PK` indica participação na chave primária. `PRODUTO` e `PRODUTO_COSIF` são tabelas de referência e podem receber dados demonstrativos para testar a aplicação.

### PRODUTO

| Coluna | Tipo | Aceita nulo | Chave |
| --- | --- | --- | --- |
| COD_PRODUTO | CHAR(4) | Não | PK |
| DES_PRODUTO | VARCHAR(30) | Sim | - |
| STA_STATUS | CHAR(1) | Sim | - |

### PRODUTO_COSIF

| Coluna | Tipo | Aceita nulo | Chave |
| --- | --- | --- | --- |
| COD_PRODUTO | CHAR(4) | Não | PK e FK |
| COD_COSIF | CHAR(11) | Não | PK |
| COD_CLASSIFICACAO | CHAR(6) | Sim | - |
| STA_STATUS | CHAR(1) | Sim | - |

Chave primária composta: `(COD_PRODUTO, COD_COSIF)`. Chave estrangeira: `COD_PRODUTO` referencia `PRODUTO.COD_PRODUTO`.

### MOVIMENTO_MANUAL

| Coluna | Tipo | Aceita nulo | Chave |
| --- | --- | --- | --- |
| DAT_MES | NUMERIC(2,0) | Não | PK |
| DAT_ANO | NUMERIC(4,0) | Não | PK |
| NUM_LANCAMENTO | NUMERIC(18,0) | Não | PK |
| COD_PRODUTO | CHAR(4) | Não | PK e FK composta |
| COD_COSIF | CHAR(11) | Não | PK e FK composta |
| DES_DESCRICAO | VARCHAR(50) | Não | - |
| DAT_MOVIMENTO | SMALLDATETIME | Não | - |
| COD_USUARIO | VARCHAR(15) | Não | - |
| VAL_VALOR | NUMERIC(18,2) | Não | - |

Chave primária composta: `(DAT_MES, DAT_ANO, NUM_LANCAMENTO, COD_PRODUTO, COD_COSIF)`. Chave estrangeira composta: `(COD_PRODUTO, COD_COSIF)` referencia o mesmo par em `PRODUTO_COSIF`.

Relacionamentos:

- Um produto pode ter vários COSIFs vinculados; cada registro de `PRODUTO_COSIF` pertence a um único produto.
- Um par produto/COSIF pode ter vários movimentos; cada movimento referencia exatamente um par existente.

