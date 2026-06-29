import {LitElement, html} from 'lit';
import {ifDefined} from 'lit/directives/if-defined.js';
import {map} from 'lit/directives/map.js';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-behavior-tooltip/ptcs-behavior-tooltip.js';
import 'ptcs-behavior-validate/ptcs-behavior-validate.js';

import './ptcs-file-upload-list-item.js';
import {fileLabel, getFileType, isFileTypeAllowed, allFilesInListAllowed, exceedMaxFileSize, openValidationDialog} from './file-library';

const MULTI_FILE_DELIMITER = '|';

const STATUS_UPLOADING = 'uploading';
const STATUS_READY = 'ready';
const STATUS_COMPLETED = 'completed';
const STATUS_FAILED = 'failed';
const STATUS_CANCELED = 'canceled';

const EVENT_DIRECT_UPLOAD_FAILED = 'direct-upload-failed';
const EVENT_DIRECT_UPLOAD_COMPLETE = 'direct-upload-complete';
const EVENT_FAILED = 'upload-failed';
const EVENT_COMPLETE = 'upload-complete';
const EVENT_STARTED = 'upload-started';
const EVENT_DELETE = 'delete-file';
const EVENT_CANCEL = 'cancel-file';

PTCS.FileUploadList = class extends PTCS.BehaviorValidate(PTCS.BehaviorTooltip(
    PTCS.BehaviorFocus(PTCS.BehaviorStyleable(L2Pw(LitElement))))) {

    render() {
        return html`
            <div id="files-list" part="files-list">
              ${map(this._filesToUpload, (item) => html`
                    <ptcs-file-upload-list-item part="file-item" .item=${item}
                      @cancel-file=${this._cancelUpload} @replace-file=${this._replaceFile}
                      .hideFileTypeIcon=${this.hideFileTypeIcon} .uploadedFileHeight=${this.uploadedFileHeight}
                      .hideValidationError=${this.hideValidationError} hide-validation-criteria hide-validation-success
                      .allowedFileTypes=${this.allowedFileTypes} .allowedFileTypesMessage=${this.allowedFileTypesMessage}
                      .allowedFileTypesMessageDetails=${this.allowedFileTypesMessageDetails}
                      .maxFileSize=${this.maxFileSize} .maxFileSizeFailureMessage=${this.maxFileSizeFailureMessage}
                      .maxFileSizeFailureTitle=${this.maxFileSizeFailureTitle} .maxUploadSize=${this.maxUploadSize}
                      .maxUploadSizeFailureTitle=${this.maxUploadSizeFailureTitle}
                      .maxUploadSizeFailureMessage=${this.maxUploadSizeFailureMessage}
                      .replaceLabel=${this.replaceLabel} .fileUploadErrorMessage=${this.fileUploadErrorMessage}
                      .fileUploadErrorDetails=${this.fileUploadErrorDetails} .disabled=${this.disabled}
                      .removeFileTooltip=${this.removeFileTooltip}
                      tabindex=${ifDefined(this._delegatedFocus)}>
                    </ptcs-file-upload-list-item>
              `)}
            </div>`;
    }

    static get is() {
        return 'ptcs-file-upload-list';
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

            _filesToUpload: {
                type:   Array,
                notify: true
            },

            _duplicateFiles: {
                type: Array
            },

            _filesToUploadStatus: {
                type:     Number,
                value:    Date.now(),
                notify:   true,
                validate: '_validateFiles(fileRequired, allowedFileTypes, maxNumberOfFiles, maxFileSize, maxUploadSize, extraValidation)'
            },

            fileRequired: {
                type:      Boolean,
                isValue:   fileRequired => !!fileRequired,
                attribute: 'file-required'
            },

            disableInstantUpload: {
                type:      Boolean,
                value:     false,
                attribute: 'disable-instant-upload'
            },

            _failedUploads: {
                type:  Number,
                value: 0
            },

            maxFileSize: {
                type:      Number,
                attribute: 'max-file-size'
            },

            maxFileSizeFailureTitle: {
                type:      String,
                value:     'Maximum File Size',
                attribute: 'max-file-size-failure-title'
            },

            maxFileSizeFailureMessage: {
                type:      String,
                value:     'File exceeds the maximum ${value} MB limit per file',
                attribute: 'max-file-size-failure-message'
            },

            maxUploadSize: {
                type:      Number,
                attribute: 'max-upload-size'
            },

            maxUploadSizeFailureTitle: {
                type:      String,
                value:     'Upload Limit Reached',
                attribute: 'max-upload-size-failure-title'
            },

            maxUploadSizeFailureMessage: {
                type:      String,
                value:     'Selection exceeds the ${value} MB upload limit',
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

            repo: {
                type: String
            },

            path: {
                type:        String,
                observer:    '_updateFileNamesAndFullPaths',
                observeWhen: 'immediate'
            },

            hideFileTypeIcon: {
                type:      Boolean,
                attribute: 'hide-file-type-icon'
            },

            hasFiles: {
                type:        Boolean,
                readOnly:    true,
                notify:      true,
                observer:    '_hasFilesChanged',
                observeWhen: 'immediate',
                attribute:   'has-files'
            },

            hasFilesToUpload: {
                type:      Boolean,
                readOnly:  true,
                notify:    true,
                attribute: 'has-files-to-upload'
            },

            uploadedFileHeight: {
                type:      Number,
                attribute: 'uploaded-file-height'
            },

            replaceFileTitle: {
                type:      String,
                value:     'Replace File',
                attribute: 'replaced-file-title'
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

            cancelFileTitle: {
                type:      String,
                value:     'Cancel the File Upload?',
                attribute: 'cancel-file-title'
            },

            cancelFileMsg: {
                type:      String,
                value:     'If you cancel the file upload, you will lose the uploading process.',
                attribute: 'cancel-file-msg'
            },

            deleteActionLabel: {
                type:      String,
                value:     'Remove',
                attribute: 'delete-action-label'
            },

            replaceActionLabel: {
                type:      String,
                value:     'Replace',
                attribute: 'replace-action-label'
            },

            cancelActionLabel: {
                type:      String,
                value:     'Cancel Upload',
                attribute: 'cancel-action-label'
            },

            cancelCancelLabel: {
                type:      String,
                value:     'Continue Uploading',
                attribute: 'cancel-cancel-label'
            },

            cancelLabel: {
                type:      String,
                value:     'Cancel',
                attribute: 'cancel-label'
            },

            fileNames: {
                type:      String,
                readOnly:  true,
                notify:    true,
                attribute: 'file-names'
            },

            fullPaths: {
                type:      String,
                readOnly:  true,
                notify:    true,
                attribute: 'full-paths'
            },

            uploadInProgress: {
                type:      Boolean,
                notify:    true,
                readOnly:  true,
                attribute: 'upload-in-progress'
            },

            _totalUploadSize: {
                type:  Number,
                value: 0
            },

            _itemRefreshed: {
                type: Number
            },

            _delegatedFocus: String

            // Array of filenames of files being processed. The file is added on _upload() invocation and removed on completion,
            // failure, cancelation, or deletion
            //
            // $NUP _filesInProgress: {
            //     type:  Array,
            //     value: []
            // },

            // Array of filenames of files that have finished uploading
            //
            // $NUP _filesUploaded: {
            //     type:  Array,
            //     value: []
            // },

            // Array of filenames of files submitted when disabledInstantUploadFiles is true. This is for
            // legacy reasons with Polymer implementation that reports the whole list of files in processing
            // as failed, even if only one file failed and remainder were successful (see pre-existing UT).
            // The _filesToUpload array is filtered so that only files with status !== STATUS_COMPLETED remain,
            // so the successfully uploaded files are no longer in the array which cannot be used in the event
            // dispatch; using instead _disabledInstantUploadFiles = the list of files that were processed.
            //
            //  $NUP _disabledInstantUploadFiles: {
            //     type:  Array,
            //     value: []
            // }

        };
    }

    static get observers() {
        return [
            '_observeRepoAndPath(repo, path)'
        ];
    }

    constructor() {
        super();
        this._filesToUpload = [];
        this._duplicateFiles = [];
        this._filesInProgress = [];
        this._filesUploaded = [];
    }

    willUpdate(changedProperties) {
        super.willUpdate(changedProperties);

        if (changedProperties.has('_filesToUpload') || changedProperties.has('_itemRefreshed')) {
            this._observeFiles();
        }
    }

    _dispatchFileUploadEvent(eventName, filename) {

        if (!eventName.startsWith('direct')) {
            this.dispatchEvent(new CustomEvent(eventName, {
                bubbles:  true,
                composed: true,
                detail:   {
                    filename,
                    repo: this.repo,
                    path: this.path
                }
            }));
        }

        if (eventName === EVENT_COMPLETE || eventName === EVENT_DIRECT_UPLOAD_COMPLETE) {
            const idx = this._filesInProgress.indexOf(filename[0]);
            if (idx !== -1) {
                this._filesUploaded.push(...this._filesInProgress.splice(idx, 1));
            }
        }

        if (eventName === EVENT_FAILED || eventName === EVENT_DIRECT_UPLOAD_FAILED ||
            eventName === EVENT_CANCEL || eventName === EVENT_DELETE) {
            const idx = this._filesInProgress.indexOf(filename);
            if (idx !== -1) {
                this._filesInProgress.splice(idx, 1);
            }
        }

        this._setHasFiles(this._filesInProgress.length > 0);
    }

    _observeFiles() {
        let failedUploads = 0;
        let hasFilesToUpload = false;
        let uploadInProgress = false;
        let uploadComplete = true;
        let hasUploadedFiles = false;
        let totalUploadSize = 0;

        const _filesToUpload = this._filesToUpload;
        const hasFiles = this.numberOfFilesToUpload > 0;
        const uploadedFiles = [];

        _filesToUpload.forEach(file => {
            failedUploads += (file.status === STATUS_FAILED ? 1 : 0);
            totalUploadSize += file.file.size;

            if (file.status === STATUS_READY) {
                hasFilesToUpload = true;
            }

            if (file.status === STATUS_UPLOADING) {
                uploadInProgress = true;
            }

            if (file.status === STATUS_COMPLETED || file.status === STATUS_FAILED) {
                uploadedFiles.push(file.filename);
            } else {
                uploadComplete = false;
            }

            if (file.status === STATUS_COMPLETED) {
                hasUploadedFiles = true;
            }
        });

        this._failedUploads = failedUploads;
        this._totalUploadSize = totalUploadSize;

        this._setHasFilesToUpload(hasFilesToUpload);
        this._setHasFiles(hasFiles);
        this._setUploadInProgress(uploadInProgress);

        this._updateFileNamesAndFullPaths(this.path);

        if (this.disableInstantUpload && hasFiles && uploadComplete) {
            if (failedUploads > 0) {
                this._dispatchFileUploadEvent(EVENT_FAILED, this._disabledInstantUploadFiles);
            } else {
                this._dispatchFileUploadEvent(EVENT_COMPLETE, this._disabledInstantUploadFiles);
            }
            if (hasUploadedFiles) {
                this._filesToUpload = this._filesToUpload.filter(file => file.status !== STATUS_COMPLETED);
            }
        }

        this._filesToUploadStatus = Date.now(); // Trigger validation
    }

    _observeRepoAndPath(repo, path) {
        // The repo/path has been updated, reset any failed items back to STATUS_READY
        this._filesToUpload.forEach((file, index) => {
            if (file.status === STATUS_FAILED) {
                this._refreshItem(file.filename, {status: STATUS_READY});
            }
        });
    }

    _insertValidationMessage(messageElement) {
        this.shadowRoot.insertBefore(messageElement, this.shadowRoot.firstChild);
    }

    _validateFiles(fileRequired, allowedFileTypes, maxNumberOfFiles, maxFileSize, maxUploadSize, extraValidation) {
        const messages = [];

        if (fileRequired && this.numberOfFilesToUpload === 0) {
            messages.push(this.fileRequiredMessage);
        }

        if (this.numberOfFilesToUpload > 0 && allowedFileTypes && !allFilesInListAllowed(this._filesToUpload, allowedFileTypes)) {
            messages.push(this.allowedFileTypesMessageDetails);
        }

        if (maxNumberOfFiles && Number(maxNumberOfFiles) < this.numberOfFilesToUpload) {
            const msg = PTCS.replaceStringTokens(this.maxNumberOfFilesFailureMessage, {value: this.maxNumberOfFiles});
            messages.push(msg ? msg.join('. ') : false);
        }

        if (maxFileSize && exceedMaxFileSize(this._filesToUpload, maxFileSize)) {
            const msg = PTCS.replaceStringTokens(this.maxFileSizeFailureMessage, {value: maxFileSize});
            messages.push(msg ? msg.join('. ') : false);
        }

        if (maxUploadSize && this._totalUploadSize > Number(maxUploadSize) * Math.pow(1024, 2)) {
            const msg = PTCS.replaceStringTokens(this.maxUploadSizeFailureMessage, {value: maxUploadSize});
            messages.push(msg ? msg.join('. ') : false);
        }

        if (this._failedUploads > 0) {
            messages.push(false);
        }

        // At least one validation failed
        if (messages.length) {
            return messages;
        }

        return typeof extraValidation === 'function' ? extraValidation(this) : true;
    }

    _upload(file, {indexOfFileToReplace} = {}) {
        const fileToUpload = {
            file,
            progress: 0,
            filename: file.name,
            status:   STATUS_READY,
            type:     getFileType(file).toUpperCase()
        };

        if (!isNaN(indexOfFileToReplace)) {
            this.splice('_filesToUpload', indexOfFileToReplace, 1, fileToUpload);
            this.splice('_filesInProgress', indexOfFileToReplace, 1, fileToUpload.filename);
        } else {
            this.push('_filesToUpload', fileToUpload);
            this._filesInProgress.push(fileToUpload.filename);
        }

        if (!this.disableInstantUpload) {
            this._doUpload(fileToUpload, indexOfFileToReplace !== undefined);
        }
    }

    // Search for focusable element into the element (this).
    // If no focusable element is found, go back to the document to find focusable element.
    get focusElement() {
        if (!this.tabindex) {
            return null; // Not focusable
        }
        let hit = this.shadowRoot.activeElement;
        let el = hit || document.activeElement;
        while (el && el.shadowRoot && el.shadowRoot.activeElement) {
            hit = hit || el === this || this.contains(el);
            el = el.shadowRoot.activeElement;
        }
        return hit ? hit && el : el;
    }

    get numberOfFilesToUpload() {
        return this._filesToUpload ? this._filesToUpload.length : 0;
    }

    _handleDuplicate(file, onreplace) {
        if (!this._filesToUpload) {
            return;
        }
        const duplicateFileIndex = this._filesToUpload.findIndex(fileToUpload => fileToUpload.filename === file.name);
        const duplicateFile = this._filesToUpload[duplicateFileIndex];

        this._lastFocusEl = this.focusElement;

        const doReplace = () => {
            this._upload(file, {
                indexOfFileToReplace: duplicateFileIndex
            });

            if (onreplace) {
                onreplace();
            }

            // Clear the list
            this._duplicateFiles = [];
        };

        const dlg = this._confirmationDialog;

        // Add a separate callback to clear the file list on cancel/close
        const doClose = () => {
            // Clear the list
            this._duplicateFiles = [];
            dlg.removeEventListener('close-action', doClose);
        };

        dlg.addEventListener('close-action', doClose);

        // One more...
        this._duplicateFiles.push(file);

        const singleFile = this._duplicateFiles.length === 1;

        let title, msg;

        if (singleFile) {
            // Single duplicate file
            title = this.replaceFileTitle;
            msg = PTCS.replaceStringTokens(this.replaceFileMsg, {
                filename: fileLabel(duplicateFile.file, duplicateFile.filename)
            }).join();
        } else {
            // Multiple files, use new properties
            title = this.replaceMultiFileTitle;

            // Create a comma-separated list of the file names
            const files = this._duplicateFiles.map(f => fileLabel(f, f.name)).join(', ');

            msg = PTCS.replaceStringTokens(this.replaceMultiFileMsg, {
                filenames: files
            }).join();
        }

        this._openDialog(doReplace, {
            titleText:          title,
            messageText:        msg,
            primaryActionLabel: this.replaceActionLabel
        });
    }

    _isDuplicate(file) {
        if (!this._filesToUpload) {
            return -1;
        }

        const duplicateFileIndex = this._filesToUpload.findIndex(fileToUpload => fileToUpload.filename === file.name);

        return duplicateFileIndex !== -1;
    }

    add(file) {
        const okAction = () => {
            this._confirmationDialog.removeEventListener('primary-action', okAction);
            this._switchFocus('last');
        };

        if (!this.disableInstantUpload) {
            this._lastFocusEl = this.focusElement;
            this._isInvalidFileType = this.allowedFileTypes
                ? !isFileTypeAllowed(this.allowedFileTypes, getFileType(file) || file.type)
                : false;
            // Open a confirmation dialog with the relevant validation message and prevent the file from being uploaded
            if (this._isInvalidFileType) {
                openValidationDialog(this._confirmationDialog, this.allowedFileTypesMessage, this.allowedFileTypesMessageDetails);
                this._confirmationDialog.addEventListener('primary-action', okAction);
                return;
            }

            if (this.maxFileSize && file.size >= Number(this.maxFileSize) * Math.pow(1024, 2)) {
                const msg = PTCS.replaceStringTokens(this.maxFileSizeFailureMessage, {value: this.maxFileSize});
                openValidationDialog(this._confirmationDialog, this.maxFileSizeFailureTitle, msg ? msg.join('. ') : '');
                this._confirmationDialog.addEventListener('primary-action', okAction);
                return;
            }

            if (this.maxUploadSize && this._totalUploadSize + file.size >= Number(this.maxUploadSize) * Math.pow(1024, 2)) {
                const msg = PTCS.replaceStringTokens(this.maxUploadSizeFailureMessage, {value: this.maxUploadSize});
                openValidationDialog(this._confirmationDialog, this.maxUploadSizeFailureTitle, msg ? msg.join('. ') : '');
                this._confirmationDialog.addEventListener('primary-action', okAction);
                return;
            }
        }

        if ((this.maxNumberOfFiles && Number(this.maxNumberOfFiles) < 1 + this.numberOfFilesToUpload) && !this._isDuplicate(file)) {
            if (Number(this.maxNumberOfFiles) === 1 && Number(this.maxNumberOfFiles) === this.numberOfFilesToUpload) {
                this._upload(file, {indexOfFileToReplace: 0});
                return;
            }
            const msg = PTCS.replaceStringTokens(this.maxNumberOfFilesFailureMessage, {value: this.maxNumberOfFiles});
            openValidationDialog(this._confirmationDialog, this.maxUploadSizeFailureTitle, msg ? msg.join('. ') : false);
            this._confirmationDialog.addEventListener('primary-action', okAction);
            return;
        }

        if (!this._isDuplicate(file)) {
            this._upload(file);
        } else {
            this._handleDuplicate(file);
        }
    }

    _getItemIndex(file) {
        return this._filesToUpload.findIndex(fileToUpload => fileToUpload.filename === file.filename);
    }

    _replaceFile(ev) {
        if (this.disabled) {
            return;
        }

        const index = this._getItemIndex(ev.currentTarget.item);
        const file = ev.detail.file;

        if (!this._isDuplicate(file)) {
            this._upload(file, {
                indexOfFileToReplace: index
            });
        } else {
            this._handleDuplicate(file, () => {
                // We are actually replacing 2 files now. The original one (where "replace" was clicked)
                // and the duplicate one.
                this.splice('_filesToUpload', index, 1);
                this.requestUpdate('_filesToUpload');
            });
        }
    }

    _switchFocus(type) {
        switch (type) {
            case 'delete':
                this.getRootNode().host.$['file-select'].focus();
                break;
            case 'last':
                if (this._lastFocusEl) {
                    this._lastFocusEl.focus();
                }
                break;
        }
    }

    _openDialog(action, {titleText, messageText = '', primaryActionLabel,
        cancelActionLabel = this.cancelLabel, primaryButtonStyle = 'primary', hideCancelAction = false}) {
        const dlg = this._confirmationDialog;

        const primaryAction = () => {
            action();

            dlg.removeEventListener('primary-action', primaryAction);
            // eslint-disable-next-line no-use-before-define
            dlg.removeEventListener('close-action', closeAction);
            this._switchFocus('delete');
        };

        const closeAction = () => {
            dlg.removeEventListener('primary-action', primaryAction);
            dlg.removeEventListener('close-action', closeAction);
            this._switchFocus('last');
        };

        dlg.addEventListener('primary-action', primaryAction);
        dlg.addEventListener('close-action', closeAction);

        dlg.titleText = titleText;
        dlg.messageText = messageText;
        dlg.primaryButtonStyle = primaryButtonStyle;
        dlg.primaryActionLabel = primaryActionLabel;
        dlg.cancelActionLabel = cancelActionLabel;
        dlg.hideCancelAction = hideCancelAction;

        dlg.open();
    }

    _cancelUpload(ev) {
        if (this.disabled) {
            return;
        }

        const el = ev.currentTarget;
        const item = el.item;
        const index = this._getItemIndex(item);

        this._lastFocusEl = this.focusElement;

        if (item.status !== STATUS_UPLOADING) {
            const doDelete = () => {
                this.isFileRemoved = true;
                // File is not being uploaded right now. Delete it.
                this.splice('_filesToUpload', index, 1);
                this.requestUpdate('_filesToUpload');
                this._dispatchFileUploadEvent(EVENT_DELETE, item.filename);
            };

            this._openDialog(doDelete, {
                titleText:          this.deleteFileTitle,
                messageText:        this.deleteFileMsg,
                primaryActionLabel: this.deleteActionLabel,
                primaryButtonStyle: 'danger'
            });

            return;
        }

        this._dispatchFileUploadEvent(EVENT_CANCEL, item.filename);

        const doCancel = () => {
            el.actions.cancel();
            item.status = STATUS_CANCELED;
        };

        this._openDialog(doCancel, {
            titleText:          this.cancelFileTitle,
            messageText:        this.cancelFileMsg,
            primaryActionLabel: this.cancelActionLabel,
            primaryButtonStyle: 'danger',
            cancelActionLabel:  this.cancelCancelLabel
        });
    }

    deleteAll(noconfirm = false) {
        if (this.disabled) {
            return;
        }

        this._lastFocusEl = this.focusElement;

        const doDeleteAll = () => {
            this.isFileRemoved = true;
            this._filesToUpload.forEach((file) => {
                if (file.status === STATUS_UPLOADING) {
                    file.actions.cancel();
                }

                this._dispatchFileUploadEvent(EVENT_DELETE, file.filename);
            });

            this._filesToUpload = [];
        };

        if (!noconfirm) {
            this._openDialog(doDeleteAll, {
                titleText:          this.deleteFileTitle,
                messageText:        this.deleteFileMsg,
                primaryActionLabel: this.deleteActionLabel,
                primaryButtonStyle: 'danger'
            });
        } else {
            doDeleteAll();
        }
    }

    clearFileList() {
        if (this._filesToUpload.filter(file => file.status !== STATUS_COMPLETED).length === 0) {
            this._filesToUpload = [];
        }

        for (let index = 0; index < this._filesToUpload.length; index++) {
            const isFileUploaded = this._filesToUpload[index].status !== STATUS_UPLOADING ||
                this._filesToUpload[index].progress === 100;

            if (isFileUploaded) {
                this._dispatchFileUploadEvent(EVENT_DELETE, this._filesToUpload[index].filename);
                this._filesToUpload.splice(index, 1);
                index--;
            }
        }
    }

    _updateFileNamesAndFullPaths(path) {
        if (!path) {
            path = '/';
        }

        const nIndexSlash = path.lastIndexOf('/');
        if (nIndexSlash !== path.length - 1) {
            path = path + '/';
        }

        if (this.numberOfFilesToUpload > 0) {
            let fileNames = '';
            let fullPaths = '';
            for (const fileToUpload of this._filesToUpload) {
                const filename = fileToUpload.filename;

                if (fileNames === '') {
                    fileNames = filename;
                } else {
                    fileNames += MULTI_FILE_DELIMITER + filename;
                }
                // full path
                let currentPath;
                if (fullPaths === '') {
                    currentPath = path + filename;
                } else {
                    currentPath = MULTI_FILE_DELIMITER + path + filename;
                }
                fullPaths += currentPath;
            }
            this._setFullPaths(fullPaths);
            this._setFileNames(fileNames);
        } else if (this.isFileRemoved) {
            this.isFileRemoved = false;
            this._setFullPaths(path);
            this._setFileNames('');
        }
    }

    // Invoked from the Upload button, when disableInstantUpload is true
    uploadAll() {
        if (this.disabled) {
            return;
        }

        if (!this.disableInstantUpload) {
            return;
        }

        this._disabledInstantUploadFiles = [];
        for (const fileToUpload of this._filesToUpload) {
            this._doUpload(fileToUpload);
            this._disabledInstantUploadFiles.push(fileToUpload.filename);
        }
        // Emit the EVENT_STARTED here as _doUpload() specifically doesn't do it in disableInstantUpload mode.
        this._dispatchFileUploadEvent(EVENT_STARTED, this._disabledInstantUploadFiles);
    }

    // Refresh ptcs-file-upload-list-item based on its filename
    _refreshItem(filename, opts) {
        this.performUpdate();

        const fileIndex = () => this._filesToUpload.findIndex(f => f.filename === filename);
        if (fileIndex() === -1) {
            // On file replace / delete, the progress update may still invoke _refreshItem on the removed item.
            return;
        }
        const entry = this.$['files-list'].querySelectorAll('[part=file-item]')[fileIndex()];
        const updatedItem = Object.assign(entry.item,
            {status: opts.status || entry.item.status}, {progress: opts.progress || entry.item.progress});
        // Force reactive update of entry.item
        entry.item = null;
        entry.item = updatedItem;
        if (opts.actions) {
            entry.actions = opts.actions;
        }
        entry.performUpdate();
        this._itemRefreshed = Date.now();
    }

    _doUpload(fileToUpload, replace) {
        if (fileToUpload.status !== STATUS_READY) {
            return;
        }

        if (!this.uploadManager || !(typeof this.uploadManager.upload === 'function')) {
            return;
        }

        const filename = fileToUpload.filename;

        const onprogress = e => {
            if (e.progress !== 100) {
                // 100% progress means only that the file fully reached the target server.
                // The action is completed only when the server responds with 200 status.
                this._refreshItem(filename, {progress: e.progress});
            }
        };

        const onstatuschange = e => {
            if (e.status === 200) {
                this._refreshItem(filename, {progress: 100});

                // Setting "completed" status hides the progress bar so doing it with a small delay
                setTimeout(() => {
                    this._refreshItem(filename, {status: STATUS_COMPLETED});
                    if (!this.disableInstantUpload) {
                        this._dispatchFileUploadEvent(EVENT_COMPLETE, [filename]);
                    } else {
                        this._dispatchFileUploadEvent(EVENT_DIRECT_UPLOAD_COMPLETE, [filename]);
                    }
                    if (this.disableInstantUpload) {
                        // Clear the file list if all files have STATUS_COMPLETED
                        if (this._filesToUpload.filter(file => file.status !== STATUS_COMPLETED).length === 0) {
                            this._filesToUpload = [];
                        }
                        // Update the file list while uploading or ready to upload
                        if (this._filesToUpload.filter(file => (file.status === STATUS_UPLOADING || file.status === STATUS_READY)).length === 0) {
                            this._filesToUpload = this._filesToUpload.filter(file => file.status !== STATUS_COMPLETED);
                        }
                    }
                    this._filesToUploadStatus = Date.now(); // Trigger validation
                }, 500);
            } else if (e.status === 0) {
                this._refreshItem(filename, {status: STATUS_CANCELED});
            } else {
                this._refreshItem(filename, {status: STATUS_FAILED});
                if (!this.disableInstantUpload) {
                    this._dispatchFileUploadEvent(EVENT_FAILED, [filename]);
                } else {
                    this._dispatchFileUploadEvent(EVENT_DIRECT_UPLOAD_FAILED, [filename]);
                }
            }
            this._filesToUploadStatus = Date.now(); // Trigger validation
        };

        const actions = this.uploadManager.upload(fileToUpload.file, {
            repo: this.repo,
            path: this.path,
            replace,
            filename
        }, onprogress, onstatuschange);

        if (actions !== null) {
            fileToUpload.actions = actions;
            this._refreshItem(filename, {status: STATUS_UPLOADING, progress: 0, actions: actions});

            if (!this.disableInstantUpload) {
                // In direct upload mode, EVENT_STARTED is emitted for each file
                this._dispatchFileUploadEvent(EVENT_STARTED, [filename]);
            }
        }
    }

    _hasFilesChanged(_hasFiles) {
        if (this.disableInstantUpload && !_hasFiles) {
            if (this._failedUploads > 0) {
                this._dispatchFileUploadEvent(EVENT_FAILED, this._disabledInstantUploadFiles);
            } else if (this._filesUploaded.length > 0) {
                this._dispatchFileUploadEvent(EVENT_COMPLETE, this._disabledInstantUploadFiles);
            }
            this._filesInProgress = [];
            this._filesUploaded = [];
        }

        this._filesToUploadStatus = Date.now(); // Trigger validation
    }
};

customElements.define(PTCS.FileUploadList.is, PTCS.FileUploadList);
