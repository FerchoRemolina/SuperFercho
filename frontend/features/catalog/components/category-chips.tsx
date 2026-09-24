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
      className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1 [-ms-overflow-style:none] [scrollbar-width:none] md:flex-wrap md:overflow-visible [&::-webkit-scrollbar]:hidden"
    >
      <Link
        href="/catalog"
        className={cx(
          "inline-flex min-h-11 shrink-0 items-center rounded-lg px-3 text-sm font-semibold",
          activeCategoryId
            ? "bg-sf-bg text-sf-ink hover:text-sf-primary"
            : "bg-sf-primary text-white",
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
              "inline-flex min-h-11 max-w-[12rem] shrink-0 items-center rounded-lg px-3 text-sm font-semibold md:max-w-none",
              active
                ? "bg-sf-primary text-white"
                : "bg-sf-bg text-sf-ink hover:text-sf-primary",
            )}
          >
            <span className="truncate">{category.name}</span>
          </Link>
        );
      })}
    </nav>
  );
}
