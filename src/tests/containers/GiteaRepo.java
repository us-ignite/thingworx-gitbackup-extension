package containers;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Base64;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import util.TestingCredentials;

public class GiteaRepo extends GenericContainer<GiteaRepo> {
    TestingCredentials credentials;

    public GiteaRepo(Network network, TestingCredentials credentials) throws Exception {
        super("gitea/gitea:latest");
        this.credentials = credentials;
        withNetwork(network);
        withNetworkAliases("gitea");
        withEnv("GITEA__security__INSTALL_LOCK", "true");
        withEnv("GITEA__server__DOMAIN", "localhost");
        withEnv("GITEA__server__HTTP_PORT", "3000");
        withEnv("GITEA__server__ROOT_URL", "http://localhost:3000/");
        withEnv("GITEA__database__DB_TYPE", "sqlite3");
        withExposedPorts(3000);
        withCommand("sh", "-c",
                "su git -c 'gitea web' & " +
                "for i in $(seq 1 90); do " +
                "  curl -sf http://localhost:3000/ >/dev/null 2>&1 && break; " +
                "  sleep 2; " +
                "done; " +
                "su git -c 'gitea admin user create --username " + credentials.giteaUser + " --password " + credentials.giteaPass + " --email admin@example.com --admin --must-change-password=false' 2>/dev/null || true; " +
                "su git -c 'gitea admin repo create --name " + credentials.repoName + " --owner " + credentials.giteaUser + " --auto-init=true --private=false' 2>/dev/null || true; " +
                "wait");
        waitingFor(Wait.forHttp("/").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(3)));
    }

    public HttpRequest.Builder apiRequest(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create("http://gitea:3000" + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((credentials.giteaUser + ":" + credentials.giteaPass).getBytes()));
    }
}
