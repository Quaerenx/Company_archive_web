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

    boolean updateCustomer(CustomerDTO customer, String actorUserId) {
        return customerDAO.updateCustomer(customer, actorUserId);
    }

    boolean addCustomer(CustomerDTO customer) {
        return customerDAO.addCustomer(customer);
    }

    boolean addCustomer(CustomerDTO customer, String actorUserId) {
        return customerDAO.addCustomer(customer, actorUserId);
    }

    boolean deleteCustomer(String customerName) {
        return customerDAO.deleteCustomer(customerName);
    }

    boolean deleteCustomer(String customerName, String actorUserId) {
        return customerDAO.deleteCustomer(customerName, actorUserId);
    }

    boolean saveCustomerDetail(CustomerEnvironment environment, CustomerDetailDTO detail) {
        return saveCustomerDetail(environment, detail, null);
    }

    boolean saveCustomerDetail(
            CustomerEnvironment environment,
            CustomerDetailDTO detail,
            String actorUserId) {
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(detail, "detail");
        if (customerDAO.getCustomerByName(detail.getCustomerName()) == null) {
            return false;
        }

        return switch (environment) {
            case PROD -> detailDAO.saveOrUpdateCustomerDetail(
                    detail, actorUserId);
            case STAGING -> detailDAO.saveOrUpdateCustomerDetailStg(detail);
            case DEVELOPMENT -> detailDAO.saveOrUpdateCustomerDetailDev(detail);
        };
    }
}
