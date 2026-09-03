package com.company.mypage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.model.CustomerDTO;
import com.company.model.MaintenanceCustomerAssignment;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.MaintenanceSchedule;
import com.company.mypage.WorkInboxItem.Severity;
import com.company.mypage.WorkInboxItem.Type;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkInboxServiceTest {
    private final WorkInboxService service = new WorkInboxService();

    @Test
    void findsCustomersAssignedAsMainOrSubManager() {
        CustomerDTO main = customer("Alpha", "Manager A", "Other");
        CustomerDTO sub = customer("Beta", "Other", " manager a ");
        CustomerDTO unrelated = customer("Gamma", "Other", "Another");

        List<CustomerDTO> assigned = service.assignedCustomers(
                "Manager A", List.of(main, sub, unrelated));

        assertEquals(
                List.of("Alpha", "Beta"),
                assigned.stream().map(CustomerDTO::getCustomerName).toList());
    }

    @Test
    void ordersActionableAlertsAndKeepsOnlyTheDisplayLimit() {
        CustomerDTO alpha = completeCustomer("Alpha", "Manager A");
        CustomerDTO beta = completeCustomer("Beta", "Manager A");
        beta.setSaid("-");
        CustomerDTO quarterly = completeCustomer("Quarterly", "Manager A");

        MaintenanceRecordDTO alphaLatest = maintenance(
                "Alpha", "2026-07-20", "106.0");
        MaintenanceRecordDTO betaLatest = maintenance(
                "Beta", "2026-08-12", "95.0");
        MaintenanceRecordDTO futureAlpha = maintenance(
                "Alpha", "2026-08-31", "106.0");
        MaintenanceSchedule notDue = new MaintenanceSchedule(
                3,
                YearMonth.of(2026, 7),
                LocalDate.of(2020, 1, 1),
                null,
                true);

        WorkInbox inbox = service.build(
                List.of(alpha, beta, quarterly),
                List.of(
                        new MaintenanceCustomerAssignment(
                                "Alpha", "Manager A"),
                        new MaintenanceCustomerAssignment(
                                "Beta", "Manager A"),
                        new MaintenanceCustomerAssignment(
                                "Quarterly", "Manager A", notDue)),
                List.of(betaLatest, futureAlpha),
                List.of(alphaLatest, betaLatest),
                LocalDate.of(2026, 8, 30),
                3);

        assertEquals(4, inbox.getTotalCount());
        assertEquals(1, inbox.getDangerCount());
        assertEquals(2, inbox.getWarningCount());
        assertEquals(1, inbox.getInfoCount());
        assertEquals(1, inbox.getHiddenCount());
        assertEquals(
                List.of(
                        Severity.DANGER,
                        Severity.WARNING,
                        Severity.WARNING),
                inbox.getItems().stream()
                        .map(WorkInboxItem::getSeverity)
                        .toList());
        assertEquals(
                "라이선스 사용률 위험",
                inbox.getItems().getFirst().getTitle());
        assertEquals(
                "이번 달 정기점검 미진행",
                inbox.getItems().get(1).getTitle());
        assertTrue(inbox.getItems().getFirst().getPath().contains(
                "customerName=Alpha"));
        assertEquals(Type.LICENSE_RISK,
                inbox.getItems().getFirst().getType());
        WorkInboxItem missingMaintenance = inbox.getItems().stream()
                .filter(item -> item.getType() == Type.MAINTENANCE_MISSING)
                .findFirst()
                .orElseThrow();
        assertEquals(LocalDate.of(2026, 8, 31),
                missingMaintenance.getDueDate());
        assertEquals("D-1", missingMaintenance.getTimelineLabel());
        assertEquals("점검 등록", missingMaintenance.getActionLabel());
    }

    private static CustomerDTO customer(
            String name, String mainManager, String subManager) {
        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName(name);
        customer.setManagerName(mainManager);
        customer.setSubManagerName(subManager);
        return customer;
    }

    private static CustomerDTO completeCustomer(
            String name, String manager) {
        CustomerDTO customer = customer(name, manager, null);
        customer.setVerticaVersion("23.4.0-13");
        customer.setDbName("archive");
        customer.setNodes("3");
        customer.setLicenseSize("10TB");
        customer.setSaid("A-S100000000");
        return customer;
    }

    private static MaintenanceRecordDTO maintenance(
            String customerName,
            String date,
            String usagePercentage) {
        MaintenanceRecordDTO record = new MaintenanceRecordDTO();
        record.setCustomerName(customerName);
        record.setInspectionDate(Date.valueOf(date));
        record.setLicenseUsagePct(usagePercentage);
        return record;
    }
}
