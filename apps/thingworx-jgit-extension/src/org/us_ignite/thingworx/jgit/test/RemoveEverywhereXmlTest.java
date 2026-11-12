package org.us_ignite.thingworx.jgit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RemoveEverywhereXmlTest {
    @TempDir Path directory;

    @Test
    void readsMultipleEntities() throws Exception {
        Path file = directory.resolve("multi.xml");
        Files.writeString(
                file,
                "<Entities><Things><Thing name=\"One\"/><Thing name=\"Two\"/></Things><ThingShapes><ThingShape name=\"Shape\"/></ThingShapes></Entities>");
        assertEquals(3, RemoveEverywhere.readTargets(file).size());
    }

    @Test
    void acceptsMashupExport() throws Exception {
        Path file = directory.resolve("mashup.xml");
        Files.writeString(file, "<Entities><Mashups><Mashup name=\"Page\"/></Mashups></Entities>");
        assertEquals(1, RemoveEverywhere.readTargets(file).size());
    }

    @Test
    void rejectsUnsupportedOrEmptyEntities() throws Exception {
        Path file = directory.resolve("unsupported.xml");
        Files.writeString(
                file,
                "<Entities><Things><Thing name=\"One\"/></Things><Unknowns><Unknown name=\"Page\"/></Unknowns></Entities>");
        assertThrows(IllegalArgumentException.class, () -> RemoveEverywhere.readTargets(file));
        Files.writeString(file, "<Entities><Things><Thing/></Things></Entities>");
        assertThrows(IllegalArgumentException.class, () -> RemoveEverywhere.readTargets(file));
        Files.writeString(file, "<Entities/>");
        assertThrows(IllegalArgumentException.class, () -> RemoveEverywhere.readTargets(file));
    }

    @Test
    void rejectsExternalEntityDeclarations() throws Exception {
        Path file = directory.resolve("unsafe.xml");
        Files.writeString(
                file,
                "<!DOCTYPE Entities [<!ENTITY xxe SYSTEM 'file:///etc/passwd'>]><Entities><Things><Thing name='&xxe;'/></Things></Entities>");
        assertThrows(Exception.class, () -> RemoveEverywhere.readTargets(file));
    }
}
