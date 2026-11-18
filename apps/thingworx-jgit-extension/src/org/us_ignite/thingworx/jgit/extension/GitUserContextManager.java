package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.data.util.InfoTableInstanceFactory;
import com.thingworx.entities.utils.UserUtilities;
import com.thingworx.security.users.User;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.InfoTablePrimitive;
import com.thingworx.types.primitives.PasswordPrimitive;
import com.thingworx.types.primitives.StringPrimitive;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.eclipse.jgit.transport.URIish;

/** User-scoped Git configuration and GPG-key access. */
final class GitUserContextManager {
    private final User user;

    GitUserContextManager() throws Exception {
        String currentUserName = UserUtilities.getCurrentUser();
        if (currentUserName == null || currentUserName.trim().isEmpty()) {
            throw new IllegalStateException("No authenticated user context is available.");
        }
        User currentUser = UserUtilities.findUser(currentUserName);
        if (currentUser == null) {
            throw new IllegalStateException("Authenticated user was not found: " + currentUserName);
        }
        this.user = currentUser;
    }

    /** Settings of another user, for the upgrade manager; callers must be administrators. */
    GitUserContextManager(User user) {
        this.user = user;
    }

    /** The user whose settings these are, or {@code null} without an authenticated user. */
    User user() {
        return user;
    }

    User requireUser() {
        if (user == null)
            throw new IllegalStateException("No authenticated user context is available.");
        return user;
    }

    InfoTable credentials() throws Exception {
        InfoTable table =
                typedTable(
                        Const.UserRepositoryConfiguration,
                        Const.GitCredentialsDataShapeName,
                        "GitThing");
        return table == null ? null : withSigningFields(table);
    }

    /** An empty {@code GIT.RepositoryConfiguration.UserExtension.DataShape} table. */
    static InfoTable newCredentialsTable() throws Exception {
        return withSigningFields(
                InfoTableInstanceFactory.createInfoTableFromDataShape(
                        Const.GitCredentialsDataShapeName));
    }

    /**
     * Adds the signing fields when the table lacks them. An upgrade keeps the earlier copy of the
     * data shape, which is an editable extension object, and values outside a table's fields are
     * dropped when the user is saved.
     */
    static InfoTable withSigningFields(InfoTable table) {
        addField(table, Const.SigningFormat, 9);
        addField(table, Const.SigningSshKeyFingerprint, 10);
        return table;
    }

    private static void addField(InfoTable table, String name, int ordinal) {
        if (table.hasField(name)) return;
        var field =
                new com.thingworx.metadata.FieldDefinition(
                        name, com.thingworx.types.BaseTypes.STRING);
        field.setOrdinal(ordinal);
        table.addField(field);
    }

    InfoTable gpgKeys() throws Exception {
        return typedTable(
                Const.UserGpgKeys, Const.UserGpgKeyDataShapeName, Const.GpgKeyFingerprint);
    }

    InfoTable sshKeys() throws Exception {
        return typedTable(
                Const.UserSshKeys, Const.UserSshKeyDataShapeName, Const.SshKeyFingerprint);
    }

    /**
     * The user's per-remote sign-in rows, or {@code null} before InitUserExtensionProperties has
     * defined the property; remotes then use the repository-wide row.
     */
    InfoTable remoteCredentials() throws Exception {
        try {
            if (user == null || user.getPropertyValue(Const.UserRemoteCredentials) == null) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        return typedTable(
                Const.UserRemoteCredentials,
                Const.RemoteCredentialsDataShapeName,
                Const.RemoteName);
    }

    /** The user's own sign-in row for one remote, or {@code null} when the remote has none. */
    ValueCollection remoteCredentialsRow(String repositoryThingName, String remote)
            throws Exception {
        if (remote == null) return null;
        return remoteCredentialsRow(remoteCredentials(), repositoryThingName, remote);
    }

    /**
     * The sign-in used for one remote: the remote's own row when the user saved one, otherwise the
     * repository-wide row that earlier versions stored. Both rows name the user, password, SSH key
     * and approved origins with the same fields.
     */
    ValueCollection credentialsFor(String repositoryThingName, String remote) throws Exception {
        ValueCollection own = remoteCredentialsRow(repositoryThingName, remote);
        return own != null ? own : credentialsFor(repositoryThingName);
    }

    /**
     * Saves the sign-in for one remote. A {@code null} password keeps the password the remote uses
     * now, including one it inherits from the repository-wide row.
     */
    void saveRemoteCredentials(
            String repositoryThingName,
            String remote,
            String username,
            String password,
            String sshKeyFingerprint,
            String approvedOrigins)
            throws Exception {
        InfoTable table = remoteCredentials();
        if (table == null) {
            table =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.RemoteCredentialsDataShapeName);
        }
        ValueCollection row = remoteCredentialsRow(table, repositoryThingName, remote);
        if (password == null) {
            ValueCollection current = credentialsFor(repositoryThingName, remote);
            password = text(current, Const.GitCommitterPassword);
        }
        if (row == null) {
            row = new ValueCollection();
            row.put("GitThing", new StringPrimitive(repositoryThingName));
            row.put(Const.RemoteName, new StringPrimitive(remote));
            table.addRow(row);
        }
        row.put(Const.GitCommitterUser, new StringPrimitive(nullToEmpty(username).trim()));
        row.put(Const.GitCommitterPassword, new PasswordPrimitive(password));
        row.put(Const.SshKeyFingerprint, new StringPrimitive(nullToEmpty(sshKeyFingerprint)));
        row.put(Const.ApprovedRemoteOrigins, new StringPrimitive(nullToEmpty(approvedOrigins)));
        requireUser().setPropertyValue(Const.UserRemoteCredentials, new InfoTablePrimitive(table));
    }

    /** Re-binds a remote's saved sign-in to its current origins, when it has one of its own. */
    void setRemoteApprovedOriginsIfConfigured(
            String repositoryThingName, String remote, String origins) throws Exception {
        InfoTable table = remoteCredentials();
        ValueCollection row = remoteCredentialsRow(table, repositoryThingName, remote);
        if (row == null) return;
        row.put(Const.ApprovedRemoteOrigins, new StringPrimitive(nullToEmpty(origins)));
        requireUser().setPropertyValue(Const.UserRemoteCredentials, new InfoTablePrimitive(table));
    }

    /**
     * Removes the user's own sign-in rows for a repository: for one remote, or for every remote
     * when {@code remote} is {@code null}.
     *
     * @return true when a row was removed
     */
    boolean removeRemoteCredentials(String repositoryThingName, String remote) throws Exception {
        return rewriteRemoteCredentials(
                row ->
                        repositoryThingName.equals(row.getStringValue("GitThing"))
                                && (remote == null
                                        || remote.equals(row.getStringValue(Const.RemoteName))),
                null);
    }

    /** Clears a deleted SSH key from every remote that selected it. */
    boolean clearRemoteSshKey(String fingerprint) throws Exception {
        return rewriteRemoteCredentials(
                null,
                row -> {
                    if (!fingerprint.equals(row.getStringValue(Const.SshKeyFingerprint)))
                        return false;
                    row.put(Const.SshKeyFingerprint, new StringPrimitive(""));
                    return true;
                });
    }

    private boolean rewriteRemoteCredentials(
            java.util.function.Predicate<ValueCollection> drop,
            java.util.function.Predicate<ValueCollection> change)
            throws Exception {
        InfoTable table = remoteCredentials();
        if (table == null) return false;
        InfoTable kept =
                InfoTableInstanceFactory.createInfoTableFromDataShape(
                        Const.RemoteCredentialsDataShapeName);
        boolean changed = false;
        for (int i = 0; i < table.getRowCount(); i++) {
            ValueCollection row = table.getRow(i);
            if (drop != null && drop.test(row)) {
                changed = true;
                continue;
            }
            if (change != null && change.test(row)) changed = true;
            kept.addRow(row);
        }
        if (changed) {
            requireUser()
                    .setPropertyValue(Const.UserRemoteCredentials, new InfoTablePrimitive(kept));
        }
        return changed;
    }

    private static ValueCollection remoteCredentialsRow(
            InfoTable table, String repositoryThingName, String remote) {
        if (table == null) return null;
        for (int i = 0; i < table.getRowCount(); i++) {
            ValueCollection row = table.getRow(i);
            if (repositoryThingName.equals(row.getStringValue("GitThing"))
                    && remote.equals(row.getStringValue(Const.RemoteName))) return row;
        }
        return null;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** One change the upgrade manager made, or chose not to make, to this user's settings. */
    record Change(String repository, String item, String action, String detail) {}

    /**
     * Moves the commit name and email that earlier versions stored per repository to the user's own
     * identity, then removes the per-repository copies. A user whose repositories hold different
     * names or emails keeps them until they choose one under Commit identity.
     */
    List<Change> moveLegacyIdentity() throws Exception {
        List<Change> changes = new ArrayList<>();
        InfoTable rows = credentials();
        if (rows == null) return changes;
        TreeSet<String> names = new TreeSet<>();
        TreeSet<String> emails = new TreeSet<>();
        for (int i = 0; i < rows.getRowCount(); i++) {
            String name = text(rows.getRow(i), Const.GitCommitterName);
            String email = text(rows.getRow(i), Const.GitCommitterEmail);
            if (!name.isEmpty()) names.add(name);
            if (!email.isEmpty()) emails.add(email);
        }
        if (names.isEmpty() && emails.isEmpty()) return changes;
        Identity current = identityFor(null);
        String name = current.name();
        String email = current.email();
        if (name.isEmpty() && names.size() == 1) {
            name = names.first();
            setIdentity(name, null);
            changes.add(new Change("", Const.GitUserName, "set", name));
        }
        if (email.isEmpty() && emails.size() == 1) {
            email = emails.first();
            setIdentity(null, email);
            changes.add(new Change("", Const.GitUserEmail, "set", email));
        }
        boolean clearNames = !name.isEmpty() && !names.isEmpty();
        boolean clearEmails = !email.isEmpty() && !emails.isEmpty();
        if (!names.isEmpty() && !clearNames) {
            changes.add(
                    new Change(
                            "",
                            Const.GitUserName,
                            "kept",
                            "repositories have different commit names; choose one under Commit identity"));
        }
        if (!emails.isEmpty() && !clearEmails) {
            changes.add(
                    new Change(
                            "",
                            Const.GitUserEmail,
                            "kept",
                            "repositories have different commit emails; choose one under Commit identity"));
        }
        if (!clearNames && !clearEmails) return changes;
        for (int i = 0; i < rows.getRowCount(); i++) {
            ValueCollection row = rows.getRow(i);
            if (clearNames) row.put(Const.GitCommitterName, new StringPrimitive(""));
            if (clearEmails) row.put(Const.GitCommitterEmail, new StringPrimitive(""));
        }
        requireUser()
                .setPropertyValue(Const.UserRepositoryConfiguration, new InfoTablePrimitive(rows));
        changes.add(
                new Change(
                        "",
                        "per-repository identity",
                        "removed",
                        "the user's commit identity applies to every repository"));
        return changes;
    }

    /**
     * Gives every remote the repository-wide sign-in that earlier versions stored, then removes the
     * repository-wide copy. A remote receives the username and password only when it has an http(s)
     * URL, and the SSH key only when it has an ssh URL. The password stays approved only for the
     * URLs the repository-wide sign-in was approved for. A sign-in saved by a version without
     * approvals had one remote, which is now {@code origin}, so its password is approved for
     * origin's URL. Remotes that already have a sign-in of their own keep it.
     *
     * @param remotesByRepository the URLs of each remote, for every repository that could be
     *     opened; repositories missing from it keep their repository-wide sign-in
     */
    List<Change> moveRepositorySignIn(Map<String, Map<String, List<URIish>>> remotesByRepository)
            throws Exception {
        List<Change> changes = new ArrayList<>();
        InfoTable rows = credentials();
        if (rows == null) return changes;
        InfoTable perRemote = remoteCredentials();
        if (perRemote == null) {
            perRemote =
                    InfoTableInstanceFactory.createInfoTableFromDataShape(
                            Const.RemoteCredentialsDataShapeName);
        }
        boolean legacyChanged = false;
        boolean remotesChanged = false;
        for (int i = 0; i < rows.getRowCount(); i++) {
            ValueCollection row = rows.getRow(i);
            String repository = text(row, "GitThing");
            String username = text(row, Const.GitCommitterUser);
            String password = text(row, Const.GitCommitterPassword);
            String sshKey = text(row, Const.SshKeyFingerprint);
            String approved = text(row, Const.ApprovedRemoteOrigins);
            if (username.isEmpty() && password.isEmpty() && sshKey.isEmpty()) continue;
            // Versions before approved origins existed kept one remote, now origin, and sent the
            // password to its URL. Approving origin's URL here gives the password no new
            // destination; other remotes still need the sign-in saved for them.
            boolean savedBeforeApprovals = approved.isEmpty();
            Map<String, List<URIish>> remotes = remotesByRepository.get(repository);
            if (remotes == null) {
                changes.add(
                        new Change(
                                repository,
                                "repository-wide sign-in",
                                "kept",
                                "the repository could not be opened or no longer exists"));
                continue;
            }
            if (remotes.isEmpty()) {
                changes.add(
                        new Change(
                                repository,
                                "repository-wide sign-in",
                                "kept",
                                "the repository has no remotes yet; remotes added later use it"));
                continue;
            }
            for (Map.Entry<String, List<URIish>> remote : remotes.entrySet()) {
                String name = remote.getKey();
                if (remoteCredentialsRow(perRemote, repository, name) != null) {
                    changes.add(
                            new Change(repository, name, "kept", "the remote has its own sign-in"));
                    continue;
                }
                boolean http = false;
                boolean ssh = false;
                boolean allApproved = true;
                TreeSet<String> scopes = new TreeSet<>();
                for (URIish uri : remote.getValue()) {
                    if ("ssh".equals(ConnectionStatus.transport(uri))) {
                        ssh = true;
                        continue;
                    }
                    http = true;
                    if ((savedBeforeApprovals && Remotes.DEFAULT_REMOTE.equals(name))
                            || Remotes.credentialApproved(approved, uri)) {
                        scopes.add(Remotes.credentialScope(uri));
                    } else {
                        allApproved = false;
                    }
                }
                ValueCollection copy = new ValueCollection();
                copy.put("GitThing", new StringPrimitive(repository));
                copy.put(Const.RemoteName, new StringPrimitive(name));
                copy.put(Const.GitCommitterUser, new StringPrimitive(http ? username : ""));
                copy.put(Const.GitCommitterPassword, new PasswordPrimitive(http ? password : ""));
                copy.put(Const.SshKeyFingerprint, new StringPrimitive(ssh ? sshKey : ""));
                copy.put(
                        Const.ApprovedRemoteOrigins,
                        new StringPrimitive(String.join("\n", scopes)));
                perRemote.addRow(copy);
                remotesChanged = true;
                List<String> copied = new ArrayList<>();
                if (http && !username.isEmpty()) copied.add("username");
                if (http && !password.isEmpty()) copied.add("password or token");
                if (ssh && !sshKey.isEmpty()) copied.add("SSH key");
                String detail =
                        copied.isEmpty()
                                ? "nothing applied to its URLs"
                                : String.join(", ", copied);
                if (http && !password.isEmpty() && !allApproved) {
                    detail +=
                            "; the password was not approved for this remote's URL, so save its sign-in again before it is sent";
                }
                changes.add(new Change(repository, name, "sign-in copied", detail));
            }
            row.put(Const.GitCommitterUser, new StringPrimitive(""));
            row.put(Const.GitCommitterPassword, new PasswordPrimitive(""));
            row.put(Const.SshKeyFingerprint, new StringPrimitive(""));
            row.put(Const.ApprovedRemoteOrigins, new StringPrimitive(""));
            legacyChanged = true;
            changes.add(
                    new Change(
                            repository,
                            "repository-wide sign-in",
                            "removed",
                            "each remote has its own sign-in"));
        }
        // Write the copies first: if the second write fails, the sign-in exists twice, never not
        // at all, and the next run removes the repository-wide copy.
        if (remotesChanged) {
            requireUser()
                    .setPropertyValue(
                            Const.UserRemoteCredentials, new InfoTablePrimitive(perRemote));
        }
        if (legacyChanged) {
            requireUser()
                    .setPropertyValue(
                            Const.UserRepositoryConfiguration, new InfoTablePrimitive(rows));
        }
        return changes;
    }

    /**
     * Removes sign-in saved for remotes that no longer exist in repositories that could be opened,
     * so a new remote with the same name starts without it.
     */
    List<Change> pruneRemovedRemotes(Map<String, Map<String, List<URIish>>> remotesByRepository)
            throws Exception {
        List<Change> changes = new ArrayList<>();
        InfoTable perRemote = remoteCredentials();
        if (perRemote == null) return changes;
        for (int i = 0; i < perRemote.getRowCount(); i++) {
            ValueCollection row = perRemote.getRow(i);
            Map<String, List<URIish>> remotes = remotesByRepository.get(text(row, "GitThing"));
            String remote = text(row, Const.RemoteName);
            if (remotes != null && !remotes.containsKey(remote)) {
                changes.add(
                        new Change(
                                text(row, "GitThing"),
                                remote,
                                "sign-in removed",
                                "the remote no longer exists"));
            }
        }
        if (changes.isEmpty()) return changes;
        rewriteRemoteCredentials(
                row -> {
                    Map<String, List<URIish>> remotes =
                            remotesByRepository.get(text(row, "GitThing"));
                    return remotes != null && !remotes.containsKey(text(row, Const.RemoteName));
                },
                null);
        return changes;
    }

    /**
     * Returns a user's stored table with its declared data shape.
     *
     * <p>An INFOTABLE user property defined with an empty default starts as a table without fields.
     * Rows added to such a table work until the user is reloaded from the database, when every
     * value outside the (empty) data shape is dropped and the rows come back blank. So a table
     * without the key field is replaced by one built from the data shape, keeping only rows that
     * still have a key. Writing the returned table back stores it with the right shape.
     */
    private InfoTable typedTable(String propertyName, String dataShapeName, String keyField)
            throws Exception {
        InfoTable stored = infoTableProperty(propertyName);
        if (stored == null) return null;
        if (hasField(stored, keyField)) return stored;
        InfoTable typed = InfoTableInstanceFactory.createInfoTableFromDataShape(dataShapeName);
        for (int i = 0; i < stored.getRowCount(); i++) {
            ValueCollection row = stored.getRow(i);
            Object key = row == null ? null : row.getValue(keyField);
            if (key == null || key.toString().isBlank()) continue;
            typed.addRow(row);
        }
        return typed;
    }

    private static boolean hasField(InfoTable table, String field) {
        var shape = table.getDataShape();
        return shape != null && shape.hasField(field);
    }

    /**
     * Rewrites the user's stored tables that lack their data shape, so values added from now on
     * survive a reload. Rows that already lost their values cannot be recovered and are dropped.
     *
     * @return the names of the properties that were rewritten
     */
    java.util.List<String> repairStoredTables() throws Exception {
        java.util.List<String> repaired = new java.util.ArrayList<>();
        if (user == null) return repaired;
        String[][] tables = {
            {Const.UserRepositoryConfiguration, "GitThing"},
            {Const.UserGpgKeys, Const.GpgKeyFingerprint},
            {Const.UserSshKeys, Const.SshKeyFingerprint},
            {Const.UserRemoteCredentials, Const.RemoteName}
        };
        for (String[] table : tables) {
            InfoTable stored = infoTableProperty(table[0]);
            if (stored == null || hasField(stored, table[1])) continue;
            InfoTable typed =
                    switch (table[0]) {
                        case Const.UserRepositoryConfiguration -> credentials();
                        case Const.UserGpgKeys -> gpgKeys();
                        case Const.UserSshKeys -> sshKeys();
                        default -> remoteCredentials();
                    };
            requireUser().setPropertyValue(table[0], new InfoTablePrimitive(typed));
            repaired.add(table[0]);
        }
        return repaired;
    }

    /** Returns the current user's SSH key row with this fingerprint, or {@code null}. */
    ValueCollection sshKey(String fingerprint) throws Exception {
        InfoTable keys = sshKeys();
        if (keys == null || fingerprint == null || fingerprint.isBlank()) return null;
        for (int i = 0; i < keys.getRowCount(); i++) {
            ValueCollection row = keys.getRow(i);
            if (fingerprint.equals(row.getStringValue(Const.SshKeyFingerprint))) return row;
        }
        return null;
    }

    /**
     * Selects the SSH key used for a repository's SSH remotes, creating the repository row when the
     * user has none yet. A blank fingerprint clears the selection.
     */
    void setRepositorySshKey(String repositoryThingName, String fingerprint) throws Exception {
        setRepositoryField(repositoryThingName, Const.SshKeyFingerprint, fingerprint);
    }

    /** The name and email recorded on commits made by the current user. */
    record Identity(String name, String email) {
        boolean complete() {
            return !name.isBlank() && !email.isBlank();
        }
    }

    /**
     * Returns the user's commit identity. It is stored once per user. Users who only have the name
     * and email that earlier versions stored per repository keep using those for that repository
     * until they save a user-level identity.
     */
    Identity identityFor(String repositoryThingName) throws Exception {
        String name = stringProperty(Const.GitUserName);
        String email = stringProperty(Const.GitUserEmail);
        if ((name.isBlank() || email.isBlank()) && repositoryThingName != null) {
            ValueCollection legacy = credentialsFor(repositoryThingName);
            if (name.isBlank()) name = text(legacy, Const.GitCommitterName);
            if (email.isBlank()) email = text(legacy, Const.GitCommitterEmail);
        }
        return new Identity(name, email);
    }

    /** Saves the user-level commit identity; a blank value leaves that part unchanged. */
    void setIdentity(String name, String email) throws Exception {
        User currentUser = requireUser();
        if (name != null && !name.isBlank()) {
            currentUser.setPropertyValue(Const.GitUserName, new StringPrimitive(name.trim()));
        }
        if (email != null && !email.isBlank()) {
            currentUser.setPropertyValue(Const.GitUserEmail, new StringPrimitive(email.trim()));
        }
    }

    private String stringProperty(String propertyName) {
        try {
            Object value = user.getPropertyValue(propertyName);
            Object raw =
                    value instanceof com.thingworx.types.primitives.IPrimitiveType
                            ? ((com.thingworx.types.primitives.IPrimitiveType<?, ?>) value)
                                    .getValue()
                            : value;
            return raw == null ? "" : raw.toString().trim();
        } catch (Exception e) {
            // The property does not exist until InitUserExtensionProperties has run.
            return "";
        }
    }

    /**
     * Reads a secret field of a stored row, such as a GPG private key or passphrase. Returns an
     * empty string when the field is missing or blank, which happens when a PASSWORD value was
     * cleared, and accepts a value stored as plain text.
     */
    static String secret(ValueCollection row, String field) {
        if (row == null) return "";
        Object primitive = row.getPrimitive(field);
        if (primitive instanceof PasswordPrimitive password) {
            String value = password.getValue();
            return value == null ? "" : value;
        }
        if (primitive instanceof com.thingworx.types.primitives.IPrimitiveType<?, ?> other
                && other.getValue() != null) {
            return String.valueOf(other.getValue());
        }
        return "";
    }

    private static String text(ValueCollection row, String field) {
        try {
            String value = row.getStringValue(field);
            return value == null ? "" : value.trim();
        } catch (Exception e) {
            return "";
        }
    }

    ValueCollection credentialsFor(String repositoryThingName) throws Exception {
        InfoTable credentials = credentials();
        if (credentials != null) {
            for (int i = 0; i < credentials.getRowCount(); i++) {
                ValueCollection row = credentials.getRow(i);
                if (repositoryThingName.equals(row.getStringValue("GitThing"))) return row;
            }
        }
        return new ValueCollection();
    }

    ValueCollection gpgKey(String fingerprint) throws Exception {
        return gpgKeyByFingerprintOrLabel(fingerprint, null);
    }

    /**
     * Resolves an owned GPG key row by fingerprint and/or label. When both are provided, both must
     * match the same row. When neither is provided, an IllegalArgumentException is thrown.
     */
    ValueCollection gpgKeyByFingerprintOrLabel(String fingerprint, String label) throws Exception {
        boolean hasFingerprint = fingerprint != null && !fingerprint.isBlank();
        boolean hasLabel = label != null && !label.isBlank();
        if (!hasFingerprint && !hasLabel) {
            throw new IllegalArgumentException("GpgKeyFingerprint or GpgKeyLabel is required.");
        }
        InfoTable keys = gpgKeys();
        if (keys != null) {
            for (int i = 0; i < keys.getRowCount(); i++) {
                ValueCollection row = keys.getRow(i);
                boolean fingerprintMatches =
                        fingerprint != null
                                && fingerprint.equals(row.getStringValue(Const.GpgKeyFingerprint));
                boolean labelMatches =
                        label != null && label.equals(row.getStringValue(Const.GpgKeyLabel));
                if (hasFingerprint && hasLabel) {
                    if (fingerprintMatches && labelMatches) return row;
                } else if (hasFingerprint) {
                    if (fingerprintMatches) return row;
                } else if (hasLabel) {
                    if (labelMatches) return row;
                }
            }
        }
        return null;
    }

    void validateGpgKeyOwnership(String fingerprint) throws Exception {
        validateGpgKeyOwnership(fingerprint, null);
    }

    void validateGpgKeyOwnership(String fingerprint, String label) throws Exception {
        if (gpgKeyByFingerprintOrLabel(fingerprint, label) == null) {
            String identifier =
                    label != null && !label.isBlank()
                            ? "label '" + label + "'"
                            : "fingerprint " + fingerprint;
            throw new IllegalArgumentException(
                    "GPG key is not owned by the current user: " + identifier);
        }
    }

    void setRepositoryGpgKey(String repositoryThingName, String fingerprint) throws Exception {
        setRepositorySigning(
                repositoryThingName,
                fingerprint == null || fingerprint.isBlank() ? "" : SigningSelection.OPENPGP,
                fingerprint);
    }

    /**
     * Saves the repository's commit signing choice. {@code openpgp} and {@code ssh} store the
     * fingerprint in that format's field and keep the other field. A blank format turns signing off
     * and clears the GPG fingerprint, because a blank format with a GPG fingerprint still means
     * OpenPGP for rows saved by earlier versions.
     */
    void setRepositorySigning(String repositoryThingName, String format, String fingerprint)
            throws Exception {
        String normalized = SigningSelection.format(format);
        if (normalized.equals(SigningSelection.NONE)) normalized = "";
        String value = fingerprint == null ? "" : fingerprint.trim();
        RepositoryRow entry = repositoryRow(repositoryThingName);
        ValueCollection row = entry.row();
        row.put(Const.SigningFormat, new StringPrimitive(normalized));
        switch (normalized) {
            case SigningSelection.OPENPGP ->
                    row.put(Const.GpgKeyFingerprint, new StringPrimitive(value));
            case SigningSelection.SSH ->
                    row.put(Const.SigningSshKeyFingerprint, new StringPrimitive(value));
            default -> row.put(Const.GpgKeyFingerprint, new StringPrimitive(""));
        }
        save(entry);
    }

    /**
     * Saves approved origins only when the user already has a credential row for this repository.
     */
    void setApprovedOriginsIfConfigured(String repositoryThingName, String origins)
            throws Exception {
        User currentUser = requireUser();
        InfoTable configurations = credentials();
        if (configurations == null) return;
        ValueCollection row = null;
        for (int i = 0; i < configurations.getRowCount(); i++) {
            if (repositoryThingName.equals(configurations.getRow(i).getStringValue("GitThing"))) {
                row = configurations.getRow(i);
                break;
            }
        }
        if (row == null) return;
        row.put(Const.ApprovedRemoteOrigins, new StringPrimitive(origins == null ? "" : origins));
        currentUser.setPropertyValue(
                Const.UserRepositoryConfiguration, new InfoTablePrimitive(configurations));
    }

    /** A repository row and the table it belongs to, which is saved as a whole. */
    private record RepositoryRow(InfoTable table, ValueCollection row) {}

    /** The user's row for a repository, added to the table when missing. */
    private RepositoryRow repositoryRow(String repositoryThingName) throws Exception {
        requireUser();
        InfoTable configurations = credentials();
        if (configurations == null) configurations = newCredentialsTable();
        for (int i = 0; i < configurations.getRowCount(); i++) {
            if (repositoryThingName.equals(configurations.getRow(i).getStringValue("GitThing")))
                return new RepositoryRow(configurations, configurations.getRow(i));
        }
        ValueCollection row = new ValueCollection();
        row.put("GitThing", new StringPrimitive(repositoryThingName));
        configurations.addRow(row);
        return new RepositoryRow(configurations, row);
    }

    private void save(RepositoryRow row) throws Exception {
        requireUser()
                .setPropertyValue(
                        Const.UserRepositoryConfiguration, new InfoTablePrimitive(row.table()));
    }

    private void setRepositoryField(String repositoryThingName, String field, String fingerprint)
            throws Exception {
        RepositoryRow entry = repositoryRow(repositoryThingName);
        entry.row().put(field, new StringPrimitive(fingerprint == null ? "" : fingerprint));
        save(entry);
    }

    /**
     * Removes everything this user keeps for a repository: its repository-wide row and the sign-in
     * of each of its remotes.
     *
     * @return true when anything was removed
     */
    boolean forgetRepository(String repositoryThingName) throws Exception {
        boolean changed = false;
        InfoTable rows = credentials();
        if (rows != null) {
            InfoTable kept = newCredentialsTable();
            for (int i = 0; i < rows.getRowCount(); i++) {
                ValueCollection row = rows.getRow(i);
                if (repositoryThingName.equals(row.getStringValue("GitThing"))) changed = true;
                else kept.addRow(row);
            }
            if (changed) {
                requireUser()
                        .setPropertyValue(
                                Const.UserRepositoryConfiguration, new InfoTablePrimitive(kept));
            }
        }
        if (removeRemoteCredentials(repositoryThingName, null)) changed = true;
        return changed;
    }

    /**
     * Removes a repository's settings and sign-in from every user, so a repository created later
     * under the same name does not inherit them.
     *
     * @return the users whose settings changed
     */
    static List<String> forgetRepositoryForAllUsers(String repositoryThingName) throws Exception {
        List<String> changed = new ArrayList<>();
        for (String name : userNames()) {
            User user = UserUtilities.findUser(name);
            if (user == null) continue;
            if (new GitUserContextManager(user).forgetRepository(repositoryThingName)) {
                changed.add(name);
            }
        }
        return changed;
    }

    /** The names of every ThingWorx user. */
    static List<String> userNames() throws Exception {
        InfoTable table =
                new com.thingworx.resources.entities.EntityServices()
                        .GetEntityList(
                                com.thingworx.relationships.RelationshipTypes
                                        .ThingworxRelationshipTypes.User.name(),
                                "",
                                new com.thingworx.types.TagCollection(),
                                1_000_000.0);
        List<String> names = new ArrayList<>();
        if (table == null) return names;
        for (int i = 0; i < table.getRowCount(); i++) {
            String name = table.getRow(i).getStringValue("name");
            if (name != null && !name.isBlank()) names.add(name);
        }
        return names;
    }

    private InfoTable infoTableProperty(String propertyName) throws Exception {
        if (user == null) return null;
        Object value = user.getPropertyValue(propertyName);
        return value instanceof InfoTablePrimitive ? ((InfoTablePrimitive) value).getValue() : null;
    }
}
