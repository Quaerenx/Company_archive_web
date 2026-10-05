package com.company.controller;

import com.company.util.BusinessDate;
import com.company.util.StrictDateParser;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import com.company.model.CustomerDAO;
import com.company.model.CustomerAssignmentDAO;
import com.company.model.CustomerDTO;
import com.company.model.MaintenanceCustomerAssignment;
import com.company.model.MaintenanceFormHistoryContext;
import com.company.model.MaintenanceHistoryFilter;
import com.company.model.MaintenanceRecordDAO;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.MaintenanceSchedule;
import com.company.model.PageResult;
import com.company.model.UserDTO;
import com.company.security.SessionPrincipal;
import com.company.web.ApplicationError;
import com.company.web.CsvResponse;
import com.company.web.JsonResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// @WebServlet("/maintenance") - web.xml에서 매핑하므로 주석 처리
public class MaintenanceServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final int HISTORY_PAGE_SIZE = 20;
    private static final int EXPORT_LIMIT = 10_000;
    private static final String MAINTENANCE_CUSTOMER_TYPE =
            "정기점검 계약 고객사";
    private final MaintenanceRecordDAO maintenanceDAO;
    private final CustomerDAO customerDAO;
    private final CustomerAssignmentDAO customerAssignmentDAO;
    private final Clock clock;
    private final MaintenanceRecordRequestMapper requestMapper =
            new MaintenanceRecordRequestMapper();

    public MaintenanceServlet() {
        this(new MaintenanceRecordDAO(), new CustomerDAO(),
                new CustomerAssignmentDAO(),
                BusinessDate.systemClock());
    }

    MaintenanceServlet(MaintenanceRecordDAO maintenanceDAO) {
        this(maintenanceDAO, new CustomerDAO(), new CustomerAssignmentDAO(),
                BusinessDate.systemClock());
    }

    MaintenanceServlet(
            MaintenanceRecordDAO maintenanceDAO,
            CustomerDAO customerDAO) {
        this(maintenanceDAO, customerDAO, new CustomerAssignmentDAO(),
                BusinessDate.systemClock());
    }

    MaintenanceServlet(
            MaintenanceRecordDAO maintenanceDAO,
            CustomerDAO customerDAO,
            Clock clock) {
        this(maintenanceDAO, customerDAO, new CustomerAssignmentDAO(), clock);
    }

    MaintenanceServlet(
            MaintenanceRecordDAO maintenanceDAO,
            CustomerDAO customerDAO,
            CustomerAssignmentDAO customerAssignmentDAO,
            Clock clock) {
        this.maintenanceDAO = Objects.requireNonNull(
                maintenanceDAO, "maintenanceDAO");
        this.customerDAO = Objects.requireNonNull(
                customerDAO, "customerDAO");
        this.customerAssignmentDAO = Objects.requireNonNull(
                customerAssignmentDAO, "customerAssignmentDAO");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        UserDTO user = SessionPrincipal.expose(request, session);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        FlashMessage.expose(request);
        exposeCardsReturnContext(request);

        String viewType = request.getParameter("view");
        if (viewType == null || viewType.isEmpty()) {
            viewType = "cards";
        }
        switch (viewType) {
            case "cards" -> showCards(request, response, user);
            case "history" -> showHistory(request, response, user);
            case "export" -> exportHistory(request, response);
            case "add" -> showAddForm(
                    request,
                    response,
                    request.getParameter("customerName"),
                    null,
                    Map.of(),
                    HttpServletResponse.SC_OK,
                    user);
            case "formContext" -> writeFormContext(request, response, user);
            case "edit" -> showEdit(request, response, user);
            default -> redirectToCards(request, response);
        }
    }

    private void exportHistory(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String customerName = request.getParameter("customerName");
        if (customerName == null || customerName.isBlank()) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "missing_customer_name",
                    "고객사명이 필요합니다.");
            return;
        }
        MaintenanceHistoryFilter filter;
        try {
            filter = MaintenanceHistoryFilter.parse(
                    request.getParameter("historyYear"),
                    request.getParameter("historyVersion"),
                    request.getParameter("historyQuery"));
        } catch (IllegalArgumentException exception) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_history_filter",
                    "점검 이력 검색 조건이 올바르지 않습니다.");
            return;
        }
        PageResult<MaintenanceRecordDTO> page =
                maintenanceDAO.getMaintenanceRecordsByCustomer(
                        customerName.strip(), 1, EXPORT_LIMIT + 1, filter);
        if (page.totalCount() > EXPORT_LIMIT) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "maintenance_export_too_large",
                    "검색 조건을 좁힌 뒤 다시 내려받아 주세요.");
            return;
        }

        List<List<String>> rows = MaintenanceHistoryRowView
                .fromRecords(page.items()).stream()
                .map(row -> List.of(
                        csvValue(row.getRecord().getInspectionDate()),
                        csvValue(row.getRecord().getVerticaVersion()),
                        usageSummary(row),
                        csvValue(row.getUsagePercentage()),
                        csvValue(row.getDeltaLabel()),
                        csvValue(row.getRecord().getInspectorName()),
                        csvValue(row.getRecord().getNote())))
                .toList();
        CsvResponse.write(
                response,
                "maintenance-history.csv",
                List.of(
                        "점검일", "버전", "라이선스 사용량", "사용률(%)",
                        "이전 대비", "점검자", "점검 내용"),
                rows);
    }

    private static String usageSummary(MaintenanceHistoryRowView row) {
        String used = row.getUsedTerabytes();
        String capacity = row.getCapacityTerabytes();
        if (used != null && capacity != null) {
            return used + " / " + capacity + " TB";
        }
        if (used != null) {
            return used + " TB 사용";
        }
        return capacity == null ? "" : capacity + " TB 한도";
    }

    private static String csvValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private void showCards(
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO user) throws ServletException, IOException {
        YearMonth selectedMonth;
        String registrationStatus;
        try {
            selectedMonth = parseCardsMonth(request.getParameter("maintenanceMonth"));
            registrationStatus = parseRegistrationStatus(request.getParameter("registrationStatus"));
        } catch (IllegalArgumentException exception) {
            ApplicationError.send(request, response, HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_maintenance_filter", "정기점검 조회 월 또는 등록 상태가 올바르지 않습니다.");
            return;
        }
        List<CustomerDTO> personalMaintenanceCustomers =
                customerAssignmentDAO.getMaintenanceCustomersByAssignee(
                        user.getUserId(), user.getUserName());
        Map<String, List<CustomerDTO>> inspectorCustomers =
                excludePersonalCustomers(
                        prioritizeInspector(
                                getInspectorCustomersMap(),
                                user.getUserName()),
                        personalMaintenanceCustomers);
        List<MaintenanceCustomerAssignment> assignments =
                customerAssignmentDAO.getAllMaintenanceCustomerAssignments();
        Map<String, Boolean> registeredCustomers = getCurrentMonthMaintenanceCustomers(
                inspectorCustomers, personalMaintenanceCustomers, selectedMonth);
        Map<String, Boolean> dueCustomers = getMaintenanceDueCustomers(
                inspectorCustomers, personalMaintenanceCustomers, assignments, selectedMonth);
        request.setAttribute("maintenanceMonthParam", selectedMonth.toString());
        request.setAttribute("maintenanceMonthLabel", selectedMonth.toString());
        request.setAttribute("registrationStatus", registrationStatus);
        request.setAttribute("personalMaintenanceAssignedCount", personalMaintenanceCustomers.size());
        request.setAttribute("globalMaintenanceCustomerCount",
                inspectorCustomers.values().stream().mapToInt(List::size).sum());
        Map<String, List<CustomerDTO>> filteredInspectorCustomers = new LinkedHashMap<>();
        inspectorCustomers.forEach((inspector, customers) -> {
            List<CustomerDTO> filtered = filterMaintenanceCustomers(
                    customers, registrationStatus, registeredCustomers, dueCustomers);
            if (!filtered.isEmpty()) {
                filteredInspectorCustomers.put(inspector, filtered);
            }
        });
        request.setAttribute("inspectorCustomers", filteredInspectorCustomers);
        request.setAttribute(
                "personalMaintenanceCustomers",
                filterMaintenanceCustomers(personalMaintenanceCustomers,
                        registrationStatus, registeredCustomers, dueCustomers));
        request.setAttribute(
                "maintenanceFrequencyLabels",
                getMaintenanceFrequencyLabels(assignments));
        request.setAttribute("currentMonthMaintenanceCustomers", registeredCustomers);
        request.setAttribute("maintenanceDueCustomers", dueCustomers);
        request.setAttribute("viewType", "cards");
        request.getRequestDispatcher("/maintenance/maintenance_cards.jsp")
                .forward(request, response);
    }

    private YearMonth parseCardsMonth(String rawMonth) {
        String value = trimToNull(rawMonth);
        if (value == null) {
            return BusinessDate.currentMonth(clock);
        }
        if (!value.matches("[0-9]{4}-[0-9]{2}")) {
            throw new IllegalArgumentException("Maintenance month must use YYYY-MM");
        }
        try {
            YearMonth month = YearMonth.parse(value);
            if (month.getYear() < 1900 || month.getYear() > 2100) {
                throw new IllegalArgumentException("Maintenance month is outside the supported range");
            }
            return month;
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Maintenance month is invalid", exception);
        }
    }

    private String parseRegistrationStatus(String rawStatus) {
        String status = trimToNull(rawStatus);
        if (status == null) {
            return "all";
        }
        return switch (status) {
            case "all", "registered", "unregistered" -> status;
            default -> throw new IllegalArgumentException("Registration status is invalid");
        };
    }

    private CardsReturnContext cardsReturnContext(HttpServletRequest request) {
        String rawMonth = trimToNull(request.getParameter("returnCardsMonth"));
        if (rawMonth == null) {
            return null;
        }
        try {
            return new CardsReturnContext(
                    parseCardsMonth(rawMonth).toString(),
                    parseRegistrationStatus(request.getParameter("returnCardsStatus")));
        } catch (IllegalArgumentException exception) {
            // Invalid optional navigation hints must not prevent viewing or saving history.
            return null;
        }
    }

    private void exposeCardsReturnContext(HttpServletRequest request) {
        CardsReturnContext context = cardsReturnContext(request);
        if (context != null) {
            request.setAttribute("returnCardsMonth", context.month());
            request.setAttribute("returnCardsStatus", context.registrationStatus());
        }
    }

    private record CardsReturnContext(String month, String registrationStatus) {
    }

    private List<CustomerDTO> filterMaintenanceCustomers(
            List<CustomerDTO> customers,
            String registrationStatus,
            Map<String, Boolean> registeredCustomers,
            Map<String, Boolean> dueCustomers) {
        if ("all".equals(registrationStatus)) {
            return customers;
        }
        return customers.stream().filter(customer -> {
            String name = customer.getCustomerName();
            boolean registered = Boolean.TRUE.equals(registeredCustomers.get(name));
            return "registered".equals(registrationStatus)
                    ? registered
                    : Boolean.TRUE.equals(dueCustomers.get(name)) && !registered;
        }).toList();
    }

    private void showHistory(
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO user) throws ServletException, IOException {
        String customerName = request.getParameter("customerName");
        if (customerName == null || customerName.isEmpty()) {
            redirectToCards(request, response);
            return;
        }
        int historyPage;
        try {
            historyPage = parseHistoryPage(
                    request.getParameter("historyPage"));
        } catch (IllegalArgumentException exception) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_history_page",
                    "점검 이력 페이지가 올바르지 않습니다.");
            return;
        }
        MaintenanceHistoryFilter historyFilter;
        try {
            historyFilter = MaintenanceHistoryFilter.parse(
                    request.getParameter("historyYear"),
                    request.getParameter("historyVersion"),
                    request.getParameter("historyQuery"));
        } catch (IllegalArgumentException exception) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_history_filter",
                    "점검 이력 검색 조건이 올바르지 않습니다.");
            return;
        }
        MaintenanceRecordDAO.MaintenanceHistoryPage history =
                maintenanceDAO.getMaintenanceHistoryPageByCustomer(
                        customerName,
                        historyPage,
                        HISTORY_PAGE_SIZE,
                        historyFilter);
        CustomerDTO customer = customerDAO.getCustomerByName(customerName);
        MaintenanceHistoryViewData.from(
                history.page(), historyFilter, customer, customerName,
                history.olderRecord())
                .expose(request);
        request.setAttribute(
                "canManageCustomer",
                canManageCustomer(user, customerName));
        request.getRequestDispatcher("/maintenance/maintenance_history.jsp")
                .forward(request, response);
    }

    private void showEdit(
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO user) throws ServletException, IOException {
        Long maintenanceId = parsePositiveLong(request.getParameter("id"));
        if (maintenanceId == null) {
            sendInvalidMaintenanceId(request, response);
            return;
        }
        MaintenanceRecordDTO record =
                maintenanceDAO.getMaintenanceRecordById(maintenanceId);
        if (record != null
                && canManageCustomer(user, record.getCustomerName())) {
            request.setAttribute("record", record);
            prepareFormView(
                    request,
                    maintenanceFormOptions(record.getInspectorName(), user),
                    record,
                    Map.of(),
                    true);
            request.setAttribute("viewType", "edit");
            request.getRequestDispatcher(
                    "/maintenance/maintenance_edit.jsp")
                    .forward(request, response);
            return;
        }
        if (record == null) {
            FlashMessage.redirect(
                    request,
                    response,
                    cardsReturnLocation(request),
                    "해당 정기점검 이력을 찾을 수 없습니다.",
                    "error");
            return;
        }
        FlashMessage.redirect(
                request,
                response,
                historyReturnLocation(request, record.getCustomerName()),
                "수정 권한이 없습니다.",
                "error");
    }

 // 담당자별 고객사 목록을 Map으로 구성 (정기점검 계약 고객사이면서 활성 상태인 것만)
    private Map<String, List<CustomerDTO>> getInspectorCustomersMap() {
        Map<String, List<CustomerDTO>> inspectorCustomers = new LinkedHashMap<>();

        // getAllCustomers already filters inactive customers.
        List<CustomerDTO> allCustomers = customerDAO.getAllCustomers("manager_name", "ASC", "maintenance");
        for (CustomerDTO customer : allCustomers) {
            String mainManager = customer.getManagerName();
            String customerType = customer.getCustomerType();

            if (mainManager != null && !mainManager.trim().isEmpty()
                    && "정기점검 계약 고객사".equals(customerType)) {
                inspectorCustomers.computeIfAbsent(mainManager.trim(), key -> new ArrayList<>()).add(customer);
            }
        }
        return inspectorCustomers;
    }

    static Map<String, List<CustomerDTO>> prioritizeInspector(
            Map<String, List<CustomerDTO>> inspectorCustomers,
            String preferredInspector) {
        Objects.requireNonNull(inspectorCustomers, "inspectorCustomers");
        Map<String, List<CustomerDTO>> ordered = new LinkedHashMap<>();
        String preferred = preferredInspector == null
                ? null
                : preferredInspector.trim();
        if (preferred != null && !preferred.isEmpty()) {
            List<CustomerDTO> preferredCustomers =
                    inspectorCustomers.get(preferred);
            if (preferredCustomers != null) {
                ordered.put(preferred, preferredCustomers);
            }
        }
        inspectorCustomers.forEach(ordered::putIfAbsent);
        return ordered;
    }

    private Map<String, List<CustomerDTO>> excludePersonalCustomers(
            Map<String, List<CustomerDTO>> inspectorCustomers,
            List<CustomerDTO> personalMaintenanceCustomers) {
        Set<String> personalCustomerNames = new HashSet<>();
        for (CustomerDTO customer : personalMaintenanceCustomers) {
            String customerName = trimToNull(customer.getCustomerName());
            if (customerName != null) {
                personalCustomerNames.add(customerName);
            }
        }
        Map<String, List<CustomerDTO>> remainingCustomers = new LinkedHashMap<>();
        inspectorCustomers.forEach((inspector, customers) -> {
            List<CustomerDTO> remaining = customers.stream()
                    .filter(customer -> !personalCustomerNames.contains(
                            trimToNull(customer.getCustomerName())))
                    .toList();
            if (!remaining.isEmpty()) {
                remainingCustomers.put(inspector, remaining);
            }
        });
        return remainingCustomers;
    }

    private Map<String, String> getMaintenanceFrequencyLabels(
            List<MaintenanceCustomerAssignment> assignments) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (MaintenanceCustomerAssignment assignment : assignments) {
            labels.put(
                    assignment.customerName(),
                    assignment.schedule().isQuarterly() ? "분기" : "월별");
        }
        return labels;
    }

    private Map<String, Boolean> getMaintenanceDueCustomers(
            Map<String, List<CustomerDTO>> inspectorCustomers,
            List<CustomerDTO> personalMaintenanceCustomers,
            List<MaintenanceCustomerAssignment> assignments,
            YearMonth selectedMonth) {
        Map<String, Boolean> dueByName = new LinkedHashMap<>();
        for (MaintenanceCustomerAssignment assignment : assignments) {
            String name = trimToNull(assignment.customerName());
            if (name != null) {
                dueByName.putIfAbsent(name, assignment.schedule().isDue(selectedMonth));
            }
        }
        boolean defaultDue = MaintenanceSchedule.monthlyDefault().isDue(selectedMonth);
        Map<String, Boolean> dueCustomers = new LinkedHashMap<>();
        Stream.concat(inspectorCustomers.values().stream().flatMap(List::stream),
                personalMaintenanceCustomers.stream()).forEach(customer -> {
                    String name = customer.getCustomerName();
                    String normalizedName = trimToNull(name);
                    if (normalizedName != null) {
                        dueCustomers.put(name, dueByName.getOrDefault(normalizedName, defaultDue));
                    }
                });
        return Map.copyOf(dueCustomers);
    }

    private Map<String, Boolean> getCurrentMonthMaintenanceCustomers(
            Map<String, List<CustomerDTO>> inspectorCustomers,
            List<CustomerDTO> personalMaintenanceCustomers,
            YearMonth selectedMonth) {
        List<String> customerNames = Stream.concat(
                        inspectorCustomers.values().stream().flatMap(List::stream),
                        personalMaintenanceCustomers.stream())
                .map(CustomerDTO::getCustomerName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .toList();
        if (customerNames.isEmpty()) {
            return Map.of();
        }

        Date startDate = Date.valueOf(selectedMonth.atDay(1));
        Date endDate = Date.valueOf(selectedMonth.plusMonths(1).atDay(1));
        Map<String, Boolean> registeredCustomers = new LinkedHashMap<>();
        for (MaintenanceRecordDTO record
                : maintenanceDAO.getMaintenanceRecordsByMonthForCustomers(
                        startDate, endDate, customerNames)) {
            String customerName = record.getCustomerName();
            if (customerName != null && !customerName.isBlank()) {
                registeredCustomers.put(customerName, Boolean.TRUE);
            }
        }
        return Map.copyOf(registeredCustomers);
    }

    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        UserDTO currentUser = SessionPrincipal.from(session);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        exposeCardsReturnContext(request);

        String actionType = request.getParameter("action");
        if (actionType == null) {
            redirectToCards(request, response);
            return;
        }
        switch (actionType) {
            case "add" -> addRecord(request, response, currentUser);
            case "update" -> updateRecord(request, response, currentUser);
            case "delete" -> deleteRecord(request, response, currentUser);
            default -> redirectToCards(request, response);
        }
    }

    private void addRecord(
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO currentUser) throws ServletException, IOException {
        MaintenanceFormSubmission submission = requestMapper.map(
                request::getParameter,
                currentUser.getUserId(),
                maintenanceFormOptions(null, currentUser));
        MaintenanceRecordDTO record = submission.record();
        if (!submission.valid()) {
            showAddForm(
                    request,
                    response,
                    record.getCustomerName(),
                    record,
                    submission.fieldErrors(),
                    HttpServletResponse.SC_BAD_REQUEST,
                    currentUser);
            return;
        }
        if (!canManageCustomer(currentUser, record.getCustomerName())) {
            FlashMessage.redirect(
                    request,
                    response,
                    cardsReturnLocation(request),
                    "담당 고객사만 이력을 추가할 수 있습니다.",
                    "error");
            return;
        }

        boolean success = maintenanceDAO.addMaintenanceRecord(record);
        FlashMessage.redirect(
                request,
                response,
                historyReturnLocation(request, record.getCustomerName()),
                success
                        ? "정기점검 이력이 성공적으로 추가되었습니다."
                        : "정기점검 이력 추가 중 오류가 발생했습니다.",
                success ? "success" : "error");
    }

    private void updateRecord(
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO currentUser) throws ServletException, IOException {
        Long maintenanceId = parsePositiveLong(
                request.getParameter("maintenance_id"));
        if (maintenanceId == null) {
            sendInvalidMaintenanceId(request, response);
            return;
        }
        MaintenanceRecordDTO existing =
                maintenanceDAO.getMaintenanceRecordById(maintenanceId);
        if (existing == null
                || !canManageCustomer(
                        currentUser, existing.getCustomerName())) {
            FlashMessage.redirect(
                    request,
                    response,
                    cardsReturnLocation(request),
                    "수정 권한이 없거나 이력을 찾을 수 없습니다.",
                    "error");
            return;
        }
        MaintenanceFormOptions options = maintenanceFormOptions(
                existing.getInspectorName(), currentUser);
        MaintenanceFormSubmission submission = requestMapper.mapForUpdate(
                request::getParameter,
                currentUser.getUserId(),
                options,
                maintenanceId,
                existing.getLicenseSizeGb(),
                existing.getVerticaVersion(),
                existing.getLicenseUsageSize(),
                existing.getLicenseUsagePct());
        MaintenanceRecordDTO record = submission.record();
        if (!submission.valid()) {
            request.setAttribute("record", record);
            prepareFormView(
                    request,
                    options,
                    record,
                    submission.fieldErrors(),
                    true);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("viewType", "edit");
            request.getRequestDispatcher(
                    "/maintenance/maintenance_edit.jsp")
                    .forward(request, response);
            return;
        }

        boolean success = maintenanceDAO.updateMaintenanceRecordForCustomer(
                record, existing.getCustomerName());
        FlashMessage.redirect(
                request,
                response,
                historyReturnLocation(request, record.getCustomerName()),
                success
                        ? "정기점검 이력이 성공적으로 수정되었습니다."
                        : "정기점검 이력 수정 중 오류가 발생했습니다.",
                success ? "success" : "error");
    }

    private void deleteRecord(
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO currentUser) throws IOException {
        String maintenanceIdValue = request.getParameter("maintenance_id");
        String customerName = request.getParameter("customer_name");
        String message = null;
        String messageType = null;

        if (maintenanceIdValue != null && !maintenanceIdValue.isEmpty()) {
            try {
                Long maintenanceId = Long.parseLong(maintenanceIdValue);
                MaintenanceRecordDTO existing =
                        maintenanceDAO.getMaintenanceRecordById(maintenanceId);
                if (existing != null) {
                    customerName = existing.getCustomerName();
                }
                boolean authorized = existing != null
                        && canManageCustomer(
                                currentUser, existing.getCustomerName());
                boolean success = authorized
                        && maintenanceDAO.deleteMaintenanceRecordForCustomer(
                                maintenanceId, existing.getCustomerName());
                message = success
                        ? "정기점검 이력이 성공적으로 삭제되었습니다."
                        : "정기점검 이력 삭제 중 오류가 발생했습니다.";
                messageType = success ? "success" : "error";
            } catch (NumberFormatException exception) {
                message = "잘못된 요청입니다.";
                messageType = "error";
            }
        }

        String location = customerName != null && !customerName.isEmpty()
                ? historyReturnLocation(request, customerName)
                : cardsReturnLocation(request);
        if (message == null) {
            response.sendRedirect(location);
            return;
        }
        FlashMessage.redirect(
                request, response, location, message, messageType);
    }

    private static String historyLocation(String customerName) {
        return "maintenance?view=history&customerName="
                + URLEncoder.encode(customerName, StandardCharsets.UTF_8);
    }

    private String historyReturnLocation(
            HttpServletRequest request, String customerName) {
        StringBuilder location = new StringBuilder(
                historyLocation(customerName));
        String rawPage = request.getParameter("returnHistoryPage");
        if (rawPage != null && !rawPage.isBlank()) {
            try {
                appendHistoryParameter(
                        location,
                        "historyPage",
                        Integer.toString(parseHistoryPage(rawPage)));
            } catch (IllegalArgumentException ignored) {
                // Ignore an invalid return hint and use the first history page.
            }
        }
        try {
            MaintenanceHistoryFilter filter = MaintenanceHistoryFilter.parse(
                    request.getParameter("returnHistoryYear"),
                    request.getParameter("returnHistoryVersion"),
                    request.getParameter("returnHistoryQuery"));
            if (filter.year() != null) {
                appendHistoryParameter(
                        location,
                        "historyYear",
                        filter.year().toString());
            }
            appendHistoryParameter(
                    location, "historyVersion", filter.version());
            appendHistoryParameter(
                    location, "historyQuery", filter.query());
        } catch (IllegalArgumentException ignored) {
            // Ignore invalid return filters instead of reflecting raw values.
        }
        CardsReturnContext cardsContext = cardsReturnContext(request);
        if (cardsContext != null) {
            appendHistoryParameter(location, "returnCardsMonth", cardsContext.month());
            appendHistoryParameter(location, "returnCardsStatus", cardsContext.registrationStatus());
        }
        return location.toString();
    }

    private static void appendHistoryParameter(
            StringBuilder location, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        location.append('&')
                .append(name)
                .append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    private String cardsReturnLocation(HttpServletRequest request) {
        StringBuilder location = new StringBuilder("maintenance?view=cards");
        CardsReturnContext context = cardsReturnContext(request);
        if (context != null) {
            appendHistoryParameter(location, "maintenanceMonth", context.month());
            appendHistoryParameter(location, "registrationStatus", context.registrationStatus());
        }
        return location.toString();
    }

    private void redirectToCards(
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendRedirect(cardsReturnLocation(request));
    }

    // 날짜 문자열을 Date 객체로 변환
    private Date parseDate(String dateString) {
        return StrictDateParser.parseSqlDateOrNull(dateString);
    }

    private String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private void showAddForm(
            HttpServletRequest request,
            HttpServletResponse response,
            String customerName,
            MaintenanceRecordDTO submittedRecord,
            Map<String, String> fieldErrors,
            int status,
            UserDTO user) throws ServletException, IOException {
        MaintenanceFormOptions options = maintenanceFormOptions(null, user);
        CustomerDTO customer = options.customer(customerName);
        MaintenanceRecordDTO formRecord = submittedRecord == null
                ? defaultFormRecord(customer)
                : submittedRecord;
        prepareFormView(
                request,
                options,
                formRecord,
                fieldErrors,
                customer != null);
        request.setAttribute("customerName", formRecord.getCustomerName());
        request.setAttribute("viewType", "add");
        response.setStatus(status);
        request.getRequestDispatcher("/maintenance/maintenance_add.jsp")
                .forward(request, response);
    }

    private MaintenanceFormOptions maintenanceFormOptions(
            String retainedInspector,
            UserDTO user) {
        return MaintenanceFormOptions.from(
                customerAssignmentDAO.getMaintenanceCustomersByAssignee(
                        user.getUserId(), user.getUserName()),
                retainedInspector);
    }

    private MaintenanceRecordDTO defaultFormRecord(CustomerDTO customer) {
        MaintenanceRecordDTO record = new MaintenanceRecordDTO();
        record.setInspectionDate(Date.valueOf(BusinessDate.today(clock)));
        if (customer != null) {
            record.setCustomerName(customer.getCustomerName());
            record.setInspectorName(firstNonBlank(
                    customer.getManagerName(),
                    customer.getSubManagerName()));
            record.setVerticaVersion(
                    trimToNull(customer.getVerticaVersion()));
            record.setLicenseSizeGb(
                    trimToNull(customer.getLicenseSize()));
        }
        return record;
    }

    private void prepareFormView(
            HttpServletRequest request,
            MaintenanceFormOptions options,
            MaintenanceRecordDTO record,
            Map<String, String> fieldErrors,
            boolean customerFixed) {
        MaintenanceFormHistoryContext history =
                maintenanceDAO.getMaintenanceFormHistoryContext(
                        record.getCustomerName(),
                        record.getInspectionDate(),
                        record.getMaintenanceId());
        request.setAttribute("formRecord", record);
        request.setAttribute("formOptions", options);
        request.setAttribute("fieldErrors", fieldErrors);
        request.setAttribute("formCustomerFixed", customerFixed);
        request.setAttribute(
                "previousMaintenanceRecord", history.previousRecord());
        request.setAttribute(
                "duplicateMaintenanceRecord", history.duplicateRecord());
        request.setAttribute(
                "licenseSizeInput",
                MaintenanceRecordRequestMapper.normalizeTerabytesForInput(
                        record.getLicenseSizeGb()));
        request.setAttribute(
                "licenseUsageInput",
                MaintenanceRecordRequestMapper.normalizeTerabytesForInput(
                        record.getLicenseUsageSize()));
        request.setAttribute(
                "licensePercentageInput",
                MaintenanceRecordRequestMapper.normalizePercentageForInput(
                        record.getLicenseUsagePct()));
    }

    private void writeFormContext(
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO user) throws IOException {
        String customerName = trimToNull(
                request.getParameter("customerName"));
        Date inspectionDate = parseDate(
                request.getParameter("inspectionDate"));
        Long excludedId = parseOptionalLong(
                request.getParameter("excludeId"));
        CustomerDTO customer = customerName == null
                ? null
                : customerDAO.getCustomerByName(customerName);
        if (!isActiveMaintenanceCustomer(customer)
                || inspectionDate == null) {
            JsonResponse.sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_maintenance_context",
                    "고객사와 점검일을 확인해 주세요.");
            return;
        }
        if (!canManageCustomer(user, customerName)) {
            JsonResponse.sendError(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "customer_assignment_required",
                    "담당 고객사만 이력을 관리할 수 있습니다.");
            return;
        }
        MaintenanceFormHistoryContext context =
                maintenanceDAO.getMaintenanceFormHistoryContext(
                        customerName, inspectionDate, excludedId);
        JsonResponse.write(
                response,
                HttpServletResponse.SC_OK,
                MaintenanceFormContextJson.encode(customer, context));
    }

    private static boolean isActiveMaintenanceCustomer(
            CustomerDTO customer) {
        return customer != null
                && MAINTENANCE_CUSTOMER_TYPE.equals(
                        customer.getCustomerType());
    }

    private static Long parseOptionalLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Long parsePositiveLong(String value) {
        Long parsed = parseOptionalLong(value);
        return parsed != null && parsed > 0 ? parsed : null;
    }

    private static void sendInvalidMaintenanceId(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        ApplicationError.send(
                request,
                response,
                HttpServletResponse.SC_BAD_REQUEST,
                "invalid_maintenance_id",
                "정기점검 이력 번호가 올바르지 않습니다.");
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null || second.isBlank() ? null : second.trim();
    }

    private boolean canManageCustomer(UserDTO user, String customerName) {
        return user != null
                && customerName != null
                && customerAssignmentDAO.getCustomerNamesByAssignee(
                        user.getUserId(), user.getUserName())
                        .contains(customerName);
    }

    private static int parseHistoryPage(String value) {
        if (value == null || value.isBlank()) {
            return 1;
        }
        try {
            long page = Long.parseLong(value.trim());
            if (page < 1 || page > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "History page must be a positive integer");
            }
            return (int) page;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "History page must be a positive integer", exception);
        }
    }

}
