package com.superfercho.orders.infrastructure.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.application.port.OwnedAddress;
import com.superfercho.identity.domain.model.Address;
import com.superfercho.identity.domain.model.AddressStatus;
import com.superfercho.orders.application.dto.AddressSnapshot;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerAddressAdapterTest {

    private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER_CUSTOMER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ADDRESS_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private AddressRepository addressRepository;

    private CustomerAddressAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CustomerAddressAdapter(addressRepository);
    }

    @Test
    void shouldMapOwnedActiveAddressToSnapshot() {
        when(addressRepository.findOwnedById(ADDRESS_ID))
                .thenReturn(Optional.of(new OwnedAddress(CUSTOMER_ID, activeAddress())));

        Optional<AddressSnapshot> result = adapter.getAddressForCustomer(CUSTOMER_ID, ADDRESS_ID);

        assertTrue(result.isPresent());
        AddressSnapshot snapshot = result.get();
        assertEquals("Ada Lovelace", snapshot.recipientName());
        assertEquals("Calle 1 # 2-3", snapshot.addressLine());
        assertEquals("Apto 101", snapshot.additionalInfo());
        assertEquals("Bogotá", snapshot.city());
        assertEquals("Cundinamarca", snapshot.department());
        assertEquals("3001234567", snapshot.phone());
        verify(addressRepository).findOwnedById(ADDRESS_ID);
    }

    @Test
    void shouldReturnEmptyWhenAddressDoesNotExist() {
        when(addressRepository.findOwnedById(ADDRESS_ID)).thenReturn(Optional.empty());

        assertTrue(adapter.getAddressForCustomer(CUSTOMER_ID, ADDRESS_ID).isEmpty());
        verify(addressRepository).findOwnedById(ADDRESS_ID);
    }

    @Test
    void shouldReturnEmptyWhenAddressBelongsToAnotherCustomer() {
        when(addressRepository.findOwnedById(ADDRESS_ID))
                .thenReturn(Optional.of(new OwnedAddress(OTHER_CUSTOMER_ID, activeAddress())));

        assertTrue(adapter.getAddressForCustomer(CUSTOMER_ID, ADDRESS_ID).isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenAddressIsInactive() {
        when(addressRepository.findOwnedById(ADDRESS_ID))
                .thenReturn(Optional.of(new OwnedAddress(CUSTOMER_ID, inactiveAddress())));

        assertTrue(adapter.getAddressForCustomer(CUSTOMER_ID, ADDRESS_ID).isEmpty());
    }

    private static Address activeAddress() {
        return Address.create(
                ADDRESS_ID,
                "Casa",
                "Ada Lovelace",
                "Calle 1 # 2-3",
                "Apto 101",
                "Bogotá",
                "Cundinamarca",
                "3001234567",
                true,
                AddressStatus.ACTIVE,
                NOW,
                NOW);
    }

    private static Address inactiveAddress() {
        return Address.create(
                ADDRESS_ID,
                "Casa",
                "Ada Lovelace",
                "Calle 1 # 2-3",
                "Apto 101",
                "Bogotá",
                "Cundinamarca",
                "3001234567",
                false,
                AddressStatus.INACTIVE,
                NOW,
                NOW);
    }
}
