package com.company.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.company.model.UserDTO;
import com.company.model.UserVmHostDAO;
import com.company.model.UserVmHostDTO;
import com.company.security.SessionPrincipal;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class UserVmHostServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final UserVmHostDAO userVmHostDAO = new UserVmHostDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        UserDTO user = SessionPrincipal.from(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        UserVmHostDTO editHost = null;
        String editIp = trim(request.getParameter("editIp"));
        if (editIp != null) {
            editHost = userVmHostDAO.getHostByIpAndOwner(editIp, user.getUserId());
        }

        renderPage(request, response, user, editHost);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        UserDTO user = SessionPrincipal.from(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String returnTo = trim(request.getParameter("returnTo"));
        boolean dashboardRequest = "dashboard".equals(returnTo);
        String action = trim(request.getParameter("action"));
        if ("delete".equals(action)) {
            userVmHostDAO.deleteByIpAndOwner(trim(request.getParameter("ip")), user.getUserId());
            if (dashboardRequest) {
                response.sendRedirect(buildDashboardRedirect(request, "deleted"));
            } else {
                response.sendRedirect(request.getContextPath() + "/vm-hosts?result=deleted");
            }
            return;
        }

        UserVmHostDTO dto = new UserVmHostDTO();
        dto.setIp(trim(request.getParameter("ip")));
        dto.setOwnerUserId(user.getUserId());
        dto.setOwnerUserName(user.getUserName());
        dto.setPurpose(trim(request.getParameter("purpose")));
        dto.setOsInfo(trim(request.getParameter("osInfo")));
        dto.setVerticaVersion(trim(request.getParameter("verticaVersion")));
        dto.setRemoteHost(trim(request.getParameter("remoteHost")));
        dto.setNote(trim(request.getParameter("note")));

        String originalIp = trim(request.getParameter("originalIp"));
        String errorMessage = userVmHostDAO.save(dto, originalIp);
        if (errorMessage != null) {
            if (dashboardRequest) {
                renderDashboard(request, response, user, dto, originalIp, errorMessage);
                return;
            }
            request.setAttribute("errorMessage", errorMessage);
            renderPage(request, response, user, dto);
            return;
        }

        if (dashboardRequest) {
            response.sendRedirect(buildDashboardRedirect(request, "saved"));
        } else {
            response.sendRedirect(request.getContextPath() + "/vm-hosts?result=saved");
        }
    }

    private void renderPage(HttpServletRequest request, HttpServletResponse response, UserDTO user, UserVmHostDTO editHost)
            throws ServletException, IOException {
        List<UserVmHostDTO> vmHosts = userVmHostDAO.getActiveHostsByOwner(user.getUserId());
        int vmHostLimit = userVmHostDAO.getMaxHostsPerUser();
        int vmHostCount = vmHosts.size();

        request.setAttribute("user", user);
        request.setAttribute("vmHosts", vmHosts);
        request.setAttribute("vmHostCount", vmHostCount);
        request.setAttribute("vmHostLimit", vmHostLimit);
        request.setAttribute("vmHostRemaining", Math.max(0, vmHostLimit - vmHostCount));
        request.setAttribute("editHost", editHost);
        request.getRequestDispatcher("/vm_hosts/list.jsp").forward(request, response);
    }

    private void renderDashboard(HttpServletRequest request, HttpServletResponse response, UserDTO user,
            UserVmHostDTO formData, String originalIp, String errorMessage)
            throws ServletException, IOException {
        List<UserVmHostDTO> vmHosts = userVmHostDAO.getActiveHostsByOwner(user.getUserId());
        int vmHostLimit = userVmHostDAO.getMaxHostsPerUser();
        int vmHostCount = vmHosts.size();

        request.setAttribute("user", user);
        request.setAttribute("vmHosts", vmHosts);
        request.setAttribute("vmHostCount", vmHostCount);
        request.setAttribute("vmHostLimit", vmHostLimit);
        request.setAttribute("vmHostRemaining", Math.max(0, vmHostLimit - vmHostCount));
        request.setAttribute("vmHostForm", formData);
        request.setAttribute("vmHostOriginalIp", originalIp);
        request.setAttribute("vmHostErrorMessage", errorMessage);
        request.setAttribute("dashboardMenus", DashboardMenuProvider.build());
        request.getRequestDispatcher("/dashboard.jsp").forward(request, response);
    }


    private String buildDashboardRedirect(HttpServletRequest request, String result) {
        return request.getContextPath() + "/dashboard?vmHostResult="
                + URLEncoder.encode(result, StandardCharsets.UTF_8);
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
