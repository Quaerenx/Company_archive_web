'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync(
    'src/main/webapp/resources/js/pages/customer_detail_edit.js',
    'utf8'
);

function createHarness(year, monthIndex, day) {
    const documentListeners = new Map();
    const dateInputs = [
        dateInput('createDate'),
        dateInput('installDate'),
        dateInput('eosDate')
    ];
    const form = { addEventListener() {} };

    class FixedLocalDate {
        getDate() { return day; }
        getFullYear() { return year; }
        getMonth() { return monthIndex; }
    }

    const document = {
        addEventListener(name, listener) {
            documentListeners.set(name, listener);
        },
        getElementById(id) {
            return id === 'customerDetailForm' ? form : null;
        },
        querySelectorAll(selector) {
            return selector === 'input[type="date"]' ? dateInputs : [];
        }
    };

    vm.runInNewContext(source, {
        Date: FixedLocalDate,
        document,
        String,
        window: {}
    });
    documentListeners.get('DOMContentLoaded')();

    return dateInputs;
}

function dateInput(name) {
    const attributes = new Map();
    return {
        name,
        getAttribute(attribute) {
            return attributes.get(attribute) || null;
        },
        setAttribute(attribute, value) {
            attributes.set(attribute, String(value));
        }
    };
}

test('uses the browser local calendar date as the maximum customer date', () => {
    const [createDate, installDate, eosDate] = createHarness(2026, 7, 25);

    assert.equal(createDate.getAttribute('max'), '2026-08-25');
    assert.equal(installDate.getAttribute('max'), '2026-08-25');
    assert.equal(eosDate.getAttribute('max'), null);
});

test('pads single-digit month and day values', () => {
    const [createDate] = createHarness(2026, 0, 5);

    assert.equal(createDate.getAttribute('max'), '2026-01-05');
});
