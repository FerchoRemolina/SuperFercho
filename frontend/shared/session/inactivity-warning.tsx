"use client";

import {
  CONTINUE_SESSION_LABEL,
  INACTIVITY_WARNING_MESSAGE,
} from "@/shared/session/inactivity";
import { Alert } from "@/shared/ui/alert";
import { Button } from "@/shared/ui/button";

export function InactivityWarning({
  onContinue,
}: {
  onContinue: () => void;
}) {
  return (
    <div className="pointer-events-none fixed inset-x-0 top-0 z-50 flex justify-center p-4">
      <div className="pointer-events-auto w-full max-w-lg shadow-lg">
        <Alert tone="warning" title={INACTIVITY_WARNING_MESSAGE}>
          <div className="mt-3">
            <Button type="button" variant="secondary" onClick={onContinue}>
              {CONTINUE_SESSION_LABEL}
            </Button>
          </div>
        </Alert>
      </div>
    </div>
  );
}
