import { LitElement, html, css } from 'lit';
import { state, property } from 'lit/decorators.js';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { TwxService, twx } from './lib/twx-service.js';
import { WidgetBridge } from './lib/widget-bridge.js';
import { GitElementBase } from './components/git-base.js';

declare const __GW_VERSION__: string;

(window as any).__GW = {
  version: __GW_VERSION__,
  LitElement,
  html,
  css,
  state,
  property,
  unsafeHTML,
  TwxService,
  twx,
  WidgetBridge,
  GitElementBase,
};
