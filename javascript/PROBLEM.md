# Topsort: Mini Ad Platform

## Quick facts

| | |
|---|---|
| **Language** | JavaScript (Node.js) |
| **What you write** | One class (`AdPlatform`) with four methods in `solution.js` |
| **Allowed** | Node.js built-ins only, no npm packages |
| **Done means** | All 15 tests in `tests.js` pass |

```bash
node tests.js
```

---

## Background

Topsort runs a retail media platform. Advertisers bid on search keywords so their products appear when shoppers search on a marketplace. When someone searches, an auction picks which ads to show based on bid amounts. The platform then tracks interactions (impressions, clicks, purchases) and uses **last-click attribution** to connect purchases back to the ads that influenced them. Advertisers use this to understand their return on ad spend (ROAS).

You will build a small in-memory version of this system.

---

## What to implement

Copy `starter.js` to `solution.js` and implement the `AdPlatform` class:

```js
class AdPlatform {
  constructor(attributionWindowSeconds = 3600) { ... }
  placeBid(advertiserId, productId, keyword, bidAmount) { ... }   // returns bid_id string
  runAuction(query, numSlots) { ... }                             // returns array of objects
  recordEvent(event) { ... }
  getAdvertiserStats(advertiserId) { ... }                        // returns object
}
```

### `placeBid` → `bid_id`

Register a bid. Returns a unique string ID: `"bid_1"`, `"bid_2"`, etc.

### `runAuction` → `Array`

Return the top `numSlots` bids matching `query` (case-insensitive), ranked by `bidAmount` descending. Ties broken by `bid_id` ascending (earlier bid wins). Each result: `{ slot, bid_id, advertiser_id, product_id }`. Auctions don't charge anyone.

### `recordEvent`

Record an impression, click, or purchase:

```js
{ type: "impression", timestamp: ..., user_id: ..., advertiser_id: ..., product_id: ... }
{ type: "click",      timestamp: ..., user_id: ..., advertiser_id: ..., product_id: ... }
{ type: "purchase",   timestamp: ..., user_id: ..., product_id: ...,   amount: ... }
```

Clicks charge the advertiser the `bidAmount` of their matching bid. Purchases trigger last-click attribution: find the most recent click from the same user for the same product within `attributionWindowSeconds`. The advertiser whose click gets credit earns the conversion and revenue.

### `getAdvertiserStats` → `Object`

```js
{
  impressions: number,
  clicks: number,
  spend: number,                  // rounded to 2 decimal places
  attributed_conversions: number,
  attributed_revenue: number,     // rounded to 2 decimal places
}
```

---

## Example

```js
const p = new AdPlatform(3600);
p.placeBid("adv_a", "prod_x", "shoes", 1.50);  // "bid_1"
p.placeBid("adv_b", "prod_y", "shoes", 1.20);  // "bid_2"

p.runAuction("shoes", 1);
// [{ slot: 0, bid_id: "bid_1", advertiser_id: "adv_a", product_id: "prod_x" }]

p.recordEvent({ type: "impression", timestamp: 1700000000, user_id: "u1", advertiser_id: "adv_a", product_id: "prod_x" });
p.recordEvent({ type: "click",      timestamp: 1700000010, user_id: "u1", advertiser_id: "adv_a", product_id: "prod_x" });
p.recordEvent({ type: "purchase",   timestamp: 1700000300, user_id: "u1", product_id: "prod_x", amount: 49.99 });

p.getAdvertiserStats("adv_a");
// { impressions: 1, clicks: 1, spend: 1.50, attributed_conversions: 1, attributed_revenue: 49.99 }
```
