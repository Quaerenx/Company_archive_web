(function() {
    'use strict';

    const page = document.querySelector('.monthly-response-page');
    const config = document.getElementById('monthlyResponseConfig');
    if (!page || !config) {
        return;
    }

    const selectedYear = Number(config.dataset.year);
    const selectedMonth = Number(config.dataset.month);
    const filterForm = document.getElementById('filterForm');
    const responseForm = document.getElementById('responseForm');
    const modal = document.getElementById('responseModal');

    document.addEventListener('DOMContentLoaded', initialize);

    function initialize() {
        page.querySelectorAll('[data-monthly-action="add"]').forEach(function(button) {
            button.addEventListener('click', openAddModal);
        });
        page.querySelectorAll('[data-monthly-auto-submit]').forEach(function(select) {
            select.addEventListener('change', submitFilter);
        });
        page.querySelectorAll('.btn-edit').forEach(function(button) {
            button.addEventListener('click', function() {
                const row = button.closest('tr');
                openEditModal(
                    row.dataset.responseId,
                    row.querySelector('.response-date').value,
                    row.querySelector('.response-customer-name').value,
                    row.querySelector('.response-reason').value,
                    row.querySelector('.response-action-content').value,
                    row.querySelector('.response-note').value
                );
            });
        });
        page.querySelectorAll('.btn-delete').forEach(function(button) {
            button.addEventListener('click', function() {
                deleteResponse(button.closest('tr').dataset.responseId);
            });
        });
        page.querySelectorAll('[data-monthly-action="close"]').forEach(function(button) {
            button.addEventListener('click', closeModal);
        });
        modal.addEventListener('click', function(event) {
            if (event.target === modal) {
                closeModal();
            }
        });
        document.addEventListener('keydown', function(event) {
            if (event.key === 'Escape') {
                closeModal();
            }
        });
    }

    function submitFilter() {
        filterForm.submit();
    }

    function getDefaultResponseDate() {
        const today = new Date();
        const day = today.getFullYear() === selectedYear
                && today.getMonth() + 1 === selectedMonth
            ? today.getDate()
            : 1;
        return selectedYear
            + '-' + String(selectedMonth).padStart(2, '0')
            + '-' + String(day).padStart(2, '0');
    }

    function openAddModal() {
        responseForm.reset();
        document.getElementById('modalTitle').textContent = '고객 응대 추가';
        document.getElementById('formAction').value = 'addResponse';
        document.getElementById('responseId').value = '';
        document.getElementById('responseDate').value = getDefaultResponseDate();
        modal.classList.add('show');
    }

    function openEditModal(id, date, customerName, reason, actionContent, note) {
        document.getElementById('modalTitle').textContent = '고객 응대 수정';
        document.getElementById('formAction').value = 'updateResponse';
        document.getElementById('responseId').value = id;
        document.getElementById('responseDate').value = date;
        document.getElementById('customerName').value = customerName;
        document.getElementById('reason').value = reason;
        document.getElementById('actionContent').value = actionContent;
        document.getElementById('note').value = note;
        modal.classList.add('show');
    }

    function closeModal() {
        modal.classList.remove('show');
    }

    function deleteResponse(id) {
        if (!window.confirm('정말로 이 응대 기록을 삭제하시겠습니까?')) {
            return;
        }

        const form = document.createElement('form');
        form.method = 'POST';
        form.action = responseForm.action;
        form.hidden = true;
        appendHiddenInput(form, 'formAction', 'deleteResponse');
        window.Frog2Csrf.appendTo(form);
        appendHiddenInput(form, 'responseId', id);
        appendHiddenInput(form, 'year', String(selectedYear));
        appendHiddenInput(form, 'month', String(selectedMonth));
        document.body.appendChild(form);
        form.submit();
    }

    function appendHiddenInput(form, name, value) {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = name;
        input.value = value;
        form.appendChild(input);
    }
})();
