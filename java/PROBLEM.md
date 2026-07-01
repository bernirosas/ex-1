# Topsort: Mini Ad Platform

## Quick facts

| | |
|---|---|
| **Language** | Java 11+ |
| **What you write** | One class (`AdPlatform`) with four methods in `AdPlatform.java` |
| **Allowed** | Java standard library only |
| **Done means** | All 15 tests pass |

```bash
javac *.java && java Tests
```

To grade a candidate's file:
```bash
# rename their file to AdPlatform.java, then:
javac *.java && java Tests
```

---

## Background

Topsort runs a retail media platform. Advertisers bid on search keywords so their products appear when shoppers search on a marketplace. When someone searches, an auction picks which ads to show based on bid amounts. The platform then tracks interactions (impressions, clicks, purchases) and uses **last-click attribution** to connect purchases back to the ads that influenced them. Advertisers use this to understand their return on ad spend (ROAS).

You will build a small in-memory version of this system.

---

## What to implement

Fill in `AdPlatform.java`:

```java
public class AdPlatform {
    public AdPlatform(int attributionWindowSeconds) { ... }
    public AdPlatform() { ... }  // defaults to 3600

    public String placeBid(String advertiserId, String productId, String keyword, double bidAmount) { ... }
    public List<Map<String, Object>> runAuction(String query, int numSlots) { ... }
    public void recordEvent(Map<String, Object> event) { ... }
    public Map<String, Object> getAdvertiserStats(String advertiserId) { ... }
}
```

### `placeBid` → `String`

Register a bid. Returns a unique string ID: `"bid_1"`, `"bid_2"`, etc.

### `runAuction` → `List<Map<String, Object>>`

Return the top `numSlots` bids matching `query` (case-insensitive), ranked by `bidAmount` descending. Ties broken by `bid_id` ascending (earlier bid wins). Each map: `{ slot, bid_id, advertiser_id, product_id }`. Auctions don't charge anyone.

### `recordEvent`

Record an impression, click, or purchase. The `event` map has a `"type"` key: `"impression"`, `"click"`, or `"purchase"`.

Clicks charge the advertiser the `bidAmount` of their matching bid. Purchases trigger last-click attribution: find the most recent click from the same user for the same product within `attributionWindowSeconds`. The advertiser whose click gets credit earns the conversion and revenue.

### `getAdvertiserStats` → `Map<String, Object>`

```
{ impressions, clicks, spend (2dp), attributed_conversions, attributed_revenue (2dp) }
```

---

## Example

```java
AdPlatform p = new AdPlatform(3600);
p.placeBid("adv_a", "prod_x", "shoes", 1.50);  // "bid_1"
p.placeBid("adv_b", "prod_y", "shoes", 1.20);  // "bid_2"

// runAuction("shoes", 1) -> [{ slot:0, bid_id:"bid_1", advertiser_id:"adv_a", product_id:"prod_x" }]

// After recording impression, click (ts=+10s), purchase (ts=+300s, amount=49.99):
// getAdvertiserStats("adv_a") -> { impressions:1, clicks:1, spend:1.50, attributed_conversions:1, attributed_revenue:49.99 }
```
