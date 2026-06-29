// data-viewer UI for LOCATION
import {createPTCLabelForGridCellWithUIProp, configureValidation} from './gv-text';

const editableField = Symbol('editable');
const assignedValueField = Symbol('assigned');
const assignedDataField = Symbol('data');

const f4 = v => typeof v === 'number' ? v.toFixed(4) : v;

// Encode location as text
function _locationText(value, f = (v => v)) {
    if (!value && value !== 0) {
        return '';
    }
    return typeof value === 'object'
        ? `${f(value.latitude)} : ${f(value.longitude)}${value.elevation ? (' : ' + f(value.elevation)) : ''}`
        : `${value}`;
}

function locationText(value) {
    return _locationText(value, f4);
}

function toLocation(value) {
    const values = typeof value === 'string' ? value.split(':') : [];

    return (value.length >= 2 && values.every(v => !isNaN(v)))
        ? {latitude: Number(values[0]), longitude: Number(values[1]), elevation: Number(values[2])}
        : undefined;
}

// returns: {value, item, baseIndex, dataManager}
const validationValuesOf = el => Object.assign({value: toLocation(el.text)}, el[assignedDataField]);

function valueChanged(ev) {
    const el = ev.target;
    const old = el[assignedValueField];
    const value = el.text;
    if (old !== el.text) {
        const location = toLocation(value);
        if (location === undefined && value !== '') {
            // Correct invalid input
            el.text = old;
        } else {
            // Assign new valid input
            el[assignedValueField] = el.text;
            el.dispatchEvent(new CustomEvent('edited-value', {bubbles: true, detail: {value: location}}));
        }
    }
}

function textKeydown(ev) {
    if (ev.key === 'Enter') {
        valueChanged(ev);
    }
}

function createLocation(cell, editable, options) {
    let el;
    if (editable) {
        el = document.createElement('ptcs-textfield');
        el.setAttribute('grid-action', 'tab enter');
        el.noTabindex = true;
        el[editableField] = true;
        el.addEventListener('blur', valueChanged);
        el.addEventListener('keydown', textKeydown);
        if (options.editorProps) {
            for (const propName in options.editorProps) {
                el[propName] = options.editorProps[propName];
            }
        }
        configureValidation(el, options, validationValuesOf);
    } else {
        // ptcs-label that displays the string
        el = createPTCLabelForGridCellWithUIProp(options);
        el.setAttribute('variant', 'grid-item');
    }

    el.setAttribute('disable-tooltip', '');
    cell.tooltipFunc = () => el.tooltipFunc();

    return el;
}

// Assign new value to location element (ptcs-label)
function assignLocation(el, value, index, dataManager) {
    if (el[editableField]) {
        const text = _locationText(value);
        el[assignedDataField] = {item: dataManager.item(index), baseIndex: dataManager.baseIndex(index), dataManager};
        el[assignedValueField] = text;
        el.text = text;
    } else {
        el.label = locationText(value);
    }
}

export function uiLocation(_config) {
    const config = (_config && typeof _config === 'object') ? _config : {};
    const options = {
        singleLine:         config.singleLineRows,
        minHeight:          config.minHeightRow,
        maxHeight:          config.maxHeightRow,
        halign:             config.halign,
        valign:             config.valign,
        editorProps:        config.editorProps,
        editable:           config.editable,
        validationFunction: config.validationFunction
    };
    const externalEdit = (config.editor === 'textarea') || undefined;
    const create = externalEdit ? cell => createLocation(cell, false, options) : (cell, editable) => createLocation(cell, editable, options);
    return {create, assign: assignLocation, format: locationText, externalEdit};
}
