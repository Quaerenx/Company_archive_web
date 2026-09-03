(function () {
    'use strict';

    var MAX_AGE_MS = 2 * 60 * 60 * 1000;
    var HIGHLIGHT_MS = 1200;

    function storageKey() {
        var userId = document.body
            ? document.body.getAttribute('data-user-id') || 'anonymous'
            : 'anonymous';
        return 'frog2.listReturn.v1:' + userId;
    }

    function currentUrlKey() {
        var parameters = new URLSearchParams(window.location.search);
        parameters.delete('_flash');
        parameters.sort();
        var query = parameters.toString();
        return window.location.pathname + (query ? '?' + query : '');
    }

    function remember(source) {
        var list = source.closest('[data-ui-return-list]');
        var key = source.getAttribute('data-ui-return-key')
            || source.getAttribute('data-ui-return-source-key');
        if (!list || !key) return;
        try {
            window.sessionStorage.setItem(storageKey(), JSON.stringify({
                url: currentUrlKey(),
                rowKey: key,
                scrollY: window.scrollY || 0,
                savedAt: Date.now()
            }));
        } catch (error) {
            // Navigation remains usable when session storage is unavailable.
        }
    }

    function restore() {
        var raw;
        try {
            raw = window.sessionStorage.getItem(storageKey());
        } catch (error) {
            return;
        }
        if (!raw) return;

        var state;
        try {
            state = JSON.parse(raw);
        } catch (error) {
            window.sessionStorage.removeItem(storageKey());
            return;
        }
        if (!state || state.url !== currentUrlKey()
                || !state.rowKey
                || Date.now() - Number(state.savedAt || 0) > MAX_AGE_MS) {
            return;
        }

        var row = Array.prototype.find.call(
            document.querySelectorAll('[data-ui-return-row]'),
            function (candidate) {
                return candidate.getAttribute('data-ui-return-key') === state.rowKey;
            });
        if (!row) return;

        window.sessionStorage.removeItem(storageKey());
        window.requestAnimationFrame(function () {
            window.requestAnimationFrame(function () {
                if (typeof row.scrollIntoView === 'function') {
                    row.scrollIntoView({block: 'center', inline: 'nearest'});
                } else if (Number.isFinite(state.scrollY)) {
                    window.scrollTo(0, state.scrollY);
                }
                row.classList.add('ui-return-highlight');
                window.setTimeout(function () {
                    row.classList.remove('ui-return-highlight');
                }, HIGHLIGHT_MS);
            });
        });
    }

    document.addEventListener('click', function (event) {
        if (event.defaultPrevented || event.button > 0
                || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
            return;
        }
        var target = event.target;
        var row = target && typeof target.closest === 'function'
            ? target.closest('[data-ui-return-row]')
            : null;
        var source = row || (target && typeof target.closest === 'function'
            ? target.closest('[data-ui-return-source-key]')
            : null);
        if (!source) return;
        var link = target.closest('a[href]');
        if (!link && !source.hasAttribute('data-detail-url')) return;
        remember(source);
    }, true);

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', restore);
    } else {
        restore();
    }
}());
