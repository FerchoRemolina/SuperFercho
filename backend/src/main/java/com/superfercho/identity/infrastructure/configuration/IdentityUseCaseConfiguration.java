package com.superfercho.identity.infrastructure.configuration;

import com.superfercho.identity.application.dto.AddAddressCommand;
import com.superfercho.identity.application.dto.AddressResult;
import com.superfercho.identity.application.dto.SetDefaultAddressCommand;
import com.superfercho.identity.application.port.AccessTokenIssuer;
import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.identity.application.port.PasswordHasher;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.application.usecase.AddAddressUseCase;
import com.superfercho.identity.application.usecase.AuthenticateUserUseCase;
import com.superfercho.identity.application.usecase.DeactivateAddressUseCase;
import com.superfercho.identity.application.usecase.ListAddressesUseCase;
import com.superfercho.identity.application.usecase.RegisterCustomerUseCase;
import com.superfercho.identity.application.usecase.SetDefaultAddressUseCase;
import com.superfercho.identity.application.usecase.UpdateAddressUseCase;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@ConditionalOnBean(UserRepository.class)
public class IdentityUseCaseConfiguration {

    @Bean
    RegisterCustomerUseCase registerCustomerUseCase(
            UserRepository userRepository, PasswordHasher passwordHasher, Clock clock) {
        return new RegisterCustomerUseCase(userRepository, passwordHasher, clock);
    }

    @Bean
    AuthenticateUserUseCase authenticateUserUseCase(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            AccessTokenIssuer accessTokenIssuer) {
        return new AuthenticateUserUseCase(userRepository, passwordHasher, accessTokenIssuer);
    }

    @Bean
    AddAddressUseCase addAddressUseCase(
            CurrentUserProvider currentUserProvider,
            UserRepository userRepository,
            AddressRepository addressRepository,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return new AddAddressUseCase(currentUserProvider, userRepository, addressRepository, clock) {
            @Override
            public AddressResult execute(AddAddressCommand command) {
                return transaction.execute(status -> super.execute(command));
            }
        };
    }

    @Bean
    ListAddressesUseCase listAddressesUseCase(
            CurrentUserProvider currentUserProvider,
            UserRepository userRepository,
            AddressRepository addressRepository) {
        return new ListAddressesUseCase(currentUserProvider, userRepository, addressRepository);
    }

    @Bean
    UpdateAddressUseCase updateAddressUseCase(
            CurrentUserProvider currentUserProvider, AddressRepository addressRepository, Clock clock) {
        return new UpdateAddressUseCase(currentUserProvider, addressRepository, clock);
    }

    @Bean
    DeactivateAddressUseCase deactivateAddressUseCase(
            CurrentUserProvider currentUserProvider, AddressRepository addressRepository, Clock clock) {
        return new DeactivateAddressUseCase(currentUserProvider, addressRepository, clock);
    }

    @Bean
    SetDefaultAddressUseCase setDefaultAddressUseCase(
            CurrentUserProvider currentUserProvider,
            AddressRepository addressRepository,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return new SetDefaultAddressUseCase(currentUserProvider, addressRepository, clock) {
            @Override
            public AddressResult execute(SetDefaultAddressCommand command) {
                return transaction.execute(status -> super.execute(command));
            }
        };
    }
}
