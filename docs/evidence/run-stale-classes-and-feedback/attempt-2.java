package com.enrichmeai.ref.shipping;

import com.enrichmeai.ref.platform.Errors;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Generated from specs/001-shipping-quote/spec.md: the template is the framework's, the method body is the model's. */
@Service
public class ShippingQuoteService {

    public Quote quote(String zone, int weightKg) {
        // Convert zone to uppercase
            String upperZone = zone.toUpperCase();

            // Validate weight
            if (weightKg < 1 || weightKg > 30) {
                throw Errors.unprocessable("WEIGHT_LIMIT", "Weight must be between 1 and 30 kg");
            }

            // Validate zone
            if (!upperZone.equals("A") && !upperZone.equals("B") && !upperZone.equals("C")) {
                throw Errors.notFound("UNKNOWN_ZONE", "Unknown zone");
            }

            // Calculate price
            int base = 0;
            if (upperZone.equals("A")) {
                base = 500;
            } else if (upperZone.equals("B")) {
                base = 800;
            } else if (upperZone.equals("C")) {
                base = 1200;
            }

            int pricePence = base + weightKg * 100;

            return new Quote(upperZone, weightKg, pricePence);
    }
}
