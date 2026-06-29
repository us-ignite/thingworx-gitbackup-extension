import {LitElement, html, css} from 'lit';
import {PTCS} from 'ptcs-library/library.js';

import 'ptcs-label/ptcs-label.js';
import 'ptcs-textfield/ptcs-textfield.js';
import 'ptcs-textarea/ptcs-textarea.js';
import 'ptcs-dropdown/ptcs-dropdown.js';
import 'ptcs-list/ptcs-list.js';
import 'ptcs-radio/ptcs-radio.js';
import 'ptcs-toggle-button/ptcs-toggle-button.js';
import 'ptcs-datepicker/ptcs-datepicker.js';
import 'ptcs-button/ptcs-button.js';
import 'ptcs-modal-overlay/ptcs-modal-overlay.js';

import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';

// Symbol that stores field name on input UI controls
const columnRef = Symbol('columnRef'); // Attach column definition to input element

const textarea = 'textarea';

const dfltEditControl = {
    BOOLEAN:  'checkbox',
    DATETIME: 'datepicker',
    // HYPERLINK:
    HTML:     textarea,
    // IMAGE:
    // IMAGELINK:
    // INTEGER:      textfield,
    // LONG:         textfield,
    // NUMBER:       textfield,
    // STRING:       textfield,
    TEXT:     textarea,
    // BASETYPENAME: textfield,
    // LOCATION:
    JSON:     textarea,
    XML:      textarea
};

const valueProp = {
    'PTCS-TEXTFIELD':     'text',
    'PTCS-TEXTAREA':      'text',
    'PTCS-DATEPICKER':    'dateTime',
    'PTCS-CHECKBOX':      'checked',
    'PTCS-TOGGLE-BUTTON': 'checked',
    'PTCS-DROPDOWN':      'selectedValue',
    'PTCS-LIST':          'selectedValue'
};

// Components that always needs a label
const needLabel = ['PTCS-CHECKBOX', 'PTCS-TOGGLE-BUTTON'];


const toBoolean = v => typeof v === 'string' ? v && v.trim() !== 'false' : !!v;
// eslint-disable-next-line no-nested-ternary
const toString = v => typeof v === 'string' ? v : (v !== undefined && v !== null) ? `${v}` : null;

const toDate = v => {
    if (v instanceof Date) {
        return v;
    }
    const d = new Date(v); // Try to convert value to a Date
    return isNaN(d) ? undefined : d;
};

const toJSON = v => {
    try {
        return JSON.parse(v);
    } catch (e) {
        console.log('Error parsing JSON data');
    }

    // Failure
    return null;
};

const splitValues = (v, n, s) => {
    const values = typeof v === 'string' ? v.split(s || ':') : [];
    return values.length === n ? values : null;
};

const isNumericType = type => ['INTEGER', 'NUMBER', 'LONG'].includes(type);

const checkNumValidity = v => {
    if (Array.isArray(v)) {
        return v.reduce((p, n) => p && checkNumValidity(n), true);
    }
    return !isNaN(Number(v));
};

const toThingCode = v => {
    const values = splitValues(v, 2);
    return checkNumValidity(values) ? {domainId: Number(values[0]), instanceId: Number(values[1])} : null;
};

const toLocation = v => {
    const values = splitValues(v, 3);
    return checkNumValidity(values) ? {latitude: Number(values[0]), longitude: Number(values[1]), elevation: Number(values[2])} : null;
};

const toVEC2 = v => {
    const values = splitValues(v, 2, ',');
    return checkNumValidity(values) ? {x: Number(values[0]), y: Number(values[1])} : null;
};

const toVEC3 = v => {
    const values = splitValues(v, 3, ',');
    return checkNumValidity(values) ? {x: Number(values[0]), y: Number(values[1]), z: Number(values[2])} : null;
};

const toVEC4 = v => {
    const values = splitValues(v, 4, ',');
    return checkNumValidity(values) ? {x: Number(values[0]), y: Number(values[1]), z: Number(values[2]), w: Number(values[3])} : null;
};

const isEmptyString = v => typeof v === 'string' && v.trim().length === 0;

const toInt = v => {
    if (isEmptyString(v)) {
        return null;
    }
    const value = Math.round(v);
    return checkNumValidity(value) ? value : null;
};

const toNumber = v => {
    if (isEmptyString(v)) {
        return null;
    }
    const value = Number(v);
    return checkNumValidity(value) ? value : null;
};

const _obj = v => typeof v === 'object';
const fromJSON = v => JSON.stringify(v);
const fromThingCode = v => _obj(v) ? `${v.domainId} : ${v.instanceId}` : '';
const fromLocation = v => _obj(v) ? `${v.latitude} : ${v.longitude} : ${v.elevation}` : '';
const fromVEC2 = v => _obj(v) ? `${v.x} , ${v.y}` : '';
const fromVEC3 = v => _obj(v) ? `${v.x} , ${v.y} , ${v.z}` : '';
const fromVEC4 = v => _obj(v) ? `${v.x} , ${v.y} , ${v.z} , ${v.w}` : '';

const convertValues = {
    BOOLEAN:      toBoolean,
    DATETIME:     toDate,
    // HYPERLINK:
    VEC2:         toVEC2,
    VEC3:         toVEC3,
    VEC4:         toVEC4,
    THINGCODE:    toThingCode,
    HTML:         toString,
    // IMAGE:
    // IMAGELINK:
    INTEGER:      toInt,
    LONG:         toInt,
    NUMBER:       toNumber,
    STRING:       toString,
    TEXT:         toString,
    BASETYPENAME: toString,
    LOCATION:     toLocation,
    JSON:         toJSON,
    XML:          toString
};

const stringifyValues = {
    VEC2:      fromVEC2,
    VEC3:      fromVEC3,
    VEC4:      fromVEC4,
    LOCATION:  fromLocation,
    THINGCODE: fromThingCode,
    JSON:      fromJSON
};

const noConversion = v => v;
const convertValue = (v, type) => (convertValues[type] || noConversion)(v);
const stringifyValue = (v, type) => (stringifyValues[type] || noConversion)(v);

function createTextfield(field, item, column) {
    const el = document.createElement('ptcs-textfield');
    const value = item[field] || (item[field] === 0 ? '0' : '');
    el.text = (value && typeof value === 'object' && value.href) ? value.href : stringifyValue(value, column.type);
    el.addEventListener('text-changed', this._textChangedRef);
    if (value !== item[field]) {
        el.updateComplete.then(() => this._setValue(el, el.text));
    }
    return el;
}

function createTextarea(field, item, column) {
    const el = document.createElement('ptcs-textarea');
    const value = item[field];
    el.text = stringifyValue(value, column.type);
    el.addEventListener('text-changed', this._textChangedRef);
    return el;
}

function createDatepicker(field, item, column) {
    const el = document.createElement('ptcs-datepicker');
    el.dateTime = item[field];
    el.addEventListener('date-time-changed', this._dateChangedRef);
    el.dateLabel = this.dateLabel;
    el.monthLabel = this.monthLabel;
    el.yearLabel = this.yearLabel;
    el.hoursLabel = this.hoursLabel;
    el.minutesLabel = this.minutesLabel;
    el.secondsLabel = this.secondsLabel;
    el.meridiemLabel = this.meridiemLabel;
    el.selectLabel = this.selectLabel;
    el.cancelLabel = this.cancelLabel;
    el.formatToken = (column && column.config && column.config.editorProps && column.config.editorProps.formatToken) || '';
    return el;
}

function createCheckbox(field, item) {
    const el = document.createElement('ptcs-checkbox');
    el.checked = toBoolean(item[field]);
    el.addEventListener('checked-changed', this._valueChangedRef);
    return el;
}

function createToggle(field, item) {
    const el = document.createElement('ptcs-toggle-button');
    el.checked = toBoolean(item[field]);
    el.style.justifyContent = 'flex-start';
    el.addEventListener('checked-changed', this._valueChangedRef);
    return el;
}

function createDropdown(field, item, column) {
    if (column.config && Array.isArray(column.config.enum)) {
        // Fixed values
        const el = document.createElement('ptcs-dropdown');
        el.items = column.config.enum;
        el.selectedValue = item[field];
        el.addEventListener('selected-value-changed', this._valueChangedRef);
        return el;
    }
    if (column.enum) {
        // Dynamic values
        const el = document.createElement('ptcs-dropdown');
        const value = column.select(item, this.baseIndex, this.dataManager);

        el.selector = 'label';
        el.valueSelector = 'value';

        // Get allowed values from the client
        if (Array.isArray(column.enum)) {
            if (column.enum.length > 0) {
                el.items = column.enum;
            }
        } else {
            el.setAttribute('field', field);
            const items = column.enum(item, this.baseIndex, this.dataManager);
            if (items instanceof Promise) {
                items.then(_items => {
                    if (Array.isArray(_items)) {
                        el.items = _items;
                    }
                });
            } else if (Array.isArray(items) && items.length > 0) {
                el.items = items;
            }
        }
        if (!el.items) {
            el.items = [{label: value, value}]; // Hopefully more values will come later
        }

        el.selectedValue = value;
        el.addEventListener('selected-value-changed', this._valueChangedRef);
        return el;
    }

    return createTextfield.call(this, field, item, column);
}

function createList(field, item, column) {
    if (column.config && Array.isArray(column.config.enum)) {
        const el = document.createElement('ptcs-list');
        el.items = column.config.enum;
        el.selectedValue = item[field];
        el.addEventListener('selected-value-changed', this._valueChangedRef);
        return el;
    }
    return createTextfield.call(this, field, item, column);
}

function createRadio(field, item, column) {
    if (!column.config && Array.isArray(column.config.enum)) {
        return createTextfield.call(this, field, item, column);
    }

    // Create radio group
    const el = document.createElement('div');
    const type = column.type;
    el.style.display = 'flex';
    el.style.flexDirection = 'column';

    // Label (only show in row-edit mode)
    if (!this.field) {
        if (typeof column.title === 'string') {
            const label = document.createElement('ptcs-label');
            label.label = column.title;
            el.appendChild(label);
        } else if (typeof column.title === 'function') {
            el.appendChild(column.title());
        }
    }

    // Radio buttons
    const radiogroup = 'rg' + performance.now().toString().replace('.', '-');
    let active;
    column.config.enum.forEach(value => {
        const btn = document.createElement('ptcs-radio');
        btn.label = value;
        btn.preventAutoSelect = true;
        btn.radiogroup = radiogroup;
        btn.setAttribute('tabindex', '0');
        if (item[field] === value) {
            active = btn;
        }
        btn.addEventListener('checked-changed', ev => {
            if (ev.detail.value) {
                // This radio button has been selected
                if (this.item[field] !== value || this.values.hasOwnProperty(field)) {
                    this.values[field] = convertValue(value, type);
                    if (this.field) {
                        // Selected a new value when editing a single field: editing is done
                        this.save();
                    }
                }
            }
        });
        el.appendChild(btn);

        // For some strange reason, we must check the active radiobutton after a delay... (bug?)
        if (active) {
            requestAnimationFrame(() => {
                active.checked = true;
                active.focus();
            });
        }
    });
    return el;
}

const createDefault = createDropdown;

const createEditor = {
    textarea:   createTextarea,
    datepicker: createDatepicker,
    checkbox:   createCheckbox,
    toggle:     createToggle,
    dropdown:   createDropdown,
    list:       createList,
    radiogroup: createRadio
};


PTCS.EditGridCells = class extends PTCS.BehaviorTabindex(PTCS.BehaviorStyleable(LitElement)) {
    static get styles() {
        return css`
        :host {
            display: block;
            z-index: 99996;
            box-sizing: border-box;
        }

        [part=controls] {
            overflow: auto;
        }

        [part=buttons] {
            display: flex;
            justify-content: flex-end;
        }

        [part=update-button],
        [part=cancel-button] {
            z-index: 9000;
        }

        [hidden] {
            display: none !important;
        }

        [part=overlay] {
            display: none;
        }

        :host([__pending]) [part=overlay] {
            display: block;
        }

        :host([__pending]) [part=spinner] {
            display: block;
        }

        [part=spinner],
        [part=spinner]:after {
            border-radius: 50%;
            width: 34px;
            height: 34px;
        }

        [part=spinner] {
            display: none;
            z-index: 9999;
            position: absolute;
            top: calc(50% - 17px);
            left: calc(50% - 17px);
            -webkit-transform: translateZ(0);
            -ms-transform: translateZ(0);
            transform: translateZ(0);
            -webkit-animation: load8 1.1s infinite linear;
            animation: load8 1.1s infinite linear;
        }

        @-webkit-keyframes load8 {
            0% {
                -webkit-transform: rotate(0deg);
                transform: rotate(0deg);
            }
            100% {
                -webkit-transform: rotate(360deg);
                transform: rotate(360deg);
            }
        }

        @keyframes load8 {
            0% {
                -webkit-transform: rotate(0deg);
                transform: rotate(0deg);
            }
            100% {
                -webkit-transform: rotate(360deg);
                transform: rotate(360deg);
            }
        }`;
    }

    render() {
        const buttons = !this.field && html`<div part="buttons">
            <ptcs-button part="update-button" variant="primary" tabindex="0"
                .label=${this.updateButtonText} .disabled=${this.__pending} @action=${this.save}></ptcs-button>
            <ptcs-button part="cancel-button" variant="tertiary" tabindex="0"
                .label=${this.cancelButtonText} @action=${this.cancel}></ptcs-button>
        </div>`;

        return html`
            <ptcs-modal-overlay part=overlay backdrop-color="white" backdrop-opacity="60%" backdrop-z-index="9000"></ptcs-modal-overlay>
            <div part=spinner></div>
            <ptcs-label part="label" .label=${this.label} ?hidden=${this._hideTitle()} variant="title"></ptcs-label>
            ${this._buildEditor()}${buttons || ''}`;
    }

    static get is() {
        return 'ptcs-edit-grid-cells';
    }

    static get properties() {
        return {
            // Title
            label: {
                type: String
            },

            // Declarations for all columns, from the grid view configrator (absolutely read-only!)
            columns: {
                type: Array
            },

            // If only a single column should be edited (field must match columns[n].editable)
            field: {
                type:    String,
                reflect: true // So theme engine knows if cell or row is edited
            },

            // Item that has the original values (absolutely read-only!)
            item: {
                type: Object
            },

            // The changed values: {fieldName: new value}
            values: {
                type: Object
            },

            // {field: [validation message, validation details]}
            validation: {
                type: Object
            },

            // Hide validation error message
            hideValidationError: {
                type:      Boolean,
                attribute: 'hide-validation-error'
            },

            // Hide validation criteria message
            hideValidationCriteria: {
                type:      Boolean,
                attribute: 'hide-validation-criteria'
            },

            // Hide validation success message
            hideValidationSuccess: {
                type:      Boolean,
                attribute: 'hide-validation-success'
            },

            // Icon for validation error
            validationErrorIcon: {
                type:      String,
                attribute: 'validation-error-icon'
            },

            // Icon for validation success
            validationSuccessIcon: {
                type:      String,
                attribute: 'validation-success-icon'
            },

            // Icon for validation criteria
            validationCriteriaIcon: {
                type:      String,
                attribute: 'validation-criteria-icon'
            },

            // 'Update' button label
            updateButtonText: {
                type:      String,
                attribute: 'update-button-text'
            },

            // 'Cancel' button label
            cancelButtonText: {
                type:      String,
                attribute: 'cancel-button-text'
            },

            // Calendar labels
            dateLabel: {
                type:      String,
                attribute: 'date-label'
            },

            monthLabel: {
                type:      String,
                attribute: 'month-label'
            },

            yearLabel: {
                type:      String,
                attribute: 'year-label'
            },

            hoursLabel: {
                type:      String,
                attribute: 'hours-label'
            },

            minutesLabel: {
                type:      String,
                attribute: 'minutes-label'
            },

            secondsLabel: {
                type:      String,
                attribute: 'seconds-label'
            },

            meridiemLabel: {
                type:      String,
                attribute: 'meridiem-label'
            },

            selectLabel: {
                type:      String,
                attribute: 'select-label'
            },

            cancelLabel: {
                type:      String,
                attribute: 'cancel-label'
            },

            parentLabel: {
                type:      String,
                attribute: 'parent-label'
            },

            // The label used as the "Parent" value in the edit form when adding a root item
            noParentLabel: {
                type:      String,
                attribute: 'no-parent-label'
            },

            __pending: {
                type:    Boolean,
                reflect: true
            }
        };
    }

    constructor() {
        super();

        this._textChangedRef = this._textChanged.bind(this);
        this._dateChangedRef = this._dateChanged.bind(this);
        this._valueChangedRef = this._valueChanged.bind(this);

        this.updateButtonText = 'Update';
        this.cancelButtonText = 'Cancel';
        this.dateLabel = 'Date';
        this.monthLabel = 'Month';
        this.yearLabel = 'Year';
        this.hoursLabel = 'Hours';
        this.minutesLabel = 'Minutes';
        this.secondsLabel = 'Seconds';
        this.meridiemLabel = 'AM/PM';
        this.selectLabel = 'Select';
        this.cancelLabel = 'Cancel';
        this.parentLabel = 'Parent';
        this.noParentLabel = 'None';
        this.values = {};

        this.__pending = false;
    }

    firstUpdated() {
        super.firstUpdated();

        this.shadowRoot.addEventListener('keydown', this._keydownEv.bind(this));
    }

    update(changedProperties) {
        // Every update rebuilds entire shadow dom. Must manually retain focus
        const focus = this.shadowRoot && this.shadowRoot.activeElement;
        this.__focusOnColumn = focus && focus[columnRef];

        if (focus) {
            this.focus();
        }

        super.update(changedProperties);
    }

    updated(changedProperties) {
        super.updated(changedProperties);

        // Every update rebuilds entire shadow dom. Must manually retain focus
        if (this.__focusOnColumn) {
            const focus = [...this.shadowRoot.querySelectorAll('[tabindex]:not(ptcs-radio:not([checked]))')]
                .find(el => el[columnRef] === this.__focusOnColumn);
            if (focus) {
                focus.focus();
            }
            this.__focusOnColumn = undefined;
        }
    }

    _hideTitle() {
        return !(this.label && !this.field); // Return true or false
    }

    _createEditControl(frag, column, item, validation) {
        const cntr = document.createElement('div');
        cntr.setAttribute('part', 'column');

        // Input
        const type = (column.config && column.config.editor) || dfltEditControl[column.type];
        const input = (createEditor[type] || createDefault).call(this, column.editable, item, column);
        input.setAttribute('part', 'input');

        if (input.tagName !== 'DIV') { // DIV = a compound control that configures itself
            input.setAttribute('tabindex', '0');

            // Keep track of edited field (for validation reporting)
            input[columnRef] = column;

            // Label (only show in row-edit mode)
            if (!this.field || needLabel.indexOf(input.tagName) >= 0) {
                if (typeof column.title === 'string') {
                    input.label = column.title;
                } else if (typeof column.title === 'function') {
                    cntr.appendChild(column.title());
                }
            }

            // Assign editor properties
            if (column.config && column.config.editorProps) {
                for (const propName in column.config.editorProps) {
                    input[propName] = column.config.editorProps[propName];
                }
            }

            if (column && typeof column.validationFunction === 'function' && column.editable && valueProp[input.tagName]) {
                const colName = column.editable;
                const propName = valueProp[input.tagName];
                const validationFunction = column.validationFunction;
                if (propName) {
                    input.extraValidation = el => Promise.resolve(validationFunction(
                        Object.assign({
                            __baseIndex__: this.baseIndex,
                            __el__:        el}, item, {[colName]: el[propName]}))).then(v => !v);
                }
            }

            // Turn on validation?
            if (validation && validation[column.editable] && typeof input.enableValidationMessage === 'function') {
                input.enableValidationMessage();
            }
        }

        // Validation setup
        input._forceAppendValidationMessage = true; // Show the validation message at the bottom of editor always
        input.hideValidationError = this.hideValidationError;
        input.hideValidationCriteria = this.hideValidationCriteria;
        input.hideValidationSuccess =  this.hideValidationSuccess;
        input.validationErrorIcon = this.validationErrorIcon || undefined;
        input.validationSuccessIcon = this.validationSuccessIcon || undefined;
        input.validationCriteriaIcon = this.validationCriteriaIcon || undefined;

        cntr.appendChild(input);
        frag.appendChild(cntr);
    }

    _startValidityPolling() {
        if (this.__pendingInterval) {
            clearInterval(this.__pendingInterval);
        }

        this.__pendingInterval = setInterval(() => {
            // Show the spinner if an element is waiting for validation
            this.__pending = [...this.shadowRoot.querySelectorAll('[part=input]')]
                .some(el => el[columnRef] && !el._stayUnvalidated && typeof el.getValidity === 'function' && el.getValidity() === 'unvalidated');
        }, 120);
    }

    _stopValidityPolling() {
        this.__pending = false;
        if (this.__pendingInterval) {
            clearInterval(this.__pendingInterval);
            this.__pendingInterval = undefined;
        }
    }

    _buildEditor() {
        const {columns, field, item, validation} = this;

        // Create new edit controls
        const el = document.createElement('div');
        el.setAttribute('id', 'controls');
        el.setAttribute('part', 'controls');

        if (field) {
            // ptcs-grid editLevel is single cell or whole grid
            const column = columns.find(col => col.editable === field);
            if (!column) {
                console.error('Unknown column: ' + field);
                return null;
            }
            this._createEditControl(el, column, item, validation);
        } else {
            // ptcs-grid editLevel is single row
            if (this.theParentLabel !== false && this.theParentLabel !== undefined) {
                const div = document.createElement('div');
                div.innerHTML = '<div><ptcs-label></ptcs-label></div><div><ptcs-label variant="body"></ptcs-label></div>';

                // Add external labels without risking injections
                const labels = div.querySelectorAll('ptcs-label');
                labels[0].label = this.parentLabel;
                // theParentLabel === null => adding a root item
                labels[1].label = this.theParentLabel !== null ? this.theParentLabel : this.noParentLabel;

                // Append parent name
                el.appendChild(div);
            }

            columns.forEach(column => {
                if (column.editable && !column.noRowEdit) {
                    this._createEditControl(el, column, item, validation);
                }
            });
        }

        return el;
    }

    _setValue(el, value) {
        return this._setRawValue(el[columnRef], value);
    }

    _setRawValue(column, value) {
        const field = column?.editable;
        if (!field) {
            return false;
        }

        let newValue = convertValue(value, column.type);

        // in case of numeric type, Reject a string or an empty string for partial behavior alignment with legacy grid on clearing the cell contents
        if ((!value || newValue === null) && isNumericType(column.type)) {
            newValue = this.item[field];
        }

        if (column.select(this.item, this.baseIndex, this.dataManager) !== newValue) {
            this.values[field] = column.encode ? column.encode(newValue, this.item, this.baseIndex, this.dataManager) : newValue; // Changed value
            return true;
        }
        if (this.values.hasOwnProperty(field)) {
            delete this.values[field]; // Restored original value
            return true;
        }

        return false; // No change
    }

    // Text value changed. More changes may be coming
    _textChanged(ev) {
        this._setValue(ev.target, ev.detail.value);
    }

    _dateChanged(ev) {
        // If the date field is empty, don't close the dialog
        this._valueChanged(ev, ev.detail.value === undefined);
    }

    // Value changed. Can validate now, and check for dependent values
    _valueChanged(ev, keepOpen) {
        if (!this._setValue(ev.target, ev.detail.value)) {
            return;
        }

        // Selected a new value when editing a single field: editing is done
        // The component may need a moment to perform all validations
        const close = () => !keepOpen && setTimeout(this.save.bind(this), 20);

        if (this.viewManager && this.viewManager.findDependentChanges) {
            const f = ev.target[columnRef]?.editable;
            const promise = f && this.viewManager.findDependentChanges(this.item, this.values, f, this.baseIndex, this.dataManager);
            if (promise instanceof Promise) {
                promise.then(reset => {
                    if (!reset) {
                        if (this.field) {
                            close();
                        }
                        return; // No changes needed
                    }
                    if (this.field) {
                        // Single cell value editor (values can be affected, but no UI controls)
                        for (const field in reset) {
                            // What about secondary dependencies? Danger!
                            this._setRawValue(this.columns.find(col => col.editable === field), reset[field].value);
                        }
                        close();
                    } else {
                        // Row values editor (dropdowns can be affected)
                        for (const field in reset) {
                            const el = this.shadowRoot.querySelector(`ptcs-dropdown[field="${field}"]`);
                            if (!el) {
                                continue;
                            }
                            const {value, values} = reset[field];
                            if (PTCS.sameArray(values, el.items, (v, item) => v === item.value)) {
                                // Already has the correct values
                                continue;
                            }
                            // Values has changed. (Make sure the values are not only permutated?)
                            el.selectedValue = undefined;
                            el.items = values.map(v => ({label: v, value: v}));
                            el.selectedValue = value === null ? undefined : value;
                        }
                    }
                }, error => console.error(error));

                return; // The Promise will finish things
            }
        }

        if (this.field) {
            close();
        }
    }

    initFocus() {
        // Don't focus on unselected radio-buttons - or focus on anything
        const el = this.shadowRoot.querySelector('[tabindex]:not(ptcs-radio:not([checked]))') || this.shadowRoot.querySelector('[tabindex]');
        if (el) {
            el.focus();
        }
    }

    cancel() {
        this._stopValidityPolling();
        this.dispatchEvent(new CustomEvent('close', {detail: {action: 'cancel'}}));
    }

    save() {
        requestAnimationFrame(this._doSave.bind(this));
    }

    _doSave() {
        if (this.__pending) {
            return; // Not ready to be saved
        }
        // eslint-disable-next-line no-unused-vars
        for (const k in this.values) {
            // Found at least one updated value
            const validation = {};
            this.shadowRoot.querySelectorAll('[validity], [part=input]').forEach(el => {
                if (el[columnRef] && typeof el.getValidity === 'function' && el.getValidity() === 'invalid') {
                    // Numbers whose edited value is NaN are rejected unconditionally so the corresponding grid field
                    // should not be set as invalid, but pass through other use cases.
                    if (!(['INTEGER', 'NUMBER', 'LONG'].includes(el[columnRef].type) && isNaN(el.text))) {
                        const a = [el.validationMessage || 'Invalid'];
                        if (el.validationCriteria) {
                            a.push(el.validationCriteria);
                        }
                        validation[el[columnRef].editable] = a;
                    }
                }
            });
            this._stopValidityPolling();
            this.dispatchEvent(new CustomEvent('close', {detail: {values: this.values, validation}}));
            return;
        }

        this._stopValidityPolling();
        this.dispatchEvent(new CustomEvent('close'));
    }

    _keydownEv(ev) {
        if (ev.defaultPrevented) {
            return;
        }
        switch (ev.key) {
            case 'Enter':
                if (!ev.shiftKey) { // Shift-Enter adds newlines to controls that supports Enter
                    this.save();
                    ev.preventDefault();
                }
                break;

            case 'Escape':
                this.cancel();
                ev.preventDefault();
                break;
        }
    }

    setProperties(properties) {
        // Reset values
        this.values = {};

        for (const name in properties) {
            this[name] = properties[name];
        }

        this.updateComplete.then(this._startValidityPolling.bind(this));
    }
};

customElements.define(PTCS.EditGridCells.is, PTCS.EditGridCells);

