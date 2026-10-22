package com.superfercho.identity.infrastructure.rest;

import com.superfercho.identity.application.usecase.CloseCustomerAccountUseCase;
import com.superfercho.identity.application.usecase.GetCustomerProfileUseCase;
import com.superfercho.identity.application.usecase.RegisterCustomerUseCase;
import com.superfercho.identity.application.usecase.UpdateCustomerProfileUseCase;
import com.superfercho.identity.application.dto.RegisterCustomerCommand;
import com.superfercho.identity.application.dto.UpdateCustomerProfileCommand;
import com.superfercho.identity.infrastructure.rest.dto.CustomerProfileRestResponse;
import com.superfercho.identity.infrastructure.rest.dto.RegisterCustomerRequest;
import com.superfercho.identity.infrastructure.rest.dto.RegisteredCustomerRestResponse;
import com.superfercho.identity.infrastructure.rest.dto.UpdateCustomerProfileRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!test")
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final RegisterCustomerUseCase registerCustomerUseCase;
    private final CloseCustomerAccountUseCase closeCustomerAccountUseCase;
    private final GetCustomerProfileUseCase getCustomerProfileUseCase;
    private final UpdateCustomerProfileUseCase updateCustomerProfileUseCase;

    public CustomerController(
            RegisterCustomerUseCase registerCustomerUseCase,
            CloseCustomerAccountUseCase closeCustomerAccountUseCase,
            GetCustomerProfileUseCase getCustomerProfileUseCase,
            UpdateCustomerProfileUseCase updateCustomerProfileUseCase) {
        this.registerCustomerUseCase = registerCustomerUseCase;
        this.closeCustomerAccountUseCase = closeCustomerAccountUseCase;
        this.getCustomerProfileUseCase = getCustomerProfileUseCase;
        this.updateCustomerProfileUseCase = updateCustomerProfileUseCase;
    }

    @PostMapping
    public ResponseEntity<RegisteredCustomerRestResponse> register(@RequestBody RegisterCustomerRequest request) {
        RegisteredCustomerRestResponse body = RegisteredCustomerRestResponse.from(
                registerCustomerUseCase.execute(new RegisterCustomerCommand(
                        request.documentType(),
                        request.documentNumber(),
                        request.firstName(),
                        request.lastName(),
                        request.email(),
                        request.phone(),
                        request.password())));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/me")
    public CustomerProfileRestResponse myProfile() {
        return CustomerProfileRestResponse.from(getCustomerProfileUseCase.execute());
    }

    @PutMapping("/me")
    public CustomerProfileRestResponse updateMyProfile(@RequestBody UpdateCustomerProfileRequest request) {
        return CustomerProfileRestResponse.from(updateCustomerProfileUseCase.execute(
                new UpdateCustomerProfileCommand(
                        request.firstName(),
                        request.lastName(),
                        request.email(),
                        request.phone(),
                        request.documentType(),
                        request.documentNumber())));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> closeMyAccount() {
        closeCustomerAccountUseCase.execute();
        return ResponseEntity.noContent().build();
    }
}
