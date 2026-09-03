(function () {
    'use strict';

    var form = document.getElementById('file-import-form');
    if (!form) {
        return;
    }

    var selectAll = document.getElementById('import-select-all');
    var selectedCount = document.getElementById('import-selected-count');
    var submitButton = document.getElementById('import-submit-button');
    var progress = document.getElementById('import-progress');
    var status = document.getElementById('import-status');
    var resultSection = document.getElementById('import-result');
    var resultSummary = document.getElementById('import-result-summary');
    var resultBody = document.getElementById('import-result-body');
    var retryButton = document.getElementById('import-retry-button');
    var csrfInput = form.querySelector('input[name="_csrf"]');
    var failedPaths = [];

    function selectableInputs() {
        return Array.prototype.slice.call(
            form.querySelectorAll('input[name="selectedPath"]:not(:disabled)'));
    }

    function selectedInputs() {
        return selectableInputs().filter(function (input) {
            return input.checked;
        });
    }

    function updateSelection() {
        var available = selectableInputs();
        var selected = selectedInputs();
        selectedCount.textContent = selected.length + '개 선택';
        submitButton.disabled = selected.length === 0;
        if (selectAll) {
            selectAll.checked = available.length > 0 && selected.length === available.length;
            selectAll.indeterminate = selected.length > 0 && selected.length < available.length;
        }
    }

    function createCell(text, label) {
        var cell = document.createElement('td');
        cell.dataset.label = label;
        cell.textContent = text;
        return cell;
    }

    function renderResult(payload) {
        resultBody.textContent = '';
        failedPaths = [];
        payload.files.forEach(function (file) {
            var row = document.createElement('tr');
            row.appendChild(createCell(file.name, '파일'));
            row.appendChild(createCell(file.label, '상태'));
            row.appendChild(createCell(file.reason, '결과'));
            resultBody.appendChild(row);
            if (file.retryable) {
                failedPaths.push(file.path);
            }
            if (['imported', 'conflict', 'rejected'].indexOf(file.status) >= 0) {
                selectableInputs().forEach(function (input) {
                    if (input.value === file.path) {
                        input.checked = false;
                        input.disabled = true;
                    }
                });
            }
        });
        var summary = payload.summary;
        resultSummary.textContent = '등록 ' + summary.imported
            + '건 · 충돌 ' + summary.conflicts
            + '건 · 거부 ' + summary.rejected
            + '건 · 대기 ' + summary.deferred
            + '건 · 실패 ' + summary.failed + '건';
        retryButton.hidden = failedPaths.length === 0;
        resultSection.hidden = false;
        updateSelection();
        resultSection.scrollIntoView({ behavior: 'auto', block: 'nearest' });
    }

    if (selectAll) {
        selectAll.addEventListener('change', function () {
            selectableInputs().forEach(function (input) {
                input.checked = selectAll.checked;
            });
            updateSelection();
        });
    }

    form.addEventListener('change', function (event) {
        if (event.target.name === 'selectedPath') {
            updateSelection();
        }
    });

    retryButton.addEventListener('click', function () {
        selectableInputs().forEach(function (input) {
            input.checked = failedPaths.indexOf(input.value) >= 0;
        });
        updateSelection();
        form.requestSubmit();
    });

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        if (selectedInputs().length === 0) {
            window.Frog2UI.setStatus(status, '반입할 파일을 선택해 주세요.', 'danger');
            return;
        }
        if (!csrfInput || !csrfInput.value) {
            window.Frog2UI.setStatus(
                status,
                '요청 확인 토큰이 없습니다. 페이지를 새로 고쳐 주세요.',
                'danger');
            return;
        }

        progress.hidden = false;
        progress.removeAttribute('value');
        window.Frog2UI.setButtonLoading(submitButton, true, '반입 중');
        window.Frog2UI.setStatus(status, '선택한 서버 파일을 반입하고 있습니다.', 'info');

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
                throw new Error(result.payload.message || '서버 파일 반입에 실패했습니다.');
            }
            renderResult(result.payload);
            var tone = result.payload.summary.failed > 0 ? 'warning' : 'success';
            window.Frog2UI.setStatus(status, '서버 파일 반입이 끝났습니다.', tone);
        }).catch(function (error) {
            if (window.Frog2Session.isSessionExpired(error)) {
                return;
            }
            window.Frog2UI.setStatus(
                status,
                error.message || '서버 파일 반입에 실패했습니다.',
                'danger');
        }).finally(function () {
            progress.hidden = true;
            window.Frog2UI.setButtonLoading(submitButton, false);
            updateSelection();
        });
    });

    updateSelection();
}());
