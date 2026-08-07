(function() {
    'use strict';

    const page = document.querySelector(
        '.troubleshooting-form-page[data-troubleshooting-form-mode]');
    if (!page) {
        return;
    }

    const form = document.getElementById('troubleshootingForm');
    const title = document.getElementById('title');
    const customerName = document.getElementById('customer_name');
    const mode = page.dataset.troubleshootingFormMode;

    document.addEventListener('DOMContentLoaded', initialize);

    function initialize() {
        if (mode === 'add') {
            setDefaultOccurrenceDate();
        } else if (mode === 'edit') {
            title.focus();
        }
        form.addEventListener('submit', validateAndConfirm);
    }

    function setDefaultOccurrenceDate() {
        const occurrenceDate = document.getElementById('occurrence_date');
        if (occurrenceDate && !occurrenceDate.value) {
            const today = new Date();
            const year = today.getFullYear();
            const month = String(today.getMonth() + 1).padStart(2, '0');
            const day = String(today.getDate()).padStart(2, '0');
            occurrenceDate.value = year + '-' + month + '-' + day;
        }
    }

    function validateAndConfirm(event) {
        if (!title.value.trim()) {
            event.preventDefault();
            window.Frog2UI.showFieldError(title, '제목은 필수 입력 항목입니다.');
            return;
        }

        if (!customerName.value) {
            event.preventDefault();
            window.Frog2UI.showFieldError(
                customerName,
                '고객사는 필수 선택 항목입니다.');
            return;
        }

        if (mode === 'edit'
                && !window.Frog2UI.confirmAction('트러블 슈팅 정보를 수정하시겠습니까?')) {
            event.preventDefault();
        }
    }
})();
