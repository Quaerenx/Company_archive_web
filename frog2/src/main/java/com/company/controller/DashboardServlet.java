package com.company.controller;

import com.company.util.LicenseRiskPolicy;
import com.company.util.LicenseSummaryFormatter;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.company.model.CustomerDAO;
import com.company.model.MaintenanceRecordDAO;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.MaintenanceCustomerAssignment;
import com.company.model.UserDTO;
import com.company.security.SessionPrincipal;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DashboardServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final MaintenanceRecordDAO maintenanceRecordDAO;
    private final CustomerDAO customerDAO;
    private static final DateTimeFormatter MONTH_PARAM_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM");
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

    public DashboardServlet() {
        this(new MaintenanceRecordDAO(), new CustomerDAO());
    }

    DashboardServlet(
            MaintenanceRecordDAO maintenanceRecordDAO,
            CustomerDAO customerDAO) {
        this.maintenanceRecordDAO = maintenanceRecordDAO;
        this.customerDAO = customerDAO;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        UserDTO user = SessionPrincipal.expose(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        YearMonth selectedMonth = parseMaintenanceMonth(request.getParameter("maintenanceMonth"));
        LocalDate monthStart = selectedMonth.atDay(1);
        LocalDate nextMonthStart = selectedMonth.plusMonths(1).atDay(1);
        LocalDate today = LocalDate.now();
        List<MonthlyMaintenanceCard> monthlyMaintenanceCards =
                buildMonthlyMaintenanceCards(
                        maintenanceRecordDAO.getMaintenanceRecordsByMonth(
                                Date.valueOf(monthStart),
                                Date.valueOf(nextMonthStart)),
                        today);
        List<MaintenanceCustomerAssignment> allAssignments =
                customerDAO.getAllMaintenanceCustomerAssignments();
        List<MaintenanceAssigneeGroup> monthlyMaintenanceAssigneeGroups =
                buildMaintenanceAssigneeGroups(
                        allAssignments,
                        monthlyMaintenanceCards,
                        selectedMonth);

        request.setAttribute("maintenanceMonthParam", selectedMonth.format(MONTH_PARAM_FORMATTER));
        request.setAttribute("maintenanceMonthLabel", selectedMonth.format(MONTH_LABEL_FORMATTER));
        request.setAttribute("maintenanceMonthTabs", buildMaintenanceMonthTabs(selectedMonth));
        request.setAttribute(
                "monthlyMaintenanceAssigneeGroups",
                monthlyMaintenanceAssigneeGroups);
        request.getRequestDispatcher("/dashboard.jsp").forward(request, response);
    }

    private YearMonth parseMaintenanceMonth(String rawMonth) {
        YearMonth currentMonth = YearMonth.now();
        YearMonth previousMonth = currentMonth.minusMonths(1);

        if (rawMonth == null || rawMonth.trim().isEmpty()) {
            return currentMonth;
        }

        try {
            YearMonth requestedMonth = YearMonth.parse(rawMonth.trim(), MONTH_PARAM_FORMATTER);
            if (requestedMonth.equals(currentMonth) || requestedMonth.equals(previousMonth)) {
                return requestedMonth;
            }
        } catch (DateTimeParseException e) {
            return currentMonth;
        }
        return currentMonth;
    }

    private List<MonthTab> buildMaintenanceMonthTabs(YearMonth selectedMonth) {
        List<MonthTab> tabs = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        YearMonth previousMonth = currentMonth.minusMonths(1);

        tabs.add(new MonthTab(
                previousMonth.format(MONTH_LABEL_FORMATTER),
                previousMonth.format(MONTH_PARAM_FORMATTER),
                previousMonth.equals(selectedMonth)));
        tabs.add(new MonthTab(
                currentMonth.format(MONTH_LABEL_FORMATTER),
                currentMonth.format(MONTH_PARAM_FORMATTER),
                currentMonth.equals(selectedMonth)));
        return tabs;
    }

    private List<MonthlyMaintenanceCard> buildMonthlyMaintenanceCards(List<MaintenanceRecordDTO> records, LocalDate today) {
        List<MonthlyMaintenanceCard> cards = new ArrayList<>();
        if (records == null) {
            return cards;
        }

        for (MaintenanceRecordDTO record : records) {
            LocalDate inspectionDate = null;
            if (record.getInspectionDate() != null) {
                inspectionDate = record.getInspectionDate().toLocalDate();
            }

            LicenseRiskPolicy.Level licenseRiskLevel =
                    LicenseSummaryFormatter.resolveUsageRiskLevel(record);
            boolean licenseRisk = licenseRiskLevel
                    == LicenseRiskPolicy.Level.WARNING
                    || licenseRiskLevel == LicenseRiskPolicy.Level.RISK;
            String statusCode = "done";
            String statusLabel = "완료";
            if (inspectionDate != null && inspectionDate.isAfter(today)) {
                statusCode = "due";
                statusLabel = "예정";
            }

            cards.add(new MonthlyMaintenanceCard(
                    valueOrDash(record.getCustomerName()),
                    formatDate(inspectionDate),
                    valueOrDash(record.getVerticaVersion()),
                    valueOrDash(LicenseSummaryFormatter.format(record)),
                    valueOrDash(record.getNote()),
                    valueOrDash(record.getInspectorName()),
                    statusCode,
                    statusLabel,
                    licenseRisk));
        }
        return cards;
    }

    private List<MaintenanceAssigneeGroup> buildMaintenanceAssigneeGroups(
            List<MaintenanceCustomerAssignment> assignments,
            List<MonthlyMaintenanceCard> cards,
            YearMonth selectedMonth) {
        Map<String, CustomerMonthState> stateByCustomer = new LinkedHashMap<>();
        for (MonthlyMaintenanceCard card : cards) {
            String customerName = normalizedName(card.getCustomerName());
            if (customerName.isEmpty()) {
                continue;
            }
            CustomerMonthState state = stateByCustomer.computeIfAbsent(
                    customerName,
                    ignored -> new CustomerMonthState());
            state.done = state.done || "done".equals(card.getStatusCode());
            state.licenseRisk = state.licenseRisk || card.isLicenseRisk();
            if (!isMissingValue(card.getInspectorName())) {
                state.inspectorName = card.getInspectorName().trim();
            }
        }

        Map<String, List<MonthlyMaintenanceCustomer>> customersByManager =
                new LinkedHashMap<>();
        Map<String, MaintenanceCustomerAssignment> assignmentByCustomer =
                new LinkedHashMap<>();
        Set<String> assignedCustomers = new HashSet<>();
        if (assignments != null) {
            for (MaintenanceCustomerAssignment assignment : assignments) {
                String customerName = normalizedName(assignment.customerName());
                if (customerName.isEmpty()) {
                    continue;
                }
                assignmentByCustomer.putIfAbsent(customerName, assignment);
                if (!assignment.schedule().isDue(selectedMonth)
                        || !assignedCustomers.add(customerName)) {
                    continue;
                }
                CustomerMonthState state = stateByCustomer.get(customerName);
                addMaintenanceCustomer(
                        customersByManager,
                        displayManagerName(assignment.managerName()),
                        customerName,
                        state,
                        assignment.schedule().isQuarterly());
            }
        }

        for (Map.Entry<String, CustomerMonthState> entry : stateByCustomer.entrySet()) {
            if (assignedCustomers.contains(entry.getKey())) {
                continue;
            }
            MaintenanceCustomerAssignment assignment =
                    assignmentByCustomer.get(entry.getKey());
            addMaintenanceCustomer(
                    customersByManager,
                    displayManagerName(assignment == null
                            ? entry.getValue().inspectorName
                            : assignment.managerName()),
                    entry.getKey(),
                    entry.getValue(),
                    assignment != null
                            && assignment.schedule().isQuarterly());
        }

        List<MaintenanceAssigneeGroup> groups = new ArrayList<>();
        customersByManager.forEach((managerName, customers) ->
                groups.add(new MaintenanceAssigneeGroup(managerName, customers)));
        return groups;
    }

    private void addMaintenanceCustomer(
            Map<String, List<MonthlyMaintenanceCustomer>> customersByManager,
            String managerName,
            String customerName,
            CustomerMonthState state,
            boolean quarterly) {
        boolean done = state != null && state.done;
        boolean licenseRisk = state != null && state.licenseRisk;
        customersByManager
                .computeIfAbsent(managerName, ignored -> new ArrayList<>())
                .add(new MonthlyMaintenanceCustomer(
                        customerName,
                        done,
                        licenseRisk,
                        quarterly));
    }

    private String displayManagerName(String value) {
        String normalized = normalizedName(value);
        return normalized.isEmpty() ? "담당자 미지정" : normalized;
    }

    private String normalizedName(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        return "-".equals(normalized) ? "" : normalized;
    }

    private boolean isMissingValue(String value) {
        return normalizedName(value).isEmpty();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.toString();
    }

    private String valueOrDash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }
        return value.trim();
    }


    public static class MenuItem {
        private final String title;
        private final String url;
        private final String icon;

        public MenuItem(String title, String url, String icon) {
            this.title = title;
            this.url = url;
            this.icon = icon;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

        public String getIcon() {
            return icon;
        }
    }

    public static class MonthTab {
        private final String label;
        private final String value;
        private final boolean active;

        public MonthTab(String label, String value, boolean active) {
            this.label = label;
            this.value = value;
            this.active = active;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }

        public boolean isActive() {
            return active;
        }
    }

    public static class MonthlyMaintenanceCard {
        private final String customerName;
        private final String inspectionDate;
        private final String verticaVersion;
        private final String licenseSummary;
        private final String note;
        private final String inspectorName;
        private final String statusCode;
        private final String statusLabel;
        private final boolean licenseRisk;

        public MonthlyMaintenanceCard(String customerName, String inspectionDate, String verticaVersion,
                String licenseSummary, String note, String inspectorName, String statusCode, String statusLabel,
                boolean licenseRisk) {
            this.customerName = customerName;
            this.inspectionDate = inspectionDate;
            this.verticaVersion = verticaVersion;
            this.licenseSummary = licenseSummary;
            this.note = note;
            this.inspectorName = inspectorName;
            this.statusCode = statusCode;
            this.statusLabel = statusLabel;
            this.licenseRisk = licenseRisk;
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getInspectionDate() {
            return inspectionDate;
        }

        public String getVerticaVersion() {
            return verticaVersion;
        }

        public String getLicenseSummary() {
            return licenseSummary;
        }

        public String getNote() {
            return note;
        }

        public String getInspectorName() {
            return inspectorName;
        }

        public String getStatusCode() {
            return statusCode;
        }

        public String getStatusLabel() {
            return statusLabel;
        }

        public boolean isLicenseRisk() {
            return licenseRisk;
        }
    }

    public static class MaintenanceAssigneeGroup {
        private final String managerName;
        private final List<MonthlyMaintenanceCustomer> customers;

        public MaintenanceAssigneeGroup(
                String managerName,
                List<MonthlyMaintenanceCustomer> customers) {
            this.managerName = managerName;
            this.customers = List.copyOf(customers);
        }

        public String getManagerName() {
            return managerName;
        }

        public List<MonthlyMaintenanceCustomer> getCustomers() {
            return customers;
        }
    }

    public static class MonthlyMaintenanceCustomer {
        private final String customerName;
        private final boolean done;
        private final boolean licenseRisk;
        private final boolean quarterly;

        public MonthlyMaintenanceCustomer(
                String customerName,
                boolean done,
                boolean licenseRisk,
                boolean quarterly) {
            this.customerName = customerName;
            this.done = done;
            this.licenseRisk = licenseRisk;
            this.quarterly = quarterly;
        }

        public String getCustomerName() {
            return customerName;
        }

        public boolean isDone() {
            return done;
        }

        public boolean isLicenseRisk() {
            return licenseRisk;
        }

        public boolean isQuarterly() {
            return quarterly;
        }

        public String getStatusCode() {
            return done ? "done" : "due";
        }

        public String getStatusLabel() {
            return done ? "점검 완료" : "미진행";
        }
    }

    private static class CustomerMonthState {
        private boolean done;
        private boolean licenseRisk;
        private String inspectorName;
    }
}
