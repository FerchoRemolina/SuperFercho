import type { SVGProps } from "react";
import { cx } from "@/shared/utils/cx";

type ArtProps = SVGProps<SVGSVGElement>;

/** Small decorative accents for Admin Hub section cards (no external assets). */
export function HubProductsArt({ className, ...props }: ArtProps) {
  return (
    <svg
      viewBox="0 0 128 112"
      fill="none"
      aria-hidden="true"
      className={cx("h-full w-full", className)}
      {...props}
    >
      <ellipse cx="64" cy="98" rx="42" ry="8" className="fill-current opacity-20" />
      <path
        d="M28 58l36-20 36 20v28L64 106 28 86V58z"
        className="fill-current opacity-25"
      />
      <path
        d="M28 58l36 20 36-20M64 78v28"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinejoin="round"
        className="opacity-55"
      />
      <path
        d="M40 52l12-7 12 7v14l-12 7-12-7V52z"
        className="fill-current opacity-35"
      />
      <rect
        x="72"
        y="44"
        width="14"
        height="22"
        rx="3"
        className="fill-current opacity-40"
      />
      <rect
        x="88"
        y="50"
        width="12"
        height="16"
        rx="3"
        className="fill-current opacity-30"
      />
    </svg>
  );
}

export function HubCategoriesArt({ className, ...props }: ArtProps) {
  return (
    <svg
      viewBox="0 0 128 112"
      fill="none"
      aria-hidden="true"
      className={cx("h-full w-full", className)}
      {...props}
    >
      <ellipse cx="64" cy="98" rx="40" ry="8" className="fill-current opacity-20" />
      <path
        d="M34 42h60l-8 40H42L34 42z"
        className="fill-current opacity-25"
      />
      <path
        d="M40 38c0-8 6-14 14-14h4c8 0 14 6 14 14"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
        className="opacity-50"
      />
      <circle cx="48" cy="58" r="5" className="fill-current opacity-45" />
      <circle cx="64" cy="54" r="6" className="fill-current opacity-55" />
      <circle cx="80" cy="60" r="5" className="fill-current opacity-40" />
      <path
        d="M52 72c4-6 10-8 16-6s10 6 12 12"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        className="opacity-35"
      />
    </svg>
  );
}

export function HubOrdersArt({ className, ...props }: ArtProps) {
  return (
    <svg
      viewBox="0 0 128 112"
      fill="none"
      aria-hidden="true"
      className={cx("h-full w-full", className)}
      {...props}
    >
      <ellipse cx="64" cy="98" rx="38" ry="8" className="fill-current opacity-20" />
      <rect
        x="38"
        y="28"
        width="52"
        height="68"
        rx="8"
        className="fill-current opacity-25"
      />
      <rect
        x="50"
        y="22"
        width="28"
        height="12"
        rx="4"
        className="fill-current opacity-45"
      />
      <path
        d="M50 52h28M50 64h22M50 76h18"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
        className="opacity-50"
      />
      <circle cx="92" cy="70" r="10" className="fill-current opacity-35" />
      <path
        d="M88 70h8M92 66v8"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        className="opacity-55"
      />
    </svg>
  );
}

export function HubKnowledgeArt({ className, ...props }: ArtProps) {
  return (
    <svg
      viewBox="0 0 128 112"
      fill="none"
      aria-hidden="true"
      className={cx("h-full w-full", className)}
      {...props}
    >
      <ellipse cx="64" cy="98" rx="40" ry="8" className="fill-current opacity-20" />
      <rect
        x="30"
        y="48"
        width="36"
        height="44"
        rx="4"
        transform="rotate(-8 48 70)"
        className="fill-current opacity-30"
      />
      <rect
        x="46"
        y="42"
        width="38"
        height="48"
        rx="4"
        transform="rotate(4 65 66)"
        className="fill-current opacity-40"
      />
      <rect
        x="62"
        y="36"
        width="36"
        height="52"
        rx="4"
        className="fill-current opacity-50"
      />
      <path
        d="M70 48h20M70 58h16M70 68h12"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        className="opacity-40"
      />
    </svg>
  );
}

export function HubStockEmptyArt({ className, ...props }: ArtProps) {
  return (
    <svg
      viewBox="0 0 120 100"
      fill="none"
      aria-hidden="true"
      className={cx("h-full w-full", className)}
      {...props}
    >
      <ellipse cx="52" cy="86" rx="34" ry="7" className="fill-current opacity-15" />
      <path
        d="M28 48l24-14 24 14v26L52 88 28 74V48z"
        className="fill-current opacity-25"
      />
      <path
        d="M28 48l24 14 24-14M52 62v26"
        stroke="currentColor"
        strokeWidth="2.25"
        strokeLinejoin="round"
        className="opacity-50"
      />
      <circle cx="86" cy="40" r="16" className="fill-emerald-100" />
      <circle
        cx="86"
        cy="40"
        r="16"
        stroke="currentColor"
        strokeWidth="2"
        className="text-emerald-600 opacity-70"
      />
      <path
        d="M78.5 40.5l5 5 10-11"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="text-emerald-700"
      />
    </svg>
  );
}
