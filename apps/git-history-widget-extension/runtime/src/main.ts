import widgetCss from "./widget.css?inline";
const style = document.createElement("style"); style.textContent = widgetCss; document.head.appendChild(style);
function getSymbol(symbolDesc) {
  window.TW[symbolDesc] ||= window.Symbol
    ? window.Symbol(symbolDesc)
    : symbolDesc;
  return window.TW[symbolDesc];
}
const willBindSymbol = getSymbol("@@_willBind");
const didBindSymbol = getSymbol("@@_didBind");
const decoratedPropertiesSymbol = getSymbol("@@_decoratedProperties");
const decoratedServicesSymbol = getSymbol("@@_decoratedServices");
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
  _getInheritedProperty(proto, decoratedPropertiesSymbol);
const _getDecoratedServices = (proto) =>
  _getInheritedProperty(proto, decoratedServicesSymbol);
const _getDecoratedWillBind = (proto) =>
  _getInheritedProperty(proto, willBindSymbol);
const _getDecoratedDidBind = (proto) =>
  _getInheritedProperty(proto, didBindSymbol);
function property(...args): any {
  let name;
  let aspectsIndex;
  const decorator = (target, key, descriptor) => {
    let setter;
    const hasDescriptor = descriptor !== undefined;
    if (!hasDescriptor) {
      descriptor = {};
    }
    name ||= key;
    if (descriptor.set) {
      const previousSetter = descriptor.set;
      setter = function (value) {
        this.setProperty(name, value);
        previousSetter.apply(this, arguments);
      };
    } else {
      setter = function (value) {
        this.setProperty(name, value);
      };
    }
    descriptor.set = setter;
    descriptor.get = function () {
      return this.getProperty(name);
    };
    _getDecoratedProperties(target)[name] = key;
    if (aspectsIndex !== undefined) {
      for (let i = aspectsIndex; i < args.length; i++) {
        const aspect = args[i];
        switch (aspect._key) {
          case "_willBind":
            _getDecoratedWillBind(target)[name] = aspect._value;
            break;
          case "_didBind":
            _getDecoratedDidBind(target)[name] = aspect._value;
        }
      }
    }
    if (!hasDescriptor) {
      Object.defineProperty(target, key, descriptor);
    }
  };
  if (typeof args[0] == "string") {
    name = args[0];
    aspectsIndex = 1;
    return decorator;
  } else if (args[0]._key && args[0]._value) {
    aspectsIndex = 0;
    return decorator;
  } else {
    name = args[1];
    decorator.apply(this, arguments);
    return;
  }
}
function service(arg1, ..._decoratorArgs: any[]): any {
  let name = arg1;
  const decorator = (target, key, descriptor) => {
    _getDecoratedServices(target)[name] = key;
  };
  if (typeof arg1 == "string") {
    return decorator;
  }
  name = arguments[1];
  decorator.apply(this, arguments);
}
function widgetRuntimeSupport_event(arg1, ..._decoratorArgs: any[]): any {
  let name = arg1;
  const decorator = (target, key, descriptor) => {
    const event = function () {
      this.jqElement.triggerHandler(name);
    };
    if (descriptor) {
      descriptor.get = function () {
        return event;
      };
      descriptor.writable = false;
    } else {
      Object.defineProperty(target, key, {
        get: () => event,
      });
    }
  };
  if (typeof arg1 == "string") {
    return decorator;
  }
  name = arguments[1];
  decorator.apply(this, arguments);
}
function TWWidgetDefinition(arg1): any {
  let name;
  const decorator = function (widget) {
    Object.defineProperty(widget, "prototype", {
      writable: false,
    });
    _getDecoratedProperties(widget.prototype);
    _getDecoratedServices(widget.prototype);
    _getDecoratedWillBind(widget.prototype);
    _getDecoratedDidBind(widget.prototype);
    TW.Runtime.Widgets[name] = function () {
      return new widget();
    };
  };
  if (typeof arg1 == "string") {
    name = arg1;
    return decorator;
  }
  name = arg1.name;
  decorator(arg1);
}
if (typeof TW.Widget == "function") {
  (function () {
    let __BMTWInternalState;
    let TWWidgetConstructor = TW.Widget;
    if (window.TWRuntimeWidget) {
      if (
        !TWRuntimeWidget.prototype[versionSymbol] ||
        TWRuntimeWidget.prototype[versionSymbol] < 4
      ) {
        let prototype = {
          updateProperty(info) {
            const decoratedWillBind = this[willBindSymbol];
            const value = info.RawSinglePropertyValue;
            let shouldUpdate = true;
            if (info.TargetProperty in decoratedWillBind) {
              shouldUpdate = this[decoratedWillBind[info.TargetProperty]](
                value,
                info,
              );
            }
            const decoratedProperties = this[decoratedPropertiesSymbol];
            if (shouldUpdate && info.TargetProperty in decoratedProperties) {
              const currentValue = this.getProperty(info.TargetProperty);
              this[decoratedProperties[info.TargetProperty]] = value;
              const decoratedDidBind = this[didBindSymbol];
              if (info.TargetProperty in decoratedDidBind) {
                this[decoratedDidBind[info.TargetProperty]](currentValue, info);
              }
            }
          },
          serviceInvoked(name) {
            const decoratedServices = this[decoratedServicesSymbol];
            if (name in decoratedServices) {
              this[decoratedServices[name]]();
            }
          },
        };
        TWRuntimeWidget.prototype.updateProperty = prototype.updateProperty;
        TWRuntimeWidget.prototype.serviceInvoked = prototype.serviceInvoked;
        TWRuntimeWidget.prototype[versionSymbol] = 4;
      }
      return;
    }
    TW.Widget = function () {
      TWWidgetConstructor.apply(this, arguments);
      __BMTWInternalState = this;
      return this;
    };
    TW.Widget.prototype = TWWidgetConstructor.prototype;
    Object.keys(TWWidgetConstructor).forEach((key) => {
      TW.Widget[key] = TWWidgetConstructor[key];
    });
    let internalStates = new WeakMap();
    window.TWRuntimeWidget = function () {
      internalStates.set(this, __BMTWInternalState);
      Object.keys(__BMTWInternalState).forEach((key) => {
        let value = __BMTWInternalState[key];
        if (typeof value == "function") {
          TWRuntimeWidget.prototype[key] ||= function () {
            return internalStates.get(this)[key].apply(this, arguments);
          };
          __BMTWInternalState[key] = value.bind(this);
        } else {
          this[key] = value;
        }
      });
      __BMTWInternalState = undefined;
    };
    TWRuntimeWidget.prototype = Object.assign(
      Object.create(TWWidgetConstructor.prototype),
      {
        updateProperty(info) {
          const decoratedWillBind = this[willBindSymbol];
          const value = info.RawSinglePropertyValue;
          let shouldUpdate = true;
          if (info.TargetProperty in decoratedWillBind) {
            shouldUpdate = this[decoratedWillBind[info.TargetProperty]](
              value,
              info,
            );
          }
          const decoratedProperties = this[decoratedPropertiesSymbol];
          if (shouldUpdate && info.TargetProperty in decoratedProperties) {
            const currentValue = this.getProperty(info.TargetProperty);
            this[decoratedProperties[info.TargetProperty]] = value;
            const decoratedDidBind = this[didBindSymbol];
            if (info.TargetProperty in decoratedDidBind) {
              this[decoratedDidBind[info.TargetProperty]](currentValue, info);
            }
          }
        },
        serviceInvoked(name) {
          const decoratedServices = this[decoratedServicesSymbol];
          if (name in decoratedServices) {
            this[decoratedServices[name]]();
          }
        },
        [versionSymbol]: 4,
      },
    );
  })();
}

let VISIBLE_INDEX_POSITIONS;
import { buildLaneLayout, buildSvgPrimitives, renderToSvgString } from 'graph-lab';
import hljs from 'highlight.js/lib/core';
import diffLanguage from 'highlight.js/lib/languages/diff';
import xmlLanguage from 'highlight.js/lib/languages/xml';
import jsLanguage from 'highlight.js/lib/languages/javascript';
import jsonLanguage from 'highlight.js/lib/languages/json';
import javaLanguage from 'highlight.js/lib/languages/java';
hljs.registerLanguage('diff', diffLanguage); hljs.registerLanguage('xml', xmlLanguage);
hljs.registerLanguage('javascript', jsLanguage); hljs.registerLanguage('json', jsonLanguage); hljs.registerLanguage('java', javaLanguage);
const rows = (value: any): any[] => value && Array.isArray(value.rows) ? value.rows : [];
const element = (tag: string, cls: string, text = ''): HTMLElement => { const e = document.createElement(tag); e.className = cls; e.textContent = text; return e; };
@TWWidgetDefinition
class GitHistoryWidget extends TWRuntimeWidget {
  [key: string]: any;
  root: HTMLElement; tab = 'Content'; query = '';
  renderHtml() { return '<div class="widget-content git-history"></div>'; }
  afterRender() {
    const host = this.jqElement[0];
    this.root = host.matches('.git-history') ? host : host.querySelector('.git-history');
    this.render();
  }
  updateProperty(info: any) {
    if (info.TargetProperty === 'Repository' && this.Repository !== info.RawSinglePropertyValue) {
      this.SelectedCommitID = ''; this.SelectedFilePath = ''; this.SelectedRef = ''; this.AllBranches = false; this.Commits = null; this.CommitInfo = null;
      this.TreeEntries = null; this.FileContent = null; this.Diff = '';
    }
    super.updateProperty(info); if (this.root) this.render();
  }
  beforeDestroy() { if (this.root) this.root.replaceChildren(); }
  emit(name: string) { this.jqElement.triggerHandler(name); }
  renderTree(entries: any[]) {
    const top: any = { folders: new Map(), files: [] };
    for (const item of entries) {
      const parts = String(item.Path).split('/'); let node = top;
      for (const part of parts.slice(0, -1)) {
        if (!node.folders.has(part)) node.folders.set(part, { folders: new Map(), files: [] });
        node = node.folders.get(part);
      }
      node.files.push(item);
    }
    const build = (node: any): HTMLElement => {
      const container = element('div', 'gh-tree');
      for (const [name, child] of node.folders) {
        const folder = document.createElement('details'); folder.open = true;
        const summary = document.createElement('summary'); summary.textContent = name; folder.append(summary, build(child)); container.append(folder);
      }
      for (const file of node.files) {
        const button = element('button', 'gh-tree-file', file.Name || file.Path);
        button.onclick = () => this.chooseFile(file.Path); container.append(button);
      }
      return container;
    };
    return build(top);
  }
  chooseCommit(id: string) { this.SelectedCommitID = id; this.SelectedFilePath = ''; this.CommitInfo = null; this.TreeEntries = null; this.FileContent = null; this.Diff = ''; this.render(); this.emit('CommitSelected'); }
  chooseFile(path: string) { this.SelectedFilePath = path; this.FileContent = null; this.Diff = ''; this.render(); this.emit('FileSelected'); }
  render() {
    const root = this.root; if (!root) return; root.replaceChildren();
    const toolbar = element('div','gh-toolbar');
    const search = document.createElement('input'); search.placeholder='Search commits'; search.value=this.query;
    search.onchange = () => { this.query=search.value.toLowerCase(); this.render(); }; toolbar.append(search);
    const branches = document.createElement('select');
    const def = document.createElement('option'); def.value=''; def.textContent='Current branch'; branches.append(def);
    for (const b of rows(this.Branches)) { const o=document.createElement('option'); o.value=b.BranchName; o.textContent=b.ShortBranchName || b.BranchName; branches.append(o); }
    branches.value=this.SelectedRef || ''; branches.onchange=()=>{this.SelectedRef=branches.value;this.emit('RefreshRequested');}; toolbar.append(branches);
    const all=document.createElement('input'); all.type='checkbox'; all.checked=!!this.AllBranches; all.onchange=()=>{this.AllBranches=all.checked;this.emit('RefreshRequested');};
    const label=element('label','','All branches'); label.prepend(all); toolbar.append(label);
    const refresh=element('button','','Refresh'); refresh.onclick=()=>this.emit('RefreshRequested'); toolbar.append(refresh);
    const more=element('button','','Load more'); more.onclick=()=>{this.MaxEntries=(Number(this.MaxEntries)||100)+100;this.emit('LoadMoreRequested');}; toolbar.append(more); root.append(toolbar);
    const body=element('div','gh-body'); const history=element('div','gh-history'); const list=element('div','gh-list');
    const commits=rows(this.Commits); const graph=element('div','gh-graph');
    if (commits.length) { try { const timeline=commits.map(c=>({id:String(c.CommitID),predecessorIds:String(c.ParentCommitIDs||'').split(',').filter(Boolean)})); const primitives=buildSvgPrimitives(buildLaneLayout(timeline),{rowHeight:56,showLabels:false},timeline); graph.style.width=`${Math.max(40, primitives.graphWidth)}px`; graph.innerHTML=renderToSvgString(primitives); const svg=graph.querySelector('svg'); if(svg){svg.style.transform='translateY(-28px) scaleY(-1)'; svg.style.display='block';} } catch(e) { console.error('Git history graph',e); } }
    list.append(graph); const commitRows=element('div','gh-rows');
    for (const c of commits) {const b=element('button','gh-commit'+(c.CommitID===this.SelectedCommitID?' selected':'')); const labels=[...rows(this.Branches).filter(b=>b.CommitID===c.CommitID).map(b=>b.ShortBranchName),...rows(this.Tags).filter(t=>t.CommitID===c.CommitID).map(t=>t.TagName)].filter(Boolean); const title=element('strong','',(labels.length ? '['+labels.join(', ')+'] ' : '')+(c.CommitName||'(no message)')); const date=c.CommitTime ? new Date(c.CommitTime).toLocaleString() : ''; const meta=element('small','',`${c.AuthorName||''} · ${date} · ${String(c.CommitID).slice(0,8)}`); if(this.query && !(`${c.CommitName} ${c.AuthorName} ${c.CommitID}`.toLowerCase().includes(this.query))) b.style.opacity='0.3'; b.append(title,meta); b.onclick=()=>this.chooseCommit(c.CommitID); commitRows.append(b); }
    if(!commits.length) commitRows.append(element('div','gh-empty','Select a repository to view its history.')); list.append(commitRows); history.append(list); body.append(history);
    const detail=element('div','gh-details'); const info=rows(this.CommitInfo)[0];
    if(!this.SelectedCommitID) detail.append(element('div','gh-empty','Select a commit to inspect its files.'));
    else if (!info || info.CommitID !== this.SelectedCommitID) detail.append(element('div','gh-empty','Loading commit…'));
    else {detail.append(element('h3','',info.CommitDescription||'Commit'),element('small','',`${info.Author||''} · ${info.Date ? new Date(info.Date).toLocaleString() : ''} · ${String(info.CommitID).slice(0,8)}`));
      const changed=rows(info.ChangedFiles); if(changed.length){detail.append(element('h4','','Changed files')); const files=element('div','gh-files'); for(const f of changed){const b=element('button','',`${f.Status||''} ${f.FileName}`); b.onclick=()=>this.chooseFile(f.FileName); files.append(b);} detail.append(files);}
      const tree=rows(this.TreeEntries); if(tree.length){detail.append(element('h4','','Files in commit')); detail.append(this.renderTree(tree));}
      if(this.SelectedFilePath){detail.append(element('h4','',this.SelectedFilePath)); const tabs=element('div','gh-tabs'); for(const name of ['Content','Diff']) {const b=element('button',this.tab===name?'active':'',name); b.onclick=()=>{this.tab=name;this.render();}; tabs.append(b);} detail.append(tabs); const file=rows(this.FileContent)[0]; const pre=element('pre','gh-code'); if(this.tab==='Diff'){pre.textContent=this.Diff||'No diff available.'; if(this.Diff) pre.innerHTML=hljs.highlight(this.Diff,{language:'diff'}).value;} else if(file && file.Path===this.SelectedFilePath){if(file.Binary) pre.textContent='Binary file'; else {const text=file.Content||''; const ext=this.SelectedFilePath.split('.').pop().toLowerCase(); const lang=({js:'javascript',ts:'javascript',xml:'xml',html:'xml',json:'json',java:'java'} as any)[ext]; pre.textContent=text+(file.Truncated?'\n[Content truncated]':''); if(lang && !file.Truncated) pre.innerHTML=hljs.highlight(text,{language:lang}).value;}} else if(changed.some(f=>f.FileName===this.SelectedFilePath && f.Status==='DELETE')) pre.textContent='This file was deleted in this commit.'; else pre.textContent='Loading file…'; detail.append(pre);}
    } body.append(detail); root.append(body);
  }
  @property declare Commits: any;
  @property declare Branches: any;
  @property declare Tags: any;
  @property declare CommitInfo: any;
  @property declare TreeEntries: any;
  @property declare FileContent: any;
  @property declare Repository: any;
  @property declare Diff: any;
  @property declare SelectedCommitID: any;
  @property declare SelectedFilePath: any;
  @property declare SelectedRef: any;
  @property declare MaxEntries: any;
  @property declare AllBranches: any;
}
