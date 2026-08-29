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
