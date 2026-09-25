import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

describe("addresses composition", () => {
  it("uses delivery-oriented page copy", () => {
    const page = source(
      "features/account/components/addresses-page-content.tsx",
    );
    expect(page).toContain("Direcciones de entrega");
    expect(page).toContain("Guarda dónde quieres recibir tus pedidos.");
    expect(page).toContain(
      "Agrega tu primera dirección de entrega para pedir a domicilio.",
    );
    expect(page).toContain("Predeterminada");
    expect(page).toContain("Usar como dirección predeterminada");
    expect(page).toContain(
      "La dirección no se eliminará. Solo dejará de estar disponible para",
    );
  });

  it("uses Colombian delivery labels and placeholders in the form", () => {
    const form = source("features/account/components/address-form.tsx");

    expect(form).toContain('label="Nombre de la dirección"');
    expect(form).toContain('placeholder="Ej. Casa, Oficina, Casa de mamá"');

    expect(form).toContain('label="Quién recibe"');
    expect(form).toContain(
      'placeholder="Nombre de quien recibe el pedido"',
    );

    expect(form).toContain('label="Dirección de entrega"');
    expect(form).toContain('placeholder="Ej. Calle 45 # 12-34"');

    expect(form).toContain('label="Detalle del lugar (opcional)"');
    expect(form).toContain(
      'placeholder="Apto, torre, conjunto o cómo llegar"',
    );

    expect(form).toContain('label="Ciudad"');
    expect(form).toContain('placeholder="Ej. Cúcuta"');

    expect(form).toContain('label="Departamento"');
    expect(form).toContain('placeholder="Ej. Norte de Santander"');

    expect(form).toContain('label="Celular de contacto"');
    expect(form).toContain('placeholder="10 dígitos, ej. 3001234567"');

    expect(form).toContain("Usar como dirección predeterminada");
    expect(form).toContain("Indica la dirección de entrega.");
    expect(form).toContain("Indica quién recibe el pedido.");
    expect(form).toContain("Indica un celular de contacto.");
  });

  it("does not invent out-of-scope address fields", () => {
    const form = source("features/account/components/address-form.tsx");
    expect(form).not.toMatch(/barrio/i);
    expect(form).not.toMatch(/código postal|codigo postal/i);
    expect(form).not.toMatch(/localidad/i);
    expect(form).not.toMatch(/tipo de vía|tipo de via/i);
  });
});
