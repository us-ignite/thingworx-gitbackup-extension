#!/usr/bin/env python3
"""
ThingWorx Mashup Migration: Legacy -> Converged (PTC Convergence Theme)

Transforms the mashupContent JSON inside Mashup XML entities to convert
legacy widget types to their converged (ptcs*) equivalents.

Usage:
    python3 scripts/migrate_mashups.py [--dry-run] [--mashup NAME]

Uses the same mapping as ThingWorx's built-in Mashup Migration dialog
(available in Composer), but operates on raw XML files directly.
"""

import json
import os
import re
import copy
import sys
import glob
import argparse

ENTITIES_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "Entities")


# ── Legacy -> Converged Widget Type Mapping ──────────────────────────────
LEGACY_TO_CONVERGED = {
    "label": {
        "type": "ptcslabel",
        "displayName": "Label",
        "keep_props": [
            "Area", "DisplayName", "Height", "Id", "LastContainer",
            "Left", "Margin", "ResponsiveLayout", "ShowDataLoading",
            "TooltipField", "Top", "Visible", "Width", "Z-index",
            "__TypeDisplayName",
        ],
        "transform_props": {
            # legacy_prop: converged_prop (or None to drop)
            "Text": "LabelText",
            "Alignment": "HorizontalAlignment",
            "AllowEllipsis": None,
            "Style": None,
            "ToolTipStyle": None,
            "__supportsTooltip": None,
        },
        "add_props": {
            "LabelType": "label",
            "MultiLine": False,
            "PreserveWhiteSpace": False,
            "UseTheme": True,
            "VerticalAlignment": "flex-start",
            "DisclosureControl": "show-more",
        },
    },
    "button": {
        "type": "ptcsbutton",
        "displayName": "Button",
        "keep_props": [
            "Area", "Disabled", "DisplayName", "Height", "Id",
            "LastContainer", "Left", "Margin", "ResponsiveLayout",
            "ShowDataLoading", "TabSequence", "TooltipField", "Top",
            "Visible", "Width", "Z-index", "Label", "__TypeDisplayName",
        ],
        "transform_props": {
            "CustomClass": "CustomClass",
            "ContextId": "ContextID",
            "ActiveStyle": None,
            "HoverStyle": None,
            "FocusStyle": None,
            "DisabledStyle": None,
            "DefaultButtonStyle": None,
            "Style": None,
            "RoundedCorners": None,
            "SingleClickSelectOnTablets": None,
            "IconAlignment": None,
            "CancelConfirmationButton": None,
            "ConfirmationButton1Label": None,
            "ConfirmationButton2Label": None,
            "ConfirmationPrompt": None,
            "ConfirmationRequired": None,
            "ConfirmationTitle": None,
            "DefaultConfirmationButton": None,
            "ToolTipStyle": None,
            "__supportsTooltip": None,
        },
        "add_props": {
            "ButtonType": "secondary",
            "MultiLine": True,
            "UseTheme": True,
        },
    },
    "textbox": {
        "type": "ptcstextfield",
        "displayName": "Text Field",
        "keep_props": [
            "Area", "Disabled", "DisplayName", "Height", "Id",
            "LastContainer", "Left", "Margin", "ResponsiveLayout",
            "ReadOnly", "ShowDataLoading", "TabSequence",
            "TooltipField", "Top", "Visible", "Width", "Z-index",
            "Text", "PlaceholderText", "__TypeDisplayName",
        ],
        "transform_props": {
            "PlaceholderText": "Placeholder",
            "Text": "Text",
            "CursorPosition": None,
            "DefaultTextboxFocusStyle": None,
            "InnerShadow": None,
            "Label": None,
            "LabelAlignment": None,
            "MaskInputCharacters": None,
            "TextAlign": "TextAlignment",
            "TextboxLabelStyle": None,
            "Style": None,
            "ToolTipStyle": None,
            "__supportsLabel": None,
            "__supportsTooltip": None,
        },
        "add_props": {
            "Label": "",
            "LabelAlignment": "left",
            "RequiredMessage": "A value is required",
            "ShowValidationCriteria": False,
            "ShowValidationFailure": False,
            "ShowValidationSuccess": False,
            "UseTheme": True,
            "ValidationCriteriaIcon": "cds:icon_info",
            "ValidationFailureIcon": "cds:icon_error",
            "ValidationState": "undefined",
            "ValidationSuccessIcon": "cds:icon_success",
            "ValueRequired": False,
        },
    },
    "textarea": {
        "type": "ptcstextarea",
        "displayName": "TextArea",
        "keep_props": [
            "Area", "Disabled", "DisplayName", "Height", "Id",
            "LastContainer", "Left", "Margin", "ReadOnly",
            "ResponsiveLayout", "ShowDataLoading", "TabSequence",
            "TooltipField", "Top", "Visible", "Width", "Z-index",
            "Text", "__TypeDisplayName",
        ],
        "transform_props": {
            "Text": "Text",
            "Style": None,
            "ToolTipStyle": None,
            "__supportsTooltip": None,
        },
        "add_props": {
            "Counter": False,
            "CriteriaMessage": "",
            "FillContainer": False,
            "Label": "",
            "LabelAlignment": "left",
            "MaxLengthFailureMessage": "${value} characters is the maximum",
            "MaxNumberOfCharacters": 1000000,
            "MinLengthFailureMessage": "${value} characters is the minimum",
            "RequiredMessage": "A value is required",
            "ShowValidationCriteria": False,
            "ShowValidationFailure": False,
            "ShowValidationSuccess": False,
            "TextAlignment": "left",
            "UseTheme": True,
            "ValidationCriteriaIcon": "cds:icon_info",
            "ValidationFailureIcon": "cds:icon_error",
            "ValidationState": "undefined",
            "ValidationSuccessIcon": "cds:icon_success",
            "ValueRequired": False,
        },
    },
    "checkbox": {
        "type": "checkbox",
        "displayName": "Checkbox",
        "keep_props": "*",
        "add_props": {
            "UseTheme": True,
        },
    },
    "image": {
        "type": "ptcsimage",
        "displayName": "Image",
        "keep_props": [
            "Area", "DisplayName", "Height", "Id",
            "LastContainer", "Left", "Margin", "ResponsiveLayout",
            "ShowDataLoading", "Top", "Visible", "Width", "Z-index",
            "SourceURL", "AlternateText", "__TypeDisplayName",
        ],
        "transform_props": {
            "Style": None,
        },
        "add_props": {
            "FillContainer": False,
            "Position": "top",
            "PreventCaching": False,
            "RawURL": False,
            "Scaling": "fit-y",
            "UseTheme": True,
        },
    },
    "link": {
        "type": "ptcslink",
        "displayName": "Link",
        "keep_props": [
            "Area", "DisplayName", "Height", "Id",
            "LastContainer", "Left", "Margin", "ResponsiveLayout",
            "ShowDataLoading", "TooltipField", "Top",
            "Visible", "Width", "Z-index", "Text", "Disabled",
            "__TypeDisplayName",
        ],
        "transform_props": {
            "Alignment": "Alignment",
            "ToolTipStyle": None,
            "Style": None,
        },
        "add_props": {
            "LinkType": "primary",
            "LinkURL": "",
            "SingleLine": False,
            "TabSequence": 0,
            "TargetWindow": "_blank",
            "UseTheme": True,
            "VerticalAlignment": "center",
        },
    },
    "gridadvanced": {
        "type": "ptcsgrid",
        "displayName": "Grid Advanced",
        "keep_props": [
            "Area", "DisplayName", "Height", "Id",
            "LastContainer", "Left", "Margin", "ResponsiveLayout",
            "ShowDataLoading", "Top", "Visible", "Width", "Z-index",
            "__TypeDisplayName",
        ],
        "transform_props": {
            "Style": None,
            "DefaultGridStyle": None,
            "DefaultFocusStyle": None,
        },
        "add_props": {
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
            "UseTheme": True,
            "WasMigrated": True,
        },
    },
    "dhxgrid": {
        "type": "ptcsgrid",
        "displayName": "Grid Advanced",
        "keep_props": [
            "Area", "DisplayName", "Height", "Id",
            "LastContainer", "Left", "Margin", "ResponsiveLayout",
            "ShowDataLoading", "Top", "Visible", "Width", "Z-index",
            "__TypeDisplayName",
        ],
        "transform_props": {
            "Style": None,
            "DefaultGridStyle": None,
        },
        "add_props": {
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
            "UseTheme": True,
            "WasMigrated": True,
        },
    },
    "divider": {
        "type": "divider",
        "displayName": "Divider",
        "keep_props": [
            "Area", "DisplayName", "Height", "Id",
            "LastContainer", "Left", "Margin", "ResponsiveLayout",
            "ShowDataLoading", "Top", "Visible", "Width", "Z-index",
            "__TypeDisplayName",
        ],
        "transform_props": {
            "Style": None,
        },
        "add_props": {
            "UseTheme": True,
            "VerticalDivider": False,
        },
    },
    "flexcontainer": {
        "type": "flexcontainer",
        "displayName": "Responsive Container",
        "keep_props": "*",
        "add_props": {
            "UseTheme": True,
        },
    },
    "container": {
        "type": "flexcontainer",
        "displayName": "Responsive Container",
        "keep_props": [
            "Area", "DisplayName", "Id", "LastContainer",
            "Margin", "ShowDataLoading",
            "Visible", "Z-index", "ResponsiveLayout",
        ],
        "transform_props": {
            "Style": None,
            "StyleProperties": "StyleProperties",
            "EnableExpandCollapse": "EnableExpandCollapse",
            "Expanded": "Expanded",
            "ShowExpandCollapseTab": "ShowExpandCollapseTab",
            "SourceURL": "SourceURL",
        },
        "add_props": {
            "EnableExpandCollapse": False,
            "Expanded": True,
            "ShowExpandCollapseTab": False,
            "SourceURL": "",
            "UseTheme": True,
            "__TypeDisplayName": "Responsive Container",
            "align-content": "flex-start",
            "align-items": "flex-start",
            "flex-basis": "auto",
            "flex-direction": "row",
            "flex-grow": 1,
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
        },
    },
    "panel": {
        "type": "flexcontainer",
        "displayName": "Responsive Container",
        "keep_props": [
            "Area", "DisplayName", "Id", "LastContainer",
            "Margin", "ShowDataLoading",
            "Visible", "Z-index",
        ],
        "transform_props": {
            "Style": None,
            "StyleProperties": "StyleProperties",
        },
        "add_props": {
            "EnableExpandCollapse": False,
            "Expanded": True,
            "ShowExpandCollapseTab": False,
            "SourceURL": "",
            "ResponsiveLayout": True,
            "UseTheme": True,
            "__TypeDisplayName": "Responsive Container",
            "align-content": "flex-start",
            "align-items": "flex-start",
            "flex-basis": "auto",
            "flex-direction": "row",
            "flex-grow": 1,
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
        },
    },
    "layout": {
        "type": "flexcontainer",
        "displayName": "Container",
        "keep_props": [
            "Area", "DisplayName", "Id", "LastContainer",
            "Margin", "ShowDataLoading",
            "Visible", "Z-index",
        ],
        "transform_props": {
            "Style": None,
        },
        "add_props": {
            "EnableExpandCollapse": False,
            "Expanded": True,
            "ShowExpandCollapseTab": False,
            "SourceURL": "",
            "ResponsiveLayout": True,
            "UseTheme": True,
            "__TypeDisplayName": "Container",
            "align-content": "flex-start",
            "align-items": "flex-start",
            "flex-basis": "auto",
            "flex-direction": "row",
            "flex-grow": 1,
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
        },
    },
}

# Widgets we leave completely unchanged (no converged equivalent yet)
SKIP_WIDGET_TYPES = {
    "expression", "expression2", "eventsrouter", "entitypicker",
    "tagpicker", "navigation", "mashup", "autocomplete",
    "infotableselector", "collection", "validator", "tree",
    "buttonbar", "dropdown", "list",
    "ptcslabel", "ptcsbutton", "ptcstextfield", "ptcstextarea",
    "ptcsgrid", "ptcsimage", "ptcslink", "flexcontainer",
    # No converged equivalent yet
    "tabsv2", "radiobuttonlist", "dhxlist",
    "gitinfotableselector", "diffviewer",
    "statusmessage", "pagemashupcontainer", "validator2",
}


def normalize_widget_type(raw_type):
    """Normalize variations in widget type names."""
    t = raw_type.lower().strip()
    if t == "text box":
        return "textbox"
    if t == "text field":
        return "textbox"
    if t == "html textarea":
        return "textarea"
    if t == "htmltextarea":
        return "textarea"
    return t


def transform_widget_props(props, mapping):
    """Transform a single widget's Properties dict."""
    result = {}
    keep_set = set(mapping.get("keep_props", []))
    transform_map = mapping.get("transform_props", {})
    add_map = mapping.get("add_props", {})

    if keep_set == {"*"}:
        # Keep all original properties
        for k, v in props.items():
            result[k] = v
    else:
        for k, v in props.items():
            if k in keep_set:
                result[k] = v
            elif k in transform_map:
                new_name = transform_map[k]
                if new_name is not None:
                    result[new_name] = v
                # else: drop the property

    # Add new properties
    for k, v in add_map.items():
        if k not in result:
            result[k] = v

    # Set converged type
    result["Type"] = mapping["type"]
    result["__TypeDisplayName"] = mapping["displayName"]

    return result


def transform_widget(widget):
    """Recursively transform a widget and its children."""
    props = widget.get("Properties", {})
    raw_type = props.get("Type", "")
    widget_type = normalize_widget_type(raw_type)

    if widget_type in SKIP_WIDGET_TYPES:
        # Leave as-is but ensure UseTheme for non-ptcs widgets
        if widget_type not in ("expression", "expression2", "eventsrouter"):
            pass  # keep unchanged
        children = widget.get("Widgets", [])
        new_children = [transform_widget(c) for c in children]
        return {"Properties": props, "Widgets": new_children}

    mapping = LEGACY_TO_CONVERGED.get(widget_type)
    if mapping is None:
        print(f"  WARNING: Unknown widget type '{raw_type}' (normalized: '{widget_type}') — keeping as-is")
        children = widget.get("Widgets", [])
        new_children = [transform_widget(c) for c in children]
        return {"Properties": props, "Widgets": new_children}

    new_props = transform_widget_props(props, mapping)

    children = widget.get("Widgets", [])
    new_children = [transform_widget(c) for c in children]

    return {"Properties": new_props, "Widgets": new_children}


def transform_mashup_json(mashup_json):
    """Transform the entire mashup JSON structure."""
    # Transform StyleTheme
    if "UI" in mashup_json:
        ui_props = mashup_json["UI"].get("Properties", {})
        ui_props["StyleTheme"] = "PTC Convergence Theme"
        ui_props["UseTheme"] = False
        ui_props["UseMasterTheme"] = False
        if "UseThemeForHybrids" in ui_props:
            del ui_props["UseThemeForHybrids"]
        # Set MigratedOn timestamp
        from datetime import datetime, timezone
        ui_props["MigratedOn"] = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.") + "000Z"

        # Transform all widgets in the UI tree
        widgets = mashup_json["UI"].get("Widgets", [])
        transformed = [transform_widget(w) for w in widgets]
        mashup_json["UI"]["Widgets"] = transformed

        # Add Reminders
        mashup_json["Reminders"] = mashup_json.get("Reminders", [])
        mashup_json["Reminders"].append({
            "area": "Mashup",
            "category": "migration",
            "id": "mashup-root.migration.mashup.info",
            "msgKey": "tw.mb.migration.messages.legacy-functions-migrated",
            "msgParams": {},
            "severity": "info",
            "status": "pending",
            "type": "mashup",
            "widgetId": "mashup-root"
        })

    return mashup_json


def extract_json_from_cdata(text):
    """Extract JSON from <mashupContent><![CDATA[ ... ]]></mashupContent>."""
    # Match CDATA within mashupContent (must anchor to <mashupContent> to
    # avoid matching CDATA sections in unrelated elements like MobileSettings)
    m = re.search(r'<mashupContent[^>]*>\s*<!\[CDATA\[(.*?)\]\]>\s*</mashupContent>', text, re.DOTALL)
    if m:
        return json.loads(m.group(1))
    # Try non-CDATA fallback
    m = re.search(r'<mashupContent[^>]*>(.*?)</mashupContent>', text, re.DOTALL)
    if m:
        return json.loads(m.group(1))
    return None


def replace_json_in_cdata(text, new_json):
    """Replace the JSON inside the CDATA section."""
    new_json_str = json.dumps(new_json, indent=2)
    # Try CDATA replacement (must anchor to <mashupContent>)
    m = re.search(r'(<mashupContent[^>]*>\s*<!\[CDATA\[).*?(\]\]>\s*</mashupContent>)', text, re.DOTALL)
    if m:
        return text[:m.start(1)] + m.group(1) + "\n" + new_json_str + "\n" + m.group(2) + text[m.end(2):]
    # Try non-CDATA
    m = re.search(r'(<mashupContent[^>]*>).*?(</mashupContent>)', text, re.DOTALL)
    if m:
        return text[:m.start(1)] + m.group(1) + new_json_str + text[m.end(2):]
    raise ValueError("Could not find mashupContent in XML")


def process_mashup_file(filepath, dry_run=False):
    """Process a single mashup XML file."""
    print(f"\n{'='*60}")
    print(f"Processing: {os.path.basename(filepath)}")

    with open(filepath, 'r', encoding='utf-8') as f:
        original_text = f.read()

    # Extract JSON from CDATA
    try:
        mashup_json = extract_json_from_cdata(original_text)
    except json.JSONDecodeError as e:
        print(f"  SKIP: Invalid JSON in CDATA: {e}")
        return False

    if mashup_json is None:
        print(f"  SKIP: No mashupContent found")
        return False

    # Track what we're transforming
    widget_count = count_widgets(mashup_json)
    print(f"  Widgets in tree: {widget_count}")

    # Transform
    try:
        new_json = transform_mashup_json(copy.deepcopy(mashup_json))
    except Exception as e:
        print(f"  ERROR during transform: {e}")
        return False

    new_widget_count = count_widgets(new_json)

    # Dry run?
    if dry_run:
        print(f"  [DRY-RUN] Would transform {widget_count} legacy widgets -> {new_widget_count} converged widgets")
        return True

    # Write back
    try:
        new_text = replace_json_in_cdata(original_text, new_json)
    except ValueError as e:
        print(f"  ERROR: {e}")
        return False

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(new_text)

    print(f"  SUCCESS: Transformed {widget_count} widgets")
    return True


def count_widgets(mashup_json):
    """Count total widget instances in the UI tree."""
    count = 0
    def walk(widgets):
        nonlocal count
        for w in widgets:
            count += 1
            children = w.get("Widgets", [])
            if children:
                walk(children)
    ui = mashup_json.get("UI", {})
    walk(ui.get("Widgets", []))
    return count


def main():
    parser = argparse.ArgumentParser(
        description="Migrate legacy mashup widgets to PTC Convergence Theme"
    )
    parser.add_argument("--dry-run", action="store_true",
                        help="Preview changes without writing")
    parser.add_argument("--mashup", type=str, default=None,
                        help="Process only specific mashup (filename glob, e.g. 'Main' or 'Push*')")
    args = parser.parse_args()

    # Find mashup XML files
    pattern = os.path.join(ENTITIES_DIR, "Mashups_*.xml")
    all_files = sorted(glob.glob(pattern))

    if args.mashup:
        files = [f for f in all_files if args.mashup in os.path.basename(f)]
        if not files:
            print(f"No mashup files match '{args.mashup}'")
            print(f"Available: {[os.path.basename(f) for f in all_files]}")
            sys.exit(1)
    else:
        files = all_files

    print(f"Found {len(files)} mashup XML files to process")

    success = 0
    for f in files:
        if process_mashup_file(f, dry_run=args.dry_run):
            success += 1

    total = len(files)
    print(f"\n{'='*60}")
    print(f"Results: {success}/{total} mashups processed successfully")
    if args.dry_run:
        print("DRY RUN — no files were modified")
    else:
        print("Files have been modified — rebuild extension and test!")


if __name__ == "__main__":
    main()
