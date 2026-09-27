import { Suspense } from "react";
import { ResetPasswordTokenReader } from "@/features/auth/components/reset-password-token-reader";
import { Container } from "@/shared/ui/container";

export default function ResetPasswordPage() {
  return (
    <Container as="main" width="narrow" className="py-10 md:py-16">
      <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink">
        Restablecer contraseña
      </h1>
      <p className="mt-2 mb-8 text-base text-sf-muted">
        Elige una contraseña nueva para tu cuenta de SuperFercho.
      </p>
      <Suspense
        fallback={
          <p className="text-sf-muted" role="status">
            Cargando…
          </p>
        }
      >
        <ResetPasswordTokenReader />
      </Suspense>
    </Container>
  );
}
