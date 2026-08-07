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

    const context = {
        clearRectCalls: 0,
        fillRectCalls: 0,
        beginPath() {},
        clearRect() { this.clearRectCalls += 1; },
        fillRect() { this.fillRectCalls += 1; },
        lineTo() {},
        moveTo() {},
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
            constructor(callback) { this.callback = callback; }
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
        performance: { now: () => 1000 },
        window
    });

    return {
        canvas,
        context,
        document,
        documentListeners,
        windowListeners,
        animationFrames,
        runAnimationFrame(time = 1034) {
            const entry = animationFrames.entries().next().value;
            assert.ok(entry, 'expected one pending animation frame');
            const [id, callback] = entry;
            animationFrames.delete(id);
            callback(time);
        }
    };
}

function mediaQuery(matches) {
    return {
        matches,
        addEventListener() {}
    };
}

test('standard desktop caps drawing at 72 particles and 30fps', () => {
    const harness = createHarness();

    assert.equal(harness.animationFrames.size, 1);
    harness.runAnimationFrame();
    assert.equal(harness.context.fillRectCalls, 73);
    assert.equal(harness.animationFrames.size, 1);
    assert.equal(harness.canvas.width, 2160);

    harness.runAnimationFrame(1050);
    assert.equal(harness.context.fillRectCalls, 73);
    harness.runAnimationFrame(1067);
    assert.equal(harness.context.fillRectCalls, 146);
});

test('low-power desktop reduces the particle loop to 60 items', () => {
    const harness = createHarness({ hardwareConcurrency: 4 });

    harness.runAnimationFrame();
    assert.equal(harness.context.fillRectCalls, 61);
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
