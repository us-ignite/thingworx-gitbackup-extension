// data-viewer UI for NUMBER, LONG, INTEGER

import 'ptcs-label/ptcs-label';
import 'ptcs-textfield/ptcs-textfield';
import {createPTCLabelForGridCellWithUIProp, configureValidation} from './gv-text';

const editableField = Symbol('editable');
const assignedValueField = Symbol('assigned');
const assignedDataField = Symbol('data');

// returns: {value, item, baseIndex, dataManager}
const validationValuesOf = el => Object.assign({value: Number(el.text)}, el[assignedDataField]);

/* eslint-disable no-nested-ternary */

function toString(v) {
    if (!v) {
        return '';
    }
    return v.toString ? v.toString() : `${v}`;
}

function valueChanged(ev) {
    const el = ev.target;
    const old = el[assignedValueField];
    if (old !== el.text) {
        const value = Number(el.text);
        if (isNaN(value)) {
            // restore old value
            el.text = el[assignedValueField];
        } else {
            const isInteger = ['LONG', 'INTEGER'].indexOf(el[editableField]) >= 0;
            el.dispatchEvent(new CustomEvent('edited-value', {bubbles: true, detail: {value: isInteger ? Math.round(value) : value}}));
        }
    }
}

function textKeydown(ev) {
    if (ev.key === 'Enter') {
        valueChanged(ev);
    }
}

function createNumber(cell, editable, options) {
    let el;
    if (editable) {
        el = document.createElement('ptcs-textfield');
        el.setAttribute('grid-action', 'tab enter');
        el.noTabindex = true;
        el[editableField] = options.baseType || true;
        el.addEventListener('blur', valueChanged);
        el.addEventListener('keydown', textKeydown);
        if (options.editorProps) {
            for (const propName in options.editorProps) {
                el[propName] = options.editorProps[propName];
            }
        }
        configureValidation(el, options, validationValuesOf);
    } else {
        el = createPTCLabelForGridCellWithUIProp(options);
        el.setAttribute('variant', 'grid-item');

        if (options.format) {
            el.__numberFormat = options.format;
        }
    }

    el.setAttribute('disable-tooltip', '');
    cell.tooltipFunc = () => el.tooltipFunc();

    return el;
}

function createRowNumber(cell, options) {
    const el = createNumber(cell, false, options);

    // Remove right padding if there is a select checkbox
    if (options.selectMethod === 'multiple') {
        cell.style.paddingRight = '0px';
    }

    return el;
}

function updateNumber(el, value, index, dataManager) {
    if (el[editableField]) {
        const text = value !== undefined ? `${value}` : '';
        el[assignedValueField] = text;
        el[assignedDataField] = {item: dataManager.item(index), baseIndex: dataManager.baseIndex(index), dataManager};
        el.text = text;
    } else if (value !== undefined) {
        el.label = el.__numberFormat ? `${el.__numberFormat.format(value)}` : `${value}`;
    } else {
        el.label = '';
    }
}

export function uiNumber(_config) {
    const config = (_config && typeof _config === 'object') ? _config : {};
    const minHeight = config.minHeightRow;
    const maxHeight = config.maxHeightRow;
    const {baseType, halign, valign, selectMethod, editable, editorProps, validationFunction} = config;

    const numberFormat = config.locales
        ? (config.options ? new Intl.NumberFormat(config.locales, config.options) : new Intl.NumberFormat(config.locales))
        : null;

    // if format === null, then this item skips filtering - so it must be undefined
    const format = (numberFormat && numberFormat.format && (v => [toString(v), numberFormat.format(v)])) || undefined;
    if (config.showRowNumbers) {
        const options = {format: numberFormat, singleLine: true, minHeight, maxHeight, halign, valign, selectMethod};
        return {create: cell => createRowNumber(cell, options), assign: updateNumber, format};

    }
    const options = {format: numberFormat, singleLine: true, minHeight, maxHeight, halign, valign, baseType, editable, editorProps,
        validationFunction};
    const externalEdit = (config.editor === 'textarea') || undefined;
    const create = externalEdit ? cell => createNumber(cell, false, options) : (cell, _editable) => createNumber(cell, _editable, options);

    // if format === null, then this item skips filtering - so it must be undefined
    return {create, assign: updateNumber, format, externalEdit};
}
