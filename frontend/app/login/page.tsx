import { Suspense } from "react";
import { LoginForm } from "@/features/auth/components/login-form";
import { Container } from "@/shared/ui/container";

export default function LoginPage() {
  return (
    <Container as="main" width="narrow" className="py-10 md:py-16">
      <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink">
        Iniciar sesión
      </h1>
      <p className="mt-2 mb-8 text-base text-sf-muted">
        Entra para comprar, guardar tu mercado y seguir tus pedidos.
      </p>
      <Suspense
        fallback={
          <p className="text-sf-muted" role="status">
            Cargando…
          </p>
        }
      >
        <LoginForm />
      </Suspense>
    </Container>
  );
}
