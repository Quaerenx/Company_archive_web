(function() {
    'use strict';

    const refreshButton = document.getElementById('pool-refresh');
    if (refreshButton) {
        refreshButton.addEventListener('click', function() {
            window.location.reload();
        });
    }
})();
