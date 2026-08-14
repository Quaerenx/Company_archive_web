(function() {
    'use strict';

    function formatLicensePercentageHalfUp(value) {
        if (!Number.isFinite(value) || value < 0) return '';
        const scaled = value * 10;
        const tolerance = Number.EPSILON
            * Math.max(1, Math.abs(scaled)) * 2;
        return (Math.floor(scaled + 0.5 + tolerance) / 10).toFixed(1);
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
    let visibleCalendarMonth = null;
    let initialFormState = '';
    let submitting = false;

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
        if (inspectionDateField) {
            inspectionDateField.addEventListener(
                'change', handleInspectionDateChange);
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

        initializeInlineCalendar();
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

    function initializeInlineCalendar() {
        if (!inspectionDateField || !inlineCalendar || !calendarGrid
                || !calendarMonthLabel) return;

        const selectedDate = parseCalendarDate(inspectionDateField.value);
        const initialDate = selectedDate || startOfCalendarDay(new Date());
        visibleCalendarMonth = startOfCalendarMonth(initialDate);
        inlineCalendar.hidden = false;

        calendarGrid.addEventListener('click', function(event) {
            const button = event.target.closest('[data-calendar-date]');
            if (!button || !calendarGrid.contains(button)) return;
            selectCalendarDate(button.dataset.calendarDate, true);
        });
        calendarGrid.addEventListener('keydown', handleCalendarKeydown);
        if (calendarPreviousButton) {
            calendarPreviousButton.addEventListener('click', function() {
                changeVisibleCalendarMonth(-1);
            });
        }
        if (calendarNextButton) {
            calendarNextButton.addEventListener('click', function() {
                changeVisibleCalendarMonth(1);
            });
        }
        if (calendarTodayButton) {
            calendarTodayButton.addEventListener('click', function() {
                const today = startOfCalendarDay(new Date());
                selectCalendarDate(formatCalendarValue(today), true);
                focusCalendarDate(today);
            });
        }
        renderInlineCalendar();
        enableInlineCalendarMode();
    }

    function enableInlineCalendarMode() {
        inspectionDateField.type = 'hidden';
        root.classList.add('is-calendar-enhanced');
    }

    function handleInspectionDateChange() {
        const selectedDate = parseCalendarDate(inspectionDateField.value);
        if (selectedDate) {
            visibleCalendarMonth = startOfCalendarMonth(selectedDate);
        }
        renderInlineCalendar();
        refreshFormContext();
    }

    function changeVisibleCalendarMonth(offset) {
        if (!visibleCalendarMonth) return;
        const targetMonth = new Date(
            visibleCalendarMonth.getFullYear(),
            visibleCalendarMonth.getMonth() + offset,
            1);
        if (!isSupportedCalendarYear(targetMonth)) return;
        visibleCalendarMonth = targetMonth;
        renderInlineCalendar();
    }

    function renderInlineCalendar(preferredFocusValue) {
        if (!visibleCalendarMonth || !calendarGrid || !calendarMonthLabel) {
            return;
        }
        const year = visibleCalendarMonth.getFullYear();
        const month = visibleCalendarMonth.getMonth();
        const selectedValue = normalizedValue(inspectionDateField.value);
        const todayValue = formatCalendarValue(startOfCalendarDay(new Date()));
        const firstWeekday = new Date(year, month, 1).getDay();
        const daysInMonth = new Date(year, month + 1, 0).getDate();
        const preferredValue = preferredFocusValue
            || calendarFocusValue(year, month, selectedValue, todayValue);
        const fragment = document.createDocumentFragment();

        calendarMonthLabel.textContent = year + '년 ' + (month + 1) + '월';
        calendarGrid.replaceChildren();

        for (let rowIndex = 0; rowIndex < 6; rowIndex += 1) {
            const row = document.createElement('div');
            row.className = 'maintenance-calendar-row';
            row.setAttribute('role', 'row');
            for (let columnIndex = 0; columnIndex < 7; columnIndex += 1) {
                const cellIndex = rowIndex * 7 + columnIndex;
                const dayNumber = cellIndex - firstWeekday + 1;
                const cell = document.createElement('span');
                cell.className = 'maintenance-calendar-cell';
                cell.setAttribute('role', 'gridcell');
                if (dayNumber < 1 || dayNumber > daysInMonth) {
                    cell.classList.add('maintenance-calendar-empty');
                    cell.setAttribute('aria-disabled', 'true');
                } else {
                    const date = new Date(year, month, dayNumber);
                    const dateValue = formatCalendarValue(date);
                    const selected = dateValue === selectedValue;
                    const dayButton = document.createElement('button');
                    dayButton.type = 'button';
                    dayButton.className = 'maintenance-calendar-day';
                    dayButton.dataset.calendarDate = dateValue;
                    dayButton.textContent = String(dayNumber);
                    dayButton.tabIndex = dateValue === preferredValue ? 0 : -1;
                    dayButton.setAttribute('aria-label', calendarDateLabel(date));
                    cell.setAttribute('aria-selected', String(selected));
                    if (selected) dayButton.classList.add('is-selected');
                    if (dateValue === todayValue) {
                        dayButton.classList.add('is-today');
                        dayButton.setAttribute('aria-current', 'date');
                    }
                    cell.appendChild(dayButton);
                }
                row.appendChild(cell);
            }
            fragment.appendChild(row);
        }
        calendarGrid.appendChild(fragment);
        updateSelectedDateLabel(parseCalendarDate(selectedValue));
    }

    function calendarFocusValue(year, month, selectedValue, todayValue) {
        const selectedDate = parseCalendarDate(selectedValue);
        if (selectedDate && selectedDate.getFullYear() === year
                && selectedDate.getMonth() === month) {
            return selectedValue;
        }
        const today = parseCalendarDate(todayValue);
        if (today && today.getFullYear() === year && today.getMonth() === month) {
            return todayValue;
        }
        return formatCalendarValue(new Date(year, month, 1));
    }

    function selectCalendarDate(dateValue, announceChange) {
        const selectedDate = parseCalendarDate(dateValue);
        if (!selectedDate || !inspectionDateField) return;
        inspectionDateField.value = formatCalendarValue(selectedDate);
        visibleCalendarMonth = startOfCalendarMonth(selectedDate);
        inspectionDateField.dispatchEvent(new Event('input', { bubbles: true }));
        inspectionDateField.dispatchEvent(new Event('change', { bubbles: true }));
        if (announceChange) updateSelectedDateLabel(selectedDate);
    }

    function handleCalendarKeydown(event) {
        const button = event.target.closest('[data-calendar-date]');
        if (!button) return;
        const currentDate = parseCalendarDate(button.dataset.calendarDate);
        if (!currentDate) return;

        let targetDate = null;
        switch (event.key) {
            case 'ArrowLeft':
                targetDate = addCalendarDays(currentDate, -1);
                break;
            case 'ArrowRight':
                targetDate = addCalendarDays(currentDate, 1);
                break;
            case 'ArrowUp':
                targetDate = addCalendarDays(currentDate, -7);
                break;
            case 'ArrowDown':
                targetDate = addCalendarDays(currentDate, 7);
                break;
            case 'Home':
                targetDate = addCalendarDays(currentDate, -currentDate.getDay());
                break;
            case 'End':
                targetDate = addCalendarDays(
                    currentDate, 6 - currentDate.getDay());
                break;
            case 'PageUp':
                targetDate = addCalendarMonths(currentDate, -1);
                break;
            case 'PageDown':
                targetDate = addCalendarMonths(currentDate, 1);
                break;
            default:
                return;
        }
        if (!isSupportedCalendarYear(targetDate)) return;
        event.preventDefault();
        focusCalendarDate(targetDate);
    }

    function focusCalendarDate(date) {
        visibleCalendarMonth = startOfCalendarMonth(date);
        const dateValue = formatCalendarValue(date);
        renderInlineCalendar(dateValue);
        const target = calendarGrid.querySelector(
            '[data-calendar-date="' + dateValue + '"]');
        if (target) target.focus();
    }

    function addCalendarDays(date, amount) {
        return new Date(date.getFullYear(), date.getMonth(), date.getDate() + amount);
    }

    function addCalendarMonths(date, amount) {
        const targetMonth = new Date(
            date.getFullYear(), date.getMonth() + amount, 1);
        const targetDay = Math.min(
            date.getDate(),
            new Date(
                targetMonth.getFullYear(),
                targetMonth.getMonth() + 1,
                0).getDate());
        return new Date(
            targetMonth.getFullYear(), targetMonth.getMonth(), targetDay);
    }

    function parseCalendarDate(value) {
        const match = normalizedValue(value).match(
            /^(\d{4})-(\d{2})-(\d{2})$/);
        if (!match) return null;
        const year = Number(match[1]);
        const month = Number(match[2]) - 1;
        const day = Number(match[3]);
        const date = new Date(year, month, day);
        if (date.getFullYear() !== year || date.getMonth() !== month
                || date.getDate() !== day) return null;
        return date;
    }

    function formatCalendarValue(date) {
        return String(date.getFullYear()).padStart(4, '0')
            + '-' + String(date.getMonth() + 1).padStart(2, '0')
            + '-' + String(date.getDate()).padStart(2, '0');
    }

    function calendarDateLabel(date) {
        const weekdays = [
            '일요일', '월요일', '화요일', '수요일',
            '목요일', '금요일', '토요일'
        ];
        return date.getFullYear() + '년 ' + (date.getMonth() + 1) + '월 '
            + date.getDate() + '일 ' + weekdays[date.getDay()];
    }

    function updateSelectedDateLabel(date) {
        if (!selectedDateLabel) return;
        selectedDateLabel.textContent = date
            ? calendarDateLabel(date)
            : '날짜를 선택해 주세요.';
    }

    function startOfCalendarDay(date) {
        return new Date(date.getFullYear(), date.getMonth(), date.getDate());
    }

    function startOfCalendarMonth(date) {
        return new Date(date.getFullYear(), date.getMonth(), 1);
    }

    function isSupportedCalendarYear(date) {
        const year = date && date.getFullYear();
        return Number.isInteger(year) && year >= 1 && year <= 9999;
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
                if (error.name !== 'AbortError') {
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
            if (supportedCapacity) {
                licenseUsageField.removeAttribute('aria-describedby');
            } else {
                licenseUsageField.setAttribute(
                    'aria-describedby',
                    'maintenanceUnsupportedCapacityHelp');
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
        if (!parseCalendarDate(
                inspectionDateField && inspectionDateField.value)) {
            event.preventDefault();
            updateSelectedDateLabel(null);
            focusCalendarForValidation();
            return;
        }
        if (!form.checkValidity()) {
            event.preventDefault();
            form.reportValidity();
            return;
        }
        submitting = true;
    }

    function focusCalendarForValidation() {
        const focusTarget = calendarGrid
            && calendarGrid.querySelector('[tabindex="0"]');
        if (focusTarget) {
            focusTarget.focus();
        } else if (inlineCalendar && !inlineCalendar.hidden) {
            inlineCalendar.focus();
        } else if (inspectionDateField) {
            inspectionDateField.focus();
        }
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
