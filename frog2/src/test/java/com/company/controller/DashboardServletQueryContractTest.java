package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.company.model.CustomerDAO;
import com.company.model.MaintenanceCustomerAssignment;
import com.company.model.MaintenanceRecordDAO;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.MaintenanceSchedule;
import com.company.model.UserDTO;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DashboardServletQueryContractTest {
    @Test
    void dashboardLoadsOnlyMaintenanceSummaryAndViewContract() throws Exception {
        StubMaintenanceRecordDAO maintenanceDAO = new StubMaintenanceRecordDAO();
        LocalDate today = LocalDate.now();
        maintenanceDAO.records = List.of(
                maintenanceRecord("past-low", today.minusDays(1), "40"),
                maintenanceRecord("future-low", today.plusDays(1), "40"),
                maintenanceRecord("past-high", today.minusDays(1), "95"),
                maintenanceRecord("future-high", today.plusDays(1), "95"));
        StubCustomerDAO customerDAO = new StubCustomerDAO();
        customerDAO.assignments = List.of(
                new MaintenanceCustomerAssignment("past-low", "Manager A"),
                new MaintenanceCustomerAssignment("future-low", "Manager A"),
                new MaintenanceCustomerAssignment("future-high", "Manager B"),
                new MaintenanceCustomerAssignment("past-high", "Manager B"),
                new MaintenanceCustomerAssignment("pending-only", "Manager B"));
        DashboardServlet servlet = new DashboardServlet(
                maintenanceDAO,
                customerDAO);
        RequestFixture request = new RequestFixture();
        ResponseFixture response = new ResponseFixture();

        servlet.doGet(request.proxy(), response.proxy());

        assertFalse(request.attributes.containsKey("vmHosts"));
        assertFalse(request.attributes.containsKey("vmHostCount"));
        assertFalse(request.attributes.containsKey("dashboardMenus"));
        assertFalse(request.attributes.containsKey("monthlyMaintenanceTotal"));
        assertFalse(request.attributes.containsKey("monthlyMaintenanceDoneCount"));
        assertFalse(request.attributes.containsKey("monthlyMaintenanceDueCount"));
        assertFalse(request.attributes.containsKey(
                "monthlyMaintenanceLicenseRiskCount"));
        assertFalse(request.attributes.containsKey(
                "monthlyMaintenanceAttentionCount"));
        assertFalse(request.attributes.containsKey(
                "monthlyMaintenanceReviewCount"));
        assertFalse(request.attributes.containsKey("monthlyMaintenanceCards"));
        @SuppressWarnings("unchecked")
        List<DashboardServlet.MaintenanceAssigneeGroup> groups =
                (List<DashboardServlet.MaintenanceAssigneeGroup>) request.attributes.get(
                        "monthlyMaintenanceAssigneeGroups");
        assertEquals(2, groups.size());
        assertEquals("Manager A", groups.getFirst().getManagerName());
        assertEquals(2, groups.getFirst().getCustomers().size());
        assertEquals("past-low", groups.getFirst().getCustomers().getFirst().getCustomerName());
        assertEquals(true, groups.getFirst().getCustomers().getFirst().isDone());
        assertEquals(false, groups.getFirst().getCustomers().get(1).isDone());
        assertEquals("/dashboard.jsp", request.forwardedPath);
        assertEquals(1, maintenanceDAO.monthCalls);
        assertEquals(1, customerDAO.assignmentCalls);
    }

    @Test
    void recordOnlyQuarterlyCustomerKeepsItsConfiguredFrequencyLabel()
            throws Exception {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        StubMaintenanceRecordDAO maintenanceDAO = new StubMaintenanceRecordDAO();
        maintenanceDAO.records = List.of(
                maintenanceRecord("quarterly-extra", today, "89.95"));
        StubCustomerDAO customerDAO = new StubCustomerDAO();
        customerDAO.assignments = List.of(
                new MaintenanceCustomerAssignment(
                        "quarterly-extra",
                        "Quarterly Manager",
                        new MaintenanceSchedule(
                                3,
                                currentMonth.minusMonths(1),
                                LocalDate.of(2000, 1, 1),
                                null,
                                true)));
        DashboardServlet servlet = new DashboardServlet(
                maintenanceDAO, customerDAO);
        RequestFixture request = new RequestFixture();

        servlet.doGet(request.proxy(), new ResponseFixture().proxy());

        @SuppressWarnings("unchecked")
        List<DashboardServlet.MaintenanceAssigneeGroup> groups =
                (List<DashboardServlet.MaintenanceAssigneeGroup>)
                        request.attributes.get(
                                "monthlyMaintenanceAssigneeGroups");
        assertEquals(1, groups.size());
        assertEquals("Quarterly Manager", groups.getFirst().getManagerName());
        DashboardServlet.MonthlyMaintenanceCustomer customer =
                groups.getFirst().getCustomers().getFirst();
        assertEquals("quarterly-extra", customer.getCustomerName());
        assertEquals(true, customer.isQuarterly());
        assertEquals(true, customer.isLicenseRisk());
    }

    private static MaintenanceRecordDTO maintenanceRecord(
            String customerName, LocalDate inspectionDate, String usagePercentage) {
        MaintenanceRecordDTO record = new MaintenanceRecordDTO();
        record.setCustomerName(customerName);
        record.setInspectionDate(Date.valueOf(inspectionDate));
        record.setLicenseSizeGb("100");
        record.setLicenseUsageSize(usagePercentage);
        record.setLicenseUsagePct(usagePercentage);
        return record;
    }

    private static final class StubMaintenanceRecordDAO extends MaintenanceRecordDAO {
        private List<MaintenanceRecordDTO> records = new ArrayList<>();
        private int monthCalls;

        @Override
        public List<MaintenanceRecordDTO> getMaintenanceRecordsByMonth(Date startDate, Date endDate) {
            monthCalls++;
            return records;
        }
    }

    private static final class StubCustomerDAO extends CustomerDAO {
        private List<MaintenanceCustomerAssignment> assignments = new ArrayList<>();
        private int assignmentCalls;

        @Override
        public List<MaintenanceCustomerAssignment>
                getAllMaintenanceCustomerAssignments() {
            assignmentCalls++;
            return assignments;
        }
    }

    private static final class RequestFixture {
        private final Map<String, Object> attributes = new HashMap<>();
        private final HttpSession session;
        private String forwardedPath;

        private RequestFixture() {
            UserDTO user = new UserDTO("tester", "", "Tester", "QA");
            session = (HttpSession) Proxy.newProxyInstance(
                    HttpSession.class.getClassLoader(),
                    new Class<?>[] {HttpSession.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "getAttribute" -> "user".equals(args[0]) ? user : null;
                        default -> defaultValue(call.getReturnType());
                    });
        }

        private HttpServletRequest proxy() {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[] {HttpServletRequest.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "getSession" -> session;
                        case "getServletPath" -> "/dashboard";
                        case "getParameter" -> null;
                        case "setAttribute" -> {
                            attributes.put((String) args[0], args[1]);
                            yield null;
                        }
                        case "getRequestDispatcher" -> dispatcher((String) args[0]);
                        default -> defaultValue(call.getReturnType());
                    });
        }

        private RequestDispatcher dispatcher(String path) {
            return (RequestDispatcher) Proxy.newProxyInstance(
                    RequestDispatcher.class.getClassLoader(),
                    new Class<?>[] {RequestDispatcher.class},
                    (ignored, call, args) -> {
                        if ("forward".equals(call.getName())) {
                            forwardedPath = path;
                        }
                        return null;
                    });
        }
    }

    private static final class ResponseFixture {
        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] {HttpServletResponse.class},
                    (ignored, call, args) -> defaultValue(call.getReturnType()));
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
