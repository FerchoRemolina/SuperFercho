import type { Role } from "@/shared/session/session";

export const INACTIVITY_WARNING_MESSAGE =
  "Tu sesión se cerrará en 1 minuto por inactividad";

export const CONTINUE_SESSION_LABEL = "Continuar sesión";

export const ACTIVITY_THROTTLE_MS = 1_000;

export type InactivityThresholds = {
  warningMs: number;
  logoutMs: number;
};

/** Only ADMIN uses inactivity timeouts. CUSTOMER uses JWT countdown instead. */
export const INACTIVITY_THRESHOLDS: Partial<Record<Role, InactivityThresholds>> & {
  ADMIN: InactivityThresholds;
} = {
  ADMIN: {
    warningMs: 9 * 60_000,
    logoutMs: 10 * 60_000,
  },
};

export type InactivityTracker = {
  recordActivity: () => void;
  continueSession: () => void;
  stop: () => void;
};

export type CreateInactivityTrackerOptions = {
  thresholds: InactivityThresholds;
  onWarning: () => void;
  onLogout: () => void;
  onWarningCleared?: () => void;
};

export function createInactivityTracker(
  options: CreateInactivityTrackerOptions,
): InactivityTracker {
  let warningTimer: ReturnType<typeof setTimeout> | null = null;
  let logoutTimer: ReturnType<typeof setTimeout> | null = null;
  let stopped = false;
  let logoutFired = false;
  let warningVisible = false;

  function clearTimers(): void {
    if (warningTimer !== null) {
      clearTimeout(warningTimer);
      warningTimer = null;
    }
    if (logoutTimer !== null) {
      clearTimeout(logoutTimer);
      logoutTimer = null;
    }
  }

  function hideWarning(): void {
    if (!warningVisible) {
      return;
    }
    warningVisible = false;
    options.onWarningCleared?.();
  }

  function arm(): void {
    if (stopped || logoutFired) {
      return;
    }
    clearTimers();
    hideWarning();
    warningTimer = setTimeout(() => {
      warningVisible = true;
      options.onWarning();
    }, options.thresholds.warningMs);
    logoutTimer = setTimeout(() => {
      if (stopped || logoutFired) {
        return;
      }
      logoutFired = true;
      clearTimers();
      options.onLogout();
    }, options.thresholds.logoutMs);
  }

  function recordActivity(): void {
    if (stopped || logoutFired) {
      return;
    }
    arm();
  }

  function continueSession(): void {
    recordActivity();
  }

  function stop(): void {
    stopped = true;
    clearTimers();
  }

  arm();

  return {
    recordActivity,
    continueSession,
    stop,
  };
}
