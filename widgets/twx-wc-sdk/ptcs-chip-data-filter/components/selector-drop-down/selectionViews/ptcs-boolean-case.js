import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-dropdown/ptcs-dropdown.js';

const booleanDefaultValue = 'true';
const booleanTypes = [
    {name: 'true', translationKey: 'stringTrue', label: 'True'},
    {name: 'false', translationKey: 'stringFalse', label: 'False'}
];

class PTCSBooleanCase extends PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))) {

    static get styles() {
        return css`
                :host([display="compact"]) {
                    flex-direction: column;
                    width: 100%;
                }

                :host([display="compact"]) #drop-down {
                    width: 100%;
                }

                #drop-down {
                    width:  var(--ptcs-chip-data-filter-selector-dropdown-number-case-subcomponent-width);
                    margin-right: var(--subcomponent-margin-spacing);
                }`;
    }

    render() {
        return html`<ptcs-dropdown id="drop-down" part="drop-down"
            .label=${this.conditionLabel || this.dictionary.stringCondition}
            .selectedValue=${this.__currentBooleanValue} .selector=${'label'} .valueSelector=${'name'}
            @selected-indexes-changed=${this.__setIsFilled} tabindex=${this._delegatedFocus}></ptcs-dropdown>`;
    }

    static get is() {
        return 'ptcs-boolean-case';
    }

    static get properties() {
        return {
            dictionary: {
                type:     Object,
                observer: '__updateTranslations'
            },

            dataEnteredByUser: {
                type:       Object,
                noAccessor: true
            },

            // This prop needs to be declared, otherwise changes to it will not trigger a render()...
            __currentBooleanValue: {
                type: Object
            },

            conditionLabel: {
                type: String
            },

            isFilled: { // carries information whether the view is filled with enough amount of data
                readOnly: true,
                notify:   true,
                value:    false
            },

            display: {
                type:    String,
                value:   'compact',
                reflect: true
            },

            _delegatedFocus: {
                type:  String,
                value: null
            }
        };
    }


    ready() {
        super.ready();
        this.__updateTranslations();
        this.setDefaultValues();
    }

    firstUpdated(changedProperties) {
        super.firstUpdated();
        if (changedProperties.has('dictionary')) {
            this.__updateTranslations();
        }
    }

    set dataEnteredByUser(newData) {
        this.__currentBooleanValue = newData;
    }

    get dataEnteredByUser() {
        return this.$['drop-down'].selectedValue;
    }

    /*
        Example:

        'type': 'EQ'
        'fieldName': 'user'
        'value': 'true'
    */

    get query() {
        if (this.isError() || !this.__queryFieldName) {
            return null;
        }

        return {
            fieldName: this.__queryFieldName,
            type:      'EQ',
            value:     this.__currentBooleanValue
        };
    }

    queryFieldName(newFieldName) {
        if (arguments.length === 0) {
            return this.__queryFieldName;
        }
        this.__queryFieldName = newFieldName;
        return this.__queryFieldName;
    }

    setDefaultValues() {
        this.__currentBooleanValue = booleanDefaultValue;
    }

    setAspects(aspects) {
        this.__aspects = aspects;
    }

    isError() {
        return false;
    }

    getFormatted() {
        if (typeof this.__currentBooleanValue === 'boolean') {
            this.__currentBooleanValue = this.__currentBooleanValue.toString();
        }
        return booleanTypes.find(o => o.name === this.__currentBooleanValue).label;
    }

    __setIsFilled(ev) {
        this.__currentBooleanValue = this.$['drop-down'].selectedValue;
        this._setIsFilled(this.$['drop-down'].selectedValue);
    }

    __updateTranslations() {
        if (this.dictionary) {
            for (const o of booleanTypes) {
                if (this.dictionary[o.translationKey]) {
                    o.label = this.dictionary[o.translationKey];
                }
            }
        }
        this.$['drop-down'].items = booleanTypes;

        this.__currentBooleanValue = '';
        this.__currentBooleanValue = booleanDefaultValue;
    }
}

customElements.define(PTCSBooleanCase.is, PTCSBooleanCase);
