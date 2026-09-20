package com.superfercho.identity.infrastructure.rest;

import com.superfercho.identity.application.dto.RegisterCustomerCommand;
import com.superfercho.identity.application.usecase.RegisterCustomerUseCase;
import com.superfercho.identity.infrastructure.rest.dto.RegisterCustomerRequest;
import com.superfercho.identity.infrastructure.rest.dto.RegisteredCustomerRestResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!test")
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final RegisterCustomerUseCase registerCustomerUseCase;

    public CustomerController(RegisterCustomerUseCase registerCustomerUseCase) {
        this.registerCustomerUseCase = registerCustomerUseCase;
    }

    @PostMapping
    public ResponseEntity<RegisteredCustomerRestResponse> register(@RequestBody RegisterCustomerRequest request) {
        RegisteredCustomerRestResponse body = RegisteredCustomerRestResponse.from(
                registerCustomerUseCase.execute(new RegisterCustomerCommand(
                        request.documentType(),
                        request.documentNumber(),
                        request.fullName(),
                        request.email(),
                        request.phone(),
                        request.password())));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
