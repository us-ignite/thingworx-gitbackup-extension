export function getFocusHiliteEl(e) {
    if (!e.__elHilite) {
        return e;
    }
    if (typeof e.__elHilite === 'function') {
        return e.__elHilite();
    }
    return e.__elHilite;
}
