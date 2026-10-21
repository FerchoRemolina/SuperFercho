"use client";

import { useQuery } from "@tanstack/react-query";
import {
  getPublicStorefrontFooterSettings,
  listPublicStorefrontFaqs,
} from "@/features/storefront/api";

export const storefrontPublicKeys = {
  faqs: () => ["storefront", "public", "faqs"] as const,
  footerSettings: () => ["storefront", "public", "footer-settings"] as const,
};

export function usePublicStorefrontFaqsQuery() {
  return useQuery({
    queryKey: storefrontPublicKeys.faqs(),
    queryFn: listPublicStorefrontFaqs,
  });
}

export function usePublicStorefrontFooterSettingsQuery() {
  return useQuery({
    queryKey: storefrontPublicKeys.footerSettings(),
    queryFn: getPublicStorefrontFooterSettings,
  });
}
