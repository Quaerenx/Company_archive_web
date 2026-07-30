(function() {
    'use strict';

    document.querySelectorAll('.js-vm-host-delete').forEach(function(form) {
        form.addEventListener('submit', function(event) {
            if (!window.confirm('해당 호스트를 삭제하시겠습니까?')) {
                event.preventDefault();
            }
        });
    });
})();
