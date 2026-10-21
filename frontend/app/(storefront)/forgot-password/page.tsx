import { ForgotPasswordForm } from "@/features/auth/components/forgot-password-form";
import { Container } from "@/shared/ui/container";
import { Card } from "@/shared/ui/card";

export default function ForgotPasswordPage() {
  return (
    <Container as="main" width="narrow" className="py-12 md:py-[72px]">
      <div
        aria-hidden="true"
        className="mb-4 h-1.5 w-14 rounded-full bg-gradient-to-r from-sf-primary to-sf-yellow"
      />
      <h1 className="text-[1.75rem] font-bold tracking-tight text-sf-ink md:text-[2rem]">
        Recuperar contraseña
      </h1>
      <p className="mt-2 text-sm text-sf-muted md:text-base">
        Te enviaremos un enlace para restablecer tu acceso si el correo está
        registrado.
      </p>
      <Card className="mt-6 !p-4 md:!p-6">
        <ForgotPasswordForm />
      </Card>
    </Container>
  );
}
