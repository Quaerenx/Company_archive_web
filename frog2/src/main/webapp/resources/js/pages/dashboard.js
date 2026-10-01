(function () {
  'use strict';

  const maintenanceBody = document.getElementById('maintenanceMonthBoardBody');
  const personalMaintenanceBoard = document.querySelector(
      '[data-personal-section="dashboard-maintenance"]');
  const maintenanceMonthLabel = document.querySelector(
      '#maintenanceMonthTitle .maintenance-month-label');
  const toggleMaintenanceBtn = document.getElementById('toggleMaintenanceBoardBtn');
  const maintenanceCollapseStorageKey = 'frog2.dashboard.monthly-maintenance.collapsed';
  const loadingState = document.getElementById('maintenanceLoadingState');
  const errorState = document.getElementById('maintenanceMonthError');
  const retryButton = document.getElementById('retryMaintenanceMonthBtn');
  const announcement = document.getElementById('maintenanceMonthAnnouncement');
  const monthViewport = document.querySelector('.maintenance-month-tabs');
  const monthTrack = document.querySelector('.maintenance-month-track');
  const monthLinks = Array.from(document.querySelectorAll('.maintenance-month-tab'));
  let currentMonthLink = monthLinks.find(function (link) {
    return link.getAttribute('aria-current') === 'page';
  });
  const initialMonthLink = currentMonthLink;
  const initialScrollRestoration = window.history && window.history.scrollRestoration;
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  const carouselAvailable = monthViewport && monthTrack && currentMonthLink
      && maintenanceBody && personalMaintenanceBoard && maintenanceMonthLabel
      && typeof monthTrack.animate === 'function'
      && typeof window.fetch === 'function'
      && typeof window.AbortController === 'function'
      && typeof window.DOMParser === 'function'
      && window.history && typeof window.history.pushState === 'function';
  let positionAnimation = null;
  let pendingRequest = null;
  let failedRequest = null;

  function writeCollapsePreference(collapsed) {
    try {
      window.localStorage.setItem(maintenanceCollapseStorageKey, collapsed ? 'true' : 'false');
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
    if (!maintenanceBody || !toggleMaintenanceBtn) return;
    maintenanceBody.classList.toggle('is-collapsed', collapsed);
    toggleMaintenanceBtn.textContent = collapsed ? '펼치기' : '접기';
    toggleMaintenanceBtn.setAttribute('aria-expanded', String(!collapsed));
    writeCollapsePreference(collapsed);
  }

  if (toggleMaintenanceBtn && maintenanceBody) {
    toggleMaintenanceBtn.addEventListener('click', function () {
      setMaintenanceCollapsed(!maintenanceBody.classList.contains('is-collapsed'));
    });
  }

  function showLoadingIndicator(visible) {
    if (loadingState) loadingState.hidden = !visible;
  }

  function setMaintenanceLoading(loading) {
    for (const board of [personalMaintenanceBoard, maintenanceBody]) {
      if (!board) continue;
      board.setAttribute('aria-busy', String(loading));
      board.classList.toggle('is-loading', loading);
      board.inert = loading;
    }
    if (monthViewport) monthViewport.setAttribute('aria-busy', String(loading));
    if (!loading) showLoadingIndicator(false);
  }

  function clearRequest() {
    if (!pendingRequest) return;
    const previous = pendingRequest;
    pendingRequest = null;
    window.clearTimeout(previous.loadingTimer);
    window.clearTimeout(previous.timeoutTimer);
    previous.controller.abort();
  }

  function setActiveMonth(link) {
    monthLinks.forEach(function (monthLink) {
      monthLink.classList.toggle('active', monthLink === link);
      monthLink.setAttribute('tabindex', monthLink === link ? '0' : '-1');
      if (monthLink === currentMonthLink) {
        monthLink.setAttribute('aria-current', 'page');
      } else {
        monthLink.removeAttribute('aria-current');
      }
    });
  }

  function centerMonth(link, animate) {
    const startTransform = window.getComputedStyle(monthTrack).transform;
    const offset = monthViewport.clientWidth / 2 - link.offsetLeft - link.offsetWidth / 2;
    if (positionAnimation) positionAnimation.cancel();
    positionAnimation = monthTrack.animate([
      { transform: startTransform },
      { transform: 'translateX(' + offset + 'px)' }
    ], {
      duration: animate && !reducedMotion.matches ? 320 : 0,
      easing: 'cubic-bezier(0.16, 1, 0.3, 1)',
      fill: 'forwards'
    });
  }

  function syncHistory(link, mode) {
    if (mode === 'none') return;
    const url = new URL(window.location.href);
    const month = new URL(link.href).searchParams.get('maintenanceMonth');
    url.searchParams.set('maintenanceMonth', month);
    window.history[mode === 'replace' ? 'replaceState' : 'pushState'](
        Object.assign({}, window.history.state, { maintenanceMonth: month }),
        '', url.pathname + url.search + url.hash);
  }

  function cloneContents(node) {
    return Array.from(node.childNodes, function (child) {
      return document.importNode(child, true);
    });
  }

  function readMaintenanceContents(html, link) {
    const page = new window.DOMParser().parseFromString(html, 'text/html');
    if (page.body.getAttribute('data-user-id') !== document.body.getAttribute('data-user-id')) {
      window.Frog2Session.requireActiveSession({ status: 401 });
    }
    const selected = page.querySelector('.maintenance-month-tab[aria-current="page"]');
    const personal = page.querySelector('[data-personal-section="dashboard-maintenance"]');
    const body = page.getElementById('maintenanceMonthBoardBody');
    const label = page.querySelector('#maintenanceMonthTitle .maintenance-month-label');
    const personalLabel = page.querySelector('#personalMaintenanceTitle .maintenance-month-label');
    const month = new URL(link.href).searchParams.get('maintenanceMonth');
    if (!selected || new URL(selected.getAttribute('href'), window.location.href)
        .searchParams.get('maintenanceMonth') !== month
        || !personal || !body || !label || !personalLabel
        || label.textContent.trim() !== month || personalLabel.textContent.trim() !== month
        || personal.querySelector('.dashboard-state--error')
        || body.querySelector('.dashboard-state--error')) {
      throw new Error('The dashboard month response is incomplete or inconsistent');
    }
    return { personal: cloneContents(personal), body: cloneContents(body), label: label.textContent };
  }

  async function loadMonth(request) {
    try {
      const response = await window.fetch(request.link.href, {
        headers: { Accept: 'text/html', 'X-Requested-With': 'XMLHttpRequest' },
        credentials: 'same-origin', cache: 'no-store', signal: request.controller.signal
      });
      if (pendingRequest !== request) return;
      window.Frog2Session.requireActiveSession(response);
      if (!response.ok || !response.headers.get('Content-Type')?.includes('text/html')) {
        throw new Error('Unable to load dashboard month (HTTP ' + response.status + ')');
      }
      const html = await response.text();
      if (pendingRequest !== request) return;
      const contents = readMaintenanceContents(html, request.link);
      const scrollX = window.scrollX;
      const scrollY = window.scrollY;
      syncHistory(request.link, request.historyMode);
      personalMaintenanceBoard.replaceChildren(...contents.personal);
      maintenanceBody.replaceChildren(...contents.body);
      maintenanceMonthLabel.textContent = contents.label;
      currentMonthLink = request.link;
      clearRequest();
      setActiveMonth(currentMonthLink);
      setMaintenanceLoading(false);
      window.scrollTo({ left: scrollX, top: scrollY, behavior: 'instant' });
      if (announcement) announcement.textContent = contents.label + ' 점검 현황을 업데이트했습니다.';
    } catch (error) {
      if (pendingRequest !== request) return;
      clearRequest();
      setActiveMonth(currentMonthLink);
      setMaintenanceLoading(false);
      centerMonth(currentMonthLink, true);
      if (monthLinks.includes(document.activeElement)) currentMonthLink.focus({ preventScroll: true });
      if (request.historyMode === 'none') syncHistory(currentMonthLink, 'replace');
      if (window.Frog2Session.isSessionExpired(error)) return;
      failedRequest = request;
      if (errorState) errorState.hidden = false;
    }
  }

  function selectMonth(link, fromKeyboard, historyMode = 'push') {
    if (pendingRequest && pendingRequest.link === link) return;
    clearRequest();
    failedRequest = null;
    if (errorState) errorState.hidden = true;
    if (announcement) announcement.textContent = '';
    showLoadingIndicator(false);
    setActiveMonth(link);
    if (fromKeyboard || monthLinks.includes(document.activeElement)) link.focus({ preventScroll: true });
    centerMonth(link, true);
    if (link === currentMonthLink) {
      setMaintenanceLoading(false);
      return;
    }
    const request = { link: link, historyMode: historyMode, controller: new window.AbortController() };
    pendingRequest = request;
    setMaintenanceLoading(true);
    request.loadingTimer = window.setTimeout(function () {
      if (pendingRequest === request) showLoadingIndicator(true);
    }, 250);
    request.timeoutTimer = window.setTimeout(function () {
      if (pendingRequest === request) request.controller.abort();
    }, 15000);
    loadMonth(request);
  }

  function monthFromLocation() {
    const month = new URL(window.location.href).searchParams.get('maintenanceMonth');
    return monthLinks.find(function (link) {
      return new URL(link.href).searchParams.get('maintenanceMonth') === month;
    }) || initialMonthLink;
  }

  monthLinks.forEach(function (link, index) {
    link.addEventListener('click', function (event) {
      if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey
          || event.shiftKey || event.altKey) return;
      if (carouselAvailable) {
        event.preventDefault();
        selectMonth(link, event.detail === 0);
      }
    });
    if (!carouselAvailable) return;
    link.addEventListener('keydown', function (event) {
      if (event.defaultPrevented || event.metaKey || event.ctrlKey
          || event.shiftKey || event.altKey) return;
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
      selectMonth(monthLinks[nextIndex], true);
    });
  });

  if (retryButton) {
    retryButton.addEventListener('click', function () {
      if (!failedRequest) return;
      selectMonth(failedRequest.link, true,
          failedRequest.historyMode === 'none' ? 'replace' : failedRequest.historyMode);
    });
  }

  if (carouselAvailable) {
    monthViewport.classList.add('is-carousel');
    setActiveMonth(currentMonthLink);
    centerMonth(currentMonthLink, false);
    window.history.scrollRestoration = 'manual';
    const recenter = function () {
      centerMonth(pendingRequest ? pendingRequest.link : currentMonthLink, false);
    };
    if (typeof window.ResizeObserver === 'function') {
      const observer = new window.ResizeObserver(recenter);
      observer.observe(monthViewport);
      observer.observe(monthTrack);
    } else {
      window.addEventListener('resize', recenter);
    }
    reducedMotion.addEventListener('change', function () {
      if (reducedMotion.matches) recenter();
    });
    window.addEventListener('popstate', function () {
      selectMonth(monthFromLocation(), false, 'none');
    });
  }

  window.addEventListener('pagehide', function () {
    clearRequest();
    if (positionAnimation) positionAnimation.cancel();
    if (carouselAvailable) window.history.scrollRestoration = initialScrollRestoration;
  });

  window.addEventListener('pageshow', function () {
    if (!carouselAvailable) return;
    window.history.scrollRestoration = 'manual';
    setMaintenanceLoading(false);
    selectMonth(monthFromLocation(), false, 'none');
  });

  setMaintenanceCollapsed(readCollapsePreference(false));
}());
