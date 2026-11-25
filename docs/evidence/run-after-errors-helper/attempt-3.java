package com.enrichmeai.ref.shipping;

import com.enrichmeai.ref.platform.Errors;
import com.enrichmeai.ref.shipping.model.Quote;

public class ShippingQuoteService {
    public Quote quote(String zone, int weightKg) {
        // Validate zone
        if (!zone.matches("[A-Z]+")) {
            throw Errors.unknownZone("UNKNOWN_ZONE", "Invalid zone");
        }

        // Validate weight
        if (weightKg < 1 || weightKg > 30) {
            throw Errors.unprocessable("WEIGHT_LIMIT", "Weight out of range");
        }

        // Calculate price
        int zoneBase = 0;
        switch (zone.toUpperCase()) {
            case "A":
                zoneBase = 500;
                break;
            case "B":
                zoneBase = 800;
                break;
            case "C":
                zoneBase = 1200;
                break;
            default:
                throw Errors.unknownZone("UNKNOWN_ZONE", "Invalid zone");
        }

        int pricePence = zoneBase + weightKg * 100;
        return new Quote(zone.toUpperCase(), weightKg, pricePence);
    }
}
