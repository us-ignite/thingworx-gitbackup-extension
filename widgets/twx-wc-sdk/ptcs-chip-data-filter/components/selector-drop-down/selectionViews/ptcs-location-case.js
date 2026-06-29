import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';

const proximityTypeDefaultValue = 'NEAR';
const proximityUnitDefaultValue = 'M';
const proximityTypes = [
    {name: 'NEAR', translationKey: 'stringWithin', label: 'Within'},
    {name: 'NOTNEAR', translationKey: 'stringNotWithin', label: 'Not Within'}
];
const proximityUnits = [
    {name: 'M', translationKey: 'stringMiles', label: 'miles'},
    {name: 'K', translationKey: 'stringKilometers', label: 'kilometers'},
    {name: 'N', translationKey: 'stringNauticalMiles', label: 'nautical miles'}
];

class PTCSLocationCase extends PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))) {


    static get styles() {
        return css`
                :host {
                    display: flex;
                    align-items: flex-end;
                    flex-wrap: wrap;
                }

                :host([hidden]) {
                    display: none;
                }

                :host([display="compact"]) {
                    flex-direction: column;
                    align-items: flex-start;
                    width: 100%;
                }

                span {
                    line-height: var(--ptcs-chip-data-filter-selector-dropdown-subcomponent-height)
                }

                #proximity-type, #proximity-unit, ptcs-textfield {
                    width: var(--ptcs-chip-data-filter-selector-dropdown-number-case-subcomponent-width);
                }

                #proximity-type, #proximity-unit, #proximity-value, #latitude-text-field, #longitude-text-field {
                    margin-right: var(--subcomponent-margin-spacing);
                }

                :host([display="compact"]) #proximity-type {
                    width: 100%;
                    margin-right: 0px;
                }

                :host([display="compact"]) #proximity-unit {
                    width: 100%;
                    margin-top: var(--ptcs-chip-data-filter-selector-subcomponent-compact-top-margin);
                    margin-right: 0px;
                }

                :host([display="compact"]) #proximity-value {
                    width: 100%;
                    margin-top: var(--ptcs-chip-data-filter-selector-subcomponent-compact-top-margin);
                    margin-right: 0px;
                }

                :host([display="compact"]) #latitude-text-field {
                    width: 100%;
                    margin-top: var(--ptcs-chip-data-filter-selector-subcomponent-compact-top-margin);
                    margin-right: 0px;
                }

                :host([display="compact"]) #longitude-text-field {
                    width: 100%;
                    margin-top: var(--ptcs-chip-data-filter-selector-subcomponent-compact-top-margin);
                    margin-right: 0px;
                }

                span {
                    margin: var(--subcomponent-margin-spacing) var(--subcomponent-margin-spacing) 0px 0px;
                }

                #between-outside-container {
                    display: none;
                }

                #between-outside-container[data-enabled] {
                    display: flex;
                }`;
    }

    render() {
        return html`<ptcs-dropdown id="proximity-type" part="drop-down"
                  .label=${this.conditionLabel || this.dictionary.stringCondition}
                  .selector=${'label'} .valueSelector=${'name'}
                  @selected-value-changed=${this._selectedValueProximityTypeChanged}
                  @selected-indexes-changed=${this.__setIsFilled} tabindex=${this._delegatedFocus}
                ></ptcs-dropdown>
                <ptcs-textfield id="proximity-value" part="text-field"
                  .label=${this.valueLabel || this.dictionary.stringValue}
                  @text-changed=${this.__setIsFilled} @keyup=${this.__handleKeyUp} tabindex=${this._delegatedFocus}
                ></ptcs-textfield>
                <ptcs-dropdown id="proximity-unit" part="drop-down" .label=${this.unitsLabel || this.dictionary.stringUnits}
                  .selector=${'label'} .valueSelector=${'name'}
                  @selected-value-changed=${this._selectedValueProximityUnitChanged}
                  @selected-indexes-changed=${this.__setIsFilled} tabindex=${this._delegatedFocus}
                ></ptcs-dropdown>
                <ptcs-textfield id="latitude-text-field" part="text-field"
                  .label=${this.latitudeLabel || this.dictionary.stringLatitude}
                  @text-changed=${this.__setIsFilled} @keyup=${this.__handleKeyUp} tabindex=${this._delegatedFocus}
                ></ptcs-textfield>
                <ptcs-textfield id="longitude-text-field" part="text-field"
                  .label=${this.longitudeLabel || this.dictionary.stringLongitude}
                  @text-changed=${this.__setIsFilled} @keyup=${this.__handleKeyUp} tabindex=${this._delegatedFocus}
                ></ptcs-textfield>`;
    }

    static get is() {
        return 'ptcs-location-case';
    }

    static get properties() {
        return {
            dictionary: {
                type:     Object,
                observer: '__updateTranslations'
            },

            unitsLabel: {
                type: String
            },

            conditionLabel: {
                type: String
            },

            valueLabel: {
                type: String
            },

            latitudeLabel: {
                type: String
            },

            longitudeLabel: {
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
        this.setDefaultValues();
        this.__updateTranslations();
    }

    firstUpdated(changedProperties) {
        super.firstUpdated();
        this.setDefaultValues();
    }

    set dataEnteredByUser(newData) {
        if (newData) {
            this.__currentSelectionProximityType = newData.type;
            this.$['proximity-value'].text = newData.value;
            this.__currentSelectionProximityUnit = newData.units;
            this.$['latitude-text-field'].text = newData.latitude;
            this.$['longitude-text-field'].text = newData.longitude;
        }
    }

    _selectedValueProximityTypeChanged(ev) {
        if (ev.detail.value) {
            this.__currentSelectionProximityType = ev.detail.value;
        }
    }

    _selectedValueProximityUnitChanged(ev) {
        if (ev.detail.value) {
            this.__currentSelectionProximityUnit = ev.detail.value;
        }
    }
    get dataEnteredByUser() {
        return {
            type:      this.__currentSelectionProximityType,
            value:     this.$['proximity-value'].text,
            units:     this.__currentSelectionProximityUnit,
            latitude:  this.$['latitude-text-field'].text,
            longitude: this.$['longitude-text-field'].text
        };
    }

    get query() {
        if (this.isError() || !this.__queryFieldName) {
            return null;
        }

        const query = {
            fieldName: this.__queryFieldName,
            type:      this.__currentSelectionProximityType,
            location:  {
                latitude:  this.$['latitude-text-field'].text,
                longitude: this.$['longitude-text-field'].text,
                elevation: 0,
                units:     'WGS84'
            },
            distance: this.$['proximity-value'].text,
            units:    this.__currentSelectionProximityUnit,
        };

        return query;
    }

    queryFieldName(newFieldName) {
        if (arguments.length !== 0) {
            this.__queryFieldName = newFieldName;
        }
        return this.__queryFieldName;
    }

    isError() {
        return this.$['proximity-value'].text.length === 0 ||
            this.$['latitude-text-field'].text.length === 0 ||
            this.$['longitude-text-field'].text.length === 0;
    }

    setDefaultValues() {
        this.$['proximity-value'].text = this.$['latitude-text-field'].text = this.$['longitude-text-field'].text = '';
        this.__currentSelectionProximityType = proximityTypeDefaultValue;
        this.$['proximity-type'].selectedValue = proximityTypeDefaultValue;
        this.__currentSelectionProximityUnit = proximityUnitDefaultValue;
        this.$['proximity-unit'].selectedValue = proximityUnitDefaultValue;
    }

    setAspects(aspects) {
        this.__aspects = aspects;
    }

    getFormatted() {
        const formatted = proximityTypes.find(o => o.name === this.__currentSelectionProximityType).label +
             ` ${this.$['proximity-value'].text.toLowerCase()} ` +
            proximityUnits.find(o => o.name === this.__currentSelectionProximityUnit).label +
            ` of ${this.$['latitude-text-field'].text}, ${this.$['longitude-text-field'].text}`;

        return formatted;
    }

    __setIsFilled() {
        this._setIsFilled(this.$['proximity-value'].text && this.$['latitude-text-field'].text && this.$['longitude-text-field'].text);
    }

    __updateTranslations() {
        if (this.dictionary) {
            for (const o of proximityTypes) {
                if (this.dictionary[o.translationKey]) {
                    o.label = this.dictionary[o.translationKey];
                }
            }
            for (const o of proximityUnits) {
                if (this.dictionary[o.translationKey]) {
                    o.label = this.dictionary[o.translationKey];
                }
            }
        }
        this.$['proximity-type'].items = proximityTypes;
        this.$['proximity-unit'].items = proximityUnits;

        this.__currentSelectionProximityUnit = '';
        this.__currentSelectionProximityUnit = proximityUnitDefaultValue;
        this.__currentSelectionProximityType = '';
        this.__currentSelectionProximityType = proximityTypeDefaultValue;
    }

    __handleKeyUp(event) {
        if (event.key === 'Enter' && !this.isError()) {
            this.blur();
            this.dispatchEvent(new CustomEvent('data-approved', {
                bubbles:  true,
                composed: true
            }));
        }
    }
}

customElements.define(PTCSLocationCase.is, PTCSLocationCase);
