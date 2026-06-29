# ptcs-collection

## Overview

The collection component contains a collection of arbitrary UI components. The client has full control over the creation of the components and can chose between three layouts: _grid_, _flex_, or _table_.

The component is data driven; the client supplies an array of items and methods that convert the items into UI components. The returned components are managed in a virtual scroller, allowing the component to handle many sub-components efficiently.

The collection component supports _sorting_ and _grouping_ with _headers_ and _footers_.

## Usage Example

### Basic Usage

~~~js
<ptcs-collection items="[[items]]" item-width="[[itemWidth]]" item-height="[[itemHeight]]"
                 create-cell="[[createCell]]" assign-cell="[[assignCell]]"></ptcs-collection>
~~~

Note that `itemWidth`, `itemHeight`, `createCell`, and `assignCell` are callback methods (functions).


### Grouping with headers and footers

~~~js
<ptcs-collection items="[[items]]" item-width="[[itemWidth]]" item-height="[[itemHeight]]"
                 create-cell="[[createCell]]" assign-cell="[[assignCell]]"
                 group="groupKey" headers footers></ptcs-collection>
~~~


## Component API

### Properties
| Property | Type | Description | Default | Triggers a changed event? |
|----------|------|-------------|---------|---------------------------|
|assignCell|Function|Binds a cell element, created by `createCell`, to an item in the `items` array. The prototype is: (`cellElement`, `item`, `index`, `selected`, `collection`). Note: `selected` is `true` if the element is selected. `collection` is the collection component. It can e.g. be used to check if the collection has been disabled.| | |
|assignFooter|Function|Binds a footer element, created by `createFooter`, to a group key (a string). The prototype is: (`footerElement`, `groupKey`, `set`). Note: `set` is a Set that contains all items that belongs to the group.| | |
|assignHeader|Function|Binds a header element, created by `createHeader`, to a group key (a string). The prototype is: (`headerElement`, `groupKey`, `set`). Note: `set` is a Set that contains all items that belongs to the group.| | |
|autoSelectFirstItem|Boolean|Automatically select the first item of `items`` when starting?|`false`| |
|autoScroll|Boolean|Scroll to the closest selected item when the collection is resized?|`false`| |
|columnGap|Number or String|Specifies the gap between columns|`8px`| |
|cellTabKeyScope|String|Controls the scope of navigation when interacting with the contents of a cell using the keyboard. When the scope is set to `cell`, using the TAB inside a cell loops over the elements within the cell. When the scope is set to `collection`, the TAB key navigates to the next cell in the collection after the last element in the current cell is selected.|`cell`| |
|createCell|Function|Function that returns an empty cell DOM element: `()` => `Element`. The element will be used by `assignCell` to assign an item to it. Note that the element can be reused by any number of items.| | |
|createFooter|Function|Function that returns an empty footer DOM element: `()` => `Element`. The element will be used by `assignFooter` to assign an group identfier to it. Note that the element can be reused by any number of groups.| | |
|createHeader|Function|Function that returns an empty header DOM element: `()` => `Element`. The element will be used by `assignHeader` to assign an group identfier to it. Note that the element can be reused by any number of groups.| | |
|dataState|String|Specifies an explicit data state: `data` - display items, `loading` - display message as specified by `iconStateLoading` and `labelStateLoading`. `error` - display message as specified by `iconStateError` and `labelStateError`|`data`| |
|disabled|Boolean|Disable component?|`false`| |
|disableWrapping|Boolean|Put whole group in a single row? (Only supported in the flex layout.) |`false`| |
|footers|Boolean or String|Show footers? If `true` (Boolean) show footers. If `pinned` (String) show footers and pin bottomost footer to bottom of collection viewport.|`false`| |
|group|String or Function|Specify grouping. If `group` is a string, it specifies the name of a field that contains a group key in the items. If `group` is a function, it should use the following prototype: (`item`, `index`) => `String`. The returned value is the grouping key.| | |
|groupGap|Number or String|Specifies the gap between groups. Only applicable if grouping is enabled.|`8px`| |
|headers|Boolean or String|Show headers? If `true` (Boolean) show headers. If `pinned` (String) show headers and pin topmost header to top of collection viewport.|`false`| |
|rowHorizontalAlignment|String|Specify horizontal alignment: `space-between`, `space-evenly`,  `left`, `center`, `right`, `stretch`.| `space-evenly`| |
|iconStateEmpty|String|Specify icon to display when `items` is not an array| | |
|iconStateError|String|Specify icon to display when `dataState` is `error`| | |
|iconStateLoading|String|Specify icons to display when `dataState` is `loading`| | |
|iconStateNoData|String|Specify icon to display when `items` is not an array| | |
|itemHeight|Function|Function that returns the DOM element height of an item: `(items[index], index)` => _height_ (a `number`)| | |
|items|Array|The rendered collection items.| | |
|itemWidth|Function|Function that returns the DOM element width of an item: `(items[index], index)` => _width_ (a `number`)| | |
|labelStateEmpty|String|Specify label to display when `items` is an empty array| | |
|labelStateError|String|Specify label to display when `dataState` is `error`| | |
|labelStateLoading|String|Specify label to display when `dataState` is `loading`| | |
|labelStateNoData|String|Specify label to display when `items` is not an array| | |
|lastLineAlignment|String|Specify horizontal alignment on the last row of a group or of the collection. The supported values are: `space-between`, `space-evenly`,  `left`, `center`, `right`, `stretch`. Note that lastLineAlignment is only applicable when the layout is `flex`. If unassigned, the value of rowHorizontalAlignment is used. | | |
|layout|String|Select layout method: `flex`, `grid`, or `table`. `flex` places as many cells as fits on each rown. `grid` aligns the cells in columns. `table` uses one cell per row.|`flex`| |
|leftAlignLastRow|Boolean|Pad the last rows (plural when grouping is active) with ghost cells? Only available in `flex` mode.|`false`| |
|rowGap|Number or String|Specifies the gap between rows|`4px`| |
|selectionMode|String|Specify selection mode: `none` - no cells can be selected. `single` - one cell can be selected. `multiple` - any number of cells can be selected. |`none`| |
|spaceAbove|Number or String|Specifies the space above the first row|`0`| |
|spaceBelow|Number or String|Specifies the space below the last row|`0`| |
|spaceLeft|Number or String|Specifies the space to the left of each row|`0`| |
|spaceRight|Number or String|Specifies the space the right od each row|`0`| |
|sort|String or Function|Specify sorting. If `sort` is a string, it specifies a name of a field that contains a sort key in the items. The sort key is sorted _numerically_ when comparing two numbers and _alphabetically_ otherwise. If `sort` is a function, it should use the following prototype: (`itemA`, `itemB`) => `Number`. The returned value should be less than zero if itemA < itemB, greater than zero if itemA > itemB, and zero if the items have the same sort position.| | |
|stretchRowHeight|Booelan|If `disableWrapping` is `true` and `group` unassigned, stretches the height of the (single) row to the collection height.|`false`| |
|uniformHeight|Boolean|Use the same row height for all rows?|`false`| |
|rowVerticalAlignment|String|Specify vertical alignment: `top`, `center`, `bottom`, `stretch`.|`center`| |
|preventCellContextMenu|Boolean|Specify if right-click menu is visible for the collection cells|`false`| |

### Events

| Name | Data | Description |
|------|------|-------------|
|selection | selection | Triggered when the current selection changes. If `selectionMode` is `single`, then `selection` is an integer that specifies the index of the selected item or `-1` if no item is selected. If `selectionMode` is `multiple`, then `selection` is an array of integers that specifies the indexes of the selected items or `null` if no items are selected.  |
|cell-select-mode|index, selected|Triggered for each item when its selection state changes. `index` specifies the index of the item. `selected` is `true` if the item has become selected or `false` if it has become unselected.|
|cell-clicked|index, clickedOn|Triggered when the user clicks on a cell. `index` is the index of the clicked item. `clickedOn` is the DOM node that was clicked.|
|cell-double-clicked|index, clickedOn|Triggered when cell is double clicked. `index` is the index of the clicked item. `clickedOn` is the DOM node that was clicked.|
|cell-long-clicked|index, clickedOn|Triggered when cell is long clicked. `index` is the index of the clicked item. `clickedOn` is the DOM node that was clicked.|
|cell-right-clicked|index, clickedOn|Triggered when cell is right clicked. `index` is the index of the clicked item. `clickedOn` is the DOM node that was clicked.|



### Methods

| Signature | Description |
|-----------|-------------|
|refreshCell(index)|Inform the collection that the item at `index` needs to be redrawn. Note that the item will only be redrawn if it is currently visible (inside the viewport)|
|reLayout()|Inform the collection that the entire layout must be recomputed. This is needed whenever any item changes sizes. The request is debounced; it will take a while before it takes effect, so there is no performance penalty to call it several times.|
|select(index, selected)|Manually select or unselect individual items. `index` is the index of the item, selected should be: `true` if the item should be selected, `false` if the item should be unselected, and `undefined` if the current selection state should toggle.|
|selectAll()|If `selectionMode` is `multiple`, select all items.|
|unselectAll()|Unselect all items|


## Styling

### Parts

| Part    | Description |
|---------|-------------|
|container|The element that contains the virtual scroller (that displays the items)|
|footer   |A footer row|
|header   |A header row|
|message-container|The container that contains an message, when `items` is invalid or empty, or `dataState` is `error` or `loading`. Note that the message is only displayed if the client has specified an icon and / or a label for the message state.|
|message-icon|The message icon|
|message-label|The message label|
|row     |A row that contains cell|

