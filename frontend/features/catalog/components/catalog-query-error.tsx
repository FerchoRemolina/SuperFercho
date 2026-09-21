import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";

export function CatalogQueryError({
  error,
  title = "No se pudo cargar",
}: {
  error: unknown;
  title?: string;
}) {
  const message = isApiError(error)
    ? messageForApiProblem(error.problem)
    : "No se pudo completar la solicitud.";

  return (
    <Alert tone="error" title={title}>
      {message}
    </Alert>
  );
}
