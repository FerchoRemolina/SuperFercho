import type {
  PackageProposal,
  PackageProposalOption,
  SensitiveActionType,
} from "@/features/assistant/api";

export type ChatThreadMessage = {
  id: string;
  role: "user" | "assistant";
  content: string;
};

export type PendingConfirmation = {
  token: string;
  type: SensitiveActionType;
};

export function confirmationTitle(type: SensitiveActionType): string {
  switch (type) {
    case "CHECKOUT":
      return "Confirmar pedido";
    case "CANCEL_ORDER":
      return "Confirmar cancelación";
  }
}

export function confirmationDescription(type: SensitiveActionType): string {
  switch (type) {
    case "CHECKOUT":
      return "Fercho preparó un pedido. Confirma para finalizarlo desde el asistente.";
    case "CANCEL_ORDER":
      return "Fercho preparó la cancelación de un pedido. Confirma para aplicarla desde el asistente.";
  }
}

export function confirmationConfirmLabel(type: SensitiveActionType): string {
  switch (type) {
    case "CHECKOUT":
      return "Confirmar pedido";
    case "CANCEL_ORDER":
      return "Confirmar cancelación";
  }
}

export function packageProposalTitle(proposal: PackageProposal): string {
  return `Elige paquetes de ${proposal.productName}`;
}

export function packageOptionLabel(option: PackageProposalOption): string {
  return `${option.packageCount} paquete(s) · ${option.physicalTotalBase} ${option.baseUnit}`;
}

export function nextMessageId(
  messages: readonly ChatThreadMessage[],
  prefix: string,
): string {
  return `${prefix}-${messages.length + 1}`;
}
