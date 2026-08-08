(function () {
    'use strict';

    var canvas = document.querySelector('[data-app-ambient-background]');
    if (!canvas) {
        return;
    }

    var context = canvas.getContext('2d');
    if (!context) {
        return;
    }

    var DEFAULT_PARTICLE_COUNT = 36;
    var LOW_POWER_PARTICLE_COUNT = 24;
    var TARGET_FRAME_RATE = 30;
    var FRAME_INTERVAL = 1000 / TARGET_FRAME_RATE;
    var MAX_DEVICE_PIXEL_RATIO = 1.5;
    var SPEED = 0.18;
    var DENSITY = 36;
    var STAR_SIZE = 4;
    var FOCAL_DEPTH = 21;
    var BRIGHTNESS = 9;
    var GLITTER_INTENSITY = 0.03;
    var TRAIL_AMOUNT = 30;

    var stepZ = SPEED * 0.0008;
    var focalDepth = FOCAL_DEPTH / 100;
    var starScale = STAR_SIZE * 0.15;
    var glitter = GLITTER_INTENSITY * 0.1;
    var brightness = Math.min(1, BRIGHTNESS / 100);
    var trail = TRAIL_AMOUNT / 100;
    var stars = [];
    var size = { width: 0, height: 0, dpr: 1 };
    var elapsed = 0;
    var lastTime = performance.now();
    var frameId = null;
    var desktopQuery = window.matchMedia('(min-width: 1051px)');
    var ambientActive = desktopQuery.matches;
    var reducedMotionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
    var reducedMotion = reducedMotionQuery.matches;
    var particleColor = window.getComputedStyle(canvas).color;

    function preferredParticleCount() {
        var logicalProcessors = navigator.hardwareConcurrency || 8;
        var deviceMemory = navigator.deviceMemory || 8;
        return logicalProcessors <= 4 || deviceMemory <= 4
            ? LOW_POWER_PARTICLE_COUNT
            : DEFAULT_PARTICLE_COUNT;
    }

    var particleCount = preferredParticleCount();

    function resetStar(star, initial) {
        var angle = Math.random() * Math.PI * 2;
        var radius = (0.2 + Math.random() * 0.8) * (DENSITY / 15);

        star.x = Math.cos(angle) * radius;
        star.y = Math.sin(angle) * radius;
        star.z = initial ? Math.random() : 1;
        star.px = Number.NaN;
        star.py = Number.NaN;
        star.vmul = 0.6 + Math.random() * 0.8;
        star.flashUntil = 0;
        star.nextFlash = elapsed
            + 1
            + Math.random() * 4 * (1 / Math.max(0.0001, glitter));
    }

    function createStar() {
        var star = {
            x: 0,
            y: 0,
            z: 0,
            px: Number.NaN,
            py: Number.NaN,
            vmul: 1,
            flashUntil: 0,
            nextFlash: 0
        };
        resetStar(star, true);
        return star;
    }

    function syncCount() {
        while (stars.length < particleCount) {
            stars.push(createStar());
        }
        if (stars.length > particleCount) {
            stars.length = particleCount;
        }
    }

    function resize() {
        var dpr = Math.min(
            window.devicePixelRatio || 1,
            MAX_DEVICE_PIXEL_RATIO
        );
        var width = Math.max(1, Math.floor(document.documentElement.clientWidth));
        var height = Math.max(1, Math.floor(document.documentElement.clientHeight));

        ambientActive = desktopQuery.matches;
        if (size.width === width && size.height === height && size.dpr === dpr) {
            return false;
        }

        size = { width: width, height: height, dpr: dpr };
        canvas.width = Math.floor(width * dpr);
        canvas.height = Math.floor(height * dpr);
        context.setTransform(dpr, 0, 0, dpr, 0, 0);
        context.clearRect(0, 0, width, height);
        return true;
    }

    function drawFrame(deltaSeconds) {
        if (!ambientActive) {
            return;
        }

        var width = size.width;
        var height = size.height;
        var centerX = width / 2;
        var centerY = height / 2;
        var projectionScale = Math.min(width, height) * 0.9;
        var dt = Math.max(0.001, Math.min(0.1, deltaSeconds)) * 60;
        var keep = Math.pow(Math.min(0.98, Math.max(0, trail)), dt);
        var trailAlpha = Math.max(0.02, 1 - keep);

        context.globalCompositeOperation = 'destination-out';
        context.globalAlpha = trailAlpha;
        context.fillStyle = particleColor;
        context.fillRect(0, 0, width, height);
        context.globalCompositeOperation = 'lighter';

        for (var index = 0; index < stars.length; index += 1) {
            var star = stars[index];
            star.z -= stepZ * star.vmul * dt;
            if (star.z <= focalDepth) {
                resetStar(star, false);
                continue;
            }

            var perspective = focalDepth / Math.max(star.z, 0.0001);
            var screenX = centerX + star.x * perspective * projectionScale;
            var screenY = centerY + star.y * perspective * projectionScale;

            if (
                screenX < -20 || screenX > width + 20
                || screenY < -20 || screenY > height + 20
            ) {
                resetStar(star, false);
                continue;
            }

            var flashMultiplier = 1;
            if (elapsed >= star.nextFlash && star.flashUntil < elapsed) {
                star.flashUntil = elapsed + 0.04 + Math.random() * 0.04;
                star.nextFlash = elapsed
                    + 1
                    + Math.random() * 4 * (1 / Math.max(0.0001, glitter));
            }
            if (elapsed <= star.flashUntil) {
                flashMultiplier = 1 + 2 * glitter;
            }

            var sizePerspective = Math.min(
                2.5,
                (focalDepth / Math.max(star.z, 0.0001)) * 0.6
            );
            var baseRadius = Math.max(0.25, starScale * (0.4 + sizePerspective));
            var maximumRadius = 1 + starScale * 2.5;
            var radius = Math.min(baseRadius * flashMultiplier, maximumRadius);
            var life = 1 - star.z;
            var alpha = Math.min(1, life * 0.9 + 0.05)
                * brightness
                * (flashMultiplier > 1 ? 1 : 0.85);

            if (!Number.isNaN(star.px) && !Number.isNaN(star.py)) {
                context.globalAlpha = alpha * 0.35;
                context.strokeStyle = particleColor;
                context.lineWidth = Math.max(0.3, radius * 0.3);
                context.beginPath();
                context.moveTo(star.px, star.py);
                context.lineTo(screenX, screenY);
                context.stroke();
            }

            context.globalAlpha = alpha;
            context.fillStyle = particleColor;
            context.fillRect(
                screenX - radius,
                screenY - radius,
                radius * 2,
                radius * 2
            );
            star.px = screenX;
            star.py = screenY;
        }

        context.globalAlpha = 1;
        context.globalCompositeOperation = 'source-over';
        elapsed += Math.min(0.1, Math.max(0, deltaSeconds));
    }

    function loop(time) {
        var deltaMilliseconds = time - lastTime;
        if (deltaMilliseconds >= FRAME_INTERVAL) {
            drawFrame(deltaMilliseconds / 1000);
            lastTime = time - (deltaMilliseconds % FRAME_INTERVAL);
        }
        frameId = window.requestAnimationFrame(loop);
    }

    function start() {
        if (
            frameId !== null
            || reducedMotion
            || document.hidden
            || !ambientActive
        ) {
            return;
        }
        lastTime = performance.now();
        frameId = window.requestAnimationFrame(loop);
    }

    function stop() {
        if (frameId !== null) {
            window.cancelAnimationFrame(frameId);
            frameId = null;
        }
    }

    function renderReducedMotionFrame() {
        context.clearRect(0, 0, size.width, size.height);
    }

    function handleResize() {
        var changed = resize();
        if (!ambientActive) {
            stop();
            context.clearRect(0, 0, size.width, size.height);
            return;
        }
        if (reducedMotion) {
            if (changed) {
                renderReducedMotionFrame();
            }
        } else {
            start();
        }
    }

    function handleVisibilityChange() {
        if (document.hidden) {
            stop();
        } else {
            start();
        }
    }

    function handleReducedMotionChange(event) {
        reducedMotion = event.matches;
        stop();
        context.clearRect(0, 0, size.width, size.height);
        if (reducedMotion) {
            renderReducedMotionFrame();
        } else {
            start();
        }
    }

    function handleDesktopChange(event) {
        ambientActive = event.matches;
        stop();
        context.clearRect(0, 0, size.width, size.height);
        if (ambientActive) {
            if (reducedMotion) {
                renderReducedMotionFrame();
            } else {
                start();
            }
        }
    }

    syncCount();
    resize();

    var resizeObserver = typeof window.ResizeObserver === 'function'
        ? new window.ResizeObserver(handleResize)
        : null;
    if (resizeObserver) {
        resizeObserver.observe(document.documentElement);
    } else {
        window.addEventListener('resize', handleResize);
    }

    if (typeof reducedMotionQuery.addEventListener === 'function') {
        reducedMotionQuery.addEventListener('change', handleReducedMotionChange);
    }
    if (typeof desktopQuery.addEventListener === 'function') {
        desktopQuery.addEventListener('change', handleDesktopChange);
    }
    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener('pagehide', stop);
    window.addEventListener('pageshow', start);

    if (reducedMotion) {
        renderReducedMotionFrame();
    } else {
        start();
    }
}());
