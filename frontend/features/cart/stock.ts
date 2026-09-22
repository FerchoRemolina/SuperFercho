/** Cart UX over catalog stock already loaded beside GET /cart. Not a backend total. */

export function canIncreaseCartQuantity(
  quantity: number,
  stock: number | undefined,
): boolean {
  if (stock === undefined) {
    return true;
  }
  return quantity < stock;
}

export function isCartQuantityAboveStock(
  quantity: number,
  stock: number | undefined,
): boolean {
  return stock !== undefined && quantity > stock;
}

export function cartStockWarning(
  quantity: number,
  stock: number | undefined,
): string | null {
  if (stock === undefined) {
    return null;
  }
  if (stock <= 0) {
    return "Este producto ya no tiene unidades disponibles. Quita la línea o espera a que haya existencias.";
  }
  if (quantity > stock) {
    const units =
      stock === 1 ? "1 unidad disponible" : `${stock} unidades disponibles`;
    return `Solo hay ${units}. Reduce la cantidad para continuar.`;
  }
  return null;
}
