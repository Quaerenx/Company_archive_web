(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
        document.querySelectorAll(
            '.troubleshooting-management .troubleshooting-table tbody tr[data-detail-url]')
            .forEach(function(row) {
                row.addEventListener('click', function(event) {
                    if (!event.target.closest('a')) {
                        window.location.href = row.dataset.detailUrl;
                    }
                });
            });
    });
})();
