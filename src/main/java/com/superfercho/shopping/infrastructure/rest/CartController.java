package com.superfercho.shopping.infrastructure.rest;

import com.superfercho.shopping.application.dto.cart.AddProductToCartCommand;
import com.superfercho.shopping.application.dto.cart.ChangeCartItemQuantityCommand;
import com.superfercho.shopping.application.dto.cart.ClearCartCommand;
import com.superfercho.shopping.application.dto.cart.GetCartQuery;
import com.superfercho.shopping.application.dto.cart.RemoveProductFromCartCommand;
import com.superfercho.shopping.application.port.in.AddProductToCartUseCase;
import com.superfercho.shopping.application.port.in.ChangeCartItemQuantityUseCase;
import com.superfercho.shopping.application.port.in.ClearCartUseCase;
import com.superfercho.shopping.application.port.in.GetCartUseCase;
import com.superfercho.shopping.application.port.in.RemoveProductFromCartUseCase;
import com.superfercho.shopping.infrastructure.rest.dto.AddItemRequest;
import com.superfercho.shopping.infrastructure.rest.dto.CartRestResponse;
import com.superfercho.shopping.infrastructure.rest.dto.ChangeItemQuantityRequest;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!test")
@RequestMapping("/api/v1/customers/{customerId}/cart")
public class CartController {

    private final GetCartUseCase getCartUseCase;
    private final AddProductToCartUseCase addProductToCartUseCase;
    private final ChangeCartItemQuantityUseCase changeCartItemQuantityUseCase;
    private final RemoveProductFromCartUseCase removeProductFromCartUseCase;
    private final ClearCartUseCase clearCartUseCase;

    public CartController(
            GetCartUseCase getCartUseCase,
            AddProductToCartUseCase addProductToCartUseCase,
            ChangeCartItemQuantityUseCase changeCartItemQuantityUseCase,
            RemoveProductFromCartUseCase removeProductFromCartUseCase,
            ClearCartUseCase clearCartUseCase) {
        this.getCartUseCase = getCartUseCase;
        this.addProductToCartUseCase = addProductToCartUseCase;
        this.changeCartItemQuantityUseCase = changeCartItemQuantityUseCase;
        this.removeProductFromCartUseCase = removeProductFromCartUseCase;
        this.clearCartUseCase = clearCartUseCase;
    }

    @GetMapping
    public CartRestResponse getCart(@PathVariable UUID customerId) {
        return CartRestResponse.from(getCartUseCase.execute(new GetCartQuery(customerId)));
    }

    @PostMapping("/items")
    public CartRestResponse addItem(@PathVariable UUID customerId, @RequestBody AddItemRequest request) {
        return CartRestResponse.from(addProductToCartUseCase.execute(
                new AddProductToCartCommand(customerId, request.productId(), request.quantity())));
    }

    @PatchMapping("/items/{productId}")
    public CartRestResponse changeQuantity(
            @PathVariable UUID customerId,
            @PathVariable UUID productId,
            @RequestBody ChangeItemQuantityRequest request) {
        return CartRestResponse.from(changeCartItemQuantityUseCase.execute(
                new ChangeCartItemQuantityCommand(customerId, productId, request.quantity())));
    }

    @DeleteMapping("/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@PathVariable UUID customerId, @PathVariable UUID productId) {
        removeProductFromCartUseCase.execute(new RemoveProductFromCartCommand(customerId, productId));
    }

    @DeleteMapping("/items")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@PathVariable UUID customerId) {
        clearCartUseCase.execute(new ClearCartCommand(customerId));
    }
}
