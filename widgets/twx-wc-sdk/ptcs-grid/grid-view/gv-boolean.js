import 'ptcs-checkbox/ptcs-checkbox';
import 'ptcs-textfield/ptcs-textfield';
import {createPTCLabelForGridCellWithUIProp, configureValidation} from './gv-text';
import {PTCS} from 'ptcs-library/library.js';

const assignedValueField = Symbol('editable');
const editableField = Symbol('editable');
const boolTypeField = Symbol('boolType');
const assignedDataField = Symbol('data');

const validationValuesOfCheckbox = el => Object.assign({value: el.checked}, el[assignedDataField]);
const validationValuesOfTextfield = el => Object.assign({value: el.text !== 'true'}, el[assignedDataField]);

const trueString = v => v !== '' && v !== 'false';
const toBoolean = v => typeof v === 'string' ? trueString(v.trim()) : !!v;

// eslint-disable-next-line no-nested-ternary
const valign = v => v === 'top' ? 'flex-start' : (v === 'bottom' ? 'flex-end' : v);

function doNothing() {
}

function valueChanged(el, value) {
    const checked = toBoolean(value);
    const old = el[assignedValueField];
    if (old !== value) {
        el[assignedValueField] = checked;
        el.dispatchEvent(new CustomEvent('edited-value', {bubbles: true, detail: {value: checked}}));
    }
}

function checkedChanged(ev) {
    valueChanged(ev.target, ev.detail.value);
}

function createCheckbox(config) {
    const el = document.createElement('ptcs-checkbox');
    el.setAttribute('part', 'cell-checkbox');
    el.noTabindex = true;
    el.label = '';
    el.verticalAlignment = valign(config.valign);
    return el;
}

function assignCheckbox(value) {
    this.checked = value;
}

function createTextfield(config) {
    const el = document.createElement('ptcs-textfield');
    el.noTabindex = true;
    el.label = '';
    el.verticalAlignment = valign(config.valign);
    return el;
}

function textChanged(ev) {
    valueChanged(ev.target, ev.target.text);
}

function textKeydown(ev) {
    if (ev.key === 'Enter') {
        valueChanged(ev.target, ev.target.text);
    }
}

function assignTextfield(value) {
    this.text = `${value}`;
}

function createBoolean(cell, editable, config) {
    let el;

    if (editable) {
        if (config.editor === 'textfield') {
            el = createTextfield(config);
            el.setAttribute('grid-action', 'updown');
            el.addEventListener('keydown', textKeydown);
            el.addEventListener('blur', textChanged);
            el[editableField] = assignTextfield;
            configureValidation(el, config, validationValuesOfTextfield);
        } else {
            el = createCheckbox(config);
            el.setAttribute('grid-action', 'tab enter');
            el.addEventListener('checked-changed', checkedChanged);
            el[editableField] = assignCheckbox;
            configureValidation(el, config, validationValuesOfCheckbox);
        }
        if (config.editorProps) {
            for (const propName in config.editorProps) {
                el[propName] = config.editorProps[propName];
            }
        }
    } else if (config.boolType === 'checkbox') {
        // ptcs-checkbox
        el = createCheckbox(config);
        el.disabled = true;
        el._zeroPadding = true; // Reset padding (override theming)
        el.performGridDisable = doNothing;
    } else {
        // ptcs-label that displays 'true' or 'false'
        el = createPTCLabelForGridCellWithUIProp({
            singleLine: config.singleLineRows,
            minHeight:  config.minHeightRow,
            maxHeight:  config.maxHeightRow,
            valign:     config.valign
        });
        el.setAttribute('variant', 'grid-item');
        el[boolTypeField] = config.boolType;
    }

    el.setAttribute('disable-tooltip', '');
    cell.tooltipFunc = () => el.tooltipFunc();

    return el;
}

function assignBoolean(el, value, index, dataManager) {
    const checked = toBoolean(value);

    if (el[editableField]) {
        el[assignedDataField] = {item: dataManager.item(index), baseIndex: dataManager.baseIndex(index), dataManager};
        el[assignedValueField] = checked;
        el[editableField](checked);
    } else if (el.tagName === 'PTCS-CHECKBOX') {
        // Read-only checkbox
        el.checked = checked;
    } else {
        // label
        el.label = el[boolTypeField] === 'notext' ? '' : `${checked}`;
    }
}

export function uiBoolean(_config) {
    const config = (_config && typeof _config === 'object') ? _config : {};
    const format = v => v ? 'true' : 'false';

    if (config.editor === 'textarea') {
        return {create: cell => createBoolean(cell, false, config), assign: assignBoolean, format, externalEdit: true};
    }

    return {create: (cell, editable) => createBoolean(cell, editable, config), assign: assignBoolean, format};
}

// Custom element that observes a Data Manager
class GridBoolSelectionObserver extends HTMLElement {
    constructor() {
        super();
        this.__stateChangedAnimationFrame = null;
    }
    static get is() {
        return 'ptcs-grid-bool-selection-observer';
    }

    set dataMgr(dataMgr) {
        if (this.isConnected && this._dataMgr) {
            this._dataMgr.unobserve(this);
        }
        this._dataMgr = dataMgr;
        if (this._dataMgr && this.isConnected) {
            this._dataMgr.observe(this);
        }
    }
    connectedCallback() {
        if (this._dataMgr) {
            this._dataMgr.observe(this);
        }
    }
    disconnectedCallback() {
        if (this._dataMgr) {
            this._dataMgr.unobserve(this);
        }
    }
    dmView() {
        this._updateState();
    }
    dmRemoved() {
        this._updateState();
    }
    dmInserted() {
        this._updateState();
    }
    dmItem() {
        this._updateState();
    }
    dmUpdated() {
        this._updateState();
    }
    _updateState() {
        if (this.firstChild && typeof this.firstChild.stateChanged === 'function') {
            if (this.__stateChangedAnimationFrame) {
                cancelAnimationFrame(this.__stateChangedAnimationFrame);
            }
            this.__stateChangedAnimationFrame = requestAnimationFrame(() => {
                this.firstChild.stateChanged();
            });
        }
    }
}
customElements.define(GridBoolSelectionObserver.is, GridBoolSelectionObserver);

// Create bulkCheckbox selector
export function bulkCreatorFunc(dataMgr, dataView, columnName) {
    if (!columnName) {
        return false;
    }
    const cntr = document.createElement('ptcs-grid-bool-selection-observer');
    cntr.dataMgr = dataMgr;
    cntr.dataView = dataView;
    cntr.style.display = 'inline-flex';
    const el = PTCS.createElement('ptcs-checkbox', {part: 'bulk-checkbox-selection', 'grid-action': ''});
    el.noTabindex = true;
    el.label = '';

    const countSelected = (dm) => {
        const shouldCount = dm.isDisabled
            ? baseIndex => dm.baseItem(baseIndex)[columnName] && !dm.isDisabled(baseIndex)
            : baseIndex => dm.baseItem(baseIndex)[columnName];
        let count = 0;
        for (let baseIndex = 0; baseIndex < dm.baseLength; baseIndex++) {
            if (shouldCount(baseIndex)) {
                count++;
            }
        }
        return count;
    };

    el.stateChanged = function() {
        if (!dataMgr) {
            return;
        }
        const count = countSelected(dataMgr);
        el.setProperties({checked: count > 0, partial: 0 < count && count < dataMgr.baseLength});
    };

    el.addEventListener('checked-changed', function(ev) {
        if (!dataMgr) {
            return;
        }
        const cbChecked = ev.detail.value;
        const dmChecked = countSelected(dataMgr) > 0;
        if (!cbChecked !== !dmChecked) {
            this.dispatchEvent(new CustomEvent('bulk-select-changed', {
                bubbles: true,
                detail:  {value: cbChecked, columnName}
            }));
            el.stateChanged();
        }
    });
    el.stateChanged();
    cntr.appendChild(el);

    return cntr;
}
