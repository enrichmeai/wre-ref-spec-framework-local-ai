package com.enrichmeai.ref.shipping;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Written by people. Thin on purpose: the rules live in ShippingQuoteService, the slot the model fills. */
@RestController
class ShippingQuoteController {
    private final ShippingQuoteService service;

    ShippingQuoteController(ShippingQuoteService service) {
        this.service = service;
    }

    @GetMapping("/quotes")
    Quote quote(@RequestParam String zone, @RequestParam int weightKg) {
        return service.quote(zone, weightKg);
    }
}
