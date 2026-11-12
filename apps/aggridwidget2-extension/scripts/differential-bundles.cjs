const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

// Minified baseline bundles otherwise print multi-megabyte source lines in stacks.
process.on('uncaughtException', error => {
  console.error(error.message.split('\n')[0]);
  process.exitCode = 1;
});

const extensionRoot = path.resolve(__dirname, '..');
const rounds = Number(process.env.PARITY_ROUNDS ?? 20);
const seed = Number(process.env.PARITY_SEED ?? 0x5eed);
assert.ok(Number.isSafeInteger(rounds) && rounds > 0, 'PARITY_ROUNDS must be a positive integer');
assert.ok(Number.isSafeInteger(seed), 'PARITY_SEED must be an integer');

function createHost() {
  function Widget() {
    this._props = {};
  }
  Widget.prototype.getProperty = function (key) { return this._props[key]; };
  Widget.prototype.setProperty = function (key, value) { this._props[key] = value; };
  Widget.prototype.destroy = function () {};

  // Supplying the runtime base avoids ThingWorx's internal constructor wrapper,
  // which depends on the real Mashup runtime to provide its internal state.
  function RuntimeWidget() {
    Widget.call(this);
    this.jqElement = { triggerHandler() {} };
  }
  RuntimeWidget.prototype = Object.create(Widget.prototype);
  RuntimeWidget.prototype.constructor = RuntimeWidget;

  const TW = { IDE: { Widget, Widgets: {} }, Widget, Runtime: { Widgets: {} } };
  const head = {
    style: {}, appendChild() {}, setAttribute() {}, getAttribute() { return null; },
  };
  const document = {
    baseURI: 'http://localhost/',
    documentElement: { style: {} },
    head,
    querySelector() { return head; },
    createTextNode(text) { return { textContent: text }; },
    createElement() {
      return {
        style: {}, appendChild() {}, setAttribute() {}, getAttribute() { return null; },
      };
    },
  };
  function $(element) { return { element }; }
  $.fn = {};
  $.extend = Object.assign;

  const quietConsole = { ...console, log() {}, table() {} };
  const context = {
    TW, TWRuntimeWidget: RuntimeWidget, document, $, Headers, URL, Symbol,
    console: quietConsole, setTimeout, clearTimeout,
    navigator: { userAgent: 'Mozilla/5.0' },
  };
  context.window = context;
  context.self = context;
  context.globalThis = context;
  return { TW, context };
}

function loadBundle(artifact, version) {
  const file = version === 'original'
    ? path.join(extensionRoot, 'src', 'ui', 'agGridWidget2', `agGridWidget2.${artifact}.bundle.js`)
    : path.join(extensionRoot, 'build', 'vite', artifact, `agGridWidget2.${artifact}.bundle.js`);
  const host = createHost();
  vm.runInNewContext(fs.readFileSync(file, 'utf8'), host.context, {
    filename: file,
    timeout: 20000,
  });
  const registry = artifact === 'ide' ? host.TW.IDE.Widgets : host.TW.Runtime.Widgets;
  assert.equal(typeof registry.AgGridWidget2, 'function', `${version} ${artifact} registration`);
  return { ...host, factory: registry.AgGridWidget2 };
}

function snapshot(value) {
  if (value === undefined) return { $undefined: true };
  if (typeof value === 'number' && Number.isNaN(value)) return { $nan: true };
  if (Array.isArray(value)) return Array.from(value, snapshot);
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.keys(value).map(key => [key, snapshot(value[key])]));
  }
  return value;
}

function compare(label, original, rewritten) {
  try {
    assert.deepStrictEqual(snapshot(rewritten), snapshot(original));
  } catch (error) {
    error.message = `${label}: ${error.message}`;
    throw error;
  }
}

function newIdeWidget(host) {
  return host.factory.call(Object.create({ constructor() {} }));
}

function metadata(widget) {
  return {
    properties: widget.widgetProperties(),
    services: widget.widgetServices(),
    events: widget.widgetEvents(),
  };
}

const originalIde = loadBundle('ide', 'original');
const rewrittenIde = loadBundle('ide', 'rewritten');
const originalIdeWidget = newIdeWidget(originalIde);
const rewrittenIdeWidget = newIdeWidget(rewrittenIde);
originalIdeWidget._props = {};
rewrittenIdeWidget._props = {};
const originalMetadata = metadata(originalIdeWidget);
const rewrittenMetadata = metadata(rewrittenIdeWidget);
const expectedDateFormatDescription = 'Day.js format to use to render date values (AdvancedFormat and LocalizedFormat plugins enabled); see https://day.js.org/docs/en/display/format';
assert.equal(rewrittenMetadata.properties.properties.DateFormat.description, expectedDateFormatDescription);
const comparableOriginalMetadata = structuredClone(originalMetadata);
// DateFormat documentation changed with the deliberate Moment-to-Day.js migration.
comparableOriginalMetadata.properties.properties.DateFormat.description = expectedDateFormatDescription;
compare('IDE metadata, including types, defaults, and binding flags', comparableOriginalMetadata, rewrittenMetadata);
for (const part of ['properties', 'services', 'events']) {
  const originalEntries = part === 'properties' ? originalMetadata.properties.properties : originalMetadata[part];
  const rewrittenEntries = part === 'properties' ? rewrittenMetadata.properties.properties : rewrittenMetadata[part];
  compare(`IDE ${part} order`, Object.keys(originalEntries), Object.keys(rewrittenEntries));
}

const originalRuntime = loadBundle('runtime', 'original');
const rewrittenRuntime = loadBundle('runtime', 'rewritten');
const originalRuntimeWidget = originalRuntime.factory();
const rewrittenRuntimeWidget = rewrittenRuntime.factory();
function runtimeMap(host, widget, name) {
  return widget[host.TW[`@@_${name}`]];
}
const originalProperties = runtimeMap(originalRuntime, originalRuntimeWidget, 'decoratedProperties');
const rewrittenProperties = runtimeMap(rewrittenRuntime, rewrittenRuntimeWidget, 'decoratedProperties');
const originalServices = runtimeMap(originalRuntime, originalRuntimeWidget, 'decoratedServices');
const rewrittenServices = runtimeMap(rewrittenRuntime, rewrittenRuntimeWidget, 'decoratedServices');
compare('runtime property bindings', originalProperties, rewrittenProperties);
compare('runtime service bindings', originalServices, rewrittenServices);
compare('runtime property order', Object.keys(originalProperties), Object.keys(rewrittenProperties));
compare('runtime service order', Object.keys(originalServices), Object.keys(rewrittenServices));

function prepare(widget) {
  const calls = [];
  widget.buildGrid = () => calls.push('buildGrid');
  widget.updateCustomServerSideStatusPanelRowCount = () => calls.push('updateRowCount');
  widget.widgetGridApi = {
    deselectAll: () => calls.push('deselectAll'),
    getStatusPanel: () => ({ updateRowCount: value => calls.push(['updateRowCount', value]) }),
  };
  widget.gridOptions = {};
  widget.jqElement = { triggerHandler: name => calls.push(`event:${name}`) };
  return calls;
}
const originalCalls = prepare(originalRuntimeWidget);
const rewrittenCalls = prepare(rewrittenRuntimeWidget);

// All values are JSON-like because ThingWorx bound properties are serialized.
// The sequence is deterministic so a failure can be repeated with one seed.
let state = seed >>> 0;
function random() {
  state ^= state << 13;
  state ^= state >>> 17;
  state ^= state << 5;
  return state >>> 0;
}
const values = [undefined, null, false, true, 0, 1, '', 'sample', [], [0, null], {}, { rows: [], dataShape: { fieldDefinitions: {} } }];
for (const [index, propertyName] of Object.keys(originalMetadata.properties.properties).entries()) {
  const value = values[index % values.length];
  originalIdeWidget[propertyName] = structuredClone(value);
  rewrittenIdeWidget[propertyName] = structuredClone(value);
  compare(`IDE accessor ${propertyName}`, originalIdeWidget[propertyName], rewrittenIdeWidget[propertyName]);
}
const ideBindingCalls = [0, 0];
originalIdeWidget.dataDidBind = () => { ideBindingCalls[0]++; };
rewrittenIdeWidget.dataDidBind = () => { ideBindingCalls[1]++; };
originalIdeWidget.afterAddBindingSource({ targetProperty: 'Data' });
rewrittenIdeWidget.afterAddBindingSource({ targetProperty: 'Data' });
compare('IDE Data binding callback', ideBindingCalls[0], ideBindingCalls[1]);
assert.equal(ideBindingCalls[0], 1, 'Data binding callback was invoked');

const propertyNames = Object.keys(originalProperties);
for (let round = 0; round < rounds + values.length; round++) {
  for (const [index, propertyName] of propertyNames.entries()) {
    originalCalls.length = 0;
    rewrittenCalls.length = 0;
    const value = round < values.length
      ? values[(round + index) % values.length]
      : values[random() % values.length];
    const originalValue = structuredClone(value);
    const rewrittenValue = structuredClone(value);
    originalRuntimeWidget.updateProperty({
      TargetProperty: propertyName,
      RawSinglePropertyValue: originalValue,
      SinglePropertyValue: originalValue,
    });
    rewrittenRuntimeWidget.updateProperty({
      TargetProperty: propertyName,
      RawSinglePropertyValue: rewrittenValue,
      SinglePropertyValue: rewrittenValue,
    });
    compare(`bound ${propertyName}, round ${round}`, originalRuntimeWidget[propertyName], rewrittenRuntimeWidget[propertyName]);
    compare(`binding effects for ${propertyName}, round ${round}`, originalCalls, rewrittenCalls);
    compare(`grid options after binding ${propertyName}, round ${round}`,
      originalRuntimeWidget.gridOptions, rewrittenRuntimeWidget.gridOptions);

    originalCalls.length = 0;
    rewrittenCalls.length = 0;
    const directValue = round < values.length
      ? values[(round + index + 5) % values.length]
      : values[random() % values.length];
    originalRuntimeWidget[propertyName] = structuredClone(directValue);
    rewrittenRuntimeWidget[propertyName] = structuredClone(directValue);
    compare(`direct ${propertyName}, round ${round}`, originalRuntimeWidget[propertyName], rewrittenRuntimeWidget[propertyName]);
    compare(`setter effects for ${propertyName}, round ${round}`, originalCalls, rewrittenCalls);
  }
}

// Replace service bodies to test dispatch, without invoking real grid or network operations.
for (const serviceName of Object.keys(originalServices)) {
  originalCalls.length = 0;
  rewrittenCalls.length = 0;
  const originalMethod = originalServices[serviceName];
  const rewrittenMethod = rewrittenServices[serviceName];
  originalRuntimeWidget[originalMethod] = () => originalCalls.push(`service:${originalMethod}`);
  rewrittenRuntimeWidget[rewrittenMethod] = () => rewrittenCalls.push(`service:${rewrittenMethod}`);
  originalRuntimeWidget.serviceInvoked(serviceName);
  rewrittenRuntimeWidget.serviceInvoked(serviceName);
  compare(`service ${serviceName}`, originalCalls, rewrittenCalls);
}
for (const eventName of Object.keys(originalMetadata.events)) {
  originalCalls.length = 0;
  rewrittenCalls.length = 0;
  assert.equal(typeof originalRuntimeWidget[eventName], 'function', `original event ${eventName}`);
  assert.equal(typeof rewrittenRuntimeWidget[eventName], 'function', `rewritten event ${eventName}`);
  originalRuntimeWidget[eventName]();
  rewrittenRuntimeWidget[eventName]();
  compare(`event ${eventName}`, originalCalls, rewrittenCalls);
}

console.log(`IDE: ${Object.keys(originalMetadata.properties.properties).length} properties, ` +
  `${Object.keys(originalMetadata.services).length} services, ${Object.keys(originalMetadata.events).length} events match`);
console.log(`Runtime: ${propertyNames.length} property bindings, ` +
  `${Object.keys(originalServices).length} service bindings, ` +
  `${values.length} edge-value rounds + ${rounds} generated rounds match (seed ${seed})`);
