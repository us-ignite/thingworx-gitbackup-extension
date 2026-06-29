import {LitElement, html, css} from 'lit';
import {ifDefined} from 'lit/directives/if-defined.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {styleMap} from 'lit/directives/style-map.js';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';

import 'ptcs-label/ptcs-label.js';
import 'ptcs-button/ptcs-button.js';
import 'ptcs-confirmation/ptcs-confirmation.js';

import {getDraggedFiles, allFilesInListAllowed, openValidationDialog} from './file-library';

PTCS.FileSelect = class extends PTCS.BehaviorTooltip(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement)))) {
    static get styles() {
        return css`
                [part=browse-button]{
                    max-width: 100%;
                    display: none;
                }

                :host([file-select-type=select]) [part=browse-button]{
                    display: inline-block;
                }

                :host([file-select-type=select]) [part=drop-zone]{
                    display: none;
                }

                [part=drop-zone] {
                    width: 100%;

                    display: flex;
                    flex-direction: column;
                    flex-wrap: wrap;

                    align-content: center;
                    align-items: center;
                    justify-content: center;

                    overflow: hidden;
                    box-sizing: border-box;
                }

                :host(:not([disabled])) [part=drop-zone] {
                    cursor: pointer;
            }`;
    }

    render() {
        const _dropZoneStyle = {height: this.dropZoneHeight + 'px'};

        return html`
            <div id="drop-zone" part="drop-zone" @drop=${this._dropEv} @dragover=${this._dragoverEv}
                 @touchend=${this._mouseDownEv} @mousedown=${this._mouseDownEv} style=${styleMap(_dropZoneStyle)}>
                 <ptcs-button part="add-button" id="add-button" variant="transparent"
                     .icon=${this.dropZoneIcon} .iconPlacement=${'left'} .contentAlign=${'center'}
                     mode="icon" no-tabindex .disabled=${this.disabled}>
                 </ptcs-button>
                <ptcs-label part="label" .label=${this.dropZoneLabel} multi-line .horizontalAlignment=${'center'}></ptcs-label>
            </div>
            <ptcs-button id="browse" part="browse-button" class="button" .label=${this.browseButtonLabel} variant=${this.browseButtonStyle}
                @touchend=${this._mouseDownEv} @mousedown=${this._mouseDownEv} @keydown=${this._keydownEv}
                .disabled=${this.disabled} tabindex=${ifDefined(this._delegatedFocus)}>
            </ptcs-button>
            <input id="browse-file" type="file" hidden ?multiple=${!this.singleFileSelection} accept=${this.allowedFileTypes}
                @click=${this._clickEv} @change=${this._changeEv}>`;
    }

    static get is() {
        return 'ptcs-file-select';
    }

    static get properties() {
        return {
            disabled: {
                type:    Boolean,
                reflect: true
            },


            allowedFileTypes: {
                type:      String,
                attribute: 'allowed-file-types'
            },

            allowedFileTypesMessage: {
                type:      String,
                attribute: 'allowed-file-types-message'
            },

            allowedFileTypesMessageDetails: {
                type:      String,
                attribute: 'allowed-file-types-message-details'
            },

            // select/drag
            fileSelectType: {
                type:      String,
                value:     'drag',
                reflect:   true,
                attribute: 'file-select-type'
            },

            file: {
                type:     Object,
                readOnly: true
            },

            browseButtonLabel: {
                type:      String,
                value:     'Browse',
                attribute: 'browse-button-label'
            },

            browseButtonStyle: {
                type:      String,
                value:     'tertiary',
                attribute: 'browse-button-style'
            },

            dropZoneLabel: {
                type:      String,
                value:     'Drag files here or click to browse',
                attribute: 'drop-zone-label'
            },

            dropZoneIcon: {
                type:      String,
                value:     'cds:icon_add',
                attribute: 'drop-zone-icon'
            },

            dropZoneHeight: {
                type:      Number,
                value:     96,
                attribute: 'drop-zone-height'
            },

            singleFileSelection: {
                type:      Boolean,
                value:     false,
                attribute: 'single-file-selection'
            },

            // Flag to signal that the component has been modified by the user
            _stayUnvalidated: {
                type:   Boolean,
                notify: true
            },

            _delegatedFocus: String
        };
    }

    ready() {
        super.ready();
        this._stayUnvalidated = true;

        this.addEventListener('keydown', ev => {
            if (!this.disabled && this.fileSelectType === 'drag' && (ev.key === 'Enter' || ev.key === ' ')) {
                this.$['browse-file'].click();
            }
        });

        document.addEventListener('dragleave', (e) => {
            const {posX, posY} = PTCS.getCoordinatesFromEvent(e);

            if (posX === undefined && posY === undefined) {
                // we are outside the page
                this.removeAttribute('drag-over-page');
            }
        });

        document.addEventListener('dragover', e => {
            if (this.disabled) {
                return;
            }

            const _dragObjectIsFile = (ev) => {
                if (ev.dataTransfer.items && ev.dataTransfer.items[0].kind === 'file') {
                    return true;
                }

                if (ev.dataTransfer.files.length > 0) {
                    return true;
                }

                return false;
            };

            if (!_dragObjectIsFile(e)) {
                return;
            }

            this.setAttribute('drag-over-page', '');

            const mm = () => {
                this.removeAttribute('drag-over-page');
                this.removeAttribute('drag-over');

                if (this.hasAttribute('not-allowed')) {
                    openValidationDialog(this._confirmationDialog, this.allowedFileTypesMessage, this.allowedFileTypesMessageDetails);
                    this.removeAttribute('not-allowed');
                }

                document.removeEventListener('mousemove', mm);
            };

            // Using "mousemove" event to clear the drop zone states is a workaround. I can't find an appropriate event
            // when I drop the file on an invalid target.
            document.addEventListener('mousemove', mm);

            const x = PTCS.getCoordinatesFromEvent(e).posX;
            const y = PTCS.getCoordinatesFromEvent(e).posY;

            if (this._insideDropZone(x, y)) {
                const draggedFiles = getDraggedFiles(e);
                const unknownFileTypeFound = draggedFiles.some(file => !file.type);

                if (!unknownFileTypeFound && !allFilesInListAllowed(draggedFiles, this.allowedFileTypes)) {
                    this.setAttribute('not-allowed', '');
                    e.dataTransfer.dropEffect = 'none';
                }
                this.setAttribute('drag-over', '');
            } else {
                this.removeAttribute('drag-over');
                this.removeAttribute('not-allowed');
            }
        });
    }

    _mouseDownEv(ev) {
        if (this.disabled || PTCS.wrongMouseButton(ev) || ev.defaultPrevented) {
            return;
        }
        ev.preventDefault();

        if (typeof PTCS.resetMouseCanceller === 'function') {
            // Polymer dependency - Polymer sets a 2500 ms timeout on click events when processing touch events, reset it.
            PTCS.resetMouseCanceller();
        }
        this.$['browse-file'].click();
    }

    assignFile(file) {
        this._setFile(file);
        this.dispatchEvent(new CustomEvent('file-changed', {detail: {value: file}}));
    }

    _changeEv() {
        const files = this.$['browse-file'].files;

        // Not an array, so doesn't work with forEach
        for (let i = 0; i < files.length; i++) {
            this.assignFile(files[i]);
        }
    }

    _keydownEv(ev) {
        if (!this.disabled && (ev.key === 'Enter' || ev.key === ' ')) {
            this.$['browse-file'].click();
        }
    }

    _clickEv() {
        // If a user selects the same file, the "change" event will not be triggered
        // because the current value is the same as the previous. To prevent this problem I'm erasing the value first.
        this.$['browse-file'].value = '';

        // Change pristine state when closing the browse-file input
        document.body.onfocus = () => {
            this._stayUnvalidated = false;
        };
    }

    _dropEv(ev) {
        if (this.disabled) {
            return;
        }

        // dragging is canceled
        this.removeAttribute('drag-over-page');
        this.removeAttribute('drag-over');

        // Prevent default behavior (Prevent file from being opened)
        ev.preventDefault();

        const draggedFiles = getDraggedFiles(ev);
        if (allFilesInListAllowed(draggedFiles, this.allowedFileTypes)) {
            draggedFiles.forEach(file => this.assignFile(file));
        } else {
            this.setAttribute('not-allowed', '');
        }

        this._stayUnvalidated = false;
    }

    _insideDropZone(x, y) {
        const dzR = this.$['drop-zone'].getBoundingClientRect();

        return x >= dzR.left && x <= dzR.right && y >= dzR.top && y <= dzR.bottom;
    }

    _dragoverEv(ev) {
        // Prevent default behavior (Prevent file from being opened)
        ev.preventDefault();
    }
};

customElements.define(PTCS.FileSelect.is, PTCS.FileSelect);
