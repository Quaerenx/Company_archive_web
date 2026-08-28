(function() {
    'use strict';

    function formatLicensePercentageHalfUp(value) {
        if (!Number.isFinite(value) || value < 0) return '';
        const scaled = value * 100;
        const tolerance = Number.EPSILON
            * Math.max(1, Math.abs(scaled)) * 2;
        const formatted = (Math.floor(scaled + 0.5 + tolerance) / 100)
            .toFixed(2);
        return formatted.endsWith('0') ? formatted.slice(0, -1) : formatted;
    }

    if (typeof module === 'object' && module.exports) {
        module.exports = {formatLicensePercentageHalfUp};
        return;
    }

    const root = document.querySelector(
        '[data-maintenance-form-mode][data-context-path]');
    if (!root) return;

    const form = document.getElementById('maintenanceForm');
    if (!form || !root.contains(form)) return;

    const contextPath = root.getAttribute('data-context-path') || '';
    const formMode = root.getAttribute('data-maintenance-form-mode') || '';
    const maxLicensePercentage = 1000000;
    const customerField = document.getElementById('customer_name');
    const inspectorField = document.getElementById('inspector_name');
    const inspectionDateField = document.getElementById('inspection_date');
    const inlineCalendar = document.getElementById(
        'maintenanceInlineCalendar');
    const calendarMonthLabel = document.getElementById(
        'maintenanceCalendarMonthLabel');
    const calendarGrid = document.getElementById(
        'maintenanceCalendarGrid');
    const calendarPreviousButton = document.getElementById(
        'maintenanceCalendarPreviousMonth');
    const calendarNextButton = document.getElementById(
        'maintenanceCalendarNextMonth');
    const calendarTodayButton = document.getElementById(
        'maintenanceCalendarToday');
    const selectedDateLabel = document.getElementById(
        'maintenanceSelectedDateLabel');
    const versionField = document.getElementById('vertica_version');
    const versionDisplay = document.getElementById(
        'vertica_version_display');
    const licenseSizeField = document.getElementById('license_size_gb');
    const licenseSizeDisplay = document.getElementById(
        'license_size_gb_display');
    const licenseCapacityUnit = document.getElementById(
        'maintenanceCapacityUnit');
    const licenseUsageField = document.getElementById('license_usage_size');
    const licenseUsageUnit = document.getElementById(
        'maintenanceUsageUnit');
    const unsupportedCapacityHelp = document.getElementById(
        'maintenanceUnsupportedCapacityHelp');
    const licensePercentageField = document.getElementById(
        'license_usage_pct');
    const licensePercentageDisplay = document.getElementById(
        'license_usage_pct_display');
    const noteField = document.getElementById('note');
    const noteCount = document.getElementById('maintenanceNoteCount');
    const noteTemplateButton = document.getElementById(
        'insertMaintenanceNoteTemplate');
    const previousCallout = document.getElementById(
        'previousMaintenanceCallout');
    const previousSummary = document.getElementById(
        'previousMaintenanceSummary');
    const previousButton = document.getElementById(
        'applyPreviousMaintenanceDefaults');
    const contextStatus = document.getElementById(
        'maintenanceContextStatus');
    const contextMessage = document.getElementById(
        'maintenanceContextMessage');
    const retryContextButton = document.getElementById(
        'retryMaintenanceContext');
    const duplicateWarning = document.getElementById(
        'duplicateMaintenanceWarning');
    const duplicateMessage = document.getElementById(
        'duplicateMaintenanceMessage');
    const duplicateHistoryLink = document.getElementById(
        'duplicateMaintenanceHistoryLink');
    const deleteForm = document.getElementById('deleteFormHeader');
    const maintenanceId = document.querySelector(
        'input[name="maintenance_id"]');
    let contextController = null;
    let initialFormState = '';
    let submitting = false;
    const calendarController = window.Frog2MaintenanceCalendar.create({
        calendar: inlineCalendar,
        dateField: inspectionDateField,
        document,
        grid: calendarGrid,
        monthLabel: calendarMonthLabel,
        nextButton: calendarNextButton,
        onChange: refreshFormContext,
        previousButton: calendarPreviousButton,
        root,
        selectedDateLabel,
        todayButton: calendarTodayButton
    });

    initialize();

    function initialize() {
        form.addEventListener('submit', validateForm);
        form.addEventListener('input', markUserModified);
        if (deleteForm) {
            deleteForm.addEventListener('submit', confirmDelete);
        }
        if (customerField && customerField.tagName === 'SELECT') {
            customerField.addEventListener('change', refreshFormContext);
        }
        [licenseSizeField, licenseUsageField].forEach(function(field) {
            if (field) field.addEventListener('input', calculateLicensePercentage);
        });
        if (previousButton) {
            previousButton.addEventListener('click', applyPreviousDefaults);
        }
        if (retryContextButton) {
            retryContextButton.addEventListener(
                'click', retryMaintenanceContext);
        }
        if (noteTemplateButton) {
            noteTemplateButton.addEventListener('click', insertNoteTemplate);
        }
        if (noteField) {
            noteField.addEventListener('input', function() {
                autoResizeNote();
                updateNoteCount();
            });
        }
        window.addEventListener('beforeunload', warnAboutUnsavedChanges);

        calendarController.initialize();
        renderFixedVersion();
        renderFixedCapacity();
        calculateLicensePercentage();
        autoResizeNote();
        updateNoteCount();
        initialFormState = serializeForm();
    }

    function markUserModified(event) {
        if (event.target instanceof HTMLElement) {
            event.target.dataset.userModified = 'true';
        }
    }

    function refreshFormContext() {
        const customerName = normalizedValue(customerField && customerField.value);
        const inspectionDate = normalizedValue(
            inspectionDateField && inspectionDateField.value);
        if (!customerName || !inspectionDate) {
            updatePreviousContext(null);
            updateDuplicateWarning(null);
            showContextStatus(null);
            return;
        }
        if (contextController) contextController.abort();
        contextController = new AbortController();
        showContextStatus(null);
        const parameters = new URLSearchParams({
            view: 'formContext',
            customerName: customerName,
            inspectionDate: inspectionDate
        });
        if (maintenanceId && normalizedValue(maintenanceId.value)) {
            parameters.set('excludeId', normalizedValue(maintenanceId.value));
        }

        fetch(contextPath + '/maintenance?' + parameters.toString(), {
            headers: { Accept: 'application/json' },
            signal: contextController.signal
        })
            .then(readJsonResponse)
            .then(function(context) {
                applyCustomerDefaults(context);
                updatePreviousContext(context.previous);
                updateDuplicateWarning(context.duplicate);
                showContextStatus(null);
            })
            .catch(function(error) {
                if (error.name !== 'AbortError'
                        && !window.Frog2Session.isSessionExpired(error)) {
                    console.error('Unable to load maintenance form context:', error);
                    showContextStatus(
                        '이전 점검 정보와 같은 달 등록 여부를 불러오지 못했습니다.');
                }
            });
    }

    function retryMaintenanceContext() {
        refreshFormContext();
    }

    function showContextStatus(message) {
        if (!contextStatus || !contextMessage) return;
        contextStatus.hidden = !message;
        if (message) contextMessage.textContent = message;
    }

    function readJsonResponse(response) {
        window.Frog2Session.requireActiveSession(response);
        if (!response.ok) {
            throw new Error('Maintenance request failed with status '
                + response.status);
        }
        return response.json();
    }

    function applyCustomerDefaults(context) {
        if (!context) return;
        applyIfUntouched(inspectorField, context.defaultInspector);
        if (formMode === 'add') {
            setFixedVersion(context.defaultVersion);
            setFixedCapacity(context.defaultLicenseSize);
        }
    }

    function applyIfUntouched(field, value) {
        if (!field || field.dataset.userModified === 'true') return;
        const normalized = normalizedValue(value);
        if (normalized) field.value = normalized;
    }

    function updatePreviousContext(previous) {
        if (!previousCallout || !previousButton) return;
        previousCallout.hidden = !previous;
        if (!previous) return;
        previousButton.dataset.inspector = previous.inspector || '';
        previousButton.dataset.licenseUsage = previous.licenseUsage || '';
        if (previousSummary) {
            previousSummary.textContent = formatDate(previous.inspectionDate)
                + ' 점검 · ' + (previous.version || '버전 미등록');
        }
    }

    function updateDuplicateWarning(duplicate) {
        if (!duplicateWarning || !duplicateMessage) return;
        duplicateWarning.hidden = !duplicate;
        if (!duplicate) return;
        duplicateMessage.textContent = formatDate(duplicate.inspectionDate)
            + '에 등록한 같은 달 점검 이력이 있습니다.';
        if (duplicateHistoryLink && customerField) {
            duplicateHistoryLink.href = contextPath
                + '/maintenance?view=history&customerName='
                + encodeURIComponent(customerField.value);
        }
    }

    function applyPreviousDefaults() {
        if (!previousButton) return;
        setFieldValue(inspectorField, previousButton.dataset.inspector);
        setFieldValue(licenseUsageField, previousButton.dataset.licenseUsage);
        calculateLicensePercentage();
        if (licenseUsageField) licenseUsageField.focus();
    }

    function setFixedVersion(value) {
        if (!versionField) return;
        versionField.value = normalizedValue(value);
        renderFixedVersion();
    }

    function renderFixedVersion() {
        if (!versionDisplay) return;
        const version = normalizedValue(versionField && versionField.value);
        versionDisplay.value = version || '미등록';
        versionDisplay.textContent = version || '미등록';
    }

    function setFixedCapacity(value) {
        if (!licenseSizeField) return;
        licenseSizeField.value = normalizeLegacyNumber(value);
        renderFixedCapacity();
        calculateLicensePercentage();
    }

    function renderFixedCapacity() {
        if (!licenseSizeDisplay) return;
        const rawCapacity = normalizedValue(
            licenseSizeField && licenseSizeField.value);
        const supportedCapacity = isSupportedCapacity(rawCapacity);
        const capacity = supportedCapacity
            ? normalizeLegacyNumber(rawCapacity)
            : rawCapacity;
        licenseSizeDisplay.value = capacity || '—';
        licenseSizeDisplay.textContent = capacity || '—';
        if (licenseCapacityUnit) licenseCapacityUnit.hidden = !supportedCapacity;
        if (licenseUsageUnit) licenseUsageUnit.hidden = !supportedCapacity;
        if (unsupportedCapacityHelp) {
            unsupportedCapacityHelp.hidden = supportedCapacity;
        }
        if (licenseUsageField) {
            licenseUsageField.readOnly = !supportedCapacity;
            const describedBy = [];
            if (!supportedCapacity) {
                describedBy.push('maintenanceUnsupportedCapacityHelp');
            }
            if (document.getElementById('maintenanceLicenseUsageError')) {
                describedBy.push('maintenanceLicenseUsageError');
            }
            if (describedBy.length > 0) {
                licenseUsageField.setAttribute(
                    'aria-describedby', describedBy.join(' '));
            } else {
                licenseUsageField.removeAttribute('aria-describedby');
            }
        }
    }

    function setFieldValue(field, value) {
        if (!field) return;
        field.value = normalizeLegacyNumber(value);
        field.dataset.userModified = 'true';
    }

    function normalizeLegacyNumber(value) {
        const normalized = normalizedValue(value);
        if (!normalized) return '';
        const match = normalized.match(
            /^([+-]?\d+(?:\.\d+)?)\s*(TB|GB)?$/i);
        if (!match) return normalized;
        const number = Number(match[1]);
        if (!Number.isFinite(number)) return normalized;
        return match[2] && match[2].toUpperCase() === 'GB'
            ? String(Number((number / 1024).toFixed(6)))
            : String(number);
    }

    function isSupportedCapacity(value) {
        const normalized = normalizedValue(value);
        return !normalized || /^([+-]?\d+(?:\.\d+)?)\s*(TB|GB)?$/i
            .test(normalized);
    }

    function calculateLicensePercentage() {
        if (!licensePercentageField || !licensePercentageDisplay) return;
        clearLicenseValidity();
        let percentage = '';
        if (!isSupportedCapacity(
            licenseSizeField && licenseSizeField.value)) {
            licensePercentageField.value = '';
            licensePercentageDisplay.value = '—';
            licensePercentageDisplay.textContent = '—';
            return;
        }
        const capacity = parsePositiveNumber(licenseSizeField);
        const used = parsePositiveNumber(licenseUsageField);
        if (capacity !== null && used !== null) {
            if (capacity === 0 && used > 0) {
                licenseUsageField.setCustomValidity(
                    '전체 용량이 0일 때 사용량을 입력할 수 없습니다.');
            } else {
                const calculatedPercentage = capacity === 0
                    ? 0
                    : (used / capacity) * 100;
                if (calculatedPercentage > maxLicensePercentage) {
                    licenseUsageField.setCustomValidity(
                        '계산된 사용률의 입력 범위를 확인해 주세요.');
                } else {
                    percentage = formatLicensePercentageHalfUp(
                        calculatedPercentage);
                }
            }
        }
        licensePercentageField.value = percentage;
        licensePercentageDisplay.value = percentage || '—';
        licensePercentageDisplay.textContent = percentage || '—';
    }

    function parsePositiveNumber(field) {
        if (!field) return null;
        const value = normalizedValue(field.value);
        if (!value) return null;
        const normalized = normalizeLegacyNumber(value);
        const number = Number(normalized);
        if (!Number.isFinite(number) || number < 0) {
            field.setCustomValidity('0 이상의 숫자로 입력해 주세요.');
            return null;
        }
        return number;
    }

    function clearLicenseValidity() {
        [licenseSizeField, licenseUsageField].forEach(function(field) {
            if (field) field.setCustomValidity('');
        });
    }

    function insertNoteTemplate() {
        if (!noteField) return;
        const template = [
            '점검 결과',
            '- ',
            '',
            '발견 이슈',
            '- 없음',
            '',
            '조치 사항',
            '- ',
            '',
            '후속 작업',
            '- 없음'
        ].join('\n');
        if (normalizedValue(noteField.value)
                && !window.Frog2UI.confirmAction(
                    '현재 메모를 양식으로 바꾸시겠습니까?')) {
            return;
        }
        noteField.value = template;
        noteField.dataset.userModified = 'true';
        autoResizeNote();
        updateNoteCount();
        noteField.focus();
        noteField.setSelectionRange(8, 8);
    }

    function autoResizeNote() {
        if (!noteField) return;
        noteField.style.height = 'auto';
        noteField.style.height = Math.min(noteField.scrollHeight, 520) + 'px';
    }

    function updateNoteCount() {
        if (noteField && noteCount) {
            noteCount.textContent = noteField.value.length.toLocaleString('ko-KR');
        }
    }

    function validateForm(event) {
        calculateLicensePercentage();
        if (!calendarController.hasValidDate()) {
            event.preventDefault();
            calendarController.focusForValidation();
            return;
        }
        if (!form.checkValidity()) {
            event.preventDefault();
            form.reportValidity();
            return;
        }
        submitting = true;
    }

    function warnAboutUnsavedChanges(event) {
        if (!submitting && serializeForm() !== initialFormState) {
            event.preventDefault();
            event.returnValue = '';
        }
    }

    function serializeForm() {
        return new URLSearchParams(new FormData(form)).toString();
    }

    function confirmDelete(event) {
        if (!window.Frog2UI.confirmAction('정말 삭제하시겠습니까?')) {
            event.preventDefault();
        }
    }

    function formatDate(value) {
        const normalized = normalizedValue(value);
        return normalized ? normalized.replaceAll('-', '.') : '날짜 미등록';
    }

    function normalizedValue(value) {
        return typeof value === 'string' ? value.trim() : '';
    }
})();
