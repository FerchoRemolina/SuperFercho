import Link from "next/link";
import { brandingAssets } from "@/shared/branding/assets";
import { cx } from "@/shared/utils/cx";

export function BrandLogo({
  className,
}: {
  className?: string;
}) {
  return (
    <Link
      href="/"
      className={cx(
        "inline-flex min-h-11 items-center text-lg font-bold tracking-tight text-sf-primary",
        className,
      )}
      data-brand-logo={brandingAssets.logo}
      data-brand-mark={brandingAssets.mark}
    >
      SuperFercho
    </Link>
  );
}
