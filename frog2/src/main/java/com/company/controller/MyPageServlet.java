package com.company.controller;

import com.company.util.StrictDateParser;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.company.model.MonthlyCustomerResponseDAO;
import com.company.model.MonthlyCustomerResponseDTO;
import com.company.model.UserDAO;
import com.company.model.UserDTO;
import com.company.security.PasswordPolicy;
import com.company.security.SessionPrincipal;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class MyPageServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final int RECENT_ACTIVITY_LIMIT = 5;
    private static final String HOSTS_SECTION = "hosts";

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
        String action = request.getParameter("action");
        
        if (action == null) {
            action = "view";
        }

        switch (action) {
            case "view":
                renderMainPage(request, response, currentUser);
                break;
            case "editProfile":
                showEditProfile(request, response, currentUser);
                break;
            case "changePassword":
                showChangePassword(request, response);
                break;
            case "monthlyResponse":
                showMonthlyResponse(request, response, currentUser);
                break;
            default:
                renderMainPage(request, response, currentUser);
                break;
        }
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

        if ("updateProfile".equals(formAction)) {
            updateProfile(request, response, currentUser, session);
        } else if ("updatePassword".equals(formAction)) {
            updatePassword(request, response, currentUser);
        } else if ("addResponse".equals(formAction)) {
            addMonthlyResponse(request, response, currentUser);
        } else if ("updateResponse".equals(formAction)) {
            updateMonthlyResponse(request, response, currentUser);
        } else if ("deleteResponse".equals(formAction)) {
            deleteMonthlyResponse(request, response, currentUser);
        } else {
            renderMainPage(request, response, currentUser);
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
    private void showEditProfile(HttpServletRequest request, HttpServletResponse response, UserDTO user) 
            throws ServletException, IOException {
        
        UserDAO userDAO = new UserDAO();
        UserDTO userInfo = userDAO.getUserById(user.getUserId());
        
        request.setAttribute("userInfo", userInfo);
        request.getRequestDispatcher("/mypage/edit_profile.jsp").forward(request, response);
    }

    // 비밀번호 변경 화면
    private void showChangePassword(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        request.getRequestDispatcher("/mypage/change_password.jsp").forward(request, response);
    }

    // 프로필 업데이트 처리
    private void updateProfile(HttpServletRequest request, HttpServletResponse response, 
                               UserDTO currentUser, HttpSession session) 
            throws ServletException, IOException {
        
        String userName = request.getParameter("userName");
        
        UserDAO userDAO = new UserDAO();
        boolean success = userDAO.updateUserName(currentUser.getUserId(), userName);
        
        if (success) {
            // 세션 업데이트
            currentUser.setUserName(userName);
            SessionPrincipal.store(session, currentUser);
            
            request.setAttribute("message", "프로필이 성공적으로 업데이트되었습니다.");
            request.setAttribute("messageType", "success");
        } else {
            request.setAttribute("message", "프로필 업데이트에 실패했습니다.");
            request.setAttribute("messageType", "error");
        }
        
        renderMainPage(request, response, currentUser);
    }

    // 비밀번호 변경 처리
    private void updatePassword(HttpServletRequest request, HttpServletResponse response, UserDTO currentUser) 
            throws ServletException, IOException {
        
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");
        
        Optional<String> validationError =
                PasswordPolicy.validate(currentPassword, newPassword, confirmPassword);
        if (validationError.isPresent()) {
            request.setAttribute("message", validationError.get());
            request.setAttribute("messageType", "error");
            showChangePassword(request, response);
            return;
        }
        
        UserDAO userDAO = new UserDAO();
        
        // 현재 비밀번호 확인
        UserDTO authUser = userDAO.authenticateUser(currentUser.getUserId(), currentPassword);
        if (authUser == null) {
            request.setAttribute("message", "현재 비밀번호가 올바르지 않습니다.");
            request.setAttribute("messageType", "error");
            showChangePassword(request, response);
            return;
        }
        
        // 비밀번호 변경
        boolean success = userDAO.updatePassword(currentUser.getUserId(), newPassword);
        
        if (success) {
            request.setAttribute("message", "비밀번호가 성공적으로 변경되었습니다.");
            request.setAttribute("messageType", "success");
        } else {
            request.setAttribute("message", "비밀번호 변경에 실패했습니다.");
            request.setAttribute("messageType", "error");
        }
        
        renderMainPage(request, response, currentUser);
    }
    
    // 월별 고객 응대 화면
    private void showMonthlyResponse(HttpServletRequest request, HttpServletResponse response, UserDTO user) 
            throws ServletException, IOException {
        
        String yearStr = request.getParameter("year");
        String monthStr = request.getParameter("month");
        
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH) + 1;
        int year = currentYear;
        int month = currentMonth;
        
        if (yearStr != null && monthStr != null) {
            try {
                year = Integer.parseInt(yearStr);
                month = Integer.parseInt(monthStr);
            } catch (NumberFormatException e) {
                // 기본값 사용
            }
        }
        
        MonthlyCustomerResponseDAO dao = new MonthlyCustomerResponseDAO();
        List<MonthlyCustomerResponseDTO> monthlyResponses = 
            dao.getMonthlyResponses(
                    user.getUserId(), year, month);
        FlashMessage.expose(request);
        
        request.setAttribute("currentYear", currentYear);
        request.setAttribute("currentMonth", currentMonth);
        request.setAttribute("selectedYear", year);
        request.setAttribute("selectedMonth", month);
        request.setAttribute("monthlyResponses", monthlyResponses);
        request.setAttribute("hasData", yearStr != null && monthStr != null);
        
        request.getRequestDispatcher("/mypage/monthly_customer_response.jsp").forward(request, response);
    }
    
    // 월별 고객 응대 추가
    private void addMonthlyResponse(HttpServletRequest request, HttpServletResponse response, UserDTO user) 
            throws ServletException, IOException {
        
        try {
            String responseDateStr = request.getParameter("responseDate");
            String customerName = request.getParameter("customerName");
            String reason = request.getParameter("reason");
            String actionContent = request.getParameter("actionContent");
            String note = request.getParameter("note");
            
            Date responseDate = StrictDateParser.parseDate(responseDateStr);
            
            MonthlyCustomerResponseDTO dto = new MonthlyCustomerResponseDTO();
            dto.setUserId(user.getUserId());
            dto.setUserName(user.getUserName());
            dto.setResponseDate(responseDate);
            dto.setCustomerName(customerName);
            dto.setReason(reason);
            dto.setActionContent(actionContent);
            dto.setNote(note);
            
            MonthlyCustomerResponseDAO dao = new MonthlyCustomerResponseDAO();
            boolean success = dao.addResponse(dto);
            
            FlashMessage.store(
                    request,
                    success ? "고객 응대 기록이 추가되었습니다." : "고객 응대 기록 추가에 실패했습니다.",
                    success ? "success" : "error");
            
        } catch (ParseException e) {
            FlashMessage.store(request, "날짜 형식이 올바르지 않습니다.", "error");
        }
        
        // 선택된 년월로 다시 조회
        String year = request.getParameter("year");
        String month = request.getParameter("month");
        response.sendRedirect(request.getContextPath() + "/mypage?action=monthlyResponse&year=" + year + "&month=" + month);
    }
    
    // 월별 고객 응대 수정
    private void updateMonthlyResponse(HttpServletRequest request, HttpServletResponse response, UserDTO user) 
            throws ServletException, IOException {
        
        try {
            int id = Integer.parseInt(request.getParameter("responseId"));
            String responseDateStr = request.getParameter("responseDate");
            String customerName = request.getParameter("customerName");
            String reason = request.getParameter("reason");
            String actionContent = request.getParameter("actionContent");
            String note = request.getParameter("note");
            
            Date responseDate = StrictDateParser.parseDate(responseDateStr);
            
            MonthlyCustomerResponseDTO dto = new MonthlyCustomerResponseDTO();
            dto.setId(id);
            dto.setUserId(user.getUserId());
            dto.setUserName(user.getUserName());
            dto.setResponseDate(responseDate);
            dto.setCustomerName(customerName);
            dto.setReason(reason);
            dto.setActionContent(actionContent);
            dto.setNote(note);
            
            MonthlyCustomerResponseDAO dao = new MonthlyCustomerResponseDAO();
            boolean success = dao.updateResponse(dto);
            
            FlashMessage.store(
                    request,
                    success ? "고객 응대 기록이 수정되었습니다." : "고객 응대 기록 수정에 실패했습니다.",
                    success ? "success" : "error");
            
        } catch (ParseException | NumberFormatException e) {
            FlashMessage.store(request, "입력값이 올바르지 않습니다.", "error");
        }
        
        // 선택된 년월로 다시 조회
        String year = request.getParameter("year");
        String month = request.getParameter("month");
        response.sendRedirect(request.getContextPath() + "/mypage?action=monthlyResponse&year=" + year + "&month=" + month);
    }
    
    // 월별 고객 응대 삭제
    private void deleteMonthlyResponse(HttpServletRequest request, HttpServletResponse response, UserDTO user) 
            throws ServletException, IOException {
        
        try {
            int id = Integer.parseInt(request.getParameter("responseId"));
            
            MonthlyCustomerResponseDAO dao = new MonthlyCustomerResponseDAO();
            boolean success = dao.deleteResponse(id, user.getUserId());
            
            FlashMessage.store(
                    request,
                    success ? "고객 응대 기록이 삭제되었습니다." : "고객 응대 기록 삭제에 실패했습니다.",
                    success ? "success" : "error");
            
        } catch (NumberFormatException e) {
            FlashMessage.store(request, "잘못된 요청입니다.", "error");
        }
        
        // 선택된 년월로 다시 조회
        String year = request.getParameter("year");
        String month = request.getParameter("month");
        response.sendRedirect(request.getContextPath() + "/mypage?action=monthlyResponse&year=" + year + "&month=" + month);
    }
}
