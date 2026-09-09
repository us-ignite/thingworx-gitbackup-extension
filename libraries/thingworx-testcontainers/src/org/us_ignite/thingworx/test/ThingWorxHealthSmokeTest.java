package org.us_ignite.thingworx.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpClient;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.us_ignite.thingworx.testcontainers.TestingCredentials;
import org.us_ignite.thingworx.testcontainers.ThingWorxTestStack;

class ThingWorxHealthSmokeTest {
    @Test
    void thingworxBootsAndHealthEndpointResponds() throws Exception {
        var credentials = new TestingCredentials();
        try (var stack = new ThingWorxTestStack(credentials)) {
            var response =
                    HttpClient.newHttpClient()
                            .send(
                                    stack.thingworx.healthCheckRequest(),
                                    java.net.http.HttpResponse.BodyHandlers.discarding());
            assertEquals(200, response.statusCode());
        }
    }
}
