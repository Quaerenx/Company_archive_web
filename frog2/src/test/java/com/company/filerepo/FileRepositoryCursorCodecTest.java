package com.company.filerepo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.filerepo.FileRepositoryCursorCodec.SortKey;
import org.junit.jupiter.api.Test;

class FileRepositoryCursorCodecTest {
    @Test
    void roundTripPreservesSortKey() throws Exception {
        SortKey key = new SortKey(1, "folded", "Name", "id-1");

        assertEquals(key, FileRepositoryCursorCodec.decode(
                FileRepositoryCursorCodec.encode(key)));
        assertNull(FileRepositoryCursorCodec.decode(null));
        assertNull(FileRepositoryCursorCodec.decode("  "));
    }

    @Test
    void malformedCursorFailsWithStableClientError() {
        FileRepositoryException exception = assertThrows(
                FileRepositoryException.class,
                () -> FileRepositoryCursorCodec.decode("not-a-cursor"));

        assertEquals(400, exception.getHttpStatus());
        assertEquals("invalid_cursor", exception.getCode());
    }
}
