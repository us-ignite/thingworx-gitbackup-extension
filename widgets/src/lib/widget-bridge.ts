import { TwxService } from './twx-service.js';

declare const TW: any;

export interface WidgetConfig {
  elementName: string;
  properties?: Record<string, { src?: string }>;
  events?: Record<string, { src?: string }>;
  services?: Record<string, { method?: string }>;
}

export class WidgetBridge {
  private widget: any;
  private element: HTMLElement | null = null;
  private config: WidgetConfig;
  private service: TwxService;
  private propertyValues: Record<string, unknown> = {};

  constructor(widgetInstance: any, config: WidgetConfig) {
    this.widget = widgetInstance;
    this.config = config;
    this.service = new TwxService();
  }

  attach(): void {
    this.element = this.widget.jqElement?.[0]?.firstElementChild || null;
    if (!this.element) return;

    // Sync initial properties
    if (this.config.properties) {
      for (const [propName, propCfg] of Object.entries(this.config.properties)) {
        const attrName = propCfg.src || propName.charAt(0).toLowerCase() + propName.slice(1);
        const val = this.propertyValues[propName] ?? this.widget.getProperty?.(propName);
        if (val !== undefined && this.element) {
          (this.element as any)[attrName] = val;
        }
      }
    }

    // Forward Lit events to ThingWorx
    if (this.config.events) {
      for (const [eventName, eventCfg] of Object.entries(this.config.events)) {
        const domEvent = eventCfg.src || eventName.toLowerCase();
        this.element?.addEventListener(domEvent, (e: Event) => {
          const detail = (e as CustomEvent).detail;
          if (detail && typeof detail === 'object') {
            for (const [key, value] of Object.entries(detail)) {
              this.widget.setProperty?.(key, value);
            }
          }
          this.widget.fireEvent?.(eventName);
        });
      }
    }
  }

  updateProperty(info: { TargetProperty: string; SinglePropertyValue: unknown }): void {
    this.propertyValues[info.TargetProperty] = info.SinglePropertyValue;
    if (!this.element) return;
    const propCfg = this.config.properties?.[info.TargetProperty];
    const attrName = propCfg?.src || info.TargetProperty.charAt(0).toLowerCase() + info.TargetProperty.slice(1);
    (this.element as any)[attrName] = info.SinglePropertyValue;
  }

  async callService(serviceName: string, params: Record<string, unknown> = {}): Promise<unknown> {
    const gitThing = this.propertyValues['GitThing'] || this.widget.getProperty?.('GitThing');
    if (gitThing) {
      return this.service.invokeService(gitThing as string, serviceName, params);
    }
    throw new Error('GitThing not set');
  }
}
