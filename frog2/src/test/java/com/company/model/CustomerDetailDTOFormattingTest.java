package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CustomerDetailDTOFormattingTest {
    @Test
    void formatsCapacityAndNodeUnitsForReadOnlyViews() {
        CustomerDetailDTO detail = new CustomerDetailDTO();
        detail.setLicenseInfo("80TB");
        detail.setNodeCount("8");

        assertEquals("80 TB", detail.getLicenseDisplay());
        assertEquals("8대", detail.getNodeCountDisplay());

        detail.setLicenseInfo("54nodes");
        detail.setNodeCount("3+1");
        assertEquals("54nodes", detail.getLicenseDisplay());
        assertEquals("3+1", detail.getNodeCountDisplay());
    }

    @Test
    void cleansCommonSpreadsheetArtifactsOnlyForDisplay() {
        CustomerDetailDTO detail = new CustomerDetailDTO();
        detail.setBackupNote("\"- BACKUP : NFS에 저장- DR : vbr copy_cluster\"");

        assertEquals(
                "- BACKUP : NFS에 저장\n- DR : vbr copy_cluster",
                detail.getBackupNoteDisplay());

        assertNull(new CustomerDetailDTO().getBackupNoteDisplay());
    }
}
