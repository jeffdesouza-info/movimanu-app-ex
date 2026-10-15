# Especificação — Movimentos Manuais

## 1. Objetivo e referências

Permitir consultar e incluir movimentos manuais pela interface de referência, com API Java 21/Spring Boot e frontend Angular, conforme `constitution.md`.

Referência do exercício: `document/references/enunciado-movimentos-manuais.md` (requisitos e modelo relacional) e `document/references/enunciado-movimentos-manuais.pdf` (apoio visual à disposição da tela).

## 2. Consulta

- **RF-CON-01:** consultar os movimentos ao carregar a tela e após cada inclusão bem-sucedida.
- **RF-CON-02:** apresentar no grid as seguintes colunas, nesta ordem:

| Coluna | Origem |
| --- | --- |
| Mês | `MOVIMENTO_MANUAL.DAT_MES` |
| Ano | `MOVIMENTO_MANUAL.DAT_ANO` |
| Código do Produto | `MOVIMENTO_MANUAL.COD_PRODUTO` |
| Descrição do Produto | `PRODUTO.DES_PRODUTO`, pelo produto do movimento |
| Nr Lançamento | `MOVIMENTO_MANUAL.NUM_LANCAMENTO` |
| Descrição do Movimento | `MOVIMENTO_MANUAL.DES_DESCRICAO` |
| Valor | `MOVIMENTO_MANUAL.VAL_VALOR` |

- **RF-CON-03:** ordenar por ano decrescente e, dentro do ano, mês decrescente. Manter ordenação estável nos empates para permitir paginação consistente.
- **RF-CON-04:** exibir no máximo seis registros por página e permitir acessar os restantes quando houver mais de seis resultados.
- **RF-CON-05:** quando não houver registros, apresentar o grid vazio com indicação de ausência de movimentos.

## 3. Inclusão

### 3.1. Estado da tela

- **RF-INC-01:** iniciar com os campos de texto vazios, as combo-boxes em **-- Selecione --** e os campos de entrada desabilitados. Ao clicar em **Novo**, habilitar Mês, Ano, Produto, Cosif, Valor e Descrição para inclusão.
- **RF-INC-02:** o botão **Incluir** deve permitir salvar somente durante a inclusão e com os dados válidos.
- **RF-INC-03:** após persistência bem-sucedida, atualizar a consulta, esvaziar os campos de texto, retornar as combo-boxes a **-- Selecione --** e desabilitar todos os campos de entrada.
- **RF-INC-04:** em falha de validação ou persistência, informar o problema e manter os dados disponíveis para correção, sem apresentar a inclusão como concluída.

### 3.2. Campos

Todos os campos abaixo são obrigatórios.

| Campo | Regra |
| --- | --- |
| Mês | Inteiro de 1 a 12, com até dois dígitos; persistir em `DAT_MES`. |
| Ano | Inteiro positivo, com até quatro dígitos; persistir em `DAT_ANO`. |
| Produto | Combo-box de `PRODUTO`: exibir `DES_PRODUTO` e identificar por `COD_PRODUTO`. |
| Cosif | Combo-box de `PRODUTO_COSIF`: listar os registros do produto selecionado, identificar por `COD_COSIF` nesse produto e exibir `COD_COSIF - (COD_CLASSIFICACAO)`. |
| Descrição do Movimento | Texto digitado, não vazio, com até 50 caracteres; persistir em `DES_DESCRICAO`. |
| Valor | Número decimal positivo ou negativo, diferente de zero, compatível com `NUMERIC(18,2)`: até 16 dígitos inteiros e duas casas decimais; persistir em `VAL_VALOR` preservando a precisão. |

- **RF-INC-05:** ao trocar o produto, retornar Cosif a **-- Selecione --** e atualizar suas opções. Aceitar somente um par produto/COSIF existente em `PRODUTO_COSIF`.
- **RF-INC-06:** validar os dados também na API e rejeitar entradas incompatíveis com estas regras e com os limites do esquema.
- **RF-INC-07:** cada combo-box deve oferecer **-- Selecione --**, representando ausência de seleção e sem corresponder a um código válido. Essa opção não satisfaz a obrigatoriedade do campo.
- **RF-INC-08:** ao clicar em **Limpar**, esvaziar os campos de texto e retornar ambas as combo-boxes a **-- Selecione --**, preservando o modo da tela e o estado de habilitação dos campos.

### 3.3. Dados automáticos e integridade

- **RN-01:** gerar `NUM_LANCAMENTO` como o maior número existente para o mesmo mês e ano, acrescido de um; na ausência de movimentos nesse período, atribuir um. A sequência independe do produto e do COSIF.
- **RN-02:** inclusões simultâneas devem respeitar essa sequência, sem repetir o número de lançamento no mesmo mês e ano.
- **RN-03:** preencher `DAT_MOVIMENTO` automaticamente com a data e hora da persistência.
- **RN-04:** preencher `COD_USUARIO` automaticamente com o valor fixo `TESTE`.
- **RN-05:** persistir o movimento integralmente ou rejeitar a inclusão, preservando as chaves e os relacionamentos do modelo.

## 4. Critérios de aceite

- **CA-01 — Consulta:** ao abrir a tela, carregar o grid com as sete colunas especificadas; janeiro de 2026 deve preceder dezembro de 2025.
- **CA-02 — Paginação:** com sete resultados, apresentar seis na primeira página e um na segunda, mantendo a ordenação.
- **CA-03 — Seleções:** exibir produtos e COSIFs conforme os rótulos definidos; impedir inclusão com **-- Selecione --** ou com par produto/COSIF sem vínculo; ao trocar o produto, retornar Cosif à opção de seleção pendente.
- **CA-04 — Numeração:** gerar um para o primeiro movimento do período e, existindo lançamentos um e três, gerar quatro; um período diferente inicia sua própria sequência. Validar também inclusões simultâneas.
- **CA-05 — Persistência:** salvar os dados informados, a data/hora da inclusão e o usuário `TESTE`; após o sucesso, atualizar a consulta, esvaziar os campos de texto, retornar as combo-boxes a **-- Selecione --** e desabilitar os campos.
- **CA-06 — Validação:** rejeitar campos obrigatórios ausentes, mês fora de 1 a 12, mês/ano não inteiros, ano não positivo, valor zero, limites excedidos e pares produto/COSIF inválidos, mantendo o formulário disponível para correção.
- **CA-07 — Limites e sinal:** aceitar os meses 1 e 12 e rejeitar 0 e 13; aceitar valores como `150,00` e `-150,00` e rejeitar `0,00`.
- **CA-08 — Limpeza:** com campos preenchidos e opções selecionadas, **Limpar** deve esvaziar os textos e retornar ambas as combo-boxes a **-- Selecione --**, mantendo os campos habilitados durante a inclusão.
