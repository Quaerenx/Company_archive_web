(function () {
    'use strict';

    var MAX_TOASTS_PER_REGION = 3;
    var submittingForms = new WeakSet();
    var originalButtonState = new WeakMap();
    var generatedId = 0;
    var openDialogCount = 0;
    var tableHeaderResizeObserver = null;

    var TABLE_STICKY_OFFSET_PROPERTY = '--table-sticky-offset';
    var TABLE_STICKY_READY_CLASS = 'is-table-sticky-ready';

    var FOCUSABLE_SELECTOR = [
        'a[href]',
        'area[href]',
        'button:not([disabled])',
        'input:not([disabled]):not([type="hidden"])',
        'select:not([disabled])',
        'textarea:not([disabled])',
        'iframe',
        '[contenteditable="true"]',
        '[tabindex]:not([tabindex="-1"])'
    ].join(',');
    var INTERACTIVE_TARGET_SELECTOR = [
        'a',
        'button',
        'input',
        'select',
        'textarea',
        'summary',
        '[contenteditable="true"]',
        '[role="button"]',
        '[role="link"]'
    ].join(',');

    function statusRegion() {
        return document.getElementById('ui-status-region');
    }

    function toastRegion(tone) {
        return document.getElementById(
            tone === 'danger' ? 'ui-toast-region-assertive' : 'ui-toast-region-polite');
    }

    function setButtonLoading(button, loading, busyLabel) {
        if (!button) {
            return;
        }
        if (loading) {
            if (!originalButtonState.has(button)) {
                originalButtonState.set(button, {
                    markup: button.innerHTML,
                    disabled: button.disabled,
                    ariaDisabled: button.getAttribute('aria-disabled'),
                    ariaBusy: button.getAttribute('aria-busy')
                });
            }
            button.classList.add('is-loading');
            button.setAttribute('aria-disabled', 'true');
            button.setAttribute('aria-busy', 'true');
            if (!button.name) {
                button.disabled = true;
            }
            var label = busyLabel || button.getAttribute('data-busy-label');
            if (label) {
                button.textContent = label;
            }
            return;
        }

        var original = originalButtonState.get(button);
        if (original) {
            button.innerHTML = original.markup;
            button.disabled = original.disabled;
            if (original.ariaDisabled === null) {
                button.removeAttribute('aria-disabled');
            } else {
                button.setAttribute('aria-disabled', original.ariaDisabled);
            }
            if (original.ariaBusy === null) {
                button.removeAttribute('aria-busy');
            } else {
                button.setAttribute('aria-busy', original.ariaBusy);
            }
            originalButtonState.delete(button);
        }
        button.classList.remove('is-loading');
    }

    function unlockForm(form) {
        submittingForms.delete(form);
        form.removeAttribute('aria-busy');
        form.querySelectorAll('.ui-button.is-loading').forEach(function (button) {
            setButtonLoading(button, false);
        });
    }

    function clearFieldError(field) {
        if (!field) {
            return;
        }
        var errorId = field.getAttribute('data-ui-error-id');
        if (errorId) {
            var error = document.getElementById(errorId);
            if (error) {
                error.remove();
            }
            field.removeAttribute('data-ui-error-id');
        }
        field.removeAttribute('aria-invalid');
        var describedBy = (field.getAttribute('aria-describedby') || '')
            .split(/\s+/)
            .filter(function (id) {
                return id && id !== errorId;
            });
        if (describedBy.length) {
            field.setAttribute('aria-describedby', describedBy.join(' '));
        } else {
            field.removeAttribute('aria-describedby');
        }
    }

    function showFieldError(field, message) {
        if (!field) {
            notify(message, 'danger', { persistent: true });
            return;
        }

        clearFieldError(field);
        if (!field.id) {
            generatedId += 1;
            field.id = 'ui-field-' + generatedId;
        }
        var errorId = field.id + '-error';
        var error = document.createElement('span');
        error.id = errorId;
        error.className = 'ui-field-error';
        error.setAttribute('role', 'alert');
        error.textContent = message;
        field.insertAdjacentElement('afterend', error);
        field.setAttribute('aria-invalid', 'true');
        field.setAttribute('data-ui-error-id', errorId);

        var describedBy = (field.getAttribute('aria-describedby') || '')
            .split(/\s+/)
            .filter(Boolean);
        if (describedBy.indexOf(errorId) < 0) {
            describedBy.push(errorId);
        }
        field.setAttribute('aria-describedby', describedBy.join(' '));
        field.focus();
    }

    function announce(message) {
        var region = statusRegion();
        if (!region) {
            return;
        }
        region.textContent = '';
        window.requestAnimationFrame(function () {
            region.textContent = message;
        });
    }

    function notify(message, tone, options) {
        var resolvedTone = tone || 'info';
        var region = toastRegion(resolvedTone);
        if (!region || !message) {
            announce(message);
            return null;
        }

        var toast = document.createElement('div');
        toast.className = 'ui-toast';
        toast.dataset.tone = resolvedTone;

        var content = document.createElement('span');
        content.textContent = message;
        var close = document.createElement('button');
        close.type = 'button';
        close.className = 'ui-toast__close';
        close.setAttribute('aria-label', '알림 닫기');
        close.textContent = '×';
        close.addEventListener('click', function () {
            toast.remove();
        });

        toast.appendChild(content);
        toast.appendChild(close);
        while (region.childElementCount >= MAX_TOASTS_PER_REGION) {
            region.firstElementChild.remove();
        }
        region.appendChild(toast);

        var persistent = options && options.persistent;
        if (!persistent && resolvedTone === 'success') {
            window.setTimeout(function () {
                toast.remove();
            }, 5000);
        }
        return toast;
    }

    function setStatus(element, message, tone) {
        if (!element) {
            announce(message);
            return;
        }
        element.textContent = message || '';
        element.classList.add('ui-status');
        [
            'ui-status--success',
            'ui-status--danger',
            'ui-status--warning',
            'ui-status--info',
            'ui-status--neutral'
        ].forEach(function (className) {
            element.classList.remove(className);
        });
        element.classList.add('ui-status--' + (tone || 'neutral'));
    }

    function confirmAction(message) {
        if (typeof message !== 'string' || !message.trim()) {
            throw new TypeError('Confirmation message is required.');
        }
        return window.confirm(message);
    }

    function isVisible(element) {
        return element.getClientRects().length > 0
            && window.getComputedStyle(element).visibility !== 'hidden'
            && element.getAttribute('aria-hidden') !== 'true';
    }

    function focusableElements(dialog) {
        return Array.prototype.filter.call(
            dialog.querySelectorAll(FOCUSABLE_SELECTOR),
            isVisible
        );
    }

    function createDialogController(dialog) {
        if (!dialog) {
            throw new TypeError('Dialog element is required.');
        }

        var opener = null;
        var opened = false;

        function focusInitialElement() {
            var initial = dialog.querySelector('[data-dialog-initial-focus]');
            if (!initial || !isVisible(initial)) {
                initial = focusableElements(dialog)[0] || dialog;
            }
            initial.focus();
        }

        function handleKeydown(event) {
            if (!opened) {
                return;
            }
            if (event.key === 'Escape') {
                event.preventDefault();
                event.stopPropagation();
                close();
                return;
            }
            if (event.key !== 'Tab') {
                return;
            }

            var focusable = focusableElements(dialog);
            if (!focusable.length) {
                event.preventDefault();
                dialog.focus();
                return;
            }

            var first = focusable[0];
            var last = focusable[focusable.length - 1];
            if (event.shiftKey && (document.activeElement === first
                    || !dialog.contains(document.activeElement))) {
                event.preventDefault();
                last.focus();
            } else if (!event.shiftKey && (document.activeElement === last
                    || !dialog.contains(document.activeElement))) {
                event.preventDefault();
                first.focus();
            }
        }

        function open(trigger) {
            if (opened) {
                return;
            }
            opener = trigger && typeof trigger.focus === 'function'
                ? trigger
                : document.activeElement;
            opened = true;
            openDialogCount += 1;
            dialog.classList.add('show');
            dialog.setAttribute('aria-hidden', 'false');
            document.body.classList.add('ui-dialog-open');
            document.addEventListener('keydown', handleKeydown);
            window.requestAnimationFrame(focusInitialElement);
        }

        function close() {
            if (!opened) {
                return;
            }
            opened = false;
            openDialogCount = Math.max(0, openDialogCount - 1);
            dialog.classList.remove('show');
            dialog.setAttribute('aria-hidden', 'true');
            document.removeEventListener('keydown', handleKeydown);
            if (openDialogCount === 0) {
                document.body.classList.remove('ui-dialog-open');
            }

            var focusTarget = opener;
            opener = null;
            if (focusTarget && focusTarget.isConnected
                    && typeof focusTarget.focus === 'function') {
                window.requestAnimationFrame(function () {
                    focusTarget.focus();
                });
            }
        }

        function isOpen() {
            return opened;
        }

        return Object.freeze({
            close: close,
            isOpen: isOpen,
            open: open
        });
    }

    function updateTableStickyOffset() {
        var header = document.querySelector('.main-header');
        var root = document.documentElement;
        if (!header || !root || !root.style) {
            return false;
        }

        var headerBounds = header.getBoundingClientRect();
        var headerBottom = Number.isFinite(headerBounds.bottom)
            ? Math.max(0, Math.ceil(headerBounds.bottom))
            : 0;
        root.style.setProperty(TABLE_STICKY_OFFSET_PROPERTY, headerBottom + 'px');
        return headerBottom > 0;
    }

    function updateScrollableTableRegions() {
        var stickyOffsetReady = updateTableStickyOffset();
        document.querySelectorAll(
            '.' + TABLE_STICKY_READY_CLASS
        ).forEach(function (surface) {
            surface.classList.remove(TABLE_STICKY_READY_CLASS);
        });

        document.querySelectorAll('[data-ui-scroll-region]').forEach(function (region) {
            var scrollable = region.scrollWidth > region.clientWidth + 1;
            region.dataset.uiScrollable = String(scrollable);
            updateScrollableRegionHint(region, scrollable);
            var workSurface = region.closest(
                '.ui-work-surface, [data-ui-table-surface]'
            );
            if (workSurface && stickyOffsetReady && !scrollable) {
                workSurface.classList.add(TABLE_STICKY_READY_CLASS);
            }
            if (scrollable) {
                region.setAttribute('tabindex', '0');
                region.setAttribute('role', 'region');
                var label = region.dataset.uiScrollLabel;
                if (label) {
                    region.setAttribute('aria-label', label);
                }
                return;
            }
            region.removeAttribute('tabindex');
            region.removeAttribute('role');
            region.removeAttribute('aria-label');
        });
    }

    function updateScrollableRegionHint(region, scrollable) {
        var hintId = region.dataset.uiScrollHintId;
        if (!hintId) {
            return;
        }

        var hint = document.getElementById(hintId);
        if (!hint) {
            return;
        }
        hint.hidden = !scrollable;

        var describedBy = (region.getAttribute('aria-describedby') || '')
            .split(/\s+/)
            .filter(Boolean)
            .filter(function (id) {
                return id !== hintId;
            });
        if (scrollable) {
            describedBy.push(hintId);
        }
        if (describedBy.length) {
            region.setAttribute('aria-describedby', describedBy.join(' '));
        } else {
            region.removeAttribute('aria-describedby');
        }
    }

    function observeTableHeaderSize() {
        if (tableHeaderResizeObserver || typeof window.ResizeObserver !== 'function') {
            return;
        }
        var header = document.querySelector('.main-header');
        if (!header) {
            return;
        }
        tableHeaderResizeObserver = new window.ResizeObserver(
            scheduleScrollableTableRegionUpdate
        );
        tableHeaderResizeObserver.observe(header);
    }

    function initializeScrollableTableRegions() {
        updateScrollableTableRegions();
        observeTableHeaderSize();
    }

    var scrollRegionFrame = 0;
    function scheduleScrollableTableRegionUpdate() {
        if (scrollRegionFrame) {
            return;
        }
        scrollRegionFrame = window.requestAnimationFrame(function () {
            scrollRegionFrame = 0;
            updateScrollableTableRegions();
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener(
            'DOMContentLoaded', initializeScrollableTableRegions, { once: true });
    } else {
        initializeScrollableTableRegions();
    }
    window.addEventListener('load', initializeScrollableTableRegions, { once: true });
    window.addEventListener('resize', scheduleScrollableTableRegionUpdate);

    function hasSelectedTextWithin(element) {
        var selection = window.getSelection();
        if (!element || !selection || selection.isCollapsed
                || selection.toString().trim().length === 0) {
            return false;
        }
        if (typeof selection.containsNode === 'function') {
            try {
                return selection.containsNode(element, true);
            } catch (error) {
                // Fall through for detached nodes and older selection APIs.
            }
        }
        return Boolean(
            (selection.anchorNode && element.contains(selection.anchorNode))
            || (selection.focusNode && element.contains(selection.focusNode))
        );
    }

    var disclosureAnimations = new WeakMap();

    function prefersReducedDisclosureMotion() {
        return typeof window.matchMedia === 'function'
            && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    }

    function finishDisclosureImmediately(detail, expanded) {
        detail.hidden = !expanded;
        if (expanded) {
            detail.removeAttribute('aria-hidden');
            detail.removeAttribute('inert');
        } else {
            detail.setAttribute('aria-hidden', 'true');
            detail.setAttribute('inert', '');
        }
    }

    function animateDisclosure(detail, toggle, expanded) {
        var content = detail.querySelector('[data-ui-disclosure-content]');
        var runningAnimation = disclosureAnimations.get(detail);
        var interruptedHeight = null;
        if (runningAnimation) {
            if (content && typeof content.getBoundingClientRect === 'function') {
                var interruptedRect = content.getBoundingClientRect();
                if (interruptedRect && Number.isFinite(interruptedRect.height)) {
                    interruptedHeight = Math.max(0, interruptedRect.height);
                }
            }
            runningAnimation.cancel();
            disclosureAnimations.delete(detail);
        }

        if (!content || typeof content.animate !== 'function'
                || prefersReducedDisclosureMotion()) {
            finishDisclosureImmediately(detail, expanded);
            return;
        }

        detail.hidden = false;
        if (expanded) {
            detail.removeAttribute('aria-hidden');
            detail.removeAttribute('inert');
        } else {
            detail.setAttribute('aria-hidden', 'true');
            detail.setAttribute('inert', '');
        }

        var contentHeight = content.scrollHeight;
        if (!Number.isFinite(contentHeight) || contentHeight <= 0) {
            finishDisclosureImmediately(detail, expanded);
            return;
        }

        var targetHeight = expanded ? contentHeight : 0;
        var startHeight = interruptedHeight === null
            ? (expanded ? 0 : contentHeight)
            : Math.min(interruptedHeight, contentHeight);
        var startOpacity = contentHeight > 0 ? startHeight / contentHeight : 0;
        var startFrame = {
            height: startHeight + 'px',
            opacity: startOpacity,
            overflow: 'hidden'
        };
        var endFrame = {
            height: targetHeight + 'px',
            opacity: expanded ? 1 : 0,
            overflow: 'hidden'
        };
        var animation;
        try {
            animation = content.animate(
                [startFrame, endFrame],
                {
                    duration: 180,
                    easing: 'cubic-bezier(0.42, 0, 0.58, 1)',
                    fill: 'both'
                }
            );
        } catch (error) {
            finishDisclosureImmediately(detail, expanded);
            return;
        }

        disclosureAnimations.set(detail, animation);
        animation.onfinish = function () {
            if (disclosureAnimations.get(detail) !== animation) {
                return;
            }
            disclosureAnimations.delete(detail);
            var stillExpanded = toggle.getAttribute('aria-expanded') === 'true';
            finishDisclosureImmediately(detail, stillExpanded);
            animation.cancel();
        };
        animation.oncancel = function () {
            if (disclosureAnimations.get(detail) === animation) {
                disclosureAnimations.delete(detail);
            }
        };
    }

    function toggleDisclosure(toggle) {
        var detailId = toggle.getAttribute('aria-controls');
        var detail = detailId ? document.getElementById(detailId) : null;
        if (!detail) {
            return false;
        }

        var expanded = toggle.getAttribute('aria-expanded') !== 'true';
        toggle.setAttribute('aria-expanded', String(expanded));
        animateDisclosure(detail, toggle, expanded);

        var row = toggle.closest('[data-ui-disclosure-row]');
        if (row) {
            row.classList.toggle('is-expanded', expanded);
        }
        return true;
    }

    function handleDisclosureClick(event) {
        var directToggle = event.target.closest('[data-ui-disclosure-toggle]');
        var row = event.target.closest('[data-ui-disclosure-row]');
        if (!directToggle && !row) {
            return false;
        }

        if (event.defaultPrevented || event.button !== 0
                || event.ctrlKey || event.metaKey || event.shiftKey
                || event.altKey || hasSelectedTextWithin(row || directToggle)) {
            return true;
        }

        if (!directToggle && event.target.closest(INTERACTIVE_TARGET_SELECTOR)) {
            return true;
        }
        var toggle = directToggle
            || row.querySelector('[data-ui-disclosure-toggle]');
        if (toggle) {
            toggleDisclosure(toggle);
        }
        return true;
    }

    document.addEventListener('click', function (event) {
        if (!(event.target instanceof Element)) {
            return;
        }

        if (handleDisclosureClick(event)) {
            return;
        }

        var row = event.target.closest('.ui-data-row[data-detail-url]');
        var interactiveTarget = event.target.closest(INTERACTIVE_TARGET_SELECTOR);
        var isSelectingText = hasSelectedTextWithin(row);

        if (!row || interactiveTarget || event.defaultPrevented
                || event.button !== 0 || event.ctrlKey || event.metaKey
                || event.shiftKey || event.altKey || isSelectingText
                || row.getAttribute('aria-disabled') === 'true') {
            return;
        }

        window.location.assign(row.dataset.detailUrl);
    });

    document.addEventListener('input', function (event) {
        if (event.target.matches('[data-ui-error-id]')) {
            clearFieldError(event.target);
        }
    });

    document.addEventListener('change', function (event) {
        if (event.target.matches('[data-ui-error-id]')) {
            clearFieldError(event.target);
        }
    });

    document.addEventListener('submit', function (event) {
        var form = event.target.closest('form.ui-form[data-ui-submit-lock="auto"]');
        if (!form || event.defaultPrevented) {
            return;
        }
        if (submittingForms.has(form)) {
            event.preventDefault();
            return;
        }
        if (typeof form.checkValidity === 'function' && !form.checkValidity()) {
            return;
        }

        submittingForms.add(form);
        form.setAttribute('aria-busy', 'true');
        var submitter = event.submitter
            || form.querySelector('button[type="submit"], input[type="submit"]');
        setButtonLoading(submitter, true);
    });

    window.addEventListener('pageshow', function () {
        document.querySelectorAll('form.ui-form[aria-busy="true"]').forEach(unlockForm);
    });

    window.Frog2UI = Object.freeze({
        announce: announce,
        clearFieldError: clearFieldError,
        confirmAction: confirmAction,
        createDialogController: createDialogController,
        notify: notify,
        setButtonLoading: setButtonLoading,
        setStatus: setStatus,
        showFieldError: showFieldError,
        unlockForm: unlockForm
    });
}());
