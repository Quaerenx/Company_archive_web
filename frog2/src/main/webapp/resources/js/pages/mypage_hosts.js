(function () {
  'use strict';

  const backdrop = document.getElementById('vmHostModalBackdrop');
  const modal = document.getElementById('vmHostModal');
  const modalTitle = document.getElementById('vmHostModalTitle');
  const openAddBtn = document.getElementById('openVmHostAddBtn');
  const closeBtn = document.getElementById('closeVmHostModalBtn');
  const cancelBtn = document.getElementById('cancelVmHostModalBtn');
  const deleteForm = document.getElementById('vmHostDeleteForm');
  const originalIpInput = document.getElementById('vmHostOriginalIp');
  const ipInput = document.getElementById('vmHostIp');
  const purposeInput = document.getElementById('vmHostPurpose');
  const osInfoInput = document.getElementById('vmHostOsInfo');
  const verticaVersionInput = document.getElementById('vmHostVerticaVersion');
  const remoteHostInput = document.getElementById('vmHostRemoteHost');
  const noteInput = document.getElementById('vmHostNote');
  const deleteIpInput = document.getElementById('vmHostDeleteIp');
  const modalDialog = modal && window.Frog2UI
      ? window.Frog2UI.createDialogController(modal)
      : null;

  function openModal(trigger) {
    if (!backdrop || !modalDialog) {
      return;
    }
    backdrop.hidden = false;
    backdrop.setAttribute('aria-hidden', 'false');
    document.body.classList.add('vm-modal-open');
    modalDialog.open(trigger);
  }

  function closeModal() {
    if (!backdrop || !modalDialog) {
      return;
    }
    modalDialog.close();
    backdrop.hidden = true;
    backdrop.setAttribute('aria-hidden', 'true');
    document.body.classList.remove('vm-modal-open');
  }

  function populateModal(data, isEdit, trigger) {
    if (!modalTitle || !deleteForm) {
      return;
    }
    modalTitle.textContent = isEdit ? '호스트 수정' : '호스트 등록';
    originalIpInput.value = isEdit ? (data.originalIp || data.ip || '') : '';
    ipInput.value = data.ip || '';
    purposeInput.value = data.purpose || '';
    osInfoInput.value = data.osInfo || '';
    verticaVersionInput.value = data.verticaVersion || '';
    remoteHostInput.value = data.remoteHost || '';
    noteInput.value = data.note || '';
    deleteIpInput.value = isEdit ? (data.originalIp || data.ip || '') : '';
    deleteForm.hidden = !isEdit;
    openModal(trigger);
  }

  if (openAddBtn) {
    openAddBtn.addEventListener('click', function () {
      populateModal({}, false, openAddBtn);
    });
  }

  document.querySelectorAll('.vm-edit-btn').forEach(function (button) {
    button.addEventListener('click', function () {
      populateModal({
        ip: button.dataset.ip || '',
        originalIp: button.dataset.ip || '',
        purpose: button.dataset.purpose || '',
        osInfo: button.dataset.osInfo || '',
        verticaVersion: button.dataset.verticaVersion || '',
        remoteHost: button.dataset.remoteHost || '',
        note: button.dataset.note || ''
      }, true, button);
    });
  });

  if (closeBtn) {
    closeBtn.addEventListener('click', closeModal);
  }
  if (cancelBtn) {
    cancelBtn.addEventListener('click', closeModal);
  }

  if (deleteForm) {
    deleteForm.addEventListener('submit', function (event) {
      if (!window.Frog2UI.confirmAction('해당 호스트를 삭제하시겠습니까?')) {
        event.preventDefault();
      }
    });
  }

  if (backdrop) {
    backdrop.addEventListener('click', function (event) {
      if (event.target === backdrop) {
        closeModal();
      }
    });
  }
  if (modal) {
    modal.addEventListener('keydown', function (event) {
      if (event.key === 'Escape') {
        event.preventDefault();
        event.stopPropagation();
        closeModal();
      }
    });
  }

  const formSeed = document.getElementById('vmHostFormSeed');
  if (formSeed) {
    populateModal({
      ip: formSeed.dataset.ip || '',
      originalIp: formSeed.dataset.originalIp || '',
      purpose: formSeed.dataset.purpose || '',
      osInfo: formSeed.dataset.osInfo || '',
      verticaVersion: formSeed.dataset.verticaVersion || '',
      remoteHost: formSeed.dataset.remoteHost || '',
      note: formSeed.dataset.note || ''
    }, Boolean(formSeed.dataset.originalIp), openAddBtn);
  }
}());
