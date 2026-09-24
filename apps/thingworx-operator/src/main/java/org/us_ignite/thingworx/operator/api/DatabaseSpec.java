package org.us_ignite.thingworx.operator.api;

/** External PostgreSQL connection and credentials used by PTC initialization tooling. */
public class DatabaseSpec {
    private boolean internal = true;
    private String image = "postgres:16";
    private String host;
    private Integer port = 5432;
    private String database;
    private String schema;
    private String username;
    private String adminUsername;
    private SecretKeyReference credentials;
    private SecretKeyReference adminCredentials;

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean value) {
        internal = value;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String value) {
        image = value;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String value) {
        host = value;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer value) {
        port = value;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String value) {
        database = value;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String value) {
        schema = value;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String value) {
        username = value;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String value) {
        adminUsername = value;
    }

    public SecretKeyReference getCredentials() {
        return credentials;
    }

    public void setCredentials(SecretKeyReference value) {
        credentials = value;
    }

    public SecretKeyReference getAdminCredentials() {
        return adminCredentials;
    }

    public void setAdminCredentials(SecretKeyReference value) {
        adminCredentials = value;
    }
}
