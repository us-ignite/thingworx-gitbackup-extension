import {LitElement, html, css} from 'lit';
import {map} from 'lit/directives/map.js';
import {styleMap} from 'lit/directives/style-map.js';
import {classMap} from 'lit/directives/class-map.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import './ptcs-tab.js';
import './ptcs-tabs.js';
import 'ptcs-page-select/ptcs-page-select.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';

PTCS.TabSet = class extends PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))) {
    static get styles() {
        return css`
        :host {
            display: flex;
            flex-direction: column;

            min-width: 168px;
            min-height: 36px;

            box-sizing: border-box;
            overflow: hidden;
        }

        [part="tabs-header"] {
            flex: none;
            overflow: hidden !important;
        }

        [part="pages"] {
            flex: auto;
        }

        [part="pages"].fixed {
            overflow: auto;
        }

        [part="divider"] {
            z-index: 12;
        }

        :host([disabled]) {
            pointer-events: none;
        }

        /* to override content-box definition in IDE mode (IE case) we have to generate rule with higher priority */
        ptcs-tabs, ptcs-tab {
            box-sizing: border-box;
        }

        /* Avoid scrollbar when focus border surronds overflow button */
        ptcs-tabs {
            padding-right: calc(1px + var(--ptcs-focus-overlay--border-width, 0px));
        }`;
    }

    render() {
        const classes = {fixed: this.tabHeight > 0};
        const stylestabHeight = {height: this.tabHeight > 0 ? this.tabHeight + 'px' : ''};
        const stylestabNameHeight = {height: this._getTabNameHeight(this.tabNameHeight)};
        return html`
        <ptcs-tabs id="tabs-header" part="tabs-header" exportparts=${this._exportparts} orientation="horizontal"
            .selected=${this.selected} ?disabled=${this.disabled} .nameItems=${this.items}
            .switchTabOnFocus=${this.switchTabOnFocus} style=${styleMap(stylestabNameHeight)}>
            ${map(this.items, (item, index) => html`
                <ptcs-tab part="tabs-tab" .labelContent=${this._text(item)} tab-number=${this._displayIndex(index)}
                    ?hidden=${this._hidden(item)} ?disabled=${this._or(item.disabled, this.disabled)}
                    style="height:${this._getTabNameHeight(this.tabNameHeight)}"
                    tabindex=${this._getCurrTabindex(index, this.selected, this._delegatedFocus)}
                    .selected=${this._isTabSelected(index, this.selected)}>
                    <ptcs-label part="tabs-tab-label" label=${this._text(item)}
                        ?disabled=${this._or(item.disabled, this.disabled)} max-width=${this.tabNameMaxWidth}></ptcs-label>
                </ptcs-tab>`)}
        </ptcs-tabs>
        <div part="divider" id="divider"></div>
        <ptcs-page-select id="pages" part="pages" .selected=${this.selected} ?disabled=${this.disabled}
            class=${classMap(classes)} style=${styleMap(stylestabHeight)}>
            <slot></slot>
        </ptcs-page-select>`;
    }

    static get is() {
        return 'ptcs-tab-set';
    }

    static get properties() {
        return {
            selected: {
                type:        Number,
                notify:      true,
                reflect:     true,
                observeWhen: 'immediate',
                observer:    '_selectedChanged'
            },

            defaultTabNumber: {
                type:      Number,
                attribute: 'default-tab-number'
            },

            items: {
                type:        Array,
                observeWhen: 'immediate',
                observer:    '_itemsChanged'
            },

            tabHeight: {
                type:      Number,
                reflect:   true,
                attribute: 'tab-height'
            },

            selectedTabValue: {
                type:        String,
                notify:      true,
                attribute:   'selected-tab-value',
                observeWhen: 'immediate',
                observer:    '_selectedTabValueChanged'
            },

            disabled: {
                type:    Boolean,
                reflect: true
            },

            selectedTabName: {
                type:        String,
                notify:      true,
                attribute:   'selected-tab-name',
                observeWhen: 'immediate',
                observer:    '_selectedTabNameChanged'
            },

            switchTabOnFocus: {
                type:      Boolean,
                attribute: 'switch-tab-on-focus'
            },

            tabNameMaxWidth: {
                type:      Number,
                attribute: 'tab-name-max-width',
                observer:  '_tabNameMaxWidthChanged'
            },

            _exportparts: {
                type:     String,
                readOnly: true,
                value:    PTCS.exportparts('tabs-header-', PTCS.Tabs)
            },

            tabNameHeight: {
                type:      Number,
                attribute: 'tab-name-height'
            },

            _delegatedFocus: {
                type: String
            }
        };
    }

    constructor() {
        super();
        this.tabNameHeight = 34;
        this.items = [];
        this.disabled = false;
        this._delegatedFocus = null;
    }

    ready() {
        super.ready();

        this.addEventListener('selected-changed', () => {
            // Verify if ptcs-tab-set contains nested ptcs-tab-set(s). Vaadin overflow computation may need refresh
            const nestedTabsets = this.querySelectorAll('ptcs-tab-set');
            nestedTabsets.forEach((tabset) => {
                const el = tabset.shadowRoot.querySelector('ptcs-tabs');
                if (el && el._updateOverflow && typeof el._updateOverflow === 'function') {
                    el._updateOverflow();
                }
            });
        });

        this.addEventListener('swap-tab', (ev) => {
            this.selected = ev.detail.selected;
            ev.stopPropagation();
        });
    }

    firstUpdated() {
        super.firstUpdated();
        if (this.selected === undefined) {
            this.selected = this.defaultTabNumber > 0 ? this.defaultTabNumber - 1 : 0;
        }
    }

    _getCurrTabindex(index, selected, delegatedFocus) {
        if (index === selected) {
            return 0;
        }
        return -1;
    }

    _displayIndex(index) {
        return index + 1;
    }

    _isTabSelected(index, selected) {
        return (selected === index);
    }

    _text(item) {
        if (item.name === null || item.name === '') {
            return '';
        }
        return item.name || String(item);
    }

    _hidden(item) {
        return item.visible === false;
    }

    _or(a, b) {
        return a || b;
    }

    _tabNameMaxWidthChanged() {
        // tabs.header needs to recompute the tab widths
        this.$['tabs-header'].refresh();
    }

    _selectedTabValueChanged(value) {
        this.selected = this.items.findIndex(item => item.value === value);
    }

    _selectedTabNameChanged(name) {
        this.selected = this.items.findIndex(item => (item.name || item) === name);
    }

    _selectedChanged(selected) {
        if (!this.items) {
            return;
        }
        const selectedItem = selected >= 0 && selected < this.items.length && this.items[selected];
        if (selectedItem) {
            this.selectedTabName = typeof selectedItem.name === 'string' ? selectedItem.name : String(selectedItem);
            this.selectedTabValue = selectedItem.value;
        }
    }

    _itemsChanged(items) {
        if (items && items.length) {
            if (this.selected > items.length - 1) {
                this.selected = items.length - 1;
            } else {
                // Make sure the new tab name and value are retrieved
                this._selectedChanged(this.selected);
            }
        }
    }

    _getTabNameHeight(tabNameHeight) {
        if (typeof tabNameHeight === 'number') {
            return tabNameHeight + 'px';
        }
        // We don't want 'undefinedpx'
        return 'unset';
    }
};

customElements.define(PTCS.TabSet.is, PTCS.TabSet);
