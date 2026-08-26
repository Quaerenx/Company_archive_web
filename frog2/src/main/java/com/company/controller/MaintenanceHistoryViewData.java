package com.company.controller;

import com.company.model.CustomerDTO;
import com.company.model.MaintenanceHistoryFilter;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.PageResult;
import com.company.util.LicenseUsageSeriesBuilder;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable view data for the maintenance history page.
 *
 * <p>The servlet remains responsible for authentication, request validation,
 * DAO calls and forwarding. This class owns only the deterministic mapping
 * from query results to the JSP attribute contract.
 */
record MaintenanceHistoryViewData(
        List<MaintenanceRecordDTO> records,
        List<MaintenanceHistoryRowView> historyRows,
        List<Map<String, Object>> usageSeries,
        CustomerDTO customer,
        String customerName,
        int currentPage,
        int pageSize,
        int totalPages,
        int totalCount,
        Integer historyYear,
        String historyVersion,
        String historyQuery,
        boolean historyFiltersActive) {

    MaintenanceHistoryViewData {
        records = List.copyOf(records);
        historyRows = List.copyOf(historyRows);
        usageSeries = List.copyOf(usageSeries);
    }

    static MaintenanceHistoryViewData from(
            PageResult<MaintenanceRecordDTO> page,
            MaintenanceHistoryFilter filter,
            CustomerDTO customer,
            String customerName) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(filter, "filter");
        List<MaintenanceRecordDTO> records = page.items();
        return new MaintenanceHistoryViewData(
                records,
                MaintenanceHistoryRowView.fromRecords(records),
                LicenseUsageSeriesBuilder.build(records),
                customer,
                customerName,
                page.page(),
                page.pageSize(),
                page.totalPages(),
                page.totalCount(),
                filter.year(),
                filter.version(),
                filter.query(),
                filter.hasFilters());
    }

    void expose(HttpServletRequest request) {
        Objects.requireNonNull(request, "request");
        request.setAttribute("records", records);
        request.setAttribute("historyRows", historyRows);
        request.setAttribute("usageSeries", usageSeries);
        request.setAttribute("customer", customer);
        request.setAttribute("customerName", customerName);
        request.setAttribute("currentPage", currentPage);
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalCount", totalCount);
        request.setAttribute("historyYear", historyYear);
        request.setAttribute("historyVersion", historyVersion);
        request.setAttribute("historyQuery", historyQuery);
        request.setAttribute("historyFiltersActive", historyFiltersActive);
        request.setAttribute("viewType", "history");
    }
}
