(function () {
    'use strict';

    var TABLE_STICKY_OFFSET_PROPERTY = '--table-sticky-offset';
    var TABLE_STICKY_READY_CLASS = 'is-table-sticky-ready';
    var tableHeaderResizeObserver = null;
    var scrollRegionFrame = 0;

    function updateTableStickyOffset() {
        var header = document.querySelector('.main-header');
        var root = document.documentElement;
        if (!header || !root || !root.style) {
            return false;
        }

        var headerBounds = header.getBoundingClientRect();
        var headerBottom = Number.isFinite(headerBounds.bottom)
            ? Math.max(0, Math.ceil(headerBounds.bottom))
            : 0;
        root.style.setProperty(
            TABLE_STICKY_OFFSET_PROPERTY,
            headerBottom + 'px'
        );
        return headerBottom > 0;
    }

    function updateScrollableRegionHint(region, scrollable) {
        var hintId = region.dataset.uiScrollHintId;
        if (!hintId) {
            return;
        }

        var hint = document.getElementById(hintId);
        if (!hint) {
            return;
        }
        hint.hidden = !scrollable;

        var describedBy = (region.getAttribute('aria-describedby') || '')
            .split(/\s+/)
            .filter(Boolean)
            .filter(function (id) {
                return id !== hintId;
            });
        if (scrollable) {
            describedBy.push(hintId);
        }
        if (describedBy.length) {
            region.setAttribute('aria-describedby', describedBy.join(' '));
        } else {
            region.removeAttribute('aria-describedby');
        }
    }

    function updateScrollableTableRegions() {
        var stickyOffsetReady = updateTableStickyOffset();
        document.querySelectorAll(
            '.' + TABLE_STICKY_READY_CLASS
        ).forEach(function (surface) {
            surface.classList.remove(TABLE_STICKY_READY_CLASS);
        });

        document.querySelectorAll('[data-ui-scroll-region]').forEach(function (region) {
            var scrollable = region.scrollWidth > region.clientWidth + 1;
            region.dataset.uiScrollable = String(scrollable);
            updateScrollableRegionHint(region, scrollable);
            var workSurface = region.closest(
                '.ui-work-surface, [data-ui-table-surface]'
            );
            if (workSurface && stickyOffsetReady && !scrollable) {
                workSurface.classList.add(TABLE_STICKY_READY_CLASS);
            }
            if (scrollable) {
                region.setAttribute('tabindex', '0');
                region.setAttribute('role', 'region');
                var label = region.dataset.uiScrollLabel;
                if (label) {
                    region.setAttribute('aria-label', label);
                }
                return;
            }
            region.removeAttribute('tabindex');
            region.removeAttribute('role');
            region.removeAttribute('aria-label');
        });
    }

    function scheduleScrollableTableRegionUpdate() {
        if (scrollRegionFrame) {
            return;
        }
        scrollRegionFrame = window.requestAnimationFrame(function () {
            scrollRegionFrame = 0;
            updateScrollableTableRegions();
        });
    }

    function observeTableHeaderSize() {
        if (tableHeaderResizeObserver
                || typeof window.ResizeObserver !== 'function') {
            return;
        }
        var header = document.querySelector('.main-header');
        if (!header) {
            return;
        }
        tableHeaderResizeObserver = new window.ResizeObserver(
            scheduleScrollableTableRegionUpdate
        );
        tableHeaderResizeObserver.observe(header);
    }

    function initializeScrollableTableRegions() {
        updateScrollableTableRegions();
        observeTableHeaderSize();
    }

    if (document.readyState === 'loading') {
        document.addEventListener(
            'DOMContentLoaded',
            initializeScrollableTableRegions,
            { once: true }
        );
    } else {
        initializeScrollableTableRegions();
    }
    window.addEventListener(
        'load', initializeScrollableTableRegions, { once: true });
    window.addEventListener('resize', scheduleScrollableTableRegionUpdate);
}());
