'use strict';

document.addEventListener('DOMContentLoaded', function() {
    var headerElement = document.querySelector('.main-header');

    window.Frog2Csrf = Object.freeze({
        token: function() {
            var token = headerElement ? headerElement.getAttribute('data-csrf-token') : '';
            if (!token) {
                throw new Error('CSRF token is unavailable');
            }
            return token;
        },
        appendTo: function(form) {
            var input = document.createElement('input');
            input.type = 'hidden';
            input.name = '_csrf';
            input.value = this.token();
            form.appendChild(input);
        }
    });

    var logoutLink = document.getElementById('logoutLink');
    if (logoutLink) {
        logoutLink.addEventListener('click', function(event) {
            event.preventDefault();
            if (logoutLink.getAttribute('aria-busy') === 'true') {
                return;
            }
            logoutLink.setAttribute('aria-busy', 'true');
            logoutLink.setAttribute('aria-disabled', 'true');
            var form = document.createElement('form');
            form.method = 'POST';
            form.action = logoutLink.href;
            window.Frog2Csrf.appendTo(form);
            document.body.appendChild(form);
            form.submit();
        });
    }

    if (!headerElement) {
        return;
    }

    var mobileToggle = document.getElementById('mobileNavToggle');
    var primaryNavigation = document.getElementById('primaryNavigation');
    var dropdownItems = Array.prototype.slice.call(
            headerElement.querySelectorAll('.main-nav .dropdown'));
    var mobileMedia = window.matchMedia('(max-width: 768px)');
    var suppressDropdownFocusOpen = false;
    var quickNavOpenButton = document.getElementById('quickNavOpenButton');
    var quickNavBackdrop = document.getElementById('quickNavBackdrop');
    var quickNavDialog = document.getElementById('quickNavDialog');
    var quickNavCloseButton = document.getElementById('quickNavCloseButton');
    var quickNavInput = document.getElementById('quickNavInput');
    var quickNavResults = document.getElementById('quickNavResults');
    var quickNavEmpty = document.getElementById('quickNavEmpty');
    var quickNavStatus = document.getElementById('quickNavStatus');

    if (!mobileToggle || !primaryNavigation) {
        return;
    }

    function isMobile() {
        return mobileMedia.matches;
    }

    function dropdownToggle(item) {
        return item.querySelector('.dropdown-toggle');
    }

    function setDropdown(item, shouldOpen) {
        dropdownItems.forEach(function(otherItem) {
            if (otherItem !== item && shouldOpen) {
                otherItem.classList.remove('open');
                dropdownToggle(otherItem).setAttribute('aria-expanded', 'false');
            }
        });

        item.classList.toggle('open', shouldOpen);
        dropdownToggle(item).setAttribute(
                'aria-expanded', shouldOpen ? 'true' : 'false');
    }

    function closeAllDropdowns() {
        dropdownItems.forEach(function(item) {
            setDropdown(item, false);
        });
    }

    function focusDropdownToggle(item) {
        suppressDropdownFocusOpen = true;
        dropdownToggle(item).focus();
        window.setTimeout(function() {
            suppressDropdownFocusOpen = false;
        }, 0);
    }

    function mobileMenuIsOpen() {
        return headerElement.classList.contains('mobile-nav-open');
    }

    function mobileMenuFocusableControls() {
        return Array.prototype.slice.call(primaryNavigation.querySelectorAll(
                'a[href], button:not([disabled])')).filter(function(control) {
            if (typeof control.closest !== 'function') {
                return true;
            }
            var menu = control.closest('.dropdown-menu');
            if (!menu) {
                return true;
            }
            var dropdown = menu.closest('.dropdown');
            return dropdown && dropdown.classList.contains('open');
        });
    }

    function focusedDropdownMenu() {
        return dropdownItems.find(function(item) {
            var menu = item.querySelector('.dropdown-menu');
            return item.classList.contains('open')
                    && menu
                    && menu.contains(document.activeElement);
        });
    }

    function focusCurrentDesktopNavigation() {
        var currentLink = primaryNavigation.querySelector('[aria-current="page"]');
        var currentDropdown = currentLink ? currentLink.closest('.dropdown') : null;
        if (currentDropdown) {
            focusDropdownToggle(currentDropdown);
            return;
        }
        var firstControl = primaryNavigation.querySelector('a[href], button');
        var brandLink = headerElement.querySelector('.brand-link');
        var target = currentLink || firstControl || brandLink;
        if (target) {
            target.focus();
        }
    }

    function setMobileMenu(shouldOpen, options) {
        var open = isMobile() && shouldOpen;
        var restoreFocus = options && options.restoreFocus;

        headerElement.classList.toggle('mobile-nav-open', open);
        mobileToggle.setAttribute('aria-expanded', open ? 'true' : 'false');
        mobileToggle.setAttribute('aria-label', open ? '메뉴 닫기' : '메뉴 열기');

        if (!open && primaryNavigation.contains(document.activeElement)) {
            mobileToggle.focus();
        }

        if (isMobile() && !open) {
            primaryNavigation.setAttribute('aria-hidden', 'true');
            closeAllDropdowns();
        } else {
            primaryNavigation.removeAttribute('aria-hidden');
        }

        if (open) {
            var firstControl = mobileMenuFocusableControls()[0];
            if (firstControl) {
                firstControl.focus();
            }
        } else if (restoreFocus) {
            mobileToggle.focus();
        }
    }

    function handleViewportChange() {
        var mobile = isMobile();
        var focusWillBeHidden = mobile
                && primaryNavigation.contains(document.activeElement);
        var mobileToggleHadFocus = document.activeElement === mobileToggle;
        var desktopDropdownFocus = focusedDropdownMenu();

        closeAllDropdowns();
        headerElement.classList.remove('mobile-nav-open');
        mobileToggle.setAttribute('aria-expanded', 'false');
        mobileToggle.setAttribute('aria-label', '메뉴 열기');

        if (mobile) {
            primaryNavigation.setAttribute('aria-hidden', 'true');
            if (focusWillBeHidden) {
                mobileToggle.focus();
            }
        } else {
            primaryNavigation.removeAttribute('aria-hidden');
            if (mobileToggleHadFocus) {
                focusCurrentDesktopNavigation();
            } else if (desktopDropdownFocus) {
                focusDropdownToggle(desktopDropdownFocus);
            }
        }
    }

    function initializeQuickNavigation() {
        if (!quickNavOpenButton || !quickNavBackdrop || !quickNavDialog
                || !quickNavCloseButton || !quickNavInput || !quickNavResults
                || !quickNavEmpty || !quickNavStatus || !window.Frog2UI) {
            return;
        }

        var dialogController = window.Frog2UI.createDialogController(quickNavDialog);
        var menuEntries = [];
        var remoteEntries = [];
        var seen = new Set();
        var visibleEntries = [];
        var activeIndex = -1;
        var requestVersion = 0;
        var searchTimer = null;
        var searchEndpoint = quickNavBackdrop.getAttribute('data-search-url');

        primaryNavigation.querySelectorAll('a[href]:not(#logoutLink)').forEach(function(link) {
            var label = link.textContent.replace(/\s+/g, ' ').trim();
            var url = link.href;
            var key = label + '\n' + url;
            if (!label || !url || seen.has(key)) {
                return;
            }
            seen.add(key);
            menuEntries.push({
                category: '메뉴',
                label: label,
                description: '',
                normalizedLabel: label.toLocaleLowerCase('ko-KR'),
                url: url
            });
        });

        function queryLength(query) {
            return Array.from(query).length;
        }

        function setStatus(message) {
            quickNavStatus.textContent = message || '';
            quickNavStatus.hidden = !message;
        }

        function cancelPendingSearch() {
            requestVersion += 1;
            if (searchTimer !== null) {
                window.clearTimeout(searchTimer);
                searchTimer = null;
            }
        }

        function setActive(index) {
            if (!visibleEntries.length) {
                activeIndex = -1;
                quickNavInput.removeAttribute('aria-activedescendant');
                return;
            }
            activeIndex = (index + visibleEntries.length) % visibleEntries.length;
            quickNavResults.querySelectorAll('[role="option"]').forEach(function(option, optionIndex) {
                var active = optionIndex === activeIndex;
                option.classList.toggle('is-active', active);
                option.setAttribute('aria-selected', active ? 'true' : 'false');
                if (active) {
                    quickNavInput.setAttribute('aria-activedescendant', option.id);
                    option.scrollIntoView({ block: 'nearest' });
                }
            });
        }

        function renderResults() {
            var query = quickNavInput.value.trim().toLocaleLowerCase('ko-KR');
            var matchingMenus = menuEntries.filter(function(entry) {
                return !query || entry.normalizedLabel.indexOf(query) >= 0;
            });
            visibleEntries = matchingMenus.concat(remoteEntries);
            quickNavResults.textContent = '';

            visibleEntries.forEach(function(entry, index) {
                var option = document.createElement('li');
                option.id = 'quick-nav-option-' + index;
                option.className = 'quick-nav-result';
                option.setAttribute('role', 'option');
                option.setAttribute('aria-selected', 'false');

                var link = document.createElement('a');
                link.className = 'quick-nav-result-link';
                link.href = entry.url;
                link.tabIndex = -1;

                var category = document.createElement('span');
                category.className = 'quick-nav-result-type';
                category.textContent = entry.category;
                link.appendChild(category);

                var copy = document.createElement('span');
                copy.className = 'quick-nav-result-copy';
                var label = document.createElement('strong');
                label.className = 'quick-nav-result-label';
                label.textContent = entry.label;
                copy.appendChild(label);
                if (entry.description) {
                    var description = document.createElement('span');
                    description.className = 'quick-nav-result-description';
                    description.textContent = entry.description;
                    copy.appendChild(description);
                }
                link.appendChild(copy);
                link.addEventListener('mouseenter', function() {
                    setActive(index);
                });
                link.addEventListener('focus', function() {
                    setActive(index);
                });
                option.appendChild(link);
                quickNavResults.appendChild(option);
            });

            quickNavEmpty.hidden = visibleEntries.length !== 0;
            if (!quickNavEmpty.hidden) {
                quickNavEmpty.textContent = queryLength(query) < 2
                        ? '2자 이상 입력하면 업무 데이터까지 검색합니다.'
                        : '일치하는 결과가 없습니다.';
            }
            setActive(visibleEntries.length ? 0 : -1);
        }

        function normalizeRemoteEntries(payload) {
            if (!payload || !Array.isArray(payload.results)) {
                return [];
            }
            return payload.results.filter(function(entry) {
                return entry
                        && typeof entry.category === 'string'
                        && typeof entry.label === 'string'
                        && typeof entry.url === 'string'
                        && entry.url.indexOf('/') === 0
                        && entry.url.indexOf('//') !== 0;
            }).map(function(entry) {
                return {
                    category: entry.category,
                    label: entry.label,
                    description: typeof entry.description === 'string'
                            ? entry.description
                            : '',
                    url: entry.url
                };
            });
        }

        function normalizeUnavailableCategories(payload) {
            if (!payload || !Array.isArray(payload.unavailableCategories)) {
                return [];
            }
            return payload.unavailableCategories.filter(function(category) {
                return typeof category === 'string' && category.trim();
            }).map(function(category) {
                return category.trim();
            });
        }

        function runRemoteSearch(query, version) {
            searchTimer = null;
            window.fetch(
                    searchEndpoint + '?q=' + encodeURIComponent(query),
                    {
                        credentials: 'same-origin',
                        headers: {'Accept': 'application/json'}
                    })
                    .then(function(response) {
                        if (!response.ok) {
                            var error = new Error('Search request failed');
                            error.status = response.status;
                            throw error;
                        }
                        return response.json();
                    })
                    .then(function(payload) {
                        if (version !== requestVersion
                                || quickNavInput.value.trim() !== query) {
                            return;
                        }
                        remoteEntries = normalizeRemoteEntries(payload);
                        var unavailableCategories =
                                normalizeUnavailableCategories(payload);
                        var unavailableSuffix = unavailableCategories.length
                                ? ' · ' + unavailableCategories.join(', ')
                                        + ' 제외'
                                : '';
                        setStatus(remoteEntries.length
                                ? '업무 데이터 검색 결과 '
                                        + remoteEntries.length + '건'
                                        + unavailableSuffix
                                : unavailableCategories.length
                                        ? unavailableCategories.join(', ')
                                                + ' 검색원을 제외하고 일치하는 결과가 없습니다.'
                                        : '업무 데이터에서 일치하는 결과가 없습니다.');
                        renderResults();
                    })
                    .catch(function(error) {
                        if (version !== requestVersion) {
                            return;
                        }
                        remoteEntries = [];
                        setStatus(error && error.status === 401
                                ? '로그인이 만료되었습니다. 새로고침 후 다시 시도해 주세요.'
                                : '업무 데이터 검색을 일시적으로 사용할 수 없습니다.');
                        renderResults();
                    });
        }

        function searchInputChanged() {
            cancelPendingSearch();
            remoteEntries = [];
            var query = quickNavInput.value.trim();
            if (queryLength(query) < 2) {
                setStatus(query
                        ? '2자 이상 입력하면 업무 데이터까지 검색합니다.'
                        : '');
                renderResults();
                return;
            }
            if (!searchEndpoint || typeof window.fetch !== 'function') {
                setStatus('이 브라우저에서는 메뉴 검색만 사용할 수 있습니다.');
                renderResults();
                return;
            }

            setStatus('업무 데이터를 검색하는 중입니다…');
            renderResults();
            var version = requestVersion;
            searchTimer = window.setTimeout(function() {
                runRemoteSearch(query, version);
            }, 180);
        }

        function openQuickNavigation(trigger) {
            if (dialogController.isOpen()) {
                quickNavInput.focus();
                return;
            }
            if (window.Frog2UI.hasOpenDialog()) {
                return;
            }
            var focusReturnTarget = trigger || quickNavOpenButton;
            if (isMobile() && mobileMenuIsOpen()) {
                setMobileMenu(false, {restoreFocus: false});
            }
            if (isMobile()) {
                focusReturnTarget = mobileToggle;
            }
            closeAllDropdowns();
            quickNavBackdrop.hidden = false;
            quickNavBackdrop.setAttribute('aria-hidden', 'false');
            quickNavOpenButton.setAttribute('aria-expanded', 'true');
            quickNavInput.value = '';
            quickNavInput.setAttribute('aria-expanded', 'true');
            cancelPendingSearch();
            remoteEntries = [];
            setStatus('');
            renderResults();
            dialogController.open(focusReturnTarget);
        }

        function closeQuickNavigation() {
            if (!dialogController.isOpen()) {
                return;
            }
            dialogController.close();
            quickNavBackdrop.hidden = true;
            quickNavBackdrop.setAttribute('aria-hidden', 'true');
            quickNavOpenButton.setAttribute('aria-expanded', 'false');
            quickNavInput.setAttribute('aria-expanded', 'false');
            quickNavInput.removeAttribute('aria-activedescendant');
            cancelPendingSearch();
            remoteEntries = [];
            setStatus('');
        }

        quickNavOpenButton.addEventListener('click', function() {
            openQuickNavigation(quickNavOpenButton);
        });
        quickNavCloseButton.addEventListener('click', closeQuickNavigation);
        quickNavBackdrop.addEventListener('click', function(event) {
            if (event.target === quickNavBackdrop) {
                closeQuickNavigation();
            }
        });
        quickNavDialog.addEventListener('keydown', function(event) {
            if (event.key === 'Escape') {
                event.preventDefault();
                event.stopPropagation();
                closeQuickNavigation();
            }
        });
        quickNavInput.addEventListener('input', searchInputChanged);
        quickNavInput.addEventListener('keydown', function(event) {
            if (event.key === 'ArrowDown') {
                event.preventDefault();
                setActive(activeIndex + 1);
            } else if (event.key === 'ArrowUp') {
                event.preventDefault();
                setActive(activeIndex - 1);
            } else if (event.key === 'Home') {
                event.preventDefault();
                setActive(0);
            } else if (event.key === 'End') {
                event.preventDefault();
                setActive(visibleEntries.length - 1);
            } else if (event.key === 'Enter' && activeIndex >= 0) {
                event.preventDefault();
                window.location.assign(visibleEntries[activeIndex].url);
            }
        });
        document.addEventListener('keydown', function(event) {
            var target = event.target;
            var isEditable = target && typeof target.matches === 'function'
                    && (target.matches('input, textarea, select')
                        || target.isContentEditable);
            if (isEditable || event.altKey || event.shiftKey
                    || !(event.ctrlKey || event.metaKey)
                    || event.key.toLocaleLowerCase('en-US') !== 'k') {
                return;
            }
            event.preventDefault();
            openQuickNavigation(quickNavOpenButton);
        });
    }

    mobileToggle.hidden = false;
    headerElement.classList.add('nav-enhanced');
    handleViewportChange();
    initializeQuickNavigation();

    mobileToggle.addEventListener('click', function() {
        var shouldOpen = !mobileMenuIsOpen();
        setMobileMenu(shouldOpen, {restoreFocus: !shouldOpen});
    });

    dropdownItems.forEach(function(item) {
        var toggle = dropdownToggle(item);
        var menu = item.querySelector('.dropdown-menu');

        toggle.addEventListener('click', function() {
            setDropdown(item, !item.classList.contains('open'));
        });

        toggle.addEventListener('keydown', function(event) {
            if (event.key === 'ArrowDown') {
                event.preventDefault();
                setDropdown(item, true);
                var firstLink = menu.querySelector('a[href]');
                if (firstLink) {
                    firstLink.focus();
                }
            }
        });

        item.addEventListener('mouseenter', function() {
            if (!isMobile()) {
                setDropdown(item, true);
            }
        });

        item.addEventListener('mouseleave', function() {
            if (!isMobile() && !item.contains(document.activeElement)) {
                setDropdown(item, false);
            }
        });

        item.addEventListener('focusin', function() {
            if (!isMobile() && !suppressDropdownFocusOpen) {
                setDropdown(item, true);
            }
        });

        item.addEventListener('focusout', function() {
            window.setTimeout(function() {
                if (!isMobile()
                        && !item.contains(document.activeElement)
                        && !item.matches(':hover')) {
                    setDropdown(item, false);
                }
            }, 0);
        });
    });

    primaryNavigation.querySelectorAll('a[href]').forEach(function(link) {
        link.addEventListener('click', function() {
            if (isMobile()) {
                setMobileMenu(false, {restoreFocus: false});
            } else {
                closeAllDropdowns();
            }
        });
    });

    document.addEventListener('click', function(event) {
        if (headerElement.contains(event.target)) {
            return;
        }

        var mobile = isMobile();
        var restoreMobileFocus = mobile
                && mobileMenuIsOpen()
                && primaryNavigation.contains(document.activeElement);
        var restoreDesktopDropdownFocus = mobile
                ? null : focusedDropdownMenu();
        closeAllDropdowns();
        if (mobile && mobileMenuIsOpen()) {
            setMobileMenu(false, {restoreFocus: restoreMobileFocus});
        } else if (restoreDesktopDropdownFocus) {
            focusDropdownToggle(restoreDesktopDropdownFocus);
        }
    });

    document.addEventListener('keydown', function(event) {
        if (event.key !== 'Escape') {
            return;
        }

        var openDropdown = dropdownItems.find(function(item) {
            return item.classList.contains('open');
        });
        if (openDropdown) {
            event.preventDefault();
            setDropdown(openDropdown, false);
            focusDropdownToggle(openDropdown);
            return;
        }

        if (isMobile() && mobileMenuIsOpen()) {
            event.preventDefault();
            setMobileMenu(false, {restoreFocus: true});
        }
    });

    if (typeof mobileMedia.addEventListener === 'function') {
        mobileMedia.addEventListener('change', handleViewportChange);
    } else {
        mobileMedia.addListener(handleViewportChange);
    }
});
