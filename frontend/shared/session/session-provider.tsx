"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useSyncExternalStore,
  type ReactNode,
} from "react";
import { useQueryClient } from "@tanstack/react-query";
import { usePathname, useRouter } from "next/navigation";
import { setUnauthenticatedHandler } from "@/shared/api/client";
import {
  clearSession,
  getSession,
  hydrateSession,
  setSession,
  subscribeToSession,
  type Session,
} from "@/shared/session/session";

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

  const logout = useCallback(() => {
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

  return (
    <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
  );
}

export function useSession(): SessionContextValue {
  const context = useContext(SessionContext);
  if (!context) {
    throw new Error("useSession must be used within SessionProvider");
  }
  return context;
}
