// 탭 전환 스크립트
var customerDetailRoot = document.querySelector(".customer-detail");
var customerContextPath = customerDetailRoot ? customerDetailRoot.getAttribute("data-context-path") : "";

document.addEventListener('DOMContentLoaded', function() {
	initializeDetailSectionCounts();
	var tabs = customerDetailRoot
		? Array.prototype.slice.call(customerDetailRoot.querySelectorAll('.tab-btn'))
		: [];
	var panels = customerDetailRoot
		? Array.prototype.slice.call(customerDetailRoot.querySelectorAll('.tab-panel'))
		: [];
	var tabNavigation = customerDetailRoot
		? customerDetailRoot.querySelector('.tab-nav')
		: null;
	var tabIndicator = tabNavigation
		? tabNavigation.querySelector('.tab-indicator')
		: null;

	function syncTabIndicator() {
		var activeTab = tabs.find(function(tab) {
			return tab.classList.contains('active');
		});
		if (!tabNavigation || !tabIndicator || !activeTab) {
			return;
		}
		tabNavigation.classList.add('is-enhanced');
		tabIndicator.style.inlineSize = activeTab.offsetWidth + 'px';
		tabIndicator.style.transform = 'translateX(' + activeTab.offsetLeft + 'px)';
	}

	function setActive(targetId) {
		panels.forEach(function(panel) {
			var isActive = panel.id === targetId;
			panel.classList.toggle('active', isActive);
			panel.hidden = !isActive;
		});
		tabs.forEach(function(tab) {
			var isActive = tab.getAttribute('data-target') === targetId;
			tab.classList.toggle('active', isActive);
			tab.setAttribute('aria-selected', String(isActive));
			tab.tabIndex = isActive ? 0 : -1;
		});
		window.requestAnimationFrame(syncTabIndicator);
	}

	function targetForEnvironment(environment) {
		if (environment === 'stg') return 'env-stg';
		if (environment === 'dev') return 'env-dev';
		return 'env-prod';
	}

	function environmentForTarget(targetId) {
		if (targetId === 'env-stg') return 'stg';
		if (targetId === 'env-dev') return 'dev';
		return 'prod';
	}

	function targetFromLocation() {
		var params = new URLSearchParams(window.location.search);
		return targetForEnvironment((params.get('env') || '').trim().toLowerCase());
	}

	function syncEnvironmentUrl(targetId, replace) {
		var environment = environmentForTarget(targetId);
		var url = new URL(window.location.href);
		if (url.searchParams.get('env') === environment) return;

		url.searchParams.set('env', environment);
		var currentState = window.history.state;
		var state = currentState && typeof currentState === 'object'
			? Object.assign({}, currentState)
			: {};
		state.customerEnvironment = environment;
		window.history[replace ? 'replaceState' : 'pushState'](
			state,
			'',
			url.pathname + url.search + url.hash);
	}

	// 초기 활성 탭 결정: URL env 우선, 없으면 운영 > 스테이징 > 개발 순
	var initial = 'env-prod';
	var params = new URLSearchParams(window.location.search);
	var envParam = (params.get('env') || '').trim().toLowerCase();
	var hasRequestedEnvironment = envParam === 'prod' || envParam === 'stg' || envParam === 'dev';
	if (hasRequestedEnvironment) initial = targetForEnvironment(envParam);
	var prodEmpty = document.querySelector('#env-prod .alert');
	var stgEmpty = document.querySelector('#env-stg .alert');
	var devEmpty = document.querySelector('#env-dev .alert');
	if (!hasRequestedEnvironment && prodEmpty) {
		if (!stgEmpty) initial = 'env-stg';
		else if (!devEmpty) initial = 'env-dev';
	}
	setActive(initial);
	syncEnvironmentUrl(initial, true);
	window.addEventListener('resize', syncTabIndicator);
	window.addEventListener('popstate', function() {
		setActive(targetFromLocation());
	});

	tabs.forEach(function(tab){
		tab.addEventListener('click', function(){
			var targetId = tab.getAttribute('data-target');
			setActive(targetId);
			syncEnvironmentUrl(targetId, false);
		});
		tab.addEventListener('keydown', function(event) {
			var currentIndex = tabs.indexOf(tab);
			var nextIndex = currentIndex;

			if (event.key === 'ArrowRight') {
				nextIndex = (currentIndex + 1) % tabs.length;
			} else if (event.key === 'ArrowLeft') {
				nextIndex = (currentIndex - 1 + tabs.length) % tabs.length;
			} else if (event.key === 'Home') {
				nextIndex = 0;
			} else if (event.key === 'End') {
				nextIndex = tabs.length - 1;
			} else {
				return;
			}

			event.preventDefault();
			var nextTab = tabs[nextIndex];
			var targetId = nextTab.getAttribute('data-target');
			setActive(targetId);
			syncEnvironmentUrl(targetId, false);
			nextTab.focus();
		});
	});

	var editButton = document.getElementById('editCustomerButton');
	if (editButton) {
		editButton.addEventListener('click', function(event) {
			event.preventDefault();
			editCustomer(this.href, getActiveEnv());
		});
	}
	var deleteButton = document.getElementById('deleteCustomerButton');
	if (deleteButton) {
		deleteButton.addEventListener('click', function(event) {
			deleteCustomer(this.dataset.customerName, this);
		});
	}
});

function initializeDetailSectionCounts() {
	if (!customerDetailRoot) return;
	customerDetailRoot.querySelectorAll('[data-detail-section]')
		.forEach(function(section) {
			var fields = section.querySelectorAll(
				':scope > .detail-grid > .detail-item');
			var count = section.querySelector(
				':scope > .detail-section-title [data-detail-section-count]');
			if (!count || fields.length === 0) return;
			var filled = Array.prototype.filter.call(fields, function(field) {
				return !field.classList.contains('detail-item--empty');
			}).length;
			count.textContent = filled + ' / ' + fields.length;
			count.setAttribute('aria-label',
				fields.length + '개 중 ' + filled + '개 등록');
			section.classList.toggle('detail-section--empty', filled === 0);
		});
}
function getActiveEnv() {
    var active = document.querySelector('.tab-btn.active');
    if (!active) return 'prod';
    var target = active.getAttribute('data-target');
    if (target === 'env-stg') return 'stg';
    if (target === 'env-dev') return 'dev';
    return 'prod';
}
function editCustomer(url, env) {
	var destination = new URL(url, window.location.href);
	if (env) {
		destination.searchParams.set('env', env);
	}
	window.location.href = destination.toString();
}

function deleteCustomer(customerName, trigger) {
	if (window.Frog2UI.confirmAction('정말로 "' + customerName + '" 고객사를 삭제하시겠습니까?\n\n삭제된 데이터는 복구할 수 없습니다.')) {
		var form = document.createElement('form');
		form.method = 'POST';
		form.action = customerContextPath + "/customers";
		form.className = 'ui-form';
		form.hidden = true;

		var actionInput = document.createElement('input');
		actionInput.type = 'hidden';
		actionInput.name = 'action';
		actionInput.value = 'delete';

		var nameInput = document.createElement('input');
		nameInput.type = 'hidden';
		nameInput.name = 'customer_name';
		nameInput.value = customerName;

		form.appendChild(actionInput);
		window.Frog2Csrf.appendTo(form);
		form.appendChild(nameInput);
		document.body.appendChild(form);
		window.Frog2UI.setButtonLoading(trigger, true, '삭제 중');
		form.requestSubmit();
	}
}
