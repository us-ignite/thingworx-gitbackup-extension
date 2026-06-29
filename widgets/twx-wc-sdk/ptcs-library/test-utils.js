import {expect} from '@open-wc/testing/index.js';
import {waitUntil} from '@open-wc/testing-helpers';

export function checkTooltip(lines, notitle = false, {icons, colors} = {}) {
    const tEl = document.getElementById('ptcs-tooltip-overlay');
    expect(window.getComputedStyle(tEl).visibility).to.not.eql('hidden', 'tooltip should be visible');

    const tElRoot = document.getElementById('ptcs-tooltip-overlay').shadowRoot;

    if (!Array.isArray(lines)) {
        lines = [lines];
    }

    if (!notitle) {
        const title = tElRoot.querySelector('[part=title]');
        expect(title.textContent).to.be.eql(lines[0]);
    }

    const textLines = tElRoot.querySelectorAll('[part=text]');
    for (let i = 0; i < textLines.length; i++) {
        let text = textLines[i].textContent;
        if (text.indexOf(' 00:00:00') !== -1) {
            text = text.substring(0, text.indexOf(' 00:00:00'));
        }
        expect(text).to.be.eql(lines[notitle ? i : i + 1]);
    }

    if (icons) {
        const ttIcons = tElRoot.querySelectorAll(`[part=${notitle ? 'text' : 'title'}-icon]`);
        expect(ttIcons.length).to.be.eql(icons.length);

        for (let i = 0; i < ttIcons.length; i++) {
            expect(ttIcons[i].icon).to.be.eql(icons[i]);
        }
    }

    if (colors) {
        const ttMarkers = tElRoot.querySelectorAll('[part=marker]');
        expect(ttMarkers.length).to.be.eql(colors.length);

        for (let i = 0; i < ttMarkers.length; i++) {
            expect(window.getComputedStyle(ttMarkers[i]).backgroundColor).to.be.eql(colors[i]);
        }
    }
}

export function checkTooltipHide() {
    const tEl = document.getElementById('ptcs-tooltip-overlay');
    if (window.getComputedStyle(tEl).visibility === 'hidden') {
        return true;
    }
    return false;
}

export function getBaseURL(component) {
    return `/base/src/components/${component}`;
}

/**
 * Waits until a specified assertion test is true.
 *
 * @param {Function} assertionFunc - The assertion function to be used.
 * @param {Object} [options={}] - Options for the waiting period.
 * @param {number} [options.timeout=1000] - Maximum time to wait (in milliseconds). Default is 1000 ms.
 * @param {number} [options.interval=25] - Interval between checks (in milliseconds). Default is 25 ms.
 */
export const waitFor = async(assertionFunc, options = {}) => {
    if (typeof assertionFunc !== 'function') {
        throw new Error('The assertionFunc parameter must be a function');
    }

    const {timeout = 1000, interval = 25} = options;
    let assertionError;

    const condition = () => {
        try {
            assertionFunc();
            return true;
        } catch (err) {
            assertionError = err;
            return false;
        }
    };

    try {
        await waitUntil(condition, null, {timeout, interval});
    } catch (error) {
        throw assertionError || error;
    }
};
