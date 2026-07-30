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

        if (isMobile() && !open) {
            primaryNavigation.setAttribute('aria-hidden', 'true');
            closeAllDropdowns();
        } else {
            primaryNavigation.removeAttribute('aria-hidden');
        }

        if (!open && restoreFocus) {
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

    mobileToggle.hidden = false;
    headerElement.classList.add('nav-enhanced');
    handleViewportChange();

    mobileToggle.addEventListener('click', function() {
        setMobileMenu(!mobileMenuIsOpen());
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
