/** Onda orgánica que une la zona crema del FAQ con el footer verde. */
export function FooterWave({ className }: { className?: string }) {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 1440 96"
      preserveAspectRatio="none"
      className={className}
    >
      <path
        d="M0 64C180 20 320 8 480 28c160 20 240 60 400 56 160-4 260-48 400-60 80-6 120 2 160 12v60H0V64z"
        fill="currentColor"
      />
    </svg>
  );
}
