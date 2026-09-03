// 탭 전환 스크립트
var customerDetailRoot = document.querySelector(".customer-detail");
var customerContextPath = customerDetailRoot ? customerDetailRoot.getAttribute("data-context-path") : "";

document.addEventListener('DOMContentLoaded', function() {
	initializeDetailSectionCounts();
	initializeCustomerFavorite();
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
	var fieldNames = {
		'DB명': 'dbName',
		'OS': 'osInfo',
		'Vertica 버전': 'verticaVersion',
		'SAID': 'said',
		'DB 모드': 'dbMode',
		'주 담당자': 'mainManager',
		'노드 수': 'nodeCount',
		'부 담당자': 'subManager',
		'라이선스': 'licenseInfo',
		'고객사': 'customerName',
		'시스템명': 'systemName',
		'고객사 담당자': 'customerManager',
		'도입 연도': 'introductionYear',
		'담당 SI': 'siCompany',
		'설치일': 'installDate',
		'SI 담당자': 'siManager',
		'작성일': 'createDate',
		'작성자': 'creator',
		'Vertica 관리자': 'verticaAdmin',
		'MC 사용': 'mcYn',
		'사용자 정의 리소스 풀': 'customResourcePoolYn',
		'MC 버전': 'mcVersion',
		'Subcluster 사용': 'subclusterYn',
		'MC 호스트': 'mcHost',
		'백업 사용': 'backupYn',
		'MC 관리자': 'mcAdmin',
		'백업 비고': 'backupNote',
		'메모리': 'memoryInfo',
		'인프라 유형': 'infraType',
		'SWAP 메모리': 'swapMemory',
		'데이터 영역': 'dataArea',
		'CPU 소켓': 'cpuSocket',
		'카탈로그 영역': 'catalogArea',
		'CPU 코어': 'cpuCore',
		'Depot 영역': 'depotArea',
		'하이퍼스레딩': 'hyperThreading',
		'Object 영역': 'objectArea',
		'Public 네트워크': 'publicNetwork',
		'Private 네트워크': 'privateNetwork',
		'스토리지 네트워크': 'storageNetwork',
		'ETL': 'etlTool',
		'BI': 'biTool',
		'DB 암호화': 'dbEncryption',
		'CDC': 'cdcTool',
		'EOS 일자': 'eosDate',
		'고객 유형': 'customerType',
		'비고': 'note'
	};
	var editUrl = customerDetailRoot.getAttribute('data-customer-edit-url');

	function closePanels(except) {
		customerDetailRoot.querySelectorAll('[data-detail-missing-panel]')
			.forEach(function(panel) {
				if (panel === except) return;
				panel.hidden = true;
				var owner = panel.previousElementSibling
					? panel.previousElementSibling.querySelector('[data-detail-section-count]')
					: null;
				if (owner) owner.setAttribute('aria-expanded', 'false');
			});
	}

	function environmentFor(section) {
		var panel = section.closest('.tab-panel');
		if (!panel) return 'prod';
		if (panel.id === 'env-stg') return 'stg';
		if (panel.id === 'env-dev') return 'dev';
		return 'prod';
	}

	function createMissingPanel(section, missing) {
		var panel = document.createElement('div');
		panel.className = 'detail-missing-panel';
		panel.setAttribute('data-detail-missing-panel', '');
		panel.hidden = true;
		var heading = document.createElement('strong');
		heading.textContent = '누락 항목';
		var list = document.createElement('ul');
		missing.forEach(function(field) {
			var label = field.getAttribute('data-detail-field-label') || '미등록 항목';
			var item = document.createElement('li');
			var fieldName = fieldNames[label];
			if (editUrl && fieldName) {
				var link = document.createElement('a');
				var destination = new URL(editUrl, window.location.href);
				destination.searchParams.set('env', environmentFor(section));
				destination.searchParams.set('focus', fieldName);
				link.href = destination.toString();
				link.textContent = label;
				link.title = label + ' 입력란으로 이동';
				item.appendChild(link);
			} else {
				item.textContent = label;
			}
			list.appendChild(item);
		});
		panel.appendChild(heading);
		panel.appendChild(list);
		return panel;
	}

	customerDetailRoot.querySelectorAll('[data-detail-section]')
		.forEach(function(section) {
			var fields = section.querySelectorAll(
				':scope > .detail-grid > .detail-item');
			var count = section.querySelector(
				':scope > .detail-section-title [data-detail-section-count]');
			if (!count || fields.length === 0) return;
			var missing = Array.prototype.filter.call(fields, function(field) {
				return field.getAttribute('data-detail-field-missing') === 'true';
			});
			var filled = Array.prototype.filter.call(fields, function(field) {
				return field.getAttribute('data-detail-field-missing') !== 'true';
			}).length;
			count.textContent = filled + ' / ' + fields.length;
			count.setAttribute('aria-label',
				missing.length
					? fields.length + '개 중 ' + filled + '개 등록, 누락 항목 보기'
					: fields.length + '개 모두 등록');
			count.setAttribute('aria-expanded', 'false');
			count.disabled = missing.length === 0;
			section.classList.toggle('detail-section--empty', filled === 0);
			if (!missing.length) return;

			var panel = createMissingPanel(section, missing);
			var title = count.closest('.detail-section-title');
			title.insertAdjacentElement('afterend', panel);
			count.addEventListener('click', function(event) {
				event.preventDefault();
				event.stopPropagation();
				var opening = panel.hidden;
				if (opening && section.tagName === 'DETAILS') section.open = true;
				closePanels(opening ? panel : null);
				panel.hidden = !opening;
				count.setAttribute('aria-expanded', String(opening));
			});
		});
	document.addEventListener('click', function(event) {
		if (!event.target.closest('[data-detail-missing-panel], [data-detail-section-count]')) {
			closePanels(null);
		}
	});
	document.addEventListener('keydown', function(event) {
		if (event.key === 'Escape') closePanels(null);
	});
}

function initializeCustomerFavorite() {
	var button = customerDetailRoot
		? customerDetailRoot.querySelector('[data-customer-favorite]')
		: null;
	if (!button || !document.body) return;
	var customerName = button.getAttribute('data-customer-name');
	var customerUrl = button.getAttribute('data-customer-url');
	var userId = document.body.getAttribute('data-user-id') || 'anonymous';
	var storageKey = 'frog2.quickNav.recent.v1:' + userId;

	function readState() {
		try {
			var state = JSON.parse(window.localStorage.getItem(storageKey) || '{}');
			return state && typeof state === 'object' ? state : {};
		} catch (error) {
			return {};
		}
	}

	function favorites(state) {
		return Array.isArray(state.favorites)
			? state.favorites.filter(function(item) {
				return item && typeof item.url === 'string'
					&& typeof item.label === 'string';
			})
			: [];
	}

	function isFavorite(state) {
		return favorites(state).some(function(item) {
			return item.url === customerUrl;
		});
	}

	function render(state) {
		var active = isFavorite(state);
		button.setAttribute('aria-pressed', String(active));
		button.setAttribute('aria-label', customerName
			+ (active ? ' 즐겨찾기 해제' : ' 즐겨찾기'));
		button.classList.toggle('is-active', active);
		var icon = button.querySelector('i');
		if (icon) {
			icon.classList.toggle('fas', active);
			icon.classList.toggle('far', !active);
		}
		var label = button.querySelector('span');
		if (label) label.textContent = active ? '즐겨찾기 해제' : '즐겨찾기';
	}

	button.addEventListener('click', function() {
		var state = readState();
		var current = favorites(state);
		if (isFavorite(state)) {
			state.favorites = current.filter(function(item) {
				return item.url !== customerUrl;
			});
		} else {
			state.favorites = [{label: customerName, url: customerUrl}]
				.concat(current.filter(function(item) {
					return item.url !== customerUrl;
				})).slice(0, 12);
		}
		try {
			window.localStorage.setItem(storageKey, JSON.stringify(state));
		} catch (error) {
			if (window.Frog2UI) {
				window.Frog2UI.notify('즐겨찾기를 저장할 수 없습니다.', 'warning');
			}
			return;
		}
		render(state);
		if (window.Frog2UI) {
			window.Frog2UI.announce(
				isFavorite(state) ? '즐겨찾기에 추가했습니다.' : '즐겨찾기에서 제거했습니다.');
		}
	});

	render(readState());
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
