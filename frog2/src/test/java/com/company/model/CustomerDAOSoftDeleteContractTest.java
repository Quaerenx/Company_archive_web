package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CustomerDAOSoftDeleteContractTest {
    @Test
    void singleReadUpdateAndDeleteOnlyTargetActiveCustomers() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/company/model/CustomerDAO.java"));

        assertEquals(
                3,
                occurrences(source, "WHERE customer_name = ? AND is_deleted = 1"));
    }

    private static int occurrences(String value, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }
}
