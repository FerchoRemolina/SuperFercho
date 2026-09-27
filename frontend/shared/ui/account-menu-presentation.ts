/** Display helpers for the SiteHeader Cuenta dropdown. */

export function accountAvatarInitial(displayName: string): string {
  const trimmed = displayName.trim();
  if (!trimmed) {
    return "?";
  }
  return trimmed.charAt(0).toLocaleUpperCase("es-CO");
}

/**
 * Prefer the authenticated session firstName from login.
 * Preview customers have no profile name in session — keep the existing preview label.
 */
export function accountMenuDisplayName(input: {
  firstName: string | undefined;
  isPreview: boolean;
}): string | null {
  if (input.isPreview) {
    return "Cliente de prueba";
  }
  const name = input.firstName?.trim();
  return name ? name : null;
}

/** Full name when both parts are available. */
export function customerDisplayFullName(input: {
  firstName: string | undefined;
  lastName: string | undefined;
}): string | null {
  const first = input.firstName?.trim() ?? "";
  const last = input.lastName?.trim() ?? "";
  if (!first && !last) {
    return null;
  }
  if (!last) {
    return first;
  }
  if (!first) {
    return last;
  }
  return `${first} ${last}`;
}
