(function() {
    'use strict';

    const root = document.querySelector('.meeting-view[data-context-path][data-meeting-id]');
    if (!root) {
        return;
    }

    const commentEndpoint = root.getAttribute('data-context-path') + '/comment';
    const meetingId = root.getAttribute('data-meeting-id');

    document.querySelectorAll('.comment-item[data-comment-id]').forEach(function(item) {
        const commentId = item.getAttribute('data-comment-id');
        const editButton = item.querySelector('.comment-btn.edit');
        const deleteButton = item.querySelector('.comment-btn.delete');
        const saveButton = item.querySelector('.btn-save');
        const cancelButton = item.querySelector('.btn-cancel-edit');

        if (editButton) {
            editButton.addEventListener('click', function() {
                editComment(commentId, editButton);
            });
        }
        if (deleteButton) {
            deleteButton.addEventListener('click', function() {
                deleteComment(commentId, deleteButton);
            });
        }
        saveButton.addEventListener('click', function() {
            saveComment(commentId, saveButton);
        });
        cancelButton.addEventListener('click', function() {
            cancelEdit(commentId, editButton);
        });
    });

    document.getElementById('commentForm').addEventListener('submit', function(event) {
        event.preventDefault();

        const content = document.getElementById('commentContent').value.trim();
        if (!content) {
            window.Frog2UI.showFieldError(
                document.getElementById('commentContent'),
                '댓글 내용을 입력해주세요.');
            return;
        }

        const submitButton = this.querySelector('.btn-comment');
        setSubmitState(submitButton, true);
        postComment(
                'add',
                {meeting_id: meetingId, content: content},
                '댓글 등록 중 오류가 발생했습니다.')
            .finally(function() {
                setSubmitState(submitButton, false);
            });
    });

    function editComment(commentId, editButton) {
        const content = document.getElementById('content-' + commentId);
        const editForm = document.getElementById('edit-form-' + commentId);
        content.hidden = true;
        editForm.hidden = false;
        editForm.classList.add('is-open');
        editButton.setAttribute('aria-expanded', 'true');
        document.getElementById('edit-content-' + commentId).focus();
    }

    function cancelEdit(commentId, editButton) {
        const content = document.getElementById('content-' + commentId);
        const editForm = document.getElementById('edit-form-' + commentId);
        content.hidden = false;
        editForm.hidden = true;
        editForm.classList.remove('is-open');
        if (editButton) {
            editButton.setAttribute('aria-expanded', 'false');
            editButton.focus();
        }
    }

    function saveComment(commentId, button) {
        const newContent = document.getElementById('edit-content-' + commentId).value.trim();
        if (!newContent) {
            window.Frog2UI.showFieldError(
                document.getElementById('edit-content-' + commentId),
                '댓글 내용을 입력해주세요.');
            return;
        }

        window.Frog2UI.setButtonLoading(button, true, '저장 중');
        postComment(
                'update',
                {comment_id: commentId, content: newContent},
                '댓글 수정 중 오류가 발생했습니다.')
            .finally(function() {
                window.Frog2UI.setButtonLoading(button, false);
            });
    }

    function deleteComment(commentId, button) {
        if (!window.Frog2UI.confirmAction('정말로 이 댓글을 삭제하시겠습니까?')) {
            return;
        }

        window.Frog2UI.setButtonLoading(button, true, '삭제 중');
        postComment(
                'delete',
                {comment_id: commentId},
                '댓글 삭제 중 오류가 발생했습니다.')
            .finally(function() {
                window.Frog2UI.setButtonLoading(button, false);
            });
    }

    function postComment(action, values, fallbackMessage) {
        const parameters = new URLSearchParams();
        parameters.set('_csrf', window.Frog2Csrf.token());
        parameters.set('action', action);
        Object.keys(values).forEach(function(key) {
            parameters.set(key, values[key]);
        });

        return fetch(commentEndpoint, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: parameters.toString()
        })
            .then(function(response) {
                return response.json()
                    .catch(function() {
                        return {};
                    })
                    .then(function(payload) {
                        if (!response.ok) {
                            throw new Error(payload.message || fallbackMessage);
                        }
                        return payload;
                    });
            })
            .then(function(data) {
                if (data.success) {
                    if (action === 'add') {
                        window.location.assign(
                            root.getAttribute('data-context-path')
                                + '/meeting?view=view&id='
                                + encodeURIComponent(meetingId)
                                + '#comments');
                        return;
                    }
                    window.location.reload();
                    return;
                }
                window.Frog2UI.notify(
                    data.message || fallbackMessage,
                    'danger',
                    { persistent: true });
            })
            .catch(function(error) {
                window.Frog2UI.notify(
                    error.message || fallbackMessage,
                    'danger',
                    { persistent: true });
            });
    }

    function setSubmitState(button, loading) {
        window.Frog2UI.setButtonLoading(button, loading, '등록 중');
    }
})();
