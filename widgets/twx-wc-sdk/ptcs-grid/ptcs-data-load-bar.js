import {LitElement, html, css} from 'lit';
import {when} from 'lit/directives/when.js';
import {styleMap} from 'lit/directives/style-map.js';
import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-behavior-styleable/ptcs-behavior-styleable.js';
import 'ptcs-image/ptcs-image.js';


PTCS.DataLoadBar = class extends PTCS.BehaviorStyleable(LitElement) {
    static get styles() {
        return css`
        :host {
            display: inline-block;
        }

        [part=bar-container] {
          position: absolute;
          align-items: center;
          justify-content: center;
        }

        :host(:not([show-bar])) [part=bar-container] {
          display: none;
        }

        :host([show-bar]) [part=bar-container] {
          display: flex;
        }

        :host([image-option]) [part=track] {
          display: none;
        }

        :host(:not([image-option])) [part=image] {
          display: none;
        }

        [part=track] {
           overflow: hidden;
        }

        [part=slider] {
           position: relative;
           animation: slider 2s linear 0s infinite normal both;
        }

        @keyframes slider {
          from {left: 0%;}
          to {left: 100%;}
        }

        [part=image] {
           animation: fadeIn 1s infinite alternate;
        }

        @keyframes fadeIn {
           from { opacity: 0.25; }
        }`;
    }

    render() {
        const {size} = this;
        const center = `calc(50% - ${size / 2}px)`;
        const length = `${size}px`;

        return html`<div part="bar-container" style=${styleMap({height: length, width: length, left: center, top: center})}>
            <div part="track"><div part="slider"></div></div>
            ${when(this.image, () => html`<ptcs-image part="image" size="contain" position="center" src=${this.image} no-placeholder
                @load=${this._loadSuccess} @error=${this._loadError}></ptcs-image>`)}
        </div>`;
    }

    static get is() {
        return 'ptcs-data-load-bar';
    }

    static get properties() {
        return {
            // Show custom image instead of the default load indicator?
            imageOption: {
                type:      Boolean,
                attribute: 'image-option',
                reflect:   true
            },

            // Image to display instead of the default animated bar
            image: {
                type: String
            },

            // Container size for the load indicator (bar / image)
            size: {
                type: Number
            },

            // Delay in ms until the data loading is shown
            delay: {
                type: Number
            },

            // Flag to display data loading / progress bar indicator
            showBar: {
                type: Boolean
            }
        };
    }

    constructor() {
        super();
        this.size = 200;
        this.delay = 0;
    }

    updated(changedProperties) {
        super.updated(changedProperties);

        if (changedProperties.has('showBar')) {
            this._showBarChanged(this.showBar);
        }

        if (changedProperties.has('image') && !this.image) {
            this.imageOption = false;
        }
    }

    _showBarChanged(showBar) {
        if (showBar) {
            setTimeout(() => PTCS.setbattr(this, 'show-bar', this.showBar), this.delay);
        } else {
            this.removeAttribute('show-bar');
        }
    }

    _loadSuccess() {
        this.imageOption = true;
    }

    _loadError() {
        this.imageOption = false;
    }

};

customElements.define(PTCS.DataLoadBar.is, PTCS.DataLoadBar);
