# 001 Shipping quote

Owner: the product manager. Approved by: product and the tech lead.

## Why
Checkout shows a delivery price before payment. Today it is a hard-coded table in the web app;
it needs to be one service that every channel calls.

## User story
As the checkout, I ask for a quote for a parcel's zone and weight, so the customer sees the price.

## Rules
- Price in pence = zone base + 100 per kg.
- Zone bases: A = 500, B = 800, C = 1200. Zones are case-insensitive; the reply names the zone in upper case.
- Weight is whole kilograms, 1 to 30 inclusive.
- Errors, checked in this order, each a different code:
  1. Zone not A, B or C: 404, code UNKNOWN_ZONE.
  2. Weight below 1 kg: 422, code INVALID_WEIGHT.
  3. Weight above 30 kg: 422, code WEIGHT_LIMIT.

## Acceptance criteria
- AC-1: GIVEN zone A and 2 kg WHEN checkout asks for a quote THEN the reply is 200 with pricePence 700.
- AC-2: GIVEN zone C and 30 kg (the limit) THEN the reply is 200 with pricePence 4200.
- AC-3: GIVEN zone "b" and 1 kg THEN the reply is 200 with zone "B" and pricePence 900.
- AC-4: GIVEN 31 kg THEN the reply is 422 with code WEIGHT_LIMIT.
- AC-5: GIVEN 0 kg THEN the reply is 422 with code INVALID_WEIGHT.
- AC-6: GIVEN zone "Z" THEN the reply is 404 with code UNKNOWN_ZONE.
- AC-7: GIVEN zone "Z" and 0 kg (two errors at once) THEN the reply is 404 with code UNKNOWN_ZONE: the zone is checked first.

## Built on the framework
- Errors come from the platform's `Errors` factory (`Errors.notFound`, `Errors.unprocessable`); the platform turns them into problem details with a `code`.
- No error handlers, no `System.out`, no database access in a controller (the platform rules check all three).

## Slot to fill
`ShippingQuoteService.java`: the rules above. Everything else in the service is template or written by people.
