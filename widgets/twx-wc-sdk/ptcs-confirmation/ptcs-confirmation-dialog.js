import {LitElement, html, css} from 'lit';
import 'ptcs-hbar/ptcs-hbar.js';
import 'ptcs-button/ptcs-button.js';
import 'ptcs-icons/cds-icons.js';
import 'ptcs-label/ptcs-label.js';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-modal-overlay/ptcs-modal-overlay.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';

const DIALOG_WIDTH_HEIGHT_PADDING = 48; // 24 * 2, both sides
const DIALOG_DEFAULT_WIDTH = 600;
const DIALOG_DEFAULT_HEIGHT = 260;

PTCS.ConfirmationDialog = class extends PTCS.BehaviorFocus(PTCS.BehaviorStyleable(LitElement)) {
    static get styles() {
        return css`
            :host([mode=open]) {
                display: block;
            }

            :host(:not([mode=open])) {
                display: none;
            }

            [part=secondary-button][no-secondary-button] {
                display: none;
            }

            [part=secondary-button]:not([no-secondary-button]) {
                display: inline-flex;
            }

            [part=cancel-button][no-cancel-button] {
                display: none;
            }

            [part=cancel-button]:not([no-cancel-button]) {
                display: inline-flex;
            }

            [part=close-button][no-close-button] {
                display: none;
            }

            [part=root] { /* Root of the dialog, used to position it centered and above the opaque background  */
                z-index: 99998;
                position: absolute;
                top: 0;
                left: 0;
                bottom: 0;
                right: 0;
                display: flex;
                align-items: center;
                justify-content: center;
            }

            [part=dialog] { /* The dialog itself */
                position: fixed;
                background-color: white;
                display: flex;
                flex-direction: column;
                padding: 24px;
            }

            [part=message-container] {
                padding-right: 18px;
                padding-left: 18px;
                align-self: flex-start;
                flex-shrink: 10;
                display: flex;
                flex-direction: column;
                min-height: 0px;
            }

            [part=title] {
                padding-bottom: 16px;
                min-width: 0px;
            }

            [part=message-wrapper] {
                flex-shrink: 100;
                overflow: auto;
            }

            [part=message] {
                padding-right: 8px;
                min-width: 0px;
            }

            [part=buttons-container] {
                padding-top: 16px;
                padding-right: 18px;
                padding-left: 18px;
                margin-top: auto;
                display: flex;
                justify-content: flex-end;
            }

            [part=buttons-wrapper] {
                display: flex;
            }

            [part=buttons-wrapper].reverse { /* to toggle the Primary Action Position */
                flex-direction: row-reverse;
            }

            ptcs-button[variant=secondary],
            ptcs-button[variant=tertiary],
            ptcs-button[variant=danger],
            ptcs-button[variant=transparent] {
                margin-left: 16px;
            }

            ptcs-button[variant=secondary].reverse,
            ptcs-button[variant=tertiary].reverse,
            ptcs-button[variant=danger].reverse,
            ptcs-button[variant=transparent].reverse {
                margin-right: 16px;
                margin-left: 0px;
            }
        `;
    }

    render() {
        const dialogWidth = this._dialogWidth(this.confWidth);
        const dialogHeight = this._dialogHeight(this.confHeight);
        const dialogStyle = `width:${dialogWidth}px; height:${dialogHeight}px`;
        // setting max width for labels, for IE11 label bug
        const messageStyle = `max-width:${dialogWidth - 36}px;`;
        const actionButtonsStyle = this._actionButtonsWidth ? `width:${this._actionButtonsWidth}px;` : '';

        return html`
            <ptcs-modal-overlay part="overlay"></ptcs-modal-overlay>
            <div id="root" part="root">
                <div id="dialog" class="dialog" part="dialog" style=${dialogStyle}>
                    <ptcs-hbar end="">
                        <ptcs-button variant="small" exportparts=${this._exportclose} id="close" part="close-button"
                        mode="icon" icon="cds:icon_close_mini" ?no-close-button=${!this.displayCloseButton}
                        @click=${this.cancelAction} .tooltip=${this.closeButtonTooltipField} tabindex=4>
                        </ptcs-button>
                    </ptcs-hbar>
                    <div part="message-container">
                        <ptcs-label part="title" id="dlg-title" .label=${this.titleText} variant="header"
                        ?hidden=${this._isEmpty(this.titleText)} multi-line style=${messageStyle}>
                        </ptcs-label>
                        <div part="message-wrapper" id="message-wrapper" ?hidden=${this._isEmpty(this.messageText)} style=${messageStyle}>
                            <ptcs-label part="message" id="dlg-msg" .label=${this.messageText} variant="body" multi-line>
                            </ptcs-label>
                        </div>
                    </div>
                    <div part="buttons-container" class=${this._clsButtons(this.actionPosition)}>
                        <div part="buttons-wrapper" class=${this._clsButtons(this.actionPosition)}>
                            <ptcs-button id="primary-button" variant=${this.primaryButtonStyle}
                                exportparts=${this._exportprimary}
                                part="primary-button" @click=${this.primaryAction} .label=${this.primaryActionLabel}
                                class=${this._clsButtons(this.actionPosition)}
                                .icon=${this.primaryActionIcon} .tooltip=${this.actionButtonTooltipField}
                                .tooltipIcon=${this.actionButtonTooltipIcon} style=${actionButtonsStyle}
                                tabindex=1>
                            </ptcs-button>
                            <ptcs-button id="secondary-button" variant="tertiary" part="secondary-button"
                                exportparts=${this._exporttertiary} @click=${this.secondaryAction} .label=${this.secondaryActionLabel}
                                class=${this._clsButtons(this.actionPosition)} .icon=${this.secondaryActionIcon}
                                ?no-secondary-button=${!this.displaySecondaryAction}
                                .tooltip=${this.secondButtonTooltipField} .tooltipIcon=${this.secondButtonTooltipIcon}
                                style=${actionButtonsStyle} tabindex=2>
                            </ptcs-button>
                            <ptcs-button id="cancel-button" variant="secondary" exportparts=${this._exportsecondary}
                                part="cancel-button" @click=${this.cancelAction} .label=${this.cancelActionLabel}
                                class=${this._clsButtons(this.actionPosition)} .icon=${this.cancelActionIcon}
                                ?no-cancel-button=${this.hideCancelAction}
                                .tooltip=${this.cancelButtonTooltipField} .tooltipIcon=${this.cancelButtonTooltipIcon}
                                style=${actionButtonsStyle} tabindex=3>
                            </ptcs-button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    static get is() {
        return 'ptcs-confirmation-dialog';
    }

    static get properties() {
        return {
            mode: {
                type:    String,
                reflect: true
            },
            titleText: {
                type: String
            },
            messageText: {
                type: String
            },
            confWidth: {
                type: String
            },
            confHeight: {
                type: String
            },
            displaySecondaryAction: {
                type: Boolean
            },
            displayCloseButton: {
                type: Boolean
            },
            hideCancelAction: {
                type: Boolean
            },
            actionPosition: {
                type: String
            },
            primaryButtonStyle: {
                type: String
            },
            primaryActionLabel: {
                type: String
            },
            primaryActionIcon: {
                type: String
            },
            secondaryActionLabel: {
                type: String
            },
            secondaryActionIcon: {
                type: String
            },
            cancelActionLabel: {
                type: String
            },
            cancelActionIcon: {
                type: String
            },
            actionButtonTooltipField: {
                type: String
            },
            actionButtonTooltipIcon: {
                type: String
            },
            secondButtonTooltipField: {
                type: String
            },
            secondButtonTooltipIcon: {
                type: String
            },
            closeButtonTooltipField: {
                type: String
            },
            cancelButtonTooltipField: {
                type: String
            },
            cancelButtonTooltipIcon: {
                type: String
            },

            _exportclose: {
                type:  String,
                state: true
            },

            _exportprimary: {
                type:  String,
                state: true
            },

            _exporttertiary: {
                type:  String,
                state: true
            },

            _exportsecondary: {
                type:  String,
                state: true
            },

            _actionButtonsWidth: {
                type:  Number,
                state: true
            }
        };
    }

    constructor() {
        super();
        this.mode = 'closed';
        this.confWidth = '600px';
        this.confHeight = '260px';
        this.actionButtonTooltipField = null;
        this.actionButtonTooltipIcon = null;
        this.secondButtonTooltipField = null;
        this.secondButtonTooltipIcon = null;
        this.closeButtonTooltipField = null;
        this.cancelButtonTooltipField = null;
        this.cancelButtonTooltipIcon = null;
        this._exportclose = PTCS.exportparts('close-button-', PTCS.Button);
        this._exportprimary = PTCS.exportparts('primary-button-', PTCS.Button);
        this._exporttertiary = PTCS.exportparts('tertiary-button-', PTCS.Button);
        this._exportsecondary = PTCS.exportparts('secondary-button-', PTCS.Button);
        this._actionButtonsWidth = undefined;
        this.addEventListener('keydown', this._keyDownNavigation.bind(this));

    }

    shouldUpdate(changedProperties) {
        if (this.mode !== 'open') {
            this._actionButtonsWidth = undefined;
        }

        return true;
    }

    updated(changedProperties) {
        super.updated(changedProperties);

        if (changedProperties.has('mode') && this.mode === 'open') {
            setTimeout(() => {
                const primaryButton = this.shadowRoot.getElementById('primary-button');
                primaryButton.__disabledTooltipOnfocus = true;
                primaryButton.focus();
                primaryButton.addEventListener('blur', () => {
                    delete primaryButton.__disabledTooltipOnfocus;
                }, {once: true});
                this._actionButtonsWidth = this._calcActionButtonsWidth();
            }, 50);
        }
    }

    _isEmpty(str) {
        return (!str);
    }

    primaryAction() {
        this.dispatchEvent(new CustomEvent('primary-action'), {
            bubbles:  true,
            composed: true
        });
    }

    secondaryAction() {
        this.dispatchEvent(new CustomEvent('secondary-action'), {
            bubbles:  true,
            composed: true
        });
    }

    cancelAction() {
        this.dispatchEvent(new CustomEvent('close-action'), {
            bubbles:  true,
            composed: true
        });
    }

    _clsButtons(actionPosition) {
        return actionPosition.toLowerCase() === 'right' ? 'reverse' : '';
    }

    _dialogWidth(confWidth) {
        let num = DIALOG_DEFAULT_WIDTH;
        if (confWidth) {
            num = parseInt(confWidth);
        }
        num -= DIALOG_WIDTH_HEIGHT_PADDING;
        return num;
    }

    _dialogHeight(confHeight) {
        let num = DIALOG_DEFAULT_HEIGHT;
        if (confHeight) {
            num = parseInt(confHeight);
        }
        num -= DIALOG_WIDTH_HEIGHT_PADDING;
        return num;
    }

    _keyDownNavigation(ev) {
        const keyEsc = ev.key;
        const key = ev.which || ev.keyCode;
        const lastLoopComponent = this.displayCloseButton ? this.shadowRoot.getElementById('close') : this.shadowRoot.getElementById('cancel-button');
        if (keyEsc === 'Escape') {
            ev.preventDefault();
            this.cancelAction();
            return;
        }
        if (!ev.shiftKey && key === 9 && this.shadowRoot.activeElement === lastLoopComponent) {
            this.shadowRoot.getElementById('primary-button').focus();
            ev.preventDefault();
        }
        if (ev.shiftKey && key === 9 && this.shadowRoot.activeElement === this.shadowRoot.getElementById('primary-button')) {
            lastLoopComponent.focus();
            ev.preventDefault();
        }
    }

    _getActualActionButtonWidth(el) {
        const bbWidth = el.getBoundingClientRect().width;
        return bbWidth ? Math.ceil(bbWidth) : 0;
    }

    _calcActionButtonsWidth() {
        const dialogWidth = this._dialogWidth(this.confWidth);
        let numOfButtons = (this.displaySecondaryAction) ? 3 : 2;
        if (this.hideCancelAction) {
            --numOfButtons;
        }

        const primaryButtonWidth = this._getActualActionButtonWidth(this.shadowRoot.getElementById('primary-button'));
        const secondaryButtonWidth = this._getActualActionButtonWidth(this.shadowRoot.getElementById('secondary-button'));
        const cancelButtonWidth = this._getActualActionButtonWidth(this.shadowRoot.getElementById('cancel-button'));
        let maxWidth = Math.max(primaryButtonWidth, secondaryButtonWidth, cancelButtonWidth);
        // dialog width (excluding padding) - buttons side padding - buttons in between margins
        const maxAllowedWidth = (dialogWidth - 36 - (16 * (numOfButtons - 1))) / numOfButtons;
        if (maxWidth > maxAllowedWidth) {
            maxWidth = maxAllowedWidth;
        }
        return maxWidth;
    }
};

customElements.define(PTCS.ConfirmationDialog.is, PTCS.ConfirmationDialog);
