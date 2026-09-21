"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  addAddress,
  addressKeys,
  deactivateAddress,
  listAddresses,
  setDefaultAddress,
  updateAddress,
  type AddAddressRequest,
  type UpdateAddressRequest,
} from "@/features/account/api";
import { useSession } from "@/shared/session/session-provider";

const keys = addressKeys();

export function useAddressesQuery() {
  const { session } = useSession();
  const enabled = session?.role === "CUSTOMER";

  return useQuery({
    queryKey: keys.root(),
    queryFn: listAddresses,
    enabled,
  });
}

export function useAddAddressMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: AddAddressRequest) => addAddress(body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.root() }),
  });
}

export function useUpdateAddressMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      addressId,
      body,
    }: {
      addressId: string;
      body: UpdateAddressRequest;
    }) => updateAddress(addressId, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.root() }),
  });
}

export function useDeactivateAddressMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (addressId: string) => deactivateAddress(addressId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.root() }),
  });
}

export function useSetDefaultAddressMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (addressId: string) => setDefaultAddress(addressId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.root() }),
  });
}
