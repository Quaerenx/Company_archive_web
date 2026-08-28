(function () {
    'use strict';

    var form = document.getElementById('file-upload-form');
    var input = document.getElementById('upload-files');
    var selected = document.getElementById('selected-files');
    var status = document.getElementById('upload-status');
    var button = document.getElementById('upload-button');
    var csrfInput = form.querySelector('input[name="_csrf"]');
    var maxFiles = 5;
    var maxFileSize = 10 * 1024 * 1024;

    function setFileState(state, label) {
        selected.querySelectorAll('.file-item').forEach(function (item) {
            item.dataset.fileState = state;
            var stateLabel = item.querySelector('.file-state');
            if (stateLabel) {
                stateLabel.textContent = label;
            }
        });
    }

    function renderFiles() {
        selected.textContent = '';
        Array.prototype.forEach.call(input.files, function (file) {
            var item = document.createElement('div');
            item.className = 'file-item';
            item.dataset.fileState = 'queued';
            var summary = document.createElement('span');
            summary.className = 'file-item__summary';
            var stateMark = document.createElement('span');
            stateMark.className = 'file-state-mark';
            stateMark.setAttribute('aria-hidden', 'true');
            var name = document.createElement('span');
            name.className = 'file-name';
            name.textContent = file.name;
            summary.appendChild(stateMark);
            summary.appendChild(name);
            var meta = document.createElement('span');
            meta.className = 'file-item__meta';
            var size = document.createElement('span');
            size.className = 'file-size';
            size.textContent = (file.size / 1024 / 1024).toFixed(2) + ' MB';
            var fileState = document.createElement('span');
            fileState.className = 'file-state';
            fileState.textContent = '대기';
            meta.appendChild(size);
            meta.appendChild(fileState);
            item.appendChild(summary);
            item.appendChild(meta);
            selected.appendChild(item);
        });
    }

    input.addEventListener('change', renderFiles);

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        window.Frog2UI.setStatus(status, '', 'neutral');

        if (!csrfInput || !csrfInput.value) {
            window.Frog2UI.setStatus(
                status,
                '요청 확인 토큰이 없습니다. 페이지를 새로 고쳐 주세요.',
                'danger');
            return;
        }
        if (input.files.length < 1 || input.files.length > maxFiles) {
            window.Frog2UI.setStatus(
                status,
                '1개 이상 5개 이하의 파일을 선택해 주세요.',
                'danger');
            return;
        }
        for (var i = 0; i < input.files.length; i += 1) {
            if (input.files[i].size < 1 || input.files[i].size > maxFileSize) {
                window.Frog2UI.setStatus(
                    status,
                    '각 파일은 1 byte 이상 10 MB 이하여야 합니다.',
                    'danger');
                return;
            }
        }

        window.Frog2UI.setButtonLoading(button, true, '업로드 중');
        setFileState('uploading', '업로드 중');
        window.Frog2UI.setStatus(status, '업로드 중입니다.', 'info');
        fetch(form.action, {
            method: 'POST',
            body: new FormData(form),
            credentials: 'same-origin',
            headers: {
                'Accept': 'application/json',
                'X-CSRF-Token': csrfInput.value
            }
        }).then(function (response) {
            return response.json().catch(function () {
                return { status: 'error', message: '서버 응답을 해석할 수 없습니다.' };
            }).then(function (payload) {
                return { response: response, payload: payload };
            });
        }).then(function (result) {
            window.Frog2Session.requireActiveSession(result.response);
            if (!result.response.ok || result.payload.status !== 'ok') {
                throw new Error(result.payload.message || '업로드에 실패했습니다.');
            }
            window.Frog2UI.setStatus(
                status,
                result.payload.files.length + '개 파일을 업로드했습니다.',
                'success');
            setFileState('complete', '완료');
            window.Frog2UI.setButtonSuccess(button, '완료', 900);
            window.setTimeout(function () {
                window.location.assign(form.dataset.successUrl);
            }, 700);
        }).catch(function (error) {
            if (window.Frog2Session.isSessionExpired(error)) {
                setFileState('queued', '대기');
                window.Frog2UI.setButtonLoading(button, false);
                return;
            }
            window.Frog2UI.setStatus(
                status,
                error.message || '업로드에 실패했습니다.',
                'danger');
            setFileState('error', '오류');
            window.Frog2UI.setButtonLoading(button, false);
        });
    });
}());
