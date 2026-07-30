document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.meeting-management tr[data-detail-url]').forEach(function(row) {
        row.addEventListener('click', function(event) {
            if (!event.target.closest('a')) {
                window.location.href = this.dataset.detailUrl;
            }
        });
    });
});