(function () {
    'use strict';

    var root = document.querySelector('[data-work-inbox]');
    if (!root) return;

    var form = root.querySelector('[data-work-inbox-filter]');
    var items = Array.prototype.slice.call(
        root.querySelectorAll('[data-work-inbox-item]'));
    var count = root.querySelector('[data-work-inbox-visible-count]');
    var empty = root.querySelector('[data-filter-empty]');
    var userId = root.getAttribute('data-user-id') || 'anonymous';
    var storageKey = 'frog2.workInbox.deferrals.v1:' + userId;
    var deferrals = readDeferrals();

    function readDeferrals() {
        try {
            var parsed = JSON.parse(window.localStorage.getItem(storageKey) || '{}');
            return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
                ? parsed
                : {};
        } catch (error) {
            return {};
        }
    }

    function writeDeferrals() {
        try {
            window.localStorage.setItem(storageKey, JSON.stringify(deferrals));
        } catch (error) {
            // Filtering still works when browser storage is unavailable.
        }
    }

    function isoToday() {
        var now = new Date();
        return [
            now.getFullYear(),
            String(now.getMonth() + 1).padStart(2, '0'),
            String(now.getDate()).padStart(2, '0')
        ].join('-');
    }

    function defaultUntil() {
        var date = new Date();
        date.setDate(date.getDate() + 7);
        return [
            date.getFullYear(),
            String(date.getMonth() + 1).padStart(2, '0'),
            String(date.getDate()).padStart(2, '0')
        ].join('-');
    }

    function validIsoDate(value) {
        if (!/^\d{4}-\d{2}-\d{2}$/.test(String(value || ''))) {
            return false;
        }
        var parts = value.split('-').map(Number);
        var date = new Date(parts[0], parts[1] - 1, parts[2]);
        return date.getFullYear() === parts[0]
            && date.getMonth() === parts[1] - 1
            && date.getDate() === parts[2];
    }

    function isDeferred(item) {
        var value = deferrals[item.getAttribute('data-item-key')];
        return Boolean(value && validIsoDate(value.until)
            && value.until >= isoToday());
    }

    function pruneDeferrals() {
        var keys = new Set(items.map(function (item) {
            return item.getAttribute('data-item-key');
        }));
        var changed = false;
        Object.keys(deferrals).forEach(function (key) {
            if (!keys.has(key) || !deferrals[key]
                    || !validIsoDate(deferrals[key].until)
                    || deferrals[key].until < isoToday()) {
                delete deferrals[key];
                changed = true;
            }
        });
        if (changed) writeDeferrals();
    }

    function updateItemState(item) {
        var key = item.getAttribute('data-item-key');
        var value = deferrals[key];
        var deferred = isDeferred(item);
        var copy = item.querySelector('[data-deferred-copy]');
        var panel = item.querySelector('[data-defer-panel]');
        var resume = item.querySelector('[data-resume]');
        item.dataset.deferred = deferred ? 'true' : 'false';
        if (copy) {
            copy.hidden = !deferred;
            copy.textContent = deferred
                ? '보류: ' + value.reason + ' · ' + value.until + '까지'
                : '';
        }
        if (panel) {
            panel.hidden = deferred;
            var until = panel.querySelector('[name="until"]');
            if (until && !until.value) until.value = defaultUntil();
        }
        if (resume) resume.hidden = !deferred;
    }

    function applyFilters() {
        var data = new FormData(form);
        var severity = data.get('severity') || 'all';
        var type = data.get('type') || 'all';
        var status = data.get('status') || 'active';
        var customer = String(data.get('customer') || '').trim().toLowerCase();
        var visible = 0;
        items.forEach(function (item) {
            var deferred = isDeferred(item);
            var matches = (severity === 'all' || item.dataset.severity === severity)
                && (type === 'all' || item.dataset.type === type)
                && (status === 'all'
                    || (status === 'deferred' ? deferred : !deferred))
                && (!customer || item.dataset.customer.toLowerCase().includes(customer));
            item.hidden = !matches;
            if (matches) visible += 1;
        });
        if (count) count.textContent = String(visible);
        if (empty) empty.hidden = visible !== 0;
    }

    pruneDeferrals();
    items.forEach(function (item) {
        updateItemState(item);
        var deferForm = item.querySelector('[data-defer-form]');
        var resume = item.querySelector('[data-resume]');
        if (deferForm) {
            deferForm.addEventListener('submit', function (event) {
                event.preventDefault();
                var data = new FormData(deferForm);
                var reason = String(data.get('reason') || '').trim();
                var until = String(data.get('until') || '');
                if (!reason || !validIsoDate(until)
                        || until < isoToday()) return;
                deferrals[item.getAttribute('data-item-key')] = {
                    reason: reason,
                    until: until,
                    createdAt: new Date().toISOString()
                };
                writeDeferrals();
                updateItemState(item);
                applyFilters();
            });
        }
        if (resume) {
            resume.addEventListener('click', function () {
                delete deferrals[item.getAttribute('data-item-key')];
                writeDeferrals();
                updateItemState(item);
                applyFilters();
            });
        }
    });

    form.addEventListener('input', applyFilters);
    form.addEventListener('change', applyFilters);
    form.addEventListener('reset', function () {
        window.setTimeout(applyFilters, 0);
    });
    applyFilters();
}());
