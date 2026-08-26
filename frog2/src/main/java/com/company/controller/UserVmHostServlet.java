package com.company.controller;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

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

    private final UserVmHostDAO userVmHostDAO;
    private final UserVmHostService userVmHostService;

    public UserVmHostServlet() {
        this(new UserVmHostDAO());
    }

    UserVmHostServlet(UserVmHostDAO userVmHostDAO) {
        this.userVmHostDAO = Objects.requireNonNull(
                userVmHostDAO, "userVmHostDAO");
        this.userVmHostService = new UserVmHostService(userVmHostDAO);
    }

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
        boolean myPageRequest = "mypage".equals(returnTo);
        String action = trim(request.getParameter("action"));
        if (!"save".equals(action) && !"delete".equals(action)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if ("delete".equals(action)) {
            userVmHostDAO.deleteByIpAndOwner(trim(request.getParameter("ip")), user.getUserId());
            if (myPageRequest) {
                response.sendRedirect(buildMyPageRedirect(request, "deleted"));
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
        UserVmHostService.SaveResult result =
                userVmHostService.save(dto, originalIp);
        if (result != UserVmHostService.SaveResult.SAVED) {
            String errorMessage = saveErrorMessage(result);
            if (myPageRequest) {
                renderMyPage(request, response, user, dto, originalIp, errorMessage);
                return;
            }
            request.setAttribute("errorMessage", errorMessage);
            renderPage(request, response, user, dto);
            return;
        }

        if (myPageRequest) {
            response.sendRedirect(buildMyPageRedirect(request, "saved"));
        } else {
            response.sendRedirect(request.getContextPath() + "/vm-hosts?result=saved");
        }
    }

    private String saveErrorMessage(UserVmHostService.SaveResult result) {
        return switch (result) {
            case USER_REQUIRED -> "사용자 정보가 없습니다. 다시 로그인해 주세요.";
            case INVALID_IP -> "IP는 192.168.40.1 ~ 192.168.40.254 범위만 등록할 수 있습니다.";
            case PURPOSE_REQUIRED -> "사용 목적은 필수입니다.";
            case HOST_NOT_FOUND -> "수정 대상 호스트를 찾을 수 없습니다.";
            case DUPLICATE_OWN_IP -> "이미 등록한 IP입니다.";
            case DUPLICATE_OTHER_IP -> "이미 다른 사용자가 등록한 IP입니다.";
            case HOST_LIMIT_REACHED -> "사용자당 VM 호스트는 최대 20개까지만 등록할 수 있습니다.";
            case WRITE_FAILED -> "호스트 정보를 저장하지 못했습니다. 다시 시도해 주세요.";
            case SAVED -> throw new IllegalArgumentException(
                    "SAVED does not have an error message");
        };
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

    private void renderMyPage(HttpServletRequest request, HttpServletResponse response, UserDTO user,
            UserVmHostDTO formData, String originalIp, String errorMessage)
            throws ServletException, IOException {
        request.setAttribute("myPageSection", "hosts");
        request.setAttribute("vmHostForm", formData);
        request.setAttribute("vmHostOriginalIp", originalIp);
        request.setAttribute("vmHostErrorMessage", errorMessage);
        MyPageServlet.renderMainPage(request, response, user);
    }

    private String buildMyPageRedirect(HttpServletRequest request, String result) {
        return request.getContextPath()
                + "/mypage?section=hosts&vmHostResult=" + result;
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
