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
    const children = [...(options.children || [])];
    let textContent = '';
    const element = {
        classList: new ClassList(),
        children,
        hidden: false,
        addEventListener(name, listener) {
            if (!listeners.has(name)) {
                listeners.set(name, []);
            }
            listeners.get(name).push(listener);
        },
        appendChild(child) {
            children.push(child);
            return child;
        },
        contains(candidate) {
            return candidate === element || children.includes(candidate);
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
            if (options.querySelectorAll) {
                return options.querySelectorAll(selector);
            }
            if (selector === '[role="option"]') {
                return children.filter((child) =>
                    child.getAttribute('role') === 'option');
            }
            return [];
        },
        removeAttribute(name) {
            attributes.delete(name);
        },
        setAttribute(name, value) {
            attributes.set(name, String(value));
        },
        scrollIntoView() {
        }
    };
    Object.defineProperty(element, 'textContent', {
        get() {
            return textContent;
        },
        set(value) {
            textContent = String(value);
            children.length = 0;
        }
    });
    return element;
}

function createHarness({ mobile, dropdown = false, quickNav = false,
        otherDialogOpen = false, searchPayload = null,
        searchStatus = 200 }) {
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
    firstNavLink.textContent = '대시보드';
    firstNavLink.href = '/frog2/dashboard';
    lastNavLink.textContent = '마이페이지';
    lastNavLink.href = '/frog2/mypage';
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
    const quickNavResults = quickNav ? createElement(document) : null;
    const quickNavEmpty = quickNav ? createElement(document) : null;
    const quickNavStatus = quickNav ? createElement(document) : null;
    if (quickNavInput) {
        quickNavInput.value = '';
        quickNavBackdrop.setAttribute(
            'data-search-url', '/frog2/search');
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
            if (id === 'quickNavStatus') return quickNavStatus;
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
    const fetchCalls = [];
    let assignedLocation = null;
    const window = {
        clearTimeout() {},
        location: {
            assign(url) {
                assignedLocation = url;
            }
        },
        matchMedia() {
            return mediaQuery;
        },
        setTimeout(callback) {
            callback();
            return 1;
        }
    };
    if (searchPayload !== null) {
        window.fetch = (url, options) => {
            fetchCalls.push({ url, options });
            return Promise.resolve({
                ok: searchStatus >= 200 && searchStatus < 300,
                status: searchStatus,
                json() {
                    return Promise.resolve(searchPayload);
                }
            });
        };
    }
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
        quickNavEmpty,
        quickNavInput,
        quickNavOpenButton,
        quickNavResults,
        quickNavStatus,
        fetchCalls,
        get assignedLocation() {
            return assignedLocation;
        },
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

test('integrated search fetches and renders safe domain results', async () => {
    const harness = createHarness({
        mobile: false,
        quickNav: true,
        searchPayload: {
            partial: true,
            unavailableCategories: ['자료실'],
            results: [
                {
                    category: '고객사',
                    label: '조폐공사',
                    description: 'Vertica 12.0.2-1',
                    url: '/frog2/customers?view=detail&customerName=%EC%A1%B0%ED%8F%90'
                },
                {
                    category: '외부',
                    label: '차단 대상',
                    description: '',
                    url: 'https://example.com'
                }
            ]
        }
    });

    harness.quickNavOpenButton.dispatch('click');
    harness.quickNavInput.value = '조폐';
    harness.quickNavInput.dispatch('input');
    await new Promise((resolve) => setImmediate(resolve));
    await new Promise((resolve) => setImmediate(resolve));

    assert.equal(harness.fetchCalls.length, 1);
    assert.equal(harness.fetchCalls[0].url,
        '/frog2/search?q=%EC%A1%B0%ED%8F%90');
    assert.equal(harness.fetchCalls[0].options.credentials, 'same-origin');
    assert.equal(harness.quickNavResults.children.length, 2);
    assert.equal(harness.quickNavResults.children[0].children[0].textContent,
        '고객사');
    const option = harness.quickNavResults.children[1];
    const link = option.children[0];
    assert.equal(link.children[0].textContent, '고객사');
    assert.equal(link.children[1].children[0].textContent, '조폐공사');
    assert.equal(harness.quickNavStatus.textContent,
        '업무 데이터 검색 결과 1건 · 자료실 제외');

    harness.quickNavInput.dispatch('keydown', keyboardEvent('Enter'));
    assert.equal(harness.assignedLocation,
        '/frog2/customers?view=detail&customerName=%EC%A1%B0%ED%8F%90');
});

test('integrated search waits for two characters before requesting data', () => {
    const harness = createHarness({
        mobile: false,
        quickNav: true,
        searchPayload: { results: [] }
    });

    harness.quickNavOpenButton.dispatch('click');
    harness.quickNavInput.value = '조';
    harness.quickNavInput.dispatch('input');

    assert.equal(harness.fetchCalls.length, 0);
    assert.equal(harness.quickNavStatus.textContent,
        '2자 이상 입력하면 업무 데이터까지 검색합니다.');
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
