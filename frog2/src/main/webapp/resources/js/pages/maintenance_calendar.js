(function(global) {
    'use strict';

    function normalizedValue(value) {
        return typeof value === 'string' ? value.trim() : '';
    }

    function parseDate(value) {
        var match = normalizedValue(value).match(/^(\d{4})-(\d{2})-(\d{2})$/);
        if (!match) return null;
        var year = Number(match[1]);
        var month = Number(match[2]) - 1;
        var day = Number(match[3]);
        var date = new Date(year, month, day);
        if (date.getFullYear() !== year || date.getMonth() !== month
                || date.getDate() !== day) return null;
        return date;
    }

    function formatValue(date) {
        return String(date.getFullYear()).padStart(4, '0')
            + '-' + String(date.getMonth() + 1).padStart(2, '0')
            + '-' + String(date.getDate()).padStart(2, '0');
    }

    function focusButton(grid, dateValue) {
        if (!grid || !dateValue) return false;
        var target = grid.querySelector(
            '[data-calendar-date="' + dateValue + '"]');
        if (!target || typeof target.focus !== 'function') return false;
        target.focus();
        return true;
    }

    function create(options) {
        var root = options.root;
        var dateField = options.dateField;
        var calendar = options.calendar;
        var monthLabel = options.monthLabel;
        var grid = options.grid;
        var previousButton = options.previousButton;
        var nextButton = options.nextButton;
        var todayButton = options.todayButton;
        var selectedDateLabel = options.selectedDateLabel;
        var onChange = typeof options.onChange === 'function'
            ? options.onChange
            : function() {};
        var calendarDocument = options.document || global.document;
        var visibleMonth = null;

        function initialize() {
            if (!dateField || !calendar || !grid || !monthLabel) return false;

            var selectedDate = parseDate(dateField.value);
            var initialDate = selectedDate || startOfDay(new Date());
            visibleMonth = startOfMonth(initialDate);
            calendar.hidden = false;

            grid.addEventListener('click', function(event) {
                var button = event.target.closest('[data-calendar-date]');
                if (!button || !grid.contains(button)) return;
                selectDate(button.dataset.calendarDate, true);
            });
            grid.addEventListener('keydown', handleKeydown);
            dateField.addEventListener('change', handleDateChange);
            if (previousButton) {
                previousButton.addEventListener('click', function() {
                    changeMonth(-1);
                });
            }
            if (nextButton) {
                nextButton.addEventListener('click', function() {
                    changeMonth(1);
                });
            }
            if (todayButton) {
                todayButton.addEventListener('click', function() {
                    var today = startOfDay(new Date());
                    selectDate(formatValue(today), true);
                    focusDate(today);
                });
            }

            render();
            dateField.type = 'hidden';
            root.classList.add('is-calendar-enhanced');
            return true;
        }

        function handleDateChange() {
            var restoreFocus = Boolean(
                grid && grid.contains(calendarDocument.activeElement));
            var selectedDate = parseDate(dateField.value);
            if (selectedDate) visibleMonth = startOfMonth(selectedDate);
            var selectedValue = selectedDate ? formatValue(selectedDate) : '';
            render(selectedValue);
            if (restoreFocus) focusButton(grid, selectedValue);
            onChange();
        }

        function changeMonth(offset) {
            if (!visibleMonth) return;
            var targetMonth = new Date(
                visibleMonth.getFullYear(),
                visibleMonth.getMonth() + offset,
                1);
            if (!isSupportedYear(targetMonth)) return;
            visibleMonth = targetMonth;
            render();
        }

        function render(preferredFocusValue) {
            if (!visibleMonth || !grid || !monthLabel) return;
            var year = visibleMonth.getFullYear();
            var month = visibleMonth.getMonth();
            var selectedValue = normalizedValue(dateField.value);
            var todayValue = formatValue(startOfDay(new Date()));
            var firstWeekday = new Date(year, month, 1).getDay();
            var daysInMonth = new Date(year, month + 1, 0).getDate();
            var preferredValue = preferredFocusValue
                || calendarFocusValue(year, month, selectedValue, todayValue);
            var fragment = calendarDocument.createDocumentFragment();

            monthLabel.textContent = year + '년 ' + (month + 1) + '월';
            grid.replaceChildren();

            for (var rowIndex = 0; rowIndex < 6; rowIndex += 1) {
                var row = calendarDocument.createElement('div');
                row.className = 'maintenance-calendar-row';
                row.setAttribute('role', 'row');
                for (var columnIndex = 0; columnIndex < 7; columnIndex += 1) {
                    var cellIndex = rowIndex * 7 + columnIndex;
                    var dayNumber = cellIndex - firstWeekday + 1;
                    var cell = calendarDocument.createElement('span');
                    cell.className = 'maintenance-calendar-cell';
                    cell.setAttribute('role', 'gridcell');
                    if (dayNumber < 1 || dayNumber > daysInMonth) {
                        cell.classList.add('maintenance-calendar-empty');
                        cell.setAttribute('aria-disabled', 'true');
                    } else {
                        var date = new Date(year, month, dayNumber);
                        var dateValue = formatValue(date);
                        var selected = dateValue === selectedValue;
                        var dayButton = calendarDocument.createElement('button');
                        dayButton.type = 'button';
                        dayButton.className = 'maintenance-calendar-day';
                        dayButton.dataset.calendarDate = dateValue;
                        dayButton.textContent = String(dayNumber);
                        dayButton.tabIndex = dateValue === preferredValue ? 0 : -1;
                        dayButton.setAttribute('aria-label', dateLabel(date));
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
            grid.appendChild(fragment);
            updateSelectedDateLabel(parseDate(selectedValue));
        }

        function calendarFocusValue(year, month, selectedValue, todayValue) {
            var selectedDate = parseDate(selectedValue);
            if (selectedDate && selectedDate.getFullYear() === year
                    && selectedDate.getMonth() === month) {
                return selectedValue;
            }
            var today = parseDate(todayValue);
            if (today && today.getFullYear() === year
                    && today.getMonth() === month) {
                return todayValue;
            }
            return formatValue(new Date(year, month, 1));
        }

        function selectDate(dateValue, announceChange) {
            var selectedDate = parseDate(dateValue);
            if (!selectedDate || !dateField) return;
            dateField.value = formatValue(selectedDate);
            visibleMonth = startOfMonth(selectedDate);
            dateField.dispatchEvent(new Event('input', {bubbles: true}));
            dateField.dispatchEvent(new Event('change', {bubbles: true}));
            if (announceChange) updateSelectedDateLabel(selectedDate);
        }

        function handleKeydown(event) {
            var button = event.target.closest('[data-calendar-date]');
            if (!button) return;
            var currentDate = parseDate(button.dataset.calendarDate);
            if (!currentDate) return;
            var targetDate = null;
            switch (event.key) {
                case 'ArrowLeft':
                    targetDate = addDays(currentDate, -1);
                    break;
                case 'ArrowRight':
                    targetDate = addDays(currentDate, 1);
                    break;
                case 'ArrowUp':
                    targetDate = addDays(currentDate, -7);
                    break;
                case 'ArrowDown':
                    targetDate = addDays(currentDate, 7);
                    break;
                case 'Home':
                    targetDate = addDays(currentDate, -currentDate.getDay());
                    break;
                case 'End':
                    targetDate = addDays(currentDate, 6 - currentDate.getDay());
                    break;
                case 'PageUp':
                    targetDate = addMonths(currentDate, -1);
                    break;
                case 'PageDown':
                    targetDate = addMonths(currentDate, 1);
                    break;
                default:
                    return;
            }
            if (!isSupportedYear(targetDate)) return;
            event.preventDefault();
            focusDate(targetDate);
        }

        function focusDate(date) {
            visibleMonth = startOfMonth(date);
            var dateValue = formatValue(date);
            render(dateValue);
            focusButton(grid, dateValue);
        }

        function focusForValidation() {
            var focusTarget = grid && grid.querySelector('[tabindex="0"]');
            if (focusTarget) {
                focusTarget.focus();
            } else if (calendar && !calendar.hidden) {
                calendar.focus();
            } else if (dateField) {
                dateField.focus();
            }
        }

        function updateSelectedDateLabel(date) {
            if (!selectedDateLabel) return;
            selectedDateLabel.textContent = date
                ? dateLabel(date)
                : '날짜를 선택해 주세요.';
        }

        function hasValidDate() {
            return Boolean(parseDate(dateField && dateField.value));
        }

        return {
            focusForValidation: focusForValidation,
            hasValidDate: hasValidDate,
            initialize: initialize
        };
    }

    function addDays(date, amount) {
        return new Date(date.getFullYear(), date.getMonth(), date.getDate() + amount);
    }

    function addMonths(date, amount) {
        var targetMonth = new Date(date.getFullYear(), date.getMonth() + amount, 1);
        var targetDay = Math.min(
            date.getDate(),
            new Date(targetMonth.getFullYear(), targetMonth.getMonth() + 1, 0)
                .getDate());
        return new Date(targetMonth.getFullYear(), targetMonth.getMonth(), targetDay);
    }

    function dateLabel(date) {
        var weekdays = [
            '일요일', '월요일', '화요일', '수요일',
            '목요일', '금요일', '토요일'
        ];
        return date.getFullYear() + '년 ' + (date.getMonth() + 1) + '월 '
            + date.getDate() + '일 ' + weekdays[date.getDay()];
    }

    function startOfDay(date) {
        return new Date(date.getFullYear(), date.getMonth(), date.getDate());
    }

    function startOfMonth(date) {
        return new Date(date.getFullYear(), date.getMonth(), 1);
    }

    function isSupportedYear(date) {
        var year = date && date.getFullYear();
        return Number.isInteger(year) && year >= 1 && year <= 9999;
    }

    var api = {
        create: create,
        focusButton: focusButton,
        formatValue: formatValue,
        parseDate: parseDate
    };

    if (typeof module === 'object' && module.exports) {
        module.exports = api;
    } else {
        global.Frog2MaintenanceCalendar = api;
    }
})(typeof window !== 'undefined' ? window : globalThis);
