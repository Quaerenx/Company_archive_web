package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class CustomerDAOSoftDeleteContractTest {
    private static final Pattern ACTIVE_CUSTOMER_PREDICATE = Pattern.compile(
            "WHERE\\s+(?:d\\.)?customer_name\\s*=\\s*\\?\\s+AND\\s+"
                    + "(?:d\\.)?is_deleted\\s*=\\s*1");

    @Test
    void singleReadUpdateAndDeleteOnlyTargetActiveCustomers() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/company/model/CustomerDAO.java"));

        assertEquals(
                3,
                occurrences(source, ACTIVE_CUSTOMER_PREDICATE));
    }

    private static int occurrences(String value, Pattern pattern) {
        int count = 0;
        Matcher matcher = pattern.matcher(value);
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
