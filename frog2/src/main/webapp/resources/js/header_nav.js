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
                || !quickNavEmpty || !window.Frog2UI) {
            return;
        }

        var dialogController = window.Frog2UI.createDialogController(quickNavDialog);
        var entries = [];
        var seen = new Set();
        var visibleEntries = [];
        var activeIndex = -1;

        primaryNavigation.querySelectorAll('a[href]:not(#logoutLink)').forEach(function(link) {
            var label = link.textContent.replace(/\s+/g, ' ').trim();
            var url = link.href;
            var key = label + '\n' + url;
            if (!label || !url || seen.has(key)) {
                return;
            }
            seen.add(key);
            entries.push({
                label: label,
                normalizedLabel: label.toLocaleLowerCase('ko-KR'),
                url: url
            });
        });

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
            visibleEntries = entries.filter(function(entry) {
                return !query || entry.normalizedLabel.indexOf(query) >= 0;
            });
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
                link.textContent = entry.label;
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
            setActive(visibleEntries.length ? 0 : -1);
        }

        function openQuickNavigation(trigger) {
            if (dialogController.isOpen()) {
                quickNavInput.focus();
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
        quickNavInput.addEventListener('input', renderResults);
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
