'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync('src/main/webapp/resources/js/pages/dashboard.js', 'utf8');

function harness(options = {}) {
    const listeners = {};
    const animations = [];
    const navigations = [];
    const storage = new Map();
    const sessionStorage = new Map();
    let focused = null;
    let resize;
    function element(classes = []) {
        const names = new Set(classes);
        const attributes = new Map();
        const events = {};
        return {
            attributes, events,
            classList: {
                add(name) { names.add(name); },
                remove(name) { names.delete(name); },
                contains(name) { return names.has(name); },
                toggle(name, force) { if (force) names.add(name); else names.delete(name); }
            },
            getAttribute(name) { return attributes.get(name) || null; },
            setAttribute(name, value) { attributes.set(name, value); },
            removeAttribute(name) { attributes.delete(name); },
            addEventListener(name, listener) { (events[name] ||= []).push(listener); },
            focus() { focused = this; },
            dispatch(name, overrides = {}) {
                const event = { button: 0, detail: 1, defaultPrevented: false,
                    preventDefault() { this.defaultPrevented = true; }, ...overrides };
                (events[name] || []).forEach(listener => listener(event));
                return event;
            }
        };
    }
    const current = options.current ?? 1;
    const links = Array.from({ length: options.count ?? 2 }, (_, index) => {
        const link = element(index === current ? ['active'] : []);
        link.href = 'http://127.0.0.1/frog2/dashboard?maintenanceMonth=2026-'
            + String(index + 1).padStart(2, '0');
        link.offsetLeft = index * 112;
        link.offsetWidth = 104;
        if (index === current) link.setAttribute('aria-current', 'page');
        return link;
    });
    const viewport = element();
    viewport.clientWidth = 336;
    const track = element();
    track.transform = 'none';
    if (!options.noAnimations) {
        track.animate = function (frames, timing) {
            const animation = {
                frames, timing, cancelled: false,
                cancel() { this.cancelled = true; },
                finish() {
                    if (!this.cancelled) track.transform = frames[1].transform;
                    // A stale callback must never navigate, even after cancellation.
                    if (this.onfinish) this.onfinish();
                }
            };
            animations.push(animation);
            return animation;
        };
    }
    const body = element();
    const personal = element();
    const toggle = element();
    const loading = { hidden: true };
    const motion = {
        matches: Boolean(options.reduced),
        addEventListener(name, listener) { this.change = listener; }
    };
    if (options.restoreFocus) sessionStorage.set('frog2.dashboard.month.focus', new URL(links[current].href).searchParams.get('maintenanceMonth'));
    if (options.collapsed) storage.set('frog2.dashboard.monthly-maintenance.collapsed', 'true');
    function store(map) {
        return {
            getItem(key) { if (options.storageDenied) throw new Error('Storage denied'); return map.get(key) ?? null; },
            setItem(key, value) { if (options.storageDenied) throw new Error('Storage denied'); map.set(key, value); },
            removeItem(key) { if (options.storageDenied) throw new Error('Storage denied'); map.delete(key); }
        };
    }
    const window = {
        matchMedia(query) { assert.equal(query, '(prefers-reduced-motion: reduce)'); return motion; },
        localStorage: store(storage), sessionStorage: store(sessionStorage),
        getComputedStyle(target) { assert.equal(target, track); return { transform: track.transform }; },
        location: { assign(href) { navigations.push(href); } },
        addEventListener(name, listener) { (listeners[name] ||= []).push(listener); }
    };
    if (!options.noResizeObserver) {
        window.ResizeObserver = class {
            constructor(callback) { resize = callback; }
            observe(target) { assert.ok(target === viewport || target === track); }
        };
    }
    vm.runInNewContext(source, {
        window, URL,
        document: {
            get activeElement() { return focused; },
            getElementById(id) {
                return { maintenanceMonthBoardBody: body,
                    toggleMaintenanceBoardBtn: toggle, maintenanceLoadingState: loading }[id] || null;
            },
            querySelector(selector) {
                return { '[data-personal-section="dashboard-maintenance"]': personal,
                    '.maintenance-month-tabs': viewport, '.maintenance-month-track': track }[selector] || null;
            },
            querySelectorAll(selector) { assert.equal(selector, '.maintenance-month-tab'); return links; }
        }
    });
    return { links, viewport, track, animations, navigations, body, personal, loading, motion,
        toggle, storage, sessionStorage,
        get focused() { return focused; },
        latest() { return animations.at(-1); },
        resize() { if (resize) resize(); else (listeners.resize || []).forEach(fn => fn()); },
        dispatch(name) { (listeners[name] || []).forEach(fn => fn()); }
    };
}

test('both endpoints center in the fixed slot without extra month links', () => {
    for (const current of [0, 5, 11]) {
        const h = harness({ count: 12, current });
        const link = h.links[current];
        const offset = Number(h.latest().frames[1].transform.match(/\(([-\d.]+)px\)/)[1]);
        assert.equal(link.offsetLeft + link.offsetWidth / 2 + offset, h.viewport.clientWidth / 2);
        assert.equal(h.latest().timing.duration, 0);
        assert.equal(h.links.length, 12);
        assert.deepEqual(h.navigations, []);
        assert.equal(h.viewport.classList.contains('is-carousel'), true);
    }
});

test('a click animates the rail before navigating to the original month filter URL', () => {
    const h = harness();
    const originalHref = h.links[0].href;
    assert.equal(h.links[0].dispatch('click').defaultPrevented, true);
    assert.equal(h.latest().timing.duration, 320);
    assert.equal(h.personal.getAttribute('aria-busy'), 'true');
    assert.equal(h.body.classList.contains('is-loading'), true);
    assert.equal(h.loading.hidden, false);
    assert.equal(h.links[0].classList.contains('active'), true);
    assert.equal(h.links[1].getAttribute('aria-current'), 'page');
    assert.deepEqual(h.navigations, []);
    h.latest().finish();
    assert.deepEqual(h.navigations, [originalHref]);
    assert.equal(h.links[0].getAttribute('aria-disabled'), 'true');
});

test('modified, non-primary and prevented clicks retain native navigation', () => {
    for (const overrides of [{ ctrlKey: true }, { metaKey: true }, { shiftKey: true },
        { altKey: true }, { button: 1 }, { button: 2 }, { defaultPrevented: true }]) {
        const h = harness();
        const event = h.links[0].dispatch('click', overrides);
        assert.equal(event.defaultPrevented, Boolean(overrides.defaultPrevented));
        assert.equal(h.animations.length, 1);
        assert.equal(h.body.classList.contains('is-loading'), false);
    }
});

test('returning to the current month cancels pending navigation and clears loading', () => {
    const h = harness();
    h.links[0].dispatch('click');
    const stale = h.latest();
    h.links[1].dispatch('click');
    stale.finish();
    h.latest().finish();
    assert.deepEqual(h.navigations, []);
    assert.equal(h.body.classList.contains('is-loading'), false);
    assert.equal(h.viewport.getAttribute('aria-busy'), 'false');
});

test('rapid selections start at the rendered position and only the last request navigates', () => {
    const h = harness({ count: 4, current: 3 });
    h.links[0].dispatch('click');
    const stale = h.latest();
    h.track.transform = 'matrix(1, 0, 0, 1, 45, 0)';
    h.links[1].dispatch('click');
    const latest = h.latest();
    assert.equal(latest.frames[0].transform, h.track.transform);
    h.links[1].dispatch('click');
    assert.equal(h.latest(), latest);
    stale.finish();
    assert.deepEqual(h.navigations, []);
    latest.finish();
    assert.deepEqual(h.navigations, [h.links[1].href]);
});

test('navigation is handed off once even if layout changes during a server response', () => {
    const h = harness();
    h.links[0].dispatch('click');
    h.latest().finish();
    h.resize();
    h.latest().finish();
    h.links[1].dispatch('click');
    assert.deepEqual(h.navigations, [h.links[0].href]);
});

test('reduced motion and a live preference change remove the travel duration', () => {
    const h = harness({ reduced: true });
    h.links[0].dispatch('click');
    assert.equal(h.latest().timing.duration, 0);
    h.latest().finish();
    assert.deepEqual(h.navigations, [h.links[0].href]);
    const live = harness();
    live.links[0].dispatch('click');
    const stale = live.latest();
    live.motion.matches = true;
    live.motion.change();
    assert.equal(live.latest().timing.duration, 0);
    stale.finish();
    live.latest().finish();
    assert.deepEqual(live.navigations, [live.links[0].href]);
});

test('arrows, Home, End and Space change real links and retain keyboard focus on navigation', () => {
    for (const [current, key, target] of [[1, 'ArrowLeft', 0], [0, 'ArrowRight', 1],
        [1, 'Home', 0], [0, 'End', 1], [1, ' ', 0]]) {
        const h = harness({ current });
        const start = key === ' ' ? 0 : current;
        assert.equal(h.links[start].dispatch('keydown', { key }).defaultPrevented, true);
        assert.equal(h.focused, h.links[target]);
        h.latest().finish();
        assert.deepEqual(h.navigations, [h.links[target].href]);
        assert.equal(h.sessionStorage.get('frog2.dashboard.month.focus'), new URL(h.links[target].href).searchParams.get('maintenanceMonth'));
    }
    const restored = harness({ restoreFocus: true });
    assert.equal(restored.focused, restored.links[1]);
    assert.equal(restored.sessionStorage.size, 0);
});

test('keyboard endpoints do not wrap and unrelated or modified keys remain native', () => {
    for (const [current, key] of [[0, 'ArrowLeft'], [1, 'ArrowRight']]) {
        const h = harness({ current });
        h.links[current].dispatch('keydown', { key });
        h.latest().finish();
        assert.deepEqual(h.navigations, []);
    }
    for (const overrides of [{ key: 'Tab' }, { key: 'Enter' }, { key: 'ArrowLeft', ctrlKey: true },
        { key: 'ArrowLeft', defaultPrevented: true }]) {
        const h = harness();
        const event = h.links[1].dispatch('keydown', overrides);
        assert.equal(event.defaultPrevented, Boolean(overrides.defaultPrevented));
        assert.equal(h.animations.length, 1);
    }
});

test('resizing centers the same endpoint instantly with or without ResizeObserver', () => {
    for (const noResizeObserver of [false, true]) {
        const h = harness({ noResizeObserver });
        h.viewport.clientWidth = 250;
        h.resize();
        const offset = Number(h.latest().frames[1].transform.match(/\(([-\d.]+)px\)/)[1]);
        assert.equal(h.links[1].offsetLeft + h.links[1].offsetWidth / 2 + offset, 125);
        assert.equal(h.latest().timing.duration, 0);
        assert.deepEqual(h.navigations, []);
    }
});

test('pagehide and pageshow clear transient state and restore the server-selected month', () => {
    const h = harness();
    h.links[0].dispatch('click');
    const stale = h.latest();
    h.dispatch('pagehide');
    stale.finish();
    assert.deepEqual(h.navigations, []);
    h.dispatch('pageshow');
    assert.equal(h.links[1].classList.contains('active'), true);
    assert.equal(h.links[0].classList.contains('active'), false);
    assert.equal(h.links[0].getAttribute('aria-disabled'), null);
    assert.equal(h.personal.getAttribute('aria-busy'), 'false');
    assert.equal(h.body.classList.contains('is-loading'), false);
    h.links[0].dispatch('click');
    h.latest().finish();
    assert.deepEqual(h.navigations, [h.links[0].href]);
});

test('collapse preferences and blocked browser storage do not interfere with month selection', () => {
    for (const options of [{ collapsed: true }, { storageDenied: true }]) {
        const h = harness(options);
        assert.equal(h.body.classList.contains('is-collapsed'), Boolean(options.collapsed));
        h.links[0].dispatch('keydown', { key: ' ' });
        h.latest().finish();
        assert.deepEqual(h.navigations, [h.links[0].href]);
        assert.equal(h.body.classList.contains('is-collapsed'), Boolean(options.collapsed));
        h.toggle.dispatch('click');
        assert.equal(h.body.classList.contains('is-collapsed'), !options.collapsed);
    }
});

test('without animation support or month data, links keep their native fallback', () => {
    const h = harness({ noAnimations: true });
    assert.equal(h.viewport.classList.contains('is-carousel'), false);
    assert.equal(h.links[0].dispatch('click').defaultPrevented, false);
    assert.equal(h.loading.hidden, false);
    h.dispatch('pageshow');
    assert.equal(h.loading.hidden, true);
    const empty = harness({ count: 0 });
    empty.dispatch('pageshow');
    empty.dispatch('pagehide');
    assert.equal(empty.animations.length, 0);
});


test('keyboard focus persistence stores only the month even when links use URL session rewriting', () => {
    const h = harness();
    h.links[0].href = h.links[0].href.replace('/dashboard?', '/dashboard;jsessionid=synthetic-session?');
    h.links[0].dispatch('click', { detail: 0 });
    h.latest().finish();
    assert.equal(h.sessionStorage.get('frog2.dashboard.month.focus'), '2026-01');
    assert.equal([...h.sessionStorage.values()].some(value => value.includes('jsessionid')), false);
});


test('only the centered month is a Tab stop in a twelve-month rail', () => {
    const h = harness({ count: 12, current: 11 });
    assert.deepEqual(h.links.filter(link => link.getAttribute('tabindex') === '0'), [h.links[11]]);
    assert.equal(h.links.filter(link => link.getAttribute('tabindex') === '-1').length, 11);
    h.links[11].dispatch('keydown', { key: 'Home' });
    assert.equal(h.focused, h.links[0]);
    assert.deepEqual(h.links.filter(link => link.getAttribute('tabindex') === '0'), [h.links[0]]);
    h.latest().finish();
    assert.deepEqual(h.navigations, [h.links[0].href]);
});

test('back navigation restores focus to the current month instead of a hidden stale selection', () => {
    for (const storageDenied of [false, true]) {
        const h = harness({ count: 12, current: 11, storageDenied });
        h.links[11].dispatch('keydown', { key: 'Home' });
        assert.equal(h.focused, h.links[0]);
        h.dispatch('pagehide');
        h.dispatch('pageshow');
        assert.equal(h.focused, h.links[11]);
        assert.equal(h.links[11].getAttribute('tabindex'), '0');
        assert.equal(h.links[0].getAttribute('tabindex'), '-1');
        h.latest().finish();
        assert.deepEqual(h.navigations, []);
    }
});
