import {LitElement, html, css} from 'lit';
import {ifDefined} from 'lit/directives/if-defined.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-button/ptcs-button.js';
import 'ptcs-menubar/ptcs-menu-submenu.js';
import {createSubComponent} from 'ptcs-library/create-sub-component.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';
import 'ptcs-icons/cds-icons.js';

const _menuComponentConfig = `<ptcs-menu-submenu part$="menu" style="position:absolute;left:0;top:0;"
level="[[__firstMenuLevel]]" disabled="[[disabled]]" hidden="[[hidden]]" class="[[menuPopupClass]]" menu-popup-class="[[menuPopupClass]]"
display-icons="[[displayIcons]]" allow-missing-icons="[[allowMissingIcons]]" menu-icon-size="[[menuIconSize]]"
variant="[[menuColor]]" menu-max-width="[[_menuMaxWidth]]" menu-min-width="[[_menuMinWidth]]"
max-submenu-items="[[maxMenuItems]]" more-items-icon="[[moreItemsIcon]]" more-items-label="[[moreItemsLabel]]" __az-navigation="[[__azNavigation]]">`;

PTCS.MenuButton = class extends PTCS.BehaviorTabindex(PTCS.BehaviorTooltip(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))))) {
    static get styles() {
        return css`
        :host {
            display: inline-flex;
            justify-content: space-between;
            align-items: stretch;
            outline: none;
        }

        [part=button] {
            flex-grow: 1;
        }`;
    }

    render() {
        return html`<ptcs-button id="button" .disabled=${this.disabled} part="button" tabindex=${ifDefined(this._tabindex)}
            .noTabindex=${this.noTabindex} ?selected=${this._selected}
            .variant=${this.buttonVariant} .size=${this.buttonSize} .icon=${this.icon} .svgIcon=${this.svgIcon} .label=${this.label}
            .disableTooltip=${this.disableTooltip} .tooltip=${this.tooltip} .tooltipIcon=${this.tooltipIcon}
            aria-expanded=${this._mode === 'open'} aria-haspopup=${this.items && this.items.length > 0}
            .contentAlign=${this.contentAlign} .iconPlacement=${this.iconPlacement} .buttonMaxWidth=${this.buttonMaxWidth}
            @click=${this._openEv} @touchstart=${this._openEv} @keydown=${this._keyDown} @mouseover=${this._mouseover}>
        </ptcs-button>`;
    }

    static get is() {
        return 'ptcs-menu-button';
    }

    static get properties() {
        return {
            items: {
                type:        Array,
                value:       () => [],
                observer:    '_itemsChanged',
                observeWhen: 'immediate'
            },

            disabled: {
                type:    Boolean,
                value:   false,
                reflect: true
            },

            offset: {
                type:     Number,
                value:    8,
                observer: '_setMenuPosition'
            },

            _mode: {
                type:  String,
                value: 'closed'
            },

            _position: {
                type:  String,
                value: 'below'
            },

            _selected: {
                type:    Boolean,
                value:   false,
                reflect: true
            },

            openOnHover: {
                type:      Boolean,
                value:     false,
                attribute: 'open-on-hover'
            },

            blockOutsideClick: {
                type:      Boolean,
                value:     false,
                attribute: 'block-outside-click'
            },

            buttonVariant: {
                type:      String,
                value:     'tertiary',
                attribute: 'button-variant'
            },

            buttonSize: {
                type:      String,
                value:     'standard',
                attribute: 'button-size'
            },

            icon: {
                type:  String,
                value: null
            },

            svgIcon: {
                type:      String,
                attribute: 'svg-icon',
                value:     'cds:icon_more_vertical'
            },

            contentAlign: {
                type:      String,
                value:     'center',
                reflect:   true,
                attribute: 'content-align'
            },

            menuPlacement: {
                type:      String,
                value:     'vertical',
                attribute: 'menu-placement'
            },

            buttonMaxWidth: {
                type:      Number,
                attribute: 'button-max-width'
            },

            label: {
                type: String
            },

            iconPlacement: {
                type:      String,
                value:     'right',
                attribute: 'icon-placement'
            },

            allowMissingIcons: {
                type:      Boolean,
                value:     false,
                attribute: 'allow-missing-icons'
            },

            displayIcons: {
                type:      Boolean,
                value:     false,
                attribute: 'display-icons'
            },

            menuMaxWidth: {
                type:      String,
                value:     'auto',
                attribute: 'menu-max-width'
            },

            _menuMaxWidth: {
                type:     String,
                computed: '_computeWidth(menuMaxWidth)'
            },

            menuMinWidth: {
                type:      String,
                value:     'auto',
                attribute: 'menu-min-width'
            },

            _menuMinWidth: {
                type:     String,
                computed: '_computeWidth(menuMinWidth)'
            },

            maxMenuItems: {
                type:      Number,
                value:     5,
                attribute: 'max-menu-items'
            },

            menuColor: {
                type:      String,
                value:     'light',
                reflect:   true,
                attribute: 'menu-color'
            },

            moreItemsIcon: {
                type:      String,
                value:     'cds:icon_more_horizontal',
                attribute: 'more-items-icon'
            },

            moreItemsLabel: {
                type:      String,
                value:     'More...',
                attribute: 'more-items-label'
            },

            // Set a class on ptcs-menu-submenu to allow for custom styling of ptcs-menu-button submenus
            menuPopupClass: {
                type:      String,
                attribute: 'menu-popup-class'
            },

            menuIconSize: {
                type:      Number,
                attribute: 'menu-icon-size'
            },

            __firstMenuLevel: {
                type:     Number,
                value:    1,
                readOnly: true
            },

            // Temporary property that enables the a-z navigation only for ptcs menu button
            __azNavigation: {
                type:     Boolean,
                value:    true,
                readOnly: true
            },

            // Ignore the tabindex provided by _delegate if in toolbar
            __ignoreTabIndex: {
                type: Boolean
            },

            // Focus delegation
            _delegatedFocus: String
        };
    }

    constructor() {
        super();
        this._delegatedFocus = '0'; // Default
        this._windowResizedHandler = this._windowResized.bind(this);
    }

    ready() {
        super.ready();

        this.tooltipFunc = this._monitorTooltip.bind(this);

        this._clickOutsideHandler = ev => {
            if (this._isEventOutside(ev) && this._mode === 'open') {
                if (this.blockOutsideClick) {
                    ev.preventDefault();
                    ev.stopPropagation();
                }

                this.closeMenu();
            }
        };

        this._mouseOutsideHandler = ev => {
            if (this._isEventOutside(ev) && this._mode === 'open') {
                if (this.blockOutsideClick) {
                    ev.preventDefault();
                    ev.stopPropagation();
                }
            }
        };

        this._touchOutsideHandler = ev => {
            if (this._mode === 'open' && this._isEventOutside(ev)) {
                this.closeMenu(); // Silently close menu if user "touched" outside of it
            }
        };

        this._mouseOverHandler = ev => {
            if (this._isEventOutside(ev) && this._mode === 'open') {
                if (this.blockOutsideClick) {
                    ev.preventDefault();
                    ev.stopPropagation();
                }

                this.closeMenu();
            }
        };
    }

    // button should be focusable on toolbar
    get _tabindex() {
        if (this.noTabindex) {
            return undefined; // Client doesn't want the menu button to be focusable
        }
        return this.__ignoreTabIndex ? 0 : this._delegatedFocus;
    }

    connectedCallback() {
        super.connectedCallback();

        // We should close all popups if the window is resized...
        window.addEventListener('resize', this._windowResizedHandler);
    }

    disconnectedCallback() {
        super.disconnectedCallback();

        window.removeEventListener('resize', this._windowResizedHandler);

        this.closeMenu();
    }

    _monitorTooltip() {
        return this.$.button.tooltipFunc();
    }

    // refresh states when clicking on checkbox
    _refreshStates() {
        this._menu.items = this._createMenuItems(this.items);
        this._menu.requestUpdate();
    }

    _itemsChanged() {
        if (this._menu && this._mode === 'open') {
            this._refreshStates();
        }
    }

    _createMenuItems(menu, numItemsFitInWindow) {
        // Start with any value set in the component props
        let numMenuItems = this.maxMenuItems;

        // Then make sure that everything fits within the window
        if (typeof numItemsFitInWindow === 'number') {
            numMenuItems = Math.min(numMenuItems, numItemsFitInWindow);
        }

        // Finally, make sure we display at least two items (one "real" plus the 'More...'), otherwise you will never reach
        // any actual menu items, just an endless chain of 'More...' items
        numMenuItems = Math.max(numMenuItems, 2);

        if (menu.length > numMenuItems) {
            // Now, we can get to a situation where the input menu *is* already truncated...so in order to have the
            // truncation working properly we have to make the "rest" of the items into one logical array again...
            const lastItem = menu[menu.length - 1];
            let lastItemContent = [];

            if (lastItem.label === this.moreItemsLabel && lastItem.icon === this.moreItemsIcon && lastItemContent) {
                // Remember the items in the previous 'More...' menu
                lastItemContent = lastItem.content;

                // Remove the 'More...' item itself from the menu
                menu = menu.splice(0, menu.length - 1);
            }

            // Too many menu items, move the surplus ones to a 'More...' entry...
            const menuItems = [...menu, ...lastItemContent];
            const moreItems = menuItems.splice(numMenuItems > 0 ? numMenuItems - 1 : 0);
            const more = {};
            more.icon = this.moreItemsIcon;
            more.label = this.moreItemsLabel;
            more.content = moreItems;
            menuItems.push(more);
            return menuItems;
        }

        // Within limits, use original...
        return menu;
    }

    closeMenu(closeChildren = true) {
        if (this._mode === 'open') {
            if (closeChildren) {
                // We may want not to close the children in case they are already closed by the internal logic of the submenu
                this._menu._closeChildren();
            }

            document.body.removeChild(this._menu);

            this._mode = 'closed';

            if (this._clickOutsideHandler) {
                document.removeEventListener('click', this._clickOutsideHandler);
            }

            if (this._mouseOutsideHandler) {
                document.removeEventListener('mousedown', this._mouseOutsideHandler);
                document.removeEventListener('mouseup', this._mouseOutsideHandler);
                document.removeEventListener('touchstart', this._touchOutsideHandler);
            }

            if (this._mouseOverHandler) {
                document.removeEventListener('mousemove', this._mouseOverHandler);
            }

            this._selected = false;

            this.disableTooltip = false;

            return;
        }
    }

    _computeWidth(w) {
        return w === 'auto' ? undefined : w;
    }

    // Is called by the sub menu when it's closed
    _closeParents() {
        this.closeMenu(false);

        if (this.hasAttribute('tabindex')) {
            this.$.button.focus();
        }
    }

    _createMenu() {
        if (!this._menu) {
            this._menu = createSubComponent(this, _menuComponentConfig);
            this.setExternalComponentId();
            this._menu._parent = this;
        }
        if (this._menu.className) {
            this._menu.classList.remove(this._menu.className);
        }
        if (this.menuPopupClass) {
            this._menu.classList.add(this.menuPopupClass);
        }
    }

    openMenu(focusOn = 'first', noSelection) {
        if (this.disabled) {
            return;
        }

        if (this._mode === 'open') {
            this.closeMenu();
            return;
        }

        // Is the menu empty or not?
        const hasMenuItems = this.items && this.items.length > 0;

        if (!hasMenuItems) {
            // Empty menu---don't bother displaying anything, generate an event instead...
            this.dispatchEvent(new CustomEvent('button-clicked'));
            return;
        }

        this._createMenu();

        // Hide the popup until it is time to do the positioning
        this._menu.setAttribute('hidden', '');

        this.__isOpeningPopup = true; // Prevent unstoppable resize event to immediately close the popup

        document.body.appendChild(this._menu);

        this._mode = 'open';

        // I'm not able to get the correct visual order of the items using _menuComponentConfig. Something is not refreshed on dom-repeat.
        // For now passing the items this way.
        this._menu.items = this._createMenuItems(this.items);

        // Set the tabindex to be whatever the "main" component is having
        const tabindex = this.getAttribute('tabindex');

        if (tabindex) {
            this._menu.setAttribute('tabindex', tabindex);
        } else {
            this._menu.removeAttribute('tabindex');
        }

        document.addEventListener('click', this._clickOutsideHandler, true);
        document.addEventListener('mousedown', this._mouseOutsideHandler, true);
        document.addEventListener('mouseup', this._mouseOutsideHandler, true);
        document.addEventListener('touchstart', this._touchOutsideHandler, true);

        if (this.openOnHover && noSelection) {
            document.addEventListener('mousemove', this._mouseOverHandler, true);
        }

        this._tooltipClose();
        this.disableTooltip = true;

        // Wait a small timeout for the menu to get the real size
        setTimeout(() => {
            // Make sure it is visible again
            this._menu.removeAttribute('hidden');

            this._setMenuPosition();

            if (this.hasAttribute('tabindex')) {
                setTimeout(() => {
                    this.__isOpeningPopup = undefined;
                    // Move focus to the new popup
                    this._menu.focus();
                }, 50);

                if (focusOn === 'last') {
                    this._menu._focusedItem = this._menu._getMenuItemElements().length - 1;
                }
            }

            if (!noSelection) {
                this._selected = true;
            }
        }, 100);
    }

    _closeCurrentPopup() {
        this._closeParents();
    }

    _setMenuPosition() {
        if (!this._menu) {
            return;
        }

        const r = this.$.button.getBoundingClientRect();

        const widthWindow = window.innerWidth;
        const heightWindow = window.innerHeight;

        const root = this._menu.shadowRoot.getElementById('root');
        const rectRoot = root.getBoundingClientRect();
        const heightRoot = rectRoot.bottom - rectRoot.top;

        const numItems = root.querySelectorAll('ptcs-menu-item').length;
        const itemHeight = heightRoot / numItems;
        const numItemsFit = Math.floor(heightWindow / itemHeight);

        if (heightWindow < heightRoot) {
            this._menu.items = this._createMenuItems(this._menu.items, numItemsFit);
        }

        const rMenu = this._menu.getBoundingClientRect();
        const w = rMenu.width;
        const h = rMenu.height;

        let posX, posY, hOffset = 0, vOffset = 0;

        if (this.menuPlacement === 'horizontal') {
            hOffset = PTCS.cssDecodeSize(this.offset);

            posX = r.left + r.width + hOffset;
            posY = r.top;
        } else {
            vOffset = PTCS.cssDecodeSize(this.offset);

            posX = r.left;
            posY = r.top + r.height + vOffset;
        }

        // Does the menu fit below the button?
        if (posY + h > heightWindow) {
            const topSidePos = (this.menuPlacement === 'horizontal' ? r.bottom : r.top) - h - vOffset;

            if (topSidePos > 0) {
                // Fits to the top
                posY = topSidePos;
                this._position = this.menuPlacement === 'horizontal' ? this._position : 'above';
            } else {
                // Here it won't fit on either side, so emit the popup aligned to the bottom window border,
                // slightly offset horizontally
                posY = heightWindow - (h + 1);
            }
        } else {
            this._position = this.menuPlacement === 'horizontal' ? this._position : 'below';
        }

        // Does the menu fit to the right of the button?
        if (posX + w > widthWindow) {
            const leftSidePos = (this.menuPlacement === 'horizontal' ? r.left : r.right) - w - hOffset;

            if (leftSidePos > 0) {
                // Fits to the left
                this._menu.positionSide = 'left';
                posX = leftSidePos;
                this._position = this.menuPlacement === 'horizontal' ? 'left' : this._position;
            } else {
                // Here it won't fit on either side, so emit the popup aligned to the bottom window border,
                // slightly offset vertically
                posX = widthWindow - (w + 1);
            }
        } else {
            this._position = this.menuPlacement === 'horizontal' ? 'right' : this._position;
        }

        // Make sure we never end up outside of the window
        posX = Math.max(posX, 0);
        posY = Math.max(posY, 0);

        this._menu.style.left = `${posX}px`;
        this._menu.style.top = `${posY}px`;
    }

    // eslint-disable-next-line spaced-comment
    _isEventOutside(/**@type {MouseEvent}*/ ev) {
        if (this._mode === 'closed' || !this._menu) {
            return true;
        }

        const {clientX, clientY} = ev.targetTouches?.[0] || ev;

        // Is event inside the button?
        const inButton = (x, y) => {
            const rect = this.$.button.getBoundingClientRect();
            let left, right, top, bottom;
            ({left, right, top, bottom} = rect);
            const offset = PTCS.cssDecodeSize(this.offset);

            switch (this._position) {
                case 'above':
                    top -= offset;
                    break;
                case 'below':
                    bottom += offset;
                    break;
                case 'left':
                    left -= offset;
                    break;
                case 'right':
                    right += offset;
                    break;
                default:
            }

            return x >= Math.floor(left) && x <= Math.floor(right) && y >= Math.floor(top) && y <= Math.floor(bottom);
        };

        if (inButton(clientX, clientY)) {
            return false;
        }

        // Is event inside the menu? and not a toggle element
        const menuItemElts = this._menu._getMenuItemElements();

        return Array.prototype.reduce.call(menuItemElts, (outside, menuItemElt) => {
            const _containsPoint = menuItemElt._containsPoint(clientX, clientY);
            if (!_containsPoint) {
                return outside && !_containsPoint;
            }
            return outside && !menuItemElt.item.type === 'toggle';
        }, true);
    }

    _windowResized() {
        this.closeMenu();
    }

    _mouseover() {
        if (this.openOnHover && this._mode === 'closed') {
            this.openMenu('first', true);
        }
    }

    _openEv(ev) {
        if (!ev.defaultPrevented) {
            this.openMenu('first');
            ev.preventDefault();
        }
    }

    _keyDown(ev) {
        switch (ev.key) {
            case 'ArrowUp':
                this.openMenu('last');
                ev.preventDefault();
                break;
            case 'ArrowDown':
                this.openMenu('first');
                ev.preventDefault();
                break;
            case 'Enter':
                this.click();
                ev.preventDefault();
                break;
        }
    }

    getExternalComponentId() {
        return this._menuId;
    }

    setExternalComponentId(id) {
        if (id) {
            this._menuId = id;
        } else if (!this._menuId) {
            this._menuId = 'ptcs-menu-button-' + performance.now().toString().replace('.', '');
        }
        if (this._menu) {
            this._menu.setAttribute('id', this._menuId);
        }
    }

    static get $parts() {
        return [];
    }
};

customElements.define(PTCS.MenuButton.is, PTCS.MenuButton);
