class AdPlatform {
  constructor(attributionWindowSeconds = 3600) {
    this.attributionWindowSeconds = attributionWindowSeconds;
  }

  placeBid(advertiserId, productId, keyword, bidAmount) {
    throw new Error("Not implemented");
  }

  runAuction(query, numSlots) {
    throw new Error("Not implemented");
  }

  recordEvent(event) {
    throw new Error("Not implemented");
  }

  getAdvertiserStats(advertiserId) {
    throw new Error("Not implemented");
  }
}

module.exports = { AdPlatform };
