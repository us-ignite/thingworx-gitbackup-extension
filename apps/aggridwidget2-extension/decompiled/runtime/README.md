# Runtime widget code map

Only widget-specific code is retained here. The original webcrack output contained 177 runtime modules, including 137 Moment.js locales; those and other confirmed library modules have been removed. Start here:

| File | Widget code |
| --- | --- |
| [`widget-implementation.js`](widget-implementation.js) | Main `AgGridWidget2` runtime class: grid setup, property updates, events, selection, editing, services, and data flow. |
| [`custom-components.js`](custom-components.js) | Custom editors, renderers, tool panels, status panels, and loading overlay from the webpack entry code. |
| [`widget-constants.js`](widget-constants.js) | Runtime constants used by the widget. |
| [`widget.css`](widget.css) | Widget-specific styles, with [`check.svg`](check.svg) and [`close.svg`](close.svg). |
| [`7386.js`](7386.js) | Grid data builder: column definitions, row data, tree paths, filters, and aggregation. |
| [`2531.js`](2531.js) | Custom image renderer. |
| [`2982.js`](2982.js) | File uploader used by the image renderer. |
| [`3190.js`](3190.js) | Custom date renderer. |
| [`4528.js`](4528.js) | Date picker editor. |
| [`8988.js`](8988.js) | Custom JSON renderer. |
| [`9880.js`](9880.js) | Custom aggregate footer renderer. |

The focused files began as excerpts from webcrack's full `deobfuscated.js` output, which was removed after extraction because it duplicated the widget code alongside AG Grid and other libraries. The seven numbered files retain their original module IDs and include widget behavior; `2982.js`, `2531.js`, and `custom-components.js` now show the corresponding `async`/`await` source instead of TypeScript's emitted generator state machines. These files are for reading rather than direct execution. The complete executable bundles remain under `src/ui/agGridWidget2/`.
