package com.company.controller;

import com.company.util.LicenseSummaryFormatter;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.company.model.MaintenanceRecordDAO;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.UserDTO;
import com.company.model.UserVmHostDAO;
import com.company.model.UserVmHostDTO;
import com.company.security.SessionPrincipal;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DashboardServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final UserVmHostDAO userVmHostDAO;
    private final MaintenanceRecordDAO maintenanceRecordDAO;
    private static final DateTimeFormatter MONTH_PARAM_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM");
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

    public DashboardServlet() {
        this(new UserVmHostDAO(), new MaintenanceRecordDAO());
    }

    DashboardServlet(UserVmHostDAO userVmHostDAO, MaintenanceRecordDAO maintenanceRecordDAO) {
        this.userVmHostDAO = userVmHostDAO;
        this.maintenanceRecordDAO = maintenanceRecordDAO;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        UserDTO user = SessionPrincipal.expose(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }


        List<UserVmHostDTO> vmHosts =
                userVmHostDAO.getActiveHostsByOwner(user.getUserId());
        int vmHostCount = vmHosts.size();
        int vmHostLimit = userVmHostDAO.getMaxHostsPerUser();

        request.setAttribute("vmHosts", vmHosts);
        request.setAttribute("vmHostCount", vmHostCount);
        request.setAttribute("vmHostLimit", vmHostLimit);
        request.setAttribute("vmHostRemaining", Math.max(0, vmHostLimit - vmHostCount));

        YearMonth selectedMonth = parseMaintenanceMonth(request.getParameter("maintenanceMonth"));
        LocalDate monthStart = selectedMonth.atDay(1);
        LocalDate nextMonthStart = selectedMonth.plusMonths(1).atDay(1);
        LocalDate today = LocalDate.now();
        List<MonthlyMaintenanceCard> monthlyMaintenanceCards = buildMonthlyMaintenanceCards(
                maintenanceRecordDAO.getMaintenanceRecordsByMonth(Date.valueOf(monthStart), Date.valueOf(nextMonthStart)),
                today);

        int doneCount = 0;
        int dueCount = 0;
        int licenseRiskCount = 0;
        int attentionCount = 0;
        for (MonthlyMaintenanceCard card : monthlyMaintenanceCards) {
            if ("done".equals(card.getStatusCode())) {
                doneCount++;
            } else if ("due".equals(card.getStatusCode())) {
                dueCount++;
            }
            if (card.isLicenseRisk()) {
                licenseRiskCount++;
            }
            if ("due".equals(card.getStatusCode()) || card.isLicenseRisk()) {
                attentionCount++;
            }
        }

        request.setAttribute("maintenanceMonthParam", selectedMonth.format(MONTH_PARAM_FORMATTER));
        request.setAttribute("maintenanceMonthLabel", selectedMonth.format(MONTH_LABEL_FORMATTER));
        request.setAttribute("maintenanceMonthTabs", buildMaintenanceMonthTabs(selectedMonth));
        request.setAttribute("monthlyMaintenanceCards", monthlyMaintenanceCards);
        request.setAttribute("monthlyMaintenanceTotal", monthlyMaintenanceCards.size());
        request.setAttribute("monthlyMaintenanceDoneCount", doneCount);
        request.setAttribute("monthlyMaintenanceDueCount", dueCount);
        request.setAttribute("monthlyMaintenanceLicenseRiskCount", licenseRiskCount);
        request.setAttribute("monthlyMaintenanceAttentionCount", attentionCount);
        request.setAttribute("monthlyMaintenanceReviewCount", licenseRiskCount);

        request.setAttribute("dashboardMenus", DashboardMenuProvider.build());
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

            Double usagePct = LicenseSummaryFormatter.resolveUsagePercentage(record);
            boolean licenseRisk = usagePct != null && usagePct >= 90.0;
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
}
