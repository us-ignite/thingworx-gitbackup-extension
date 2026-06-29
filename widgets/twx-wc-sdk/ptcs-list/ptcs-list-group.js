import {LitElement, html} from 'lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-label/ptcs-label.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';

PTCS.ListGroup = class extends PTCS.BehaviorStyleable(LitElement) {
    render() {
        return html`<ptcs-label part="group-label" variant="list-item" .label=${this.label} .disabled=${this.disabled}
            .multiLine=${this.multiLine} .horizontalAlignment=${this.alignment} vertical-alignment=center
            .tooltip=${this.ownerTooltip} .tooltipIcon=${this.ownerTooltipIcon}></ptcs-label>`;
    }

    static get properties() {
        return {
            label: {
                type:      String,
                attribute: 'aria-label'
            },

            disabled: {
                type:    Boolean,
                reflect: true
            },

            alignment: { // 'left', 'center', 'right'
                type: String,
            },

            multiLine: {
                type:      Boolean,
                attribute: 'multi-line'
            },

            // Tooltip data provided by list owner (like a dropdown) to be shown during list item truncation tooltip
            ownerTooltip: {
                type:      String,
                attribute: 'owner-tooltip'
            },

            ownerTooltipIcon: {
                type:      String,
                attribute: 'owner-tooltip-icon'
            }
        };
    }

    createRenderRoot() {
        return this;
    }

    // Get auto width of item
    get autoWidth() {
        return this.querySelector('ptcs-label')?.offsetWidth || 0;
    }
};

customElements.define('ptcs-list-group', PTCS.ListGroup);
