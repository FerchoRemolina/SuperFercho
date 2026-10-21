import Link from "next/link";
import type { Category } from "@/features/catalog/api";
import { cx } from "@/shared/utils/cx";

export function CategoryChips({
  categories,
  activeCategoryId,
}: {
  categories: Category[];
  activeCategoryId?: string;
}) {
  return (
    <nav
      aria-label="Categorías"
      className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1.5 [-ms-overflow-style:none] [scrollbar-width:none] md:flex-wrap md:overflow-visible [&::-webkit-scrollbar]:hidden"
    >
      <Link
        href="/catalog"
        className={cx(
          "inline-flex min-h-11 shrink-0 items-center rounded-full px-3.5 text-sm font-semibold transition-colors duration-150",
          activeCategoryId
            ? "border border-sf-border bg-sf-surface text-sf-ink shadow-sm hover:border-sf-primary/40 hover:text-sf-primary"
            : "bg-sf-primary text-white shadow-sm hover:bg-sf-primary-hover",
        )}
      >
        Todas
      </Link>
      {categories.map((category) => {
        const active = category.id === activeCategoryId;
        return (
          <Link
            key={category.id}
            href={`/categories/${category.id}`}
            title={category.name}
            className={cx(
              "inline-flex min-h-11 max-w-[12rem] shrink-0 items-center rounded-full px-3.5 text-sm font-semibold transition-colors duration-150 md:max-w-none",
              active
                ? "bg-sf-primary text-white shadow-sm hover:bg-sf-primary-hover"
                : "border border-sf-border bg-sf-surface text-sf-ink shadow-sm hover:border-sf-primary/40 hover:text-sf-primary",
            )}
          >
            <span className="truncate">{category.name}</span>
          </Link>
        );
      })}
    </nav>
  );
}
