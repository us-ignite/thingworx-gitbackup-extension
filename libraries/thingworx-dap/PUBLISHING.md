# Publishing

The DAP core and runtime are published together to GitHub Packages. Keep their `.version` files
equal, then create the matching `dap-vX.Y.Z` tag to publish both artifacts to the repository's
Maven registry.

Use `./gradlew bumpVersion -Ppatch` (or `-Pminor` / `-Pmajor`) to bump every changed component.
Add `-PdryRun` to inspect the affected version files without editing them.

Consumers authenticate with a personal access token (classic) with `read:packages`, then configure:

```groovy
repositories {
    maven {
        url = uri('https://maven.pkg.github.com/us-ignite/thingworx-gitbackup-extension')
        credentials {
            username = findProperty('gpr.user') ?: System.getenv('GITHUB_ACTOR')
            password = findProperty('gpr.key') ?: System.getenv('GITHUB_TOKEN')
        }
    }
}
```

Verify publication locally with:

```bash
./gradlew :libraries:thingworx-dap:publishToMavenLocal \
  :libraries:thingworx-dap-runtime:publishToMavenLocal
```
