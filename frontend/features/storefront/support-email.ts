/** Normaliza el correo público del footer para un mailto: seguro. */
export function normalizeSupportEmail(
  value: string | null | undefined,
): string | null {
  if (!value) {
    return null;
  }

  let email = value.trim();
  const lower = email.toLowerCase();

  if (lower.startsWith("mailto:")) {
    email = email.slice("mailto:".length).trim();
  }

  if (lower.startsWith("https://") || lower.startsWith("http://")) {
    try {
      const parsed = new URL(email);
      const candidate = decodeURIComponent(
        `${parsed.username ? `${parsed.username}@` : ""}${parsed.hostname}${parsed.pathname}`,
      )
        .replace(/^\/+/, "")
        .replace(/\/+$/, "");
      email = candidate.includes("@") ? candidate : email;
    } catch {
      // keep trimmed value
    }
  }

  email = email.replace(/^\/+/, "").trim();

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return null;
  }

  return email;
}

export function mailtoHref(email: string): string {
  return `mailto:${encodeURIComponent(email).replace(/%40/g, "@")}`;
}

export async function copyTextToClipboard(text: string): Promise<boolean> {
  try {
    if (typeof navigator !== "undefined" && navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
      return true;
    }
  } catch {
    // fallback below
  }

  try {
    const textarea = document.createElement("textarea");
    textarea.value = text;
    textarea.setAttribute("readonly", "");
    textarea.style.position = "fixed";
    textarea.style.left = "-9999px";
    document.body.appendChild(textarea);
    textarea.select();
    const ok = document.execCommand("copy");
    document.body.removeChild(textarea);
    return ok;
  } catch {
    return false;
  }
}
