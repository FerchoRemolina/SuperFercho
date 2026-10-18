package com.superfercho.storefront.infrastructure.rest;

import com.superfercho.storefront.application.dto.StorefrontFooterSettingsResult;
import com.superfercho.storefront.application.usecase.GetStorefrontFooterSettingsUseCase;
import com.superfercho.storefront.application.usecase.GetStorefrontHeroUseCase;
import com.superfercho.storefront.application.usecase.ListActiveStorefrontFaqsUseCase;
import com.superfercho.storefront.domain.model.StorefrontFaq;
import com.superfercho.storefront.infrastructure.rest.dto.FooterSettingsRestResponse;
import com.superfercho.storefront.infrastructure.rest.dto.FaqPublicRestResponse;
import com.superfercho.storefront.infrastructure.rest.dto.HeroContentRestResponse;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lectura pública del contenido de la tienda: se muestra sin sesión. */
@RestController
@Profile("!test")
@RequestMapping("/api/v1/storefront-content")
public class StorefrontContentController {

    private final GetStorefrontHeroUseCase getStorefrontHeroUseCase;
    private final ListActiveStorefrontFaqsUseCase listActiveStorefrontFaqsUseCase;
    private final GetStorefrontFooterSettingsUseCase getStorefrontFooterSettingsUseCase;

    public StorefrontContentController(
            GetStorefrontHeroUseCase getStorefrontHeroUseCase,
            ListActiveStorefrontFaqsUseCase listActiveStorefrontFaqsUseCase,
            GetStorefrontFooterSettingsUseCase getStorefrontFooterSettingsUseCase) {
        this.getStorefrontHeroUseCase = getStorefrontHeroUseCase;
        this.listActiveStorefrontFaqsUseCase = listActiveStorefrontFaqsUseCase;
        this.getStorefrontFooterSettingsUseCase = getStorefrontFooterSettingsUseCase;
    }

    @GetMapping("/hero")
    public HeroContentRestResponse hero() {
        return HeroContentRestResponse.from(getStorefrontHeroUseCase.execute());
    }

    @GetMapping("/faqs")
    public List<FaqPublicRestResponse> faqs() {
        return listActiveStorefrontFaqsUseCase.execute().stream()
                .map(FaqPublicRestResponse::from)
                .toList();
    }

    @GetMapping("/footer-settings")
    public FooterSettingsRestResponse footerSettings() {
        return FooterSettingsRestResponse.from(getStorefrontFooterSettingsUseCase.execute());
    }
}
