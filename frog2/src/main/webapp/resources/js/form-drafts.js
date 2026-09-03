(function () {
    'use strict';

    var VERSION = 'v1';
    var MAX_AGE_MS = 24 * 60 * 60 * 1000;
    var SAVE_DELAY_MS = 600;
    var PERIODIC_SAVE_MS = 5000;
    var MAX_SERIALIZED_LENGTH = 250000;
    var pendingKey = 'frog2.formDraft.pending.' + VERSION;

    function storageAvailable(storage) {
        try {
            var key = '__frog2_draft_test__';
            storage.setItem(key, key);
            storage.removeItem(key);
            return true;
        } catch (error) {
            return false;
        }
    }

    function safeParse(value, fallback) {
        try {
            var parsed = JSON.parse(value);
            return parsed == null ? fallback : parsed;
        } catch (error) {
            return fallback;
        }
    }

    function draftKey(form) {
        var userId = document.body.getAttribute('data-user-id') || 'anonymous';
        var formId = form.getAttribute('data-ui-draft-id') || form.id || 'form';
        return 'frog2.formDraft.' + VERSION + ':'
                + encodeURIComponent(userId) + ':'
                + encodeURIComponent(window.location.pathname) + ':'
                + encodeURIComponent(formId);
    }

    function eligibleField(field) {
        if (!field.name || field.disabled
                || field.hasAttribute('data-ui-draft-exclude')) {
            return false;
        }
        var type = String(field.type || '').toLowerCase();
        return type !== 'hidden' && type !== 'password' && type !== 'file'
                && type !== 'submit' && type !== 'button' && type !== 'reset';
    }

    function serialize(form) {
        var values = {};
        Array.prototype.forEach.call(form.elements, function (field) {
            if (!eligibleField(field)) return;
            var type = String(field.type || '').toLowerCase();
            if (type === 'checkbox' || type === 'radio') {
                if (!Object.prototype.hasOwnProperty.call(
                        values, field.name)) {
                    values[field.name] = [];
                }
                if (field.checked) {
                    values[field.name].push(field.value);
                }
                return;
            }
            if (Object.prototype.hasOwnProperty.call(values, field.name)) {
                if (!Array.isArray(values[field.name])) {
                    values[field.name] = [values[field.name]];
                }
                values[field.name].push(field.value);
            } else {
                values[field.name] = field.value;
            }
        });
        return values;
    }

    function sameValues(first, second) {
        return JSON.stringify(first) === JSON.stringify(second);
    }

    function applyValues(form, values) {
        Array.prototype.forEach.call(form.elements, function (field) {
            if (!eligibleField(field)
                    || !Object.prototype.hasOwnProperty.call(values, field.name)) {
                return;
            }
            var saved = values[field.name];
            var candidates = Array.isArray(saved) ? saved : [saved];
            var type = String(field.type || '').toLowerCase();
            if (type === 'checkbox' || type === 'radio') {
                field.checked = candidates.indexOf(field.value) >= 0;
            } else {
                field.value = candidates[0] == null ? '' : candidates[0];
            }
            field.dispatchEvent(new Event('input', {bubbles: true}));
            field.dispatchEvent(new Event('change', {bubbles: true}));
        });
    }

    function pendingDrafts() {
        var values = safeParse(window.sessionStorage.getItem(pendingKey), []);
        return Array.isArray(values) ? values : [];
    }

    function writePending(values) {
        window.sessionStorage.setItem(pendingKey, JSON.stringify(values));
    }

    function cleanSuccessfulSubmissions(formsByKey) {
        var currentView = new URLSearchParams(window.location.search).get('view')
                || 'list';
        var remaining = [];
        pendingDrafts().forEach(function (pending) {
            var matchingForm = formsByKey[pending.key];
            var successViews = Array.isArray(pending.successViews)
                    ? pending.successViews
                    : [];
            if (matchingForm || window.location.pathname !== pending.path
                    || successViews.indexOf(currentView) < 0) {
                remaining.push(pending);
                return;
            }
            window.sessionStorage.removeItem(pending.key);
        });
        writePending(remaining);
    }

    function createBanner(form, draft, key) {
        var banner = document.createElement('div');
        banner.className = 'ui-draft-banner ui-alert ui-alert--neutral';
        banner.setAttribute('role', 'status');
        var copy = document.createElement('span');
        copy.textContent = '이 브라우저에 저장된 작성 중 초안이 있습니다.';
        var actions = document.createElement('span');
        actions.className = 'ui-draft-banner__actions';
        var restore = document.createElement('button');
        restore.type = 'button';
        restore.className = 'ui-button button--primary button--sm';
        restore.textContent = '복원';
        var discard = document.createElement('button');
        discard.type = 'button';
        discard.className = 'ui-button button--secondary button--sm';
        discard.textContent = '버리기';
        actions.appendChild(restore);
        actions.appendChild(discard);
        banner.appendChild(copy);
        banner.appendChild(actions);
        form.parentNode.insertBefore(banner, form);

        restore.addEventListener('click', function () {
            applyValues(form, draft.values);
            banner.remove();
            var first = form.querySelector('input:not([type="hidden"]), select, textarea');
            if (first) first.focus();
        });
        discard.addEventListener('click', function () {
            window.sessionStorage.removeItem(key);
            banner.remove();
        });
    }

    function initializeForm(form) {
        if (form.querySelector('input[type="password"], input[type="file"]')) {
            return null;
        }
        var key = draftKey(form);
        var baseline = serialize(form);
        var timer = null;

        function save() {
            timer = null;
            var values = serialize(form);
            if (sameValues(values, baseline)) {
                window.sessionStorage.removeItem(key);
                return;
            }
            var payload = JSON.stringify({savedAt: Date.now(), values: values});
            if (payload.length <= MAX_SERIALIZED_LENGTH) {
                window.sessionStorage.setItem(key, payload);
            }
        }

        function scheduleSave() {
            if (timer !== null) window.clearTimeout(timer);
            timer = window.setTimeout(save, SAVE_DELAY_MS);
        }

        var stored = safeParse(window.sessionStorage.getItem(key), null);
        if (stored && typeof stored.savedAt === 'number'
                && Date.now() - stored.savedAt <= MAX_AGE_MS
                && stored.values && !sameValues(stored.values, baseline)) {
            createBanner(form, stored, key);
        } else if (stored) {
            window.sessionStorage.removeItem(key);
        }

        form.addEventListener('input', scheduleSave);
        form.addEventListener('change', scheduleSave);
        form.addEventListener('submit', function (event) {
            Promise.resolve().then(function () {
                if (event.defaultPrevented || !form.checkValidity()) return;
                save();
                var successViews = String(
                        form.getAttribute('data-ui-draft-success-views') || '')
                        .split(',').map(function (value) { return value.trim(); })
                        .filter(Boolean);
                var pending = pendingDrafts().filter(function (value) {
                    return value.key !== key;
                });
                pending.push({
                    key: key,
                    path: window.location.pathname,
                    successViews: successViews
                });
                writePending(pending);
            });
        });
        window.setInterval(save, PERIODIC_SAVE_MS);
        return key;
    }

    function initialize() {
        if (!storageAvailable(window.sessionStorage)) return;
        var forms = Array.prototype.slice.call(
                document.querySelectorAll('form[data-ui-draft="auto"]'));
        var formsByKey = {};
        forms.forEach(function (form) {
            var key = initializeForm(form);
            if (key) formsByKey[key] = form;
        });
        cleanSuccessfulSubmissions(formsByKey);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initialize);
    } else {
        initialize();
    }
}());
