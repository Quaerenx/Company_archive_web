<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.io.*" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.*" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%
    // 세션 확인
    HttpSession userSession = request.getSession(false);
    if (userSession == null || userSession.getAttribute("user") == null) {
        response.sendRedirect("login");
        return;
    }
%>

<c:set var="pageTitle" value="업무자료" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers" scope="request" />

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${pageTitle}</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/main_style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/utilities.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/customers.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/dashboard_box.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/download.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
</head>
<body class="page-1050 page-customers">

<%
    String baseDir = "/files";
    String relativePath = request.getParameter("path");
    if (relativePath == null) relativePath = "";

    // 보안 검증
    if (relativePath.contains("..") || relativePath.contains("\\")) {
        out.println("<h3>잘못된 경로입니다.</h3>");
        return;
    }
    
    String realPath = application.getRealPath(baseDir + "/" + relativePath);
    if (realPath == null) {
        out.println("<h3>서버 설정 문제로 실제 경로를 확인할 수 없습니다.</h3>");
        return;
    }
    File currentDir = new File(realPath);
    if (!currentDir.exists()) {
        currentDir.mkdirs();
    }
    if (!currentDir.isDirectory()) {
        out.println("<h3>잘못된 경로입니다.</h3>");
        return;
    }

    File[] files = currentDir.listFiles();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    // 정렬: 폴더 먼저, 그 다음 파일
    if (files != null) {
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });
    }
%>

<% 
    // 페이지 타이틀 설정
    pageContext.setAttribute("pageTitle", "업무자료 파일서버 - " + (relativePath.isEmpty() ? "/" : ("/" + relativePath)));
%>

<!-- Header Include -->
<%@ include file="/WEB-INF/includes/header_nav.jspf" %>

<c:set var="currentPathText" value="/" />
<c:if test="${not empty param.path}">
    <c:set var="currentPathText" value="/${param.path}" />
</c:if>

<div class="customer-management">
    <t:pageHeader>
        <jsp:attribute name="title">
            <i class="fas fa-file-alt"></i> 업무자료
        </jsp:attribute>
        <jsp:attribute name="subtitle">
            현재 위치: ${currentPathText}
        </jsp:attribute>
        <jsp:attribute name="actions">
            <a href="filerepo_upload.jsp?path=${param.path}" class="add-button">
                <i class="fas fa-upload"></i> 업로드
            </a>
        </jsp:attribute>
    </t:pageHeader>

    <div class="table-container">
        <div class="table-wrapper">
        <div class="file-main">
            <!-- 업로드 섹션 -->
            <div class="upload-section">
                <div class="upload-info">
                    <h5>📤 파일 업로드</h5>
                    <small>이 폴더에 새 파일을 업로드할 수 있습니다</small>
                </div>
                <a href="filerepo_upload.jsp?path=<%= relativePath %>" class="upload-btn">
                    <span>📁</span>
                    파일 업로드하기
                </a>
            </div>

            <div class="breadcrumb">
                <strong>📍 현재 위치:</strong> 
                <a href="filerepo_downlist.jsp">/</a><%
                if (!relativePath.isEmpty()) {
                    String[] parts = relativePath.split("/");
                    String currentPath = "";
                    for (int i = 0; i < parts.length; i++) {
                        if (!parts[i].isEmpty()) {
                            currentPath += parts[i];
                            out.print("<a href=\"filerepo_downlist.jsp?path=" + currentPath + "\">" + parts[i] + "</a>");
                            if (i < parts.length - 1) out.print("/");
                            currentPath += "/";
                        }
                    }
                }
                %>
            </div>

            <table class="file-table">
                <thead>
                    <tr>
                        <th width="20"></th>
                        <th><a href="?path=<%= relativePath %>&sort=name">📄 이름</a></th>
                        <th width="150"><a href="?path=<%= relativePath %>&sort=date">📅 수정일</a></th>
                        <th width="80"><a href="?path=<%= relativePath %>&sort=size">💾 크기</a></th>
                        <th width="200">📝 설명</th>
                    </tr>
                </thead>
                <tbody>
                    <%-- 상위 디렉토리 링크 --%>
                    <% if (!relativePath.isEmpty()) {
                        String[] parts = relativePath.split("/");
                        String parentPath = "";
                        for (int i = 0; i < parts.length - 1; i++) {
                            if (!parts[i].isEmpty()) {
                                parentPath += parts[i] + "/";
                            }
                        }
                        if (parentPath.endsWith("/")) {
                            parentPath = parentPath.substring(0, parentPath.length() - 1);
                        }
                    %>
                    <tr class="parent-dir">
                        <td><span class="icon">⬆️</span></td>
                        <td class="file-name">
                            <a href="filerepo_downlist.jsp?path=<%= parentPath %>"><strong>상위 디렉토리</strong></a>
                        </td>
                        <td class="date">-</td>
                        <td class="size">-</td>
                        <td>상위 폴더로 이동</td>
                    </tr>
                    <% } %>

                    <%-- 파일 및 폴더 목록 --%>
                    <%
                        int fileCount = 0;
                        int dirCount = 0;
                        long totalSize = 0;
                        
                        if (files != null && files.length > 0) {
                            for (File file : files) {
                                String name = file.getName();
                                String encodedPath = relativePath.isEmpty() ? name : (relativePath + "/" + name);
                                Date lastModified = new Date(file.lastModified());
                                
                                if (file.isDirectory()) {
                                    dirCount++;
                    %>
                    <tr class="directory">
                        <td><span class="icon">📁</span></td>
                        <td class="file-name">
                            <a href="filerepo_downlist.jsp?path=<%= encodedPath %>"><%= name %>/</a>
                        </td>
                        <td class="date"><%= dateFormat.format(lastModified) %></td>
                        <td class="size">-</td>
                        <td>폴더</td>
                    </tr>
                    <%
                                } else {
                                    fileCount++;
                                    totalSize += file.length();
                                    String fileExt = "";
                                    int lastDot = name.lastIndexOf('.');
                                    if (lastDot > 0) {
                                        fileExt = name.substring(lastDot + 1).toLowerCase();
                                    }
                                    
                                    String icon = "📄";
                                    String description = "파일";
                                    
                                    // 파일 타입별 아이콘 및 설명
                                    if (fileExt.matches("jpg|jpeg|png|gif|bmp|svg")) {
                                        icon = "🖼️";
                                        description = "이미지 파일";
                                    } else if (fileExt.matches("mp4|avi|mov|wmv|flv|mkv")) {
                                        icon = "🎬";
                                        description = "동영상 파일";
                                    } else if (fileExt.matches("mp3|wav|flac|aac|ogg")) {
                                        icon = "🎵";
                                        description = "음악 파일";
                                    } else if (fileExt.matches("pdf")) {
                                        icon = "📋";
                                        description = "PDF 문서";
                                    } else if (fileExt.matches("doc|docx")) {
                                        icon = "📝";
                                        description = "Word 문서";
                                    } else if (fileExt.matches("xls|xlsx")) {
                                        icon = "📊";
                                        description = "Excel 문서";
                                    } else if (fileExt.matches("zip|rar|7z|tar|gz")) {
                                        icon = "📦";
                                        description = "압축 파일";
                                    } else if (fileExt.matches("txt|log")) {
                                        icon = "📃";
                                        description = "텍스트 파일";
                                    }
                    %>
                    <tr>
                        <td><span class="icon"><%= icon %></span></td>
                        <td class="file-name">
                            <a href="filerepo_download.jsp?path=<%= relativePath.isEmpty() ? "" : (relativePath + "/") %>&filename=<%= name %>">
                                <%= name %>
                            </a>
                        </td>
                        <td class="date"><%= dateFormat.format(lastModified) %></td>
                        <td class="size"><%
                            long fileLength = file.length();
                            String sizeStr = "";
                            if (fileLength == 0) {
                                sizeStr = "0";
                            } else {
                                String[] units = {"", "K", "M", "G", "T"};
                                int unitIndex = 0;
                                double fileSize = fileLength;
                                
                                while (fileSize >= 1024 && unitIndex < units.length - 1) {
                                    fileSize /= 1024;
                                    unitIndex++;
                                }
                                
                                if (unitIndex == 0) {
                                    sizeStr = String.valueOf((long)fileSize);
                                } else {
                                    sizeStr = String.format("%.1f%s", fileSize, units[unitIndex]);
                                }
                            }
                            out.print(sizeStr);
                        %></td>
                        <td><%= description %></td>
                    </tr>
                    <%
                                }
                            }
                        } else {
                    %>
                    <tr>
                        <td colspan="5" class="text-center p-5 text-muted">
                            📭 이 폴더는 비어 있습니다.
                            <br><br>
                            <a href="filerepo_upload.jsp?path=<%= relativePath %>" class="text-primary text-decoration-none">
                                📤 첫 번째 파일을 업로드해보세요
                            </a>
                        </td>
                    </tr>
                    <%
                        }
                    %>
                </tbody>
            </table>

            <div class="stats">
                📊 <strong>통계:</strong> 
                폴더 <%= dirCount %>개, 
                파일 <%= fileCount %>개
                <% if (totalSize > 0) { 
                    String totalSizeStr = "";
                    if (totalSize == 0) {
                        totalSizeStr = "0";
                    } else {
                        String[] units = {"", "K", "M", "G", "T"};
                        int unitIndex = 0;
                        double fileSize = totalSize;
                        
                        while (fileSize >= 1024 && unitIndex < units.length - 1) {
                            fileSize /= 1024;
                            unitIndex++;
                        }
                        
                        if (unitIndex == 0) {
                            totalSizeStr = String.valueOf((long)fileSize);
                        } else {
                            totalSizeStr = String.format("%.1f%s", fileSize, units[unitIndex]);
                        }
                    }
                %>
                    (총 용량: <%= totalSizeStr %>B)
                <% } %>
            </div>
        </div>
        </div>
    </div>
  </div>

        <div class="file-footer">
            <p>🔒 보안 파일 서버 | 안전한 파일 공유를 위해 항상 최신 보안을 유지합니다.</p>
            <p>© 2025 File Server. 무단 접근을 금지합니다.</p>
        </div>

<!-- Footer Include -->
<%@ include file="/WEB-INF/includes/footer_content.jspf" %>

</body>
</html>


