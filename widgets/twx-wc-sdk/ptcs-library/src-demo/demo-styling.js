import {PTCS} from 'ptcs-library/library.js';
import {applyTheme} from 'ptcs-styling/styling/apply-theming.js';

// Perhaps this could have been implemented in a more dynamic way...
import {themeProps as baseTheme} from 'ptcs-styling/styling/base-theme/theme-props.js';
import {themeProps as alternative} from 'ptcs-styling/styling/alternative/theme-props.js';
import {themeProps as composerTheme} from 'ptcs-styling/styling/composer-theme/theme-props.js';
import {themeProps as legacyStylesTheme} from 'ptcs-styling/styling/legacy-styles-theme/theme-props.js';
import {themeProps as ptcNewTheme} from 'ptcs-styling/styling/ptc-convergence-theme---green-accent/theme-props.js';
import {themeProps as ptcConvergenceTheme} from 'ptcs-styling/styling/ptc-convergence-theme/theme-props.js';

import 'ptcs-dropdown/ptcs-dropdown.js';
import 'ptcs-styling/styling/themable/ptcs-dropdown.su.js';

const initialTheme = 'ptc-convergence-theme';

const themes = [
    {id: 'base-theme', data: baseTheme, name: 'Base Theme'},
    {id: 'ptc-convergence-theme', data: ptcConvergenceTheme, name: 'Convergence Theme'},
    {id: 'composer-theme', data: composerTheme, name: 'Composer Theme'},
    {id: 'legacy-styles-theme', data: legacyStylesTheme, name: 'Legacy Theme'},
    {id: 'ptc-new-theme', data: ptcNewTheme, name: 'PTC Convergence Theme - Green Accent'},
    {id: 'alternative', data: alternative, name: 'Alternative'}
];

const initialThemeIndex = themes.findIndex(theme => theme.id === initialTheme);

console.assert(initialThemeIndex >= 0);


// Search for parameter in the URL
function getParam(name) {
    let value = null;
    location.search
        .substr(1)
        .split('&')
        .find(item => {
            const pair = item.split('=');
            if (pair[0] === name) {
                value = decodeURIComponent(pair[1]);
            }
        });
    return value;
}


if (getParam('td-active')) {
    console.log('ptcs-multi-theme: the theme designer handles theme styling');
} else {
    // Enable theming for this document
    const headerCSS = `
    .ptcs-theme--header {
        height: 64px;
        padding: 0px 20px;
        background-color: #F2F3F7;
        color: #2D343C;
        font-size: 24px;
        display: flex;
        justify-content: space-between;
        align-items: center;
    }

    header.ptcs-theme--header {
        position: fixed;
        left: 0px;
        right: 0px;
        top: 0px;
        z-index: 10;
    }

    .ptcs-theme--select {
        width: 200px;
        margin: 0px !important;
    }`;


    let currentThemeIndex = -1; // Avoid loading the same theme data twice

    function apply(index) {
        if (currentThemeIndex !== index) {
            applyTheme(themes[currentThemeIndex = index].data);
        }
    }

    apply(initialThemeIndex);

    function init() {
        const m = /\/node_modules\/(ptc(s|1)-[a-z0-9-]+)/.exec(location.pathname);
        const appName = m ? m[1] : location.pathname;
        const body = document.body;

        let themeHead = body.querySelector('.ptcs-theme--header');
        if (!themeHead) {
            themeHead = PTCS.createElement('header', {class: 'ptcs-theme--header'});
            body.insertBefore(themeHead, body.firstChild, themeHead);
            const orgMargin = getComputedStyle(body).getPropertyValue('margin-top');
            body.style.marginTop = `calc(72px + ${orgMargin})`;
        }

        // eslint-disable-next-line max-len
        themeHead.innerHTML = `<style>${headerCSS}</style><div>&lt;${appName}&gt;</div><ptcs-dropdown items='${JSON.stringify(themes.map(t => t.name))}' selected="${initialThemeIndex}" class="ptcs-theme--select"></ptcs-dropdown>`;
        themeHead.querySelector('ptcs-dropdown').addEventListener('selected-changed', ev => apply(ev.detail.value));
    }

    PTCS.ready(init);
}
