// data-viewer UI for IMAGELINK

import {assignHeightMinusCellPadding} from './gv-text';
import 'ptcs-image/ptcs-image';

function setMinH(img, minHeight) {
    img.style.minHeight = minHeight + 'px';
}

function setMaxH(img, maxHeight) {
    img.style.maxHeight = maxHeight + 'px';
}

function createImagelink(config) {
    const img = document.createElement('ptcs-image');
    img.setAttribute('part', 'cell-image');
    // remove default tabindex
    img.noTabindex = true;

    if (config) {
        img.position = config.position ? config.position : config.valign;
        if (config.size) {
            img.size = config.size;
        }
        assignHeightMinusCellPadding(img, config.minHeightRow, config.maxHeightRow, setMinH, setMaxH);
    }
    return img;
}

function updateImagelink(el, value) {
    el.noPlaceholder = !value; // Don't show placeholder if there is no image
    el.src = value;
}

// Don't generate text for images
function imageText() {
    return [];
}

export function uiImagelink(_config) {
    const config = (_config && typeof _config === 'object') ? _config : {};
    return {create: () => createImagelink(config), assign: updateImagelink, format: imageText, externalEdit: true};
}
