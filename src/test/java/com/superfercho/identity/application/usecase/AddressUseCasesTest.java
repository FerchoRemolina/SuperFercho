package com.superfercho.identity.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.identity.application.dto.AddAddressCommand;
import com.superfercho.identity.application.dto.AddressResult;
import com.superfercho.identity.application.dto.DeactivateAddressCommand;
import com.superfercho.identity.application.dto.SetDefaultAddressCommand;
import com.superfercho.identity.application.dto.UpdateAddressCommand;
import com.superfercho.identity.application.exception.AddressNotFoundException;
import com.superfercho.identity.application.exception.AddressOwnershipException;
import com.superfercho.identity.application.exception.InactiveAddressException;
import com.superfercho.identity.application.exception.UnauthenticatedUserException;
import com.superfercho.identity.application.exception.UserNotFoundException;
import com.superfercho.identity.application.fakes.InMemoryAddressRepository;
import com.superfercho.identity.application.fakes.InMemoryUserRepository;
import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.identity.domain.model.Address;
import com.superfercho.identity.domain.model.AddressStatus;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddressUseCasesTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-15T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-15T12:30:00Z");
    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER_USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    private CurrentUserProvider currentUserProvider;

    private InMemoryUserRepository users;
    private InMemoryAddressRepository addresses;
    private AddAddressUseCase addAddress;
    private ListAddressesUseCase listAddresses;
    private UpdateAddressUseCase updateAddress;
    private DeactivateAddressUseCase deactivateAddress;
    private SetDefaultAddressUseCase setDefaultAddress;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        addresses = new InMemoryAddressRepository();
        users.save(user(USER_ID, "owner@example.com", "1001"));
        users.save(user(OTHER_USER_ID, "other@example.com", "2002"));
        addAddress = new AddAddressUseCase(
                currentUserProvider, users, addresses, Clock.fixed(CREATED_AT, ZoneOffset.UTC));
        listAddresses = new ListAddressesUseCase(currentUserProvider, users, addresses);
        Clock later = Clock.fixed(UPDATED_AT, ZoneOffset.UTC);
        updateAddress = new UpdateAddressUseCase(currentUserProvider, addresses, later);
        deactivateAddress = new DeactivateAddressUseCase(currentUserProvider, addresses, later);
        setDefaultAddress = new SetDefaultAddressUseCase(currentUserProvider, addresses, later);
    }

    @Test
    void shouldAddAddressForCurrentUserFromProvider() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);

        AddressResult result = addAddress.execute(addCommand("Casa", false));

        verify(currentUserProvider).getCurrentUserId();
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
    void shouldRejectAddAddressWhenCurrentUserDoesNotExist() {
        UUID missingUserId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        when(currentUserProvider.getCurrentUserId()).thenReturn(missingUserId);

        assertThrows(UserNotFoundException.class, () -> addAddress.execute(addCommand("Casa", false)));
    }

    @Test
    void shouldClearPreviousDefaultWhenAddingAnotherDefaultAddress() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        AddressResult first = addAddress.execute(addCommand("Casa", true));

        AddressResult second = addAddress.execute(addCommand("Oficina", true));

        assertTrue(second.isDefault());
        assertFalse(addresses.findById(first.id()).orElseThrow().isDefault());
        assertTrue(addresses.findById(second.id()).orElseThrow().isDefault());
        assertEquals(AddressStatus.ACTIVE, addresses.findById(first.id()).orElseThrow().status());
    }

    @Test
    void shouldListAddressesForCurrentUserFromProvider() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        AddressResult first = addAddress.execute(addCommand("Casa", true));
        AddressResult second = addAddress.execute(addCommand("Oficina", false));

        List<AddressResult> result = listAddresses.execute();

        verify(currentUserProvider, atLeastOnce()).getCurrentUserId();
        assertEquals(2, result.size());
        assertEquals(first.id(), result.get(0).id());
        assertEquals(second.id(), result.get(1).id());
        assertEquals("Casa", result.get(0).label());
        assertEquals("Oficina", result.get(1).label());
    }

    @Test
    void shouldReturnEmptyListWhenCurrentUserHasNoAddresses() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);

        List<AddressResult> result = listAddresses.execute();

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectListAddressesWhenCurrentUserDoesNotExist() {
        UUID missingUserId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        when(currentUserProvider.getCurrentUserId()).thenReturn(missingUserId);

        assertThrows(UserNotFoundException.class, listAddresses::execute);
    }

    @Test
    void shouldNotIncludeAddressesBelongingToAnotherUser() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        AddressResult own = addAddress.execute(addCommand("Casa", false));
        when(currentUserProvider.getCurrentUserId()).thenReturn(OTHER_USER_ID);
        addAddress.execute(addCommand("Otro", true));
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);

        List<AddressResult> result = listAddresses.execute();

        assertEquals(1, result.size());
        assertEquals(own.id(), result.get(0).id());
        assertEquals("Casa", result.get(0).label());
    }

    @Test
    void shouldUpdateAddressUsingCurrentUserFromProvider() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        AddressResult created = addAddress.execute(addCommand("Casa", true));

        AddressResult updated = updateAddress.execute(updateCommand(created.id(), "Oficina"));

        verify(currentUserProvider, atLeastOnce()).getCurrentUserId();
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
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        AddressResult created = addAddress.execute(addCommand("Casa", false));
        when(currentUserProvider.getCurrentUserId()).thenReturn(OTHER_USER_ID);

        assertThrows(
                AddressOwnershipException.class,
                () -> updateAddress.execute(updateCommand(created.id(), "Oficina")));
    }

    @Test
    void shouldRejectAddressUpdateWhenAddressIsInactive() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        AddressResult created = addAddress.execute(addCommand("Casa", false));
        deactivateAddress.execute(new DeactivateAddressCommand(created.id()));

        assertThrows(
                InactiveAddressException.class,
                () -> updateAddress.execute(updateCommand(created.id(), "Oficina")));
    }

    @Test
    void shouldDeactivateAddressAndClearDefaultFlag() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        AddressResult created = addAddress.execute(addCommand("Casa", true));

        AddressResult deactivated =
                deactivateAddress.execute(new DeactivateAddressCommand(created.id()));

        verify(currentUserProvider, atLeastOnce()).getCurrentUserId();
        assertEquals(AddressStatus.INACTIVE, deactivated.status());
        assertFalse(deactivated.isDefault());
        assertEquals(created.id(), deactivated.id());
        assertEquals(UPDATED_AT, deactivated.updatedAt());
    }

    @Test
    void shouldRejectDeactivateWhenAddressBelongsToAnotherUser() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        AddressResult created = addAddress.execute(addCommand("Casa", false));
        when(currentUserProvider.getCurrentUserId()).thenReturn(OTHER_USER_ID);

        assertThrows(
                AddressOwnershipException.class,
                () -> deactivateAddress.execute(new DeactivateAddressCommand(created.id())));
        assertEquals(AddressStatus.ACTIVE, addresses.findById(created.id()).orElseThrow().status());
    }

    @Test
    void shouldRejectDeactivateWhenAddressDoesNotExist() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        UUID missingAddressId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

        assertThrows(
                AddressNotFoundException.class,
                () -> deactivateAddress.execute(new DeactivateAddressCommand(missingAddressId)));
    }

    @Test
    void shouldRejectUpdateWhenAddressDoesNotExist() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        UUID missingAddressId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

        assertThrows(
                AddressNotFoundException.class,
                () -> updateAddress.execute(updateCommand(missingAddressId, "Oficina")));
    }

    @Test
    void shouldRejectSetDefaultWhenAddressDoesNotExist() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        UUID missingAddressId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

        assertThrows(
                AddressNotFoundException.class,
                () -> setDefaultAddress.execute(new SetDefaultAddressCommand(missingAddressId)));
    }

    @Test
    void shouldSetDefaultAddressUsingCurrentUserFromProvider() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        AddressResult first = addAddress.execute(addCommand("Casa", true));
        AddressResult second = addAddress.execute(addCommand("Oficina", false));

        AddressResult result = setDefaultAddress.execute(new SetDefaultAddressCommand(second.id()));

        verify(currentUserProvider, atLeastOnce()).getCurrentUserId();
        assertTrue(result.isDefault());
        assertEquals(second.id(), result.id());
        Address previous = addresses.findById(first.id()).orElseThrow();
        assertFalse(previous.isDefault());
        assertEquals(AddressStatus.ACTIVE, previous.status());
    }

    @Test
    void shouldRejectSetDefaultWhenAddressBelongsToAnotherUser() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        AddressResult created = addAddress.execute(addCommand("Casa", false));
        when(currentUserProvider.getCurrentUserId()).thenReturn(OTHER_USER_ID);

        assertThrows(
                AddressOwnershipException.class,
                () -> setDefaultAddress.execute(new SetDefaultAddressCommand(created.id())));
    }

    @Test
    void shouldRejectSetDefaultWhenAddressIsInactive() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        AddressResult created = addAddress.execute(addCommand("Casa", false));
        deactivateAddress.execute(new DeactivateAddressCommand(created.id()));

        assertThrows(
                InactiveAddressException.class,
                () -> setDefaultAddress.execute(new SetDefaultAddressCommand(created.id())));
    }

    @Test
    void shouldNotChangeOwnershipWhenUpdatingAddress() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        AddressResult created = addAddress.execute(addCommand("Casa", false));

        updateAddress.execute(updateCommand(created.id(), "Oficina"));

        assertNotEquals(OTHER_USER_ID, addresses.findOwnedById(created.id()).orElseThrow().userId());
        assertEquals(USER_ID, addresses.findOwnedById(created.id()).orElseThrow().userId());
    }

    @Test
    void shouldPropagateUnauthenticatedUserWhenAddingAddress() {
        when(currentUserProvider.getCurrentUserId()).thenThrow(new UnauthenticatedUserException());

        assertThrows(UnauthenticatedUserException.class, () -> addAddress.execute(addCommand("Casa", false)));
    }

    @Test
    void shouldPropagateUnauthenticatedUserWhenListingAddresses() {
        when(currentUserProvider.getCurrentUserId()).thenThrow(new UnauthenticatedUserException());

        assertThrows(UnauthenticatedUserException.class, listAddresses::execute);
    }

    @Test
    void shouldPropagateUnauthenticatedUserWhenUpdatingAddress() {
        UUID addressId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        when(currentUserProvider.getCurrentUserId()).thenThrow(new UnauthenticatedUserException());

        assertThrows(
                UnauthenticatedUserException.class, () -> updateAddress.execute(updateCommand(addressId, "Oficina")));
    }

    @Test
    void shouldPropagateUnauthenticatedUserWhenDeactivatingAddress() {
        UUID addressId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        when(currentUserProvider.getCurrentUserId()).thenThrow(new UnauthenticatedUserException());

        assertThrows(
                UnauthenticatedUserException.class,
                () -> deactivateAddress.execute(new DeactivateAddressCommand(addressId)));
    }

    @Test
    void shouldPropagateUnauthenticatedUserWhenSettingDefaultAddress() {
        UUID addressId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        when(currentUserProvider.getCurrentUserId()).thenThrow(new UnauthenticatedUserException());

        assertThrows(
                UnauthenticatedUserException.class,
                () -> setDefaultAddress.execute(new SetDefaultAddressCommand(addressId)));
    }

    private static AddAddressCommand addCommand(String label, boolean isDefault) {
        return new AddAddressCommand(
                label,
                "Ada Lovelace",
                "Calle 1 # 2-3",
                "Apto 101",
                "Bogotá",
                "Cundinamarca",
                "3001234567",
                isDefault);
    }

    private static UpdateAddressCommand updateCommand(UUID addressId, String label) {
        return new UpdateAddressCommand(
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
