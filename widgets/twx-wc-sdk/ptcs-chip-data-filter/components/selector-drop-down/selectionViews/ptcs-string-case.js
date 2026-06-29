import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';

const dropDownDefaultValue = 'exact';
const dropDownSelectDefaultValue = {val: '', label: ''};
const operations = [
    {name: 'exact', translationKey: 'stringExact', label: 'is exactly'},
    {name: 'startWith', translationKey: 'stringStartsWith', label: 'starts with'},
    {name: 'endWith', translationKey: 'stringEndsWith', label: 'ends with'},
    {name: 'contains', translationKey: 'stringContains', label: 'contains'},
    {name: 'isNot', translationKey: 'stringNot', label: 'is not'},
    {name: 'notStartWith', translationKey: 'stringNotStartsWith', label: 'does not start with'},
    {name: 'notEndWith', translationKey: 'stringNotEndsWith', label: 'does not end with'},
    {name: 'notContains', translationKey: 'stringNotContains', label: 'does not contain'}
];
class PTCSStringCase extends PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))) {

    static get styles() {
        return css`
                :host {
                    display: flex;
                    align-items: flex-end;
                    flex-wrap: wrap;
                }

                :host([display="compact"]) {
                    flex-direction: column;
                    width: 100%;
                }

                #string-text-container[hidden], #string-select-container[hidden] {
                    display: none;
                }

                #string-text-container, #string-select-container {
                    display: flex;
                }

                #drop-down, #text-field {
                    width: var(--ptcs-chip-data-filter-selector-dropdown-number-case-subcomponent-width);
                    margin-right: var(--subcomponent-margin-spacing);
                }

                :host([display="compact"]) #drop-down {
                    width: 100%;
                    margin-right: 0px;
                }

                :host([display="compact"]) #text-field {
                    width: 100%;
                    margin-top: var(--ptcs-chip-data-filter-selector-subcomponent-compact-top-margin);
                    margin-right: 0px;
                }

                #string-select-field {
                    width:        var(--ptcs-chip-data-filter-selector-dropdown-base-subcomponent-width);
                    margin-right: var(--subcomponent-margin-spacing);
                }

                :host([display="compact"]) #string-select-field {
                    width: 100%;
                    margin-right: 0px;
                }

                :host([display="compact"]) #string-text-container,
                :host([display="compact"]) #string-select-container {
                    flex-direction: column;
                    width: 100%;
                }`;
    }

    render() {
        return html`<div id="string-text-container" ?hidden=${this._isSelectMode}>
       <ptcs-dropdown id="drop-down" part="drop-down"
           .label=${this.conditionLabel || this.dictionary.stringCondition}
           .selector=${'label'} .valueSelector=${'name'}
           @selected-value-changed=${this.__currentSelectionDropDownChanged}
           @selected-indexes-changed=${this.__setIsFilled} tabindex=${this._delegatedFocus}
         ></ptcs-dropdown>
         <ptcs-textfield
           id="text-field" part="text-field"
          .label=${this.valueLabel || this.dictionary.stringValue}
          @text-changed=${this.__setIsFilled}
          @keyup=${this.__handleKeyUp}
          tabindex=${this._delegatedFocus}
         ></ptcs-textfield>
       </div>
       <div id="string-select-container" ?hidden=${!this._isSelectMode}>
          <ptcs-dropdown
            id="string-select-field" part="string-select-field"
            .label=${this.valueLabel || this.dictionary.stringValue}
            .selector=${'label'} .valueSelector=${'val'}
            .filterHintText=${this.filterHintText || this.dictionary.stringFilterHint}
            filter
            treat-value-as-string
            @selected-items-changed=${this.__handleSelected}
            tabindex=${this._delegatedFocus}>
         </ptcs-dropdown>
       </div>`;
    }

    static get is() {
        return 'ptcs-string-case';
    }

    static get properties() {
        return {
            dictionary: {
                type:     Object,
                observer: '__updateTranslations'
            },

            conditionLabel: {
                type: String
            },

            valueLabel: {
                type: String
            },

            filterHintText: {
                type: String
            },

            isFilled: { // carries information whether the view is filled with enough amount of data
                type:     Boolean,
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
            },

            _isSelectMode: {
                type: Boolean
            },

            __queryFieldName: {
                type: String
            }
        };
    }

    constructor() {
        super();
        this._isSelectMode = false;
    }

    ready() {
        super.ready();
        this.setDefaultValues();
        this.__updateTranslations();
    }

    firstUpdated(changedProperties) {
        super.firstUpdated();

        if (changedProperties.has('dictionary')) {
            this.__updateTranslations();
        }
    }

    set dataEnteredByUser(newData) {
        if (newData) {
            this.__currentSelectionDropDown = newData.operation;
            this.__setValue(newData.value);
            this.__setIsFilled();
        } else {
            this.setDefaultValues();
        }
    }

    get dataEnteredByUser() {
        return this.__getCurrentData();
    }

    __currentSelectionDropDownChanged(ev) {
        if (ev.detail.value) {
            this.__currentSelectionDropDown = ev.detail.value;
        }
    }

    /*
        An example of query:
        {
             "type": "LIKE",
             "fieldName": "textBasedCategory",
             "value": "Pol"
        }
    */
    get query() {
        if (this.isError() || !this.__queryFieldName) {
            return null;
        }

        const data = this.__getCurrentData();
        let operator = 'LIKE';
        let value = null;

        switch (data.operation) {
            case 'contains':
                if (data.value.indexOf('*') !== -1) {
                    value =  data.value;
                } else {
                    value = '*' + data.value + '*';
                }
                break;
            case 'startWith':
                value = data.value + '*';
                break;
            case 'endWith':
                value = '*' + data.value;
                break;
            case 'exact':
                value = data.value;
                operator = 'EQ';
                break;
            case 'notStartWith':
                operator = 'NOTLIKE';
                value = data.value + '*';
                break;
            case 'notEndWith':
                operator = 'NOTLIKE';
                value = '*' + data.value;
                break;
            case 'notContains':
                operator = 'NOTLIKE';
                value = '*' + data.value + '*';
                break;
            case 'isNot':
                operator = 'NOTLIKE';
                value = data.value;
                break;
            default:
                console.warn('Ignoring filter condition. Unknown operation type: "' + data.operation + '" in the filter: ', data);
                this.setDefaultValues();
                return null;
        }

        return {
            fieldName: this.__queryFieldName,
            type:      operator,
            value:     value
        };
    }

    queryFieldName(newFieldName) {
        this.__queryFieldName = newFieldName;
    }

    setDefaultValues() {
        this.__currentSelectionDropDown = dropDownDefaultValue;
        this.$['drop-down'].selectedValue = dropDownDefaultValue;
        this.__currentSelectionStringDropDown = dropDownSelectDefaultValue;
        this.__setTextDefault();
    }

    setAspects(aspects) {
        this.__aspects = aspects;
        this.__processSelectOptions();
        this.__setTextDefault();
    }

    __processSelectOptions() {
        this._isSelectMode = false;
        this.__currentSelectionStringDropDownIndex = [];

        const aspects = this.__aspects || {};
        if (!aspects._selectOptions || aspects._selectOptions.length === 0) {
            return;
        }

        this._isSelectMode = true;
        const selectField = this.shadowRoot.getElementById('string-select-field');
        selectField.items = aspects._selectOptions;
        if (aspects.defaultValue) {
            const idx = aspects._selectOptions.findIndex(item => {
                return item.val === aspects.defaultValue;
            });
            this.__currentSelectionStringDropDownIndex = idx === -1 ? [] : [idx];
            selectField.selectedIndexes = this.__currentSelectionStringDropDownIndex;
        }
    }

    __setTextDefault() {
        const aspects = this.__aspects || {};
        this.$['text-field'].text = aspects.defaultValue || '';
    }

    isError() {
        return !this.isFilled;
    }

    getFormatted() {
        const data = this.__getCurrentData();
        const display = this._isSelectMode ? this.__currentSelectionStringDropDown.label : data.value;

        return operations.find(o => o.name === data.operation).label + ': ' + display;
    }

    __setIsFilled() {
        this._setIsFilled(Boolean(this.__getValue()));
    }

    __getCurrentData() {
        return {operation: this.__currentSelectionDropDown, value: this.__getValue()};
    }

    __getValue() {
        return this._isSelectMode ? this.__currentSelectionStringDropDown.val : this.$['text-field'].text;
    }

    __setValue(val) {
        if (this._isSelectMode) {
            this.__currentSelectionStringDropDown = this.__aspects._selectOptions.find(item => {
                return item.val === val;
            }) || dropDownSelectDefaultValue;
        } else {
            this.$['text-field'].text = val;
            this.__currentSelectionStringDropDown.val = val;
        }
    }

    __updateTranslations() {
        if (this.dictionary) {
            for (const o of operations) {
                if (this.dictionary[o.translationKey]) {
                    o.label = this.dictionary[o.translationKey];
                }
            }
        }
        this.$['drop-down'].items = operations;

        this.__currentSelectionDropDown = dropDownDefaultValue;
    }

    __handleKeyUp(ev) {
        if (ev.key === 'Enter' && !this.isError()) {
            this.$['text-field'].blur();
            this.dispatchEvent(new CustomEvent('data-approved', {
                bubbles:  true,
                composed: true
            }));
        }
    }

    __handleSelected(ev) {
        this.__currentSelectionStringDropDown = ev.detail.value.length ? ev.detail.value[0] : '';
        if (ev.detail.value.length) {
            const selectField = this.shadowRoot.getElementById('string-select-field');
            const idx = selectField.items.findIndex(item => {
                return item.val === ev.detail.value[0].val;
            });
            this.selectedIndexes = [idx];
            this.__setIsFilled();
        }
    }
}

customElements.define(PTCSStringCase.is, PTCSStringCase);
