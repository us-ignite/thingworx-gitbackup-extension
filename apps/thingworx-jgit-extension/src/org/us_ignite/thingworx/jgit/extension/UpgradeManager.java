package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.entities.utils.EntityUtilities;
import com.thingworx.entities.utils.UserUtilities;
import com.thingworx.logging.LogUtilities;
import com.thingworx.relationships.RelationshipTypes.ThingworxRelationshipTypes;
import com.thingworx.resources.entities.EntityServices;
import com.thingworx.security.users.User;
import com.thingworx.things.Thing;
import com.thingworx.thingshape.ThingShape;
import com.thingworx.thingtemplates.ThingTemplate;
import com.thingworx.types.InfoTable;
import com.thingworx.types.TagCollection;
import com.thingworx.types.primitives.IPrimitiveType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;

/**
 * Brings every repository and every user's saved Git settings up to the current storage format.
 *
 * <p>For each repository Thing it opens the repository, which moves a Git directory out of the
 * FileRepository folder and the retired {@code GitRepoURL} property into {@code .git/config}, and
 * converts the retired {@code ProjectName} and {@code RepoPath} properties into the tracked
 * projects table: together they become one tracked project whose {@code Path} is {@code
 * <RepoPath>/<ProjectName>}, so its files stay where they are. For each user it then:
 *
 * <ul>
 *   <li>stores their settings tables with their data shapes, so saved rows survive a reload;
 *   <li>moves the commit name and email kept per repository to the user's own identity; and
 *   <li>copies the repository-wide sign-in to each remote of the repository and removes the
 *       repository-wide copy, and deletes sign-in saved for remotes that no longer exist.
 * </ul>
 *
 * <p>Every step is safe to repeat: settings already in the current format are left as they are. A
 * repository that cannot be opened keeps its users' repository-wide sign-in, which its remotes keep
 * using until a later run moves it. The upgrade runs when the extension is imported and when the
 * platform starts, and an administrator can run it with {@code RunUpgrade}.
 */
final class UpgradeManager {
    private static final Logger LOG =
            LogUtilities.getInstance().getApplicationLogger(UpgradeManager.class);
    private static final double MAX_ITEMS = 1_000_000;

    /** One line of the upgrade report. */
    record Step(String scope, String name, String item, String action, String detail) {}

    private final List<Step> steps = new ArrayList<>();

    /** The remotes of every repository that could be opened, by repository and remote name. */
    private final Map<String, Map<String, List<URIish>>> remotes = new TreeMap<>();

    private UpgradeManager() {}

    /** Runs every upgrade step; the caller must be an administrator. */
    static List<Step> run() {
        UpgradeManager upgrade = new UpgradeManager();
        upgrade.upgradeRepositories();
        upgrade.upgradeUsers();
        return upgrade.steps;
    }

    private void step(String scope, String name, String item, String action, String detail) {
        Step step = new Step(scope, name, item, action, detail);
        steps.add(step);
        LOG.warn(
                "Upgrade: {} {}{}: {}{}",
                scope,
                name,
                item.isEmpty() ? "" : " " + item,
                action,
                detail.isEmpty() ? "" : " (" + detail + ")");
    }

    private void upgradeRepositories() {
        List<String> names;
        try {
            names = repositoryNames();
        } catch (Exception e) {
            step("system", "", "repositories", "skipped", "could not list them: " + e.getMessage());
            return;
        }
        for (String name : names) {
            FileRepositoryManager repository = null;
            try {
                Thing thing =
                        (Thing) EntityUtilities.findEntity(name, ThingworxRelationshipTypes.Thing);
                if (thing == null) continue;
                String legacyUrl = text(thing.getPropertyValue(Const.GitRepoURL));
                String legacyProject = text(thing.getPropertyValue(Const.LegacyProjectName));
                String legacyPath = text(thing.getPropertyValue(Const.RepoPath));
                // Opening the repository applies its own upgrades; see FileRepositoryManager.
                repository = new FileRepositoryManager(thing);
                reportProjectSettings(name, repository, legacyProject, legacyPath);
                Git git = repository.git();
                if (!legacyUrl.isEmpty()
                        && text(thing.getPropertyValue(Const.GitRepoURL)).isEmpty()) {
                    step(
                            "repository",
                            name,
                            Const.GitRepoURL,
                            "moved",
                            "to the origin remote in .git/config");
                }
                Map<String, List<URIish>> urls = new LinkedHashMap<>();
                for (RemoteConfig remote : Remotes.list(git)) {
                    List<URIish> uris = new ArrayList<>(remote.getURIs());
                    for (URIish push : remote.getPushURIs())
                        if (!uris.contains(push)) uris.add(push);
                    urls.put(remote.getName(), uris);
                }
                remotes.put(name, urls);
            } catch (Exception e) {
                step("repository", name, "", "not upgraded", String.valueOf(e.getMessage()));
            } finally {
                if (repository != null) repository.close();
            }
        }
    }

    /**
     * Reports the conversion of the retired {@code ProjectName} and {@code RepoPath} properties,
     * which opening the repository performed, or performs it now and reports why it failed.
     */
    private void reportProjectSettings(
            String name,
            FileRepositoryManager repository,
            String legacyProject,
            String legacyPath) {
        if (legacyProject.isEmpty() && legacyPath.isEmpty()) return;
        String item = Const.LegacyProjectName + ", " + Const.RepoPath;
        try {
            // Normally a no-op: opening converted them already. Repeat it to report a failure.
            repository.migrateLegacyProjectSettings();
            List<String> rows = new ArrayList<>();
            for (FileRepositoryManager.TrackedProject project :
                    repository.trackedProjectEntries()) {
                rows.add(
                        project.name()
                                + " in "
                                + FileRepositoryManager.projectFolder(project)
                                + "/");
            }
            step(
                    "repository",
                    name,
                    item,
                    "moved",
                    rows.isEmpty()
                            ? "no project was set; both properties were cleared"
                            : "to TrackedProjects: " + String.join(", ", rows));
        } catch (Exception e) {
            step(
                    "repository",
                    name,
                    item,
                    "not moved",
                    e.getMessage() + "; synchronization is refused until they are cleared");
        }
    }

    private void upgradeUsers() {
        ThingShape userExtensions =
                (ThingShape)
                        EntityUtilities.findEntity(
                                "UserExtensions", ThingworxRelationshipTypes.ThingShape);
        if (userExtensions == null
                || userExtensions.getEffectivePropertyDefinition(Const.UserRepositoryConfiguration)
                        == null) {
            step("system", "", "users", "skipped", "user extension properties are not defined yet");
            return;
        }
        boolean perRemote =
                userExtensions.getEffectivePropertyDefinition(Const.UserRemoteCredentials) != null;
        if (!perRemote) {
            step(
                    "system",
                    "",
                    Const.UserRemoteCredentials,
                    "skipped",
                    "the property is not defined yet");
        }
        List<String> names;
        try {
            names = userNames();
        } catch (Exception e) {
            step("system", "", "users", "skipped", "could not list them: " + e.getMessage());
            return;
        }
        for (String name : names) {
            try {
                User user = UserUtilities.findUser(name);
                if (user == null) continue;
                GitUserContextManager settings = new GitUserContextManager(user);
                for (String repaired : settings.repairStoredTables()) {
                    step("user", name, repaired, "repaired", "stored again with its data shape");
                }
                List<GitUserContextManager.Change> changes =
                        new ArrayList<>(settings.moveLegacyIdentity());
                if (perRemote) {
                    changes.addAll(settings.moveRepositorySignIn(remotes));
                    changes.addAll(settings.pruneRemovedRemotes(remotes));
                }
                for (GitUserContextManager.Change change : changes) {
                    String item =
                            change.repository().isEmpty()
                                    ? change.item()
                                    : change.repository() + " " + change.item();
                    step("user", name, item, change.action(), change.detail());
                }
            } catch (Exception e) {
                step("user", name, "", "not upgraded", String.valueOf(e.getMessage()));
            }
        }
    }

    private static List<String> repositoryNames() throws Exception {
        ThingTemplate template =
                (ThingTemplate)
                        EntityUtilities.findEntity(
                                Const.RepositoryThingTemplateName,
                                ThingworxRelationshipTypes.ThingTemplate);
        if (template == null) return List.of();
        return names(template.GetImplementingThings());
    }

    private static List<String> userNames() throws Exception {
        return names(
                new EntityServices()
                        .GetEntityList(
                                ThingworxRelationshipTypes.User.name(),
                                "",
                                new TagCollection(),
                                MAX_ITEMS));
    }

    private static List<String> names(InfoTable table) {
        List<String> names = new ArrayList<>();
        if (table == null) return names;
        for (int i = 0; i < table.getRowCount(); i++) {
            String name = table.getRow(i).getStringValue("name");
            if (name != null && !name.isBlank()) names.add(name);
        }
        return names;
    }

    private static String text(Object value) {
        Object raw =
                value instanceof IPrimitiveType ? ((IPrimitiveType<?, ?>) value).getValue() : value;
        return raw == null ? "" : raw.toString().trim();
    }
}
