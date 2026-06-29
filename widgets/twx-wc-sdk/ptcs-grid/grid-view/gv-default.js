// Fallback data-viewer UI for unknown baseTypes
import {createPTCLabelForGridCellWithUIProp} from './gv-text';

function createDefault(cell, opt) {
    const el = createPTCLabelForGridCellWithUIProp(opt);
    el.setAttribute('variant', 'grid-item');
    el.setAttribute('disable-tooltip', '');
    cell.tooltipFunc = () => el.tooltipFunc();
    return el;
}

function assignDefault(el, value) {
    switch (typeof value) {
        case 'string':
            el.label = value;
            break;

        case 'boolean':
        case 'number':
        case 'undefined':
        case 'bigint':
            el.label = `${value}`;
            break;

        default: // symbol, function, or object
            el.label = value instanceof Date ? `${value}` : `unknown ${typeof value}`;
    }
}

export function uiDefault(config) {
    const singleLine = config.singleLineRows;
    const minHeight = config.minHeightRow;
    const maxHeight = config.maxHeightRow;
    const halign = config.halign;
    const valign = config.valign;

    return {create: cell => createDefault(cell, {singleLine, minHeight, maxHeight, halign, valign}), assign: assignDefault};
}
