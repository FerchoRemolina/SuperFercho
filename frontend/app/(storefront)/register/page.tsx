import { Suspense } from "react";
import { RegisterForm } from "@/features/auth/components/register-form";
import { Container } from "@/shared/ui/container";

export default function RegisterPage() {
  return (
    <Container as="main" width="narrow" className="py-10 md:py-16">
      <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink">
        Crear cuenta
      </h1>
      <p className="mt-2 mb-8 text-base text-sf-muted">
        Crea tu cuenta para comprar en SuperFercho y organizar tu mercado.
      </p>
      <Suspense
        fallback={
          <p className="text-sf-muted" role="status">
            Cargando…
          </p>
        }
      >
        <RegisterForm />
      </Suspense>
    </Container>
  );
}
