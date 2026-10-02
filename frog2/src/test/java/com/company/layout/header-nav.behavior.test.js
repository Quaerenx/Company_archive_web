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
        tagName: (options.tagName || 'button').toUpperCase(),
        tabIndex: 0,
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
            return candidate === element || children.some(child => child.contains(candidate));
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
        click() {
            const event = keyboardEvent('');
            element.dispatch('click', event);
            if (!event.defaultPrevented && element.href) document.activateLink(element.href);
        },
        getAttribute(name) {
            return attributes.has(name) ? attributes.get(name) : null;
        },
        matches() {
            return false;
        },
        querySelector(selector) {
            if (options.querySelector) return options.querySelector(selector);
            if (selector === '.quick-nav-result-link') {
                return children.find(child => child.className === 'quick-nav-result-link') || null;
            }
            return null;
        },
        querySelectorAll(selector) {
            if (options.querySelectorAll) {
                return options.querySelectorAll(selector);
            }
            if (selector === '.quick-nav-result') {
                return children.filter(child => child.className === 'quick-nav-result');
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
        searchStatus = 200, deferredSearch = false, abortSupported = true }) {
    const documentListeners = new Map();
    const document = {
        activeElement: null,
        addEventListener(name, listener) {
            if (!documentListeners.has(name)) {
                documentListeners.set(name, []);
            }
            documentListeners.get(name).push(listener);
        },
        createElement(tagName) {
            return createElement(document, { tagName });
        },
        activateLink(href) {
            assignedLocation = href;
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
    const pendingSearches = [];
    const windowListeners = new Map();
    let assignedLocation = null;
    const window = {
        AbortController: abortSupported ? AbortController : undefined,
        addEventListener(name, listener) {
            if (!windowListeners.has(name)) windowListeners.set(name, []);
            windowListeners.get(name).push(listener);
        },
        dispatch(name) {
            (windowListeners.get(name) || []).forEach((listener) => listener());
        },
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
            if (deferredSearch) {
                return new Promise((resolve, reject) => {
                    pendingSearches.push({resolve, reject});
                });
            }
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
    let quickNavController;
    if (quickNav) {
        window.Frog2UI = {
            createDialogController(dialog, options) {
                quickNavController = {
                    close() {
                        if (!quickNavOpen) return;
                        quickNavOpen = false;
                        if (options && typeof options.onClose === 'function') {
                            options.onClose();
                        }
                    },
                    isOpen() {
                        return quickNavOpen;
                    },
                    open() {
                        quickNavOpen = true;
                        quickNavOpenCalls += 1;
                    }
                };
                return quickNavController;
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
        quickNavBackdrop,
        quickNavController,
        quickNavInput,
        quickNavOpenButton,
        quickNavCloseButton,
        quickNavResults,
        quickNavStatus,
        fetchCalls,
        window,
        resolveSearch(index, payload = {results: []}) {
            pendingSearches[index].resolve({
                ok: true,
                status: 200,
                json() { return Promise.resolve(payload); }
            });
        },
        rejectSearch(index, error) {
            pendingSearches[index].reject(error);
        },
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

test('controller-level quick navigation close cleans the backdrop and search state', () => {
    const harness = createHarness({ mobile: false, quickNav: true });
    harness.quickNavOpenButton.dispatch('click');
    assert.equal(harness.quickNavBackdrop.hidden, false);
    assert.equal(harness.quickNavInput.getAttribute('aria-expanded'), null);
    harness.quickNavController.close();

    assert.equal(harness.quickNavBackdrop.hidden, true);
    assert.equal(harness.quickNavBackdrop.getAttribute('aria-hidden'), 'true');
    assert.equal(harness.quickNavOpenButton.getAttribute('aria-expanded'), 'false');
    assert.equal(harness.quickNavInput.getAttribute('aria-expanded'), null);
    assert.equal(harness.quickNavInput.getAttribute('aria-activedescendant'), null);
});

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


test('integrated search reuses an unchanged query and aborts superseded requests', async () => {
    const harness = createHarness({
        mobile: false, quickNav: true, searchPayload: {results: []}, deferredSearch: true
    });
    harness.quickNavOpenButton.dispatch('click');
    harness.quickNavInput.value = 'alpha';
    harness.quickNavInput.dispatch('input');
    harness.quickNavInput.value = ' alpha ';
    harness.quickNavInput.dispatch('input');
    assert.equal(harness.fetchCalls.length, 1);
    assert.equal(harness.fetchCalls[0].options.signal.aborted, false);

    harness.quickNavInput.value = 'beta';
    harness.quickNavInput.dispatch('input');
    assert.equal(harness.fetchCalls.length, 2);
    assert.equal(harness.fetchCalls[0].options.signal.aborted, true);
    harness.resolveSearch(1, {results: [{
        category: '고객사', label: 'Beta', url: '/frog2/customers?customerName=Beta'
    }]});
    await new Promise((resolve) => setImmediate(resolve));
    const currentResult = harness.quickNavResults.children[1];
    harness.resolveSearch(0, {results: [{
        category: '고객사', label: 'Alpha', url: '/frog2/customers?customerName=Alpha'
    }]});
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(harness.quickNavResults.children[1], currentResult);
    harness.quickNavInput.dispatch('input');
    assert.equal(harness.fetchCalls.length, 2);
});

test('integrated search cancels requests on short input, close and pagehide', () => {
    const harness = createHarness({
        mobile: false, quickNav: true, searchPayload: {results: []}, deferredSearch: true
    });
    harness.quickNavOpenButton.dispatch('click');
    harness.quickNavInput.value = 'alpha';
    harness.quickNavInput.dispatch('input');
    harness.quickNavInput.value = 'a';
    harness.quickNavInput.dispatch('input');
    assert.equal(harness.fetchCalls.length, 1);
    assert.equal(harness.fetchCalls[0].options.signal.aborted, true);

    harness.quickNavInput.value = 'beta';
    harness.quickNavInput.dispatch('input');
    harness.quickNavCloseButton.dispatch('click');
    assert.equal(harness.fetchCalls[1].options.signal.aborted, true);
    harness.quickNavOpenButton.dispatch('click');
    harness.quickNavInput.value = 'gamma';
    harness.quickNavInput.dispatch('input');
    harness.window.dispatch('pagehide');
    assert.equal(harness.fetchCalls[2].options.signal.aborted, true);
});

test('integrated search can retry an unchanged query after a request failure', async () => {
    const harness = createHarness({
        mobile: false, quickNav: true, searchPayload: {results: []}, deferredSearch: true
    });
    harness.quickNavOpenButton.dispatch('click');
    harness.quickNavInput.value = 'alpha';
    harness.quickNavInput.dispatch('input');
    harness.rejectSearch(0, new Error('Temporary search failure'));
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(harness.quickNavStatus.textContent,
        '업무 데이터 검색을 일시적으로 사용할 수 없습니다.');
    harness.quickNavInput.dispatch('input');
    assert.equal(harness.fetchCalls.length, 2);
});

test('integrated search preserves the stale-response guard without AbortController', async () => {
    const harness = createHarness({
        mobile: false, quickNav: true, searchPayload: {results: []},
        deferredSearch: true, abortSupported: false
    });
    harness.quickNavOpenButton.dispatch('click');
    harness.quickNavInput.value = 'alpha';
    harness.quickNavInput.dispatch('input');
    harness.quickNavInput.value = 'beta';
    harness.quickNavInput.dispatch('input');
    assert.equal(harness.fetchCalls[0].options.signal, undefined);
    harness.resolveSearch(1);
    await new Promise((resolve) => setImmediate(resolve));
    const currentStatus = harness.quickNavStatus.textContent;
    harness.resolveSearch(0, {results: [{
        category: '고객사', label: 'Alpha', url: '/frog2/customers?customerName=Alpha'
    }]});
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(harness.quickNavStatus.textContent, currentStatus);
});


async function resultLinksHarness(options = {}) {
    const h = createHarness({
        mobile: false, quickNav: true, ...options,
        searchPayload: { results: [
            { category: 'Customer', group: 'customers', label: 'Alpha',
                url: '/frog2/customers?customerName=Alpha',
                moreUrl: '/frog2/customers',
                actions: [{ label: 'History', url: '/frog2/maintenance?customerName=Alpha' }] },
            { category: 'Customer', group: 'customers', label: 'Beta',
                url: '/frog2/customers?customerName=Beta' }
        ] }
    });
    h.quickNavOpenButton.dispatch('click');
    h.quickNavInput.value = 'test';
    h.quickNavInput.dispatch('input');
    await new Promise(resolve => setImmediate(resolve));
    return h;
}

test('search results expose native links including group and customer actions', async () => {
    const h = await resultLinksHarness();
    const [group, alpha, beta] = h.quickNavResults.children;
    assert.equal(group.getAttribute('role'), null);
    assert.equal(alpha.getAttribute('role'), null);
    assert.equal(alpha.getAttribute('aria-selected'), null);
    for (const link of [group.children[1], alpha.children[0], alpha.children[1].children[0], beta.children[0]]) {
        assert.equal(link.tagName, 'A');
        assert.equal(link.tabIndex, 0);
        assert.ok(link.href.startsWith('/frog2/'));
    }
    alpha.children[1].children[0].click();
    assert.equal(h.assignedLocation, '/frog2/maintenance?customerName=Alpha');
});

test('search arrows move real link focus while Home and End still edit the query', async () => {
    const h = await resultLinksHarness();
    const alpha = h.quickNavResults.children[1].children[0];
    const beta = h.quickNavResults.children[2].children[0];
    for (const key of ['Home', 'End']) {
        const event = keyboardEvent(key);
        h.quickNavInput.dispatch('keydown', event);
        assert.equal(event.defaultPrevented, undefined);
    }
    h.quickNavInput.dispatch('keydown', keyboardEvent('ArrowDown'));
    assert.equal(h.document.activeElement, alpha);
    alpha.dispatch('keydown', keyboardEvent('ArrowDown'));
    assert.equal(h.document.activeElement, beta);
    beta.dispatch('keydown', keyboardEvent('Home'));
    assert.equal(h.document.activeElement, alpha);
    alpha.dispatch('keydown', keyboardEvent('End'));
    assert.equal(h.document.activeElement, beta);
    beta.dispatch('keydown', keyboardEvent('ArrowUp'));
    assert.equal(h.document.activeElement, alpha);
    const modified = Object.assign(keyboardEvent('ArrowDown'), { ctrlKey: true });
    alpha.dispatch('keydown', modified);
    assert.equal(modified.defaultPrevented, undefined);
    assert.equal(h.document.activeElement, alpha);
    assert.equal(h.quickNavInput.getAttribute('aria-activedescendant'), null);
});

test('replacing search results returns removed link focus to the query', async () => {
    const h = await resultLinksHarness();
    const link = h.quickNavResults.children[1].children[0];
    link.focus();
    h.quickNavInput.value = 'another';
    h.quickNavInput.dispatch('input');
    assert.equal(h.document.activeElement, h.quickNavInput);
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(h.document.activeElement, h.quickNavInput);
});
