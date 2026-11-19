// ThingWorx base-type aliases and the TypescriptWidget base class that the BMCoreUIWidgets
// TypeScript class host provides to helper code. Everything else comes from typescriptwebpacksupport.

declare interface FieldDefinitionCore<key extends string, T = any> {
    name: key;
    baseType: BASETYPENAME;
}

declare interface FieldDefinitionBase<key extends string, T = any> extends FieldDefinitionCore<key, T> {
    description?: string;
    ordinal?: number;
    aspects?: {
        isPrimaryKey?: boolean;
        defaultValue?: T;
    }
}
declare interface JSONInfoTable<T> {
    rows: T[];
    dataShape: {
        fieldDefinitions: {[key in keyof T]: key extends string ? FieldDefinitionBase<key, T[key]> : never}
    }
}

type NOTHING = void;
type STRING = string;
type NUMBER = number;
type BOOLEAN = boolean;

type ANYSCALAR = unknown;

type DATETIME = Date;
type datetime = Date;

type TIMESPAN = number;
type timespan = number;

type TWJSON = unknown;
type json = unknown;

// This is technically a Location object, but it doesn't contain any relevant methods
type LOCATION = {latitude: number, longitude: number, altitude?: number};
type location = LOCATION;

type INFOTABLE<T> = JSONInfoTable<T>;

type RENDERERWITHFORMAT = unknown;
type RENDERERWITHSTATE = unknown;

type FIELDNAME = string;

type IMAGE = string;
type HYPERLINK = string;
type IMAGELINK = string;
type PASSWORD = string;
type HTML = string;
type TEXT = string;
type TAGS = string;
type SCHEDULE = string;
type VARIANT = any;
type GUID = string;
type BLOB = any;
type INTEGER = number;
type LONG = number;
type PROPERTYNAME = string;
type SERVICENAME = string;
type EVENTNAME = string;
type THINGSHAPENAME = string;
type THINGTEMPLATENAME = string;
type DATASHAPENAME = string;
type MASHUPNAME = string;
type MENUNAME = string;
type BASETYPENAME = string;
type USERNAME = string;
type GROUPNAME = string;
type CATEGORYNAME = string;
type STATEDEFINITIONNAME = string;
type STYLEDEFINITIONNAME = string;
type MODELTAGVOCABULARYNAME = string;
type DATATAGVOCABULARYNAME = string;
type NETWORKNAME = string;
type MEDIAENTITYNAME = string;
type APPLICATIONKEYNAME = string;
type LOCALIZATIONTABLENAME = string;
type ORGANIZATIONNAME = string;
type DASHBOARDNAME = string;
type PERSISTENCEPROVIDERPACKAGENAME = string;
type PERSISTENCEPROVIDERNAME = string;
type PROJECTNAME = string;
type VEC2 = string;
type VEC3 = string;
type VEC4 = string;
type THINGCODE = string;
type NOTIFICATIONCONTENTNAME = string;
type NOTIFICATIONDEFINITIONNAME = string;
type THINGNAME<Template extends string | undefined = undefined, Shape extends string | undefined = undefined> = string;


/**
 * A subclass of `TWRuntimeWidgets` that provides support for widget classes that can be defined using
 * the `BMCodeHost` extensions.
 */
 declare abstract class TypescriptWidget extends TWRuntimeWidget {
    renderHtml(): string;
    afterRender(): void;

    /**
     * Retrieves a widget instance using its display name.
     * @param displayName   The display name to search for.
     * @returns             A widget instance, if one was found, `undefined` otherwise.
     */
    getWidget(displayName: TWLocalDisplayName): TWRuntimeWidget;
}
