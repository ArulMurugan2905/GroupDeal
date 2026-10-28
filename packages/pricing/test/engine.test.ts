import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { quote, validateSchedule, finalUnitPrice, type Schedule } from '../src/engine.ts';
const g = JSON.parse(readFileSync(new URL('./golden-vectors.json', import.meta.url), 'utf8'));
const ghee: Schedule = g.schedule;

test('golden vectors (shared with Android + web)', () => {
  for (const c of g.cases) {
    const q = quote(ghee, c.n);
    assert.equal(q.tierIndex, c.tierIndex, `tier n=${c.n}`);
    assert.equal(q.unitPricePaise, c.unitPricePaise, `price n=${c.n}`);
    assert.equal(q.savingsPercent, c.savingsPercent, `saving n=${c.n}`);
    assert.equal(q.membersToNextTier, c.membersToNextTier, `next n=${c.n}`);
  }
});
test('18 members -> ₹949, 12 more for ₹799, locality saves ₹8,118', () => {
  const q = quote(ghee, 18);
  assert.equal(q.unitPricePaise, 94900); assert.equal(q.nextTierPricePaise, 79900); assert.equal(q.membersToNextTier, 12);
  assert.equal((140000 - q.unitPricePaise) * 18, 811800);
});
test('membersToNextTier is never negative (fixes prototype defect D2)', () => {
  for (let n = 0; n <= 100; n++) { const m = quote(ghee, n).membersToNextTier; assert.ok(m === null || m > 0); }
});
test('validation', () => {
  assert.deepEqual(validateSchedule(ghee), []);
  assert.ok(validateSchedule({ ...ghee, tiers: [] }).includes('NO_TIERS'));
  assert.ok(validateSchedule({ ...ghee, tiers: [ghee.tiers[1], ghee.tiers[0]] }).includes('THRESHOLD_NOT_INCREASING'));
  assert.ok(validateSchedule({ ...ghee, tiers: [{ minMembers: 5, unitPricePaise: 90000 }, { minMembers: 15, unitPricePaise: 95000 }] }).includes('PRICE_NOT_DECREASING'));
  assert.ok(validateSchedule({ ...ghee, basePricePaise: 150000 }).includes('BASE_ABOVE_MRP'));
  assert.ok(validateSchedule({ ...ghee, mrpPaise: 1400.5 }).includes('NON_INTEGER_MONEY'));
});
test('auction final price never exceeds the locked ceiling', () => {
  assert.equal(finalUnitPrice(94900, 92000), 92000);
  assert.equal(finalUnitPrice(94900, null), 94900);
  assert.throws(() => finalUnitPrice(94900, 95000), /BID_ABOVE_CEILING/);
});
test('monotonic: more members never increases price', () => {
  let prev = Infinity; for (let n = 0; n <= 60; n++) { const p = quote(ghee, n).unitPricePaise; assert.ok(p <= prev); prev = p; }
});
