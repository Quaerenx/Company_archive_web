package com.company.controller;

import com.company.model.CustomerDAO;
import com.company.model.CustomerDTO;
import com.company.model.CustomerDetailDAO;
import com.company.model.CustomerDetailDTO;
import com.company.model.CustomerDetailSet;
import com.company.model.VerticaEosDAO;
import java.util.Date;
import java.util.Objects;

final class CustomerDetailQueryService {
    private final CustomerDAO customerDAO;
    private final CustomerDetailDAO detailDAO;
    private final VerticaEosDAO eosDAO;

    CustomerDetailQueryService(
            CustomerDAO customerDAO, CustomerDetailDAO detailDAO, VerticaEosDAO eosDAO) {
        this.customerDAO = Objects.requireNonNull(customerDAO, "customerDAO");
        this.detailDAO = Objects.requireNonNull(detailDAO, "detailDAO");
        this.eosDAO = Objects.requireNonNull(eosDAO, "eosDAO");
    }

    ViewData load(String customerName) {
        CustomerDTO customer = customerDAO.getCustomerByName(customerName);
        if (customer == null) {
            return new ViewData(null, null, null, null, null);
        }

        CustomerDetailSet details = detailDAO.getCustomerDetails(customerName);
        String version = firstNonBlank(
                details.production() == null ? null : details.production().getVerticaVersion(),
                customer == null ? null : customer.getVerticaVersion());
        Date eosDate = version == null ? null : eosDAO.findEosDateByVersion(version);
        return new ViewData(customer, details.production(), details.staging(),
                details.development(), eosDate);
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        return second != null && !second.trim().isEmpty() ? second : null;
    }

    record ViewData(
            CustomerDTO customer,
            CustomerDetailDTO production,
            CustomerDetailDTO staging,
            CustomerDetailDTO development,
            Date eosDate) {
    }
}
