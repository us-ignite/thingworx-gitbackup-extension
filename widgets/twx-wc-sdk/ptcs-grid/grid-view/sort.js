import {PTCS} from 'ptcs-library/library.js';
import 'ptcs-icons/cds-icons.js';

const iconName = order => ({asc: 'cds:icon_ascending_mini', desc: 'cds:icon_descending_mini'}[order] || 'cds:icon_reorder_mini');

function actionEv(ev) {
    const hitArea = ev.target.closest('[part=hit-area]');
    const icon = hitArea && hitArea.firstElementChild;
    if (!icon || icon.disabled || hitArea.disabled) {
        return;
    }

    ev.preventDefault();

    icon.dispatchEvent(new CustomEvent('sort-action', {bubbles: true}));
}

export const sortIcon = (name, order = 'none') => {
    const icon = document.createElement('ptcs-icon');
    icon.setAttribute('part', 'sort-icon');
    icon.name = name;
    icon.icon = iconName(order);

    // Add sortOrder property that controls the sort icon
    Object.defineProperty(icon, 'sortOrder', {
        get: () => order,
        set: newOrder => {
            if (newOrder !== order) {
                order = newOrder;
                icon.icon = iconName(order);
            }
        }
    });

    const hitArea = document.createElement('div');
    hitArea.setAttribute('part', 'hit-area');
    hitArea.setAttribute('grid-action', '');
    hitArea.setAttribute('style-focus', ''); // Need help with focus styling
    hitArea.appendChild(icon);

    hitArea.addEventListener('click', ev => PTCS.wrongMouseButton(ev) || actionEv(ev));
    hitArea.addEventListener('keydown', ev => (ev.key === ' ' || ev.key === 'Enter') && actionEv(ev));

    if (!PTCS.isIOS) {
        // Trigger highlight of icon via a state attribute 'hit' styling the icon same as when hovered
        hitArea.addEventListener('mouseenter', ev => ev.target.firstChild.setAttribute('hit', ''));
        hitArea.addEventListener('mouseleave', ev => ev.target.firstChild.removeAttribute('hit'));
    }

    // Workaround to prevent Chrome from selecting the next element when double-clicking on the icon
    hitArea.addEventListener('mousedown', ev => PTCS.wrongMouseButton(ev) || ev.preventDefault());

    return hitArea;
};
