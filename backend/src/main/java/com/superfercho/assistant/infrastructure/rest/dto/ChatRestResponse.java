package com.superfercho.assistant.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.superfercho.assistant.application.confirmation.SensitiveActionType;
import com.superfercho.assistant.application.dto.chat.ChatResponse;
import com.superfercho.assistant.application.dto.chat.PackageProposalView;
import java.util.UUID;

public record ChatRestResponse(
        UUID conversationId,
        String assistantMessage,
        boolean awaitingConfirmation,
        String confirmationToken,
        SensitiveActionType confirmationType,
        boolean authenticationRequired,
        @JsonInclude(JsonInclude.Include.ALWAYS) PackageProposalView packageProposal) {

    public static ChatRestResponse from(ChatResponse response) {
        return new ChatRestResponse(
                response.conversationId(),
                response.assistantMessage(),
                response.awaitingConfirmation(),
                response.confirmationToken(),
                response.confirmationType(),
                response.authenticationRequired(),
                response.packageProposal());
    }
}
