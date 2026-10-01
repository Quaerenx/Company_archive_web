(function () {
  'use strict';

  const maintenanceBody = document.getElementById('maintenanceMonthBoardBody');
  const personalMaintenanceBoard = document.querySelector(
      '[data-personal-section="dashboard-maintenance"]');
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

  function setMaintenanceLoading(loading) {
    if (personalMaintenanceBoard) {
      personalMaintenanceBoard.setAttribute('aria-busy', String(loading));
    }
    if (loadingState) {
      loadingState.hidden = !loading;
    }
    if (maintenanceBody) {
      if (loading) {
        maintenanceBody.classList.add('is-loading');
      } else {
        maintenanceBody.classList.remove('is-loading');
      }
      maintenanceBody.setAttribute('aria-busy', String(loading));
    }
  }

  const monthViewport = document.querySelector('.maintenance-month-tabs');
  const monthTrack = document.querySelector('.maintenance-month-track');
  const monthLinks = Array.from(document.querySelectorAll('.maintenance-month-tab'));
  const currentMonthLink = monthLinks.find(function (link) {
    return link.getAttribute('aria-current') === 'page';
  });
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  const monthFocusStorageKey = 'frog2.dashboard.month.focus';
  const carouselAvailable = monthViewport && monthTrack && currentMonthLink
      && typeof monthTrack.animate === 'function';
  let positionAnimation = null;
  let pendingMonthLink = null;
  let keyboardSelection = false;
  let navigationStarted = false;

  function centerMonth(link, animate) {
    const startTransform = window.getComputedStyle(monthTrack).transform;
    const offset = monthViewport.clientWidth / 2
        - link.offsetLeft - link.offsetWidth / 2;
    if (positionAnimation) {
      positionAnimation.cancel();
    }
    const animation = monthTrack.animate([
      { transform: startTransform },
      { transform: 'translateX(' + offset + 'px)' }
    ], {
      duration: animate && !reducedMotion.matches ? 320 : 0,
      easing: 'cubic-bezier(0.16, 1, 0.3, 1)',
      fill: 'forwards'
    });
    positionAnimation = animation;
    animation.onfinish = function () {
      if (positionAnimation !== animation || !pendingMonthLink || navigationStarted) {
        return;
      }
      if (keyboardSelection) {
        try {
          window.sessionStorage.setItem(monthFocusStorageKey,
              new URL(pendingMonthLink.href).searchParams.get('maintenanceMonth'));
        } catch (ignore) {
          // Native links remain usable when focus persistence is unavailable.
        }
      }
      navigationStarted = true;
      monthLinks.forEach(function (link) {
        link.setAttribute('aria-disabled', 'true');
      });
      window.location.assign(pendingMonthLink.href);
    };
  }

  function selectMonth(link, fromKeyboard) {
    if (navigationStarted || link === pendingMonthLink) {
      return;
    }
    pendingMonthLink = link === currentMonthLink ? null : link;
    keyboardSelection = fromKeyboard;
    monthLinks.forEach(function (monthLink) {
      monthLink.classList.toggle('active', monthLink === link);
    });
    monthViewport.setAttribute('aria-busy', String(Boolean(pendingMonthLink)));
    setMaintenanceLoading(Boolean(pendingMonthLink));
    centerMonth(link, true);
  }

  function resetMonthCarousel() {
    if (!carouselAvailable) {
      return;
    }
    pendingMonthLink = null;
    keyboardSelection = false;
    navigationStarted = false;
    monthLinks.forEach(function (link) {
      link.classList.toggle('active', link === currentMonthLink);
      link.removeAttribute('aria-disabled');
    });
    monthViewport.setAttribute('aria-busy', 'false');
    centerMonth(currentMonthLink, false);
    try {
      const focusMonth = window.sessionStorage.getItem(monthFocusStorageKey);
      window.sessionStorage.removeItem(monthFocusStorageKey);
      if (focusMonth === new URL(currentMonthLink.href).searchParams.get('maintenanceMonth')) {
        currentMonthLink.focus({ preventScroll: true });
      }
    } catch (ignore) {
      // Focus restoration is optional when browser storage is unavailable.
    }
  }

  monthLinks.forEach(function (link, index) {
    link.addEventListener('click', function (event) {
      if (event.defaultPrevented
          || event.button !== 0
          || event.metaKey
          || event.ctrlKey
          || event.shiftKey
          || event.altKey) {
        return;
      }
      if (carouselAvailable) {
        event.preventDefault();
        selectMonth(link, event.detail === 0);
      } else if (link !== currentMonthLink) {
        setMaintenanceLoading(true);
      }
    });
    if (!carouselAvailable) {
      return;
    }
    link.addEventListener('keydown', function (event) {
      if (event.defaultPrevented || event.metaKey || event.ctrlKey
          || event.shiftKey || event.altKey) {
        return;
      }
      let nextIndex;
      switch (event.key) {
        case 'ArrowLeft': nextIndex = Math.max(0, index - 1); break;
        case 'ArrowRight': nextIndex = Math.min(monthLinks.length - 1, index + 1); break;
        case 'Home': nextIndex = 0; break;
        case 'End': nextIndex = monthLinks.length - 1; break;
        case ' ': nextIndex = index; break;
        default: return;
      }
      event.preventDefault();
      if (navigationStarted) {
        return;
      }
      monthLinks[nextIndex].focus({ preventScroll: true });
      selectMonth(monthLinks[nextIndex], true);
    });
  });

  if (carouselAvailable) {
    monthViewport.classList.add('is-carousel');
    resetMonthCarousel();
    const recenter = function () {
      centerMonth(pendingMonthLink || currentMonthLink, false);
    };
    if (typeof window.ResizeObserver === 'function') {
      const observer = new window.ResizeObserver(recenter);
      observer.observe(monthViewport);
      observer.observe(monthTrack);
    } else {
      window.addEventListener('resize', recenter);
    }
    reducedMotion.addEventListener('change', function () {
      if (reducedMotion.matches) {
        recenter();
      }
    });
  }

  window.addEventListener('pagehide', function () {
    pendingMonthLink = null;
    if (positionAnimation) {
      positionAnimation.cancel();
    }
  });

  window.addEventListener('pageshow', function () {
    setMaintenanceLoading(false);
    resetMonthCarousel();
  });

  setMaintenanceCollapsed(readCollapsePreference(false));
}());
