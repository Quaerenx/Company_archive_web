(function () {
    'use strict';

    // Glitter Wrap — Originkit preset `custom-style`, adapted to plain Canvas.
    var canvas = document.querySelector('[data-glitter-wrap]');
    if (!canvas) {
        return;
    }

    var context = canvas.getContext('2d');
    if (!context) {
        return;
    }

    var PARTICLE_COUNT = 260;
    var SPEED = 1;
    var DENSITY = 44;
    var STAR_SIZE = 15;
    var FOCAL_DEPTH = 21;
    var BRIGHTNESS = 20;
    var GLITTER_INTENSITY = 1;
    var TRAIL_AMOUNT = 75;
    var TURBULENCE = 0;
    var REVERSE = false;

    var stepZ = SPEED * 0.0008;
    var focalDepth = FOCAL_DEPTH / 100;
    var starScale = STAR_SIZE * 0.15;
    var turbulence = TURBULENCE * 0.2;
    var glitter = GLITTER_INTENSITY * 0.1;
    var brightness = Math.min(1, BRIGHTNESS / 100);
    var trail = TRAIL_AMOUNT / 100;
    var stars = [];
    var size = { width: 0, height: 0, dpr: 1 };
    var elapsed = 0;
    var lastTime = performance.now();
    var frameId = null;
    var reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    var particleColor = window.getComputedStyle(canvas).color;

    function resetStar(star, initial) {
        var angle = Math.random() * Math.PI * 2;
        var radius = (0.2 + Math.random() * 0.8) * (DENSITY / 15);

        star.x = Math.cos(angle) * radius;
        star.y = Math.sin(angle) * radius;
        if (REVERSE) {
            star.z = initial
                ? focalDepth + Math.random() * (1 - focalDepth)
                : focalDepth;
        } else {
            star.z = initial ? Math.random() : 1;
        }
        star.px = Number.NaN;
        star.py = Number.NaN;
        star.seed = Math.random() * 1000;
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
            seed: 0,
            vmul: 1,
            flashUntil: 0,
            nextFlash: 0
        };
        resetStar(star, true);
        return star;
    }

    function syncCount() {
        while (stars.length < PARTICLE_COUNT) {
            stars.push(createStar());
        }
        if (stars.length > PARTICLE_COUNT) {
            stars.length = PARTICLE_COUNT;
        }
    }

    function resize() {
        var dpr = Math.min(window.devicePixelRatio || 1, 2);
        var width = Math.max(1, Math.floor(canvas.clientWidth) || 600);
        var height = Math.max(1, Math.floor(canvas.clientHeight) || 400);

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
            var velocity = stepZ * star.vmul * dt;

            if (REVERSE) {
                star.z += velocity;
                if (star.z >= 1) {
                    resetStar(star, false);
                    continue;
                }
            } else {
                star.z -= velocity;
                if (star.z <= focalDepth) {
                    resetStar(star, false);
                    continue;
                }
            }

            var projectedX = star.x;
            var projectedY = star.y;
            if (turbulence > 0) {
                var turbulenceTime = elapsed * 1.2 + star.seed;
                var amplitude = turbulence * (1 - star.z) * 0.25;
                projectedX += Math.sin(turbulenceTime + star.seed) * amplitude;
                projectedY += Math.cos(turbulenceTime * 1.13 + star.seed * 0.7) * amplitude;
            }

            var perspective = focalDepth / Math.max(star.z, 0.0001);
            var screenX = centerX + projectedX * perspective * projectionScale;
            var screenY = centerY + projectedY * perspective * projectionScale;

            if (!REVERSE && (
                screenX < -20 || screenX > width + 20
                || screenY < -20 || screenY > height + 20
            )) {
                resetStar(star, false);
                continue;
            }

            var flashMultiplier = 1;
            if (glitter > 0) {
                if (elapsed >= star.nextFlash && star.flashUntil < elapsed) {
                    star.flashUntil = elapsed + 0.04 + Math.random() * 0.07;
                    star.nextFlash = elapsed
                        + 1
                        + Math.random() * 4 * (1 / Math.max(0.0001, glitter));
                }
                if (elapsed <= star.flashUntil) {
                    flashMultiplier = 1 + 2.5 * glitter;
                }
            }

            var sizePerspective = Math.min(
                2.5,
                (focalDepth / Math.max(star.z, 0.0001)) * 0.6
            );
            var baseRadius = Math.max(0.25, starScale * (0.4 + sizePerspective));
            var maximumRadius = 1 + starScale * 2.5;
            var radius = Math.min(baseRadius * flashMultiplier, maximumRadius);
            var life = REVERSE ? star.z : 1 - star.z;
            var fadeIn = REVERSE
                ? Math.min(1, (star.z - focalDepth) / (1 - focalDepth) / 0.12)
                : 1;
            var alpha = Math.min(
                1,
                REVERSE ? 0.85 - life * 0.6 : life * 0.9 + 0.05
            ) * fadeIn * brightness * (flashMultiplier > 1 ? 1 : 0.85);

            if (!Number.isNaN(star.px) && !Number.isNaN(star.py)) {
                context.globalAlpha = alpha * 0.5;
                context.strokeStyle = particleColor;
                context.lineWidth = Math.max(0.4, radius * 0.4);
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

            if (flashMultiplier > 1) {
                var flashRadius = Math.min(radius * 1.4, maximumRadius * 1.4);
                context.globalAlpha = alpha * 0.5;
                context.fillRect(
                    screenX - flashRadius,
                    screenY - flashRadius,
                    flashRadius * 2,
                    flashRadius * 2
                );
            }

            star.px = screenX;
            star.py = screenY;
        }

        context.globalAlpha = 1;
        context.globalCompositeOperation = 'source-over';
        elapsed += Math.min(0.1, Math.max(0, deltaSeconds));
    }

    function loop(time) {
        var deltaSeconds = (time - lastTime) / 1000;
        lastTime = time;
        drawFrame(deltaSeconds);
        frameId = window.requestAnimationFrame(loop);
    }

    function start() {
        if (frameId !== null || reducedMotion || document.hidden) {
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

    function handleVisibilityChange() {
        if (document.hidden) {
            stop();
        } else {
            start();
        }
    }

    function renderReducedMotionFrame() {
        for (var warmup = 0; warmup < 80; warmup += 1) {
            drawFrame(1 / 60);
        }
    }

    function handleResize() {
        if (resize() && reducedMotion) {
            renderReducedMotionFrame();
        }
    }

    syncCount();
    resize();

    var resizeObserver = typeof window.ResizeObserver === 'function'
        ? new window.ResizeObserver(handleResize)
        : null;
    if (resizeObserver) {
        resizeObserver.observe(canvas);
    } else {
        window.addEventListener('resize', handleResize);
    }

    if (reducedMotion) {
        renderReducedMotionFrame();
    } else {
        start();
        document.addEventListener('visibilitychange', handleVisibilityChange);
    }
}());
