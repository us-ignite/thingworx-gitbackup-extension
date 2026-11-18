package org.us_ignite.thingworx.jgit.extension;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * XML parsing that refuses DTDs and external entities.
 *
 * <p>ThingWorx puts its own Apache Xerces on the classpath, so {@link
 * DocumentBuilderFactory#newInstance()} may return a parser that predates JAXP 1.5 and throws for
 * the {@code accessExternalDTD} and {@code accessExternalSchema} attributes. Those attributes are
 * set only where the parser supports them. The protection that matters, {@code
 * disallow-doctype-decl}, is supported by every Xerces and stays required: a document with a
 * DOCTYPE is rejected before any entity could be resolved.
 */
final class SafeXml {
    private SafeXml() {}

    static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        return harden(DocumentBuilderFactory.newInstance()).newDocumentBuilder();
    }

    static DocumentBuilderFactory harden(DocumentBuilderFactory factory)
            throws ParserConfigurationException {
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        setIfSupported(factory, XMLConstants.ACCESS_EXTERNAL_DTD);
        setIfSupported(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA);
        return factory;
    }

    private static void setIfSupported(DocumentBuilderFactory factory, String attribute) {
        try {
            factory.setAttribute(attribute, "");
        } catch (IllegalArgumentException unsupported) {
            // Older Xerces; disallow-doctype-decl already blocks external DTDs.
        }
    }
}
