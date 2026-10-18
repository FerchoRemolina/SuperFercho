package com.superfercho.storefront.infrastructure.rest;

import com.superfercho.storefront.application.dto.CreateStorefrontFaqCommand;
import com.superfercho.storefront.application.dto.ReorderStorefrontFaqsCommand;
import com.superfercho.storefront.application.dto.UpdateStorefrontFaqCommand;
import com.superfercho.storefront.application.dto.UpdateStorefrontFooterSettingsCommand;
import com.superfercho.storefront.application.usecase.CreateStorefrontFaqUseCase;
import com.superfercho.storefront.application.usecase.DeleteStorefrontFaqUseCase;
import com.superfercho.storefront.application.usecase.ListStorefrontFaqsUseCase;
import com.superfercho.storefront.application.usecase.ReorderStorefrontFaqsUseCase;
import com.superfercho.storefront.application.usecase.UpdateStorefrontFooterSettingsUseCase;
import com.superfercho.storefront.application.usecase.UpdateStorefrontFaqUseCase;
import com.superfercho.storefront.application.usecase.UpdateStorefrontHeroUseCase;
import com.superfercho.storefront.application.usecase.UpdateStorefrontQrImageUseCase;
import com.superfercho.storefront.infrastructure.rest.dto.FaqAdminRestResponse;
import com.superfercho.storefront.infrastructure.rest.dto.QrImageRestResponse;
import com.superfercho.storefront.infrastructure.rest.dto.ReorderStorefrontFaqsRequest;
import com.superfercho.storefront.infrastructure.rest.dto.FooterSettingsRestResponse;
import com.superfercho.storefront.infrastructure.rest.dto.UpdateFooterSettingsRequest;
import com.superfercho.storefront.infrastructure.rest.dto.UpsertStorefrontFaqRequest;
import com.superfercho.media.application.MediaImageFormat;
import com.superfercho.media.infrastructure.web.ImageSniffer;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Administración de FAQs, ajustes del footer y la imagen del QR (ADMIN;
 * seguridad en IdentitySecurityConfiguration).
 */
@RestController
@Profile("!test")
@RequestMapping("/api/v1/admin/storefront-content")
public class AdminStorefrontContentController {

    private final UpdateStorefrontHeroUseCase updateStorefrontHeroUseCase;
    private final ListStorefrontFaqsUseCase listStorefrontFaqsUseCase;
    private final CreateStorefrontFaqUseCase createStorefrontFaqUseCase;
    private final UpdateStorefrontFaqUseCase updateStorefrontFaqUseCase;
    private final ReorderStorefrontFaqsUseCase reorderStorefrontFaqsUseCase;
    private final DeleteStorefrontFaqUseCase deleteStorefrontFaqUseCase;
    private final UpdateStorefrontFooterSettingsUseCase updateStorefrontFooterSettingsUseCase;
    private final UpdateStorefrontQrImageUseCase updateStorefrontQrImageUseCase;
    private final ImageSniffer imageSniffer;

    public AdminStorefrontContentController(
            UpdateStorefrontHeroUseCase updateStorefrontHeroUseCase,
            ListStorefrontFaqsUseCase listStorefrontFaqsUseCase,
            CreateStorefrontFaqUseCase createStorefrontFaqUseCase,
            UpdateStorefrontFaqUseCase updateStorefrontFaqUseCase,
            ReorderStorefrontFaqsUseCase reorderStorefrontFaqsUseCase,
            DeleteStorefrontFaqUseCase deleteStorefrontFaqUseCase,
            UpdateStorefrontFooterSettingsUseCase updateStorefrontFooterSettingsUseCase,
            UpdateStorefrontQrImageUseCase updateStorefrontQrImageUseCase,
            ImageSniffer imageSniffer) {
        this.updateStorefrontHeroUseCase = updateStorefrontHeroUseCase;
        this.listStorefrontFaqsUseCase = listStorefrontFaqsUseCase;
        this.createStorefrontFaqUseCase = createStorefrontFaqUseCase;
        this.updateStorefrontFaqUseCase = updateStorefrontFaqUseCase;
        this.reorderStorefrontFaqsUseCase = reorderStorefrontFaqsUseCase;
        this.deleteStorefrontFaqUseCase = deleteStorefrontFaqUseCase;
        this.updateStorefrontFooterSettingsUseCase = updateStorefrontFooterSettingsUseCase;
        this.updateStorefrontQrImageUseCase = updateStorefrontQrImageUseCase;
        this.imageSniffer = imageSniffer;
    }

    @PutMapping("/hero")
    public com.superfercho.storefront.infrastructure.rest.dto.HeroContentRestResponse update(
            @RequestBody com.superfercho.storefront.infrastructure.rest.dto.UpdateHeroContentRequest request) {
        return com.superfercho.storefront.infrastructure.rest.dto.HeroContentRestResponse.from(
                updateStorefrontHeroUseCase.execute(
                        com.superfercho.storefront.application.dto.UpdateStorefrontHeroCommand.of(
                                request.title(),
                                request.description(),
                                request.ctaLabel(),
                                request.imageUrl(),
                                request.mode())));
    }

    @GetMapping("/faqs")
    public List<FaqAdminRestResponse> listFaqs() {
        return listStorefrontFaqsUseCase.execute().stream()
                .map(FaqAdminRestResponse::from)
                .toList();
    }

    @PostMapping("/faqs")
    public FaqAdminRestResponse createFaq(@RequestBody UpsertStorefrontFaqRequest request) {
        return FaqAdminRestResponse.from(createStorefrontFaqUseCase.execute(
                new CreateStorefrontFaqCommand(
                        request.question(),
                        request.answer(),
                        request.displayOrder(),
                        request.active())));
    }

    @PutMapping("/faqs/{faqId}")
    public FaqAdminRestResponse updateFaq(
            @PathVariable UUID faqId, @RequestBody UpsertStorefrontFaqRequest request) {
        return FaqAdminRestResponse.from(updateStorefrontFaqUseCase.execute(
                new UpdateStorefrontFaqCommand(
                        faqId,
                        request.question(),
                        request.answer(),
                        request.displayOrder(),
                        request.active())));
    }

    @PutMapping("/faqs/order")
    public List<FaqAdminRestResponse> reorderFaqs(@RequestBody ReorderStorefrontFaqsRequest request) {
        return reorderStorefrontFaqsUseCase
                .execute(new ReorderStorefrontFaqsCommand(request.orderedIds()))
                .stream()
                .map(FaqAdminRestResponse::from)
                .toList();
    }

    @DeleteMapping("/faqs/{faqId}")
    public void deleteFaq(@PathVariable UUID faqId) {
        deleteStorefrontFaqUseCase.execute(faqId);
    }

    @PutMapping("/footer-settings")
    public FooterSettingsRestResponse updateFooterSettings(
            @RequestBody UpdateFooterSettingsRequest request) {
        return FooterSettingsRestResponse.from(updateStorefrontFooterSettingsUseCase.execute(
                new UpdateStorefrontFooterSettingsCommand(
                        request.supportEmail(),
                        request.linkedinUrl(),
                        request.githubUrl())));
    }

    @PostMapping(
            value = "/footer-settings/qr-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public QrImageRestResponse uploadQrImage(@RequestParam("file") MultipartFile file)
            throws IOException {
        byte[] content = file.getBytes();
        MediaImageFormat format = imageSniffer.detect(content);
        var stored = updateStorefrontQrImageUseCase.execute(
                content,
                format.extension(),
                format.contentType());
        return new QrImageRestResponse(stored.qrImageUrl());
    }

    @DeleteMapping("/footer-settings/qr-image")
    public QrImageRestResponse deleteQrImage() {
        return new QrImageRestResponse(updateStorefrontQrImageUseCase.removeImage().qrImageUrl());
    }
}
