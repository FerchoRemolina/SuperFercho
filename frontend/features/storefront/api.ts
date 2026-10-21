import { request } from "@/shared/api/client";

/** Mirrors FaqPublicRestResponse. */
export type PublicStorefrontFaq = {
  id: string;
  question: string;
  answer: string;
  displayOrder: number;
};

/** Mirrors FooterSettingsRestResponse. */
export type PublicStorefrontFooterSettings = {
  supportEmail: string;
  linkedinUrl: string;
  githubUrl: string;
  qrImageUrl: string | null;
};

export async function listPublicStorefrontFaqs(): Promise<PublicStorefrontFaq[]> {
  return request<PublicStorefrontFaq[]>("/storefront-content/faqs", {
    anonymous: true,
  });
}

export async function getPublicStorefrontFooterSettings(): Promise<PublicStorefrontFooterSettings> {
  const settings = await request<PublicStorefrontFooterSettings>(
    "/storefront-content/footer-settings",
    { anonymous: true },
  );
  return {
    ...settings,
    qrImageUrl: settings.qrImageUrl ?? null,
  };
}
