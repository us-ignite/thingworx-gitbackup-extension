# GitMarkdownWidget

ThingWorx widget extension that renders Markdown as sanitised HTML. `GIT.Mashup.Repository` uses it
to show a repository's README. The runtime parses with [marked](https://marked.js.org) and cleans
the HTML with [DOMPurify](https://github.com/cure53/DOMPurify); both are pinned in `package-lock.json`.

| Property | Type | Effect |
| --- | --- | --- |
| `Markdown` | STRING | Markdown to render. Used instead of `Data` when not blank. |
| `Data` | INFOTABLE | First row's `ContentField` is rendered, for example the `Result` of `GetReadme`. |
| `ContentField` | STRING | Field of `Data` that holds the Markdown. Default `Content`. |
| `EmptyText` | STRING | Shown when there is no Markdown. Nothing is shown before the first value arrives. |

README files come from a Git remote, so the widget treats them as untrusted:

- Scripts, event handlers, forms, iframes, `style` tags, and `id`, `name`, `class` and `style`
  attributes are removed. Removing ids keeps headings from matching the mashups' `[id$="..."]` selectors.
- `http`, `https` and `mailto` links open in a new tab with `rel="noopener noreferrer"`.
  `#anchor` links scroll to the heading by script, because the ThingWorx runtime routes on the URL hash.
  Links to other repository paths have no URL on the server, so they are shown as text with the path in the tooltip.
- Images load only from `https` or inline `data:image` URLs, without a referrer. Other images
  (including relative paths) are replaced by their alt text.

Run `npm ci` and `npm run build` (which runs `scripts/smoke-bundles.cjs`), or
`./gradlew :apps:markdown-widget-extension:packageExtension` from the repository root. The JGit
`packageExtensionPack` and `devSetup` tasks collect this ZIP with the other widget dependencies.
