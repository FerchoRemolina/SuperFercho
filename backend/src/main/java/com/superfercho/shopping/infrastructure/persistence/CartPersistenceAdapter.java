package com.superfercho.shopping.infrastructure.persistence;

import com.superfercho.shopping.application.port.out.CartRepositoryPort;
import com.superfercho.shopping.domain.model.Cart;
import com.superfercho.shopping.infrastructure.persistence.mapper.CartPersistenceMapper;
import com.superfercho.shopping.infrastructure.persistence.repository.CartJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CartPersistenceAdapter implements CartRepositoryPort {

    private final CartJpaRepository cartJpaRepository;
    private final CartPersistenceMapper cartPersistenceMapper;

    public CartPersistenceAdapter(
            CartJpaRepository cartJpaRepository, CartPersistenceMapper cartPersistenceMapper) {
        this.cartJpaRepository = cartJpaRepository;
        this.cartPersistenceMapper = cartPersistenceMapper;
    }

    @Override
    public Cart save(Cart cart) {
        try {
            return cartPersistenceMapper.toDomain(
                    cartJpaRepository.saveAndFlush(cartPersistenceMapper.toEntity(cart)));
        } catch (DataIntegrityViolationException exception) {
            throw ShoppingConstraintViolationTranslator.translate(exception);
        }
    }

    @Override
    public Optional<Cart> findByCustomerId(UUID customerId) {
        return cartJpaRepository.findByCustomerId(customerId).map(cartPersistenceMapper::toDomain);
    }
}
