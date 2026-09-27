"use client";

import { useSearchParams } from "next/navigation";
import { ResetPasswordForm } from "@/features/auth/components/reset-password-form";

export function ResetPasswordTokenReader() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token") ?? "";
  return <ResetPasswordForm token={token} />;
}
