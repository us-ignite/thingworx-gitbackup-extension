// data-viewer UI for DATETIME

import 'ptcs-label/ptcs-label';
import {asDate} from 'ptcs-datepicker/ptcs-datepicker';
import {createPTCLabelForGridCellWithUIProp, configureValidation} from './gv-text';

const assignedValueField = Symbol('editable');
const editableField = Symbol('editable');
const assignedDataField = Symbol('data');

// returns: {value, item, baseIndex, dataManager}
const validationValuesOf = el => Object.assign({value: el.dateTime}, el[assignedDataField]);

const i18nLabels = ['monthLabel', 'yearLabel', 'hoursLabel', 'minutesLabel', 'secondsLabel', 'meridiemLabel', 'selectLabel', 'cancelLabel'];

function valueChanged(ev) {
    const el = ev.target;
    const old = el[assignedValueField];
    const {value} = ev.detail;
    if (old !== value) {
        el[assignedValueField] = value;
        el.dispatchEvent(new CustomEvent('edited-value', {bubbles: true, detail: {value}}));
    }
}

function createDatetime(cell, editable, format, config) {
    let el;
    if (editable) {
        el = document.createElement('ptcs-datepicker');
        el[editableField] = config.editable || true;
        el.setAttribute('grid-action', 'tab enter');
        el.addEventListener('date-time-changed', valueChanged);
        el.noTabindex = true;

        if (config.editor === 'textfield') {
            el.disableMaskedInput = true;
            el.setAttribute('hide-calendar-icon', '');
        }

        if (config.editorProps) {
            for (const propName in config.editorProps) {
                el[propName] = config.editorProps[propName];
            }
        }
        configureValidation(el, config, validationValuesOf);
        if (config.$i18n) {
            i18nLabels.forEach(key => {
                if (typeof config.$i18n[key] === 'string') {
                    el[key] = config.$i18n[key];
                }
            });
        }
    } else {
        el = createPTCLabelForGridCellWithUIProp({
            singleLine: config.singleLineRows,
            minHeight:  config.minHeightRow,
            maxHeight:  config.maxHeightRow,
            halign:     config.halign,
            valign:     config.valign
        });
        el.setAttribute('variant', 'grid-item');
        if (format) {
            el.__dateTimeFormat = format;
        }
    }

    el.setAttribute('disable-tooltip', '');
    cell.tooltipFunc = () => el.tooltipFunc();

    return el;
}

function assignDatetime(el, value, index, dataManager) {
    if (el[editableField]) {
        const item = dataManager.item(index);
        const dateTime = (item && item[el[editableField]]) || asDate(value);
        el[assignedDataField] = {item, baseIndex: dataManager.baseIndex(index), dataManager};
        el[assignedValueField] = dateTime;
        el.dateTime = dateTime;
    } else if (value instanceof Date) {
        el.label = (!isNaN(value) && el.__dateTimeFormat) ? `${el.__dateTimeFormat.format(value)}` : `${value}`;
    } else if (value) {
        el.label = `${value}`;
    } else {
        el.label = '';
    }
}

// This function is only called when the client want to search the date value
// It returns a stringified version of: (i) the rendered date, if the client specifies it, or (ii) the selected date
function formatDate(selected, rendered) {
    return `${rendered !== undefined ? rendered : selected}`;
}

export function uiDatetime(_config, $i18n) {
    const config = (_config && typeof _config === 'object') ? _config : {};
    config.$i18n = $i18n;

    // eslint-disable-next-line no-nested-ternary
    const format = (config && config.locales)
        ? (config.options ? new Intl.DateTimeFormat(config.locales, config.options) : new Intl.DateTimeFormat(config.locales))
        : null;

    const externalEdit = (config.editor === 'textarea') || undefined;
    const create = externalEdit
        ? cell => createDatetime(cell, false, format, config)
        : (cell, editable) => createDatetime(cell, editable, format, config);

    return {create, assign: assignDatetime, format: (format && format.format) || formatDate, externalEdit};
}
