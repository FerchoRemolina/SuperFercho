import type { ComponentType, SVGProps } from "react";
import {
  AppleIcon,
  BabyIcon,
  BreadIcon,
  CupIcon,
  DeviceIcon,
  HomeIcon,
  MeatIcon,
  MilkIcon,
  PackageIcon,
  PawIcon,
  SparklesIcon,
  TagIcon,
} from "@/shared/ui/icons";
import { type CategoryIconId } from "@/features/catalog/api";
import { cx } from "@/shared/utils/cx";

type IconComponent = ComponentType<SVGProps<SVGSVGElement>>;

export type CategoryIconOption = {
  id: CategoryIconId;
  label: string;
  Icon: IconComponent;
  /** Fondo pastel + color de icono (puramente visual; el backend solo
   *  persiste el identificador). */
  tile: string;
};

/** Orden del picker: primero las categorías típicas del súper. */
export const CATEGORY_ICON_OPTIONS: readonly CategoryIconOption[] = [
  { id: "CLEANING", label: "Aseo y hogar", Icon: HomeIcon, tile: "bg-sky-100 text-sky-600" },
  { id: "DRINKS", label: "Bebidas", Icon: CupIcon, tile: "bg-rose-100 text-rose-600" },
  { id: "PERSONAL_CARE", label: "Cuidado personal", Icon: SparklesIcon, tile: "bg-amber-100 text-amber-600" },
  { id: "GROCERY", label: "Despensa", Icon: PackageIcon, tile: "bg-violet-100 text-violet-600" },
  { id: "FRUITS", label: "Frutas y verduras", Icon: AppleIcon, tile: "bg-emerald-100 text-emerald-600" },
  { id: "BAKERY", label: "Panadería", Icon: BreadIcon, tile: "bg-orange-100 text-orange-600" },
  { id: "MEAT", label: "Carnes y pescados", Icon: MeatIcon, tile: "bg-red-100 text-red-500" },
  { id: "DAIRY", label: "Lácteos y refrigerados", Icon: MilkIcon, tile: "bg-indigo-100 text-indigo-600" },
  { id: "PETS", label: "Mascotas", Icon: PawIcon, tile: "bg-teal-100 text-teal-600" },
  { id: "BABY", label: "Bebé", Icon: BabyIcon, tile: "bg-pink-100 text-pink-600" },
  { id: "ELECTRONICS", label: "Electrónica", Icon: DeviceIcon, tile: "bg-slate-200 text-slate-600" },
  { id: "HOME", label: "Hogar", Icon: HomeIcon, tile: "bg-cyan-100 text-cyan-700" },
  { id: "OTHER", label: "Otro", Icon: TagIcon, tile: "bg-sf-bg text-sf-muted" },
];

const ICON_BY_ID = new Map<CategoryIconId, CategoryIconOption>(
  CATEGORY_ICON_OPTIONS.map((option) => [option.id, option]),
);

const FALLBACK_OPTION: CategoryIconOption = {
  id: "OTHER",
  label: "Otro",
  Icon: TagIcon,
  tile: "bg-sf-bg text-sf-muted",
};

/** Resolución por identificador persistido. IDs desconocidos (datos viejos)
 *  caen defensivamente en OTHER. */
export function resolveCategoryIcon(
  icon: string | null | undefined,
): CategoryIconOption {
  return ICON_BY_ID.get(icon as CategoryIconId) ?? FALLBACK_OPTION;
}

export function CategoryIcon({
  icon,
  className = "h-10 w-10 rounded-lg",
  iconClassName = "h-5 w-5",
}: {
  icon: string | null | undefined;
  className?: string;
  iconClassName?: string;
}) {
  const option = resolveCategoryIcon(icon);
  return (
    <span
      className={cx(
        "flex shrink-0 items-center justify-center",
        option.tile,
        className,
      )}
    >
      <option.Icon className={iconClassName} />
    </span>
  );
}
