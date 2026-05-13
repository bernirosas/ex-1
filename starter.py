
class AdPlatform:
    def __init__(self, attribution_window_seconds: int = 3600):
        """
        Initialize the platform.

        attribution_window_seconds: max gap (in seconds) between a click and a
        later purchase for the click to be considered a conversion.
        """
        self.attribution_window_seconds = attribution_window_seconds


    def place_bid(
        self,
        advertiser_id: str,  # who is bidding
        product_id: str,     # which of their products is being promoted
        keyword: str,        # search keyword this bid targets (case-insensitive)
        bid_amount: float,   # higher wins the auction
    ) -> str:
        """
        Register a manual bid. Returns a unique bid_id of the form 'bid_{n}',
        starting at 'bid_1' and incrementing by 1 with each call.
        """
        # TODO
        raise NotImplementedError

    def run_auction(self, query: str, num_slots: int) -> list[dict]:
        """
        Return up to num_slots winners ranked by bid_amount desc, tiebreak
        by bid_id asc. Each item: {slot, bid_id, advertiser_id, product_id}.
        """
        # TODO
        raise NotImplementedError

    def record_event(self, event: dict) -> None:
        """
        Record an impression, click, or purchase.

        Click events charge the matching bid: spend += bid_amount of the bid
        with the same (advertiser_id, product_id). Purchase events trigger
        last-click attribution within attribution_window_seconds.
        """
        # TODO
        raise NotImplementedError

    def get_advertiser_stats(self, advertiser_id: str) -> dict:
        """
        Return {impressions, clicks, spend, attributed_conversions, attributed_revenue}.

        `spend` is the sum of bid_amount across all of this advertiser's clicks
        (each click is charged the bid_amount of its matching bid).
        """
        # TODO
        raise NotImplementedError
