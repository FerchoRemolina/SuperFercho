package com.superfercho.identity.infrastructure.rest;

import com.superfercho.identity.application.dto.AddAddressCommand;
import com.superfercho.identity.application.dto.DeactivateAddressCommand;
import com.superfercho.identity.application.dto.SetDefaultAddressCommand;
import com.superfercho.identity.application.dto.UpdateAddressCommand;
import com.superfercho.identity.application.usecase.AddAddressUseCase;
import com.superfercho.identity.application.usecase.DeactivateAddressUseCase;
import com.superfercho.identity.application.usecase.ListAddressesUseCase;
import com.superfercho.identity.application.usecase.SetDefaultAddressUseCase;
import com.superfercho.identity.application.usecase.UpdateAddressUseCase;
import com.superfercho.identity.infrastructure.rest.dto.AddAddressRequest;
import com.superfercho.identity.infrastructure.rest.dto.AddressRestResponse;
import com.superfercho.identity.infrastructure.rest.dto.UpdateAddressRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@Profile("!test")
@RequestMapping("/api/v1/addresses")
public class AddressController {

    private final AddAddressUseCase addAddressUseCase;
    private final ListAddressesUseCase listAddressesUseCase;
    private final UpdateAddressUseCase updateAddressUseCase;
    private final DeactivateAddressUseCase deactivateAddressUseCase;
    private final SetDefaultAddressUseCase setDefaultAddressUseCase;

    public AddressController(
            AddAddressUseCase addAddressUseCase,
            ListAddressesUseCase listAddressesUseCase,
            UpdateAddressUseCase updateAddressUseCase,
            DeactivateAddressUseCase deactivateAddressUseCase,
            SetDefaultAddressUseCase setDefaultAddressUseCase) {
        this.addAddressUseCase = addAddressUseCase;
        this.listAddressesUseCase = listAddressesUseCase;
        this.updateAddressUseCase = updateAddressUseCase;
        this.deactivateAddressUseCase = deactivateAddressUseCase;
        this.setDefaultAddressUseCase = setDefaultAddressUseCase;
    }

    @PostMapping
    public ResponseEntity<AddressRestResponse> add(@RequestBody AddAddressRequest request) {
        AddressRestResponse body = AddressRestResponse.from(addAddressUseCase.execute(new AddAddressCommand(
                request.label(),
                request.recipientName(),
                request.addressLine(),
                request.additionalInfo(),
                request.city(),
                request.department(),
                request.phone(),
                request.isDefault())));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{addressId}")
                        .buildAndExpand(body.id())
                        .toUri())
                .body(body);
    }

    @GetMapping
    public List<AddressRestResponse> list() {
        return listAddressesUseCase.execute().stream().map(AddressRestResponse::from).toList();
    }

    @PutMapping("/{addressId}")
    public AddressRestResponse update(
            @PathVariable UUID addressId, @RequestBody UpdateAddressRequest request) {
        return AddressRestResponse.from(updateAddressUseCase.execute(new UpdateAddressCommand(
                addressId,
                request.label(),
                request.recipientName(),
                request.addressLine(),
                request.additionalInfo(),
                request.city(),
                request.department(),
                request.phone())));
    }

    @DeleteMapping("/{addressId}")
    public AddressRestResponse deactivate(@PathVariable UUID addressId) {
        return AddressRestResponse.from(
                deactivateAddressUseCase.execute(new DeactivateAddressCommand(addressId)));
    }

    @PostMapping("/{addressId}/default")
    public AddressRestResponse setDefault(@PathVariable UUID addressId) {
        return AddressRestResponse.from(
                setDefaultAddressUseCase.execute(new SetDefaultAddressCommand(addressId)));
    }
}
