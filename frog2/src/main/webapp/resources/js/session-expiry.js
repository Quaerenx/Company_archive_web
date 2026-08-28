(function (root, factory) {
    'use strict';

    if (typeof module === 'object' && module.exports) {
        module.exports = factory;
        return;
    }

    var script = document.currentScript;
    var contextPath = script ? script.getAttribute('data-context-path') : '';
    root.Frog2Session = Object.freeze(factory({
        contextPath: contextPath,
        notify: function (message) {
            if (root.Frog2UI && typeof root.Frog2UI.notify === 'function') {
                root.Frog2UI.notify(message, 'warning', { persistent: true });
            }
        },
        redirect: function (url) {
            root.location.assign(url);
        },
        schedule: function (callback, delay) {
            return root.setTimeout(callback, delay);
        }
    }));
}(typeof window === 'undefined' ? globalThis : window, function (options) {
    'use strict';

    var SESSION_EXPIRED_MESSAGE =
        '로그인 세션이 만료되었습니다. 다시 로그인해주세요.';
    var redirectScheduled = false;
    var contextPath = normalizeContextPath(options && options.contextPath);
    var notify = options && typeof options.notify === 'function'
        ? options.notify
        : function () {};
    var redirect = options && typeof options.redirect === 'function'
        ? options.redirect
        : function () {};
    var schedule = options && typeof options.schedule === 'function'
        ? options.schedule
        : function (callback) { callback(); };

    function normalizeContextPath(value) {
        var candidate = typeof value === 'string' ? value.trim() : '';
        if (!candidate) return '';
        if (candidate.charAt(0) !== '/' || candidate.indexOf('//') === 0
                || /[?#\\\u0000-\u001f]/.test(candidate)) {
            return '';
        }
        return candidate.replace(/\/+$/, '');
    }

    function createSessionExpiredError() {
        var error = new Error(SESSION_EXPIRED_MESSAGE);
        error.name = 'SessionExpiredError';
        error.sessionExpired = true;
        return error;
    }

    function isSessionExpired(error) {
        return Boolean(error && error.sessionExpired === true);
    }

    function requireActiveSession(response) {
        if (!response || response.status !== 401) {
            return response;
        }

        if (!redirectScheduled) {
            redirectScheduled = true;
            notify(SESSION_EXPIRED_MESSAGE);
            schedule(function () {
                redirect(contextPath + '/login');
            }, 900);
        }
        throw createSessionExpiredError();
    }

    return {
        isSessionExpired: isSessionExpired,
        requireActiveSession: requireActiveSession
    };
}));
