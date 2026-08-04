# ThingWorx JGit Extension

[![Tests](https://github.com/us-ignite/thingworx-gitbackup-extension/actions/workflows/build-and-publish.yml/badge.svg?branch=main)](https://github.com/us-ignite/thingworx-gitbackup-extension/actions/workflows/build-and-publish.yml)
[![Documentation](https://github.com/us-ignite/thingworx-gitbackup-extension/actions/workflows/docs.yml/badge.svg?branch=main)](https://github.com/us-ignite/thingworx-gitbackup-extension/actions/workflows/docs.yml)
[![Latest release](https://img.shields.io/github/v/release/us-ignite/thingworx-gitbackup-extension?sort=semver)](https://github.com/us-ignite/thingworx-gitbackup-extension/releases/latest)

Git integration for ThingWorx application entities, delivered as a ThingWorx extension and
backed by [JGit](https://www.eclipse.org/jgit/).

> **Pre-1.0 software — use at your own risk.** This project is provided strictly **as is** and
> **without warranties or guarantees of any kind**, express or implied. It is not a supported or
> production-certified product. Features, behavior, service contracts, and configuration formats
> may change without notice and without backward compatibility. Review, test, and back up your
> ThingWorx environment and repositories before using the extension.

## Documentation

Read the **[ThingWorx JGit Extension documentation](https://us-ignite.github.io/thingworx-gitbackup-extension/)**
for installation, configuration, usage, service details, troubleshooting, and development.

The documentation is also available in the repository:

- [Documentation home](docs/index.md)
- [Installation](docs/installation.md)
- [Configuration](docs/configuration.md)
- [Git operations](docs/operations.md)
- [ThingWorx entity import and export](docs/import-export.md)
- [GPG commit signing](docs/gpg.md)
- [ThingWorx service reference](docs/service-reference.md)
- [Java API reference](docs/api/index.md)

## What it provides

The extension exposes Git repositories as ThingWorx repository Things. This lets services,
mashups, and scheduled workflows manage source-controlled ThingWorx projects from the platform.

### Repository and Git workflows

- Create, configure, list, and delete repository Things.
- Clone and manage local repositories in ThingWorx FileRepository storage.
- Stage, commit, push, pull, fetch, and inspect working-tree status.
- Create, switch, check out, and delete branches; inspect commits and reflogs.
- Merge and rebase branches, including conflict inspection, resolution, continuation, and abort.
- Create, list, and delete tags, and retrieve commit and file-diff information.

### ThingWorx project synchronization

- Export the project configured on a repository Thing into its Git working tree.
- Import ThingWorx entities recursively from a repository path.
- Include project dependents and localization resources where configured.
- Keep synchronization tied to the repository’s required ThingWorx project.
- Keep export, commit, and push as explicit steps so changes can be reviewed before publication.

### Credentials, signing, and operations

- Store Git credentials and committer identity per ThingWorx user and repository.
- Store reusable, user-isolated GPG keys and select a signing key per repository.
- Use HTTP proxy settings for repository connections.
- Record service failures and operational events in the extension log data table.
- Provide service results and InfoTables for use by ThingWorx applications and integrations.

## Supported platforms

The project builds Java 21 extension variants for ThingWorx 9.6, 9.7, and 10.1. Install the
package variant matching the target ThingWorx platform; see the [installation guide](docs/installation.md)
for details.

## Building from source

Building locally requires Java 21, Docker for integration tests, and a ThingWorx Extension SDK
archive from PTC. The SDK archive is intentionally excluded from Git.

```bash
./gradlew build
```

Packages are written to `build/distributions/`. To start the local development stack, see the
[development guide](docs/development.md) and run:

```bash
./gradlew devSetup
```

## Security

Use least-privilege Git accounts and protect the ThingWorx FileRepository containing local clones.
Never commit passwords, private GPG keys, passphrases, SDK archives, or exported protected
ThingWorx user properties.

## License

[MIT](LICENSE)
