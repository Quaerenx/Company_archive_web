(function() {
    'use strict';

    var form = document.getElementById('profileEditForm');
    var userName = document.getElementById('userName');

    form.addEventListener('submit', function(event) {
        if (!userName.value.trim()) {
            event.preventDefault();
            window.Frog2UI.showFieldError(userName, '이름을 입력해주세요.');
            return;
        }

        if (!window.Frog2UI.confirmAction('프로필을 수정하시겠습니까?')) {
            event.preventDefault();
        }
    });
})();
