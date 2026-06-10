package com.company.controller;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.company.model.MaintenanceRecordDAO;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.UserDTO;
import com.company.model.UserVmHostDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class DashboardServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final UserVmHostDAO userVmHostDAO = new UserVmHostDAO();
    private final MaintenanceRecordDAO maintenanceRecordDAO = new MaintenanceRecordDAO();
    private static final DateTimeFormatter MONTH_PARAM_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM");
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("login");
            return;
        }

        UserDTO user = (UserDTO) session.getAttribute("user");
        request.setAttribute("user", user);

        String servletPath = request.getServletPath();
        if ("/dashboard2".equals(servletPath)) {
            request.getRequestDispatcher("/dashboard2.jsp").forward(request, response);
            return;
        }

        int vmHostCount = userVmHostDAO.countActiveHostsByOwner(user.getUserId());
        int vmHostLimit = userVmHostDAO.getMaxHostsPerUser();

        request.setAttribute("vmHosts", userVmHostDAO.getActiveHostsByOwner(user.getUserId()));
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
        int reviewCount = 0;
        for (MonthlyMaintenanceCard card : monthlyMaintenanceCards) {
            if ("done".equals(card.getStatusCode())) {
                doneCount++;
            } else if ("due".equals(card.getStatusCode())) {
                dueCount++;
            } else if ("issue".equals(card.getStatusCode())) {
                reviewCount++;
            }
        }

        request.setAttribute("maintenanceMonthParam", selectedMonth.format(MONTH_PARAM_FORMATTER));
        request.setAttribute("maintenanceMonthLabel", selectedMonth.format(MONTH_LABEL_FORMATTER));
        request.setAttribute("maintenanceMonthTabs", buildMaintenanceMonthTabs(selectedMonth));
        request.setAttribute("monthlyMaintenanceCards", monthlyMaintenanceCards);
        request.setAttribute("monthlyMaintenanceTotal", monthlyMaintenanceCards.size());
        request.setAttribute("monthlyMaintenanceDoneCount", doneCount);
        request.setAttribute("monthlyMaintenanceDueCount", dueCount);
        request.setAttribute("monthlyMaintenanceReviewCount", reviewCount);

        Map<String, List<MenuItem>> dashboardMenus = new LinkedHashMap<>();

        List<MenuItem> customerMenus = new ArrayList<>();
        customerMenus.add(new MenuItem("고객 정보", "customers?view=list", "fas fa-address-card"));
        customerMenus.add(new MenuItem("정기점검 이력", "maintenance", "fas fa-clipboard-check"));
        dashboardMenus.put("고객 관리", customerMenus);

        List<MenuItem> archiveMenus = new ArrayList<>();
        archiveMenus.add(new MenuItem("회의록", "meeting?view=list", "fas fa-users"));
        archiveMenus.add(new MenuItem("자료실", "filerepo/filerepo_downlist.jsp", "fas fa-file-alt"));
        archiveMenus.add(new MenuItem("트러블슈팅", "troubleshooting?view=list", "fas fa-tools"));
        dashboardMenus.put("자료 관리", archiveMenus);

        request.setAttribute("dashboardMenus", dashboardMenus);
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

            Double usagePct = parseNumber(record.getLicenseUsagePct());
            String statusCode = "done";
            String statusLabel = "완료";
            if (inspectionDate != null && inspectionDate.isAfter(today)) {
                statusCode = "due";
                statusLabel = "예정";
            } else if (usagePct != null && usagePct >= 90.0) {
                statusCode = "issue";
                statusLabel = "확인 필요";
            }

            cards.add(new MonthlyMaintenanceCard(
                    valueOrDash(record.getCustomerName()),
                    formatDate(inspectionDate),
                    valueOrDash(record.getVerticaVersion()),
                    buildLicenseSummaryTB(record),
                    valueOrDash(record.getNote()),
                    valueOrDash(record.getInspectorName()),
                    statusCode,
                    statusLabel));
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

    private String buildLicenseSummaryTB(MaintenanceRecordDTO rec) {
        if (rec == null) return "-";
        Double sizeGb = parseToGb(rec.getLicenseSizeGb());
        Double usageGb = parseToGb(rec.getLicenseUsageSize());
        Double pct = parseNumber(rec.getLicenseUsagePct());

        Double sizeTb = (sizeGb != null ? sizeGb / 1024.0 : null);
        Double usageTb = (usageGb != null ? usageGb / 1024.0 : null);

        if (pct == null && sizeGb != null && usageGb != null && sizeGb > 0) {
            pct = (usageGb / sizeGb) * 100.0;
        }

        if (sizeTb == null && usageTb == null && pct == null) {
            return "-";
        }

        String sizeStr = (sizeTb != null ? format2(sizeTb) + "TB" : "-");
        String usageStr = (usageTb != null ? format2(usageTb) + "TB" : "-");
        String pctStr = (pct != null ? String.valueOf(Math.round(pct)) + "%" : "-");
        return sizeStr + " 중 " + usageStr + " 총 " + pctStr + " 사용 중";
    }

    private Double parseToGb(String s) {
        if (s == null) return null;
        String raw = s.trim();
        if (raw.isEmpty()) return null;
        String lower = raw.toLowerCase();
        boolean isTb = lower.contains("tb");
        boolean isGb = lower.contains("gb");
        Double n = parseNumber(raw);
        if (n == null) return null;
        if (isTb || (!isGb && !isTb)) {
            return n * 1024.0;
        }
        return n;
    }

    private Double parseNumber(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        t = t.replace(",", "");
        t = t.replaceAll("[^0-9.\\-]", "");
        if (t.isEmpty() || t.equals("-") || t.equals(".")) return null;
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String format2(double v) {
        return String.format(java.util.Locale.US, "%.2f", v);
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

        public MonthlyMaintenanceCard(String customerName, String inspectionDate, String verticaVersion,
                String licenseSummary, String note, String inspectorName, String statusCode, String statusLabel) {
            this.customerName = customerName;
            this.inspectionDate = inspectionDate;
            this.verticaVersion = verticaVersion;
            this.licenseSummary = licenseSummary;
            this.note = note;
            this.inspectorName = inspectorName;
            this.statusCode = statusCode;
            this.statusLabel = statusLabel;
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
    }
}
