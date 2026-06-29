import {LitElement} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import {closeTooltip} from 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';
import {AnimScroller} from './anim-scroller.js';
import {KineticScroller} from './kinetic-scroller.js';

// TODO: Optimize number of calls to _placeItems

const itemHeight = el => {
    // itemHeight need to use sub-pixel precision (el.offsetHeight is not good enough)
    const bb = el.getBoundingClientRect();
    return bb.bottom - bb.top;
};

// A virtual scroller
PTCS.VScroller2 = class extends PTCS.BehaviorFocus(L2Pw(LitElement)) {
    static get is() {
        return 'ptcs-v-scroller2';
    }

    static get properties() {
        return {
            // Number of items as assigned by the client
            numItems: {
                type:        Number,
                observer:    '_change',
                observeWhen: 'immediate'
            },

            // Client supplied function for creating elements from items
            createItemElement: {
                type:        Function, // args: (index, el-reuse?)
                value:       () => PTCS.VScroller2._dfltCreateItemElement,
                observer:    '_change',
                observeWhen: 'immediate'
            },

            // Client supplied function for recycling item elements
            // $NUP recycleItemElement: {
            //    type: Function // args: (el)
            // },

            // Client supplied function for picking element to reuse
            // $NUP pickReuseItemElement: {
            //    type: Function // args: (listOfItemElements, index)
            // },

            // Client supplied function for recycling item elements
            // $NUP removeItemElement: {
            //    type: Function // args: (el)
            // },

            // Total number of items
            // $NUP _numItems: {
            //    type:  Number,
            //    value: 0
            // },

            // Index to first loaded item
            // $NUP _startIx: {
            //    type:  Number,
            //    value: 0
            // },

            // Index to last loaded item + 1 (if _startIx === _endIx no items are loaded)
            // $NUP _endIx: {
            //    type:  Number,
            //    value: 0
            // },

            // Height of loaded items that are above the viewport
            // $NUP _aboveH: {
            //    type:  Number,
            //    value: 0
            // },

            // $NUP Adapt if scroller area changes
            // _resizeObserver: ResizeObserver,

            // Check if item areas changes
            // $NUP _resizeItemObserver: ResizeObserver,

            // Container that represent the full height of all items
            // $NUP _elSpace: Element,

            // Container that scrolls _elSpace
            // $NUP _elScroll: Element,

            // Container for _elItems
            // $NUP _elCntr: Element,

            // Container for the loaded items
            // $NUP _elItems: Element,

            // Index of item with keyboard focus
            // $NUP focusedItemIndex: {
            //    type:  Number,
            //    value: 0
            // },

            // Should the focus "wrap", e.g. should 'ArrowUp' from the first item navigate to the
            // last item and 'ArrowDown' from the last item move focus to the top?
            // $NUP wrapFocus: {
            //     type: Boolean
            // }
        };
    }

    static _dfltCreateItemElement(index, el) {
        if (!el) {
            el = document.createElement('div');
        }
        el.textContent = `ITEM ${index}`;
        return el;
    }

    constructor(...arg) {
        super(arg);
        this.__old = {}; // Keep track of old scroll values
        this.pickReuseItemElement = reuse => reuse.pop();
        this.style.display = 'block';
        this.style.position = 'relative';
        this.style.overflow = 'clip';
        this._elSpace = document.createElement('div');
        this._elScroll = document.createElement('div');
        this._elScroll.style.outline = 'none'; // No UA focus
        this._elScroll.style.position = 'absolute';
        this._elScroll.style.overflow = 'auto';
        this._elScroll.style.width = '100%';
        this._elScroll.style.height = '100%';
        this._elScroll.appendChild(this._elSpace);
        this._elCntr = document.createElement('div');
        this._elCntr.style.outline = 'none'; // No UA focus
        this._elCntr.style.overflow = 'clip';
        this._elCntr.style.position = 'absolute';
        this._elCntr.style.left = '0';
        this._elCntr.style.top = '0';
        this._elCntr.style.right = '0';
        this._elCntr.style.bottom = '0';
        this._elCntr.style.display = 'flex';
        this._elCntr.style.alignItems = 'stretch';
        this._elCntr.style.justifyContent = 'space-between';
        this._elItems = document.createElement('div');
        this._elItems.style.outline = 'none'; // No UA focus
        this._elItems.style.flex = '1 1 auto';
        this._elCntr.appendChild(this._elItems);
        this._items = []; // The loaded elements
        this._itemsH = 0; // Height of all elements in _items
        this._height = 0;
        this._sbWidth = 0;
        this._sbHeight = 0;
        this._animating$ = 0;
        this._contentAlignment = 'top';

        // Total number of items
        this._numItems = 0;

        // Index to first loaded item
        this._startIx = 0;

        // Index to last loaded item + 1 (if _startIx === _endIx no items are loaded)
        this._endIx = 0;

        // Height of loaded items that are above the viewport
        this._aboveH = 0;

        // Index of item with keyboard focus
        this.focusedItemIndex = 0;

        // Attach scroll and container elements
        this.appendChild(this._elScroll);
        this.appendChild(this._elCntr);

        this._resizeObserver = new ResizeObserver(this.resized.bind(this));
        this._resizeItemObserver = new ResizeObserver(this._resizeItemEv.bind(this));
        this._kineticScroller = new KineticScroller(this._kineticScroll.bind(this));
    }

    // The virtual scroller doesn't use a shadow dom
    createRenderRoot() {
        return this;
    }

    ready() {
        super.ready();
        this._elScroll.addEventListener('mousedown', this._onmousedown.bind(this));
        this._elScroll.addEventListener('scroll', this._onscroll.bind(this));
        this.addEventListener('wheel', this._wheel.bind(this));
        this.addEventListener('mousedown', this._mouseDown.bind(this));
        this.addEventListener('keydown', this._keyDown.bind(this));
        this._elCntr.addEventListener('touchstart', this._kineticScroller.touchstart);

        if (PTCS.getVerticalScrollbarWidth() === 0) {
            this.enableHackForFloatingScrollbars();
        }
    }

    get viewportWidth() {
        return this._elScroll.clientWidth;
    }

    get viewportHeight() {
        return this._elScroll.clientHeight;
    }

    get startIx() {
        return this._startIx;
    }

    get endIx() {
        return this._endIx;
    }

    get firstVisibleIx() {
        let y = this._aboveH;
        for (let i = 0; i < this._items.length; i++) {
            y += this._items[i].h;
            if (y > 0) {
                return i + this._startIx;
            }
        }
        // Should never be reached...
        return this._startIx;
    }

    get lastVisibleIx() {
        const h = this.viewportHeight;
        let y = this._aboveH + this._itemsH;
        for (let i = this._items.length - 1; i >= 0; i--) {
            y -= this._items[i].h;
            if (y < h) {
                return i + this._startIx;
            }
        }
        // Should never be reached...
        return this._endIx;
    }


    // Get quick access to loaded elements
    get itemElements() {
        return this._items.map(item => item.el);
    }

    get contentAlignment() {
        return this._contentAlignment;
    }

    set contentAlignment(_contentAlignment) {
        const value = {center: 'center', bottom: 'bottom'}[_contentAlignment] || 'top';
        if (value === this._contentAlignment) {
            return;
        }
        this._contentAlignment = value;
        if (Array.isArray(this._items)) {
            this._placeItems();
        }
    }

    getRow(index) {
        return (this._startIx <= index && index < this._endIx) ? this._items[index - this._startIx].el : null;
    }

    getFocusRow() {
        return this.getRow(this.focusedItemIndex);
    }

    setFocusRowIndex(index) {
        // Negative index goes backwards
        const fi = index < 0 ? Math.max(this._numItems + index, 0) : Math.min(Math.max(index, 0), this._numItems - 1);
        if (fi !== this.focusedItemIndex) {
            this.focusedItemIndex = fi;
            this.scrollTo(fi);
            this.dispatchEvent(new CustomEvent('focused-item-updated', {detail: {value: fi}}));
        }
    }

    getElItemsTransform() {
        // Get the transform of the element which contains the scroller items
        return this._elItems.style.transform;
    }

    get hasScrollbar() {
        return this._numItems > 0 && (this._startIx > 0 || this._endIx < this._numItems || Math.floor(this._itemsH) > this.viewportHeight);
    }

    get elScroll() {
        return this._elScroll;
    }

    _initTrackFocus() {
        this._trackFocus(this, () => this.getFocusRow() || this);
    }

    connectedCallback() {
        super.connectedCallback();
        this._resizeObserver.observe(this._elSpace); // For width
        this._resizeObserver.observe(this); // For height
        this._items.forEach(item => this._resizeItemObserver.observe(item.el));

        // TW-109993 fix
        if (this.__old && this._elScroll) {
            // May need to restore scroll position - but first has to wait for _elScroll to be reconnected, or it doesn't have any dimensions
            requestAnimationFrame(() => {
                // eslint-disable-next-line max-len
                if (this.__old.scrollTop !== this._elScroll.scrollTop && this.__old.clientHeight === this._elScroll.clientHeight && this.__old.scrollHeight === this._elScroll.scrollHeight) {
                    this._elScroll.scrollTop = this.__old.scrollTop;
                }
            });
        }
    }

    disconnectedCallback() {
        this._resizeObserver.unobserve(this._elSpace);
        this._resizeObserver.unobserve(this);
        this._items.forEach(item => this._resizeItemObserver.unobserve(item.el));
        super.disconnectedCallback();
    }

    // numItems or createItemElement has changed
    _change() {
        const {numItems} = this;
        this._numItems = numItems || 0;

        // Empty or has all loaded items has been lost?
        if (this._endIx === 0 || this._startIx >= numItems) {
            // Reload all
            this._load();
        } else {
            // Remove truncated items
            const reuse = [];
            while (this._endIx > numItems) {
                reuse.push(this._deleteItem(this._items.length - 1));
                this._endIx--;
            }
            this._fill(reuse);
        }

        this._setGap();

        if (this.focusedItemIndex >= numItems) {
            this.setFocusRowIndex(-1); // Focus on last item
        }

        // Make sure the grid height will shrink if the data shrinks...
        this._setGap();
    }

    _initItem(el) {
        el.style.position = 'absolute';
        el.style.left = '0';
        el.style.minWidth = '100%';
        this._elItems.appendChild(el);
        this._resizeItemObserver.observe(el);
        return itemHeight(el);
    }

    _addItem(index, reuse, prepend) {
        const elOrg = reuse && this.pickReuseItemElement(reuse, index);
        const el = this.createItemElement(index, elOrg);
        let h;

        if (elOrg) {
            if (el === elOrg) {
                h = itemHeight(el); // Same element, different height?

                // This should NOT be needed! (But it is, in Navigate, for unknown reasons...)
                this._resizeItemObserver.unobserve(el);
                this._resizeItemObserver.observe(el);
            } else {
                reuse.unshift(elOrg); // Didn't use the suggested element
                h = this._initItem(el); // New element
            }
        } else {
            h = this._initItem(el); // New element
        }

        // console.assert(h > 0);

        if (prepend) {
            this._items.unshift({el, h});
        } else {
            this._items.push({el, h});
        }
        this._itemsH += h;

        return h;
    }

    _updateItem(index) {
        console.assert(this._startIx <= index && index < this._endIx, `updateIndex: startIx: ${this._startIx} ix: ${index}, endIx: ${this._endIx}`);

        const item = this._items[index - this._startIx];
        const el = this.createItemElement(index, item.el);

        let h;
        if (el !== item.el) {
            this._recycle(item.el);
            item.el = el;
            h = this._initItem(el);
        } else {
            // TW-117023: Same fix as in _addItem (see PR for TW-93494, also for a Navigate ticket)
            this._resizeItemObserver.unobserve(el);
            this._resizeItemObserver.observe(el);
            h = itemHeight(el);
        }

        if (h === 0 && !(this.clientHeight > 0)) {
            // Item got zero height (probably) because the scroller is hidden. Wait for resize
            return;
        }

        if (h !== item.h) {
            this._itemsH += (h - item.h);
            item.h = h;

            // Remember that the heights have changed. Must _placeItems()
            this._updatedItemH = true;
        }
    }

    // Returns the deleted element for reuse, still attached to DOM for optimization
    _deleteItem(index) {
        console.assert(0 <= index && index < this._items.length, `index: ${index} length: ${this._items.length}`);
        const item = this._items[index];
        this._items.splice(index, 1);
        this._itemsH -= item.h;
        return item.el;
    }

    // Remove all loaded items
    _clearItems() {
        const reuse = this._items.map(item => item.el);
        this._items = [];
        this._itemsH = 0;
        return reuse;
    }

    // Detach element(s) from DOM and recycle it
    _recycle(el) {
        if (el instanceof Array) {
            el.forEach(el2 => this._recycle(el2));
        } else if (el) {
            this._resizeItemObserver.unobserve(el);
            this._elItems.removeChild(el);
            if (this.recycleItemElement) {
                this.recycleItemElement(el);
            }
        }
    }

    // Approximate the current scroll height based on height of the loaded data
    _setScrollHeight() {
        if (this._itemsH && this._endIx - this._startIx < this._numItems) {
            this._elSpace.style.height = `${Math.floor(Math.min(5500000, this._numItems * this._itemsH / (this._endIx - this._startIx)))}px`;
        } else {
            this._elSpace.style.height = `${Math.floor(this._itemsH)}px`;
        }
    }

    // Map (relative) point in item to viewport y-coordinate based on the scrollbar values
    _getScrollTarget(scrollTop, clientHeight, scrollHeight) {
        if (!(this._numItems >= 0) || isNaN(scrollTop) || scrollHeight <= clientHeight) {
            return {ix: 0, ixOffs: 0, y: 0}; // No scrollbars, so put the topmost element at the top
        }

        const fx = (this._numItems * scrollTop) / (scrollHeight - clientHeight);
        const ix = Math.min(Math.floor(fx), this._numItems - 1); // At the absolute bottom we go one item too far

        return {
            ix,
            // 0 .. 1: 0 = top-most pixel-row. 1 = bottom-most pixel-row
            ixOffs: fx - ix,
            // Where the ix pixel-row should be placed on the viewport
            y:      clientHeight * (fx / this._numItems)
        };
    }

    // Adapt scrollbar so it matches the loaded data
    _setScrollPos(force) {
        if (this.clientHeight === 0 || this.clientWidth === 0) {
            return; // Not visible. Do nothing
        }

        if (!this.hasScrollbar) {
            // No scrollbar, no scrolltop
            this._setAboveH(0);
            this._elScroll.scrollTop = 0;
            this._elSpace.style.height = `${Math.floor(this._itemsH)}px`;
            return;
        }

        if (this.__blockScrollPos && !force) {
            return;
        }

        if (this._startIx === 0 && this._endIx === this._numItems) {
            // Everything is loaded. No estimations needed
            this._elSpace.style.height = `${Math.floor(this._itemsH)}px`;
            this._elScroll.scrollTop = Math.round(-this._elScroll.scrollHeight * (this._aboveH / this._itemsH));
            return;
        }

        const clientHeight = this.viewportHeight;

        // Best estimate for the scrollHeight
        const scrollHeight = Math.floor(Math.min(5500000, this._numItems * this._itemsH / (this._endIx - this._startIx)));
        if (scrollHeight !== this._elScroll.scrollHeight) {
            this._elSpace.style.height = `${scrollHeight}px`;
        }

        // If we are at the top, stay at the top
        if (this._startIx === 0 && this._aboveH === 0) {
            this._elScroll.scrollTop = 0;
            return;
        }

        // What is the actual y-value of (ix, ixOffs)?
        const y2 = (ix, ixOffs) => {
            let y = this._aboveH;
            if (ix < this._startIx) {
                return y - 1; // Before loaded window
            }
            for (let i = this._startIx; i < this._endIx; i++) {
                const itemH = this._items[i - this._startIx].h;
                if (i === ix) {
                    return y + itemH * ixOffs;
                }
                y += itemH;
            }
            return y; // After loaded window
        };

        // What is the difference between the current viewport and scrollTop === top?
        const err = top => {
            const st = this._getScrollTarget(top, clientHeight, scrollHeight);
            return st.y - y2(st.ix, st.ixOffs);
        };

        // Binary search for a scrollTop that corresponds to the current viewport
        let a = (this._startIx / this._numItems) * (scrollHeight - clientHeight);
        let b = (this.endIx / this._numItems) * (scrollHeight - clientHeight);

        for (let i = 0; i < 32; i++) { // 32 iterations must be enough
            const m = (a + b) / 2;
            const e = err(m);

            if (Math.abs(e) < 0.005) {
                break; // Found good enough value
            }
            if (e > 0) {
                a = m;
            } else {
                b = m;
            }
        }

        this._elScroll.scrollTop = Math.round((a + b) / 2);
    }

    // Set the scroll adjustment for the loaded items
    _setAboveH(aboveH) {
        this._aboveH = aboveH;
        if (this._scrollLeft > 0) {
            // Horizontal scrolling too
            this._elItems.style.transform = `translate(${-this._scrollLeft}px, ${this._aboveH}px)`;
        } else {
            this._elItems.style.transform = `translateY(${this._aboveH}px)`;
        }
    }

    // Generate an event that tells the client that the items might have been repositioned
    _fireRepainted() {
        if (!this.__repaintEvent) {
            this.__repaintEvent = true;
            requestAnimationFrame(() => {
                this.__repaintEvent = undefined;
                this.dispatchEvent(new CustomEvent('repainted'));
            });
        }
    }

    // Put the loaded items at their proper places
    _placeItems() {
        // Set position of items
        let y = 0;

        // Adjust for contentAlignment
        if (this._aboveH === 0 && this._contentAlignment !== 'top' && !this.hasScrollbar) {
            const dh = this.viewportHeight - this._itemsH;
            if (dh > 0) {
                switch (this._contentAlignment) {
                    case 'center': y = dh / 2; break;
                    case 'bottom': y = dh; break;
                }
            }
        }

        this._items.forEach(item => {
            item.el.style.transform = `translateY(${y}px)`;
            y += item.h;
        });

        // Rows have been put at their places
        this._updatedItemH = undefined;

        // Generate an event that tells the client that the items might have been repositioned
        this._fireRepainted();
    }

    // Throw out all loaded content and reload the page
    _load(wipe) {
        const reuse = this._clearItems();

        if (wipe) {
            // Prevent previous elements from being recycled
            reuse.forEach(el => {
                this._resizeItemObserver.unobserve(el);
                this._elItems.removeChild(el);
                if (this.removeItemElement) {
                    this.removeItemElement(el);
                }
            });
            reuse.length = 0;
        }

        if (!this._numItems) {
            this._recycle(reuse);
            this._setAboveH(0);
            this._startIx = this._endIx = 0;
            this._setScrollHeight();
            return;
        }

        // Adjust current location
        if (this._startIx >= this._numItems) {
            this._startIx = this._numItems - 1;
        }
        this._endIx = this._startIx;

        this._fill(reuse);
    }

    // Is any items representation on the scrollbar higher than the item itself? If so, keep loading...
    _mustLoadMore() {
        // Added support for loadAll, which allows the client to fill the scroller with all items (= a misuse of the virtual scroller)
        const sbH = this.viewportHeight / this._numItems; // Height for an item on scrollbar
        return (this._startIx > 0 || this._endIx < this._numItems) && (this.loadAll || this._items.some(item => item.h < sbH && item.h > 0));
    }

    // Eliminate all blank areas on the viewport, if possible
    // Keep the current scroll position, if possible
    _fill(reuse, dirty) {
        this._height = this.clientHeight; // This is what _fill last operated on

        const height = this.viewportHeight - this._aboveH;

        // Fill downwards
        while (height > this._itemsH && this._endIx < this._numItems) {
            dirty = true;
            this._addItem(this._endIx++, reuse);
        }

        // Fill upwards
        let dAbove = 0;
        while (height > this._itemsH && this._startIx > 0) {
            dirty = true;
            this._addItem(--this._startIx, reuse, true);

            if (height < this._itemsH) {
                // The last item closed the gap above the loaded items. Now try to keep scrollbar at same place.
                // Adjust aboveH so that only the part of the item that filled the gap is visible. The rest should be placed above the viewport.
                dAbove = height - this._itemsH;
            }
        }

        // Load items until the scroller estimation becomes viable
        while (this._mustLoadMore()) {
            // Expand view
            if (this._startIx > 0) {
                this._addItem(--this._startIx, reuse, true);
            }
            if (this._endIx < this._numItems) {
                this._addItem(this._endIx++, reuse);
            }
        }

        // Recycle any remaining elements
        this._recycle(reuse);

        if (dirty) {
            if (dAbove) {
                this._setAboveH(this._aboveH + dAbove);
            }
            this._placeItems();
        }

        this._setScrollPos();
    }

    // Allow client to adjust scroller with pixel values
    scrollDY(dy) {
        if (Math.abs(dy) < this.viewportHeight) {
            this._move(dy, []);
            this._setScrollPos();
        }
    }

    // Move item[ix] dy pixels
    _move(dy, reuse, recursive) {
        console.assert(this._endIx <= this._numItems, `_endIx: ${this._endIx} numItems: ${this._numItems}`);
        let aboveH = this._aboveH + dy;
        let belowH = this.viewportHeight - aboveH - this._itemsH;

        // Recycle out-of-sight items above viewport
        while (this._items.length && -aboveH > this._items[0].h && !this._mustLoadMore()) {
            aboveH += this._items[0].h;
            reuse.push(this._deleteItem(0));
            this._startIx++;
        }

        // Recycle out-of-sight items below viewport
        while (this._items.length && -belowH > this._items[this._items.length - 1].h && !this._mustLoadMore()) {
            belowH += this._items[this._items.length - 1].h;
            reuse.push(this._deleteItem(this._items.length - 1));
            this._endIx--;
        }

        // Fill items to top of viewport
        while (this._startIx > 0 && aboveH > 0) {
            aboveH -= this._addItem(--this._startIx, reuse, true);
        }

        // Fill items to bottom of viewport
        while (this._endIx < this._numItems && belowH > 0) {
            belowH -= this._addItem(this._endIx++, reuse);
        }

        // Load items until the scroller estimation becomes viable
        while (this._mustLoadMore()) {
            // Expand view
            if (this._startIx > 0) {
                aboveH -= this._addItem(--this._startIx, reuse, true);
            }
            if (this._endIx < this._numItems) {
                belowH -= this._addItem(this._endIx++, reuse);
            }
        }

        // Put items at their proper place
        this._placeItems();

        if (this._startIx === 0 && aboveH > 0) {
            // Reached the first item and found white space above it
            this._setAboveH(0);
            this._fill(reuse);
        } else if (this._endIx === this._numItems && belowH > 0.5 && this._startIx > 0 && !recursive) {
            // Reached the last item and found white space below it
            this._setAboveH(aboveH);
            this._move(belowH, reuse, true);
        } else {
            // Cleanup and fixup
            this._recycle(reuse);

            this._setAboveH(aboveH);
        }
    }

    // Place the loaded content according to the scrollbar
    // Aproach:
    // - compute a relative offset [pos] for the current scroll position - a value between 0..1
    // - select the item that occupies this slot [Math.floor(ix = this._numItems * pos)]
    // - find the pixel-row on the selected item that corresponds to pos (pos slides over the height as it grows...)
    // - find the pixel-row in the viewport that corresponds to pos
    // - align the pixel-rows
    _scroll() {
        console.assert(this._endIx <= this._numItems);
        const sb = this.__old;
        const scrollTop = this._elScroll.scrollTop;
        const clientHeight = this.viewportHeight;
        const scrollHeight = this._elScroll.scrollHeight;
        const clientWidth = this.viewportWidth;

        if (clientHeight === 0 || clientWidth === 0) {
            return;
        }

        if (sb.scrollTop !== scrollTop || sb.clientHeight !== clientHeight || sb.scrollHeight !== scrollHeight) {
            Object.assign(sb, {scrollTop, clientHeight, scrollHeight});
            this._vscroll(scrollTop, clientHeight, scrollHeight);
        }

        const scrollLeft = this._elScroll.scrollLeft;
        if (sb.scrollLeft !== scrollLeft) {
            sb.scrollLeft = scrollLeft;
            this._hscroll(scrollLeft);
        }
    }

    _vscroll(scrollTop, clientHeight, scrollHeight) {
        if (!this._numItems || scrollHeight <= clientHeight) {
            this._setScrollHeight();
            this._setAboveH(0);
            this._placeItems();
            return;
        }

        if (this._startIx === 0 && this._endIx === this._numItems) {
            // Everything is loaded. No estimations needed
            this._setAboveH(-(scrollTop / scrollHeight) * this._itemsH);
            this._fireRepainted();
            return;
        }

        const s = this._getScrollTarget(scrollTop, clientHeight, scrollHeight);
        let reuse = [];

        // Need to load target item?
        if (s.ix < this._startIx || this._endIx <= s.ix) {
            if (this._startIx === s.ix + 1) {
                this._addItem(--this._startIx, reuse, true);
            } else if (this._endIx === s.ix) {
                this._addItem(this._endIx++, reuse);
            } else {
                // Target node is out-of-scope
                this._setAboveH(0);

                // Add target item
                reuse = this._clearItems();
                this._startIx = this._endIx = s.ix;
                this._addItem(this._endIx++, reuse);
            }
            this._placeItems();
        }

        // The target item is loaded
        const item = this._items[s.ix - this._startIx];
        const bb0 = this.getBoundingClientRect();
        const bb1 = item.el.getBoundingClientRect();
        this._move(s.y - (bb1.top - bb0.top + bb1.height * s.ixOffs), reuse); // Move item target to viewport target
    }

    _hscroll(scrollLeft) {
        if (this._scrollLeft !== scrollLeft) {
            this._scrollLeft = scrollLeft;
            this._setAboveH(this._aboveH);
            this.dispatchEvent(new CustomEvent('scroll-left-changed', {
                bubbles:  true,
                composed: true,
                detail:   {value: scrollLeft}
            }));
        }
    }

    // The gap at the end of the grid: gap > 0 => all items fit
    get gap() {
        return this._$gap;
    }

    set gap(_gap) {
        if (_gap !== this._$gap) {
            this._$gap = _gap;
            // Notification must be instant, or we risk flicker
            this.dispatchEvent(new CustomEvent('gap-changed', {detail: {value: _gap}}));
        }
    }

    _setGap() {
        this.gap = this.hasScrollbar
            ? -1 - this._numItems // Count number items - and one (generates a change event when number of items changes)
            : Math.floor(this.viewportHeight) - Math.floor(this._itemsH) - 1; // Pretend that the gap is negative on perfect match (prevent loops)
    }

    // Public method for forcing gap calculation (probably to avoid flicker)
    setGap() {
        this._setGap();
        return this.gap;
    }

    // Grid has been resized
    resized() {
        const clientWidth = this.clientWidth;
        const clientHeight = this.clientHeight;
        const clientWidthSw = this.viewportWidth;
        const clientHeightSw = this.viewportHeight;

        if (clientWidth === 0 || clientHeight === 0) {
            return; // Not visible. Do nothing
        }

        // eslint-disable-next-line max-len
        if (this._height === clientHeight && this.__old.clientWidth === clientWidth && this.__old.clientHeight === clientHeight && this.__old.clientWidthSw === clientWidthSw && this.__old.clientHeightSw === clientHeightSw) {
            // Nothing has changed, so ignore this event
            return;
        }

        Object.assign(this.__old, {clientWidth, clientHeight, clientWidthSw, clientHeightSw});

        if (this._height !== clientHeight) {
            const growed = this._height < clientHeight;
            this._height = clientHeight;
            if (growed) {
                // Area got higher - might need more items
                this._fill();
            } else {
                // Area (only) got smaller - might need scrollbar
                this._setScrollPos();

                if (this._contentAlignment !== 'top' && this._startIx === 0 && this._endIx === this._numItems) {
                    this._placeItems();
                }
            }
        }

        // Reserve at least 5px for the scrollbar if a scrollbar is active (for Firefoxs new floating scrollbars)
        const sbWidth = Math.max(clientWidth - clientWidthSw, this._elScroll.scrollHeight > clientHeightSw ? 5 : 0);
        const sbHeight = Math.max(clientHeight - clientHeightSw, this._elCntr.scrollWidth > clientWidth ? 5 : 0);

        if (this._sbWidth !== sbWidth || this._sbHeight !== sbHeight) {
            this._sbWidth = sbWidth;
            this._sbHeight = sbHeight;

            // Scrollable area needs to be inside scroll viewport
            this._elCntr.style.right = `${sbWidth}px`;
            this._elCntr.style.bottom = `${sbHeight}px`;
        }

        requestAnimationFrame(() => {
            if (!(this.clientWidth > 0) || !(this.clientHeight > 0)) {
                return; // No longer visible
            }

            // Avoid "ResizeObserver loop limit exceeded"
            this.dispatchEvent(new CustomEvent('resized-width', {
                bubbles:  true,
                composed: true,
                detail:   {width: this.viewportWidth, height: this.viewportHeight, sbWidth: this._sbWidth, sbHeight: this._sbHeight}
            }));

            this._setGap();
        });
    }

    // While dragging the scroll thumb, treat scrollTop and scrollHeight as read-only
    _onmousedown(ev) {
        if (PTCS.wrongMouseButton(ev)) {
            return;
        }
        this.__mouseDown = true;
        document.addEventListener('mouseup', () => {
            if (this.__blockScrollPos) {
                this._setScrollPos(true);
                this.__blockScrollPos = false;
            }
            this.__mouseDown = false;
        }, {once: true});
    }


    // User has scrolled the view
    _onscroll() {
        if (this.__mouseDown) {
            this.__blockScrollPos = true;
        }
        this._scroll();
        requestAnimationFrame(closeTooltip);
    }

    _kineticScroll(deltaX, deltaY, cancelable) {
        const {scrollLeft, scrollTop} = this._elScroll;

        this._elScroll.scrollTop += deltaY;
        this._elScroll.scrollLeft += deltaX;

        const sq = v => v * v;
        // Return actual scrolling distance
        return Math.sqrt(sq(scrollLeft - this._elScroll.scrollLeft) + sq(scrollTop - this._elScroll.scrollTop));
    }

    // Item has been resized
    _resizeItemEv() {
        if (this._animating$) {
            // Ignore event becase animation is playing. Items will be placed when animation ends.
            return;
        }
        if (this.__resizeItemEv_waiting) {
            return;
        }
        if (!(this.viewportHeight > 0) || !(this.viewportWidth > 0)) {
            return; // Not visible
        }

        this.__resizeItemEv_waiting = true;
        requestAnimationFrame(() => {
            this.__resizeItemEv_waiting = undefined;

            if (!(this.viewportHeight > 0) || !(this.viewportWidth > 0)) {
                return; // No longer visible
            }

            let resized = this._updatedItemH; // Is there a resizing debt?
            this._updatedItemH = undefined;

            this._items.forEach(item => {
                const h = itemHeight(item.el);
                if (h !== item.h) {
                    this._itemsH += h - item.h;
                    item.h = h;
                    resized = true;
                }
            });

            if (resized) {
                const clientHeight = this.viewportHeight;

                if (!(clientHeight > 0)) {
                    // scroller appears to have become be hidden since the requestAnimationFrame...
                    return; // Not returning here can be _very_ costly for huge grids
                }

                if (clientHeight - this._aboveH > this._itemsH && (this._startIx > 0 || this._endIx < this._numItems)) {
                    // Need more items and have more items
                    this._fill(undefined, true);
                } else {
                    // Relayout items
                    this._placeItems();
                    this._setScrollPos();
                }
            }

            this._setGap();

            const w1 = this._items && this._items.reduce((width, item) => Math.max(width, item.el.clientWidth), 0);
            const w2 = this.viewportWidth;
            const w = w1 > w2 ? `${w1}px` : '';
            if (w !== this._elSpace.style.width) {
                this._elSpace.style.width = w;
            }
        });
    }

    // Client request: refresh item(s)
    refresh(item) {
        if (item === undefined) {
            for (let index = this._startIx; index < this._endIx; index++) {
                this._updateItem(index);
            }
            this._fill();
        } else if (typeof item === 'number') {
            const index = Math.floor(item);
            if (this._startIx <= index && index < this._endIx) {
                this._updateItem(index);
            }
        } else {
            console.warn(`Don't know how to refresh ${JSON.stringify(item)}`);
        }
    }

    // Client request: throw out all loaded items and restart
    rebuild(wipe) {
        this._load(wipe);
    }

    scrollRowToBottom(index) {
        if (!(0 <= index && index <= this._numItems)) {
            return;
        }
        if (index < this._startIx || index >= this._endIx) {
            this.scrollTo(index);
        }
        this._scrollToFirstVisibleRow(index);
    }

    scrollRowToTop(index) {
        if (!(0 <= index && index <= this._numItems)) {
            return;
        }
        if (index < this._startIx || index >= this._endIx) {
            this.scrollTo(index);
        }
        this._scrollToLastVisibleRow(index);
    }

    _scrollToFirstVisibleRow(ix) {
        const br1 = this.getBoundingClientRect();
        const br2 = this._items[ix - this._startIx].el.getBoundingClientRect();
        this._move(br1.bottom - br2.bottom, []);
        this._setScrollPos();
    }

    _scrollToLastVisibleRow(ix) {
        const br1 = this.getBoundingClientRect();
        const br2 = this._items[ix - this._startIx].el.getBoundingClientRect();
        this._move(br1.top - br2.top, []);
        this._setScrollPos();
    }

    _findFirstVisibleRowIndex() {
        let startIx = this._startIx;
        const br1 = this.getBoundingClientRect();

        while (startIx < this._endIx - 1) {
            const br2 = this._items[startIx - this._startIx].el.getBoundingClientRect();

            if (br2.bottom - br1.top >= br2.height * 0.75) {
                return startIx;
            }

            startIx++;
        }

        return -1;
    }

    _findLastVisibleRowIndex() {
        let endIx = this._endIx - 1;
        const br1 = this.getBoundingClientRect();

        while (endIx >= this._startIx) {
            const br2 = this._items[endIx - this._startIx].el.getBoundingClientRect();

            if (br1.bottom - br2.top >= br2.height * 0.75) {
                return endIx;
            }

            endIx--;
        }

        return -1;
    }

    // Client request: scroll to specific item
    scrollTo(index) {
        // Viewport height
        const h = this._elScroll.scrollHeight - this._elScroll.clientHeight;

        // Scroll index item into view
        if (this._startIx <= index && index < this._endIx) {
            // Item is already loaded but might be outside of the viewport
            const br1 = this.getBoundingClientRect();
            const br2 = this._items[index - this._startIx].el.getBoundingClientRect();
            if (br1.top > br2.top) {
                this._move(br1.top - br2.top, []);
                this._setScrollPos();
            } else if (br1.bottom < br2.bottom) {
                this._move(br1.bottom - br2.bottom, []);
                this._setScrollPos();
            }
        } else if (0 <= index && index < this._numItems) {
            // Full scroll
            this.__old.scrollTop = -1;
            this._elScroll.scrollTop = ((h + h / this._numItems) * index) / this._numItems;
            this._scroll();
        }
    }

    _animate(anim, dy = 0) {
        this._animating$++; // Start animation

        const finishedCb = () => {
            this._animating$--; // End animation
            this._resizeItemEv(); // Make sure everything is at its proper place
        };

        const recyleEl = this._recycle.bind(this);

        new AnimScroller(anim, {dy, recyleEl, finishedCb});
    }

    // Client message: _inserted = [[$index, $count] ...] - $count items has been removed, starting from $index
    inserted(_inserted) {
        this._numItems = this._numItems + _inserted.reduce((a, v) => a + v[1], 0);
        this.numItems = this._numItems;

        // Adjust _startIx / _endIx for all items that has been inserted before viewport
        let mustUpdate = false;
        _inserted.forEach(([index, count]) => {
            if (index + count <= this._startIx) {
                this._startIx += count;
                this._endIx += count;
                mustUpdate = true;
            } else if (index <= this._endIx) {
                mustUpdate = true;
            }
        });

        if (mustUpdate) {
            this._items.forEach((_, i) => this._updateItem(this._startIx + i));
        }

        this._placeItems();
        this._fill();

        // Collect animated items
        const anim = [];
        for (let ix = this._startIx; ix < this._endIx; ix++) {
            const item = this._items[ix - this._startIx];
            const insert = _inserted.some(([index, count]) => index <= ix && ix < index + count);
            anim.push({el: item.el, h: item.h, state: insert && 'insert'});
        }

        // Start animation when view has stabilized
        if (anim.find(item => item.state)) { // Is there anything to animate?
            requestAnimationFrame(() => this._animate(anim));
        }
    }

    // Client message: _removed = [[$index, $count] ...] - $count items has been removed, starting from $index
    removed(_removed) {
        this._numItems = this._numItems - _removed.reduce((a, v) => a + v[1], 0);
        this.numItems = this._numItems;

        // Adjust _startIx / _endIx for all items that has been removed before viewport
        const adjustIxs = () => {
            _removed.forEach(([index, count]) => {
                if (index + count <= this._startIx) {
                    this._startIx -= count;
                    this._endIx -= count;
                }
            });
            if (this._startIx >= this._numItems) {
                this._startIx = Math.max(0, this._numItems - 1);
            }
            this._items.forEach((_, i) => this._updateItem(this._startIx + i));
        };

        // Are any removed items visible?
        if (_removed.every(([index, count]) => index + count <= this._startIx || index >= this._endIx)) {
            // No visible items where removed. No animation needed
            adjustIxs();
            this._setScrollPos();
            return;
        }

        const aboveH0 = this._aboveH;

        // Collect animated items
        const anim = [];
        for (let ix = this._startIx; ix < this._endIx; ix++) {
            const item = this._items[ix - this._startIx];
            const remove = _removed.some(([index, count]) => index <= ix && ix < index + count);
            anim.push({el: item.el, h: item.h, state: remove && 'remove'});
        }

        anim[0].topmost = true;

        for (let i = anim.length - 1; i >= 0; i--) {
            if (anim[i].state) {
                this._deleteItem(i);
            }
        }

        // Update viewport indexes
        adjustIxs();

        // Adjust endIx
        this._endIx = this._startIx + this._items.length;

        const startIx = this._startIx;
        const endIx = this._endIx;

        this._placeItems();
        this._fill();

        // Add new elements to animation
        for (let i = startIx - 1; i >= this._startIx; i--) {
            const item = this._items[i - this._startIx];
            anim.unshift({el: item.el, h: item.h});
        }
        for (let i = endIx; i < this._endIx; i++) {
            const item = this._items[i - this._startIx];
            anim.push({el: item.el, h: item.h});
        }

        // Start animation when view has stabilized
        requestAnimationFrame(() => this._animate(anim, this._aboveH - aboveH0));
    }

    // Must handle the wheel event, because _elItems covers the scroll window
    _wheel(ev) {
        if (ev.deltaY) {
            const scrollTop = this._elScroll.scrollTop;
            switch (ev.deltaMode) {
                case 0: // pixels
                    this._elScroll.scrollTop += ev.deltaY;
                    break;

                case 1: // lines (rows)
                    if (this._endIx > this._startIx) {
                        this._elScroll.scrollTop += ev.deltaY * this._itemsH / (this._endIx - this._startIx);
                    }
                    break;

                case 2: // pages
                    this._elScroll.scrollTop += ev.deltaY * (0.9 * this._elItems.clientHeight);
                    break;
            }
            if (this._elScroll.scrollTop !== scrollTop) {
                ev.preventDefault();
            }
        }
        if (ev.deltaX) {
            const scrollLeft = this._elScroll.scrollLeft;
            this._elScroll.scrollLeft += ev.deltaX;
            if (this._elScroll.scrollLeft !== scrollLeft) {
                ev.preventDefault();
            }
        }
    }

    _mouseDown(ev) {
        // Adapt focus
        for (let el = ev.target; el; el = el.assignedSlot || el.parentNode) {
            if (el.parentNode === this._elItems) {
                const index = this._items.findIndex(item => item.el === el);
                if (index >= 0) {
                    this.setFocusRowIndex(this._startIx + index);
                }
                return;
            }
        }
    }

    _keyDown(ev, keepTrackOfFocusOnly = false) {
        // Do nothing if the list is disabled or if a sub-element has the actual keyboard focus
        if (this.disabled || (!PTCS.hasFocus(this) && !keepTrackOfFocusOnly) || ev.defaultPrevented) {
            return;
        }

        let fi = this.focusedItemIndex;
        switch (ev.key) {
            case 'ArrowRight':
            case 'ArrowUp':
                if (this.wrapFocus && fi === 0) {
                    fi = Math.max(0, this._numItems - 1);
                } else {
                    fi = Math.max(fi - 1, 0);
                }
                break;
            case 'ArrowLeft':
            case 'ArrowDown':
                if (this.wrapFocus && fi === this._numItems - 1) {
                    fi = 0;
                } else {
                    fi = Math.min(fi + 1, this._numItems - 1);
                }
                break;
            case 'PageUp':
                fi = this._findFirstVisibleRowIndex();

                // Move first visible row to bottom of page
                this._scrollToFirstVisibleRow(fi);
                break;
            case 'Home':
                fi = Math.min(0, this._numItems - 1);
                break;
            case 'PageDown':
                fi = this._findLastVisibleRowIndex();

                // Move last visible row to top of page
                this._scrollToLastVisibleRow(fi);
                break;
            case 'End':
                fi = Math.max(0, this._numItems - 1);
                break;
            case ' ':
            case 'Enter':
                // Click on focused item
                if (this._startIx <= fi && fi < this._endIx) {
                    this._items[fi - this._startIx].el.click();
                } else {
                    this.scrollTo(fi);
                    requestAnimationFrame(() => {
                        if (fi === this.focusedItemIndex && this._startIx <= fi && fi < this._endIx) {
                            this._items[fi - this._startIx].el.click();
                        }
                    });
                }
                break;
            default:
                // Not handled
                return;
        }

        // We consumed this keyboard event. Don't propagate
        ev.preventDefault();

        // Set new focus index, if any
        this.setFocusRowIndex(fi);
    }

    // A hack for Firefoxs new floating scrollbars
    /* istanbul ignore next */
    enableHackForFloatingScrollbars() {
        const mousemove = ev => {
            const bb = this._elScroll.getBoundingClientRect();
            const hoverVsb = bb.right - ev.clientX < 18 && this._elScroll.scrollHeight > this._elScroll.clientHeight;
            const hoverHsb = bb.bottom - ev.clientY < 18 && this._elCntr.scrollWidth > this.clientWidth;
            // Place scroll window over item window if mouse is close to scrollbars, so the scrollbars are fully exposed
            this._elScroll.style.zIndex = (hoverVsb || hoverHsb) ? '10' : '';
        };

        this._elScroll.addEventListener('mousemove', mousemove);
        this._elCntr.addEventListener('mousemove', mousemove);
        this._elScroll.addEventListener('mouseleave', () => {
            this._elScroll.style.zIndex = '';
        });
    }
};

customElements.define(PTCS.VScroller2.is, PTCS.VScroller2);
