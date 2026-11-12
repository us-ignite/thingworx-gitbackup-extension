@TWWidgetDefinition
class GitResultPopover extends TypescriptWidget {
  @property(bindingTarget) gitMessage!: STRING;
  @twevent resolveRequested!: TWEvent;
  private resultPopover?: HTMLElement;

  beforeDestroy() {
    this.resultPopover?.remove();
  }

  @service showGitResult() {
    if (!this.gitMessage) return;
    this.resultPopover?.remove();
    const popover = document.createElement("div");
    popover.setAttribute("popover", "manual");
    popover.setAttribute("role", "dialog");
    popover.setAttribute("aria-label", "Git operation result");
    Object.assign(popover.style, {
      position: "fixed", top: "5rem", right: "1rem", left: "auto", bottom: "auto",
      margin: "0", width: "min(34rem, calc(100vw - 2rem))", maxHeight: "min(60vh, 30rem)",
      overflow: "auto", padding: "1rem", background: "white", color: "#222",
      border: "1px solid #aaa", borderRadius: "4px", boxShadow: "0 4px 16px #0003",
      zIndex: "10000", boxSizing: "border-box"
    });
    const close = document.createElement("button");
    close.type = "button";
    close.textContent = "Close";
    close.setAttribute("aria-label", "Close Git operation result");
    close.style.float = "right";
    close.addEventListener("click", () => { popover.remove(); this.resultPopover = undefined; });
    const title = document.createElement("strong");
    title.textContent = "Git operation result";
    const message = document.createElement("p");
    message.style.whiteSpace = "pre-wrap";
    message.style.overflowWrap = "anywhere";
    message.textContent = this.gitMessage;
    popover.append(close, title, message);
    if (this.gitMessage.includes("[CONFLICT]")) {
      const resolve = document.createElement("button");
      resolve.type = "button";
      resolve.textContent = "Resolve conflicts";
      resolve.addEventListener("click", () => { popover.remove(); this.resultPopover = undefined; this.resolveRequested(); });
      popover.appendChild(resolve);
    }
    document.body.appendChild(popover);
    this.resultPopover = popover;
    if (typeof popover.showPopover === "function") popover.showPopover();
    else popover.style.display = "block";
  }

}
