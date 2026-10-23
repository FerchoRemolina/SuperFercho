package com.superfercho.assistant.application.packagesel;

import com.superfercho.assistant.application.dto.chat.PackageProposalOptionView;
import com.superfercho.assistant.application.dto.chat.PackageProposalView;
import java.util.List;

public final class PackageProposalViews {

    private PackageProposalViews() {}

    public static PackageProposalView from(PackageSelectionProposal proposal) {
        List<PackageProposalOptionView> options = proposal.options().stream()
                .map(option -> new PackageProposalOptionView(
                        option.packageCount(), option.physicalTotalBase(), option.baseUnit()))
                .toList();
        return new PackageProposalView(
                proposal.proposalId(),
                proposal.productId(),
                proposal.productName(),
                proposal.presentationQuantity(),
                proposal.presentationUnit(),
                proposal.requestedQuantity(),
                proposal.requestedUnit(),
                options);
    }
}
