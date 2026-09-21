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
    <nav aria-label="Categorías" className="flex flex-wrap gap-2">
      <Link
        href="/catalog"
        className={cx(
          "inline-flex min-h-11 items-center rounded-lg px-3 text-sm font-semibold",
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
            className={cx(
              "inline-flex min-h-11 items-center rounded-lg px-3 text-sm font-semibold",
              active
                ? "bg-sf-primary text-white"
                : "bg-sf-bg text-sf-ink hover:text-sf-primary",
            )}
          >
            {category.name}
          </Link>
        );
      })}
    </nav>
  );
}
