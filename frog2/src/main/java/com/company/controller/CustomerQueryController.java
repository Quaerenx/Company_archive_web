package com.company.controller;

import com.company.customerhistory.CustomerHistoryRepository;
import com.company.model.CustomerDAO;
import com.company.model.CustomerCounts;
import com.company.model.CustomerDTO;
import com.company.model.CustomerDetailDAO;
import com.company.model.CustomerDetailDTO;
import com.company.model.CustomerDetailSet;
import com.company.model.CustomerPage;
import com.company.model.MaintenanceRecordDAO;
import com.company.model.PageResult;
import com.company.model.TroubleshootingDAO;
import com.company.model.VerticaEosDAO;
import com.company.performance.RequestPerformanceContext;
import com.company.performance.RequestPerformanceContext.Operation;
import com.company.web.ApplicationError;
import com.company.web.CsvResponse;
import com.company.web.JsonResponse;
import com.company.util.BusinessDate;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class CustomerQueryController {
    private static final int EXPORT_LIMIT = 10_000;
    private final CustomerDAO customerDAO;
    private final CustomerDetailDAO detailDAO;
    private final CustomerDetailQueryService detailQueryService;
    private final CustomerRequestMapper mapper;
    private final Clock clock;
    private final CustomerActivityLoader activityLoader;

    CustomerQueryController() {
        this(
                new CustomerDAO(),
                new CustomerDetailDAO(),
                new VerticaEosDAO(),
                new CustomerRequestMapper(),
                BusinessDate.systemClock(),
                customerName -> DefaultActivityLoaderHolder.INSTANCE
                        .load(customerName));
    }

    CustomerQueryController(
            CustomerDAO customerDAO,
            CustomerDetailDAO detailDAO,
            VerticaEosDAO eosDAO,
            CustomerRequestMapper mapper) {
        this(
                customerDAO,
                detailDAO,
                eosDAO,
                mapper,
                BusinessDate.systemClock(),
                CustomerActivityLoader.empty());
    }

    CustomerQueryController(
            CustomerDAO customerDAO,
            CustomerDetailDAO detailDAO,
            VerticaEosDAO eosDAO,
            CustomerRequestMapper mapper,
            Clock clock) {
        this(
                customerDAO,
                detailDAO,
                eosDAO,
                mapper,
                clock,
                CustomerActivityLoader.empty());
    }

    CustomerQueryController(
            CustomerDAO customerDAO,
            CustomerDetailDAO detailDAO,
            VerticaEosDAO eosDAO,
            CustomerRequestMapper mapper,
            Clock clock,
            CustomerActivityLoader activityLoader) {
        this.customerDAO = Objects.requireNonNull(customerDAO, "customerDAO");
        this.detailDAO = Objects.requireNonNull(detailDAO, "detailDAO");
        this.detailQueryService = new CustomerDetailQueryService(
                detailDAO, Objects.requireNonNull(eosDAO, "eosDAO"));
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.activityLoader = Objects.requireNonNull(
                activityLoader, "activityLoader");
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
            case "export" -> exportList(request, response);
            case "detail" -> showDetail(request, response);
            case "edit" -> showEdit(request, response);
            case "editDetail" -> showDetailEdit(request, response);
            case "add" -> forward(request, response, "/customers/customers_add.jsp", "add");
            default -> response.sendRedirect("customers?view=list");
        }
    }

    private void exportList(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String sortField = defaultValue(request.getParameter("sortField"), "");
        String sortDirection = defaultValue(
                request.getParameter("sortDirection"), "ASC");
        String filter = defaultValue(request.getParameter("filter"), "maintenance");
        String query;
        try {
            query = mapper.searchQuery(request);
        } catch (IllegalArgumentException exception) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_search_query",
                    exception.getMessage());
            return;
        }

        PageResult<CustomerDTO> page = customerDAO.getCustomerPage(
                sortField,
                sortDirection,
                filter,
                query,
                1,
                EXPORT_LIMIT + 1).result();
        if (page.totalCount() > EXPORT_LIMIT) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "customer_export_too_large",
                    "검색 조건을 좁힌 뒤 다시 내려받아 주세요.");
            return;
        }

        List<List<String>> rows = page.items().stream()
                .map(customer -> List.of(
                        csvValue(customer.getCustomerName()),
                        csvValue(customer.getVerticaVersion()),
                        csvValue(customer.getMode()),
                        csvValue(customer.getOs()),
                        csvValue(customer.getNodes()),
                        csvValue(customer.getLicenseSize()),
                        csvValue(customer.getSaid()),
                        csvValue(customer.getManagerName())))
                .toList();
        CsvResponse.write(
                response,
                "customers.csv",
                List.of(
                        "고객사", "버전", "모드", "OS", "노드수",
                        "라이선스", "SAID", "담당자"),
                rows);
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestPerformanceContext.markOperation(Operation.CUSTOMERS_LIST);
        long dataLoadStart = System.nanoTime();
        String sortField = defaultValue(request.getParameter("sortField"), "");
        String sortDirection = defaultValue(request.getParameter("sortDirection"), "ASC");
        String filter = defaultValue(request.getParameter("filter"), "maintenance");
        String query;
        try {
            query = mapper.searchQuery(request);
        } catch (IllegalArgumentException exception) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_search_query",
                    exception.getMessage());
            return;
        }
        CustomerPage customerPage = customerDAO.getCustomerPage(
                sortField,
                sortDirection,
                filter,
                query,
                mapper.requestedPage(request),
                mapper.requestedPageSize(request));
        PageResult<CustomerDTO> page = customerPage.result();
        CustomerCounts counts = customerPage.counts();
        RequestPerformanceContext.recordDataLoad(
                System.nanoTime() - dataLoadStart);

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
        long viewRenderStart = System.nanoTime();
        try {
            forward(
                    request,
                    response,
                    "/customers/customers_list.jsp",
                    "list");
        } finally {
            RequestPerformanceContext.recordViewRender(
                    System.nanoTime() - viewRenderStart);
        }
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
            request.setAttribute("verticaEosNotice", CustomerEosNotice.from(
                    viewData.eosDate(),
                    BusinessDate.today(clock)));
        }

        request.setAttribute("customer", viewData.customer());
        request.setAttribute("customerDetail", viewData.production());
        request.setAttribute("customerDetailStg", viewData.staging());
        request.setAttribute("customerDetailDev", viewData.development());
        if (viewData.customer() != null) {
            request.setAttribute(
                    "customerActivity",
                    activityLoader.load(customerName));
        }
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

        CustomerDetailSet details = detailDAO.getCustomerDetails(customerName);
        request.setAttribute("customer", customer);
        request.setAttribute("customerDetail", details.production());
        request.setAttribute("customerDetailStg", details.staging());
        request.setAttribute("customerDetailDev", details.development());
        request.setAttribute(
                "customerDetailEnvironments",
                List.of(
                        new CustomerDetailEditEnvironmentView(
                                "prod", "운영", details.production()),
                        new CustomerDetailEditEnvironmentView(
                                "stg", "스테이징", details.staging()),
                        new CustomerDetailEditEnvironmentView(
                                "dev", "개발", details.development())));
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

    private static String csvValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private static List<CustomerDTO> nullToEmpty(List<CustomerDTO> customers) {
        return customers == null ? new ArrayList<>() : customers;
    }

    private static final class DefaultActivityLoaderHolder {
        private static final CustomerActivityLoader INSTANCE =
                new CustomerActivityQueryService(
                        new MaintenanceRecordDAO(),
                        new CustomerHistoryRepository(),
                        new TroubleshootingDAO());
    }
}
