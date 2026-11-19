// Git History IDE widget. Decorators come from typescriptwebpacksupport.
import widgetCss from "./widget.css?inline";
const style = document.createElement("style");
style.textContent = widgetCss;
document.head.appendChild(style);
import {
  TWWidgetDefinition,
  autoResizable,
  bindingSource,
  bindingTarget,
  defaultValue,
  description,
  event,
  property,
} from "typescriptwebpacksupport/widgetIDESupport";


@description("Browse repository history")
@TWWidgetDefinition("Git History", autoResizable)
class GitHistoryWidget extends TWComposerWidget {
  [key: string]: any;
  // Optional in practice: Composer falls back to its defaults when these are absent.
  declare widgetIconUrl: () => string;
  declare afterRender: () => void;
  declare beforeDestroy: () => void;
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
  @description("GetCurrentBranch result, used to mark the commit HEAD points at")
  @property("INFOTABLE", bindingTarget) declare CurrentBranch: any;
  @event declare CommitSelected: any;
  @event declare FileSelected: any;
  @event declare RefreshRequested: any;
  @event declare LoadMoreRequested: any;
  @description("Fires when the user asks to check out the selected commit (SelectedCommitID)")
  @event declare CheckoutRequested: any;
  @description("Comma-separated IDs of the commits ticked for squashing, newest first")
  @property("STRING", bindingSource) declare SquashCommitIDs: any;
  @description("Message for the squashed commit, as the user edited it")
  @property("STRING", bindingSource) declare SquashMessage: any;
  @description("Fires when the user asks to squash the ticked commits (SquashCommitIDs, SquashMessage)")
  @event declare SquashRequested: any;
}
