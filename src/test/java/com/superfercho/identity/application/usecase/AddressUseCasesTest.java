package com.superfercho.identity.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.identity.application.dto.AddAddressCommand;
import com.superfercho.identity.application.dto.AddressResult;
import com.superfercho.identity.application.dto.DeactivateAddressCommand;
import com.superfercho.identity.application.dto.SetDefaultAddressCommand;
import com.superfercho.identity.application.dto.UpdateAddressCommand;
import com.superfercho.identity.application.exception.AddressOwnershipException;
import com.superfercho.identity.application.exception.InactiveAddressException;
import com.superfercho.identity.application.exception.UserNotFoundException;
import com.superfercho.identity.application.fakes.InMemoryAddressRepository;
import com.superfercho.identity.application.fakes.InMemoryUserRepository;
import com.superfercho.identity.domain.model.Address;
import com.superfercho.identity.domain.model.AddressStatus;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AddressUseCasesTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-15T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-15T12:30:00Z");
    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER_USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private InMemoryUserRepository users;
    private InMemoryAddressRepository addresses;
    private AddAddressUseCase addAddress;
    private UpdateAddressUseCase updateAddress;
    private DeactivateAddressUseCase deactivateAddress;
    private SetDefaultAddressUseCase setDefaultAddress;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        addresses = new InMemoryAddressRepository();
        users.save(user(USER_ID, "owner@example.com", "1001"));
        users.save(user(OTHER_USER_ID, "other@example.com", "2002"));
        addAddress = new AddAddressUseCase(users, addresses, Clock.fixed(CREATED_AT, ZoneOffset.UTC));
        Clock later = Clock.fixed(UPDATED_AT, ZoneOffset.UTC);
        updateAddress = new UpdateAddressUseCase(addresses, later);
        deactivateAddress = new DeactivateAddressUseCase(addresses, later);
        setDefaultAddress = new SetDefaultAddressUseCase(addresses, later);
    }

    @Test
    void shouldAddAddressSuccessfully() {
        AddressResult result = addAddress.execute(addCommand(USER_ID, "Casa", false));

        assertEquals("Casa", result.label());
        assertEquals("Ada Lovelace", result.recipientName());
        assertEquals("Calle 1 # 2-3", result.addressLine());
        assertEquals("Bogotá", result.city());
        assertEquals("Cundinamarca", result.department());
        assertEquals("3001234567", result.phone());
        assertFalse(result.isDefault());
        assertEquals(AddressStatus.ACTIVE, result.status());
        assertEquals(CREATED_AT, result.createdAt());
        assertEquals(USER_ID, addresses.findOwnedById(result.id()).orElseThrow().userId());
    }

    @Test
    void shouldRejectAddAddressWhenUserDoesNotExist() {
        UUID missingUserId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

        assertThrows(
                UserNotFoundException.class, () -> addAddress.execute(addCommand(missingUserId, "Casa", false)));
    }

    @Test
    void shouldUpdateAddressSuccessfully() {
        AddressResult created = addAddress.execute(addCommand(USER_ID, "Casa", true));

        AddressResult updated = updateAddress.execute(updateCommand(USER_ID, created.id(), "Oficina"));

        assertEquals(created.id(), updated.id());
        assertEquals("Oficina", updated.label());
        assertEquals("Ada Lovelace", updated.recipientName());
        assertTrue(updated.isDefault());
        assertEquals(CREATED_AT, updated.createdAt());
        assertEquals(UPDATED_AT, updated.updatedAt());
        assertEquals(USER_ID, addresses.findOwnedById(updated.id()).orElseThrow().userId());
    }

    @Test
    void shouldRejectAddressUpdateWhenAddressBelongsToAnotherUser() {
        AddressResult created = addAddress.execute(addCommand(USER_ID, "Casa", false));

        assertThrows(
                AddressOwnershipException.class,
                () -> updateAddress.execute(updateCommand(OTHER_USER_ID, created.id(), "Oficina")));
    }

    @Test
    void shouldRejectAddressUpdateWhenAddressIsInactive() {
        AddressResult created = addAddress.execute(addCommand(USER_ID, "Casa", false));
        deactivateAddress.execute(new DeactivateAddressCommand(USER_ID, created.id()));

        assertThrows(
                InactiveAddressException.class,
                () -> updateAddress.execute(updateCommand(USER_ID, created.id(), "Oficina")));
    }

    @Test
    void shouldDeactivateAddressAndClearDefaultFlag() {
        AddressResult created = addAddress.execute(addCommand(USER_ID, "Casa", true));

        AddressResult deactivated =
                deactivateAddress.execute(new DeactivateAddressCommand(USER_ID, created.id()));

        assertEquals(AddressStatus.INACTIVE, deactivated.status());
        assertFalse(deactivated.isDefault());
        assertEquals(created.id(), deactivated.id());
        assertEquals(UPDATED_AT, deactivated.updatedAt());
    }

    @Test
    void shouldSetDefaultAddress() {
        AddressResult first = addAddress.execute(addCommand(USER_ID, "Casa", true));
        AddressResult second = addAddress.execute(addCommand(USER_ID, "Oficina", false));

        AddressResult result =
                setDefaultAddress.execute(new SetDefaultAddressCommand(USER_ID, second.id()));

        assertTrue(result.isDefault());
        assertEquals(second.id(), result.id());
        Address previous = addresses.findById(first.id()).orElseThrow();
        assertFalse(previous.isDefault());
        assertEquals(AddressStatus.ACTIVE, previous.status());
    }

    @Test
    void shouldRejectSetDefaultWhenAddressBelongsToAnotherUser() {
        AddressResult created = addAddress.execute(addCommand(USER_ID, "Casa", false));

        assertThrows(
                AddressOwnershipException.class,
                () -> setDefaultAddress.execute(new SetDefaultAddressCommand(OTHER_USER_ID, created.id())));
    }

    @Test
    void shouldRejectSetDefaultWhenAddressIsInactive() {
        AddressResult created = addAddress.execute(addCommand(USER_ID, "Casa", false));
        deactivateAddress.execute(new DeactivateAddressCommand(USER_ID, created.id()));

        assertThrows(
                InactiveAddressException.class,
                () -> setDefaultAddress.execute(new SetDefaultAddressCommand(USER_ID, created.id())));
    }

    @Test
    void shouldNotChangeOwnershipWhenUpdatingAddress() {
        AddressResult created = addAddress.execute(addCommand(USER_ID, "Casa", false));

        updateAddress.execute(updateCommand(USER_ID, created.id(), "Oficina"));

        assertNotEquals(OTHER_USER_ID, addresses.findOwnedById(created.id()).orElseThrow().userId());
        assertEquals(USER_ID, addresses.findOwnedById(created.id()).orElseThrow().userId());
    }

    private static AddAddressCommand addCommand(UUID userId, String label, boolean isDefault) {
        return new AddAddressCommand(
                userId,
                label,
                "Ada Lovelace",
                "Calle 1 # 2-3",
                "Apto 101",
                "Bogotá",
                "Cundinamarca",
                "3001234567",
                isDefault);
    }

    private static UpdateAddressCommand updateCommand(UUID userId, UUID addressId, String label) {
        return new UpdateAddressCommand(
                userId,
                addressId,
                label,
                "Ada Lovelace",
                "Calle 1 # 2-3",
                "Apto 101",
                "Bogotá",
                "Cundinamarca",
                "3001234567");
    }

    private static User user(UUID id, String email, String documentNumber) {
        return User.create(
                id,
                "CC",
                documentNumber,
                "Ada Lovelace",
                email,
                "3001234567",
                "hashed:secret",
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                CREATED_AT,
                CREATED_AT);
    }
}
