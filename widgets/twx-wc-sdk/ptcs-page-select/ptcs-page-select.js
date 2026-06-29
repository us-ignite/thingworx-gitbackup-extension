import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';

PTCS.PageSelect = class extends L2Pw(LitElement) {
    static get styles() {
        return css`
        :host {
        flex: 1 1 auto;
        position: relative;
        }

        #root {
        flex: 1 1 auto;
        display: flex;
        justify-content: space-between;
        align-items: stretch;
        }

        div ::slotted(.ps--hide) {
        display: none !important;
        }`;
    }

    render() {
        return html`<div id="root">
        <slot id="slot" @slotchange=${this._slotchange}></slot>
      </div>`;
    }

    static get is() {
        return 'ptcs-page-select';
    }

    static get properties() {
        return {
            disabled: {
                type:  Boolean,
                value: false
            },

            selected: {
                observer: '_selectedChanged',
                reflect:  true,
                notify:   true
            },

            attrForSelected: {
                type:      String,
                attribute: 'attr-for-selected',
                value:     null
            },

            fallbackSelection: {
                type:      String,
                attribute: 'fallback-selection',
                value:     null
            }
        };
    }

    _getSlottedElements() {
        return this.$.slot.assignedElements({flatten: true});
    }

    _initSelection() {
        const nodes = this._getSlottedElements();
        const sel = this._find_selected(nodes, this.selected);
        nodes.forEach(node => this._select_node(node, node === sel));
    }

    ready() {
        super.ready();
        this._initSelection();
    }

    _slotchange() {
        this._initSelection();
    }

    select(selected) {
        this.selected = selected;
    }

    // Not smart and not fast...
    indexOf(el) {
        const nodes = this._getSlottedElements();
        let ix = 0;

        for (let i = 0; i < nodes.length; ++i) {
            const node = nodes[i];

            if (node.nodeName && node.nodeType === 1) {
                if (node.contains(el)) {
                    return ix;
                }
                ++ix;
            }
        }
        return -1;
    }

    _selectedChanged(selected, old) {
        const nodes = this._getSlottedElements();
        const sel = selected !== undefined && this._find_selected(nodes, selected);
        const unsel = old !== undefined && this._find_selected(nodes, old);

        if (sel === unsel) {
            return;
        }
        if (unsel) {
            this._select_node(unsel, false);
        }
        if (sel) {
            this._select_node(sel, true);
        }
    }

    // Compute the selected node
    _find_selected(nodes, selected) {
        if (this.attrForSelected) {
            const sel = nodes.find(node => selected === node.getAttribute(this.attrForSelected));
            return sel || (this.fallbackSelection && nodes.find(node => this.fallbackSelection === node.getAttribute(this.attrForSelected)));
        }
        return nodes[Number(selected)];
    }

    // Select or unselect node
    _select_node(node, select) {
        if ('opened' in node) {
            node.opened = select;
        }

        if ('hidden' in node) {
            node.hidden = !select;
        }

        if (select) {
            node.classList.remove('ps--hide');
        } else {
            node.classList.add('ps--hide');
        }
    }
};

customElements.define(PTCS.PageSelect.is, PTCS.PageSelect);
