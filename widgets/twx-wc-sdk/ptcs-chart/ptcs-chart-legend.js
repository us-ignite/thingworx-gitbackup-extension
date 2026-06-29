import {LitElement, html, css} from 'lit';
import {map} from 'lit/directives/map.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import {enableSvgGradients, disableSvgGradients} from 'ptcs-library/svg-gradients.js';
import 'ptcs-behavior-binary/ptcs-behavior-binary.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-checkbox/ptcs-checkbox.js';
import 'ptcs-div/ptcs-div.js';

PTCS.ChartLegend = class extends PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))) {

    static get styles() {
        return css`
        :host {
            width: 100%;
            height: 100%;
            display: flex;
            flex-direction: column;
            justify-content: flex-start;
            align-content: flex-start;
            overflow: auto;
            box-sizing: border-box;
        }

        :host([align=center]:not([horizontal]):not([scrollbar])) {
            justify-content: center;
        }

        :host([align=end]:not([horizontal]):not([scrollbar])) {
            justify-content: flex-end;
        }

        [part=grid] {
            display: grid;
            grid-template-columns: var(--ptcs-legend-col-widths, 1fr 1fr 1fr);
        }

        :host([align=start]) [part=grid] {
            justify-content: start;
        }

        :host([align=center]) [part=grid] {
            justify-content: center;
        }

        :host([align=end]) [part=grid] {
            justify-content: end;
        }

        [part=item] {
            justify-self: start;
            display: flex;
            flex-direction: row;
            justify-content: flex-start;
            align-items: center;
            max-width: var(--ptcs-legend-max-width);
            outline: 1px solid white;
        }

        [part=item][hidden] {
            display: none !important;
        }

        :host([filter]) [part=item]:not(.legend-title) {
            cursor: pointer;
        }

        [part=marker] {
            display: flex;
            flex: 0 0 auto;
        }

        :host([shape=none]) [part=marker] {
            display: none;
        }

        :host([shape=circle]) [part=marker] {
            border-radius: 50%;
        }

        ptcs-checkbox {
            min-height: unset;
            padding: unset;
        }

        :not([has-icon]) > ptcs-icon {
            display: none;
        }

        ptcs-icon {
            width: 100%;
            height: 100%;
        }

        :host(:not([filter])) ptcs-checkbox {
            display: none;
        }

        .legend-title [part=marker], .legend-title [part=checkbox] {
            display: none;
        }`;
    }

    render() {
        return html`<div id="grid" part="grid" @click=${this._clickLegend}>${map(this._items, item => html`<div part="item"
            legend-id=${item.$legend} legend=${this._legend(item.$legend)} class=${item.class} ?hidden=${item.empty}>
                <ptcs-div part="marker" ._depfield=${item.depfield} ?has-icon=${this._hasIcon(item)} style=${this._setStyle(item)}>
                    <ptcs-icon part="icon" .icon=${this._icon(item)}></ptcs-icon>
                </ptcs-div>
                <ptcs-checkbox part="checkbox" no-tabindex single-line checked
                               .disabled=${this.disabled} @checked-changed=${this._checkedChanged}></ptcs-checkbox>
                <ptcs-label variant=${item._$part || 'label'} part=${item._$part || 'label'} .label=${item.label}></ptcs-label>
                </div>`)}
            </template>
        </div>`;
    }

    static get is() {
        return 'ptcs-chart-legend';
    }

    static get properties() {
        return {
            horizontal: {
                type:    Boolean,
                reflect: true
            },

            // Do the legend need (vertical) scrollbars? [only enabled in horizontal mode]
            // This is a workaround for a possible CSS / Browser problem (TW-103140)
            scrollbar: {
                type:    Boolean,
                reflect: true
            },

            maxWidth: {
                type:      String,
                attribute: 'max-width'
            },

            align: {
                type:    String,
                value:   'start',
                reflect: true
            },

            disabled: {
                type:    Boolean,
                reflect: true
            },

            // Indexes of selected items
            selected: {
                type:   Array,
                notify: true
            },

            shape: {
                type:    String,
                reflect: true
            },

            // If specified, uses icon instead of shape
            icon: {
                type: String
            },

            items: {
                type: Array
            },

            // Group items according to .group information? {id, label}
            grouping: {
                type: Boolean
            },

            // Massaged items. Can include grouping
            _items: {
                type:     Array,
                computed: '_computeItems(items, grouping)'
            },

            // Should legend filter the related chart(s)
            filter: {
                type:     Boolean,
                observer: '_createSelected',
                reflect:  true
            }
        };
    }

    static get observers() {
        return [
            '_itemsChanged(_items)',
            '_updateMaxWidth(maxWidth, horizontal)'
        ];
    }

    constructor() {
        super();

        this.items = [];
        this._unselected = {}; // _unselected[i] = true, if i is unselected
        this._resizeObserver = new ResizeObserver(this._refreshGrid.bind(this));
    }

    ready() {
        super.ready();
        this.addEventListener('keydown', this._keyDown.bind(this));
    }

    connectedCallback() {
        super.connectedCallback();
        this._resizeObserver.observe(this);
        enableSvgGradients(this, this._gradientsDefs.bind(this));
    }

    _gradientsDefs(length) {
        let svg = this.shadowRoot.getElementById('svg-gradients');
        if (svg) {
            return svg.firstElementChild;
        }
        if (!length) {
            return undefined;
        }

        svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('id', 'svg-gradients');
        svg.style.width = '0';
        svg.style.height = '0';
        svg.style.userSelect = 'none';

        const defs = document.createElementNS('http://www.w3.org/2000/svg', 'defs');
        svg.appendChild(defs);
        this.shadowRoot.prepend(svg);
        return defs;
    }

    disconnectedCallback() {
        this._resizeObserver.unobserve(this);
        disableSvgGradients(this);
        super.disconnectedCallback();
    }

    _legend(index) {
        // This controls the legend styling. Max 24 legends are styled, then styling is recycled.
        return index !== undefined ? `L${(index % 24) + 1}` : null;
    }

    _icon(item) {
        return item.icon || this.icon;
    }

    _hasIcon(item) {
        return (item.icon || this.icon) && true;
    }

    _setStyle(item) {
        const {color} = item;
        return color ? `color:${color};background:${color}` : '';
    }

    _computeItems(items, grouping) {
        if (!Array.isArray(items)) {
            return [];
        }

        const dup = (item, index) => Object.assign(typeof item === 'object' ? {...item} : {label: item}, {$legend: index});

        if (grouping && items.every(item => (item.group && item.group.id) || item.empty)) {
            // Group legend according to group keys
            const titles = [];
            const groups = {};

            items.forEach((item, index) => {
                if (item.empty) {
                    return;
                }
                const id = item.group.id;
                if (!groups[id]) {
                    titles.push(groups[id] = []);
                }
                groups[id].push(dup(item, index));
            });

            // There must be at least two groups to enable grouping
            if (titles.length < 2) {
                return items.map(dup);
            }

            // Rearrange legend items
            return titles.reduce((a, g) => {
                a.push({label: g[0].group.label || g[0].group.id || 'No title', class: 'legend-title', _$part: 'title'});
                g.forEach(item => a.push(item));
                return a;
            }, []);
        }

        // No grouping
        return items.map(dup);
    }

    _itemsChanged() {
        // New legend items
        this._refreshGrid();

        requestAnimationFrame(() => {
            // Transfer filter settings to new items
            this.shadowRoot.querySelectorAll('[part=item][legend-id] ptcs-checkbox').forEach(el => {
                el.checked = !this._unselected[Number(el.parentNode.getAttribute('legend-id'))];
            });

            // Create the new filter
            this._createSelected();
        });
    }

    _refreshGrid() {
        if (!this.__callRefreshGrid) {
            this.__callRefreshGrid = true;
            requestAnimationFrame(() => {
                this.__callRefreshGrid = false;
                // Max-width of items might have been affected
                this.__updateMaxWidth(this.maxWidth, this.horizontal);

                // Collect legend items and compute max width
                const gridEl = this.$.grid;
                const list = [];
                let wMax = 0;
                for (let el = gridEl.firstChild; el; el = el.nextSibling) {
                    if (el.getAttribute && el.getAttribute('part') === 'item' && !el.hasAttribute('hidden')) {
                        const w = el.clientWidth;
                        if (!(wMax >= w)) {
                            wMax = w;
                        }
                        list.push(el);
                    }
                }

                // Compute number of columns
                const gap = PTCS.cssDecodeSize(getComputedStyle(gridEl).getPropertyValue('grid-column-gap'), gridEl);

                // 1, 2 or 3 columns? nc finds column that doesn't fit
                const nc = [1, 2].find(i => (i + 1) * wMax + i * gap > this.clientWidth);

                // Note: 4 legends with space for 3 columns will use 2 columns.
                //       >4 legends will use 3 columns
                const numCol = Math.min(
                    // eslint-disable-next-line no-nested-ternary
                    this.horizontal ? (nc > 0 ? nc : list.length !== 4 ? 3 : 2) : 1,
                    list.length);

                // Assign legend item to columns
                const d = Math.floor((list.length - 0.5) / numCol) + 1;
                list.forEach((el, index) => {
                    const col = Math.floor(index / d) + 1;
                    const row = index - (col - 1) * d + 1;
                    el.style.gridColumn = col;
                    el.style.gridRow = row;
                });
                // Assign CSS grid-template-columns for the item grid
                const cw = `${wMax}px`;
                this.style.setProperty('--ptcs-legend-col-widths', [...Array(numCol).keys()].map(() => cw).join(' '));
            });
        }
    }

    _createSelected() {
        const compute = () => {
            const r = [];
            const num = this.items ? this.items.length : 0;
            for (let i = 0; i < num; i++) {
                if (!this._unselected[i]) {
                    r.push(i);
                }
            }
            return r;
        };

        const selected = this.filter ? compute() : null;
        if (PTCS.sameArray(selected, this.selected)) {
            // Filter has not changed. Don't generate a change event
            return;
        }

        this.selected = selected;

        this._refreshGrid();
    }

    _checkedChanged(ev) {
        const el = ev.target.closest('[part=item]');
        const id = el && el.getAttribute('legend-id');
        if (!id) {
            return; // A hidden title checkbox changed its value? Hm...
        }

        const index = Number(id);
        const unselected = ev.detail.value !== true;
        if (this._unselected[index] !== unselected) {
            this._unselected[index] = unselected;
        }

        this._createSelected();
    }

    _clickLegend(ev) {
        if (this.disabled) {
            return;
        }

        const el = ev.target.closest('[part=item]');
        if (!el || !el.hasAttribute('legend-id')) {
            return; // Probably clicked on grouping title. Ignore.
        }

        // Focus on clicked legend
        this._focusOn(el.getAttribute('legend-id'));

        // Manually toggle checkbox? (Not needed if the checkbox was clicked directly)
        if (!ev.defaultPrevented) {
            const checkbox = el.querySelector('ptcs-checkbox[part=checkbox]');
            checkbox.checked = !checkbox.checked;
        }
    }

    // Callback when any values changes
    _updateMaxWidth(maxWidth, horizontal) {
        this.__updateMaxWidth(maxWidth, horizontal);
        this._refreshGrid();
    }

    // Handle changed properties or resized component
    __updateMaxWidth(maxWidth, horizontal) {
        let mw = maxWidth ? PTCS.cssDecodeSize(maxWidth, this) : NaN;
        if (horizontal && !(mw < this.clientWidth)) {
            mw = this.clientWidth;
        }
        if (isNaN(mw)) {
            this.style.removeProperty('--ptcs-legend-max-width');
        } else {
            this.style.setProperty('--ptcs-legend-max-width', `${mw}px`);
        }

        // Do the legend need a (vertical) scrollbar?
        this.scrollbar = !horizontal && ((this.scrollHeight > this.clientHeight) || (this.$.grid.offsetHeight > this.offsetHeight));
    }

    _resetToDefaultValues() {
        this.shadowRoot.querySelectorAll('[part=item][legend-id] [part~=checkbox]').forEach(checkbox => {
            checkbox.checked = true;
        });
    }

    _initTrackFocus() {
        this._trackFocus(this, () => this._focus);
    }

    _notifyFocus() {
        if (this._focus) {
            this._focus.scrollIntoViewIfNeeded();
        } else {
            this._focusOn(0);
        }
    }

    _focusOn(legendId) {
        const el = this.shadowRoot.querySelector(`[part=item][legend-id="${legendId}"]`);
        this._focus = el ? el : undefined;
        if (el) {
            el.scrollIntoViewIfNeeded();
        }
    }

    _keyDown(ev) {
        const focusIndex = Number(this._focus && this._focus.getAttribute('legend-id'));
        if (isNaN(focusIndex)) {
            return;
        }

        // Is legend item visible?
        const isVisible = i => !this.items[i].empty;

        // Find visible legend item backwards
        const prev = i => {
            for (; i >= 0; i--) {
                if (isVisible(i)) {
                    return i;
                }
            }
            for (i = this.items.length - 1; i >= 0; i--) {
                if (isVisible(i)) {
                    return i;
                }
            }
            return undefined;
        };

        // Find visible legend item forwards
        const next = i => {
            for (; i < this.items.length; i++) {
                if (isVisible(i)) {
                    return i;
                }
            }
            for (i = 0; i < this.items.length; i++) {
                if (isVisible(i)) {
                    return i;
                }
            }
            return undefined;
        };

        let nextIndex;
        switch (ev.key) {
            case 'Home':
            case 'PageUp':
                nextIndex = next(0);
                break;
            case 'ArrowUp':
            case 'ArrowLeft':
                nextIndex = prev(focusIndex - 1);
                break;
            case 'End':
            case 'PageDown':
                nextIndex = prev(this.items.length - 1);
                break;
            case 'ArrowDown':
            case 'ArrowRight':
                nextIndex = next(focusIndex + 1);
                break;
            case ' ':
                this._focus.click();
                break;
            default:
                // Not handled
                return;
        }

        // We consumed this keyboard event. Don't propagate
        ev.preventDefault();

        if (nextIndex !== undefined && nextIndex !== focusIndex) {
            this._focusOn(nextIndex);
        }
    }
};

customElements.define(PTCS.ChartLegend.is, PTCS.ChartLegend);
