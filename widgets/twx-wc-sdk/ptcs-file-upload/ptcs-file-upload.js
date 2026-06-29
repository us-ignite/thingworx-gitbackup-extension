import {LitElement, html, css} from 'lit';
import {ifDefined} from 'lit/directives/if-defined.js';
import {when} from 'lit/directives/when.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';
import 'ptcs-behavior-validate/ptcs-behavior-validate.js';

import 'ptcs-label/ptcs-label.js';
import 'ptcs-icon/ptcs-icon.js';
import 'ptcs-button/ptcs-button.js';
import 'ptcs-confirmation/ptcs-confirmation.js';

import './ptcs-file-select.js';
import './ptcs-file-upload-list.js';

import {parseAllowedFileTypes} from './file-library';

const STATUS_READY = 'ready';
const STATUS_COMPLETED = 'completed';
PTCS.FileUpload = class extends PTCS.BehaviorTooltip(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement)))) {
    static get styles() {
        return css`
                :host {
                    display: inline-flex;
                    flex-direction: column;

                    box-sizing: border-box;

                    overflow: auto;
                }

                [part=upload-button] {
                    max-width: 50%;
                    align-self: flex-start;
                }

                [part=delete-all] {
                    max-width: 50%;
                    align-self: flex-start;
                }

                [part=upload-button][hidden] {
                    visibility: hidden;
                }

                [part=delete-all][hidden] {
                    visibility: hidden;
                }

                [part=label] {
                    flex-shrink: 0;
                }

                [part=desc] {
                    flex-shrink: 0;
                }

                .footer {
                    display: flex;
                    justify-content: space-between;
                }

                :host([show-repository-selector][show-repository-path]) [part=repo] {
                    display: flex;
                }

                :host(:not([show-repository-selector])) [part=repos-list] {
                    display: none;
                }

                :host([show-repository-selector]:not([show-repository-path])) [part=repos-list] {
                    width: 100%;
                }
                :host([show-repository-selector][show-repository-path]) [part=repos-list] {
                    width: 50%;
                }

                :host(:not([show-repository-selector])[show-repository-path]) [part=path] {
                    width: 100%;
                }

                :host([show-repository-selector][show-repository-path]) [part=path] {
                    width: 50%;
                }

                :host(:not([show-repository-path])) [part=path] {
                    display: none;
                }

                :host(:focus) {
                    outline: none;
                }

                [part=file-select]:focus {
                    outline: none;
                }

                [part=file-upload-list]:focus {
                    outline: none;
                }`;
    }

    render() {
        return html`
            ${when(this.title, () => html`<ptcs-label part="label" id="title" .label=${this.title}
               variant=${this.titleStyle}></ptcs-label>`)}

            ${when(this.description, () => html`<ptcs-label part="desc" .label=${this.description} variant=${this.descriptionStyle}
               multi-line></ptcs-label>`)}

            ${when(this.showRepositorySelector || this.showRepositoryPath, () => html`<div part="repo">
                ${when(this.showRepositorySelector, () => html`<ptcs-dropdown part="repos-list" .label=${this.fileRepoLabel}
                   .items=${this.repositoryList} .selectedValue=${this.repo} @selected-value-changed=${this._selectedValueChangedEv}
                   .disabled=${this.disabled} tabindex=${ifDefined(this._delegatedFocus)}>
                </ptcs-dropdown>`)}
                ${when(this.showRepositoryPath, () => html`<ptcs-textfield part="path" .label=${this.pathLabel}
                   .text=${this.path} @text-changed=${this._textChangedEv} .disabled =${this.disabled} tabindex=${ifDefined(this._delegatedFocus)}>
                </ptcs-textfield>`)}
            </div>`)}

            <ptcs-file-select part="file-select" id="file-select" .fileSelectType=${this.fileUploadType}
                .browseButtonLabel=${this.browseButtonLabel} .browseButtonStyle=${this.browseButtonStyle} tabindex=${ifDefined(this._delegatedFocus)}
                .dropZoneLabel=${this.dropZoneLabel} .dropZoneIcon=${this.dropZoneIcon} .dropZoneHeight=${this.dropZoneHeight}
                .disabled =${this._fileSelectIsDisabled(this.disabled, this.disableInstantUpload, this.uploadInProgress)}
                ._stayUnvalidated=${this._stayUnvalidated} @_stay-unvalidated-changed=${this._stayUnvalidatedEv} @file-changed=${this._fileChangedEv}
                .allowedFileTypes=${this._parseAllowedFileTypes(this.allowedFileTypes)}
                .allowedFileTypesMessage=${this.allowedFileTypesMessage} .allowedFileTypesMessageDetails=${this.allowedFileTypesMessageDetails}
                ._confirmationDialog=${this._confirmationDialog} .singleFileSelection=${this.singleFileSelection}>
            </ptcs-file-select>

            <ptcs-file-upload-list part="file-upload-list" id="file-upload-list" .uploadManager=${this.uploadManager}
                ?hidden=${this._hideFilesList(this.externalValidity, this.fileRequired, this._stayUnvalidated, this._hasFiles)}
                validity=${this.validity} @validity-changed=${this._validityChangedEv}
                .disableInstantUpload=${this.disableInstantUpload} .uploadedFileHeight=${this.uploadedFileHeight}
                tabindex=${ifDefined(this._delegatedFocus)} hide-validation-success .repo=${this.repo} .path=${this.path}
                .disabled =${this.disabled} .hideFileTypeIcon=${this.hideFileTypeIcon} @has-files-changed=${this._hasFilesChangedEv}
                ._filesToUploadStatus=${this._filesToUploadStatus} @_files-to-upload-status-changed=${this._filesToUploadStatusChangedEv}
                @_files-to-upload-changed=${this._filesToUploadChangedEv} @has-files-to-upload-changed=${this._hasFilesToUploadChangedEv}
                @upload-in-progress-changed=${this._uploadInProgressChangedEv}
                @file-names-changed=${this._fileNamesChangedEv} @full-paths-changed=${this._fullPathsChangedEv}
                .hideValidationError=${this.hideValidationError} .validationErrorIcon=${this.validationErrorIcon} hide-validation-criteria
                .validationMessage=${this.validationMessage} .validationCriteria=${this.validationCriteria}
                .allowedFileTypes=${this._parseAllowedFileTypes(this.allowedFileTypes)} .allowedFileTypesMessage=${this.allowedFileTypesMessage}
                .allowedFileTypesMessageDetails=${this.allowedFileTypesMessageDetails} ._confirmationDialog=${this._confirmationDialog}
                .fileRequired=${this.fileRequired} .fileRequiredMessage=${this.fileRequiredMessage} ._stayUnvalidated=${this._stayUnvalidated}
                .maxNumberOfFiles=${this.maxNumberOfFiles} .maxNumberOfFilesFailureMessage=${this.maxNumberOfFilesFailureMessage}
                .maxFileSize=${this.maxFileSize} .maxFileSizeFailureTitle=${this.maxFileSizeFailureTitle}
                .maxFileSizeFailureMessage=${this.maxFileSizeFailureMessage} .maxUploadSize=${this.maxUploadSize}
                .maxUploadSizeFailureTitle=${this.maxUploadSizeFailureTitle} .maxUploadSizeFailureMessage=${this.maxUploadSizeFailureMessage}
                .fileUploadErrorMessage=${this.fileUploadErrorMessage} .fileUploadErrorDetails=${this.fileUploadErrorDetails}
                .replaceLabel=${this.replaceLabel} .cancelLabel=${this.cancelLabel} .replaceActionLabel=${this.replaceActionLabel}
                .replaceFileTitle=${this.replaceFileTitle} .replaceFileMsg=${this.replaceFileMsg}
                .replaceMultiFileTitle=${this.replaceMultiFileTitle} .replaceMultiFileMsg=${this.replaceMultiFileMsg}
                .deleteActionLabel=${this.deleteActionLabel} .deleteFileTitle=${this.deleteFileTitle}
                .deleteFileMsg=${this.deleteFileMsg} .extraValidation=${this.extraValidation} .externalValidity=${this.externalValidity}
                .validationOutput=${this.validationOutput} @validation-output-changed=${this._validationOutputChangedEv}
                .removeFileTooltip=${this.removeFileTooltip}>
            </ptcs-file-upload-list>

            ${when(!this._footerIsHidden(this.disableInstantUpload, this.showUploadButton, this._hasFiles, this.showDeleteAllButton), () =>
        html`<div class="footer" part="footer">
                  <ptcs-button part="upload-button" id="upload-button" .label=${this.uploadButtonLabel} variant=${this.uploadButtonVariant}
                   ?hidden=${this._uploadIsHidden(this.disableInstantUpload, this.showUploadButton)} tabindex=${ifDefined(this._delegatedFocus)}
                   .disabled =${this._uploadIsDisabled(this.disabled, this._hasFiles, this.validity, this.externalValidity)} @click=${this.uploadAll}>
                  </ptcs-button>

            ${when(!this._deleteAllIsHidden(this._hasFiles, this.showDeleteAllButton), () =>
        html`<ptcs-link part="delete-all" id="delete-all" .label=${this.deleteAllButtonLabel} tabindex=${ifDefined(this._delegatedFocus)}
                         .disabled =${this.disabled} @click=${this.deleteAllClick}></ptcs-link>`)}
                </div>`)}

            <ptcs-confirmation id="dlg"></ptcs-confirmation>
        `;
    }

    static get is() {
        return 'ptcs-file-upload';
    }

    static get properties() {
        return {
            disabled: {
                type:    Boolean,
                reflect: true
            },

            uploadManager: {
                type:      Object,
                attribute: 'upload-manager'
            },

            showRepositoryPath: {
                type:      Boolean,
                value:     false,
                reflect:   true,
                attribute: 'show-repository-path'
            },

            showRepositorySelector: {
                type:      Boolean,
                value:     false,
                reflect:   true,
                attribute: 'show-repository-selector'
            },

            repositoryList: {
                type:      Array,
                attribute: 'repository-list'
            },

            // select/drag
            fileUploadType: {
                type:      String,
                value:     'drag',
                reflect:   true,
                attribute: 'file-upload-type'
            },

            fileRepoLabel: {
                type:      String,
                value:     'File Repository',
                attribute: 'file-repo-label'
            },

            replaceLabel: {
                type:      String,
                value:     'Replace',
                attribute: 'replace-label'
            },

            replaceFileTitle: {
                type:      String,
                value:     'Replace File',
                attribute: 'replace-file-title'
            },

            replaceMultiFileTitle: {
                type:      String,
                value:     'Replace Files',
                attribute: 'replace-multi-file-title'
            },

            replaceFileMsg: {
                type:      String,
                value:     '${filename} already exists. Replace this file or cancel and keep the current version.',
                attribute: 'replace-file-msg'
            },

            replaceMultiFileMsg: {
                type:      String,
                value:     'The following files already exist: ${filenames}. Replace these files or cancel and keep the current version.',
                attribute: 'replace-multi-file-msg'
            },

            deleteFileTitle: {
                type:      String,
                value:     'Remove files?',
                attribute: 'delete-file-title'
            },

            deleteFileMsg: {
                type:      String,
                value:     'If you remove files, you will not be able to undo the action.',
                attribute: 'delete-file-msg'
            },

            removeFileTooltip: {
                type:      String,
                value:     'Remove file',
                attribute: 'remove-file-tooltip'
            },

            cancelLabel: {
                type:      String,
                value:     'Cancel',
                attribute: 'cancel-label'
            },

            replaceActionLabel: {
                type:      String,
                value:     'Replace',
                attribute: 'replace-action-label'
            },

            deleteActionLabel: {
                type:      String,
                value:     'Remove',
                attribute: 'delete-action-label'
            },

            pathLabel: {
                type:      String,
                value:     'Path',
                attribute: 'path-label'
            },

            title: {
                type:  String,
                value: 'Upload'
            },

            titleStyle: {
                type:      String,
                value:     'label',
                attribute: 'title-style'
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

            uploadButtonLabel: {
                type:      String,
                value:     'Upload',
                attribute: 'upload-button-label'
            },

            uploadButtonVariant: {
                type:      String,
                value:     'primary',
                attribute: 'upload-button-variant'
            },

            showUploadButton: {
                type:      Boolean,
                value:     false,
                reflect:   true,
                attribute: 'show-upload-button'
            },

            description: {
                type:  String,
                value: ''
            },

            descriptionStyle: {
                type:      String,
                value:     'body',
                attribute: 'description-style'
            },

            dropZoneLabel: {
                type:      String,
                value:     'Drag files here or click to browse',
                attribute: 'drop-zone-label'
            },

            dropZoneIcon: {
                type:      String,
                attribute: 'drop-zone-icon'
            },

            dropZoneHeight: {
                type:      Number,
                attribute: 'drop-zone-height'
            },

            disableInstantUpload: {
                type:      Boolean,
                attribute: 'disable-instant-upload'
            },

            maxHeight: {
                type:      Number,
                observer:  '_maxHeightChanged',
                attribute: 'max-height'
            },

            hideFileTypeIcon: {
                type:      Boolean,
                attribute: 'hide-file-type-icon'
            },

            deleteAllButtonLabel: {
                type:      String,
                value:     'Remove All',
                attribute: 'delete-all-button-label'
            },

            showDeleteAllButton: {
                type:      Boolean,
                value:     false,
                attribute: 'show-delete-all-button'
            },

            _hasFiles: {
                type: Boolean
            },

            _hasFilesToUpload: {
                type: Boolean
            },

            uploadedFileHeight: {
                type:      Number,
                attribute: 'uploaded-file-height'
            },

            repo: {
                type:   String,
                notify: true
            },

            path: {
                type:   String,
                notify: true
            },

            filesList: {
                type:  Array,
                value: () => []
            },

            _filesToUpload: {
                type:     Array,
                observer: '_filesToUploadChanged'
            },

            uploadInProgress: {
                type:     Boolean,
                readOnly: true
            },

            fileNames: {
                type:     String,
                readOnly: true,
                notify:   true
            },

            fullPaths: {
                type:     String,
                readOnly: true,
                notify:   true
            },

            validationMessage: {
                type:      String,
                value:     'File Upload Error(s)',
                attribute: 'validation-message'
            },

            validationCriteria: {
                type:      String,
                attribute: 'validation-criteria'
            },

            allowedFileTypes: {
                type:      String,
                attribute: 'allowed-file-types'
            },

            allowedFileTypesMessage: {
                type:      String,
                value:     'File type is not allowed',
                attribute: 'allowed-file-types-message'
            },

            allowedFileTypesMessageDetails: {
                type:      String,
                value:     'Select a supported file type',
                attribute: 'allowed-file-types-message-details'
            },

            fileRequired: {
                type:      Boolean,
                attribute: 'file-required'
            },

            fileRequiredMessage: {
                type:      String,
                attribute: 'file-required-message'
            },

            singleFileSelection: {
                type:      Boolean,
                computed:  '_singleFileSelection(maxNumberOfFiles)',
                attribute: 'single-file-selection'
            },

            maxNumberOfFiles: {
                type:      Number,
                attribute: 'max-number-of-files'
            },

            maxNumberOfFilesFailureMessage: {
                type:      String,
                attribute: 'max-number-of-files-failure-message'
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

            fileUploadErrorMessage: {
                type:      String,
                attribute: 'file-upload-error-message'
            },

            fileUploadErrorDetails: {
                type:      String,
                attribute: 'file-upload-error-details'
            },

            // Custom validation function that complements the existing client-side validation
            extraValidation: {
                type:      Function,
                attribute: 'extra-validation'
            },

            externalValidity: {
                type:      String,
                attribute: 'external-validity'
            },

            validity: {
                type:    String,
                notify:  true,
                reflect: true
            },

            validationOutput: {
                type:      String,
                notify:    true,
                attribute: 'validation-output'
            },

            hideValidationError: {
                type:      Boolean,
                attribute: 'hide-validation-error'
            },

            validationErrorIcon: {
                type:      String,
                attribute: 'validation-error-icon'
            },

            _confirmationDialog: {
                type: Element
            },

            _filesToUploadStatus: {
                type:   Number,
                notify: true
            },

            _delegatedFocus: {
                type:  String,
                state: true
            }
        };
    }

    ready() {
        super.ready();

        if (this.dropZoneIcon === undefined) {
            this.dropZoneIcon = 'cds:icon_add';
        }

        this._confirmationDialog = this.$['dlg'];
    }

    _hideFilesList(externalValidity, fileRequired, _stayUnvalidated, _hasFiles) {
        return externalValidity !== 'invalid' && !fileRequired ? !_hasFiles : _stayUnvalidated;
    }

    _fileSelectIsDisabled(disabled, disableInstantUpload, uploadInProgress) {
        return disabled || (disableInstantUpload && uploadInProgress);
    }

    _uploadIsDisabled(disabled, _hasFiles, validity, externalValidity) {
        return disabled || !_hasFiles || validity === 'invalid';
    }

    _deleteAllIsHidden(_hasFiles, showDeleteAllButton) {
        return !(_hasFiles && showDeleteAllButton);
    }

    _uploadIsHidden(disableInstantUpload, showUploadButton) {
        return !disableInstantUpload || !showUploadButton;
    }

    _footerIsHidden(disableInstantUpload, showUploadButton, _hasFiles, showDeleteAllButton) {
        return this._uploadIsHidden(this.disableInstantUpload, this.showUploadButton) &&
          this._deleteAllIsHidden(this._hasFiles, this.showDeleteAllButton);
    }

    _disabled(disabled, disableInstantUpload) {
        return disabled || !disableInstantUpload;
    }

    _selectedValueChangedEv(ev) {
        this.repo = ev.detail.value;
    }

    _textChangedEv(ev) {
        this.path = ev.detail.value;
    }

    _filesToUploadChangedEv(ev) {
        this._filesToUpload = [...ev.detail.value];
    }

    _filesToUploadStatusChangedEv(ev) {
        this._filesToUploadStatus = ev.detail.value;
    }

    _hasFilesToUploadChangedEv(ev) {
        this._hasFilesToUpload = ev.detail.value;
    }

    _validityChangedEv(ev) {
        this.validity = ev.detail.value;
    }

    _validationOutputChangedEv(ev) {
        this.validationOutput = ev.detail.value;
    }

    _stayUnvalidatedEv(ev) {
        this._filesToUploadStatus = Date.now(); // Trigger validation
        this._stayUnvalidated = ev.detail.value;
    }

    _fileChangedEv(ev) {
        this.$['file-upload-list'].add(ev.detail.value);
    }

    _uploadInProgressChangedEv(ev) {
        this._setUploadInProgress(ev.detail.value);
    }

    _fileNamesChangedEv(ev) {
        this._setFileNames(ev.detail.value);
    }

    _fullPathsChangedEv(ev) {
        this._setFullPaths(ev.detail.value);
    }

    _hasFilesChangedEv(ev) {
        this._hasFiles = ev.detail.value;
    }

    // Allow access of the private member _stayUnvalidated the same way as you can in components implementing the validation behavior
    enableValidationMessage(enable) {
        this._filesToUploadStatus = Date.now(); // Trigger validation
        this._stayUnvalidated = (enable === false);
    }

    uploadAll() {
        if (!this._uploadIsDisabled(this.disabled, this._hasFiles, this.validity, this.externalValidity)) {
            this.$['file-upload-list'].uploadAll();
            this._stayUnvalidated = true;
        }
        // eslint-disable-next-line max-len
        const uniqueFiles = new Set([...this.filesList, ...this._filesToUpload.filter(file => file.status !== STATUS_COMPLETED).map(file => file.file)]);
        this.filesList = Array.from(uniqueFiles);
        this._dispatchFilesListChangedEvent();
    }

    deleteAll(noconfirm) {
        this.$['file-upload-list'].deleteAll(noconfirm);
    }

    deleteAllClick() {
        this.$['file-upload-list'].deleteAll();
    }

    clearFilesList() {
        this.filesList = [];
        this._dispatchFilesListChangedEvent();
    }

    clearFileList() {
        this.$['file-upload-list'].clearFileList();
    }

    _maxHeightChanged(maxHeight) {
        this.style.maxHeight = maxHeight ? `${maxHeight}px` : '';
    }

    _singleFileSelection(maxNumberOfFiles) {
        return Number(maxNumberOfFiles) === 1;
    }

    _parseAllowedFileTypes(allowedFileTypes) {
        return parseAllowedFileTypes(allowedFileTypes);
    }

    _filesToUploadChanged(_filesToUpload) {
        if (!this.disableInstantUpload) {
            // eslint-disable-next-line max-len
            const uniqueFiles = new Set([...this.filesList, ...this._filesToUpload.filter(file => file.status !== STATUS_READY && file.status !== STATUS_COMPLETED).map(file => file.file)]);
            this.filesList = Array.from(uniqueFiles);
            this._dispatchFilesListChangedEvent();
        }
    }

    _dispatchFilesListChangedEvent() {
        this.dispatchEvent(new CustomEvent('files-list-changed', {
            bubbles:  true,
            composed: true,
            detail:   this.filesList
        }));
    }
};

customElements.define(PTCS.FileUpload.is, PTCS.FileUpload);
