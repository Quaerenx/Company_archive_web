(function() {
    'use strict';

    var form = document.getElementById('passwordChangeForm');
    var currentPassword = document.getElementById('currentPassword');
    var newPassword = document.getElementById('newPassword');
    var confirmPassword = document.getElementById('confirmPassword');

    form.addEventListener('submit', function(event) {
        if (!currentPassword.value) {
            event.preventDefault();
            window.alert('현재 비밀번호를 입력해주세요.');
            currentPassword.focus();
            return;
        }

        if (newPassword.value.length < 8) {
            event.preventDefault();
            window.alert('새 비밀번호는 최소 8자 이상이어야 합니다.');
            newPassword.focus();
            return;
        }

        if (newPassword.value !== confirmPassword.value) {
            event.preventDefault();
            window.alert('새 비밀번호가 일치하지 않습니다.');
            confirmPassword.focus();
            return;
        }

        if (currentPassword.value === newPassword.value) {
            event.preventDefault();
            window.alert('현재 비밀번호와 새 비밀번호가 동일합니다.\n다른 비밀번호를 입력해주세요.');
            newPassword.focus();
            return;
        }

        if (!window.confirm('비밀번호를 변경하시겠습니까?')) {
            event.preventDefault();
        }
    });
})();
