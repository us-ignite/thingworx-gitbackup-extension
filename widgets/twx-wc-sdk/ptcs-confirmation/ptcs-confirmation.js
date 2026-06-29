import {LitElement, html, css} from 'lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';
import './ptcs-confirmation-dialog.js';

PTCS.Confirmation = class extends PTCS.BehaviorStyleable(LitElement) {
    static get styles() {
        return css`
            :host {
                display: none;
            }
        `;
    }

    render() {
        return html`
            <ptcs-confirmation-dialog .mode=${this.mode} part="popdialog" .titleText=${this.titleText} .messageText=${this.messageText}
            ?displayCloseButton=${this.displayCloseButton} ?displaySecondaryAction=${this.displaySecondaryAction}
            .primaryActionLabel=${this.primaryActionLabel} .primaryButtonStyle=${this.primaryButtonStyle}
            .actionButtonTooltipField=${this.actionButtonTooltipField} .secondaryActionLabel=${this.secondaryActionLabel}
            .secondButtonTooltipField=${this.secondButtonTooltipField} .primaryActionIcon=${this.primaryActionIcon}
            .secondaryActionIcon=${this.secondaryActionIcon} .cancelActionIcon=${this.cancelActionIcon}
            .actionButtonTooltipIcon=${this.actionButtonTooltipIcon} .secondButtonTooltipIcon=${this.secondButtonTooltipIcon}
            .cancelButtonTooltipIcon=${this.cancelButtonTooltipIcon} .cancelActionLabel=${this.cancelActionLabel}
            .cancelButtonTooltipField=${this.cancelButtonTooltipField} .closeButtonTooltipField=${this.closeButtonTooltipField}
            .actionPosition=${this.actionPosition} @primary-action=${this._primary} @secondary-action=${this._secondary}
            @close-action=${this.close} .confWidth=${this.confWidth} .confHeight=${this.confHeight} ?hideCancelAction=${this.hideCancelAction}>
            </ptcs-confirmation-dialog>
        `;
    }

    static get is() {
        return 'ptcs-confirmation';
    }

    static get properties() {
        return {

            mode: {
                type:    String,
                reflect: true
            },

            titleText: {
                type:      String,
                reflect:   true,
                attribute: 'title-text'
            },

            messageText: {
                type:      String,
                reflect:   true,
                attribute: 'message-text'
            },

            actionPosition: {
                type:      String,
                attribute: 'action-position'
            },

            displayCloseButton: {
                type:      Boolean,
                attribute: 'display-close-button'
            },

            primaryButtonStyle: {
                type:      String,
                attribute: 'primary-button-style'
            },

            primaryActionIcon: {
                type:      String,
                attribute: 'primary-action-icon'
            },

            cancelActionIcon: {
                type:      String,
                attribute: 'cancel-action-icon'
            },

            secondaryActionIcon: {
                type:      String,
                attribute: 'secondary-action-icon'
            },

            displaySecondaryAction: {
                type:      Boolean,
                attribute: 'display-secondary-action'
            },

            /* Button text labels */
            primaryActionLabel: {
                type:      String,
                attribute: 'primary-action-label'
            },

            secondaryActionLabel: {
                type:      String,
                attribute: 'secondary-action-label'
            },

            hideCancelAction: {
                type:      Boolean,
                attribute: 'hide-cancel-action'
            },

            cancelActionLabel: {
                type:      String,
                attribute: 'cancel-action-label'
            },
            confWidth: {
                type:      String,
                attribute: 'conf-width'
            },
            confHeight: {
                type:      String,
                attribute: 'conf-height'
            },

            actionButtonTooltipField: {
                type:      String,
                attribute: 'action-button-tooltip-field'
            },
            actionButtonTooltipIcon: {
                type:      String,
                attribute: 'action-button-tooltip-icon'
            },
            secondButtonTooltipField: {
                type:      String,
                attribute: 'second-button-tooltip-field'
            },
            secondButtonTooltipIcon: {
                type:      String,
                attribute: 'second-button-tooltip-icon'
            },
            closeButtonTooltipField: {
                type:      String,
                attribute: 'close-button-tooltip-field'
            },
            cancelButtonTooltipField: {
                type:      String,
                attribute: 'cancel-button-tooltip-field'
            },
            cancelButtonTooltipIcon: {
                type:      String,
                attribute: 'cancel-button-tooltip-icon'
            }
        };
    }

    constructor() {
        super();
        this.mode = 'closed';
        this.titleText = '';
        this.messageText = '';
        this.actionPosition = 'left';
        this.primaryButtonStyle = 'primary';
        this.primaryActionIcon = null;
        this.secondaryActionIcon = null;
        this.cancelActionIcon = null;
        this.displayCloseButton = false;
        this.displaySecondaryAction = false;
        this.hideCancelAction = false;
        this.primaryActionLabel = null;
        this.secondaryActionLabel = null;
        this.cancelActionLabel = null;
        this.confWidth = '600px';
        this.confHeight = '260px';
        this.actionButtonTooltipField = null;
        this.actionButtonTooltipIcon = null;
        this.secondButtonTooltipField = null;
        this.secondButtonTooltipIcon = null;
        this.closeButtonTooltipField = null;
        this.cancelButtonTooltipField = null;
        this.cancelButtonTooltipIcon = null;

        const uniqueSuffixForId = performance.now().toString().replace('.', '');
        this._dialogId = 'ptcs-confirmation-dialog-' + uniqueSuffixForId;
    }

    connectedCallback() {
        super.connectedCallback();
        if (this._dialog) {
            this._dialog.__saSa = this.__saSa;
            document.body.appendChild(this._dialog);
        }
    }

    disconnectedCallback() {
        if (this._dialog) {
            document.body.removeChild(this._dialog);
        }
        super.disconnectedCallback();
    }

    updated(changedProperties) {
        super.updated(changedProperties);

        if (changedProperties.has('mode') && changedProperties.get('mode') !== undefined ||
            changedProperties.has('hideCancelAction') && changedProperties.get('hideCancelAction') !== undefined ||
            changedProperties.has('displaySecondaryAction') && changedProperties.get('displaySecondaryAction') !== undefined) {
            this._initDialog();
        }
    }

    _initDialog() {
        if (!this._dialog) {
            const dialog = this.shadowRoot.querySelector('ptcs-confirmation-dialog');
            dialog.remove();
            dialog.__saSa = this.__saSa;
            this._dialog = document.body.appendChild(dialog);
            this._dialog.setAttribute('id', this._dialogId);
        }
    }

    open() {
        this.mode = 'open';
    }

    _primary() {
        this.dispatchEvent(new CustomEvent('primary-action'), {
            bubbles:  true,
            composed: true
        });
        this.mode = 'closed';
    }

    _secondary() {
        this.dispatchEvent(new CustomEvent('secondary-action'), {
            bubbles:  true,
            composed: true
        });
        this.mode = 'closed';
    }

    close() {
        this.dispatchEvent(new CustomEvent('close-action'), {
            bubbles:  true,
            composed: true
        });
        this.mode = 'closed';
    }
};

customElements.define(PTCS.Confirmation.is, PTCS.Confirmation);
