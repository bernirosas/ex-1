# Topsort: Mini Ad Platform

## Quick facts

| | |
|---|---|
| **Language** | Go 1.21+ |
| **What you write** | `AdPlatform` struct and four methods in `starter.go` |
| **Allowed** | Go standard library only |
| **Done means** | All 15 tests pass |

```bash
go test -v
```

---

## Background

Topsort runs a retail media platform. Advertisers bid on search keywords so their products appear when shoppers search on a marketplace. When someone searches, an auction picks which ads to show based on bid amounts. The platform then tracks interactions (impressions, clicks, purchases) and uses **last-click attribution** to connect purchases back to the ads that influenced them. Advertisers use this to understand their return on ad spend (ROAS).

You will build a small in-memory version of this system.

---

## What to implement

Fill in `starter.go` (implement the methods on `AdPlatform`):

```go
func NewAdPlatform(attributionWindowSeconds int) *AdPlatform

func (p *AdPlatform) PlaceBid(advertiserID, productID, keyword string, bidAmount float64) (string, error)
func (p *AdPlatform) RunAuction(query string, numSlots int) ([]map[string]interface{}, error)
func (p *AdPlatform) RecordEvent(event map[string]interface{}) error
func (p *AdPlatform) GetAdvertiserStats(advertiserID string) (map[string]interface{}, error)
```

### `PlaceBid` → `string`

Register a bid. Returns a unique string ID: `"bid_1"`, `"bid_2"`, etc.

### `RunAuction` → `[]map[string]interface{}`

Return the top `numSlots` bids matching `query` (case-insensitive), ranked by `bidAmount` descending. Ties broken by `bid_id` ascending (earlier bid wins). Each map: `{ slot, bid_id, advertiser_id, product_id }`. Auctions don't charge anyone.

### `RecordEvent`

Record an impression, click, or purchase. The map has a `"type"` key.

Clicks charge the advertiser the `bidAmount` of their matching bid. Purchases trigger last-click attribution: find the most recent click from the same user for the same product within `AttributionWindowSeconds`. The advertiser whose click gets credit earns the conversion and revenue.

### `GetAdvertiserStats` → `map[string]interface{}`

```
{ "impressions", "clicks", "spend" (2dp), "attributed_conversions", "attributed_revenue" (2dp) }
```

---

## Example

```go
p := NewAdPlatform(3600)
p.PlaceBid("adv_a", "prod_x", "shoes", 1.50)  // "bid_1"
p.PlaceBid("adv_b", "prod_y", "shoes", 1.20)  // "bid_2"

// RunAuction("shoes", 1) -> [{ "slot":0, "bid_id":"bid_1", "advertiser_id":"adv_a", "product_id":"prod_x" }]

// After impression, click (+10s), purchase (+300s, amount=49.99):
// GetAdvertiserStats("adv_a") -> { "impressions":1, "clicks":1, "spend":1.5, "attributed_conversions":1, "attributed_revenue":49.99 }
```
