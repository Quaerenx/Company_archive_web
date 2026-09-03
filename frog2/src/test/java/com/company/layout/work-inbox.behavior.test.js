'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync(
    'src/main/webapp/resources/js/pages/work_inbox.js',
    'utf8'
);

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

function createHarness() {
    const storageKey = 'frog2.workInbox.deferrals.v1:user-1';
    const storageValues = new Map([[storageKey, JSON.stringify({
        'license:Alpha': {
            reason: '담당자 확인 중',
            until: '2999-01-01'
        },
        obsolete: {
            reason: '이미 해결됨',
            until: '2999-01-01'
        }
    })]]);
    const localStorage = {
        getItem(key) {
            return storageValues.has(key) ? storageValues.get(key) : null;
        },
        setItem(key, value) {
            storageValues.set(key, String(value));
        }
    };
    const filterValues = {
        severity: 'all',
        type: 'all',
        status: 'active',
        customer: ''
    };
    const filter = eventTarget({values: filterValues});
    const count = {textContent: ''};
    const empty = {hidden: true};
    const copy = {hidden: true, textContent: ''};
    const resume = eventTarget({hidden: true});
    const until = {value: ''};
    const panel = {
        hidden: false,
        querySelector(selector) {
            return selector === '[name="until"]' ? until : null;
        }
    };
    const item = {
        dataset: {severity: 'danger', type: 'license', customer: 'Alpha'},
        hidden: false,
        getAttribute(name) {
            return name === 'data-item-key' ? 'license:Alpha' : null;
        },
        querySelector(selector) {
            if (selector === '[data-deferred-copy]') return copy;
            if (selector === '[data-defer-panel]') return panel;
            if (selector === '[data-resume]') return resume;
            return null;
        }
    };
    const root = {
        getAttribute(name) {
            return name === 'data-user-id' ? 'user-1' : null;
        },
        querySelector(selector) {
            if (selector === '[data-work-inbox-filter]') return filter;
            if (selector === '[data-work-inbox-visible-count]') return count;
            if (selector === '[data-filter-empty]') return empty;
            return null;
        },
        querySelectorAll(selector) {
            return selector === '[data-work-inbox-item]' ? [item] : [];
        }
    };
    const document = {
        querySelector(selector) {
            return selector === '[data-work-inbox]' ? root : null;
        }
    };
    class FormDataFixture {
        constructor(form) {
            this.values = form.values;
        }
        get(name) {
            return this.values[name];
        }
    }
    vm.runInNewContext(source, {
        document,
        window: {
            localStorage,
            setTimeout(callback) {
                callback();
            }
        },
        Date,
        FormData: FormDataFixture,
        Set
    });
    return {
        copy,
        count,
        empty,
        filter,
        filterValues,
        item,
        resume,
        storageKey,
        storageValues
    };
}

test('inbox filters deferred items and prunes resolved source keys', () => {
    const harness = createHarness();

    assert.equal(harness.item.dataset.deferred, 'true');
    assert.equal(harness.item.hidden, true);
    assert.equal(harness.count.textContent, '0');
    assert.equal(harness.copy.hidden, false);
    assert.match(harness.copy.textContent, /담당자 확인 중/);
    assert.equal(harness.resume.hidden, false);

    const persisted = JSON.parse(
        harness.storageValues.get(harness.storageKey));
    assert.equal(Object.hasOwn(persisted, 'obsolete'), false);

    harness.filterValues.status = 'deferred';
    harness.filter.dispatch('change');
    assert.equal(harness.item.hidden, false);
    assert.equal(harness.count.textContent, '1');
    assert.equal(harness.empty.hidden, true);
});
