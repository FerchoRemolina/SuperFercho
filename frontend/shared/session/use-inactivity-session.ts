"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import {
  ACTIVITY_THROTTLE_MS,
  INACTIVITY_THRESHOLDS,
  createInactivityTracker,
  type InactivityTracker,
} from "@/shared/session/inactivity";
import type { Session } from "@/shared/session/session";

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
  /** Admin idle logout. Customer sessions do not use inactivity. */
  onIdleEnd: () => void;
  pathname: string;
};

export type UseInactivitySessionResult = {
  warningVisible: boolean;
  continueSession: () => void;
};

export function useInactivitySession({
  session,
  onIdleEnd,
  pathname,
}: UseInactivitySessionArgs): UseInactivitySessionResult {
  const [warningVisible, setWarningVisible] = useState(false);
  const trackerRef = useRef<InactivityTracker | null>(null);
  const idleOnceRef = useRef(false);
  const onIdleEndRef = useRef(onIdleEnd);
  onIdleEndRef.current = onIdleEnd;

  const isAdmin = session?.role === "ADMIN";

  useEffect(() => {
    if (!isAdmin) {
      trackerRef.current?.stop();
      trackerRef.current = null;
      idleOnceRef.current = false;
      setWarningVisible(false);
      return;
    }

    idleOnceRef.current = false;
    const tracker = createInactivityTracker({
      thresholds: INACTIVITY_THRESHOLDS.ADMIN,
      onWarning: () => setWarningVisible(true),
      onWarningCleared: () => setWarningVisible(false),
      onLogout: () => {
        if (idleOnceRef.current) {
          return;
        }
        idleOnceRef.current = true;
        onIdleEndRef.current();
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
  }, [isAdmin]);

  useEffect(() => {
    if (!isAdmin) {
      return;
    }
    trackerRef.current?.recordActivity();
  }, [pathname, isAdmin]);

  const continueSession = useCallback(() => {
    trackerRef.current?.continueSession();
  }, []);

  return {
    warningVisible,
    continueSession,
  };
}
