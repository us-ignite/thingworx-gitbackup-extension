// data-viewer UI for STRING
import 'ptcs-label/ptcs-label';
import 'ptcs-textfield/ptcs-textfield';
import 'ptcs-dropdown/ptcs-dropdown';
import {createPTCLabelForGridCellWithUIProp, configureValidation} from './gv-text';

const editableField = Symbol('editable');
const assignedValueField = Symbol('value');
const assignedDataField = Symbol('data');
const queryField = Symbol('query');
const baseIndexField = Symbol('baseIndex');
const oldValueField = Symbol('oldValue');

// returns: {value, item, baseIndex, dataManager}
const validationValuesOfTextfield = el => Object.assign({value: el.text}, el[assignedDataField]);
const validationValuesOfDropdown = el => Object.assign({value: el.selectedValue}, el[assignedDataField]);


function valueChanged(el, value) {
    const old = el[assignedValueField];
    if ((old || undefined) !== (value || undefined)) {
        el[assignedValueField] = value;
        el.dispatchEvent(new CustomEvent('edited-value', {bubbles: true, detail: {value}}));
    }
}

function textValueChanged(ev) {
    valueChanged(ev.target, ev.target.text);
}

function textKeydown(ev) {
    if (ev.key === 'Enter') {
        valueChanged(ev.target, ev.target.text);
    }
}

function selectedValueChanged(ev) {
    valueChanged(ev.target, ev.target.selectedValue);
}

function dropdownModeChanged(ev) {
    const el = ev.target;
    if (ev.detail.value === 'open' && el.items[queryField]) {
        // Need new options in the dropdown. Ask for them
        const $query = el.items[queryField];
        const {item, index, dataManager, getValues} = $query;
        if (item === dataManager.item(index)) {
            const result = getValues(item, dataManager.baseIndex(index), dataManager);
            if (result instanceof Promise) {
                result.then(items => {
                    if (Array.isArray(items) && el.items[queryField] === $query) {
                        el.items = items.map(opt => opt?.value || opt);
                    }
                });
            } else if (Array.isArray(result) && result.length > 0) {
                el.items = result.map(opt => opt?.value || opt);
            }
        }
    }
}

function createString(cell, editable, options) {
    let el;
    if (editable) {
        if (Array.isArray(options.enum)) {
            el = document.createElement('ptcs-dropdown');
            el.setAttribute('grid-action', 'updown');
            el.items = options.enum.map(opt => opt?.value || opt);
            el[editableField] = value => {
                el.selectedValue = value;
            };
            el.addEventListener('selected-value-changed', selectedValueChanged);
            configureValidation(el, options, validationValuesOfDropdown);
        } else if (typeof options.enum === 'function') {
            const getValues = options.enum;
            el = document.createElement('ptcs-dropdown');
            el.setAttribute('grid-action', 'updown');
            el[editableField] = (value, index, dataManager) => {
                const requestValidValues = () => {
                    el.items[queryField] = {item: dataManager.item(index), index, dataManager, getValues};
                };
                const items = Array.isArray(el.items) ? el.items : [];
                const baseIndex = dataManager.baseIndex(index);

                if (value !== null && value !== undefined && value !== '' && items.indexOf(value) === -1) {
                    // Assigned a value that is not a valid dropdown item
                    el.items = [value]; // Make sure at least the assigned value is a valid value
                    requestValidValues();
                } else if (items.length === 0) {
                    el.items = ['--wait--']; // Don't need valid values until the user clicks on the dropdown
                    requestValidValues();
                } else if (el[baseIndexField] !== baseIndex) {
                    // Dropdown used for other item, or value has changed (so we might need new valid values)
                    requestValidValues();
                } else if (value !== el.selectedValue) {
                    // A changed value came from the data manager. The valid values may therefore have changed
                    requestValidValues();
                }

                el[baseIndexField] = baseIndex;
                el[oldValueField] = value;
                el.selectedValue = value;
            };
            el.addEventListener('selected-value-changed', selectedValueChanged);
            el.addEventListener('mode-changed', dropdownModeChanged);
            configureValidation(el, options, validationValuesOfDropdown);
        } else {
            el = document.createElement('ptcs-textfield');
            el.setAttribute('grid-action', 'tab enter');
            el[editableField] = value => {
                el.text = value;
            };
            el.addEventListener('blur', textValueChanged);
            el.addEventListener('keydown', textKeydown);
            configureValidation(el, options, validationValuesOfTextfield);
        }

        el.noTabindex = true;

        if (options.editorProps) {
            for (const propName in options.editorProps) {
                el[propName] = options.editorProps[propName];
            }
        }
    } else {
        // ptcs-label that displays the string
        el = createPTCLabelForGridCellWithUIProp(options);
        el.setAttribute('variant', 'grid-item');
    }

    el.setAttribute('disable-tooltip', '');
    cell.tooltipFunc = () => el.tooltipFunc();

    return el;
}

// Assign new string to ptcs-label
function assignString(el, value, index, dataManager) {
    if (el[editableField]) {
        el[assignedValueField] = value;
        el[assignedDataField] = {item: dataManager.item(index), baseIndex: dataManager.baseIndex(index), dataManager};
        el[editableField](value, index, dataManager);
    } else {
        el.label = value;
    }
}

export function uiString(_config) {
    const config = (_config && typeof _config === 'object') ? _config : {};
    const options = {
        singleLine:         config.singleLineRows,
        minHeight:          config.minHeightRow,
        maxHeight:          config.maxHeightRow,
        halign:             config.halign,
        valign:             config.valign,
        enum:               config.enum,
        editorProps:        config.editorProps,
        editable:           config.editable,
        validationFunction: config.validationFunction,
        preserveWhiteSpace: config.preserveWhiteSpace
    };

    const externalEdit = (config.editor === 'textarea');
    const create = externalEdit ? cell => createString(cell, false, options) : (cell, editable) => createString(cell, editable, options);

    return {create, assign: assignString, externalEdit};
}
