// 탭 전환 스크립트
var customerDetailRoot = document.querySelector(".customer-detail");
var customerContextPath = customerDetailRoot ? customerDetailRoot.getAttribute("data-context-path") : "";

document.addEventListener('DOMContentLoaded', function() {
	var tabs = document.querySelectorAll('.tab-btn');
	var panels = document.querySelectorAll('.tab-panel');

	function setActive(targetId) {
		panels.forEach(function(p){ p.classList.remove('active'); });
		tabs.forEach(function(t){ t.classList.remove('active'); });
		var target = document.getElementById(targetId);
		if (target) target.classList.add('active');
		var btn = document.querySelector('.tab-btn[data-target="' + targetId + '"]');
		if (btn) btn.classList.add('active');
	}

	// 초기 활성 탭 결정: URL env 우선, 없으면 운영 > 스테이징 > 개발 순
	var initial = 'env-prod';
	var params = new URLSearchParams(window.location.search);
	var envParam = (params.get('env') || '').trim().toLowerCase();
	var hasRequestedEnvironment = envParam === 'prod' || envParam === 'stg' || envParam === 'dev';
	if (envParam === 'stg') initial = 'env-stg';
	if (envParam === 'dev') initial = 'env-dev';
	var prodEmpty = document.querySelector('#env-prod .alert');
	var stgEmpty = document.querySelector('#env-stg .alert');
	var devEmpty = document.querySelector('#env-dev .alert');
	if (!hasRequestedEnvironment && prodEmpty) {
		if (!stgEmpty) initial = 'env-stg';
		else if (!devEmpty) initial = 'env-dev';
	}
	setActive(initial);

	tabs.forEach(function(tab){
		tab.addEventListener('click', function(){
			setActive(tab.getAttribute('data-target'));
		});
	});

	var editButton = document.getElementById('editCustomerButton');
	if (editButton) {
		editButton.addEventListener('click', function(event) {
			event.preventDefault();
			editCustomer(this.dataset.customerName, getActiveEnv());
		});
	}
	var deleteButton = document.getElementById('deleteCustomerButton');
	if (deleteButton) {
		deleteButton.addEventListener('click', function(event) {
			event.preventDefault();
			deleteCustomer(this.dataset.customerName);
		});
	}
});
function getActiveEnv() {
    var active = document.querySelector('.tab-btn.active');
    if (!active) return 'prod';
    var target = active.getAttribute('data-target');
    if (target === 'env-stg') return 'stg';
    if (target === 'env-dev') return 'dev';
    return 'prod';
}
function editCustomer(customerName, env) {
    var encodedName = encodeURIComponent(customerName);
    var url = customerContextPath + "/customers?view=editDetail&customerName=" + encodedName;
    if (env) url += '&env=' + encodeURIComponent(env);
    window.location.href = url;
}

function deleteCustomer(customerName) {
	if (confirm('정말로 "' + customerName + '" 고객사를 삭제하시겠습니까?\n\n삭제된 데이터는 복구할 수 없습니다.')) {
		var form = document.createElement('form');
		form.method = 'POST';
		form.action = customerContextPath + "/customers";

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
		form.submit();
	}
}
