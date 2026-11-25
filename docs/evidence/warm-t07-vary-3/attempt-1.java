package com.enrichmeai.ref.shipping;

import com.enrichmeai.ref.platform.Errors;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Generated from specs/001-shipping-quote/spec.md: the template is the framework's, the method body is the model's. */
@Service
public class ShippingQuoteService {

    public Quote quote(String zone, int weightKg) {
        if (!"A".equalsIgnoreCase(zone) && !"B".equalsIgnoreCase(zone) && !"C".equalsIgnoreCase(zone)) {
            throw Errors.notFound("UNKNOWN_ZONE", "Zone must be A, B, or C");
        }
        if (weightKg < 1) {
            throw Errors.unprocessable("INVALID_WEIGHT", "Weight must be at least 1 kg");
        }
        if (weightKg > 30) {
            throw Errors.unprocessable("WEIGHT_LIMIT", "Weight cannot exceed 30 kg");
        }
        int base = switch (zone.toUpperCase()) {
            case "A" -> 500;
            case "B" -> 800;
            case "C" -> 1200;
            default -> throw Errors.notFound("UNKNOWN_ZONE", "Zone must be A, B, or C");
        };
        int pricePence = base + weightKg * 100;
        return new Quote(zone.toUpperCase(), weightKg, pricePence);
    }
}
