package com.superfercho.identity.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminCustomerRecordStatusTest {

    @Test
    void shouldDeriveActiveWhenAnyLiveActiveUserExists() {
        assertThat(AdminCustomerRecordStatus.derive(1, 0, 0))
                .isEqualTo(AdminCustomerRecordStatus.ACTIVE);
        assertThat(AdminCustomerRecordStatus.derive(1, 2, 3))
                .isEqualTo(AdminCustomerRecordStatus.ACTIVE);
    }

    @Test
    void shouldDeriveInactiveWhenOnlyLiveInactiveUsersExist() {
        assertThat(AdminCustomerRecordStatus.derive(0, 1, 0))
                .isEqualTo(AdminCustomerRecordStatus.INACTIVE);
        assertThat(AdminCustomerRecordStatus.derive(0, 1, 2))
                .isEqualTo(AdminCustomerRecordStatus.INACTIVE);
    }

    @Test
    void shouldDeriveClosedWhenOnlyDeletedUsersExist() {
        assertThat(AdminCustomerRecordStatus.derive(0, 0, 1))
                .isEqualTo(AdminCustomerRecordStatus.CLOSED);
    }

    @Test
    void shouldDeriveNoAccountWhenNoUsersAreLinked() {
        assertThat(AdminCustomerRecordStatus.derive(0, 0, 0))
                .isEqualTo(AdminCustomerRecordStatus.NO_ACCOUNT);
    }
}
