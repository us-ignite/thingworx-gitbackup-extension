package org.us_ignite.thingworx.jgit.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.us_ignite.thingworx.jgit.tests.containers.JGitExtensionTestStack;
import org.us_ignite.thingworx.jgit.tests.util.TestingCredentials;

@Testcontainers
public class ThingWorxEntitiesTest {

    private static final HttpClient httpClient = HttpClient.newBuilder().build();

    private static final String DB_INIT_IMAGE =
            System.getProperty("test.dbInitImage", "devopscadit/postgresql-init-twx:platform9.6.3");
    private static final String PLATFORM_IMAGE =
            System.getProperty("test.platformImage", "devopscadit/platform-postgres:platform9.6.3");

    private TestingCredentials credentials = new TestingCredentials();

    @Test
    void entitiesTestsForVersion() throws Exception {

        var stack = new JGitExtensionTestStack(DB_INIT_IMAGE, PLATFORM_IMAGE, credentials);

        var baseUrl =
                stack.thingworx.getExternalUrl() != null
                        ? stack.thingworx.getExternalUrl()
                        : "http://thingworx:8080";
        var utilityResponse = get(baseUrl, "Things/GIT.Utility.Thing");
        assertEquals(
                200,
                utilityResponse.statusCode(),
                "GIT.Utility.Thing GET status. Body: " + utilityResponse.body());
        assertNotNull(
                utilityResponse.body(), "GIT.Utility.Thing response body must not be null");

        var repositoriesProjectResponse = get(baseUrl, "Projects/GIT.Repositories");
        assertEquals(
                200,
                repositoriesProjectResponse.statusCode(),
                "GIT.Repositories GET status. Body: " + repositoriesProjectResponse.body());
        assertNotNull(
                repositoriesProjectResponse.body(),
                "GIT.Repositories response body must not be null");

        stack.close();
    }

    private HttpResponse<String> get(String baseUrl, String entityPath) throws Exception {
        var request =
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/Thingworx/" + entityPath))
                        .header("Accept", "application/json")
                        .header("X-XSRF-TOKEN", "TWX-XSRF-TOKEN-VALUE")
                        .header("X-Requested-By", "ThingWorx")
                        .header(
                                "Authorization",
                                "Basic "
                                        + java.util.Base64.getEncoder()
                                                .encodeToString(
                                                        (credentials.thingworxAdminUser
                                                                        + ":"
                                                                        + credentials
                                                                                .thingworxAdminPass)
                                                                .getBytes()))
                        .GET()
                        .timeout(Duration.ofMinutes(2))
                        .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
