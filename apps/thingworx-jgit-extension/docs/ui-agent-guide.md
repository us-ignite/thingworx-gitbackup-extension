# Building UI in this ThingWorx project

Read [mashup-format-primer.md](mashup-format-primer.md) for the export format and
schema checks. This guide covers the project model that an agent needs before
adding widgets, services, or bindings. It is scoped to these two mashups and
the installed extension set; inspect Composer for the exact contract of a new
widget or a different platform release.

## Start with the model

The UI calls services on `GIT.Repository.ThingTemplate` through a dynamic thing
template data source, and services on `GIT.Utility.Thing` through a fixed Thing
source. `GetImplementingThings` lists repository Things. A selected Thing name
feeds the dynamic data source's `EntityName`; calls such as `Status`, `Commit`,
`GetBranchList`, and `GetCurrentBranch` then execute against that Thing.

| Question | Source of truth |
| --- | --- |
| What entities and services are declared? | `src/org/us_ignite/thingworx/jgit/extension/GitRepositoryShape.java`, `GitUtilityThingShape.java`, and `JGitEntityDeclarations.java`. |
| What data shapes and service result envelopes are generated? | `build/generated/dap/DataShapes/` after compiling the extension. |
| Which service instances are added to a mashup? | Embedded JSON `Data` in each `Mashups_*.xml`. |
| Which events run a service? | Embedded JSON `Events`; adding a service alone does not make it execute. |
| Where does a value go? | Embedded JSON `DataBindings`, then the widget's declared bindable properties. |

Services using `@DapServicePayload` return an outer service result infotable.
Its fields commonly include `Result`, `Message`, and `Error`. `Result` may itself
be an infotable with a separate payload Data Shape. Read both shapes before
binding a field. PTC describes [All Data and Selected Row(s)](https://support.ptc.com/help/thingworx/platform/r9/en/ThingWorx/Help/Mashup_Builder/DataServicesandLinking/BindingDataFromInfotables.html)
as distinct binding sources. A Data Shape defines each infotable column's name
and base type.

## Widget inventory and extension dependencies

The mashups use standard platform widgets (`ptcs*`), layout containers, and
these imported widgets:

| Widget | Provider | Check before editing |
| --- | --- | --- |
| `AgGridWidget2` | `apps/aggridwidget2-extension` | `src/metadata.xml` and the packaged IDE/runtime bundles; the source here is a vendored bundle. |
| `BMTypescriptClassHost` | `apps/bmwidgets-extension` / BMCoreUIWidgets | `src/GitHelper.ts`, `src/types/`, and the pinned upstream release described in that extension's README. |

The Operations mashup has three `AgGridWidget2` instances and one
`BMTypescriptClassHost`. Settings uses only platform widgets. These are
**package dependencies**, not Java services. Install the widget extensions
before importing a mashup that names their widget types. A widget's property,
service, and event definitions come from its extension's design-time code and
metadata; TypeScript declarations in `src/types/` help the IDE but do not
define ThingWorx runtime behavior. PTC distinguishes [standard and imported
widgets](https://support.ptc.com/help/thingworx/platform/r9/en/ThingWorx/Help/Mashup_Builder/Widgets/WidgetsOverView.html).

For a new widget, create it in Composer first when possible, export a small
example, and compare the widget's `Properties` and binding records. Do not
guess a widget property name from its visible label. Preserve its `Type`, `Id`,
and any extension-specific properties when editing an existing instance.

## Editable sources and generated content

- `src/GitHelper.ts` is the editable TypeScript for the class-host widget.
- `src/Operations.css` is the editable Operations CSS.
- `embedGitHelper` embeds those sources into the Operations mashup XML.
- `Mashups_GIT.Mashup.Operations.xml` and `Mashups_GIT.Mashup.Settings.xml`
  hold layout, widgets, service instances, bindings, events, and parameters.
- The XML `ParameterDefinitions` and embedded JSON
  `UI.Properties._currentParameterDefs` both represent mashup parameters.
  Update both for a new parameter, including infotable `dataShape`.

The Java declarations generate Data Shapes and related entity XML through the
DAP annotation processor. Do not treat a generated Data Shape XML file as the
source of truth. See [development.md](development.md).

## Workflow for a UI change

1. Identify the user interaction and the service that supplies or changes its
   data. Check its Java signature, result base type, payload Data Shape, inputs,
   and whether the service runs for a fixed Thing or selected repository Thing.
2. Identify the installed widget and inspect its real bindable properties.
   Prefer an existing widget instance or a Composer-created export as an
   example for serialization.
3. Add the service to `Data` if necessary, bind an event to execute it, then
   connect a compatible result field to a compatible widget property. Keep
   widget IDs and binding IDs unique.
4. For nested infotables, use a correctly typed intermediate parameter when
   needed. Keep the XML and JSON parameter definitions in sync.
5. Check XML against the bundled ThingWorx XSD and embedded JSON against the
   local structural schema. These checks cannot resolve service or widget
   contracts.
6. Import/package for the matching ThingWorx version. In Composer inspect
   Design > Reminders and the Bindings panel, then use **View Mashup** for a
   runtime check. [Design mode does not run services or events](https://support.ptc.com/help/thingworx/platform/r9/en/ThingWorx/Help/Mashup_Builder/Mashups/ViewingAMashup.html).

The local development task is
`./gradlew :apps:thingworx-jgit-extension:devSetup`. It packages and installs
the extension into the Docker Compose ThingWorx instance. Package version is
read from `.version`. See [development.md](development.md) for build
details and the widget extension READMEs for their installation dependencies.

## Current limitation

No schema here validates a binding against the installed widget definitions
and live service metadata. The local JSON schema is a structural profile of
these exports. Composer remains necessary for semantic validation, and a
runtime mashup view is necessary for service execution and interaction checks.
