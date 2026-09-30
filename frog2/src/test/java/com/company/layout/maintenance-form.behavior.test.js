'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const {createLatestRequestGuard, formatLicensePercentageHalfUp} = require(
    '../../../../../main/webapp/resources/js/pages/maintenance_form.js');
const {focusButton, formatValue, parseDate} = require(
    '../../../../../main/webapp/resources/js/pages/maintenance_calendar.js');

test('license percentage uses the same positive HALF_UP boundary as Java', () => {
    assert.equal(formatLicensePercentageHalfUp(0), '0.0');
    assert.equal(formatLicensePercentageHalfUp(55.6), '55.6');
    assert.equal(formatLicensePercentageHalfUp(65.575), '65.58');
    assert.equal(formatLicensePercentageHalfUp(89.949), '89.95');
    assert.equal(formatLicensePercentageHalfUp(89.95), '89.95');
    assert.equal(formatLicensePercentageHalfUp(105.049), '105.05');
    assert.equal(formatLicensePercentageHalfUp(105.05), '105.05');
});

test('starting a blank form-context request aborts the previous request', () => {
    const guard = createLatestRequestGuard();
    const previous = guard.begin('["Acme","2026-08-25"]');

    const blank = guard.begin('');

    assert.equal(previous.controller.signal.aborted, true);
    assert.equal(blank.controller, null);
    assert.equal(
        guard.isCurrent(previous, '["Acme","2026-08-25"]'),
        false
    );
});

test('only the latest matching form-context response remains current', () => {
    const guard = createLatestRequestGuard();
    const previous = guard.begin('["Acme","2026-08-25"]');
    const latest = guard.begin('["Beta","2026-08-26"]');

    assert.equal(previous.controller.signal.aborted, true);
    assert.equal(
        guard.isCurrent(previous, '["Acme","2026-08-25"]'),
        false
    );
    assert.equal(
        guard.isCurrent(latest, '["Beta","2026-08-25"]'),
        false
    );
    assert.equal(
        guard.isCurrent(latest, '["Beta","2026-08-26"]'),
        true
    );

    guard.complete(latest);
    assert.equal(
        guard.isCurrent(latest, '["Beta","2026-08-26"]'),
        false
    );
});

test('calendar focus is restored to the selected date after rerender', () => {
    let focused = false;
    const selectedDateButton = {
        focus() {
            focused = true;
        }
    };
    const calendarGrid = {
        querySelector(selector) {
            assert.equal(
                selector,
                '[data-calendar-date="2026-08-25"]'
            );
            return selectedDateButton;
        }
    };

    assert.equal(
        focusButton(calendarGrid, '2026-08-25'),
        true
    );
    assert.equal(focused, true);
});

test('calendar focus restoration fails safely without a rendered date', () => {
    const calendarGrid = {
        querySelector() {
            return null;
        }
    };

    assert.equal(
        focusButton(calendarGrid, '2026-08-25'),
        false
    );
    assert.equal(focusButton(null, '2026-08-25'), false);
});

test('calendar module validates and formats local calendar dates', () => {
    assert.equal(formatValue(parseDate('2026-08-25')), '2026-08-25');
    assert.equal(parseDate('2026-02-30'), null);
    assert.equal(parseDate('2026-8-25'), null);
});

function createFormHarness(mode, modifiedInspector = false) {
    const fs = require('node:fs');
    const vm = require('node:vm');
    class Field {
        constructor(value, tagName = 'INPUT') {
            this.value = value;
            this.tagName = tagName;
            this.dataset = {};
            this.listeners = {};
        }
        addEventListener(name, listener) { this.listeners[name] = listener; }
    }
    const customer = new Field('Synthetic customer');
    const inspector = new Field('Historical inspector', 'SELECT');
    if (modifiedInspector) inspector.dataset.userModified = 'true';
    const date = new Field('2026-08-01');
    const fields = { customer_name: customer, inspector_name: inspector, inspection_date: date };
    const form = { addEventListener() {} };
    let calendarOptions;
    const root = {
        contains() { return true; },
        getAttribute(name) { return name === 'data-maintenance-form-mode' ? mode : '/frog2'; }
    };
    vm.runInNewContext(fs.readFileSync('src/main/webapp/resources/js/pages/maintenance_form.js', 'utf8'), {
        document: {
            querySelector(selector) { return selector === '[data-maintenance-form-mode][data-context-path]' ? root : null; },
            getElementById(id) { return id === 'maintenanceForm' ? form : fields[id] || null; }
        },
        window: {
            addEventListener() {},
            Frog2MaintenanceCalendar: { create(options) { calendarOptions = options; return { initialize() {} }; } },
            Frog2Session: { requireActiveSession() {}, isSessionExpired() { return false; } }
        },
        HTMLElement: Field, URLSearchParams, AbortController,
        FormData: class { [Symbol.iterator]() { return Object.entries(fields).map(([name, field]) => [name, field.value])[Symbol.iterator](); } },
        fetch() { return Promise.resolve({ ok: true, json: () => Promise.resolve({ defaultInspector: 'Current manager', previous: null, duplicate: null }) }); }
    });
    return { inspector, async changeDate() { date.value = '2026-08-02'; calendarOptions.onChange(); await new Promise((resolve) => setImmediate(resolve)); } };
}

test('changing an edit inspection date preserves the stored inspector', async () => {
    const harness = createFormHarness('edit');
    await harness.changeDate();
    assert.equal(harness.inspector.value, 'Historical inspector');
});

test('new maintenance records still receive untouched customer inspector defaults', async () => {
    const harness = createFormHarness('add');
    await harness.changeDate();
    assert.equal(harness.inspector.value, 'Current manager');
});

test('new maintenance records preserve an inspector explicitly chosen by the user', async () => {
    const harness = createFormHarness('add', true);
    await harness.changeDate();
    assert.equal(harness.inspector.value, 'Historical inspector');
});
