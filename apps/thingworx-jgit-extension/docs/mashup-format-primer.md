# ThingWorx mashup export primer

This is a working guide for agents editing the mashups in `../ui/`. It
describes the ThingWorx 9.7 style export used here. Treat the exported file as
Composer-owned data: preserve unknown fields and let Composer confirm semantic
changes. This is **not** a formal format specification.

## What is published

PTC documents XML entity export and including Composer-exported mashups in an
extension package. A vendor XSD is bundled at `com/thingworx/documents/schema.xsd`
inside the local `thingworx-ext-sdk-*.jar` and the platform's
`thingworx-platform-common-*.jar`. The 10.1 SDK XSD validates both mashup XML
files here. Its `mashupContent` declaration is only `xs:string`, so it does not
validate the embedded JSON or binding semantics. No vendor JSON schema or
standalone binding validator was found. PTC describes configuring and viewing
bindings in Composer and its built-in validation/reminders. A matching
ThingWorx Composer instance remains the semantic validator.

The local [mashup-content.schema.json](mashup-content.schema.json) is a
deliberately permissive structural profile derived from the two exports here.
It validates both, but is not a vendor specification. Unknown fields are
allowed to preserve compatibility across ThingWorx and widget versions.

- [Importing and exporting entities](https://support.ptc.com/help/thingworx/platform/r9.6/en/ThingWorx/Help/Getting_Started/ImportingandExportinginThingWorx/ImportingandExportingDataEntitiesandExtensions.html)
- [Configuring bindings](https://support.ptc.com/help/thingworx/platform/r9.7/en/ThingWorx/Help/Mashup_Builder/DataServicesandLinking/ConfiguringBindingsInAMashup.html)
- [Viewing bindings](https://support.ptc.com/help/thingworx/platform/r9/en/ThingWorx/Help/Mashup_Builder/DataServicesandLinking/ViewingConnectionsInAMashup.html)
- [Configuring mashup parameters](https://support.ptc.com/help/thingworx/platform/r10.0/en/ThingWorx/Help/Mashup_Builder/Mashups/ConfigureMashupParameters.html)
- [Widget validation hooks and reminders](https://support.ptc.com/help/thingworx/platform/r9.6/en/ThingWorx/Help/Best_Practices_for_Developing_Applications/visualization_WidgetApiMashupBuilder.html)

The observations below come from the exported files and Java service contracts
in this repository. They are not claims about every ThingWorx version.

## Two serialization layers

The outer file is an XML entity export: `Entities > Mashups > Mashup`. Entity
metadata, permissions, configuration tables, and `ParameterDefinitions` are
XML. The `mashupContent` element contains a JSON document as text. Editing
JSON must preserve valid XML text; XML escaping and JSON escaping are separate.

In `Mashups_GIT.Mashup.Operations.xml`, the JSON contains:

| Key | Role observed in this export |
| --- | --- |
| `Data` | Entities and services added to the mashup, including dynamic thing templates. |
| `DataBindings` | Property-to-property connections. |
| `Events` | Events that invoke services or widget actions. |
| `UI` | Root mashup `Properties` and recursive `Widgets` tree. |
| `CustomMashupCss` | CSS stored in the export; generated from `src/Operations.css` here. |

`UI.Properties._currentParameterDefs` repeats mashup parameter metadata in
JSON. The XML `ParameterDefinitions` is a second representation. When adding or
renaming a parameter, keep **both** representations consistent, including base
type, binding direction, and infotable data shape. Do not infer that Composer's
warning count alone proves they match.

## Reading a data binding

Each `DataBindings` entry has source and target areas, IDs, sections, and one
or more `PropertyMaps`. `SourceArea: Data` identifies a service result;
`SourceArea: Mashup` identifies a mashup parameter. `TargetArea: UI` identifies
a widget. The `SourceId`/`TargetId` values refer to service or widget IDs in the
same JSON, not necessarily display names. `SourceSection` identifies the entity
for a service, such as `DynamicThingTemplates_GIT.Repository.ThingTemplate`.

`SourcePropertyBaseType` and `TargetPropertyBaseType` describe the **selected
binding endpoints**. A string widget property such as `LabelText` cannot take
an entire `INFOTABLE` field. For an infotable mashup parameter, this export
uses `SubProperty` to select a column and records the selected field's type as
`SourcePropertyBaseType`. Compare the `GpgVerification` parameter to its
`GpgKeyFingerprint` text binding, or `SelectedBranch` to `BranchName`.

The current branch service illustrates why the level of nesting matters:

```text
GetCurrentBranch() -> GIT.CurrentBranchStatus.ServiceResult.DataShape
  Result: INFOTABLE -> GIT.CurrentBranchStatus.DataShape
    BranchName: STRING
    DetachedHEAD: BOOLEAN
  Message: STRING
  Error: BOOLEAN
```

These types are declared by `GitRepositoryShape.GetCurrentBranch`,
`JGitEntityDeclarations.CurrentBranchStatusSpec`, and `DapResults.payloadResult`.
`BranchName` is not a direct column of the service's outer result table. The
binding pattern used here is:

```text
GetCurrentBranch.Result (INFOTABLE)
  -> mashup CurrentBranchStatus (INFOTABLE, GIT.CurrentBranchStatus.DataShape)
  -> CurrentBranchStatus.BranchName (STRING)
  -> ptcslabel-92.LabelText (STRING)
```

Do not map the outer `Result` infotable directly to `LabelText`, or change only
the source name to `BranchName` while leaving the source as the outer service.

## Safe edit workflow

1. Read the actual service return type and declared data shape before editing a
   service binding. For DAP services, inspect both the payload shape and the
   generated `*.ServiceResult.DataShape` envelope.
2. Locate the binding by stable `Id`, then locate its source service and target
   widget. Compare similar bindings already in the export.
3. Keep XML `ParameterDefinitions` and JSON `UI.Properties._currentParameterDefs` aligned.
4. Parse the XML and the text of `mashupContent` as JSON to catch syntax errors.
   This does **not** check references, binding types, widget behavior, or runtime
   service results.
5. Package/import into the matching ThingWorx version. Open Design > Reminders
   and inspect the Bindings panel. Open **View Mashup** to check runtime behavior;
   PTC notes that services and events do not execute in the design canvas.

For this extension, `ui/src/GitHelper.ts` and `ui/src/Operations.css` are source files;
the build embeds them into the Operations mashup. See [ui/README.md](../ui/README.md)
before touching the generated `Code`, `TranspiledCode`, `Exports`, or
`CustomMashupCss` fields. Preserve unrelated export fields and IDs.

## Local syntax check

The vendor XSD can be extracted from the SDK JAR already present after a build:

```sh
unzip -p apps/thingworx-jgit-extension/build/sdk-10.1/thingworx-ext-sdk-10.1.0-b47.jar \
  com/thingworx/documents/schema.xsd > /tmp/thingworx-entities.xsd
```

An XML Schema validator can then check an export, for example
`xmllint --noout --schema /tmp/thingworx-entities.xsd Mashups_*.xml` if
`xmllint` is installed. The 10.1 SDK XSD was also checked against both exports
with Java's built-in `SchemaFactory`/`Validator` and accepted both.

This checks the embedded JSON against the repository profile without contacting
ThingWorx (requires the Python `jsonschema` package):

```sh
python3 - <<'PY'
import json
import xml.etree.ElementTree as ET
from pathlib import Path
from jsonschema import Draft202012Validator

schema = json.loads(Path('apps/thingworx-jgit-extension/docs/mashup-content.schema.json').read_text())
validator = Draft202012Validator(schema)
for path in Path('apps/thingworx-jgit-extension/ui').glob('Mashups_*.xml'):
    root = ET.parse(path).getroot()
    content = root.find('.//mashupContent')
    if content is None or content.text is None:
        raise ValueError(f'{path}: missing mashupContent')
    validator.validate(json.loads(content.text))
    print(f'{path}: embedded JSON matches local structural profile')
PY
```

Neither schema resolves service result shapes, widget property types, or
cross-references. If a standalone semantic validator becomes available, verify
its supported ThingWorx version and whether it checks those relationships.
