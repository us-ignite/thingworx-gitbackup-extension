import {PTCS} from 'ptcs-library/library.js';

PTCS.BehaviorBinary = superClass => class extends superClass {

    static get properties() {
        return {
            checked: {
                type:               Boolean,
                notify:             true,
                reflectToAttribute: true, // Polymer
                reflect:            true, // Lit
                observer:           '_checkedChanged',
                observeWhen:        'immediate'
            },

            state: {
                type:        Boolean,
                observer:    '_stateChanged',
                observeWhen: 'immediate'
            },

            disabled: {
                type:               Boolean,
                reflectToAttribute: true, // Polymer
                reflect:            true, // Lit
            },

            // The variable controlled by this checkbox control
            variable: {
                notify:      true,
                observer:    '_variableChanged',
                observeWhen: 'immediate'
            },

            // Value of variable that represents on mode
            valueOn: {
                attribute: 'value-on'
            },

            // Value of variable that represents off mode
            valueOff: {
                attribute: 'value-off'
            }
        };
    }

    constructor() {
        super();

        // Initialize the values here
        this.checked = false;
        this.state = false;
        this.disabled = false;
        this.valueOn = true;
        this.valueOff = false;
    }

    _variableChanged(variable) {
        if (variable === this.valueOn) {
            if (!this.checked) {
                this.checked = true;
            }
        } else if (variable === this.valueOff) {
            if (this.checked) {
                this.checked = false;
            }
        } else {
            console.error('Checkbox value is unknown: ' + variable);
        }
    }

    _checkedChanged(checked) {
        this.variable = checked ? this.valueOn : this.valueOff;
        this.state = checked;
    }

    _stateChanged(state) {
        this.variable = state ? this.valueOn : this.valueOff;
        this.checked = state;
    }
};

