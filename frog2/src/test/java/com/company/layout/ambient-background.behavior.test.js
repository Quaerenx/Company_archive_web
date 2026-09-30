'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync(
    'src/main/webapp/resources/js/ambient-background.js',
    'utf8'
);

function createHarness(options = {}) {
    const documentListeners = new Map();
    const windowListeners = new Map();
    const animationFrames = new Map();
    let nextId = 1;
    let now = 1000;
    let resizeCallback;

    const context = {
        clearRectCalls: 0,
        fillRectCalls: 0,
        fillRects: [],
        lineSegments: [],
        beginPath() {},
        clearRect() { this.clearRectCalls += 1; },
        fillRect(x, y, width, height) {
            this.fillRectCalls += 1;
            this.fillRects.push({ x, y, width, height });
        },
        lineTo(x, y) { this.lineSegments.push({ from: this.lineStart, x, y }); },
        moveTo(x, y) { this.lineStart = { x, y }; },
        setTransform() {},
        stroke() {}
    };
    const canvas = {
        getContext() { return context; },
        width: 0,
        height: 0
    };
    const desktopQuery = mediaQuery(options.desktop !== false);
    const reducedMotionQuery = mediaQuery(options.reducedMotion === true);
    const document = {
        body: {
            classList: {
                contains() { return false; }
            }
        },
        documentElement: {
            clientHeight: options.height || 900,
            clientWidth: options.width || 1440
        },
        hidden: false,
        addEventListener(name, listener) {
            documentListeners.set(name, listener);
        },
        querySelector() { return canvas; }
    };
    const window = {
        ResizeObserver: class {
            constructor(callback) { resizeCallback = callback; }
            observe() {}
        },
        addEventListener(name, listener) {
            windowListeners.set(name, listener);
        },
        cancelAnimationFrame(id) { animationFrames.delete(id); },
        devicePixelRatio: options.devicePixelRatio || 2,
        getComputedStyle() { return { color: '#F1F3F5' }; },
        matchMedia(query) {
            return query.includes('prefers-reduced-motion')
                ? reducedMotionQuery
                : desktopQuery;
        },
        requestAnimationFrame(callback) {
            const id = nextId++;
            animationFrames.set(id, callback);
            return id;
        }
    };
    const deterministicMath = Object.create(Math);
    deterministicMath.random = () => 0.5;

    vm.runInNewContext(source, {
        document,
        Math: deterministicMath,
        navigator: {
            deviceMemory: options.deviceMemory || 8,
            hardwareConcurrency: options.hardwareConcurrency || 8
        },
        performance: { now: () => now },
        window
    });

    return {
        canvas,
        context,
        document,
        documentListeners,
        windowListeners,
        animationFrames,
        desktopQuery,
        reducedMotionQuery,
        resize(width, height) {
            document.documentElement.clientWidth = width;
            document.documentElement.clientHeight = height;
            resizeCallback();
        },
        runAnimationFrame(time = 1034) {
            const entry = animationFrames.entries().next().value;
            assert.ok(entry, 'expected one pending animation frame');
            const [id, callback] = entry;
            animationFrames.delete(id);
            now = time;
            callback(time);
        }
    };
}

function mediaQuery(matches) {
    const listeners = new Map();
    return {
        matches,
        addEventListener(name, listener) { listeners.set(name, listener); },
        change(nextMatches) {
            this.matches = nextMatches;
            listeners.get('change')({ matches: nextMatches });
        }
    };
}

test('standard desktop caps drawing at 36 particles and 30fps', () => {
    const harness = createHarness();

    assert.equal(harness.animationFrames.size, 1);
    harness.runAnimationFrame();
    assert.equal(harness.context.fillRectCalls, 36);
    assert.equal(harness.animationFrames.size, 1);
    assert.equal(harness.canvas.width, 2160);

    harness.runAnimationFrame(1050);
    assert.equal(harness.context.fillRectCalls, 36);
    harness.runAnimationFrame(1067);
    assert.equal(harness.context.fillRectCalls, 72);
});

test('each rendered frame clears old pixels instead of accumulating translucent trails', () => {
    const harness = createHarness();
    const initialClearCount = harness.context.clearRectCalls;

    harness.runAnimationFrame(1040);
    harness.runAnimationFrame(1080);

    assert.equal(harness.context.clearRectCalls, initialClearCount + 2);
    assert.equal(harness.context.fillRects.filter(rect => rect.width > 10).length, 0);
    assert.equal(harness.context.lineSegments.length, 0);
});

test('resizing clears the old frame and draws particles in the new viewport', () => {
    const harness = createHarness();
    harness.runAnimationFrame(1040);
    harness.runAnimationFrame(1080);
    const previousFills = harness.context.fillRectCalls;
    const previousClears = harness.context.clearRectCalls;

    harness.resize(1600, 1000);
    harness.runAnimationFrame(1120);

    assert.equal(harness.context.clearRectCalls, previousClears + 2);
    assert.equal(harness.context.fillRectCalls, previousFills + 36);
    for (const rect of harness.context.fillRects.slice(-36)) {
        assert.ok(rect.x >= 0 && rect.x + rect.width <= 1600);
        assert.ok(Math.abs(rect.y + rect.height / 2 - 500) < 1e-9);
    }
    harness.runAnimationFrame(1160);
    assert.equal(harness.context.fillRectCalls, previousFills + 72);
    assert.equal(harness.context.lineSegments.length, 0);
});

test('resuming visibility clears the old frame and draws without trails', () => {
    const harness = createHarness();
    harness.runAnimationFrame(1040);
    harness.runAnimationFrame(1080);
    const previousFills = harness.context.fillRectCalls;
    const previousClearCount = harness.context.clearRectCalls;

    harness.document.hidden = true;
    harness.documentListeners.get('visibilitychange')();
    assert.equal(harness.context.clearRectCalls, previousClearCount + 1);
    harness.document.hidden = false;
    harness.documentListeners.get('visibilitychange')();
    harness.runAnimationFrame(1120);

    assert.equal(harness.context.fillRectCalls, previousFills + 36);
    assert.equal(harness.context.lineSegments.length, 0);
});

test('movement timing does not count scheduler remainder twice', () => {
    const irregular = createHarness();
    irregular.runAnimationFrame(1040);
    irregular.runAnimationFrame(1079);
    const singleFrame = createHarness();
    singleFrame.runAnimationFrame(1079);
    const irregularPosition = irregular.context.fillRects.filter(rect => rect.width < 10).at(-36);
    const singlePosition = singleFrame.context.fillRects.filter(rect => rect.width < 10).at(-36);

    assert.ok(Math.abs((irregularPosition.x + irregularPosition.width / 2)
        - (singlePosition.x + singlePosition.width / 2)) < 1e-9,
        'the same elapsed time must produce the same particle position');
    assert.ok(Math.abs((irregularPosition.y + irregularPosition.height / 2)
        - (singlePosition.y + singlePosition.height / 2)) < 1e-9);
});

test('desktop, reduced-motion and page lifecycle changes clear and resume without trails', () => {
    for (const mode of ['desktop', 'reduced-motion', 'page']) {
        const harness = createHarness();
        harness.runAnimationFrame(1040);
        harness.runAnimationFrame(1080);
        const previousFills = harness.context.fillRectCalls;
        const previousClearCount = harness.context.clearRectCalls;

        if (mode === 'desktop') {
            harness.desktopQuery.change(false);
        } else if (mode === 'reduced-motion') {
            harness.reducedMotionQuery.change(true);
        } else {
            harness.windowListeners.get('pagehide')();
        }
        assert.equal(harness.animationFrames.size, 0, mode);
        assert.ok(harness.context.clearRectCalls > previousClearCount, mode);

        if (mode === 'desktop') {
            harness.desktopQuery.change(true);
        } else if (mode === 'reduced-motion') {
            harness.reducedMotionQuery.change(false);
        } else {
            harness.windowListeners.get('pageshow')();
        }
        assert.equal(harness.animationFrames.size, 1, mode);
        harness.runAnimationFrame(1120);
        assert.equal(harness.context.fillRectCalls, previousFills + 36, mode);
        harness.runAnimationFrame(1160);
        assert.equal(harness.context.fillRectCalls, previousFills + 72, mode);
        assert.equal(harness.context.lineSegments.length, 0, mode);
    }
});

test('irregular frames and long pauses never leave particle strokes', () => {
    const harness = createHarness();
    const initialClears = harness.context.clearRectCalls;
    const timestamps = [1040, 1080, 1120, 1600, 1640, 8000, 8040];

    for (const timestamp of timestamps) {
        harness.runAnimationFrame(timestamp);
    }

    assert.equal(harness.context.clearRectCalls, initialClears + timestamps.length);
    assert.equal(harness.context.fillRectCalls, 36 * timestamps.length);
    assert.equal(harness.context.lineSegments.length, 0);
    assert.equal(harness.animationFrames.size, 1);
});

test('low-power desktop reduces the particle loop to 24 items', () => {
    const harness = createHarness({ hardwareConcurrency: 4 });

    harness.runAnimationFrame();
    assert.equal(harness.context.fillRectCalls, 24);
});

test('mobile and reduced-motion modes do not start animation', () => {
    const mobile = createHarness({ desktop: false, width: 390 });
    const reduced = createHarness({ reducedMotion: true });

    assert.equal(mobile.animationFrames.size, 0);
    assert.equal(reduced.animationFrames.size, 0);
    assert.ok(reduced.context.clearRectCalls >= 1);
});

test('visibility and page lifecycle events stop and resume work', () => {
    const harness = createHarness();

    harness.document.hidden = true;
    harness.documentListeners.get('visibilitychange')();
    assert.equal(harness.animationFrames.size, 0);

    harness.document.hidden = false;
    harness.documentListeners.get('visibilitychange')();
    assert.equal(harness.animationFrames.size, 1);

    harness.windowListeners.get('pagehide')();
    assert.equal(harness.animationFrames.size, 0);

    harness.windowListeners.get('pageshow')();
    assert.equal(harness.animationFrames.size, 1);
});
