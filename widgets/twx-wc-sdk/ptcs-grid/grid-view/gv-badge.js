// data-viewer UI for badge change marker (#badge)
function createBadge(cell, config) {
    const el = document.createElement('div');
    el.setAttribute('part', 'change-badge');
    return el;
}

function doNothing() {}

export function uiBadge(/* config */) {
    return {create: createBadge, assign: doNothing, format: null};
}
