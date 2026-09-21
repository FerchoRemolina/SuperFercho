"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/shared/ui/button";
import { SearchIcon } from "@/shared/ui/icons";
import { cx } from "@/shared/utils/cx";

export function SearchBar({
  initialText = "",
  compact = false,
  autoFocus = false,
}: {
  initialText?: string;
  compact?: boolean;
  autoFocus?: boolean;
}) {
  const router = useRouter();
  const [text, setText] = useState(initialText);

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const submitted = String(
      new FormData(event.currentTarget).get("text") ?? "",
    ).trim();
    setText(submitted);
    const href = submitted
      ? `/search?text=${encodeURIComponent(submitted)}`
      : "/search";
    router.push(href);
  }

  return (
    <form
      action="/search"
      method="get"
      onSubmit={onSubmit}
      className={cx("flex w-full gap-2", compact ? "max-w-sm" : "max-w-xl")}
      role="search"
    >
      <label htmlFor={compact ? "header-search" : "catalog-search"} className="sr-only">
        Buscar productos
      </label>
      <input
        id={compact ? "header-search" : "catalog-search"}
        type="search"
        name="text"
        value={text}
        onChange={(event) => setText(event.target.value)}
        placeholder="Buscar productos"
        autoFocus={autoFocus}
        className="min-h-11 flex-1 rounded-lg border border-sf-border bg-sf-surface px-3 text-base text-sf-ink"
      />
      <Button type="submit" className="gap-2 px-3" aria-label="Buscar">
        <SearchIcon />
        {compact ? null : <span>Buscar</span>}
      </Button>
    </form>
  );
}
