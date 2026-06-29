# The Grid Value Manager

The _Value Manager_ is a grid component that enables _delayed values_.

A delayed value cannot be displayed until the value has been _resolved_. The value can either be resolved immediately, in the grid data, or later by assigning the value in the Value Manager. While waiting for the value to be resolved, the grid typically shows some kind of "spinner".

The interchange of values is handled by the Value Manager, a hub where the grid requests values and the client assigns values. The grid informs the Value Manager about the values it currently needs, and the Value Manager informs the Client about the values that is currently needed.

The Value Manager allows the client and the grid to operate completely independent, using a straightforward event-based approach.

## Basic Processing Model

When the Grid needs a delayed cell value, it asks the Value Manager. If the Value Manager has the value, it returns it and the grid displays it. Otherwise, the Value Manager returns a "not a value" token (`ValueManager.NaV`). When the grid receives a `NaV` token it indicates that the value is not yet available, allowing the theming to display the missing value any way it wants, which probably is some kind of spinner.

The grid informs the Value Manager about the items (rows) with unresolved values that it wants to render. These rows are inside the _viewport_, the part of the grid that is visible on the screen. Whenever new items enter the viewport, the Value Manager fires an event that the client can listen to.

The client can retrieve the items in the viewport, or any unresolved items. When the client resolves a value, the Value Manager informs the grid about the assigned value.



## Methods

Note: the `key` of the value methods represents an item. The key is either an index to the item (which can retrieve the item using the `item()` method) or the item itself.

| Name      | Arguments | Description |
|-----------|-----------|-------------|
| NaV       |           | A static read-only property that specifies the `NaV` token. |
| observe   | object    | Registers object as an observer. See below |
| unobserve | object    | Unregisters object as an observer. See below |
| size      |           | A read-only property that returns the number of items. |
| item      | index     | Returns the item with the specified `index`. |
| isResolved | index, columnId |
| unresolvedItems | max?  | Returns an array that contains at most `max` number of unresolved items. If `max` is not specified, all unresolved items are returned. |
| viewport  |            | A property that contains the indexes of the items in the viewport that needs values to be resolved. |
| columnIds |           |  A property that contains the columns ids of the value manager. Each item can have a delayed value for each columnId. A column id is a string. |
| setData   | dataManager | Assigns the DataManager that manages the grid items. |
| value     | key, columnId | Get a delayed value of an item. If the value has not yet been resolved, `NaV` is returned. |
|setValue   | key, columnId, value | Resolve a delayed value of an item. |
|resetValue | key, columnId | Unresolve a delayed value of an item. |
|resolveValue | key, columnId | Syntactic sugar for `setValue(key, columnId, NaV)`. This assignment means that the value will be considered as resolved, but when asking for the value, it still appears to be unresolved. |


## Observers

The Value Manager calls these methods in the observer:

| Function     | Arguments               | Description |
|--------------|-------------------------|-------------|
|gvViewport    |viewport, added, removed | The viewport has changed. The `viewport` argument contains the items of the viewport, `added` the items that has been added since the previous even change, and `removed` the items that has been removed. |
|gvValueChanged|key, columnId, value?    | The `columnsId` value of the `key` item has been assigned to `value`. If `value` is not part of the argument list, the value has been unresolved. |


