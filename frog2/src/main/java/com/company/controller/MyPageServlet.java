package com.company.controller;

import com.company.model.MonthlyCustomerResponseDAO;
import com.company.model.MonthlyCustomerResponseDTO;
import com.company.model.UserDAO;
import com.company.model.UserDTO;
import com.company.security.SessionPrincipal;
import com.company.util.BusinessDate;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

public class MyPageServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final int RECENT_ACTIVITY_LIMIT = 5;
    private static final String HOSTS_SECTION = "hosts";
    private final UserDAO userDAO;
    private final MonthlyCustomerResponseDAO monthlyResponseDAO;
    private final MyPageRequestMapper requestMapper;
    private final MyPageCommandService commandService;

    public MyPageServlet() {
        this(
                new UserDAO(),
                new MonthlyCustomerResponseDAO(),
                new MyPageRequestMapper());
    }

    MyPageServlet(UserDAO userDAO) {
        this(
                userDAO,
                new MonthlyCustomerResponseDAO(),
                new MyPageRequestMapper());
    }

    MyPageServlet(
            UserDAO userDAO,
            MonthlyCustomerResponseDAO monthlyResponseDAO,
            MyPageRequestMapper requestMapper) {
        this.userDAO = Objects.requireNonNull(userDAO, "userDAO");
        this.monthlyResponseDAO = Objects.requireNonNull(
                monthlyResponseDAO, "monthlyResponseDAO");
        this.requestMapper = Objects.requireNonNull(
                requestMapper, "requestMapper");
        this.commandService = new MyPageCommandService(
                userDAO, monthlyResponseDAO);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // 세션 확인
        HttpSession session = request.getSession(false);
        UserDTO currentUser = SessionPrincipal.from(session);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        FlashMessage.expose(request);
        dispatchGet(
                request.getParameter("action"),
                request,
                response,
                currentUser);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        UserDTO currentUser = SessionPrincipal.from(session);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        String formAction = request.getParameter("formAction");
        if (formAction == null || formAction.isBlank()) {
            formAction = request.getParameter("action");
        }

        dispatchPost(
                formAction,
                request,
                response,
                currentUser,
                session);
    }

    private void dispatchGet(
            String action,
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO currentUser) throws ServletException, IOException {
        switch (action == null ? "view" : action) {
            case "editProfile" ->
                    handleShowEditProfile(request, response, currentUser);
            case "changePassword" ->
                    handleShowChangePassword(request, response);
            case "monthlyResponse" ->
                    handleShowMonthlyResponse(request, response, currentUser);
            default -> renderMainPage(request, response, currentUser);
        }
    }

    private void dispatchPost(
            String action,
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO currentUser,
            HttpSession session) throws IOException {
        if (action == null) {
            response.sendRedirect(request.getContextPath() + "/mypage");
            return;
        }
        switch (action) {
            case "updateProfile" ->
                    handleUpdateProfile(
                            request, response, currentUser, session);
            case "updatePassword" ->
                    handleUpdatePassword(request, response, currentUser);
            case "addResponse" ->
                    handleAddMonthlyResponse(request, response, currentUser);
            case "updateResponse" ->
                    handleUpdateMonthlyResponse(
                            request, response, currentUser);
            case "deleteResponse" ->
                    handleDeleteMonthlyResponse(
                            request, response, currentUser);
            default -> response.sendRedirect(
                    request.getContextPath() + "/mypage");
        }
    }

    // 마이페이지 메인 화면
    static void renderMainPage(HttpServletRequest request, HttpServletResponse response, UserDTO user)
            throws ServletException, IOException {
        renderMainPage(
                request,
                response,
                user,
                new MyPageQueryService());
    }

    static void renderMainPage(
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO user,
            MyPageQueryService queryService)
            throws ServletException, IOException {
        String section = resolveSection(request);
        MyPageQueryService.ViewData viewData = HOSTS_SECTION.equals(section)
                ? queryService.loadHosts(user.getUserId())
                : queryService.loadOverview(
                        user.getUserId(), RECENT_ACTIVITY_LIMIT);

        request.setAttribute("userInfo", viewData.user());
        request.setAttribute("myPageSection", section);
        request.setAttribute("vmHostLimit", viewData.vmHostLimit());
        exposeHostSummary(
                request,
                viewData.vmHostLimit(),
                viewData.vmHostCount());

        if (HOSTS_SECTION.equals(section)) {
            request.setAttribute("vmHosts", viewData.vmHosts());
        } else {
            request.setAttribute(
                    "recentMaintenanceRecords",
                    viewData.recentMaintenanceRecords());
            request.setAttribute(
                    "recentTroubleshootings",
                    viewData.recentTroubleshootings());
            request.setAttribute(
                    "maintenanceCount", viewData.maintenanceCount());
            request.setAttribute(
                    "troubleshootingCount",
                    viewData.troubleshootingCount());
        }

        request.getRequestDispatcher("/mypage/mypage.jsp").forward(request, response);
    }

    private static String resolveSection(HttpServletRequest request) {
        Object forwardedSection = request.getAttribute("myPageSection");
        String section = forwardedSection instanceof String
                ? (String) forwardedSection
                : request.getParameter("section");
        return HOSTS_SECTION.equals(section) ? HOSTS_SECTION : "overview";
    }

    private static void exposeHostSummary(
            HttpServletRequest request, int vmHostLimit, int vmHostCount) {
        request.setAttribute("vmHostCount", vmHostCount);
        request.setAttribute(
                "vmHostRemaining", Math.max(0, vmHostLimit - vmHostCount));
    }

    // 프로필 수정 화면
    private void handleShowEditProfile(
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO user)
            throws ServletException, IOException {
        
        UserDTO userInfo = userDAO.getUserById(user.getUserId());
        
        request.setAttribute("userInfo", userInfo);
        request.getRequestDispatcher("/mypage/edit_profile.jsp")
                .forward(request, response);
    }

    // 비밀번호 변경 화면
    private void handleShowChangePassword(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {
        
        request.getRequestDispatcher("/mypage/change_password.jsp")
                .forward(request, response);
    }

    // 프로필 업데이트 처리
    private void handleUpdateProfile(
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO currentUser,
            HttpSession session) throws IOException {
        final String userName;
        try {
            userName = requestMapper.profileName(request);
        } catch (IllegalArgumentException exception) {
            redirectToProfileForm(
                    request, response, exception.getMessage());
            return;
        }

        boolean success = commandService.updateProfile(
                currentUser.getUserId(), userName);
        if (success) {
            currentUser.setUserName(userName);
            SessionPrincipal.store(session, currentUser);
            redirectToMyPage(
                    request,
                    response,
                    "프로필이 성공적으로 업데이트되었습니다.",
                    "success");
        } else {
            redirectToProfileForm(
                    request, response, "프로필 업데이트에 실패했습니다.");
        }
    }

    // 비밀번호 변경 처리
    private void handleUpdatePassword(
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO currentUser) throws IOException {
        MyPageCommandService.PasswordChangeResult result =
                commandService.updatePassword(
                        currentUser.getUserId(),
                        requestMapper.passwordChange(request));
        if (result.status()
                == MyPageCommandService.PasswordChangeStatus.SUCCESS) {
            redirectToMyPage(
                    request,
                    response,
                    "비밀번호가 성공적으로 변경되었습니다.",
                    "success");
        } else {
            redirectToPasswordForm(
                    request, response, result.errorMessage());
        }
    }

    private static void redirectToProfileForm(
            HttpServletRequest request,
            HttpServletResponse response,
            String message) throws IOException {
        FlashMessage.redirect(
                request,
                response,
                request.getContextPath() + "/mypage?action=editProfile",
                message,
                "error");
    }

    private static void redirectToPasswordForm(
            HttpServletRequest request,
            HttpServletResponse response,
            String message) throws IOException {
        FlashMessage.redirect(
                request,
                response,
                request.getContextPath() + "/mypage?action=changePassword",
                message,
                "error");
    }

    private static void redirectToMyPage(
            HttpServletRequest request,
            HttpServletResponse response,
            String message,
            String messageType) throws IOException {
        FlashMessage.redirect(
                request,
                response,
                request.getContextPath() + "/mypage",
                message,
                messageType);
    }
    
    // 월별 고객 응대 화면
    private void handleShowMonthlyResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO user)
            throws ServletException, IOException {
        
        YearMonth currentBusinessMonth = BusinessDate.currentMonth(
                BusinessDate.systemClock());
        int currentYear = currentBusinessMonth.getYear();
        int currentMonth = currentBusinessMonth.getMonthValue();
        MyPageRequestMapper.MonthSelection selection =
                requestMapper.monthSelection(request, currentBusinessMonth);
        List<MonthlyCustomerResponseDTO> monthlyResponses = 
            monthlyResponseDAO.getMonthlyResponses(
                    user.getUserId(), selection.year(), selection.month());
        request.setAttribute("currentYear", currentYear);
        request.setAttribute("currentMonth", currentMonth);
        request.setAttribute("selectedYear", selection.year());
        request.setAttribute("selectedMonth", selection.month());
        request.setAttribute("monthlyResponses", monthlyResponses);
        request.setAttribute("hasData", selection.explicitlySelected());
        
        request.getRequestDispatcher("/mypage/monthly_customer_response.jsp")
                .forward(request, response);
    }
    
    // 월별 고객 응대 추가
    private void handleAddMonthlyResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO user)
            throws IOException {
        String message;
        String messageType;
        try {
            MonthlyCustomerResponseDTO dto = requestMapper.monthlyResponse(
                    request, user, false);
            boolean success = commandService.addMonthlyResponse(dto);
            message = success
                    ? "고객 응대 기록이 추가되었습니다."
                    : "고객 응대 기록 추가에 실패했습니다.";
            messageType = success ? "success" : "error";
        } catch (IllegalArgumentException exception) {
            message = exception.getMessage();
            messageType = "error";
        }
        redirectToMonthlyResponses(request, response, message, messageType);
    }
    
    // 월별 고객 응대 수정
    private void handleUpdateMonthlyResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO user)
            throws IOException {
        String message;
        String messageType;
        try {
            MonthlyCustomerResponseDTO dto = requestMapper.monthlyResponse(
                    request, user, true);
            boolean success = commandService.updateMonthlyResponse(dto);
            message = success
                    ? "고객 응대 기록이 수정되었습니다."
                    : "고객 응대 기록 수정에 실패했습니다.";
            messageType = success ? "success" : "error";
        } catch (IllegalArgumentException exception) {
            message = exception.getMessage();
            messageType = "error";
        }
        redirectToMonthlyResponses(request, response, message, messageType);
    }
    
    // 월별 고객 응대 삭제
    private void handleDeleteMonthlyResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO user)
            throws IOException {
        String message;
        String messageType;
        try {
            int id = requestMapper.responseIdForDelete(request);
            boolean success = commandService.deleteMonthlyResponse(
                    id, user.getUserId());
            message = success
                    ? "고객 응대 기록이 삭제되었습니다."
                    : "고객 응대 기록 삭제에 실패했습니다.";
            messageType = success ? "success" : "error";
        } catch (IllegalArgumentException exception) {
            message = exception.getMessage();
            messageType = "error";
        }
        redirectToMonthlyResponses(request, response, message, messageType);
    }

    private static void redirectToMonthlyResponses(
            HttpServletRequest request,
            HttpServletResponse response,
            String message,
            String messageType) throws IOException {
        String year = request.getParameter("year");
        String month = request.getParameter("month");
        FlashMessage.redirect(
                request,
                response,
                request.getContextPath() + "/mypage?action=monthlyResponse&year="
                        + year + "&month=" + month,
                message,
                messageType);
    }
}
