# ThingWorx JGit Extension

The ThingWorx JGit Extension provides Git integration for ThingWorx application entities. It supports repository operations, bulk project import/export, GPG commit signing, credentials, proxy configuration, and operation logging.

## Supported platforms

The extension is built for Java 21 and is tested against ThingWorx 9.6, 9.7, 10.1, and 10.2 variants. Check the release artifacts and compatibility notes before installing into a production platform.

## Start here

- [Install or upgrade the extension](installation.md)
- [Configure a repository](configuration.md)
- [Run Git operations](operations.md)
- [Export and import ThingWorx entities](import-export.md)
- [Configure GPG signing](gpg.md)
- [Browse the Java API reference](api/index.md)
- [Understand the mashup export format](mashup-format-primer.md)
- [Build UI with the project widgets and data model](ui-agent-guide.md)

## Security reminder

Credentials and private keys must be supplied through the ThingWorx UI’s per-user `UserExtensions` properties and should never be committed to a repository or pasted into documentation. Use a least-privilege Git account and protect the FileRepository used for local repository data.
