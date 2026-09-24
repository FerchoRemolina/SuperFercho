package com.superfercho.shopping.application.port.out;

import com.superfercho.shopping.domain.model.Favorite;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavoriteRepositoryPort {

    Favorite save(Favorite favorite);

    Optional<Favorite> findByCustomerIdAndProductId(UUID customerId, UUID productId);

    List<Favorite> findAllByCustomerId(UUID customerId);

    void deleteByCustomerIdAndProductId(UUID customerId, UUID productId);
}
