(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
        var root = document.querySelector('.customer-detail--edit');
        if (!root) return;

        var forms = Array.prototype.slice.call(
            root.querySelectorAll('[data-customer-detail-form]'));
        var tabs = Array.prototype.slice.call(root.querySelectorAll('.tab-btn'));
        var panels = Array.prototype.slice.call(root.querySelectorAll('.tab-panel'));
        var tabNavigation = root.querySelector('.tab-nav');
        var tabIndicator = tabNavigation
            ? tabNavigation.querySelector('.tab-indicator')
            : null;
        var environmentLinks = Array.prototype.slice.call(
            root.querySelectorAll('[data-customer-environment-link]'));
        var dirtyForms = new Set();
        var conditionalActiveValues = new WeakMap();
        var isSubmitting = false;

        function runAfterLayout(callback) {
            if (typeof window.requestAnimationFrame === 'function') {
                window.requestAnimationFrame(callback);
                return;
            }
            callback();
        }

        function syncTabIndicator() {
            var activeTab = tabs.find(function(tab) {
                return tab.classList.contains('active');
            });
            if (!tabNavigation || !tabIndicator || !activeTab) return;

            tabNavigation.classList.add('is-enhanced');
            tabIndicator.style.inlineSize = activeTab.offsetWidth + 'px';
            tabIndicator.style.transform =
                'translateX(' + activeTab.offsetLeft + 'px)';
        }

        function environmentForTab(tab) {
            return (tab.getAttribute('data-environment') || 'prod')
                .trim()
                .toLowerCase();
        }

        function targetForEnvironment(environment) {
            return 'env-' + environment + '-edit';
        }

        function syncEnvironmentLinks(environment) {
            environmentLinks.forEach(function(link) {
                var destination = new URL(link.href, window.location.href);
                destination.searchParams.set('env', environment);
                link.href = destination.toString();
            });
        }

        function syncEnvironmentUrl(environment, replace) {
            if (!window.history || typeof window.history.pushState !== 'function') {
                return;
            }
            var url = new URL(window.location.href);
            if (url.searchParams.get('env') === environment) return;

            url.searchParams.set('env', environment);
            var method = replace ? 'replaceState' : 'pushState';
            window.history[method](
                {customerEnvironment: environment},
                '',
                url.pathname + url.search + url.hash);
        }

        function setActiveEnvironment(environment, replaceHistory) {
            var targetId = targetForEnvironment(environment);
            var hasTarget = panels.some(function(panel) {
                return panel.id === targetId;
            });
            if (!hasTarget) {
                environment = 'prod';
                targetId = targetForEnvironment(environment);
            }

            panels.forEach(function(panel) {
                var active = panel.id === targetId;
                panel.classList.toggle('active', active);
                panel.hidden = !active;
            });
            tabs.forEach(function(tab) {
                var active = environmentForTab(tab) === environment;
                tab.classList.toggle('active', active);
                tab.setAttribute('aria-selected', String(active));
                tab.tabIndex = active ? 0 : -1;
            });
            root.setAttribute('data-current-environment', environment);
            syncEnvironmentLinks(environment);
            syncEnvironmentUrl(environment, replaceHistory);
            runAfterLayout(syncTabIndicator);
        }

        function environmentFromLocation() {
            var params = new URLSearchParams(window.location.search);
            var requested = (params.get('env') || '').trim().toLowerCase();
            return requested === 'stg' || requested === 'dev'
                ? requested
                : 'prod';
        }

        function fieldValue(field) {
            var control = field.querySelector('input, select, textarea');
            return control
                && !control.disabled
                && String(control.value || '').trim() !== '';
        }

        function namedControl(form, name) {
            return form.querySelector(
                '.customer-detail-edit-control[name="' + name + '"]');
        }

        function conditionalMirror(form, name) {
            return form.querySelector(
                'input[type="hidden"][data-conditional-field-mirror="'
                    + name + '"]');
        }

        function ensureConditionalMirror(form, control, name) {
            var mirror = conditionalMirror(form, name);
            if (mirror) return mirror;

            mirror = document.createElement('input');
            mirror.type = 'hidden';
            mirror.name = control.name;
            mirror.disabled = true;
            mirror.setAttribute('data-conditional-field-mirror', name);
            form.appendChild(mirror);
            return mirror;
        }

        function setConditionalControlEnabled(
                form, name, enabled, disabledValue) {
            var control = namedControl(form, name);
            if (!control) return;

            var mirror = ensureConditionalMirror(form, control, name);
            var field = typeof control.closest === 'function'
                ? control.closest('[data-customer-detail-field]')
                : null;
            if (enabled) {
                if (conditionalActiveValues.has(control)) {
                    control.value = conditionalActiveValues.get(control);
                    conditionalActiveValues.delete(control);
                } else if (String(control.value || '') === disabledValue) {
                    control.value = '';
                }
                control.disabled = false;
                mirror.disabled = true;
            } else {
                var currentValue = String(control.value || '');
                if (!control.disabled
                        && currentValue !== ''
                        && currentValue !== disabledValue) {
                    conditionalActiveValues.set(control, currentValue);
                }
                control.value = disabledValue;
                mirror.value = disabledValue;
                mirror.disabled = false;
                control.disabled = true;
            }
            if (field) {
                field.classList.toggle('is-conditionally-disabled', !enabled);
            }
        }

        function normalizedControlValue(control) {
            return control
                ? String(control.value || '').trim().toUpperCase()
                : '';
        }

        function syncConditionalFields(form) {
            var eonEnabled = normalizedControlValue(
                namedControl(form, 'dbMode')) === 'EON';
            setConditionalControlEnabled(
                form, 'depotArea', eonEnabled, '미사용');
            setConditionalControlEnabled(
                form, 'objectArea', eonEnabled, '미사용');
            setConditionalControlEnabled(
                form, 'storageNetwork', eonEnabled, '미사용');

            var mcEnabled = normalizedControlValue(
                namedControl(form, 'mcYn')) === 'Y';
            ['mcHost', 'mcVersion', 'mcAdmin'].forEach(function(name) {
                setConditionalControlEnabled(form, name, mcEnabled, '미사용');
            });
        }

        function initializeConditionalFields(form) {
            ['dbMode', 'mcYn'].forEach(function(name) {
                var trigger = namedControl(form, name);
                if (!trigger) return;
                trigger.addEventListener('change', function() {
                    syncConditionalFields(form);
                    updateSectionCounts(form);
                });
            });
            syncConditionalFields(form);
        }

        function updateSectionCounts(form) {
            form.querySelectorAll('[data-detail-section]').forEach(function(section) {
                var fields = Array.prototype.slice.call(
                    section.querySelectorAll('[data-customer-detail-field]'));
                var count = section.querySelector('[data-detail-section-count]');
                if (!count || fields.length === 0) return;

                var filled = fields.filter(fieldValue).length;
                count.textContent = filled + ' / ' + fields.length;
                count.setAttribute(
                    'aria-label',
                    fields.length + '개 중 ' + filled + '개 입력');
                section.classList.toggle('detail-section--empty', filled === 0);
            });
        }

        function showErrorSummary(form, message) {
            var summary = form.querySelector(
                '[data-customer-detail-error-summary]');
            if (!summary) return;
            var messageElement = summary.querySelector(
                '[data-customer-detail-error-message]');
            if (messageElement) messageElement.textContent = message;
            summary.hidden = false;
            if (typeof summary.focus === 'function') summary.focus();
        }

        function clearErrorSummary(form) {
            var summary = form.querySelector(
                '[data-customer-detail-error-summary]');
            if (summary) summary.hidden = true;
        }

        function confirmAction(message) {
            if (window.Frog2UI
                    && typeof window.Frog2UI.confirmAction === 'function') {
                return window.Frog2UI.confirmAction(message);
            }
            return false;
        }

        function markDirty(form) {
            dirtyForms.add(form);
            form.classList.add('is-dirty');
            var environment = form.getAttribute('data-environment');
            var tab = tabs.find(function(candidate) {
                return environmentForTab(candidate) === environment;
            });
            if (tab) tab.classList.add('is-dirty');
            updateSectionCounts(form);
        }

        function initializeDateBounds() {
            var now = new Date();
            var today = now.getFullYear()
                + '-' + String(now.getMonth() + 1).padStart(2, '0')
                + '-' + String(now.getDate()).padStart(2, '0');

            forms.forEach(function(form) {
                form.querySelectorAll('input[type="date"]').forEach(function(input) {
                    if (input.name === 'createDate' || input.name === 'installDate') {
                        input.setAttribute('max', today);
                    }
                });
            });
        }

        forms.forEach(function(form) {
            initializeConditionalFields(form);
            updateSectionCounts(form);
            form.addEventListener('input', function() {
                markDirty(form);
            });
            form.addEventListener('change', function() {
                markDirty(form);
            });
            form.addEventListener('submit', function(event) {
                clearErrorSummary(form);
                var customerNameField = form.querySelector(
                    'input[name="customerName"]');
                var customerName = customerNameField
                    ? customerNameField.value.trim()
                    : '';
                if (!customerName) {
                    event.preventDefault();
                    if (window.Frog2UI
                            && typeof window.Frog2UI.showFieldError === 'function') {
                        window.Frog2UI.showFieldError(
                            customerNameField,
                            '고객사명이 필요합니다.');
                    }
                    showErrorSummary(form, '고객사명을 확인해 주세요.');
                    return;
                }

                var otherDirtyCount = Array.from(dirtyForms).filter(
                    function(dirtyForm) {
                        return dirtyForm !== form;
                    }).length;
                var environmentLabel = form.getAttribute(
                    'data-environment-label') || '현재';
                var message = otherDirtyCount > 0
                    ? environmentLabel + ' 환경만 저장합니다. 다른 환경의 저장하지 않은 변경사항은 사라집니다. 계속하시겠습니까?'
                    : environmentLabel + ' 환경의 변경사항을 저장하시겠습니까?';
                if (!confirmAction(message)) {
                    event.preventDefault();
                    return;
                }

                isSubmitting = true;
                dirtyForms.delete(form);
                form.classList.remove('is-dirty');
                var submittedEnvironment = form.getAttribute('data-environment');
                var submittedTab = tabs.find(function(candidate) {
                    return environmentForTab(candidate) === submittedEnvironment;
                });
                if (submittedTab) submittedTab.classList.remove('is-dirty');
            });
        });

        tabs.forEach(function(tab) {
            tab.addEventListener('click', function() {
                setActiveEnvironment(environmentForTab(tab), false);
            });
            tab.addEventListener('keydown', function(event) {
                var currentIndex = tabs.indexOf(tab);
                var nextIndex = currentIndex;
                if (event.key === 'ArrowRight') {
                    nextIndex = (currentIndex + 1) % tabs.length;
                } else if (event.key === 'ArrowLeft') {
                    nextIndex = (currentIndex - 1 + tabs.length) % tabs.length;
                } else if (event.key === 'Home') {
                    nextIndex = 0;
                } else if (event.key === 'End') {
                    nextIndex = tabs.length - 1;
                } else {
                    return;
                }

                event.preventDefault();
                var nextTab = tabs[nextIndex];
                setActiveEnvironment(environmentForTab(nextTab), false);
                nextTab.focus();
            });
        });

        window.addEventListener('resize', syncTabIndicator);
        window.addEventListener('popstate', function() {
            setActiveEnvironment(environmentFromLocation(), true);
        });
        window.addEventListener('beforeunload', function(event) {
            if (dirtyForms.size === 0 || isSubmitting) return;
            event.preventDefault();
            event.returnValue = '';
        });

        initializeDateBounds();
        if (tabs.length > 0 && panels.length > 0) {
            setActiveEnvironment(environmentFromLocation(), true);
        }
    });
})();
