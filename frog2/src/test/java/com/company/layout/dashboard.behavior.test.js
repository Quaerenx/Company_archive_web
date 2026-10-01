'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync('src/main/webapp/resources/js/pages/dashboard.js', 'utf8');
const flush = () => new Promise(resolve => setImmediate(resolve));

function harness(options = {}) {
    const listeners = {};
    const animations = [];
    const requests = [];
    const navigations = [];
    const storage = new Map();
    const timers = new Map();
    const pages = new Map();
    let focused = null;
    let resize;
    let nextTimer = 0;
    let expired = 0;
    function element(classes = []) {
        const names = new Set(classes);
        const attributes = new Map();
        const events = {};
        return {
            attributes, events, childNodes: [], hidden: true, inert: false,
            classList: {
                add(name) { names.add(name); },
                remove(name) { names.delete(name); },
                contains(name) { return names.has(name); },
                toggle(name, force) { if (force) names.add(name); else names.delete(name); }
            },
            getAttribute(name) { return attributes.get(name) ?? null; },
            setAttribute(name, value) { attributes.set(name, value); },
            removeAttribute(name) { attributes.delete(name); },
            addEventListener(name, listener) { (events[name] ||= []).push(listener); },
            focus() { focused = this; },
            replaceChildren(...nodes) { this.childNodes = nodes; },
            querySelector() { return null; },
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
    const month = links[current] ? new URL(links[current].href).searchParams.get('maintenanceMonth') : '2026-02';
    const viewport = element();
    viewport.clientWidth = 336;
    const track = element();
    track.transform = 'none';
    if (!options.noAnimations) {
        track.animate = function (frames, timing) {
            const animation = { frames, timing, cancelled: false,
                cancel() { this.cancelled = true; },
                finish() { if (!this.cancelled) track.transform = frames[1].transform; }
            };
            animations.push(animation);
            return animation;
        };
    }
    const body = element();
    const personal = element();
    body.childNodes = [{ month, type: 'global' }];
    personal.childNodes = [{ month, type: 'personal' }];
    const label = { textContent: month };
    const toggle = element();
    const loading = element();
    const error = element();
    const retry = element();
    const announcement = { textContent: '' };
    const documentBody = element();
    documentBody.setAttribute('data-user-id', 'fixture-owner');
    const motion = { matches: Boolean(options.reduced),
        addEventListener(name, listener) { this.change = listener; } };
    if (options.collapsed) storage.set('frog2.dashboard.monthly-maintenance.collapsed', 'true');
    const location = { href: 'http://127.0.0.1/frog2/dashboard?maintenanceMonth=' + month,
        assign(href) { navigations.push(href); } };
    const entries = [{ state: null, href: location.href }];
    let entryIndex = 0;
    const history = {
        scrollRestoration: 'auto',
        get state() { return entries[entryIndex].state; },
        pushState(state, title, url) {
            if (options.historyDenied) throw new Error('History unavailable');
            entries.splice(entryIndex + 1);
            entries.push({ state, href: new URL(url, location.href).href });
            entryIndex += 1;
            location.href = entries[entryIndex].href;
        },
        replaceState(state, title, url) {
            if (options.historyDenied) throw new Error('History unavailable');
            entries[entryIndex] = { state, href: new URL(url, location.href).href };
            location.href = entries[entryIndex].href;
        }
    };
    const window = {
        location, history, scrollX: 0, scrollY: 180,
        scrollTo(point) { this.scrollX = point.left; this.scrollY = point.top; },
        setTimeout(callback, delay) { timers.set(++nextTimer, { callback, delay }); return nextTimer; },
        clearTimeout(id) { timers.delete(id); },
        matchMedia(query) { assert.equal(query, '(prefers-reduced-motion: reduce)'); return motion; },
        localStorage: {
            getItem(key) { if (options.storageDenied) throw new Error('Storage denied'); return storage.get(key) ?? null; },
            setItem(key, value) { if (options.storageDenied) throw new Error('Storage denied'); storage.set(key, value); }
        },
        getComputedStyle(target) { assert.equal(target, track); return { transform: track.transform }; },
        addEventListener(name, listener) { (listeners[name] ||= []).push(listener); },
        AbortController,
        DOMParser: class { parseFromString(html, mime) { assert.equal(mime, 'text/html'); return pages.get(html); } },
        fetch(href, settings) {
            return new Promise((resolve, reject) => {
                requests.push({ href, settings, resolve, reject });
                if (!options.ignoreAbort) settings.signal.addEventListener('abort', () => {
                    const failure = new Error('Request aborted');
                    failure.name = 'AbortError';
                    reject(failure);
                });
            });
        },
        Frog2Session: {
            requireActiveSession(response) {
                if (response.status !== 401) return response;
                expired += 1;
                const failure = new Error('Session expired');
                failure.sessionExpired = true;
                throw failure;
            },
            isSessionExpired(failure) { return failure.sessionExpired === true; }
        }
    };
    if (options.noFetch) delete window.fetch;
    if (!options.noResizeObserver) {
        window.ResizeObserver = class {
            constructor(callback) { resize = callback; }
            observe(target) { assert.ok(target === viewport || target === track); }
        };
    }
    const document = {
        body: documentBody,
        get activeElement() { return focused; },
        importNode(node) { return { ...node }; },
        getElementById(id) { return {
            maintenanceMonthBoardBody: body, toggleMaintenanceBoardBtn: toggle,
            maintenanceLoadingState: loading,
            maintenanceMonthError: error, retryMaintenanceMonthBtn: retry,
            maintenanceMonthAnnouncement: announcement
        }[id] || null; },
        querySelector(selector) { return {
            '[data-personal-section="dashboard-maintenance"]': personal,
            '#maintenanceMonthTitle .maintenance-month-label': label,
            '.maintenance-month-tabs': viewport, '.maintenance-month-track': track
        }[selector] || null; },
        querySelectorAll(selector) { assert.equal(selector, '.maintenance-month-tab'); return links; }
    };
    vm.runInNewContext(source, { window, document, URL });
    function response(index, overrides = {}) {
        const requested = new URL(requests[index].href).searchParams.get('maintenanceMonth');
        const nextMonth = overrides.month ?? requested;
        const nextPersonal = element();
        const nextBody = element();
        nextPersonal.childNodes = [{ month: nextMonth, type: 'personal' }];
        nextBody.childNodes = [{ month: nextMonth, type: 'global' }];
        if (overrides.dataError) nextBody.querySelector = () => ({ textContent: 'Data unavailable' });
        const nextLabel = { textContent: nextMonth };
        const nextUser = element();
        nextUser.setAttribute('data-user-id', overrides.user ?? 'fixture-owner');
        const selected = element();
        selected.setAttribute('href', '?maintenanceMonth=' + nextMonth);
        const html = 'response-' + pages.size;
        pages.set(html, {
            body: nextUser,
            querySelector(selector) {
                if (overrides.missing) return null;
                return {
                    '.maintenance-month-tab[aria-current="page"]': selected,
                    '[data-personal-section="dashboard-maintenance"]': nextPersonal,
                    '#maintenanceMonthTitle .maintenance-month-label': nextLabel,
                    '#personalMaintenanceTitle .maintenance-month-label': nextLabel
                }[selector] || null;
            },
            getElementById() { return nextBody; }
        });
        return { ok: (overrides.status ?? 200) === 200, status: overrides.status ?? 200,
            headers: { get() { return overrides.contentType ?? 'text/html; charset=UTF-8'; } },
            text() { return overrides.text ?? Promise.resolve(html); }
        };
    }
    return { links, viewport, track, animations, requests, navigations, body, personal, label,
        loading, error, retry, announcement, motion, toggle, storage, timers,
        window, entries, response,
        get expired() { return expired; }, get focused() { return focused; },
        latest() { return animations.at(-1); },
        async complete(index = requests.length - 1, overrides) { requests[index].resolve(response(index, overrides)); await flush(); },
        resize() { if (resize) resize(); else (listeners.resize || []).forEach(fn => fn()); },
        dispatch(name) { (listeners[name] || []).forEach(fn => fn()); },
        back() { entryIndex -= 1; location.href = entries[entryIndex].href; this.dispatch('popstate'); },
        forward() { entryIndex += 1; location.href = entries[entryIndex].href; this.dispatch('popstate'); },
        fireTimers(delay) { for (const [id, timer] of [...timers]) if (timer.delay === delay) { timers.delete(id); timer.callback(); } }
    };
}

function assertMonth(h, expected) {
    assert.equal(h.label.textContent.trim(), expected);
    assert.equal(h.personal.childNodes[0].month, expected);
    assert.equal(h.body.childNodes[0].month, expected);
    assert.equal(new URL(h.window.location.href).searchParams.get('maintenanceMonth'), expected);
    assert.deepEqual(h.links.filter(link => link.getAttribute('aria-current') === 'page'),
        h.links.filter(link => new URL(link.href).searchParams.get('maintenanceMonth') === expected));
}

test('the first, middle and last of twelve months center in the fixed slot', () => {
    for (const current of [0, 5, 11]) {
        const h = harness({ count: 12, current });
        const link = h.links[current];
        const offset = Number(h.latest().frames[1].transform.match(/\(([-\d.]+)px\)/)[1]);
        assert.equal(link.offsetLeft + link.offsetWidth / 2 + offset, h.viewport.clientWidth / 2);
        assert.equal(h.latest().timing.duration, 0);
        assert.equal(h.viewport.classList.contains('is-carousel'), true);
        assert.equal(h.requests.length, 0);
    }
});

test('a click starts fetching during the animation and keeps the old contents until success', async () => {
    const h = harness();
    assert.equal(h.links[0].dispatch('click').defaultPrevented, true);
    assert.equal(h.latest().timing.duration, 320);
    assert.equal(h.requests.length, 1);
    assert.equal(h.requests[0].href, h.links[0].href);
    assert.equal(h.personal.getAttribute('aria-busy'), 'true');
    assert.equal(h.body.inert, true);
    assert.equal(h.personal.childNodes[0].month, '2026-02');
    assert.equal(h.body.childNodes[0].month, '2026-02');
    assert.equal(h.loading.hidden, true);
    assert.equal(h.entries.length, 1);
    await h.complete();
    assertMonth(h, '2026-01');
    assert.equal(h.entries.length, 2);
    assert.equal(h.window.scrollY, 180);
    assert.equal(h.body.inert, false);
    assert.equal(h.personal.getAttribute('aria-busy'), 'false');
    assert.equal(h.timers.size, 0);
    assert.deepEqual(h.navigations, []);
    assert.match(h.announcement.textContent, /2026-01/);
});

test('modified, non-primary and prevented clicks retain native navigation', () => {
    for (const overrides of [{ ctrlKey: true }, { metaKey: true }, { shiftKey: true },
        { altKey: true }, { button: 1 }, { button: 2 }, { defaultPrevented: true }]) {
        const h = harness();
        const event = h.links[0].dispatch('click', overrides);
        assert.equal(event.defaultPrevented, Boolean(overrides.defaultPrevented));
        assert.equal(h.requests.length, 0);
        assert.equal(h.body.inert, false);
    }
});

test('slow responses retain delayed loading announcements and existing list contents', async () => {
    const h = harness();
    h.links[0].dispatch('click');
    h.fireTimers(250);
    assert.equal(h.loading.hidden, false);
    assert.equal(h.body.childNodes.length, 1);
    await h.complete();
    assert.equal(h.loading.hidden, true);
});

test('returning to the rendered month cancels a request and ignores its late response', async () => {
    const h = harness({ ignoreAbort: true });
    h.links[0].dispatch('click');
    h.links[1].dispatch('click');
    assert.equal(h.requests[0].settings.signal.aborted, true);
    await h.complete(0);
    assertMonth(h, '2026-02');
    assert.equal(h.body.inert, false);
    assert.equal(h.viewport.getAttribute('aria-busy'), 'false');
    assert.equal(h.entries.length, 1);
    assert.equal(h.timers.size, 0);
});

test('rapid selections keep their rendered position and only the last response applies', async () => {
    const h = harness({ count: 4, current: 3, ignoreAbort: true });
    h.links[0].dispatch('click');
    h.track.transform = 'matrix(1, 0, 0, 1, 45, 0)';
    h.links[1].dispatch('click');
    assert.equal(h.latest().frames[0].transform, h.track.transform);
    h.links[1].dispatch('click');
    assert.equal(h.requests.length, 2);
    await h.complete(1);
    await h.complete(0);
    assertMonth(h, '2026-02');
    assert.equal(h.entries.length, 2);
    assert.deepEqual(h.navigations, []);
});

test('an old response body completing after a newer selection cannot overwrite it', async () => {
    const h = harness({ count: 4, current: 3 });
    let finishBody;
    const body = new Promise(resolve => { finishBody = resolve; });
    h.links[0].dispatch('click');
    h.requests[0].resolve(h.response(0, { text: body }));
    await flush();
    h.links[1].dispatch('click');
    await h.complete(1);
    finishBody('response-0');
    await flush();
    assertMonth(h, '2026-02');
});

test('a stale failure does not clear another request loading state or show an error', async () => {
    const h = harness({ count: 4, current: 3, ignoreAbort: true });
    h.links[0].dispatch('click');
    h.links[1].dispatch('click');
    h.requests[0].reject(new Error('Network unavailable'));
    await flush();
    assert.equal(h.viewport.getAttribute('aria-busy'), 'true');
    assert.equal(h.error.hidden, true);
    await h.complete(1);
    assertMonth(h, '2026-02');
});

test('network failure restores the rendered selection and retry applies the failed month', async () => {
    const h = harness();
    h.links[0].dispatch('click', { detail: 0 });
    h.requests[0].reject(new Error('Network unavailable'));
    await flush();
    assertMonth(h, '2026-02');
    assert.equal(h.error.hidden, false);
    assert.equal(h.focused, h.links[1]);
    assert.equal(h.body.inert, false);
    assert.equal(h.entries.length, 1);
    h.retry.dispatch('click');
    assert.equal(h.error.hidden, true);
    await h.complete(1);
    assertMonth(h, '2026-01');
    assert.equal(h.focused, h.links[0]);
});

test('HTTP, content type, missing markup, wrong month and data errors preserve both old sections', async () => {
    for (const overrides of [{ status: 500 }, { contentType: 'application/json' },
        { missing: true }, { month: '2026-02' }, { dataError: true }]) {
        const h = harness();
        h.links[0].dispatch('click');
        await h.complete(0, overrides);
        assertMonth(h, '2026-02');
        assert.equal(h.error.hidden, false);
        assert.equal(h.entries.length, 1);
    }
});

test('session expiry and a different authenticated user use the existing session handler', async () => {
    for (const overrides of [{ status: 401 }, { user: 'another-user' }]) {
        const h = harness();
        h.links[0].dispatch('click');
        await h.complete(0, overrides);
        assertMonth(h, '2026-02');
        assert.equal(h.expired, 1);
        assert.equal(h.error.hidden, true);
        assert.equal(h.body.inert, false);
    }
});

test('a hung request times out and can be retried without a page navigation', async () => {
    const h = harness();
    h.links[0].dispatch('click');
    h.fireTimers(15000);
    await flush();
    assert.equal(h.error.hidden, false);
    assert.equal(h.viewport.getAttribute('aria-busy'), 'false');
    h.retry.dispatch('click');
    await h.complete(1);
    assertMonth(h, '2026-01');
});

test('requests include the authenticated AJAX marker and never cache customer contents', () => {
    const h = harness();
    h.links[0].dispatch('click');
    const settings = h.requests[0].settings;
    assert.equal(settings.headers['X-Requested-With'], 'XMLHttpRequest');
    assert.equal(settings.headers.Accept, 'text/html');
    assert.equal(settings.credentials, 'same-origin');
    assert.equal(settings.cache, 'no-store');
});

test('reduced motion and a live preference change remove travel without delaying the request', async () => {
    for (const reduced of [true, false]) {
        const h = harness({ reduced });
        h.links[0].dispatch('click');
        assert.equal(h.requests.length, 1);
        if (!reduced) { h.motion.matches = true; h.motion.change(); }
        assert.equal(h.latest().timing.duration, 0);
        await h.complete();
        assertMonth(h, '2026-01');
    }
});

test('arrows, Home, End and Space select months and retain focus in the same document', async () => {
    for (const [current, key, target] of [[1, 'ArrowLeft', 0], [0, 'ArrowRight', 1],
        [1, 'Home', 0], [0, 'End', 1], [1, ' ', 0]]) {
        const h = harness({ current });
        const start = key === ' ' ? 0 : current;
        assert.equal(h.links[start].dispatch('keydown', { key }).defaultPrevented, true);
        assert.equal(h.focused, h.links[target]);
        await h.complete();
        assert.equal(h.focused, h.links[target]);
        assert.equal(h.links[target].getAttribute('aria-current'), 'page');
    }
});

test('keyboard endpoints do not wrap and unrelated or modified keys remain native', () => {
    for (const [current, key] of [[0, 'ArrowLeft'], [1, 'ArrowRight']]) {
        const h = harness({ current });
        h.links[current].dispatch('keydown', { key });
        assert.equal(h.requests.length, 0);
    }
    for (const overrides of [{ key: 'Tab' }, { key: 'Enter' }, { key: 'ArrowLeft', ctrlKey: true },
        { key: 'ArrowLeft', defaultPrevented: true }]) {
        const h = harness();
        const event = h.links[1].dispatch('keydown', overrides);
        assert.equal(event.defaultPrevented, Boolean(overrides.defaultPrevented));
        assert.equal(h.requests.length, 0);
    }
});

test('resize and repeated animation completion never start extra requests', async () => {
    for (const noResizeObserver of [false, true]) {
        const h = harness({ noResizeObserver });
        h.links[0].dispatch('click');
        h.viewport.clientWidth = 250;
        h.resize();
        const offset = Number(h.latest().frames[1].transform.match(/\(([-\d.]+)px\)/)[1]);
        assert.equal(h.links[0].offsetLeft + h.links[0].offsetWidth / 2 + offset, 125);
        h.latest().finish();
        h.latest().finish();
        assert.equal(h.requests.length, 1);
        await h.complete();
        assert.deepEqual(h.navigations, []);
    }
});

test('pagehide aborts unfinished work and pageshow restores the rendered month', async () => {
    const h = harness({ ignoreAbort: true });
    h.links[0].dispatch('click');
    h.dispatch('pagehide');
    await h.complete(0);
    h.dispatch('pageshow');
    assertMonth(h, '2026-02');
    assert.equal(h.body.inert, false);
    assert.equal(h.viewport.getAttribute('aria-busy'), 'false');
    assert.equal(h.requests.length, 1);
});

test('collapse changes during a fetch and blocked storage survive content replacement', async () => {
    for (const options of [{ collapsed: true }, { storageDenied: true }]) {
        const h = harness(options);
        h.links[0].dispatch('click');
        h.toggle.dispatch('click');
        await h.complete();
        assert.equal(h.body.classList.contains('is-collapsed'), !options.collapsed);
        assert.equal(h.toggle.getAttribute('aria-expanded'), String(Boolean(options.collapsed)));
    }
});

test('only the centered month is a Tab stop before and after a twelve-month selection', async () => {
    const h = harness({ count: 12, current: 11 });
    assert.deepEqual(h.links.filter(link => link.getAttribute('tabindex') === '0'), [h.links[11]]);
    h.links[11].dispatch('keydown', { key: 'Home' });
    assert.deepEqual(h.links.filter(link => link.getAttribute('tabindex') === '0'), [h.links[0]]);
    await h.complete();
    assert.equal(h.focused, h.links[0]);
    assert.equal(h.links[0].getAttribute('aria-current'), 'page');
});

test('back and forward reload the correct month without adding history entries or scrolling', async () => {
    const h = harness();
    h.links[0].dispatch('click');
    await h.complete();
    h.window.scrollY = 260;
    h.back();
    await h.complete(1);
    assertMonth(h, '2026-02');
    assert.equal(h.entries.length, 2);
    assert.equal(h.window.scrollY, 260);
    h.forward();
    await h.complete(2);
    assertMonth(h, '2026-01');
    assert.equal(h.entries.length, 2);
    assert.equal(h.window.history.scrollRestoration, 'manual');
});

test('failed history navigation restores the URL, and retry replaces that entry', async () => {
    const h = harness();
    h.links[0].dispatch('click');
    await h.complete();
    h.back();
    h.requests[1].reject(new Error('Offline'));
    await flush();
    assertMonth(h, '2026-01');
    assert.equal(h.error.hidden, false);
    h.retry.dispatch('click');
    await h.complete(2);
    assertMonth(h, '2026-02');
    assert.equal(h.entries.length, 2);
});

test('browser history failure does not leave partially replaced data', async () => {
    const h = harness({ historyDenied: true });
    h.links[0].dispatch('click');
    await h.complete();
    assertMonth(h, '2026-02');
    assert.equal(h.error.hidden, false);
});

test('successful updates preserve scrolling that happened while a request was in progress', async () => {
    const h = harness();
    h.links[0].dispatch('click');
    h.window.scrollY = 320;
    await h.complete();
    assert.equal(h.window.scrollY, 320);
});

test('history state contains only the selected month even when links rewrite session URLs', async () => {
    const h = harness();
    h.links[0].href = h.links[0].href.replace('/dashboard?', '/dashboard;jsessionid=synthetic-session?');
    h.links[0].dispatch('click');
    await h.complete();
    assert.deepEqual(JSON.parse(JSON.stringify(h.window.history.state)), { maintenanceMonth: '2026-01' });
});

test('missing fetch or animation support and an empty rail retain native links', () => {
    for (const options of [{ noAnimations: true }, { noFetch: true }, { count: 0 }]) {
        const h = harness(options);
        assert.equal(h.viewport.classList.contains('is-carousel'), false);
        if (h.links.length) assert.equal(h.links[0].dispatch('click').defaultPrevented, false);
        h.dispatch('pageshow');
        h.dispatch('pagehide');
        assert.equal(h.requests.length, 0);
        assert.equal(h.animations.length, 0);
    }
});


test('leaving the dashboard restores the browser scroll policy for other pages', () => {
    const h = harness();
    assert.equal(h.window.history.scrollRestoration, 'manual');
    h.dispatch('pagehide');
    assert.equal(h.window.history.scrollRestoration, 'auto');
    h.dispatch('pageshow');
    assert.equal(h.window.history.scrollRestoration, 'manual');
});
