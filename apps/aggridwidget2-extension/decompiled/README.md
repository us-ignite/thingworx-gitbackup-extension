# Decompiled AG Grid widget bundles

These files were recovered from the packaged JavaScript bundles with [webcrack](https://github.com/j4k0xb/webcrack) 2.16.0. Library modules, webpack/style-loader code, and full-bundle copies have been removed. This is reference material, not the original TypeScript source or a buildable replacement for the bundles in `src/ui/agGridWidget2/`.

| File | Contents |
| --- | --- |
| [`ide/widget-implementation.js`](ide/widget-implementation.js) | Focused excerpt containing the IDE widget class, its properties, events, and services. |
| [`runtime/widget-implementation.js`](runtime/widget-implementation.js) | Focused excerpt containing the runtime widget class and behavior. |
| [`runtime/custom-components.js`](runtime/custom-components.js) | Focused excerpt containing custom editors, renderers, and panels. |
| [`runtime/README.md`](runtime/README.md) | Map of the remaining widget-specific runtime files. |
| `ide/widget.css`, `runtime/widget.css` | Widget styles extracted from css-loader modules. |

Webcrack found 9 IDE modules and 177 runtime modules. Of the runtime modules, 137 were Moment.js locales. The widget classes were in the webpack entry code, so they were copied into focused files before the full output was removed. Seven runtime modules with widget-specific behavior remain under their original numeric IDs. These files reference libraries and helpers in the original bundles, so they are not standalone scripts. Generated identifiers and TypeScript compilation patterns remain where the bundles do not preserve the original names or structure.

To inspect the complete bundles again, run webcrack from the repository root into a temporary directory with a compatible Node.js version (22 or 24):

```sh
npm exec --yes --package=node@24 --package=webcrack@2.16.0 -- webcrack apps/aggridwidget2-extension/src/ui/agGridWidget2/agGridWidget2.ide.bundle.js -o /tmp/aggrid-ide
npm exec --yes --package=node@24 --package=webcrack@2.16.0 -- webcrack apps/aggridwidget2-extension/src/ui/agGridWidget2/agGridWidget2.runtime.bundle.js -o /tmp/aggrid-runtime
```

The focused excerpts were taken from the resulting entry code. The packaged bundles and their third-party notices remain in `src/ui/agGridWidget2/`.
