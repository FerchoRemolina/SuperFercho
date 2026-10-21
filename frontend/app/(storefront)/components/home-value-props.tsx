import { brandingAssets } from "@/shared/branding/assets";
import { Container } from "@/shared/ui/container";
import { CartIcon, PackageIcon } from "@/shared/ui/icons";
import { cx } from "@/shared/utils/cx";
import styles from "./home-value-props.module.css";

const benefits = [
  {
    visual: "delivery",
    title: "Entrega a domicilio",
    description: "Recibe tu mercado donde quieras, sin filas ni demoras.",
  },
  {
    visual: "catalog",
    title: "Todo en un solo lugar",
    description:
      "Encuentra tus productos favoritos y arma tu mercado sin complicaciones.",
  },
  {
    visual: "assistant",
    title: "Fercho, tu asistente",
    description:
      "Encuentra lo que buscas, arma tus listas y sigue tus pedidos con Fercho.",
  },
] as const;

type BenefitVisual = (typeof benefits)[number]["visual"];

const iconMotionClassName =
  "flex shrink-0 items-center justify-center transition-transform duration-300 ease-out group-hover:-rotate-3 group-hover:scale-105";

function BenefitIcon({ visual }: { visual: BenefitVisual }) {
  if (visual === "assistant") {
    return (
      <span
        aria-hidden="true"
        className={cx(iconMotionClassName, "size-12 md:size-14 lg:size-16")}
      >
        {/* eslint-disable-next-line @next/next/no-img-element -- static branding SVG from /public */}
        <img
          src={brandingAssets.ferchoIcon}
          alt=""
          className="size-full object-contain drop-shadow-[0_10px_18px_rgba(8,116,67,0.28)]"
        />
      </span>
    );
  }
  const Icon = visual === "delivery" ? PackageIcon : CartIcon;
  return (
    <span
      aria-hidden="true"
      className={cx(
        iconMotionClassName,
        "size-11 rounded-2xl bg-sf-primary text-sf-yellow-soft shadow-[0_12px_26px_-12px_rgba(8,116,67,0.6)] ring-1 ring-sf-primary/30 md:size-12 lg:size-14",
      )}
    >
      <Icon className="size-5 md:size-6" />
    </span>
  );
}

export function HomeValueProps() {
  return (
    <section aria-label="Beneficios" className="relative z-10">
      <Container width="wide">
        <div className="relative -mt-6 overflow-hidden rounded-[1.75rem] border border-sf-border bg-gradient-to-br from-sf-primary-soft/80 via-sf-surface to-sf-yellow-soft/70 shadow-[0_1px_2px_rgba(23,33,27,0.05),0_24px_60px_-28px_rgba(8,116,67,0.35)] sm:-mt-8 md:-mt-10 md:rounded-[2rem]">
          <div
            aria-hidden="true"
            className="pointer-events-none absolute -right-16 -top-24 size-64 rounded-full bg-sf-yellow/40 blur-3xl"
          />
          <div
            aria-hidden="true"
            className="pointer-events-none absolute -bottom-24 -left-20 size-56 rounded-full bg-sf-primary/10 blur-3xl"
          />
          <ol className="relative grid divide-y divide-sf-border/70 sm:grid-cols-3 sm:divide-y-0">
            {benefits.map((benefit, index) => (
              <li
                key={benefit.title}
                className={cx(
                  styles.item,
                  "group relative flex items-start gap-3.5 p-4 transition-colors duration-200 hover:bg-sf-surface/80 sm:gap-4 sm:p-5 md:p-6 lg:gap-5 lg:p-8",
                  index > 0 &&
                    "before:content-[''] sm:before:absolute sm:before:top-1/2 sm:before:left-0 sm:before:h-10 sm:before:w-px sm:before:-translate-y-1/2 sm:before:bg-sf-border/70",
                )}
              >
                <BenefitIcon visual={benefit.visual} />
                <div className="min-w-0">
                  <p className="text-[0.9375rem] font-semibold leading-snug tracking-[-0.01em] text-sf-ink transition-colors duration-200 group-hover:text-sf-primary lg:text-base">
                    {benefit.title}
                  </p>
                  <p className="mt-1.5 text-[0.8125rem] leading-relaxed text-sf-muted lg:text-sm">
                    {benefit.description}
                  </p>
                </div>
              </li>
            ))}
          </ol>
        </div>
      </Container>
    </section>
  );
}
