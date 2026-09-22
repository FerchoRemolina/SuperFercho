import type {
  ChangeAdminProductPriceRequest,
  CreateAdminCategoryRequest,
  CreateAdminProductRequest,
  UpdateAdminCategoryRequest,
  UpdateAdminProductRequest,
} from "@/features/admin/api";
import { copMoney } from "@/features/admin/api";
import type { Category, Product } from "@/features/catalog/api";

export type AdminCategoryFormValues = {
  name: string;
  description: string;
};

export type AdminCategoryFieldErrors = Partial<
  Record<keyof AdminCategoryFormValues, string>
>;

export function emptyAdminCategoryFormValues(): AdminCategoryFormValues {
  return {
    name: "",
    description: "",
  };
}

export function validateAdminCategory(
  values: AdminCategoryFormValues,
): AdminCategoryFieldErrors {
  const errors: AdminCategoryFieldErrors = {};
  if (values.name.trim().length === 0) {
    errors.name = "Escribe el nombre de la categoría.";
  }
  return errors;
}

export function createAdminCategoryRequestFromValues(
  values: AdminCategoryFormValues,
): CreateAdminCategoryRequest {
  return {
    name: values.name.trim(),
    description: optionalText(values.description),
  };
}

export function updateAdminCategoryRequestFromValues(
  values: AdminCategoryFormValues,
): UpdateAdminCategoryRequest {
  return {
    name: values.name.trim(),
    description: optionalText(values.description),
  };
}

export function adminCategoryFormValuesFromCategory(
  category: Category,
): AdminCategoryFormValues {
  return {
    name: category.name,
    description: category.description ?? "",
  };
}

export type AdminProductFormValues = {
  categoryId: string;
  barcode: string;
  name: string;
  brand: string;
  description: string;
  price: string;
  stock: string;
  imageUrl: string;
};

export type AdminProductFieldErrors = Partial<
  Record<keyof AdminProductFormValues, string>
>;

export function emptyAdminProductFormValues(): AdminProductFormValues {
  return {
    categoryId: "",
    barcode: "",
    name: "",
    brand: "",
    description: "",
    price: "",
    stock: "",
    imageUrl: "",
  };
}

export function optionalText(value: string): string | null {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

export function parseNonNegativeAmount(raw: string): number | null {
  const trimmed = raw.trim().replace(",", ".");
  if (trimmed.length === 0) {
    return null;
  }
  if (!/^\d+(\.\d{1,2})?$/.test(trimmed)) {
    return null;
  }
  const amount = Number(trimmed);
  if (!Number.isFinite(amount) || amount < 0) {
    return null;
  }
  return amount;
}

export function parseNonNegativeInt(raw: string): number | null {
  const trimmed = raw.trim();
  if (trimmed.length === 0 || !/^\d+$/.test(trimmed)) {
    return null;
  }
  const value = Number(trimmed);
  if (!Number.isInteger(value) || value < 0) {
    return null;
  }
  return value;
}

export function validateCreateAdminProduct(
  values: AdminProductFormValues,
): AdminProductFieldErrors {
  const errors: AdminProductFieldErrors = {};
  if (values.name.trim().length === 0) {
    errors.name = "Escribe el nombre del producto.";
  }
  if (values.categoryId.trim().length === 0) {
    errors.categoryId = "Elige una categoría.";
  }
  if (parseNonNegativeAmount(values.price) === null) {
    errors.price = "Indica un precio mayor o igual a 0, con hasta dos decimales.";
  }
  if (parseNonNegativeInt(values.stock) === null) {
    errors.stock = "Indica el stock inicial con un número entero mayor o igual a 0.";
  }
  return errors;
}

export function validateUpdateAdminProduct(
  values: AdminProductFormValues,
): AdminProductFieldErrors {
  const errors: AdminProductFieldErrors = {};
  if (values.name.trim().length === 0) {
    errors.name = "Escribe el nombre del producto.";
  }
  if (values.categoryId.trim().length === 0) {
    errors.categoryId = "Elige una categoría.";
  }
  return errors;
}

export function validateAdminProductPrice(raw: string): string | undefined {
  if (parseNonNegativeAmount(raw) === null) {
    return "Indica un precio mayor o igual a 0, con hasta dos decimales.";
  }
  return undefined;
}

export function createAdminProductRequestFromValues(
  values: AdminProductFormValues,
): CreateAdminProductRequest {
  const amount = parseNonNegativeAmount(values.price);
  const stock = parseNonNegativeInt(values.stock);
  if (amount === null || stock === null) {
    throw new Error("createAdminProductRequestFromValues requires valid price and stock");
  }
  return {
    categoryId: values.categoryId.trim(),
    barcode: optionalText(values.barcode),
    name: values.name.trim(),
    brand: optionalText(values.brand),
    description: optionalText(values.description),
    price: copMoney(amount),
    stock,
    imageUrl: optionalText(values.imageUrl),
  };
}

export function updateAdminProductRequestFromValues(
  values: AdminProductFormValues,
): UpdateAdminProductRequest {
  return {
    categoryId: values.categoryId.trim(),
    barcode: optionalText(values.barcode),
    name: values.name.trim(),
    brand: optionalText(values.brand),
    description: optionalText(values.description),
    imageUrl: optionalText(values.imageUrl),
  };
}

export function changeAdminProductPriceRequestFromAmount(
  raw: string,
): ChangeAdminProductPriceRequest {
  const amount = parseNonNegativeAmount(raw);
  if (amount === null) {
    throw new Error("changeAdminProductPriceRequestFromAmount requires a valid price");
  }
  return { price: copMoney(amount) };
}

export function adminProductFormValuesFromProduct(
  product: Product,
): AdminProductFormValues {
  return {
    categoryId: product.categoryId,
    barcode: product.barcode ?? "",
    name: product.name,
    brand: product.brand ?? "",
    description: product.description ?? "",
    price: formatAmountInput(product.price.amount),
    stock: String(product.stock),
    imageUrl: product.imageUrl ?? "",
  };
}

function formatAmountInput(amount: number): string {
  return Number.isInteger(amount) ? String(amount) : amount.toFixed(2);
}
