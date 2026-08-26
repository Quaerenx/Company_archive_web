(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        var deleteForm = document.querySelector('[data-customer-history-delete]');
        if (!deleteForm) {
            return;
        }
        deleteForm.addEventListener('submit', function (event) {
            if (!window.Frog2UI
                    || !window.Frog2UI.confirmAction(
                        '이 고객사 히스토리를 삭제하시겠습니까?')) {
                event.preventDefault();
            }
        });
    });
}());
