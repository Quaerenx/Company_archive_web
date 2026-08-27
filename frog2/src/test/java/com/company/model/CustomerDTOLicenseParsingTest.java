package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CustomerDTOLicenseParsingTest {
    @Test
    void separatesCapacityAndNodeLicenseValuesForPresentation() {
        CustomerDTO customer = new CustomerDTO();

        customer.setLicenseSize("500TB");
        assertEquals("500", customer.getLicenseAmount());
        assertEquals("TB", customer.getLicenseUnit());

        customer.setLicenseSize("54nodes");
        assertEquals("54", customer.getLicenseAmount());
        assertEquals("nodes", customer.getLicenseUnit());

        customer.setLicenseSize(" 12.5 TB ");
        assertEquals("12.5", customer.getLicenseAmount());
        assertEquals("TB", customer.getLicenseUnit());

        customer.setLicenseSize("unlimited");
        assertNull(customer.getLicenseAmount());
        assertNull(customer.getLicenseUnit());
    }
}
