export const COP = "COP";

export type Money = {
  amount: number;
  currency: typeof COP;
};

export function formatMoney(money: Money): string {
  return new Intl.NumberFormat("es-CO", {
    style: "currency",
    currency: money.currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(money.amount);
}
