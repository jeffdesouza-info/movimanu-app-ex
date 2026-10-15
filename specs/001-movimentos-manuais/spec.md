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

- **RF-INC-01:** iniciar com os campos de texto vazios, as combo-boxes em **-- Selecione --** e os campos de entrada desabilitados. Ao clicar em **Novo**, habilitar Mês, Ano, Produto, Cosif, Valor e Descrição para inclusão. A habilitação de Cosif também deve respeitar RF-INC-10.
- **RF-INC-02:** o botão **Incluir** deve permitir salvar somente durante a inclusão e com os dados válidos.
- **RF-INC-03:** após persistência bem-sucedida, atualizar a consulta, esvaziar os campos de texto, retornar as combo-boxes a **-- Selecione --** e desabilitar todos os campos de entrada.
- **RF-INC-04:** em falha de validação ou persistência, informar o problema e preservar os valores e as seleções para correção, sem apresentar a inclusão como concluída. Em falha confirmada após o envio, retornar ao modo de inclusão e restaurar a edição e as ações, mantendo **Incluir** condicionado aos dados válidos.
- **RF-INC-09:** ao iniciar o envio e enquanto o POST estiver pendente, desabilitar Mês, Ano, Produto, Cosif, Valor e Descrição e os botões **Novo**, **Limpar** e **Incluir**, preservando os valores e as seleções e impedindo novo envio.
- **RF-INC-10:** durante o carregamento ou após falha na consulta de COSIFs, manter Cosif em **-- Selecione --**, sem opções anteriores e desabilitado; **Incluir** permanece indisponível. Indicar o carregamento e, em falha, substituí-lo por mensagem e ação **Tentar novamente**, que consulta apenas os COSIFs do produto atual sem alterar os demais dados. Em sucesso, disponibilizar as opções sem seleção automática e habilitar Cosif quando o modo da tela permitir.

### 3.2. Campos

Todos os campos abaixo são obrigatórios.

| Campo | Regra |
| --- | --- |
| Mês | Inteiro de 1 a 12, com até dois dígitos; persistir em `DAT_MES`. |
| Ano | Inteiro positivo, com até quatro dígitos; persistir em `DAT_ANO`. |
| Produto | Combo-box de `PRODUTO`: exibir `DES_PRODUTO` e identificar por `COD_PRODUTO`. |
| Cosif | Combo-box de `PRODUTO_COSIF`: listar os registros do produto selecionado, identificar por `COD_COSIF` nesse produto e exibir `COD_COSIF - (COD_CLASSIFICACAO)`. |
| Descrição do Movimento | Texto digitado com até 50 caracteres e ao menos um caractere diferente de espaço, conforme RF-INC-12; persistir em `DES_DESCRICAO`. |
| Valor | Número decimal positivo ou negativo, diferente de zero, compatível com `NUMERIC(18,2)`: até 16 dígitos inteiros e duas casas decimais, conforme RF-INC-11; persistir em `VAL_VALOR` preservando a precisão. |

- **RF-INC-05:** ao trocar o produto, retornar Cosif a **-- Selecione --** e atualizar suas opções. Aceitar somente um par produto/COSIF existente em `PRODUTO_COSIF`.
- **RF-INC-06:** validar os dados também na API e rejeitar entradas incompatíveis com estas regras e com os limites do esquema.
- **RF-INC-07:** cada combo-box deve oferecer **-- Selecione --**, representando ausência de seleção e sem corresponder a um código válido. Essa opção não satisfaz a obrigatoriedade do campo.
- **RF-INC-08:** ao clicar em **Limpar**, esvaziar os campos de texto e retornar ambas as combo-boxes a **-- Selecione --**, preservando o modo da tela e o estado de habilitação dos campos.

- **RF-INC-11:** no campo Valor, aceitar notação decimal simples: sinal negativo opcional no início, de um a 16 dígitos inteiros e, opcionalmente, um único separador decimal (vírgula ou ponto) seguido de uma ou duas casas. Rejeitar separadores de milhar, formatos mistos, notação científica, espaços e símbolos monetários. Validar o texto completo antes de normalizar; preservar sinal e precisão e rejeitar zero ou excesso de casas, sem arredondar ou remover caracteres para tornar a entrada válida. Converter somente a entrada válida para o formato textual com ponto do contrato da API.

- **RF-INC-12:** validar a Descrição no frontend e na API, rejeitando valor nulo, texto vazio ou formado somente por espaços. Verificar o conteúdo sem remover espaços do texto a persistir; aplicar o limite de 50 caracteres ao texto informado. Na interface, indicar o erro no campo, preservar o texto para correção e impedir a inclusão. Na API, retornar `400` no formato contratado, associado a `descricao`, sem persistir o movimento.

### 3.3. Dados automáticos e integridade

- **RN-01:** gerar `NUM_LANCAMENTO` como o maior número existente para o mesmo mês e ano, acrescido de um; na ausência de movimentos nesse período, atribuir um. A sequência independe do produto e do COSIF.
- **RN-02:** inclusões simultâneas devem respeitar essa sequência, sem repetir o número de lançamento no mesmo mês e ano.
- **RN-03:** preencher `DAT_MOVIMENTO` automaticamente com a data e hora da persistência.
- **RN-04:** preencher `COD_USUARIO` automaticamente com o valor fixo `TESTE`.
- **RN-05:** persistir o movimento integralmente ou rejeitar a inclusão, preservando as chaves e os relacionamentos do modelo.

## 4. Respostas da API

- **RF-API-01:** distinguir ausência de resultados, recurso inexistente e entrada inválida conforme a tabela. Os campos dos DTOs, da paginação e dos erros seguem o contrato definido no `plan.md` desta funcionalidade.

| Situação | Resposta |
| --- | --- |
| Consulta de produtos sem registros. | `200`, com lista `[]`. |
| Consulta de COSIFs de produto existente, sem COSIFs vinculados. | `200`, com lista `[]`. |
| Consulta de COSIFs de produto inexistente. | `404`, no formato de erro contratado. |
| Inclusão com par produto/COSIF inválido, inclusive produto inexistente. | `400`, sem persistir o movimento. |
| Consulta de movimentos com `page` negativo ou de formato inválido. | `400`, no formato de erro contratado. |
| Consulta de movimentos sem registros ou com página além dos resultados. | `200`, com `content=[]`; preservar a página solicitada e os totais reais da consulta. |

## 5. Critérios de aceite

- **CA-01 — Consulta:** ao abrir a tela, carregar o grid com as sete colunas especificadas; janeiro de 2026 deve preceder dezembro de 2025.
- **CA-02 — Paginação:** com sete resultados, apresentar seis na primeira página e um na segunda, mantendo a ordenação.
- **CA-03 — Seleções:** exibir produtos e COSIFs conforme os rótulos definidos; impedir inclusão com **-- Selecione --** ou com par produto/COSIF sem vínculo; ao trocar o produto, retornar Cosif à opção de seleção pendente.
- **CA-04 — Numeração:** gerar um para o primeiro movimento do período e, existindo lançamentos um e três, gerar quatro; um período diferente inicia sua própria sequência. Validar também inclusões simultâneas.
- **CA-05 — Persistência:** salvar os dados informados, a data/hora da inclusão e o usuário `TESTE`; após o sucesso, atualizar a consulta, esvaziar os campos de texto, retornar as combo-boxes a **-- Selecione --** e desabilitar os campos.
- **CA-06 — Validação:** rejeitar campos obrigatórios ausentes, mês fora de 1 a 12, mês/ano não inteiros, ano não positivo, valor zero, limites excedidos e pares produto/COSIF inválidos, mantendo o formulário disponível para correção.
- **CA-07 — Limites e sinal:** aceitar os meses 1 e 12 e rejeitar 0 e 13; aceitar valores como `150,00` e `-150,00` e rejeitar `0,00`.
- **CA-08 — Limpeza:** com campos preenchidos e opções selecionadas, **Limpar** deve esvaziar os textos e retornar ambas as combo-boxes a **-- Selecione --**, mantendo os campos habilitados durante a inclusão.
- **CA-09 — Bloqueio durante o envio:** com o POST pendente, impedir a alteração dos seis campos e o acionamento de **Novo**, **Limpar** e **Incluir**, sem disparar outro POST. Em falha confirmada, retornar à inclusão com valores e seleções preservados e restaurar a edição e as ações, mantendo **Incluir** condicionado aos dados válidos. Em sucesso, cumprir CA-05.
- **CA-10 — Consulta de COSIFs:** ao selecionar um produto, manter Cosif sem seleção e indisponível durante a consulta. Em falha, exibir erro e **Tentar novamente**, preservando os demais valores e seleções. Repetir somente o GET de COSIFs do produto atual; em sucesso, permitir selecionar uma opção sem seleção automática. Ignorar respostas de produto anterior e impedir inclusão enquanto Cosif não tiver seleção válida.
- **CA-11 — Contrato REST:** validar os casos de RF-API-01 e os nomes e tipos dos campos dos DTOs, da paginação e dos erros definidos no plano. Com sete movimentos, `page=2` retorna `200`, `content=[]`, `page=2`, `size=6`, `totalElements=7` e `totalPages=2`; `page=-1` retorna `400`.
- **CA-12 — Formato monetário:** aceitar no formulário `150`, `150,5`, `150.50`, `-150,00` e `1234,56`; `1,23` e `1.23` representam o mesmo valor. Rejeitar `1.234`, `1,234`, `1.234,56`, `1,234.56`, `1 234,56`, `1e3`, `1E3` e `150,001`. Aceitar o limite `9999999999999999,99`, rejeitar `10000000000000000,00` e qualquer representação de zero, incluindo `-0,00`. Enviar `-150,00` como `"-150.00"` e preservar o valor do limite sem perda de precisão. Na API, formatos incompatíveis com o contrato, inclusive vírgula decimal, retornam `400` associado a `valor`.
- **CA-13 — Descrição com conteúdo:** com os demais campos válidos, rejeitar descrição vazia (`""`) ou formada somente por espaços (`"   "`) no frontend e na API. A interface deve informar o erro e manter **Incluir** indisponível, preservando os dados para correção; o POST direto deve retornar `400`, com erro associado a `descricao` e sem gravação. Aceitar `"Ajuste manual"` e `"  Ajuste manual  "`, preservando o texto informado; manter o limite total de 50 caracteres, incluindo os espaços.
