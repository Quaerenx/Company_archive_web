package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.customerhistory.CustomerHistoryCategory;
import com.company.customerhistory.CustomerHistoryDraft;
import com.company.customerhistory.CustomerHistoryRepository;
import com.company.customerhistory.CustomerHistoryStatus;
import com.company.model.MaintenanceRecordDAO;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.PageResult;
import com.company.model.TroubleshootingDAO;
import com.company.model.TroubleshootingDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CustomerActivityQueryServiceTest {
    @TempDir
    java.nio.file.Path temporaryDirectory;

    @Test
    void loadsThreeRecentActivitySourcesAndIsolatesOneFailure() {
        CustomerHistoryRepository history = new CustomerHistoryRepository(
                temporaryDirectory.resolve("history"));
        history.create(
                new CustomerHistoryDraft(
                        "Alpha",
                        LocalDate.of(2026, 8, 20),
                        CustomerHistoryCategory.CONFIGURATION,
                        "TLS 적용",
                        "운영 TLS 적용",
                        CustomerHistoryStatus.COMPLETED),
                "owner-1",
                "담당자");
        TroubleshootingDTO troubleshooting = new TroubleshootingDTO();
        troubleshooting.setCustomerName("Alpha");
        troubleshooting.setTitle("백업 복구");

        CustomerActivityQueryService service =
                new CustomerActivityQueryService(
                        new FailingMaintenanceDAO(),
                        history,
                        new StubTroubleshootingDAO(troubleshooting));

        CustomerActivityViewData result = service.load("Alpha");

        assertTrue(result.getMaintenanceRecords().isEmpty());
        assertEquals("TLS 적용", result.getHistoryRecords().getFirst().getTitle());
        assertEquals("백업 복구",
                result.getTroubleshootingRecords().getFirst().getTitle());
    }

    private static final class FailingMaintenanceDAO
            extends MaintenanceRecordDAO {
        @Override
        public PageResult<MaintenanceRecordDTO> getMaintenanceRecordsByCustomer(
                String customerName, int requestedPage, int pageSize) {
            throw new IllegalStateException("test failure");
        }
    }

    private static final class StubTroubleshootingDAO
            extends TroubleshootingDAO {
        private final TroubleshootingDTO record;

        private StubTroubleshootingDAO(TroubleshootingDTO record) {
            this.record = record;
        }

        @Override
        public PageResult<TroubleshootingDTO> getTroubleshootingPageByCustomer(
                String customerName, int requestedPage, int pageSize) {
            return new PageResult<>(List.of(record), 1, 1, pageSize);
        }
    }
}
