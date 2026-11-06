# GitHistoryWidget

Standalone ThingWorx widget extension used by `GIT.Mashup.History`. The IDE and
runtime bundles follow the AG Grid 2 extension's Vite packaging pattern.
The widget receives data and emits selection events through mashup bindings; it
does not call repository services itself.

Run `npm ci`, `npm run typecheck`, and `npm run build`, or run
`./gradlew :apps:git-history-widget-extension:packageExtension` from the
repository root. The JGit `packageExtensionPack` task collects this ZIP and the
other widget dependencies alongside the JGit ZIP.
