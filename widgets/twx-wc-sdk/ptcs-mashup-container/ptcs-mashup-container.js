import {LitElement} from 'lit';
import {PTCS} from 'ptcs-library/library.js';

// The Mashupbuilder appears to miss resize changes when elements are slotted and unslotted.
// This snippet helps the mashupbuiilder to catch these elements.
// Maybe this problem should be addressed in the mashupbuilder instead
let resizeObserver;

function observeResize(container) {
    if (!resizeObserver) {
        resizeObserver = new ResizeObserver(entries => {
            const set = new Set([...entries].map(e => e.target));
            requestAnimationFrame(() => {
                set.forEach(({_mashup, offsetWidth, offsetHeight, __$oldDim}) => {
                    if (_mashup && offsetWidth > 0 && offsetHeight > 0 && (__$oldDim?.[0] !== offsetWidth || __$oldDim?.[1] !== offsetHeight)) {
                        _mashup.rootWidget.handleResponsiveWidgets();
                    }
                    if (__$oldDim) {
                        __$oldDim[0] = offsetWidth;
                        __$oldDim[1] = offsetHeight;
                    }
                });
            });
        });
    }

    resizeObserver.observe(container);
}

function unobserveResize(container) {
    if (resizeObserver) {
        resizeObserver.unobserve(container);
    }
}

const uuid = () => performance.now().toString().replace('.', '');
PTCS.MashupContainer = class extends LitElement {
    static MashupDefinitionCache = {};

    static get is() {
        return 'ptcs-mashup-container';
    }

    static get properties() {
        return {
            name: {
                type: String
            },

            parameters: {
                type: Object
            },

            _mashup: {
                type: Object
            },

            _renderStatus: {
                type: String
            }
        };
    }

    constructor() {
        super();
        console.assert(TW && TW.Runtime, 'ptcs-mashup-container can be used in TWX Runtime environment only');

        this.parameters = {};
        this._renderStatus = 'no_mashup';
    }

    connectedCallback() {
        super.connectedCallback();
        this.__connected = true;
        this.__disconnected = false;

        observeResize(this);

        if (this._renderStatus === 'stopped') {
            // Try again
            this._renderMashup(this.name);
        }
    }

    disconnectedCallback() {
        this.__connected = false;
        this.__disconnected = true;

        unobserveResize(this);

        super.disconnectedCallback();
    }

    destroy() {
        this._mashup.destroyMashup();
        this._mashup = undefined;
    }

    createRenderRoot() {
        return this;
    }

    updated(changedProperties) {
        if (changedProperties.has('name') && changedProperties.get('name') !== this.name) {
            this._renderStatus = 'started';
            this._renderMashup(this.name);
        }

        if (changedProperties.has('parameters') && this._mashup) {
            this._setParameters(this._mashup, this.parameters);
        }

        if (changedProperties.has('_renderStatus') && this._mashup) {
            this.setAttribute('render-status', this._renderStatus);
        }
    }

    // Change individual parameters
    setParameter(key, value) {
        if (this.parameters[key] !== value) {
            this.parameters[key] = value;
            if (this._mashup) {
                this._mashup.setParameter(key, value);
            }
        }
    }

    _setParameters(mashup, parameters) {
        if (mashup && parameters) {
            for (const key in parameters) {
                mashup.setParameter(key, parameters[key]);
            }
        }
    }

    _loadMashup(name) {
        return new Promise((resolve, reject) => {
            this._fetchMashup(name)
                .then(response => {
                    if (response.ok) {
                        response.json().then(v => {
                            PTCS.MashupContainer.MashupDefinitionCache[name] = v;
                            resolve(v);
                        });
                    }
                })
                .catch(error => {
                    reject(error);
                });
        });
    }

    /* istanbul ignore next */
    _fetchMashup(name) {
        const mashupRequest = new Request(`/Thingworx/Mashups/${name}`);
        const mashupRequestHeaders = new Headers();
        mashupRequestHeaders.append('Content-Type', 'application/json');
        mashupRequestHeaders.append('Accept', 'application/json');
        mashupRequestHeaders.append('x-thingworx-session', 'true');

        return fetch(mashupRequest, {
            method:  'GET',
            headers: mashupRequestHeaders
        });
    }

    _renderMashup(name) {
        if (!name) {
            this._renderStatus = 'no_mashup';
            return;
        }

        const render = definition => {
            if (!this.__connected) {
                this._renderStatus = 'stopped';
                return;
            }

            this._renderStatus = 'rendering';

            const currentMashup = TW.Runtime.Workspace.Mashups.Current;

            const mashupContent = definition.mashupContent;

            // Clean previous mashup
            this.replaceChildren();

            const root = document.createElement('div');
            const id = `mashup-${uuid()}`;
            root.id = `${id}_${TW.IDE.escapeJQueryID(name)}`;

            root.className = 'mashupcontainer-root';
            this.appendChild(root);

            // Construct the mashup object and its associated data object
            const mashup = new TW.MashupDefinition();
            mashup.dataMgr = new window.DataManager();

            mashup.rootName = id;

            mashup.htmlIdOfMashup = `#${root.id}`;
            TW.Runtime.HtmlIdOfCurrentlyLoadedMashup = mashup.htmlIdOfMashup;
            mashup.mashupName = name;
            mashup.loadFromJSON(mashupContent, definition);
            mashup.dataMgr.migrateAnyBindings(mashup);

            TW.Runtime.Workspace.Mashups.Current = mashup;

            mashup.rootWidget.appendTo($(root));

            mashup.dataMgr.loadFromMashup(mashup).then(() => {
                mashup.parameterDefinitions = definition.parameterDefinitions;
                this._setParameters(mashup, this.parameters);

                // Fire the MashupLoaded event to signal that loading is complete
                mashup.fireMashupLoadedEvent();

                this.__$oldDim = [this.offsetWidth, this.offsetHeight];

                TW.Runtime.handleMashupBeingResized(mashup);

                this._renderStatus = 'completed';

                let responsive = definition.aspects.isResponsive;
                let width =  mashup.UI.Properties.Width;
                let height = mashup.UI.Properties.Height;

                // Is this a responsive mashup with a static size?
                if (responsive) {
                    const p = (mashup.UI.Widgets && mashup.UI.Widgets[0] && mashup.UI.Widgets[0].Properties) || {};
                    if (p.positioning === 'static') {
                        const w1 = p['flex-min-width'];
                        const w2 = p['flex-max-width'];
                        const h1 = p['flex-min-height'];
                        const h2 = p['flex-max-height'];
                        if (w1 && h1 && w1 === w2 && h1 === h2) {
                            const w = PTCS.cssDecodeSize(w1);
                            const h = PTCS.cssDecodeSize(h1);
                            if (w > 0 && h > 0) {
                                // Mark this as a static mashup
                                responsive = false;
                                width = w;
                                height = h;
                            }
                        }
                    }
                }

                // eslint-disable-next-line max-len
                this.dispatchEvent(new CustomEvent('render-status', {detail: {status: this._renderStatus, static: !responsive, width, height, mashup}}));

            }).catch((error) => {
                this._renderStatus = 'failed';
                this.dispatchEvent(new CustomEvent('render-status', {detail: {status: this._renderStatus, mashup}}));
                console.error(error);
            });

            this._mashup = mashup;

            TW.Runtime.Workspace.Mashups.Current = currentMashup;
        };

        if (!PTCS.MashupContainer.MashupDefinitionCache[name]) {
            PTCS.MashupContainer.MashupDefinitionCache[name] = this._loadMashup(name);
        }

        // At this point, PTCS.MashupContainer.MashupDefinitionCache[name] can be either a mashup definition or a Promise
        Promise.resolve(PTCS.MashupContainer.MashupDefinitionCache[name])
            .then(v => {
                render(v);
            })
            .catch((error) => {
                this._renderStatus = 'failed';
                TW.Runtime.showStatusText('permission-error', `Could not load ${JSON.stringify(name)}. Reason: ${JSON.stringify(error)}`, true);
            });
    }
};

customElements.define(PTCS.MashupContainer.is, PTCS.MashupContainer);
