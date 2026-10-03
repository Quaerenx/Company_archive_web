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

function createHarness(options = {}) {
    const storageKey = 'frog2.workInbox.deferrals.v1:user-1';
    const storageValues = new Map([[storageKey, JSON.stringify(options.deferrals ?? {
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
    const deferForm = eventTarget({values: {reason: '', until: ''}});
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
            if (selector === '[data-defer-form]') return deferForm;
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
        Date: options.Date ?? Date,
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
        panel,
        resume,
        until,
        deferForm,
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


function clockAt(instant) {
    let now = Date.parse(instant);
    class ClockDate extends Date {
        constructor(...args) {
            super(...(args.length ? args : [now]));
        }
        static now() {
            return now;
        }
    }
    return {
        Date: ClockDate,
        advanceTo(instant) {
            now = Date.parse(instant);
        }
    };
}

test('resuming a deferred item restores the active state and removes saved data', () => {
    const harness = createHarness();
    harness.resume.dispatch('click');

    assert.equal(harness.item.dataset.deferred, 'false');
    assert.equal(harness.item.hidden, false);
    assert.equal(harness.count.textContent, '1');
    assert.equal(harness.copy.hidden, true);
    assert.equal(harness.copy.textContent, '');
    assert.equal(harness.panel.hidden, false);
    assert.equal(harness.resume.hidden, true);
    assert.equal(Object.hasOwn(JSON.parse(harness.storageValues.get(harness.storageKey)),
        'license:Alpha'), false);
});

test('invalid stored dates do not interrupt inbox initialization or filtering', () => {
    for (const until of [['2999-01-01'], null, {}, 29990101, '2026-02-30']) {
        const harness = createHarness({deferrals: {
            'license:Alpha': {reason: 'Awaiting review', until}
        }});
        assert.equal(harness.item.dataset.deferred, 'false');
        assert.equal(harness.item.hidden, false);
        assert.equal(harness.count.textContent, '1');
        assert.equal(harness.copy.hidden, true);
        assert.equal(Object.hasOwn(JSON.parse(harness.storageValues.get(harness.storageKey)),
            'license:Alpha'), false);
        harness.filterValues.status = 'deferred';
        harness.filter.dispatch('change');
        assert.equal(harness.item.hidden, true);
    }
});

test('deferral expiration and defaults follow the server business day in other timezones', () => {
    const originalTimezone = process.env.TZ;
    process.env.TZ = 'America/Los_Angeles';
    try {
        const clock = clockAt('2026-10-02T15:30:00Z');
        const harness = createHarness({Date: clock.Date, deferrals: {
            'license:Alpha': {reason: 'Awaiting review', until: '2026-10-02'}
        }});
        assert.equal(harness.item.dataset.deferred, 'false');
        assert.equal(harness.item.hidden, false);
        assert.equal(harness.until.value, '2026-10-10');

        harness.deferForm.values = {reason: 'Awaiting review', until: '2026-10-02'};
        harness.deferForm.dispatch('submit', {preventDefault() {}});
        assert.equal(harness.item.dataset.deferred, 'false');

        harness.deferForm.values.until = '2026-10-03';
        harness.deferForm.dispatch('submit', {preventDefault() {}});
        assert.equal(harness.item.dataset.deferred, 'true');
    } finally {
        if (originalTimezone === undefined) delete process.env.TZ;
        else process.env.TZ = originalTimezone;
    }
});

test('filtering after business midnight also refreshes deferral controls and copy', () => {
    const clock = clockAt('2026-10-03T03:00:00Z');
    const harness = createHarness({Date: clock.Date, deferrals: {
        'license:Alpha': {reason: 'Awaiting review', until: '2026-10-03'}
    }});
    assert.equal(harness.item.dataset.deferred, 'true');

    clock.advanceTo('2026-10-04T03:00:00Z');
    harness.filter.dispatch('change');

    assert.equal(harness.item.dataset.deferred, 'false');
    assert.equal(harness.item.hidden, false);
    assert.equal(harness.count.textContent, '1');
    assert.equal(harness.copy.hidden, true);
    assert.equal(harness.copy.textContent, '');
    assert.equal(harness.resume.hidden, true);
    assert.equal(harness.panel.hidden, false);
});

test('seven business days cross leap days and year boundaries correctly', () => {
    for (const [instant, expected] of [
        ['2028-02-24T15:30:00Z', '2028-03-03'],
        ['2026-12-28T15:30:00Z', '2027-01-05']
    ]) {
        const harness = createHarness({Date: clockAt(instant).Date, deferrals: {}});
        assert.equal(harness.until.value, expected);
    }
});
