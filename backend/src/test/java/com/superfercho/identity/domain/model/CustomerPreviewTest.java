package com.superfercho.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.identity.domain.exception.InvalidCustomerPreviewException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerPreviewTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T12:00:00Z");

    @Test
    void startSetsExpiresAtTwentyMinutesAfterCreatedAt() {
        UUID adminId = UUID.randomUUID();
        UUID tempId = UUID.randomUUID();

        CustomerPreview preview = CustomerPreview.start(UUID.randomUUID(), adminId, tempId, CREATED);

        assertThat(preview.status()).isEqualTo(CustomerPreviewStatus.ACTIVE);
        assertThat(preview.expiresAt()).isEqualTo(CREATED.plus(CustomerPreview.LIFETIME));
        assertThat(preview.closedAt()).isNull();
        assertThat(preview.isUsable(CREATED.plusSeconds(60))).isTrue();
        assertThat(preview.isUsable(CREATED.plus(CustomerPreview.LIFETIME))).isFalse();
    }

    @Test
    void closeIsIdempotent() {
        CustomerPreview preview =
                CustomerPreview.start(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), CREATED);
        Instant closedAt = CREATED.plusSeconds(30);

        CustomerPreview closed = preview.close(closedAt);
        CustomerPreview again = closed.close(closedAt.plusSeconds(10));

        assertThat(closed.status()).isEqualTo(CustomerPreviewStatus.CLOSED);
        assertThat(closed.closedAt()).isEqualTo(closedAt);
        assertThat(again.closedAt()).isEqualTo(closedAt);
    }

    @Test
    void rejectsSameAdminAndTemporaryIds() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> CustomerPreview.start(UUID.randomUUID(), id, id, CREATED))
                .isInstanceOf(InvalidCustomerPreviewException.class);
    }
}
