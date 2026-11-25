package com.enrichmeai.ref.shipping;

import com.enrichmeai.ref.platform.Errors;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Generated from specs/001-shipping-quote/spec.md: the template is the framework's, the method body is the model's. */
@Service
public class ShippingQuoteService {

    public Quote quote(String zone, int weightKg) {
        // Validate zone
            if (!zone.toUpperCase().equals("A") && !zone.toUpperCase().equals("B") && !zone.toUpperCase().equals("C")) {
                throw Errors.notFound("UNKNOWN_ZONE", "Zone not found");
            }

            // Validate weight
            if (weightKg < 1) {
                throw Errors.unprocessable("INVALID_WEIGHT", "Weight must be at least 1 kg");
            }
            if (weightKg > 30) {
                throw Errors.unprocessable("WEIGHT_LIMIT", "Weight must be at most 30 kg");
            }

            // Calculate price
            int base = 0;
            if ("A".equals(zone.toUpperCase())) {
                base = 500;
            } else if ("B".equals(zone.toUpperCase())) {
                base = 800;
            } else {
                base = 1200;
            }

            int pricePence = base + weightKg * 100;
            return Quote.builder().zone(zone).weightKg(weightKg).pricePence(pricePence).build();
    }
}
