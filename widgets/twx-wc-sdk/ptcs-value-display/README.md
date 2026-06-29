# ptcs-value-display

## Visual

<img src="img/ptcs-value-display.png">

The value display consists of an optional *label* (referred to as the KEY in the key / label data pair of the *property display* component)
and a display area in which to display a formatted *value*.


## Overview

The value display web component displays the data formats text, password, checkbox, image, link and html _natively_, either using other PTCS web components (ptcs-label, ptcs-textfield, ptcs-checkbox, ptcs-image (indirectly via ptcs-modal-image-popup), ptcs-link&mdash;respectively), or as plain HTML.

The native `type` values are

* `text`
* `image`
* `html`
* `link`
* `password`
* `checkbox`



## Usage Examples

### Text Value

```
<ptcs-value-display label = "A Famous Quote"
                    data  = "Veni, vidi, vici"></ptcs-value-display>

```
See the demo page for many additional examples with bound type.

## Component API

The value display grows to accommodate its content. You can constrain the value
display area using maximum width / height or by setting a fixed size.

### Properties

| Property           | Type   | Description                                                                    | Default     |
| ------------------ | ------ | ------------------------------------------------------------------------------ | ----------- |
| backdropColor      | String | The modal backdrop color                                                       |             |
| backdropOpacity    | Number | The modal backdrop opacity, a value betwenn 0-1.0                              |             |
| data               | Object | The data to display                                                            |             |
| defaultText        | String | Fallback textual contents to show when there is no data to display             |             |
| disabled           | Boolean| Is the value display disabled?                                                 | false       |
| height             | Number | Fixed height in pixels for the value display                                   |             |
| horizontalAlignment| String | Horizontal alignment within the display area: `left`, `center`, `right`        | "left"      |
| verticalAlignment  | String | Vertical alignment within the display area: `flex-start`, `center`, `flex-end` | "flex-start"|
| itemMeta           | Object | The type of the data to display. If you are running inside of MashupBuilder application you must use `{baseType: 'NUMBER'}` instead: This will apply default MashupBuilder formatter rules. `baseType` can be any type supported by MashupBuilder. Using it will override selector attribute described above. You should use only one of `type` or `baseType`|`{type: 'text')`|
| label              | String | The (key) label of the data value                                              | ""          |
| labelAlignment     | String | Horizontal alignment of the key label: `left`, `center`, `right`               | "left"      |
| maxHeight          | Number | Maximum height in pixels for the value display                                 |             |
| maxWidth           | Number | Maximum width in pixels for the value display                                  |             |
| modalHeight        | Number | Height of the _general_ modal pop-up (not image)                               | 380         |
| modalWidth         | Number | Width of the _general_ modal pop-up (not image)                                | 600         |
| selector           | Any    | Selector for the value holder. If `null` the value is `data`; if the selector is of type `string` it identifies the name of the property in `data` that holds the value; otherwise it is interpreted as a function name to apply on `data`| null |
| textWrap           | Boolean| Allow text content to wrap in the value?                                       | false       |
| tooltip            | String | Tooltip text for the value display. In addition to any tooltip text on the web component itself, the tooltip will be populated from truncation overflow (when the value is truncated with ellipsis on account of size constraints)|""|
| tooltipIcon        | String | Icon for the tooltip text                                                      | ""          |
| twNumberFormatToken| String | Format numbers according to TWX localization token. Ex.: <ptcs-value-display ... tw-number-format-token='numberFormat_Custom'/>||
| overflowOption     | String | Controls display of overflow using disclosure button (default), 'Show More' textual link, or horizontal ellipsis with content truncation. Corresponding values are `disclosure`, `showmore`, and `ellipsis` (respectively) | "disclosure" |
| valueDisplayType   | String | One of the pre-defined label types (header, sub-header, label, or body)        |             |
| width              | Number | Fixed width in pixels for the value display                                    |             |

### Events

| Name             | Description                                         |
| ---------------- | --------------------------------------------------- |
|popup-close-action| Triggered when clicking on close button of a pop-up |

### Methods

_No methods_


## Styling

### Parts

The ptcs-value-display has following parts:

| Part                       | Description                                                                      |
| -------------------------- | -------------------------------------------------------------------------------- |
| root                       | The container for the value display                                              |
| value-display-area         | The container for the are where the value is displayed                           |
| overflow-control           | A container to monitor overflow of the value                                     |
| value-display-label        | The label above the value                                                        |
| value-container            | The container for the displayed value                                            |
| item-value-container       | The container for `part=item-value`                                              |
| item-value                 | The displayed value content holder (this can be various elements)                |
| disclosure-button-overlay  | This container covers the bottommost part of the display area on content overflow|
| disclosure-button-container| The 34px x 34px hit area for the disclosure button                               |
| disclosure-button          | The disclosure button that shows the overflowing contents in a pop-up            |
| show-button                | The `Show more` button container, an alternative to the disclosure button        |
| text-link                  | The textual (Show More / Show Less) link                                         |

The `ptcs-value-display-popup` is a child of `<body>`, shown when the disclosure button is clicked. It holds the key label and value as modal popup, without its inline size constraints. It has following parts:

| Part                        | Description                                                                   |
| --------------------------- | ----------------------------------------------------------------------------- |
| popup-container             | The root container of the popup                                               |
| value-display-popup         | The container for the value display in the pop-up, used for centering purposes|
| live-contents-area-popup    | The container for the close button section (part `popup-close-button-container`), the key label in the pop-up (part `value-display-label-popup`), and the part `value-container-popup`|
| popup-close-button-container| The 34px x 34px hit area that contains the close button                       |
| popup-close-button          | The button to close the pop-up                                                |
| value-display-label-popup   | The key label in the pop-up modal dialog                                      |
| value-container-popup       | The value container in the modal pop-up, parent of item-value-container       |
| item-value-container        | The container for `part=item-value`                                           |
| item-value                  | The displayed value content holder                                            |

#### Custom Styling

The `Show More` disclosure link has the PTCS Base Theme primary background color set in the theme. If you were to use this component programmatically inside of your own component with another background color than the PTCS Base Theme, say `yellow`, you could override the background color of the "Show More" link (associated to `part=show-button`) in a couple of ways:

1. By injecting a `ptcs-style-unit` into the DOM:

```xml
   <ptcs-style-unit wc="PTCS-VALUE-DISPLAY">
     :host(:not([_show-all])) [part~=show-button] {
     background-color: yellow;
     box-shadow: yellow 0px -8px 16px;
     }
   </ptcs-style-unit>
```
where the `:host()` predicate ensures to only apply the styling in the collapsed state when "Show More" is showing.


2. Alternatively, you could use the [CSS Shadow Parts Draft Specification](https://drafts.csswg.org/css-shadow-parts/) (<https://drafts.csswg.org/css-shadow-parts/>) syntax:

```css
    ptcs-value-display::part(show-button) {
    background-color: yellow;
    box-shadow: yellow 0px -8px 16px;
    }
```

This latter option only works well in browsers that have Shadow DOM support such as Chrome or Mozilla FireFox.


### State attributes
| Attribute | Description                                       | Part  |
| --------- | ------------------------------------------------- | ----- |
| text-wrap | Set when the value area contents should line wrap | :host |


