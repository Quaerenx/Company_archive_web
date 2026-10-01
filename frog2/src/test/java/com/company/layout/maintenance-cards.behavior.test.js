'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync(
    'src/main/webapp/resources/js/pages/maintenance_cards.js', 'utf8'
);

function harness(cardCount = 2, hasMonthForm = false) {
    const listeners = { document: {}, window: {} };
    function listen(target, name, listener) {
        (listeners[target][name] ||= []).push(listener);
    }
    let submissions = 0;
    const monthListeners = [];
    const month = {
        value: '2026-09', defaultValue: '2026-09', valid: true,
        checkValidity() { return this.valid; },
        addEventListener(name, listener) { assert.equal(name, 'change'); monthListeners.push(listener); },
        change() { monthListeners.forEach(listener => listener.call(this)); }
    };
    const monthForm = {requestSubmit() { submissions++; }};
    const cards = Array.from({ length: cardCount }, (_, index) => {
        const classes = new Set(['customer-card', index === 0 ? 'synthetic-personal' : 'synthetic-global']);
        const events = {};
        return {
            href: '/frog2/maintenance?view=history&customerName=synthetic-' + index,
            classList: {
                add(name) { classes.add(name); },
                remove(name) { classes.delete(name); },
                contains(name) { return classes.has(name); }
            },
            addEventListener(name, listener) { (events[name] ||= []).push(listener); },
            click(overrides = {}) {
                const event = { button: 0, detail: 1, defaultPrevented: false,
                    ctrlKey: false, metaKey: false, shiftKey: false, altKey: false,
                    preventDefault() { this.defaultPrevented = true; }, ...overrides };
                (events.click || []).forEach(listener => listener.call(this, event));
                return event;
            }
        };
    });
    vm.runInNewContext(source, {
        document: {
            addEventListener(name, listener) { listen('document', name, listener); },
            querySelector(selector) { assert.equal(selector, '.maintenance-month-form'); return hasMonthForm ? monthForm : null; },
            getElementById(id) { assert.equal(id, 'maintenanceMonth'); return hasMonthForm ? month : null; },
            querySelectorAll(selector) { assert.equal(selector, '.customer-card'); return cards; }
        },
        window: { addEventListener(name, listener) { listen('window', name, listener); } }
    });
    (listeners.document.DOMContentLoaded || []).forEach(listener => listener());
    return {
        cards, month,
        get submissions() { return submissions; },
        show(persisted) { (listeners.window.pageshow || []).forEach(listener => listener({ persisted })); }
    };
}

test('ordinary and keyboard activation keep native navigation and mark only the activated card', () => {
    for (const detail of [1, 0]) {
        const { cards } = harness();
        const href = cards[0].href;
        const event = cards[0].click({ detail });
        assert.equal(event.defaultPrevented, false);
        assert.equal(cards[0].href, href);
        assert.equal(cards[0].classList.contains('is-loading'), true);
        assert.equal(cards[1].classList.contains('is-loading'), false);
    }
});

test('modified, middle and cancelled clicks leave the current page unchanged', () => {
    for (const overrides of [
        { ctrlKey: true }, { metaKey: true }, { shiftKey: true }, { altKey: true },
        { button: 1 }, { button: 2 }, { defaultPrevented: true }
    ]) {
        const { cards } = harness();
        const event = cards[0].click(overrides);
        assert.equal(cards[0].classList.contains('is-loading'), false);
        assert.equal(event.defaultPrevented, Boolean(overrides.defaultPrevented));
    }
});

test('restoring the same DOM from bfcache clears loading without rebuilding cards', () => {
    const { cards, show } = harness();
    cards[0].click();
    const original = cards[0];
    show(true);
    assert.equal(cards[0], original);
    assert.equal(cards[0].classList.contains('is-loading'), false);
    assert.equal(cards[0].classList.contains('synthetic-personal'), true);
    assert.equal(cards[1].classList.contains('synthetic-global'), true);
});

test('restored cards can navigate again and repeated pageshow clears all transient loading states', () => {
    const { cards, show } = harness();
    cards.forEach(card => card.click());
    show(true);
    assert.equal(cards.every(card => !card.classList.contains('is-loading')), true);
    cards[1].click();
    assert.equal(cards[1].classList.contains('is-loading'), true);
    show(true);
    show(true);
    assert.equal(cards.every(card => !card.classList.contains('is-loading')), true);
});

test('pageshow also clears stale loading on an ordinary page restore', () => {
    const { cards, show } = harness();
    cards[0].click();
    show(false);
    assert.equal(cards[0].classList.contains('is-loading'), false);
});

test('empty cards pages initialize and restore without an error', () => {
    const { cards, show } = harness(0);
    show(false);
    show(true);
    assert.equal(cards.length, 0);
});


test('a different valid month submits the native GET form', () => {
    const h = harness(2, true);
    h.month.value = '2026-10';
    h.month.change();
    assert.equal(h.submissions, 1);
});

test('unchanged and invalid month values do not start navigation', () => {
    const h = harness(2, true);
    h.month.change();
    h.month.value = '';
    h.month.valid = false;
    h.month.change();
    assert.equal(h.submissions, 0);
});

test('back-forward cache restores the month represented by the rendered cards', () => {
    const h = harness(2, true);
    h.month.value = '2026-10';
    h.cards[0].click();
    h.show(true);
    assert.equal(h.month.value, '2026-09');
    assert.equal(h.cards[0].classList.contains('is-loading'), false);
});

test('ordinary pageshow keeps a month edit that has not been submitted', () => {
    const h = harness(2, true);
    h.month.value = '2026-10';
    h.show(false);
    assert.equal(h.month.value, '2026-10');
});
