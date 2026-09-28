package com.superfercho.identity.infrastructure.rest;

import com.superfercho.identity.application.dto.RegisterCustomerCommand;
import com.superfercho.identity.application.usecase.CloseCustomerAccountUseCase;
import com.superfercho.identity.application.usecase.RegisterCustomerUseCase;
import com.superfercho.identity.infrastructure.rest.dto.RegisterCustomerRequest;
import com.superfercho.identity.infrastructure.rest.dto.RegisteredCustomerRestResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!test")
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final RegisterCustomerUseCase registerCustomerUseCase;
    private final CloseCustomerAccountUseCase closeCustomerAccountUseCase;

    public CustomerController(
            RegisterCustomerUseCase registerCustomerUseCase,
            CloseCustomerAccountUseCase closeCustomerAccountUseCase) {
        this.registerCustomerUseCase = registerCustomerUseCase;
        this.closeCustomerAccountUseCase = closeCustomerAccountUseCase;
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

    @DeleteMapping("/me")
    public ResponseEntity<Void> closeMyAccount() {
        closeCustomerAccountUseCase.execute();
        return ResponseEntity.noContent().build();
    }
}
