import { request } from "@/shared/api/client";

/** Mirrors AddressStatus. */
export type AddressStatus = "ACTIVE" | "INACTIVE";

/** Mirrors AddressRestResponse. Does not include customerId. */
export type Address = {
  id: string;
  label: string;
  recipientName: string;
  addressLine: string;
  additionalInfo: string | null;
  city: string;
  department: string;
  phone: string;
  isDefault: boolean;
  status: AddressStatus;
  createdAt: string;
  updatedAt: string;
};

/** Mirrors AddAddressRequest. Identity comes from the JWT. */
export type AddAddressRequest = {
  label: string;
  recipientName: string;
  addressLine: string;
  additionalInfo: string | null;
  city: string;
  department: string;
  phone: string;
  isDefault: boolean;
};

/** Mirrors UpdateAddressRequest. isDefault is not updatable here. */
export type UpdateAddressRequest = {
  label: string;
  recipientName: string;
  addressLine: string;
  additionalInfo: string | null;
  city: string;
  department: string;
  phone: string;
};

export function addressKeys() {
  return {
    root: () => ["addresses"] as const,
  };
}

export async function listAddresses(): Promise<Address[]> {
  return request<Address[]>("/addresses");
}

export async function addAddress(body: AddAddressRequest): Promise<Address> {
  return request<Address>("/addresses", {
    method: "POST",
    body,
  });
}

export async function updateAddress(
  addressId: string,
  body: UpdateAddressRequest,
): Promise<Address> {
  return request<Address>(`/addresses/${encodeURIComponent(addressId)}`, {
    method: "PUT",
    body,
  });
}

export async function deactivateAddress(addressId: string): Promise<Address> {
  return request<Address>(`/addresses/${encodeURIComponent(addressId)}`, {
    method: "DELETE",
  });
}

export async function setDefaultAddress(addressId: string): Promise<Address> {
  return request<Address>(
    `/addresses/${encodeURIComponent(addressId)}/default`,
    { method: "POST" },
  );
}

export function isAddressActive(address: Address): boolean {
  return address.status === "ACTIVE";
}

export function activeAddresses(addresses: Address[]): Address[] {
  return addresses.filter(isAddressActive);
}
