export function PlaceholderScreen({
  title,
  description,
}: {
  title: string;
  description: string;
}) {
  return (
    <main className="mx-auto max-w-3xl px-4 py-10">
      <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
      <p className="mt-3 text-sf-muted">{description}</p>
      <p className="mt-6 rounded-md border border-sf-border bg-sf-surface px-4 py-3 text-sm text-sf-muted">
        Esta ruta existe como cascarón de la fundación frontend. No llama al API
        ni implementa la funcionalidad de negocio todavía.
      </p>
    </main>
  );
}
