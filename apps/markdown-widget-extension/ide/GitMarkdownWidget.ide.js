// Composer definition for GitMarkdownWidget. Plain ES5 with no dependencies, so it
// ships as written; the runtime bundle does the parsing and sanitising.
(function () {
  var style = document.createElement('style');
  style.textContent = '.git-markdown-placeholder{box-sizing:border-box;height:100%;padding:16px;' +
    'color:#536776;background:#f5f7fa;border:1px dashed #c2cbd3;font:14px/1.5 sans-serif;overflow:hidden}';
  document.head.appendChild(style);

  TW.IDE.Widgets.GitMarkdownWidget = function () {
    this.widgetProperties = function () {
      return {
        name: 'Git Markdown',
        description: 'Renders Markdown as sanitised HTML. Bind Markdown text directly, or bind an ' +
          'infotable to Data and name the field that holds the Markdown in ContentField.',
        category: ['Common'],
        supportsAutoResize: true,
        isResizable: true,
        properties: {
          Width: { defaultValue: 480 },
          Height: { defaultValue: 320 },
          Markdown: {
            description: 'Markdown text to render. Takes precedence over Data when it is not blank.',
            baseType: 'STRING',
            isBindingTarget: true,
            defaultValue: ''
          },
          Data: {
            description: 'Infotable whose first row holds the Markdown in ContentField, such as the ' +
              'Result of GetReadme or GetFileContent.',
            baseType: 'INFOTABLE',
            isBindingTarget: true
          },
          ContentField: {
            description: 'Field of the first Data row that holds the Markdown.',
            baseType: 'STRING',
            defaultValue: 'Content'
          },
          EmptyText: {
            description: 'Shown when there is no Markdown to render.',
            baseType: 'STRING',
            isBindingTarget: true,
            isLocalizable: true,
            defaultValue: 'Nothing to show.'
          }
        }
      };
    };

    this.widgetEvents = function () {
      return {};
    };

    this.renderHtml = function () {
      return '<div class="widget-content git-markdown-placeholder">Markdown</div>';
    };
  };
})();
