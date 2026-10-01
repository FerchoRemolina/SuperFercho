package com.superfercho.identity.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.superfercho.identity.application.dto.AdminNewCustomersResult;
import com.superfercho.identity.application.dto.GetAdminNewCustomersCommand;
import com.superfercho.identity.application.exception.InvalidAdminCustomerQueryException;
import com.superfercho.identity.application.fakes.InMemoryCustomerRecordRepository;
import com.superfercho.identity.domain.model.CustomerRecord;
import com.superfercho.platform.time.BucketGranularity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetAdminNewCustomersUseCaseTest {

    private static final Instant MAY_START = Instant.parse("2026-05-01T05:00:00Z");
    private static final Instant JUNE_START = Instant.parse("2026-06-01T05:00:00Z");

    private InMemoryCustomerRecordRepository repository;
    private GetAdminNewCustomersUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCustomerRecordRepository();
        useCase = new GetAdminNewCustomersUseCase(repository);
    }

    @Test
    void shouldCountNewCustomersByRegistrationInstantWithinBogotaMonth() {
        seed("r1", "2026-05-01T02:00:00Z"); // 30 abr 21:00 Bogota -> fuera del período
        seed("r2", "2026-05-01T05:00:00Z"); // from inclusivo
        seed("r3", "2026-05-15T20:00:00Z");
        seed("r4", "2026-06-01T04:59:59Z"); // 31 may 23:59 Bogota -> dentro
        seed("r5", "2026-06-01T05:00:00Z"); // to exclusivo -> fuera

        AdminNewCustomersResult result = useCase.execute(
                GetAdminNewCustomersCommand.of("2026-05-01T00:00:00", "2026-06-01T00:00:00", "MONTH"));

        assertEquals(3, result.total());
        assertEquals(1, result.buckets().size());
        assertEquals(MAY_START, result.buckets().get(0).periodStart());
        assertEquals(3, result.buckets().get(0).count());
    }

    @Test
    void shouldFillDailyBucketsInBogotaIncludingPreviousDayForLateUtcRegistrations() {
        seed("r1", "2026-05-01T02:00:00Z"); // 30 abr en Bogota
        seed("r2", "2026-05-01T05:00:00Z"); // 1 may 00:00 Bogota

        AdminNewCustomersResult result = useCase.execute(
                GetAdminNewCustomersCommand.of("2026-04-30T00:00:00", "2026-05-02T00:00:00", "DAY"));

        assertEquals(2, result.total());
        assertEquals(2, result.buckets().size());
        assertEquals(Instant.parse("2026-04-30T05:00:00Z"), result.buckets().get(0).periodStart());
        assertEquals(1, result.buckets().get(0).count());
        assertEquals(Instant.parse("2026-05-01T05:00:00Z"), result.buckets().get(1).periodStart());
        assertEquals(1, result.buckets().get(1).count());
    }

    @Test
    void shouldDefaultGranularityToDayAndParseNaiveDatesAsBogota() {
        GetAdminNewCustomersCommand command =
                GetAdminNewCustomersCommand.of("2026-05-01T00:00:00", "2026-06-01T00:00:00", null);
        seed("r1", "2026-05-10T12:00:00Z");

        AdminNewCustomersResult result = useCase.execute(command);

        assertEquals(BucketGranularity.DAY, command.granularity());
        assertEquals(1, result.total());
        assertEquals(31, result.buckets().size());
        assertEquals(1, result.buckets().stream().filter(bucket -> bucket.count() == 1).count());
        assertEquals(
                Instant.parse("2026-05-10T05:00:00Z"),
                result.buckets().stream()
                        .filter(bucket -> bucket.count() == 1)
                        .findFirst()
                        .orElseThrow()
                        .periodStart());
    }

    @Test
    void shouldRejectMissingSwappedOversizedOrUnknownPeriods() {
        assertThrows(
                InvalidAdminCustomerQueryException.class,
                () -> GetAdminNewCustomersCommand.of(null, "2026-06-01", "DAY"));
        assertThrows(
                InvalidAdminCustomerQueryException.class,
                () -> GetAdminNewCustomersCommand.of("2026-06-01", "2026-05-01", "DAY"));
        assertThrows(
                InvalidAdminCustomerQueryException.class,
                () -> GetAdminNewCustomersCommand.of("2026-05-01", "2026-06-01", "QUARTER"));
        assertThrows(
                InvalidAdminCustomerQueryException.class,
                () -> GetAdminNewCustomersCommand.of("2020-01-01", "2026-06-01", "DAY"));
        assertThrows(
                InvalidAdminCustomerQueryException.class,
                () -> GetAdminNewCustomersCommand.of("not-a-date", "2026-06-01", "DAY"));
    }

    private void seed(String documentSuffix, String createdAt) {
        Instant instant = Instant.parse(createdAt);
        repository.save(CustomerRecord.create(
                UUID.randomUUID(),
                "CC",
                "DOC-" + documentSuffix,
                "Ada",
                "Lovelace",
                instant,
                instant));
    }
}
