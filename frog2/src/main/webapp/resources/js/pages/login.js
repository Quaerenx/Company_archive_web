(function () {
    'use strict';

    var idInput = document.getElementById('userId');
    var remember = document.getElementById('rememberId');
    var form = document.querySelector('form[action="login"]');

    if (!idInput || !remember || !form) {
        return;
    }

    try {
        var saved = window.localStorage.getItem('savedUserId');
        var enabled = window.localStorage.getItem('rememberId') === 'Y';
        if (enabled && saved) {
            idInput.value = saved;
            remember.checked = true;
        }
    } catch (ignore) {
        // Login remains available when browser storage is unavailable.
    }

    form.addEventListener('submit', function () {
        try {
            if (remember.checked) {
                window.localStorage.setItem('rememberId', 'Y');
                window.localStorage.setItem('savedUserId', idInput.value || '');
            } else {
                window.localStorage.removeItem('rememberId');
                window.localStorage.removeItem('savedUserId');
            }
        } catch (ignore) {
            // Storage errors must not block authentication.
        }
    });
}());
