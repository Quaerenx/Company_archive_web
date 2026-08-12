(function() {
    'use strict';

    function copyWithSelection(target) {
        var selection = window.getSelection();
        if (!selection || typeof document.execCommand !== 'function') {
            return false;
        }

        var previousRanges = [];
        for (var index = 0; index < selection.rangeCount; index += 1) {
            previousRanges.push(selection.getRangeAt(index).cloneRange());
        }

        var range = document.createRange();
        range.selectNodeContents(target);
        selection.removeAllRanges();
        selection.addRange(range);

        var copied = false;
        try {
            copied = document.execCommand('copy');
        } finally {
            selection.removeAllRanges();
            previousRanges.forEach(function(previousRange) {
                selection.addRange(previousRange);
            });
        }
        return copied;
    }

    function initScriptCopy() {
        var button = document.querySelector('[data-copy-target]');
        if (!button) {
            return;
        }

        var target = document.getElementById(button.dataset.copyTarget);
        var label = button.querySelector('[data-copy-label]');
        if (!target || !label) {
            return;
        }

        button.addEventListener('click', function() {
            var text = target.textContent || '';
            var copyOperation;
            if (window.isSecureContext && navigator.clipboard) {
                copyOperation = navigator.clipboard.writeText(text);
            } else {
                copyOperation = copyWithSelection(target)
                    ? Promise.resolve()
                    : Promise.reject(new Error('Clipboard unavailable'));
            }

            copyOperation.then(function() {
                label.textContent = '복사됨';
                window.Frog2UI.announce('스크립트를 복사했습니다.');
                window.setTimeout(function() {
                    label.textContent = '복사';
                }, 1600);
            }).catch(function() {
                window.Frog2UI.notify(
                    '스크립트를 복사하지 못했습니다.', 'danger');
            });
        });
    }

    document.addEventListener('DOMContentLoaded', function() {
        initScriptCopy();

        const form = document.getElementById('deleteTroubleshootingForm');
        if (!form) {
            return;
        }

        form.addEventListener('submit', function(event) {
            if (!window.Frog2UI.confirmAction(
                    '정말로 이 트러블 슈팅을 삭제하시겠습니까?\n\n'
                    + '삭제된 데이터는 복구할 수 없습니다.')) {
                event.preventDefault();
            }
        });
    });
})();
