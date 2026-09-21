import { Container } from "@/shared/ui/container";

export function PlaceholderScreen({
  title,
  description,
}: {
  title: string;
  description: string;
}) {
  return (
    <Container as="main" className="py-10 md:py-16">
      <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
        {title}
      </h1>
      <p className="mt-3 max-w-2xl text-base text-sf-muted">{description}</p>
    </Container>
  );
}
