# Publishing

The DAP core and runtime are published together to GitHub Packages. Their `.version` files are
owned by the commit hook and remain equal; the main-branch workflow creates the matching
`dap-vX.Y.Z` tag after it publishes both artifacts.

Use a Conventional Commit when changing either package. `fix:` and `perf:` create a patch release,
`feat:` creates a minor release, and a breaking change creates a major release. The hook stages both
version files automatically. To inspect the proposed update without changing the index, run
`./extensions/jgit/scripts/bump-version.sh 'fix: describe the change'` after staging the change.

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
