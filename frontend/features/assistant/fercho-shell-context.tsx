"use client";

import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";

type FerchoShellContextValue = {
  isOpen: boolean;
  isExpanded: boolean;
  open: () => void;
  minimize: () => void;
  toggle: () => void;
  toggleExpanded: () => void;
};

const FerchoShellContext = createContext<FerchoShellContextValue | null>(null);

/**
 * Presentation-only open/minimized flag for the storefront Fercho shell.
 * Does not persist across reloads and never sends chat, cart, or package actions.
 */
export function FerchoShellProvider({ children }: { children: ReactNode }) {
  const [isOpen, setIsOpen] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const open = useCallback(() => {
    setIsOpen(true);
  }, []);
  const minimize = useCallback(() => {
    setIsOpen(false);
  }, []);
  const toggle = useCallback(() => {
    setIsOpen((current) => !current);
  }, []);
  const toggleExpanded = useCallback(() => {
    setIsExpanded((current) => !current);
  }, []);
  const value = useMemo(
    () => ({ isOpen, isExpanded, open, minimize, toggle, toggleExpanded }),
    [isExpanded, isOpen, minimize, open, toggle, toggleExpanded],
  );

  return (
    <FerchoShellContext.Provider value={value}>
      {children}
    </FerchoShellContext.Provider>
  );
}

export function useFerchoShell(): FerchoShellContextValue {
  const context = useContext(FerchoShellContext);
  if (!context) {
    throw new Error("useFerchoShell must be used within FerchoShellProvider");
  }
  return context;
}
