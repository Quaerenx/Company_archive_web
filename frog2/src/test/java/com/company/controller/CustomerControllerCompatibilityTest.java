package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.model.CustomerDAO;
import com.company.model.CustomerCounts;
import com.company.model.CustomerDTO;
import com.company.model.CustomerDetailDAO;
import com.company.model.CustomerDetailDTO;
import com.company.model.CustomerDetailSet;
import com.company.model.CustomerPage;
import com.company.model.PageResult;
import com.company.model.UserDTO;
import com.company.model.VerticaEosDAO;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CustomerControllerCompatibilityTest {
    @Test
    void listKeepsDefaultFilterSortCountsAndView() throws Exception {
        StubCustomerDAO customerDAO = new StubCustomerDAO();
        CustomerDTO first = new CustomerDTO();
        first.setCustomerName("Acme");
        CustomerDTO second = new CustomerDTO();
        second.setCustomerName("Beta");
        customerDAO.customers = List.of(first, second);
        CustomersServlet servlet = servlet(customerDAO, new StubDetailDAO());

        RequestFixture request = new RequestFixture();
        ResponseFixture response = new ResponseFixture();
        servlet.doGet(request.proxy(), response.proxy());

        assertEquals("", customerDAO.lastSortField);
        assertEquals("ASC", customerDAO.lastSortDirection);
        assertEquals("maintenance", customerDAO.lastFilter);
        assertEquals(1, customerDAO.lastPage);
        assertEquals(50, customerDAO.lastPageSize);
        assertEquals(null, customerDAO.lastQuery);
        assertEquals("/customers/customers_list.jsp", request.forwardedPath);
        assertEquals("list", request.attributes.get("viewType"));
        assertEquals("maintenance", request.attributes.get("filter"));
        assertSame(customerDAO.customers, request.attributes.get("customerList"));
        assertEquals(2, request.attributes.get("currentCount"));
        assertEquals(2, request.attributes.get("pageItemCount"));
        assertEquals(3, request.attributes.get("totalCount"));
        assertEquals(2, request.attributes.get("maintenanceCount"));
        assertEquals(1, request.attributes.get("currentPage"));
        assertEquals(1, request.attributes.get("totalPages"));
    }

    @Test
    void detailKeepsCustomerNameParameterAndLoadsAllEnvironments() throws Exception {
        StubCustomerDAO customerDAO = new StubCustomerDAO();
        StubDetailDAO detailDAO = new StubDetailDAO();
        detailDAO.production.setCustomerName("Acme Corp");
        detailDAO.production.setDbName("archive");
        detailDAO.production.setVerticaVersion("12.0");
        detailDAO.production.setDbMode("ENT");
        detailDAO.production.setMainManager("Owner");
        CustomersServlet servlet = servlet(customerDAO, detailDAO);

        RequestFixture request = new RequestFixture();
        request.parameters.put("view", "detail");
        request.parameters.put("customerName", "Acme Corp");
        ResponseFixture response = new ResponseFixture();
        servlet.doGet(request.proxy(), response.proxy());

        assertEquals(null, customerDAO.lastCustomerName);
        assertEquals(List.of("prod:Acme Corp", "stg:Acme Corp", "dev:Acme Corp"),
                detailDAO.reads);
        assertEquals("/customers/customers_detail.jsp", request.forwardedPath);
        assertEquals("detail", request.attributes.get("viewType"));
        assertSame(detailDAO.production, request.attributes.get("customerDetail"));
        assertSame(detailDAO.staging, request.attributes.get("customerDetailStg"));
        assertSame(detailDAO.development, request.attributes.get("customerDetailDev"));
        CustomerDTO customer = (CustomerDTO) request.attributes.get("customer");
        assertEquals("Acme Corp", customer.getCustomerName());
        assertEquals("archive", customer.getDbName());
        assertEquals("12.0", customer.getVerticaVersion());
        assertEquals("ENT", customer.getMode());
        assertEquals("Owner", customer.getManagerName());
    }

    @Test
    void editDetailKeepsEnvironmentAllowlistAndLegacyView() throws Exception {
        StubCustomerDAO customerDAO = new StubCustomerDAO();
        customerDAO.customer = customer("Acme");
        StubDetailDAO detailDAO = new StubDetailDAO();
        CustomersServlet servlet = servlet(customerDAO, detailDAO);

        RequestFixture request = new RequestFixture();
        request.parameters.put("view", "editDetail");
        request.parameters.put("customerName", "Acme");
        request.parameters.put("env", "STG");
        ResponseFixture response = new ResponseFixture();
        servlet.doGet(request.proxy(), response.proxy());

        assertEquals(List.of("stg:Acme"), detailDAO.reads);
        assertEquals("stg", request.attributes.get("env"));
        assertEquals("editDetail", request.attributes.get("viewType"));
        assertEquals("/customers/customers_detail_edit.jsp", request.forwardedPath);
    }

    @Test
    void addAndSaveDetailKeepLegacyParametersAndRedirects() throws Exception {
        StubCustomerDAO customerDAO = new StubCustomerDAO();
        StubDetailDAO detailDAO = new StubDetailDAO();
        CustomersServlet servlet = servlet(customerDAO, detailDAO);

        RequestFixture add = new RequestFixture();
        add.parameters.put("action", "add");
        add.parameters.put("customer_name", "Acme");
        ResponseFixture addResponse = new ResponseFixture();
        servlet.doPost(add.proxy(), addResponse.proxy());

        assertEquals("Acme", customerDAO.added.getCustomerName());
        assertEquals("customers?view=list", addResponse.redirect);
        assertTrue(add.sessionAttributes.containsKey("message"));

        customerDAO.customer = customer("Acme Corp");
        RequestFixture save = new RequestFixture();
        save.parameters.put("action", "saveDetail");
        save.parameters.put("env", "dev");
        save.parameters.put("customerName", "Acme Corp");
        save.parameters.put("createDate", "2026-07-21");
        ResponseFixture saveResponse = new ResponseFixture();
        servlet.doPost(save.proxy(), saveResponse.proxy());

        assertEquals(List.of("dev:Acme Corp"), detailDAO.writes);
        assertEquals(
                "customers?view=detail&customerName=Acme+Corp&env=dev",
                saveResponse.redirect);
        assertTrue(save.sessionAttributes.containsKey("message"));
    }

    @Test
    void invalidEnvironmentReturnsBadRequestBeforeDetailReadOrWrite() throws Exception {
        StubCustomerDAO customerDAO = new StubCustomerDAO();
        customerDAO.customer = customer("Acme");
        StubDetailDAO detailDAO = new StubDetailDAO();
        CustomersServlet servlet = servlet(customerDAO, detailDAO);

        RequestFixture get = new RequestFixture();
        get.parameters.put("view", "editDetail");
        get.parameters.put("customerName", "Acme");
        get.parameters.put("env", "development-copy");
        ResponseFixture getResponse = new ResponseFixture();
        servlet.doGet(get.proxy(), getResponse.proxy());

        RequestFixture post = new RequestFixture();
        post.parameters.put("action", "saveDetail");
        post.parameters.put("customerName", "Acme");
        post.parameters.put("env", "development-copy");
        ResponseFixture postResponse = new ResponseFixture();
        servlet.doPost(post.proxy(), postResponse.proxy());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, getResponse.status);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, postResponse.status);
        assertTrue(postResponse.body.toString().contains("\"code\":\"invalid_environment\""));
        assertTrue(detailDAO.reads.isEmpty());
        assertTrue(detailDAO.writes.isEmpty());
    }

    @Test
    void missingEnvironmentKeepsLegacyProductionSaveRedirect() throws Exception {
        StubCustomerDAO customerDAO = new StubCustomerDAO();
        customerDAO.customer = customer("Acme");
        StubDetailDAO detailDAO = new StubDetailDAO();
        CustomersServlet servlet = servlet(customerDAO, detailDAO);

        RequestFixture request = new RequestFixture();
        request.parameters.put("action", "saveDetail");
        request.parameters.put("customerName", "Acme");
        ResponseFixture response = new ResponseFixture();
        servlet.doPost(request.proxy(), response.proxy());

        assertEquals(List.of("prod:Acme"), detailDAO.writes);
        assertEquals(
                "customers?view=detail&customerName=Acme&env=prod",
                response.redirect);
    }

    @Test
    void inactiveCustomerCannotExposeOrChangeEnvironmentDetails() throws Exception {
        StubCustomerDAO customerDAO = new StubCustomerDAO();
        StubDetailDAO detailDAO = new StubDetailDAO();
        detailDAO.production = null;
        StubEosDAO eosDAO = new StubEosDAO();
        CustomersServlet servlet = servlet(customerDAO, detailDAO, eosDAO);

        RequestFixture detail = new RequestFixture();
        detail.parameters.put("view", "detail");
        detail.parameters.put("customerName", "Retired");
        servlet.doGet(detail.proxy(), new ResponseFixture().proxy());

        RequestFixture edit = new RequestFixture();
        edit.parameters.put("view", "editDetail");
        edit.parameters.put("customerName", "Retired");
        edit.parameters.put("env", "dev");
        ResponseFixture editResponse = new ResponseFixture();
        servlet.doGet(edit.proxy(), editResponse.proxy());

        RequestFixture json = new RequestFixture();
        json.parameters.put("action", "getDetail");
        json.parameters.put("customerName", "Retired");
        ResponseFixture jsonResponse = new ResponseFixture();
        servlet.doGet(json.proxy(), jsonResponse.proxy());

        RequestFixture save = new RequestFixture();
        save.parameters.put("action", "saveDetail");
        save.parameters.put("customerName", "Retired");
        save.parameters.put("env", "dev");
        ResponseFixture saveResponse = new ResponseFixture();
        servlet.doPost(save.proxy(), saveResponse.proxy());

        assertEquals("/customers/customers_detail.jsp", detail.forwardedPath);
        assertEquals("customers?view=list", editResponse.redirect);
        assertEquals(HttpServletResponse.SC_NOT_FOUND, jsonResponse.status);
        assertTrue(jsonResponse.body.toString().contains("\"code\":\"customer_not_found\""));
        assertEquals(
                "customers?view=detail&customerName=Retired&env=dev",
                saveResponse.redirect);
        assertTrue(save.sessionAttributes.containsKey("error"));
        assertEquals(null, detail.attributes.get("customer"));
        assertEquals(null, detail.attributes.get("customerDetail"));
        assertEquals(null, detail.attributes.get("customerDetailStg"));
        assertEquals(null, detail.attributes.get("customerDetailDev"));
        assertTrue(detailDAO.writes.isEmpty());
        assertEquals(0, eosDAO.reads);
    }

    private static CustomerDTO customer(String name) {
        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName(name);
        return customer;
    }

    private static CustomersServlet servlet(
            StubCustomerDAO customerDAO, StubDetailDAO detailDAO) {
        return servlet(customerDAO, detailDAO, new StubEosDAO());
    }

    private static CustomersServlet servlet(
            StubCustomerDAO customerDAO, StubDetailDAO detailDAO, StubEosDAO eosDAO) {
        CustomerQueryController query = new CustomerQueryController(
                customerDAO, detailDAO, eosDAO, new CustomerRequestMapper());
        CustomerCommandService service = new CustomerCommandService(customerDAO, detailDAO);
        CustomerCommandController command =
                new CustomerCommandController(service, new CustomerRequestMapper());
        return new CustomersServlet(query, command);
    }

    private static final class StubCustomerDAO extends CustomerDAO {
        private List<CustomerDTO> customers = new ArrayList<>();
        private CustomerDTO customer;
        private CustomerDTO added;
        private String lastSortField;
        private String lastSortDirection;
        private String lastFilter;
        private String lastQuery;
        private String lastCustomerName;
        private int lastPage;
        private int lastPageSize;

        @Override
        public CustomerPage getCustomerPage(
                String sortField,
                String sortDirection,
                String filter,
                String query,
                int requestedPage,
                int pageSize) {
            lastSortField = sortField;
            lastSortDirection = sortDirection;
            lastFilter = filter;
            lastQuery = query;
            lastPage = requestedPage;
            lastPageSize = pageSize;
            return new CustomerPage(
                    new PageResult<>(
                            customers,
                            customers.size(),
                            1,
                            pageSize),
                    new CustomerCounts(3, 2));
        }

        @Override
        public CustomerDTO getCustomerByName(String customerName) {
            lastCustomerName = customerName;
            return customer;
        }

        @Override
        public boolean addCustomer(CustomerDTO customer) {
            added = customer;
            return true;
        }

        @Override
        public boolean updateCustomer(CustomerDTO customer) {
            return true;
        }

        @Override
        public boolean deleteCustomer(String customerName) {
            return true;
        }
    }

    private static final class StubDetailDAO extends CustomerDetailDAO {
        private CustomerDetailDTO production = new CustomerDetailDTO();
        private final CustomerDetailDTO staging = new CustomerDetailDTO();
        private final CustomerDetailDTO development = new CustomerDetailDTO();
        private final List<String> reads = new ArrayList<>();
        private final List<String> writes = new ArrayList<>();

        @Override
        public CustomerDetailDTO getCustomerDetail(String customerName) {
            reads.add("prod:" + customerName);
            return production;
        }

        @Override
        public CustomerDetailDTO getCustomerDetailStg(String customerName) {
            reads.add("stg:" + customerName);
            return staging;
        }

        @Override
        public CustomerDetailDTO getCustomerDetailDev(String customerName) {
            reads.add("dev:" + customerName);
            return development;
        }

        @Override
        public CustomerDetailSet getCustomerDetails(String customerName) {
            reads.add("prod:" + customerName);
            reads.add("stg:" + customerName);
            reads.add("dev:" + customerName);
            return new CustomerDetailSet(production, staging, development);
        }

        @Override
        public boolean saveOrUpdateCustomerDetail(CustomerDetailDTO detail) {
            writes.add("prod:" + detail.getCustomerName());
            return true;
        }

        @Override
        public boolean saveOrUpdateCustomerDetailStg(CustomerDetailDTO detail) {
            writes.add("stg:" + detail.getCustomerName());
            return true;
        }

        @Override
        public boolean saveOrUpdateCustomerDetailDev(CustomerDetailDTO detail) {
            writes.add("dev:" + detail.getCustomerName());
            return true;
        }
    }

    private static final class StubEosDAO extends VerticaEosDAO {
        private int reads;

        @Override
        public Date findEosDateByVersion(String versionText) {
            reads++;
            return new Date(1_000L);
        }
    }

    private static final class RequestFixture {
        private final Map<String, String> parameters = new HashMap<>();
        private final Map<String, Object> attributes = new HashMap<>();
        private final Map<String, Object> sessionAttributes = new HashMap<>();
        private final HttpSession session;
        private String forwardedPath;

        private RequestFixture() {
            sessionAttributes.put("user", new UserDTO("tester", "", "Tester", "QA"));
            session = (HttpSession) Proxy.newProxyInstance(
                    HttpSession.class.getClassLoader(),
                    new Class<?>[] {HttpSession.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "getAttribute" -> sessionAttributes.get((String) args[0]);
                        case "setAttribute" -> {
                            sessionAttributes.put((String) args[0], args[1]);
                            yield null;
                        }
                        default -> defaultValue(call.getReturnType());
                    });
        }

        private HttpServletRequest proxy() {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[] {HttpServletRequest.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "getSession" -> session;
                        case "getParameter" -> parameters.get((String) args[0]);
                        case "setAttribute" -> {
                            attributes.put((String) args[0], args[1]);
                            yield null;
                        }
                        case "getRequestDispatcher" -> dispatcher((String) args[0]);
                        default -> defaultValue(call.getReturnType());
                    });
        }

        private RequestDispatcher dispatcher(String path) {
            return (RequestDispatcher) Proxy.newProxyInstance(
                    RequestDispatcher.class.getClassLoader(),
                    new Class<?>[] {RequestDispatcher.class},
                    (ignored, call, args) -> {
                        if ("forward".equals(call.getName())) {
                            forwardedPath = path;
                        }
                        return null;
                    });
        }
    }

    private static final class ResponseFixture {
        private final StringWriter body = new StringWriter();
        private final PrintWriter writer = new PrintWriter(body);
        private String redirect;
        private int status;

        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] {HttpServletResponse.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "sendRedirect" -> {
                            redirect = (String) args[0];
                            yield null;
                        }
                        case "sendError" -> {
                            status = (Integer) args[0];
                            yield null;
                        }
                        case "setStatus" -> {
                            status = (Integer) args[0];
                            yield null;
                        }
                        case "getWriter" -> writer;
                        default -> defaultValue(call.getReturnType());
                    });
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
