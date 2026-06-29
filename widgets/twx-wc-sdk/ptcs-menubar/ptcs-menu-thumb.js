import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';
import 'ptcs-icon/ptcs-icon.js';
import 'ptcs-icons/cds-icons.js';

PTCS.MenuThumb = class extends PTCS.BehaviorTooltip(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement)))) {
    static get styles() {
        return css`
        :host {
            outline: none;
        }

        [part~=icon] {
            width: 100%;
            height: 100%;
        }`;
    }

    render() {
        return html`<ptcs-icon part="icon" id="icon" icon="cds:icon_drag"></ptcs-icon>`;
    }

    static get is() {
        return 'ptcs-menu-thumb';
    }

    static get properties() {
        return {
        };
    }
};

customElements.define(PTCS.MenuThumb.is, PTCS.MenuThumb);
