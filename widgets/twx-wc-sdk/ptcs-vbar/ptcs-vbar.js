import {LitElement} from 'lit';
import {L2Pw} from 'ptcs-library/library-lit';
import {PTCS} from 'ptcs-library/library.js';

PTCS.Vbar = class extends L2Pw(LitElement) {
    static get is() {
        return 'ptcs-vbar';
    }

    static get properties() {
        return {
            // inline
            inline:
                {
                    type:  Boolean,
                    value: false
                },

            // valign
            center:
                {
                    type:  Boolean,
                    value: false
                },

            bottom:
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

            // halign
            start:
                {
                    type:  Boolean,
                    value: false
                },

            end:
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

        const halign = this.__halign(this.start, this.end, this.xstretch);

        this.style.display = this.inline ? 'inline-flex' : 'flex';
        this.style.flexDirection = 'column';
        this.style.justifyContent = this.__valign(this.center, this.bottom, this.stretch, this.spaceAround);
        this.style.alignItems = halign;
        this.style.alignContent = halign;
        this.style.flexWrap = this.wrap ? 'wrap' : 'nowrap';
    }

    __valign(center, bottom, stretch, spaceAround) {
        let temp;
        if (spaceAround) {
            temp = 'space-around';
        } else if (stretch) {
            temp = 'space-between';
        } else if (bottom) {
            temp = 'flex-end';
        } else if (center) {
            temp = 'center';
        } else {
            temp = 'flex-start';
        }
        return temp;
    }


    __halign(start, end, xstretch) {
        let temp;
        if (xstretch) {
            temp = 'stretch';
        } else if (start) {
            temp = 'flex-start';
        } else if (end) {
            temp = 'flex-end';
        } else {
            temp = 'center';
        }
        return temp;
    }
};

customElements.define(PTCS.Vbar.is, PTCS.Vbar);
