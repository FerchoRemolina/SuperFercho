import { Suspense } from "react";
import { RegisterForm } from "@/features/auth/components/register-form";
import { Container } from "@/shared/ui/container";
import { Card } from "@/shared/ui/card";

export default function RegisterPage() {
  return (
    <Container as="main" width="narrow" className="py-12 md:py-[72px]">
      <div
        aria-hidden="true"
        className="mb-4 h-1.5 w-14 rounded-full bg-gradient-to-r from-sf-primary to-sf-yellow"
      />
      <h1 className="text-[1.75rem] font-bold tracking-tight text-sf-ink md:text-[2rem]">
        Crear cuenta
      </h1>
      <p className="mt-2 text-sm text-sf-muted md:text-base">
        Crea tu cuenta para comprar en SuperFercho y organizar tu mercado.
      </p>
      <Card className="mt-6 !p-4 md:!p-6">
        <Suspense
          fallback={
            <p className="text-sf-muted" role="status">
              Cargando…
            </p>
          }
        >
          <RegisterForm />
        </Suspense>
      </Card>
    </Container>
  );
}
