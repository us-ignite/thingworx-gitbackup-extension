import {LitElement, html, css} from 'lit';
import {when} from 'lit/directives/when.js';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-behavior-tabindex/ptcs-behavior-tabindex.js';
import 'ptcs-behavior-focus/ptcs-behavior-focus.js';
import 'ptcs-icon/ptcs-icon.js';
import 'ptcs-label/ptcs-label.js';
import 'ptcs-link/ptcs-link.js';
import 'ptcs-divider/ptcs-divider.js';

const States = {
    CURRENT:  'current',
    COMPLETE: 'complete',
    INACTIVE: 'inactive',
    ERROR:    'error'
};

const Sizes = {
    S: 'small',
    M: 'medium',
    L: 'large'
};

const Directions = {
    L: 'left',
    R: 'right'
};

Object.freeze(States);
Object.freeze(Sizes);
Object.freeze(Directions);

const errorStateData = [
    {stepNumber: 1, stepState: States.CURRENT, isInteractive: false},
    {stepNumber: 2, stepState: States.INACTIVE, isInteractive: false},
    {stepNumber: 3, stepState: States.INACTIVE, isInteractive: false}
];


PTCS.ProgressTracker = class extends PTCS.BehaviorTabindex(PTCS.BehaviorFocus(PTCS.BehaviorStyleable(LitElement))) {
    static get styles() {
        return css`
            :host {
                display: flex;
                overflow: auto;
                outline: none;
            }

            #ctr {
                display: flex;
                flex: 1 1 auto;
                align-items: center;
            }

            #stepctr {
                display: flex;
                flex: 1 1 auto;
                outline: none;
            }

            [part=step] {
                display: flex;
                flex: 1 1 0;
                flex-direction: column;
                align-items: center;
                justify-content: flex-start;
            }

            [part=step][interactive]:not([disabled]):hover {
                cursor: pointer;
            }

            [part=step-number]::part(label) {
                display: flex;
                box-sizing: border-box;
                align-items: center;
                justify-content: center;
            }

            [part=step-label][hidden], [part=step-link][hidden] {
                display: none;
            }

            [part=pipe] {
                flex: 1 1 auto;
            }

            [part=pipe][hidden] {
                visibility: hidden;
            }

            .error-ctr {
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                width: 100%;
            }

            [part=error-detail] {
                display: flex;
                align-items: center;
                justify-content: center;
            }

            [part=error-container][error] > [part=error-detail] {
                flex-direction: column;
            }

            .flex-row-ctr {
                display: flex;
                flex-direction: row;
                width: 100%;
            }
        `;
    }

    render() {
        return html`
            <div id="ctr" part="root">
                ${this._errorState ? this.renderErrorState() : this.renderSteps()}
            </div>
        `;
    }

    renderSteps() {
        const data = this._errorState ? errorStateData : this.steps;

        return html`
            <div id="stepctr" part="step-container">
                ${data.map((step, i) => this.renderStep(step, i + 1))}
            </div>
        `;
    }

    renderStep(step, i) {
        step.stepState = this._getUpdatedStepState(step.stepState, i);
        const interactive = this.isInteractive && (step.isInteractive === undefined || step.isInteractive === true) && i !== this.currentStepNumber;

        return html`
            <div part="step" index=${i} ?interactive=${interactive} state=${step.stepState}
            ?visited=${i < this.currentStepNumber && !this.disableAutoStepCompletion}
                ?disabled=${this.disabled || this._errorState} @click=${() => this._stepClicked(interactive, i)}>
                <div part="step-top" class="flex-row-ctr">
                    ${this.renderPipe(i, Directions.L)}
                    ${this.renderIcon(step)}
                    ${this.renderPipe(i, Directions.R)}
                </div>
                ${this.renderLabel(step.stepLabel, interactive)}
            </div>
        `;
    }

    renderIcon(step) {
        if (step.stepState === States.COMPLETE || step.stepState === States.ERROR) {
            const icon = step.stepState === States.ERROR ? this.errorStateIcon : this.completedStepIcon;

            return html`
                <ptcs-icon part="step-icon" ?disabled=${this.disabled} size="custom" icon=${icon}/>
            `;
        }

        // Don't show step numbers when stepSize is 'small' or there is an error state
        const stepNumber = this.stepSize !== Sizes.S && !this._errorState ? step.stepNumber : null;

        return html`
            <ptcs-label part="step-number" .label=${stepNumber} .horizontalAlignment=${'center'} ?disabled=${this.disabled || this._errorState}
                .verticalAlignment=${'flex-end'}/>
        `;
    }

    renderLabel(label, interactive) {
        if (interactive) {
            return html`
                <ptcs-link part="step-link" variant="secondary" tabindex="-1" .label=${label} .singleLine=${false} .maxNumberOfLines=${2}
                    ?disabled=${this.disabled} .alignment=${'center'} ?hidden=${!label}/>
            `;
        }

        return html`
            <ptcs-label part="step-label" .label=${label} .multiLine=${true} .maxNumberOfLines=${2} .disclosureControl=${'ellipsis'}
                ?disabled=${this.disabled} .horizontalAlignment=${'center'} ?hidden=${!label}/>
        `;
    }

    renderPipe(index, direction) {
        const uncompleted = this.disableAutoStepCompletion ? true : (this.currentStepNumber === null || this.currentStepNumber === undefined) ||
        (index > this.currentStepNumber) || (index === this.currentStepNumber && direction === Directions.R) || this._errorState;
        const length = this._errorState ? errorStateData.length : this.steps.length;
        const hidden = (direction === Directions.L && index === 1) || (direction === Directions.R && index === length);

        return html `
            <ptcs-divider part="pipe" ?hidden=${hidden} ?disabled=${this.disabled || this._errorState} ?uncompleted=${uncompleted}/>
        `;
    }

    renderErrorState() {
        let error = true;
        let icon = this.errorStateIcon;
        let label = this.errorStateText;

        if (this.steps === undefined || this.steps === null) {
            error = false;
            icon = this.noBindingIcon;
            label = this.noBindingText;
        } else if (Array.isArray(this.steps) && this.steps.length === 0) {
            error = false;
            icon = this.noDataIcon;
            label = this.noDataText;
        }

        const msg = html`
            <div part="error-detail">
                <ptcs-icon part="error-state" ?error=${error} .icon=${icon} size="small"></ptcs-icon>
                <ptcs-label part="error-label" .label=${label} .horizontalAlignment=${'center'} .verticalAlignment=${'flex-end'}></ptcs-label>
            </div>
        `;

        const msgWithTracker = html`
            ${this.renderSteps()}
            ${msg}
        `;

        return html`
            <div part="error-container" class="error-ctr" ?error=${error}>
                ${when(error, () => msg, () => msgWithTracker)}
            </div>
        `;
    }

    static get is() {
        return 'ptcs-progress-tracker';
    }

    static get properties() {
        return {
            steps: {
                type: Array
            },

            currentStepNumber: {
                type:      Number,
                attribute: 'current-step-number'
            },

            selectedData: {
                type:      Object,
                attribute: 'selected-data'
            },

            isInteractive: {
                type:      Boolean,
                attribute: 'is-interactive',
                reflect:   true
            },

            // 'small' / 'medium' / 'large'
            stepSize: {
                type:      String,
                attribute: 'step-size'
            },

            minPipeLength: {
                type:      Number,
                attribute: 'min-pipe-length'
            },

            completedStepIcon: {
                type:      String,
                attribute: 'completed-step-icon'
            },

            noDataText: {
                type:      String,
                attribute: 'no-data-text'
            },

            noDataIcon: {
                type:      String,
                attribute: 'no-data-icon'
            },

            noBindingText: {
                type:      String,
                attribute: 'no-binding-text'
            },

            noBindingIcon: {
                type:      String,
                attribute: 'no-binding-icon'
            },

            errorStateText: {
                type:      String,
                attribute: 'error-state-text'
            },

            errorStateIcon: {
                type:      String,
                attribute: 'error-state-icon'
            },

            disabled: {
                type:    Boolean,
                reflect: true
            },

            _errorState: {
                type:  Boolean,
                state: true
            },

            disableAutoStepCompletion: {
                type:      Boolean,
                attribute: 'disable-auto-step-completion'
            }
        };
    }

    constructor() {
        super();

        this.isInteractive = true;
        this.stepSize = Sizes.M;
        this.completedStepIcon = 'cds:icon_ok_mini';
        this.noDataText = 'No progress tracker steps to display.';
        this.noDataIcon = 'cds:icon_not_visible';
        this.noBindingText = 'No progress tracker data to display.';
        this.noBindingIcon = 'cds:icon_bind';
        this.errorStateText = 'Unable to load data.';
        this.errorStateIcon = 'cds:icon_error';
        this.disabled = false;
        this._errorState = true;
        this.disableAutoStepCompletion = false;
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener('keydown', this._handleKeyDown);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener('keydown', this._handleKeyDown);
    }

    // Callback for BehaviorFocus
    _initTrackFocus() {
        this._trackFocus(this, () => {
            const ctr = this.shadowRoot.getElementById('stepctr');
            if (ctr && !this._errorState) {
                if (this._focusedIndex >= 0 && this._focusedIndex <= this.steps.length - 1) {
                    return ctr.children[this._focusedIndex].querySelector('[part~=step-link]') || this;
                }
            }
            // No individual step has focus, highlight the entire Progress Tracker
            return this;
        });
    }

    firstUpdated() {
        super.firstUpdated();
        this.minPipeLength = 90;
    }

    willUpdate(changedProperties) {
        super.willUpdate(changedProperties);

        if (changedProperties.has('steps')) {
            this._stepsChanged();
        }

        if (changedProperties.has('_errorState')) {
            if (this._errorState) {
                this._prevStepSize = this.stepSize; // Save the lastest stepSize config before changing to error state
            }
            PTCS.setAttribute(this, 'step-size', !this._errorState ? this._prevStepSize : null);
        }

        if (changedProperties.has('stepSize')) {
            this._updateStepSize();
        }
    }

    updated(changedProperties) {
        super.updated(changedProperties);
        PTCS.setAttribute(this, 'error-message', this._errorState);
        if (!this._errorState) {
            this.setAttribute('step-size', this.stepSize);
        }
    }

    set currentStepNumber(newVal) {
        if (newVal !== null && newVal !== undefined) {
            if (!this.steps) {
                // out-of-sequence assignment of currentStepNumber, before steps contains data
                this._pendingCurrentStepNumber = newVal;
                return;
            }
            newVal = Number(this._pendingCurrentStepNumber ? this._pendingCurrentStepNumber : newVal);
            const oldVal = this._currentStepNumber;
            const isValid = this._isValidIndex(newVal);
            console.assert(isValid, 'Must be a positive Integer and valid step.');
            this._currentStepNumber = isValid ? newVal : oldVal ;
            this.selectedData = this._getCurrentStepData();
        } else {
            // When there is no current step defined by the user, on the infotable, the current step number should be undefined
            this._currentStepNumber = newVal;
        }
    }

    get currentStepNumber() {
        return this._currentStepNumber;
    }

    set minPipeLength(newVal) {
        const oldVal = this._minPipeLength;
        const isValid = !isNaN(newVal) && newVal >= 60;
        console.assert(isValid, 'minPipeLength must be a valid number and cannot be less then 60.');
        this._minPipeLength = isValid ? newVal : oldVal || 90;
        this.style.setProperty('--ptcs-progress-min-pipe-length', `${this._minPipeLength / 2}px`);
    }

    get minPipeLength() {
        return this._minPipeLength;
    }

    _stepsChanged() {
        this._errorState = !this._isValidData();

        if (!this._errorState) {
            this.currentStepNumber = this._pendingCurrentStepNumber !== undefined ? this._pendingCurrentStepNumber : undefined;
            this.steps = this._sortData();
            this._initCurrentStep();
        }

        this._updateStepSize();
    }

    // Check if the currentStepNumber is a positive number and a valid step
    _isValidIndex(index) {
        return (Number.isInteger(index) && index > 0 && (!this.steps || index <= this.steps.length));
    }

    _stepClicked(interactive, index) {
        if (!interactive || this.disabled || this._errorState) {
            return;
        }

        this.currentStepNumber = index;

        // Make sure the DOM is updated before we start looking at the interactive attribute below...
        this.performUpdate();

        // Move the focus to the next "valid" item to the right
        this._focusOnStep(index - 1, Directions.R);

        this.selectedData = this._getCurrentStepData();
        this.dispatchEvent(new CustomEvent('step-clicked',
            {detail: {selectedData: this.selectedData, currentStepNumber: this.currentStepNumber}, bubbles: true}
        ));
    }

    _isValidData() {
        const steps = this.steps;

        if (!Array.isArray(steps) || steps.length < 1) {
            return false;
        }

        return !steps.some(step => !step || !step.stepNumber || this._isInteger(step.stepNumber) || step.stepNumber < 1);
    }

    // return a number or string of number
    _isInteger(stepNumber) {
        // eslint-disable-next-line eqeqeq
        return parseInt(stepNumber) != stepNumber;
    }

    _sortData() {
        return this.steps.sort((a, b) => a.stepNumber - b.stepNumber);
    }

    _findStepNumberIndex() {
        if (Array.isArray(this.steps) && this.steps.length !== 0) {
            if (this._pendingCurrentStepNumber !== undefined) {
                return this._pendingCurrentStepNumber - 1;
            }
            return this.steps.findIndex(step => step.stepState === States.CURRENT);
        }
        return null;
    }

    _initCurrentStep() {
        const index = this._findStepNumberIndex();
        this.currentStepNumber = index !== null && index !== -1 ? index + 1 : null;

        // Since we set the this.currentStepNumber, set the selectedData as well
        this.selectedData = this._getCurrentStepData();

        if (this.currentStepNumber !== null && this.currentStepNumber !== undefined) {
            // Focus on the first "interactive" item (once the items are in place)
            requestAnimationFrame(() => this._focusOnStep(this.currentStepNumber - 1, Directions.R));
        }
        this._pendingCurrentStepNumber = undefined;
    }

    _focusOnStep(index, direction) {
        const steps = Array.from(this.shadowRoot.querySelectorAll('div[part=step]'));
        const length = steps.length;

        // Loop through the entire array starting from 'index'
        for (let i = 1; i < length; i++) {
            const equation = direction === Directions.R ? (i + index) : (index - i + length); // Go left or right
            const nextIndex = equation % length;
            if (steps[nextIndex].hasAttribute('interactive')) {
                this._scrollToStep(nextIndex);
                this._focusedIndex = nextIndex;
                break;
            }
        }
    }

    _handleKeyDown(ev) {
        if (this._errorState || this.disabled) {
            return;
        }

        switch (ev.key) {
            case 'Home':
                this._focusOnStep(this.steps.length - 1, Directions.R);
                ev.preventDefault();
                break;
            case 'ArrowRight':
                this._focusOnStep(this._focusedIndex, Directions.R);
                ev.preventDefault();
                break;
            case 'ArrowLeft':
                this._focusOnStep(this._focusedIndex, Directions.L);
                ev.preventDefault();
                break;
            case 'End':
                this._focusOnStep(0, Directions.L);
                ev.preventDefault();
                break;
            case 'Enter':
            case ' ':
                this._stepClicked(true, this._focusedIndex + 1);
                ev.preventDefault();
                break;
        }
    }

    _getStepByIndex(index) {
        return this.shadowRoot.querySelector(`div[part=step][index="${index + 1}"]`); // Indexes: 1 - steps.length
    }

    _scrollToStep(index) {
        const ptRect = this.getBoundingClientRect();
        const stepRect = this._getStepByIndex(index).getBoundingClientRect();

        if (stepRect.right > ptRect.right) {
            this.scrollLeft += stepRect.right - ptRect.right;
        } else if (stepRect.left < ptRect.left) {
            this.scrollLeft += stepRect.left - ptRect.left;
        }
    }

    _getUpdatedStepState(currState, index) {
        if (this._errorState) {
            return currState;
        }

        if (this.currentStepNumber !== null && this.currentStepNumber !== undefined) {
            if (currState === States.ERROR) {
                return States.ERROR;
            } else if (index < this.currentStepNumber && !this.disableAutoStepCompletion) {
                return States.COMPLETE;
            } else if (index > this.currentStepNumber && !this.disableAutoStepCompletion) {
                return States.INACTIVE;
            } else if (index === this.currentStepNumber) {
                return States.CURRENT;
            }
        }

        return States.INACTIVE;
    }

    _getCurrentStepData() {
        if (!Array.isArray(this.steps) || this.steps.length < 1 || this.currentStepNumber === null || this.currentStepNumber === undefined) {
            return null;
        }
        return this._pendingCurrentStepNumber ? this._pendingCurrentStepNumber - 1 : this.steps[this.currentStepNumber - 1];
    }

    _updateStepSize() {
        let stepSizeInPx;

        if (this.stepSize === Sizes.S || this._errorState) {
            stepSizeInPx = 18;
        } else if (this.stepSize === Sizes.L) {
            stepSizeInPx = 32;
        } else {
            this.stepSize = Sizes.M;
            stepSizeInPx = 24;
        }

        this.style.setProperty('--ptcs-progress-step-size', `${stepSizeInPx}px`);
    }
};

customElements.define(PTCS.ProgressTracker.is, PTCS.ProgressTracker);
