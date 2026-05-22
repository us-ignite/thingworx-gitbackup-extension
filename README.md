# ThingWorx GitBackup Extension
[Unofficial/Not Supported] Contains the code of the ThingWorx GitBackup Extension built on Extension SDK version 9.0 (not provided here).

## Prerequisites

- **JDK 21** — The build and test tasks require Java 21 (Corretto recommended). The `fetchJdk21` Gradle task can download and cache it automatically for the test and dev workflows, but your system still needs a JDK to compile.
- **ThingWorx Extension SDK** — Download the SDK zip (version 9.6.0) from the PTC support portal and place it at `twx-lib/MED-61098-CD-096_9-6-0_ThingWorx-Extension-SDK-9-6-0.zip`. This path is gitignored; you must obtain the SDK separately.
- **Docker** — Required for the development environment (Docker Compose stack).

## Building

The project uses **Gradle** (8.12) with the Gradle Wrapper.

```bash
./gradlew build
```

This produces two artifacts in `build/distributions/`:
- `GitBackupExtension.zip` — the extension package
- `GitBackupExtensionPack.zip` — the extension bundled with 3rd-party extensions

### Development environment

A Docker Compose stack (PostgreSQL 15, ThingWorx 9.7.5, Gitea 1.22.3) is available for local testing.
Start it and install the extension automatically with:

```bash
./gradlew devSetup
```

Set the following in a `.env` file (gitignored) at the project root to authenticate with the container registry:

```
LS_USERNAME=your-license-server-username
LS_PASSWORD=your-license-server-password
```

See the available `compose*` and `dev*` tasks for more options.

In case you encounter issues:
 - Please open issues [here](https://github.com/PTCInc/thingworx-gitbackup-extension/issues) but be aware there's no guaranteed SLA or SLT.
 - Feel free to fork it - it's an Open Source extension and Pull Requests are accepted
 - Do not open PTC Technical Support tickets for this Extension, since it's not a PTC supported product.

Documentation is maintained in a PDF specific for each release.

Download the extension from the Releases section above, link here for ease of use: https://github.com/PTCInc/thingworx-gitbackup-extension/releases.

This Extension is provided as-is and without warranty or support. It is not part of the PTC product suite. This project is licensed under the terms of the MIT license.

## CI/CD — GitHub Actions

The project includes a GitHub Actions workflow (`.github/workflows/build-and-publish.yml`) that:

- **On push/PR to `main`**: builds the extension via `./gradlew build` (requires JDK 21) and uploads both `GitBackupExtension.zip` and `GitBackupExtensionPack.zip` as build artifacts.
- **On tags matching `v*`** (e.g. `v5.1.0`): creates a [GitHub Release](https://github.com/PTCInc/thingworx-gitbackup-extension/releases) with auto-generated release notes and attaches both ZIPs.

To trigger a release:

```bash
git tag v5.1.0
git push origin v5.1.0
```
