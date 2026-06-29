import {PTCS} from 'ptcs-library/library.js';
import {baseTypeComparator} from './grid-view.js';

const NOP = () => {};

function _sortTree(items, compare, getSubItems, setSubItems) {
    const _assignSorted = (item, children) => {
        if (children instanceof Promise) {
            return children.then(r => _assignSorted(item, r));
        }
        if (Array.isArray(children) && children.length > 0) {
            setSubItems(item, children);
        }
        return item;
    };

    const _subSort = (item, children) => {
        if (children instanceof Promise) {
            return children.then(r => _subSort(item, r));
        }
        if (Array.isArray(children) && children.length > 0) {
            return _assignSorted(item, _sortTree(children, compare, getSubItems, setSubItems));
        }
        return item;
    };

    // Sort descendants of top-level elements (recursively)
    const promises = items.reduce((acc, item) => {
        const r = _subSort(item, getSubItems(item));
        if (r instanceof Promise) {
            acc.push(r);
        }
        return acc;
    }, []);

    // Only return a promise if needed - otherwise return result instantly
    return promises.length === 0 ? items.sort(compare) : Promise.all(promises).then(() => items.sort(compare));
}

function _sortByGrid(grid) {
    const {data, view} = grid;
    if (!data || !data.length || !view || !view.columns) {
        return null;
    }

    const subItems = grid.subItems || data.subItems;
    const length = data.length;
    const items = [];

    for (let i = 0; i < length; i++) {
        items.push(PTCS.clone(data.baseItem(i)));
    }

    if (!data.sort) {
        return items;
    }

    if (typeof subItems === 'string') {
        return _sortTree(items, data.sort, item => item[subItems], NOP);
    }

    if (typeof subItems === 'function') {
        items.$subItems = new Map();

        return _sortTree(items, data.sort, subItems, (item, sub) => items.$subItems.set(item, sub));
    }

    return items.sort(data.sort);
}

// config = {
//    value?:    field name or function item => value. Default item => item
//    compare?:  compare function (a, b) => integer. Default: use baseType, with fallback: (a, b) => 0;
//    baseType?: baseType. Defines sort order if compare is unassigned
//    sortOrder: 'asc' || 'desc'
// };
export function sortFunction(config) {
    if (!Array.isArray(config)) {
        return null;
    }

    const accessor = value => {
        switch (typeof value) {
            case 'string':
                return item => item[value];

            case 'function':
                return value;
        }
        return item => item;
    };

    const acompare = config.reduce((acc, col) => {
        const sortOrder = col.sortOrder || 'asc';
        if (sortOrder === 'asc' || sortOrder === 'desc') {
            const value = accessor(col.value);
            const compare = col.compare || baseTypeComparator(col.baseType);
            if (compare) {
                acc.push(sortOrder === 'asc' ? (a, b) => compare(value(a), value(b)) : (a, b) => compare(value(b), value(a)));
            }
        }
        return acc;
    }, []);

    switch (acompare.length) {
        case 0:
            return () => 0; // Can't compare items.

        case 1:
            return acompare[0]; // Only one comparator
    }

    // Several comparators
    return (a, b) => {
        for (let i = 0; i < acompare.length; i++) {
            const c = acompare[i](a, b);
            if (c) {
                return c;
            }
        }
        return 0;
    };
}

// Extract sort order from view configurator or column specification, with optional sortOrder
export function sortConfig(columns, sortExpr) {
    const _columns = !columns || (Array.isArray(columns) && columns) || columns.columns || columns.columnsDef;
    if (!Array.isArray(_columns)) {
        return undefined; // Cannot find any sorting columns
    }

    const short = e => e && e.short; // optional chaining not yet available

    // Is there a specified sort order?
    const _sortExpr = sortExpr || (typeof columns.getSortExpression === 'function' && short(columns.getSortExpression()));
    const sorting = PTCS.decodeViewExpr(_sortExpr).reduce((acc, [name, sortOrder]) => {
        const col = name && (sortOrder === 'asc' || sortOrder === 'desc') &&
            (_columns.find(c => c.$sortName === name) || _columns.find(c => (c.name || c.title || c.label) === name));
        if (col) {
            acc.push({value: col.value, compare: col.compare || baseTypeComparator(col.baseType), baseType: col.baseType, sortOrder});
        }
        return acc;
    }, []);

    return (sorting.length > 0 && sorting) || _columns.reduce((acc, col) => {
        if (col.sortable && col.sortOrder) {
            acc.push({value: col.value, compare: col.compare || baseTypeComparator(col.baseType), baseType: col.baseType, sortOrder: col.sortOrder});
        }
        return acc;
    }, []);
}


// sort(grid) or sort(items, compare, getSubItems?, setSumItems?)
// - compare = function || config
// - getSubItems = fieldName || function
// - setSubItems = function
export function sort() {
    if (arguments.length === 1 && arguments[0] instanceof Element && arguments[0].tagName === 'PTCS-GRID') {
        // Retrive and sort the data as specified in the grid
        return _sortByGrid(arguments[0]);
    }

    const [items, compare, subItems, assignSubItems] = arguments;

    if (!Array.isArray(items)) {
        throw Error('Invalid arguments: first argument is not an array');
    }

    const _compare = (typeof compare === 'function' && compare) || sortFunction(compare);

    if (typeof _compare !== 'function') {
        throw Error('Invalid arguments: second argument is not a compare function nor compare configuration');
    }

    if (typeof subItems === 'string') {
        return _sortTree(items, _compare, item => item[subItems], assignSubItems || NOP);
    }

    if (typeof subItems === 'function') {
        return _sortTree(items, _compare, subItems, assignSubItems || NOP);
    }

    return items.sort(_compare);
}
