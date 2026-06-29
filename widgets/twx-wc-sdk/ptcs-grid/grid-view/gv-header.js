// data-viewer UI for creating grid column headers

import 'ptcs-label/ptcs-label';
import 'ptcs-icon/ptcs-icon';
import {PTCS} from 'ptcs-library/library.js';
import {createPTCLabelForGridCellWithUIProp} from './gv-text';

import {sortIcon} from './sort';
import {bulkCreatorFunc} from './gv-boolean';

function isColumnSortable(sortable, compare) {
    if (!sortable) {
        return false;
    }

    const f = typeof sortable === 'function' ? sortable : compare;

    return !!f;
}

// Create column header creating function
export function headerCreatorFunc({label, sortable, sortOrder, compare, singleLine, maxHeight, hAlign, vAlign, name, bulkSelection}) {

    const isSortable = isColumnSortable(sortable, compare);

    if (isSortable && !label && !bulkSelection) {
        // Sortable, but no label and no bulkSelection: create a single sort icon (backwards compatible behavior)
        return () => sortIcon(name, sortOrder);
    }

    // Create single wrapper element for the header cell
    const wrap = (cell, el, opt, dm, dv) => {
        const div = PTCS.createElement('div', {style: 'display:flex; align-items:center; width:100%; box-sizing:border-box'});
        let div2 = div;

        // Bulk checkbox?
        if (bulkSelection && (opt?.inlineEditing) && !(opt && opt.noActions) && dm && dv) {
            div.style.justifyContent = 'flex-start'; // Needed when column is too narrow. Prevents checkbox from "disappearing"
            div.appendChild(bulkCreatorFunc(dm, dv, name));
            div2 = PTCS.createElement('div', {style: 'display:flex; align-items:center; width:100%;'});
            div.appendChild(div2);
        }

        // Column title
        div2.style.overflow = 'hidden';
        div2.appendChild(el);

        // Sort button?
        if (isSortable && !(opt && opt.noActions)) {
            div2.appendChild(sortIcon(name, sortOrder));
        }

        // Move title tooltip to header cell?
        if (typeof el.tooltipFunc === 'function') {
            el.setAttribute('disable-tooltip', '');
            cell.tooltipFunc = el.tooltipFunc.bind(el);
        }

        return div;
    };

    if (typeof label === 'string') {
        return (dm, dv, cell, opt) => {
            const el = createPTCLabelForGridCellWithUIProp({singleLine, minHeight: '0', maxHeight, part: 'header-label'});
            el.setAttribute('variant', 'grid-item');
            el.label = label;
            if (!singleLine) {
                // In multiline case label strethes itself on the cell. We should use its own horizontal alignment.
                el.horizontalAlignment = hAlign;
                let vAlignment = vAlign;
                if (vAlign === 'top') {
                    vAlignment = 'flex-start';
                } else if (vAlign === 'bottom') {
                    vAlignment = 'flex-end';
                }
                el.verticalAlignment = vAlignment;
            }

            return wrap(cell, el, opt, dm, dv);
        };
    }
    if (typeof label === 'function') {
        return (dm, dv, cell, opt) => wrap(cell, label({singleLine, maxHeight}), opt);
    }
    console.error('Invalid label');
    return null;
}
