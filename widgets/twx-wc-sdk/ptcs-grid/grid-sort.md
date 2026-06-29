# The Grid Sort Functions

The grid component supports a hierarchical data model with multi-field sorting. The _grid sort functions_ exposes the sorting functionality to external developers.

The grid displays its data without touching it (aka corrupting it) and displays it in a virtual scroller that accesses as little data as possible. A general sorting functionality is therefore not compatible with the grids processing model and has no place inside it. Instead, the sort function has been implemented in a separate module.

It has several features:

- The sort function can take its input from an existing grid component
- The sort function can sort both linear (array) data and tree data
- The sub-nodes can either reside inside the data items or be retrieved from external sources
- External sub-nodes can be delivered as promises, which allow the client to retrieve them from arbitrary services
- The sorted sub-nodes can be stored inside the data items or be directly delivered back to the external sources
- The compare function can be an explicit javascript function or an implicit declaration of sortable columns


## Functions Overview

There are three grid sort functions:

- `sort()` - the main function that performs the actual sorting
- `sortConfig()` - a support function that generates sorting data (and can be used with a grid view configurator)
- `sortFunction()` - a support function that converts a sort configuration to a sort compare function

## sort

### Importing the sort function

~~~js
import {sort} from 'ptcs-grid/grid-sort.js';
~~~

### Using the sort function on a grid

If the grid element is available as `grid`, call:

~~~js
const result = sort(grid);
~~~

The sort function uses the current data and sort order of the grid. The returned `result` is either:
- `null` (if the `grid` doesn't define sortable data), or
- an array (the sorted data), or
- a `Promise` that resolves to an array (unless the promise is rejected)

The returned array contains the top-level nodes. If the `grid` contains linear data, then the array contains the complete result of the sort operation.

If the `grid` is a tree grid, then the result also includes sub-nodes. The sub-nodes are either returned as properties in the data items or as a separate `Map`.

If `grid.subItems` specifies a property name, then the sort function will process the children in this property. Hence, the sub-nodes will be sorted in-place.

The grid client may also configure `grid.data` to retrive the sub-nodes using callbacks (that may return promises). If this is the case, the sorted sub-nodes will be returned as a Javascript `Map` attached to the array as `.$subItems`.

This is a example of how to do a depth-first traversal of such data (where `items` is the returned array):

~~~js
function depthFirstTraversal(items, callback) {
    const {$subItems} = items;

    const processTree = _items => _items && _items.forEach(item => {
        callback(item);
        processTree($subItems.get(item));
    });

    processTree(items);
}
~~~

The returned data is cloned, to make it independent of the data in the grid. It can therefore be edited without any risk of corrupting the grid.

### Using the sort function on explicit data

The sort function can also sort data that doesn't reside in a grid. In that case the sorting can be specified using 2 - 4 arguments:

~~~js
sort(items, compare, getSubItems?, setSubItems?)
~~~

#### The items argument

The `items` argument is an array of the top-level items.

#### The compare argument

The `compare` argument is either a `function` or an array of column specifiers.

If the `compare` argument is a `function`, then the function should take two items as input and return:
- a negative number if the first item is less than the second
- a positive number if the first item is greater than the second
- `0` (zero) if the items are the same

In other words, the client should supply a regular compare function.

If the `compare` argument is an array of columns specifiers, each specifier should contain the following fields:


|Field |Description|Default|
|------|-----------|-------|
|`value`|Specifies how to get the value of an item. Can be a field name or function that takes an item as input and returns its column value.| item => item|
|`compare`|A regular compare function. The input to this function is the items values (as specified be the `value` field).|Use the default compare function for the `baseType`, if specified, or fallback to: `(a, b) => 0` (that is, give all items the same sort order)|
|`baseType`|Specifies a ThingWorx BaseType for the column value. It defines the sort order if `compare` is unassigned.| `undefined`|
|`sortOrder`| `'asc'` for ascending or `'desc'` for descending. If a descending sort order is requested, the sort function swaps the arguments to the compare function. | `'asc'`|

The sort function starts by comparing the first column. If it results in a non-zero value, that value determines the sort order. Otherwise the sort function compares the next column and keeps doing so until it either finds a non-zero value or runs out of columns. If the latter happens, the compared items gets the same sort order.


#### The getSubItems (optional) argument

The `getSubItems` argument is only needed for tree data. It specifies how to retrive the child nodes of an item. It can be specified in two ways:

- as a field name (a string), that specifies the name of the property that contains the child nodes
- as a function that takes the item as input and returns the child nodes, possibly via a `Promise`.


#### The setSubItems (optional) argument

The `setSubItems` argument is only useful for tree data. It specifies a function that recives the sorted child nodes of an item. It is called as `setSubItems(item, children)`, where `children` is an array of the sorted child nodes.

Note that `setSubItems` is not needed if `getSubItems` specifies a field name. In that case the sorted children will be processed _in place_, and therefore don't need to be stored separately. However, if `getSubItems` retrieves the child nodes from an external source, `setSubItems` is the hook where to send them back, sorted.


## sortConfig

The sortConfig function helps creating _sort configurations_ that can be used when invoking the `sort` function. This function has been specifically designed to be compatible with the column format of the grid view configurator.

A sort configuration can be specified in two ways:

- as an array of sortable fields, or
- as a view configurator

In both cases, the sort order can be explicitly specified as an optional parameter.


### Importing the sortConfig function

~~~js
import {sortConfig} from 'ptcs-grid/grid-sort.js';
~~~

### Using sortConfig on array data

~~~js
const result = sortConfig(columns, sortExpression);
~~~

#### The columns parameter

The `columns` parameter is an array of column value specifiers, where each array entry can specify the followining fields:


|Field |Description|Default|
|------|-----------|-------|
|`sortable`|A Boolean that specifies if the column is sortable. The column will be ignored unluess `sortable` is _truthy_.|_falsy_|
|`$sortName`|The name of the column. It takes precedence over any alternative columns name|`undefined`|
|`name`, `title`, `label`|Alternative ways to specify the name of the column, in precedence order. Added for historical reasons.|`undefined`|
|`value`|Specifies how to get the column value of an item. Can be a field name or function (that takes an item as input and returns its value).| item => item|
|`compare`|A regular compare function. The input to this function is the value of the item, as retrieved by the `value` field.|Use the default compare function for the `baseType`, if specified, or fallback to: `(a, b) => 0` (that is, give all items the same sort order)|
|`baseType`|Specifies a ThingWorx BaseType for the column value. It defines the sort order if `compare` is unassigned.| `undefined`|
|`sortOrder`| `'asc'` for ascending or `'desc'` for descending. |`undefined`|

If `sortConfig` is invoked _without_ the optional `sortExpression`, the function collects all columns that are `sortable` and has a `sortOrder` and derives a sort configuration from them. The format of the latter is described in the `sort` function section (see above).


If `sortConfig` is invoked _with_ a `sortExpression`, it ignores `sortable` and `sortOrder`, but instead creates the sort configuration based on the sort expression.


#### The sortExpression parameter

The `sortExpression` parameter specifies the sort order as a comma separated string of columns names and sort orders, combined with a `':'`.

Examples:

|SortExpression|Description|
|--------------|-----------|
|`'Title:asc'`|Sort data in ascending order according to the values in the Title column.|
|`'Name:desc,Title:asc'`|Sort data in descending order according to the values in the Name column. Sort identical names in ascending order according to the Title column.|


### Using sortConfig on a view configurator

~~~js
sortConfig(view, sortExpression)
~~~

The `view` parameter is supposed to be a grid view configurator. However, `sortConfig` uses duck typing, any object that has the following properties can be used:

|Property|Description|
|--------|-----------|
|columns |An array of column specifiers (see _The columns parameter_). This is the first place where `sortConfig` looks for columns.|
|columnsDef |An array of column specifiers. This is the second place where `sortConfig` looks for columns.|
|getSortExpression|A function that returns the current sort expression as: `{short:` _sortExprerssion_`}`

Hence, `sortConfig` will first look for the columns as `view.columns`, then as `view.columnsDef`. If it finds an array, it will pick the sort expression from:

- the sortExpression parameter, if specified, or
- the sortExpression returned by `view.getSortExpression()`, if available, or
- use the implicit sort expression that is specified by the `sortable` and `sortOrder` fields in the columns array


## sortFunction

The `sortFunction` converts a _sort configurations_ (as described in the `sort` function) info a function that compares two grid rows.


### Importing the sortConfig function

~~~js
import {sortFunction} from 'ptcs-grid/grid-sort.js';
~~~

### Using sortConfig

~~~js
const compareFunction = sortConfig(sortConfig);
~~~