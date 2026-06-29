export const NaV = Symbol('NaV'); // Not a value

const isName = name => name.match(/^[a-zA-Z_][a-zA-Z0-9_.]*$/) && name[name.length - 1] !== '.';
const isFunc = func => typeof func === 'function';
const isNumber = number => !isNaN(number);
const dashToCamelCase = dash => dash.indexOf('-') < 0 ? dash : dash.replace(/-[a-z]/g, m => m[1].toUpperCase());
const camelToDashCase = camel => camel.replace(/([A-Z])/g, '-$1').toLowerCase();
const not = v => v === NaV ? NaV : !v;
const ignoreNaV = v => v !== NaV ? v : undefined;

/* Context interface
class Context {
    addObserver(path, callback);
    get(path);
    set(path, value);
    method(methodName);
}
*/

const setAttribute = (el, name, value) => {
    if (value === undefined || value === false || value === null) {
        el.removeAttribute(name);
    } else if (value === true) {
        el.setAttribute(name, '');
    } else {
        el.setAttribute(name, value);
    }
};


function propChanged(ev, path) {
    console.assert(ev.target.$templatizeContext);
    if (ev.detail.path) {
        // path is changed as ev.detail.path. Replace the property name of the component with our property name
        ev.target.$templatizeContext.set([path, ...ev.detail.path.split('.').slice(1)].join('.'), ev.detail.value);
    } else {
        ev.target.$templatizeContext.set(path, ev.detail.value);
    }
}

export function compileExpr(expr, propNames) {
    if (expr[0] === '!') {
        const t = compileExpr(expr.substring(1), propNames);
        return isFunc(t) ? context => not(t(context)) : !t;
    }

    if (isName(expr)) {
        propNames.add(expr);
        return context => context.get(expr);
    }

    if (isNumber(expr)) {
        return Number(expr);
    }

    const lit = /^"([^"]*)"$|^'([^']*)'$/.exec(expr);
    if (lit) {
        return lit[1] || lit[2];
    }

    const func = /^([$\w]+)\((.+)\)$/.exec(expr);
    if (func) {
        const method = func[1];
        const params = func[2].split(',').map(s => compileExpr(s.trim(), propNames));
        return context => context.method(method)(...params.map(t => isFunc(t) ? ignoreNaV(t(context)) : t));
    }

    // propName.*
    const star = /^([a-zA-Z0-9_]+)\.\*$/.exec(expr);
    if (star) {
        const propName = star[1];
        propNames.add(propName);
        // Create fake change record
        return context => ({base: context.get(propName)});
    }

    throw new Error('Unexpected expression: ' + JSON.stringify(expr));
}

export function compileTemplate(template) {
    console.assert(template.tagName === 'TEMPLATE');
    console.assert(template.content.childNodes);

    const webComponents = new Set();

    function compileText(text) {
        const propNames = new Set();

        // Two-way binding?
        if (text[0] === '{' && text[1] === '{' && text[text.length - 1] === '}' && text[text.length - 2] === '}') {
            const propName = text.substring(2, text.length - 2).trim();
            if (isName(propName)) {
                propNames.add(propName);
                return {propNames, value: context => context.get(propName), twoWay: propName};
            }
            console.warn(`Invalid two-way expression: ${JSON.stringify(propName)}. Expected a property name`);
            return {propNames, value: compileExpr(propName, propNames)};
        }

        const a = []; // Result, if multi-part string
        let i = 0;
        let iNext;

        for (;; i = iNext + 2) {
            const i1 = text.indexOf('[[', i);
            if (i1 < 0) {
                break;
            }

            iNext = text.indexOf(']]', i1 + 2);
            if (iNext < 0) {
                break;
            }

            // Make sure we don't have nested [[ [[
            const i2 = text.indexOf('[[', i1 + 2);
            if (i2 >= 0 && i2 < iNext) {
                throw new Error(`Invalid value: ${text}`);
            }

            if (i < i1) {
                a.push(text.substring(i, i1));
            }

            a.push(compileExpr(text.substring(i1 + 2, iNext).trim(), propNames));
        }

        if (!i) {
            return {propNames, value: text};
        }

        if (i < text.length) {
            a.push(text.substring(i));
        }

        return {propNames, value: a.length === 1 ? a[0] : (context => a.map(t => isFunc(t) ? ignoreNaV(t(context)) : t).join(''))};
    }

    function compileAttributes($attributes) {
        const attr = [];
        const prop = [];

        [...$attributes].forEach($attr => {
            const isAttr = $attr.name[$attr.name.length - 1] === '$'; // Attribute marker?
            const name = isAttr ? $attr.name.substring(0, $attr.name.length - 1) : $attr.name;
            const {propNames, value, twoWay} = compileText($attr.value);
            if (isAttr || !isFunc(value)) {
                attr.push({name, nmsp: $attr.namespaceURI, value, propNames, twoWay});
            } else {
                prop.push({name: dashToCamelCase(name), value, propNames, twoWay});
            }
        });

        return {attr, prop};
    }

    function compileElement($el) {
        const {attr, prop} = compileAttributes($el.attributes);

        if ($el.localName.indexOf('-') > 0) {
            webComponents.add($el.localName);
        }

        // eslint-disable-next-line no-use-before-define
        return {name: $el.localName, nmsp: $el.namespaceURI, attr, prop, sub: [...$el.childNodes].map(compileNode)};
    }


    function compileNode($node) {
        switch ($node.nodeType) {
            case Node.TEXT_NODE: {
                const {propNames, value} = compileText($node.textContent);
                return {text: value, propNames};
            }

            case Node.ELEMENT_NODE:
                if ($node.tagName === 'TEMPLATE') {
                    const is = $node.getAttribute('is');
                    if (is) {
                        const {attr, prop} = compileAttributes($node.attributes);
                        const clone = document.createElement('template');
                        clone.innerHTML = $node.innerHTML; // Only way that works with both Chrome and Firefox
                        return {name: is, nmsp: $node.namespaceURI, attr, prop, sub: [{clone}]};
                    }
                    return {clone: $node};
                }
                return compileElement($node);

            default: // Comment, Processing instruction, CDATA, ...
                return {clone: $node};
        }
    }

    const ctors = [...template.content.childNodes].map($node => compileNode($node));

    ctors.whenDefined = () => {
        webComponents.delete('ptcs-grid-column-def'); // Not a web component
        const a = [...webComponents].filter(wc => customElements.get(wc)); // Exclude any already defiend component
        if (a.length > 0) {
            console.log('waiting for ' + a.join(', ') + '.');
        }
        return Promise.all(a.map(wc => customElements.whenDefined(wc)));
    };

    return ctors;
}


export function createNodes(ctors, assign) {
    function createNode(ctor) {
        const {clone, text} = ctor;
        if (clone) {
            return clone.cloneNode(true);
        }
        if (text) {
            if (isFunc(text)) {
                const textNode = document.createTextNode(' ');
                const {propNames} = ctor;

                const f = context => {
                    console.assert(!textNode.$templatizeContext || textNode.$templatizeContext === context);
                    const oldContext = textNode.$templatizeContext;
                    textNode.$templatizeContext = context;

                    const value = text(context);
                    if (value !== NaV) {
                        textNode.textContent = value;
                    }

                    if (oldContext === textNode.$templatizeContext) {
                        return;
                    }

                    // (el, name) => [assign, changed, twoWay]
                    const update = () => {
                        textNode.textContent = ignoreNaV(text(context));
                    };

                    propNames.forEach(propName => context.addObserver(propName, update));
                };

                assign.push(f);

                return textNode;
            }
            return document.createTextNode(text);
        }

        const el = ctor.nmsp ? document.createElementNS(ctor.nmsp, ctor.name) : document.createElement(ctor.name);

        // Assign element to a context
        if (ctor.prop.length > 0 || ctor.attr.length > 0) {
            assign.push(context => {
                const oldContext = el.$templatizeContext;
                console.assert(!oldContext || oldContext === context); // For now, don't allow client to change context for already stamped content
                el.$templatizeContext = context;

                ctor.attr.forEach(({name, value, propNames, twoWay}) => {
                    if (!isFunc(value)) {
                        if (oldContext) {
                            return; // Alredy assigned
                        }
                        if (name.startsWith('on-')) {
                            el.addEventListener(`${name.substring(3)}`, ev => context.method(value)(ev));
                        } else {
                            setAttribute(el, name, value);
                        }
                        return;
                    }

                    const v = value(context);
                    if (v !== NaV) {
                        setAttribute(el, name, v);
                    }

                    if (oldContext) {
                        return;
                    }

                    const f = () => setAttribute(el, name, ignoreNaV(value(context)));

                    propNames.forEach(propName => context.addObserver(propName, f));

                    if (twoWay) {
                        el.addEventListener(`${name}-changed`, ev => propChanged(ev, twoWay));
                    }
                });

                ctor.prop.forEach(({name, value, propNames, twoWay}) => {
                    console.assert(isFunc(value));

                    const v = value(context);
                    if (v !== NaV) {
                        el[name] = v;
                    }

                    if (oldContext) {
                        return;
                    }

                    const f = () => {
                        el[name] = ignoreNaV(value(context));
                    };

                    propNames.forEach(propName => context.addObserver(propName, f));

                    if (twoWay) {
                        el.addEventListener(`${camelToDashCase(name)}-changed`, ev => propChanged(ev, twoWay));
                    }
                });
            });
        }

        ctor.sub.forEach(child => el.appendChild(createNode(child)));

        return el;
    }

    return ctors.map(ctor => createNode(ctor));
}
