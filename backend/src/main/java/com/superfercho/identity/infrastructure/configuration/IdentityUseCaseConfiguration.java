package com.superfercho.identity.infrastructure.configuration;

import com.superfercho.identity.application.dto.AddAddressCommand;
import com.superfercho.identity.application.dto.AddressResult;
import com.superfercho.identity.application.dto.RegisterCustomerCommand;
import com.superfercho.identity.application.dto.RegisteredCustomer;
import com.superfercho.identity.application.dto.SetDefaultAddressCommand;
import com.superfercho.identity.application.port.AccessTokenIssuer;
import com.superfercho.identity.application.port.AddressRepository;
import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.identity.application.port.CustomerAccountAssistantCleanupPort;
import com.superfercho.identity.application.port.CustomerAccountCheckoutCleanupPort;
import com.superfercho.identity.application.port.CustomerAccountShoppingCleanupPort;
import com.superfercho.identity.application.port.CustomerCommercialHistoryPort;
import com.superfercho.identity.application.port.CustomerPreviewRepository;
import com.superfercho.identity.application.port.CustomerRecordRepository;
import com.superfercho.identity.application.port.PasswordHasher;
import com.superfercho.identity.application.port.PasswordRecoveryTokenRepository;
import com.superfercho.identity.application.port.PreviewAssistantCleanupPort;
import com.superfercho.identity.application.port.PreviewOrdersCleanupPort;
import com.superfercho.identity.application.port.PreviewShoppingCleanupPort;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.application.usecase.ActivateAdminCustomerAccountUseCase;
import com.superfercho.identity.application.usecase.AddAddressUseCase;
import com.superfercho.identity.application.usecase.AuthenticateUserUseCase;
import com.superfercho.identity.application.usecase.CloseCustomerAccountUseCase;
import com.superfercho.identity.application.usecase.DeactivateAddressUseCase;
import com.superfercho.identity.application.usecase.DeactivateAdminCustomerAccountUseCase;
import com.superfercho.identity.application.usecase.ExitStorefrontPreviewUseCase;
import com.superfercho.identity.application.usecase.ExpireStorefrontPreviewsUseCase;
import com.superfercho.identity.application.usecase.FinalizeStorefrontPreviewUseCase;
import com.superfercho.identity.application.usecase.FindAdminCustomerByDocumentUseCase;
import com.superfercho.identity.application.usecase.GetAdminCustomerRecordUseCase;
import com.superfercho.identity.application.usecase.GetAdminNewCustomersUseCase;
import com.superfercho.identity.application.usecase.GetStorefrontPreviewUseCase;
import com.superfercho.identity.application.usecase.ListAddressesUseCase;
import com.superfercho.identity.application.usecase.ListAdminCustomerOrdersUseCase;
import com.superfercho.identity.application.usecase.ListAdminCustomerPaymentsUseCase;
import com.superfercho.identity.application.usecase.RegisterCustomerUseCase;
import com.superfercho.identity.application.usecase.SetDefaultAddressUseCase;
import com.superfercho.identity.application.usecase.StartStorefrontPreviewUseCase;
import com.superfercho.identity.application.usecase.UpdateAddressUseCase;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@ConditionalOnBean(UserRepository.class)
public class IdentityUseCaseConfiguration {

    @Bean
    @ConditionalOnBean(CustomerRecordRepository.class)
    RegisterCustomerUseCase registerCustomerUseCase(
            UserRepository userRepository,
            CustomerRecordRepository customerRecordRepository,
            PasswordHasher passwordHasher,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        RegisterCustomerUseCase useCase = new RegisterCustomerUseCase(
                userRepository, customerRecordRepository, passwordHasher, clock);
        return new RegisterCustomerUseCase(
                userRepository, customerRecordRepository, passwordHasher, clock) {
            @Override
            public RegisteredCustomer execute(RegisterCustomerCommand command) {
                return transaction.execute(status -> useCase.execute(command));
            }
        };
    }

    @Bean
    @ConditionalOnBean({
        AddressRepository.class,
        PasswordRecoveryTokenRepository.class,
        CustomerAccountShoppingCleanupPort.class,
        CustomerAccountCheckoutCleanupPort.class,
        CustomerAccountAssistantCleanupPort.class
    })
    CloseCustomerAccountUseCase closeCustomerAccountUseCase(
            CurrentUserProvider currentUserProvider,
            UserRepository userRepository,
            AddressRepository addressRepository,
            PasswordRecoveryTokenRepository passwordRecoveryTokenRepository,
            CustomerAccountShoppingCleanupPort shoppingCleanupPort,
            CustomerAccountCheckoutCleanupPort checkoutCleanupPort,
            CustomerAccountAssistantCleanupPort assistantCleanupPort,
            ObjectProvider<CustomerPreviewRepository> customerPreviewRepository,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        CustomerPreviewRepository previewRepository = customerPreviewRepository.getIfAvailable();
        CloseCustomerAccountUseCase useCase = previewRepository == null
                ? new CloseCustomerAccountUseCase(
                        currentUserProvider,
                        userRepository,
                        addressRepository,
                        passwordRecoveryTokenRepository,
                        shoppingCleanupPort,
                        checkoutCleanupPort,
                        assistantCleanupPort,
                        clock)
                : new CloseCustomerAccountUseCase(
                        currentUserProvider,
                        userRepository,
                        addressRepository,
                        passwordRecoveryTokenRepository,
                        shoppingCleanupPort,
                        checkoutCleanupPort,
                        assistantCleanupPort,
                        previewRepository,
                        clock);
        return new CloseCustomerAccountUseCase(
                currentUserProvider,
                userRepository,
                addressRepository,
                passwordRecoveryTokenRepository,
                shoppingCleanupPort,
                checkoutCleanupPort,
                assistantCleanupPort,
                previewRepository,
                clock) {
            @Override
            public void execute() {
                transaction.executeWithoutResult(status -> useCase.execute());
            }
        };
    }

    @Bean
    @ConditionalOnBean(CustomerRecordRepository.class)
    FindAdminCustomerByDocumentUseCase findAdminCustomerByDocumentUseCase(
            CustomerRecordRepository customerRecordRepository, UserRepository userRepository) {
        return new FindAdminCustomerByDocumentUseCase(customerRecordRepository, userRepository);
    }

    @Bean
    @ConditionalOnBean(CustomerRecordRepository.class)
    GetAdminCustomerRecordUseCase getAdminCustomerRecordUseCase(
            CustomerRecordRepository customerRecordRepository, UserRepository userRepository) {
        return new GetAdminCustomerRecordUseCase(customerRecordRepository, userRepository);
    }

    @Bean
    @ConditionalOnBean(CustomerRecordRepository.class)
    ActivateAdminCustomerAccountUseCase activateAdminCustomerAccountUseCase(
            CustomerRecordRepository customerRecordRepository, UserRepository userRepository, Clock clock) {
        return new ActivateAdminCustomerAccountUseCase(customerRecordRepository, userRepository, clock);
    }

    @Bean
    @ConditionalOnBean(CustomerRecordRepository.class)
    DeactivateAdminCustomerAccountUseCase deactivateAdminCustomerAccountUseCase(
            CustomerRecordRepository customerRecordRepository, UserRepository userRepository, Clock clock) {
        return new DeactivateAdminCustomerAccountUseCase(customerRecordRepository, userRepository, clock);
    }

    @Bean
    @ConditionalOnBean(CustomerRecordRepository.class)
    GetAdminNewCustomersUseCase getAdminNewCustomersUseCase(
            CustomerRecordRepository customerRecordRepository) {
        return new GetAdminNewCustomersUseCase(customerRecordRepository);
    }

    @Bean
    @ConditionalOnBean({CustomerRecordRepository.class, CustomerCommercialHistoryPort.class})
    ListAdminCustomerOrdersUseCase listAdminCustomerOrdersUseCase(
            CustomerRecordRepository customerRecordRepository,
            UserRepository userRepository,
            CustomerCommercialHistoryPort commercialHistoryPort) {
        return new ListAdminCustomerOrdersUseCase(
                customerRecordRepository, userRepository, commercialHistoryPort);
    }

    @Bean
    @ConditionalOnBean({CustomerRecordRepository.class, CustomerCommercialHistoryPort.class})
    ListAdminCustomerPaymentsUseCase listAdminCustomerPaymentsUseCase(
            CustomerRecordRepository customerRecordRepository,
            UserRepository userRepository,
            CustomerCommercialHistoryPort commercialHistoryPort) {
        return new ListAdminCustomerPaymentsUseCase(
                customerRecordRepository, userRepository, commercialHistoryPort);
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

    @Bean
    @ConditionalOnBean({CustomerPreviewRepository.class, PasswordRecoveryTokenRepository.class})
    FinalizeStorefrontPreviewUseCase finalizeStorefrontPreviewUseCase(
            CustomerPreviewRepository customerPreviewRepository,
            PreviewOrdersCleanupPort previewOrdersCleanupPort,
            PreviewShoppingCleanupPort previewShoppingCleanupPort,
            PreviewAssistantCleanupPort previewAssistantCleanupPort,
            PasswordRecoveryTokenRepository passwordRecoveryTokenRepository,
            AddressRepository addressRepository,
            UserRepository userRepository,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        FinalizeStorefrontPreviewUseCase useCase = new FinalizeStorefrontPreviewUseCase(
                customerPreviewRepository,
                previewOrdersCleanupPort,
                previewShoppingCleanupPort,
                previewAssistantCleanupPort,
                passwordRecoveryTokenRepository,
                addressRepository,
                userRepository,
                clock);
        return new FinalizeStorefrontPreviewUseCase(
                customerPreviewRepository,
                previewOrdersCleanupPort,
                previewShoppingCleanupPort,
                previewAssistantCleanupPort,
                passwordRecoveryTokenRepository,
                addressRepository,
                userRepository,
                clock) {
            @Override
            public boolean execute(UUID previewId) {
                Boolean result = transaction.execute(status -> useCase.execute(previewId));
                return Boolean.TRUE.equals(result);
            }
        };
    }

    @Bean
    @ConditionalOnBean(FinalizeStorefrontPreviewUseCase.class)
    StartStorefrontPreviewUseCase startStorefrontPreviewUseCase(
            CurrentUserProvider currentUserProvider,
            UserRepository userRepository,
            CustomerPreviewRepository customerPreviewRepository,
            PasswordHasher passwordHasher,
            AccessTokenIssuer accessTokenIssuer,
            FinalizeStorefrontPreviewUseCase finalizeStorefrontPreviewUseCase,
            Clock clock) {
        return new StartStorefrontPreviewUseCase(
                currentUserProvider,
                userRepository,
                customerPreviewRepository,
                passwordHasher,
                accessTokenIssuer,
                finalizeStorefrontPreviewUseCase,
                clock);
    }

    @Bean
    @ConditionalOnBean(FinalizeStorefrontPreviewUseCase.class)
    GetStorefrontPreviewUseCase getStorefrontPreviewUseCase(
            CurrentUserProvider currentUserProvider,
            UserRepository userRepository,
            CustomerPreviewRepository customerPreviewRepository,
            FinalizeStorefrontPreviewUseCase finalizeStorefrontPreviewUseCase,
            Clock clock) {
        return new GetStorefrontPreviewUseCase(
                currentUserProvider,
                userRepository,
                customerPreviewRepository,
                finalizeStorefrontPreviewUseCase,
                clock);
    }

    @Bean
    @ConditionalOnBean(FinalizeStorefrontPreviewUseCase.class)
    ExitStorefrontPreviewUseCase exitStorefrontPreviewUseCase(
            CurrentUserProvider currentUserProvider,
            UserRepository userRepository,
            CustomerPreviewRepository customerPreviewRepository,
            FinalizeStorefrontPreviewUseCase finalizeStorefrontPreviewUseCase) {
        return new ExitStorefrontPreviewUseCase(
                currentUserProvider,
                userRepository,
                customerPreviewRepository,
                finalizeStorefrontPreviewUseCase);
    }

    @Bean
    @ConditionalOnBean(FinalizeStorefrontPreviewUseCase.class)
    ExpireStorefrontPreviewsUseCase expireStorefrontPreviewsUseCase(
            CustomerPreviewRepository customerPreviewRepository,
            FinalizeStorefrontPreviewUseCase finalizeStorefrontPreviewUseCase,
            Clock clock) {
        return new ExpireStorefrontPreviewsUseCase(
                customerPreviewRepository, finalizeStorefrontPreviewUseCase, clock);
    }
}
