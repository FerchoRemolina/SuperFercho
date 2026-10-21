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
  id,
}: {
  initialText?: string;
  compact?: boolean;
  autoFocus?: boolean;
  /** Sobreescribe el id del campo (necesario si hay varias barras en el DOM). */
  id?: string;
}) {
  const router = useRouter();
  const [text, setText] = useState(initialText);
  const inputId = id ?? (compact ? "header-search" : "catalog-search");

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
      className={cx(
        "flex w-full items-center gap-2 rounded-full border border-sf-border bg-sf-surface p-1.5 pl-3.5 shadow-sm",
        "transition-colors focus-within:border-sf-primary focus-within:ring-2 focus-within:ring-sf-primary/25",
        compact ? "max-w-2xl" : "max-w-xl",
      )}
      role="search"
    >
      <label htmlFor={inputId} className="sr-only">
        Buscar productos
      </label>
      <SearchIcon className="h-4 w-4 shrink-0 text-sf-muted" />
      <input
        id={inputId}
        type="search"
        name="text"
        value={text}
        onChange={(event) => setText(event.target.value)}
        placeholder={
          compact
            ? "Buscar productos, marcas o categorías…"
            : "Buscar productos"
        }
        autoFocus={autoFocus}
        className={cx(
          "min-h-10 min-w-0 flex-1 bg-transparent text-sm text-sf-ink outline-none placeholder:text-sf-muted md:text-base",
          // El contenedor ya comunica el foco (borde + ring verde); el campo
          // no dibuja su propio contorno rectangular interior ni el botón
          // nativo de limpieza de WebKit/Blink.
          "focus-visible:outline-none [&::-webkit-search-cancel-button]:hidden [&::-webkit-search-decoration]:hidden",
        )}
      />
      <Button
        type="submit"
        className={
          compact
            ? "h-10 w-14 shrink-0 justify-center rounded-xl px-0"
            : "min-h-10 shrink-0 rounded-full px-4"
        }
        aria-label="Buscar"
      >
        <SearchIcon className="h-4 w-4" />
        {compact ? null : <span className="pl-1">Buscar</span>}
      </Button>
    </form>
  );
}
