package com.superfercho.shopping.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.platform.error.ApiExceptionHandler;
import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.dto.cart.AddProductToCartCommand;
import com.superfercho.shopping.application.dto.cart.CartItemResponse;
import com.superfercho.shopping.application.dto.cart.CartResponse;
import com.superfercho.shopping.application.dto.cart.ChangeCartItemQuantityCommand;
import com.superfercho.shopping.application.dto.cart.ClearCartCommand;
import com.superfercho.shopping.application.dto.cart.RemoveProductFromCartCommand;
import com.superfercho.shopping.application.exception.CartNotFoundException;
import com.superfercho.shopping.application.exception.ProductNotFoundException;
import com.superfercho.shopping.application.port.in.AddProductToCartUseCase;
import com.superfercho.shopping.application.port.in.ChangeCartItemQuantityUseCase;
import com.superfercho.shopping.application.port.in.ClearCartUseCase;
import com.superfercho.shopping.application.port.in.GetCartUseCase;
import com.superfercho.shopping.application.port.in.RemoveProductFromCartUseCase;
import com.superfercho.shopping.domain.model.CartStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CartController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ShoppingExceptionHandler.class, ApiExceptionHandler.class})
class CartControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-04-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-04-01T10:05:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CART_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID ITEM_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final Money PRICE = Money.cop(new BigDecimal("10.50"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetCartUseCase getCartUseCase;

    @MockitoBean
    private AddProductToCartUseCase addProductToCartUseCase;

    @MockitoBean
    private ChangeCartItemQuantityUseCase changeCartItemQuantityUseCase;

    @MockitoBean
    private RemoveProductFromCartUseCase removeProductFromCartUseCase;

    @MockitoBean
    private ClearCartUseCase clearCartUseCase;

    @Test
    void shouldGetCart() throws Exception {
        when(getCartUseCase.execute()).thenReturn(cartWithItem(2));

        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CART_ID.toString()))
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].priceAtAddition.amount").value(10.50))
                .andExpect(jsonPath("$.items[0].priceAtAddition.currency").value("COP"))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.updatedAt").value(UPDATED_AT.toString()));

        verify(getCartUseCase).execute();
    }

    @Test
    void shouldAddProductToCart() throws Exception {
        when(addProductToCartUseCase.execute(any())).thenReturn(cartWithItem(2));

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","quantity":2}
                                """.formatted(PRODUCT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        verify(addProductToCartUseCase).execute(new AddProductToCartCommand(PRODUCT_ID, 2));
    }

    @Test
    void shouldChangeCartItemQuantity() throws Exception {
        when(changeCartItemQuantityUseCase.execute(any())).thenReturn(cartWithItem(3));

        mockMvc.perform(put("/api/v1/cart/items/{productId}", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(3));

        verify(changeCartItemQuantityUseCase).execute(new ChangeCartItemQuantityCommand(PRODUCT_ID, 3));
    }

    @Test
    void shouldRemoveProductFromCart() throws Exception {
        when(removeProductFromCartUseCase.execute(any())).thenReturn(emptyCart());

        mockMvc.perform(delete("/api/v1/cart/items/{productId}", PRODUCT_ID)).andExpect(status().isNoContent());

        verify(removeProductFromCartUseCase).execute(new RemoveProductFromCartCommand(PRODUCT_ID));
    }

    @Test
    void shouldClearCart() throws Exception {
        when(clearCartUseCase.execute(any())).thenReturn(emptyCart());

        mockMvc.perform(delete("/api/v1/cart")).andExpect(status().isNoContent());

        verify(clearCartUseCase).execute(new ClearCartCommand());
    }

    @Test
    void shouldRejectInvalidQuantity() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","quantity":0}
                                """.formatted(PRODUCT_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldMapCartNotFoundTo404() throws Exception {
        when(changeCartItemQuantityUseCase.execute(any())).thenThrow(new CartNotFoundException(CUSTOMER_ID));

        mockMvc.perform(put("/api/v1/cart/items/{productId}", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":3}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CART_NOT_FOUND"));
    }

    @Test
    void shouldMapProductNotFoundTo404() throws Exception {
        when(addProductToCartUseCase.execute(any())).thenThrow(new ProductNotFoundException(PRODUCT_ID));

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","quantity":2}
                                """.formatted(PRODUCT_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    private static CartResponse emptyCart() {
        return new CartResponse(CART_ID, CUSTOMER_ID, CartStatus.ACTIVE, List.of(), CREATED_AT, UPDATED_AT);
    }

    private static CartResponse cartWithItem(int quantity) {
        return new CartResponse(
                CART_ID,
                CUSTOMER_ID,
                CartStatus.ACTIVE,
                List.of(new CartItemResponse(ITEM_ID, PRODUCT_ID, quantity, PRICE, CREATED_AT, UPDATED_AT)),
                CREATED_AT,
                UPDATED_AT);
    }
}
