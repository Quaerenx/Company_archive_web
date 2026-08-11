package com.company.controller;

import com.company.model.CustomerDTO;
import com.company.model.CustomerDetailDAO;
import com.company.model.CustomerDetailDTO;
import com.company.model.CustomerDetailSet;
import com.company.model.VerticaEosDAO;
import java.util.Date;
import java.util.Objects;

final class CustomerDetailQueryService {
    private final CustomerDetailDAO detailDAO;
    private final VerticaEosDAO eosDAO;

    CustomerDetailQueryService(
            CustomerDetailDAO detailDAO, VerticaEosDAO eosDAO) {
        this.detailDAO = Objects.requireNonNull(detailDAO, "detailDAO");
        this.eosDAO = Objects.requireNonNull(eosDAO, "eosDAO");
    }

    ViewData load(String customerName) {
        CustomerDetailSet details = detailDAO.getCustomerDetails(customerName);
        CustomerDetailDTO production = details.production();
        if (production == null) {
            return new ViewData(null, null, null, null, null);
        }
        CustomerDTO customer = customerSummary(production);
        String version = firstNonBlank(
                production.getVerticaVersion(), customer.getVerticaVersion());
        Date eosDate = version == null ? null : eosDAO.findEosDateByVersion(version);
        return new ViewData(customer, production, details.staging(),
                details.development(), eosDate);
    }

    private static CustomerDTO customerSummary(CustomerDetailDTO detail) {
        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName(detail.getCustomerName());
        customer.setDbName(detail.getDbName());
        customer.setVerticaVersion(detail.getVerticaVersion());
        customer.setMode(detail.getDbMode());
        customer.setOs(detail.getOsInfo());
        customer.setNodes(detail.getNodeCount());
        customer.setLicenseSize(detail.getLicenseInfo());
        customer.setManagerName(detail.getMainManager());
        customer.setSubManagerName(detail.getSubManager());
        customer.setSaid(detail.getSaid());
        customer.setCustomerType(detail.getCustomerType());
        return customer;
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
