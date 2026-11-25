package com.enrichmeai.ref.shipping;

import com.enrichmeai.ref.platform.DomainException;

public class ShippingQuoteService {
    public Quote quote(String zone, int weightKg) {
        // Validate zone
        if (!zone.matches("[A-Z]|[a-z]+")) {
            throw new DomainException(HttpStatus.NOT_FOUND, "UNKNOWN_ZONE", "Invalid zone");
        }

        // Validate weight
        if (weightKg < 1 || weightKg > 30) {
            throw new DomainException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_WEIGHT", "Weight must be between 1 and 30 kg");
        }

        // Convert zone to uppercase
        String uppercaseZone = zone.toUpperCase();

        // Calculate base price
        int basePrice = 0;
        switch (uppercaseZone) {
            case "A":
                basePrice = 500;
                break;
            case "B":
                basePrice = 800;
                break;
            case "C":
                basePrice = 1200;
                break;
            default:
                throw new DomainException(HttpStatus.NOT_FOUND, "UNKNOWN_ZONE", "Invalid zone");
        }

        // Calculate total price
        int totalPrice = basePrice + weightKg * 100;

        return new Quote(uppercaseZone, weightKg, totalPrice);
    }
}
