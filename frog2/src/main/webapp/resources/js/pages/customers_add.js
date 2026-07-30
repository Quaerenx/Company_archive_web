// 현재 연도 설정
document.addEventListener('DOMContentLoaded', function() {
    const currentYear = new Date().getFullYear();
    const yearInput = document.getElementById('first_introduction_year');
    if (yearInput && !yearInput.value) {
        yearInput.value = currentYear;
    }
});

// 폼 유효성 검사
const customerForm = document.querySelector('.customer-form-page form.ui-form');
if (customerForm) {
    customerForm.addEventListener('submit', function(e) {
        const customerNameField = document.getElementById('customer_name');
        const customerName = customerNameField.value.trim();

        if (!customerName) {
            e.preventDefault();
            window.Frog2UI.showFieldError(
                customerNameField,
                '고객사명은 필수 입력 항목입니다.');
        }
    });
}
