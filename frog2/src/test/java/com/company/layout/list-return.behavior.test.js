'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync(
    'src/main/webapp/resources/js/list-return.js',
    'utf8'
);

function harness(initialValue, search = '?q=KT&page=2') {
    const listeners = new Map();
    const storage = new Map();
    if (initialValue) storage.set('frog2.listReturn.v1:user-1', initialValue);
    const classChanges = [];
    let scrolled = false;
    const list = {};
    const row = {
        classList: {
            add(name) { classChanges.push('add:' + name); },
            remove(name) { classChanges.push('remove:' + name); }
        },
        closest(selector) { return selector === '[data-ui-return-list]' ? list : null; },
        getAttribute(name) { return name === 'data-ui-return-key' ? 'row-7' : null; },
        hasAttribute() { return false; },
        scrollIntoView() { scrolled = true; }
    };
    const document = {
        readyState: 'complete',
        body: { getAttribute() { return 'user-1'; } },
        addEventListener(name, listener) { listeners.set(name, listener); },
        querySelectorAll(selector) {
            return selector === '[data-ui-return-row]' ? [row] : [];
        }
    };
    const window = {
        location: { pathname: '/customers', search },
        scrollY: 440,
        sessionStorage: {
            getItem(key) { return storage.get(key) || null; },
            setItem(key, value) { storage.set(key, value); },
            removeItem(key) { storage.delete(key); }
        },
        requestAnimationFrame(callback) { callback(); },
        setTimeout(callback) { callback(); },
        scrollTo() {}
    };
    vm.runInNewContext(source, {
        document,
        window,
        Date,
        JSON,
        Number,
        URLSearchParams
    });
    return { listeners, storage, row, classChanges, wasScrolled: () => scrolled };
}

test('list return remembers the exact row before following its link', () => {
    const state = harness();
    const link = {
        closest(selector) {
            if (selector === '[data-ui-return-row]') return state.row;
            if (selector === 'a[href]') return link;
            return null;
        }
    };

    state.listeners.get('click')({
        target: link,
        defaultPrevented: false,
        button: 0,
        metaKey: false,
        ctrlKey: false,
        shiftKey: false,
        altKey: false
    });

    const saved = JSON.parse(state.storage.get('frog2.listReturn.v1:user-1'));
    assert.equal(saved.url, '/customers?page=2&q=KT');
    assert.equal(saved.rowKey, 'row-7');
    assert.equal(saved.scrollY, 440);
});

test('list return restores and briefly highlights the saved row', () => {
    const state = harness(JSON.stringify({
        url: '/customers?page=2&q=KT',
        rowKey: 'row-7',
        scrollY: 440,
        savedAt: Date.now()
    }), '?q=KT&_flash=12345678901234567890123456789012&page=2');

    assert.equal(state.wasScrolled(), true);
    assert.deepEqual(state.classChanges, [
        'add:ui-return-highlight',
        'remove:ui-return-highlight'
    ]);
    assert.equal(state.storage.has('frog2.listReturn.v1:user-1'), false);
});

test('list return remembers a detail action linked to its summary row', () => {
    const state = harness();
    const action = {
        closest(selector) {
            return selector === '[data-ui-return-list]' ? {} : null;
        },
        getAttribute(name) {
            return name === 'data-ui-return-source-key'
                ? 'maintenance-42'
                : null;
        },
        hasAttribute() { return false; }
    };
    const link = {
        closest(selector) {
            if (selector === '[data-ui-return-row]') return null;
            if (selector === '[data-ui-return-source-key]') return action;
            if (selector === 'a[href]') return link;
            return null;
        }
    };

    state.listeners.get('click')({
        target: link,
        defaultPrevented: false,
        button: 0,
        metaKey: false,
        ctrlKey: false,
        shiftKey: false,
        altKey: false
    });

    const saved = JSON.parse(state.storage.get('frog2.listReturn.v1:user-1'));
    assert.equal(saved.rowKey, 'maintenance-42');
});
