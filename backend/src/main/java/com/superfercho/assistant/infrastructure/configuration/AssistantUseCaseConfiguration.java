package com.superfercho.assistant.infrastructure.configuration;

import com.superfercho.assistant.application.fercho.FerchoLimits;
import com.superfercho.assistant.application.fercho.FerchoSupervisionPort;
import com.superfercho.assistant.application.fercho.FerchoUsageGate;
import com.superfercho.assistant.application.port.out.ClockPort;
import com.superfercho.assistant.application.port.out.ConversationStore;
import com.superfercho.assistant.application.port.out.CurrentUserProvider;
import com.superfercho.assistant.application.port.out.LLMPort;
import com.superfercho.assistant.application.port.out.PackageSelectionProposalStore;
import com.superfercho.assistant.application.port.out.PendingSensitiveActionStore;
import com.superfercho.assistant.application.service.ChatApplicationService;
import com.superfercho.assistant.application.tool.AssistantToolAllowlist;
import com.superfercho.assistant.application.tool.ToolRegistry;
import com.superfercho.assistant.infrastructure.clock.SystemClockAdapter;
import com.superfercho.assistant.infrastructure.confirmation.InMemoryPendingSensitiveActionStore;
import com.superfercho.assistant.infrastructure.conversation.InMemoryConversationStore;
import com.superfercho.assistant.infrastructure.packagesel.InMemoryPackageSelectionProposalStore;
import com.superfercho.assistant.infrastructure.configuration.FerchoCostProperties;
import com.superfercho.assistant.infrastructure.configuration.FerchoLimitsProperties;
import com.superfercho.catalog.application.usecase.GetProductUseCase;
import com.superfercho.catalog.application.usecase.ListCategoriesUseCase;
import com.superfercho.catalog.application.usecase.ListProductsUseCase;
import com.superfercho.catalog.application.usecase.ResolveProductQuantityEquivalenceUseCase;
import com.superfercho.catalog.application.usecase.SearchProductsUseCase;
import com.superfercho.identity.application.usecase.ListAddressesUseCase;
import com.superfercho.knowledge.application.usecase.SearchKnowledgeUseCase;
import com.superfercho.orders.application.port.in.CancelOrderUseCase;
import com.superfercho.orders.application.port.in.CheckoutUseCase;
import com.superfercho.orders.application.usecase.GetOrderUseCase;
import com.superfercho.orders.application.usecase.ListOrdersUseCase;
import com.superfercho.shopping.application.port.in.AddProductToCartUseCase;
import com.superfercho.shopping.application.port.in.ApplyIdempotentCartAddUseCase;
import com.superfercho.shopping.application.port.in.AddProductToShoppingListUseCase;
import com.superfercho.shopping.application.port.in.AddShoppingListToCartUseCase;
import com.superfercho.shopping.application.port.in.ChangeCartItemQuantityUseCase;
import com.superfercho.shopping.application.port.in.ChangeShoppingListItemQuantityUseCase;
import com.superfercho.shopping.application.port.in.ClearCartUseCase;
import com.superfercho.shopping.application.port.in.ClearShoppingListUseCase;
import com.superfercho.shopping.application.port.in.CreateShoppingListUseCase;
import com.superfercho.shopping.application.port.in.GetCartUseCase;
import com.superfercho.shopping.application.port.in.GetShoppingListUseCase;
import com.superfercho.shopping.application.port.in.ListShoppingListsUseCase;
import com.superfercho.shopping.application.port.in.RemoveProductFromCartUseCase;
import com.superfercho.shopping.application.port.in.RemoveProductFromShoppingListUseCase;
import com.superfercho.shopping.application.port.in.RenameShoppingListUseCase;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
@EnableConfigurationProperties({FerchoLimitsProperties.class, FerchoCostProperties.class})
public class AssistantUseCaseConfiguration {

    @Bean
    ClockPort assistantClockPort(Clock clock) {
        return new SystemClockAdapter(clock);
    }

    @Bean
    ConversationStore conversationStore() {
        return new InMemoryConversationStore();
    }

    @Bean
    PendingSensitiveActionStore pendingSensitiveActionStore() {
        return new InMemoryPendingSensitiveActionStore();
    }

    @Bean
    PackageSelectionProposalStore packageSelectionProposalStore() {
        return new InMemoryPackageSelectionProposalStore();
    }

    @Bean
    ToolRegistry assistantToolRegistry(
            SearchProductsUseCase searchProductsUseCase,
            GetProductUseCase getProductUseCase,
            ResolveProductQuantityEquivalenceUseCase resolveProductQuantityEquivalenceUseCase,
            ListProductsUseCase listProductsUseCase,
            ListCategoriesUseCase listCategoriesUseCase,
            GetCartUseCase getCartUseCase,
            AddProductToCartUseCase addProductToCartUseCase,
            ChangeCartItemQuantityUseCase changeCartItemQuantityUseCase,
            RemoveProductFromCartUseCase removeProductFromCartUseCase,
            ClearCartUseCase clearCartUseCase,
            ListShoppingListsUseCase listShoppingListsUseCase,
            GetShoppingListUseCase getShoppingListUseCase,
            CreateShoppingListUseCase createShoppingListUseCase,
            RenameShoppingListUseCase renameShoppingListUseCase,
            AddProductToShoppingListUseCase addProductToShoppingListUseCase,
            ChangeShoppingListItemQuantityUseCase changeShoppingListItemQuantityUseCase,
            RemoveProductFromShoppingListUseCase removeProductFromShoppingListUseCase,
            ClearShoppingListUseCase clearShoppingListUseCase,
            AddShoppingListToCartUseCase addShoppingListToCartUseCase,
            ListOrdersUseCase listOrdersUseCase,
            GetOrderUseCase getOrderUseCase,
            CurrentUserProvider assistantCurrentUserProviderAdapter,
            ClockPort assistantClockPort,
            PendingSensitiveActionStore pendingSensitiveActionStore,
            PackageSelectionProposalStore packageSelectionProposalStore,
            ListAddressesUseCase listAddressesUseCase,
            SearchKnowledgeUseCase searchKnowledgeUseCase) {
        return AssistantToolAllowlist.create(
                searchProductsUseCase,
                getProductUseCase,
                resolveProductQuantityEquivalenceUseCase,
                listProductsUseCase,
                listCategoriesUseCase,
                getCartUseCase,
                addProductToCartUseCase,
                changeCartItemQuantityUseCase,
                removeProductFromCartUseCase,
                clearCartUseCase,
                listShoppingListsUseCase,
                getShoppingListUseCase,
                createShoppingListUseCase,
                renameShoppingListUseCase,
                addProductToShoppingListUseCase,
                changeShoppingListItemQuantityUseCase,
                removeProductFromShoppingListUseCase,
                clearShoppingListUseCase,
                addShoppingListToCartUseCase,
                listOrdersUseCase,
                getOrderUseCase,
                assistantCurrentUserProviderAdapter,
                assistantClockPort,
                pendingSensitiveActionStore,
                packageSelectionProposalStore,
                listAddressesUseCase,
                searchKnowledgeUseCase);
    }

    @Bean
    @ConditionalOnBean(LLMPort.class)
    ChatApplicationService chatApplicationService(
            CurrentUserProvider assistantCurrentUserProviderAdapter,
            ClockPort assistantClockPort,
            ConversationStore conversationStore,
            PendingSensitiveActionStore pendingSensitiveActionStore,
            LLMPort llmPort,
            ToolRegistry assistantToolRegistry,
            CheckoutUseCase checkoutUseCase,
            CancelOrderUseCase cancelOrderUseCase,
            FerchoUsageGate ferchoUsageGate,
            FerchoLimitsProperties ferchoLimitsProperties,
            PackageSelectionProposalStore packageSelectionProposalStore,
            ApplyIdempotentCartAddUseCase applyIdempotentCartAddUseCase,
            GetProductUseCase getProductUseCase) {
        return new ChatApplicationService(
                assistantCurrentUserProviderAdapter,
                assistantClockPort,
                conversationStore,
                pendingSensitiveActionStore,
                llmPort,
                assistantToolRegistry,
                checkoutUseCase,
                cancelOrderUseCase,
                ferchoUsageGate,
                new FerchoLimits(
                        ferchoLimitsProperties.maxRequestsPerWindow(),
                        ferchoLimitsProperties.window(),
                        ferchoLimitsProperties.pause(),
                        ferchoLimitsProperties.recidiveWindow()),
                packageSelectionProposalStore,
                applyIdempotentCartAddUseCase,
                getProductUseCase);
    }
}
