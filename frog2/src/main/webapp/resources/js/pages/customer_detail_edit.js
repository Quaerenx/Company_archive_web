// 폼 제출 시 유효성 검사
document.getElementById('customerDetailForm').addEventListener('submit', function(e) {
    var customerName = document.querySelector('input[name="customerName"]').value;

    if (!customerName || customerName.trim() === '') {
        e.preventDefault();
        window.Frog2UI.showFieldError(
            document.querySelector('input[name="customerName"]'),
            '고객사명이 필요합니다.');
        return;
    }

    // 저장 확인
    if (!window.Frog2UI.confirmAction('변경사항을 저장하시겠습니까?')) {
        e.preventDefault();
        return false;
    }

    return true;
});

// 날짜 필드 제한 (미래 날짜 제한)
document.addEventListener('DOMContentLoaded', function() {
    var today = new Date().toISOString().split('T')[0];
    var dateInputs = document.querySelectorAll('input[type="date"]');

    dateInputs.forEach(function(input) {
        if (input.name === 'createDate' || input.name === 'installDate') {
            input.setAttribute('max', today);
        }
    });
});
