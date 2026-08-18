(function() {
    'use strict';

    var root = document.querySelector('.customer-management[data-customer-list]');
    if (!root) {
        return;
    }

    var contextPath = root.getAttribute('data-context-path') || '';
    var currentFilter = root.getAttribute('data-filter') || 'maintenance';
    var currentSortField = root.getAttribute('data-sort-field') || '';
    var currentSortDirection = root.getAttribute('data-sort-direction') || 'ASC';
    var currentQuery = root.getAttribute('data-query') || '';
    var currentPageSize = root.getAttribute('data-page-size') || '50';

    document.addEventListener('DOMContentLoaded', function() {
        updateSortIcons();
        initializeRows();
        initializeSearch();
        initializeFilters();
        initializeSortLinks();
    });

    function updateSortIcons() {
        if (!currentSortField || !currentSortDirection) {
            return;
        }
        document.querySelectorAll('.sort-icon.active').forEach(function(icon) {
            icon.classList.remove('fa-sort');
            icon.classList.add(currentSortDirection === 'ASC' ? 'fa-sort-up' : 'fa-sort-down');
        });
    }

    function initializeRows() {
        document.querySelectorAll('.customer-table tbody tr').forEach(function(row, index) {
            row.style.animationDelay = (index * 0.05) + 's';
        });

        document.querySelectorAll('.customer-table td[title]').forEach(function(cell) {
            cell.addEventListener('mouseenter', function() {
                if (this.offsetWidth < this.scrollWidth) {
                    this.setAttribute('data-toggle', 'tooltip');
                }
            });
        });

    }

    function initializeSearch() {
        var searchForm = document.getElementById('customer-search-form');
        var searchInput = document.getElementById('search-input');
        var clearButton = document.getElementById('clear-search');

        if (!searchForm || !searchInput || !clearButton) {
            return;
        }

        clearButton.addEventListener('click', function() {
            searchInput.value = '';
            searchForm.requestSubmit();
        });
    }

    function initializeFilters() {
        document.querySelectorAll('.js-customer-filter[data-filter]').forEach(function(button) {
            button.addEventListener('click', function() {
                navigateToList(
                        button.getAttribute('data-filter'),
                        currentSortField,
                        currentSortDirection,
                        currentQuery);
            });
        });
    }

    function initializeSortLinks() {
        document.querySelectorAll('.js-customer-sort[data-sort-field]').forEach(function(link) {
            link.addEventListener('click', function(event) {
                event.preventDefault();
                var field = link.getAttribute('data-sort-field');
                var direction = currentSortField === field && currentSortDirection === 'ASC'
                        ? 'DESC'
                        : 'ASC';
                navigateToList(currentFilter, field, direction, currentQuery);
            });
        });
    }

    function navigateToList(filter, sortField, sortDirection, query) {
        var parameters = new URLSearchParams();
        parameters.set('view', 'list');
        parameters.set('filter', filter);
        parameters.set('pageSize', currentPageSize);
        if (sortField) {
            parameters.set('sortField', sortField);
            parameters.set('sortDirection', sortDirection);
        }
        if (query) {
            parameters.set('q', query);
        }
        window.location.href = contextPath + '/customers?' + parameters.toString();
    }
})();
