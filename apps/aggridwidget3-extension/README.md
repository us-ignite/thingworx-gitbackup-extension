# agGridWidget3

US Ignite rebuild of the Sabel Systems `agGridWidget2` ThingWorx widget extension, as two TypeScript/Vite projects. It is renamed to `agGridWidget3` so it installs alongside, and never collides with, the original `agGridWidget2` or other copies of it.

- **Source artifact:** Sabel Systems `agGridWidget2-min-100.1.20.zip` (package version `100.1.20`, dated 2025-05-12).
- **Projects:** [`ide/`](ide/) builds `agGridWidget3.ide.bundle.js`; [`runtime/`](runtime/) builds `agGridWidget3.runtime.bundle.js`. Each has its own Vite config and TypeScript source.
- **Browser target:** the IDE emits a minified ES5 IIFE (Vite followed by Babel); runtime emits a minified ES2020 IIFE. Both are classic scripts for ThingWorx. The IDE retains ES2020 type libraries for APIs used by the recovered source; downleveling syntax does not polyfill APIs such as `WeakMap`.
- **Dependencies:** the runtime uses AG Grid Community and Enterprise 32.3.3, Day.js with AdvancedFormat and LocalizedFormat, typescriptwebpacksupport, JSZip, Font Awesome, and multiple-select from npm. See `extension/ui/agGridWidget3/THIRD_PARTY_NOTICES.txt` for included library notices.
- **Change:** extension identity adjusted (package `agGridWidget3`, widget type `AgGridWidget3`, vendor `US Ignite / Ryan Guild`, package version `1.0.1`, minimum ThingWorx `9.6.0`). The GIT mashups use the `AgGridWidget3` type.
- **Recovery:** the source in `ide/src` and `runtime/src` was recovered from the bundled code with webcrack; the inlined widget decorator support was replaced with the `typescriptwebpacksupport` package it came from. Recovered classes have TypeScript declarations for dynamic ThingWorx properties; the file uploader uses typed `async` methods. Both projects are checked by `tsc`.

From this directory:

```sh
npm ci
npm run typecheck
npm run build
```

`npm run build` writes both single-file IIFE bundles under `build/vite/` and runs a smoke check that loads each bundle and verifies ThingWorx widget registration. The Gradle `packageExtension` task builds them and puts the generated files into the extension zip.

The `SelectAllFiltered` and `DeselectAll` services are additions in agGridWidget3: `SelectAllFiltered` selects every row that passes the current filters, and `DeselectAll` clears the selection. Each publishes `SelectedRows` once rather than once per row.

`DateFormat` now uses Day.js formatting. AdvancedFormat and LocalizedFormat are enabled, so common Moment patterns such as `DD-MM-YYYY`, `Do`, and `L LT` work. Less common Moment tokens may render differently; the smoke check covers the supported examples.
