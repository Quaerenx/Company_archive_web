package com.company.controller;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.company.model.CustomerAssignmentDAO;
import com.company.model.CustomerDTO;
import com.company.model.MaintenanceAssigneeData;
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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DashboardServletQueryContractTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-24T03:00:00Z"),
            ZoneId.of("Asia/Seoul"));

    @Test
    void twelveMonthWindowAndQueryBoundsFollowTheServerSelectionAcrossYearEnd() throws Exception {
        Clock januaryClock = Clock.fixed(
                Instant.parse("2026-01-15T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        List<String> available = List.of("2025-02", "2025-03", "2025-04", "2025-05",
                "2025-06", "2025-07", "2025-08", "2025-09", "2025-10", "2025-11",
                "2025-12", "2026-01");
        List<String> requests = new ArrayList<>(available);
        requests.addAll(java.util.Arrays.asList(null, "", "2025-01", "2026-02", "2026-13", "invalid"));
        for (String rawMonth : requests) {
            StubMaintenanceRecordDAO maintenanceDAO = new StubMaintenanceRecordDAO();
            DashboardServlet servlet = new DashboardServlet(
                    maintenanceDAO, new StubCustomerAssignmentDAO(), januaryClock);
            RequestFixture request = new RequestFixture();
            request.parameters.put("maintenanceMonth", rawMonth);

            servlet.doGet(request.proxy(), new ResponseFixture().proxy());

            String expected = rawMonth != null && available.contains(rawMonth) ? rawMonth : "2026-01";
            YearMonth selected = YearMonth.parse(expected);
            assertEquals(expected, request.attributes.get("maintenanceMonthParam"));
            assertEquals(expected, request.attributes.get("maintenanceMonthLabel"));
            assertEquals(Date.valueOf(selected.atDay(1)), maintenanceDAO.lastStartDate);
            assertEquals(Date.valueOf(selected.plusMonths(1).atDay(1)), maintenanceDAO.lastEndDate);
            assertEquals(1, maintenanceDAO.monthCalls);
            @SuppressWarnings("unchecked")
            List<DashboardServlet.MonthTab> tabs =
                    (List<DashboardServlet.MonthTab>) request.attributes.get("maintenanceMonthTabs");
            assertEquals(available, tabs.stream().map(DashboardServlet.MonthTab::getValue).toList());
            assertEquals(available, tabs.stream().map(DashboardServlet.MonthTab::getLabel).toList());
            assertEquals(List.of(expected), tabs.stream().filter(DashboardServlet.MonthTab::isActive)
                    .map(DashboardServlet.MonthTab::getValue).toList());
        }
    }

    @Test
    void oldestMonthAppliesItsScheduleAndRecordStateToPersonalAndGlobalBoards() throws Exception {
        YearMonth oldest = YearMonth.of(2025, 9);
        StubMaintenanceRecordDAO records = new StubMaintenanceRecordDAO();
        records.records = List.of(maintenanceRecord("historical-done", oldest.atDay(15), "40"));
        StubCustomerAssignmentDAO assignments = new StubCustomerAssignmentDAO();
        assignments.personalAssignments = List.of(
                new MaintenanceCustomerAssignment("historical-done", "Tester"),
                new MaintenanceCustomerAssignment("historical-quarterly-due", "Tester",
                        new MaintenanceSchedule(3, oldest, LocalDate.of(2025, 1, 1), null, true)));
        assignments.assignments = assignments.personalAssignments;
        RequestFixture request = new RequestFixture();
        request.parameters.put("maintenanceMonth", "2025-09");

        new DashboardServlet(records, assignments, FIXED_CLOCK)
                .doGet(request.proxy(), new ResponseFixture().proxy());

        assertEquals("2025-09", request.attributes.get("maintenanceMonthParam"));
        assertEquals(Date.valueOf("2025-09-01"), records.lastStartDate);
        assertEquals(Date.valueOf("2025-10-01"), records.lastEndDate);
        assertEquals(2, request.attributes.get("personalMaintenanceTargetCount"));
        assertEquals(1, request.attributes.get("personalMaintenanceRegisteredCount"));
        assertEquals(1, request.attributes.get("personalMaintenanceUnregisteredCount"));
        assertEquals(2, personalCustomers(request).size());
        assertEquals(true, personalCustomers(request).getFirst().isDone());
        assertEquals(false, personalCustomers(request).getLast().isDone());
        assertEquals(true, personalCustomers(request).getLast().isQuarterly());
        @SuppressWarnings("unchecked")
        List<DashboardServlet.MaintenanceAssigneeGroup> global =
                (List<DashboardServlet.MaintenanceAssigneeGroup>) request.attributes.get(
                        "monthlyMaintenanceAssigneeGroups");
        assertEquals(2, global.getFirst().getCustomers().size());
        assertEquals(true, global.getFirst().getCustomers().getFirst().isDone());
        assertEquals(false, global.getFirst().getCustomers().getLast().isDone());
        assertEquals(true, global.getFirst().getCustomers().getLast().isQuarterly());
    }

    @Test
    void dashboardLoadsOnlyMaintenanceSummaryAndViewContract() throws Exception {
        StubMaintenanceRecordDAO maintenanceDAO = new StubMaintenanceRecordDAO();
        LocalDate today = LocalDate.now(FIXED_CLOCK);
        maintenanceDAO.records = List.of(
                maintenanceRecord("past-low", today.minusDays(1), "40"),
                maintenanceRecord("future-low", today.plusDays(1), "40"),
                maintenanceRecord("past-high", today.minusDays(1), "95"),
                maintenanceRecord("future-high", today.plusDays(1), "95"));
        StubCustomerAssignmentDAO customerDAO =
                new StubCustomerAssignmentDAO();
        customerDAO.assignments = List.of(
                new MaintenanceCustomerAssignment("past-low", "Manager A"),
                new MaintenanceCustomerAssignment("future-low", "Manager A"),
                new MaintenanceCustomerAssignment("future-high", "Manager B"),
                new MaintenanceCustomerAssignment("past-high", "Manager B"),
                new MaintenanceCustomerAssignment("pending-only", "Manager B"));
        DashboardServlet servlet = new DashboardServlet(
                maintenanceDAO,
                customerDAO,
                FIXED_CLOCK);
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
        LocalDate today = LocalDate.now(FIXED_CLOCK);
        YearMonth currentMonth = YearMonth.from(today);
        StubMaintenanceRecordDAO maintenanceDAO = new StubMaintenanceRecordDAO();
        maintenanceDAO.records = List.of(
                maintenanceRecord("quarterly-extra", today, "89.95"));
        StubCustomerAssignmentDAO customerDAO =
                new StubCustomerAssignmentDAO();
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
                maintenanceDAO, customerDAO, FIXED_CLOCK);
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
        assertEquals(false, customer.isLicenseRisk());
    }

    @Test
    void multipleRecordsCombineCompletionAndLicenseRiskForOneCustomer()
            throws Exception {
        LocalDate today = LocalDate.now(FIXED_CLOCK);
        StubMaintenanceRecordDAO maintenanceDAO = new StubMaintenanceRecordDAO();
        maintenanceDAO.records = List.of(
                maintenanceRecord("mixed-state", today.plusDays(2), "40"),
                maintenanceRecord("mixed-state", today.minusDays(2), "40"),
                maintenanceRecord("mixed-state", today.plusDays(1), "95"));
        StubCustomerAssignmentDAO customerDAO =
                new StubCustomerAssignmentDAO();
        customerDAO.assignments = List.of(
                new MaintenanceCustomerAssignment(
                        "mixed-state", "Manager C"));
        DashboardServlet servlet = new DashboardServlet(
                maintenanceDAO, customerDAO, FIXED_CLOCK);
        RequestFixture request = new RequestFixture();

        servlet.doGet(request.proxy(), new ResponseFixture().proxy());

        @SuppressWarnings("unchecked")
        List<DashboardServlet.MaintenanceAssigneeGroup> groups =
                (List<DashboardServlet.MaintenanceAssigneeGroup>)
                        request.attributes.get(
                                "monthlyMaintenanceAssigneeGroups");
        DashboardServlet.MonthlyMaintenanceCustomer customer =
                groups.getFirst().getCustomers().getFirst();
        assertEquals("mixed-state", customer.getCustomerName());
        assertEquals(true, customer.isDone());
        assertEquals(true, customer.isLicenseRisk());
    }

    @Test
    void defaultMonthAndCompletionUseSeoulAtUtcMonthBoundary()
            throws Exception {
        Clock utcClockAtSeoulMidnight = Clock.fixed(
                Instant.parse("2026-08-31T15:00:00Z"),
                ZoneOffset.UTC);
        StubMaintenanceRecordDAO maintenanceDAO =
                new StubMaintenanceRecordDAO();
        maintenanceDAO.records = List.of(
                maintenanceRecord(
                        "boundary-customer",
                        LocalDate.of(2026, 9, 1),
                        "40"));
        StubCustomerAssignmentDAO customerDAO =
                new StubCustomerAssignmentDAO();
        customerDAO.assignments = List.of(
                new MaintenanceCustomerAssignment(
                        "boundary-customer", "Manager"));
        DashboardServlet servlet = new DashboardServlet(
                maintenanceDAO, customerDAO, utcClockAtSeoulMidnight);
        RequestFixture request = new RequestFixture();

        servlet.doGet(request.proxy(), new ResponseFixture().proxy());

        assertEquals("2026-09",
                request.attributes.get("maintenanceMonthParam"));
        @SuppressWarnings("unchecked")
        List<DashboardServlet.MaintenanceAssigneeGroup> groups =
                (List<DashboardServlet.MaintenanceAssigneeGroup>)
                        request.attributes.get(
                                "monthlyMaintenanceAssigneeGroups");
        assertEquals(true,
                groups.getFirst().getCustomers().getFirst().isDone());
    }

    @Test
    void personalSectionUsesSessionIdentityAndIncludesSubAssignedCustomers()
            throws Exception {
        LocalDate today = LocalDate.now(FIXED_CLOCK);
        StubMaintenanceRecordDAO maintenanceDAO = new StubMaintenanceRecordDAO();
        maintenanceDAO.records = List.of(
                maintenanceRecord("my-primary", today, "40"),
                maintenanceRecord("my-sub-assignment", today.plusDays(1), "95"),
                maintenanceRecord("same-name-other-user", today, "40"),
                maintenanceRecord("unassigned-record", today, "40"));
        StubCustomerAssignmentDAO customerDAO = new StubCustomerAssignmentDAO();
        customerDAO.expectedUserId = "session-owner";
        customerDAO.assignments = List.of(
                new MaintenanceCustomerAssignment("my-primary", "Same Name"),
                new MaintenanceCustomerAssignment("my-sub-assignment", "Another Manager"),
                new MaintenanceCustomerAssignment("same-name-other-user", "Same Name"));
        customerDAO.personalAssignments = customerDAO.assignments.subList(0, 2);
        RequestFixture request = new RequestFixture(
                new UserDTO("session-owner", "", "Same Name", "QA"));
        request.parameters.put("userId", "another-user");
        request.parameters.put("userName", "Another Manager");

        new DashboardServlet(maintenanceDAO, customerDAO, FIXED_CLOCK)
                .doGet(request.proxy(), new ResponseFixture().proxy());

        assertEquals("session-owner", customerDAO.lastUserId);
        assertEquals("Same Name", customerDAO.lastDisplayName);
        assertEquals(1, customerDAO.personalAssignmentCalls);
        assertEquals(2, request.attributes.get("personalMaintenanceAssignedCount"));
        List<DashboardServlet.MonthlyMaintenanceCustomer> personal = personalCustomers(request);
        assertEquals(List.of("my-primary", "my-sub-assignment"),
                personal.stream().map(DashboardServlet.MonthlyMaintenanceCustomer::getCustomerName).toList());
        assertEquals(true, personal.getFirst().isDone());
        assertEquals(false, personal.get(1).isDone());
        assertEquals(true, personal.get(1).isLicenseRisk());
        assertEquals(2, request.attributes.get("personalMaintenanceTargetCount"));
        assertEquals(2, request.attributes.get("personalMaintenanceRegisteredCount"));
        assertEquals(0, request.attributes.get("personalMaintenanceUnregisteredCount"));
        assertEquals(1, maintenanceDAO.monthCalls);
        assertEquals(1, customerDAO.assignmentCalls);
        @SuppressWarnings("unchecked")
        List<DashboardServlet.MaintenanceAssigneeGroup> global =
                (List<DashboardServlet.MaintenanceAssigneeGroup>) request.attributes.get(
                        "monthlyMaintenanceAssigneeGroups");
        assertEquals(4, global.stream().mapToInt(group -> group.getCustomers().size()).sum());
    }

    @Test
    void personalMonthlySectionKeepsRecordedOffCycleCustomerAndSkipsUndueCustomer()
            throws Exception {
        LocalDate today = LocalDate.now(FIXED_CLOCK);
        MaintenanceSchedule offCycle = new MaintenanceSchedule(
                3, YearMonth.from(today).minusMonths(1), LocalDate.of(2000, 1, 1), null, true);
        StubMaintenanceRecordDAO maintenanceDAO = new StubMaintenanceRecordDAO();
        maintenanceDAO.records = List.of(maintenanceRecord("off-cycle-recorded", today, "40"));
        StubCustomerAssignmentDAO customerDAO = new StubCustomerAssignmentDAO();
        customerDAO.personalAssignments = List.of(
                new MaintenanceCustomerAssignment("monthly-due", "Tester"),
                new MaintenanceCustomerAssignment("off-cycle-recorded", "Another Manager", offCycle),
                new MaintenanceCustomerAssignment("off-cycle-not-recorded", "Tester", offCycle));
        customerDAO.assignments = customerDAO.personalAssignments;
        RequestFixture request = new RequestFixture();

        new DashboardServlet(maintenanceDAO, customerDAO, FIXED_CLOCK)
                .doGet(request.proxy(), new ResponseFixture().proxy());

        assertEquals(3, request.attributes.get("personalMaintenanceAssignedCount"));
        List<DashboardServlet.MonthlyMaintenanceCustomer> personal = personalCustomers(request);
        assertEquals(List.of("monthly-due", "off-cycle-recorded"),
                personal.stream().map(DashboardServlet.MonthlyMaintenanceCustomer::getCustomerName).toList());
        assertEquals(false, personal.getFirst().isDone());
        assertEquals(true, personal.get(1).isDone());
        assertEquals(true, personal.get(1).isQuarterly());
        assertEquals(1, request.attributes.get("personalMaintenanceTargetCount"));
        assertEquals(0, request.attributes.get("personalMaintenanceRegisteredCount"));
        assertEquals(1, request.attributes.get("personalMaintenanceUnregisteredCount"));
        assertEquals(1, maintenanceDAO.monthCalls);
    }

    @Test
    void offCycleAssignmentsRemainAssignedWhenThereAreNoMonthlyTargets()
            throws Exception {
        LocalDate today = LocalDate.now(FIXED_CLOCK);
        StubCustomerAssignmentDAO customerDAO = new StubCustomerAssignmentDAO();
        customerDAO.personalAssignments = List.of(new MaintenanceCustomerAssignment(
                "quarterly-not-due", "Another Manager", new MaintenanceSchedule(
                        3, YearMonth.from(today).minusMonths(1),
                        LocalDate.of(2000, 1, 1), null, true)));
        customerDAO.assignments = customerDAO.personalAssignments;
        RequestFixture request = new RequestFixture();

        new DashboardServlet(new StubMaintenanceRecordDAO(), customerDAO, FIXED_CLOCK)
                .doGet(request.proxy(), new ResponseFixture().proxy());

        assertEquals(1, request.attributes.get("personalMaintenanceAssignedCount"));
        assertEquals(List.of(), personalCustomers(request));
        assertEquals(0, request.attributes.get("personalMaintenanceTargetCount"));
        assertEquals(0, request.attributes.get("personalMaintenanceRegisteredCount"));
        assertEquals(0, request.attributes.get("personalMaintenanceUnregisteredCount"));
    }

    @Test
    void personalSummaryUsesOnlyUniqueDueCustomersAndScheduleEffectiveWindow()
            throws Exception {
        LocalDate today = LocalDate.now(FIXED_CLOCK);
        YearMonth month = YearMonth.from(today);
        MaintenanceSchedule quarterlyDue = new MaintenanceSchedule(
                3, month.minusMonths(3), LocalDate.of(2000, 1, 1), null, true);
        MaintenanceSchedule expired = new MaintenanceSchedule(
                1, YearMonth.of(2000, 1), LocalDate.of(2000, 1, 1),
                month.minusMonths(1).atEndOfMonth(), true);
        MaintenanceSchedule disabled = new MaintenanceSchedule(
                1, YearMonth.of(2000, 1), LocalDate.of(2000, 1, 1), null, false);
        StubCustomerAssignmentDAO customers = new StubCustomerAssignmentDAO();
        customers.personalAssignments = List.of(
                new MaintenanceCustomerAssignment("monthly", "Tester"),
                new MaintenanceCustomerAssignment("monthly", "Tester"),
                new MaintenanceCustomerAssignment("quarterly", "Other Manager", quarterlyDue),
                new MaintenanceCustomerAssignment("expired", "Tester", expired),
                new MaintenanceCustomerAssignment("disabled", "Tester", disabled));
        customers.assignments = customers.personalAssignments;
        StubMaintenanceRecordDAO records = new StubMaintenanceRecordDAO();
        records.records = List.of(
                maintenanceRecord("monthly", today.plusDays(1), "40"),
                maintenanceRecord("monthly", today.plusDays(2), "40"),
                maintenanceRecord("expired", today, "40"));
        RequestFixture request = new RequestFixture();

        new DashboardServlet(records, customers, FIXED_CLOCK)
                .doGet(request.proxy(), new ResponseFixture().proxy());

        assertEquals(2, request.attributes.get("personalMaintenanceTargetCount"));
        assertEquals(1, request.attributes.get("personalMaintenanceRegisteredCount"));
        assertEquals(1, request.attributes.get("personalMaintenanceUnregisteredCount"));
        assertEquals(false, personalCustomers(request).getFirst().isDone());
        assertEquals(1, records.monthCalls);
        assertEquals(1, customers.assignmentCalls);
        assertEquals(1, customers.personalAssignmentCalls);
    }

    @Test
    void personalSectionStaysEmptyWhenOnlyGlobalRecordsAndAssignmentsExist()
            throws Exception {
        StubMaintenanceRecordDAO maintenanceDAO = new StubMaintenanceRecordDAO();
        MaintenanceRecordDTO record = maintenanceRecord("another-users-customer", LocalDate.now(FIXED_CLOCK), "40");
        record.setInspectorName("Tester");
        maintenanceDAO.records = List.of(record);
        StubCustomerAssignmentDAO customerDAO = new StubCustomerAssignmentDAO();
        customerDAO.assignments = List.of(new MaintenanceCustomerAssignment("another-users-customer", "Tester"));
        RequestFixture request = new RequestFixture();

        new DashboardServlet(maintenanceDAO, customerDAO, FIXED_CLOCK)
                .doGet(request.proxy(), new ResponseFixture().proxy());

        assertEquals(0, request.attributes.get("personalMaintenanceAssignedCount"));
        assertEquals(List.of(), personalCustomers(request));
        assertEquals(0, request.attributes.get("personalMaintenanceTargetCount"));
        assertEquals(0, request.attributes.get("personalMaintenanceRegisteredCount"));
        assertEquals(0, request.attributes.get("personalMaintenanceUnregisteredCount"));
        assertEquals(1, customerDAO.personalAssignmentCalls);
        assertEquals(1, customerDAO.assignmentCalls);
    }

    @SuppressWarnings("unchecked")
    private static List<DashboardServlet.MonthlyMaintenanceCustomer> personalCustomers(
            RequestFixture request) {
        return (List<DashboardServlet.MonthlyMaintenanceCustomer>)
                request.attributes.get("personalMaintenanceCustomers");
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
        private Date lastStartDate;
        private Date lastEndDate;

        @Override
        public List<MaintenanceRecordDTO> getMaintenanceRecordsByMonth(Date startDate, Date endDate) {
            monthCalls++;
            lastStartDate = startDate;
            lastEndDate = endDate;
            return records;
        }
    }

    private static final class StubCustomerAssignmentDAO
            extends CustomerAssignmentDAO {
        private List<MaintenanceCustomerAssignment> assignments = new ArrayList<>();
        private List<MaintenanceCustomerAssignment> personalAssignments = List.of();
        private int assignmentCalls;
        private int personalAssignmentCalls;
        private String expectedUserId = "tester";
        private String lastUserId;
        private String lastDisplayName;

        @Override
        public MaintenanceAssigneeData getMaintenanceAssigneeData(
                String userId, String displayName) {
            personalAssignmentCalls++;
            lastUserId = userId;
            lastDisplayName = displayName;
            List<MaintenanceCustomerAssignment> selected = expectedUserId.equals(userId)
                    ? personalAssignments : List.of();
            List<CustomerDTO> customers = selected.stream().map(assignment -> {
                CustomerDTO customer = new CustomerDTO();
                customer.setCustomerName(assignment.customerName());
                return customer;
            }).toList();
            return new MaintenanceAssigneeData(customers, selected);
        }

        @Override
        public List<MaintenanceCustomerAssignment>
                getAllMaintenanceCustomerAssignments() {
            assignmentCalls++;
            return assignments;
        }
    }

    private static final class RequestFixture {
        private final Map<String, Object> attributes = new HashMap<>();
        private final Map<String, String> parameters = new HashMap<>();
        private final HttpSession session;
        private String forwardedPath;

        private RequestFixture() {
            this(new UserDTO("tester", "", "Tester", "QA"));
        }

        private RequestFixture(UserDTO user) {
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
                        case "getParameter" -> parameters.get((String) args[0]);
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

}
