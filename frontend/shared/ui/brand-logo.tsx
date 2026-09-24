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
        "inline-flex min-h-11 items-center",
        className,
      )}
      aria-label="SuperFercho"
    >
      {/* eslint-disable-next-line @next/next/no-img-element -- static branding SVG from /public */}
      <img
        src={brandingAssets.logo}
        alt="SuperFercho"
        width={176}
        height={32}
        className="h-8 w-auto"
      />
    </Link>
  );
}
