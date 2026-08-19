package org.us_ignite.thingworx.jgit.extension;

import com.thingworx.data.util.InfoTableInstanceFactory;
import com.thingworx.entities.utils.UserUtilities;
import com.thingworx.security.users.User;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.InfoTablePrimitive;
import com.thingworx.types.primitives.StringPrimitive;

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

    User requireUser() {
        if (user == null) throw new IllegalStateException("No authenticated user context is available.");
        return user;
    }

    InfoTable credentials() throws Exception {
        return infoTableProperty(Const.UserRepositoryConfiguration);
    }

    InfoTable gpgKeys() throws Exception {
        return infoTableProperty(Const.UserGpgKeys);
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
        InfoTable keys = gpgKeys();
        if (keys != null) {
            for (int i = 0; i < keys.getRowCount(); i++) {
                ValueCollection row = keys.getRow(i);
                if (fingerprint.equals(row.getStringValue(Const.GpgKeyFingerprint))) return row;
            }
        }
        return null;
    }

    void validateGpgKeyOwnership(String fingerprint) throws Exception {
        if (gpgKey(fingerprint) == null) {
            throw new IllegalArgumentException("GPG key is not owned by the current user: " + fingerprint);
        }
    }

    void setRepositoryGpgKey(String repositoryThingName, String fingerprint) throws Exception {
        User currentUser = requireUser();
        InfoTable configurations = credentials();
        if (configurations == null) {
            configurations = InfoTableInstanceFactory.createInfoTableFromDataShape(Const.GitCredentialsDataShapeName);
        }
        ValueCollection row = null;
        for (int i = 0; i < configurations.getRowCount(); i++) {
            if (repositoryThingName.equals(configurations.getRow(i).getStringValue("GitThing"))) {
                row = configurations.getRow(i);
                break;
            }
        }
        if (row == null) {
            row = new ValueCollection();
            row.put("GitThing", new StringPrimitive(repositoryThingName));
            configurations.addRow(row);
        }
        row.put(Const.GpgKeyFingerprint, new StringPrimitive(fingerprint == null ? "" : fingerprint));
        currentUser.setPropertyValue(Const.UserRepositoryConfiguration, new InfoTablePrimitive(configurations));
    }

    private InfoTable infoTableProperty(String propertyName) throws Exception {
        if (user == null) return null;
        Object value = user.getPropertyValue(propertyName);
        return value instanceof InfoTablePrimitive ? ((InfoTablePrimitive) value).getValue() : null;
    }
}
