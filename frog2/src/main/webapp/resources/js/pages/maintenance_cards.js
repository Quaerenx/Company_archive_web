document.addEventListener('DOMContentLoaded', function() {
    const cards = document.querySelectorAll('.customer-card');

    // Keep the native link contract so keyboard and modified-click navigation work.
    cards.forEach(card => {
        card.addEventListener('click', function(event) {
            if (event.defaultPrevented || event.button !== 0 || event.metaKey
                    || event.ctrlKey || event.shiftKey || event.altKey) {
                return;
            }
            this.classList.add('is-loading');
        });
    });
});
