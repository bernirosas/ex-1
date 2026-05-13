# Topsort Take-Home — Mini Ad Platform

## Quick facts

| | |
|---|---|
| **Time budget** | 25–30 minutes |
| **Language** | Python 3.10+ |
| **What you write** | One class (`AdPlatform`) with five methods in `solution.py` |
| **What you're allowed** | Python standard library only |
| **What "done" means** | All 15 tests in `tests.py` pass (`python tests.py` exits with `15/15 tests passed`) |
| **How it's graded** | Tests passed, time taken, code clarity. Passing tests is the primary signal. |

---

## Context

At Topsort, advertisers (sellers) bid on **keywords** so their products appear in a marketplace's search results. When a shopper searches, an **auction** picks which ads to show. The platform then tracks **events** (impressions, clicks, purchases) and uses **attribution** to figure out which clicks led to which purchases — that's how we report ROAS to our customers.

You will build a tiny in-memory version of that system.

---

## What you need to write

Open `starter.py`, copy it to `solution.py`, and implement exactly this class:

```python
class AdPlatform:
    def __init__(self, attribution_window_seconds: int = 3600): ...
    def place_bid(self, advertiser_id: str, product_id: str,
                  keyword: str, bid_amount: float) -> str: ...
    def run_auction(self, query: str, num_slots: int) -> list[dict]: ...
    def record_event(self, event: dict) -> None: ...
    def get_advertiser_stats(self, advertiser_id: str) -> dict: ...
```

The class name and method signatures must match — the tests import `AdPlatform` and call these exact methods.

Below is the contract for each method. Read all five before you write code; the pieces interlock.

---

### 1. `place_bid(advertiser_id, product_id, keyword, bid_amount) -> bid_id`

Register a manual bid placed by an advertiser.

| | |
|---|---|
| **`advertiser_id`** | Who is bidding. |
| **`product_id`** | Which of their products is being promoted. |
| **`keyword`** | Search keyword this bid targets. Matched **case-insensitively** later. |
| **`bid_amount`** | A number used for ranking and for charging clicks (higher = wins more, costs more). |
| **Returns** | A unique `bid_id` of the form `"bid_1"`, `"bid_2"`, ... incrementing by 1 with each call. |

### 2. `run_auction(query, num_slots) -> list[dict]`

Pick the top `num_slots` ads for a search query.

| | |
|---|---|
| **Eligibility** | A bid is eligible iff its `keyword` equals `query` (case-insensitive exact match). |
| **Ranking** | Sort eligible bids by `bid_amount` **descending**, breaking ties by `bid_id` **ascending** (earlier-placed bid wins ties). |
| **Truncation** | Take the top `num_slots`. If fewer eligible bids exist, return what you have (don't pad). |
| **Returns** | A list ordered by slot. Each item: `{"slot": int, "bid_id": str, "advertiser_id": str, "product_id": str}` |

Running an auction does **not** charge anyone — charging happens on clicks (next).

### 3. `record_event(event) -> None`

Record one event. There are three event types:

```python
# Impression: ad was shown.
{"type": "impression", "timestamp": 1700000000,
 "user_id": "u1", "advertiser_id": "adv_a", "product_id": "prod_x"}

# Click: shopper clicked the ad.
{"type": "click", "timestamp": 1700000010,
 "user_id": "u1", "advertiser_id": "adv_a", "product_id": "prod_x"}

# Purchase: shopper bought a product (may or may not be from an ad).
{"type": "purchase", "timestamp": 1700000500,
 "user_id": "u1", "product_id": "prod_x", "amount": 49.99}
```

**Per-type behaviour:**

| Event | What happens |
|-------|--------------|
| **impression** | Store it. No money moves. |
| **click** | Store it, **and** charge the matching bid. Find the bid with the same `(advertiser_id, product_id)` (if multiple match, pick the lowest `bid_id`). Add that bid's `bid_amount` to the advertiser's `spend`. If no bid matches, the click is still recorded but no money moves. |
| **purchase** | Store it, **and** run attribution (see below). |

You can assume events arrive in non-decreasing timestamp order.

### 4. Attribution rules (triggered by purchase events)

For each purchase, find the **most recent prior click** such that all of these hold:

- `click.user_id == purchase.user_id`
- `click.product_id == purchase.product_id`
- `click.timestamp <= purchase.timestamp <= click.timestamp + attribution_window_seconds`

If such a click exists, that click's advertiser gets credit for **one conversion** and `purchase.amount` of **revenue**. If no qualifying click exists, the purchase is **unattributed** (organic) and shows up in no one's stats.

A single click can be the attribution source for **multiple purchases** (each purchase finds its own most-recent click independently).

### 5. `get_advertiser_stats(advertiser_id) -> dict`

Return a summary for one advertiser. Exact shape:

```python
{
    "impressions": int,             # impression events for this advertiser
    "clicks": int,                  # click events for this advertiser
    "spend": float,                 # sum of bid_amount across their clicks (2 decimals)
    "attributed_conversions": int,  # purchases attributed to this advertiser's clicks
    "attributed_revenue": float,    # sum of those purchase amounts (2 decimals)
}
```

Round both `spend` and `attributed_revenue` to 2 decimal places.

---

## Worked example

```python
p = AdPlatform(attribution_window_seconds=3600)

p.place_bid("adv_a", "prod_x", "shoes", bid_amount=1.50)   # -> "bid_1"
p.place_bid("adv_b", "prod_y", "shoes", bid_amount=1.20)   # -> "bid_2"

winners = p.run_auction(query="shoes", num_slots=1)
# winners == [{"slot": 0, "bid_id": "bid_1",
#              "advertiser_id": "adv_a", "product_id": "prod_x"}]

p.record_event({"type": "impression", "timestamp": 1_700_000_000,
                "user_id": "u1", "advertiser_id": "adv_a", "product_id": "prod_x"})
p.record_event({"type": "click", "timestamp": 1_700_000_010,
                "user_id": "u1", "advertiser_id": "adv_a", "product_id": "prod_x"})
p.record_event({"type": "purchase", "timestamp": 1_700_000_300,
                "user_id": "u1", "product_id": "prod_x", "amount": 49.99})

p.get_advertiser_stats("adv_a")
# {
#     "impressions": 1,
#     "clicks": 1,
#     "spend": 1.50,
#     "attributed_conversions": 1,
#     "attributed_revenue": 49.99,
# }
```

---

## What "expected outcome" means

Your solution passes when **all 15 tests** in `tests.py` pass. The suite covers:

- **Bid placement** — sequential `bid_1`, `bid_2`, ... IDs.
- **Auction correctness** — empty input, case-insensitive keyword matching, descending sort by `bid_amount`, tie-break by `bid_id`, truncation when fewer eligible bids than slots.
- **Click charging** — a click adds `bid_amount` to `spend`, and `spend` accumulates across multiple clicks.
- **Attribution** — purchase within window attributes; purchase outside window does not; different product does not; **last-click wins** when multiple eligible clicks exist; one click can credit multiple purchases.
- **Isolation** — stats for advertiser A don't leak into advertiser B.
- **End-to-end** — full scenario combining bid → auction → impression → click → purchase → stats.

---

## How to run the tests

From inside this folder:

```bash
python tests.py
```

Output ends with `N/15 tests passed`. Exit code is `0` if all pass, non-zero otherwise.

You can re-run as many times as you want.

---

## Tips (optional)

- Plain dicts and lists are enough state — you don't need classes for `Bid` or `Event`.
- Read the spec end-to-end before coding. The auction tie-break rule and the attribution window's `<=` bounds are easy to get subtly wrong.
- If you're stuck on attribution, build the simple "click within window → conversion" case first and only then handle the "last-click among many" case.

Good luck!
