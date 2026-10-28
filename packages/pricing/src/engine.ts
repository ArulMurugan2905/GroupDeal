/**
 * GroupDeal pricing engine — the single authoritative pricing rule.
 * Runs inside Cloud Functions (authoritative) and the web app (display only).
 * Android ports this file 1:1 for display; parity is enforced by golden-vectors.json.
 * All money values are integer paise (₹1 = 100 paise). No floating point.
 */
export interface Tier { minMembers: number; unitPricePaise: number }
export interface Schedule { mrpPaise: number; basePricePaise: number; tiers: Tier[] }
export interface Quote {
  memberCount: number;
  tierIndex: number;              // -1 = base price, otherwise index into ascending tiers
  unitPricePaise: number;
  savingsPaise: number;           // per unit, vs MRP (never negative)
  savingsPercent: number;         // floor((mrp - price) * 100 / mrp)
  nextTierIndex: number | null;
  nextTierPricePaise: number | null;
  membersToNextTier: number | null; // never negative; null at top tier
}
export type ValidationError =
  | 'NO_TIERS' | 'NON_INTEGER_MONEY' | 'NON_POSITIVE_PRICE' | 'BASE_ABOVE_MRP'
  | 'FIRST_TIER_NOT_BELOW_BASE' | 'THRESHOLD_NOT_INCREASING' | 'PRICE_NOT_DECREASING' | 'THRESHOLD_BELOW_2';

export function validateSchedule(s: Schedule): ValidationError[] {
  const e: ValidationError[] = [];
  const money = [s.mrpPaise, s.basePricePaise, ...s.tiers.map(t => t.unitPricePaise)];
  if (s.tiers.length === 0) e.push('NO_TIERS');
  if (money.some(m => !Number.isInteger(m))) e.push('NON_INTEGER_MONEY');
  if (money.some(m => m <= 0)) e.push('NON_POSITIVE_PRICE');
  if (s.basePricePaise > s.mrpPaise) e.push('BASE_ABOVE_MRP');
  if (s.tiers.length && s.tiers[0].unitPricePaise >= s.basePricePaise) e.push('FIRST_TIER_NOT_BELOW_BASE');
  if (s.tiers.length && s.tiers[0].minMembers < 2) e.push('THRESHOLD_BELOW_2');
  for (let i = 1; i < s.tiers.length; i++) {
    if (s.tiers[i].minMembers <= s.tiers[i - 1].minMembers) e.push('THRESHOLD_NOT_INCREASING');
    if (s.tiers[i].unitPricePaise >= s.tiers[i - 1].unitPricePaise) e.push('PRICE_NOT_DECREASING');
  }
  return [...new Set(e)];
}

/** Pure quote for a member count. Assumes a validated schedule (tiers ascending). */
export function quote(s: Schedule, memberCount: number): Quote {
  if (!Number.isInteger(memberCount)) throw new Error('memberCount must be an integer');
  const n = Math.max(0, memberCount);                  // negative counts are clamped, never propagated
  let tierIndex = -1;
  for (let i = 0; i < s.tiers.length; i++) if (n >= s.tiers[i].minMembers) tierIndex = i;
  const unitPricePaise = tierIndex === -1 ? s.basePricePaise : s.tiers[tierIndex].unitPricePaise;
  const savingsPaise = Math.max(0, s.mrpPaise - unitPricePaise);
  const savingsPercent = Math.floor((savingsPaise * 100) / s.mrpPaise);
  const nextTierIndex = tierIndex + 1 < s.tiers.length ? tierIndex + 1 : null;
  return {
    memberCount: n, tierIndex, unitPricePaise, savingsPaise, savingsPercent, nextTierIndex,
    nextTierPricePaise: nextTierIndex === null ? null : s.tiers[nextTierIndex].unitPricePaise,
    membersToNextTier: nextTierIndex === null ? null : s.tiers[nextTierIndex].minMembers - n,
  };
}

/** Final price after the reverse auction: never above the locked (ceiling) price. */
export function finalUnitPrice(lockedUnitPricePaise: number, winningBidPaise: number | null): number {
  if (winningBidPaise === null) return lockedUnitPricePaise;      // anchor supplier fallback
  if (winningBidPaise > lockedUnitPricePaise) throw new Error('BID_ABOVE_CEILING');
  return winningBidPaise;
}

export const rupees = (paise: number) => '₹' + (paise / 100).toLocaleString('en-IN');
