# ThingWorx JGit Extension

Git integration for ThingWorx application entities, including repository operations, bulk
import/export, GPG commit signing, credentials, proxy support, and operation logging.

## Documentation

Read the complete documentation on the published GitHub Pages site. The repository copy is also
available in [`docs/`](docs/index.md).

The site includes installation, configuration, Git workflows, import/export, GPG signing,
troubleshooting, development, and generated Java API documentation.

## Highlights

- Git operations through ThingWorx services and mashups
- Per-user credentials and GPG key isolation
- Bulk project export and recursive repository import
- Optional automatic commit and push after export
- Repository proxy configuration and operation logging
- Gradle build with Java 21 and supported ThingWorx variants

## Quick start

The local development environment requires Docker, Java 21, and a ThingWorx Extension SDK archive
from PTC. The SDK archive is intentionally excluded from Git.

```bash
./gradlew build
./gradlew devSetup
```

Run integration tests with Docker using:

```bash
./gradlew test
```

Build artifacts are written to `build/distributions/`. See the [development guide](docs/development.md)
for environment and testing details.

## Supported versions

The project currently builds variants for ThingWorx 9.6, 9.7, and 10.1. Use the extension package
variant matching the target platform.

## Security

Never commit passwords, private keys, SDK archives, or exported protected ThingWorx properties.
Use a least-privilege Git account and protect the ThingWorx FileRepository containing local clones.

## License

MIT. See [LICENSE](LICENSE).
