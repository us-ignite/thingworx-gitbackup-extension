// Recovered ThingWorx IDE widget and decorator support from the packaged bundle.
import widgetCss from "./widget.css?inline";
const style = document.createElement("style");
style.textContent = widgetCss;
document.head.appendChild(style);
class TWPropertyAspect {
  _key: any;
  _value: any;
  static aspectWithKeyAndValue(key, value) {
    let aspect = new TWPropertyAspect();
    aspect._key = key;
    aspect._value = value;
    return aspect;
  }
}
const bindingTarget = TWPropertyAspect.aspectWithKeyAndValue(
  "isBindingTarget",
  true,
);
const bindingSource = TWPropertyAspect.aspectWithKeyAndValue(
  "isBindingSource",
  true,
);
TWPropertyAspect.aspectWithKeyAndValue("isEditable", false);
TWPropertyAspect.aspectWithKeyAndValue("canBeUndefined", true);
TWPropertyAspect.aspectWithKeyAndValue("isVisible", false);
TWPropertyAspect.aspectWithKeyAndValue("isLocalizable", true);
TWPropertyAspect.aspectWithKeyAndValue("isLocalizable", "ModelTags");
function defaultValue(value) {
  return TWPropertyAspect.aspectWithKeyAndValue("defaultValue", value);
}
function selectOptions(value) {
  return TWPropertyAspect.aspectWithKeyAndValue("selectOptions", value);
}
function getSymbol(symbolDesc) {
  TW[symbolDesc] ||= window.Symbol ? window.Symbol(symbolDesc) : symbolDesc;
  return TW[symbolDesc];
}
const willSetSymbol = getSymbol("@@_willSet");
const didSetSymbol = getSymbol("@@_didSet");
const didBindSymbol = getSymbol("@@_didBind");
const versionSymbol = getSymbol("@@_version");
const _getInheritedProperty = function (proto, property) {
  if (proto.hasOwnProperty(property)) {
    return proto[property];
  }
  proto[property] = {};
  const superproto = Object.getPrototypeOf(proto);
  for (const key in superproto[property]) {
    proto[property][key] = superproto[property][key];
  }
  return proto[property];
};
const _getDecoratedProperties = (proto) =>
  _getInheritedProperty(proto, "_decoratedProperties");
const _getDecoratedWillSet = (proto) =>
  _getInheritedProperty(proto, willSetSymbol);
const _getDecoratedDidSet = (proto) =>
  _getInheritedProperty(proto, didSetSymbol);
const _getDecoratedDidBind = (proto) =>
  _getInheritedProperty(proto, didBindSymbol);
function property(baseType, ...args) {
  return function (target, key, descriptor?) {
    var setter;
    var hasDescriptor = descriptor !== undefined;
    if (!hasDescriptor) {
      descriptor = {};
    }
    if (descriptor.set) {
      var previousSetter = descriptor.set;
      setter = function (value) {
        this.setProperty(key, value);
        previousSetter.apply(this, arguments);
      };
    } else {
      setter = function (value) {
        this.setProperty(key, value);
      };
    }
    descriptor.set = setter;
    descriptor.get = function () {
      return this.getProperty(key);
    };
    if (!target.hasOwnProperty("_decoratedProperties")) {
      target._decoratedProperties = _getDecoratedProperties(target);
    }
    target._decoratedProperties[key] = {
      baseType,
      type: "property",
    };
    if (args) {
      for (const arg of args) {
        if (arg._key.startsWith("_")) {
          switch (arg._key) {
            case "_willSet":
              target[willSetSymbol] = _getDecoratedWillSet(target);
              target[willSetSymbol][key] = arg._value;
              break;
            case "_didSet":
              target[didSetSymbol] = _getDecoratedDidSet(target);
              target[didSetSymbol][key] = arg._value;
              break;
            case "_didBind":
              target[didBindSymbol] = _getDecoratedDidBind(target);
              target[didBindSymbol][key] = arg._value;
          }
        } else {
          target._decoratedProperties[key][arg._key] = arg._value;
        }
      }
    }
    if (!hasDescriptor) {
      Object.defineProperty(target, key, descriptor);
    }
  };
}
function service(target, key, descriptor?) {
  if (!target.hasOwnProperty("_decoratedProperties")) {
    target._decoratedProperties = _getDecoratedProperties(target);
  }
  target._decoratedProperties[key] = {
    type: "service",
  };
}
function widgetIDESupport_event(target, key, descriptor?) {
  if (!target.hasOwnProperty("_decoratedProperties")) {
    target._decoratedProperties = _getDecoratedProperties(target);
  }
  target._decoratedProperties[key] = {
    type: "event",
  };
}
function description(description) {
  return function (target, key?, descriptor?) {
    if (key) {
      target._decoratedProperties[key].description = description;
    } else {
      target._aspects.description = description;
    }
  };
}
class TWWidgetAspect {
  [key: string]: any;
  static aspectWithKeyAndValue(key, value) {
    let aspect = new TWWidgetAspect();
    aspect._key = key;
    aspect._value = value;
    return aspect;
  }
}
const autoResizable = TWWidgetAspect.aspectWithKeyAndValue(
  "supportsAutoResize",
  true,
);
TWWidgetAspect.aspectWithKeyAndValue("onlySupportedInResponsiveParents", true);
TWWidgetAspect.aspectWithKeyAndValue("isDraggable", true);
TWWidgetAspect.aspectWithKeyAndValue("userCannotRemove", true);
TWWidgetAspect.aspectWithKeyAndValue("isContainer", true);
TWWidgetAspect.aspectWithKeyAndValue("isContainerForOneResizeableWidget", true);
TWWidgetAspect.aspectWithKeyAndValue(
  "isContainerWithDeclarativeSpotsForSubWidgets",
  true,
);
TWWidgetAspect.aspectWithKeyAndValue("allowPositioning", false);
TWWidgetAspect.aspectWithKeyAndValue("allowPasteOrDrop", false);
TWWidgetAspect.aspectWithKeyAndValue("allowCopy", false);
TWWidgetAspect.aspectWithKeyAndValue("supportsLabel", true);
const TWWidgetFactory = function (widget) {
  return function () {
    return new widget(Object.getPrototypeOf(this));
  };
};
function TWWidgetDefinition(name, ...args) {
  return function (widget) {
    Object.defineProperty(widget, "prototype", {
      writable: false,
    });
    _getDecoratedProperties(widget.prototype);
    _getDecoratedWillSet(widget.prototype);
    _getDecoratedDidSet(widget.prototype);
    _getDecoratedDidBind(widget.prototype);
    const superclass = Object.getPrototypeOf(widget.prototype).constructor;
    let aspects: any = {};
    for (const key in superclass._aspects || {}) {
      aspects[key] = superclass._aspects[key];
    }
    if (args) {
      for (let arg of args) {
        aspects[arg._key] = arg._value;
      }
    }
    aspects.name = name;
    widget._aspects = aspects;
    TW.IDE.Widgets[widget.name] = TWWidgetFactory(widget);
  };
}
if (TW.IDE && typeof TW.IDE.Widget == "function") {
  (function () {
    let TWWidgetConstructor = TW.IDE.Widget;
    const afterLoadCreate = function () {
      const properties = this.allWidgetProperties().properties;
      for (const property in properties) {
        if (properties[property].displayName) {
          properties[property].name = properties[property].displayName;
        }
      }
      this.updatedProperties({
        updateUi: true,
      });
      if (this.jqElement) {
        this.updateProperties({
          updateUi: true,
        });
      }
    };
    let internalStates = new WeakMap();
    const constructor = function (proto) {
      let keys = Object.keys(this);
      let values = {};
      let self = this;
      keys.forEach((key) => (values[key] = self[key]));
      const __BMTWInternalState = proto;
      Object.keys(__BMTWInternalState).forEach((key) => {
        let value = __BMTWInternalState[key];
        if (typeof value == "function") {
          TWComposerWidget.prototype[key] ||= function () {
            return internalStates.get(this)[key].apply(this, arguments);
          };
          __BMTWInternalState[key] = value.bind(this);
          if (keys.indexOf(key) == -1) {
            delete this[key];
          } else {
            Object.defineProperty(this, key, {
              value: values[key],
              configurable: false,
              writable: true,
            });
          }
        }
      });
      internalStates.set(this, __BMTWInternalState);
      proto.constructor.apply(this, []);
      Object.keys(__BMTWInternalState).forEach((key) => {
        let value = __BMTWInternalState[key];
        let state = __BMTWInternalState;
        if (typeof value != "function") {
          let writable = key !== "properties";
          const configurable = [
            "active",
            "selected",
            "eventHelper",
            "visible",
            "properties",
          ].includes(key);
          if (keys.indexOf(key) != -1) {
            Object.defineProperty(this, key, {
              value: values[key],
              configurable,
              writable,
            });
          } else {
            Object.defineProperty(this, key, {
              value: state[key],
              configurable,
              writable,
            });
          }
        }
      });
    };
    if (window.TWComposerWidget) {
      if (
        !TWComposerWidget.prototype[versionSymbol] ||
        TWComposerWidget.prototype[versionSymbol] < 5
      ) {
        if (TWComposerWidget.prototype[versionSymbol] < 5) {
          const proto = TWComposerWidget.prototype;
          window.TWComposerWidget = constructor;
          TWComposerWidget.prototype = proto;
        }
        let prototype = {
          widgetProperties() {
            if (this.constructor._aspects) {
              let result: any = {};
              for (let aspect in this.constructor._aspects) {
                result[aspect] = this.constructor._aspects[aspect];
              }
              result.properties = {};
              if (this._decoratedProperties) {
                for (let property in this._decoratedProperties) {
                  if (this._decoratedProperties[property].type == "property") {
                    result.properties[property] =
                      this._decoratedProperties[property];
                  }
                }
              }
              return result;
            }
          },
          widgetServices() {
            var result: any = {};
            if (this._decoratedProperties) {
              for (let property in this._decoratedProperties) {
                if (this._decoratedProperties[property].type == "service") {
                  result[property] = this._decoratedProperties[property];
                }
              }
            }
            return result;
          },
          widgetEvents() {
            var result: any = {};
            if (this._decoratedProperties) {
              for (let property in this._decoratedProperties) {
                if (this._decoratedProperties[property].type == "event") {
                  result[property] = this._decoratedProperties[property];
                }
              }
            }
            return result;
          },
          afterLoad: afterLoadCreate,
          afterCreate: afterLoadCreate,
          beforeSetProperty(key, value) {
            if (this[willSetSymbol] && key in this[willSetSymbol]) {
              return this[this[willSetSymbol][key]](value);
            }
          },
          afterSetProperty(key, value) {
            if (this[didSetSymbol] && key in this[didSetSymbol]) {
              return this[this[didSetSymbol][key]](value);
            }
          },
          afterAddBindingSource(info) {
            if (
              this[didBindSymbol] &&
              info.targetProperty in this[didBindSymbol]
            ) {
              return this[this[didBindSymbol][info.targetProperty]](info);
            }
          },
          destroy() {
            Object.defineProperty(this, "properties", {
              configurable: true,
              writable: true,
              value: this.properties,
            });
            TWWidgetConstructor.prototype.destroy.apply(this, arguments);
          },
        };
        TWComposerWidget.prototype.widgetProperties =
          prototype.widgetProperties;
        TWComposerWidget.prototype.widgetEvents = prototype.widgetEvents;
        TWComposerWidget.prototype.widgetServices = prototype.widgetServices;
        TWComposerWidget.prototype.afterLoad = prototype.afterLoad;
        TWComposerWidget.prototype.afterCreate = prototype.afterCreate;
        TWComposerWidget.prototype.beforeSetProperty =
          prototype.beforeSetProperty;
        TWComposerWidget.prototype.afterSetProperty =
          prototype.afterSetProperty;
        TWComposerWidget.prototype.afterAddBindingSource =
          prototype.afterAddBindingSource;
        TWComposerWidget.prototype.destroy = prototype.destroy;
        TWComposerWidget.prototype[versionSymbol] = 5;
        Object.defineProperty(window.TWComposerWidget, "prototype", {
          writable: false,
        });
      }
    } else {
      window.TWComposerWidget = constructor;
      TWComposerWidget.prototype = Object.assign(
        Object.create(TWWidgetConstructor.prototype),
        {
          widgetProperties() {
            if (this.constructor._aspects) {
              let result: any = {};
              for (let aspect in this.constructor._aspects) {
                result[aspect] = this.constructor._aspects[aspect];
              }
              result.properties = {};
              if (this._decoratedProperties) {
                for (let property in this._decoratedProperties) {
                  if (this._decoratedProperties[property].type == "property") {
                    result.properties[property] =
                      this._decoratedProperties[property];
                  }
                }
              }
              return result;
            }
          },
          widgetServices() {
            var result: any = {};
            if (this._decoratedProperties) {
              for (let property in this._decoratedProperties) {
                if (this._decoratedProperties[property].type == "service") {
                  result[property] = this._decoratedProperties[property];
                }
              }
            }
            return result;
          },
          widgetEvents() {
            var result: any = {};
            if (this._decoratedProperties) {
              for (let property in this._decoratedProperties) {
                if (this._decoratedProperties[property].type == "event") {
                  result[property] = this._decoratedProperties[property];
                }
              }
            }
            return result;
          },
          afterLoad: afterLoadCreate,
          afterCreate: afterLoadCreate,
          beforeSetProperty(key, value) {
            if (this[willSetSymbol] && key in this[willSetSymbol]) {
              return this[this[willSetSymbol][key]](value);
            }
          },
          afterSetProperty(key, value) {
            if (this[didSetSymbol] && key in this[didSetSymbol]) {
              return this[this[didSetSymbol][key]](value);
            }
          },
          afterAddBindingSource(info) {
            if (
              this[didBindSymbol] &&
              info.targetProperty in this[didBindSymbol]
            ) {
              return this[this[didBindSymbol][info.targetProperty]](info);
            }
          },
          destroy() {
            Object.defineProperty(this, "properties", {
              configurable: true,
              writable: true,
              value: this.properties,
            });
            TWWidgetConstructor.prototype.destroy.apply(this, arguments);
          },
          [versionSymbol]: 5,
        },
      );
      Object.defineProperty(window.TWComposerWidget, "prototype", {
        writable: false,
      });
    }
  })();
}


@description("Browse repository history")
@TWWidgetDefinition("Git History", autoResizable)
class GitHistoryWidget extends TWComposerWidget {
  [key: string]: any;
  renderHtml() { return '<div class="widget-content git-history-placeholder">Git history</div>'; }
  @property("INFOTABLE", bindingTarget) declare Commits: any;
  @property("INFOTABLE", bindingTarget) declare Branches: any;
  @property("INFOTABLE", bindingTarget) declare Tags: any;
  @property("INFOTABLE", bindingTarget) declare CommitInfo: any;
  @property("INFOTABLE", bindingTarget) declare TreeEntries: any;
  @property("INFOTABLE", bindingTarget) declare FileContent: any;
  @property("STRING", bindingTarget) declare Repository: any;
  @property("STRING", bindingTarget) declare Diff: any;
  @property("STRING", bindingSource, bindingTarget) declare SelectedCommitID: any;
  @property("STRING", bindingSource, bindingTarget) declare SelectedFilePath: any;
  @property("STRING", bindingSource, bindingTarget) declare SelectedRef: any;
  @property("INTEGER", bindingSource, bindingTarget, defaultValue(100)) declare MaxEntries: any;
  @property("BOOLEAN", bindingSource, bindingTarget, defaultValue(false)) declare AllBranches: any;
  @widgetIDESupport_event declare CommitSelected: any;
  @widgetIDESupport_event declare FileSelected: any;
  @widgetIDESupport_event declare RefreshRequested: any;
  @widgetIDESupport_event declare LoadMoreRequested: any;
}
