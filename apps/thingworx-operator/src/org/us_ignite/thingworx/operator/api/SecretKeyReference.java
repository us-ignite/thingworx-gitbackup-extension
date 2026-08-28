package org.us_ignite.thingworx.operator.api;

/** References one key in a namespace-local Secret without copying its value into the CR. */
public class SecretKeyReference {
    private String name;
    private String key;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
