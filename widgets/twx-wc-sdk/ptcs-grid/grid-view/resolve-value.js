// data-viewer UI for delete button (#delete)
import {PTCS} from 'ptcs-library/library.js';
import {ValueManager} from '../grid-values.js';

const $type = Symbol('type');

function assignResolveValue(el, columnId, value, uiControl, index, dm) {
    const decodeType = () => { // NaV, ErrorValue, true
        if (value === ValueManager.NaV) {
            return uiControl.unresolvable && uiControl.unresolvable(dm.item(index), columnId, index, dm) ? ValueManager.ErrorValue : value;
        }
        return value === ValueManager.ErrorValue ? value : true;
    };

    const type = decodeType();

    if (type !== el[$type]) {
        while (el.firstChild) {
            el.removeChild(el.firstChild);
        }

        switch (el[$type]) {
            case ValueManager.NaV:
                el.removeAttribute('unresolved');
                el.dispatchEvent(new CustomEvent('resolved-value', {bubbles: true, detail: {columnId, index, cell: el.parentNode}}));
                break;

            case ValueManager.ErrorValue:
                el.removeAttribute('error');
                el.parentElement.removeAttribute('invalid');
                break;
        }

        el[$type] = type;

        switch (el[$type]) {
            case ValueManager.NaV:
                el.setAttribute('unresolved', columnId);
                break;

            case ValueManager.ErrorValue: {
                el.setAttribute('error', '');
                el.parentElement.setAttribute('invalid', '');
                const icon = PTCS.createElement('ptcs-icon', {part: 'invalid-icon'});
                icon.icon = 'cds:icon_error';
                icon.size = 'small';
                el.appendChild(icon);
                break;
            }

            case true: // Resolved value
                el.appendChild(uiControl.create(el.parentElement));
        }
    }

    switch (el[$type]) {
        case ValueManager.NaV:
            el.dispatchEvent(new CustomEvent('unresolved-value', {bubbles: true, detail: {columnId, index, cell: el.parentNode}}));
            break;

        case true: // Resolved value
            uiControl.assign(el.firstChild, value, index, dm);
    }
}

export function uiResolveValue(columnId, uiControl) {
    const {format, render} = uiControl;
    const _format = format ? value => (typeof value !== 'symbol' ? format(value) : undefined) : undefined;
    const _render = render ? value => (typeof value !== 'symbol' ? render(value) : undefined) : undefined;
    const _fallback = (_format && _render) ? undefined : value => typeof value !== 'symbol' ? value : undefined;

    return {
        create: () => PTCS.createElement('div', {part: 'delayed-value', class: 'resolve'}),
        assign: (el, value, index, dm) => assignResolveValue(el, columnId, value, uiControl, index, dm),
        format: _format || _fallback,
        render: _render
    };
}
