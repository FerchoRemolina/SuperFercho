"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import {
  homePathForRole,
  login,
  sessionFromAuthentication,
} from "@/features/auth/api";
import { cartKeys } from "@/features/cart/api";
import { fulfillPendingAddToCart } from "@/features/cart/pending-intent";
import { safeNextPath } from "@/shared/auth/safe-next-path";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { useSession } from "@/shared/session/session-provider";
import { Alert } from "@/shared/ui/alert";
import { Button } from "@/shared/ui/button";
import { TextField } from "@/shared/ui/text-field";

export function LoginForm() {
  const { session, login: persistSession } = useSession();
  const queryClient = useQueryClient();
  const router = useRouter();
  const searchParams = useSearchParams();
  const registered = searchParams.get("registered") === "1";
  const nextPath = safeNextPath(searchParams.get("next"));
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    if (session) {
      router.replace(nextPath ?? homePathForRole(session.role));
    }
  }, [nextPath, router, session]);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setPending(true);
    try {
      const response = await login({ email, password });
      persistSession(sessionFromAuthentication(response));
      if (response.role === "CUSTOMER") {
        await queryClient.cancelQueries({ queryKey: cartKeys().root() });
        try {
          const cart = await fulfillPendingAddToCart();
          if (cart) {
            queryClient.setQueryData(cartKeys().root(), cart);
          }
        } catch {
          // The intent is already cleared; the user can retry from the catalog.
        }
      }
      router.replace(nextPath ?? homePathForRole(response.role));
    } catch (cause) {
      if (isApiError(cause)) {
        setError(messageForApiProblem(cause.problem));
      } else {
        setError("No se pudo iniciar sesión.");
      }
    } finally {
      setPending(false);
    }
  }

  const registerHref = nextPath
    ? `/register?next=${encodeURIComponent(nextPath)}`
    : "/register";

  return (
    <form onSubmit={onSubmit} className="grid gap-4" noValidate>
      {registered ? (
        <Alert tone="success" title="Cuenta creada">
          Inicia sesión con el correo y la contraseña que registraste.
        </Alert>
      ) : null}
      {error ? (
        <Alert tone="error" title="No se pudo entrar">
          {error}
        </Alert>
      ) : null}
      <TextField
        id="login-email"
        label="Correo"
        type="email"
        name="email"
        autoComplete="email"
        required
        value={email}
        onChange={(event) => setEmail(event.target.value)}
      />
      <TextField
        id="login-password"
        label="Contraseña"
        type="password"
        name="password"
        autoComplete="current-password"
        required
        value={password}
        onChange={(event) => setPassword(event.target.value)}
      />
      <Button type="submit" disabled={pending}>
        {pending ? "Entrando…" : "Entrar"}
      </Button>
      <p className="text-sm text-sf-muted">
        ¿No tienes cuenta?{" "}
        <Link href={registerHref} className="font-semibold text-sf-accent">
          Crear cuenta
        </Link>
      </p>
    </form>
  );
}
