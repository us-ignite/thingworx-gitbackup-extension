import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-label/ptcs-label.js';
import 'ptcs-button/ptcs-button.js';
import 'ptcs-menu-button/ptcs-menu-button.js';
import 'ptcs-link/ptcs-link.js';
import 'ptcs-chip/ptcs-chip.js';
import 'ptcs-dropdown/ptcs-dropdown.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-icons/cds-icons.js';

const ICON_EMPTY = 'cds:icon_empty';

// Symbol fields in action element
const actionField = Symbol('action'); // {action, states}
const assignField = Symbol('set-value'); // value => {assign value to component}

// Only need one instance each of these functions
const selectDropdownLabel = item => item.label || item.value;
const selectDropdownValue = item => item.value;
const selectDropdownState = item => item.state;

const setWidth = (el, width, maxWidth) => {
    if (width) {
        el.style.width = `${PTCS.cssDecodeSize(width, el)}px`;
    }
    if (maxWidth) {
        el.style.maxWidth = `${PTCS.cssDecodeSize(maxWidth, el)}px`;
    }
};

// The Toolbar Tools component
PTCS.ToolBarTools = class extends PTCS.BehaviorStyleable(L2Pw(LitElement)) {
    static get styles() {
        return css`
        :host {
            flex: 1 1 auto;
            display: flex;
            overflow: hidden;
        }

        [part=actions] {
            display: flex;
            align-items: flex-end;
        }

        .filter-cntr {
            display: none;
        }

        :host([show-filter]) .filter-cntr {
            display: flex;
            align-items: flex-end;
        }

        .right-cntr {
            flex: 1 1 auto;
            display: flex;
            align-items: flex-end;
            justify-content: flex-end;
        }

        :host([filter-pos=center]) .filter-cntr {
            order: 2;
            flex: 1 1 auto;
        }

        :host([filter-pos=center][filter-align=center]) .filter-cntr {
            justify-content: center;
        }

        :host([filter-pos=center][filter-align=right]) .filter-cntr {
            justify-content: flex-end;
        }

        :host([filter-pos=center]) [part=actions] {
            order: 1;
        }

        :host([filter-pos=center]) .right-cntr {
            flex: 0 0 auto;
            order: 3;
        }

        :host([filter-pos=right]) .filter-cntr {
            order: 3;
        }

        :host([filter-pos=right]) [part=actions] {
            order: 1;
        }

        :host([filter-pos=right]) .right-cntr {
            order: 2;
        }

        [part=right-actions] {
            display: flex;
            align-items: flex-end;
        }

        [part~=overflow-button] {
            position: absolute;
        }

        [part~=action] {
            flex: 0 0 auto;
        }

        [part~=action][hidden] {
            display: none !important;
        }

        [part~=right-action] {
            flex: 0 0 auto;
        }

        [part~=right-action][hidden] {
            display: none !important;
        }

        [part~=additional-label] {
            flex: 0 0 auto;
        }

        [part~=right-overflow-button] {
            margin-left: 0 !important;
        }

        [part~=right-actions]:not([collapsed]) [part~=right-overflow-button] {
            display: none;
        }

        [part~=right-actions][collapsed] :not([part~=right-overflow-button]) {
            display: none;
        }`;
    }

    render() {
        return html`
        <div id="filterCntr" class="filter-cntr">
            <ptcs-textfield part="simple-filter" id="filter"
                .disabled=${this.disabled}
                ?hidden=${!this.additionalLabel}
                .label=${this.filterLabel}
                .icon=${this.filterIcon}
                .hintText=${this.filterHintText}
                .tooltip=${this.filterTooltip}
                .text=${this.filterString}
                @text-changed=${this._textChanged}>
            </ptcs-textfield>
        </div>
        <ptcs-label id="additional-label" variant="label" part="additional-label"
            .label=${this.additionalLabel} vertical-alignment="flex-end"
            ?hidden=${!this.additionalLabel}>
        </ptcs-label>
        <div id="actions" part="actions">
            <ptcs-menu-button part="action overflow-button" display-icons allow-missing-icons
                .maxMenuItems=${'10'}
                .icon=${'cds:icon_more_horizontal'} .menuMinWidth=${this.menuMinWidth} .menuMaxWidth=${this.menuMaxWidth}>
            </ptcs-menu-button>
            <!-- actions comes here -->
        </div>
        <div class="right-cntr">
            <div id="rightActions" part="right-actions" ?collapsed=${this._collapseRight}>
                <ptcs-menu-button part="right-action right-overflow-button" display-icons allow-missing-icons
                    .label=${this._collapseRightLabel}
                    .icon=${'cds:icon_chevron_down'}
                    .iconPlacement=${'right'}
                    .maxMenuItems=${'10'}
                    .menuMinWidth=${this.menuMinWidth} .menuMaxWidth=${this.menuMaxWidth}>
                </ptcs-menu-button>
                <!-- rightActions comes here -->
            </div>
        </div>`;
    }

    static get is() {
        return 'ptcs-toolbar-tools';
    }

    static get properties() {
        return {
            // Disables the toolbar
            disabled: {
                type:     Boolean,
                value:    false,
                observer: '_disabledChanged'
            },

            // Actions for the action region
            actions: {
                type:        Array, // Action[]
                observeWhen: 'immediate',
                observer:    '_actionsChanged'
            },

            // Actions for the right action region
            rightActions: {
                type:        Array, // Action[]
                observeWhen: 'immediate',
                observer:    '_rightActionsChanged'
            },

            // Show filter
            showFilter: {
                type:      Boolean,
                reflect:   true,
                attribute: 'show-filter',
                observer:  '_resizeActions'
            },

            // Label displayed above the simple filter
            filterLabel: {
                type:      String,
                attribute: 'filter-label'
            },

            // Specifies an additional label that occurs before the toolbar left actions
            additionalLabel: {
                type:      String,
                attribute: 'additional-label',
                observer:  '_resizeActions'
            },

            // Specifies the icon in the simple filter.
            filterIcon: {
                type:      String,
                attribute: 'filter-icon'
            },

            menuMinWidth: {
                type: String
            },

            menuMaxWidth: {
                type: String
            },

            filterWidth: {
                type:      Number,
                attribute: 'filter-width',
                observer:  '_filterWidthChanged'
            },

            // Filter position (if showFilter): 'left', 'center', 'right'
            filterPos: {
                type:      String,
                attribute: 'filter-pos',
                reflect:   true
            },

            // Placeholder text for the simple filter
            filterHintText: {
                type:      String,
                value:     'Filter',
                attribute: 'filter-fint-text'
            },

            // Filter alignment (if filterPos='center'): 'left', 'center', 'right'
            filterAlign: {
                type:      String,
                attribute: 'filter-align',
                reflect:   true
            },

            // Tooltip for the simple filter
            filterTooltip: {
                type:      String,
                attribute: 'filter-tooltip'
            },

            // The text that has been entered in the simple filter text
            filterString: {
                type:      String,
                attribute: 'filter-string',
                notify:    true
            },

            // Maximum width for toolbar tools, dynamically assigned by client
            maxWidth: {
                type:      Number,
                attribute: 'max-width',
                observer:  '_resizeActions'
            },

            // Width of overflow button, right tools and filter (minimum width needed by this component)
            minWidth: {
                type:      Number,
                attribute: 'min-width',
                notify:    true
            },

            // Overflowing elements
            _overflowElements: {
                type: Array // Action[]
            },

            focusable: {
                type:     String,
                observer: '_focusableChanged'
            },

            // Specifies a label for the collapsed right area button
            rightOverflowLabel: {
                type:      String,
                attribute: 'right-overflow-label',
                observer:  '_resizeActions'
            },

            _collapseRight: {
                type: Boolean
            },

            _collapseRightLabel: {
                type: String
            },

            _overflowButton: Element,

            _rightOverflowButton: Element,
        };
    }

    constructor() {
        super();
        this._collapseRight = false;
        this._collapseRightLabel = undefined;
        this.menuMinWidth = '48px';
        this.menuMaxWidth = '248px';
    }

    ready() {
        super.ready();
        this._overflowButton = this.$.actions.querySelector('[part~=overflow-button]');
        this._rightOverflowButton = this.$.rightActions.querySelector('[part~=right-overflow-button]');

        // Firefox needs this, or it truncates the focus border at the top of the buttons
        this._overflowButton.focusNoClipping = true;

        if (this.additionalLabel === undefined) {
            this.additionalLabel = null; // Force notification
        }

        // Add a callback to handle clicks in the overflow menu button
        this.addEventListener('action', (ev) => this._menuAction(ev));
    }

    disconnectedCallback() {
        this._closeOverflowMenus();
        super.disconnectedCallback();
    }

    // Return the CSS min-width of the simple filter
    get simpleFilterMinWidth() {
        const minWidth = PTCS.cssDecodeSize(getComputedStyle(this.$.filter).minWidth, this.$.filter);
        return !isNaN(minWidth) ? minWidth : undefined;
    }

    _textChanged(ev) {
        if (this.filterString !== ev.detail.value) {
            this.filterString = ev.detail.value;
        }
    }

    // Set width of the filter
    _filterWidthChanged(filterWidth) {
        this.$.filter.style.width = filterWidth ? filterWidth + 'px' : '';
        this._resizeActions();
    }

    _applyToItemElements(f) {
        // Make sure everything is in place...
        this.performUpdate();

        f(this.$.filter);

        for (let el = this.$.actions.firstElementChild; el; el = el.nextElementSibling) {
            f(el);
        }
        for (let el = this.$.rightActions.firstElementChild; el; el = el.nextElementSibling) {
            f(el);
        }
    }

    _disabledChanged(disabled) {
        this._closeOverflowMenus();
        this._applyToItemElements(el => {
            el.disabled = disabled || (el[actionField] && el[actionField].states.disabled);
        });
    }

    _focusableChanged(focusable) {
        if (focusable) {
            this._applyToItemElements(el => el.setAttribute('tabindex', '-1'));
        } else {
            this._applyToItemElements(el => el.removeAttribute('tabindex'));
        }
    }

    // Append action element with part name to fragment
    _addActionPart(frag, action, partName) {
        const el = PTCS.ToolBarTools.createAction(action);
        el.setAttribute('part', partName);
        frag.appendChild(el);
        return frag;
    }

    _actionsChanged() {
        this._closeOverflowMenus();
        if (!this._overflowButton) {
            return;
        }

        // Remove old actions
        while (this._overflowButton.nextSibling) {
            this.$.actions.removeChild(this._overflowButton.nextSibling);
        }

        // Add new actions
        if (Array.isArray(this.actions)) {
            this.$.actions.appendChild(this.actions.reduce(
                (a, d) => this._addActionPart(a, d, 'action'),
                document.createDocumentFragment()));
        }

        // Set taborder, if applicable
        if (this.focusable) {
            this._focusableChanged(this.focusable, this.filterPos);
        }

        // Show / Hide actions region
        this.$.actions.style.display = this._overflowButton.nextElementSibling ? '' : 'none';

        // Make sure the actions have the right disabled state from the start
        this._disabledChanged(this.disabled);

        requestAnimationFrame(() => this._resizeActions());
    }

    _firstVisibleAction(overflowButton) {
        for (let el = overflowButton.nextElementSibling; el; el = el.nextElementSibling) {
            if (!el.hasAttribute('hidden')) {
                return el;
            }
        }
        return null;
    }

    _closeOverflowMenus() {
        if (this._overflowButton) {
            this._overflowButton.closeMenu();
        }
        if (this._rightOverflowButton) {
            this._rightOverflowButton.closeMenu();
        }
    }

    _updateOverflowMenu() {
        // Hidden elements must be filtered out before being passed on to the menu button
        const isHidden = el => {
            const a = el[actionField];
            return a && a.states && a.states.hidden;
        };

        // Left overflow menu
        if (this._overflowButton) {
            // If an action is hidden, then don't create a menu item for it
            const elements = this._overflowElements ? this._overflowElements.filter(el => !isHidden(el)) : [];
            this._overflowButton.items = elements.map(el => this.createMenuItem(el[actionField], el));
        }
        // Right overflow menu
        if (this._rightOverflowButton) {
            const rightElements = [...this.$.rightActions.querySelectorAll('[part~=right-action]:not([part~=right-overflow-button]')];
            const elements = rightElements ? rightElements.filter(el => !isHidden(el)) : [];
            this._rightOverflowButton.items = elements.map(el => this.createMenuItem(el[actionField], el));
        }
    }

    static createMenuItemButton(action, states, el) {
        const {label, type} = action ?? {};
        const icon = (action.opt && action.opt.icon);
        const disabled = states && states.disabled;
        const tooltip = action.alt;
        const tooltipIcon = action.altIcon;

        return {toplevel: true, label, type, icon, disabled, tooltip, tooltipIcon, el};
    }

    static createMenuItemLink(action, states, el) {
        const {label, type} = action ?? {};
        const disabled = states && states.disabled;
        const tooltip = action.alt;
        const tooltipIcon = action.altIcon;

        return {toplevel: true, label, type, disabled, tooltip, tooltipIcon, el};
    }

    static createMenuItemToggle(action, states, el) {
        const {label, type} = action ?? {};
        const checked = states && states.value;
        const disabled = states && states.disabled;
        const tooltip = action.alt;
        const tooltipIcon = action.altIcon;
        return {toplevel: true, label, type, checked, disabled, tooltip, tooltipIcon, el};
    }

    static createMenuItemDropdown(action, states, el) {
        const {label, type} = action ?? {};
        const selectedValue = states && states.value;
        const disabled = states && states.disabled;
        const tooltip = action.alt;
        const tooltipIcon = action.altIcon;

        // Create submenu
        const content = action.opt && action.opt.values && action.opt.values.map(entry => {
            return {
                label:       entry.label || entry.value,
                value:       entry.value,
                selected:    entry.value === selectedValue,
                disabled:    entry.state === 'disabled',
                tooltip:     entry.alt,
                tooltipIcon: entry.altIcon,
                el};
        });

        return {toplevel: true, label, type, content, disabled, tooltip, tooltipIcon, el};
    }

    static createMenuItemMenuButton(action, states, el) {
        const {label, type, opt} = action ?? {};
        const disabled = states && states.disabled;
        const hidden = states && states.hidden;
        const tooltip = action.alt;
        const tooltipIcon = action.altIcon;
        const displayIcons = el.displayIcons;
        const allowMissingIcons = el.allowMissingIcons;

        const content = action.opt && action.opt.values ? PTCS.ToolBarTools.mapEntries(opt.values, action, displayIcons, allowMissingIcons) : [];

        return {toplevel: true, label, type, hidden, content, displayIcons, allowMissingIcons, disabled, tooltip, tooltipIcon, el};
    }

    static mapEntries(entries, mainAction, displayIcons, allowMissingIcons) {
        return entries.map(entry => {
            return {
                ...entry,
                displayIcons:      displayIcons,
                allowMissingIcons: allowMissingIcons,
                label:             entry.label || entry.value,
                action:            mainAction,
                content:           entry.content
                    ? PTCS.ToolBarTools.mapEntries(entry.content, mainAction, displayIcons, allowMissingIcons) : undefined,
            };
        });
    }

    static createMenuItemInvalid(action, states, el) {
        return {label: `Invalid action type ${action.type}`, icon: ICON_EMPTY, el};
    }

    createMenuItem(actionObj, el) {
        const action = actionObj.action;
        const states = actionObj.states;
        return (PTCS.ToolBarTools.menuItemCtor[action.type] || PTCS.ToolBarTools.createMenuItemInvalid)(action, states, el);
    }

    // Called when a selection has been made from one of the menu button items
    _menuAction(ev) {
        const el = ev.detail.item.el;
        // exclude menu-item of menu-button
        if (!ev.detail.item.toplevel && !el) {
            return;
        }
        const action = el[actionField];
        const type = action.action.type;

        switch (type) {
            case 'link':
                el.activateLink();
                break;
            case 'button':
                el.click();
                break;
            case 'toggle':
                el.checked = !el.checked;
                el.performUpdate();
                break;
            case 'dropdown':
                el.selectedValue = ev.detail.item.value;
                el.performUpdate();
                break;
        }

        // Make sure the overflow menu button reflects any change in toggle state / selection
        if (type !== 'toggle') {
            this._updateOverflowMenu();
        }
    }

    // Hide overflowing actions, if any, and place overflow button after last non-hidden action
    _resizeActions() {
        if (!this.maxWidth || this._overflowButton.__isOpeningPopup) {
            return; // Not ready
        }
        this._closeOverflowMenus();
        this._overflowElements = [];

        const tbBB = this.getBoundingClientRect();
        let rightWidth = PTCS.getElementWidth(this.$.rightActions);
        let filterCntrWidth = this.$.filterCntr.offsetWidth;
        let firstVisibleAction = this._firstVisibleAction(this._overflowButton);
        let overflowWidth = firstVisibleAction ? PTCS.getElementWidth(this._overflowButton) : 0;

        // Make sure we have room for the right-side items (and see if they should be collapsed)
        const toolbarWidth = tbBB.right - tbBB.left;
        const rightSideAvailable = toolbarWidth - (filterCntrWidth + overflowWidth);

        // We must keep track of the different widths
        if (!this._collapseRight) {
            // The right items are showing, store their current width to tell when they should be un-collapsed
            this.__prevFullRightWidth = rightWidth;
        } else if (this._collapseRightLabel) {
            // Do the same for the collapsed-with-label item
            this.__prevLabelRightWidth = rightWidth;
        }

        this._collapseRight = rightSideAvailable < this.__prevFullRightWidth;
        this._collapseRightLabel = rightSideAvailable < this.__prevLabelRightWidth ? undefined : this.rightOverflowLabel;

        // Update the collapse attribute to update the rightActions width
        this.performUpdate();

        // Basic geometry
        const bb0 = this.$.actions.getBoundingClientRect();
        const filterWidth = PTCS.getElementWidth(this.$.filter);
        filterCntrWidth = this.$.filterCntr.offsetWidth;
        rightWidth = PTCS.getElementWidth(this.$.rightActions);
        const additionalLabelWidth = PTCS.getElementWidth(this.$['additional-label']);
        const end = Math.min(bb0.left + this.maxWidth - filterWidth - additionalLabelWidth - rightWidth, tbBB.right);

        // Overflow buttons geometry
        firstVisibleAction = this._firstVisibleAction(this._overflowButton);
        overflowWidth = firstVisibleAction ? PTCS.getElementWidth(this._overflowButton) : 0;
        const rightOverflowWidth = this._firstVisibleAction(this._rightOverflowButton)
            ? PTCS.getElementWidth(this._rightOverflowButton.clientWidth)
            : 0;

        // Report minimum width
        this.minWidth = filterWidth + additionalLabelWidth + overflowWidth + rightOverflowWidth;

        const isHidden = el => el.hasAttribute('hidden');

        let bb;
        for (let el = firstVisibleAction; el; el = el.nextElementSibling) {
            if (isHidden(el)) {
                continue;
            }

            bb = el.getBoundingClientRect();
            if (bb.right < end) {
                // Action is fully visible
                el.style.visibility = '';
                continue;
            }

            // Found overflowing action
            if (bb.left + overflowWidth >= end && el !== firstVisibleAction) {
                // Previous action must also be moved to in the overflow bag, if any (visible) exist
                el = el.previousElementSibling;
                while (isHidden(el)) {
                    el = el.previousElementSibling;
                }
                bb = el.getBoundingClientRect();
            }

            // Place overflow button at its proper place
            this._overflowButton.style.transform = `translateX(${Math.max(bb.left - bb0.left, 0)}px)`;
            this._overflowButton.style.visibility = '';

            // Hide all following actions
            do {
                if (!isHidden(el)) {
                    this._overflowElements.push(el);
                }
                el.style.visibility = 'hidden';
                el = el.nextElementSibling;
            } while (el);

            // Create/set items of the menu button
            this._updateOverflowMenu();

            this.$.actions.style.width = `${bb.left - bb0.left + overflowWidth}px`;

            this._updateOverflowSelected();
            return;
        }

        // No overflows found
        this._overflowButton.style.visibility = 'hidden';
        this.$.actions.style.width = '';

        // We still need to update the right-menu items (if any)
        this._updateOverflowMenu();

        if (!firstVisibleAction) {
            this._updateOverflowSelected();
        }
    }

    _rightActionsChanged() {
        // Remove old rightActions
        while (this._rightOverflowButton.nextSibling) {
            this.$.rightActions.removeChild(this._rightOverflowButton.nextSibling);
        }

        // Add new rightActions
        if (Array.isArray(this.rightActions)) {
            this.$.rightActions.appendChild(this.rightActions.reduce(
                (a, d) => this._addActionPart(a, d, 'right-action'),
                document.createDocumentFragment()));
        }

        // Set taborder, if applicable
        if (this.focusable) {
            this._focusableChanged(this.focusable, this.filterPos);
        }

        // Show / Hide right actions region
        this.$.rightActions.style.display = this._rightOverflowButton.nextSibling ? '' : 'none';

        // Make sure the actions have the right disabled state from the start
        this._disabledChanged(this.disabled);

        requestAnimationFrame(() => this._resizeActions());
    }

    // Select overflow buttons, if they contain any selected actions
    _updateOverflowSelected() {
        if ([...this.$.actions.querySelectorAll('[part~=action][selected]:not([hidden])')].some(e => e.style.visibility === 'hidden')) {
            this._overflowButton.setAttribute('selected', '');
        } else {
            this._overflowButton.removeAttribute('selected');
        }
        if (this.$.rightActions.querySelector('[part~=right-action][selected]:not([part~=right-overflow-button])')) {
            this._rightOverflowButton.setAttribute('selected', '');
        } else {
            this._rightOverflowButton.removeAttribute('selected');
        }
    }

    _clicked({action}, r) {
        this.dispatchEvent(new CustomEvent('activated', {
            composed: true,
            detail:   {action, r}}));
    }

    _valueChanged({action, states}) {
        this.dispatchEvent(new CustomEvent('value-changed', {
            composed: true,
            detail:   {action, value: states.value}
        }));
        if (action.type !== 'toggle') {
            this._updateOverflowMenu();
        }
    }

    setLabel(id, label) {
        this._applyToItemElements(el => {
            const x = el[actionField];
            if (x && x.action.id === id) {
                el.label = label;
                x.action.label = label;
            }
        });
    }

    setTooltip(id, alt) {
        this._applyToItemElements(el => {
            const x = el[actionField];
            if (x && x.action.id === id) {
                el.tooltip = alt;
                x.action.tooltip = alt;
            }
        });
    }

    setDisabled(id, disabled) {
        this._applyToItemElements(el => {
            const x = el[actionField];
            if (x && x.action.id === id) {
                el.disabled = disabled;
                x.states.disabled = disabled;
            }
        });
        this._resizeActions();
    }

    setHidden(id, hidden) {
        this._applyToItemElements(el => {
            const x = el[actionField];
            if (x && x.action.id === id) {
                if (hidden) {
                    el.setAttribute('hidden', '');
                } else {
                    el.removeAttribute('hidden');
                }
                x.states.hidden = hidden;
            }
        });

        this._resizeActions();
    }

    setValue(id, value) {
        this._applyToItemElements(el => {
            const x = el[actionField];
            if (x && x.action.id === id && el[assignField]) {
                el[assignField](value);
            }
        });
    }

    setSelected(id, selected) {
        this._applyToItemElements(el => {
            const x = el[actionField];
            if (x && x.action.id === id) {
                if (selected) {
                    el.setAttribute('selected', '');
                } else {
                    el.removeAttribute('selected');
                }
            }
        });
        this._updateOverflowSelected();
    }

    setArrowDownActivate(id, activate) {
        this._applyToItemElements(el => {
            const x = el[actionField];
            if (x && x.action.id === id) {
                el._arrowDownActivate = activate;
            }
        });
    }

    // Get focusable sub-elements
    get focusableElements() {
        if (!this.focusable) {
            return [];
        }
        const a = [...[...this.$.actions.querySelectorAll('[part~=action]:not([hidden])')]
            .filter(action => action.style.visibility !== 'hidden')];

        // If overflow is visible, then put it last
        if (a[0] === this._overflowButton) {
            const menuButton = a.shift();
            // It is the button within the ptcs-menu-button that should get focus, not the menu button itself
            a.push(menuButton.$.button);
        }

        // Filter to the left or in the middle?
        if (this.showFilter) {
            if (this.filterPos === 'center') {
                // Simple filter is centered
                a.push(this.$.filter);
            } else if (this.filterPos !== 'right') {
                // Simple filter is to the left (not center and not right)
                a.unshift(this.$.filter);
            }
        }

        // Right actions
        if (this._collapseRight) {
            // Same here, it is the button within the ptcs-menu-button that should get focus
            a.push(this._rightOverflowButton.$.button);
        } else {
            a.push(...[...this.$.rightActions.querySelectorAll('[part~=right-action]:not([hidden])')]
                .filter(action => action !== this._rightOverflowButton && action.style.visibility !== 'hidden'));
        }

        // Filter to the right
        if (this.showFilter && this.filterPos === 'right') {
            // Simple filter is last
            a.push(this.$.filter);
        }
        return a;
    }


    /*
     * Create toolbar actions
     */
    static clicked(ev) {
        if (ev.target.disabled) {
            return;
        }

        // This is soo hacky... (only needed for the grid Display button)
        let el = ev.target;
        if (!el.clientWidth) {
            el = ev.target.closest('#actions');
            el = el && el.querySelector('[part~=overflow-button]');
            if (!el) {
                el = ev.target.closest('#rightActions');
                el = el && el.querySelector('[part~=right-overflow-button]');
                if (!el) {
                    el = this;
                }
            }
        }

        ev.target.getRootNode().host._clicked(ev.target[actionField], el.getBoundingClientRect());
    }

    static createButton({label, alt, altIcon, width, maxWidth, opt}) {
        const el = document.createElement('ptcs-button');
        el.variant = (opt && opt.variant) || 'transparent';
        el.icon = opt && opt.icon;
        el.iconSet = opt && opt.iconSet;

        if (opt && opt.iconPlacement) {
            el.iconPlacement = opt.iconPlacement;
        }

        el.label = label;
        el.tooltip = alt;
        el.tooltipIcon = altIcon;
        setWidth(el, width, maxWidth);
        el.addEventListener('click', PTCS.ToolBarTools.clicked);
        el.addEventListener('touchend', function(e) {
            e.preventDefault();
            PTCS.ToolBarTools.clicked(e);
        });
        return el;
    }

    static createLink({label, alt, altIcon, width, maxWidth, opt}) {
        const el = document.createElement('ptcs-link');
        el.label = label;
        el.tooltip = alt;
        el.tooltipIcon = altIcon;
        el.singleLine = true;
        el.href = opt && opt.href;
        el.target = (opt && opt.target) || 'new';
        el.variant = (opt && opt.variant) || 'primary';
        setWidth(el, width, maxWidth);
        el.addEventListener('click', PTCS.ToolBarTools.clicked);
        return el;
    }

    static toggleChanged(ev) {
        const x = ev.target[actionField];
        if ((!x.states.value) !== (!ev.detail.value)) {
            x.states.value = ev.detail.value;
            // Hidden buttons can generate change events before they have been attached.
            // The changed value has therefore been assigned by the (unreachable) host, so it doesn't need to be informed about the change
            const host = ev.target.getRootNode().host;
            if (host) {
                host._valueChanged(x);
            }
        }
    }

    static createToggle({label, alt, altIcon, width, maxWidth, opt}) {
        const el = document.createElement('ptcs-chip');
        el.hideIcon = opt && opt.hideIcon;
        el.checked = (opt && opt.value) || false;
        el.labelalign = (opt && opt.labelalign) || 'left';
        el.label = label;
        el.tooltip = alt;
        el.tooltipIcon = altIcon;
        setWidth(el, width, maxWidth);
        el.addEventListener('checked-changed', PTCS.ToolBarTools.toggleChanged);
        el[assignField] = value => {
            el.checked = !!value;
        };
        return el;
    }

    static dropdownChanged(ev) {
        const x = ev.target[actionField];
        if (x.states.value !== ev.detail.value) {
            x.states.value = ev.detail.value;
            ev.target.getRootNode().host._valueChanged(x);
        }
    }

    static createDropdown({label, alt, altIcon, width, maxWidth, opt}) {
        const el = document.createElement('ptcs-dropdown');
        el.selector = selectDropdownLabel;
        el.valueSelector = selectDropdownValue;
        el.stateSelector = selectDropdownState;
        el.label = label;
        el.hintText = opt && opt.hintText;
        el.tooltip = alt;
        el.tooltipIcon = altIcon;
        setWidth(el, width || '120px', maxWidth);
        if (label) {
            el.setAttribute('top-label', '');
        }
        el.items = opt && opt.values;
        el.selectedValue = opt && opt.value;
        el.addEventListener('selected-value-changed', PTCS.ToolBarTools.dropdownChanged);
        el[assignField] = value => {
            el.selectedValue = value;
        };
        return el;
    }

    static createMenuButton(action) {
        const {label, alt, altIcon, width, maxWidth, opt} = action;
        const el = document.createElement('ptcs-menu-button');
        el.displayIcons = opt && opt.displayIcons;
        el.allowMissingIcons = opt && opt.allowMissingIcons;
        el.menuType = 'flyout';
        el.menuMaxWidth = '248px';
        el.menuMinWidth = '48px';
        el.buttonVariant = (opt && opt.variant) || 'tertiary';
        if (opt && opt.iconPlacement) {
            el.iconPlacement = opt.iconPlacement;
        }
        el.icon = opt && opt.icon;
        el.label = label;
        el.tooltip = alt;
        el.tooltipIcon = altIcon;
        el.__ignoreTabIndex = true;
        setWidth(el, width || '120px', maxWidth);
        el.items = PTCS.ToolBarTools.mapEntries(opt && opt.values, action);
        return el;
    }

    static createInvalid({type, label, alt, altIcon}) {
        const el = document.createElement('ptcs-label');
        el.label = `${label}: type err: ${JSON.stringify(type)}`;
        el.tooltip = alt;
        el.tooltipIcon = altIcon;
        return el;
    }

    // Create action element
    static createAction(action) {
        if (action.opt && action.opt.values) {
            action.opt.values = action.opt.values.filter(value => value.state !== 'hidden');
        }
        const el = (PTCS.ToolBarTools.actionCtor[action.type] || PTCS.ToolBarTools.createInvalid)(action);
        const states = {disabled: action.disabled, hidden: action.hidden, value: action.opt && action.opt.value};
        el[actionField] = {action, states};

        el.disabled = states.disabled;
        if (states.hidden) {
            el.setAttribute('hidden', true);
        }

        // Firefox needs this, or it truncates the focus border at the top of the buttons
        el.focusNoClipping = true;

        // Don't allow flexbox to resize control
        el.style.flex = '0 0 auto';

        return el;
    }
};

// Create actions
PTCS.ToolBarTools.actionCtor = {
    button:     PTCS.ToolBarTools.createButton,
    link:       PTCS.ToolBarTools.createLink,
    toggle:     PTCS.ToolBarTools.createToggle,
    dropdown:   PTCS.ToolBarTools.createDropdown,
    menubutton: PTCS.ToolBarTools.createMenuButton,
};

// Create ptcs-menu-button menuItems
PTCS.ToolBarTools.menuItemCtor = {
    button:     PTCS.ToolBarTools.createMenuItemButton,
    link:       PTCS.ToolBarTools.createMenuItemLink,
    toggle:     PTCS.ToolBarTools.createMenuItemToggle,
    dropdown:   PTCS.ToolBarTools.createMenuItemDropdown,
    menubutton: PTCS.ToolBarTools.createMenuItemMenuButton
};

customElements.define(PTCS.ToolBarTools.is, PTCS.ToolBarTools);
