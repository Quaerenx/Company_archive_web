(function () {
    'use strict';

    var generatedId = 0;

    function text(value) {
        return String(value || '').trim();
    }

    function normalize(value) {
        return text(value).toLocaleLowerCase('ko-KR');
    }

    function initialize(select) {
        if (!select || select.dataset.uiCustomerComboboxReady === 'true') {
            return;
        }

        var placeholderOption = Array.prototype.find.call(
            select.options,
            function (option) { return option.value === ''; });
        var options = Array.prototype.filter.call(
            select.options,
            function (option) { return option.value !== '' && !option.disabled; });
        if (!options.length) return;

        generatedId += 1;
        var listboxId = 'ui-customer-combobox-list-' + generatedId;
        var required = select.required;
        var root = document.createElement('div');
        root.className = 'ui-customer-combobox';
        var input = document.createElement('input');
        input.type = 'search';
        input.className = 'ui-customer-combobox__input';
        input.autocomplete = 'off';
        input.spellcheck = false;
        input.placeholder = placeholderOption
            ? text(placeholderOption.textContent)
            : '고객사를 검색하세요';
        input.required = required;
        input.setAttribute('role', 'combobox');
        input.setAttribute('aria-autocomplete', 'list');
        input.setAttribute('aria-controls', listboxId);
        input.setAttribute('aria-expanded', 'false');
        input.setAttribute('aria-haspopup', 'listbox');
        ['aria-describedby', 'aria-invalid'].forEach(function (attribute) {
            var value = select.getAttribute(attribute);
            if (value) input.setAttribute(attribute, value);
        });
        if (select.id) {
            input.id = select.id + '-combobox';
            Array.prototype.forEach.call(document.querySelectorAll('label[for]'),
                function (label) {
                    if (label.htmlFor === select.id) label.htmlFor = input.id;
                });
        }

        var toggle = document.createElement('button');
        toggle.type = 'button';
        toggle.className = 'ui-customer-combobox__toggle';
        toggle.setAttribute('aria-label', '고객사 목록 열기');
        toggle.setAttribute('aria-controls', listboxId);
        toggle.innerHTML = '<span aria-hidden="true"></span>';

        var listbox = document.createElement('ul');
        listbox.id = listboxId;
        listbox.className = 'ui-customer-combobox__list';
        listbox.setAttribute('role', 'listbox');
        listbox.hidden = true;

        root.appendChild(input);
        root.appendChild(toggle);
        root.appendChild(listbox);
        select.insertAdjacentElement('afterend', root);

        select.dataset.uiCustomerComboboxReady = 'true';
        select.classList.add('ui-customer-combobox__native');
        select.required = false;
        select.tabIndex = -1;
        select.setAttribute('aria-hidden', 'true');

        var visibleOptions = [];
        var activeIndex = -1;
        var syncingFromInput = false;

        function selectedOption() {
            return Array.prototype.find.call(select.options, function (option) {
                return option.value === select.value;
            });
        }

        function syncFromSelect() {
            var option = selectedOption();
            input.value = option && option.value
                ? text(option.textContent)
                : '';
            input.setCustomValidity('');
        }

        function setExpanded(expanded) {
            listbox.hidden = !expanded;
            input.setAttribute('aria-expanded', String(expanded));
            root.classList.toggle('is-open', expanded);
            toggle.setAttribute(
                'aria-label', expanded ? '고객사 목록 닫기' : '고객사 목록 열기');
            if (!expanded) {
                activeIndex = -1;
                input.removeAttribute('aria-activedescendant');
            }
        }

        function setActive(index) {
            if (!visibleOptions.length) {
                activeIndex = -1;
                input.removeAttribute('aria-activedescendant');
                return;
            }
            activeIndex = (index + visibleOptions.length) % visibleOptions.length;
            listbox.querySelectorAll('[role="option"]').forEach(
                function (item, itemIndex) {
                    var active = itemIndex === activeIndex;
                    item.classList.toggle('is-active', active);
                    item.setAttribute('aria-selected', String(active));
                    if (active) {
                        input.setAttribute('aria-activedescendant', item.id);
                        item.scrollIntoView({block: 'nearest'});
                    }
                });
        }

        function selectOption(option) {
            select.value = option.value;
            input.value = text(option.textContent);
            input.setCustomValidity('');
            select.dispatchEvent(new Event('change', {bubbles: true}));
            setExpanded(false);
        }

        function renderOptions(query) {
            var needle = normalize(query);
            visibleOptions = options.filter(function (option) {
                return !needle || normalize(option.textContent).indexOf(needle) >= 0;
            });
            listbox.textContent = '';
            visibleOptions.forEach(function (option, index) {
                var item = document.createElement('li');
                item.id = listboxId + '-option-' + index;
                item.className = 'ui-customer-combobox__option';
                item.setAttribute('role', 'option');
                item.setAttribute('aria-selected', 'false');
                item.textContent = text(option.textContent);
                item.addEventListener('mousedown', function (event) {
                    event.preventDefault();
                });
                item.addEventListener('click', function () {
                    selectOption(option);
                    input.focus();
                });
                item.addEventListener('mouseenter', function () {
                    setActive(index);
                });
                listbox.appendChild(item);
            });
            if (!visibleOptions.length) {
                var empty = document.createElement('li');
                empty.className = 'ui-customer-combobox__empty';
                empty.setAttribute('role', 'presentation');
                empty.textContent = '일치하는 고객사가 없습니다.';
                listbox.appendChild(empty);
            }
            activeIndex = -1;
            input.removeAttribute('aria-activedescendant');
        }

        function syncSelectionFromInput() {
            var needle = normalize(input.value);
            var match = options.find(function (option) {
                return normalize(option.textContent) === needle;
            });
            var nextValue = match ? match.value : '';
            var changed = select.value !== nextValue;
            select.value = nextValue;
            input.setCustomValidity(
                input.value && !match ? '목록에서 고객사를 선택해 주세요.' : '');
            if (changed) {
                syncingFromInput = true;
                try {
                    select.dispatchEvent(new Event('change', {bubbles: true}));
                } finally {
                    syncingFromInput = false;
                }
            }
        }

        input.addEventListener('focus', function () {
            renderOptions(input.value);
            setExpanded(true);
        });
        input.addEventListener('input', function () {
            syncSelectionFromInput();
            renderOptions(input.value);
            setExpanded(true);
        });
        input.addEventListener('keydown', function (event) {
            if (event.key === 'ArrowDown') {
                event.preventDefault();
                if (listbox.hidden) {
                    renderOptions(input.value);
                    setExpanded(true);
                }
                setActive(activeIndex + 1);
            } else if (event.key === 'ArrowUp') {
                event.preventDefault();
                if (listbox.hidden) {
                    renderOptions(input.value);
                    setExpanded(true);
                }
                setActive(activeIndex - 1);
            } else if (event.key === 'Enter' && activeIndex >= 0) {
                event.preventDefault();
                selectOption(visibleOptions[activeIndex]);
            } else if (event.key === 'Escape') {
                event.preventDefault();
                setExpanded(false);
            } else if (event.key === 'Tab') {
                setExpanded(false);
            }
        });
        toggle.addEventListener('click', function () {
            if (listbox.hidden) {
                renderOptions('');
                setExpanded(true);
                input.focus();
            } else {
                setExpanded(false);
                input.focus();
            }
        });
        select.addEventListener('change', function () {
            if (!syncingFromInput) syncFromSelect();
        });
        document.addEventListener('click', function (event) {
            if (!root.contains(event.target)) setExpanded(false);
        });
        if (select.form) {
            select.form.addEventListener('reset', function () {
                window.setTimeout(syncFromSelect, 0);
            });
        }

        syncFromSelect();
    }

    function initializeAll() {
        document.querySelectorAll('select[data-ui-customer-combobox]')
            .forEach(initialize);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initializeAll);
    } else {
        initializeAll();
    }
}());
