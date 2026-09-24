package com.superfercho.identity.infrastructure.rest;

import com.superfercho.identity.application.usecase.ExitStorefrontPreviewUseCase;
import com.superfercho.identity.application.usecase.GetStorefrontPreviewUseCase;
import com.superfercho.identity.application.usecase.StartStorefrontPreviewUseCase;
import com.superfercho.identity.infrastructure.rest.dto.StorefrontPreviewSessionRestResponse;
import com.superfercho.identity.infrastructure.rest.dto.StorefrontPreviewStatusRestResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!test")
@RequestMapping("/api/v1/admin/storefront-preview")
public class StorefrontPreviewController {

    private final StartStorefrontPreviewUseCase startStorefrontPreviewUseCase;
    private final GetStorefrontPreviewUseCase getStorefrontPreviewUseCase;
    private final ExitStorefrontPreviewUseCase exitStorefrontPreviewUseCase;

    public StorefrontPreviewController(
            StartStorefrontPreviewUseCase startStorefrontPreviewUseCase,
            GetStorefrontPreviewUseCase getStorefrontPreviewUseCase,
            ExitStorefrontPreviewUseCase exitStorefrontPreviewUseCase) {
        this.startStorefrontPreviewUseCase = startStorefrontPreviewUseCase;
        this.getStorefrontPreviewUseCase = getStorefrontPreviewUseCase;
        this.exitStorefrontPreviewUseCase = exitStorefrontPreviewUseCase;
    }

    @PostMapping
    public StorefrontPreviewSessionRestResponse start() {
        return StorefrontPreviewSessionRestResponse.from(startStorefrontPreviewUseCase.execute());
    }

    @GetMapping
    public StorefrontPreviewStatusRestResponse get() {
        return StorefrontPreviewStatusRestResponse.from(getStorefrontPreviewUseCase.execute());
    }

    @PostMapping("/exit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void exit() {
        exitStorefrontPreviewUseCase.execute();
    }
}
