'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync(
    'src/main/webapp/resources/js/ui-system.js',
    'utf8'
);

class Element {}

class ClassList {
    constructor() {
        this.names = new Set();
    }

    add(...names) {
        names.forEach((name) => this.names.add(name));
    }

    contains(name) {
        return this.names.has(name);
    }

    remove(...names) {
        names.forEach((name) => this.names.delete(name));
    }

    toggle(name, force) {
        if (force) {
            this.names.add(name);
        } else {
            this.names.delete(name);
        }
    }
}

class StyleDeclaration {
    constructor() {
        this.properties = new Map();
    }

    getPropertyValue(name) {
        return this.properties.get(name) || '';
    }

    removeProperty(name) {
        this.properties.delete(name);
    }

    setProperty(name, value) {
        this.properties.set(name, String(value));
    }
}

class FakeAnimation {
    constructor(keyframes, options) {
        this.cancelled = false;
        this.keyframes = keyframes;
        this.oncancel = null;
        this.onfinish = null;
        this.options = options;
    }

    cancel() {
        this.cancelled = true;
        if (typeof this.oncancel === 'function') {
            this.oncancel();
        }
    }

    finish() {
        if (typeof this.onfinish === 'function') {
            this.onfinish();
        }
    }
}

class FakeElement extends Element {
    constructor(document, options = {}) {
        super();
        this.attributes = new Map();
        this.classList = new ClassList();
        this.dataset = options.dataset || {};
        this.document = document;
        this.focusables = options.focusables || [];
        this.initialFocus = options.initialFocus || null;
        this.isConnected = options.isConnected !== false;
        this.parentRow = options.parentRow || null;
        this.parentDisclosureRow = options.parentDisclosureRow || null;
        this.disclosureContent = options.disclosureContent || null;
        this.disclosureToggle = options.disclosureToggle || null;
        this.interactive = options.interactive === true;
        this.workSurface = options.workSurface || null;
        this.scrollHeight = options.scrollHeight || 0;
        this.scrollWidth = options.scrollWidth || 0;
        this.clientWidth = options.clientWidth || 0;
        this.bounds = options.bounds || { bottom: 0, height: 0 };
        this.hidden = options.hidden === true;
        this.style = new StyleDeclaration();
        this.animations = [];
        this.listeners = new Map();
    }

    animate(keyframes, options) {
        const animation = new FakeAnimation(keyframes, options);
        this.animations.push(animation);
        return animation;
    }

    addEventListener(name, listener) {
        if (!this.listeners.has(name)) {
            this.listeners.set(name, []);
        }
        this.listeners.get(name).push(listener);
    }

    dispatch(name, event) {
        (this.listeners.get(name) || []).slice().forEach((listener) => listener(event));
    }

    closest(selector) {
        if (selector === '.ui-data-row[data-detail-url]') {
            return this.parentRow || (this.dataset.detailUrl ? this : null);
        }
        if (selector === '[data-ui-disclosure-toggle]') {
            return this.disclosureToggle
                || (Object.hasOwn(this.dataset, 'uiDisclosureToggle') ? this : null);
        }
        if (selector === '[data-ui-disclosure-row]') {
            return this.parentDisclosureRow
                || (Object.hasOwn(this.dataset, 'uiDisclosureRow') ? this : null);
        }
        if (selector === '.ui-work-surface, [data-ui-table-surface]') {
            return this.workSurface;
        }
        return this.interactive ? this : null;
    }

    contains(candidate) {
        return candidate === this || this.focusables.includes(candidate);
    }

    focus() {
        this.document.activeElement = this;
    }

    getAttribute(name) {
        return this.attributes.has(name) ? this.attributes.get(name) : null;
    }

    getClientRects() {
        return [{}];
    }

    getBoundingClientRect() {
        return this.bounds;
    }

    querySelector(selector) {
        if (selector === '[data-dialog-initial-focus]') {
            return this.initialFocus;
        }
        if (selector === '[data-ui-disclosure-toggle]') {
            return this.disclosureToggle;
        }
        if (selector === '[data-ui-disclosure-content]') {
            return this.disclosureContent;
        }
        return null;
    }

    querySelectorAll() {
        return this.focusables;
    }

    removeAttribute(name) {
        this.attributes.delete(name);
    }

    removeEventListener(name, listener) {
        const registered = this.listeners.get(name) || [];
        this.listeners.set(name, registered.filter((item) => item !== listener));
    }

    setAttribute(name, value) {
        this.attributes.set(name, String(value));
    }
}

function createHarness(options = {}) {
    const documentListeners = new Map();
    const windowListeners = new Map();
    const frames = [];
    const assignedLocations = [];
    const resizeObserverCallbacks = [];
    const elementsById = new Map();
    let selectionText = '';
    let selectionOwner = null;

    const document = {
        activeElement: null,
        readyState: 'complete',
        addEventListener(name, listener) {
            if (!documentListeners.has(name)) {
                documentListeners.set(name, []);
            }
            documentListeners.get(name).push(listener);
        },
        body: new FakeElement(null),
        dispatch(name, event) {
            (documentListeners.get(name) || []).forEach((listener) => listener(event));
        },
        documentElement: null,
        getElementById(id) {
            return elementsById.get(id) || null;
        },
        querySelector() {
            return null;
        },
        querySelectorAll() {
            return [];
        },
        removeEventListener(name, listener) {
            const registered = documentListeners.get(name) || [];
            documentListeners.set(name, registered.filter((item) => item !== listener));
        }
    };
    document.body.document = document;
    document.documentElement = new FakeElement(document);

    const header = options.headerBottom === undefined
        ? null
        : new FakeElement(document, { bounds: { bottom: options.headerBottom } });
    const workSurfaces = (options.scrollRegions || []).map(
        () => new FakeElement(document)
    );
    const scrollRegions = (options.scrollRegions || []).map((region, index) =>
        new FakeElement(document, {
            clientWidth: region.clientWidth,
            dataset: {
                uiScrollHintId: region.hintId || '',
                uiScrollLabel: region.label || ''
            },
            scrollWidth: region.scrollWidth,
            workSurface: workSurfaces[index]
        })
    );
    const scrollHints = (options.scrollRegions || []).map((region) => {
        if (!region.hintId) {
            return null;
        }
        const hint = new FakeElement(document, { hidden: true });
        elementsById.set(region.hintId, hint);
        return hint;
    });
    (options.scrollRegions || []).forEach((region, index) => {
        if (region.describedBy) {
            scrollRegions[index].setAttribute('aria-describedby', region.describedBy);
        }
    });
    let dirtyGuardField = null;
    let dirtyGuardForm = null;
    if (options.dirtyGuard) {
        dirtyGuardField = new FakeElement(document);
        dirtyGuardField.disabled = false;
        dirtyGuardField.name = 'customer_name';
        dirtyGuardField.type = 'text';
        dirtyGuardField.value = '기존 고객사';
        dirtyGuardForm = new FakeElement(document, {
            dataset: { uiDirtyGuard: 'auto' },
            focusables: [dirtyGuardField]
        });
        dirtyGuardForm.elements = [dirtyGuardField];
        dirtyGuardForm.checkValidity = function () {
            return true;
        };
        dirtyGuardForm.closest = function (selector) {
            return selector === 'form[data-ui-dirty-guard="auto"]'
                ? dirtyGuardForm
                : null;
        };
    }

    document.querySelector = function (selector) {
        return selector === '.main-header' ? header : null;
    };
    document.querySelectorAll = function (selector) {
        if (selector === '[data-ui-scroll-region]') {
            return scrollRegions;
        }
        if (selector === '.is-table-sticky-ready') {
            return workSurfaces.filter((surface) =>
                surface.classList.contains('is-table-sticky-ready')
            );
        }
        if (selector === 'form[data-ui-dirty-guard="auto"]') {
            return dirtyGuardForm ? [dirtyGuardForm] : [];
        }
        return [];
    };

    const window = {
        addEventListener(name, listener) {
            if (!windowListeners.has(name)) {
                windowListeners.set(name, []);
            }
            windowListeners.get(name).push(listener);
        },
        dispatch(name, event) {
            (windowListeners.get(name) || []).forEach((listener) => listener(event));
        },
        confirm() {
            return true;
        },
        getComputedStyle() {
            return { visibility: 'visible' };
        },
        getSelection() {
            return {
                anchorNode: selectionOwner,
                containsNode(node) {
                    return node === selectionOwner;
                },
                focusNode: selectionOwner,
                isCollapsed: selectionText.length === 0,
                toString() {
                    return selectionText;
                }
            };
        },
        matchMedia(query) {
            return {
                matches: options.reducedMotion === true
                    && query === '(prefers-reduced-motion: reduce)'
            };
        },
        location: {
            assign(url) {
                assignedLocations.push(url);
            }
        },
        requestAnimationFrame(callback) {
            frames.push(callback);
            return frames.length;
        },
        clearTimeout() {},
        setTimeout() {}
    };

    if (options.resizeObserver) {
        window.ResizeObserver = class {
            constructor(callback) {
                resizeObserverCallbacks.push(callback);
            }

            observe() {}
        };
    }

    vm.runInNewContext(source, {
        document,
        Element,
        WeakMap,
        WeakSet,
        window
    });

    return {
        assignedLocations,
        document,
        dirtyGuardField,
        dirtyGuardForm,
        header,
        elementsById,
        scrollRegions,
        scrollHints,
        workSurfaces,
        flushFrames() {
            while (frames.length) {
                frames.shift()();
            }
        },
        setSelection(text, owner = null) {
            selectionText = text;
            selectionOwner = owner;
        },
        triggerHeaderResize() {
            resizeObserverCallbacks.forEach((callback) => callback());
        },
        window,
        ui: window.Frog2UI
    };
}

function keyEvent(key, shiftKey = false) {
    return {
        key,
        shiftKey,
        preventDefault() {
            this.defaultPrevented = true;
        },
        stopPropagation() {
            this.propagationStopped = true;
        }
    };
}

function clickEvent(target) {
    return {
        altKey: false,
        button: 0,
        ctrlKey: false,
        defaultPrevented: false,
        metaKey: false,
        shiftKey: false,
        target
    };
}

function createAnimatedDisclosureHarness(options = {}) {
    const harness = createHarness(options);
    const content = new FakeElement(harness.document, { scrollHeight: 144 });
    const detail = new FakeElement(harness.document, {
        disclosureContent: content,
        hidden: true
    });
    const row = new FakeElement(harness.document, {
        dataset: { uiDisclosureRow: '' }
    });
    const toggle = new FakeElement(harness.document, {
        dataset: { uiDisclosureToggle: '' },
        parentDisclosureRow: row
    });
    row.disclosureToggle = toggle;
    toggle.disclosureToggle = toggle;
    toggle.setAttribute('aria-controls', 'animated-history-detail');
    toggle.setAttribute('aria-expanded', 'false');
    harness.elementsById.set('animated-history-detail', detail);
    return { content, detail, harness, row, toggle };
}

test('dialog traps Tab, closes on Escape, and restores opener focus', () => {
    const harness = createHarness();
    const opener = new FakeElement(harness.document);
    const first = new FakeElement(harness.document);
    const last = new FakeElement(harness.document);
    const dialog = new FakeElement(harness.document, {
        focusables: [first, last],
        initialFocus: first
    });
    const controller = harness.ui.createDialogController(dialog);

    assert.equal(harness.ui.hasOpenDialog(), false);
    controller.open(opener);
    harness.flushFrames();
    assert.equal(controller.isOpen(), true);
    assert.equal(harness.ui.hasOpenDialog(), true);
    assert.equal(dialog.getAttribute('aria-hidden'), 'false');
    assert.equal(dialog.getAttribute('inert'), null);
    assert.equal(harness.document.activeElement, first);

    last.focus();
    const forwardTab = keyEvent('Tab');
    harness.document.dispatch('keydown', forwardTab);
    assert.equal(forwardTab.defaultPrevented, true);
    assert.equal(harness.document.activeElement, first);

    first.focus();
    const backwardTab = keyEvent('Tab', true);
    harness.document.dispatch('keydown', backwardTab);
    assert.equal(backwardTab.defaultPrevented, true);
    assert.equal(harness.document.activeElement, last);

    const escape = keyEvent('Escape');
    harness.document.dispatch('keydown', escape);
    harness.flushFrames();
    assert.equal(controller.isOpen(), false);
    assert.equal(harness.ui.hasOpenDialog(), false);
    assert.equal(dialog.getAttribute('aria-hidden'), 'true');
    assert.equal(dialog.getAttribute('inert'), '');
    assert.equal(harness.document.activeElement, opener);
});

test('shared dirty guard warns only after a form changes and pauses for submit', () => {
    const harness = createHarness();
    const field = new FakeElement(harness.document);
    field.name = 'customer_name';
    field.type = 'text';
    field.value = '기존 고객사';
    field.disabled = false;
    const form = new FakeElement(harness.document, { focusables: [field] });
    const controller = harness.ui.createDirtyGuard(form);

    assert.equal(controller.isDirty(), false);

    field.value = '변경 고객사';
    assert.equal(controller.isDirty(), true);
    const dirtyUnload = {
        preventDefault() {
            this.defaultPrevented = true;
        }
    };
    harness.window.dispatch('beforeunload', dirtyUnload);
    assert.equal(dirtyUnload.defaultPrevented, true);
    assert.equal(dirtyUnload.returnValue, '');

    controller.markSubmitting();
    assert.equal(controller.isDirty(), false);
    const submittingUnload = {
        preventDefault() {
            this.defaultPrevented = true;
        }
    };
    harness.window.dispatch('beforeunload', submittingUnload);
    assert.equal(submittingUnload.defaultPrevented, undefined);

    controller.resume();
    assert.equal(controller.isDirty(), true);
    controller.resetBaseline();
    assert.equal(controller.isDirty(), false);
});

test('automatic dirty guard pauses for a valid submit and resumes on pageshow', () => {
    const harness = createHarness({ dirtyGuard: true });
    harness.flushFrames();
    harness.dirtyGuardField.value = '변경 고객사';

    const dirtyUnload = {
        preventDefault() {
            this.defaultPrevented = true;
        }
    };
    harness.window.dispatch('beforeunload', dirtyUnload);
    assert.equal(dirtyUnload.defaultPrevented, true);

    harness.document.dispatch('submit', {
        defaultPrevented: false,
        target: harness.dirtyGuardForm
    });
    const submittingUnload = {
        preventDefault() {
            this.defaultPrevented = true;
        }
    };
    harness.window.dispatch('beforeunload', submittingUnload);
    assert.equal(submittingUnload.defaultPrevented, undefined);

    harness.window.dispatch('pageshow', {});
    const restoredUnload = {
        preventDefault() {
            this.defaultPrevented = true;
        }
    };
    harness.window.dispatch('beforeunload', restoredUnload);
    assert.equal(restoredUnload.defaultPrevented, true);
});

test('successful button state replaces loading state with a check state', () => {
    const harness = createHarness();
    const button = new FakeElement(harness.document);
    button.innerHTML = '<span>업로드</span>';
    button.disabled = false;

    harness.ui.setButtonLoading(button, true, '업로드 중');
    assert.equal(button.classList.contains('is-loading'), true);

    harness.ui.setButtonSuccess(button, '완료', 900);
    assert.equal(button.classList.contains('is-loading'), false);
    assert.equal(button.classList.contains('is-success'), true);
    assert.equal(button.getAttribute('aria-disabled'), 'true');
    assert.equal(button.disabled, true);
    assert.equal(button.textContent, '완료');
});

test('clickable row navigates only for an unmodified primary click', () => {
    const harness = createHarness();
    const row = new FakeElement(harness.document, {
        dataset: { detailUrl: '/frog2/detail/1' }
    });
    const target = new FakeElement(harness.document, { parentRow: row });

    harness.document.dispatch('click', clickEvent(target));
    assert.deepEqual(harness.assignedLocations, ['/frog2/detail/1']);
});

test('clickable row ignores interactive controls and selected text', () => {
    const harness = createHarness();
    const row = new FakeElement(harness.document, {
        dataset: { detailUrl: '/frog2/detail/1' }
    });
    const link = new FakeElement(harness.document, {
        interactive: true,
        parentRow: row
    });

    harness.document.dispatch('click', clickEvent(link));
    assert.deepEqual(harness.assignedLocations, []);

    const textTarget = new FakeElement(harness.document, { parentRow: row });
    harness.setSelection('selected row text', row);
    harness.document.dispatch('click', clickEvent(textTarget));
    assert.deepEqual(harness.assignedLocations, []);

    const outside = new FakeElement(harness.document);
    harness.setSelection('selected elsewhere', outside);
    harness.document.dispatch('click', clickEvent(textTarget));
    assert.deepEqual(harness.assignedLocations, ['/frog2/detail/1']);
});

test('shared disclosure keeps detail, row state, and accessible state in sync', () => {
    const harness = createHarness();
    const detail = new FakeElement(harness.document, { hidden: true });
    const row = new FakeElement(harness.document, {
        dataset: { uiDisclosureRow: '' }
    });
    const toggle = new FakeElement(harness.document, {
        dataset: { uiDisclosureToggle: '' },
        parentDisclosureRow: row
    });
    row.disclosureToggle = toggle;
    toggle.disclosureToggle = toggle;
    toggle.setAttribute('aria-controls', 'history-detail-1');
    toggle.setAttribute('aria-expanded', 'false');
    toggle.setAttribute(
        'aria-label',
        '테크핀 레이팅스 2026-08-19 개발서버 3번노드 다운 이력 상세'
    );
    harness.elementsById.set('history-detail-1', detail);

    harness.document.dispatch('click', clickEvent(toggle));
    assert.equal(toggle.getAttribute('aria-expanded'), 'true');
    assert.equal(detail.hidden, false);
    assert.equal(row.classList.contains('is-expanded'), true);
    assert.equal(
        toggle.getAttribute('aria-label'),
        '테크핀 레이팅스 2026-08-19 개발서버 3번노드 다운 이력 상세'
    );

    harness.document.dispatch('click', clickEvent(toggle));
    assert.equal(toggle.getAttribute('aria-expanded'), 'false');
    assert.equal(detail.hidden, true);
    assert.equal(row.classList.contains('is-expanded'), false);
    assert.deepEqual(harness.assignedLocations, []);
});

test('shared disclosure animates open and close before hiding the detail', () => {
    const { content, detail, harness, row, toggle }
        = createAnimatedDisclosureHarness();

    harness.document.dispatch('click', clickEvent(toggle));

    assert.equal(toggle.getAttribute('aria-expanded'), 'true');
    assert.equal(row.classList.contains('is-expanded'), true);
    assert.equal(detail.hidden, false);
    assert.equal(detail.getAttribute('aria-hidden'), null);
    assert.equal(detail.getAttribute('inert'), null);
    assert.equal(content.classList.contains('is-disclosure-expanded'), true);
    assert.equal(content.animations.length, 0);
    content.dispatch('transitionend', {
        target: content,
        propertyName: 'grid-template-rows'
    });
    assert.equal(detail.hidden, false);

    harness.document.dispatch('click', clickEvent(toggle));

    assert.equal(toggle.getAttribute('aria-expanded'), 'false');
    assert.equal(row.classList.contains('is-expanded'), false);
    assert.equal(detail.hidden, false);
    assert.equal(detail.getAttribute('aria-hidden'), 'true');
    assert.equal(detail.getAttribute('inert'), '');
    assert.equal(content.classList.contains('is-disclosure-expanded'), false);
    content.dispatch('transitionend', {
        target: content,
        propertyName: 'grid-template-rows'
    });
    assert.equal(detail.hidden, true);
    assert.equal(detail.getAttribute('aria-hidden'), 'true');
    assert.equal(detail.getAttribute('inert'), '');
});

test('shared disclosure skips animation when reduced motion is requested', () => {
    const { content, detail, harness, row, toggle }
        = createAnimatedDisclosureHarness({ reducedMotion: true });

    harness.document.dispatch('click', clickEvent(toggle));
    assert.equal(toggle.getAttribute('aria-expanded'), 'true');
    assert.equal(row.classList.contains('is-expanded'), true);
    assert.equal(detail.hidden, false);
    assert.equal(detail.getAttribute('aria-hidden'), null);
    assert.equal(detail.getAttribute('inert'), null);
    assert.equal(content.classList.contains('is-disclosure-expanded'), true);
    assert.equal(content.animations.length, 0);

    harness.document.dispatch('click', clickEvent(toggle));
    assert.equal(toggle.getAttribute('aria-expanded'), 'false');
    assert.equal(row.classList.contains('is-expanded'), false);
    assert.equal(detail.hidden, true);
    assert.equal(detail.getAttribute('aria-hidden'), 'true');
    assert.equal(detail.getAttribute('inert'), '');
    assert.equal(content.classList.contains('is-disclosure-expanded'), false);
    assert.equal(content.animations.length, 0);
});

test('shared disclosure cancels rapid reversals and ignores stale finishes', () => {
    const { content, detail, harness, toggle }
        = createAnimatedDisclosureHarness();

    harness.document.dispatch('click', clickEvent(toggle));
    assert.equal(content.classList.contains('is-disclosure-expanded'), true);

    harness.document.dispatch('click', clickEvent(toggle));
    assert.equal(content.classList.contains('is-disclosure-expanded'), false);

    harness.document.dispatch('click', clickEvent(toggle));
    assert.equal(content.classList.contains('is-disclosure-expanded'), true);
    assert.equal(toggle.getAttribute('aria-expanded'), 'true');
    assert.equal(detail.hidden, false);

    content.dispatch('transitionend', {
        target: content,
        propertyName: 'grid-template-rows'
    });
    assert.equal(toggle.getAttribute('aria-expanded'), 'true');
    assert.equal(detail.hidden, false);
    assert.equal(detail.getAttribute('aria-hidden'), null);

    harness.document.dispatch('click', clickEvent(toggle));
    assert.equal(toggle.getAttribute('aria-expanded'), 'false');
    assert.equal(detail.hidden, false);

    content.dispatch('transitionend', {
        target: content,
        propertyName: 'grid-template-rows'
    });
    assert.equal(detail.hidden, true);
    assert.equal(detail.getAttribute('aria-hidden'), 'true');
});

test('shared disclosure row ignores controls, modified clicks, and selected text', () => {
    const harness = createHarness();
    const detail = new FakeElement(harness.document, { hidden: true });
    const row = new FakeElement(harness.document, {
        dataset: { uiDisclosureRow: '' }
    });
    const toggle = new FakeElement(harness.document, {
        dataset: { uiDisclosureToggle: '' },
        parentDisclosureRow: row
    });
    const rowText = new FakeElement(harness.document, {
        parentDisclosureRow: row
    });
    row.disclosureToggle = toggle;
    toggle.disclosureToggle = toggle;
    toggle.setAttribute('aria-controls', 'history-detail-1');
    toggle.setAttribute('aria-expanded', 'false');
    harness.elementsById.set('history-detail-1', detail);

    harness.setSelection('selected row text', row);
    harness.document.dispatch('click', clickEvent(rowText));
    assert.equal(toggle.getAttribute('aria-expanded'), 'false');

    harness.setSelection('');
    const editLink = new FakeElement(harness.document, {
        interactive: true,
        parentDisclosureRow: row
    });
    harness.document.dispatch('click', clickEvent(editLink));
    assert.equal(toggle.getAttribute('aria-expanded'), 'false');

    const modified = clickEvent(rowText);
    modified.shiftKey = true;
    harness.document.dispatch('click', modified);
    assert.equal(toggle.getAttribute('aria-expanded'), 'false');

    const outside = new FakeElement(harness.document);
    harness.setSelection('selected elsewhere', outside);
    harness.document.dispatch('click', clickEvent(rowText));
    assert.equal(toggle.getAttribute('aria-expanded'), 'true');
    assert.equal(detail.hidden, false);
});

test('non-scrollable table surface is ready below the measured header', () => {
    const harness = createHarness({
        headerBottom: 72.4,
        scrollRegions: [{ clientWidth: 600, scrollWidth: 600 }]
    });
    const region = harness.scrollRegions[0];
    const surface = harness.workSurfaces[0];

    assert.equal(region.dataset.uiScrollable, 'false');
    assert.equal(region.getAttribute('tabindex'), null);
    assert.equal(region.getAttribute('role'), null);
    assert.equal(surface.classList.contains('is-table-sticky-ready'), true);
    assert.equal(
        harness.document.documentElement.style.getPropertyValue(
            '--table-sticky-offset'
        ),
        '73px'
    );
});

test('table readiness follows overflow, resize, and header ResizeObserver', () => {
    const harness = createHarness({
        headerBottom: 72,
        resizeObserver: true,
        scrollRegions: [{
            clientWidth: 400,
            label: '고객사 목록',
            scrollWidth: 640
        }]
    });
    const region = harness.scrollRegions[0];
    const surface = harness.workSurfaces[0];

    assert.equal(region.dataset.uiScrollable, 'true');
    assert.equal(region.getAttribute('tabindex'), '0');
    assert.equal(region.getAttribute('role'), 'region');
    assert.equal(region.getAttribute('aria-label'), '고객사 목록');
    assert.equal(surface.classList.contains('is-table-sticky-ready'), false);

    region.scrollWidth = 400;
    harness.header.bounds.bottom = 84;
    harness.window.dispatch('resize');
    harness.flushFrames();

    assert.equal(region.dataset.uiScrollable, 'false');
    assert.equal(region.getAttribute('tabindex'), null);
    assert.equal(region.getAttribute('role'), null);
    assert.equal(region.getAttribute('aria-label'), null);
    assert.equal(surface.classList.contains('is-table-sticky-ready'), true);
    assert.equal(
        harness.document.documentElement.style.getPropertyValue(
            '--table-sticky-offset'
        ),
        '84px'
    );

    harness.header.bounds.bottom = 91;
    harness.triggerHeaderResize();
    harness.flushFrames();
    assert.equal(
        harness.document.documentElement.style.getPropertyValue(
            '--table-sticky-offset'
        ),
        '91px'
    );
});

test('scroll hint is exposed only while its table actually overflows', () => {
    const harness = createHarness({
        headerBottom: 72,
        scrollRegions: [{
            clientWidth: 400,
            describedBy: 'historyTableHelp',
            hintId: 'historyScrollHint',
            label: '정기점검 이력 비교표',
            scrollWidth: 640
        }]
    });
    const region = harness.scrollRegions[0];
    const hint = harness.scrollHints[0];

    assert.equal(hint.hidden, false);
    assert.equal(
        region.getAttribute('aria-describedby'),
        'historyTableHelp historyScrollHint'
    );

    region.scrollWidth = 400;
    harness.window.dispatch('resize');
    harness.flushFrames();

    assert.equal(hint.hidden, true);
    assert.equal(region.getAttribute('aria-describedby'), 'historyTableHelp');
});
