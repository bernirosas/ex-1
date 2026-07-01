# Topsort: Mini Ad Platform

## Quick facts

| | |
|---|---|
| **Language** | Python 3.10+ |
| **What you write** | One class (`AdPlatform`) with four methods in `solution.py` |
| **Allowed** | Python standard library only |
| **Done means** | All 15 tests in `tests.py` pass |

```bash
python tests.py
```

---

## Background

Topsort runs a retail media platform. Advertisers bid on search keywords so their products appear when shoppers search on a marketplace. When someone searches, an auction picks which ads to show based on bid amounts. The platform then tracks interactions (impressions, clicks, purchases) and uses **last-click attribution** to connect purchases back to the ads that influenced them. Advertisers use this to understand their return on ad spend (ROAS).

You will build a small in-memory version of this system.

---

## What to implement

Copy `starter.py` to `solution.py` and implement the `AdPlatform` class:

```python
class AdPlatform:
    def __init__(self, attribution_window_seconds: int = 3600): ...
    def place_bid(self, advertiser_id: str, product_id: str, keyword: str, bid_amount: float) -> str: ...
    def run_auction(self, query: str, num_slots: int) -> list[dict]: ...
    def record_event(self, event: dict) -> None: ...
    def get_advertiser_stats(self, advertiser_id: str) -> dict: ...
```

### `place_bid` → `bid_id`

Register a bid. Returns a unique string ID: `"bid_1"`, `"bid_2"`, etc.

### `run_auction` → `list[dict]`

Return the top `num_slots` bids matching `query` (case-insensitive), ranked by `bid_amount` descending. Ties broken by `bid_id` ascending (earlier bid wins). Each result: `{"slot": int, "bid_id": str, "advertiser_id": str, "product_id": str}`. Auctions don't charge anyone.

### `record_event`

Record an impression, click, or purchase:

```python
{"type": "impression", "timestamp": ..., "user_id": ..., "advertiser_id": ..., "product_id": ...}
{"type": "click",      "timestamp": ..., "user_id": ..., "advertiser_id": ..., "product_id": ...}
{"type": "purchase",   "timestamp": ..., "user_id": ..., "product_id": ...,   "amount": ...}
```

Clicks charge the advertiser the `bid_amount` of their matching bid. Purchases trigger last-click attribution: find the most recent click from the same user for the same product within `attribution_window_seconds`. The advertiser whose click gets credit earns the conversion and revenue.

### `get_advertiser_stats` → `dict`

```python
{
    "impressions": int,
    "clicks": int,
    "spend": float,                  # rounded to 2 decimal places
    "attributed_conversions": int,
    "attributed_revenue": float,     # rounded to 2 decimal places
}
```

---

## Example

```python
p = AdPlatform(attribution_window_seconds=3600)
p.place_bid("adv_a", "prod_x", "shoes", 1.50)  # "bid_1"
p.place_bid("adv_b", "prod_y", "shoes", 1.20)  # "bid_2"

p.run_auction("shoes", 1)
# [{"slot": 0, "bid_id": "bid_1", "advertiser_id": "adv_a", "product_id": "prod_x"}]

p.record_event({"type": "impression", "timestamp": 1_700_000_000, "user_id": "u1", "advertiser_id": "adv_a", "product_id": "prod_x"})
p.record_event({"type": "click",      "timestamp": 1_700_000_010, "user_id": "u1", "advertiser_id": "adv_a", "product_id": "prod_x"})
p.record_event({"type": "purchase",   "timestamp": 1_700_000_300, "user_id": "u1", "product_id": "prod_x", "amount": 49.99})

p.get_advertiser_stats("adv_a")
# {"impressions": 1, "clicks": 1, "spend": 1.50, "attributed_conversions": 1, "attributed_revenue": 49.99}
```
