(function (root, factory) {
    'use strict';

    if (typeof module === 'object' && module.exports) {
        module.exports = factory;
        return;
    }

    factory(root);
}(typeof window === 'undefined' ? globalThis : window, function (root) {
    'use strict';

    var LOGIN_ROUTE = 'login';
    var DASHBOARD_ROUTE = 'dashboard';
    var currentUrl = parseUrl(root.location.href);
    var contextPath = currentUrl ? routeContext(currentUrl.pathname) : null;

    function parseUrl(url) {
        if (typeof url !== 'string' || !url) {
            return null;
        }
        try {
            return new root.URL(url, root.location.href);
        } catch (error) {
            return null;
        }
    }

    function normalizePath(pathname) {
        return pathname.replace(/\/+$/, '') || '/';
    }

    function routeContext(pathname) {
        var path = normalizePath(pathname);
        for (var route of [LOGIN_ROUTE, DASHBOARD_ROUTE]) {
            var suffix = '/' + route;
            if (path.endsWith(suffix)) {
                return path.slice(0, -suffix.length);
            }
        }
        return null;
    }

    function routeName(url) {
        var candidate = parseUrl(url);
        if (!candidate || !currentUrl || contextPath === null
                || candidate.origin !== currentUrl.origin) {
            return '';
        }

        var pathname = normalizePath(candidate.pathname);
        if (pathname === contextPath + '/login') {
            return LOGIN_ROUTE;
        }
        if (pathname === contextPath + '/dashboard') {
            return DASHBOARD_ROUTE;
        }
        return '';
    }

    function isStageRoutePair(fromUrl, toUrl) {
        var fromRoute = routeName(fromUrl);
        var toRoute = routeName(toUrl);
        return (fromRoute === LOGIN_ROUTE && toRoute === DASHBOARD_ROUTE)
            || (fromRoute === DASHBOARD_ROUTE && toRoute === LOGIN_ROUTE);
    }

    function prefersReducedMotion() {
        return typeof root.matchMedia === 'function'
            && root.matchMedia('(prefers-reduced-motion: reduce)').matches;
    }

    function skipUnlessStageRoute(transition, fromUrl, toUrl) {
        if (!transition) {
            return;
        }
        if ((prefersReducedMotion() || !isStageRoutePair(fromUrl, toUrl))
                && typeof transition.skipTransition === 'function') {
            transition.skipTransition();
        }
    }

    root.addEventListener('pageswap', function (event) {
        var activation = event && event.activation;
        var fromUrl = activation && activation.from
            ? activation.from.url
            : root.location.href;
        var toUrl = activation && activation.entry
            ? activation.entry.url
            : '';
        skipUnlessStageRoute(event && event.viewTransition, fromUrl, toUrl);
    });

    root.addEventListener('pagereveal', function (event) {
        var activation = root.navigation && root.navigation.activation;
        var fromUrl = activation && activation.from
            ? activation.from.url
            : '';
        var toUrl = activation && activation.entry
            ? activation.entry.url
            : root.location.href;
        skipUnlessStageRoute(event && event.viewTransition, fromUrl, toUrl);
    });

    return Object.freeze({
        isStageRoutePair: isStageRoutePair
    });
}));
