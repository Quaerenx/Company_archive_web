(function () {
    'use strict';

    var MAX_TOASTS_PER_REGION = 3;
    var submittingForms = new WeakSet();
    var originalButtonState = new WeakMap();
    var successfulButtonState = new WeakMap();
    var dirtyGuardControllers = new WeakMap();
    var dirtyGuardForms = [];
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

    function restoreButtonSuccess(button) {
        var state = successfulButtonState.get(button);
        if (!state) {
            return;
        }
        window.clearTimeout(state.timer);
        button.innerHTML = state.markup;
        button.disabled = state.disabled;
        if (state.ariaDisabled === null) {
            button.removeAttribute('aria-disabled');
        } else {
            button.setAttribute('aria-disabled', state.ariaDisabled);
        }
        button.classList.remove('is-success');
        successfulButtonState.delete(button);
    }

    function setButtonSuccess(button, label, duration) {
        if (!button) {
            return;
        }
        setButtonLoading(button, false);
        restoreButtonSuccess(button);

        var state = {
            markup: button.innerHTML,
            disabled: button.disabled,
            ariaDisabled: button.getAttribute('aria-disabled'),
            timer: 0
        };
        successfulButtonState.set(button, state);
        button.classList.add('is-success');
        button.disabled = true;
        button.setAttribute('aria-disabled', 'true');
        button.textContent = label || '완료';
        announce((label || '작업') + ' 완료');
        state.timer = window.setTimeout(function () {
            restoreButtonSuccess(button);
        }, Number.isFinite(duration) ? Math.max(0, duration) : 1200);
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
        content.className = 'ui-toast__message';
        if (resolvedTone === 'success') {
            var icon = document.createElement('span');
            icon.className = 'ui-toast__icon';
            icon.setAttribute('aria-hidden', 'true');
            icon.textContent = '\u2713';
            content.appendChild(icon);
        }
        var messageText = document.createElement('span');
        messageText.textContent = message;
        content.appendChild(messageText);
        var close = document.createElement('button');
        close.type = 'button';
        close.className = 'ui-toast__close';
        close.setAttribute('aria-label', '알림 닫기');
        close.textContent = '×';
        close.addEventListener('click', function () {
            dismissToast(toast);
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
                dismissToast(toast);
            }, 5000);
        }
        return toast;
    }

    function dismissToast(toast) {
        if (!toast || toast.classList.contains('is-leaving')) {
            return;
        }
        toast.classList.add('is-leaving');
        toast.setAttribute('aria-hidden', 'true');
        var remove = function () {
            if (toast.isConnected) {
                toast.remove();
            }
        };
        toast.addEventListener('animationend', remove, { once: true });
        window.setTimeout(remove, 240);
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

    function serializeFormState(form) {
        var controls = form.elements
            ? Array.prototype.slice.call(form.elements)
            : Array.prototype.slice.call(
                form.querySelectorAll('input, select, textarea'));
        return JSON.stringify(controls.filter(function (control) {
            return control.name && !control.disabled
                && !['button', 'reset', 'submit'].includes(
                    (control.type || '').toLocaleLowerCase('en-US'));
        }).map(function (control) {
            var type = (control.type || '').toLocaleLowerCase('en-US');
            var value;
            if (type === 'checkbox' || type === 'radio') {
                value = control.checked ? control.value : null;
            } else if (type === 'file') {
                value = Array.prototype.map.call(control.files || [], function (file) {
                    return [file.name, file.size, file.lastModified];
                });
            } else if (control.multiple && control.options) {
                value = Array.prototype.filter.call(
                    control.options, function (option) {
                        return option.selected;
                    }).map(function (option) {
                        return option.value;
                    });
            } else {
                value = control.value;
            }
            return [control.name, type, value];
        }));
    }

    function createDirtyGuard(form) {
        if (!form) {
            throw new TypeError('Form element is required.');
        }
        if (dirtyGuardControllers.has(form)) {
            return dirtyGuardControllers.get(form);
        }

        var baseline = serializeFormState(form);
        var submitting = false;

        function isDirty() {
            return !submitting && serializeFormState(form) !== baseline;
        }

        function markSubmitting() {
            submitting = true;
        }

        function resume() {
            submitting = false;
        }

        function resetBaseline() {
            baseline = serializeFormState(form);
            submitting = false;
        }

        form.addEventListener('reset', function () {
            window.requestAnimationFrame(resetBaseline);
        });

        var controller = Object.freeze({
            isDirty: isDirty,
            markSubmitting: markSubmitting,
            resetBaseline: resetBaseline,
            resume: resume
        });
        dirtyGuardControllers.set(form, controller);
        dirtyGuardForms.push(form);
        return controller;
    }

    function initializeDirtyGuards() {
        document.querySelectorAll(
            'form[data-ui-dirty-guard="auto"]'
        ).forEach(createDirtyGuard);
    }

    function scheduleDirtyGuardInitialization() {
        // Page scripts populate local date/year defaults during DOMContentLoaded.
        // Capture the baseline on the next frame so those defaults do not look dirty.
        window.requestAnimationFrame(initializeDirtyGuards);
    }

    function hasOpenDialog() {
        return openDialogCount > 0;
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
            dialog.removeAttribute('inert');
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
            dialog.setAttribute('inert', '');
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
        document.addEventListener(
            'DOMContentLoaded', scheduleDirtyGuardInitialization, { once: true });
    } else {
        initializeScrollableTableRegions();
        scheduleDirtyGuardInitialization();
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

    var disclosureTransitions = new WeakMap();

    function prefersReducedDisclosureMotion() {
        return typeof window.matchMedia === 'function'
            && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    }

    function clearDisclosureTransition(detail) {
        var state = disclosureTransitions.get(detail);
        if (!state) {
            return;
        }
        if (state.content && typeof state.content.removeEventListener === 'function') {
            state.content.removeEventListener('transitionend', state.handleTransitionEnd);
        }
        window.clearTimeout(state.fallbackTimer);
        disclosureTransitions.delete(detail);
    }

    function finishDisclosureImmediately(detail, content, expanded) {
        if (content) {
            content.classList.toggle('is-disclosure-expanded', expanded);
        }
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
        clearDisclosureTransition(detail);

        if (!content || prefersReducedDisclosureMotion()) {
            finishDisclosureImmediately(detail, content, expanded);
            return;
        }

        var wasHidden = detail.hidden;
        detail.hidden = false;
        if (expanded) {
            detail.removeAttribute('aria-hidden');
            detail.removeAttribute('inert');
        } else {
            detail.setAttribute('aria-hidden', 'true');
            detail.setAttribute('inert', '');
        }

        if (expanded && wasHidden && typeof content.getBoundingClientRect === 'function') {
            // Flush the collapsed frame after unhiding the table row so the
            // browser interpolates from 0fr instead of painting it open.
            content.getBoundingClientRect();
        }
        content.classList.toggle('is-disclosure-expanded', expanded);

        var state = {
            content: content,
            fallbackTimer: 0,
            handleTransitionEnd: null
        };
        var finish = function () {
            if (disclosureTransitions.get(detail) !== state) {
                return;
            }
            clearDisclosureTransition(detail);
            var stillExpanded = toggle.getAttribute('aria-expanded') === 'true';
            finishDisclosureImmediately(detail, content, stillExpanded);
        };
        state.handleTransitionEnd = function (event) {
            if (event.target === content
                    && event.propertyName === 'grid-template-rows') {
                finish();
            }
        };
        if (typeof content.addEventListener === 'function') {
            content.addEventListener('transitionend', state.handleTransitionEnd);
        }
        disclosureTransitions.set(detail, state);
        state.fallbackTimer = window.setTimeout(finish, 260);
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
        var guardedForm = event.target.closest(
            'form[data-ui-dirty-guard="auto"]');
        if (guardedForm && !event.defaultPrevented
                && (typeof guardedForm.checkValidity !== 'function'
                    || guardedForm.checkValidity())) {
            createDirtyGuard(guardedForm).markSubmitting();
        }

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
        dirtyGuardForms.forEach(function (form) {
            dirtyGuardControllers.get(form).resume();
        });
    });

    window.addEventListener('beforeunload', function (event) {
        var shouldWarn = dirtyGuardForms.some(function (form) {
            return form.isConnected !== false
                && dirtyGuardControllers.get(form).isDirty();
        });
        if (!shouldWarn) {
            return;
        }
        event.preventDefault();
        event.returnValue = '';
        return '';
    });

    window.Frog2UI = Object.freeze({
        announce: announce,
        clearFieldError: clearFieldError,
        confirmAction: confirmAction,
        createDirtyGuard: createDirtyGuard,
        createDialogController: createDialogController,
        hasOpenDialog: hasOpenDialog,
        notify: notify,
        setButtonLoading: setButtonLoading,
        setButtonSuccess: setButtonSuccess,
        setStatus: setStatus,
        showFieldError: showFieldError,
        unlockForm: unlockForm
    });
}());
