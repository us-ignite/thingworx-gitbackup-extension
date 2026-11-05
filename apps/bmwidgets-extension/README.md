# BMCoreUIWidgets (BMTypescriptClassHost)

Gradle wrapper subproject expressing the third-party widget dependency used by
the GIT mashups in this monorepo (the SIVOC/GitBackup mashups include a
`BMTypescriptClassHost` widget that hosts the `GitHelper` TypeScript class).

- **Upstream:** github.com/thingworx-field-work/BMCoreUIWidgets, released under the MIT license.
- **Pinned artifact:** `BMCoreUIWidgets-min-2.9.3.zip`
  (SHA-256 `7cb8d7a6a3a26c9ebd0423927feb72a56e922ff1866755402dc606bf5605aba2`).
- **Packaging:** `:apps:bmwidgets-extension:packageExtension` downloads the
  pinned release at build time, verifies the checksum, and publishes the zip
  unchanged — the upstream artifact is already a valid ThingWorx extension
  (IIFE-wrapped BMCoreUI + all dependent widgets, including BMCodeHost).
- To upgrade, bump `bmWidgetVersion` and the pin in `apps/bmwidgets-extension/build.gradle`.
