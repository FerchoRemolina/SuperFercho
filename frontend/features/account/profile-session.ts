import { getSession, setSession } from "@/shared/session/session";

/**
 * Refresca los nombres visibles de la sesión CUSTOMER después de que el
 * backend confirma la actualización del perfil. Conserva identidad,
 * credenciales, rol y expiración; solo toca firstName/lastName. Ignorado sin
 * sesión activa o para roles distintos de CUSTOMER.
 */
export function applyCustomerProfileNamesToSession(
  firstName: string,
  lastName: string,
): void {
  const current = getSession();
  if (!current || current.role !== "CUSTOMER") {
    return;
  }
  const trimmedFirstName = firstName.trim();
  const trimmedLastName = lastName.trim();
  setSession({
    ...current,
    ...(trimmedFirstName ? { firstName: trimmedFirstName } : {}),
    ...(trimmedLastName ? { lastName: trimmedLastName } : {}),
  });
}
