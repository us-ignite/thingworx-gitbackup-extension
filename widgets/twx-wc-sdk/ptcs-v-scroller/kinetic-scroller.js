// Implements a kinetic scroller (scroller with momentum)
export class KineticScroller {
    constructor(scroll) {
        this.__scroll = scroll; // Function that does the actual scrolling: scrollTo(deltaX, deltaY) => actual scrolling

        this.__trackMovement = this._trackMovement.bind(this);
        this.__autoScroll = this._autoScroll.bind(this);
        this.__touchstart = this._touchstart.bind(this);
        this.__touchmove = this._touchmove.bind(this);
        this.__touchend = this._touchend.bind(this);
        this.__touchcancel = this.abort.bind(this);

        this._vx = this._vy = 0; // Current x and y velocity
    }

    get touchstart() {
        return this.__touchstart;
    }

    abort() {
        if (this._target) {
            this._target.removeEventListener('touchmove', this.__touchmove, {passive: false});
            this._target.removeEventListener('touchend', this.__touchend, {passive: true});
            this._target.removeEventListener('touchcancel', this.__touchcancel, {passive: true});
            this._target = undefined;
        }

        if (this._trackId) {
            clearInterval(this._trackId);
            this._trackId = undefined;
            return true;
        }
        return false;
    }

    _touchstart(ev) {
        this.abort(); // Abort any previous interaction

        if (ev.defaultPrevented) {
            return;
        }

        this._target = ev.target;
        this._target.addEventListener('touchmove', this.__touchmove, {passive: false});
        this._target.addEventListener('touchend', this.__touchend, {passive: true});
        this._target.addEventListener('touchcancel', this.__touchcancel, {passive: true});

        this._x = ev.targetTouches[0].pageX;
        this._y = ev.targetTouches[0].pageY;
        this._timestamp = Date.now();
        this._tx = this._x; // Current x position for movement tracker
        this._ty = this._y; // Current y position for movement tracker
        this._trackId = setInterval(this.__trackMovement, 20);

        // This was needed on IPad/Safari before I updated to iPadOS18 (yesteray), or the document would be scrolled together with the scroller.
        // Unfortunately, if used, only the scroller could be scrolled - even if it didn't call preventDefault() when at the top or bottom.
        // Also, every interactive component had to handle touch events: there was no automatic conversion of touch events to mouse event
        // ev.preventDefault(); -- apparently not needed any longer, which is _very_ fortunate
    }

    _touchmove(ev) {
        if (!this._trackId || ev.defaultPrevented) {
            return;
        }

        const x = ev.targetTouches[0].pageX;
        const y = ev.targetTouches[0].pageY;

        // How much have we moved since the last time?
        const dx = this._x - x;
        const dy = this._y - y;

        this._x = x;
        this._y = y;

        // Returns true if the owner scrolled. If so, the event should be marked as processed
        if (this.__scroll(dx, dy) >= 1 && ev.cancelable) {
            ev.preventDefault();
        }
    }

    // Track movements to compute the velocity of the finger
    _trackMovement() {
        const timestamp = Date.now();
        const elapsed = timestamp - this._timestamp + 1; // Add 1 to make sure there won't be a division by zero...
        this._timestamp = timestamp;

        const dx = this._x - this._tx; // X movement since last sample
        const dy = this._y - this._ty; // Y movement since last sample
        this._tx = this._x;
        this._ty = this._y;

        const fx = 30 * dx / elapsed; // Force on x-velocity
        const fy = 30 * dy / elapsed; // Force on y-velocity
        this._vx = 0.6 * fx + 0.4 * this._vx; // Update x-velocity
        this._vy = 0.6 * fy + 0.4 * this._vy; // Update y-velocity
    }

    _touchend() {
        if (!this.abort()) {
            return; // Some kind of error
        }

        // If the current velocity exceeds 10 "speed units", then we want some post scrolling
        if (Math.sqrt(this._vx * this._vx + this._vy * this._vy) > 10) {
            this._timestamp = Date.now();
            requestAnimationFrame(this.__autoScroll);
        }
    }

    _autoScroll() {
        if (this._trackId) {
            return; // User is touching the screen again. Stop scrolling
        }
        const slowdown = Math.exp((this._timestamp - Date.now()) / 325);
        const dx = -this._vx * slowdown;
        const dy = -this._vy * slowdown;

        if (Math.sqrt(dx * dx + dy * dy) > 1 && this.__scroll(dx, dy) > 1) {
            requestAnimationFrame(this.__autoScroll);
        } else {
            this._vx = this._vy = 0; // Stopped scrolling
        }
    }
}
