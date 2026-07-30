(function () {
  'use strict';

  const backdrop = document.getElementById('vmHostModalBackdrop');
  const modal = document.getElementById('vmHostModal');
  const boardBody = document.getElementById('vmHostBoardBody');
  const toggleBoardBtn = document.getElementById('toggleVmHostBoardBtn');
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
  const collapseStorageKey = 'frog2.dashboard.personal-hosts.collapsed';
  const maintenanceBody = document.getElementById('maintenanceMonthBoardBody');
  const toggleMaintenanceBtn =
      document.getElementById('toggleMaintenanceBoardBtn');
  const maintenanceCollapseStorageKey =
      'frog2.dashboard.monthly-maintenance.collapsed';
  const kpiLinks =
      Array.from(document.querySelectorAll('.maintenance-kpi-link'));
  const maintenanceCards =
      Array.from(document.querySelectorAll('.maintenance-record-card'));
  const filterBar = document.getElementById('maintenanceFilterBar');
  const filterStatus = document.getElementById('maintenanceFilterStatus');
  const resetFilterBtn =
      document.getElementById('resetMaintenanceFilterBtn');
  const filteredEmpty =
      document.getElementById('maintenanceFilteredEmpty');
  const loadingState = document.getElementById('maintenanceLoadingState');
  let previouslyFocusedElement = null;

  function writeCollapsePreference(key, collapsed) {
    try {
      window.localStorage.setItem(key, collapsed ? 'true' : 'false');
    } catch (ignore) {
      // The dashboard remains usable when browser storage is unavailable.
    }
  }

  function readCollapsePreference(key, defaultValue) {
    try {
      const stored = window.localStorage.getItem(key);
      return stored === null ? defaultValue : stored === 'true';
    } catch (ignore) {
      return defaultValue;
    }
  }

  function setBoardCollapsed(collapsed) {
    if (!boardBody || !toggleBoardBtn) {
      return;
    }
    boardBody.classList.toggle('is-collapsed', collapsed);
    toggleBoardBtn.textContent = collapsed ? '펼치기' : '접기';
    toggleBoardBtn.setAttribute('aria-expanded', String(!collapsed));
    writeCollapsePreference(collapseStorageKey, collapsed);
  }

  function setMaintenanceCollapsed(collapsed) {
    if (!maintenanceBody || !toggleMaintenanceBtn) {
      return;
    }
    maintenanceBody.classList.toggle('is-collapsed', collapsed);
    toggleMaintenanceBtn.textContent = collapsed ? '펼치기' : '접기';
    toggleMaintenanceBtn.setAttribute('aria-expanded', String(!collapsed));
    writeCollapsePreference(maintenanceCollapseStorageKey, collapsed);
  }

  function focusableElements() {
    if (!modal) {
      return [];
    }
    return Array.from(modal.querySelectorAll(
        'a[href], button:not([disabled]), input:not([disabled]):not([type="hidden"]), ' +
        'select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
    )).filter(function (element) {
      return !element.hidden
          && !element.closest('[hidden]')
          && element.getAttribute('aria-hidden') !== 'true';
    });
  }

  function openModal(trigger) {
    if (!backdrop || !modal) {
      return;
    }
    previouslyFocusedElement = trigger || document.activeElement;
    backdrop.hidden = false;
    backdrop.setAttribute('aria-hidden', 'false');
    document.body.classList.add('vm-modal-open');
    window.requestAnimationFrame(function () {
      if (ipInput) {
        ipInput.focus();
      } else {
        modal.focus();
      }
    });
  }

  function closeModal() {
    if (!backdrop) {
      return;
    }
    backdrop.hidden = true;
    backdrop.setAttribute('aria-hidden', 'true');
    document.body.classList.remove('vm-modal-open');
    if (previouslyFocusedElement
        && document.documentElement.contains(previouslyFocusedElement)) {
      previouslyFocusedElement.focus();
    }
    previouslyFocusedElement = null;
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

  function matchesMaintenanceFilter(card, status) {
    const scheduleStatus = card.dataset.maintenanceStatus;
    const licenseRisk = card.dataset.licenseRisk === 'true';
    if (status === 'attention') {
      return scheduleStatus === 'due' || licenseRisk;
    }
    if (status === 'license-risk') {
      return licenseRisk;
    }
    return scheduleStatus === status;
  }

  function filterLabel(status) {
    if (status === 'attention') {
      return '확인 필요';
    }
    if (status === 'license-risk') {
      return '라이선스 위험';
    }
    if (status === 'due') {
      return '예정';
    }
    return '완료';
  }

  function clearMaintenanceFilter() {
    maintenanceCards.forEach(function (card) {
      card.hidden = false;
    });
    kpiLinks.forEach(function (link) {
      link.classList.remove('is-active');
      link.removeAttribute('aria-current');
    });
    if (filterBar) {
      filterBar.hidden = true;
    }
    if (filteredEmpty) {
      filteredEmpty.hidden = true;
    }
  }

  function applyMaintenanceFilter(status, activeLink) {
    let visibleCount = 0;
    maintenanceCards.forEach(function (card) {
      const visible = matchesMaintenanceFilter(card, status);
      card.hidden = !visible;
      if (visible) {
        visibleCount += 1;
      }
    });
    kpiLinks.forEach(function (link) {
      link.classList.toggle('is-active', link === activeLink);
      if (link === activeLink) {
        link.setAttribute('aria-current', 'true');
      } else {
        link.removeAttribute('aria-current');
      }
    });
    if (filterStatus) {
      filterStatus.textContent =
          filterLabel(status) + ' 항목 ' + visibleCount + '건을 표시합니다.';
    }
    if (filterBar) {
      filterBar.hidden = false;
    }
    if (filteredEmpty) {
      filteredEmpty.hidden = visibleCount !== 0;
    }
  }

  if (openAddBtn) {
    openAddBtn.addEventListener('click', function () {
      populateModal({}, false, openAddBtn);
    });
  }

  if (toggleBoardBtn && boardBody) {
    toggleBoardBtn.addEventListener('click', function () {
      setBoardCollapsed(!boardBody.classList.contains('is-collapsed'));
    });
  }

  if (toggleMaintenanceBtn && maintenanceBody) {
    toggleMaintenanceBtn.addEventListener('click', function () {
      setMaintenanceCollapsed(
          !maintenanceBody.classList.contains('is-collapsed'));
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
      if (!window.confirm('해당 호스트를 삭제하시겠습니까?')) {
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

  document.addEventListener('keydown', function (event) {
    if (!backdrop || backdrop.hidden) {
      return;
    }
    if (event.key === 'Escape') {
      event.preventDefault();
      closeModal();
      return;
    }
    if (event.key === 'Tab') {
      const focusable = focusableElements();
      if (focusable.length === 0) {
        event.preventDefault();
        modal.focus();
        return;
      }
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }
  });

  kpiLinks.forEach(function (link) {
    link.addEventListener('click', function (event) {
      if (maintenanceCards.length === 0) {
        return;
      }
      event.preventDefault();
      setMaintenanceCollapsed(false);
      applyMaintenanceFilter(link.dataset.status, link);
      document.getElementById('maintenanceMonthBoard').scrollIntoView({
        behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches
            ? 'auto'
            : 'smooth',
        block: 'start'
      });
    });
  });

  if (resetFilterBtn) {
    resetFilterBtn.addEventListener('click', function () {
      clearMaintenanceFilter();
      kpiLinks[0].focus();
    });
  }

  document.querySelectorAll('.maintenance-month-tab:not([aria-current="page"])')
      .forEach(function (link) {
        link.addEventListener('click', function (event) {
          if (event.defaultPrevented
              || event.button !== 0
              || event.metaKey
              || event.ctrlKey
              || event.shiftKey
              || event.altKey) {
            return;
          }
          if (loadingState) {
            loadingState.hidden = false;
          }
          if (maintenanceBody) {
            maintenanceBody.classList.add('is-loading');
            maintenanceBody.setAttribute('aria-busy', 'true');
          }
        });
      });

  setBoardCollapsed(readCollapsePreference(collapseStorageKey, true));
  setMaintenanceCollapsed(
      readCollapsePreference(maintenanceCollapseStorageKey, false));

  const formSeed = document.getElementById('vmHostFormSeed');
  if (formSeed) {
    setBoardCollapsed(false);
    populateModal({
      ip: formSeed.dataset.ip || '',
      originalIp: formSeed.dataset.originalIp || '',
      purpose: formSeed.dataset.purpose || '',
      osInfo: formSeed.dataset.osInfo || '',
      verticaVersion: formSeed.dataset.verticaVersion || '',
      remoteHost: formSeed.dataset.remoteHost || '',
      note: formSeed.dataset.note || ''
    }, Boolean(formSeed.dataset.originalIp), openAddBtn);
  } else if (document.querySelector('.vm-message, .vm-error')) {
    setBoardCollapsed(false);
  }
}());
