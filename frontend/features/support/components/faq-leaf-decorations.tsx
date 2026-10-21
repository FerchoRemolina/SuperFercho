import { brandingAssets } from "@/shared/branding/assets";

/**
 * Hojas PNG en los márgenes superiores del FAQ.
 * Nota: los archivos actuales tienen fondo blanco opaco (sin canal alfa útil).
 */
export function FaqLeafDecorations() {
  return (
    <div
      aria-hidden="true"
      className="pointer-events-none absolute inset-0 z-0 select-none overflow-hidden"
    >
      {/* eslint-disable-next-line @next/next/no-img-element -- branding /public */}
      <img
        src={brandingAssets.hojasIzquierdas}
        alt=""
        width={296}
        height={668}
        className="absolute -left-6 -top-4 hidden h-auto w-[7.5rem] max-w-[22vw] sm:block md:-left-8 md:-top-6 md:w-[9.5rem] lg:-left-10 lg:w-[11.5rem] xl:-left-12 xl:w-[13rem]"
      />
      {/* eslint-disable-next-line @next/next/no-img-element -- branding /public */}
      <img
        src={brandingAssets.hojasDerechas}
        alt=""
        width={298}
        height={673}
        className="absolute -right-6 -top-4 hidden h-auto w-[7.5rem] max-w-[22vw] sm:block md:-right-8 md:-top-6 md:w-[9.5rem] lg:-right-10 lg:w-[11.5rem] xl:-right-12 xl:w-[13rem]"
      />
    </div>
  );
}
