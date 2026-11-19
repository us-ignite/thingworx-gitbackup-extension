package thingworx.dev

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

/** Minimal HTTP helpers for the local development stack (ThingWorx and Gitea on localhost). */
class DevHttp {
    static String basic(String user, String password) {
        "${user}:${password}".getBytes('UTF-8').encodeBase64().toString()
    }

    static HttpURLConnection open(String url, String auth = null, int timeoutMs = 10000) {
        def conn = (HttpURLConnection) new URL(url).openConnection()
        conn.connectTimeout = timeoutMs
        conn.readTimeout = timeoutMs
        if (auth) conn.setRequestProperty('Authorization', "Basic ${auth}")
        conn.setRequestProperty('Accept', 'application/json')
        conn
    }

    /** HTTP status, or -1 when the server is unreachable. */
    static int status(String url, String auth = null, int timeoutMs = 10000) {
        try {
            open(url, auth, timeoutMs).responseCode
        } catch (IOException ignored) {
            -1
        }
    }

    /** GET (payload null) or POST JSON; returns the parsed JSON body. */
    static Object json(String url, String auth, Object payload = null, Map<String, String> headers = [:]) {
        def conn = open(url, auth, 30000)
        headers.each { k, v -> conn.setRequestProperty(k, v) }
        if (payload != null) {
            conn.requestMethod = 'POST'
            conn.doOutput = true
            conn.setRequestProperty('Content-Type', 'application/json')
            conn.outputStream.withCloseable { it.write(JsonOutput.toJson(payload).getBytes('UTF-8')) }
        }
        new JsonSlurper().parse(conn.inputStream, 'UTF-8')
    }

    /** ThingWorx REST requires these on state-changing calls. */
    static final Map<String, String> TWX_POST_HEADERS = ['X-XSRF-TOKEN': 'TWX-XSRF-TOKEN-VALUE', 'X-Requested-By': 'ThingWorx']
}
