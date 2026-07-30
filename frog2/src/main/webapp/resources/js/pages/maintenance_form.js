(function() {
    'use strict';

    const root = document.querySelector('[data-maintenance-form-mode][data-context-path]');
    if (!root) {
        return;
    }

    const form = document.getElementById('maintenanceForm');
    if (!form || !root.contains(form)) {
        return;
    }

    const mode = root.getAttribute('data-maintenance-form-mode');
    const contextPath = root.getAttribute('data-context-path') || '';
    const customerField = document.getElementById('customer_name');
    const inspectorField = document.getElementById('inspector_name');
    const inspectionDateField = document.getElementById('inspection_date');
    const versionField = document.getElementById('vertica_version');
    const licenseFields = [
        {
            element: document.getElementById('license_size_gb'),
            message: '라이선스 크기는 최대 50자까지 입력할 수 있습니다.'
        },
        {
            element: document.getElementById('license_usage_size'),
            message: '라이선스 사용량은 최대 50자까지 입력할 수 있습니다.'
        },
        {
            element: document.getElementById('license_usage_pct'),
            message: '라이선스 사용률은 최대 50자까지 입력할 수 있습니다.'
        }
    ];
    const deleteForm = document.getElementById('deleteFormHeader');
    let optionsUnavailable = false;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initialize, { once: true });
    } else {
        initialize();
    }

    function initialize() {
        form.addEventListener('submit', validateForm);
        if (deleteForm) {
            deleteForm.addEventListener('submit', confirmDelete);
        }

        if (mode === 'add') {
            setDefaultInspectionDate();
            if (isSelect(customerField)) {
                customerField.addEventListener('change', function() {
                    const customerName = normalizedValue(customerField.value);
                    if (customerName) {
                        prefillFromCustomerDetail(customerName).catch(logPrefillFailure);
                    }
                });
            }
        }

        loadMaintenanceOptions()
            .then(function() {
                if (mode === 'edit') {
                    preserveCurrentValues();
                    return;
                }
                if (mode === 'add' && !isSelect(customerField)) {
                    const customerName = normalizedValue(customerField.value);
                    if (customerName) {
                        return prefillFromCustomerDetail(customerName)
                            .catch(logPrefillFailure);
                    }
                }
            })
            .catch(handleOptionsFailure);
    }

    function setDefaultInspectionDate() {
        if (!inspectionDateField || inspectionDateField.value) {
            return;
        }
        const today = new Date();
        const year = today.getFullYear();
        const month = String(today.getMonth() + 1).padStart(2, '0');
        const day = String(today.getDate()).padStart(2, '0');
        inspectionDateField.value = year + '-' + month + '-' + day;
    }

    function loadMaintenanceOptions() {
        return fetch(contextPath + '/customers?action=getCustomersForMaintenance', {
            headers: { Accept: 'application/json' }
        })
            .then(readJsonResponse)
            .then(function(data) {
                if (!data || !Array.isArray(data.customers)
                        || !Array.isArray(data.inspectors)) {
                    throw new Error('Invalid maintenance options response');
                }
                populateOptions(customerField, data.customers);
                populateOptions(inspectorField, data.inspectors);
                optionsUnavailable = false;
                setOptionsValidity('');
            });
    }

    function readJsonResponse(response) {
        if (!response.ok) {
            throw new Error('Maintenance request failed with status ' + response.status);
        }
        return response.json();
    }

    function populateOptions(select, values) {
        if (!isSelect(select)) {
            return;
        }
        values.forEach(function(value) {
            ensureOption(select, value);
        });
    }

    function ensureOption(select, rawValue) {
        if (!isSelect(select)) {
            return;
        }
        const value = normalizedValue(rawValue);
        if (!value) {
            return;
        }
        const exists = Array.from(select.options).some(function(option) {
            return option.value === value;
        });
        if (!exists) {
            const option = document.createElement('option');
            option.value = value;
            option.textContent = value;
            select.appendChild(option);
        }
    }

    function preserveCurrentValues() {
        const currentCustomer = hiddenValue('current_customer_value');
        const currentInspector = hiddenValue('current_inspector_value');

        ensureOption(customerField, currentCustomer);
        ensureOption(inspectorField, currentInspector);
        if (currentCustomer) {
            customerField.value = currentCustomer;
        }
        if (currentInspector) {
            inspectorField.value = currentInspector;
        }
    }

    function hiddenValue(id) {
        const input = document.getElementById(id);
        return input ? normalizedValue(input.value) : '';
    }

    function prefillFromCustomerDetail(customerName) {
        const url = contextPath + '/customers?action=getDetail&customerName='
                + encodeURIComponent(customerName);
        return fetch(url, { headers: { Accept: 'application/json' } })
            .then(readJsonResponse)
            .then(function(data) {
                const mainManager = normalizedValue(data && data.mainManager);
                const subManager = normalizedValue(data && data.subManager);
                const defaultInspector = mainManager || subManager;
                if (defaultInspector) {
                    ensureOption(inspectorField, defaultInspector);
                    inspectorField.value = defaultInspector;
                }

                const version = normalizedValue(data && data.verticaVersion);
                if (version && versionField && !normalizedValue(versionField.value)) {
                    versionField.value = version;
                }
            });
    }

    function handleOptionsFailure(error) {
        optionsUnavailable = true;
        if (mode === 'edit') {
            preserveCurrentValues();
        }
        setOptionsValidity('고객사 및 점검자 정보를 불러올 수 없습니다.');
        console.error('Unable to load maintenance options:', error);
    }

    function logPrefillFailure(error) {
        console.error('Unable to prefill maintenance customer detail:', error);
    }

    function setOptionsValidity(message) {
        if (isSelect(customerField)) {
            customerField.setCustomValidity(message);
        }
        if (inspectorField) {
            inspectorField.setCustomValidity(message);
        }
    }

    function validateForm(event) {
        if (optionsUnavailable) {
            blockSubmission(
                event,
                isSelect(customerField) ? customerField : inspectorField,
                '고객사 및 점검자 정보를 불러오지 못했습니다. 페이지를 새로 고침한 뒤 다시 시도해주세요.');
            return;
        }
        if (!normalizedValue(customerField && customerField.value)) {
            blockSubmission(event, customerField, '고객사명을 선택해주세요.');
            return;
        }
        if (!normalizedValue(inspectorField && inspectorField.value)) {
            blockSubmission(event, inspectorField, '점검자를 선택해주세요.');
            return;
        }
        if (!inspectionDateField || !inspectionDateField.value) {
            blockSubmission(event, inspectionDateField, '점검일자를 입력해주세요.');
            return;
        }

        for (const field of licenseFields) {
            if (field.element && normalizedValue(field.element.value).length > 50) {
                blockSubmission(event, field.element, field.message);
                return;
            }
        }
    }

    function blockSubmission(event, field, message) {
        event.preventDefault();
        window.Frog2UI.showFieldError(field, message);
    }

    function confirmDelete(event) {
        if (!window.confirm('정말 삭제하시겠습니까?')) {
            event.preventDefault();
        }
    }

    function isSelect(element) {
        return element && element.tagName === 'SELECT';
    }

    function normalizedValue(value) {
        return typeof value === 'string' ? value.trim() : '';
    }
})();
