document.addEventListener('DOMContentLoaded', function() {
    // 카드 호버 효과 강화
    const cards = document.querySelectorAll('.customer-card');

    cards.forEach(card => {
        card.addEventListener('mouseenter', function() {
            this.classList.add('is-hovered');
        });

        card.addEventListener('mouseleave', function() {
            this.classList.remove('is-hovered');
        });
    });

    // 카드 클릭 시 로딩 효과
    cards.forEach(card => {
        card.addEventListener('click', function() {
            this.classList.add('is-loading');
            window.location.href = this.dataset.detailUrl;
        });
    });
});