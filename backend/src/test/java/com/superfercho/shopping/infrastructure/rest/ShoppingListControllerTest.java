package com.superfercho.shopping.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.platform.error.ApiExceptionHandler;
import com.superfercho.shopping.application.dto.shoppinglist.AddProductToShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ChangeShoppingListItemQuantityCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ClearShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.CreateShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.DeleteShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.GetShoppingListQuery;
import com.superfercho.shopping.application.dto.shoppinglist.RemoveProductFromShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.RenameShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ShoppingListItemResponse;
import com.superfercho.shopping.application.dto.shoppinglist.ShoppingListResponse;
import com.superfercho.shopping.application.exception.ProductNotFoundException;
import com.superfercho.shopping.application.exception.ShoppingListNotFoundException;
import com.superfercho.shopping.application.port.in.AddProductToShoppingListUseCase;
import com.superfercho.shopping.application.port.in.ChangeShoppingListItemQuantityUseCase;
import com.superfercho.shopping.application.port.in.ClearShoppingListUseCase;
import com.superfercho.shopping.application.port.in.CreateShoppingListUseCase;
import com.superfercho.shopping.application.port.in.DeleteShoppingListUseCase;
import com.superfercho.shopping.application.port.in.GetShoppingListUseCase;
import com.superfercho.shopping.application.port.in.ListShoppingListsUseCase;
import com.superfercho.shopping.application.port.in.RemoveProductFromShoppingListUseCase;
import com.superfercho.shopping.application.port.in.RenameShoppingListUseCase;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ShoppingListController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ShoppingExceptionHandler.class, ApiExceptionHandler.class})
class ShoppingListControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-04-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-04-01T10:05:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID LIST_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID ITEM_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateShoppingListUseCase createShoppingListUseCase;

    @MockitoBean
    private ListShoppingListsUseCase listShoppingListsUseCase;

    @MockitoBean
    private GetShoppingListUseCase getShoppingListUseCase;

    @MockitoBean
    private RenameShoppingListUseCase renameShoppingListUseCase;

    @MockitoBean
    private AddProductToShoppingListUseCase addProductToShoppingListUseCase;

    @MockitoBean
    private ChangeShoppingListItemQuantityUseCase changeShoppingListItemQuantityUseCase;

    @MockitoBean
    private RemoveProductFromShoppingListUseCase removeProductFromShoppingListUseCase;

    @MockitoBean
    private ClearShoppingListUseCase clearShoppingListUseCase;

    @MockitoBean
    private DeleteShoppingListUseCase deleteShoppingListUseCase;

    @Test
    void shouldCreateShoppingList() throws Exception {
        when(createShoppingListUseCase.execute(any())).thenReturn(emptyList("Weekly groceries"));

        mockMvc.perform(post("/api/v1/shopping-lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Weekly groceries"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.endsWith("/api/v1/shopping-lists/" + LIST_ID)))
                .andExpect(jsonPath("$.id").value(LIST_ID.toString()))
                .andExpect(jsonPath("$.name").value("Weekly groceries"));

        verify(createShoppingListUseCase).execute(new CreateShoppingListCommand("Weekly groceries"));
    }

    @Test
    void shouldListShoppingLists() throws Exception {
        when(listShoppingListsUseCase.execute()).thenReturn(List.of(emptyList("Weekly groceries")));

        mockMvc.perform(get("/api/v1/shopping-lists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(LIST_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("Weekly groceries"));

        verify(listShoppingListsUseCase).execute();
    }

    @Test
    void shouldGetShoppingList() throws Exception {
        when(getShoppingListUseCase.execute(new GetShoppingListQuery(LIST_ID))).thenReturn(listWithItem(3));

        mockMvc.perform(get("/api/v1/shopping-lists/{shoppingListId}", LIST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(LIST_ID.toString()))
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.items[0].productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.updatedAt").value(UPDATED_AT.toString()));

        verify(getShoppingListUseCase).execute(new GetShoppingListQuery(LIST_ID));
    }

    @Test
    void shouldRenameShoppingList() throws Exception {
        when(renameShoppingListUseCase.execute(any())).thenReturn(emptyList("Biweekly groceries"));

        mockMvc.perform(patch("/api/v1/shopping-lists/{shoppingListId}", LIST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Biweekly groceries"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Biweekly groceries"));

        verify(renameShoppingListUseCase).execute(new RenameShoppingListCommand(LIST_ID, "Biweekly groceries"));
    }

    @Test
    void shouldAddProductToShoppingList() throws Exception {
        when(addProductToShoppingListUseCase.execute(any())).thenReturn(listWithItem(2));

        mockMvc.perform(post("/api/v1/shopping-lists/{shoppingListId}/items", LIST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","quantity":2}
                                """.formatted(PRODUCT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        verify(addProductToShoppingListUseCase)
                .execute(new AddProductToShoppingListCommand(LIST_ID, PRODUCT_ID, 2));
    }

    @Test
    void shouldChangeShoppingListItemQuantity() throws Exception {
        when(changeShoppingListItemQuantityUseCase.execute(any())).thenReturn(listWithItem(4));

        mockMvc.perform(patch("/api/v1/shopping-lists/{shoppingListId}/items/{productId}", LIST_ID, PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":4}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(4));

        verify(changeShoppingListItemQuantityUseCase)
                .execute(new ChangeShoppingListItemQuantityCommand(LIST_ID, PRODUCT_ID, 4));
    }

    @Test
    void shouldRemoveProductFromShoppingList() throws Exception {
        when(removeProductFromShoppingListUseCase.execute(any())).thenReturn(emptyList("Weekly groceries"));

        mockMvc.perform(delete("/api/v1/shopping-lists/{shoppingListId}/items/{productId}", LIST_ID, PRODUCT_ID))
                .andExpect(status().isNoContent());

        verify(removeProductFromShoppingListUseCase)
                .execute(new RemoveProductFromShoppingListCommand(LIST_ID, PRODUCT_ID));
    }

    @Test
    void shouldClearShoppingList() throws Exception {
        when(clearShoppingListUseCase.execute(any())).thenReturn(emptyList("Weekly groceries"));

        mockMvc.perform(delete("/api/v1/shopping-lists/{shoppingListId}/items", LIST_ID))
                .andExpect(status().isNoContent());

        verify(clearShoppingListUseCase).execute(new ClearShoppingListCommand(LIST_ID));
    }

    @Test
    void shouldDeleteShoppingList() throws Exception {
        mockMvc.perform(delete("/api/v1/shopping-lists/{shoppingListId}", LIST_ID))
                .andExpect(status().isNoContent());

        verify(deleteShoppingListUseCase).execute(new DeleteShoppingListCommand(LIST_ID));
    }

    @Test
    void shouldMapShoppingListNotFoundOnDeleteTo404() throws Exception {
        org.mockito.Mockito.doThrow(new ShoppingListNotFoundException(LIST_ID))
                .when(deleteShoppingListUseCase)
                .execute(any());

        mockMvc.perform(delete("/api/v1/shopping-lists/{shoppingListId}", LIST_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SHOPPING_LIST_NOT_FOUND"));
    }

    @Test
    void shouldMapShoppingListNotFoundTo404() throws Exception {
        when(getShoppingListUseCase.execute(any())).thenThrow(new ShoppingListNotFoundException(LIST_ID));

        mockMvc.perform(get("/api/v1/shopping-lists/{shoppingListId}", LIST_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SHOPPING_LIST_NOT_FOUND"));
    }

    @Test
    void shouldMapProductNotFoundTo404() throws Exception {
        when(addProductToShoppingListUseCase.execute(any())).thenThrow(new ProductNotFoundException(PRODUCT_ID));

        mockMvc.perform(post("/api/v1/shopping-lists/{shoppingListId}/items", LIST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","quantity":2}
                                """.formatted(PRODUCT_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void shouldRejectBlankName() throws Exception {
        mockMvc.perform(post("/api/v1/shopping-lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  "}
                                """))
                .andExpect(status().isBadRequest());
    }

    private static ShoppingListResponse emptyList(String name) {
        return new ShoppingListResponse(LIST_ID, CUSTOMER_ID, name, List.of(), CREATED_AT, UPDATED_AT);
    }

    private static ShoppingListResponse listWithItem(int quantity) {
        return new ShoppingListResponse(
                LIST_ID,
                CUSTOMER_ID,
                "Weekly groceries",
                List.of(new ShoppingListItemResponse(ITEM_ID, PRODUCT_ID, quantity, CREATED_AT)),
                CREATED_AT,
                UPDATED_AT);
    }
}
