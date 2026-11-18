// Runtime for GitMarkdownWidget: Markdown -> HTML with marked, then DOMPurify.
// README files come from a Git remote, so treat them as untrusted: scripts,
// event handlers, forms, styles and element ids are removed. Ids are removed so
// headings cannot match the mashups' [id$="..."] layout selectors.
import DOMPurify from 'dompurify';
import { Marked } from 'marked';
import widgetCss from './widget.css?inline';

const style = document.createElement('style');
style.textContent = widgetCss;
document.head.appendChild(style);

const marked = new Marked({ gfm: true, breaks: false, async: false });
const purifier = DOMPurify(window);
const ABSOLUTE = /^(https?:|mailto:)/i;
const IMAGE_SOURCE = /^(https:|data:image\/(png|gif|jpe?g|webp);)/i;

purifier.addHook('afterSanitizeAttributes', node => {
  const element = node as Element;
  if (element.tagName === 'A') {
    const href = element.getAttribute('href') || '';
    if (href.startsWith('#') && href.length > 1) {
      // The runtime routes on the URL hash, so in-page anchors are scrolled by script.
      element.setAttribute('data-anchor', decodeURIComponent(href.slice(1)).toLowerCase());
      element.setAttribute('href', '#');
    } else if (ABSOLUTE.test(href)) {
      element.setAttribute('target', '_blank');
      element.setAttribute('rel', 'noopener noreferrer');
    } else if (href) {
      // A path inside the repository has no URL on this server.
      element.removeAttribute('href');
      element.setAttribute('title', 'Repository path: ' + href);
      element.setAttribute('data-gm', 'local-link');
    }
  } else if (element.tagName === 'IMG') {
    const src = element.getAttribute('src') || '';
    if (!IMAGE_SOURCE.test(src)) {
      const alt = element.getAttribute('alt') || src;
      const placeholder = element.ownerDocument.createElement('span');
      placeholder.setAttribute('data-gm', 'missing-image');
      placeholder.textContent = alt ? '[image: ' + alt + ']' : '[image]';
      placeholder.title = 'Images are shown only from https URLs: ' + src;
      element.replaceWith(placeholder);
    } else {
      element.setAttribute('loading', 'lazy');
      element.setAttribute('referrerpolicy', 'no-referrer');
    }
  }
});

/** GitHub-style heading anchor: lower case, punctuation dropped, spaces to hyphens. */
export const slug = (text: string): string =>
  text.trim().toLowerCase().replace(/[^\p{L}\p{N}\s_-]/gu, '').replace(/\s/g, '-');

/** Renders Markdown into sanitised HTML. */
export const renderMarkdown = (markdown: string): string =>
  // Inputs are removed below, so show GFM task-list boxes as text.
  purifier.sanitize((marked.parse(markdown) as string)
    .replace(/<input (?=[^>]*type="checkbox")[^>]*>/g, box => (/\schecked/.test(box) ? '☑' : '☐')), {
    USE_PROFILES: { html: true },
    FORBID_TAGS: ['style', 'form', 'input', 'button', 'textarea', 'select', 'iframe', 'object', 'embed'],
    FORBID_ATTR: ['id', 'name', 'style', 'class'],
  });

const rows = (value: any): any[] => (value && Array.isArray(value.rows) ? value.rows : []);

TW.Runtime.Widgets.GitMarkdownWidget = function (this: any) {
  let root: HTMLElement | undefined;
  let received = false;

  this.renderHtml = () => '<div class="widget-content git-markdown"></div>';

  this.afterRender = () => {
    const host: HTMLElement = this.jqElement[0];
    root = host.matches('.git-markdown') ? host : (host.querySelector('.git-markdown') as HTMLElement);
    root.addEventListener('click', event => {
      const link = (event.target as Element).closest('a[data-anchor]');
      if (!link || !root) return;
      event.preventDefault();
      const anchor = link.getAttribute('data-anchor');
      const heading = Array.from(root.querySelectorAll('[data-heading]'))
        .find(h => h.getAttribute('data-heading') === anchor);
      if (heading) heading.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
    received = !!this.getProperty('Markdown');
    render();
  };

  this.updateProperty = (info: any) => {
    const name = info.TargetProperty;
    if (name === 'Data') this.setProperty('Data', info.RawSinglePropertyValue);
    else if (name === 'Markdown' || name === 'EmptyText') this.setProperty(name, info.SinglePropertyValue);
    else return;
    if (name !== 'EmptyText') received = true;
    render();
  };

  this.beforeDestroy = () => {
    if (root) root.replaceChildren();
    root = undefined;
  };

  const markdownText = (): string => {
    const direct = this.getProperty('Markdown');
    if (direct) return String(direct);
    const row = rows(this.getProperty('Data'))[0];
    const field = this.getProperty('ContentField') || 'Content';
    return row && row[field] != null ? String(row[field]) : '';
  };

  const render = () => {
    if (!root) return;
    const markdown = markdownText();
    if (!markdown.trim()) {
      root.classList.add('gm-empty');
      root.textContent = received ? this.getProperty('EmptyText') || 'Nothing to show.' : '';
      return;
    }
    root.classList.remove('gm-empty');
    root.innerHTML = renderMarkdown(markdown);
    const seen = new Map<string, number>();
    root.querySelectorAll('h1, h2, h3, h4, h5, h6').forEach(heading => {
      const base = slug(heading.textContent || '');
      const count = seen.get(base) || 0;
      seen.set(base, count + 1);
      heading.setAttribute('data-heading', count ? `${base}-${count}` : base);
    });
  };
};
