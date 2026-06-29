import {LitElement, html, css} from 'lit';
import {styleMap} from 'lit/directives/style-map.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';

const focusProperties = [
    '--ptcs-focus-overlay--padding',
    '--ptcs-focus-overlay--padding-left',
    '--ptcs-focus-overlay--padding-right',
    '--ptcs-focus-overlay--padding-top',
    '--ptcs-focus-overlay--padding-bottom',
    '--ptcs-focus-overlay--border-style',
    '--ptcs-focus-overlay--border-width',
    '--ptcs-focus-overlay--border-radius',
    '--ptcs-focus-overlay--border-color',
    '--ptcs-focus-outline'
];

// Get all selectors and properties declared in component that affect the focusProperties
function componentProps(el) {
    function collect(styleSheets, rules) {
        if (styleSheets) {
            for (const sheet of styleSheets) {
                for (const rule of sheet.cssRules) {
                    if (focusProperties.some(propName => rule instanceof CSSStyleRule && rule.style.getPropertyValue(propName))) {
                        rules.push({
                            selector: rule.selectorText,
                            props:    focusProperties.filter(propName => rule.style.getPropertyValue(propName))
                        });
                    }
                }
            }
        }
        return rules;
    }

    return el.shadowRoot ? collect(el.shadowRoot.styleSheets, collect(el.shadowRoot.adoptedStyleSheets, [])) : [];
}

function stripHost(selector) {
    if (selector === ':host') {
        return '*';
    }
    const m = /^:host\((.+)\)$/.exec(selector);
    return m ? m[1] : selector;
}

function isOutOfContext(el) {
    const root = el.getRootNode && el.getRootNode();
    const host = root && root.host;
    return host && (host.__$mainCmpnt || isOutOfContext(host));
}

function getFocusProperties(el) {
    const props = {}; // Returned value

    const apply = (style, propNames) => propNames.forEach(name => {
        if (!props[name]) {
            props[name] = style.getPropertyValue(name);
        }
    });

    // Is el already in the correct styling context?
    if (!isOutOfContext(el)) {
        apply(getComputedStyle(el), focusProperties);
        return props;
    }

    function applyHostRules(rules) {
        const propNames = rules.reduce((a, rule) => {
            if (rule.selector.startsWith(':host') && el.matches(stripHost(rule.selector))) {
                a.push(...rule.props);
            }
            return a;
        }, []);
        if (propNames.length) {
            apply(getComputedStyle(el), propNames);
        }
    }

    // First check if el has any host rules
    applyHostRules(componentProps(el));

    while (el) {
        const host = el.__$mainCmpnt || el.getRootNode().host;
        if (host) {
            const rules = componentProps(host);

            for (; el; el = el.parentElement) {
                // Element styling
                apply(el.style, focusProperties);

                // Style sheet properties
                const a = rules.filter(rule => !rule.selector.startsWith(':host') && el.matches(rule.selector));
                if (a.length) {
                    const cs = getComputedStyle(el);
                    a.forEach(rule => apply(cs, rule.props));
                }
            }

            el = host;

            if (!isOutOfContext(el)) {
                break;
            }

            // Check if host has any direct rules
            applyHostRules(rules);
        } else {
            break;
        }
    }

    apply(getComputedStyle(el), focusProperties);

    return props;
}

PTCS.FocusOverlay = class extends PTCS.BehaviorStyleable(L2Pw(LitElement)) {
    static get styles() {
        return css`
            :host {
                position: absolute;
                z-index: 99997;
                pointer-events: none;
                box-sizing: border-box;
                overflow: hidden;
            }

            div {
                position: absolute;
                box-sizing: border-box;
            }`;
    }

    static get properties() {
        return {
            // Focus border properties
            sTransform:   String,
            sWidth:       String,
            sHeight:      String,
            borderStyle:  String,
            borderWidth:  String,
            borderRadius: String,
            borderColor:  String,
            outline:      String
        };
    }

    render() {
        const transform = this.sTransform;
        const width = this.sWidth;
        const height = this.sHeight;
        const {borderStyle, borderWidth, borderRadius, borderColor, outline} = this;

        return html`<div style=${styleMap({transform, width, height, borderStyle, borderWidth, borderRadius, borderColor, outline})}></div>`;
    }

    static get is() {
        return 'ptcs-focus-overlay';
    }

    hide() {
        this.style.display = 'none';
    }

    _getPadding(padding, defVal) {
        let retVal = defVal;
        if (padding) {
            const m = /^\s*(-?[0-9.]+)([a-zA-Z]*)\s*/g.exec(padding);
            if (m && (m[2] === '' || m[2] === 'px')) {
                const x = parseFloat(m[1]);
                if (!isNaN(x)) {
                    retVal = x;
                }
            }
        }
        console.assert(typeof retVal === 'number', 'Padding not a number');
        return retVal;
    }

    show(el, area) {
        if (!el || !area || !(area.w > 0) || !(area.h > 0)) {
            this.hide();
            return;
        }

        // CSS variables may change dynamically, so we need to check every time
        const prop = getFocusProperties(el);

        // Padding (distance between focused element and the focus border)
        const paddingAll = this._getPadding(prop['--ptcs-focus-overlay--padding'], 8);
        const paddingLeft = this._getPadding(prop['--ptcs-focus-overlay--padding-left'], paddingAll);
        const paddingRight = this._getPadding(prop['--ptcs-focus-overlay--padding-right'], paddingAll);
        const paddingTop = this._getPadding(prop['--ptcs-focus-overlay--padding-top'], paddingAll);
        const paddingBottom = this._getPadding(prop['--ptcs-focus-overlay--padding-bottom'], paddingAll);

        // The _getPadding() call should always return the value as a number, any 'px' part will be removed...
        const x1 = area.x - paddingLeft;
        const x2 = area.x + area.w + paddingRight;
        const y1 = area.y - paddingTop;
        const y2 = area.y + area.h + paddingBottom;

        // Bounding box of content: the focus element may not extrude this area, because that affects the scrollbars
        const {body} = document;
        const cs = getComputedStyle(body);
        const marginLeft = PTCS.cssDecodeSize(cs.marginLeft, body) || 0;
        const marginRight = PTCS.cssDecodeSize(cs.marginRight, body) || 0;
        const marginTop = PTCS.cssDecodeSize(cs.marginTop, body, true) || 0;
        const marginBottom = PTCS.cssDecodeSize(cs.marginBottom, body, true) || 0;
        const bWidth = Math.max(marginLeft + body.scrollWidth + marginRight, window.innerWidth);
        const bHeight = Math.max(marginTop + body.scrollHeight + marginBottom, window.innerHeight);

        if ((x1 >= bWidth || x2 < 0) && (y1 >= bHeight || y2 < 0)) {
            // Not in viewport
            this.hide();
            return;
        }

        // Place internal border element
        this.sTransform = `translate(${Math.min(x1, 0)}px,${Math.min(y1, 0)}px)`;
        this.sWidth = `${x2 - x1}px`;
        this.sHeight = `${y2 - y1}px`;

        // Place focus viewport
        const style = this.style;
        style.left = `${Math.max(x1, 0)}px`;
        style.top = `${Math.max(y1, 0)}px`;
        style.width = `${Math.min(x2, bWidth) - Math.max(x1, 0)}px`;
        style.height = `${Math.min(y2, bHeight) - Math.max(y1, 0)}px`;

        // Style focus border
        const bstyle = prop['--ptcs-focus-overlay--border-style'] || 'solid';
        const bwidth = prop['--ptcs-focus-overlay--border-width'] || '2px';
        const bradius = prop['--ptcs-focus-overlay--border-radius'] || '';
        const {hide} = area;
        if (hide) {
            const s = side => hide[side] ? 'none' : bstyle;
            const w = side => hide[side] ? '0px' : bwidth;
            const r = (side1, side2) => (hide[side1] || hide[side2]) ? '0px' : bradius;
            this.borderStyle = `${s('top')} ${s('right')} ${s('bottom')} ${s('left')}`;
            this.borderWidth = `${w('top')} ${w('right')} ${w('bottom')} ${w('left')}`;
            this.borderRadius = `${r('top', 'left')} ${r('top', 'right')} ${r('bottom', 'right')} ${r('bottom', 'left')}`;
        } else {
            this.borderStyle = bstyle;
            this.borderWidth = bwidth;
            this.borderRadius = bradius;
        }
        this.borderColor = prop['--ptcs-focus-overlay--border-color'] || '#0094c8';
        this.outline = prop['--ptcs-focus-outline'] || '';

        // Display focus
        style.display = 'block';
    }
};

customElements.define(PTCS.FocusOverlay.is, PTCS.FocusOverlay);
