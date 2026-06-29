# Prebuilt Styles

## Introduction

The Theme Engine enables theme developers to create custom themes. You can use the style engine to generate the required styling for a custom theme. Using the styling engine requires access to the full theme engine package, which may affect performance. As an alternative, prebuilt styles for the default themes are available when a web page does not require theme and style editing.

The prebuilt styles are available as two variants:

- Plain JavaScript code that automatically mounts the styling.

- ES6 modules, which allows the developer to import any number of themes, control where they should be placed, and dynamically switch between different themes.

The prebuilt styles are delivered as npm module and packaged with the twx-wc-sdk

## Automatically mounted styles

The automatically mounted styles are located in the `prebuilt-load` dist folder. You can include a style using an HTML  `<script>` element:

~~~js
<script src="twx-wc-sdk/ptcs-standalone-themes/dist/prebuilt-load/ptc-convergence-theme-load.js"></script>
~~~

The script mounts the styling in the `<head>` element.

When you include several prebuilt styles, only one is used. The last mounted style replaces any previous styling, if any exist.

## Styles as modules

The module styles are located in the `prebuilt` distribution folder.

Each style module exports three functions:

- `name()` - Returns the name of the theme.

- `styling()` - Returns the styling of the theme as a string.

- `mount()` - Mounts the styling at the regular place. If another theme has already been mounted, including auto-loaded styles, it will be replaced.

### Using the `styling()` function

`styling()` returns the entire styling as a single string. The following snippet shows how to insert the styling into an HTML element:

~~~js
import {styling} from 'twx-wc-sdk/ptcs-standalone-themes/dist/prebuilt/ptc-convergence-theme-load.js';

element.innerHTML = styling();
~~~

### Using the `mount()` function

`mount()` mounts the styling at the "regular place". This is the same place that is used for automatically mounted styles, so this function replaces any previously auto-mounted or module-mounted styling.

The following example snippet mounts the _Composer Theme_, then replaces it with the _Convergence Theme_ after five seconds:

~~~js
import {name as convergenceName, mount as mountConvergenceTheme} from 'twx-wc-sdk/ptcs-standalone-themes/dist/prebuilt/ptc-convergence-theme.js';

import {name as composerName, mount as mountComposerTheme} from 'twx-wc-sdk/ptcs-standalone-themes/dist/prebuilt/composer-theme.js';

console.log('Mounting ' + composerName());
mountComposerTheme();

setTimeout(() => {
    console.log('Mounting ' + convergenceName());
    mountConvergenceTheme();
}, 5000);
~~~
