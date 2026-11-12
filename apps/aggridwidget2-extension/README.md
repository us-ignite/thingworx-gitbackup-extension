# agGridWidget2

In-housed copy of the Sabel Systems `agGridWidget2` ThingWorx widget extension, rebuilt as two TypeScript/Vite projects.

- **Source artifact:** Sabel Systems `agGridWidget2-min-100.1.20.zip` (package version `100.1.20`, dated 2025-05-12).
- **Projects:** [`ide/`](ide/) builds `agGridWidget2.ide.bundle.js`; [`runtime/`](runtime/) builds `agGridWidget2.runtime.bundle.js`. Each has its own Vite config and TypeScript source.
- **Browser target:** the IDE emits a minified ES5 IIFE (Vite followed by Babel); runtime emits a minified ES2020 IIFE. Both are classic scripts for ThingWorx. The IDE retains ES2020 type libraries for APIs used by the recovered source; downleveling syntax does not polyfill APIs such as `WeakMap`.
- **Dependencies:** the runtime uses AG Grid Community and Enterprise 32.3.3, Day.js with AdvancedFormat and LocalizedFormat, JSZip, Font Awesome, and multiple-select from npm. See `src/ui/agGridWidget2/THIRD_PARTY_NOTICES.txt` for included library notices.
- **Change:** extension identity adjusted (vendor `US Ignite / Ryan Guild`, package version `1.0.1`, minimum ThingWorx `9.6.0`); widget name stays `agGridWidget2` so mashup bindings resolve unchanged.
- **Recovery:** the source in `ide/src` and `runtime/src` was recovered from the bundled code with webcrack. `decompiled/` retains focused excerpts for comparison. Recovered classes have TypeScript declarations for dynamic ThingWorx properties; the file uploader uses typed `async` methods. Both projects are checked by `tsc`.

From this directory:

```sh
npm ci
npm run typecheck
npm run build
npm run verify:parity
```

`npm run build` writes both single-file IIFE bundles under `build/vite/` and runs a smoke check that loads each bundle and verifies ThingWorx widget registration. The Gradle `packageExtension` task builds them and puts the generated files into the extension zip. The original bundles in `src/ui/agGridWidget2/` remain as a reference and are excluded from the zip.

`npm run verify:parity` rebuilds both bundles, then compares them with the original bundles in isolated ThingWorx shims. It checks IDE property metadata (including binding flags), services, and events; runtime property and service registration; accessor and binding behavior for edge values plus deterministic generated values; service dispatch; and event dispatch. The intentional `DateFormat` description update is checked separately. Set `PARITY_SEED` and `PARITY_ROUNDS` to reproduce or expand a run. This checks the widget contract and mocked property behavior, not AG Grid rendering or remote ThingWorx services.

`DateFormat` now uses Day.js formatting. AdvancedFormat and LocalizedFormat are enabled, so common Moment patterns such as `DD-MM-YYYY`, `Do`, and `L LT` work. Less common Moment tokens may render differently; the smoke check covers the supported examples.
