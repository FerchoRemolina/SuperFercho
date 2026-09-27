import { ForgotPasswordForm } from "@/features/auth/components/forgot-password-form";
import { Container } from "@/shared/ui/container";

export default function ForgotPasswordPage() {
  return (
    <Container as="main" width="narrow" className="py-10 md:py-16">
      <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink">
        Recuperar contraseña
      </h1>
      <p className="mt-2 mb-8 text-base text-sf-muted">
        Te enviaremos un enlace para restablecer tu acceso si el correo está
        registrado.
      </p>
      <ForgotPasswordForm />
    </Container>
  );
}
