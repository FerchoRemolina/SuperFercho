"use client";

import {
  MaximizeIcon,
  MinusIcon,
  PlusIcon,
  RestoreIcon,
} from "@/shared/ui/icons";
import { cx } from "@/shared/utils/cx";
import type { ButtonHTMLAttributes, ReactNode } from "react";

function HeaderIconButton({
  label,
  tooltip,
  className,
  children,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  label: string;
  tooltip?: string;
  children: ReactNode;
}) {
  return (
    <span className="group/icon relative inline-flex">
      <button
        {...props}
        type="button"
        aria-label={label}
        className={cx(
          "inline-flex size-11 items-center justify-center rounded-lg text-sf-ink",
          "hover:bg-sf-bg focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sf-primary",
          "disabled:cursor-not-allowed disabled:opacity-40",
          className,
        )}
      >
        {children}
      </button>
      {tooltip ? (
        <span
          role="tooltip"
          className="pointer-events-none absolute left-1/2 top-full z-10 mt-1 hidden -translate-x-1/2 whitespace-nowrap rounded-md bg-sf-ink px-2 py-0.5 text-[11px] font-medium text-white md:group-hover/icon:block md:group-focus-visible/icon:block"
        >
          {tooltip}
        </span>
      ) : null}
    </span>
  );
}

export function FerchoPanelHeader({
  onNewConversation,
  newConversationDisabled,
  isExpanded,
  onToggleExpanded,
  onMinimize,
}: {
  onNewConversation: () => void;
  newConversationDisabled: boolean;
  isExpanded: boolean;
  onToggleExpanded?: () => void;
  onMinimize?: () => void;
}) {
  return (
    <div className="flex items-center justify-between gap-2 px-3 py-1.5">
      <div className="flex min-w-0 items-center gap-0.5">
        <h2 className="px-1 text-base font-bold tracking-tight text-sf-ink">
          Fercho
        </h2>
        <HeaderIconButton
          label="Nueva conversación"
          tooltip="Nueva conversación"
          onClick={onNewConversation}
          disabled={newConversationDisabled}
        >
          <PlusIcon width={18} height={18} />
        </HeaderIconButton>
      </div>
      <div className="flex shrink-0 items-center gap-0.5">
        {onToggleExpanded ? (
          <HeaderIconButton
            label={isExpanded ? "Restaurar" : "Maximizar"}
            aria-pressed={isExpanded}
            onClick={onToggleExpanded}
          >
            {isExpanded ? (
              <RestoreIcon width={18} height={18} />
            ) : (
              <MaximizeIcon width={18} height={18} />
            )}
          </HeaderIconButton>
        ) : null}
        {onMinimize ? (
          <HeaderIconButton label="Minimizar" onClick={onMinimize}>
            <MinusIcon width={18} height={18} />
          </HeaderIconButton>
        ) : null}
      </div>
    </div>
  );
}
