(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
        const form = document.getElementById('deleteTroubleshootingForm');
        if (!form) {
            return;
        }

        form.addEventListener('submit', function(event) {
            if (!window.confirm(
                    '정말로 이 트러블 슈팅을 삭제하시겠습니까?\n\n'
                    + '삭제된 데이터는 복구할 수 없습니다.')) {
                event.preventDefault();
            }
        });
    });
})();
