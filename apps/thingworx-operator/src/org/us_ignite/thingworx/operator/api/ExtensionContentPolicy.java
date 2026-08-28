package org.us_ignite.thingworx.operator.api;

/** Strict import policy. Entity-only is the default safe mode. */
public class ExtensionContentPolicy {
    private boolean entities = true;
    private boolean extensibleEntities;
    private boolean jarResources;
    private boolean javascriptResources;
    private boolean cssResources;
    private boolean jsonResources;
    private boolean webAppResources;

    public boolean isEntities() {
        return entities;
    }

    public void setEntities(boolean value) {
        entities = value;
    }

    public boolean isExtensibleEntities() {
        return extensibleEntities;
    }

    public void setExtensibleEntities(boolean value) {
        extensibleEntities = value;
    }

    public boolean isJarResources() {
        return jarResources;
    }

    public void setJarResources(boolean value) {
        jarResources = value;
    }

    public boolean isJavascriptResources() {
        return javascriptResources;
    }

    public void setJavascriptResources(boolean value) {
        javascriptResources = value;
    }

    public boolean isCssResources() {
        return cssResources;
    }

    public void setCssResources(boolean value) {
        cssResources = value;
    }

    public boolean isJsonResources() {
        return jsonResources;
    }

    public void setJsonResources(boolean value) {
        jsonResources = value;
    }

    public boolean isWebAppResources() {
        return webAppResources;
    }

    public void setWebAppResources(boolean value) {
        webAppResources = value;
    }

    public boolean requiresRestart() {
        return jarResources || webAppResources;
    }
}
