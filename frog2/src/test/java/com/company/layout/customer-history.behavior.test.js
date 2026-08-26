'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync(
    'src/main/webapp/resources/js/pages/customer_history.js',
    'utf8'
);

function createHarness(confirmResult) {
    const documentListeners = new Map();
    const formListeners = new Map();
    let confirmationMessage = '';

    const deleteForm = {
        addEventListener(name, listener) {
            formListeners.set(name, listener);
        }
    };
    const document = {
        addEventListener(name, listener) {
            documentListeners.set(name, listener);
        },
        querySelector(selector) {
            return selector === '[data-customer-history-delete]'
                ? deleteForm
                : null;
        }
    };
    const window = {
        Frog2UI: {
            confirmAction(message) {
                confirmationMessage = message;
                return confirmResult;
            }
        }
    };

    vm.runInNewContext(source, { document, window });
    documentListeners.get('DOMContentLoaded')();

    return {
        confirmationMessage() {
            return confirmationMessage;
        },
        submit() {
            const event = {
                defaultPrevented: false,
                preventDefault() {
                    this.defaultPrevented = true;
                }
            };
            formListeners.get('submit')(event);
            return event;
        }
    };
}

test('history delete keeps the shared confirmation contract', () => {
    const cancelled = createHarness(false);
    assert.equal(cancelled.submit().defaultPrevented, true);
    assert.equal(
        cancelled.confirmationMessage(),
        '이 고객사 히스토리를 삭제하시겠습니까?'
    );

    const accepted = createHarness(true);
    assert.equal(accepted.submit().defaultPrevented, false);
});
