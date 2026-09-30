'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync(
    'src/main/webapp/resources/js/form-drafts.js',
    'utf8'
);

function createStorage(initial = {}) {
    const values = new Map(Object.entries(initial));
    const writes = [];
    const removals = [];
    return {
        writes,
        removals,
        getItem(key) {
            return values.has(key) ? values.get(key) : null;
        },
        removeItem(key) {
            removals.push(key);
            values.delete(key);
        },
        setItem(key, value) {
            writes.push(key);
            values.set(key, String(value));
        },
        values
    };
}

function eventTarget(target = {}) {
    const listeners = new Map();
    target.addEventListener = (name, listener) => {
        if (!listeners.has(name)) listeners.set(name, []);
        listeners.get(name).push(listener);
    };
    target.dispatch = (name, event = {}) => {
        (listeners.get(name) || []).forEach((listener) => listener(event));
    };
    return target;
}

function createField({name, value = '', type = 'text', excluded = false}) {
    return {
        checked: false,
        disabled: false,
        name,
        type,
        value,
        dispatchEvent() {},
        focus() {
            this.focused = true;
        },
        hasAttribute(attribute) {
            return excluded && attribute === 'data-ui-draft-exclude';
        }
    };
}

function createHarness({storedDraft = null, sensitive = false,
        hidden = false, immediateTimers = true} = {}) {
    const key = 'frog2.formDraft.v1:user-1:%2Ffrog2%2Fmeeting:meeting%3Anew';
    const storage = createStorage(storedDraft ? {
        [key]: JSON.stringify(storedDraft)
    } : {});
    const title = createField({name: 'title'});
    const csrf = createField({name: 'csrfToken', value: 'secret', type: 'hidden'});
    const excluded = createField({
        name: 'credentialHint', value: 'do-not-save', excluded: true
    });
    const password = sensitive
        ? createField({name: 'password', value: 'secret', type: 'password'})
        : null;
    const inserted = [];
    const form = eventTarget({
        elements: [title, csrf, excluded, password].filter(Boolean),
        parentNode: {
            insertBefore(element) {
                inserted.push(element);
            }
        },
        checkValidity() {
            return true;
        },
        getAttribute(name) {
            if (name === 'data-ui-draft-id') return 'meeting:new';
            if (name === 'data-ui-draft-success-views') return 'list,view';
            return null;
        },
        querySelector(selector) {
            if (selector === 'input[type="password"], input[type="file"]') {
                return password;
            }
            return title;
        }
    });
    const document = eventTarget({
        readyState: 'complete',
        hidden,
        body: {
            getAttribute(name) {
                return name === 'data-user-id' ? 'user-1' : null;
            }
        },
        createElement() {
            const element = eventTarget({
                children: [],
                appendChild(child) {
                    this.children.push(child);
                    return child;
                },
                focus() {},
                remove() {
                    this.removed = true;
                },
                setAttribute() {}
            });
            return element;
        },
        querySelectorAll(selector) {
            return selector === 'form[data-ui-draft="auto"]' ? [form] : [];
        }
    });
    const intervals = new Map();
    const timeouts = new Map();
    let timerId = 0;
    const window = eventTarget({
        location: {pathname: '/frog2/meeting', search: '?view=write'},
        sessionStorage: storage,
        clearTimeout(id) { timeouts.delete(id); },
        clearInterval(id) { intervals.delete(id); },
        setInterval(callback) {
            const id = ++timerId;
            intervals.set(id, callback);
            return id;
        },
        setTimeout(callback) {
            const id = ++timerId;
            if (immediateTimers) callback();
            else timeouts.set(id, callback);
            return id;
        }
    });
    class BrowserEvent {
        constructor(type) {
            this.type = type;
        }
    }
    vm.runInNewContext(source, {
        document,
        window,
        Event: BrowserEvent,
        URLSearchParams
    });
    return {document, excluded, form, inserted, intervals, key, password,
        storage, timeouts, title, window};
}

test('drafts are scoped and omit hidden or explicitly excluded values', async () => {
    const harness = createHarness();
    harness.title.value = '복구할 회의록';
    harness.form.dispatch('input');

    const draft = JSON.parse(harness.storage.getItem(harness.key));
    assert.deepEqual(draft.values, {title: '복구할 회의록'});

    harness.form.dispatch('submit', {defaultPrevented: false});
    await new Promise((resolve) => setImmediate(resolve));
    const pending = JSON.parse(harness.storage.getItem(
        'frog2.formDraft.pending.v1'));
    assert.equal(pending[0].key, harness.key);
    assert.deepEqual(pending[0].successViews, ['list', 'view']);
});

test('an unexpired draft can be restored and credential forms are ignored', () => {
    const stored = {savedAt: Date.now(), values: {title: '저장된 초안'}};
    const harness = createHarness({storedDraft: stored});
    assert.equal(harness.inserted.length, 1);
    harness.intervals.forEach((callback) => callback());
    assert.notEqual(harness.storage.getItem(harness.key), null);

    const banner = harness.inserted[0];
    const restore = banner.children[1].children[0];
    restore.dispatch('click');
    assert.equal(harness.title.value, '저장된 초안');
    assert.equal(banner.removed, true);

    const sensitive = createHarness({sensitive: true});
    sensitive.title.value = '저장하면 안 됨';
    sensitive.form.dispatch('input');
    assert.equal(sensitive.storage.getItem(sensitive.key), null);
});


test('drafts avoid duplicate storage writes and remove reverted changes once', () => {
    const harness = createHarness();
    harness.title.value = 'Edited draft';
    harness.form.dispatch('input');
    harness.form.dispatch('change');
    harness.intervals.forEach((callback) => callback());
    assert.equal(harness.storage.writes.filter((key) => key === harness.key).length, 1);
    harness.title.value = '';
    harness.form.dispatch('input');
    harness.form.dispatch('change');
    harness.intervals.forEach((callback) => callback());
    assert.equal(harness.storage.getItem(harness.key), null);
    assert.equal(harness.storage.removals.filter((key) => key === harness.key).length, 1);
});

test('drafts still detect programmatic changes without input events', () => {
    const harness = createHarness();
    harness.title.value = 'Programmatically inserted template';
    harness.intervals.forEach((callback) => callback());
    assert.equal(JSON.parse(harness.storage.getItem(harness.key)).values.title,
        'Programmatically inserted template');
});

test('drafts flush pending edits and pause timers while hidden or outside the page', () => {
    const harness = createHarness({immediateTimers: false});
    assert.equal(harness.intervals.size, 1);
    harness.title.value = 'Pending draft';
    harness.form.dispatch('input');
    assert.equal(harness.timeouts.size, 1);
    harness.document.hidden = true;
    harness.document.dispatch('visibilitychange');
    assert.equal(harness.intervals.size, 0);
    assert.equal(harness.timeouts.size, 0);
    assert.equal(JSON.parse(harness.storage.getItem(harness.key)).values.title,
        'Pending draft');
    harness.title.value = 'Changed while hidden';
    harness.form.dispatch('input');
    assert.equal(harness.timeouts.size, 0);
    harness.document.hidden = false;
    harness.document.dispatch('visibilitychange');
    assert.equal(harness.intervals.size, 1);
    assert.equal(JSON.parse(harness.storage.getItem(harness.key)).values.title,
        'Changed while hidden');
    harness.window.dispatch('pagehide');
    assert.equal(harness.intervals.size, 0);
    harness.window.dispatch('pageshow');
    harness.window.dispatch('pageshow');
    assert.equal(harness.intervals.size, 1);
});

test('drafts do not start periodic timers when initially hidden', () => {
    const harness = createHarness({hidden: true});
    assert.equal(harness.intervals.size, 0);
    harness.document.hidden = false;
    harness.document.dispatch('visibilitychange');
    assert.equal(harness.intervals.size, 1);
});
