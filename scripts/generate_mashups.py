#!/usr/bin/env python3
"""
Generate ThingWorx Mashup XML files for the GitBackup extension.

Produces three mashups:
  1. GitBackupExtensionSettings – user settings + GPG keys
  2. GitRepo                  – parameterized per-repo dashboard (left-nav + content)
  3. GitNewRepo               – create-repo wizard

All use PTC Convergence Theme widgets (flexcontainer, ptcslabel, ptcsbutton, ...).
"""

import json
import os

ENTITIES_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "Entities")


# ── helpers ────────────────────────────────────────────────────────────────

def _uid(prefix, n):
    """Deterministic widget uid."""
    return f"{prefix}-{n:04d}"


def w(props, children=None):
    """Build a widget dict."""
    wd = {"Properties": props}
    if children:
        wd["Widgets"] = children
    return wd


def container(name, children, flex_direction="column", flex_grow=1, **kw):
    p = {
        "Type": "flexcontainer",
        "__TypeDisplayName": "Container",
        "Id": name,
        "Area": "mashup-root",
        "DisplayName": name,
        "LastContainer": "__container__",
        "Margin": "0 0 0 0",
        "ResponsiveLayout": True,
        "ShowDataLoading": False,
        "UseTheme": True,
        "Visible": True,
        "Z-index": 1,
        "EnableExpandCollapse": False,
        "Expanded": True,
        "ShowExpandCollapseTab": False,
        "SourceURL": "",
        "align-content": "flex-start",
        "align-items": "flex-start",
        "flex-basis": "auto",
        "flex-direction": flex_direction,
        "flex-grow": flex_grow,
        "flex-max-height": "",
        "flex-max-width": "",
        "flex-min-height": "",
        "flex-min-width": "",
        "flex-shrink": 1,
        "flex-size": "default",
        "flex-wrap": "nowrap",
        "iconClass": "widgets-flexcontainer",
        "justify-content": "flex-start",
        "positioning": "static",
    }
    p.update(kw)
    return w(p, children)


def label(name, text, **kw):
    p = {
        "Type": "ptcslabel",
        "__TypeDisplayName": "Label",
        "Id": name,
        "Area": "mashup-root",
        "DisplayName": name,
        "LabelText": text,
        "LabelType": "label",
        "HorizontalAlignment": "left",
        "MultiLine": False,
        "PreserveWhiteSpace": False,
        "VerticalAlignment": "flex-start",
        "DisclosureControl": "show-more",
        "UseTheme": True,
        "Margin": "0 0 0 0",
        "ShowDataLoading": False,
        "Visible": True,
        "Z-index": 1,
        "Height": "",
        "Width": "",
        "Left": "",
        "Top": "",
    }
    p.update(kw)
    return w(p)


def textfield(name, label_text="", placeholder="", **kw):
    p = {
        "Type": "ptcstextfield",
        "__TypeDisplayName": "Text Field",
        "Id": name,
        "Area": "mashup-root",
        "DisplayName": name,
        "Text": "",
        "Placeholder": placeholder,
        "Label": label_text,
        "LabelAlignment": "left",
        "Disabled": False,
        "ReadOnly": False,
        "UseTheme": True,
        "Margin": "0 0 0 0",
        "ShowDataLoading": False,
        "Visible": True,
        "Z-index": 1,
        "Height": "",
        "Width": "",
        "Left": "",
        "Top": "",
        "RequiredMessage": "A value is required",
        "ShowValidationCriteria": False,
        "ShowValidationFailure": False,
        "ShowValidationSuccess": False,
        "ValidationCriteriaIcon": "cds:icon_info",
        "ValidationFailureIcon": "cds:icon_error",
        "ValidationState": "undefined",
        "ValidationSuccessIcon": "cds:icon_success",
        "ValueRequired": False,
    }
    p.update(kw)
    return w(p)


def textarea(name, label_text="", **kw):
    p = {
        "Type": "ptcstextarea",
        "__TypeDisplayName": "TextArea",
        "Id": name,
        "Area": "mashup-root",
        "DisplayName": name,
        "Text": "",
        "Label": label_text,
        "LabelAlignment": "left",
        "Disabled": False,
        "ReadOnly": False,
        "UseTheme": True,
        "Margin": "0 0 0 0",
        "ShowDataLoading": False,
        "Visible": True,
        "Z-index": 1,
        "Height": "",
        "Width": "",
        "Left": "",
        "Top": "",
        "Counter": False,
        "CriteriaMessage": "",
        "FillContainer": False,
        "MaxNumberOfCharacters": 1000000,
        "TextAlignment": "left",
        "Counter": False,
    }
    p.update(kw)
    return w(p)


def button(name, label_text, **kw):
    p = {
        "Type": "ptcsbutton",
        "__TypeDisplayName": "Button",
        "Id": name,
        "Area": "mashup-root",
        "DisplayName": name,
        "Label": label_text,
        "ButtonType": "secondary",
        "MultiLine": True,
        "UseTheme": True,
        "Disabled": False,
        "Margin": "0 0 0 0",
        "ShowDataLoading": False,
        "Visible": True,
        "Z-index": 1,
        "Height": "",
        "Width": "",
        "Left": "",
        "Top": "",
    }
    p.update(kw)
    return w(p)


def checkbox(name, label_text, **kw):
    p = {
        "Type": "checkbox",
        "__TypeDisplayName": "Checkbox",
        "Id": name,
        "Area": "mashup-root",
        "DisplayName": name,
        "Label": label_text,
        "Checked": False,
        "UseTheme": True,
        "Margin": "0 0 0 0",
        "ShowDataLoading": False,
        "Visible": True,
        "Z-index": 1,
        "Height": "",
        "Width": "",
        "Left": "",
        "Top": "",
    }
    p.update(kw)
    return w(p)


def grid(name, columns, data_service="", **kw):
    cols = []
    for c in columns:
        col_def = {
            "__TypeDisplayName": "Column",
            "id": c["id"],
            "label": c.get("label", c["id"]),
            "binding": {"fieldName": c["field"]},
        }
        if "action" in c:
            col_def["action"] = c["action"]
        cols.append(col_def)

    p = {
        "Type": "ptcsgrid",
        "__TypeDisplayName": "Grid Advanced",
        "Id": name,
        "Area": "mashup-root",
        "DisplayName": name,
        "UseTheme": True,
        "Margin": "0 0 0 0",
        "ShowDataLoading": False,
        "Visible": True,
        "Z-index": 1,
        "Height": "",
        "Width": "",
        "Left": "",
        "Top": "",
        "AutoScroll": True,
        "ColumnsMenuOptions": "none",
        "DisplayOnlyMode": False,
        "FilterHintText": "[[filter]]",
        "FilterWidth": 273,
        "FocusNavigationMode": "row-first",
        "HeaderVerticalAlignment": "top",
        "HideHeaderRow": False,
        "IDFieldName": "ID",
        "IsEditable": False,
        "Label": None,
        "LabelAlignment": "left",
        "LabelType": "sub-header",
        "LegacyConfiguration": "",
        "MaxHeaderHeight": 100,
        "MinRowHeight": 30,
        "ReorderColumns": False,
        "ResizeColumns": True,
        "RowSelection": "none",
        "ShowColumnFeature": True,
        "ShowFilter": True,
        "ShowFooter": False,
        "ShowResetButton": True,
        "ShowRowNumbers": False,
        "SingleLineHeader": True,
        "SingleLineRows": False,
        "SortSelectionColumn": False,
        "WasMigrated": True,
        "columns": json.dumps(cols),
    }
    p.update(kw)
    return w(p)


def dropdown(name, label_text="", **kw):
    p = {
        "Type": "dropdown",
        "__TypeDisplayName": "Dropdown",
        "Id": name,
        "Area": "mashup-root",
        "DisplayName": name,
        "Label": label_text,
        "UseTheme": True,
        "Margin": "0 0 0 0",
        "ShowDataLoading": False,
        "Visible": True,
        "Z-index": 1,
        "Height": "",
        "Width": "",
    }
    p.update(kw)
    return w(p)


def navigation(name, target_mashup, params=None):
    """Non-visual navigation function widget."""
    p = {
        "Type": "navigation",
        "__TypeDisplayName": "Navigation",
        "Id": name,
        "Area": "mashup-root",
        "DisplayName": name,
        "TargetMashup": target_mashup,
        "MashupParameters": json.dumps(params or {}),
    }
    return w(p)


# ── Extension Settings ──────────────────────────────────────────────────

def build_extension_settings():
    return container("root", [
        label("section-title-user", "User Settings", Height="30"),
        container("user-settings-form", [
            textfield("committer-name", "Committer Name", Height="40"),
            textfield("committer-email", "Committer Email", Height="40"),
            checkbox("use-global-values", "Use these values for all repositories"),
            button("save-user-settings", "Save Settings"),
        ]),
        label("section-title-gpg", "GPG Keys", Height="30"),
        grid("gpg-keys-grid", [
            {"id": "repo", "label": "Repository", "field": "GitThing"},
            {"id": "fingerprint", "label": "Fingerprint", "field": "GpgKeyFingerprint"},
            {"id": "signing", "label": "Signing", "field": "SignCommits"},
            {"id": "edit", "label": "", "field": "", "action": "edit"},
            {"id": "delete", "label": "", "field": "", "action": "delete"},
        ]),
        container("gpg-form", [
            dropdown("gpg-repo-dropdown", "Repository Thing"),
            textarea("gpg-private-key", "Private Key", Height="80"),
            textfield("gpg-passphrase", "Passphrase"),
            textfield("gpg-fingerprint", "Fingerprint"),
            checkbox("gpg-sign-commits", "Sign commits with this key"),
            container("gpg-buttons", [
                button("verify-gpg-key", "Verify Key"),
                button("save-gpg-key", "Save Key"),
            ], flex_direction="row", flex_grow=0),
        ]),
    ])


# ── Repo Details (parameterized) ────────────────────────────────────────

def build_repo_details():
    return container("root", [
        container("header", [
            label("repo-title", "Repository: {{gitThing}}"),
        ], flex_direction="row", flex_grow=0, Height="40"),
        container("body", [
            # Left nav
            container("nav-panel", [
                button("nav-commit-push", "Commit & Push", ButtonType="primary"),
                button("nav-pull", "Pull"),
                button("nav-branch", "Branch"),
                button("nav-status", "Status"),
                button("nav-settings", "Settings"),
                button("nav-log", "Log"),
                button("nav-delete", "Delete Repo"),
            ], flex_grow=0, flex_size="fixed", flex_min_width="160"),
            # Right content
            container("content-panel", [
                # Commit & Push section
                container("section-commit-push", [
                    label("cp-title", "Commit & Push"),
                    textarea("commit-msg", "Commit Message", Height="80"),
                    grid("status-grid", [
                        {"id": "sel", "label": "", "field": "selected", "action": "checkbox"},
                        {"id": "file", "label": "File", "field": "File"},
                        {"id": "status", "label": "Status", "field": "Status"},
                    ]),
                    container("cp-buttons", [
                        button("refresh-status", "Refresh"),
                        button("commit-push-btn", "Commit and Push"),
                    ], flex_direction="row", flex_grow=0),
                ]),
                # Pull section
                container("section-pull", [
                    label("pull-title", "Pull"),
                    button("pull-btn", "Pull from Remote"),
                ]),
                # Branch section
                container("section-branch", [
                    label("branch-title", "Branch Manager"),
                    container("branch-create", [
                        textfield("new-branch-name", "New Branch Name"),
                        button("create-checkout-btn", "Create and Checkout"),
                        button("create-only-btn", "Create Only"),
                    ], flex_direction="row", flex_grow=0),
                    grid("branch-grid", [
                        {"id": "branch-name", "label": "Branch", "field": "ShortBranchName"},
                        {"id": "head", "label": "HEAD", "field": "BranchType"},
                        {"id": "type", "label": "Type", "field": "BranchType"},
                        {"id": "checkout", "label": "", "field": "", "action": "checkout"},
                        {"id": "delete", "label": "", "field": "", "action": "delete"},
                    ]),
                ]),
                # Status section
                container("section-status", [
                    label("status-title", "Working Tree Status"),
                    grid("wt-status-grid", [
                        {"id": "file-path", "label": "File Path", "field": "File"},
                        {"id": "status-code", "label": "Status", "field": "Status"},
                    ]),
                    button("refresh-wt-status", "Refresh"),
                ]),
                # Settings section
                container("section-settings", [
                    label("settings-title", "Repository Settings"),
                    label("settings-cred-title", "Credentials"),
                    textfield("settings-user", "Username"),
                    textfield("settings-pass", "Password/Token"),
                    textfield("settings-email", "Email"),
                    textfield("settings-fullname", "Full Name"),
                    button("save-cred", "Save Credentials"),
                    label("settings-config-title", "Configuration"),
                    textfield("settings-project", "Project Name"),
                    button("save-project", "Save Project Name"),
                    label("settings-gpg-title", "GPG Signing Key"),
                    dropdown("settings-gpg-dropdown", "Signing Key"),
                    checkbox("settings-gpg-sign", "Sign commits"),
                    button("save-gpg", "Save GPG Selection"),
                ]),
                # Log section
                container("section-log", [
                    label("log-title", "Activity Log"),
                    container("log-filters", [
                        textfield("log-search", "Search"),
                        dropdown("log-service-filter", "Service"),
                    ], flex_direction="row", flex_grow=0),
                    grid("log-grid", [
                        {"id": "time", "label": "Time", "field": "timestamp"},
                        {"id": "user", "label": "User", "field": "User"},
                        {"id": "service", "label": "Service", "field": "ServiceName"},
                        {"id": "source", "label": "Source", "field": "Source"},
                        {"id": "content", "label": "Content", "field": "Content"},
                    ]),
                    button("refresh-log", "Refresh"),
                ]),
                # Delete section
                container("section-delete", [
                    label("delete-title", "Delete Repository"),
                    label("delete-warning", "This action permanently removes the repository Thing and its local folder. This cannot be undone."),
                    button("delete-repo-btn", "Delete This Repository"),
                ]),
            ]),
        ], flex_direction="row"),
    ])


# ── New Repo Wizard ─────────────────────────────────────────────────────

def build_new_repo():
    return container("root", [
        label("new-repo-title", "Create New Git Repository"),
        # Repository section
        label("section-repo", "Repository", Height="30"),
        container("repo-section", [
            textfield("repo-name", "Thing Name"),
            textfield("repo-url", "Git Remote URL"),
            textfield("repo-path", "Local Path"),
            textfield("repo-file-repo", "File Repository"),
            textfield("repo-project", "Project Name"),
            textfield("repo-initial-branch", "Initial Branch"),
        ]),
        # Auth section
        label("section-auth", "Authentication", Height="30"),
        container("auth-section", [
            textfield("auth-user", "Username"),
            textfield("auth-pass", "Password / Token"),
        ]),
        # Commit author section
        label("section-author", "Commit Author", Height="30"),
        container("author-section", [
            textfield("author-name", "Name"),
            textfield("author-email", "Email"),
        ]),
        # Proxy section
        label("section-proxy", "Proxy", Height="30"),
        container("proxy-section", [
            checkbox("use-proxy", "Use Proxy"),
            textfield("proxy-url", "Proxy URL"),
            textfield("proxy-port", "Port"),
        ]),
        # Actions
        label("section-actions", "Actions", Height="30"),
        container("actions-section", [
            button("create-repo-btn", "Create Repository"),
            button("enable-thing-btn", "Enable Thing"),
            button("start-thing-btn", "Start Thing"),
        ]),
        # Result
        container("result-section", [
            label("result-success", "", Visible=False),
            label("result-error", "", Visible=False),
        ]),
    ])


# ── XML builder ──────────────────────────────────────────────────────────

def mashup_xml(name, mashup_json, description="", project="GitBackup"):
    """Wrap mashup JSON in the ThingWorx export XML envelope."""
    json_str = json.dumps(mashup_json, indent=2)
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<Entities
 majorVersion="9"
 minorVersion="3"
 universal="password">
    <Mashups>
        <Mashup
         aspect.isEditableExtensionObject="true"
         aspect.isExtension="true"
         description="{description}"
         documentationContent=""
         homeMashup=""
         name="{name}"
         projectName="{project}"
         tags="">
            <avatar></avatar>
            <DesignTimePermissions>
                <Create></Create>
                <Read></Read>
                <Update></Update>
                <Delete></Delete>
                <Metadata></Metadata>
            </DesignTimePermissions>
            <RunTimePermissions></RunTimePermissions>
            <VisibilityPermissions>
                <Visibility></Visibility>
            </VisibilityPermissions>
            <ConfigurationTableDefinitions></ConfigurationTableDefinitions>
            <ConfigurationTables></ConfigurationTables>
            <mashupContent><![CDATA[
{json_str}
]]></mashupContent>
        </Mashup>
    </Mashups>
</Entities>"""


def mashup_xml_no_param(name, mashup_json, description="", project="GitBackup"):
    """Same but without MashupParameters (for non-parameterized mashups)."""
    json_str = json.dumps(mashup_json, indent=2)
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<Entities
 majorVersion="9"
 minorVersion="3"
 universal="password">
    <Mashups>
        <Mashup
         aspect.isEditableExtensionObject="true"
         aspect.isExtension="true"
         description="{description}"
         documentationContent=""
         homeMashup=""
         name="{name}"
         projectName="{project}"
         tags="">
            <avatar></avatar>
            <DesignTimePermissions>
                <Create></Create>
                <Read></Read>
                <Update></Update>
                <Delete></Delete>
                <Metadata></Metadata>
            </DesignTimePermissions>
            <RunTimePermissions></RunTimePermissions>
            <VisibilityPermissions>
                <Visibility></Visibility>
            </VisibilityPermissions>
            <ConfigurationTableDefinitions></ConfigurationTableDefinitions>
            <ConfigurationTables></ConfigurationTables>
            <mashupContent><![CDATA[
{json_str}
]]></mashupContent>
        </Mashup>
    </Mashups>
</Entities>"""


# ── Main ─────────────────────────────────────────────────────────────────

def main():
    # Mashup 1: Extension Settings (no parameter)
    ext_settings_ui = {
        "UI": {
            "Properties": {
                "Name": "GitBackupExtensionSettings",
                "StyleTheme": "PTC Convergence Theme",
                "ResponsiveLayout": True,
                "UseTheme": False,
                "UseMasterTheme": False,
            },
            "Widgets": [build_extension_settings()],
        },
        "Services": {
            "DataServices": [
                {
                    "name": "GetGitUserExtensionProperties",
                    "entityName": "GITBACKUP.Utility.Thing",
                    "serviceName": "GetGitUserExtensionsProperties",
                    "autoRefresh": True,
                    "refreshInterval": 0,
                },
                {
                    "name": "GetGpgKeys",
                    "entityName": "GITBACKUP.Utility.Thing",
                    "serviceName": "GetGpgKeys",
                    "autoRefresh": True,
                    "refreshInterval": 0,
                },
            ],
        },
    }

    # Mashup 2: Repo Details (parameterized with gitThing)
    repo_ui = {
        "UI": {
            "Properties": {
                "Name": "GitRepo",
                "StyleTheme": "PTC Convergence Theme",
                "ResponsiveLayout": True,
                "UseTheme": False,
                "UseMasterTheme": False,
            },
            "Widgets": [build_repo_details()],
        },
        "Services": {
            "DataServices": [
                {
                    "name": "Status",
                    "entityName": "$mashupParameter/gitThing",
                    "serviceName": "Status",
                    "autoRefresh": True,
                    "refreshInterval": 0,
                },
                {
                    "name": "GetBranchList",
                    "entityName": "$mashupParameter/gitThing",
                    "serviceName": "GetBranchList",
                    "autoRefresh": True,
                    "refreshInterval": 0,
                },
            ],
        },
        "Functions": {
            "NonVisualWidgets": [
                navigation("nav-to-repo", "GitRepo", {"gitThing": "__NEW_THING__"}),
            ],
        },
    }

    # Mashup 3: New Repo (no parameter)
    new_repo_ui = {
        "UI": {
            "Properties": {
                "Name": "GitNewRepo",
                "StyleTheme": "PTC Convergence Theme",
                "ResponsiveLayout": True,
                "UseTheme": False,
                "UseMasterTheme": False,
            },
            "Widgets": [build_new_repo()],
        },
        "Services": {},
    }

    # Write files
    files = [
        ("Mashups_GitBackupExtensionSettings.xml",
         mashup_xml_no_param("GitBackupExtensionSettings", ext_settings_ui,
                            "Extension-wide GitBackup settings: committer identity and GPG key management")),
        ("Mashups_GitRepo.xml",
         mashup_xml("GitRepo", repo_ui,
                    "Per-repository Git operations dashboard: commit, push, pull, branch, status, settings, log, delete")),
        ("Mashups_GitNewRepo.xml",
         mashup_xml_no_param("GitNewRepo", new_repo_ui,
                            "Create a new GitBackup repository Thing with configuration, credentials, and proxy settings")),
    ]

    for filename, content in files:
        path = os.path.join(ENTITIES_DIR, filename)
        with open(path, "w") as f:
            f.write(content)
        print(f"Written: {path}")


if __name__ == "__main__":
    main()
