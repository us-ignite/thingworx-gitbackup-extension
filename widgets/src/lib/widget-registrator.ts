declare const TW: any;

export interface WidgetConfig {
  elementName: string;
  name: string;
  description?: string;
  category?: string[];
  properties?: Record<string, any>;
  events?: Record<string, any>;
  services?: Record<string, any>;
  flags?: Record<string, boolean>;
}

export function registerRuntimeWidget<T extends HTMLElement>(
  config: WidgetConfig,
  propMap: Record<string, string> = {},
): void {
  const elementName = config.elementName;
  const widgetName = elementName.replace(/-/g, '').toLowerCase();
  if (TW?.Runtime?.Widgets?.[widgetName]) return;

  TW.Runtime.Widgets[widgetName] = function () {
    let _el: T | null = null;

    this.renderHtml = function () {
      return `<${elementName}></${elementName}>`;
    };

    this.afterRender = function () {
      _el = this.jqElement?.[0]?.firstElementChild ?? null;
      if (_el) {
        for (const [prop, attr] of Object.entries(propMap)) {
          const val = this.getProperty?.(prop);
          if (val !== undefined) (_el as any)[attr] = val;
        }
      }
    };

    this.updateProperty = function (info: { TargetProperty: string; SinglePropertyValue: unknown }) {
      if (!_el) return;
      const attr = propMap[info.TargetProperty];
      if (attr) (_el as any)[attr] = info.SinglePropertyValue;
    };

    this.beforeDestroy = function () { _el = null; };
  };
}

export function registerIdeWidget(config: WidgetConfig, iconUrl?: string): void {
  const widgetName = config.elementName.replace(/-/g, '').toLowerCase();
  if (TW?.IDE?.Widgets?.[widgetName]) return;

  TW.IDE.Widgets[widgetName] = function () {
    this.widgetIconUrl = function () { return iconUrl ?? null; };
    this.widgetProperties = function () {
      return {
        name: config.name,
        description: config.description || '',
        category: config.category || ['Common'],
        properties: config.properties || {},
        events: config.events || {},
        services: config.services || {},
        flags: config.flags || {},
      };
    };
    this.renderHtml = function () {
      const esc = (s: string) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
      return `<div style="padding:32px;border:1px dashed #ccc;border-radius:4px;text-align:center;color:#999;font-family:sans-serif;font-size:14px">${esc(config.name)}</div>`;
    };
    this.afterSetProperty = function () { return true; };
    this.beforeDestroy = function () {};
  };
}
