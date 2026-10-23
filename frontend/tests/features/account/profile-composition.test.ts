import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("customer profile composition", () => {
  it("keeps the profile page on customer routes without placeholders", () => {
    const page = source(
      "app/(storefront)/(customer)/account/profile/page.tsx",
    );
    expect(page).toContain("ProfilePageContent");
    expect(page).not.toContain("PlaceholderScreen");
  });

  it("wires the profile page to the shared query and mutation hooks", () => {
    const content = source(
      "features/account/components/profile-page-content.tsx",
    );
    expect(content).toContain("useCustomerProfileQuery");
    expect(content).toContain("useUpdateCustomerProfileMutation");
    expect(content).toContain('title="Mi perfil"');
    // Estados: carga, error recuperable y datos.
    expect(content).toContain("profileQuery.isPending");
    expect(content).toContain("profileQuery.isError");
    expect(content).toContain("Reintentar");
    expect(content).toContain("messageForApiProblem");
    // La sesión se refresca desde la mutation (etapa 1), no desde la página.
    expect(content).not.toContain("setSession");
  });

  it("renders the profile form with the six editable fields and shared validators", () => {
    const form = source("features/account/components/profile-form.tsx");
    expect(form).toContain('label="Tipo de documento"');
    expect(form).toContain('label="Número de documento"');
    expect(form).toContain('label="Nombre"');
    expect(form).toContain('label="Apellidos"');
    expect(form).toContain('label="Correo electrónico"');
    expect(form).toContain('label="Celular"');
    expect(form).toContain("REGISTER_DOCUMENT_TYPES");
    expect(form).toContain("validateDocumentNumber");
    expect(form).toContain("validateFirstName");
    expect(form).toContain("validateLastName");
    expect(form).toContain("validateEmail");
    expect(form).toContain("validatePhone");
    expect(form).toContain("noValidate");
    expect(form).toContain("disabled={pending}");
    expect(form).not.toContain("setSession");
    // La petición lleva exactamente los seis campos editables.
    expect(form).toContain("documentType: values.documentType");
    expect(form).toContain("documentNumber: values.documentNumber.trim()");
    expect(form).toContain("firstName: values.firstName.trim()");
    expect(form).toContain("lastName: values.lastName.trim()");
    expect(form).toContain("email: values.email.trim()");
    expect(form).toContain("phone: values.phone.trim()");
  });

  it("maps duplicate document and email errors to their own fields", () => {
    const form = source("features/account/components/profile-form.tsx");
    expect(form).toContain('code === "DOCUMENT_ALREADY_EXISTS"');
    expect(form).toContain("documentNumber: message");
    expect(form).toContain('code === "USER_ALREADY_EXISTS"');
    expect(form).toContain("email: message");
  });

  it("never shows update feedback before the user attempts to save", () => {
    const hooks = source("features/account/hooks.ts");
    const form = source("features/account/components/profile-form.tsx");
    const content = source(
      "features/account/components/profile-page-content.tsx",
    );

    // El PUT solo se dispara desde el submit del formulario.
    expect(form).not.toContain("mutate(");
    expect(content.match(/updateMutation\.mutate/g)?.length).toBe(1);
    // Un error de PUT previo no se restaura al reentrar a la página.
    expect(hooks).toContain("gcTime: 0");
    // El estado nulo (carga inicial / éxito) no produce mensaje rojo.
    expect(form).toContain("if (error == null)");
  });

  it("renders a single feedback block: success or error, never both", () => {
    const form = source("features/account/components/profile-form.tsx");
    // Cadena excluyente: éxito → error general → nada.
    expect(form).toContain(
      ") : serverFeedback.general ? (",
    );
    expect(form).toContain("saved && !pending");
    expect(form.match(/Alert tone="success"/g)?.length).toBe(1);
    expect(form.match(/Alert tone="error"/g)?.length).toBe(1);
  });

  it("keeps user input intact when a save fails", () => {
    const form = source("features/account/components/profile-form.tsx");
    // Los valores solo se inicializan una vez desde el perfil del query;
    // un error del servidor no reinicializa el formulario.
    expect(form.match(/profileToValues\(profile\)/g)?.length).toBe(1);
    expect(form).toContain("useState<ProfileFormValues>(() =>");
  });

  it("exposes a friendly message for customer profile forbidden", () => {
    const messages = source("shared/errors/messages.ts");
    expect(messages).toContain('case "CUSTOMER_PROFILE_FORBIDDEN"');
    expect(messages).toContain("Tu cuenta no puede gestionar este perfil.");
  });

  it("offers Mi perfil as the first Cuenta entry on desktop and mobile menus", () => {
    const header = source("shared/ui/site-header.tsx");
    expect(header.match(/href="\/account\/profile"/g)?.length).toBe(2);
    expect(header).toContain("Mi perfil");
  });
});
