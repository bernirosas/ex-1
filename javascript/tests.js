/**
 * Tests for the Topsort take-home.
 *
 * Run:   node tests.js
 * Grade: SOLUTION_MODULE=candidate node tests.js
 */

const moduleName = process.env.SOLUTION_MODULE || "solution";
let AdPlatform;
try {
  ({ AdPlatform } = require(`./${moduleName}`));
} catch (e) {
  console.error(`Failed to load module '${moduleName}': ${e.message}`);
  process.exit(1);
}

const results = [];

function test(name, fn) {
  try {
    fn();
    results.push({ name, ok: true });
    console.log(`  PASS  ${name}`);
  } catch (e) {
    results.push({ name, ok: false, msg: e.message });
    console.log(`  FAIL  ${name}`);
    console.log(`        ${e.message}`);
  }
}

function assertEqual(actual, expected, msg = "") {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) {
    throw new Error(`${msg}\n  expected: ${e}\n  got:      ${a}`);
  }
}

function assertClose(actual, expected, tol = 1e-6, msg = "") {
  if (Math.abs(actual - expected) > tol) {
    throw new Error(`${msg}\n  expected: ${expected}\n  got:      ${actual}`);
  }
}

// ------------------------------------------------------------------ //

test("placeBid returns sequential bid_ids", () => {
  const p = new AdPlatform();
  assertEqual(p.placeBid("a", "p1", "shoes", 1.0), "bid_1");
  assertEqual(p.placeBid("b", "p2", "shoes", 1.0), "bid_2");
  assertEqual(p.placeBid("a", "p3", "hats", 0.5), "bid_3");
});

test("auction with no bids returns empty list", () => {
  const p = new AdPlatform();
  assertEqual(p.runAuction("shoes", 3), []);
});

test("auction filters by keyword (case-insensitive)", () => {
  const p = new AdPlatform();
  p.placeBid("a", "p1", "Shoes", 1.0);
  p.placeBid("b", "p2", "hats", 1.0);
  const out = p.runAuction("SHOES", 3);
  assertEqual(out.length, 1);
  assertEqual(out[0].advertiser_id, "a");
});

test("auction ranks by bid_amount descending", () => {
  const p = new AdPlatform();
  p.placeBid("a", "p1", "shoes", 1.0);
  p.placeBid("b", "p2", "shoes", 2.0);
  p.placeBid("c", "p3", "shoes", 1.5);
  const out = p.runAuction("shoes", 3);
  assertEqual(
    out.map((w) => w.bid_id),
    ["bid_2", "bid_3", "bid_1"]
  );
  assertEqual(
    out.map((w) => w.slot),
    [0, 1, 2]
  );
});

test("auction ties broken by bid_id ascending", () => {
  const p = new AdPlatform();
  p.placeBid("a", "p1", "shoes", 1.5);
  p.placeBid("b", "p2", "shoes", 1.5);
  const out = p.runAuction("shoes", 1);
  assertEqual(out.length, 1);
  assertEqual(out[0].bid_id, "bid_1");
});

test("auction returns fewer winners when fewer bids exist", () => {
  const p = new AdPlatform();
  p.placeBid("a", "p1", "shoes", 1.5);
  const out = p.runAuction("shoes", 5);
  assertEqual(out.length, 1);
});

test("click charges spend at the bid_amount", () => {
  const p = new AdPlatform();
  p.placeBid("a", "p1", "shoes", 1.5);
  p.recordEvent({
    type: "click",
    timestamp: 1700000000,
    user_id: "u1",
    advertiser_id: "a",
    product_id: "p1",
  });
  const s = p.getAdvertiserStats("a");
  assertEqual(s.clicks, 1);
  assertClose(s.spend, 1.5);
});

test("spend accumulates across clicks", () => {
  const p = new AdPlatform();
  p.placeBid("a", "p1", "shoes", 0.75);
  for (const ts of [1700000000, 1700000100, 1700000200]) {
    p.recordEvent({
      type: "click",
      timestamp: ts,
      user_id: "u1",
      advertiser_id: "a",
      product_id: "p1",
    });
  }
  const s = p.getAdvertiserStats("a");
  assertEqual(s.clicks, 3);
  assertClose(s.spend, 2.25);
});

test("attribution: simple last-click within window", () => {
  const p = new AdPlatform(3600);
  p.placeBid("a", "p1", "shoes", 1.5);
  p.recordEvent({
    type: "impression",
    timestamp: 1700000000,
    user_id: "u1",
    advertiser_id: "a",
    product_id: "p1",
  });
  p.recordEvent({
    type: "click",
    timestamp: 1700000010,
    user_id: "u1",
    advertiser_id: "a",
    product_id: "p1",
  });
  p.recordEvent({
    type: "purchase",
    timestamp: 1700000500,
    user_id: "u1",
    product_id: "p1",
    amount: 49.99,
  });
  const s = p.getAdvertiserStats("a");
  assertEqual(s.impressions, 1);
  assertEqual(s.clicks, 1);
  assertClose(s.spend, 1.5);
  assertEqual(s.attributed_conversions, 1);
  assertClose(s.attributed_revenue, 49.99);
});

test("attribution: purchase outside window is unattributed", () => {
  const p = new AdPlatform(60);
  p.placeBid("a", "p1", "shoes", 1.5);
  p.recordEvent({
    type: "click",
    timestamp: 1700000000,
    user_id: "u1",
    advertiser_id: "a",
    product_id: "p1",
  });
  p.recordEvent({
    type: "purchase",
    timestamp: 1700000500,
    user_id: "u1",
    product_id: "p1",
    amount: 49.99,
  });
  const s = p.getAdvertiserStats("a");
  assertEqual(s.attributed_conversions, 0);
  assertClose(s.attributed_revenue, 0.0);
  assertClose(s.spend, 1.5);
});

test("attribution: different product is unattributed", () => {
  const p = new AdPlatform(3600);
  p.placeBid("a", "p1", "shoes", 1.5);
  p.recordEvent({
    type: "click",
    timestamp: 1700000000,
    user_id: "u1",
    advertiser_id: "a",
    product_id: "p1",
  });
  p.recordEvent({
    type: "purchase",
    timestamp: 1700000300,
    user_id: "u1",
    product_id: "p2",
    amount: 49.99,
  });
  const s = p.getAdvertiserStats("a");
  assertEqual(s.attributed_conversions, 0);
});

test("attribution: last-click wins among multiple clicks", () => {
  const p = new AdPlatform(3600);
  p.placeBid("a", "p1", "shoes", 1.0);
  p.placeBid("b", "p1", "shoes", 0.5);
  p.recordEvent({
    type: "click",
    timestamp: 1700000000,
    user_id: "u1",
    advertiser_id: "a",
    product_id: "p1",
  });
  p.recordEvent({
    type: "click",
    timestamp: 1700000100,
    user_id: "u1",
    advertiser_id: "b",
    product_id: "p1",
  });
  p.recordEvent({
    type: "purchase",
    timestamp: 1700000200,
    user_id: "u1",
    product_id: "p1",
    amount: 80.0,
  });
  const sa = p.getAdvertiserStats("a");
  const sb = p.getAdvertiserStats("b");
  assertEqual(sa.attributed_conversions, 0);
  assertEqual(sb.attributed_conversions, 1);
  assertClose(sb.attributed_revenue, 80.0);
});

test("attribution: one click can credit multiple purchases", () => {
  const p = new AdPlatform(3600);
  p.placeBid("a", "p1", "shoes", 1.0);
  p.recordEvent({
    type: "click",
    timestamp: 1700000000,
    user_id: "u1",
    advertiser_id: "a",
    product_id: "p1",
  });
  p.recordEvent({
    type: "purchase",
    timestamp: 1700000100,
    user_id: "u1",
    product_id: "p1",
    amount: 30.0,
  });
  p.recordEvent({
    type: "purchase",
    timestamp: 1700000200,
    user_id: "u1",
    product_id: "p1",
    amount: 20.0,
  });
  const s = p.getAdvertiserStats("a");
  assertEqual(s.attributed_conversions, 2);
  assertClose(s.attributed_revenue, 50.0);
});

test("multi-advertiser stats are isolated", () => {
  const p = new AdPlatform(3600);
  p.placeBid("a", "p1", "shoes", 1.5);
  p.placeBid("b", "p2", "shoes", 1.2);
  p.recordEvent({
    type: "impression",
    timestamp: 1700000000,
    user_id: "u1",
    advertiser_id: "a",
    product_id: "p1",
  });
  p.recordEvent({
    type: "impression",
    timestamp: 1700000001,
    user_id: "u1",
    advertiser_id: "b",
    product_id: "p2",
  });
  p.recordEvent({
    type: "click",
    timestamp: 1700000010,
    user_id: "u1",
    advertiser_id: "a",
    product_id: "p1",
  });
  p.recordEvent({
    type: "purchase",
    timestamp: 1700000100,
    user_id: "u1",
    product_id: "p1",
    amount: 50.0,
  });
  const sa = p.getAdvertiserStats("a");
  const sb = p.getAdvertiserStats("b");
  assertEqual(sa.impressions, 1);
  assertEqual(sa.clicks, 1);
  assertClose(sa.spend, 1.5);
  assertEqual(sa.attributed_conversions, 1);
  assertClose(sa.attributed_revenue, 50.0);
  assertEqual(sb.impressions, 1);
  assertEqual(sb.clicks, 0);
  assertClose(sb.spend, 0.0);
  assertEqual(sb.attributed_conversions, 0);
  assertClose(sb.attributed_revenue, 0.0);
});

test("end-to-end scenario from PROBLEM.md example", () => {
  const p = new AdPlatform(3600);
  p.placeBid("adv_a", "prod_x", "shoes", 1.5);
  p.placeBid("adv_b", "prod_y", "shoes", 1.2);
  const winners = p.runAuction("shoes", 1);
  assertEqual(winners.length, 1);
  assertEqual(winners[0].bid_id, "bid_1");
  assertEqual(winners[0].advertiser_id, "adv_a");
  p.recordEvent({
    type: "impression",
    timestamp: 1700000000,
    user_id: "u1",
    advertiser_id: "adv_a",
    product_id: "prod_x",
  });
  p.recordEvent({
    type: "click",
    timestamp: 1700000010,
    user_id: "u1",
    advertiser_id: "adv_a",
    product_id: "prod_x",
  });
  p.recordEvent({
    type: "purchase",
    timestamp: 1700000300,
    user_id: "u1",
    product_id: "prod_x",
    amount: 49.99,
  });
  const s = p.getAdvertiserStats("adv_a");
  assertEqual(s, {
    impressions: 1,
    clicks: 1,
    spend: 1.5,
    attributed_conversions: 1,
    attributed_revenue: 49.99,
  });
});

// ------------------------------------------------------------------ //

const passed = results.filter((r) => r.ok).length;
const total = results.length;
console.log(`\n${passed}/${total} tests passed`);
process.exit(passed === total ? 0 : 1);
