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

    // Keep the native link contract so keyboard and modified-click navigation work.
    cards.forEach(card => {
        card.addEventListener('click', function(event) {
            if (event.defaultPrevented || event.button !== 0 || event.metaKey
                    || event.ctrlKey || event.shiftKey || event.altKey) {
                return;
            }
            this.classList.add('is-loading');
        });
    });
});
