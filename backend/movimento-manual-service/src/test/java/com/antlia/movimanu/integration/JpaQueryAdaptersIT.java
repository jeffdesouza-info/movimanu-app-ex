package com.antlia.movimanu.integration;

import java.math.BigDecimal;
import java.time.Instant;

import com.antlia.movimanu.application.exception.ProductNotFoundException;
import com.antlia.movimanu.application.port.ManualMovementQueryPort;
import com.antlia.movimanu.application.port.MovementPage;
import com.antlia.movimanu.application.projection.ManualMovementProjection;
import com.antlia.movimanu.application.usecase.FindManualMovementsUseCase;
import com.antlia.movimanu.application.usecase.ListProductCosifsUseCase;
import com.antlia.movimanu.application.usecase.ListProductsUseCase;
import com.antlia.movimanu.domain.model.Product;
import com.antlia.movimanu.domain.model.ProductCosif;
import com.antlia.movimanu.infrastructure.persistence.entity.ManualMovementEntity;
import com.antlia.movimanu.infrastructure.persistence.entity.ManualMovementEntityId;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductCosifEntity;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductCosifEntityId;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@Sql("/fixtures/persistence-fixtures.sql")
class JpaQueryAdaptersIT {
    @Autowired
    private ListProductsUseCase listProductsUseCase;

    @Autowired
    private ListProductCosifsUseCase listProductCosifsUseCase;

    @Autowired
    private FindManualMovementsUseCase findManualMovementsUseCase;

    @Autowired
    private ManualMovementQueryPort manualMovementQueryPort;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void listsProductsAndDistinguishesAnExistingProductWithoutCosifsFromAMissingProduct() {
        entityManager.persist(new ProductEntity("0003", "Produto sem COSIF", "A"));
        entityManager.flush();

        assertThat(listProductsUseCase.execute())
                .extracting(Product::code)
                .containsExactly("0001", "0002", "0003");
        assertThat(listProductCosifsUseCase.execute("0001"))
                .extracting(ProductCosif::classificationCode)
                .containsExactly("100001");
        assertThat(listProductCosifsUseCase.execute("0003")).isEmpty();
        assertThatThrownBy(() -> listProductCosifsUseCase.execute("9999"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void returnsEmptyProductsAndAnEmptyFirstMovementPageWhenStorageHasNoRecords() {
        entityManager.createQuery("delete from ManualMovementEntity").executeUpdate();
        entityManager.createQuery("delete from ProductCosifEntity").executeUpdate();
        entityManager.createQuery("delete from ProductEntity").executeUpdate();

        assertThat(listProductsUseCase.execute()).isEmpty();

        MovementPage<ManualMovementProjection> page = findManualMovementsUseCase.execute(0);

        assertThat(page.content()).isEmpty();
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(6);
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
    }

    @Test
    void projectsAndOrdersMovementsAndKeepsPageMetadataForEmptyAndOutOfRangePages() {
        persistMovement(1, 2026, 1L, "0002", "20000000001", "Janeiro de 2026");
        persistMovement(12, 2025, 4L, "0001", "10000000001", "Dezembro quatro");
        persistMovement(12, 2025, 1L, "0002", "20000000001", "Dezembro um");
        persistMovement(11, 2025, 2L, "0001", "10000000001", "Novembro");
        persistMovement(10, 2024, 1L, "0002", "20000000001", "Outubro");
        persistMovement(9, 2024, 3L, "0001", "10000000001", "Setembro");
        persistMovement(8, 2024, 1L, "0002", "20000000001", "Agosto");
        entityManager.flush();
        entityManager.clear();

        MovementPage<ManualMovementProjection> firstPage = findManualMovementsUseCase.execute(0);
        MovementPage<ManualMovementProjection> secondPage = findManualMovementsUseCase.execute(1);
        MovementPage<ManualMovementProjection> beyondResults = findManualMovementsUseCase.execute(2);

        assertThat(firstPage.content())
                .extracting(movement -> movement.year() + "-" + movement.month() + "-" + movement.launchNumber())
                .containsExactly(
                        "2026-1-1",
                        "2025-12-4",
                        "2025-12-1",
                        "2025-11-2",
                        "2024-10-1",
                        "2024-9-3");
        assertThat(firstPage.content().getFirst().productDescription()).isEqualTo("Produto de fixture B");
        assertThat(firstPage.content()).hasSize(6);
        assertThat(firstPage.size()).isEqualTo(6);
        assertThat(firstPage.totalElements()).isEqualTo(7);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.content()).hasSize(1);
        assertThat(secondPage.content().getFirst().description()).isEqualTo("Agosto");
        assertThat(beyondResults.content()).isEmpty();
        assertThat(beyondResults.page()).isEqualTo(2);
        assertThat(beyondResults.size()).isEqualTo(6);
        assertThat(beyondResults.totalElements()).isEqualTo(7);
        assertThat(beyondResults.totalPages()).isEqualTo(2);
    }

    @Test
    void returnsEmptyPagesWithRealTotalsWhenTheLongOffsetExceedsIntegerMax() {
        persistMovement(1, 2026, 1L, "0002", "20000000001", "Janeiro de 2026");
        persistMovement(12, 2025, 4L, "0001", "10000000001", "Dezembro quatro");
        persistMovement(12, 2025, 1L, "0002", "20000000001", "Dezembro um");
        persistMovement(11, 2025, 2L, "0001", "10000000001", "Novembro");
        persistMovement(10, 2024, 1L, "0002", "20000000001", "Outubro");
        persistMovement(9, 2024, 3L, "0001", "10000000001", "Setembro");
        persistMovement(8, 2024, 1L, "0002", "20000000001", "Agosto");
        entityManager.flush();

        MovementPage<ManualMovementProjection> beyondIntegerOffset =
                findManualMovementsUseCase.execute(357913942);
        MovementPage<ManualMovementProjection> maximumPage =
                findManualMovementsUseCase.execute(Integer.MAX_VALUE);

        assertEmptyPageWithSevenRealResults(beyondIntegerOffset, 357913942);
        assertEmptyPageWithSevenRealResults(maximumPage, Integer.MAX_VALUE);
    }

    @Test
    void returnsTheHighestLaunchNumberForAPeriodAndNoValueForAnEmptyPeriod() {
        persistMovement(12, 2025, 4L, "0001", "10000000001", "Quatro");
        persistMovement(12, 2025, 1L, "0002", "20000000001", "Um");

        assertThat(manualMovementQueryPort.findHighestNumberFor(12, 2025)).contains(4L);
        assertThat(manualMovementQueryPort.findHighestNumberFor(1, 2026)).isEmpty();
    }

    private void persistMovement(
            int month,
            int year,
            Long launchNumber,
            String productCode,
            String cosifCode,
            String description) {
        ProductCosifEntity productCosif = entityManager.find(
                ProductCosifEntity.class,
                new ProductCosifEntityId(productCode, cosifCode));
        entityManager.persist(new ManualMovementEntity(
                new ManualMovementEntityId(month, year, launchNumber, productCode, cosifCode),
                description,
                Instant.parse("2026-01-01T12:00:00Z"),
                "TESTE",
                new BigDecimal("150.00"),
                productCosif));
    }

    private void assertEmptyPageWithSevenRealResults(
            MovementPage<ManualMovementProjection> page,
            int requestedPage) {
        assertThat((long) requestedPage * MovementPage.SIZE).isGreaterThan(Integer.MAX_VALUE);
        assertThat(page.content()).isEmpty();
        assertThat(page.page()).isEqualTo(requestedPage);
        assertThat(page.size()).isEqualTo(6);
        assertThat(page.totalElements()).isEqualTo(7);
        assertThat(page.totalPages()).isEqualTo(2);
    }
}
