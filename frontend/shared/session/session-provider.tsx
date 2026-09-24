"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useSyncExternalStore,
  type ReactNode,
} from "react";
import { useQueryClient } from "@tanstack/react-query";
import { usePathname, useRouter } from "next/navigation";
import { setUnauthenticatedHandler } from "@/shared/api/client";
import { InactivityWarning } from "@/shared/session/inactivity-warning";
import {
  clearSession,
  getSession,
  hydrateSession,
  setSession,
  subscribeToSession,
  type Session,
} from "@/shared/session/session";
import { useInactivitySession } from "@/shared/session/use-inactivity-session";

type SessionContextValue = {
  session: Session;
  login: (session: NonNullable<Session>) => void;
  logout: () => void;
};

const SessionContext = createContext<SessionContextValue | null>(null);

let clientHydrated = false;

function subscribe(onStoreChange: () => void): () => void {
  return subscribeToSession(onStoreChange);
}

function getSnapshot(): Session {
  if (!clientHydrated) {
    clientHydrated = true;
    hydrateSession();
  }
  return getSession();
}

function getServerSnapshot(): Session {
  return null;
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const session = useSyncExternalStore(
    subscribe,
    getSnapshot,
    getServerSnapshot,
  );
  const queryClient = useQueryClient();
  const router = useRouter();
  const pathname = usePathname();
  const logoutInFlightRef = useRef(false);

  useEffect(() => {
    if (session) {
      logoutInFlightRef.current = false;
    }
  }, [session]);

  const logout = useCallback(() => {
    if (logoutInFlightRef.current) {
      return;
    }
    logoutInFlightRef.current = true;
    clearSession();
    queryClient.removeQueries();
    if (pathname !== "/login") {
      router.replace("/login");
    }
  }, [pathname, queryClient, router]);

  useEffect(() => {
    setUnauthenticatedHandler(() => {
      logout();
    });
    return () => setUnauthenticatedHandler(null);
  }, [logout]);

  const value = useMemo<SessionContextValue>(
    () => ({
      session,
      login: (next) => {
        setSession(next);
      },
      logout,
    }),
    [logout, session],
  );

  const { warningVisible, continueSession } = useInactivitySession({
    session,
    logout,
    pathname,
  });

  return (
    <SessionContext.Provider value={value}>
      {warningVisible ? (
        <InactivityWarning onContinue={continueSession} />
      ) : null}
      {children}
    </SessionContext.Provider>
  );
}

export function useSession(): SessionContextValue {
  const context = useContext(SessionContext);
  if (!context) {
    throw new Error("useSession must be used within SessionProvider");
  }
  return context;
}
