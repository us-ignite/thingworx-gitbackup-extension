// data-viewer UI for HTML
import 'ptcs-textfield/ptcs-textfield';
import {assignHeightMinusCellPadding, configureValidation} from './gv-text';

const editableField = Symbol('editable');
const assignedValueField = Symbol('value');
const assignedDataField = Symbol('data');

// returns: {value, item, baseIndex, dataManager}
const validationValuesOf = el => Object.assign({value: el.text}, el[assignedDataField]);

function setMinH(el, minHeight) {
    el.style.minHeight = minHeight + 'px';
}

function setMaxH(el, maxHeight) {
    el.style.maxHeight = maxHeight + 'px';
}

function valueChanged(ev) {
    const el = ev.target;
    const value = el.text;
    const old = el[assignedValueField];
    if (old !== value) {
        el[assignedValueField] = value;
        el.dispatchEvent(new CustomEvent('edited-value', {bubbles: true, detail: {value}}));
    }
}

function textKeydown(ev) {
    if (ev.key === 'Enter') {
        valueChanged(ev);
    }
}

// Create span element to render html content
function createHtml(cell, editable, config) {
    if (editable) {
        const el = document.createElement('ptcs-textfield');
        el.setAttribute('grid-action', 'tab enter');
        el[editableField] = true;
        el.addEventListener('blur', valueChanged);
        el.addEventListener('keydown', textKeydown);
        el.noTabindex = true;
        el.hideValidationSuccess = el.hideValidationError = el.hideValidationCriteria = true;
        if (config.editorProps) {
            for (const propName in config.editorProps) {
                el[propName] = config.editorProps[propName];
            }
        }
        configureValidation(el, config, validationValuesOf);
        return el;
    }

    const ctr = document.createElement('div');
    const el = document.createElement('span');

    if (config.maxHeightRow > 0 || config.minHeightRow > 0) {
        ctr.style.width = '100%';
        el.style.overflow = 'hidden';
        el.style.textOverflow = 'ellipsis';
        assignHeightMinusCellPadding(ctr, config.minHeightRow, config.maxHeightRow, setMinH, setMaxH);
    }

    if (config.minHeightRow) {
        ctr.style.display = 'flex';

        // eslint-disable-next-line no-nested-ternary
        ctr.style.alignItems = config.valign === 'top' ? 'flex-start' : (config.valign === 'bottom' ? 'flex-end' : config.valign);
        // eslint-disable-next-line no-nested-ternary
        ctr.style.justifyContent = config.halign === 'left' ? 'flex-start' : (config.halign === 'right' ? 'flex-end' : config.halign);
    }

    ctr.appendChild(el);
    ctr.setAttribute('part', 'cell-html');

    return ctr;
}

// Assign the html content
function assignHtml(el, value, index, dataManager) {
    if (el[editableField]) {
        el[assignedValueField] = value;
        el[assignedDataField] = {item: dataManager.item(index), baseIndex: dataManager.baseIndex(index), dataManager};
        el.text = value;
    } else {
        el.firstChild.innerHTML = typeof value === 'string' ? value : '';
    }
}

function decodeHtml(html) {
    if (typeof html === 'string') {
        const entities = {
            '&amp;':  '&',
            '&lt;':   '<',
            '&gt;':   '>',
            '&apos;': '\'',
            '&quot;': '"'
        };
        const namedEntitiesPattern = new RegExp(Object.keys(entities).join('|'), 'g');
        html = html
            .replace(namedEntitiesPattern, match => entities[match])
            .replace(/&#x([0-9A-Fa-f]+);|&#(\d+);/g, (match, hex, dec) => {
                return String.fromCharCode(hex ? parseInt(hex, 16) : parseInt(dec, 10));
            });
    }

    return html;
}

export function uiHtml(_config) {
    const config = (_config && typeof _config === 'object') ? _config : {};
    const create = config.editor === 'textarea' ? cell => createHtml(cell, false, config) : (cell, editable) => createHtml(cell, editable, config);
    const format = config.stripHtml ? v => decodeHtml(typeof v === 'string' ? v.replace(/<[^>]+>/g, '') : v) : v => decodeHtml(v);

    return {create, assign: assignHtml, format, externalEdit: config.editor === 'textarea'};
}
