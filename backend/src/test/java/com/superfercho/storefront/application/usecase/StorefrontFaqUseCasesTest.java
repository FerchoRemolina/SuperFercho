package com.superfercho.storefront.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.storefront.application.dto.CreateStorefrontFaqCommand;
import com.superfercho.storefront.application.dto.ReorderStorefrontFaqsCommand;
import com.superfercho.storefront.application.dto.UpdateStorefrontFaqCommand;
import com.superfercho.storefront.application.exception.StorefrontContentNotFoundException;
import com.superfercho.storefront.application.port.ClockPort;
import com.superfercho.storefront.application.port.StorefrontFaqRepository;
import com.superfercho.storefront.domain.exception.InvalidStorefrontContentException;
import com.superfercho.storefront.domain.model.StorefrontFaq;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorefrontFaqUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");
    private static final UUID FAQ_ID = UUID.fromString("00000000-0000-0000-0000-00000000fa01");
    private static final UUID FAQ_ID_2 = UUID.fromString("00000000-0000-0000-0000-00000000fa02");

    @Mock
    private StorefrontFaqRepository faqRepository;

    @Mock
    private ClockPort clockPort;

    private CreateStorefrontFaqUseCase createUseCase;
    private UpdateStorefrontFaqUseCase updateUseCase;
    private ReorderStorefrontFaqsUseCase reorderUseCase;
    private DeleteStorefrontFaqUseCase deleteUseCase;
    private ListStorefrontFaqsUseCase listAllUseCase;
    private com.superfercho.storefront.application.usecase.ListActiveStorefrontFaqsUseCase listActiveUseCase;

    @BeforeEach
    void setUp() {
        createUseCase = new CreateStorefrontFaqUseCase(faqRepository, clockPort);
        updateUseCase = new UpdateStorefrontFaqUseCase(faqRepository, clockPort);
        reorderUseCase = new ReorderStorefrontFaqsUseCase(faqRepository, clockPort);
        deleteUseCase = new DeleteStorefrontFaqUseCase(faqRepository);
        listAllUseCase = new ListStorefrontFaqsUseCase(faqRepository);
        listActiveUseCase = new com.superfercho.storefront.application.usecase.ListActiveStorefrontFaqsUseCase(faqRepository);
    }

    @Test
    void createsFaqWithClockTimestamp() {
        when(clockPort.currentTime()).thenReturn(NOW);
        when(faqRepository.save(any(StorefrontFaq.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StorefrontFaq faq = createUseCase.execute(
                new CreateStorefrontFaqCommand("¿P?", "R.", 10, true));

        assertEquals("¿P?", faq.question());
        assertEquals(NOW, faq.createdAt());
        verify(faqRepository).save(faq);
    }

    @Test
    void updatesExistingFaqRefreshingUpdatedAt() {
        StorefrontFaq existing = StorefrontFaq.create(FAQ_ID, "P", "R", 10, true, NOW);
        when(faqRepository.findById(FAQ_ID)).thenReturn(Optional.of(existing));
        when(clockPort.currentTime()).thenReturn(NOW.plusSeconds(60));
        when(faqRepository.save(any(StorefrontFaq.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StorefrontFaq updated = updateUseCase.execute(
                new UpdateStorefrontFaqCommand(FAQ_ID, "P2", "R2", 20, false));

        assertEquals("P2", updated.question());
        assertEquals(20, updated.displayOrder());
        assertEquals(false, updated.active());
        assertEquals(NOW.plusSeconds(60), updated.updatedAt());
    }

    @Test
    void updateRejectsUnknownFaq() {
        when(faqRepository.findById(FAQ_ID)).thenReturn(Optional.empty());

        assertThrows(
                StorefrontContentNotFoundException.class,
                () -> updateUseCase.execute(new UpdateStorefrontFaqCommand(FAQ_ID, "P", "R", 0, true)));
    }

    @Test
    void reorderAssignsSequentialDisplayOrder() {
        StorefrontFaq first = StorefrontFaq.create(FAQ_ID, "P1", "R1", 0, true, NOW);
        StorefrontFaq second = StorefrontFaq.create(FAQ_ID_2, "P2", "R2", 10, true, NOW);
        when(faqRepository.findAllOrdered()).thenReturn(List.of(first, second));
        when(faqRepository.findById(FAQ_ID_2)).thenReturn(Optional.of(
                StorefrontFaq.create(FAQ_ID_2, "P2", "R2", 10, true, NOW)));
        when(faqRepository.findById(FAQ_ID)).thenReturn(Optional.of(
                StorefrontFaq.create(FAQ_ID, "P1", "R1", 0, true, NOW)));
        when(clockPort.currentTime()).thenReturn(NOW);
        when(faqRepository.save(any(StorefrontFaq.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<StorefrontFaq> reordered = reorderUseCase.execute(
                new ReorderStorefrontFaqsCommand(List.of(FAQ_ID_2, FAQ_ID)));

        assertEquals(0, reordered.get(0).displayOrder());
        assertEquals(FAQ_ID_2, reordered.get(0).id());
        assertEquals(1, reordered.get(1).displayOrder());
    }

    @Test
    void reorderRejectsDuplicatesAndIncompleteLists() {
        StorefrontFaq first = StorefrontFaq.create(FAQ_ID, "P1", "R1", 0, true, NOW);
        StorefrontFaq second = StorefrontFaq.create(FAQ_ID_2, "P2", "R2", 10, true, NOW);
        when(faqRepository.findAllOrdered()).thenReturn(List.of(first, second));

        assertThrows(
                InvalidStorefrontContentException.class,
                () -> reorderUseCase.execute(new ReorderStorefrontFaqsCommand(List.of(FAQ_ID, FAQ_ID))));
        assertThrows(
                InvalidStorefrontContentException.class,
                () -> reorderUseCase.execute(new ReorderStorefrontFaqsCommand(List.of(FAQ_ID))));
    }

    @Test
    void deleteRemovesFaq() {
        when(faqRepository.findById(FAQ_ID))
                .thenReturn(Optional.of(StorefrontFaq.create(FAQ_ID, "P", "R", 10, true, NOW)));

        deleteUseCase.execute(FAQ_ID);
        verify(faqRepository).deleteById(FAQ_ID);
    }

    @Test
    void deleteRejectsUnknownFaq() {
        when(faqRepository.findById(FAQ_ID)).thenReturn(Optional.empty());

        assertThrows(
                StorefrontContentNotFoundException.class, () -> deleteUseCase.execute(FAQ_ID));
    }

    @Test
    void listingReturnsRepositoryOrder() {
        StorefrontFaq inactive = StorefrontFaq.create(FAQ_ID, "P", "R", 10, false, NOW);
        when(faqRepository.findAllOrdered()).thenReturn(List.of(
                StorefrontFaq.create(FAQ_ID, "P1", "R1", 0, true, NOW), inactive));
        when(faqRepository.findActiveOrdered()).thenReturn(List.of(
                StorefrontFaq.create(FAQ_ID, "P1", "R1", 0, true, NOW)));

        assertEquals(2, listAllUseCase.execute().size());
        assertEquals(1, listActiveUseCase.execute().size());
        assertTrue(listActiveUseCase.execute().get(0).active());
    }
}
