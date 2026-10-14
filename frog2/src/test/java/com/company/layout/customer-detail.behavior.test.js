'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync(
    'src/main/webapp/resources/js/pages/customer_detail.js',
    'utf8'
);

function classList(initial = []) {
    const values = new Set(initial);
    return {
        add(value) { values.add(value); },
        contains(value) { return values.has(value); },
        remove(value) { values.delete(value); },
        toggle(value, enabled) {
            if (enabled) values.add(value);
            else values.delete(value);
        }
    };
}

function createHarness(initialUrl) {
    const documentListeners = {};
    const windowListeners = {};
    const indicator = { style: {} };
    const tabNavigation = {
        classList: classList(),
        querySelector(selector) {
            return selector === '.tab-indicator' ? indicator : null;
        }
    };
    const environments = ['prod', 'stg', 'dev'];
    const tabs = environments.map(function(environment, index) {
        const listeners = {};
        return {
            classList: classList(environment === 'prod' ? ['active'] : []),
            listeners,
            offsetLeft: index * 80,
            offsetWidth: 80,
            tabIndex: environment === 'prod' ? 0 : -1,
            addEventListener(name, listener) { listeners[name] = listener; },
            focus() {},
            getAttribute(name) {
                return name === 'data-target' ? 'env-' + environment : null;
            },
            setAttribute() {}
        };
    });
    const panels = environments.map(function(environment) {
        return {
            classList: classList(environment === 'prod' ? ['active'] : []),
            hidden: environment !== 'prod',
            id: 'env-' + environment
        };
    });
    const root = {
        getAttribute() { return '/frog2'; },
        querySelector(selector) {
            return selector === '.tab-nav' ? tabNavigation : null;
        },
        querySelectorAll(selector) {
            if (selector === '.tab-btn') return tabs;
            if (selector === '.tab-panel') return panels;
            if (selector === '[data-detail-section]') return [];
            return [];
        }
    };
    const location = {};

    function setLocation(url) {
        const parsed = new URL(url, location.href || initialUrl);
        location.href = parsed.href;
        location.pathname = parsed.pathname;
        location.search = parsed.search;
        location.hash = parsed.hash;
    }
    setLocation(initialUrl);

    const historyCalls = [];
    const history = {
        state: null,
        pushState(state, title, url) {
            this.state = state;
            historyCalls.push({ method: 'pushState', url });
            setLocation(url);
        },
        replaceState(state, title, url) {
            this.state = state;
            historyCalls.push({ method: 'replaceState', url });
            setLocation(url);
        }
    };
    const document = {
        addEventListener(name, listener) { documentListeners[name] = listener; },
        getElementById() { return null; },
        querySelector(selector) {
            if (selector === '.customer-detail') return root;
            return null;
        }
    };
    const window = {
        addEventListener(name, listener) { windowListeners[name] = listener; },
        history,
        location,
        requestAnimationFrame(callback) { callback(); }
    };

    vm.runInNewContext(source, { document, URL, URLSearchParams, window });
    documentListeners.DOMContentLoaded();

    return { historyCalls, location, panels, setLocation, tabs, windowListeners };
}

test('environment tab navigation updates the URL and follows browser history', () => {
    const harness = createHarness(
        'https://archive.example/frog2/customers?view=detail&customerName=KT&env=stg'
    );

    assert.equal(harness.panels[1].hidden, false);
    harness.tabs[2].listeners.click();
    assert.equal(harness.location.search.includes('env=dev'), true);
    assert.equal(harness.historyCalls.at(-1).method, 'pushState');
    assert.equal(harness.panels[2].hidden, false);

    harness.setLocation(
        'https://archive.example/frog2/customers?view=detail&customerName=KT&env=prod'
    );
    harness.windowListeners.popstate();
    assert.equal(harness.panels[0].hidden, false);
    assert.equal(harness.panels[2].hidden, true);
});

test('the automatically selected default environment is canonicalized in the URL', () => {
    const harness = createHarness(
        'https://archive.example/frog2/customers?view=detail&customerName=KT'
    );

    assert.equal(harness.location.search.includes('env=prod'), true);
    assert.equal(harness.historyCalls[0].method, 'replaceState');
});


function createMissingFieldsHarness() {
    const listeners = {};
    const document = {
        activeElement: null,
        addEventListener(name, listener) { listeners[name] = listener; },
        querySelector() { return root; },
        createElement(tagName) { return element(tagName); }
    };

    function matches(node, selector) {
        if (selector.startsWith('[data-')) {
            return node.getAttribute(selector.slice(1, -1)) !== null;
        }
        return selector.startsWith('.')
            ? node.className.split(' ').includes(selector.slice(1))
            : node.tagName.toLowerCase() === selector;
    }

    function descendants(node) {
        return node.children.flatMap(child => [child, ...descendants(child)]);
    }

    function element(tagName, className = '') {
        const attributes = {};
        return {
            tagName: tagName.toUpperCase(), className, children: [], parentElement: null,
            classList: classList(), listeners: {}, hidden: false, open: false,
            appendChild(child) { child.parentElement = this; this.children.push(child); },
            setAttribute(name, value) { attributes[name] = String(value); },
            getAttribute(name) { return Object.hasOwn(attributes, name) ? attributes[name] : null; },
            addEventListener(name, listener) { this.listeners[name] = listener; },
            focus() { document.activeElement = this; },
            contains(node) { return node === this || descendants(this).includes(node); },
            closest(selector) {
                let node = this;
                while (node) {
                    if (matches(node, selector)) return node;
                    node = node.parentElement;
                }
                return null;
            },
            querySelector(selector) {
                for (const alternative of selector.split(', ')) {
                    const direct = alternative.startsWith(':scope > ');
                    const target = direct ? alternative.slice(':scope > '.length) : alternative;
                    if (target.includes(' [data-detail-section-count]')) {
                        const title = this.children.find(child => matches(child, '.detail-section-title'));
                        if (title) return title.querySelector('[data-detail-section-count]');
                    } else {
                        const found = (direct ? this.children : descendants(this))
                            .find(child => matches(child, target));
                        if (found) return found;
                    }
                }
                return null;
            },
            querySelectorAll(selector) {
                if (selector.includes('.detail-grid > .detail-item')) {
                    return descendants(this).filter(child => matches(child, '.detail-item'));
                }
                return descendants(this).filter(child => matches(child, selector));
            },
            insertAdjacentElement(position, node) {
                assert.equal(position, 'afterend');
                node.parentElement = this.parentElement;
                const index = this.parentElement.children.indexOf(this);
                this.parentElement.children.splice(index + 1, 0, node);
            }
        };
    }

    const root = element('div', 'customer-detail');
    root.setAttribute('data-context-path', '/frog2');
    root.setAttribute('data-customer-edit-url', '/frog2/customers?view=editDetail&customerName=Alpha');
    const environment = element('div', 'tab-panel');
    environment.id = 'env-stg';
    root.appendChild(environment);

    function section(collapsible, missingLabels) {
        const wrapper = element('section', 'detail-section');
        wrapper.setAttribute('data-detail-section', '');
        environment.appendChild(wrapper);
        const disclosure = collapsible ? element('details', 'detail-section-disclosure') : null;
        if (disclosure) wrapper.appendChild(disclosure);
        const title = element(collapsible ? 'summary' : 'div', 'detail-section-title');
        (disclosure || wrapper).appendChild(title);
        const count = element('button', 'detail-section-count');
        count.setAttribute('data-detail-section-count', '');
        (collapsible ? wrapper : title).appendChild(count);
        const grid = element('div', 'detail-grid');
        (disclosure || wrapper).appendChild(grid);
        const filled = element('div', 'detail-item');
        filled.setAttribute('data-detail-field-label', 'OS');
        filled.setAttribute('data-detail-field-missing', 'false');
        grid.appendChild(filled);
        for (const label of missingLabels) {
            const field = element('div', 'detail-item');
            field.setAttribute('data-detail-field-label', label);
            field.setAttribute('data-detail-field-missing', 'true');
            grid.appendChild(field);
        }
        return { wrapper, disclosure, title, count };
    }

    const collapsible = section(true, ['DB명']);
    const summary = section(false, ['SAID']);
    const complete = section(true, []);
    const context = { document, URL, URLSearchParams,
        window: { location: { href: 'https://archive.example/frog2/customers?view=detail' } } };
    vm.runInNewContext(source, context);
    context.initializeDetailSectionCounts();
    const panelFor = item => item.wrapper.querySelector('[data-detail-missing-panel]');
    const click = (item, detail = 1) => item.count.listeners.click({
        detail, preventDefault() {}, stopPropagation() {}
    });
    return { collapsible, summary, complete, document, listeners, panelFor, click };
}

test('sibling count opens a closed disclosure and preserves missing-field edit navigation', () => {
    const harness = createMissingFieldsHarness();
    const section = harness.collapsible;
    assert.equal(section.count.textContent, '1 / 2');
    assert.equal(section.disclosure.open, false);
    assert.equal(section.title.querySelector('[data-detail-section-count]'), null);
    harness.click(section);
    const panel = harness.panelFor(section);
    assert.equal(section.disclosure.open, true);
    assert.equal(panel.hidden, false);
    assert.equal(section.count.getAttribute('aria-expanded'), 'true');
    assert.equal(section.count.getAttribute('aria-controls'), panel.id);
    const destination = new URL(panel.querySelector('a').href);
    assert.equal(destination.searchParams.get('env'), 'stg');
    assert.equal(destination.searchParams.get('focus'), 'dbName');
    assert.equal(harness.complete.count.disabled, true);
});

test('opening another count closes its sibling panel and Escape restores the original action', () => {
    const harness = createMissingFieldsHarness();
    harness.click(harness.collapsible);
    harness.click(harness.summary);
    assert.equal(harness.panelFor(harness.collapsible).hidden, true);
    assert.equal(harness.collapsible.count.getAttribute('aria-expanded'), 'false');
    const panel = harness.panelFor(harness.summary);
    panel.querySelector('a').focus();
    harness.listeners.keydown({ key: 'Escape' });
    assert.equal(panel.hidden, true);
    assert.equal(harness.summary.count.getAttribute('aria-expanded'), 'false');
    assert.equal(harness.document.activeElement, harness.summary.count);
});

test('keyboard activation enters the missing-field links and Escape returns to its sibling count', () => {
    const harness = createMissingFieldsHarness();
    harness.collapsible.count.focus();
    harness.click(harness.collapsible, 0);
    const link = harness.panelFor(harness.collapsible).querySelector('a');
    assert.equal(harness.document.activeElement, link);
    harness.listeners.keydown({ key: 'Escape' });
    assert.equal(harness.document.activeElement, harness.collapsible.count);
});

test('collapsing a disclosure closes missing-field content and clears its expanded state', () => {
    const harness = createMissingFieldsHarness();
    harness.click(harness.collapsible);
    harness.collapsible.disclosure.open = false;
    harness.collapsible.disclosure.listeners.toggle();
    assert.equal(harness.panelFor(harness.collapsible).hidden, true);
    assert.equal(harness.collapsible.count.getAttribute('aria-expanded'), 'false');
});
