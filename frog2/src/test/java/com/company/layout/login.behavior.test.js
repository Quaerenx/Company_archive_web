'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync(
    'src/main/webapp/resources/js/pages/login.js',
    'utf8'
);
const storageKey = 'archive.login.rememberedUserId.v1';
const now = Date.UTC(2026, 7, 18);

function createHarness(initialStorage = new Map(), storageError = false) {
    const listeners = { form: {}, remember: {} };
    const form = {
        addEventListener(name, listener) { listeners.form[name] = listener; }
    };
    const userId = { value: '' };
    const rememberId = {
        checked: false,
        addEventListener(name, listener) { listeners.remember[name] = listener; }
    };
    const elements = { loginForm: form, userId, rememberId };
    const localStorage = {
        getItem(key) {
            if (storageError) throw new Error('storage unavailable');
            return initialStorage.has(key) ? initialStorage.get(key) : null;
        },
        removeItem(key) {
            if (storageError) throw new Error('storage unavailable');
            initialStorage.delete(key);
        },
        setItem(key, value) {
            if (storageError) throw new Error('storage unavailable');
            initialStorage.set(key, value);
        }
    };
    class FixedDate extends Date {
        static now() { return now; }
    }

    vm.runInNewContext(source, {
        Date: FixedDate,
        document: { getElementById(id) { return elements[id] || null; } },
        window: { localStorage }
    });

    return { initialStorage, listeners, rememberId, userId };
}

test('loads an unexpired remembered user ID', () => {
    const stored = new Map([[storageKey, JSON.stringify({
        userId: 'archive-user',
        expiresAt: now + 1000
    })]]);
    const harness = createHarness(stored);

    assert.equal(harness.userId.value, 'archive-user');
    assert.equal(harness.rememberId.checked, true);
});

test('stores only a trimmed user ID when the form is submitted', () => {
    const harness = createHarness();
    harness.userId.value = '  archive-user  ';
    harness.rememberId.checked = true;
    harness.listeners.form.submit();

    const stored = JSON.parse(harness.initialStorage.get(storageKey));
    assert.equal(stored.userId, 'archive-user');
    assert.equal(stored.expiresAt, now + (90 * 24 * 60 * 60 * 1000));
    assert.equal(Object.hasOwn(stored, 'password'), false);
});

test('unchecking removes the remembered ID immediately', () => {
    const stored = new Map([[storageKey, '{}']]);
    const harness = createHarness(stored);
    harness.rememberId.checked = false;
    harness.listeners.remember.change();

    assert.equal(stored.has(storageKey), false);
});

test('blocked browser storage does not prevent form submission handling', () => {
    const harness = createHarness(new Map(), true);
    harness.userId.value = 'archive-user';
    harness.rememberId.checked = true;

    assert.doesNotThrow(() => harness.listeners.form.submit());
});
