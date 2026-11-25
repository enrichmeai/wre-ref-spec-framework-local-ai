package com.enrichmeai.ref.shipping;

import com.enrichmeai.ref.platform.Errors;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Generated from specs/001-shipping-quote/spec.md: the template is the framework's, the method body is the model's. */
@Service
public class ShippingQuoteService {

    public Quote quote(String zone, int weightKg) {
        // Check zone
            if (!zone.matches("[A-C]|\\u0020[A-C]") && !zone.matches("[a-b]|\\u0020[a-b]")) {
                throw Errors.notFound("UNKNOWN_ZONE", "Unknown zone");
            }

            // Check weight
            if (weightKg < 1) {
                throw Errors.unprocessable("INVALID_WEIGHT", "Weight must be at least 1 kg");
            }
            if (weightKg > 30) {
                throw Errors.unprocessable("WEIGHT_LIMIT", "Weight must be at most 30 kg");
            }

            // Calculate price
            int base = 0;
            switch (zone.toUpperCase()) {
                case "A":
                    base = 500;
                    break;
                case "B":
                    base = 800;
                    break;
                case "C":
                    base = 1200;
                    break;
            }

            return new Quote(zone.toUpperCase(), weightKg, base + weightKg * 100);
    }
}
