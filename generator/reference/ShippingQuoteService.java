package com.enrichmeai.ref.shipping;

import com.enrichmeai.ref.platform.Errors;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** A known-good fill of the slot, written by hand, in the template. It proves the acceptance tests can pass. */
@Service
public class ShippingQuoteService {

    public Quote quote(String zone, int weightKg) {
        String z = zone == null ? "" : zone.trim().toUpperCase(Locale.ROOT);
        int base = switch (z) {
            case "A" -> 500;
            case "B" -> 800;
            case "C" -> 1200;
            default -> throw Errors.notFound("UNKNOWN_ZONE", "No such zone: " + zone);
        };
        if (weightKg < 1) throw Errors.unprocessable("INVALID_WEIGHT", "Weight must be at least 1 kg, got " + weightKg);
        if (weightKg > 30) throw Errors.unprocessable("WEIGHT_LIMIT", "Weight limit is 30 kg, got " + weightKg);
        return new Quote(z, weightKg, base + 100 * weightKg);
    }
}
