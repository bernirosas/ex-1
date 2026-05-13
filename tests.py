"""
Validation tests for the Topsort take-home.

Run with:
    python tests.py

To grade a candidate's submission saved as candidate.py:
    SOLUTION_MODULE=candidate python tests.py
"""
from __future__ import annotations

import importlib
import os
import sys
import traceback


MODULE_NAME = os.environ.get("SOLUTION_MODULE", "solution")


def _load_platform():
    try:
        mod = importlib.import_module(MODULE_NAME)
    except Exception as e:
        print(f"FAILED to import module '{MODULE_NAME}': {e}")
        sys.exit(1)
    if not hasattr(mod, "AdPlatform"):
        print(f"FAILED: module '{MODULE_NAME}' has no class AdPlatform")
        sys.exit(1)
    return mod.AdPlatform


_results: list[tuple[str, bool, str]] = []


def test(name):
    def deco(fn):
        def wrapper():
            try:
                fn()
                _results.append((name, True, ""))
                print(f"  PASS  {name}")
            except AssertionError as e:
                _results.append((name, False, str(e) or repr(e)))
                print(f"  FAIL  {name}")
                print(f"        {e}")
            except Exception as e:
                _results.append((name, False, f"{type(e).__name__}: {e}"))
                print(f"  ERROR {name}")
                traceback.print_exc()
        return wrapper
    return deco


def assert_eq(actual, expected, msg=""):
    if actual != expected:
        raise AssertionError(
            f"{msg}\n  expected: {expected!r}\n  got:      {actual!r}"
        )


def assert_close(actual, expected, tol=1e-6, msg=""):
    if abs(actual - expected) > tol:
        raise AssertionError(
            f"{msg}\n  expected: {expected!r}\n  got:      {actual!r}"
        )


AdPlatform = _load_platform()


# ------------------------- Tests --------------------------------------- #
@test("place_bid returns sequential bid_ids")
def t_place_bid_ids():
    p = AdPlatform()
    assert_eq(p.place_bid("a", "p1", "shoes", 1.0), "bid_1")
    assert_eq(p.place_bid("b", "p2", "shoes", 1.0), "bid_2")
    assert_eq(p.place_bid("a", "p3", "hats", 0.5), "bid_3")


@test("auction with no bids returns empty list")
def t_auction_empty():
    p = AdPlatform()
    assert_eq(p.run_auction("shoes", 3), [])


@test("auction filters by keyword (case-insensitive)")
def t_auction_keyword():
    p = AdPlatform()
    p.place_bid("a", "p1", "Shoes", 1.0)
    p.place_bid("b", "p2", "hats", 1.0)
    out = p.run_auction("SHOES", 3)
    assert_eq(len(out), 1)
    assert_eq(out[0]["advertiser_id"], "a")


@test("auction ranks by bid_amount descending")
def t_auction_ranking():
    p = AdPlatform()
    p.place_bid("a", "p1", "shoes", 1.00)
    p.place_bid("b", "p2", "shoes", 2.00)
    p.place_bid("c", "p3", "shoes", 1.50)
    out = p.run_auction("shoes", 3)
    assert_eq([w["bid_id"] for w in out], ["bid_2", "bid_3", "bid_1"])
    assert_eq([w["slot"] for w in out], [0, 1, 2])


@test("auction ties broken by bid_id ascending")
def t_auction_tie_break():
    p = AdPlatform()
    p.place_bid("a", "p1", "shoes", 1.50)
    p.place_bid("b", "p2", "shoes", 1.50)
    out = p.run_auction("shoes", 1)
    assert_eq(len(out), 1)
    assert_eq(out[0]["bid_id"], "bid_1")


@test("auction returns fewer winners when fewer bids exist")
def t_auction_fewer_bids():
    p = AdPlatform()
    p.place_bid("a", "p1", "shoes", 1.50)
    out = p.run_auction("shoes", 5)
    assert_eq(len(out), 1)


@test("click charges spend at the bid_amount")
def t_click_charges_bid_amount():
    p = AdPlatform()
    p.place_bid("a", "p1", "shoes", 1.50)
    p.record_event({"type": "click", "timestamp": 1_700_000_000,
                    "user_id": "u1", "advertiser_id": "a", "product_id": "p1"})
    s = p.get_advertiser_stats("a")
    assert_eq(s["clicks"], 1)
    assert_close(s["spend"], 1.50)


@test("spend accumulates across clicks")
def t_spend_accumulates():
    p = AdPlatform()
    p.place_bid("a", "p1", "shoes", 0.75)
    for ts in (1_700_000_000, 1_700_000_100, 1_700_000_200):
        p.record_event({"type": "click", "timestamp": ts,
                        "user_id": "u1", "advertiser_id": "a", "product_id": "p1"})
    s = p.get_advertiser_stats("a")
    assert_eq(s["clicks"], 3)
    assert_close(s["spend"], 2.25)


@test("attribution: simple last-click within window")
def t_attribution_basic():
    p = AdPlatform(attribution_window_seconds=3600)
    p.place_bid("a", "p1", "shoes", 1.50)
    p.record_event({"type": "impression", "timestamp": 1_700_000_000,
                    "user_id": "u1", "advertiser_id": "a", "product_id": "p1"})
    p.record_event({"type": "click", "timestamp": 1_700_000_010,
                    "user_id": "u1", "advertiser_id": "a", "product_id": "p1"})
    p.record_event({"type": "purchase", "timestamp": 1_700_000_500,
                    "user_id": "u1", "product_id": "p1", "amount": 49.99})
    s = p.get_advertiser_stats("a")
    assert_eq(s["impressions"], 1)
    assert_eq(s["clicks"], 1)
    assert_close(s["spend"], 1.50)
    assert_eq(s["attributed_conversions"], 1)
    assert_close(s["attributed_revenue"], 49.99)


@test("attribution: purchase outside window is unattributed")
def t_attribution_outside_window():
    p = AdPlatform(attribution_window_seconds=60)
    p.place_bid("a", "p1", "shoes", 1.50)
    p.record_event({"type": "click", "timestamp": 1_700_000_000,
                    "user_id": "u1", "advertiser_id": "a", "product_id": "p1"})
    p.record_event({"type": "purchase", "timestamp": 1_700_000_500,
                    "user_id": "u1", "product_id": "p1", "amount": 49.99})
    s = p.get_advertiser_stats("a")
    assert_eq(s["attributed_conversions"], 0)
    assert_close(s["attributed_revenue"], 0.0)
    # Click still charged spend, even though purchase didn't attribute.
    assert_close(s["spend"], 1.50)


@test("attribution: different product is unattributed")
def t_attribution_different_product():
    p = AdPlatform(attribution_window_seconds=3600)
    p.place_bid("a", "p1", "shoes", 1.50)
    p.record_event({"type": "click", "timestamp": 1_700_000_000,
                    "user_id": "u1", "advertiser_id": "a", "product_id": "p1"})
    p.record_event({"type": "purchase", "timestamp": 1_700_000_300,
                    "user_id": "u1", "product_id": "p2", "amount": 49.99})
    s = p.get_advertiser_stats("a")
    assert_eq(s["attributed_conversions"], 0)


@test("attribution: last-click wins among multiple clicks")
def t_attribution_last_click():
    p = AdPlatform(attribution_window_seconds=3600)
    p.place_bid("a", "p1", "shoes", 1.00)
    p.place_bid("b", "p1", "shoes", 0.50)
    p.record_event({"type": "click", "timestamp": 1_700_000_000,
                    "user_id": "u1", "advertiser_id": "a", "product_id": "p1"})
    p.record_event({"type": "click", "timestamp": 1_700_000_100,
                    "user_id": "u1", "advertiser_id": "b", "product_id": "p1"})
    p.record_event({"type": "purchase", "timestamp": 1_700_000_200,
                    "user_id": "u1", "product_id": "p1", "amount": 80.00})
    sa = p.get_advertiser_stats("a")
    sb = p.get_advertiser_stats("b")
    assert_eq(sa["attributed_conversions"], 0)
    assert_eq(sb["attributed_conversions"], 1)
    assert_close(sb["attributed_revenue"], 80.00)


@test("attribution: one click can credit multiple purchases")
def t_attribution_two_purchases_one_click():
    p = AdPlatform(attribution_window_seconds=3600)
    p.place_bid("a", "p1", "shoes", 1.00)
    p.record_event({"type": "click", "timestamp": 1_700_000_000,
                    "user_id": "u1", "advertiser_id": "a", "product_id": "p1"})
    p.record_event({"type": "purchase", "timestamp": 1_700_000_100,
                    "user_id": "u1", "product_id": "p1", "amount": 30.00})
    p.record_event({"type": "purchase", "timestamp": 1_700_000_200,
                    "user_id": "u1", "product_id": "p1", "amount": 20.00})
    s = p.get_advertiser_stats("a")
    assert_eq(s["attributed_conversions"], 2)
    assert_close(s["attributed_revenue"], 50.00)


@test("multi-advertiser stats are isolated")
def t_multi_advertiser_stats():
    p = AdPlatform(attribution_window_seconds=3600)
    p.place_bid("a", "p1", "shoes", 1.50)
    p.place_bid("b", "p2", "shoes", 1.20)

    p.record_event({"type": "impression", "timestamp": 1_700_000_000,
                    "user_id": "u1", "advertiser_id": "a", "product_id": "p1"})
    p.record_event({"type": "impression", "timestamp": 1_700_000_001,
                    "user_id": "u1", "advertiser_id": "b", "product_id": "p2"})
    p.record_event({"type": "click", "timestamp": 1_700_000_010,
                    "user_id": "u1", "advertiser_id": "a", "product_id": "p1"})
    p.record_event({"type": "purchase", "timestamp": 1_700_000_100,
                    "user_id": "u1", "product_id": "p1", "amount": 50.00})

    sa = p.get_advertiser_stats("a")
    sb = p.get_advertiser_stats("b")

    assert_eq(sa["impressions"], 1)
    assert_eq(sa["clicks"], 1)
    assert_close(sa["spend"], 1.50)
    assert_eq(sa["attributed_conversions"], 1)
    assert_close(sa["attributed_revenue"], 50.00)

    assert_eq(sb["impressions"], 1)
    assert_eq(sb["clicks"], 0)
    assert_close(sb["spend"], 0.0)
    assert_eq(sb["attributed_conversions"], 0)
    assert_close(sb["attributed_revenue"], 0.0)


@test("end-to-end scenario from PROBLEM.md example")
def t_end_to_end_example():
    p = AdPlatform(attribution_window_seconds=3600)
    p.place_bid("adv_a", "prod_x", "shoes", 1.50)
    p.place_bid("adv_b", "prod_y", "shoes", 1.20)

    winners = p.run_auction("shoes", 1)
    assert_eq(len(winners), 1)
    assert_eq(winners[0]["bid_id"], "bid_1")
    assert_eq(winners[0]["advertiser_id"], "adv_a")

    p.record_event({"type": "impression", "timestamp": 1_700_000_000,
                    "user_id": "u1", "advertiser_id": "adv_a", "product_id": "prod_x"})
    p.record_event({"type": "click", "timestamp": 1_700_000_010,
                    "user_id": "u1", "advertiser_id": "adv_a", "product_id": "prod_x"})
    p.record_event({"type": "purchase", "timestamp": 1_700_000_300,
                    "user_id": "u1", "product_id": "prod_x", "amount": 49.99})

    s = p.get_advertiser_stats("adv_a")
    assert_eq(s, {
        "impressions": 1,
        "clicks": 1,
        "spend": 1.50,
        "attributed_conversions": 1,
        "attributed_revenue": 49.99,
    })


# ------------------------- Runner -------------------------------------- #
ALL_TESTS = [
    t_place_bid_ids,
    t_auction_empty,
    t_auction_keyword,
    t_auction_ranking,
    t_auction_tie_break,
    t_auction_fewer_bids,
    t_click_charges_bid_amount,
    t_spend_accumulates,
    t_attribution_basic,
    t_attribution_outside_window,
    t_attribution_different_product,
    t_attribution_last_click,
    t_attribution_two_purchases_one_click,
    t_multi_advertiser_stats,
    t_end_to_end_example,
]


def main():
    print(f"Running tests against module: {MODULE_NAME}\n")
    for t in ALL_TESTS:
        t()
    passed = sum(1 for _, ok, _ in _results if ok)
    total = len(_results)
    print(f"\n{passed}/{total} tests passed")
    sys.exit(0 if passed == total else 1)


if __name__ == "__main__":
    main()
