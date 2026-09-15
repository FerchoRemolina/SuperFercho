package com.superfercho.shopping.infrastructure.rest;

import com.superfercho.shopping.application.dto.shoppinglist.AddProductToShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ChangeShoppingListItemQuantityCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ClearShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.CreateShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.GetShoppingListQuery;
import com.superfercho.shopping.application.dto.shoppinglist.ListShoppingListsQuery;
import com.superfercho.shopping.application.dto.shoppinglist.RemoveProductFromShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.RenameShoppingListCommand;
import com.superfercho.shopping.application.port.in.AddProductToShoppingListUseCase;
import com.superfercho.shopping.application.port.in.ChangeShoppingListItemQuantityUseCase;
import com.superfercho.shopping.application.port.in.ClearShoppingListUseCase;
import com.superfercho.shopping.application.port.in.CreateShoppingListUseCase;
import com.superfercho.shopping.application.port.in.GetShoppingListUseCase;
import com.superfercho.shopping.application.port.in.ListShoppingListsUseCase;
import com.superfercho.shopping.application.port.in.RemoveProductFromShoppingListUseCase;
import com.superfercho.shopping.application.port.in.RenameShoppingListUseCase;
import com.superfercho.shopping.infrastructure.rest.dto.AddItemRequest;
import com.superfercho.shopping.infrastructure.rest.dto.ChangeItemQuantityRequest;
import com.superfercho.shopping.infrastructure.rest.dto.CreateShoppingListRequest;
import com.superfercho.shopping.infrastructure.rest.dto.RenameShoppingListRequest;
import com.superfercho.shopping.infrastructure.rest.dto.ShoppingListRestResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@Profile("!test")
@RequestMapping("/api/v1/customers/{customerId}/shopping-lists")
public class ShoppingListController {

    private final CreateShoppingListUseCase createShoppingListUseCase;
    private final ListShoppingListsUseCase listShoppingListsUseCase;
    private final GetShoppingListUseCase getShoppingListUseCase;
    private final RenameShoppingListUseCase renameShoppingListUseCase;
    private final AddProductToShoppingListUseCase addProductToShoppingListUseCase;
    private final ChangeShoppingListItemQuantityUseCase changeShoppingListItemQuantityUseCase;
    private final RemoveProductFromShoppingListUseCase removeProductFromShoppingListUseCase;
    private final ClearShoppingListUseCase clearShoppingListUseCase;

    public ShoppingListController(
            CreateShoppingListUseCase createShoppingListUseCase,
            ListShoppingListsUseCase listShoppingListsUseCase,
            GetShoppingListUseCase getShoppingListUseCase,
            RenameShoppingListUseCase renameShoppingListUseCase,
            AddProductToShoppingListUseCase addProductToShoppingListUseCase,
            ChangeShoppingListItemQuantityUseCase changeShoppingListItemQuantityUseCase,
            RemoveProductFromShoppingListUseCase removeProductFromShoppingListUseCase,
            ClearShoppingListUseCase clearShoppingListUseCase) {
        this.createShoppingListUseCase = createShoppingListUseCase;
        this.listShoppingListsUseCase = listShoppingListsUseCase;
        this.getShoppingListUseCase = getShoppingListUseCase;
        this.renameShoppingListUseCase = renameShoppingListUseCase;
        this.addProductToShoppingListUseCase = addProductToShoppingListUseCase;
        this.changeShoppingListItemQuantityUseCase = changeShoppingListItemQuantityUseCase;
        this.removeProductFromShoppingListUseCase = removeProductFromShoppingListUseCase;
        this.clearShoppingListUseCase = clearShoppingListUseCase;
    }

    @PostMapping
    public ResponseEntity<ShoppingListRestResponse> create(
            @PathVariable UUID customerId, @RequestBody CreateShoppingListRequest request) {
        ShoppingListRestResponse body = ShoppingListRestResponse.from(
                createShoppingListUseCase.execute(new CreateShoppingListCommand(customerId, request.name())));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{shoppingListId}")
                        .buildAndExpand(body.id())
                        .toUri())
                .body(body);
    }

    @GetMapping
    public List<ShoppingListRestResponse> list(@PathVariable UUID customerId) {
        return listShoppingListsUseCase.execute(new ListShoppingListsQuery(customerId)).stream()
                .map(ShoppingListRestResponse::from)
                .toList();
    }

    @GetMapping("/{shoppingListId}")
    public ShoppingListRestResponse get(@PathVariable UUID customerId, @PathVariable UUID shoppingListId) {
        return ShoppingListRestResponse.from(
                getShoppingListUseCase.execute(new GetShoppingListQuery(customerId, shoppingListId)));
    }

    @PatchMapping("/{shoppingListId}")
    public ShoppingListRestResponse rename(
            @PathVariable UUID customerId,
            @PathVariable UUID shoppingListId,
            @RequestBody RenameShoppingListRequest request) {
        return ShoppingListRestResponse.from(renameShoppingListUseCase.execute(
                new RenameShoppingListCommand(customerId, shoppingListId, request.name())));
    }

    @PostMapping("/{shoppingListId}/items")
    public ShoppingListRestResponse addItem(
            @PathVariable UUID customerId,
            @PathVariable UUID shoppingListId,
            @RequestBody AddItemRequest request) {
        return ShoppingListRestResponse.from(addProductToShoppingListUseCase.execute(
                new AddProductToShoppingListCommand(
                        customerId, shoppingListId, request.productId(), request.quantity())));
    }

    @PatchMapping("/{shoppingListId}/items/{productId}")
    public ShoppingListRestResponse changeQuantity(
            @PathVariable UUID customerId,
            @PathVariable UUID shoppingListId,
            @PathVariable UUID productId,
            @RequestBody ChangeItemQuantityRequest request) {
        return ShoppingListRestResponse.from(changeShoppingListItemQuantityUseCase.execute(
                new ChangeShoppingListItemQuantityCommand(
                        customerId, shoppingListId, productId, request.quantity())));
    }

    @DeleteMapping("/{shoppingListId}/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(
            @PathVariable UUID customerId, @PathVariable UUID shoppingListId, @PathVariable UUID productId) {
        removeProductFromShoppingListUseCase.execute(
                new RemoveProductFromShoppingListCommand(customerId, shoppingListId, productId));
    }

    @DeleteMapping("/{shoppingListId}/items")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@PathVariable UUID customerId, @PathVariable UUID shoppingListId) {
        clearShoppingListUseCase.execute(new ClearShoppingListCommand(customerId, shoppingListId));
    }
}
