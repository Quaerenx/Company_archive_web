'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');

const createRouteGate = require(
    '../../../../../main/webapp/resources/js/view-transition-routing.js'
);

function createHarness(options = {}) {
    const listeners = new Map();
    const reportedErrors = [];
    let skipTransitionCalls = 0;
    const root = {
        URL,
        console: {
            error(...args) {
                reportedErrors.push(args);
            }
        },
        addEventListener(name, listener) {
            listeners.set(name, listener);
        },
        location: {
            href: options.currentUrl || 'https://archive.test/frog2/login'
        },
        matchMedia() {
            return { matches: options.reducedMotion === true };
        },
        navigation: {
            activation: activation(options.fromUrl, options.toUrl)
        }
    };
    const transition = {
        ready: options.ready,
        skipTransition() {
            skipTransitionCalls += 1;
            if (options.onSkip) {
                options.onSkip();
            }
        }
    };

    const gate = createRouteGate(root);

    return {
        gate,
        reportedErrors,
        skipTransitionCalls() {
            return skipTransitionCalls;
        },
        dispatchPageReveal() {
            listeners.get('pagereveal')({ viewTransition: transition });
        },
        dispatchPageSwap(fromUrl, toUrl, includeTransition = true) {
            listeners.get('pageswap')({
                activation: activation(fromUrl, toUrl),
                viewTransition: includeTransition ? transition : null
            });
        }
    };
}

function activation(fromUrl, toUrl) {
    if (!fromUrl && !toUrl) {
        return null;
    }
    return {
        entry: toUrl ? { url: toUrl } : null,
        from: fromUrl ? { url: fromUrl } : null
    };
}

test('allows only login and dashboard stage route pairs', () => {
    const harness = createHarness();

    assert.equal(harness.gate.isStageRoutePair(
        'https://archive.test/frog2/login',
        'https://archive.test/frog2/dashboard'
    ), true);
    assert.equal(harness.gate.isStageRoutePair(
        'https://archive.test/frog2/dashboard?maintenanceMonth=2026-08',
        'https://archive.test/frog2/login'
    ), true);
    assert.equal(harness.gate.isStageRoutePair(
        'https://archive.test/frog2/login',
        'https://archive.test/frog2/login'
    ), false);
    assert.equal(harness.gate.isStageRoutePair(
        'https://archive.test/frog2/dashboard',
        'https://archive.test/frog2/dashboard?maintenanceMonth=2026-09'
    ), false);
    assert.equal(harness.gate.isStageRoutePair(
        'https://other.test/frog2/login',
        'https://archive.test/frog2/dashboard'
    ), false);
    assert.equal(harness.gate.isStageRoutePair(
        'https://archive.test/archive/frog2/login',
        'https://archive.test/frog2/dashboard'
    ), false);
});

test('keeps successful login and logout transitions', () => {
    const login = createHarness();
    login.dispatchPageSwap(
        'https://archive.test/frog2/login',
        'https://archive.test/frog2/dashboard'
    );
    assert.equal(login.skipTransitionCalls(), 0);

    const logout = createHarness({
        currentUrl: 'https://archive.test/frog2/login',
        fromUrl: 'https://archive.test/frog2/dashboard',
        toUrl: 'https://archive.test/frog2/login'
    });
    logout.dispatchPageReveal();
    assert.equal(logout.skipTransitionCalls(), 0);
});

test('skips failed login and dashboard self-navigation transitions', () => {
    const failedLogin = createHarness();
    failedLogin.dispatchPageSwap(
        'https://archive.test/frog2/login',
        'https://archive.test/frog2/login'
    );
    assert.equal(failedLogin.skipTransitionCalls(), 1);

    const monthChange = createHarness();
    monthChange.dispatchPageSwap(
        'https://archive.test/frog2/dashboard?maintenanceMonth=2026-08',
        'https://archive.test/frog2/dashboard?maintenanceMonth=2026-09'
    );
    assert.equal(monthChange.skipTransitionCalls(), 1);
});

test('fails closed for missing route data and reduced motion', () => {
    const missingRoute = createHarness();
    missingRoute.dispatchPageSwap(null, null);
    assert.equal(missingRoute.skipTransitionCalls(), 1);

    const reduced = createHarness({ reducedMotion: true });
    reduced.dispatchPageSwap(
        'https://archive.test/frog2/login',
        'https://archive.test/frog2/dashboard'
    );
    assert.equal(reduced.skipTransitionCalls(), 1);

    const unsupported = createHarness();
    unsupported.dispatchPageSwap(
        'https://archive.test/frog2/login',
        'https://archive.test/frog2/dashboard',
        false
    );
    assert.equal(unsupported.skipTransitionCalls(), 0);
});

test('observes outgoing ready rejection when a successful navigation hides the document', async () => {
    let rejectReady;
    const ready = new Promise((resolve, reject) => { rejectReady = reject; });
    const harness = createHarness({ ready });

    harness.dispatchPageSwap(
        'https://archive.test/frog2/login',
        'https://archive.test/frog2/dashboard'
    );
    rejectReady(new DOMException('The outgoing document was hidden', 'AbortError'));
    await new Promise(setImmediate);

    assert.equal(harness.skipTransitionCalls(), 0);
    assert.deepEqual(harness.reportedErrors, []);
});

test('observes incoming ready rejection when the viewport changes before capture', async () => {
    let rejectReady;
    const ready = new Promise((resolve, reject) => { rejectReady = reject; });
    const harness = createHarness({
        ready,
        currentUrl: 'https://archive.test/frog2/dashboard',
        fromUrl: 'https://archive.test/frog2/login',
        toUrl: 'https://archive.test/frog2/dashboard'
    });

    harness.dispatchPageReveal();
    rejectReady(new DOMException(
        'Transition was aborted because of invalid state. Viewport size changed',
        'InvalidStateError'
    ));
    await new Promise(setImmediate);

    assert.equal(harness.skipTransitionCalls(), 0);
    assert.deepEqual(harness.reportedErrors, []);
});

test('observes ready before explicitly skipping reduced motion and failed login', async () => {
    for (const reducedMotion of [true, false]) {
        let rejectReady;
        const ready = new Promise((resolve, reject) => { rejectReady = reject; });
        const harness = createHarness({
            ready,
            reducedMotion,
            onSkip() {
                rejectReady(new DOMException('Transition was skipped', 'AbortError'));
            }
        });

        harness.dispatchPageSwap(
            'https://archive.test/frog2/login',
            reducedMotion
                ? 'https://archive.test/frog2/dashboard'
                : 'https://archive.test/frog2/login'
        );
        await new Promise(setImmediate);

        assert.equal(harness.skipTransitionCalls(), 1);
        assert.deepEqual(harness.reportedErrors, []);
    }
});

test('reports unexpected transition readiness failures without changing navigation', async () => {
    let rejectReady;
    const ready = new Promise((resolve, reject) => { rejectReady = reject; });
    const harness = createHarness({ ready });
    const failure = new TypeError('Unexpected transition failure');

    harness.dispatchPageSwap(
        'https://archive.test/frog2/login',
        'https://archive.test/frog2/dashboard'
    );
    rejectReady(failure);
    await new Promise(setImmediate);

    assert.equal(harness.skipTransitionCalls(), 0);
    assert.equal(harness.reportedErrors.length, 1);
    assert.equal(harness.reportedErrors[0][1], failure);
});

for (const [description, message] of [
    ['duplicate snapshot names', 'Duplicate view-transition-name: archive-logo'],
    ['other invalid capture states', 'Transition was aborted because of invalid state']
]) {
    test('reports ' + description + ' instead of treating them as viewport cancellation', async () => {
        let rejectReady;
        const ready = new Promise((resolve, reject) => { rejectReady = reject; });
        const harness = createHarness({ ready });
        const failure = new DOMException(message, 'InvalidStateError');

        harness.dispatchPageSwap(
            'https://archive.test/frog2/login',
            'https://archive.test/frog2/dashboard'
        );
        rejectReady(failure);
        await new Promise(setImmediate);

        assert.equal(harness.skipTransitionCalls(), 0);
        assert.equal(harness.reportedErrors.length, 1);
        assert.equal(harness.reportedErrors[0][1], failure);
    });
}
