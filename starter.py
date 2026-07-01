from __future__ import annotations


class AdPlatform:
    def __init__(self, attribution_window_seconds: int = 3600):
        self.attribution_window_seconds = attribution_window_seconds

    def place_bid(
        self,
        advertiser_id: str,
        product_id: str,
        keyword: str,
        bid_amount: float,
    ) -> str:
        raise NotImplementedError

    def run_auction(self, query: str, num_slots: int) -> list[dict]:
        raise NotImplementedError

    def record_event(self, event: dict) -> None:
        raise NotImplementedError

    def get_advertiser_stats(self, advertiser_id: str) -> dict:
        raise NotImplementedError
