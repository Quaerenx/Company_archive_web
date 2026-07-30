// 폼 유효성 검사 및 확인
document.querySelector('form').addEventListener('submit', function(e) {
    if (!confirm('고객사 정보를 수정하시겠습니까?')) {
        e.preventDefault();
        return false;
    }
});

// 페이지 로드 시 포커스
document.addEventListener('DOMContentLoaded', function() {
    // 목록 페이지와 동일한 본문 레이아웃 클래스 적용
    document.body.classList.add('page-1050');
    // 첫 번째 편집 가능한 필드에 포커스
    const firstInput = document.querySelector('#first_introduction_year');
    if (firstInput) {
        firstInput.focus();
    }
});
