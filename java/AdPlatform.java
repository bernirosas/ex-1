import java.util.*;

public class AdPlatform {

    private final int attributionWindowSeconds;

    public AdPlatform(int attributionWindowSeconds) {
        this.attributionWindowSeconds = attributionWindowSeconds;
    }

    public AdPlatform() {
        this(3600);
    }

    public String placeBid(String advertiserId, String productId, String keyword, double bidAmount) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public List<Map<String, Object>> runAuction(String query, int numSlots) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void recordEvent(Map<String, Object> event) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Map<String, Object> getAdvertiserStats(String advertiserId) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
