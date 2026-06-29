import {LitElement, html, css} from 'lit';
import {when} from 'lit/directives/when.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';
import 'ptcs-checkbox/ptcs-checkbox.js';
import 'ptcs-label/ptcs-label.js';
import 'ptcs-icons/cds-icons.js';

const DEFAULT_OVERFLOW_OFFSET = -16;
const OVERFLOW_BORDER_HEIGHT = 4;
const OVERFLOW_BORDER_WIDTH = 4;


PTCS.MenuItem = class extends PTCS.BehaviorTooltip(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement)))) {
    static get styles() {
        return css`
        :host {
            cursor: pointer;
            display: flex;
            flex-direction: row;
            width: 100%;
            align-items: center;

            box-sizing: border-box;

            white-space: nowrap;
            overflow: hidden;
        }

        :host([hidden]) {
            display: none;
        }

        :host([disabled]) {
            cursor: default;
        }

        :host([no-text]) [part=label] {
            min-width: 0px;
        }

        [part=label] {
            flex-grow: 100;
        }

        [part~=item-value]::part(label) {
            color: inherit;
        }`;
    }

    render() {
        return html`
        ${when(!this._noIcon(this.icon, this.allowMissingIcons, this.displayIcons), () => html`
            <ptcs-icon id="icon" part="icon" icon=${this._getIcon(this.icon, this.allowMissingIcons)}
                size=${this._iconSize(this.iconWidth, this.iconHeight)}
                icon-width=${this.iconWidth} icon-height=${this.iconHeight} aria-hidden="true"></ptcs-icon>`)}
        ${when(
        this.item && this.item.type === 'toggle',
        () => html`<ptcs-checkbox id= "checkbox" part="item-value" ?disabled=${this.disabled}
            ?checked=${this.checked} @checked-changed=${this._checkedChanged} label=${this.text} tooltip=${this.tooltip}
            tooltip-icon=${this.tooltipIcon} no-tabindex></ptcs-checkbox>`,
        () => html`<ptcs-label part="label" id="label" label=${this.text} ?hidden=${this._hideLabel(this.compactMode, this.level)}
            tooltip=${this.tooltip} tooltip-icon=${this.tooltipIcon} disable-tooltip></ptcs-label>`)}
        ${when(!this._hideSubmenuIcon(this.submenu, this.compactMode, this.level), () => html`
            <ptcs-icon part="submenu-icon"
                icon=${this.variant === 'light' ? 'cds:icon_arrow_right' : 'cds:icon_arrow_right_mini'}
                aria-hidden="true"></ptcs-icon>`)}`;
    }

    static get is() {
        return 'ptcs-menu-item';
    }

    static get properties() {
        return {
            item: {
                type:        Object,
                observer:    '_itemChanged',
                observeWhen: 'immediate'
            },

            text: {
                type: String
            },

            icon: {
                type: String
            },

            iconWidth: {
                type:      String,
                attribute: 'icon-width'
            },

            iconHeight: {
                type: String
            },

            allowMissingIcons: {
                type:      Boolean,
                attribute: 'allow-missing-icons'
            },

            displayIcons: {
                type:      Boolean,
                attribute: 'display-icons'
            },

            compactMode: {
                type:      Boolean,
                attribute: 'compact-mode'
            },

            menuMaxWidth: {
                type:      String,
                attribute: 'menu-max-width'
            },

            menuMinWidth: {
                type:      String,
                attribute: 'menu-min-width'
            },

            submenu: {
                type: Array
            },

            maxSubmenuItems: {
                type:      Number,
                attribute: 'max-submenu-items',
            },

            moreItemsIcon: {
                type:      String,
                attribute: 'more-items-icon',
            },

            moreItemsLabel: {
                type:      String,
                attribute: 'more-items-label',
            },

            level: {
                type:      Number,
                attribute: 'level',
                reflect:   true
            },

            selected: {
                type:      Boolean,
                attribute: 'selected',
                reflect:   true
            },

            noText: {
                type:      Boolean,
                computed:  '_noText(text)',
                attribute: 'no-text',
                reflect:   true
            },

            noContent: {
                type:      Boolean,
                computed:  '_noContent(submenu)',
                attribute: 'no-content',
                reflect:   true
            },

            header: {
                type:      Boolean,
                attribute: 'header',
                reflect:   true
            },

            allSiblingsChildless: {
                type:      Boolean,
                attribute: 'all-siblings-childless',
                reflect:   true
            },

            role: {
                type:    String,
                reflect: true
            },

            ariaLabel: {
                type:      String,
                computed:  '_ariaLabel(text)',
                attribute: 'aria-label',
                reflect:   true
            },

            ariaHaspopup: {
                type:      String,
                computed:  '_ariaHaspopup(submenu)',
                attribute: 'aria-haspopup',
                reflect:   true
            },

            ariaExpanded: {
                type:      String,
                computed:  '_ariaExpanded(selected)',
                attribute: 'aria-expanded',
                reflect:   true
            },

            checked: {
                type:      Boolean,
                attribute: 'checked',
                reflect:   true
            },

            _popup: {
                type: Object
            },

            _parent: {
                type: Object
            },

            ignoreClick: {
                type:      Boolean,
                attribute: 'ignore-click',
            },

            disabled: {
                type:    Boolean,
                reflect: true
            },

            variant: {
                type:      String,
                attribute: 'variant',
                reflect:   true
            },

            // Set a class on ptcs-menu-submenu to allow for custom styling of ptcs-menu-button submenus
            menuPopupClass: {
                type:      String,
                attribute: 'menu-popup-class'
            },

            // Temporary property that disables the a-z navigation by default. Currently a-z navigation is enabled only for ptcs menu button.
            __azNavigation: {
                type: Boolean
            }
        };
    }

    constructor() {
        super();
        this.submenu = [];
        this.noText = true;
        this.allSiblingsChildless = false;
        this.role = 'menuitem';
        this.ignoreClick = false;
        this.disabled = false;
        this.variant = 'dark';
        this.__azNavigation = false;
    }

    ready() {
        super.ready();
        this.addEventListener('click', (ev) => {
            this._click(ev);
            ev.preventDefault();
        });
        this.tooltipFunc = this._monitorTooltip;
    }

    connectedCallback() {
        super.connectedCallback();

        // Set the _parent pointer (the submenu or the menu itself)
        this._parent = this._getClosestParentContainer();
    }

    _checkedChanged(ev) {
        if (this.checked !== ev.target.checked) {
            this.checked = ev.target.checked;
            this.item.checked = this.checked;
        }
    }

    _iconSize(iconWidth, iconHeight) {
        if (iconWidth || iconHeight) {
            return 'custom';
        }
        return 'small';
    }

    // refresh states when clicking on checkbox
    _itemChanged() {
        if (this._popup) {
            this._popup.items = this._createSubmenu(this.submenu);
        }
    }

    _getClosestParentContainer() {
        for (let el = this; el; el = el.parentNode) {
            if (el.nodeName === '#document-fragment') {
                return el.host;
            }
        }
        return null;
    }

    _hideLabel(compactMode, level) {
        return compactMode && level === 0;
    }

    _hideSubmenuIcon(submenu, compactMode, level) {
        if (compactMode && level === 0) {
            // New spec---the icon should never be shown when the menu is collapsed
            return true;
        }
        if (Array.isArray(submenu) && submenu.length > 0) {
            return false;
        }
        return true;
    }

    _noText(text) {
        return !text;
    }

    _noContent(submenu) {
        if (Array.isArray(submenu) && submenu.length > 0) {
            return false;
        }
        return true;
    }

    _ariaLabel(text) {
        return text;
    }

    _ariaHaspopup(submenu) {
        return !this._hideSubmenuIcon(submenu, false, 0);
    }

    _ariaExpanded(selected) {
        return selected;
    }

    _getIcon(icon, allowMissingIcons) {
        if (icon) {
            return icon;
        }
        // No icon specified, use default if so configured
        return allowMissingIcons ? icon : 'cds:icon_image';
    }

    _noIcon(icon, allowMissingIcons, displayIcons) {
        if (!displayIcons) {
            // Never display any icons, even if they are specified
            return true;
        }
        if (icon) {
            // Nope, there is an icon
            return false;
        }
        return allowMissingIcons;
    }

    _monitorTooltip() {
        if (this.compactMode && this.level === 0) {
            return this.text;
        }
        // In the "normal" case, the label component handles everything...
        const label = this.item && this.item.type === 'toggle' ? this.shadowRoot.querySelector('ptcs-checkbox').shadowRoot.querySelector('ptcs-label')
            : this.$.label;
        return label.tooltipFunc();
    }

    _containsPoint(xPos, yPos) {
        const _inRect = (x, y, rect) => x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom;
        if (_inRect(xPos, yPos, this.getBoundingClientRect())) {
            // This was a click inside a popup, don't do anything
            return true;
        }
        // If we got here, the point was outside this menu item---but we have to check "our" popups as well
        if (this._popup) {
            return this._popup._containsPoint(xPos, yPos);
        }
        // Nope, not within the scope of this item (or its subitems)
        return false;
    }

    _getMainComponent() {
        let el = this._parent;
        while (el._parent) {
            el = el._parent;
        }
        return el;
    }

    _dispatchClickedEvent() {
        // Find the "main" component and dispatch the event from there...
        this._getMainComponent().dispatchEvent(new CustomEvent(
            'action',
            {
                bubbles:  true,
                composed: true,
                detail:   {
                    item:      this.item,
                    closeMenu: this._needToCloseMenu
                }
            }));
    }

    // The selectionMade parameter is true if we close the "chain" of popups because a proper selection
    // was made (and false if the popup was closed because of a "click outside" or a click on some other item)
    _closeParents(selectionMade = false) {
        // If we have an open popup, remove it from the tree
        if (this._popup) {
            document.body.removeChild(this._popup);
            this._popup = null;
        }

        if (this.level === 0 && this.isWithinFlyout && selectionMade) {
            // The top-level items should now remain selected after a "proper" selection (but remove the flag
            // from any previously selected top level item...)
            if (this._parent._selectedTopLevelItem && this._parent._selectedTopLevelItem !== this) {
                this._parent._selectedTopLevelItem.selected = false;
            }
            this._parent._selectedTopLevelItem = this;
            this._parent._setSelectedRootItemIndex();
        } else {
            // No longer selected
            this.selected = false;
        }

        // Continue upwards in the tree
        this._parent._closeParents(selectionMade);
    }

    get isWithinFlyout() {
        // The feature to keep top-level items selected should ONLY be available in ptcs-menubar, not in the ptcs-menu-button
        return this._parent && this._parent.nodeName === 'PTCS-MENU-FLYOUT';
    }

    // inform if the item is a toggle, do not close the submenu when clicking on it
    get _needToCloseMenu() {
        return this.item.type !== 'toggle';
    }

    _closeChildren() {
        if (this._popup) {
            // Process children first (recursively)...
            this._popup._closeChildren();

            // ...and then remove it from the DOM tree
            document.body.removeChild(this._popup);
            this._popup = null;
        }

        if (!this.isEmptyTopLevelItem) {
            // No longer selected
            this.selected = false;
        }
    }

    get isEmptyTopLevelItem() {
        return this.level === 0 && this._noContent(this.submenu);
    }

    _closeCurrentPopup() {
        this.selected = false;
        this._parent._setSelectedItem(null);
        this._parent.focus();
    }

    _nextLevel(level) {
        return level + 1;
    }

    _getSourceEl(item) {
        if (item && item._parent && item._parent.nodeName === 'PTCS-MENU-SUBMENU') {
            return item._parent;
        }
        return item;
    }

    _getParentRoot() {
        let parent = this._parent;
        while (parent) {
            // search for the root
            if (parent.nodeName.toLowerCase() === 'ptcs-menu-button') {
                return parent;
            }
            parent = parent._parent;
        }
        return null; // No matching menu-button found
    }

    _createPopup() {
        // This was previously done using the createSubComponent, now after the Lit port we do it
        // manually, property-by-property...
        const el = document.createElement('ptcs-menu-submenu');
        el.setAttribute('id', this?.getRootNode()?.host?.getAttribute('id') || 'popup'); // Hack for ThingWorx styling
        el.setAttribute('part', 'popup');
        el.variant = this.variant;
        el.items = this._createSubmenu(this.submenu);
        el.parentItem = this.item;
        el.compactMode = this.compactMode;
        el.iconWidth = this.iconWidth;
        el.iconHeight = this.iconHeight;
        el.menuMaxWidth = this.menuMaxWidth;
        el.menuMinWidth = this.menuMinWidth;
        el.level = this._nextLevel(this.level);
        el.maxSubmenuItems = this.maxSubmenuItems;
        el.allowMissingIcons = this.allowMissingIcons;
        el.moreItemsIcon = this.moreItemsIcon;
        el.moreItemsLabel = this.moreItemsLabel;
        el.displayIcons = this.displayIcons;
        el.__azNavigation = this.__azNavigation;

        // Set custom class value
        if (this.menuPopupClass) {
            el.classList.add(this.menuPopupClass);
            el.menuPopupClass = this.menuPopupClass;
        }

        // Remember that we are the parent of this new popup
        el._parent = this;

        return el;
    }

    _createSubmenu(submenu, numItemsFitInWindow) {
        // Start with any value set in the component props
        let numSubmenuItems = this.maxSubmenuItems;

        // Then make sure that everything fits within the window
        if (typeof numItemsFitInWindow === 'number') {
            numSubmenuItems = Math.min(numSubmenuItems, numItemsFitInWindow);
        }

        // Finally, make sure we display at least two items (one "real" plus the 'More...'), otherwise you will never reach
        // any actual menu items, just an endless chain of 'More...' items
        numSubmenuItems = Math.max(numSubmenuItems, 2);

        if (Array.isArray(submenu) && submenu.length > numSubmenuItems) {
            // Now, we can get to a situation where the input menu *is* already truncated...so in order to have the
            // truncation working properly we have to make the "rest" of the items into one logical array again...
            let lastItemContent = [];
            const lastItem = submenu[submenu.length - 1];

            if (lastItem.label === this.moreItemsLabel && lastItem.icon === this.moreItemsIcon && lastItemContent) {
                // Remember the items in the previous 'More...' menu
                lastItemContent = lastItem.content;

                // Remove the 'More...' item itself from the menu
                submenu = submenu.splice(0, submenu.length - 1);
            }

            // Too many submenu items, move the surplus ones to a 'More...' entry...
            const submenuItems = [...submenu, ...lastItemContent];
            const moreItems = submenuItems.splice(numSubmenuItems > 0 ? numSubmenuItems - 1 : 0);
            const more = {};
            more.icon = this.moreItemsIcon;
            more.label = this.moreItemsLabel;
            more.content = moreItems;
            submenuItems.push(more);
            return submenuItems;
        }

        // Within limits, use original...
        return submenu;
    }

    _positionPopup() {
        const rectItem = this.getBoundingClientRect();

        const widthWindow = window.innerWidth;
        const heightWindow = window.innerHeight;

        const rectPopup = this._popup.getBoundingClientRect();
        const widthPopup = rectPopup.width;

        let posX = rectItem.left + rectItem.width;

        if (this.level === 0) {
            // Here we should SOMEHOW try to compensate the position of the first popup
            // level, it gets too far to the left (unless the menu itself is very far to the left,
            // which it usually is)...
        }

        // In the "overflow" case (where there isn't enough space to the right of this item to add a
        // popup menu), we should add a border and a slight offset to the popup, and this should
        // be done for all "subsequent" popups---so check if "our" popup is in overflow mode...
        let overflowOffset = 0;

        // By default, there should be no "overflow" border in the new popup
        this._popup.overflow = this._parent.overflow || false;

        const leftSidePos = rectItem.left - widthPopup;
        const fitsOnRight = posX + widthPopup <= widthWindow;
        const fitsOnLeft = leftSidePos > 0;

        if (this._parent) {
            if (this._parent.positionSide === 'left' && fitsOnLeft) { // the last submenu was left and there is space to the left
                posX = leftSidePos;
                this._popup.positionSide = 'left';
            } else if (!fitsOnRight && fitsOnLeft) { // the last submenu position was right and there is no space to the right
                posX = leftSidePos;
                this._popup.positionSide = 'left';
            } else if (!fitsOnRight) { // Here it won't fit on either side, so emit the popup aligned to the right window border
                posX = widthWindow - (widthPopup + 1);
                this._popup.positionSide = 'right';
            } else {
                posX = rectItem.right; // the last menu position was left and the is no space to the left
                this._popup.positionSide = 'right';
            }

            // Finally, if the popup has a different positionSide than this submenu, then we have "flipped" directions, and everything
            // from that menu (and "downwards") should be considered "overflowing"...
            if (this._parent.positionSide !== this._popup.positionSide) {
                this._popup.overflow = true;
                overflowOffset = DEFAULT_OVERFLOW_OFFSET;
            }
        }

        let posY = Math.max(0, rectItem.top + overflowOffset);

        // Adjust the vertical position as well
        const numPopupItems = Math.min(this.submenu.length, this.maxSubmenuItems ? this.maxSubmenuItems : this.submenu.length);
        let estimatedPopupHeight = numPopupItems * rectItem.height + 1;

        if (this._popup.overflow) {
            estimatedPopupHeight += OVERFLOW_BORDER_HEIGHT;

            // Depending on if the submenu is restricted by its max/min width or not, we *might* need to compensate for the added border
            const cs = getComputedStyle(this._popup);
            const maxWidthStr = cs.getPropertyValue('max-width');
            const maxWidth = Number(maxWidthStr.substr(0, maxWidthStr.indexOf('px'))) || 0;
            const widthStr = cs.getPropertyValue('width');
            const width = Number(widthStr.substr(0, widthStr.indexOf('px'))) || 0;

            const compensateXPos = width < maxWidth;

            if (compensateXPos && this._popup.positionSide === 'left') {
                const offsetX = Math.min(OVERFLOW_BORDER_WIDTH, maxWidth - width);
                posX = Math.max(posX - offsetX, 0);
            }
        }

        if (posY + estimatedPopupHeight > heightWindow) {
            posY = Math.max(0, heightWindow - estimatedPopupHeight);
        }

        // Set the final position
        this._popup.style.left = `${posX}px`;
        this._popup.style.top = `${posY}px`;
    }

    _keyboardActivate() {
        if (this.item && this.item.type === 'toggle') {
            const checkbox = this.shadowRoot.getElementById('checkbox');
            if (checkbox) {
                checkbox.checked = !checkbox.checked;
            }
        }
    }

    _activate() {
        // Disabled item should not be clickable or accessible through the keyboard
        if (this.disabled) {
            return;
        }

        if (!this._parent._setSelectedItem) {
            // We use a ptcs-menu-item to display the footer, outside of the ptcs-menu-flyout structure...
            this._tooltipClose();
            this._dispatchClickedEvent();
            return;
        }

        // Tell the parent (main component or popup) that this is now the selected item
        this._parent._setSelectedItem(this);

        // In case the current item was showing a tooltip, close it...
        this._tooltipClose();

        if (!this.submenu || this.submenu.length === 0) {
            if (this.level === 0 && this.isWithinFlyout) {
                // If it's the first level item make it selected (TW-88197), after removing any previous top-level selection
                if (this._parent._selectedTopLevelItem && this._parent._selectedTopLevelItem !== this) {
                    this._parent._selectedTopLevelItem.selected = false;
                }

                this._parent._selectedTopLevelItem = this;
                this._parent._setSelectedRootItemIndex();
                this.selected = true;
            } else if (this._needToCloseMenu) {
                // Inform the parent (popup or main component) if it should close the popup chain, and that a "proper"
                // selection has been made
                this._parent._closeParents(true);
            }

            // Here we communicate the menu selection to the world
            this._dispatchClickedEvent();

            // Done
            return;
        }

        // User click in an item---this should now be selected
        this.selected = true;

        // Create the element
        this._popup = this._createPopup();

        // Hide the popup until it is time to do the positioning
        this._popup.setAttribute('hidden', '');

        const parentRoot = this._getParentRoot();
        if (parentRoot) {
            parentRoot.__isOpeningPopup = true;
        }

        // Display the popup
        document.body.appendChild(this._popup);

        this._popup.style.position = 'absolute';
        this._popup.style.boxSizing = 'border-box';


        if (this.menuMaxWidth) {
            const stringValue = this.menuMaxWidth + '';
            if (stringValue.match(/^[0-9]+([,.][0-9]+)?$/)) {
                this._popup.style.maxWidth = this.menuMaxWidth + 'px';
            } else {
                this._popup.style.maxWidth = this.menuMaxWidth;
            }
        } else {
            this._popup.style.removeProperty('max-width');
        }

        if (this.menuMinWidth) {
            const stringValue = this.menuMinWidth + '';
            if (stringValue.match(/^[0-9]+([,.][0-9]+)?$/)) {
                this._popup.style.minWidth = this.menuMinWidth + 'px';
            } else {
                this._popup.style.minWidth = this.menuMinWidth;
            }
        } else {
            this._popup.style.removeProperty('min-width');
        }

        // Position the popup on the window where it is visible (wait a bit to let it "get" the size)
        setTimeout(() => {
            // Make sure it is visible again
            this._popup.removeAttribute('hidden');

            const heightWindow = window.innerHeight;
            const rectPopup = this._popup.getBoundingClientRect();
            const heightPopup = rectPopup.bottom - rectPopup.top;
            const root = this._popup.shadowRoot.getElementById('root');
            const rectRoot = root.getBoundingClientRect();
            const heightRoot = rectRoot.bottom - rectRoot.top;
            const borderH = heightPopup - heightRoot;

            const numItems = root.querySelectorAll('ptcs-menu-item').length;
            const itemHeight = heightRoot / numItems;
            const numItemsFit = Math.floor((heightWindow - borderH) / itemHeight);

            if (heightWindow < (heightPopup + OVERFLOW_BORDER_HEIGHT)) {
                this._popup.items = this._createSubmenu(this.submenu, numItemsFit);
            }

            // Now the "real" size of the meny should be available
            this._positionPopup();

            const mainElt = this._getMainComponent();

            // Set the tabindex to be whatever the "main" component is having
            const tabindex = mainElt.getAttribute('tabindex');

            if (tabindex) {
                this._popup.setAttribute('tabindex', tabindex);
            }

            setTimeout(() => {
                if (parentRoot) {
                    parentRoot.__isOpeningPopup = undefined;
                }
                // Move focus to the new popup
                this._popup.focus();
            }, 50);

        }, 100);
    }

    _click(ev) {
        if (!this.disabled && !this.ignoreClick) {
            // Previously only clicks on the checkbox toggled it---make sure we can toggle it by clicking
            // anywhere inside the item...
            if (this.item && this.item.type === 'toggle') {
                const checkbox = this.shadowRoot.getElementById('checkbox');
                if (checkbox) {
                    const _inRect = (x, y, rect) => x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom;
                    if (!_inRect(ev.clientX, ev.clientY, checkbox.getBoundingClientRect())) {
                        checkbox.checked = !checkbox.checked;
                    }
                }
            }
            this._activate();
        }
    }
};

customElements.define(PTCS.MenuItem.is, PTCS.MenuItem);
