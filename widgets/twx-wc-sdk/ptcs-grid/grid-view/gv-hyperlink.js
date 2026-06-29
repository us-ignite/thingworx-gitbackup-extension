// data-viewer UI for HYPERLINK
import 'ptcs-link/ptcs-link';
import 'ptcs-textfield/ptcs-textfield';

const nullLink = {};

function decode(value) {
    switch (typeof value) {
        case 'object':
            return value !== null ? {href: value.href, label: value.label} : nullLink;
        case 'string':
            return {href: value, label: value};
    }
    return nullLink;
}

function createLink(config) {
    const el = document.createElement('ptcs-link');
    el.singleLine = config.singleLineRows;
    el._zeroPaddingNoScroll = true;
    el._disScrollOnPtcsLabelEllipsMultiLine = true;
    el._disScrollOnPtcsLabelMaxHeight = config.maxHeightRow;
    el.style.minHeight = config.minHeightRow + 'px';

    // Disable the styling of the ptcs-label and the <a> within the link
    el.variant = 'grid-item';

    // eslint-disable-next-line no-nested-ternary
    el.verticalAlignment = config.valign === 'top' ? 'flex-start' : (config.valign === 'bottom' ? 'flex-end' : config.valign);

    if (config.target) {
        el.target = config.target;
    }

    el.setAttribute('part', 'cell-link state-value');
    el.setAttribute('tabindex', '-1');
    el.setAttribute('grid-action', '');

    return el;
}

// Assign new value to ptcs-link
function assignLink(el, value, index, dataManager) {
    const {label, href} = decode(value);
    el.href = href;
    el.label = label;
}

// Extract link text(s)
function linkText(value) {
    if (value === null) {
        return null;
    }
    return typeof value === 'object' ? value.label : value;
}

export function uiHyperlink(_config) {
    const config = (_config && typeof _config === 'object') ? _config : {};
    return {create: () => createLink(config), assign: assignLink, format: linkText, externalEdit: true};
}
