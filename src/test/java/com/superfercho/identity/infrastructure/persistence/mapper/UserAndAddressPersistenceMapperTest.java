package com.superfercho.identity.infrastructure.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.superfercho.identity.domain.model.Address;
import com.superfercho.identity.domain.model.AddressStatus;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import com.superfercho.identity.infrastructure.persistence.entity.AddressJpaEntity;
import com.superfercho.identity.infrastructure.persistence.entity.UserJpaEntity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserAndAddressPersistenceMapperTest {

    private final UserPersistenceMapper userMapper = new UserPersistenceMapper();
    private final AddressPersistenceMapper addressMapper = new AddressPersistenceMapper();

    @Test
    void shouldMapUserRoundTrip() {
        User user = User.create(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "CC",
                "12345678",
                "Ada Lovelace",
                "ada@example.com",
                "3001234567",
                "hashed-password",
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"));

        UserJpaEntity entity = userMapper.toEntity(user);
        User mapped = userMapper.toDomain(entity);

        assertEquals(user.id(), mapped.id());
        assertEquals(user.documentType(), mapped.documentType());
        assertEquals(user.documentNumber(), mapped.documentNumber());
        assertEquals(user.fullName(), mapped.fullName());
        assertEquals(user.email(), mapped.email());
        assertEquals(user.phone(), mapped.phone());
        assertEquals(user.passwordHash(), mapped.passwordHash());
        assertEquals(user.role(), mapped.role());
        assertEquals(user.status(), mapped.status());
        assertEquals(user.createdAt(), mapped.createdAt());
        assertEquals(user.updatedAt(), mapped.updatedAt());
    }

    @Test
    void shouldMapAddressRoundTripIncludingUserIdAndNullAdditionalInfo() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Address address = Address.create(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "Casa",
                "Ada Lovelace",
                "Calle 1 # 2-3",
                null,
                "Bogotá",
                "Cundinamarca",
                "3001234567",
                true,
                AddressStatus.ACTIVE,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"));

        AddressJpaEntity entity = addressMapper.toEntity(userId, address);
        Address mapped = addressMapper.toDomain(entity);

        assertEquals(userId, entity.getUserId());
        assertNull(mapped.additionalInfo());
        assertEquals(address.id(), mapped.id());
        assertEquals(address.label(), mapped.label());
        assertEquals(address.isDefault(), mapped.isDefault());
        assertEquals(address.status(), mapped.status());
    }
}
