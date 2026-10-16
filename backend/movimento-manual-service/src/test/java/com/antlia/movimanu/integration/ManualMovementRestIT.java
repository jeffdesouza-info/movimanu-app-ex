package com.antlia.movimanu.integration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.antlia.movimanu.application.exception.ManualMovementSequenceCollisionException;
import com.antlia.movimanu.application.port.ManualMovementInclusionPort;
import com.antlia.movimanu.application.usecase.IncludeManualMovementUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Sql("/fixtures/persistence-fixtures.sql")
class ManualMovementRestIT {
    private static final String API = "/api/v1";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoSpyBean
    private IncludeManualMovementUseCase includeManualMovementUseCase;

    @MockitoSpyBean
    private ManualMovementInclusionPort movementInclusionPort;

    @BeforeEach
    void configuresMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @AfterEach
    void clearsFixtureDataAndIncludedMovements() {
        jdbcTemplate.update("DELETE FROM MOVIMENTO_MANUAL");
        jdbcTemplate.update("DELETE FROM PRODUTO_COSIF");
        jdbcTemplate.update("DELETE FROM PRODUTO");
    }

    @Test
    void listsProductsWithTextCodesAndNullableDescriptions() throws Exception {
        jdbcTemplate.update("INSERT INTO PRODUTO (COD_PRODUTO, DES_PRODUTO, STA_STATUS) VALUES (?, ?, ?)",
                "0003", null, "A");

        MvcResult result = mockMvc.perform(get(API + "/produtos"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$[0].codigoProduto").value("0001"))
                .andExpect(jsonPath("$[0].descricaoProduto").value("Produto de fixture A"))
                .andExpect(jsonPath("$[2].codigoProduto").value("0003"))
                .andExpect(jsonPath("$[2].descricaoProduto").value(org.hamcrest.Matchers.nullValue()))
                .andReturn();

        JsonNode products = objectMapper.readTree(result.getResponse().getContentAsString());
        assertFields(products.get(0), "codigoProduto", "descricaoProduto");
        assertThat(products.get(0).get("codigoProduto").isTextual()).isTrue();
        assertThat(products.get(0).get("descricaoProduto").isTextual()).isTrue();
        assertFields(products.get(2), "codigoProduto", "descricaoProduto");
        assertThat(products.get(2).get("descricaoProduto").isNull()).isTrue();
    }

    @Test
    void listsAnEmptyProductCollection() throws Exception {
        jdbcTemplate.update("DELETE FROM PRODUTO_COSIF");
        jdbcTemplate.update("DELETE FROM PRODUTO");

        mockMvc.perform(get(API + "/produtos"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[]"));
    }

    @Test
    void listsCosifsAndReturnsAnEmptyListForAnExistingProductWithoutCosifs() throws Exception {
        jdbcTemplate.update("INSERT INTO PRODUTO (COD_PRODUTO, DES_PRODUTO, STA_STATUS) VALUES (?, ?, ?)",
                "0003", "Produto sem COSIF", "A");
        jdbcTemplate.update("INSERT INTO PRODUTO_COSIF (COD_PRODUTO, COD_COSIF, COD_CLASSIFICACAO, STA_STATUS) "
                        + "VALUES (?, ?, ?, ?)",
                "0001", "10000000002", null, "A");

        MvcResult result = mockMvc.perform(get(API + "/produtos/0001/cosifs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoCosif").value("10000000001"))
                .andExpect(jsonPath("$[1].codigoCosif").value("10000000002"))
                .andExpect(jsonPath("$[1].codigoClassificacao").value(org.hamcrest.Matchers.nullValue()))
                .andReturn();

        JsonNode cosifs = objectMapper.readTree(result.getResponse().getContentAsString());
        assertFields(cosifs.get(0), "codigoCosif", "codigoClassificacao");
        assertThat(cosifs.get(0).get("codigoCosif").isTextual()).isTrue();
        assertThat(cosifs.get(0).get("codigoClassificacao").isTextual()).isTrue();
        assertFields(cosifs.get(1), "codigoCosif", "codigoClassificacao");
        assertThat(cosifs.get(1).get("codigoClassificacao").isNull()).isTrue();

        mockMvc.perform(get(API + "/produtos/0003/cosifs"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void returnsA404ProblemForAnUnknownProductInCosifLookup() throws Exception {
        mockMvc.perform(get(API + "/produtos/9999/cosifs"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("O produto informado não foi encontrado."))
                .andExpect(jsonPath("$.instance").value(API + "/produtos/9999/cosifs"))
                .andExpect(jsonPath("$.errors[0].field").value("codigoProduto"));
    }

    @Test
    void returnsPagedMovementDtosWithTheSpecifiedMetadataAndTextualNumbers() throws Exception {
        seedSevenMovements();

        MvcResult firstPageResult = mockMvc.perform(get(API + "/movimentos-manuais"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(6))
                .andExpect(jsonPath("$.content[0].mes").value(1))
                .andExpect(jsonPath("$.content[0].ano").value(2026))
                .andExpect(jsonPath("$.content[0].codigoProduto").value("0001"))
                .andExpect(jsonPath("$.content[0].descricaoProduto").value("Produto de fixture A"))
                .andExpect(jsonPath("$.content[0].numeroLancamento").value("1"))
                .andExpect(jsonPath("$.content[0].valor").value("150.00"))
                .andExpect(jsonPath("$.content[0].dataMovimento").value("2026-01-01T12:00:00Z"))
                .andExpect(jsonPath("$.content[0].codigoUsuario").value("TESTE"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(6))
                .andExpect(jsonPath("$.totalElements").value(7))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andReturn();

        JsonNode firstPage = objectMapper.readTree(firstPageResult.getResponse().getContentAsString());
        assertFields(firstPage, "content", "page", "size", "totalElements", "totalPages");
        assertThat(firstPage.get("content").isArray()).isTrue();
        assertThat(firstPage.get("page").isIntegralNumber()).isTrue();
        assertThat(firstPage.get("size").isIntegralNumber()).isTrue();
        assertThat(firstPage.get("totalElements").isIntegralNumber()).isTrue();
        assertThat(firstPage.get("totalPages").isIntegralNumber()).isTrue();
        assertFields(firstPage.get("content").get(0),
                "mes", "ano", "codigoProduto", "codigoCosif", "descricao", "valor",
                "descricaoProduto", "numeroLancamento", "dataMovimento", "codigoUsuario");

        mockMvc.perform(get(API + "/movimentos-manuais").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].descricao").value("Agosto de 2024"))
                .andExpect(jsonPath("$.page").value(1));

        mockMvc.perform(get(API + "/movimentos-manuais").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(6))
                .andExpect(jsonPath("$.totalElements").value(7))
                .andExpect(jsonPath("$.totalPages").value(2));

        for (int page : new int[] {357_913_942, Integer.MAX_VALUE}) {
            mockMvc.perform(get(API + "/movimentos-manuais").param("page", Integer.toString(page)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.page").value(page))
                    .andExpect(jsonPath("$.size").value(6))
                    .andExpect(jsonPath("$.totalElements").value(7))
                    .andExpect(jsonPath("$.totalPages").value(2));
        }
    }

    @Test
    void returnsEmptyMovementMetadataAndAcceptsTheLargestPageIndex() throws Exception {
        mockMvc.perform(get(API + "/movimentos-manuais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(6))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        mockMvc.perform(get(API + "/movimentos-manuais").param("page", "2147483647"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(2147483647))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "", "abc", "1.0", "2147483648"})
    void rejectsInvalidPageValuesWithAFieldError(String page) throws Exception {
        MvcResult result = mockMvc.perform(get(API + "/movimentos-manuais").param("page", page))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.instance").value(API + "/movimentos-manuais"))
                .andExpect(jsonPath("$.errors[0].field").value("page"))
                .andReturn();

        assertProblemShape(result, 400);
    }

    @Test
    void includesAndReturnsTheExactDtoWithGeneratedFieldsAndPreservedInput() throws Exception {
        MvcResult result = postJson(movementJson(10, 2026, "0001", "10000000001", "  Ajuste manual  ",
                "-9999999999999999.99"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.mes").value(10))
                .andExpect(jsonPath("$.ano").value(2026))
                .andExpect(jsonPath("$.codigoProduto").value("0001"))
                .andExpect(jsonPath("$.codigoCosif").value("10000000001"))
                .andExpect(jsonPath("$.descricao").value("  Ajuste manual  "))
                .andExpect(jsonPath("$.valor").value("-9999999999999999.99"))
                .andExpect(jsonPath("$.descricaoProduto").value("Produto de fixture A"))
                .andExpect(jsonPath("$.numeroLancamento").value("1"))
                .andExpect(jsonPath("$.codigoUsuario").value("TESTE"))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertFields(response,
                "mes", "ano", "codigoProduto", "codigoCosif", "descricao", "valor",
                "descricaoProduto", "numeroLancamento", "dataMovimento", "codigoUsuario");
        assertThat(response.get("mes").isIntegralNumber()).isTrue();
        assertThat(response.get("ano").isIntegralNumber()).isTrue();
        assertThat(response.get("codigoProduto").isTextual()).isTrue();
        assertThat(response.get("codigoCosif").isTextual()).isTrue();
        assertThat(response.get("descricao").isTextual()).isTrue();
        assertThat(response.get("valor").isTextual()).isTrue();
        assertThat(response.get("descricaoProduto").isTextual()).isTrue();
        assertThat(response.get("numeroLancamento").isTextual()).isTrue();
        assertThat(response.get("dataMovimento").isTextual()).isTrue();
        assertThat(response.get("codigoUsuario").isTextual()).isTrue();
        Instant movementAt = Instant.parse(response.get("dataMovimento").asText());
        assertThat(movementAt.getNano()).isZero();
        assertThat(movementAt.atOffset(java.time.ZoneOffset.UTC).getSecond()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM MOVIMENTO_MANUAL", Long.class)).isEqualTo(1L);
    }

    @Test
    void acceptsAndPreservesAValidDescriptionOfExactlyFiftyCharacters() throws Exception {
        String description = " " + "A".repeat(48) + " ";

        postJson(movementJson(10, 2026, "0001", "10000000001", description, "150.00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").value(description));

        assertThat(description).hasSize(50);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT DES_DESCRICAO FROM MOVIMENTO_MANUAL", String.class)).isEqualTo(description);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "150|150.00",
        "150.5|150.50",
        "150.50|150.50",
        "-150.00|-150.00",
        "9999999999999999.99|9999999999999999.99"
    })
    void acceptsApiMoneyFormatsAndPreservesTheirExactDecimalValue(String input, String expected) throws Exception {
        postJson(movementJson(10, 2026, "0001", "10000000001", "Ajuste", input))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valor").value(expected));

        BigDecimal persisted = jdbcTemplate.queryForObject(
                "SELECT VAL_VALOR FROM MOVIMENTO_MANUAL", BigDecimal.class);
        assertThat(persisted).isEqualByComparingTo(expected);
    }

    @Test
    void rejectsAnUnknownProductAndAnUnlinkedCosifWithoutPersisting() throws Exception {
        postJson(movementJson(10, 2026, "9999", "10000000001", "Ajuste", "150.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("codigoProduto"));

        postJson(movementJson(10, 2026, "0001", "20000000001", "Ajuste", "150.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("codigoCosif"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM MOVIMENTO_MANUAL", Long.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "123456789012345678901234567890123456789012345678901"})
    void rejectsDescriptionsWithoutContentOrBeyondFiftyCharacters(String description) throws Exception {
        long before = movementCount();
        MvcResult result = postJson(movementJson(10, 2026, "0001", "10000000001", description, "150.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("descricao"))
                .andExpect(jsonPath("$.errors[0].message").value(description.length() > 50
                        ? "A descrição deve ter até 50 caracteres."
                        : "A descrição deve conter ao menos um caractere diferente de espaço."))
                .andReturn();
        assertProblemShape(result, 400);
        assertThat(movementCount()).isEqualTo(before);
    }

    @Test
    void reportsAllRequiredFieldsWhenTheRequestFieldsAreAbsent() throws Exception {
        MvcResult result = postJson("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.containsInAnyOrder(
                        "mes", "ano", "codigoProduto", "codigoCosif", "descricao", "valor")))
                .andReturn();
        assertProblemShape(result, 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "+150", "0", "0.00", "-0.00", "1,23", "1.234", "1,234", "1.234,56", "1,234.56", "1 234,56", "1e3",
        "1E3", "150.001", "10000000000000000.00", " 150.00", "$150.00"
    })
    void rejectsInvalidMoneyTextsOnTheApiWithoutPersisting(String value) throws Exception {
        long before = movementCount();
        postJson(movementJson(10, 2026, "0001", "10000000001", "Ajuste", value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("valor"));
        assertThat(movementCount()).isEqualTo(before);
    }

    @Test
    void rejectsInvalidMonthAndYearLimits() throws Exception {
        postJson(movementJson(0, 2026, "0001", "10000000001", "Ajuste", "150.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("mes"));
        postJson(movementJson(13, 2026, "0001", "10000000001", "Ajuste", "150.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("mes"));
        postJson(movementJson(1, 0, "0001", "10000000001", "Ajuste", "150.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("ano"));
        postJson(movementJson(1, 10000, "0001", "10000000001", "Ajuste", "150.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("ano"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "mes|\"1\"",
        "ano|2026.5",
        "codigoProduto|1",
        "codigoCosif|true",
        "descricao|{}",
        "valor|150.00"
    })
    void rejectsJsonScalarCoercionsWithTheirField(String field, String incompatibleValue) throws Exception {
        String validJson = movementJson(10, 2026, "0001", "10000000001", "Ajuste", "150.00");
        String originalValue = switch (field) {
            case "mes" -> "10";
            case "ano" -> "2026";
            case "codigoProduto" -> "\"0001\"";
            case "codigoCosif" -> "\"10000000001\"";
            case "descricao" -> "\"Ajuste\"";
            case "valor" -> "\"150.00\"";
            default -> throw new IllegalArgumentException("Campo de teste não previsto.");
        };
        String json = validJson.replace("\"" + field + "\":" + originalValue,
                "\"" + field + "\":" + incompatibleValue);
        MvcResult result = postJson(json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value(field))
                .andReturn();
        assertProblemShape(result, 400);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "mes|2147483648",
        "mes|-2147483649",
        "ano|2147483648",
        "ano|-2147483649"
    })
    void rejectsIntegerOverflowWithItsFieldAndWithoutPersisting(String field, String value) throws Exception {
        long before = movementCount();
        String validJson = movementJson(10, 2026, "0001", "10000000001", "Ajuste", "150.00");
        String json = validJson.replace("\"" + field + "\":" + (field.equals("mes") ? "10" : "2026"),
                "\"" + field + "\":" + value);

        MvcResult result = postJson(json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value(field))
                .andReturn();

        assertProblemShape(result, 400);
        assertThat(movementCount()).isEqualTo(before);
    }

    @Test
    void rejectsUnknownPropertiesMalformedJsonAndExplicitNullBody() throws Exception {
        String valid = movementJson(10, 2026, "0001", "10000000001", "Ajuste", "150.00");
        postJson(valid.substring(0, valid.length() - 1) + ",\"dataMovimento\":\"2026-01-01T00:00:00Z\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isEmpty());
        postJson("{inválido")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isEmpty());
        postJson("null")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isEmpty());
        postJson("[]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isEmpty());
        assertThat(movementCount()).isZero();
    }

    @Test
    void associatesExplicitNullFieldsWithTheirDtoFieldNames() throws Exception {
        String valid = movementJson(10, 2026, "0001", "10000000001", "Ajuste", "150.00");
        postJson(valid.replace("\"mes\":10", "\"mes\":null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("mes"));
        postJson(valid.replace("\"valor\":\"150.00\"", "\"valor\":null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("valor"));
        assertThat(movementCount()).isZero();
    }

    @Test
    void reportsSequenceExhaustionAsA409Problem() throws Exception {
        insertMovement(10, 2026, 999_999_999_999_999_999L, "0001", "10000000001", "Último lançamento");

        MvcResult result = postJson(movementJson(10, 2026, "0001", "10000000001", "Ajuste", "150.00"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito de inclusão"))
                .andExpect(jsonPath("$.errors").isEmpty())
                .andReturn();
        assertProblemShape(result, 409);
        assertThat(movementCount()).isEqualTo(1L);
    }

    @Test
    void reportsA409AfterThreeSequenceCollisionsWithoutPersisting() throws Exception {
        long before = movementCount();
        doAnswer(invocation -> {
            throw new ManualMovementSequenceCollisionException(
                    new IllegalStateException("colisão controlada para teste"));
        }).when(movementInclusionPort).insert(any());

        MvcResult result = postJson(movementJson(10, 2026, "0001", "10000000001", "Ajuste", "150.00"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito de inclusão"))
                .andExpect(jsonPath("$.errors").isEmpty())
                .andReturn();

        assertProblemShape(result, 409);
        assertThat(movementCount()).isEqualTo(before);
        verify(movementInclusionPort, times(3)).insert(any());
    }

    @Test
    void hidesUnexpectedInfrastructureMessagesInThe500Problem() throws Exception {
        doThrow(new IllegalStateException("detalhe interno sensível"))
                .when(includeManualMovementUseCase)
                .execute(any());

        MvcResult result = postJson(movementJson(10, 2026, "0001", "10000000001", "Ajuste", "150.00"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Erro interno"))
                .andExpect(jsonPath("$.detail").value("Ocorreu um erro interno ao processar a requisição."))
                .andExpect(jsonPath("$.errors").isEmpty())
                .andReturn();

        assertProblemShape(result, 500);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("detalhe interno sensível");
    }

    private org.springframework.test.web.servlet.ResultActions postJson(String json) throws Exception {
        return mockMvc.perform(post(API + "/movimentos-manuais")
                .contentType(APPLICATION_JSON)
                .content(json));
    }

    private String movementJson(
            int month,
            int year,
            String productCode,
            String cosifCode,
            String description,
            String value) {
        return "{\"mes\":" + month
                + ",\"ano\":" + year
                + ",\"codigoProduto\":\"" + productCode + "\""
                + ",\"codigoCosif\":\"" + cosifCode + "\""
                + ",\"descricao\":\"" + description + "\""
                + ",\"valor\":\"" + value + "\"}";
    }

    private long movementCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM MOVIMENTO_MANUAL", Long.class);
    }

    private void seedSevenMovements() {
        insertMovement(1, 2026, 1L, "0001", "10000000001", "Janeiro de 2026");
        insertMovement(12, 2025, 4L, "0001", "10000000001", "Dezembro quatro");
        insertMovement(12, 2025, 1L, "0002", "20000000001", "Dezembro um");
        insertMovement(11, 2025, 2L, "0001", "10000000001", "Novembro de 2025");
        insertMovement(10, 2024, 1L, "0002", "20000000001", "Outubro de 2024");
        insertMovement(9, 2024, 3L, "0001", "10000000001", "Setembro de 2024");
        insertMovement(8, 2024, 1L, "0002", "20000000001", "Agosto de 2024");
    }

    private void insertMovement(
            int month,
            int year,
            long launchNumber,
            String productCode,
            String cosifCode,
            String description) {
        jdbcTemplate.update(
                "INSERT INTO MOVIMENTO_MANUAL (DAT_MES, DAT_ANO, NUM_LANCAMENTO, COD_PRODUTO, COD_COSIF, "
                        + "DES_DESCRICAO, DAT_MOVIMENTO, COD_USUARIO, VAL_VALOR) "
                        + "VALUES (?, ?, ?, ?, ?, ?, TIMESTAMP '2026-01-01 12:00:00', ?, ?)",
                month,
                year,
                launchNumber,
                productCode,
                cosifCode,
                description,
                "TESTE",
                new BigDecimal("150.00"));
    }

    private void assertProblemShape(MvcResult result, int expectedStatus) throws Exception {
        JsonNode problem = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(problem.get("type").asText()).isEqualTo("about:blank");
        String expectedTitle = switch (expectedStatus) {
            case 400 -> "Requisição inválida";
            case 404 -> "Recurso não encontrado";
            case 409 -> "Conflito de inclusão";
            case 500 -> "Erro interno";
            default -> throw new IllegalArgumentException("Status de problema não previsto.");
        };
        assertThat(problem.get("title").asText()).isEqualTo(expectedTitle);
        assertThat(problem.get("status").isIntegralNumber()).isTrue();
        assertThat(problem.get("status").asInt()).isEqualTo(expectedStatus);
        assertThat(problem.get("detail").asText()).isNotBlank();
        assertThat(problem.get("instance").asText()).startsWith(API + "/");
        assertThat(problem.get("instance").asText()).doesNotContain("?");
        assertThat(problem.get("errors").isArray()).isTrue();
        for (JsonNode error : problem.get("errors")) {
            assertFields(error, "field", "message");
            assertThat(error.get("field").isTextual()).isTrue();
            assertThat(error.get("field").asText()).isNotBlank();
            assertThat(error.get("message").isTextual()).isTrue();
            assertThat(error.get("message").asText()).isNotBlank();
        }
        assertThat(result.getResponse().getContentType()).startsWith(APPLICATION_PROBLEM_JSON.toString());
    }

    private void assertFields(JsonNode object, String... expectedFields) {
        Set<String> actualFields = new HashSet<>();
        object.fieldNames().forEachRemaining(actualFields::add);
        assertThat(actualFields).containsExactlyInAnyOrder(expectedFields);
    }
}
