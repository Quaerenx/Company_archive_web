'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync(
    'src/main/webapp/resources/js/pages/meeting_view.js',
    'utf8'
);

function createButton() {
    const listeners = {};
    return {
        listeners,
        addEventListener(name, listener) { listeners[name] = listener; },
        setAttribute() {},
        focus() {}
    };
}

function createHarness(search = '') {
    const editButton = createButton();
    const deleteButton = createButton();
    const saveButton = createButton();
    const cancelButton = createButton();
    const content = { hidden: false, textContent: '저장된 원래 댓글' };
    const editContent = {
        value: '저장된 원래 댓글',
        focus() {}
    };
    const editForm = {
        hidden: true,
        classList: {
            add() {},
            remove() {}
        }
    };
    const commentButton = createButton();
    const commentForm = createButton();
    commentForm.querySelector = () => commentButton;
    const requests = [];
    const assignedLocations = [];
    const item = {
        getAttribute() { return '17'; },
        querySelector(selector) {
            return {
                '.comment-btn.edit': editButton,
                '.comment-btn.delete': deleteButton,
                '.btn-save': saveButton,
                '.btn-cancel-edit': cancelButton
            }[selector] || null;
        }
    };
    const root = {
        getAttribute(name) {
            return name === 'data-context-path' ? '/frog2' : '31';
        },
        querySelector() { return null; }
    };
    const elements = {
        commentForm,
        commentContent: { value: 'Synthetic comment' },
        'content-17': content,
        'edit-form-17': editForm,
        'edit-content-17': editContent
    };
    const document = {
        querySelector(selector) {
            return selector === '.meeting-view[data-context-path][data-meeting-id]'
                ? root
                : null;
        },
        querySelectorAll(selector) {
            return selector === '.comment-item[data-comment-id]' ? [item] : [];
        },
        getElementById(id) { return elements[id] || null; }
    };

    vm.runInNewContext(source, {
        document,
        URLSearchParams,
        fetch(url, options) { return new Promise((resolve) => requests.push({ resolve, options })); },
        window: {
            location: { search, assign(url) { assignedLocations.push(url); } },
            Frog2UI: { setButtonLoading(button, loading) { button.disabled = loading; }, notify() {} },
            Frog2Csrf: { token() { return 'synthetic-token'; } },
            Frog2Session: { requireActiveSession() {}, isSessionExpired() { return false; } }
        }
    });

    return { cancelButton, editButton, editContent, commentForm, commentButton, requests, assignedLocations };
}

test('cancelling a comment edit restores the persisted content', () => {
    const harness = createHarness();

    harness.editButton.listeners.click();
    harness.editContent.value = '저장하지 않을 임시 변경';
    harness.cancelButton.listeners.click();

    assert.equal(harness.editContent.value, '저장된 원래 댓글');
});

test('adding a comment preserves list filters while returning to the newest comments', async () => {
    const expected = { returnPage: '3', returnQ: 'synthetic & query', returnType: 'project', returnAuthor: '7', returnStartDate: '2026-08-01', returnEndDate: '2026-08-31' };
    const parameters = new URLSearchParams(expected);
    parameters.set('commentBefore', '29');
    const harness = createHarness('?' + parameters.toString());
    harness.commentForm.listeners.submit.call(harness.commentForm, { preventDefault() {} });
    harness.requests[0].resolve({ ok: true, json: () => Promise.resolve({ success: true }) });
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(harness.assignedLocations.length, 1);
    const destination = new URL(harness.assignedLocations[0], 'https://example.invalid');
    assert.equal(destination.pathname, '/frog2/meeting');
    assert.equal(destination.searchParams.get('view'), 'view');
    assert.equal(destination.searchParams.get('id'), '31');
    Object.entries(expected).forEach(([key, value]) => assert.equal(destination.searchParams.get(key), value));
    assert.equal(destination.searchParams.has('commentBefore'), false);
    assert.equal(destination.hash, '#comments');
});

test('a rejected comment stays on the current page and unlocks submission', async () => {
    const harness = createHarness('?returnPage=3');
    harness.commentForm.listeners.submit.call(harness.commentForm, { preventDefault() {} });
    harness.requests[0].resolve({ ok: false, json: () => Promise.resolve({ message: 'Synthetic error' }) });
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(harness.assignedLocations.length, 0);
    assert.equal(harness.commentButton.disabled, false);
});
