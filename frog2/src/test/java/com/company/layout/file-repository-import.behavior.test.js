'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');
const vm = require('node:vm');
const source = fs.readFileSync('src/main/webapp/resources/js/pages/file_repository_import.js', 'utf8');
const flush = () => new Promise((resolve) => setImmediate(resolve));

function node(properties = {}) {
    return Object.assign({
        listeners: {}, dataset: {}, disabled: false, checked: true, hidden: false,
        addEventListener(type, listener) { this.listeners[type] = listener; },
        appendChild() {}, removeAttribute() {}, scrollIntoView() {},
    }, properties);
}

function importHarness(count) {
    const inputs = Array.from({ length: count }, (_, i) => node({ name: 'selectedPath', value: 'synthetic-' + i }));
    const fields = Object.fromEntries([
        'import-select-all', 'import-selected-count', 'import-submit-button', 'import-progress',
        'import-status', 'import-result', 'import-result-summary', 'import-result-body', 'import-retry-button'
    ].map((id) => [id, node()]));
    const counters = { queries: 0, returnedInputs: 0 };
    const form = node({
        action: '/synthetic/import',
        querySelector() { return { value: 'synthetic-token' }; },
        querySelectorAll() {
            counters.queries += 1;
            const available = inputs.filter((input) => !input.disabled);
            counters.returnedInputs += available.length;
            return available;
        }
    });
    fields['file-import-form'] = form;
    form.requestSubmit = () => form.listeners.submit({ preventDefault() {} });
    const requests = [];
    vm.runInNewContext(source, {
        document: { getElementById: (id) => fields[id] || null, createElement: () => node() },
        window: {
            Frog2UI: { setStatus(element, message, tone) { element.message = message; element.tone = tone; }, setButtonLoading(button, loading) { button.disabled = loading; } },
            Frog2Session: { requireActiveSession() {}, isSessionExpired() { return false; } }
        },
        FormData: class { constructor() { this.selectedPaths = inputs.filter((input) => input.checked && !input.disabled).map((input) => input.value); } },
        fetch(url, options) { return new Promise((resolve, reject) => requests.push({ resolve, reject, options })); }
    });
    return { form, inputs, fields, requests, counters };
}

function result(files) {
    return { status: 'ok', files: files.map((file) => Object.assign({ name: 'Synthetic', label: 'Result', reason: '', retryable: false }, file)),
        summary: { imported: files.filter((file) => file.status === 'imported').length,
            conflicts: files.filter((file) => file.status === 'conflict').length,
            rejected: files.filter((file) => file.status === 'rejected').length,
            deferred: files.filter((file) => file.status === 'deferred').length,
            failed: files.filter((file) => file.status === 'failed').length } };
}

function resolve(request, payload) {
    request.resolve({ ok: true, json: () => Promise.resolve(payload) });
    return flush();
}

test('selection and retry changes cannot submit a second in-flight import', async () => {
    const harness = importHarness(2);
    harness.form.requestSubmit();
    assert.equal(harness.fields['import-submit-button'].disabled, true);
    harness.inputs[0].checked = false;
    harness.form.listeners.change({ target: harness.inputs[0] });
    assert.equal(harness.fields['import-submit-button'].disabled, true);
    assert.equal(harness.fields['import-retry-button'].disabled, true);
    harness.form.requestSubmit();
    harness.fields['import-retry-button'].listeners.click();
    assert.equal(harness.requests.length, 1);
    assert.deepEqual(harness.requests[0].options.body.selectedPaths, ['synthetic-0', 'synthetic-1']);
    await resolve(harness.requests[0], result([{ path: 'synthetic-0', status: 'deferred' }, { path: 'synthetic-1', status: 'deferred' }]));
    assert.equal(harness.fields['import-progress'].hidden, true);
    assert.equal(harness.fields['import-submit-button'].disabled, false);
    assert.equal(harness.fields['import-retry-button'].disabled, false);
});

test('partial import preserves failed and deferred inputs and retries only failures', async () => {
    const harness = importHarness(5);
    harness.form.requestSubmit();
    await resolve(harness.requests[0], result([
        { path: 'synthetic-0', status: 'imported' }, { path: 'synthetic-1', status: 'conflict' },
        { path: 'synthetic-2', status: 'rejected' }, { path: 'synthetic-3', status: 'failed', retryable: true },
        { path: 'synthetic-4', status: 'deferred' }
    ]));
    harness.inputs.slice(0, 3).forEach((input) => { assert.equal(input.disabled, true); assert.equal(input.checked, false); });
    harness.inputs.slice(3).forEach((input) => { assert.equal(input.disabled, false); assert.equal(input.checked, true); });
    assert.equal(harness.fields['import-retry-button'].hidden, false);
    harness.fields['import-retry-button'].listeners.click();
    assert.equal(harness.requests.length, 2);
    assert.deepEqual(harness.requests[1].options.body.selectedPaths, ['synthetic-3']);
    await resolve(harness.requests[1], result([{ path: 'synthetic-3', status: 'imported' }]));
    assert.equal(harness.inputs[3].disabled, true);
    assert.equal(harness.inputs[4].disabled, false);
    assert.equal(harness.inputs[4].checked, false);
    assert.equal(harness.fields['import-retry-button'].hidden, true);
    assert.equal(harness.fields['import-submit-button'].disabled, true);
});

test('request failure unlocks the form without disabling selected inputs', async () => {
    const harness = importHarness(2);
    harness.form.requestSubmit();
    harness.requests[0].reject(new Error('Synthetic failure'));
    await flush();
    assert.equal(harness.fields['import-submit-button'].disabled, false);
    assert.equal(harness.fields['import-progress'].hidden, true);
    assert.equal(harness.fields['import-status'].tone, 'danger');
    harness.inputs.forEach((input) => { assert.equal(input.disabled, false); assert.equal(input.checked, true); });
    harness.form.requestSubmit();
    assert.equal(harness.requests.length, 2);
    await resolve(harness.requests[1], result([]));
});

test('large import results visit inputs linearly without a selector query for every file', async () => {
    const count = 1000;
    const harness = importHarness(count);
    harness.form.requestSubmit();
    harness.counters.queries = 0;
    harness.counters.returnedInputs = 0;
    await resolve(harness.requests[0], result(harness.inputs.map((input) => ({ path: input.value, status: 'imported' }))));
    assert.ok(harness.counters.queries <= 6, 'result rendering should use a bounded number of selector queries');
    assert.ok(harness.counters.returnedInputs <= count * 2, 'result input visits should scale linearly');
    assert.equal(harness.inputs.filter((input) => input.disabled).length, count);
});
