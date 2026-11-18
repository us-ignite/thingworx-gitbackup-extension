package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;

class SafeXmlTest {
    /** Behaves like the Xerces on the ThingWorx classpath, which predates JAXP 1.5. */
    private static final class PreJaxp15Factory extends DocumentBuilderFactory {
        private final DocumentBuilderFactory delegate = DocumentBuilderFactory.newDefaultInstance();
        boolean doctypeDisallowed;

        @Override
        public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
            return delegate.newDocumentBuilder();
        }

        @Override
        public void setAttribute(String name, Object value) {
            throw new IllegalArgumentException("Property '" + name + "' is not recognized.");
        }

        @Override
        public Object getAttribute(String name) {
            throw new IllegalArgumentException(name);
        }

        @Override
        public void setFeature(String name, boolean value) throws ParserConfigurationException {
            if (name.endsWith("disallow-doctype-decl")) doctypeDisallowed = value;
            delegate.setFeature(name, value);
        }

        @Override
        public boolean getFeature(String name) throws ParserConfigurationException {
            return delegate.getFeature(name);
        }
    }

    @Test
    void parserWithoutJaxp15AttributesStillGetsDoctypeProtection() throws Exception {
        PreJaxp15Factory factory = new PreJaxp15Factory();
        SafeXml.harden(factory);
        assertTrue(factory.doctypeDisallowed);
    }

    @Test
    void rejectsDoctypes() throws Exception {
        String xml = "<!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><x>&e;</x>";
        assertThrows(
                Exception.class,
                () ->
                        SafeXml.newDocumentBuilder()
                                .parse(
                                        new ByteArrayInputStream(
                                                xml.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void parsesOrdinaryEntityXml() throws Exception {
        String xml = "<Entities><Things><Thing name=\"Pump\"/></Things></Entities>";
        var root =
                SafeXml.newDocumentBuilder()
                        .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                        .getDocumentElement();
        assertEquals("Entities", root.getTagName());
    }
}
