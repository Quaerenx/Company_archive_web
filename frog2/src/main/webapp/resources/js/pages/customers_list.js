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

        document.querySelectorAll('.customer-row[data-detail-url]').forEach(function(row) {
            function openDetail() {
                window.location.href = row.dataset.detailUrl;
            }
            row.addEventListener('click', openDetail);
            row.addEventListener('keydown', function(event) {
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    openDetail();
                }
            });
        });
    }

    function initializeSearch() {
        var searchInput = document.getElementById('search-input');
        var clearButton = document.getElementById('clear-search');
        var searchCount = document.getElementById('search-count');
        var searchLabel = document.getElementById('search-text');
        var noResults = document.getElementById('no-results');

        if (!searchInput || !clearButton || !searchCount || !searchLabel || !noResults) {
            return;
        }

        searchInput.addEventListener('input', function() {
            var searchTerm = this.value.toLowerCase().trim();
            var rows = document.querySelectorAll('.customer-row');
            var visibleCount = 0;

            rows.forEach(function(row) {
                var rowSearchText = (row.getAttribute('data-search-text') || '').toLowerCase();
                if (!searchTerm || rowSearchText.includes(searchTerm)) {
                    row.classList.remove('hidden');
                    visibleCount++;
                } else {
                    row.classList.add('hidden');
                }
            });

            noResults.classList.toggle('d-none', visibleCount !== 0 || !searchTerm);
            clearButton.classList.toggle('d-none', !searchTerm);
            searchCount.textContent = searchTerm ? visibleCount + '/' + rows.length : '전체';
            searchLabel.textContent = searchTerm ? '검색 결과' : '결과 표시 중';
        });

        clearButton.addEventListener('click', function() {
            searchInput.value = '';
            searchInput.focus();
            searchInput.dispatchEvent(new Event('input'));
        });
    }

    function initializeFilters() {
        document.querySelectorAll('.js-customer-filter[data-filter]').forEach(function(button) {
            button.addEventListener('click', function() {
                navigateToList(button.getAttribute('data-filter'), currentSortField, currentSortDirection);
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
                navigateToList(currentFilter, field, direction);
            });
        });
    }

    function navigateToList(filter, sortField, sortDirection) {
        var parameters = new URLSearchParams();
        parameters.set('view', 'list');
        parameters.set('filter', filter);
        if (sortField) {
            parameters.set('sortField', sortField);
            parameters.set('sortDirection', sortDirection);
        }
        window.location.href = contextPath + '/customers?' + parameters.toString();
    }
})();
