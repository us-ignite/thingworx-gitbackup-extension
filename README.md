# US Ignite ThingWorx Extensions

This monorepo contains ThingWorx extensions and reusable Java libraries maintained by US Ignite.

- [JGit extension](extensions/jgit/README.md) — Git integration for ThingWorx entities.
- [ThingWorx DAP](libraries/thingworx-dap/README.md) — annotation processor for generated
  ThingWorx entity XML.
- [ThingWorx DAP runtime](libraries/thingworx-dap-runtime/README.md) — InfoTable and typed
  service-result helpers for DAP consumers.

The local development stack remains available at the repository root:

```bash
docker compose up -d
```

The current public repository and documentation URLs remain unchanged until the planned fork
detachment and repository migration are complete.
