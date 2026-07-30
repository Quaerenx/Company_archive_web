package com.company.controller;

import com.company.model.CustomerDAO;
import com.company.model.CustomerDTO;
import com.company.model.CustomerDetailDAO;
import com.company.model.CustomerDetailDTO;
import java.util.Objects;

final class CustomerCommandService {
    private final CustomerDAO customerDAO;
    private final CustomerDetailDAO detailDAO;

    CustomerCommandService() {
        this(new CustomerDAO(), new CustomerDetailDAO());
    }

    CustomerCommandService(CustomerDAO customerDAO, CustomerDetailDAO detailDAO) {
        this.customerDAO = Objects.requireNonNull(customerDAO, "customerDAO");
        this.detailDAO = Objects.requireNonNull(detailDAO, "detailDAO");
    }

    boolean updateCustomer(CustomerDTO customer) {
        return customerDAO.updateCustomer(customer);
    }

    boolean addCustomer(CustomerDTO customer) {
        return customerDAO.addCustomer(customer);
    }

    boolean deleteCustomer(String customerName) {
        return customerDAO.deleteCustomer(customerName);
    }

    boolean saveCustomerDetail(CustomerEnvironment environment, CustomerDetailDTO detail) {
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(detail, "detail");
        if (customerDAO.getCustomerByName(detail.getCustomerName()) == null) {
            return false;
        }

        return switch (environment) {
            case PROD -> detailDAO.saveOrUpdateCustomerDetail(detail);
            case STAGING -> detailDAO.saveOrUpdateCustomerDetailStg(detail);
            case DEVELOPMENT -> detailDAO.saveOrUpdateCustomerDetailDev(detail);
        };
    }
}
