import type {
  AdjustAdminProductStockRequest,
  AdminProduct,
  BarcodeProductSuggestion,
  ChangeAdminProductPriceRequest,
  CreateAdminCategoryRequest,
  CreateAdminKnowledgeDocumentRequest,
  CreateAdminProductRequest,
  CreateAdminProductTypeRequest,
  CreateAdminProductVariantRequest,
  PresentationUnit,
  ProductType,
  ProductVariant,
  ReplaceAdminKnowledgeDocumentContentRequest,
  UpdateAdminCategoryRequest,
  UpdateAdminProductRequest,
  UpdateAdminProductTypeRequest,
  UpdateAdminProductVariantRequest,
} from "@/features/admin/api";
import { copMoney, PRESENTATION_UNITS } from "@/features/admin/api";
import type { Category } from "@/features/catalog/api";

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

export type AdminProductTypeFormValues = {
  name: string;
  description: string;
};

export type AdminProductTypeFieldErrors = Partial<
  Record<keyof AdminProductTypeFormValues, string>
>;

export function emptyAdminProductTypeFormValues(): AdminProductTypeFormValues {
  return {
    name: "",
    description: "",
  };
}

export function validateAdminProductType(
  values: AdminProductTypeFormValues,
): AdminProductTypeFieldErrors {
  const errors: AdminProductTypeFieldErrors = {};
  if (values.name.trim().length === 0) {
    errors.name = "Escribe el nombre del tipo de producto.";
  }
  return errors;
}

/** categoryId comes from Category Detail context; never from the form. */
export function createAdminProductTypeRequestFromValues(
  categoryId: string,
  values: AdminProductTypeFormValues,
): CreateAdminProductTypeRequest {
  return {
    categoryId,
    name: values.name.trim(),
    description: optionalText(values.description),
  };
}

export function updateAdminProductTypeRequestFromValues(
  values: AdminProductTypeFormValues,
): UpdateAdminProductTypeRequest {
  return {
    name: values.name.trim(),
    description: optionalText(values.description),
  };
}

export function adminProductTypeFormValuesFromProductType(
  productType: ProductType,
): AdminProductTypeFormValues {
  return {
    name: productType.name,
    description: productType.description ?? "",
  };
}

export type AdminProductVariantFormValues = {
  name: string;
  description: string;
};

export type AdminProductVariantFieldErrors = Partial<
  Record<keyof AdminProductVariantFormValues, string>
>;

export function emptyAdminProductVariantFormValues(): AdminProductVariantFormValues {
  return {
    name: "",
    description: "",
  };
}

export function validateAdminProductVariant(
  values: AdminProductVariantFormValues,
): AdminProductVariantFieldErrors {
  const errors: AdminProductVariantFieldErrors = {};
  if (values.name.trim().length === 0) {
    errors.name = "Escribe el nombre de la variante.";
  }
  return errors;
}

/** productTypeId comes from the parent ProductType; never from the form. */
export function createAdminProductVariantRequestFromValues(
  productTypeId: string,
  values: AdminProductVariantFormValues,
): CreateAdminProductVariantRequest {
  return {
    productTypeId,
    name: values.name.trim(),
    description: optionalText(values.description),
  };
}

export function updateAdminProductVariantRequestFromValues(
  values: AdminProductVariantFormValues,
): UpdateAdminProductVariantRequest {
  return {
    name: values.name.trim(),
    description: optionalText(values.description),
  };
}

export function adminProductVariantFormValuesFromProductVariant(
  productVariant: ProductVariant,
): AdminProductVariantFormValues {
  return {
    name: productVariant.name,
    description: productVariant.description ?? "",
  };
}

export type AdminProductFormValues = {
  /** UI filter only; not sent on Product create/update (category comes from ProductType). */
  categoryId: string;
  productTypeId: string;
  /** Empty string means no variant (serialized as null). */
  productVariantId: string;
  presentationQuantity: string;
  presentationUnit: PresentationUnit | "";
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
    productTypeId: "",
    productVariantId: "",
    presentationQuantity: "1",
    presentationUnit: "UNIT",
    barcode: "",
    name: "",
    brand: "",
    description: "",
    price: "",
    stock: "",
    imageUrl: "",
  };
}

/**
 * Prefills only barcode metadata fields from Open Food Facts.
 * Never touches price, stock, categoryId, productTypeId, presentation (or status).
 */
export function applyBarcodeSuggestion(
  values: AdminProductFormValues,
  suggestion: BarcodeProductSuggestion,
): AdminProductFormValues {
  return {
    ...values,
    barcode: suggestion.barcode ?? values.barcode,
    name: suggestion.name?.trim() ? suggestion.name : values.name,
    brand: suggestion.brand?.trim() ? suggestion.brand : values.brand,
    description: suggestion.description?.trim()
      ? suggestion.description
      : values.description,
    imageUrl: suggestion.imageUrl?.trim() ? suggestion.imageUrl : values.imageUrl,
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

export function parsePresentationQuantity(raw: string): number | null {
  const trimmed = raw.trim().replace(",", ".");
  if (trimmed.length === 0) {
    return null;
  }
  if (!/^\d+(\.\d{1,3})?$/.test(trimmed)) {
    return null;
  }
  const quantity = Number(trimmed);
  if (!Number.isFinite(quantity) || quantity <= 0) {
    return null;
  }
  return quantity;
}

export function isPresentationUnit(value: string): value is PresentationUnit {
  return (PRESENTATION_UNITS as readonly string[]).includes(value);
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
  if (values.productTypeId.trim().length === 0) {
    errors.productTypeId = "Elige un tipo de producto.";
  }
  if (parsePresentationQuantity(values.presentationQuantity) === null) {
    errors.presentationQuantity =
      "Indica una cantidad mayor que 0, con hasta tres decimales.";
  }
  if (!isPresentationUnit(values.presentationUnit)) {
    errors.presentationUnit = "Elige una unidad de presentación.";
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
  if (values.productTypeId.trim().length === 0) {
    errors.productTypeId = "Elige un tipo de producto.";
  }
  if (parsePresentationQuantity(values.presentationQuantity) === null) {
    errors.presentationQuantity =
      "Indica una cantidad mayor que 0, con hasta tres decimales.";
  }
  if (!isPresentationUnit(values.presentationUnit)) {
    errors.presentationUnit = "Elige una unidad de presentación.";
  }
  return errors;
}

export function validateAdminProductPrice(raw: string): string | undefined {
  if (parseNonNegativeAmount(raw) === null) {
    return "Indica un precio mayor o igual a 0, con hasta dos decimales.";
  }
  return undefined;
}

export function validateAdminProductStock(raw: string): string | undefined {
  if (parseNonNegativeInt(raw) === null) {
    return "Indica el stock con un número entero mayor o igual a 0.";
  }
  return undefined;
}

export function createAdminProductRequestFromValues(
  values: AdminProductFormValues,
): CreateAdminProductRequest {
  const amount = parseNonNegativeAmount(values.price);
  const stock = parseNonNegativeInt(values.stock);
  const quantity = parsePresentationQuantity(values.presentationQuantity);
  if (
    amount === null ||
    stock === null ||
    quantity === null ||
    !isPresentationUnit(values.presentationUnit)
  ) {
    throw new Error(
      "createAdminProductRequestFromValues requires valid price, stock, and presentation",
    );
  }
  if (values.productTypeId.trim().length === 0) {
    throw new Error("createAdminProductRequestFromValues requires productTypeId");
  }
  return {
    productTypeId: values.productTypeId.trim(),
    productVariantId: optionalText(values.productVariantId),
    presentation: { quantity, unit: values.presentationUnit },
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
  const quantity = parsePresentationQuantity(values.presentationQuantity);
  if (quantity === null || !isPresentationUnit(values.presentationUnit)) {
    throw new Error(
      "updateAdminProductRequestFromValues requires valid presentation",
    );
  }
  if (values.productTypeId.trim().length === 0) {
    throw new Error("updateAdminProductRequestFromValues requires productTypeId");
  }
  return {
    productTypeId: values.productTypeId.trim(),
    productVariantId: optionalText(values.productVariantId),
    presentation: { quantity, unit: values.presentationUnit },
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

export function adjustAdminProductStockRequestFromValue(
  raw: string,
): AdjustAdminProductStockRequest {
  const stock = parseNonNegativeInt(raw);
  if (stock === null) {
    throw new Error("adjustAdminProductStockRequestFromValue requires a valid stock");
  }
  return { stock };
}

export function adminProductFormValuesFromProduct(
  product: AdminProduct,
): AdminProductFormValues {
  return {
    categoryId: product.categoryId,
    productTypeId: product.productTypeId,
    productVariantId: product.productVariantId ?? "",
    presentationQuantity: formatPresentationQuantityInput(product.presentation.quantity),
    presentationUnit: product.presentation.unit,
    barcode: product.barcode ?? "",
    name: product.name,
    brand: product.brand ?? "",
    description: product.description ?? "",
    price: formatAmountInput(product.price.amount),
    stock: String(product.stock),
    imageUrl: product.imageUrl ?? "",
  };
}

/** Category is UI-only; changing it clears Type and Variant immediately. */
export function withAdminProductCategoryId(
  values: AdminProductFormValues,
  categoryId: string,
): AdminProductFormValues {
  return {
    ...values,
    categoryId,
    productTypeId: "",
    productVariantId: "",
  };
}

/** Changing Type clears Variant so a stale Variant of the previous Type is never kept. */
export function withAdminProductTypeId(
  values: AdminProductFormValues,
  productTypeId: string,
): AdminProductFormValues {
  return {
    ...values,
    productTypeId,
    productVariantId: "",
  };
}

function formatPresentationQuantityInput(quantity: number): string {
  return String(quantity);
}

export type AdminKnowledgeDocumentFormValues = {
  title: string;
  source: string;
  content: string;
};

export type AdminKnowledgeDocumentFieldErrors = Partial<
  Record<keyof AdminKnowledgeDocumentFormValues, string>
>;

export function emptyAdminKnowledgeDocumentFormValues(): AdminKnowledgeDocumentFormValues {
  return {
    title: "",
    source: "",
    content: "",
  };
}

export function validateAdminKnowledgeDocument(
  values: AdminKnowledgeDocumentFormValues,
): AdminKnowledgeDocumentFieldErrors {
  const errors: AdminKnowledgeDocumentFieldErrors = {};
  if (values.title.trim().length === 0) {
    errors.title = "Escribe el título del documento.";
  }
  if (values.source.trim().length === 0) {
    errors.source = "Escribe la fuente del documento.";
  }
  if (values.content.trim().length === 0) {
    errors.content = "Escribe el contenido del documento.";
  }
  return errors;
}

export function createAdminKnowledgeDocumentRequestFromValues(
  values: AdminKnowledgeDocumentFormValues,
): CreateAdminKnowledgeDocumentRequest {
  return {
    title: values.title.trim(),
    source: values.source.trim(),
    content: values.content.trim(),
  };
}

export type AdminKnowledgeDocumentContentFormValues = {
  content: string;
};

export type AdminKnowledgeDocumentContentFieldErrors = Partial<
  Record<keyof AdminKnowledgeDocumentContentFormValues, string>
>;

export function emptyAdminKnowledgeDocumentContentFormValues(): AdminKnowledgeDocumentContentFormValues {
  return { content: "" };
}

export function adminKnowledgeDocumentContentFormValuesFromDocument(args: {
  content: string;
}): AdminKnowledgeDocumentContentFormValues {
  return { content: args.content };
}

export function validateAdminKnowledgeDocumentContent(
  values: AdminKnowledgeDocumentContentFormValues,
): AdminKnowledgeDocumentContentFieldErrors {
  const errors: AdminKnowledgeDocumentContentFieldErrors = {};
  if (values.content.trim().length === 0) {
    errors.content = "Escribe el contenido del documento.";
  }
  return errors;
}

export function replaceAdminKnowledgeDocumentContentRequestFromValues(
  values: AdminKnowledgeDocumentContentFormValues,
): ReplaceAdminKnowledgeDocumentContentRequest {
  return { content: values.content.trim() };
}

function formatAmountInput(amount: number): string {
  return Number.isInteger(amount) ? String(amount) : amount.toFixed(2);
}
