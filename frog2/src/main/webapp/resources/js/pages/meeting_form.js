(function() {
    'use strict';

    const root = document.querySelector('.meeting-page-container[data-meeting-mode]');
    if (!root) {
        return;
    }

    const mode = root.getAttribute('data-meeting-mode');
    const form = document.getElementById('meetingForm');
    const modal = document.getElementById('previewModal');
    const previewDialog = window.Frog2UI.createDialogController(modal);
    const fields = {
        title: document.getElementById('title'),
        meetingType: document.getElementById('meeting_type'),
        meetingDateTime: document.getElementById('meeting_datetime'),
        content: document.getElementById('content')
    };
    const contentCount = document.getElementById('contentCount');
    const contentTemplate = [
        '참석자',
        '- ',
        '',
        '주요 논의사항',
        '- ',
        '',
        '결정사항',
        '- ',
        '',
        '후속 조치',
        '- 담당자 / 기한: ',
        '',
        '다음 회의',
        '- '
    ].join('\n');
    let isSubmitting = false;
    let originalData;

    document.addEventListener('DOMContentLoaded', initialize);

    function initialize() {
        if (mode === 'write' && !fields.meetingDateTime.value) {
            fields.meetingDateTime.value = currentLocalDateTime();
        }
        updateTitleSuggestion();
        updateContentCount();
        originalData = readFormData();

        document.querySelectorAll('[data-meeting-action="preview"]').forEach(function(button) {
            button.addEventListener('click', previewContent);
        });
        document.querySelectorAll('[data-meeting-action="close-preview"]').forEach(function(button) {
            button.addEventListener('click', closePreview);
        });
        document.querySelectorAll('[data-meeting-action="insert-template"]').forEach(function(button) {
            button.addEventListener('click', insertTemplate);
        });

        fields.meetingType.addEventListener('change', updateTitleSuggestion);
        fields.meetingDateTime.addEventListener('change', updateTitleSuggestion);
        fields.content.addEventListener('input', updateContentCount);

        const deleteButton = document.querySelector('[data-meeting-action="delete"]');
        if (deleteButton) {
            deleteButton.addEventListener('click', confirmDelete);
        }

        modal.addEventListener('click', function(event) {
            if (event.target === modal) {
                closePreview();
            }
        });
        form.addEventListener('submit', handleSubmit);
        window.addEventListener('beforeunload', warnBeforeUnload);
    }

    function currentLocalDateTime() {
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        const hours = String(now.getHours()).padStart(2, '0');
        const minutes = String(now.getMinutes()).padStart(2, '0');
        return year + '-' + month + '-' + day + 'T' + hours + ':' + minutes;
    }

    function validateForm() {
        const requiredFields = [
            [fields.title, '회의 제목을 입력해주세요.'],
            [fields.meetingType, '회의 유형을 선택해주세요.'],
            [fields.meetingDateTime, '회의 일시를 선택해주세요.'],
            [fields.content, '회의 내용을 입력해주세요.']
        ];

        for (const entry of requiredFields) {
            const field = entry[0];
            if (!field.value.trim()) {
                window.Frog2UI.showFieldError(field, entry[1]);
                return false;
            }
        }
        return true;
    }

    function previewContent(event) {
        if (!validateForm()) {
            return;
        }

        document.getElementById('preview-title').textContent = fields.title.value.trim();
        document.querySelector('.preview-type').textContent =
                getTypeLabel(fields.meetingType.value);
        document.querySelector('.preview-datetime').textContent =
                formatDateTime(fields.meetingDateTime.value);
        document.getElementById('preview-content').textContent = fields.content.value.trim();
        previewDialog.open(event.currentTarget);
    }

    function closePreview() {
        previewDialog.close();
    }

    function updateTitleSuggestion() {
        const dateValue = fields.meetingDateTime.value.slice(0, 10);
        const typeValue = fields.meetingType.value;
        if (!dateValue || !typeValue) {
            fields.title.placeholder = '회의 제목을 입력하세요';
            return;
        }
        fields.title.placeholder = dateValue.split('-').join('.')
                + ' ' + getTypeLabel(typeValue);
    }

    function insertTemplate() {
        if (fields.content.value.trim()) {
            window.Frog2UI.notify(
                '작성 중인 내용을 보호하기 위해 빈 입력란에만 기본 틀을 넣을 수 있습니다.',
                'warning');
            fields.content.focus();
            return;
        }
        fields.content.value = contentTemplate;
        updateContentCount();
        fields.content.focus();
        const firstEntryPosition = contentTemplate.indexOf('- ') + 2;
        fields.content.setSelectionRange(firstEntryPosition, firstEntryPosition);
    }

    function updateContentCount() {
        if (contentCount) {
            contentCount.textContent = Array.from(fields.content.value).length + '자';
        }
    }

    function confirmDelete(event) {
        if (window.Frog2UI.confirmAction('정말로 이 회의록을 삭제하시겠습니까?\n삭제된 데이터는 복구할 수 없습니다.')) {
            window.Frog2UI.setButtonLoading(event.currentTarget, true, '삭제 중');
            document.getElementById('deleteForm').requestSubmit();
        }
    }

    function getTypeLabel(type) {
        const typeLabels = {
            'daily': '일일 회의',
            'weekly': '주간 회의',
            'monthly': '월간 회의',
            'project': '프로젝트 회의',
            'emergency': '긴급 회의',
            'other': '기타'
        };
        return typeLabels[type] || type;
    }

    function formatDateTime(dateTimeString) {
        const date = new Date(dateTimeString);
        return date.toLocaleString('ko-KR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    function handleSubmit(event) {
        if (!validateForm()) {
            event.preventDefault();
            return;
        }
        isSubmitting = true;
    }

    function readFormData() {
        return {
            title: fields.title.value,
            meetingType: fields.meetingType.value,
            meetingDateTime: fields.meetingDateTime.value,
            content: fields.content.value
        };
    }

    function warnBeforeUnload(event) {
        let shouldWarn;
        if (mode === 'edit') {
            const currentData = readFormData();
            shouldWarn = Object.keys(originalData).some(function(key) {
                return originalData[key] !== currentData[key];
            });
        } else {
            shouldWarn = Boolean(fields.title.value.trim() || fields.content.value.trim());
        }

        if (!isSubmitting && shouldWarn) {
            event.preventDefault();
            event.returnValue = '';
            return '';
        }
    }
})();
