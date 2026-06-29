import {LitElement, html, css} from 'lit';
import {map} from 'lit/directives/map.js';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';

PTCS.ValidationMessage = class extends PTCS.BehaviorTooltip(PTCS.BehaviorStyleable(LitElement)) {
    static get is() {
        return 'ptcs-validation-message';
    }

    static get styles() {
        return css`
        :host { display: grid; grid-template-columns: auto 1fr; user-select: text; cursor: text; }
        :host([_hidden]) { display: none; }
        :host([_no-label]) [part=messages-list] { grid-row: 1; }
        :host([_no-label]) [part=icon] { place-self: start; }
        :host(:focus) { outline: none; }
        [part=icon] { grid-column: 1; grid-row: 1; place-self: center; }
        [part=label] { grid-column: 2; grid-row: 1; align-self: center; }
        [part=messages-list] { grid-column: 2; grid-row: 2; overflow: hidden; list-style-type: none; }
        [part=messages-list]:not([single-message]) [part~=message-li]::before { content: "\u2022"; padding-right: 0.5em; }
        [part~=message-li] { display: flex; }
        [part=message-text]::part(label) { overflow: hidden; }
        [hidden] { display: none !important; }`;
    }

    render() {
        return html`
        <ptcs-icon part="icon" .icon=${this.icon} ?hidden=${!this.icon}></ptcs-icon>
        <ptcs-label id="label" part="label" variant="label" disable-tooltip
                    .label=${this.label} .multiLine=${!this.singleLine} ?hidden=${this['_no-label']}
        ></ptcs-label>
        <ul id="messages-list" part="messages-list"
            ?single-message=${this._singleMessage(this.messages)} ?hidden=${this._hideMessagesList(this.messages)}>
            ${map(this.messages, item => html`<li part="message-li message-text">
                <ptcs-label .label=${item} part="message-text" variant="caption" .multiLine=${!this.singleLine} disable-tooltip></ptcs-label>
            </li>`)}
        </ul>`;
    }

    static get properties() {
        return {
            validity: {
                type:    String,
                reflect: true
            },

            icon: {
                type: String
            },

            label: {
                type: String
            },

            messages: {
                type: Array
            },

            singleLine: {
                type: Boolean,
            },

            _noLabel: {
                type:      Boolean,
                attribute: '_no-label',
                reflect:   true
            },

            _hidden: {
                type:    Boolean,
                reflect: true
            }
        };
    }

    constructor() {
        super();
        this.tooltipFunc = this._monitorTooltip.bind(this);
    }

    firstUpdated() {
        super.firstUpdated();
        this._noLabel = this._computeNoLabel(this.label);
        this._hidden = this._computeHidden(this.label, this.messages);
    }

    willUpdate(changedProperties) {
        super.willUpdate(changedProperties);

        if (changedProperties.has('label')) {
            this._noLabel = this._computeNoLabel(this.label);
            this._hidden = this._computeHidden(this.label, this.messages);
        } else if (changedProperties.has('messages')) {
            this._hidden = this._computeHidden(this.label, this.messages);
        } else if (changedProperties.has('validity')) {
            this._hidden = this._computeHidden(this.label, this.messages);
        }
    }

    get hidden() {
        return this._hidden || false;
    }

    _el(id) {
        return this.shadowRoot.getElementById(id);
    }

    // Tooltip behavior on label and messages truncation
    _monitorTooltip() {
        const label = this._el('label');
        if (!label) {
            return ''; // No ready
        }
        const messages = this._el('messages-list').querySelectorAll('ptcs-label[part=message-text]');

        if (!label.isTruncated() && !Array.from(messages).some(msg => msg.isTruncated())) {
            return '';
        }

        const labelTooltip = label ? label.label : '';
        let messagesTooltip = '';

        messages.forEach((msg, i) => {
            messagesTooltip += messages.length > 1 ? '\u2022 ' : '';
            messagesTooltip += i < messages.length - 1 ? `${msg.label}\n\n` : msg.label;
        });

        return labelTooltip.concat(labelTooltip && messagesTooltip ? '\n\n' : '', messagesTooltip);
    }

    _hideMessagesList(messages) {
        return !messages || messages.length === 0;
    }

    _singleMessage(messages) {
        return messages.length === 1;
    }

    _computeNoLabel(label) {
        return !label;
    }

    _computeHidden(label, messages) {
        return !label && this._hideMessagesList(messages);
    }
};

customElements.define(PTCS.ValidationMessage.is, PTCS.ValidationMessage);
