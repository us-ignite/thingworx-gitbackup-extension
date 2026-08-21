# Installation

## Requirements

- A supported ThingWorx platform version
- The extension ZIP for the target ThingWorx version
- A ThingWorx administrator account with permission to import extensions
- A Git repository reachable from the ThingWorx server

## Install

1. Download the extension package from the project release artifacts.
2. In ThingWorx Composer, import the extension ZIP.
3. Restart the ThingWorx platform if required by your platform’s extension policy.
4. Confirm that `GIT.Utility.Thing` and the GIT repository entities are present.
5. Create or configure a repository Thing and test its connection before importing application data.

The package variants correspond to the supported ThingWorx SDK baselines. Install the variant matching your platform rather than relying on the filename alone.

## Upgrade

Back up the ThingWorx deployment and repository data before upgrading. Import the new extension package, allow the extension migrator to run, and verify that existing repository configuration, per-user credentials, and GPG keys remain available.
