'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync(
    'src/main/webapp/resources/js/pages/customer_detail_edit.js',
    'utf8'
);

class ClassList {
    constructor(initial = []) {
        this.values = new Set(initial);
    }

    add(value) { this.values.add(value); }
    remove(value) { this.values.delete(value); }
    contains(value) { return this.values.has(value); }
    toggle(value, force) {
        if (force) this.values.add(value);
        else this.values.delete(value);
    }
}

function eventTarget(attributes = {}) {
    const listeners = new Map();
    return {
        attributes: new Map(Object.entries(attributes)),
        classList: new ClassList(),
        disabled: false,
        style: {},
        addEventListener(name, listener) { listeners.set(name, listener); },
        dispatch(name, event = {}) {
            const listener = listeners.get(name);
            if (listener) listener(event);
        },
        getAttribute(name) { return this.attributes.get(name) || null; },
        removeAttribute(name) { this.attributes.delete(name); },
        setAttribute(name, value) { this.attributes.set(name, String(value)); }
    };
}

function createHarness() {
    const documentListeners = new Map();
    const windowListeners = new Map();
    const historyCalls = [];
    const confirmMessages = [];

    const indicator = { style: {} };
    const tabNavigation = {
        classList: new ClassList(),
        querySelector(selector) {
            return selector === '.tab-indicator' ? indicator : null;
        }
    };
    const tabs = ['prod', 'stg', 'dev'].map((environment, index) => {
        const tab = eventTarget({
            'data-environment': environment,
            'data-target': `env-${environment}-edit`
        });
        tab.offsetLeft = index * 100;
        tab.offsetWidth = 100;
        tab.focused = false;
        tab.focus = function() { this.focused = true; };
        return tab;
    });
    const panels = ['prod', 'stg', 'dev'].map(environment => {
        const panel = eventTarget();
        panel.id = `env-${environment}-edit`;
        panel.hidden = environment !== 'prod';
        return panel;
    });
    const forms = ['prod', 'stg', 'dev'].map(environment => {
        const form = eventTarget({
            'data-environment': environment,
            'data-environment-label': environment === 'prod'
                ? '운영'
                : environment === 'stg' ? '스테이징' : '개발'
        });
        const customerName = { value: 'Acme' };
        const fieldValues = environment === 'prod'
            ? { dbMode: 'EON', mcYn: 'Y' }
            : { dbMode: 'ENT', mcYn: 'N' };
        const fields = new Map();
        [
            'dbMode', 'depotArea', 'objectArea', 'storageNetwork',
            'verticaAdmin', 'mcYn', 'customResourcePoolYn', 'mcVersion',
            'subclusterYn', 'mcHost', 'backupYn', 'mcAdmin', 'backupNote'
        ].forEach(name => {
            const field = eventTarget();
            const control = eventTarget();
            control.name = name;
            control.value = fieldValues[name] || `${name}-value`;
            field.querySelector = function(selector) {
                return selector === 'input, select, textarea' ? control : null;
            };
            control.closest = function(selector) {
                return selector === '[data-customer-detail-field]'
                    ? field
                    : null;
            };
            fields.set(name, { control, field });
        });
        const mirrors = [];
        function sectionFor(fieldNames) {
            const count = eventTarget();
            const section = eventTarget();
            section.querySelector = function(selector) {
                return selector === '[data-detail-section-count]' ? count : null;
            };
            section.querySelectorAll = function(selector) {
                return selector === '[data-customer-detail-field]'
                    ? fieldNames.map(name => fields.get(name).field)
                    : [];
            };
            return { count, section };
        }
        const environmentSection = sectionFor([
            'dbMode', 'depotArea', 'objectArea', 'storageNetwork'
        ]);
        const verticaSection = sectionFor([
            'verticaAdmin', 'mcYn', 'customResourcePoolYn', 'mcVersion',
            'subclusterYn', 'mcHost', 'backupYn', 'mcAdmin', 'backupNote'
        ]);
        form.fields = fields;
        form.mirrors = mirrors;
        form.verticaSectionCount = verticaSection.count;
        form.querySelector = function(selector) {
            if (selector === 'input[name="customerName"]') return customerName;
            const controlMatch = selector.match(
                /^\.customer-detail-edit-control\[name="([^"]+)"\]$/);
            if (controlMatch) return fields.get(controlMatch[1])?.control || null;
            const mirrorMatch = selector.match(
                /^input\[type="hidden"\]\[data-conditional-field-mirror="([^"]+)"\]$/);
            if (mirrorMatch) {
                return mirrors.find(mirror =>
                    mirror.getAttribute('data-conditional-field-mirror')
                        === mirrorMatch[1]) || null;
            }
            return null;
        };
        form.querySelectorAll = function(selector) {
            return selector === '[data-detail-section]'
                ? [environmentSection.section, verticaSection.section]
                : [];
        };
        form.appendChild = function(element) { mirrors.push(element); };
        return form;
    });
    const detailLink = { href: 'http://localhost/customers?view=detail&env=prod' };
    const root = {
        attributes: new Map(),
        querySelector(selector) {
            return selector === '.tab-nav' ? tabNavigation : null;
        },
        querySelectorAll(selector) {
            if (selector === '[data-customer-detail-form]') return forms;
            if (selector === '.tab-btn') return tabs;
            if (selector === '.tab-panel') return panels;
            if (selector === '[data-customer-environment-link]') return [detailLink];
            return [];
        },
        setAttribute(name, value) { this.attributes.set(name, String(value)); }
    };
    const document = {
        addEventListener(name, listener) { documentListeners.set(name, listener); },
        createElement() { return eventTarget(); },
        querySelector(selector) {
            return selector === '.customer-detail--edit' ? root : null;
        }
    };
    const window = {
        Frog2UI: {
            confirmAction(message) {
                confirmMessages.push(message);
                return true;
            }
        },
        addEventListener(name, listener) { windowListeners.set(name, listener); },
        history: {
            pushState(state, title, location) {
                historyCalls.push({ method: 'push', state, location });
            },
            replaceState(state, title, location) {
                historyCalls.push({ method: 'replace', state, location });
            }
        },
        location: {
            href: 'http://localhost/customers?view=editDetail&env=prod',
            search: '?view=editDetail&env=prod'
        },
        requestAnimationFrame(callback) { callback(); }
    };

    vm.runInNewContext(source, {
        Array,
        Date,
        document,
        Set,
        String,
        URL,
        URLSearchParams,
        WeakMap,
        window
    });
    documentListeners.get('DOMContentLoaded')();

    return {
        confirmMessages,
        detailLink,
        forms,
        historyCalls,
        indicator,
        panels,
        root,
        tabs,
        windowListeners
    };
}

test('switches environment panels without discarding form drafts', () => {
    const harness = createHarness();

    harness.forms[0].dispatch('input');
    harness.tabs[1].dispatch('click');

    assert.equal(harness.forms[0].classList.contains('is-dirty'), true);
    assert.equal(harness.tabs[0].classList.contains('is-dirty'), true);
    assert.equal(harness.panels[0].hidden, true);
    assert.equal(harness.panels[1].hidden, false);
    assert.equal(harness.tabs[1].getAttribute('aria-selected'), 'true');
    assert.equal(harness.root.attributes.get('data-current-environment'), 'stg');
    assert.match(harness.detailLink.href, /env=stg/);
    assert.equal(harness.historyCalls.at(-1).method, 'push');
});

test('warns before leaving when any environment has an unsaved draft', () => {
    const harness = createHarness();
    let prevented = false;

    harness.forms[2].dispatch('change');
    harness.windowListeners.get('beforeunload')({
        preventDefault() { prevented = true; },
        returnValue: null
    });

    assert.equal(prevented, true);
});

test('saving one environment explicitly warns about another dirty environment', () => {
    const harness = createHarness();
    let prevented = false;

    harness.forms[0].dispatch('input');
    harness.forms[1].dispatch('input');
    harness.forms[0].dispatch('submit', {
        preventDefault() { prevented = true; }
    });

    assert.equal(prevented, false);
    assert.equal(harness.confirmMessages.length, 1);
    assert.match(harness.confirmMessages[0], /다른 환경의 저장하지 않은 변경사항/);
});

test('EON and MC usage control only their dependent fields', () => {
    const harness = createHarness();
    const prod = harness.forms[0];
    const staging = harness.forms[1];

    for (const name of [
        'depotArea', 'objectArea', 'storageNetwork'
    ]) {
        assert.equal(prod.fields.get(name).control.disabled, false);
        assert.equal(staging.fields.get(name).control.disabled, true);
        assert.equal(
            staging.fields.get(name).control.classList.contains(
                'customer-detail-edit-control--blocked'),
            true);
        assert.equal(
            staging.fields.get(name).control.getAttribute('aria-disabled'),
            'true');
        assert.equal(
            prod.mirrors.find(candidate =>
                candidate.getAttribute('data-conditional-field-mirror') === name)
                .disabled,
            true);
        assert.equal(
            staging.fields.get(name).field.classList.contains(
                'is-conditionally-disabled'),
            true);
    }
    for (const name of ['mcHost', 'mcVersion', 'mcAdmin']) {
        assert.equal(prod.fields.get(name).control.disabled, false);
        assert.equal(staging.fields.get(name).control.disabled, true);
        assert.equal(
            prod.mirrors.find(candidate =>
                candidate.getAttribute('data-conditional-field-mirror') === name)
                .disabled,
            true);
    }

    staging.fields.get('dbMode').control.value = 'EON';
    staging.fields.get('dbMode').control.dispatch('change');
    staging.fields.get('mcYn').control.value = 'Y';
    staging.fields.get('mcYn').control.dispatch('change');

    for (const name of [
        'depotArea', 'objectArea', 'storageNetwork',
        'mcHost', 'mcVersion', 'mcAdmin'
    ]) {
        assert.equal(staging.fields.get(name).control.disabled, false);
        assert.equal(
            staging.fields.get(name).control.classList.contains(
                'customer-detail-edit-control--blocked'),
            false);
        assert.equal(
            staging.fields.get(name).control.getAttribute('aria-disabled'),
            null);
    }
    assert.equal(staging.fields.get('depotArea').control.value, 'depotArea-value');
    assert.equal(staging.fields.get('objectArea').control.value, 'objectArea-value');
    assert.equal(staging.fields.get('storageNetwork').control.value, 'storageNetwork-value');
    assert.equal(staging.fields.get('mcHost').control.value, 'mcHost-value');
    assert.equal(staging.fields.get('mcVersion').control.value, 'mcVersion-value');
    assert.equal(staging.fields.get('mcAdmin').control.value, 'mcAdmin-value');
});

test('disabled dependent fields submit explicit unused values', () => {
    const staging = createHarness().forms[1];

    const unusedValues = {
        depotArea: '미사용',
        objectArea: '미사용',
        storageNetwork: '미사용',
        mcHost: '미사용',
        mcVersion: '미사용',
        mcAdmin: '미사용'
    };
    for (const [name, unusedValue] of Object.entries(unusedValues)) {
        const control = staging.fields.get(name).control;
        const mirror = staging.mirrors.find(candidate =>
            candidate.getAttribute('data-conditional-field-mirror') === name);
        assert.ok(mirror);
        assert.equal(mirror.disabled, false);
        assert.equal(mirror.name, name);
        assert.equal(control.value, unusedValue);
        assert.equal(mirror.value, unusedValue);
    }
    assert.equal(staging.verticaSectionCount.textContent, '9 / 9');
    assert.equal(
        staging.verticaSectionCount.getAttribute('aria-label'),
        '9개 중 9개 입력');
});
