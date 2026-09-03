package com.company.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.customerhistory.CustomerHistoryCategory;
import com.company.customerhistory.CustomerHistoryDraft;
import com.company.customerhistory.CustomerHistoryRepository;
import com.company.customerhistory.CustomerHistoryStatus;
import com.company.filerepo.FileRepositoryService;
import com.company.model.CustomerDAO;
import com.company.model.CustomerDTO;
import com.company.model.MeetingRecordDAO;
import com.company.model.MeetingRecordDTO;
import com.company.model.MaintenanceRecordDAO;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.PageResult;
import com.company.model.TroubleshootingDAO;
import com.company.model.TroubleshootingDTO;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GlobalSearchServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void combinesSixDomainsInStableCategoryOrder() throws Exception {
        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName("조폐공사");
        customer.setVerticaVersion("12.0.2-1");
        customer.setDbName("CKDEV");

        TroubleshootingDTO troubleshooting = new TroubleshootingDTO();
        troubleshooting.setId(7);
        troubleshooting.setTitle("조폐공사 TLS 장애 조치");
        troubleshooting.setCustomerName("조폐공사");
        troubleshooting.setOccurrenceDate(new Date(
                Timestamp.valueOf("2026-07-24 10:00:00").getTime()));

        MeetingRecordDTO meeting = new MeetingRecordDTO();
        meeting.setMeetingId(9L);
        meeting.setTitle("조폐공사 작업 회의");
        meeting.setMeetingType("project");
        meeting.setMeetingDatetime(Timestamp.valueOf(
                "2026-07-23 14:00:00"));

        MaintenanceRecordDTO maintenance = new MaintenanceRecordDTO();
        maintenance.setCustomerName("조폐공사");
        maintenance.setInspectionDate(java.sql.Date.valueOf("2026-07-22"));
        maintenance.setVerticaVersion("12.0.2-1");

        CustomerHistoryRepository history = new CustomerHistoryRepository(
                temporaryDirectory.resolve("history"));
        history.create(
                new CustomerHistoryDraft(
                        "조폐공사",
                        LocalDate.of(2026, 7, 24),
                        CustomerHistoryCategory.CONFIGURATION,
                        "TLS 설정 반영",
                        "운영 환경 TLS 설정을 반영했습니다.",
                        CustomerHistoryStatus.COMPLETED),
                "owner-id",
                "담당자");

        Path fileRoot = Files.createDirectory(
                temporaryDirectory.resolve("files"));
        FileRepositoryService files = new FileRepositoryService(fileRoot);
        byte[] content = "safe".getBytes(StandardCharsets.UTF_8);
        var validated = files.validateUpload(
                "조폐공사 TLS 결과.txt", "text/plain", content.length);
        files.store(
                "",
                validated,
                content.length,
                new ByteArrayInputStream(content));

        GlobalSearchService service = new GlobalSearchService(
                new StubCustomerDAO(customer),
                history,
                new StubTroubleshootingDAO(troubleshooting),
                new StubMeetingDAO(meeting),
                new StubMaintenanceDAO(maintenance),
                files);

        GlobalSearchOutcome outcome = service.search("조폐공사");
        List<GlobalSearchResult> results = outcome.results();

        assertTrue(outcome.unavailableCategories().isEmpty());
        assertEquals(
                List.of(
                        "고객사",
                        "고객사 히스토리",
                        "정기점검 이력",
                        "트러블슈팅",
                        "회의록",
                        "자료실 파일"),
                results.stream().map(GlobalSearchResult::category).toList());
        assertTrue(results.getFirst().path().contains(
                "customerName=%EC%A1%B0%ED%8F%90%EA%B3%B5%EC%82%AC"));
        assertEquals(
                "/troubleshooting?view=view&id=7",
                results.get(3).path());
        assertTrue(results.getLast().path().startsWith(
                "/file-repository/download?"));
    }

    @Test
    void keepsAvailableResultsWhenOneSourceFails() throws Exception {
        TroubleshootingDTO troubleshooting = new TroubleshootingDTO();
        troubleshooting.setId(11);
        troubleshooting.setTitle("TLS 인증서 교체");

        Path fileRoot = Files.createDirectory(
                temporaryDirectory.resolve("partial-files"));
        GlobalSearchService service = new GlobalSearchService(
                new FailingCustomerDAO(),
                new CustomerHistoryRepository(
                        temporaryDirectory.resolve("partial-history")),
                new StubTroubleshootingDAO(troubleshooting),
                new EmptyMeetingDAO(),
                new EmptyMaintenanceDAO(),
                new FileRepositoryService(fileRoot));

        GlobalSearchOutcome outcome = service.search("TLS");

        assertEquals(List.of("고객사"), outcome.unavailableCategories());
        assertTrue(outcome.partial());
        assertTrue(!outcome.allSourcesUnavailable());
        assertEquals("트러블슈팅", outcome.results().getFirst().category());
    }

    private static final class StubCustomerDAO extends CustomerDAO {
        private final CustomerDTO customer;

        private StubCustomerDAO(CustomerDTO customer) {
            this.customer = customer;
        }

        @Override
        public List<CustomerDTO> searchCustomers(String query, int limit) {
            return List.of(customer);
        }
    }

    private static final class FailingCustomerDAO extends CustomerDAO {
        @Override
        public List<CustomerDTO> searchCustomers(String query, int limit) {
            throw new IllegalStateException("test source failure");
        }
    }

    private static final class StubTroubleshootingDAO
            extends TroubleshootingDAO {
        private final TroubleshootingDTO troubleshooting;

        private StubTroubleshootingDAO(
                TroubleshootingDTO troubleshooting) {
            this.troubleshooting = troubleshooting;
        }

        @Override
        public PageResult<TroubleshootingDTO> getTroubleshootingPage(
                String query,
                boolean includeContent,
                int requestedPage,
                int pageSize) {
            return new PageResult<>(
                    List.of(troubleshooting), 1, 1, pageSize);
        }
    }

    private static final class StubMeetingDAO extends MeetingRecordDAO {
        private final MeetingRecordDTO meeting;

        private StubMeetingDAO(MeetingRecordDTO meeting) {
            this.meeting = meeting;
        }

        @Override
        public List<MeetingRecordDTO> searchMeetingRecords(
                String query, int limit) {
            return List.of(meeting);
        }
    }

    private static final class StubMaintenanceDAO
            extends MaintenanceRecordDAO {
        private final MaintenanceRecordDTO record;

        private StubMaintenanceDAO(MaintenanceRecordDTO record) {
            this.record = record;
        }

        @Override
        public List<MaintenanceRecordDTO> searchMaintenanceRecords(
                String query, int limit) {
            return List.of(record);
        }
    }

    private static final class EmptyMeetingDAO extends MeetingRecordDAO {
        @Override
        public List<MeetingRecordDTO> searchMeetingRecords(
                String query, int limit) {
            return List.of();
        }
    }

    private static final class EmptyMaintenanceDAO
            extends MaintenanceRecordDAO {
        @Override
        public List<MaintenanceRecordDTO> searchMaintenanceRecords(
                String query, int limit) {
            return List.of();
        }
    }
}
