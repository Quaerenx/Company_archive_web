package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.company.model.UserVmHostDAO;
import com.company.model.UserVmHostDTO;
import org.junit.jupiter.api.Test;

class UserVmHostServiceTest {
    @Test
    void rejectsInvalidInputBeforeAccessingTheDao() {
        StubUserVmHostDAO dao = new StubUserVmHostDAO();
        UserVmHostService service = new UserVmHostService(dao);
        UserVmHostDTO host = host("10.0.0.1", "purpose");

        assertEquals(
                UserVmHostService.SaveResult.INVALID_IP,
                service.save(host, null));
        assertEquals(0, dao.calls);
    }

    @Test
    void normalizesAndInsertsAValidHost() {
        StubUserVmHostDAO dao = new StubUserVmHostDAO();
        UserVmHostService service = new UserVmHostService(dao);
        UserVmHostDTO host = host(
                " 192.168.40.17 ", " Development host ");
        host.setNote("   ");

        assertEquals(
                UserVmHostService.SaveResult.SAVED,
                service.save(host, null));
        assertEquals("192.168.40.17", host.getIp());
        assertEquals("Development host", host.getPurpose());
        assertNull(host.getNote());
        assertEquals("ACTIVE", host.getStatus());
        assertEquals(1, dao.saveCalls);
    }

    @Test
    void distinguishesDuplicateOwnershipAndHostLimit() {
        StubUserVmHostDAO otherOwnerDao = new StubUserVmHostDAO();
        otherOwnerDao.mutationResult =
                UserVmHostDAO.MutationResult.DUPLICATE_OTHER_IP;

        assertEquals(
                UserVmHostService.SaveResult.DUPLICATE_OTHER_IP,
                new UserVmHostService(otherOwnerDao).save(
                        host("192.168.40.17", "purpose"), null));

        StubUserVmHostDAO limitedDao = new StubUserVmHostDAO();
        limitedDao.mutationResult =
                UserVmHostDAO.MutationResult.HOST_LIMIT_REACHED;

        assertEquals(
                UserVmHostService.SaveResult.HOST_LIMIT_REACHED,
                new UserVmHostService(limitedDao).save(
                        host("192.168.40.18", "purpose"), null));
        assertEquals(1, limitedDao.saveCalls);
    }

    @Test
    void reportsAnUpdateThatDidNotModifyAHost() {
        StubUserVmHostDAO dao = new StubUserVmHostDAO();
        dao.mutationResult = UserVmHostDAO.MutationResult.WRITE_FAILED;

        assertEquals(
                UserVmHostService.SaveResult.WRITE_FAILED,
                new UserVmHostService(dao).save(
                        host("192.168.40.17", "updated"),
                        "192.168.40.17"));
        assertEquals(1, dao.saveCalls);
    }

    private static UserVmHostDTO host(String ip, String purpose) {
        UserVmHostDTO host = new UserVmHostDTO();
        host.setIp(ip);
        host.setOwnerUserId("owner-1");
        host.setOwnerUserName("Owner");
        host.setPurpose(purpose);
        return host;
    }

    private static final class StubUserVmHostDAO extends UserVmHostDAO {
        private MutationResult mutationResult = MutationResult.SAVED;
        private int calls;
        private int saveCalls;

        @Override
        public MutationResult saveNormalized(
                UserVmHostDTO dto, String originalIp) {
            calls++;
            saveCalls++;
            return mutationResult;
        }
    }
}
