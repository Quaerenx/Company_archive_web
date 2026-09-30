'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync(
    'src/main/webapp/resources/js/pages/maintenance_cards.js', 'utf8'
);

function harness(cardCount = 2) {
    const listeners = { document: {}, window: {} };
    function listen(target, name, listener) {
        (listeners[target][name] ||= []).push(listener);
    }
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
            querySelectorAll(selector) { assert.equal(selector, '.customer-card'); return cards; }
        },
        window: { addEventListener(name, listener) { listen('window', name, listener); } }
    });
    (listeners.document.DOMContentLoaded || []).forEach(listener => listener());
    return {
        cards,
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
