(function () {
    'use strict';

    var STORAGE_KEY = 'archive.login.rememberedUserId.v1';
    var STORAGE_LIFETIME_MS = 90 * 24 * 60 * 60 * 1000;
    var form = document.getElementById('loginForm');
    var userIdInput = document.getElementById('userId');
    var rememberIdInput = document.getElementById('rememberId');

    if (!form || !userIdInput || !rememberIdInput) {
        return;
    }

    function removeRememberedUserId() {
        window.localStorage.removeItem(STORAGE_KEY);
    }

    function readRememberedUserId() {
        try {
            var rawValue = window.localStorage.getItem(STORAGE_KEY);
            if (!rawValue) {
                return '';
            }

            var storedValue = JSON.parse(rawValue);
            if (!storedValue
                    || typeof storedValue.userId !== 'string'
                    || typeof storedValue.expiresAt !== 'number'
                    || storedValue.expiresAt <= Date.now()
                    || !storedValue.userId.trim()) {
                removeRememberedUserId();
                return '';
            }

            return storedValue.userId.trim();
        } catch (error) {
            return '';
        }
    }

    function storeRememberedUserId() {
        try {
            if (!rememberIdInput.checked) {
                removeRememberedUserId();
                return;
            }

            var userId = userIdInput.value.trim();
            if (!userId) {
                removeRememberedUserId();
                return;
            }

            window.localStorage.setItem(STORAGE_KEY, JSON.stringify({
                userId: userId,
                expiresAt: Date.now() + STORAGE_LIFETIME_MS
            }));
        } catch (error) {
            // Storage may be unavailable; login must remain usable.
        }
    }

    var rememberedUserId = readRememberedUserId();
    if (rememberedUserId) {
        if (!userIdInput.value) {
            userIdInput.value = rememberedUserId;
        }
        rememberIdInput.checked = true;
    }

    rememberIdInput.addEventListener('change', function () {
        if (!rememberIdInput.checked) {
            try {
                removeRememberedUserId();
            } catch (error) {
                // Storage may be unavailable; login must remain usable.
            }
        }
    });
    form.addEventListener('submit', storeRememberedUserId);
}());
