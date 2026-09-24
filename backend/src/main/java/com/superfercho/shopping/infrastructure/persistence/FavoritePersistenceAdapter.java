package com.superfercho.shopping.infrastructure.persistence;

import com.superfercho.shopping.application.port.out.FavoriteRepositoryPort;
import com.superfercho.shopping.domain.model.Favorite;
import com.superfercho.shopping.infrastructure.persistence.mapper.FavoritePersistenceMapper;
import com.superfercho.shopping.infrastructure.persistence.repository.FavoriteJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class FavoritePersistenceAdapter implements FavoriteRepositoryPort {

    private final FavoriteJpaRepository favoriteJpaRepository;
    private final FavoritePersistenceMapper favoritePersistenceMapper;

    public FavoritePersistenceAdapter(
            FavoriteJpaRepository favoriteJpaRepository, FavoritePersistenceMapper favoritePersistenceMapper) {
        this.favoriteJpaRepository = favoriteJpaRepository;
        this.favoritePersistenceMapper = favoritePersistenceMapper;
    }

    @Override
    public Favorite save(Favorite favorite) {
        try {
            return favoritePersistenceMapper.toDomain(
                    favoriteJpaRepository.saveAndFlush(favoritePersistenceMapper.toEntity(favorite)));
        } catch (DataIntegrityViolationException exception) {
            throw ShoppingConstraintViolationTranslator.translate(exception);
        }
    }

    @Override
    public Optional<Favorite> findByCustomerIdAndProductId(UUID customerId, UUID productId) {
        return favoriteJpaRepository
                .findByCustomerIdAndProductId(customerId, productId)
                .map(favoritePersistenceMapper::toDomain);
    }

    @Override
    public List<Favorite> findAllByCustomerId(UUID customerId) {
        return favoriteJpaRepository.findAllByCustomerId(customerId).stream()
                .map(favoritePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteByCustomerIdAndProductId(UUID customerId, UUID productId) {
        favoriteJpaRepository.deleteByCustomerIdAndProductId(customerId, productId);
    }
}
