package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.company.model.MaintenanceRecordDAO;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.UserDTO;
import com.company.model.UserVmHostDAO;
import com.company.model.UserVmHostDTO;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DashboardServletQueryContractTest {
    @Test
    void dashboardPreservesVmHostSummaryAndViewContract() throws Exception {
        StubUserVmHostDAO hostDAO = new StubUserVmHostDAO();
        UserVmHostDTO first = new UserVmHostDTO();
        UserVmHostDTO second = new UserVmHostDTO();
        hostDAO.hosts = List.of(first, second);
        StubMaintenanceRecordDAO maintenanceDAO = new StubMaintenanceRecordDAO();
        LocalDate today = LocalDate.now();
        maintenanceDAO.records = List.of(
                maintenanceRecord("past-low", today.minusDays(1), "40"),
                maintenanceRecord("future-low", today.plusDays(1), "40"),
                maintenanceRecord("past-high", today.minusDays(1), "95"),
                maintenanceRecord("future-high", today.plusDays(1), "95"));
        DashboardServlet servlet = new DashboardServlet(hostDAO, maintenanceDAO);
        RequestFixture request = new RequestFixture();
        ResponseFixture response = new ResponseFixture();

        servlet.doGet(request.proxy(), response.proxy());

        assertSame(hostDAO.hosts, request.attributes.get("vmHosts"));
        assertEquals(2, request.attributes.get("vmHostCount"));
        assertEquals(20, request.attributes.get("vmHostLimit"));
        assertEquals(18, request.attributes.get("vmHostRemaining"));
        assertEquals(4, request.attributes.get("monthlyMaintenanceTotal"));
        assertEquals(2, request.attributes.get("monthlyMaintenanceDoneCount"));
        assertEquals(2, request.attributes.get("monthlyMaintenanceDueCount"));
        assertEquals(2, request.attributes.get("monthlyMaintenanceLicenseRiskCount"));
        assertEquals(3, request.attributes.get("monthlyMaintenanceAttentionCount"));
        assertEquals(2, request.attributes.get("monthlyMaintenanceReviewCount"));
        assertEquals("/dashboard.jsp", request.forwardedPath);
        assertEquals(1, hostDAO.listCalls);
        assertEquals(0, hostDAO.countCalls);
        assertEquals(1, maintenanceDAO.monthCalls);
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

    private static final class StubUserVmHostDAO extends UserVmHostDAO {
        private List<UserVmHostDTO> hosts = new ArrayList<>();
        private int listCalls;
        private int countCalls;

        @Override
        public List<UserVmHostDTO> getActiveHostsByOwner(String ownerUserId) {
            listCalls++;
            return hosts;
        }

        @Override
        public int countActiveHostsByOwner(String ownerUserId) {
            countCalls++;
            return hosts.size();
        }
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
