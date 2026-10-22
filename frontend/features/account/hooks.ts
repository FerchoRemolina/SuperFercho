"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  addAddress,
  addressKeys,
  customerProfileKeys,
  deactivateAddress,
  getCustomerProfile,
  listAddresses,
  setDefaultAddress,
  updateAddress,
  updateCustomerProfile,
  type AddAddressRequest,
  type UpdateAddressRequest,
  type UpdateCustomerProfileRequest,
} from "@/features/account/api";
import { applyCustomerProfileNamesToSession } from "@/features/account/profile-session";
import { useSession } from "@/shared/session/session-provider";

const keys = addressKeys();
const profileKeys = customerProfileKeys();

export function useAddressesQuery() {
  const { session } = useSession();
  const enabled = session?.role === "CUSTOMER";

  return useQuery({
    queryKey: keys.root(),
    queryFn: listAddresses,
    enabled,
  });
}

export function useCustomerProfileQuery() {
  const { session } = useSession();
  const enabled = session?.role === "CUSTOMER";

  return useQuery({
    queryKey: profileKeys.root(),
    queryFn: getCustomerProfile,
    enabled,
  });
}

export function useUpdateCustomerProfileMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: UpdateCustomerProfileRequest) =>
      updateCustomerProfile(body),
    onSuccess: (profile) => {
      queryClient.setQueryData(profileKeys.root(), profile);
      queryClient.invalidateQueries({ queryKey: profileKeys.root() });
      applyCustomerProfileNamesToSession(profile.firstName, profile.lastName);
    },
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
