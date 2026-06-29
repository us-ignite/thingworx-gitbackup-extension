import {LitElement} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';

PTCS.Hbar = class extends L2Pw(LitElement) {
    static get is() {
        return 'ptcs-hbar';
    }

    static get properties() {
        return {
            // inline
            inline:
                {
                    type:  Boolean,
                    value: false
                },

            // halign
            end:
                {
                    type:  Boolean,
                    value: false
                },

            center:
                {
                    type:  Boolean,
                    value: false
                },

            stretch:
                {
                    type:  Boolean,
                    value: false
                },

            spaceAround:
                {
                    type:  Boolean,
                    value: false
                },

            // valign
            top:
                {
                    type:  Boolean,
                    value: false
                },

            bottom:
                {
                    type:  Boolean,
                    value: false
                },

            xstretch:
                {
                    type:  Boolean,
                    value: false
                },

            // wrap
            wrap:
                {
                    type:  Boolean,
                    value: false
                }
        };
    }

    createRenderRoot() {
        return this;
    }

    willUpdate(changedProperties) {
        super.willUpdate(changedProperties);

        const valign = this.__valign(this.top, this.bottom, this.xstretch);

        this.style.display = this.inline ? 'inline-flex' : 'flex';
        this.style.flexDirection = 'row';
        this.style.justifyContent = this.__halign(this.center, this.end, this.stretch, this.spaceAround);
        this.style.alignItems = valign;
        this.style.alignContent = valign;
        this.style.flexWrap = this.wrap ? 'wrap' : 'nowrap';
    }

    __halign(center, end, stretch, spaceAround) {
        let temp;
        if (spaceAround) {
            temp = 'space-around';
        } else if (stretch) {
            temp = 'space-between';
        } else if (end) {
            temp = 'flex-end';
        } else if (center) {
            temp = 'center';
        } else {
            temp = 'flex-start';
        }
        return temp;
    }


    __valign(top, bottom, xstretch) {
        let temp;
        if (xstretch) {
            temp = 'stretch';
        } else if (top) {
            temp = 'flex-start';
        } else if (bottom) {
            temp = 'flex-end';
        } else {
            temp = 'center';
        }
        return temp;
    }
};

customElements.define(PTCS.Hbar.is, PTCS.Hbar);
