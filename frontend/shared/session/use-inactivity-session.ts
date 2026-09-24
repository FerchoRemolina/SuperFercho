"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import {
  ACTIVITY_THROTTLE_MS,
  INACTIVITY_THRESHOLDS,
  createInactivityTracker,
  type InactivityTracker,
} from "@/shared/session/inactivity";
import type { Role, Session } from "@/shared/session/session";

const ACTIVITY_EVENTS = [
  "pointerdown",
  "click",
  "keydown",
  "scroll",
  "input",
  "change",
] as const;

type UseInactivitySessionArgs = {
  session: Session;
  logout: () => void;
  pathname: string;
};

export type UseInactivitySessionResult = {
  warningVisible: boolean;
  continueSession: () => void;
};

export function useInactivitySession({
  session,
  logout,
  pathname,
}: UseInactivitySessionArgs): UseInactivitySessionResult {
  const [warningVisible, setWarningVisible] = useState(false);
  const trackerRef = useRef<InactivityTracker | null>(null);
  const logoutOnceRef = useRef(false);
  const logoutRef = useRef(logout);
  logoutRef.current = logout;

  const role: Role | null = session?.role ?? null;

  useEffect(() => {
    if (!role) {
      trackerRef.current?.stop();
      trackerRef.current = null;
      logoutOnceRef.current = false;
      setWarningVisible(false);
      return;
    }

    logoutOnceRef.current = false;
    const tracker = createInactivityTracker({
      thresholds: INACTIVITY_THRESHOLDS[role],
      onWarning: () => setWarningVisible(true),
      onWarningCleared: () => setWarningVisible(false),
      onLogout: () => {
        if (logoutOnceRef.current) {
          return;
        }
        logoutOnceRef.current = true;
        logoutRef.current();
      },
    });
    trackerRef.current = tracker;

    let lastThrottledAt = 0;
    const onActivity = () => {
      const now = Date.now();
      if (now - lastThrottledAt < ACTIVITY_THROTTLE_MS) {
        return;
      }
      lastThrottledAt = now;
      tracker.recordActivity();
    };

    const listenerOptions: AddEventListenerOptions = {
      capture: true,
      passive: true,
    };
    for (const eventName of ACTIVITY_EVENTS) {
      window.addEventListener(eventName, onActivity, listenerOptions);
    }

    return () => {
      for (const eventName of ACTIVITY_EVENTS) {
        window.removeEventListener(eventName, onActivity, listenerOptions);
      }
      tracker.stop();
      if (trackerRef.current === tracker) {
        trackerRef.current = null;
      }
      setWarningVisible(false);
    };
  }, [role]);

  useEffect(() => {
    if (!role) {
      return;
    }
    trackerRef.current?.recordActivity();
  }, [pathname, role]);

  const continueSession = useCallback(() => {
    trackerRef.current?.continueSession();
  }, []);

  return {
    warningVisible,
    continueSession,
  };
}
