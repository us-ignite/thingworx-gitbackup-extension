# Development

## Build

The project uses Gradle and Java 21. A ThingWorx Extension SDK archive is required locally and is intentionally excluded from Git.

```bash
./gradlew :apps:thingworx-jgit-extension:build
```

The extension packages are written to `apps/thingworx-jgit-extension/build/distributions/`.

## Development stack

```bash
./gradlew :apps:thingworx-jgit-extension:devSetup
```

This starts the local PostgreSQL, ThingWorx, and Gitea stack, installs the extension, and creates a test repository. It also creates a reusable GPG key labeled `Dev Setup Test Key` for the ThingWorx Administrator, if one does not already exist. Its public key is also registered with the local Gitea `giteauser` account. The key is generated locally with no passphrase and is intended only for development; it is not automatically selected for repository signing. A copy of the public key is kept in the extension build directory to support repeat setup runs. Python 3 and GnuPG (`gpg`) are required on the host. Supply the required local license-server credentials through the project’s ignored environment file.

## Tests

Integration tests use Testcontainers and require Docker:

```bash
./gradlew :apps:thingworx-jgit-extension:test
```

The test suite covers Git operations, entity synchronization, GPG behavior, installation, and supported ThingWorx variants.

## API documentation

Generate the Java API documentation with:

```bash
./gradlew :apps:thingworx-jgit-extension:javadoc
```

The generated HTML is staged into this site’s API section by CI.

## Declarative entity generation

ThingWorx entity XML is generated during `compileJava` by the `dap` annotation processor. Java
declarations are the source of truth for DataShapes, ThingShapes, the repository ThingTemplate,
the utility Thing, the project, localization tokens, and extension metadata. Generated files are
written below `build/generated/dap/` in ThingWorx SourceControlEntities-style directories such as
`DataShapes/`, `ThingShapes/`, and `Things/`; do not add hand-authored entity XML.

The DAP example project under `examples/thingworx-dap-usage` exercises DataShape
proxies, ServiceResult generation, and Java service metadata generation against the real SDK:

```bash
./gradlew :examples:thingworx-dap-usage:verifyDapExample
```
