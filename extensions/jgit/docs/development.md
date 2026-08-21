# Development

## Build

The project uses Gradle and Java 21. A ThingWorx Extension SDK archive is required locally and is intentionally excluded from Git.

```bash
./gradlew :extensions:jgit:build
```

The extension packages are written to `extensions/jgit/build/distributions/`.

## Development stack

```bash
./gradlew :extensions:jgit:devSetup
```

This starts the local PostgreSQL, ThingWorx, and Gitea stack, installs the extension, and creates a test repository. Supply the required local license-server credentials through the project’s ignored environment file.

## Tests

Integration tests use Testcontainers and require Docker:

```bash
./gradlew :extensions:jgit:test
```

The test suite covers Git operations, entity synchronization, GPG behavior, installation, and supported ThingWorx variants.

## API documentation

Generate the Java API documentation with:

```bash
./gradlew :extensions:jgit:javadoc
```

The generated HTML is staged into this site’s API section by CI.

## Declarative entity generation

ThingWorx entity XML is generated during `compileJava` by the `dap` annotation processor. Java
declarations are the source of truth for DataShapes, ThingShapes, the repository ThingTemplate,
the utility Thing, the project, localization tokens, and extension metadata. Generated files are
written below `build/generated/dap/` in ThingWorx SourceControlEntities-style directories such as
`DataShapes/`, `ThingShapes/`, and `Things/`; do not add hand-authored entity XML.

The DAP example package under `libraries/thingworx-dap-runtime/src/test/java` exercises DataShape
proxies, ServiceResult generation, and Java service metadata generation against the real SDK:

```bash
./gradlew :libraries:thingworx-dap-runtime:verifyDapExample
```
