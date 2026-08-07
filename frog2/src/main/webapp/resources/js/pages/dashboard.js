(function () {
  'use strict';

  const maintenanceBody = document.getElementById('maintenanceMonthBoardBody');
  const toggleMaintenanceBtn =
      document.getElementById('toggleMaintenanceBoardBtn');
  const maintenanceCollapseStorageKey =
      'frog2.dashboard.monthly-maintenance.collapsed';
  const loadingState = document.getElementById('maintenanceLoadingState');

  function writeCollapsePreference(collapsed) {
    try {
      window.localStorage.setItem(
          maintenanceCollapseStorageKey,
          collapsed ? 'true' : 'false');
    } catch (ignore) {
      // The dashboard remains usable when browser storage is unavailable.
    }
  }

  function readCollapsePreference(defaultValue) {
    try {
      const stored = window.localStorage.getItem(maintenanceCollapseStorageKey);
      return stored === null ? defaultValue : stored === 'true';
    } catch (ignore) {
      return defaultValue;
    }
  }

  function setMaintenanceCollapsed(collapsed) {
    if (!maintenanceBody || !toggleMaintenanceBtn) {
      return;
    }
    maintenanceBody.classList.toggle('is-collapsed', collapsed);
    toggleMaintenanceBtn.textContent = collapsed ? '펼치기' : '접기';
    toggleMaintenanceBtn.setAttribute('aria-expanded', String(!collapsed));
    writeCollapsePreference(collapsed);
  }

  if (toggleMaintenanceBtn && maintenanceBody) {
    toggleMaintenanceBtn.addEventListener('click', function () {
      setMaintenanceCollapsed(
          !maintenanceBody.classList.contains('is-collapsed'));
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

  setMaintenanceCollapsed(readCollapsePreference(false));
}());
