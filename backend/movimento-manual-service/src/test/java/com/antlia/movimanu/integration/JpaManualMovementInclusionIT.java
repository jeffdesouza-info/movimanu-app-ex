package com.antlia.movimanu.integration;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Optional;

import com.antlia.movimanu.application.port.ManualMovementInclusionPort;
import com.antlia.movimanu.application.port.ManualMovementQueryPort;
import com.antlia.movimanu.infrastructure.persistence.repository.ManualMovementJpaRepository;
import com.antlia.movimanu.application.exception.ManualMovementSequenceConflictException;
import com.antlia.movimanu.application.exception.ManualMovementSequenceCollisionException;
import com.antlia.movimanu.application.exception.ProductCosifNotFoundException;
import com.antlia.movimanu.application.usecase.IncludeManualMovementUseCase;
import com.antlia.movimanu.domain.model.ManualMovement;
import com.antlia.movimanu.domain.model.MovementDraft;
import com.antlia.movimanu.infrastructure.persistence.entity.ManualMovementEntity;
import com.antlia.movimanu.infrastructure.persistence.entity.ManualMovementEntityId;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductCosifEntity;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductCosifEntityId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Sql("/fixtures/persistence-fixtures.sql")
class JpaManualMovementInclusionIT {
    @Autowired
    private IncludeManualMovementUseCase includeManualMovementUseCase;

    @Autowired
    private Clock movementClock;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ManualMovementJpaRepository movementRepository;

    @MockitoSpyBean
    private ManualMovementQueryPort movementQueryPort;

    @MockitoSpyBean
    private ManualMovementInclusionPort movementInclusionPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearsFixtureDataForOtherIntegrationTests() {
        jdbcTemplate.update("DELETE FROM MOVIMENTO_MANUAL");
        jdbcTemplate.update("DELETE FROM PRODUTO_COSIF");
        jdbcTemplate.update("DELETE FROM PRODUTO");
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void generatesTheFirstNumberAndPersistsTheUtcTimeAndFixedUser() {
        Instant before = Instant.now();

        ManualMovement included = includeManualMovementUseCase.execute(draft(10, 2026, "0001"));

        Instant after = Instant.now();
        assertThat(movementClock.getZone()).isEqualTo(java.time.ZoneOffset.UTC);
        assertThat(included.id().launchNumber()).isEqualTo(1L);
        assertThat(included.movementAt()).isBetween(before.minusSeconds(60), after.plusSeconds(60));
        assertThat(included.userCode()).isEqualTo("TESTE");
        assertThat(included.value()).isEqualByComparingTo("150.00");
        assertThat(included.description()).isEqualTo("Ajuste manual");
    }

    @Test
    void continuesAfterTheHighestNumberAcrossProductsAndStartsAtOneInAnotherPeriod() {
        persistMovement(7, 2025, 1L, "0001", "10000000001");
        persistMovement(7, 2025, 3L, "0001", "10000000001");

        ManualMovement afterGap = includeManualMovementUseCase.execute(draft(7, 2025, "0002"));
        ManualMovement otherPeriod = includeManualMovementUseCase.execute(draft(8, 2025, "0001"));

        assertThat(afterGap.id().launchNumber()).isEqualTo(4L);
        assertThat(otherPeriod.id().launchNumber()).isEqualTo(1L);
    }

    @Test
    void rejectsAnInvalidProductCosifPairWithoutPersisting() {
        long previousCount = countMovements();

        assertThatThrownBy(() -> includeManualMovementUseCase.execute(
                new MovementDraft(10, 2026, "9999", "99999999999", "Ajuste manual", new BigDecimal("150.00"))))
                .isInstanceOf(ProductCosifNotFoundException.class);

        assertThat(countMovements()).isEqualTo(previousCount);
    }

    @Test
    void rejectsAnIncrementBeyondTheEighteenDigitLimitWithoutPersisting() {
        persistMovement(9, 2025, 999_999_999_999_999_999L, "0001", "10000000001");
        long previousCount = countMovements();

        assertThatThrownBy(() -> includeManualMovementUseCase.execute(draft(9, 2025, "0002")))
                .isInstanceOf(ManualMovementSequenceConflictException.class);

        assertThat(countMovements()).isEqualTo(previousCount);
    }

    @Test
    @Timeout(value = 20)
    void assignsDistinctNumbersToConcurrentInclusionsInAnEmptyPeriod() throws Exception {
        List<Long> numbers = includeConcurrently(5, 2026, List.of("0001", "0002"));

        assertThat(numbers).containsExactly(1L, 2L);
        assertThat(countForPeriod(5, 2026)).isEqualTo(2L);
    }

    @Test
    @Timeout(value = 20)
    void assignsNumbersAfterTheExistingMaximumToConcurrentInclusions() throws Exception {
        persistMovement(4, 2026, 1L, "0001", "10000000001");
        persistMovement(4, 2026, 3L, "0001", "10000000001");

        List<Long> numbers = includeConcurrently(4, 2026, List.of("0001", "0002"));

        assertThat(numbers).containsExactly(4L, 5L);
        assertThat(countForPeriod(4, 2026)).isEqualTo(4L);
    }

    @Test
    @Timeout(value = 20)
    void preservesBothPayloadsWhenTheSecondInsertFollowsTheFirstCommitInAnEmptyPeriod() throws Exception {
        List<Optional<Long>> observedMaximums = includeAfterSameMaximumIsRead(5, 2026, Optional.empty());

        assertThat(observedMaximums).containsExactly(Optional.empty(), Optional.empty(), Optional.of(1L));
        assertThat(persistedForPeriod(5, 2026))
                .extracting(ManualMovementEntity::getId)
                .extracting(ManualMovementEntityId::getLaunchNumber)
                .containsExactly(1L, 2L);
        assertDistinctPayloadsArePreserved(5, 2026, 1L, 2L);
    }

    @Test
    @Timeout(value = 20)
    void preservesBothPayloadsWhenTheSecondInsertFollowsTheFirstCommitInAPopulatedPeriod() throws Exception {
        persistMovement(6, 2026, 1L, "0001", "10000000001");
        persistMovement(6, 2026, 3L, "0001", "10000000001");

        List<Optional<Long>> observedMaximums = includeAfterSameMaximumIsRead(6, 2026, Optional.of(3L));

        assertThat(observedMaximums).containsExactly(Optional.of(3L), Optional.of(3L), Optional.of(4L));
        assertThat(persistedForPeriod(6, 2026))
                .extracting(ManualMovementEntity::getId)
                .extracting(ManualMovementEntityId::getLaunchNumber)
                .containsExactly(1L, 3L, 4L, 5L);
        assertDistinctPayloadsArePreserved(6, 2026, 4L, 5L);
    }

    private List<Optional<Long>> includeAfterSameMaximumIsRead(
            int month,
            int year,
            Optional<Long> expectedInitialMaximum) throws Exception {
        CountDownLatch bothInitialReads = new CountDownLatch(2);
        CountDownLatch firstCommitted = new CountDownLatch(1);
        AtomicInteger readCount = new AtomicInteger();
        List<Optional<Long>> observedMaximums = new CopyOnWriteArrayList<>();
        List<Long> observedCounts = new CopyOnWriteArrayList<>();
        List<String> collisionConstraintNames = new CopyOnWriteArrayList<>();

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Optional<Long> highest = (Optional<Long>) invocation.callRealMethod();
            observedMaximums.add(highest);
            observedCounts.add(countForPeriod(month, year));
            if (readCount.incrementAndGet() <= 2) {
                bothInitialReads.countDown();
                if (!bothInitialReads.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("As duas inclusões não leram o máximo em conjunto.");
                }
            }
            return highest;
        }).when(movementQueryPort).findHighestNumberFor(month, year);

        doAnswer(invocation -> {
            ManualMovement movement = invocation.getArgument(0);
            if (movement.description().equals("Inclusão B")) {
                if (!firstCommitted.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("A segunda gravação avançou antes do commit da primeira.");
                }
            }
            try {
                return invocation.callRealMethod();
            } catch (ManualMovementSequenceCollisionException collision) {
                collisionConstraintNames.add(constraintName(collision));
                throw collision;
            }
        }).when(movementInclusionPort).insert(any());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ManualMovement> first = executor.submit(() -> {
                try {
                    return includeManualMovementUseCase.execute(draft(month, year, "0001", "Inclusão A", "111.11"));
                } finally {
                    firstCommitted.countDown();
                }
            });
            Future<ManualMovement> second = executor.submit(() -> includeManualMovementUseCase.execute(
                    draft(month, year, "0001", "Inclusão B", "222.22")));

            ManualMovement includedA = getResult(first);
            ManualMovement includedB = getResult(second);
            assertThat(includedA.id().launchNumber()).isNotEqualTo(includedB.id().launchNumber());
            assertThat(observedMaximums.subList(0, 2)).containsExactly(expectedInitialMaximum, expectedInitialMaximum);
            long initialCount = expectedInitialMaximum.isPresent() ? 2L : 0L;
            assertThat(observedCounts).containsExactly(initialCount, initialCount, initialCount + 1);
            assertThat(collisionConstraintNames).hasSize(1).allSatisfy(name ->
                    assertThat(name).matches(
                            "(?i)(?:[^.]+\\.)?PK_MOVIMENTO_MANUAL INDEX (?:[^.]+\\.)?PRIMARY_KEY_\\d+"));
            return List.copyOf(observedMaximums);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void assertDistinctPayloadsArePreserved(int month, int year, long firstNumber, long secondNumber) {
        List<ManualMovementEntity> included = persistedForPeriod(month, year).stream()
                .filter(movement -> movement.getId().getLaunchNumber() == firstNumber
                        || movement.getId().getLaunchNumber() == secondNumber)
                .toList();

        assertThat(included)
                .extracting(ManualMovementEntity::getDescription)
                .containsExactly("Inclusão A", "Inclusão B");
        assertThat(included)
                .extracting(ManualMovementEntity::getValue)
                .containsExactly(new BigDecimal("111.11"), new BigDecimal("222.22"));
        assertThat(included)
                .allSatisfy(movement -> {
                    assertThat(movement.getId().getProductCode()).isEqualTo("0001");
                    assertThat(movement.getId().getCosifCode()).isEqualTo("10000000001");
                    assertThat(movement.getUserCode()).isEqualTo("TESTE");
                });
    }

    private List<ManualMovementEntity> persistedForPeriod(int month, int year) {
        return movementRepository.findAll().stream()
                .filter(movement -> movement.getId().getMonth() == month && movement.getId().getYear() == year)
                .sorted(Comparator.comparing(movement -> movement.getId().getLaunchNumber()))
                .toList();
    }

    private String constraintName(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
            cause = cause.getCause();
        }
        return null;
    }

    private List<Long> includeConcurrently(int month, int year, List<String> productCodes) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(productCodes.size());
        CountDownLatch ready = new CountDownLatch(productCodes.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ManualMovement>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < productCodes.size(); index++) {
                String productCode = productCodes.get(index);
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("A barreira de início não foi liberada.");
                    }
                    return includeManualMovementUseCase.execute(draft(month, year, productCode));
                }));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return futures.stream()
                    .map(future -> getResult(future).id().launchNumber())
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private ManualMovement getResult(Future<ManualMovement> future) {
        try {
            return future.get(15, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new AssertionError("A inclusão concorrente falhou.", exception);
        }
    }

    private void persistMovement(
            int month,
            int year,
            long launchNumber,
            String productCode,
            String cosifCode) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            ProductCosifEntity cosif = entityManager.find(
                    ProductCosifEntity.class,
                    new ProductCosifEntityId(productCode, cosifCode));
            entityManager.persist(new ManualMovementEntity(
                    new ManualMovementEntityId(month, year, launchNumber, productCode, cosifCode),
                    "Movimento existente",
                Instant.parse("2025-01-01T12:00:00Z"),
                    "TESTE",
                    new BigDecimal("1.00"),
                    cosif));
        });
    }

    private long countMovements() {
        return movementRepository.count();
    }

    private long countForPeriod(int month, int year) {
        return movementRepository.findAll().stream()
                .filter(movement -> movement.getId().getMonth() == month && movement.getId().getYear() == year)
                .count();
    }

    private MovementDraft draft(int month, int year, String productCode) {
        return draft(month, year, productCode, "Ajuste manual", "150.00");
    }

    private MovementDraft draft(int month, int year, String productCode, String description, String value) {
        String cosifCode = "0001".equals(productCode) ? "10000000001" : "20000000001";
        return new MovementDraft(
                month,
                year,
                productCode,
                cosifCode,
                description,
                new BigDecimal(value));
    }
}
