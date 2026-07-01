package adplatform

import "errors"

type AdPlatform struct {
	AttributionWindowSeconds int
}

func NewAdPlatform(attributionWindowSeconds int) *AdPlatform {
	return &AdPlatform{AttributionWindowSeconds: attributionWindowSeconds}
}

func (p *AdPlatform) PlaceBid(advertiserID, productID, keyword string, bidAmount float64) (string, error) {
	return "", errors.New("not implemented")
}

func (p *AdPlatform) RunAuction(query string, numSlots int) ([]map[string]interface{}, error) {
	return nil, errors.New("not implemented")
}

func (p *AdPlatform) RecordEvent(event map[string]interface{}) error {
	return errors.New("not implemented")
}

func (p *AdPlatform) GetAdvertiserStats(advertiserID string) (map[string]interface{}, error) {
	return nil, errors.New("not implemented")
}
