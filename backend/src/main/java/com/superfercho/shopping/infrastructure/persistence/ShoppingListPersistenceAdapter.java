package com.superfercho.shopping.infrastructure.persistence;

import com.superfercho.shopping.application.port.out.ShoppingListRepositoryPort;
import com.superfercho.shopping.domain.model.ShoppingList;
import com.superfercho.shopping.infrastructure.persistence.mapper.ShoppingListPersistenceMapper;
import com.superfercho.shopping.infrastructure.persistence.repository.ShoppingListJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class ShoppingListPersistenceAdapter implements ShoppingListRepositoryPort {

    private final ShoppingListJpaRepository shoppingListJpaRepository;
    private final ShoppingListPersistenceMapper shoppingListPersistenceMapper;

    public ShoppingListPersistenceAdapter(
            ShoppingListJpaRepository shoppingListJpaRepository,
            ShoppingListPersistenceMapper shoppingListPersistenceMapper) {
        this.shoppingListJpaRepository = shoppingListJpaRepository;
        this.shoppingListPersistenceMapper = shoppingListPersistenceMapper;
    }

    @Override
    public ShoppingList save(ShoppingList shoppingList) {
        return shoppingListPersistenceMapper.toDomain(
                shoppingListJpaRepository.saveAndFlush(shoppingListPersistenceMapper.toEntity(shoppingList)));
    }

    @Override
    public Optional<ShoppingList> findById(UUID shoppingListId) {
        return shoppingListJpaRepository.findById(shoppingListId).map(shoppingListPersistenceMapper::toDomain);
    }

    @Override
    public List<ShoppingList> findAllByCustomerId(UUID customerId) {
        return shoppingListJpaRepository.findAllByCustomerIdOrderByNameIgnoreCaseAsc(customerId).stream()
                .map(shoppingListPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void delete(UUID shoppingListId) {
        shoppingListJpaRepository.deleteById(shoppingListId);
        shoppingListJpaRepository.flush();
    }

    @Override
    public void deleteAllByCustomerId(UUID customerId) {
        shoppingListJpaRepository.deleteAllByCustomerId(customerId);
    }
}
