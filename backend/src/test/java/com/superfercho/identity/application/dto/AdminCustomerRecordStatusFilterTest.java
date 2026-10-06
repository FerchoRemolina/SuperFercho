package com.superfercho.identity.application.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.identity.application.exception.InvalidAdminCustomerQueryException;
import org.junit.jupiter.api.Test;

class AdminCustomerRecordStatusFilterTest {

    @Test
    void shouldParseKnownValuesAndDefaultBlankToAll() {
        assertThat(AdminCustomerRecordStatusFilter.parse(null))
                .isEqualTo(AdminCustomerRecordStatusFilter.ALL);
        assertThat(AdminCustomerRecordStatusFilter.parse(" "))
                .isEqualTo(AdminCustomerRecordStatusFilter.ALL);
        assertThat(AdminCustomerRecordStatusFilter.parse("active"))
                .isEqualTo(AdminCustomerRecordStatusFilter.ACTIVE);
        assertThat(AdminCustomerRecordStatusFilter.parse("INACTIVE"))
                .isEqualTo(AdminCustomerRecordStatusFilter.INACTIVE);
        assertThat(AdminCustomerRecordStatusFilter.parse("CLOSED"))
                .isEqualTo(AdminCustomerRecordStatusFilter.CLOSED);
        assertThat(AdminCustomerRecordStatusFilter.parse("NO_ACCOUNT"))
                .isEqualTo(AdminCustomerRecordStatusFilter.NO_ACCOUNT);
    }

    @Test
    void shouldRejectDeletedAndUnknownValues() {
        assertThatThrownBy(() -> AdminCustomerRecordStatusFilter.parse("DELETED"))
                .isInstanceOf(InvalidAdminCustomerQueryException.class)
                .hasMessageContaining("CLOSED");
        assertThatThrownBy(() -> AdminCustomerRecordStatusFilter.parse("UNKNOWN"))
                .isInstanceOf(InvalidAdminCustomerQueryException.class);
    }
}
