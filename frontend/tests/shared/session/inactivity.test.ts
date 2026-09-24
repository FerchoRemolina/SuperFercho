import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  CONTINUE_SESSION_LABEL,
  INACTIVITY_THRESHOLDS,
  INACTIVITY_WARNING_MESSAGE,
  createInactivityTracker,
} from "@/shared/session/inactivity";

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), "../../..");

function source(relativePath: string): string {
  return readFileSync(join(frontendRoot, relativePath), "utf8");
}

afterEach(() => {
  vi.useRealTimers();
  vi.restoreAllMocks();
});

describe("inactivity thresholds", () => {
  it("uses CUSTOMER warning at 4 minutes and logout at 5 minutes", () => {
    expect(INACTIVITY_THRESHOLDS.CUSTOMER).toEqual({
      warningMs: 4 * 60_000,
      logoutMs: 5 * 60_000,
    });
  });

  it("uses ADMIN warning at 9 minutes and logout at 10 minutes", () => {
    expect(INACTIVITY_THRESHOLDS.ADMIN).toEqual({
      warningMs: 9 * 60_000,
      logoutMs: 10 * 60_000,
    });
  });

  it("exposes the exact warning copy and continue label", () => {
    expect(INACTIVITY_WARNING_MESSAGE).toBe(
      "Tu sesión se cerrará en 1 minuto por inactividad",
    );
    expect(CONTINUE_SESSION_LABEL).toBe("Continuar sesión");
  });
});

describe("createInactivityTracker CUSTOMER", () => {
  it("does not warn before 4 minutes", () => {
    vi.useFakeTimers();
    const onWarning = vi.fn();
    const onLogout = vi.fn();
    const tracker = createInactivityTracker({
      thresholds: INACTIVITY_THRESHOLDS.CUSTOMER,
      onWarning,
      onLogout,
    });

    vi.advanceTimersByTime(4 * 60_000 - 1);
    expect(onWarning).not.toHaveBeenCalled();
    expect(onLogout).not.toHaveBeenCalled();
    tracker.stop();
  });

  it("warns at 4 minutes", () => {
    vi.useFakeTimers();
    const onWarning = vi.fn();
    const onLogout = vi.fn();
    const tracker = createInactivityTracker({
      thresholds: INACTIVITY_THRESHOLDS.CUSTOMER,
      onWarning,
      onLogout,
    });

    vi.advanceTimersByTime(4 * 60_000);
    expect(onWarning).toHaveBeenCalledTimes(1);
    expect(onLogout).not.toHaveBeenCalled();
    tracker.stop();
  });

  it("logs out at 5 minutes", () => {
    vi.useFakeTimers();
    const onWarning = vi.fn();
    const onLogout = vi.fn();
    const tracker = createInactivityTracker({
      thresholds: INACTIVITY_THRESHOLDS.CUSTOMER,
      onWarning,
      onLogout,
    });

    vi.advanceTimersByTime(5 * 60_000);
    expect(onWarning).toHaveBeenCalledTimes(1);
    expect(onLogout).toHaveBeenCalledTimes(1);
    tracker.stop();
  });
});

describe("createInactivityTracker ADMIN", () => {
  it("does not warn before 9 minutes", () => {
    vi.useFakeTimers();
    const onWarning = vi.fn();
    const onLogout = vi.fn();
    const tracker = createInactivityTracker({
      thresholds: INACTIVITY_THRESHOLDS.ADMIN,
      onWarning,
      onLogout,
    });

    vi.advanceTimersByTime(9 * 60_000 - 1);
    expect(onWarning).not.toHaveBeenCalled();
    expect(onLogout).not.toHaveBeenCalled();
    tracker.stop();
  });

  it("warns at 9 minutes", () => {
    vi.useFakeTimers();
    const onWarning = vi.fn();
    const onLogout = vi.fn();
    const tracker = createInactivityTracker({
      thresholds: INACTIVITY_THRESHOLDS.ADMIN,
      onWarning,
      onLogout,
    });

    vi.advanceTimersByTime(9 * 60_000);
    expect(onWarning).toHaveBeenCalledTimes(1);
    expect(onLogout).not.toHaveBeenCalled();
    tracker.stop();
  });

  it("logs out at 10 minutes", () => {
    vi.useFakeTimers();
    const onWarning = vi.fn();
    const onLogout = vi.fn();
    const tracker = createInactivityTracker({
      thresholds: INACTIVITY_THRESHOLDS.ADMIN,
      onWarning,
      onLogout,
    });

    vi.advanceTimersByTime(10 * 60_000);
    expect(onWarning).toHaveBeenCalledTimes(1);
    expect(onLogout).toHaveBeenCalledTimes(1);
    tracker.stop();
  });
});

describe("createInactivityTracker activity and continue", () => {
  it("resets the counter on activity and hides a visible warning", () => {
    vi.useFakeTimers();
    const onWarning = vi.fn();
    const onWarningCleared = vi.fn();
    const onLogout = vi.fn();
    const tracker = createInactivityTracker({
      thresholds: INACTIVITY_THRESHOLDS.CUSTOMER,
      onWarning,
      onWarningCleared,
      onLogout,
    });

    vi.advanceTimersByTime(4 * 60_000);
    expect(onWarning).toHaveBeenCalledTimes(1);

    tracker.recordActivity();
    expect(onWarningCleared).toHaveBeenCalledTimes(1);

    vi.advanceTimersByTime(4 * 60_000 - 1);
    expect(onWarning).toHaveBeenCalledTimes(1);
    expect(onLogout).not.toHaveBeenCalled();

    vi.advanceTimersByTime(1);
    expect(onWarning).toHaveBeenCalledTimes(2);
    tracker.stop();
  });

  it("continues session by hiding the warning and resetting timers", () => {
    vi.useFakeTimers();
    const onWarning = vi.fn();
    const onWarningCleared = vi.fn();
    const onLogout = vi.fn();
    const tracker = createInactivityTracker({
      thresholds: INACTIVITY_THRESHOLDS.CUSTOMER,
      onWarning,
      onWarningCleared,
      onLogout,
    });

    vi.advanceTimersByTime(4 * 60_000);
    tracker.continueSession();
    expect(onWarningCleared).toHaveBeenCalledTimes(1);

    vi.advanceTimersByTime(4 * 60_000 - 1);
    expect(onWarning).toHaveBeenCalledTimes(1);
    expect(onLogout).not.toHaveBeenCalled();
    tracker.stop();
  });

  it("executes logout only once", () => {
    vi.useFakeTimers();
    const onWarning = vi.fn();
    const onLogout = vi.fn();
    const tracker = createInactivityTracker({
      thresholds: INACTIVITY_THRESHOLDS.CUSTOMER,
      onWarning,
      onLogout,
    });

    vi.advanceTimersByTime(5 * 60_000);
    tracker.recordActivity();
    tracker.continueSession();
    vi.advanceTimersByTime(5 * 60_000);
    expect(onLogout).toHaveBeenCalledTimes(1);
    tracker.stop();
  });

  it("stop clears timers so logout does not fire without a session", () => {
    vi.useFakeTimers();
    const onWarning = vi.fn();
    const onLogout = vi.fn();
    const tracker = createInactivityTracker({
      thresholds: INACTIVITY_THRESHOLDS.CUSTOMER,
      onWarning,
      onLogout,
    });

    tracker.stop();
    vi.advanceTimersByTime(5 * 60_000);
    expect(onWarning).not.toHaveBeenCalled();
    expect(onLogout).not.toHaveBeenCalled();
  });
});

describe("inactivity composition", () => {
  it("mounts the detector once in SessionProvider for both roles", () => {
    const provider = source("shared/session/session-provider.tsx");
    const customerLayout = source("app/(customer)/layout.tsx");
    const adminLayout = source("app/(admin)/layout.tsx");

    expect(provider).toContain("useInactivitySession");
    expect(provider).toContain("InactivityWarning");
    expect(customerLayout).not.toContain("useInactivitySession");
    expect(customerLayout).not.toContain("InactivityWarning");
    expect(adminLayout).not.toContain("useInactivitySession");
    expect(adminLayout).not.toContain("InactivityWarning");
  });

  it("does not add heartbeat, refresh, or JWT renewal calls", () => {
    const inactivity = source("shared/session/inactivity.ts");
    const hook = source("shared/session/use-inactivity-session.ts");
    const warning = source("shared/session/inactivity-warning.tsx");
    const provider = source("shared/session/session-provider.tsx");

    for (const file of [inactivity, hook, warning, provider]) {
      expect(file).not.toMatch(/heartbeat/i);
      expect(file).not.toMatch(/refreshToken/i);
      expect(file).not.toContain("request(");
      expect(file).not.toContain("fetch(");
    }
  });

  it("wires the warning UI to the exact copy and continue action", () => {
    const warning = source("shared/session/inactivity-warning.tsx");
    expect(warning).toContain("INACTIVITY_WARNING_MESSAGE");
    expect(warning).toContain("CONTINUE_SESSION_LABEL");
    expect(warning).toContain("onContinue");
  });
});
