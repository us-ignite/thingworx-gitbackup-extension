package util;
import java.util.stream.Stream;

public class ThingWorxVersion {
    public final String label;
    public final String dbInitImage;
    public final String platformImage;

    public ThingWorxVersion(String label, String dbInitImage, String platformImage) {
        this.label = label;
        this.dbInitImage = dbInitImage;
        this.platformImage = platformImage;
    }

    @Override
    public String toString() {
        return label;
    }

    public static Stream<ThingWorxVersion> thingworxVersionsTestMatrix() {
        return Stream.of(
            new ThingWorxVersion("9.5.0",
                    "devopscadit/postgresql-init-twx:platform9.5.0",
                    "devopscadit/platform-postgres:platform9.5.0"),
            new ThingWorxVersion("9.6.3",
                    "devopscadit/postgresql-init-twx:platform9.6.3",
                    "devopscadit/platform-postgres:platform9.6.3"),
            new ThingWorxVersion("9.7.5",
                    "devopscadit/postgresql-init-twx:platform9.7.5",
                    "devopscadit/platform-postgres:platform9.7.5"),
            new ThingWorxVersion("10.1.0",
                    "devopscadit/postgresql-init-twx:platform10.1.0",
                    "devopscadit/platform-postgres:platform10.1.0"));
    }
}
