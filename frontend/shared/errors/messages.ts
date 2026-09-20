import { type ApiProblem } from "@/shared/errors/api-problem";

export function messageForApiProblem(problem: ApiProblem): string {
  if (problem.detail) {
    return problem.detail;
  }

  switch (problem.status) {
    case 400:
      return "La solicitud no es válida.";
    case 401:
      return "Debes iniciar sesión de nuevo.";
    case 403:
      return problem.code === "USER_INACTIVE"
        ? "Esta cuenta está inactiva."
        : "No tienes permiso para esta acción.";
    case 404:
      return "No encontramos ese recurso.";
    case 409:
      return "No se pudo completar la operación.";
    case 500:
      return "Ocurrió un error interno. Inténtalo más tarde.";
    default:
      return problem.title ?? "No se pudo completar la solicitud.";
  }
}
