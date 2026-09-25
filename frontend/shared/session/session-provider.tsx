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
import {
  exitStorefrontPreview,
  startStorefrontPreview,
} from "@/features/admin/storefront-preview-api";
import { setUnauthenticatedHandler } from "@/shared/api/client";
import { InactivityWarning } from "@/shared/session/inactivity-warning";
import {
  activateStorefrontPreviewSession,
  adminTokenForPreviewExit,
  applyPreviewUnauthenticatedRestore,
  clearAllSessionState,
  closePreviewAndRestoreAdmin,
  hasActivePreviewState,
  reconcilePreviewSessionOnHydrate,
  restoreAdminFromPreviewStash,
} from "@/shared/session/preview-orchestration";
import {
  getPreviewMeta,
  isStorefrontPreviewActive,
  previewRemainingMs,
  subscribeToPreviewMeta,
  type PreviewMeta,
} from "@/shared/session/preview-session";
import {
  getSession,
  hydrateSession,
  setSession,
  subscribeToSession,
  type Session,
} from "@/shared/session/session";
import { useInactivitySession } from "@/shared/session/use-inactivity-session";

type SessionContextValue = {
  session: Session;
  previewMeta: PreviewMeta | null;
  isPreview: boolean;
  login: (session: NonNullable<Session>) => void;
  logout: () => void;
  enterStorefrontPreview: () => Promise<void>;
  returnToAdmin: () => void;
  exitStorefrontPreviewMode: () => Promise<void>;
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
    reconcilePreviewSessionOnHydrate();
  }
  return getSession();
}

function getServerSnapshot(): Session {
  return null;
}

function subscribePreview(onStoreChange: () => void): () => void {
  return subscribeToPreviewMeta(onStoreChange);
}

function getPreviewSnapshot(): PreviewMeta | null {
  return getPreviewMeta();
}

function getPreviewServerSnapshot(): PreviewMeta | null {
  return null;
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const session = useSyncExternalStore(
    subscribe,
    getSnapshot,
    getServerSnapshot,
  );
  const previewMeta = useSyncExternalStore(
    subscribePreview,
    getPreviewSnapshot,
    getPreviewServerSnapshot,
  );
  const queryClient = useQueryClient();
  const router = useRouter();
  const pathname = usePathname();
  const logoutInFlightRef = useRef(false);
  const isPreview = isStorefrontPreviewActive(session, previewMeta);

  useEffect(() => {
    if (session) {
      logoutInFlightRef.current = false;
    }
  }, [session]);

  const clearCaches = useCallback(() => {
    queryClient.removeQueries();
  }, [queryClient]);

  const returnToAdmin = useCallback(() => {
    const admin = restoreAdminFromPreviewStash();
    if (!admin) {
      return;
    }
    clearCaches();
    if (pathname !== "/admin" && !pathname.startsWith("/admin/")) {
      router.replace("/admin");
    }
  }, [clearCaches, pathname, router]);

  const exitStorefrontPreviewMode = useCallback(async () => {
    const adminToken = adminTokenForPreviewExit();
    try {
      await exitStorefrontPreview(adminToken ?? undefined);
    } catch {
      // Backend sweeper / gate remain the authority if exit fails.
    }
    const { admin } = closePreviewAndRestoreAdmin();
    clearCaches();
    router.replace(admin ? "/admin" : "/login");
  }, [clearCaches, router]);

  const enterStorefrontPreview = useCallback(async () => {
    const current = getSession();
    if (!current || current.role !== "ADMIN") {
      throw new Error("Solo un ADMIN puede iniciar Ver tienda");
    }
    const response = await startStorefrontPreview();
    activateStorefrontPreviewSession(current, response);
    clearCaches();
    router.push("/");
  }, [clearCaches, router]);

  const logout = useCallback(() => {
    if (logoutInFlightRef.current) {
      return;
    }
    logoutInFlightRef.current = true;

    const finishLogout = () => {
      clearAllSessionState();
      clearCaches();
      if (pathname !== "/login") {
        router.replace("/login");
      }
    };

    const adminToken = adminTokenForPreviewExit();
    if (hasActivePreviewState() && adminToken) {
      void exitStorefrontPreview(adminToken)
        .catch(() => undefined)
        .finally(finishLogout);
      return;
    }

    finishLogout();
  }, [clearCaches, pathname, router]);

  const handleUnauthenticated = useCallback(() => {
    if (logoutInFlightRef.current) {
      return;
    }
    const restored = applyPreviewUnauthenticatedRestore();
    if (restored) {
      clearCaches();
      router.replace("/admin");
      return;
    }
    logout();
  }, [clearCaches, logout, router]);

  useEffect(() => {
    setUnauthenticatedHandler(() => {
      handleUnauthenticated();
    });
    return () => setUnauthenticatedHandler(null);
  }, [handleUnauthenticated]);

  /** Admin-only inactivity → logout (may finalize ACTIVE preview). */
  const handleIdleEnd = useCallback(() => {
    logout();
  }, [logout]);

  useEffect(() => {
    if (!previewMeta) {
      return;
    }
    const tick = () => {
      if (previewRemainingMs(previewMeta) <= 0) {
        void exitStorefrontPreviewMode();
      }
    };
    tick();
    const id = window.setInterval(tick, 1000);
    return () => window.clearInterval(id);
  }, [exitStorefrontPreviewMode, previewMeta]);

  const value = useMemo<SessionContextValue>(
    () => ({
      session,
      previewMeta,
      isPreview,
      login: (next) => {
        clearAllSessionState();
        setSession(next);
      },
      logout,
      enterStorefrontPreview,
      returnToAdmin,
      exitStorefrontPreviewMode,
    }),
    [
      enterStorefrontPreview,
      exitStorefrontPreviewMode,
      isPreview,
      logout,
      previewMeta,
      returnToAdmin,
      session,
    ],
  );

  const { warningVisible, continueSession } = useInactivitySession({
    session,
    onIdleEnd: handleIdleEnd,
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
