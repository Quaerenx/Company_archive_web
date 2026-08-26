package com.company.controller;

import com.company.util.StrictDateParser;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.company.model.CustomerDAO;
import com.company.model.CustomerDTO;
import com.company.model.MaintenanceCustomerAssignment;
import com.company.model.MaintenanceFormHistoryContext;
import com.company.model.MaintenanceHistoryFilter;
import com.company.model.MaintenanceRecordDAO;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.PageResult;
import com.company.model.UserDTO;
import com.company.security.SessionPrincipal;
import com.company.web.ApplicationError;
import com.company.web.JsonResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// @WebServlet("/maintenance") - web.xml에서 매핑하므로 주석 처리
public class MaintenanceServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final int HISTORY_PAGE_SIZE = 20;
    private static final String MAINTENANCE_CUSTOMER_TYPE =
            "정기점검 계약 고객사";
    private final MaintenanceRecordDAO maintenanceDAO;
    private final CustomerDAO customerDAO;
    private final MaintenanceRecordRequestMapper requestMapper =
            new MaintenanceRecordRequestMapper();

    public MaintenanceServlet() {
        this(new MaintenanceRecordDAO(), new CustomerDAO());
    }

    MaintenanceServlet(MaintenanceRecordDAO maintenanceDAO) {
        this(maintenanceDAO, new CustomerDAO());
    }

    MaintenanceServlet(
            MaintenanceRecordDAO maintenanceDAO,
            CustomerDAO customerDAO) {
        this.maintenanceDAO = Objects.requireNonNull(
                maintenanceDAO, "maintenanceDAO");
        this.customerDAO = Objects.requireNonNull(
                customerDAO, "customerDAO");
    }

    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 세션 확인
        HttpSession session = request.getSession(false);
        UserDTO user = SessionPrincipal.expose(request, session);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // 뷰 타입 확인
        String viewType = request.getParameter("view");
        if (viewType == null || viewType.isEmpty()) {
            viewType = "cards";
        }

        if ("cards".equals(viewType)) {
            // 담당자별 고객사 카드 목록 표시 (라이선스 요약 제거)
            Map<String, List<CustomerDTO>> inspectorCustomers =
                    prioritizeInspector(
                            getInspectorCustomersMap(),
                            user.getUserName());
            request.setAttribute("inspectorCustomers", inspectorCustomers);
            request.setAttribute(
                    "maintenanceFrequencyLabels",
                    getMaintenanceFrequencyLabels(customerDAO));
            request.setAttribute("viewType", "cards");
            request.getRequestDispatcher("/maintenance/maintenance_cards.jsp").forward(request, response);

        } else if ("history".equals(viewType)) {
            // 특정 고객사의 정기점검 이력 목록
            String customerName = request.getParameter("customerName");
            if (customerName != null && !customerName.isEmpty()) {
                int historyPage;
                try {
                    historyPage = parseHistoryPage(
                            request.getParameter("historyPage"));
                } catch (IllegalArgumentException exception) {
                    ApplicationError.send(
                            request,
                            response,
                            HttpServletResponse.SC_BAD_REQUEST,
                            "invalid_history_page",
                            "점검 이력 페이지가 올바르지 않습니다.");
                    return;
                }
                MaintenanceHistoryFilter historyFilter;
                try {
                    historyFilter = MaintenanceHistoryFilter.parse(
                            request.getParameter("historyYear"),
                            request.getParameter("historyVersion"),
                            request.getParameter("historyQuery"));
                } catch (IllegalArgumentException exception) {
                    ApplicationError.send(
                            request,
                            response,
                            HttpServletResponse.SC_BAD_REQUEST,
                            "invalid_history_filter",
                            "점검 이력 검색 조건이 올바르지 않습니다.");
                    return;
                }
                PageResult<MaintenanceRecordDTO> page =
                        maintenanceDAO.getMaintenanceRecordsByCustomer(
                                customerName,
                                historyPage,
                                HISTORY_PAGE_SIZE,
                                historyFilter);
                // 고객사 기본 정보 조회
                CustomerDTO customer = customerDAO.getCustomerByName(customerName);
                MaintenanceHistoryViewData.from(
                        page, historyFilter, customer, customerName)
                        .expose(request);
                request.getRequestDispatcher("/maintenance/maintenance_history.jsp").forward(request, response);
            } else {
                response.sendRedirect("maintenance?view=cards");
            }

        } else if ("add".equals(viewType)) {
            // 새 정기점검 이력 추가 폼
            showAddForm(
                    request,
                    response,
                    request.getParameter("customerName"),
                    null,
                    Map.of(),
                    HttpServletResponse.SC_OK);

        } else if ("formContext".equals(viewType)) {
            writeFormContext(request, response);

        } else if ("edit".equals(viewType)) {
            // 정기점검 이력 수정 폼
            String maintenanceIdStr = request.getParameter("id");
            if (maintenanceIdStr != null && !maintenanceIdStr.isEmpty()) {
                try {
                    Long maintenanceId = Long.parseLong(maintenanceIdStr);
                    MaintenanceRecordDTO record = maintenanceDAO.getMaintenanceRecordById(maintenanceId);
                    if (record != null && isOwner(record, user)) {
                        request.setAttribute("record", record);
                        prepareFormView(
                                request,
                                maintenanceFormOptions(
                                        record.getInspectorName()),
                                record,
                                Map.of(),
                                false);
                        request.setAttribute("viewType", "edit");
                        request.getRequestDispatcher("/maintenance/maintenance_edit.jsp").forward(request, response);
                    } else if (record == null) {
                        session.setAttribute("error", "해당 정기점검 이력을 찾을 수 없습니다.");
                        response.sendRedirect("maintenance?view=cards");
                    } else {
                        session.setAttribute("error", "수정 권한이 없습니다.");
                        response.sendRedirect(
                                "maintenance?view=history&customerName="
                                        + java.net.URLEncoder.encode(
                                                record.getCustomerName(), "UTF-8"));
                    }
                } catch (NumberFormatException e) {
                    session.setAttribute("error", "잘못된 요청입니다.");
                    response.sendRedirect("maintenance?view=cards");
                }
            } else {
                response.sendRedirect("maintenance?view=cards");
            }

        } else {
            response.sendRedirect("maintenance?view=cards");
        }
    }

 // 담당자별 고객사 목록을 Map으로 구성 (정기점검 계약 고객사이면서 활성 상태인 것만)
    private Map<String, List<CustomerDTO>> getInspectorCustomersMap() {
        Map<String, List<CustomerDTO>> inspectorCustomers = new LinkedHashMap<>();

        // getAllCustomers already filters inactive customers.
        List<CustomerDTO> allCustomers = customerDAO.getAllCustomers("manager_name", "ASC", "maintenance");
        for (CustomerDTO customer : allCustomers) {
            String mainManager = customer.getManagerName();
            String customerType = customer.getCustomerType();

            if (mainManager != null && !mainManager.trim().isEmpty()
                    && "정기점검 계약 고객사".equals(customerType)) {
                inspectorCustomers.computeIfAbsent(mainManager.trim(), key -> new ArrayList<>()).add(customer);
            }
        }
        return inspectorCustomers;
    }

    static Map<String, List<CustomerDTO>> prioritizeInspector(
            Map<String, List<CustomerDTO>> inspectorCustomers,
            String preferredInspector) {
        Objects.requireNonNull(inspectorCustomers, "inspectorCustomers");
        Map<String, List<CustomerDTO>> ordered = new LinkedHashMap<>();
        String preferred = preferredInspector == null
                ? null
                : preferredInspector.trim();
        if (preferred != null && !preferred.isEmpty()) {
            List<CustomerDTO> preferredCustomers =
                    inspectorCustomers.get(preferred);
            if (preferredCustomers != null) {
                ordered.put(preferred, preferredCustomers);
            }
        }
        inspectorCustomers.forEach(ordered::putIfAbsent);
        return ordered;
    }

    private Map<String, String> getMaintenanceFrequencyLabels(
            CustomerDAO customerDAO) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (MaintenanceCustomerAssignment assignment
                : customerDAO.getAllMaintenanceCustomerAssignments()) {
            labels.put(
                    assignment.customerName(),
                    assignment.schedule().isQuarterly() ? "분기" : "월별");
        }
        return labels;
    }

    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 세션 확인
        HttpSession session = request.getSession(false);
        UserDTO currentUser = SessionPrincipal.from(session);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String actionType = request.getParameter("action");

        if ("add".equals(actionType)) {
            // 새 정기점검 이력 추가
            MaintenanceFormSubmission submission = requestMapper.map(
                    request::getParameter,
                    currentUser.getUserId(),
                    maintenanceFormOptions(null));
            MaintenanceRecordDTO record = submission.record();
            if (!submission.valid()) {
                showAddForm(
                        request,
                        response,
                        record.getCustomerName(),
                        record,
                        submission.fieldErrors(),
                        HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            boolean success = maintenanceDAO.addMaintenanceRecord(record);
            if (success) {
                session.setAttribute("message", "정기점검 이력이 성공적으로 추가되었습니다.");
            } else {
                session.setAttribute("error", "정기점검 이력 추가 중 오류가 발생했습니다.");
            }

            String customerName = record.getCustomerName();
            String encodedName = java.net.URLEncoder.encode(customerName, "UTF-8");
            response.sendRedirect("maintenance?view=history&customerName=" + encodedName);

        } else if ("update".equals(actionType)) {
            // 정기점검 이력 수정
            String maintenanceIdStr = request.getParameter("maintenance_id");
            if (maintenanceIdStr != null && !maintenanceIdStr.isEmpty()) {
                try {
                    Long maintenanceId = Long.parseLong(maintenanceIdStr);
                    MaintenanceRecordDTO existing =
                            maintenanceDAO.getMaintenanceRecordById(
                                    maintenanceId);
                    if (existing == null || !isOwner(existing, currentUser)) {
                        session.setAttribute(
                                "error",
                                "수정 권한이 없거나 이력을 찾을 수 없습니다.");
                        response.sendRedirect("maintenance?view=cards");
                        return;
                    }
                    MaintenanceFormOptions options = maintenanceFormOptions(
                            existing.getInspectorName());
                    MaintenanceFormSubmission submission =
                            requestMapper.mapForUpdate(
                                    request::getParameter,
                                    currentUser.getUserId(),
                                    options,
                                    maintenanceId,
                                    existing.getLicenseSizeGb(),
                                    existing.getVerticaVersion(),
                                    existing.getLicenseUsageSize(),
                                    existing.getLicenseUsagePct());
                    MaintenanceRecordDTO record = submission.record();
                    if (!submission.valid()) {
                        request.setAttribute("record", record);
                        prepareFormView(
                                request,
                                options,
                                record,
                                submission.fieldErrors(),
                                false);
                        response.setStatus(
                                HttpServletResponse.SC_BAD_REQUEST);
                        request.setAttribute("viewType", "edit");
                        request.getRequestDispatcher(
                                "/maintenance/maintenance_edit.jsp")
                                .forward(request, response);
                        return;
                    }

                    boolean success = maintenanceDAO.updateMaintenanceRecordForOwner(
                            record, currentUser.getUserId());
                    if (success) {
                        session.setAttribute("message", "정기점검 이력이 성공적으로 수정되었습니다.");
                    } else {
                        session.setAttribute("error", "정기점검 이력 수정 중 오류가 발생했습니다.");
                    }

                    String customerName = record.getCustomerName();
                    String encodedName = java.net.URLEncoder.encode(customerName, "UTF-8");
                    response.sendRedirect("maintenance?view=history&customerName=" + encodedName);

                } catch (NumberFormatException e) {
                    session.setAttribute("error", "잘못된 요청입니다.");
                    response.sendRedirect("maintenance?view=cards");
                }
            }

        } else if ("delete".equals(actionType)) {
            // 정기점검 이력 삭제
            String maintenanceIdStr = request.getParameter("maintenance_id");
            String customerName = request.getParameter("customer_name");

            if (maintenanceIdStr != null && !maintenanceIdStr.isEmpty()) {
                try {
                    Long maintenanceId = Long.parseLong(maintenanceIdStr);
                    boolean success = maintenanceDAO.deleteMaintenanceRecordForOwner(
                            maintenanceId, currentUser.getUserId());
                    if (success) {
                        session.setAttribute("message", "정기점검 이력이 성공적으로 삭제되었습니다.");
                    } else {
                        session.setAttribute("error", "정기점검 이력 삭제 중 오류가 발생했습니다.");
                    }
                } catch (NumberFormatException e) {
                    session.setAttribute("error", "잘못된 요청입니다.");
                }
            }

            if (customerName != null && !customerName.isEmpty()) {
                String encodedName = java.net.URLEncoder.encode(customerName, "UTF-8");
                response.sendRedirect("maintenance?view=history&customerName=" + encodedName);
            } else {
                response.sendRedirect("maintenance?view=cards");
            }

        } else {
            response.sendRedirect("maintenance?view=cards");
        }
    }

    // 날짜 문자열을 Date 객체로 변환
    private Date parseDate(String dateString) {
        return StrictDateParser.parseSqlDateOrNull(dateString);
    }

    private String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private void showAddForm(
            HttpServletRequest request,
            HttpServletResponse response,
            String customerName,
            MaintenanceRecordDTO submittedRecord,
            Map<String, String> fieldErrors,
            int status) throws ServletException, IOException {
        MaintenanceFormOptions options = maintenanceFormOptions(null);
        CustomerDTO customer = options.customer(customerName);
        MaintenanceRecordDTO formRecord = submittedRecord == null
                ? defaultFormRecord(customer)
                : submittedRecord;
        prepareFormView(
                request,
                options,
                formRecord,
                fieldErrors,
                customer != null);
        request.setAttribute("customerName", formRecord.getCustomerName());
        request.setAttribute("viewType", "add");
        response.setStatus(status);
        request.getRequestDispatcher("/maintenance/maintenance_add.jsp")
                .forward(request, response);
    }

    private MaintenanceFormOptions maintenanceFormOptions(
            String retainedInspector) {
        return MaintenanceFormOptions.from(
                customerDAO.getAllCustomers("", "ASC", "maintenance"),
                retainedInspector);
    }

    private MaintenanceRecordDTO defaultFormRecord(CustomerDTO customer) {
        MaintenanceRecordDTO record = new MaintenanceRecordDTO();
        record.setInspectionDate(Date.valueOf(LocalDate.now()));
        if (customer != null) {
            record.setCustomerName(customer.getCustomerName());
            record.setInspectorName(firstNonBlank(
                    customer.getManagerName(),
                    customer.getSubManagerName()));
            record.setVerticaVersion(
                    trimToNull(customer.getVerticaVersion()));
            record.setLicenseSizeGb(
                    trimToNull(customer.getLicenseSize()));
        }
        return record;
    }

    private void prepareFormView(
            HttpServletRequest request,
            MaintenanceFormOptions options,
            MaintenanceRecordDTO record,
            Map<String, String> fieldErrors,
            boolean customerFixed) {
        MaintenanceFormHistoryContext history =
                maintenanceDAO.getMaintenanceFormHistoryContext(
                        record.getCustomerName(),
                        record.getInspectionDate(),
                        record.getMaintenanceId());
        request.setAttribute("formRecord", record);
        request.setAttribute("formOptions", options);
        request.setAttribute("fieldErrors", fieldErrors);
        request.setAttribute("formCustomerFixed", customerFixed);
        request.setAttribute(
                "previousMaintenanceRecord", history.previousRecord());
        request.setAttribute(
                "duplicateMaintenanceRecord", history.duplicateRecord());
        request.setAttribute(
                "licenseSizeInput",
                MaintenanceRecordRequestMapper.normalizeTerabytesForInput(
                        record.getLicenseSizeGb()));
        request.setAttribute(
                "licenseUsageInput",
                MaintenanceRecordRequestMapper.normalizeTerabytesForInput(
                        record.getLicenseUsageSize()));
        request.setAttribute(
                "licensePercentageInput",
                MaintenanceRecordRequestMapper.normalizePercentageForInput(
                        record.getLicenseUsagePct()));
    }

    private void writeFormContext(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String customerName = trimToNull(
                request.getParameter("customerName"));
        Date inspectionDate = parseDate(
                request.getParameter("inspectionDate"));
        Long excludedId = parseOptionalLong(
                request.getParameter("excludeId"));
        CustomerDTO customer = customerName == null
                ? null
                : customerDAO.getCustomerByName(customerName);
        if (!isActiveMaintenanceCustomer(customer)
                || inspectionDate == null) {
            JsonResponse.sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_maintenance_context",
                    "고객사와 점검일을 확인해 주세요.");
            return;
        }
        MaintenanceFormHistoryContext context =
                maintenanceDAO.getMaintenanceFormHistoryContext(
                        customerName, inspectionDate, excludedId);
        JsonResponse.write(
                response,
                HttpServletResponse.SC_OK,
                MaintenanceFormContextJson.encode(customer, context));
    }

    private static boolean isActiveMaintenanceCustomer(
            CustomerDTO customer) {
        return customer != null
                && MAINTENANCE_CUSTOMER_TYPE.equals(
                        customer.getCustomerType());
    }

    private static Long parseOptionalLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null || second.isBlank() ? null : second.trim();
    }

    private static boolean isOwner(
            MaintenanceRecordDTO record, UserDTO user) {
        return record != null
                && user != null
                && Objects.equals(
                        record.getCreatorUserId(), user.getUserId());
    }

    private static int parseHistoryPage(String value) {
        if (value == null || value.isBlank()) {
            return 1;
        }
        try {
            long page = Long.parseLong(value.trim());
            if (page < 1 || page > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "History page must be a positive integer");
            }
            return (int) page;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "History page must be a positive integer", exception);
        }
    }

}
