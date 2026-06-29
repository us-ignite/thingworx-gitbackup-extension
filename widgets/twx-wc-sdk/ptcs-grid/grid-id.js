import {PTCS} from 'ptcs-library/library.js';

export function uniqueifyId(id) {
    switch (typeof id) {
        case 'string':
            return id;

        case 'number':
            return `${id}`;

        case 'object':
            if (id instanceof Date) {
                return id.getTime();
            }
            if (id.toJSON) {
                return id.toJSON();
            }
            try {
                return JSON.stringify(id);
            } catch (error) {
                // Something prevented JSON from stringifying this object. Perhaps a circular reference?
                console.warn('Can\'t convert data to JSON:', id);
            }
            return undefined;
    }
    return id;
}


export class GridIdSet {
    constructor(init) {
        this.__set = new Set();
        if (init && init.forEach) {
            init.forEach(id => this.add(id));
        }
    }

    get size() {
        return this.__set.size;
    }

    add(id) {
        const xid = uniqueifyId(id);
        if (xid !== undefined) {
            this.__set.add(xid);
        }
        return this;
    }

    delete(id) {
        return this.__set.delete(uniqueifyId(id));
    }

    has(id) {
        return this.__set.has(uniqueifyId(id));
    }

    clear() {
        this.__set.clear();
    }

    forEach(f) {
        this.__set.forEach(f.bind(this));
    }

    values() {
        return this.__set.values();
    }

    [Symbol.iterator]() {
        return this.values();
    }
}

export class GridIdMap {
    constructor() {
        this.__map = new Map();
    }

    get size() {
        return this.__map.size;
    }

    get(id) {
        return this.__map.get(uniqueifyId(id));
    }

    set(id, value) {
        const xid = uniqueifyId(id);
        if (xid !== undefined) {
            this.__map.set(xid, value);
        }
        return this;
    }

    delete(id) {
        return this.__map.delete(uniqueifyId(id));
    }

    has(id) {
        return this.__map.has(uniqueifyId(id));
    }

    clear() {
        this.__map.clear();
    }
}


// Export to ThingWorx
PTCS.uniqueifyId = uniqueifyId;
PTCS.GridIdSet = GridIdSet;
PTCS.GridIdMap = GridIdMap;
