package com.superfercho.assistant.application.tool.catalog;

import com.superfercho.assistant.application.packagesel.PackageSelectionDurations;
import com.superfercho.assistant.application.packagesel.PackageSelectionOption;
import com.superfercho.assistant.application.packagesel.PackageSelectionProposal;
import com.superfercho.assistant.application.packagesel.PackageSelectionProposalStatus;
import com.superfercho.assistant.application.port.out.ClockPort;
import com.superfercho.assistant.application.port.out.CurrentUserProvider;
import com.superfercho.assistant.application.port.out.PackageSelectionProposalStore;
import com.superfercho.assistant.application.tool.AssistantTool;
import com.superfercho.assistant.application.tool.ToolAccess;
import com.superfercho.assistant.application.tool.ToolArguments;
import com.superfercho.assistant.application.tool.ToolNames;
import com.superfercho.assistant.application.tool.ToolParameter;
import com.superfercho.assistant.application.tool.ToolResult;
import com.superfercho.assistant.application.tool.ToolRisk;
import com.superfercho.assistant.application.tool.ToolSchema;
import com.superfercho.catalog.application.dto.QuantityEquivalenceCommand;
import com.superfercho.catalog.application.dto.QuantityEquivalenceResult;
import com.superfercho.catalog.application.dto.QuantityEquivalenceStatus;
import com.superfercho.catalog.application.dto.QuantityPackageOption;
import com.superfercho.catalog.application.usecase.ResolveProductQuantityEquivalenceUseCase;
import com.superfercho.catalog.domain.model.PresentationUnit;
import com.superfercho.identity.application.exception.UnauthenticatedUserException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ResolveQuantityEquivalenceTool implements AssistantTool {

    private static final Set<String> REQUEST_UNITS = Set.of("G", "KG", "ML", "L");

    private static final String AVAILABLE = "AVAILABLE";

    private final ResolveProductQuantityEquivalenceUseCase resolveProductQuantityEquivalenceUseCase;
    private final CurrentUserProvider currentUserProvider;
    private final ClockPort clockPort;
    private final PackageSelectionProposalStore packageSelectionProposalStore;

    public ResolveQuantityEquivalenceTool(
            ResolveProductQuantityEquivalenceUseCase resolveProductQuantityEquivalenceUseCase,
            CurrentUserProvider currentUserProvider,
            ClockPort clockPort,
            PackageSelectionProposalStore packageSelectionProposalStore) {
        this.resolveProductQuantityEquivalenceUseCase = resolveProductQuantityEquivalenceUseCase;
        this.currentUserProvider = currentUserProvider;
        this.clockPort = clockPort;
        this.packageSelectionProposalStore = packageSelectionProposalStore;
    }

    @Override
    public String name() {
        return ToolNames.RESOLVE_QUANTITY_EQUIVALENCE;
    }

    @Override
    public String description() {
        return "Convert a requested physical quantity (G, KG, ML, L) into whole catalog packages for one public SKU. Does not modify the cart. exactMatch means an integer package count; otherwise lowerAlternative and upperAlternative are choices, not a purchase.";
    }

    @Override
    public ToolSchema schema() {
        return ToolSchema.of(
                new ToolParameter("productId", "uuid", true, "Catalog product id"),
                new ToolParameter("quantity", "number", true, "Positive physical quantity, at most three decimal places"),
                new ToolParameter("unit", "string", true, "Requested unit: G, KG, ML, or L"));
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.QUERY;
    }

    @Override
    public ToolAccess access() {
        return ToolAccess.PUBLIC;
    }

    @Override
    public ToolResult execute(ToolArguments arguments) {
        QuantityEquivalenceResult result = resolveProductQuantityEquivalenceUseCase.execute(
                new QuantityEquivalenceCommand(
                        arguments.requireUuid("productId"),
                        arguments.requirePositiveDecimal("quantity"),
                        PresentationUnit.valueOf(arguments.requireOneOf("unit", REQUEST_UNITS))));
        offerIfSelectable(result);
        return ToolResult.success(result.summary() + " result=" + result);
    }

    private void offerIfSelectable(QuantityEquivalenceResult result) {
        if (result.status() != QuantityEquivalenceStatus.EXACT
                && result.status() != QuantityEquivalenceStatus.INEXACT) {
            return;
        }
        if (!result.sellable() || result.presentation() == null || result.productName() == null) {
            return;
        }
        List<PackageSelectionOption> options = selectableOptions(result);
        if (options.isEmpty()) {
            return;
        }
        UUID userId;
        try {
            userId = currentUserProvider.getCurrentUserId();
        } catch (UnauthenticatedUserException ignored) {
            return;
        }
        Instant now = clockPort.currentTime();
        packageSelectionProposalStore.replaceOffered(
                new PackageSelectionProposal(
                        UUID.randomUUID(),
                        userId,
                        result.productId(),
                        result.productName(),
                        result.presentation().quantity(),
                        result.presentation().unit(),
                        options,
                        result.requestedQuantity(),
                        result.requestedUnit(),
                        PackageSelectionProposalStatus.OFFERED,
                        now,
                        now.plus(PackageSelectionDurations.PROPOSAL_TTL),
                        null),
                now);
    }

    private static List<PackageSelectionOption> selectableOptions(QuantityEquivalenceResult result) {
        List<PackageSelectionOption> options = new ArrayList<>();
        if (result.status() == QuantityEquivalenceStatus.EXACT
                && result.exactPackageCount() != null
                && AVAILABLE.equals(result.availability())) {
            options.add(new PackageSelectionOption(
                    result.exactPackageCount(), result.exactPhysicalTotalBase(), result.baseUnit()));
            return List.copyOf(options);
        }
        addIfAvailable(options, result.lowerAlternative());
        addIfAvailable(options, result.upperAlternative());
        return List.copyOf(options);
    }

    private static void addIfAvailable(List<PackageSelectionOption> options, QuantityPackageOption alternative) {
        if (alternative == null || !AVAILABLE.equals(alternative.availability())) {
            return;
        }
        options.add(new PackageSelectionOption(
                alternative.packageCount(), alternative.physicalTotalBase(), alternative.baseUnit()));
    }
}
