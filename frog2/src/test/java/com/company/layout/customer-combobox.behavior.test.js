'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync(
    'src/main/webapp/resources/js/ui-customer-combobox.js',
    'utf8'
);

class ClassList {
    constructor() {
        this.values = new Set();
    }
    add(name) { this.values.add(name); }
    toggle(name, force) {
        if (force) this.values.add(name);
        else this.values.delete(name);
    }
}

function element(tagName) {
    const attributes = new Map();
    const listeners = new Map();
    const children = [];
    const node = {
        tagName: tagName.toUpperCase(),
        children,
        classList: new ClassList(),
        dataset: {},
        hidden: false,
        value: '',
        appendChild(child) { children.push(child); return child; },
        addEventListener(name, listener) {
            if (!listeners.has(name)) listeners.set(name, []);
            listeners.get(name).push(listener);
        },
        dispatch(name, overrides = {}) {
            (listeners.get(name) || []).forEach((listener) =>
                listener(Object.assign({ preventDefault() {} }, overrides)));
        },
        setAttribute(name, value) { attributes.set(name, String(value)); },
        getAttribute(name) { return attributes.get(name) || null; },
        removeAttribute(name) { attributes.delete(name); },
        querySelectorAll(selector) {
            if (selector === '[role="option"]') {
                return children.filter((child) => child.getAttribute('role') === 'option');
            }
            return [];
        },
        contains(candidate) { return candidate === node || children.includes(candidate); },
        scrollIntoView() {},
        focus() {},
        setCustomValidity(message) { node.validationMessage = message; }
    };
    Object.defineProperty(node, 'textContent', {
        get() { return node._textContent || ''; },
        set(value) {
            node._textContent = String(value);
            children.length = 0;
        }
    });
    Object.defineProperty(node, 'innerHTML', {
        set(value) { node._innerHTML = String(value); }
    });
    return node;
}

test('customer combobox filters and keeps the submitted native value in sync', () => {
    const select = element('select');
    select.id = 'customer_name';
    select.required = true;
    select.value = 'KT';
    select.options = [
        { value: '', textContent: '고객사를 선택하세요', disabled: false },
        { value: 'KT', textContent: 'KT', disabled: false },
        { value: '삼성전자', textContent: '삼성전자', disabled: false }
    ];
    let nativeChangeCount = 0;
    select.addEventListener('change', () => { nativeChangeCount += 1; });
    let root;
    select.insertAdjacentElement = (position, candidate) => {
        assert.equal(position, 'afterend');
        root = candidate;
    };
    select.form = { addEventListener() {} };
    const label = { htmlFor: 'customer_name' };
    const document = {
        readyState: 'complete',
        createElement: element,
        addEventListener() {},
        querySelectorAll(selector) {
            if (selector === 'select[data-ui-customer-combobox]') return [select];
            if (selector === 'label[for]') return [label];
            return [];
        }
    };
    class Event {
        constructor(type) { this.type = type; }
    }
    select.dispatchEvent = (event) => select.dispatch(event.type);

    vm.runInNewContext(source, { document, Event, window: { setTimeout(callback) { callback(); } } });

    const input = root.children[0];
    assert.equal(input.value, 'KT');
    assert.equal(label.htmlFor, 'customer_name-combobox');
    assert.equal(select.required, false);

    input.value = '삼성전자';
    input.dispatch('input');
    assert.equal(select.value, '삼성전자');
    assert.equal(nativeChangeCount, 1);
    assert.equal(input.validationMessage, '');

    input.value = '없는 고객사';
    input.dispatch('input');
    assert.equal(select.value, '');
    assert.match(input.validationMessage, /목록에서/);

    input.value = '삼';
    input.dispatch('input');
    input.dispatch('keydown', { key: 'ArrowDown' });
    input.dispatch('keydown', { key: 'Enter' });
    assert.equal(select.value, '삼성전자');
    assert.equal(input.value, '삼성전자');
});
