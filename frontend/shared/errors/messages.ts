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
    case "PRODUCT_STOCK_CONFLICT":
      return "El stock cambió mientras lo actualizabas. Revisa el valor actual e inténtalo de nuevo.";
    case "CATEGORY_NOT_FOUND":
      return "No encontramos esta categoría.";
    case "CART_NOT_FOUND":
      return "No encontramos tu carrito.";
    case "SHOPPING_LIST_NOT_FOUND":
      return "No encontramos esa lista.";
    case "INVALID_SHOPPING_LIST":
      return "La lista no es válida. Revisa el nombre e inténtalo de nuevo.";
    case "INVALID_SHOPPING_LIST_ITEM":
      return "La cantidad debe ser mayor que cero.";
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
      return "Este pedido ya no puede cancelarse. El plazo de 15 minutos terminó o el pedido ya cambió de estado.";
    case "INVALID_ORDER_TRANSITION":
      return "El pedido ya no está en un estado que permita esta acción.";
    case "INVALID_ORDER_STATUS_UPDATE":
      return "No se puede establecer ese estado desde esta acción.";
    case "PAYMENT_NOT_FOUND":
      return "No se pudo cargar la información de pago de este pedido.";
    case "INVALID_DOCUMENT":
      return "El documento no es válido para esta operación.";
    case "DOCUMENT_NOT_FOUND":
      return "No encontramos ese documento.";
    case "KNOWLEDGE_PROCESSING_FAILED":
      return "No se pudo procesar el documento. Inténtalo de nuevo.";
    case "INVALID_SEARCH_REQUEST":
      return "La búsqueda no es válida. Revisa el límite (1 a 20) e inténtalo de nuevo.";
    case "INVALID_CHAT_REQUEST":
      return "El mensaje no es válido. Revisa el texto e inténtalo de nuevo.";
    case "INVALID_CONFIRMATION":
      return "La confirmación ya no es válida. Vuelve a solicitar la acción con Fercho.";
    case "CONVERSATION_NOT_FOUND":
      return "No encontramos esa conversación. Inicia una nueva.";
    case "LLM_PROVIDER_FAILED":
      return "Fercho no pudo responder ahora. Inténtalo de nuevo en unos momentos.";
    case "TOOL_NOT_ALLOWED":
      return "Fercho no puede realizar esa acción.";
    case "INVALID_TOOL_ARGUMENTS":
      return "Fercho recibió datos incompletos para esa acción. Inténtalo de nuevo.";
    case "INVALID_BARCODE":
      return "El código de barras no es válido.";
    case "BARCODE_LOOKUP_NOT_FOUND":
      return "No encontramos ese código de barras en Open Food Facts.";
    case "BARCODE_LOOKUP_FAILED":
      return "No se pudo consultar Open Food Facts. Inténtalo de nuevo o completa el producto a mano.";
    case "ACCESS_DENIED":
      return "No tienes permiso para esta acción.";
    case "UNAUTHENTICATED":
      return "Debes iniciar sesión para continuar.";
    case "INTERNAL_ERROR":
      return "Ocurrió un error interno. Inténtalo más tarde.";
    default:
      break;
  }

  // Never surface raw backend detail (often English) to end users.
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
      return "No se pudo completar la solicitud.";
  }
}
