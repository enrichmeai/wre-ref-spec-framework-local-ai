package com.enrichmeai.ref.shipping;

import com.enrichmeai.ref.platform.Errors;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Generated from specs/001-shipping-quote/spec.md: the template is the framework's, the method body is the model's. */
@Service
public class ShippingQuoteService {

    public Quote quote(String zone, int weightKg) {
        // Check zone
            if (!zone.matches("[A-C][A-Z]")) {
                throw Errors.notFound("UNKNOWN_ZONE", "Zone not A, B or C");
            }

            // Check weight
            if (weightKg < 1) {
                throw Errors.unprocessable("INVALID_WEIGHT", "Weight below 1 kg");
            }
            if (weightKg > 30) {
                throw Errors.unprocessable("WEIGHT_LIMIT", "Weight above 30 kg");
            }

            // Calculate price
            int base = switch (zone.toUpperCase()) {
                case "A" -> 500;
                case "B" -> 800;
                case "C" -> 1200;
                default -> 0;
            };

            int pricePence = base + weightKg * 100;

            return Quote.builder()
                    .zone(zone.toUpperCase())
                    .weightKg(weightKg)
                    .pricePence(pricePence)
                    .build();
    }
}
