# Development

## Build

The project uses Gradle and Java 21. A ThingWorx Extension SDK archive is required locally and is intentionally excluded from Git.

```bash
./gradlew build
```

The extension packages are written to `build/distributions/`.

## Development stack

```bash
./gradlew devSetup
```

This starts the local PostgreSQL, ThingWorx, and Gitea stack, installs the extension, and creates a test repository. Supply the required local license-server credentials through the project’s ignored environment file.

## Tests

Integration tests use Testcontainers and require Docker:

```bash
./gradlew test
```

The test suite covers Git operations, entity synchronization, GPG behavior, installation, and supported ThingWorx variants.

## API documentation

Generate the Java API documentation with:

```bash
./gradlew javadoc
```

The generated HTML is staged into this site’s API section by CI.
