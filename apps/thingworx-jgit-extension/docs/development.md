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

This starts the local PostgreSQL, ThingWorx, and Gitea stack, installs the extension, and creates a test repository. It also creates a reusable GPG key labeled `Dev Setup Test Key` for the ThingWorx Administrator, if one does not already exist. Its public key is also registered with the local Gitea `giteauser` account. The key is generated locally with no passphrase and is intended only for development; it is not automatically selected for repository signing. A copy of the public key is kept in the extension build directory to support repeat setup runs. Each setup creates a separate development package with a patch version one higher than the greater of the installed and built versions, and upgrades ThingWorx to that package. The tracked `.version` file and release package are unchanged. Development upgrades can advance the installed version beyond release versions; those releases cannot be installed on that environment until their version exceeds the development version. Python 3 and GnuPG (`gpg`) are required on the host. Supply the required local license-server credentials through the project’s ignored environment file.

`devSetup` installs the required widget dependency extensions and runs one-shot containers with `--no-deps`. The `security-init` service is idempotent: it preserves an existing keystore and license device ID and only initializes when the keystore is absent. Set `FORCE_SECURITY_INIT=true` only when intentionally rotating the keystore.

After saving mashup changes in Composer, export all existing GIT mashup source files with:

```bash
./gradlew :apps:thingworx-jgit-extension:saveChanges
```

The task replaces the corresponding files in `ui/` with exports from the local development instance. Set `TWX_URL` (including `/Thingworx`), `TWX_USERNAME`, and `TWX_PASSWORD` to use another instance. All exports are validated before any files are replaced. Embedded TypeScript and CSS are regenerated from `ui/src` during packaging, so copy edits to those fields into their corresponding source files as well. Use `-PmashupOutputDir=/path/to/directory` to export to a separate directory for review.

To save only one mashup, supply its suffix, for example `-Pmashup=Changes`.

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
