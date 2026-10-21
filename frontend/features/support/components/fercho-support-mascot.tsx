import { brandingAssets } from "@/shared/branding/assets";
import { cx } from "@/shared/utils/cx";

/**
 * Ilustración de soporte: Fercho comprando (cuerpo + carrito).
 * PNG con alfa real — sin mix-blend ni fondos artificiales.
 */
export function FerchoSupportMascot({ className }: { className?: string }) {
  return (
    // eslint-disable-next-line @next/next/no-img-element -- mascota branding desde /public
    <img
      src={brandingAssets.ferchoMascot}
      alt=""
      width={1536}
      height={1024}
      className={cx(
        "pointer-events-none h-auto w-[17rem] max-w-none shrink-0 select-none",
        "drop-shadow-[0_18px_28px_rgba(20,53,40,0.18)]",
        "sm:w-[21.5rem] md:w-[23.5rem] lg:w-[26rem] xl:w-[28rem]",
        className,
      )}
    />
  );
}
