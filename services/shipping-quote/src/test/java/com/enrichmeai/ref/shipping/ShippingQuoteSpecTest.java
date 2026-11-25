package com.enrichmeai.ref.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/** Written by people from specs/001-shipping-quote/spec.md: one test per acceptance criterion, tagged with its id. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ShippingQuoteSpecTest {

    @Autowired Environment env;
    final HttpClient http = HttpClient.newHttpClient();

    HttpResponse<String> quote(String zone, int kg) throws Exception {
        var uri = URI.create("http://localhost:" + env.getProperty("local.server.port") + "/quotes?zone=" + zone + "&weightKg=" + kg);
        return http.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test @Tag("AC-1")
    void zoneATwoKilos() throws Exception {
        var r = quote("A", 2);
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("\"pricePence\":700");
    }

    @Test @Tag("AC-2")
    void zoneCAtTheWeightLimit() throws Exception {
        var r = quote("C", 30);
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("\"pricePence\":4200");
    }

    @Test @Tag("AC-3")
    void zonesAreCaseInsensitive() throws Exception {
        var r = quote("b", 1);
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("\"zone\":\"B\"").contains("\"pricePence\":900");
    }

    @Test @Tag("AC-4")
    void overTheWeightLimitIs422() throws Exception {
        var r = quote("A", 31);
        assertThat(r.statusCode()).isEqualTo(422);
        assertThat(r.body()).contains("WEIGHT_LIMIT");
    }

    @Test @Tag("AC-5")
    void zeroKilosIs422() throws Exception {
        var r = quote("A", 0);
        assertThat(r.statusCode()).isEqualTo(422);
        assertThat(r.body()).contains("INVALID_WEIGHT");
    }

    @Test @Tag("AC-7")
    void theZoneIsCheckedBeforeTheWeight() throws Exception {
        var r = quote("Z", 0);
        assertThat(r.statusCode()).isEqualTo(404);
        assertThat(r.body()).contains("UNKNOWN_ZONE");
    }

    @Test @Tag("AC-6")
    void unknownZoneIs404() throws Exception {
        var r = quote("Z", 1);
        assertThat(r.statusCode()).isEqualTo(404);
        assertThat(r.body()).contains("UNKNOWN_ZONE");
    }
}
