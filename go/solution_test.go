package adplatform

// Run: go test
// Run with verbose output: go test -v

import (
	"math"
	"testing"
)

func close64(a, b float64) bool {
	return math.Abs(a-b) < 1e-6
}

func toI(v interface{}) int {
	switch val := v.(type) {
	case int:
		return val
	case float64:
		return int(val)
	}
	panic("not a number")
}

func toF(v interface{}) float64 {
	switch val := v.(type) {
	case float64:
		return val
	case int:
		return float64(val)
	}
	panic("not a float")
}

func ev(kvs ...interface{}) map[string]interface{} {
	m := make(map[string]interface{})
	for i := 0; i < len(kvs); i += 2 {
		m[kvs[i].(string)] = kvs[i+1]
	}
	return m
}

func TestPlaceBidSequential(t *testing.T) {
	p := NewAdPlatform(3600)
	if id, _ := p.PlaceBid("a", "p1", "shoes", 1.0); id != "bid_1" {
		t.Fatalf("expected bid_1, got %s", id)
	}
	if id, _ := p.PlaceBid("b", "p2", "shoes", 1.0); id != "bid_2" {
		t.Fatalf("expected bid_2, got %s", id)
	}
	if id, _ := p.PlaceBid("a", "p3", "hats", 0.5); id != "bid_3" {
		t.Fatalf("expected bid_3, got %s", id)
	}
}

func TestAuctionEmpty(t *testing.T) {
	p := NewAdPlatform(3600)
	out, _ := p.RunAuction("shoes", 3)
	if len(out) != 0 {
		t.Fatalf("expected empty, got %v", out)
	}
}

func TestAuctionKeywordCaseInsensitive(t *testing.T) {
	p := NewAdPlatform(3600)
	p.PlaceBid("a", "p1", "Shoes", 1.0)
	p.PlaceBid("b", "p2", "hats", 1.0)
	out, _ := p.RunAuction("SHOES", 3)
	if len(out) != 1 {
		t.Fatalf("expected 1 winner, got %d", len(out))
	}
	if out[0]["advertiser_id"] != "a" {
		t.Fatalf("wrong advertiser: %v", out[0])
	}
}

func TestAuctionRanking(t *testing.T) {
	p := NewAdPlatform(3600)
	p.PlaceBid("a", "p1", "shoes", 1.0)
	p.PlaceBid("b", "p2", "shoes", 2.0)
	p.PlaceBid("c", "p3", "shoes", 1.5)
	out, _ := p.RunAuction("shoes", 3)
	expected := []string{"bid_2", "bid_3", "bid_1"}
	for i, want := range expected {
		if out[i]["bid_id"] != want {
			t.Fatalf("slot %d: expected %s got %v", i, want, out[i]["bid_id"])
		}
		if toI(out[i]["slot"]) != i {
			t.Fatalf("slot field wrong at index %d: %v", i, out[i]["slot"])
		}
	}
}

func TestAuctionTieBrk(t *testing.T) {
	p := NewAdPlatform(3600)
	p.PlaceBid("a", "p1", "shoes", 1.5)
	p.PlaceBid("b", "p2", "shoes", 1.5)
	out, _ := p.RunAuction("shoes", 1)
	if len(out) != 1 || out[0]["bid_id"] != "bid_1" {
		t.Fatalf("expected bid_1 to win tie, got %v", out)
	}
}

func TestAuctionFewerBids(t *testing.T) {
	p := NewAdPlatform(3600)
	p.PlaceBid("a", "p1", "shoes", 1.5)
	out, _ := p.RunAuction("shoes", 5)
	if len(out) != 1 {
		t.Fatalf("expected 1 result, got %d", len(out))
	}
}

func TestClickChargesSpend(t *testing.T) {
	p := NewAdPlatform(3600)
	p.PlaceBid("a", "p1", "shoes", 1.5)
	p.RecordEvent(ev("type", "click", "timestamp", int64(1700000000),
		"user_id", "u1", "advertiser_id", "a", "product_id", "p1"))
	s, _ := p.GetAdvertiserStats("a")
	if toI(s["clicks"]) != 1 {
		t.Fatalf("expected 1 click, got %v", s["clicks"])
	}
	if !close64(toF(s["spend"]), 1.5) {
		t.Fatalf("expected spend 1.5, got %v", s["spend"])
	}
}

func TestSpendAccumulates(t *testing.T) {
	p := NewAdPlatform(3600)
	p.PlaceBid("a", "p1", "shoes", 0.75)
	for _, ts := range []int64{1700000000, 1700000100, 1700000200} {
		p.RecordEvent(ev("type", "click", "timestamp", ts,
			"user_id", "u1", "advertiser_id", "a", "product_id", "p1"))
	}
	s, _ := p.GetAdvertiserStats("a")
	if toI(s["clicks"]) != 3 {
		t.Fatalf("expected 3 clicks, got %v", s["clicks"])
	}
	if !close64(toF(s["spend"]), 2.25) {
		t.Fatalf("expected spend 2.25, got %v", s["spend"])
	}
}

func TestAttributionBasic(t *testing.T) {
	p := NewAdPlatform(3600)
	p.PlaceBid("a", "p1", "shoes", 1.5)
	p.RecordEvent(ev("type", "impression", "timestamp", int64(1700000000),
		"user_id", "u1", "advertiser_id", "a", "product_id", "p1"))
	p.RecordEvent(ev("type", "click", "timestamp", int64(1700000010),
		"user_id", "u1", "advertiser_id", "a", "product_id", "p1"))
	p.RecordEvent(ev("type", "purchase", "timestamp", int64(1700000500),
		"user_id", "u1", "product_id", "p1", "amount", 49.99))
	s, _ := p.GetAdvertiserStats("a")
	if toI(s["impressions"]) != 1 || toI(s["clicks"]) != 1 {
		t.Fatalf("wrong counts: %v", s)
	}
	if !close64(toF(s["spend"]), 1.5) {
		t.Fatalf("wrong spend: %v", s["spend"])
	}
	if toI(s["attributed_conversions"]) != 1 {
		t.Fatalf("expected 1 conversion, got %v", s["attributed_conversions"])
	}
	if !close64(toF(s["attributed_revenue"]), 49.99) {
		t.Fatalf("wrong revenue: %v", s["attributed_revenue"])
	}
}

func TestAttributionOutsideWindow(t *testing.T) {
	p := NewAdPlatform(60)
	p.PlaceBid("a", "p1", "shoes", 1.5)
	p.RecordEvent(ev("type", "click", "timestamp", int64(1700000000),
		"user_id", "u1", "advertiser_id", "a", "product_id", "p1"))
	p.RecordEvent(ev("type", "purchase", "timestamp", int64(1700000500),
		"user_id", "u1", "product_id", "p1", "amount", 49.99))
	s, _ := p.GetAdvertiserStats("a")
	if toI(s["attributed_conversions"]) != 0 {
		t.Fatal("should be unattributed")
	}
	if !close64(toF(s["spend"]), 1.5) {
		t.Fatalf("spend should still be 1.5, got %v", s["spend"])
	}
}

func TestAttributionDifferentProduct(t *testing.T) {
	p := NewAdPlatform(3600)
	p.PlaceBid("a", "p1", "shoes", 1.5)
	p.RecordEvent(ev("type", "click", "timestamp", int64(1700000000),
		"user_id", "u1", "advertiser_id", "a", "product_id", "p1"))
	p.RecordEvent(ev("type", "purchase", "timestamp", int64(1700000300),
		"user_id", "u1", "product_id", "p2", "amount", 49.99))
	s, _ := p.GetAdvertiserStats("a")
	if toI(s["attributed_conversions"]) != 0 {
		t.Fatal("different product should not attribute")
	}
}

func TestAttributionLastClick(t *testing.T) {
	p := NewAdPlatform(3600)
	p.PlaceBid("a", "p1", "shoes", 1.0)
	p.PlaceBid("b", "p1", "shoes", 0.5)
	p.RecordEvent(ev("type", "click", "timestamp", int64(1700000000),
		"user_id", "u1", "advertiser_id", "a", "product_id", "p1"))
	p.RecordEvent(ev("type", "click", "timestamp", int64(1700000100),
		"user_id", "u1", "advertiser_id", "b", "product_id", "p1"))
	p.RecordEvent(ev("type", "purchase", "timestamp", int64(1700000200),
		"user_id", "u1", "product_id", "p1", "amount", 80.0))
	sa, _ := p.GetAdvertiserStats("a")
	sb, _ := p.GetAdvertiserStats("b")
	if toI(sa["attributed_conversions"]) != 0 {
		t.Fatal("a should not get credit")
	}
	if toI(sb["attributed_conversions"]) != 1 {
		t.Fatal("b should get credit")
	}
	if !close64(toF(sb["attributed_revenue"]), 80.0) {
		t.Fatalf("wrong revenue: %v", sb["attributed_revenue"])
	}
}

func TestAttributionOneClickMultiplePurchases(t *testing.T) {
	p := NewAdPlatform(3600)
	p.PlaceBid("a", "p1", "shoes", 1.0)
	p.RecordEvent(ev("type", "click", "timestamp", int64(1700000000),
		"user_id", "u1", "advertiser_id", "a", "product_id", "p1"))
	p.RecordEvent(ev("type", "purchase", "timestamp", int64(1700000100),
		"user_id", "u1", "product_id", "p1", "amount", 30.0))
	p.RecordEvent(ev("type", "purchase", "timestamp", int64(1700000200),
		"user_id", "u1", "product_id", "p1", "amount", 20.0))
	s, _ := p.GetAdvertiserStats("a")
	if toI(s["attributed_conversions"]) != 2 {
		t.Fatalf("expected 2 conversions, got %v", s["attributed_conversions"])
	}
	if !close64(toF(s["attributed_revenue"]), 50.0) {
		t.Fatalf("wrong revenue: %v", s["attributed_revenue"])
	}
}

func TestMultiAdvertiserIsolation(t *testing.T) {
	p := NewAdPlatform(3600)
	p.PlaceBid("a", "p1", "shoes", 1.5)
	p.PlaceBid("b", "p2", "shoes", 1.2)
	p.RecordEvent(ev("type", "impression", "timestamp", int64(1700000000),
		"user_id", "u1", "advertiser_id", "a", "product_id", "p1"))
	p.RecordEvent(ev("type", "impression", "timestamp", int64(1700000001),
		"user_id", "u1", "advertiser_id", "b", "product_id", "p2"))
	p.RecordEvent(ev("type", "click", "timestamp", int64(1700000010),
		"user_id", "u1", "advertiser_id", "a", "product_id", "p1"))
	p.RecordEvent(ev("type", "purchase", "timestamp", int64(1700000100),
		"user_id", "u1", "product_id", "p1", "amount", 50.0))
	sa, _ := p.GetAdvertiserStats("a")
	sb, _ := p.GetAdvertiserStats("b")
	if toI(sa["impressions"]) != 1 || toI(sa["clicks"]) != 1 || toI(sa["attributed_conversions"]) != 1 {
		t.Fatalf("a stats wrong: %v", sa)
	}
	if !close64(toF(sa["spend"]), 1.5) || !close64(toF(sa["attributed_revenue"]), 50.0) {
		t.Fatalf("a money stats wrong: %v", sa)
	}
	if toI(sb["impressions"]) != 1 || toI(sb["clicks"]) != 0 || toI(sb["attributed_conversions"]) != 0 {
		t.Fatalf("b stats wrong: %v", sb)
	}
	if !close64(toF(sb["spend"]), 0.0) || !close64(toF(sb["attributed_revenue"]), 0.0) {
		t.Fatalf("b money stats wrong: %v", sb)
	}
}

func TestEndToEnd(t *testing.T) {
	p := NewAdPlatform(3600)
	p.PlaceBid("adv_a", "prod_x", "shoes", 1.5)
	p.PlaceBid("adv_b", "prod_y", "shoes", 1.2)
	winners, _ := p.RunAuction("shoes", 1)
	if len(winners) != 1 || winners[0]["bid_id"] != "bid_1" || winners[0]["advertiser_id"] != "adv_a" {
		t.Fatalf("wrong auction result: %v", winners)
	}
	p.RecordEvent(ev("type", "impression", "timestamp", int64(1700000000),
		"user_id", "u1", "advertiser_id", "adv_a", "product_id", "prod_x"))
	p.RecordEvent(ev("type", "click", "timestamp", int64(1700000010),
		"user_id", "u1", "advertiser_id", "adv_a", "product_id", "prod_x"))
	p.RecordEvent(ev("type", "purchase", "timestamp", int64(1700000300),
		"user_id", "u1", "product_id", "prod_x", "amount", 49.99))
	s, _ := p.GetAdvertiserStats("adv_a")
	if toI(s["impressions"]) != 1 || toI(s["clicks"]) != 1 || toI(s["attributed_conversions"]) != 1 {
		t.Fatalf("wrong stats: %v", s)
	}
	if !close64(toF(s["spend"]), 1.5) || !close64(toF(s["attributed_revenue"]), 49.99) {
		t.Fatalf("wrong money stats: %v", s)
	}
}
