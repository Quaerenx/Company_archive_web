package com.company.controller;

import com.company.model.CustomerDAO;
import com.company.model.CustomerCounts;
import com.company.model.CustomerDTO;
import com.company.model.CustomerDetailDAO;
import com.company.model.CustomerDetailDTO;
import com.company.model.CustomerPage;
import com.company.model.PageResult;
import com.company.model.VerticaEosDAO;
import com.company.web.ApplicationError;
import com.company.web.JsonResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class CustomerQueryController {
    private final CustomerDAO customerDAO;
    private final CustomerDetailDAO detailDAO;
    private final CustomerDetailQueryService detailQueryService;
    private final CustomerRequestMapper mapper;

    CustomerQueryController() {
        this(new CustomerDAO(), new CustomerDetailDAO(), new VerticaEosDAO(),
                new CustomerRequestMapper());
    }

    CustomerQueryController(
            CustomerDAO customerDAO,
            CustomerDetailDAO detailDAO,
            VerticaEosDAO eosDAO,
            CustomerRequestMapper mapper) {
        this.customerDAO = Objects.requireNonNull(customerDAO, "customerDAO");
        this.detailDAO = Objects.requireNonNull(detailDAO, "detailDAO");
        this.detailQueryService = new CustomerDetailQueryService(
                customerDAO, detailDAO, Objects.requireNonNull(eosDAO, "eosDAO"));
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    void handle(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("getDetail".equals(action)) {
            writeDetailJson(request, response);
            return;
        }
        if ("getCustomersForMaintenance".equals(action)) {
            writeMaintenanceOptions(request, response);
            return;
        }

        String view = defaultValue(request.getParameter("view"), "list");
        switch (view) {
            case "list" -> showList(request, response);
            case "detail" -> showDetail(request, response);
            case "edit" -> showEdit(request, response);
            case "editDetail" -> showDetailEdit(request, response);
            case "add" -> forward(request, response, "/customers/customers_add.jsp", "add");
            default -> response.sendRedirect("customers?view=list");
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String sortField = defaultValue(request.getParameter("sortField"), "");
        String sortDirection = defaultValue(request.getParameter("sortDirection"), "ASC");
        String filter = defaultValue(request.getParameter("filter"), "maintenance");
        String query = mapper.searchQuery(request);
        CustomerPage customerPage = customerDAO.getCustomerPage(
                sortField,
                sortDirection,
                filter,
                query,
                mapper.requestedPage(request),
                mapper.requestedPageSize(request));
        PageResult<CustomerDTO> page = customerPage.result();
        CustomerCounts counts = customerPage.counts();

        request.setAttribute("customerList", page.items());
        request.setAttribute("sortField", sortField);
        request.setAttribute("sortDirection", sortDirection);
        request.setAttribute("filter", filter);
        request.setAttribute("q", query);
        request.setAttribute("currentCount", page.totalCount());
        request.setAttribute("pageItemCount", page.items().size());
        request.setAttribute("totalCount", counts.total());
        request.setAttribute("maintenanceCount", counts.maintenance());
        request.setAttribute("currentPage", page.page());
        request.setAttribute("pageSize", page.pageSize());
        request.setAttribute("totalPages", page.totalPages());
        forward(request, response, "/customers/customers_list.jsp", "list");
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String customerName = mapper.decodedParameter(request, "customerName");
        if (customerName == null || customerName.isEmpty()) {
            response.sendRedirect("customers?view=list");
            return;
        }

        CustomerDetailQueryService.ViewData viewData = detailQueryService.load(customerName);
        if (viewData.eosDate() != null) {
            request.setAttribute("verticaEosDate", viewData.eosDate());
        }

        request.setAttribute("customer", viewData.customer());
        request.setAttribute("customerDetail", viewData.production());
        request.setAttribute("customerDetailStg", viewData.staging());
        request.setAttribute("customerDetailDev", viewData.development());
        forward(request, response, "/customers/customers_detail.jsp", "detail");
    }

    private void showEdit(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String customerName = mapper.decodedParameter(request, "name");
        if (customerName == null || customerName.isEmpty()) {
            response.sendRedirect("customers?view=list");
            return;
        }

        CustomerDTO customer = customerDAO.getCustomerByName(customerName);
        if (customer == null) {
            response.sendRedirect("customers?view=list");
            return;
        }

        request.setAttribute("customer", customer);
        forward(request, response, "/customers/customers_edit.jsp", "edit");
    }

    private void showDetailEdit(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String customerName = mapper.decodedParameter(request, "customerName");
        if (customerName == null || customerName.isEmpty()) {
            response.sendRedirect("customers?view=list");
            return;
        }

        CustomerEnvironment environment;
        try {
            environment = mapper.environment(request);
        } catch (IllegalArgumentException exception) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_environment",
                    "Invalid customer environment");
            return;
        }

        CustomerDTO customer = customerDAO.getCustomerByName(customerName);
        if (customer == null) {
            response.sendRedirect("customers?view=list");
            return;
        }

        request.setAttribute("customer", customer);
        request.setAttribute("customerDetail", detail(environment, customerName));
        request.setAttribute("env", environment.externalValue());
        forward(request, response, "/customers/customers_detail_edit.jsp", "editDetail");
    }

    private void writeDetailJson(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String customerName = mapper.decodedParameter(request, "customerName");
        CustomerDTO customer = customerDAO.getCustomerByName(customerName);
        if (customer == null) {
            JsonResponse.sendError(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "customer_not_found",
                    "고객사 정보를 찾을 수 없습니다.");
            return;
        }

        CustomerDetailDTO detail = detailDAO.getCustomerDetail(customerName);
        CustomerJsonResponse.writeDetail(response, detail, customer);
    }

    private void writeMaintenanceOptions(
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<CustomerDTO> customers =
                nullToEmpty(customerDAO.getAllCustomers("", "ASC"));
        CustomerJsonResponse.writeMaintenanceOptions(response, customers);
    }

    private CustomerDetailDTO detail(CustomerEnvironment environment, String customerName) {
        return switch (environment) {
            case PROD -> detailDAO.getCustomerDetail(customerName);
            case STAGING -> detailDAO.getCustomerDetailStg(customerName);
            case DEVELOPMENT -> detailDAO.getCustomerDetailDev(customerName);
        };
    }

    private static void forward(
            HttpServletRequest request,
            HttpServletResponse response,
            String path,
            String viewType) throws ServletException, IOException {
        request.setAttribute("viewType", viewType);
        request.getRequestDispatcher(path).forward(request, response);
    }

    private static String defaultValue(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static List<CustomerDTO> nullToEmpty(List<CustomerDTO> customers) {
        return customers == null ? new ArrayList<>() : customers;
    }
}
