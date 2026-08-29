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
