import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {map} from 'lit/directives/map.js';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';
import './ptcs-chip-data-filter-chip-child.js';
class PTCSchipDataFilterChipContainer extends PTCS.BehaviorTooltip(PTCS.BehaviorStyleable(L2Pw(LitElement))) {

    static get styles() {
        return css`
                [part="container"] {
                    display: flex;
                    flex-wrap: wrap;
                    --ptcs-tooltip-start-delay: 0;
                }`;
    }

    render() {
        return html`<div id="container" part="container">
                ${map(this.data, (item) => html`
                    <ptcs-chip-data-filter-chip-child part="chip-child" .content=${item.content} exportparts="oval-container, content"
                    @focus=${this._focusEv} @blur=${this._blurEv}
                    .dataId=${item.id} .fieldName=${item.fieldName} tabindex=${this.subTabindex}></ptcs-chip-data-filter-chip-child>
                `)}
            </div>`;
    }

    static get is() {
        return 'ptcs-chip-data-filter-chip-container';
    }

    static get properties() {
        return {
            data: {
                type: Array
            },

            subTabindex: {
                type:      String,
                attribute: 'sub-tab-index'
            }
        };
    }

    get focusedChip() {
        return this.shadowRoot.activeElement;
    }

    _focusEv(ev) {
        this._tooltipEnter(ev.target, undefined, undefined, undefined, {showAnyway: true});
    }

    _blurEv(ev) {
        this._tooltipLeave(ev.target);
    }

    get focusableElements() {
        return [...this.shadowRoot.getElementById('container').querySelectorAll('[tabindex]')].filter(el => el.clientHeight > 0)
            .map(chip => chip.shadowRoot.querySelector('[part=cross-button]'));
    }
}

customElements.define(PTCSchipDataFilterChipContainer.is, PTCSchipDataFilterChipContainer);
