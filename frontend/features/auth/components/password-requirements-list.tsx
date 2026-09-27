"use client";

import { CheckCircleIcon } from "@/shared/ui/icons";
import { cx } from "@/shared/utils/cx";
import {
  passwordRequirements,
  type PasswordRequirement,
} from "@/features/auth/register-validation";

export function PasswordRequirementsList({ password }: { password: string }) {
  const requirements: PasswordRequirement[] = passwordRequirements(password);

  return (
    <div className="rounded-xl border border-sf-border/80 bg-sf-bg/70 px-3 py-2.5">
      <p className="text-xs font-semibold text-sf-ink">
        Tu contraseña debe tener:
      </p>
      <ul className="mt-2 grid gap-1.5">
        {requirements.map((item) => (
          <li
            key={item.id}
            className={cx(
              "flex items-center gap-2 text-xs",
              item.met ? "text-sf-primary" : "text-sf-muted",
            )}
          >
            {item.met ? (
              <CheckCircleIcon className="h-3.5 w-3.5 shrink-0" />
            ) : (
              <span
                className="inline-block h-3.5 w-3.5 shrink-0 rounded-full border border-sf-border"
                aria-hidden="true"
              />
            )}
            {item.label}
          </li>
        ))}
      </ul>
    </div>
  );
}
