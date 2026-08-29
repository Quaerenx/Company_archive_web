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

function createHarness() {
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
    const commentForm = {
        addEventListener() {},
        querySelector() { return null; }
    };
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
        window: {
            Frog2UI: {},
            Frog2Csrf: {},
            Frog2Session: {}
        }
    });

    return { cancelButton, editButton, editContent };
}

test('cancelling a comment edit restores the persisted content', () => {
    const harness = createHarness();

    harness.editButton.listeners.click();
    harness.editContent.value = '저장하지 않을 임시 변경';
    harness.cancelButton.listeners.click();

    assert.equal(harness.editContent.value, '저장된 원래 댓글');
});
