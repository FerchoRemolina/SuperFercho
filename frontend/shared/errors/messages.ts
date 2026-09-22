import { type ApiProblem } from "@/shared/errors/api-problem";

export function messageForApiProblem(problem: ApiProblem): string {
  switch (problem.code) {
    case "INVALID_CREDENTIALS":
      return "Correo o contraseña incorrectos.";
    case "USER_INACTIVE":
      return "Esta cuenta está inactiva.";
    case "USER_ALREADY_EXISTS":
      return "Ya existe una cuenta con ese correo.";
    case "DOCUMENT_ALREADY_EXISTS":
      return "Ya existe una cuenta con ese documento.";
    case "PRODUCT_NOT_FOUND":
      return "No encontramos este producto.";
    case "CATEGORY_NOT_FOUND":
      return "No encontramos esta categoría.";
    case "CART_NOT_FOUND":
      return "No encontramos tu carrito.";
    case "INVALID_CART":
      return "No se pudo actualizar el carrito.";
    case "INVALID_CART_ITEM":
      return "La cantidad debe ser mayor que cero.";
    case "INVALID_ADDRESS":
      return "Revisa los datos de la dirección.";
    case "ADDRESS_NOT_FOUND":
      return "No encontramos esa dirección.";
    case "ADDRESS_INACTIVE":
      return "Esta dirección está inactiva.";
    case "DUPLICATE_DEFAULT_ADDRESS":
      return "Ya tienes una dirección predeterminada.";
    case "INVALID_CHECKOUT":
      return "La solicitud del pedido no es válida. Revisa los datos e inténtalo de nuevo.";
    case "INVALID_ORDER":
      return "No se pudo procesar el pedido.";
    case "IDEMPOTENCY_CONFLICT":
      return "Este intento de pedido ya no es válido. Revisa los datos e inicia un nuevo intento.";
    case "PRODUCT_PRICE_CHANGED":
      return "El precio de uno o más productos cambió. Revisa el pedido antes de confirmar.";
    case "PRODUCT_NOT_AVAILABLE":
      return "Uno o más productos ya no están disponibles. Revisa el pedido.";
    case "STOCK_UNAVAILABLE":
      return "No hay existencias suficientes. Revisa las cantidades.";
    case "CART_EMPTY":
      return "Tu carrito ya no tiene productos.";
    case "ADDRESS_NOT_AVAILABLE":
      return "La dirección seleccionada ya no está disponible. Elige otra.";
    case "PAYMENT_DECLINED":
      return "El pago no fue aprobado. Puedes intentar de nuevo o elegir otro método.";
    case "ORDER_NOT_FOUND":
      return "No encontramos ese pedido.";
    case "CANCELLATION_NOT_ALLOWED":
      return "Este pedido ya no puede cancelarse.";
    case "INVALID_ORDER_TRANSITION":
      return "El pedido ya no está en un estado que permita esta acción.";
    case "INVALID_ORDER_STATUS_UPDATE":
      return "No se puede establecer ese estado desde esta acción.";
    case "PAYMENT_NOT_FOUND":
      return "No se pudo cargar la información de pago de este pedido.";
    case "ACCESS_DENIED":
      return "No tienes permiso para esta acción.";
    case "UNAUTHENTICATED":
      return "Debes iniciar sesión para continuar.";
    case "INTERNAL_ERROR":
      return "Ocurrió un error interno. Inténtalo más tarde.";
    default:
      break;
  }

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
