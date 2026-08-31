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
    const form = {
        classList: { add() {}, remove() {} },
        addEventListener() {},
        getAttribute(name) {
            if (name === 'data-environment') return 'prod';
            if (name === 'data-environment-label') return '운영';
            return null;
        },
        querySelector(selector) {
            if (selector === 'input[name="customerName"]') {
                return { value: 'Acme' };
            }
            return null;
        },
        querySelectorAll(selector) {
            if (selector === 'input[type="date"]') return dateInputs;
            return [];
        }
    };
    const root = {
        querySelector() { return null; },
        querySelectorAll(selector) {
            return selector === '[data-customer-detail-form]' ? [form] : [];
        },
        setAttribute() {}
    };

    class FixedLocalDate {
        getDate() { return day; }
        getFullYear() { return year; }
        getMonth() { return monthIndex; }
    }

    const document = {
        addEventListener(name, listener) {
            documentListeners.set(name, listener);
        },
        querySelector(selector) {
            return selector === '.customer-detail--edit' ? root : null;
        },
        querySelectorAll(selector) {
            return selector === 'input[type="date"]' ? dateInputs : [];
        }
    };

    vm.runInNewContext(source, {
        Array,
        Date: FixedLocalDate,
        document,
        Set,
        String,
        URL,
        URLSearchParams,
        window: {
            addEventListener() {},
            location: { href: 'http://localhost/customers?env=prod', search: '?env=prod' }
        }
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
