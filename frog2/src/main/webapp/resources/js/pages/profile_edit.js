(function() {
    'use strict';

    var form = document.getElementById('profileEditForm');
    var userName = document.getElementById('userName');

    form.addEventListener('submit', function(event) {
        if (!userName.value.trim()) {
            event.preventDefault();
            window.alert('이름을 입력해주세요.');
            userName.focus();
            return;
        }

        if (!window.confirm('프로필을 수정하시겠습니까?')) {
            event.preventDefault();
        }
    });
})();
