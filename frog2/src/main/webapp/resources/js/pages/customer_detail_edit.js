(function() {
    'use strict';

    var form = document.getElementById('customerDetailForm');
    if (!form) return;

    var customerNameField = typeof form.querySelector === 'function'
        ? form.querySelector('input[name="customerName"]')
        : null;
    var errorSummary = document.getElementById('customerDetailErrorSummary');
    var errorMessage = errorSummary
        ? errorSummary.querySelector('[data-customer-detail-error-message]')
        : null;
    var isDirty = false;
    var isSubmitting = false;

    function showErrorSummary(message) {
        if (!errorSummary) return;
        if (errorMessage) errorMessage.textContent = message;
        errorSummary.hidden = false;
        if (typeof errorSummary.focus === 'function') errorSummary.focus();
    }

    function clearErrorSummary() {
        if (errorSummary) errorSummary.hidden = true;
    }

    function markDirty() {
        isDirty = true;
    }

    form.addEventListener('input', markDirty);
    form.addEventListener('change', markDirty);

    form.addEventListener('submit', function(event) {
        clearErrorSummary();
        var customerName = customerNameField ? customerNameField.value : '';

        if (!customerName || customerName.trim() === '') {
            event.preventDefault();
            window.Frog2UI.showFieldError(
                customerNameField,
                '고객사명이 필요합니다.');
            showErrorSummary('고객사명을 확인해 주세요.');
            return;
        }

        if (!window.Frog2UI.confirmAction('변경사항을 저장하시겠습니까?')) {
            event.preventDefault();
            return;
        }

        isSubmitting = true;
        isDirty = false;
    });

    if (typeof window.addEventListener === 'function') {
        window.addEventListener('beforeunload', function(event) {
            if (!isDirty || isSubmitting) return;
            event.preventDefault();
            event.returnValue = '';
        });
    }

    document.addEventListener('DOMContentLoaded', function() {
        var now = new Date();
        var today = now.getFullYear()
            + '-' + String(now.getMonth() + 1).padStart(2, '0')
            + '-' + String(now.getDate()).padStart(2, '0');
        var dateInputs = document.querySelectorAll('input[type="date"]');

        dateInputs.forEach(function(input) {
            if (input.name === 'createDate' || input.name === 'installDate') {
                input.setAttribute('max', today);
            }
        });
    });
})();
