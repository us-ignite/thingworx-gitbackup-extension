package gb.tests.util;

import java.util.HashMap;

public class TestingCredentials {
    public String thingworxAdminUser = "Administrator";
    public String thingworxAdminPass = "AdminP@ssw0rd!123";
    public String twxDatabaseUser = "twxuser";
    public String twxDatabaseSchema = "twx";
    public String twxDatabasePass = "twxpass_P@ssw0rd!";
    public String dbAdminUser = "postgres";
    public String dbAdminSchema = "postgres";
    public String dbAdminPass = "adminpass_P@ssw0rd!";
    public String giteaUser = "giteauser";
    public String giteaPass = "giteapass123";
    public String repoName = "gitbackup-test-repo";

    public TestingCredentials(HashMap<String, String> overrides) {
        thingworxAdminUser = overrides.getOrDefault("thingworxAdminUser", thingworxAdminUser);
        thingworxAdminPass = overrides.getOrDefault("thingworxAdminPass", thingworxAdminPass);
        twxDatabaseUser = overrides.getOrDefault("twxDatabaseUser", twxDatabaseUser);
        twxDatabaseSchema = overrides.getOrDefault("twxDatabaseSchema", twxDatabaseSchema);
        twxDatabasePass = overrides.getOrDefault("twxDatabasePass", twxDatabasePass);
        dbAdminUser = overrides.getOrDefault("dbAdminUser", dbAdminUser);
        dbAdminSchema = overrides.getOrDefault("dbAdminSchema", dbAdminSchema);
        dbAdminPass = overrides.getOrDefault("dbAdminPass", dbAdminPass);
        giteaUser = overrides.getOrDefault("giteaUser", giteaUser);
        giteaPass = overrides.getOrDefault("giteaPass", giteaPass);
        repoName = overrides.getOrDefault("repoName", repoName);
    }

    public TestingCredentials() {
    }
}
