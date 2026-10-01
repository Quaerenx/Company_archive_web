document.addEventListener('DOMContentLoaded', function() {
    const monthForm = document.querySelector('.maintenance-month-form');
    const monthInput = document.getElementById('maintenanceMonth');
    if (monthForm && monthInput) {
        const initialMonth = monthInput.value;
        monthInput.addEventListener('change', function() {
            if (this.value !== initialMonth && this.checkValidity()) {
                monthForm.requestSubmit();
            }
        });
    }

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

    window.addEventListener('pageshow', function(event) {
        if (event.persisted && monthInput) {
            monthInput.value = monthInput.defaultValue;
        }
        cards.forEach(card => card.classList.remove('is-loading'));
    });
});
