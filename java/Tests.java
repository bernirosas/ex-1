import java.util.*;

/**
 * Validation tests for the Topsort take-home.
 *
 * Compile and run:
 *   javac *.java && java Tests
 *
 * To grade a candidate's file saved as Candidate.java:
 *   javac *.java && java Tests          (uses AdPlatform by default)
 *   -- or rename Candidate.java to AdPlatform.java first --
 */
public class Tests {

    static int passed = 0;
    static int total = 0;

    // Change this to the class you want to test.
    // Swap in "Solution" here to run the reference solution.
    static final String TARGET = "AdPlatform";

    interface PlatformFactory {
        Object create(int windowSecs);
        Object create();
    }

    // ------------------------------------------------------------------ //
    // Helpers to call methods reflectively so we can test either class.   //
    // ------------------------------------------------------------------ //

    static Object newPlatform(int windowSecs) {
        try {
            Class<?> cls = Class.forName(TARGET);
            return cls.getConstructor(int.class).newInstance(windowSecs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static Object newPlatform() {
        try {
            Class<?> cls = Class.forName(TARGET);
            return cls.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static String placeBid(Object p, String adv, String prod, String kw, double amt) {
        try {
            return (String) p.getClass()
                    .getMethod("placeBid", String.class, String.class, String.class, double.class)
                    .invoke(p, adv, prod, kw, amt);
        } catch (Exception e) {
            throw new RuntimeException(e.getCause() != null ? e.getCause() : e);
        }
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> runAuction(Object p, String query, int slots) {
        try {
            return (List<Map<String, Object>>) p.getClass()
                    .getMethod("runAuction", String.class, int.class)
                    .invoke(p, query, slots);
        } catch (Exception e) {
            throw new RuntimeException(e.getCause() != null ? e.getCause() : e);
        }
    }

    @SuppressWarnings("unchecked")
    static void recordEvent(Object p, Map<String, Object> event) {
        try {
            p.getClass().getMethod("recordEvent", Map.class).invoke(p, event);
        } catch (Exception e) {
            throw new RuntimeException(e.getCause() != null ? e.getCause() : e);
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> getStats(Object p, String advertiserId) {
        try {
            return (Map<String, Object>) p.getClass()
                    .getMethod("getAdvertiserStats", String.class)
                    .invoke(p, advertiserId);
        } catch (Exception e) {
            throw new RuntimeException(e.getCause() != null ? e.getCause() : e);
        }
    }

    static Map<String, Object> event(Object... kvs) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kvs.length; i += 2) m.put((String) kvs[i], kvs[i + 1]);
        return m;
    }

    static void test(String name, Runnable fn) {
        total++;
        try {
            fn.run();
            passed++;
            System.out.println("  PASS  " + name);
        } catch (AssertionError | RuntimeException e) {
            System.out.println("  FAIL  " + name);
            System.out.println("        " + e.getMessage());
        }
    }

    static void eq(Object actual, Object expected) {
        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("expected: " + expected + "\n  got:      " + actual);
        }
    }

    static void close(double actual, double expected) {
        if (Math.abs(actual - expected) > 1e-6) {
            throw new AssertionError("expected: " + expected + "\n  got:      " + actual);
        }
    }

    static double d(Object o) {
        return ((Number) o).doubleValue();
    }

    static int i(Object o) {
        return ((Number) o).intValue();
    }

    // ------------------------------------------------------------------ //
    // Tests                                                                //
    // ------------------------------------------------------------------ //

    public static void main(String[] args) {
        System.out.println("Running tests against class: " + TARGET + "\n");

        test("placeBid returns sequential bid_ids", () -> {
            Object p = newPlatform();
            eq(placeBid(p, "a", "p1", "shoes", 1.0), "bid_1");
            eq(placeBid(p, "b", "p2", "shoes", 1.0), "bid_2");
            eq(placeBid(p, "a", "p3", "hats", 0.5), "bid_3");
        });

        test("auction with no bids returns empty list", () -> {
            Object p = newPlatform();
            eq(runAuction(p, "shoes", 3).size(), 0);
        });

        test("auction filters by keyword (case-insensitive)", () -> {
            Object p = newPlatform();
            placeBid(p, "a", "p1", "Shoes", 1.0);
            placeBid(p, "b", "p2", "hats", 1.0);
            List<Map<String, Object>> out = runAuction(p, "SHOES", 3);
            eq(out.size(), 1);
            eq(out.get(0).get("advertiser_id"), "a");
        });

        test("auction ranks by bid_amount descending", () -> {
            Object p = newPlatform();
            placeBid(p, "a", "p1", "shoes", 1.0);
            placeBid(p, "b", "p2", "shoes", 2.0);
            placeBid(p, "c", "p3", "shoes", 1.5);
            List<Map<String, Object>> out = runAuction(p, "shoes", 3);
            eq(out.get(0).get("bid_id"), "bid_2");
            eq(out.get(1).get("bid_id"), "bid_3");
            eq(out.get(2).get("bid_id"), "bid_1");
            eq(i(out.get(0).get("slot")), 0);
            eq(i(out.get(1).get("slot")), 1);
            eq(i(out.get(2).get("slot")), 2);
        });

        test("auction ties broken by bid_id ascending", () -> {
            Object p = newPlatform();
            placeBid(p, "a", "p1", "shoes", 1.5);
            placeBid(p, "b", "p2", "shoes", 1.5);
            List<Map<String, Object>> out = runAuction(p, "shoes", 1);
            eq(out.size(), 1);
            eq(out.get(0).get("bid_id"), "bid_1");
        });

        test("auction returns fewer winners when fewer bids exist", () -> {
            Object p = newPlatform();
            placeBid(p, "a", "p1", "shoes", 1.5);
            List<Map<String, Object>> out = runAuction(p, "shoes", 5);
            eq(out.size(), 1);
        });

        test("click charges spend at the bid_amount", () -> {
            Object p = newPlatform();
            placeBid(p, "a", "p1", "shoes", 1.5);
            recordEvent(p, event("type", "click", "timestamp", 1700000000L,
                    "user_id", "u1", "advertiser_id", "a", "product_id", "p1"));
            Map<String, Object> s = getStats(p, "a");
            eq(i(s.get("clicks")), 1);
            close(d(s.get("spend")), 1.5);
        });

        test("spend accumulates across clicks", () -> {
            Object p = newPlatform();
            placeBid(p, "a", "p1", "shoes", 0.75);
            for (long ts : new long[]{1700000000L, 1700000100L, 1700000200L}) {
                recordEvent(p, event("type", "click", "timestamp", ts,
                        "user_id", "u1", "advertiser_id", "a", "product_id", "p1"));
            }
            Map<String, Object> s = getStats(p, "a");
            eq(i(s.get("clicks")), 3);
            close(d(s.get("spend")), 2.25);
        });

        test("attribution: simple last-click within window", () -> {
            Object p = newPlatform(3600);
            placeBid(p, "a", "p1", "shoes", 1.5);
            recordEvent(p, event("type", "impression", "timestamp", 1700000000L,
                    "user_id", "u1", "advertiser_id", "a", "product_id", "p1"));
            recordEvent(p, event("type", "click", "timestamp", 1700000010L,
                    "user_id", "u1", "advertiser_id", "a", "product_id", "p1"));
            recordEvent(p, event("type", "purchase", "timestamp", 1700000500L,
                    "user_id", "u1", "product_id", "p1", "amount", 49.99));
            Map<String, Object> s = getStats(p, "a");
            eq(i(s.get("impressions")), 1);
            eq(i(s.get("clicks")), 1);
            close(d(s.get("spend")), 1.5);
            eq(i(s.get("attributed_conversions")), 1);
            close(d(s.get("attributed_revenue")), 49.99);
        });

        test("attribution: purchase outside window is unattributed", () -> {
            Object p = newPlatform(60);
            placeBid(p, "a", "p1", "shoes", 1.5);
            recordEvent(p, event("type", "click", "timestamp", 1700000000L,
                    "user_id", "u1", "advertiser_id", "a", "product_id", "p1"));
            recordEvent(p, event("type", "purchase", "timestamp", 1700000500L,
                    "user_id", "u1", "product_id", "p1", "amount", 49.99));
            Map<String, Object> s = getStats(p, "a");
            eq(i(s.get("attributed_conversions")), 0);
            close(d(s.get("attributed_revenue")), 0.0);
            close(d(s.get("spend")), 1.5);
        });

        test("attribution: different product is unattributed", () -> {
            Object p = newPlatform(3600);
            placeBid(p, "a", "p1", "shoes", 1.5);
            recordEvent(p, event("type", "click", "timestamp", 1700000000L,
                    "user_id", "u1", "advertiser_id", "a", "product_id", "p1"));
            recordEvent(p, event("type", "purchase", "timestamp", 1700000300L,
                    "user_id", "u1", "product_id", "p2", "amount", 49.99));
            Map<String, Object> s = getStats(p, "a");
            eq(i(s.get("attributed_conversions")), 0);
        });

        test("attribution: last-click wins among multiple clicks", () -> {
            Object p = newPlatform(3600);
            placeBid(p, "a", "p1", "shoes", 1.0);
            placeBid(p, "b", "p1", "shoes", 0.5);
            recordEvent(p, event("type", "click", "timestamp", 1700000000L,
                    "user_id", "u1", "advertiser_id", "a", "product_id", "p1"));
            recordEvent(p, event("type", "click", "timestamp", 1700000100L,
                    "user_id", "u1", "advertiser_id", "b", "product_id", "p1"));
            recordEvent(p, event("type", "purchase", "timestamp", 1700000200L,
                    "user_id", "u1", "product_id", "p1", "amount", 80.0));
            Map<String, Object> sa = getStats(p, "a");
            Map<String, Object> sb = getStats(p, "b");
            eq(i(sa.get("attributed_conversions")), 0);
            eq(i(sb.get("attributed_conversions")), 1);
            close(d(sb.get("attributed_revenue")), 80.0);
        });

        test("attribution: one click can credit multiple purchases", () -> {
            Object p = newPlatform(3600);
            placeBid(p, "a", "p1", "shoes", 1.0);
            recordEvent(p, event("type", "click", "timestamp", 1700000000L,
                    "user_id", "u1", "advertiser_id", "a", "product_id", "p1"));
            recordEvent(p, event("type", "purchase", "timestamp", 1700000100L,
                    "user_id", "u1", "product_id", "p1", "amount", 30.0));
            recordEvent(p, event("type", "purchase", "timestamp", 1700000200L,
                    "user_id", "u1", "product_id", "p1", "amount", 20.0));
            Map<String, Object> s = getStats(p, "a");
            eq(i(s.get("attributed_conversions")), 2);
            close(d(s.get("attributed_revenue")), 50.0);
        });

        test("multi-advertiser stats are isolated", () -> {
            Object p = newPlatform(3600);
            placeBid(p, "a", "p1", "shoes", 1.5);
            placeBid(p, "b", "p2", "shoes", 1.2);
            recordEvent(p, event("type", "impression", "timestamp", 1700000000L,
                    "user_id", "u1", "advertiser_id", "a", "product_id", "p1"));
            recordEvent(p, event("type", "impression", "timestamp", 1700000001L,
                    "user_id", "u1", "advertiser_id", "b", "product_id", "p2"));
            recordEvent(p, event("type", "click", "timestamp", 1700000010L,
                    "user_id", "u1", "advertiser_id", "a", "product_id", "p1"));
            recordEvent(p, event("type", "purchase", "timestamp", 1700000100L,
                    "user_id", "u1", "product_id", "p1", "amount", 50.0));
            Map<String, Object> sa = getStats(p, "a");
            Map<String, Object> sb = getStats(p, "b");
            eq(i(sa.get("impressions")), 1); eq(i(sa.get("clicks")), 1);
            close(d(sa.get("spend")), 1.5); eq(i(sa.get("attributed_conversions")), 1);
            close(d(sa.get("attributed_revenue")), 50.0);
            eq(i(sb.get("impressions")), 1); eq(i(sb.get("clicks")), 0);
            close(d(sb.get("spend")), 0.0); eq(i(sb.get("attributed_conversions")), 0);
            close(d(sb.get("attributed_revenue")), 0.0);
        });

        test("end-to-end scenario from PROBLEM.md example", () -> {
            Object p = newPlatform(3600);
            placeBid(p, "adv_a", "prod_x", "shoes", 1.5);
            placeBid(p, "adv_b", "prod_y", "shoes", 1.2);
            List<Map<String, Object>> winners = runAuction(p, "shoes", 1);
            eq(winners.size(), 1);
            eq(winners.get(0).get("bid_id"), "bid_1");
            eq(winners.get(0).get("advertiser_id"), "adv_a");
            recordEvent(p, event("type", "impression", "timestamp", 1700000000L,
                    "user_id", "u1", "advertiser_id", "adv_a", "product_id", "prod_x"));
            recordEvent(p, event("type", "click", "timestamp", 1700000010L,
                    "user_id", "u1", "advertiser_id", "adv_a", "product_id", "prod_x"));
            recordEvent(p, event("type", "purchase", "timestamp", 1700000300L,
                    "user_id", "u1", "product_id", "prod_x", "amount", 49.99));
            Map<String, Object> s = getStats(p, "adv_a");
            eq(i(s.get("impressions")), 1);
            eq(i(s.get("clicks")), 1);
            close(d(s.get("spend")), 1.5);
            eq(i(s.get("attributed_conversions")), 1);
            close(d(s.get("attributed_revenue")), 49.99);
        });

        // ------------------------------------------------------------------ //
        System.out.println("\n" + passed + "/" + total + " tests passed");
        System.exit(passed == total ? 0 : 1);
    }
}
