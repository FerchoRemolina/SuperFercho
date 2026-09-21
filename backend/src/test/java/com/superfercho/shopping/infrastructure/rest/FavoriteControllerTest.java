package com.superfercho.shopping.infrastructure.rest;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.platform.error.ApiExceptionHandler;
import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.dto.favorite.AddFavoriteCommand;
import com.superfercho.shopping.application.dto.favorite.AddFavoriteResult;
import com.superfercho.shopping.application.dto.favorite.FavoriteItemResponse;
import com.superfercho.shopping.application.dto.favorite.FavoriteListResult;
import com.superfercho.shopping.application.dto.favorite.FavoriteProductView;
import com.superfercho.shopping.application.dto.favorite.RemoveFavoriteCommand;
import com.superfercho.shopping.application.exception.ProductNotFoundException;
import com.superfercho.shopping.application.port.in.AddFavoriteUseCase;
import com.superfercho.shopping.application.port.in.ListFavoritesUseCase;
import com.superfercho.shopping.application.port.in.RemoveFavoriteUseCase;
import com.superfercho.shopping.domain.model.Favorite;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FavoriteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ShoppingExceptionHandler.class, ApiExceptionHandler.class})
class FavoriteControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-21T10:00:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID CATEGORY_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID FAVORITE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Money PRICE = Money.cop(new BigDecimal("10.50"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListFavoritesUseCase listFavoritesUseCase;

    @MockitoBean
    private AddFavoriteUseCase addFavoriteUseCase;

    @MockitoBean
    private RemoveFavoriteUseCase removeFavoriteUseCase;

    @Test
    void shouldCreateFavorite() throws Exception {
        when(addFavoriteUseCase.execute(any())).thenReturn(AddFavoriteResult.created(favorite()));

        mockMvc.perform(post("/api/v1/favorites/{productId}", PRODUCT_ID))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/favorites/" + PRODUCT_ID)))
                .andExpect(jsonPath("$.id").value(FAVORITE_ID.toString()))
                .andExpect(jsonPath("$.productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.customerId").doesNotExist());

        verify(addFavoriteUseCase).execute(new AddFavoriteCommand(PRODUCT_ID));
    }

    @Test
    void shouldReturnExistingFavorite() throws Exception {
        when(addFavoriteUseCase.execute(any())).thenReturn(AddFavoriteResult.existing(favorite()));

        mockMvc.perform(post("/api/v1/favorites/{productId}", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(FAVORITE_ID.toString()))
                .andExpect(jsonPath("$.productId").value(PRODUCT_ID.toString()));
    }

    @Test
    void shouldRemoveFavorite() throws Exception {
        mockMvc.perform(delete("/api/v1/favorites/{productId}", PRODUCT_ID)).andExpect(status().isNoContent());

        verify(removeFavoriteUseCase).execute(new RemoveFavoriteCommand(PRODUCT_ID));
    }

    @Test
    void shouldListFavorites() throws Exception {
        when(listFavoritesUseCase.execute())
                .thenReturn(new FavoriteListResult(List.of(
                        new FavoriteItemResponse(PRODUCT_ID, CREATED_AT, activeProduct()),
                        new FavoriteItemResponse(
                                UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"), CREATED_AT, null))));

        mockMvc.perform(get("/api/v1/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.items[0].createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.items[0].product.id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.items[0].product.name").value("Leche entera"))
                .andExpect(jsonPath("$.items[0].product.brand").value("Alpina"))
                .andExpect(jsonPath("$.items[0].product.price.amount").value(10.50))
                .andExpect(jsonPath("$.items[0].product.price.currency").value("COP"))
                .andExpect(jsonPath("$.items[0].product.imageUrl").value("https://img.test/milk.png"))
                .andExpect(jsonPath("$.items[0].product.categoryId").value(CATEGORY_ID.toString()))
                .andExpect(jsonPath("$.items[0].product.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].product.available").value(true))
                .andExpect(jsonPath("$.items[1].product").value(nullValue()));

        verify(listFavoritesUseCase).execute();
    }

    @Test
    void shouldMapMissingSellableProductTo404() throws Exception {
        when(addFavoriteUseCase.execute(any())).thenThrow(new ProductNotFoundException(PRODUCT_ID));

        mockMvc.perform(post("/api/v1/favorites/{productId}", PRODUCT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    private static Favorite favorite() {
        return Favorite.create(FAVORITE_ID, CUSTOMER_ID, PRODUCT_ID, CREATED_AT);
    }

    private static FavoriteProductView activeProduct() {
        return new FavoriteProductView(
                PRODUCT_ID,
                "Leche entera",
                "Alpina",
                PRICE,
                "https://img.test/milk.png",
                CATEGORY_ID,
                "ACTIVE",
                true);
    }
}
