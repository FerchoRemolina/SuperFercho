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
        "inline-flex shrink-0 items-center min-h-11",
        className,
      )}
      aria-label="SuperFercho"
    >
      {/* eslint-disable-next-line @next/next/no-img-element -- static branding SVG from /public */}
      <img
        src={brandingAssets.logo}
        alt="SuperFercho"
        width={208}
        height={38}
        className="h-auto w-[170px] shrink-0 md:w-[208px]"
      />
    </Link>
  );
}
