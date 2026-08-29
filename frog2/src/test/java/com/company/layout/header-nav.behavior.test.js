'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync(
    'src/main/webapp/resources/js/header_nav.js',
    'utf8'
);

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
        const enabled = force === undefined ? !this.names.has(name) : force;
        if (enabled) {
            this.names.add(name);
        } else {
            this.names.delete(name);
        }
        return enabled;
    }
}

function createElement(document, options = {}) {
    const attributes = new Map();
    const listeners = new Map();
    const element = {
        classList: new ClassList(),
        hidden: false,
        addEventListener(name, listener) {
            if (!listeners.has(name)) {
                listeners.set(name, []);
            }
            listeners.get(name).push(listener);
        },
        contains(candidate) {
            return candidate === element || (options.children || []).includes(candidate);
        },
        closest(selector) {
            return options.closest ? options.closest(selector) : null;
        },
        dispatch(name, event = {}) {
            (listeners.get(name) || []).forEach((listener) => listener(event));
        },
        focus() {
            document.activeElement = element;
        },
        getAttribute(name) {
            return attributes.has(name) ? attributes.get(name) : null;
        },
        matches() {
            return false;
        },
        querySelector(selector) {
            return options.querySelector ? options.querySelector(selector) : null;
        },
        querySelectorAll(selector) {
            return options.querySelectorAll ? options.querySelectorAll(selector) : [];
        },
        removeAttribute(name) {
            attributes.delete(name);
        },
        setAttribute(name, value) {
            attributes.set(name, String(value));
        }
    };
    return element;
}

function createHarness({ mobile, dropdown = false, quickNav = false,
        otherDialogOpen = false }) {
    const documentListeners = new Map();
    const document = {
        activeElement: null,
        addEventListener(name, listener) {
            if (!documentListeners.has(name)) {
                documentListeners.set(name, []);
            }
            documentListeners.get(name).push(listener);
        },
        createElement() {
            return createElement(document);
        },
        dispatch(name, event) {
            (documentListeners.get(name) || []).forEach((listener) => listener(event));
        }
    };

    const mobileToggle = createElement(document);
    const firstNavLink = createElement(document);
    const lastNavLink = createElement(document);
    const menu = createElement(document, {
        querySelector(selector) {
            return selector === 'a[href]' ? menuLink : null;
        }
    });
    const menuLink = createElement(document);
    let dropdownItem = null;
    let dropdownToggle = null;
    if (dropdown) {
        dropdownToggle = createElement(document);
        dropdownItem = createElement(document, {
            children: [dropdownToggle, menu, menuLink],
            querySelector(selector) {
                if (selector === '.dropdown-toggle') return dropdownToggle;
                if (selector === '.dropdown-menu') return menu;
                return null;
            }
        });
    }

    const primaryNavigation = createElement(document, {
        children: [firstNavLink, lastNavLink, dropdownItem, dropdownToggle, menu, menuLink]
            .filter(Boolean),
        querySelector(selector) {
            if (selector === 'a[href], button') return firstNavLink;
            return null;
        },
        querySelectorAll(selector) {
            if (selector === 'a[href]:not(#logoutLink)') {
                return [];
            }
            if (selector === 'a[href]'
                    || selector === 'a[href], button:not([disabled])') {
                return [firstNavLink, lastNavLink];
            }
            return [];
        }
    });
    const quickNavOpenButton = quickNav ? createElement(document) : null;
    const quickNavBackdrop = quickNav ? createElement(document) : null;
    const quickNavDialog = quickNav ? createElement(document) : null;
    const quickNavCloseButton = quickNav ? createElement(document) : null;
    const quickNavInput = quickNav ? createElement(document) : null;
    const quickNavResults = quickNav ? createElement(document, {
        querySelectorAll() {
            return [];
        }
    }) : null;
    const quickNavEmpty = quickNav ? createElement(document) : null;
    if (quickNavInput) {
        quickNavInput.value = '';
    }
    const header = createElement(document, {
        children: [mobileToggle, primaryNavigation, firstNavLink, lastNavLink,
            dropdownItem, dropdownToggle, menu, menuLink]
            .filter(Boolean),
        querySelector() {
            return null;
        },
        querySelectorAll(selector) {
            return selector === '.main-nav .dropdown' && dropdown ? [dropdownItem] : [];
        }
    });

    Object.assign(document, {
        body: { appendChild() {} },
        getElementById(id) {
            if (id === 'mobileNavToggle') return mobileToggle;
            if (id === 'primaryNavigation') return primaryNavigation;
            if (id === 'quickNavOpenButton') return quickNavOpenButton;
            if (id === 'quickNavBackdrop') return quickNavBackdrop;
            if (id === 'quickNavDialog') return quickNavDialog;
            if (id === 'quickNavCloseButton') return quickNavCloseButton;
            if (id === 'quickNavInput') return quickNavInput;
            if (id === 'quickNavResults') return quickNavResults;
            if (id === 'quickNavEmpty') return quickNavEmpty;
            return null;
        },
        querySelector(selector) {
            return selector === '.main-header' ? header : null;
        }
    });

    const mediaQuery = {
        matches: mobile,
        addEventListener() {}
    };
    const window = {
        matchMedia() {
            return mediaQuery;
        },
        setTimeout(callback) {
            callback();
        }
    };
    let quickNavOpen = false;
    let quickNavOpenCalls = 0;
    if (quickNav) {
        window.Frog2UI = {
            createDialogController() {
                return {
                    close() {
                        quickNavOpen = false;
                    },
                    isOpen() {
                        return quickNavOpen;
                    },
                    open() {
                        quickNavOpen = true;
                        quickNavOpenCalls += 1;
                    }
                };
            },
            hasOpenDialog() {
                return otherDialogOpen || quickNavOpen;
            }
        };
    }

    vm.runInNewContext(source, { document, window });
    document.dispatch('DOMContentLoaded', {});

    return {
        document,
        dropdownItem,
        dropdownToggle,
        firstNavLink,
        header,
        lastNavLink,
        mobileToggle,
        primaryNavigation,
        quickNavOpenButton,
        get quickNavOpenCalls() {
            return quickNavOpenCalls;
        }
    };
}

function keyboardEvent(key) {
    return {
        key,
        preventDefault() {
            this.defaultPrevented = true;
        },
        stopPropagation() {}
    };
}

test('mobile menu keeps aria-expanded in sync and Escape restores focus', () => {
    const harness = createHarness({ mobile: true });

    assert.equal(harness.mobileToggle.getAttribute('aria-expanded'), 'false');
    assert.equal(harness.primaryNavigation.getAttribute('aria-hidden'), 'true');

    harness.mobileToggle.dispatch('click');
    assert.equal(harness.header.classList.contains('mobile-nav-open'), true);
    assert.equal(harness.mobileToggle.getAttribute('aria-expanded'), 'true');
    assert.equal(harness.primaryNavigation.getAttribute('aria-hidden'), null);
    assert.equal(harness.document.activeElement, harness.firstNavLink);

    harness.document.dispatch('keydown', keyboardEvent('Escape'));
    assert.equal(harness.header.classList.contains('mobile-nav-open'), false);
    assert.equal(harness.mobileToggle.getAttribute('aria-expanded'), 'false');
    assert.equal(harness.primaryNavigation.getAttribute('aria-hidden'), 'true');
    assert.equal(harness.document.activeElement, harness.mobileToggle);
});

test('mobile menu leaves Tab navigation non-modal', () => {
    const harness = createHarness({ mobile: true });

    harness.mobileToggle.dispatch('click');
    harness.document.activeElement = harness.lastNavLink;
    const forwardTab = keyboardEvent('Tab');
    harness.primaryNavigation.dispatch('keydown', forwardTab);
    assert.equal(forwardTab.defaultPrevented, undefined);
    assert.equal(harness.document.activeElement, harness.lastNavLink);

    harness.document.activeElement = harness.firstNavLink;
    const backwardTab = keyboardEvent('Tab');
    backwardTab.shiftKey = true;
    harness.primaryNavigation.dispatch('keydown', backwardTab);
    assert.equal(backwardTab.defaultPrevented, undefined);
    assert.equal(harness.document.activeElement, harness.firstNavLink);

    harness.mobileToggle.dispatch('click');
    assert.equal(harness.document.activeElement, harness.mobileToggle);
});

test('desktop dropdown Escape closes the menu and restores toggle focus', () => {
    const harness = createHarness({ mobile: false, dropdown: true });

    harness.dropdownToggle.dispatch('click');
    assert.equal(harness.dropdownItem.classList.contains('open'), true);
    assert.equal(harness.dropdownToggle.getAttribute('aria-expanded'), 'true');

    harness.document.dispatch('keydown', keyboardEvent('Escape'));
    assert.equal(harness.dropdownItem.classList.contains('open'), false);
    assert.equal(harness.dropdownToggle.getAttribute('aria-expanded'), 'false');
    assert.equal(harness.document.activeElement, harness.dropdownToggle);
});

test('quick navigation stays closed while another dialog is open', () => {
    const harness = createHarness({
        mobile: false,
        otherDialogOpen: true,
        quickNav: true
    });

    harness.quickNavOpenButton.dispatch('click');
    assert.equal(harness.quickNavOpenCalls, 0);

    for (const modifier of ['ctrlKey', 'metaKey']) {
        const shortcut = keyboardEvent('k');
        shortcut.target = createElement(harness.document);
        shortcut.altKey = false;
        shortcut.shiftKey = false;
        shortcut.ctrlKey = false;
        shortcut.metaKey = false;
        shortcut[modifier] = true;

        harness.document.dispatch('keydown', shortcut);

        assert.equal(shortcut.defaultPrevented, true);
        assert.equal(harness.quickNavOpenCalls, 0);
    }
});
