import org.testcontainers.containers.Network;

import containers.DBInit;
import containers.GiteaRepo;
import containers.GitBackupExtensionInstaller;
import containers.Postgres;
import containers.ThingWorxContainer;
import util.TestingCredentials;
import util.ThingWorxVersion;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.file.Paths;

public class GitBackupExtensionTestStack implements AutoCloseable {
    public final Network network;
    public final Postgres postgres;
    public final DBInit dbInit;
    public final GiteaRepo gitea;
    public final ThingWorxContainer thingworx;
    public final GitBackupExtensionInstaller installer;
    public final HttpClient httpClient = HttpClient.newBuilder().build();



    public GitBackupExtensionTestStack(ThingWorxVersion thingworxVersion, TestingCredentials credentials, boolean enableGitea) throws Exception {
        network = Network.newNetwork();
        postgres = new Postgres(network, credentials);
        dbInit = new DBInit(thingworxVersion.dbInitImage, postgres, network, credentials);
        gitea = enableGitea ? new GiteaRepo(network, credentials) : null;
        thingworx = new ThingWorxContainer(thingworxVersion.platformImage, dbInit, postgres, network, credentials);
        installer = new GitBackupExtensionInstaller(Paths.get("build/distributions/GitBackupExtension.zip"), thingworx, network, credentials);   
        this.start();
    }

    public GitBackupExtensionTestStack(ThingWorxVersion thingworxVersion) throws Exception {
        this(thingworxVersion, new TestingCredentials(), true);
    }

    public GitBackupExtensionTestStack(ThingWorxVersion thingworxVersion, boolean enableGitea) throws Exception {
        this(thingworxVersion, new TestingCredentials(), enableGitea);
    }

    public GitBackupExtensionTestStack(ThingWorxVersion thingworxVersion, TestingCredentials credentials) throws Exception {
        this(thingworxVersion, credentials, true);
    }

    public void start() throws Exception {
        postgres.start();
        dbInit.start();
        thingworx.start();
        installer.start();
        if (gitea != null) {
            gitea.start();
        }

        var req1 = thingworx.serviceRequest("GIT.Utility.Thing", "InitUserExtensionProperties", null).build();
        var req2 = thingworx.serviceRequest("GIT.Utility.Thing", "InitUserExtensionGpgKeysProperty", null).build();

        httpClient.send(req1, HttpResponse.BodyHandlers.ofString());
        httpClient.send(req2, HttpResponse.BodyHandlers.ofString());
    }

    public void close() {
        installer.close();
        thingworx.close(); 
        dbInit.close();
        postgres.close();
        gitea.close();
        network.close();
    }
}
