import {LitElement, html, css} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {ifDefined} from 'lit/directives/if-defined.js';
import {when} from 'lit/directives/when.js';
import {styleMap} from 'lit/directives/style-map.js';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';
import 'ptcs-behavior-validate/ptcs-behavior-validate.js';
import 'ptcs-label/ptcs-label.js';
import 'ptcs-link/ptcs-link.js';
import 'ptcs-icon/ptcs-icon.js';
import 'ptcs-button/ptcs-button.js';
import 'ptcs-confirmation/ptcs-confirmation.js';

import {fileLabel, getFileType, isFileTypeAllowed} from './file-library';

PTCS.FileUploadListItem = class extends PTCS.BehaviorValidate(PTCS.BehaviorTooltip(
    PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))))) {
    static get styles() {
        return css`
                :host {
                    display: inline-block;
                    box-sizing: border-box;
                    width: 100%;
                }

                :host(:focus) {
                    outline: none;
                }

                :host([uploading]) [part=file-progress] {
                    display: block;
                }

                :host([uploading]) [part=file-progress-bar] {
                    display: block;
                }

                :host([failed]) [part=file-replace] {
                    display: block;
                }

                :host([validity=invalid]) [part=file-replace] {
                    display: block;
                }

                [part=file-replace], [part=file-progress], [part=file-progress-bar] {
                    display: none;
                }

                .file-item {
                    display: inline-flex;
                    flex-direction: column;
                    justify-content: center;
                    width: 100%;

                    overflow: hidden;
                }

                [part=file-to-upload] {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                }

                .file-info {
                    display: flex;
                    align-items: center;

                    overflow: hidden;
                }

                .file-actions {
                    display: flex;
                    align-items: center;

                    overflow: hidden;

                    flex: 0 0 auto;
                }

                [part=file-progress-bar] {
                    width: 100%;
                }

                [part=file-progress-bar] > span {
                    display: block;
                    height: 100%;
                    position: relative;
                    overflow: hidden;
                }

                [part=file-name] {
                    flex: 0 1 auto;
                }

                [part=file-icon] {
                    flex: 0 0 auto;
                }

                [part=file-icon][hidden] {
                    display: none;
                }`;
    }

    render() {
        const _fileToUploadStyle = {height: this.uploadedFileHeight ? this.uploadedFileHeight + 'px' : '48px'};
        const _progressBarStyle =  {width: this._progressAsPercent};
        const _svgIcon = () => html`<svg part="file-icon" xmlns="http://www.w3.org/2000/svg" width="20" height="24" fill="none" viewBox="0 0 20 24">
                  <path fill-rule="evenodd" d="M17 22.5H5A1.5 1.5 0 013.5 21V3A1.5 1.5 0 015 1.5h9v4A1.5 1.5 0 0015.5
                        7h3v14a1.5 1.5 0 01-1.5 1.5zM20 6l-5-6H5a3 3 0 00-3 3v18a3 3 0 003 3h12a3 3 0 003-3V6z" clip-rule="evenodd"/>
                  <g>
                    <rect part="file-icon-rectangle" width="17" height="8" y="12" rx="1"/>
                    <text part="file-icon-text" y="18" x=${this._getIconTextX(this.item.type)}>${this.item.type}</text>
                  </g>
                </svg>`;

        return html`<div class="file-item" part="file-item" style=${styleMap(_fileToUploadStyle)}>
                <div id="file-to-upload" part="file-to-upload">
                    <div class="file-info">
                      ${when(!this.hideFileTypeIcon, _svgIcon)}
                      <ptcs-label part="file-name" .label=${this._fileLabel(this.item.file, this.item.filename)} variant="body"></ptcs-label>
                    </div>
                    <div class="file-actions">
                        <ptcs-label class="progress" part="file-progress" .label=${this._progressAsPercent} variant="caption"></ptcs-label>
                        <ptcs-link id="file-replace" part="file-replace" .label=${this.replaceLabel} @click=${this._clickBrowseFile}
                             .disabled=${this.disabled} tabindex=${ifDefined(this._delegatedFocus)}>
                        </ptcs-link>
                        <input id="browse-file" type="file" hidden accept=${this.allowedFileTypes} @click=${this._clickEv} @change=${this._changeEv}>
                        <div part="hit-area" @click=${this._cancel}>
                            <ptcs-button part="file-cancel" icon="cds:icon_close_mini" variant="small" .disabled=${this.disabled}
                              .tooltip=${this._getCancelTooltip(this.item.status)} tabindex=${ifDefined(this._delegatedFocus)}>
                            </ptcs-button>
                        </div>
                    </div>
                </div>
                <div part="file-progress-bar">
                    <span part="file-progress-bar-progress" style=${styleMap(_progressBarStyle)}></span>
                </div>
            </div>`;
    }

    static get is() {
        return 'ptcs-file-upload-list-item';
    }

    static get properties() {
        return {
            disabled: {
                type:    Boolean,
                reflect: true
            },

            item: {
                type:        Object,
                observer:    '_itemChanged',
                observeWhen: 'immediate',
                validate:    '_validateListItem(item, allowedFileTypes, maxFileSize, maxUploadSize, failed)'
            },

            progress: {
                type:    Number,
                reflect: true
            },

            // Value of progress with '%' suffix
            _progressAsPercent: {
                type: String
            },

            failed: {
                type:    Boolean,
                reflect: true
            },

            maxFileSize: {
                type:      Number,
                attribute: 'max-file-size'
            },

            maxFileSizeFailureTitle: {
                type:      String,
                attribute: 'max-file-size-failure-title'
            },

            maxFileSizeFailureMessage: {
                type:      String,
                attribute: 'max-file-size-failure-message'
            },

            maxUploadSize: {
                type:      Number,
                attribute: 'max-upload-size'
            },

            maxUploadSizeFailureTitle: {
                type:      String,
                attribute: 'max-upload-size-failure-title'
            },

            maxUploadSizeFailureMessage: {
                type:      String,
                attribute: 'max-upload-size-failure-message'
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

            hideFileTypeIcon: {
                type:      Boolean,
                reflect:   true,
                attribute: 'hide-file-type-icon'
            },

            uploadedFileHeight: {
                type:      Number,
                attribute: 'uploaded-file-height'
            },

            replaceLabel: {
                type:      String,
                value:     'Replace',
                attribute: 'replace-label'
            },

            removeFileTooltip: {
                type:      String,
                value:     'Remove file',
                attribute: 'remove-file-tooltip'
            },

            cancelUploadTooltip: {
                type:      String,
                value:     'Cancel upload',
                attribute: 'cancel-upload-tooltip'
            },

            _delegatedFocus: String
        };
    }

    _getIconTextX(type) {
        return type.length > 3 ? 1 : 3;
    }

    _getCancelTooltip(status) {
        return status === 'uploading' ? this.cancelUploadTooltip : this.removeFileTooltip;
    }

    _fileLabel(file, filename) {
        return fileLabel(file, filename);
    }

    _sizeLimit(filesize, maxMB) {
        return filesize <= Number(maxMB) * Math.pow(1024, 2);
    }

    _computeIsFileSizeAllowed(item, maxFileSize) {
        return maxFileSize && item ? this._sizeLimit(item.file.size, maxFileSize) : true;
    }

    _computeIsUploadSizeAllowed(item, maxUploadSize) {
        return maxUploadSize && item ? this._sizeLimit(item.file.size, maxUploadSize) : true;
    }

    _computeIsFileTypeAllowed(item, allowedFileTypes) {
        return allowedFileTypes && item ? isFileTypeAllowed(allowedFileTypes, getFileType(item.file)) : true;
    }

    _validateListItem(item, allowedFileTypes, maxFileSize, maxUploadSize, failed) {
        const messages = [];

        if (!this._computeIsFileTypeAllowed(item, allowedFileTypes)) {
            this.validationMessage = this.allowedFileTypesMessage ? this.allowedFileTypesMessage : this.validationMessage;
            messages.push(this.allowedFileTypesMessageDetails);
        }

        if (!this._computeIsFileSizeAllowed(item, maxFileSize)) {
            this.validationMessage = this.maxFileSizeFailureTitle;
            const msg = PTCS.replaceStringTokens(this.maxFileSizeFailureMessage, {value: maxFileSize});
            messages.push(this.maxFileSizeFailureMessage ? msg.join('. ') : this.validationMessage);
        }

        if (!this._computeIsUploadSizeAllowed(item, maxUploadSize)) {
            this.validationMessage = this.maxUploadSizeFailureTitle;
            const msg = PTCS.replaceStringTokens(this.maxUploadSizeFailureMessage, {value: maxUploadSize});
            messages.push(this.maxUploadSizeFailureMessage ? msg.join('. ') : this.validationMessage);
        }

        if (failed) {
            this.validationMessage = this.fileUploadErrorMessage ? this.fileUploadErrorMessage : this.validationMessage;
            messages.push(this.fileUploadErrorDetails);
        }

        return messages.length ? messages : true;
    }

    _insertValidationMessage(messageElement) {
        this.shadowRoot.appendChild(messageElement);
    }

    _cancel() {
        this.dispatchEvent(new CustomEvent('cancel-file', {
            bubbles:  true,
            composed: true
        }));
    }

    _itemChanged(item) {
        if (!item) {
            return;
        }
        PTCS.setbattr(this, 'completed', item.status === 'completed');
        PTCS.setbattr(this, 'uploading', item.status === 'uploading');
        this.failed = item.status === 'failed';
        this.setAttribute('status', item.status);

        this.progress = item.progress;
        this._progressAsPercent = item.progress + '%';
    }

    _clickBrowseFile() {
        if (this.disabled) {
            return;
        }

        this.$['browse-file'].click();
    }

    _clickEv() {
        // If a user selects the same file, the "change" event will not be triggered because the current value
        // is the same as the previous. To prevent this problem I'm erasing the value first.
        this.$['browse-file'].value = '';
    }

    _changeEv() {
        this.dispatchEvent(new CustomEvent('replace-file', {
            bubbles:  true,
            composed: true,
            detail:   {
                file: this.$['browse-file'].files[0]
            }
        }));
    }
};

customElements.define(PTCS.FileUploadListItem.is, PTCS.FileUploadListItem);
