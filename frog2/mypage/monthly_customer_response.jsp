<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="com.company.model.UserDTO" %>
<%@ page import="com.company.model.MonthlyCustomerResponseDTO" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%
    UserDTO currentUser = (UserDTO) session.getAttribute("user");
    if (currentUser == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }

    List<MonthlyCustomerResponseDTO> monthlyResponses = (List<MonthlyCustomerResponseDTO>) request.getAttribute("monthlyResponses");
    Integer selectedYear = (Integer) request.getAttribute("selectedYear");
    Integer selectedMonth = (Integer) request.getAttribute("selectedMonth");
    Boolean hasData = (Boolean) request.getAttribute("hasData");
    String message = (String) request.getAttribute("message");
    String messageType = (String) request.getAttribute("messageType");

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    // 현재 연도
    java.util.Calendar cal = java.util.Calendar.getInstance();
    int currentYear = cal.get(java.util.Calendar.YEAR);
    int currentMonth = cal.get(java.util.Calendar.MONTH) + 1;
%>
<c:set var="pageTitle" value="월별 고객 응대 현황" scope="request" />

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
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
    <style>
        .monthly-response-container {
            width: 100%;
            max-width: 1000px;
            margin: 0 auto;
            padding: 32px 16px;
        }

        .filter-card {
            background: white;
            padding: 24px;
            border-radius: 8px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
            margin-bottom: 20px;
        }

        .filter-header {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 16px;
            padding-bottom: 12px;
            border-bottom: 2px solid #e5e7eb;
        }

        .filter-header h3 {
            margin: 0;
            font-size: 1.125rem;
            color: #333333;
        }

        .filter-form {
            display: flex;
            gap: 12px;
            align-items: flex-end;
            justify-content: flex-start;
            flex-wrap: wrap;
        }

        .filter-inputs {
            display: flex;
            gap: 12px;
            align-items: flex-end;
            flex-wrap: wrap;
            flex: 1;
        }

        .form-group {
            display: flex;
            flex-direction: column;
            gap: 6px;
        }

        .form-group label {
            font-size: 0.875rem;
            font-weight: 600;
            color: #666666;
        }

        .form-group select {
            padding: 8px 12px;
            border: 1px solid #d1d5db;
            border-radius: 6px;
            font-size: 0.875rem;
            min-width: 150px;
        }

        .results-card {
            background: white;
            padding: 24px;
            border-radius: 8px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
            margin-bottom: 20px;
        }

        .results-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 12px;
            flex-wrap: wrap;
            margin-bottom: 16px;
            padding-bottom: 12px;
            border-bottom: 2px solid #e5e7eb;
        }

        .results-header h3 {
            margin: 0;
            font-size: 1.125rem;
            color: #333333;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .table-responsive {
            overflow-x: auto;
        }

        .data-table {
            width: 100%;
            border-collapse: collapse;
            font-size: 0.875rem;
        }

        .data-table thead {
            background-color: #f9fafb;
        }

        .data-table th {
            padding: 12px;
            text-align: left;
            font-weight: 600;
            color: #374151;
            border-bottom: 2px solid #e5e7eb;
            white-space: nowrap;
        }

        .data-table td {
            padding: 12px;
            border-bottom: 1px solid #e5e7eb;
            color: #4b5563;
            vertical-align: top;
        }

        .data-table tbody tr:hover {
            background-color: #f9fafb;
        }

        .data-table a {
            color: #3D5A80;
            text-decoration: none;
            font-weight: 500;
        }

        .data-table a:hover {
            text-decoration: underline;
        }

        .badge {
            display: inline-block;
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 0.75rem;
            font-weight: 600;
        }

        .badge-maintenance {
            background-color: #dbeafe;
            color: #1e40af;
        }

        .badge-troubleshooting {
            background-color: #fee2e2;
            color: #991b1b;
        }

        .empty-state {
            text-align: center;
            padding: 60px 20px;
            color: #9ca3af;
        }

        .empty-state i {
            font-size: 3.5rem;
            margin-bottom: 16px;
            opacity: 0.5;
        }

        .empty-state p {
            font-size: 1rem;
            margin: 0 0 16px 0;
        }

        .back-link {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            color: #3D5A80;
            text-decoration: none;
            font-size: 0.875rem;
            font-weight: 500;
            margin-bottom: 16px;
        }

        .back-link:hover {
            text-decoration: underline;
        }

        /* 날짜 컬럼 너비 */
        .data-table th:nth-child(1),
        .data-table td:nth-child(1) {
            width: 110px;
        }

        /* 고객명 컬럼 너비 */
        .data-table th:nth-child(2),
        .data-table td:nth-child(2) {
            width: 150px;
        }

        /* 사유 컬럼 너비 */
        .data-table th:nth-child(3),
        .data-table td:nth-child(3) {
            width: 180px;
        }

        /* 조치 내용 - 가장 넓게 (자동 너비) */
        .data-table th:nth-child(4),
        .data-table td:nth-child(4) {
            width: auto;
        }

        /* 비고 컬럼 너비 */
        .data-table th:nth-child(5),
        .data-table td:nth-child(5) {
            width: 180px;
        }

        /* 액션 버튼 스타일 */
        .action-buttons {
            display: flex;
            gap: 8px;
            justify-content: center;
        }

        .btn-icon {
            padding: 6px 10px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 0.875rem;
            transition: all 0.2s;
            display: inline-flex;
            align-items: center;
            gap: 4px;
        }

        .btn-edit {
            background-color: #5B8FB9;
            color: white;
        }

        .btn-edit:hover {
            background-color: #4A7BA0;
            transform: translateY(-1px);
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }

        .btn-delete {
            background-color: #B66B6B;
            color: white;
        }

        .btn-delete:hover {
            background-color: #A05555;
            transform: translateY(-1px);
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }

        .required {
            color: #ef4444;
        }

        /* 알림 메시지 */
        .alert {
            padding: 12px 16px;
            border-radius: 6px;
            margin-bottom: 20px;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .alert-success {
            background-color: #d1fae5;
            color: #065f46;
            border: 1px solid #6ee7b7;
        }

        .alert-error {
            background-color: #fee2e2;
            color: #991b1b;
            border: 1px solid #fca5a5;
        }

        @media (max-width: 768px) {
            .filter-form {
                flex-direction: column;
                align-items: stretch;
            }

            .filter-inputs {
                flex-direction: column;
            }

            .form-group select {
                width: 100%;
            }

            .modal-footer .btn {
                width: 100%;
                justify-content: center;
            }

            /* 모바일에서는 컬럼 너비 제한 제거 */
            .data-table th,
            .data-table td {
                width: auto !important;
                min-width: auto !important;
            }

            #responseModal .modal-content {
                width: 95%;
                margin: 10% auto;
            }

            .action-buttons {
                flex-direction: column;
            }

            .results-header .btn-min,
            .empty-state .add-button {
                width: 100%;
                justify-content: center;
            }
        }
    </style>
</head>
<body class="page-customers">
<%@ include file="/WEB-INF/includes/header_nav.jspf" %>

<div class="monthly-response-container">
    <a href="${pageContext.request.contextPath}/mypage" class="back-link">
        <i class="fas fa-arrow-left"></i>
        마이페이지로 돌아가기
    </a>

    <t:pageHeader>
        <jsp:attribute name="title">
            <i class="fas fa-calendar-alt"></i> 월별 고객 응대 현황
        </jsp:attribute>
        <jsp:attribute name="subtitle">
            월별 고객 응대 및 트러블슈팅 작업 현황
        </jsp:attribute>
        <jsp:attribute name="actions">
            <button type="button" class="add-button" onclick="openAddModal()">
                <i class="fas fa-plus"></i> 응대 추가
            </button>
        </jsp:attribute>
    </t:pageHeader>

    <!-- 알림 메시지 -->
    <% if (message != null) { %>
    <div class="alert alert-<%= messageType %>">
        <i class="fas fa-<%= "success".equals(messageType) ? "check-circle" : "exclamation-circle" %>"></i>
        <%= message %>
    </div>
    <% } %>

    <!-- 필터 옵션 -->
    <div class="filter-card">
        <div class="filter-header">
            <i class="fas fa-filter"></i>
            <h3>검색 조건</h3>
        </div>
        <form class="filter-form" method="GET" action="${pageContext.request.contextPath}/mypage" id="filterForm">
            <input type="hidden" name="action" value="monthlyResponse">
            <div class="filter-inputs">
                <div class="form-group">
                    <label for="year">연도</label>
                    <select id="year" name="year" required onchange="autoSubmitForm()">
                        <% for(int y = currentYear; y >= currentYear - 5; y--) { %>
                            <option value="<%= y %>" <%= (selectedYear != null && selectedYear == y) ? "selected" : (y == currentYear ? "selected" : "") %>>
                                <%= y %>년
                            </option>
                        <% } %>
                    </select>
                </div>
                <div class="form-group">
                    <label for="month">월</label>
                    <select id="month" name="month" required onchange="autoSubmitForm()">
                        <% for(int m = 1; m <= 12; m++) { %>
                            <option value="<%= m %>" <%= (selectedMonth != null && selectedMonth == m) ? "selected" : (m == currentMonth ? "selected" : "") %>>
                                <%= m %>월
                            </option>
                        <% } %>
                    </select>
                </div>
            </div>
        </form>
    </div>

    <!-- 월별 응대 데이터 -->
    <% if (monthlyResponses != null && !monthlyResponses.isEmpty()) { %>
    <div class="results-card">
        <div class="results-header">
            <h3>
                <i class="fas fa-calendar-check"></i>
                <%= selectedYear %>년 <%= selectedMonth %>월 고객 응대 현황 (<%= monthlyResponses.size() %>건)
            </h3>
            <button type="button" class="btn-min primary" onclick="openAddModal()">
                <i class="fas fa-plus"></i> 추가
            </button>
        </div>
        <div class="table-responsive">
            <table class="data-table">
                <thead>
                    <tr>
                        <th>날짜</th>
                        <th>고객명</th>
                        <th>사유</th>
                        <th>조치 내용</th>
                        <th>비고</th>
                        <th style="width: 120px;">액션</th>
                    </tr>
                </thead>
                <tbody>
                    <% for (MonthlyCustomerResponseDTO dto : monthlyResponses) { %>
                    <tr>
                        <td><%= dto.getResponseDate() != null ? sdf.format(dto.getResponseDate()) : "-" %></td>
                        <td><%= dto.getCustomerName() != null ? dto.getCustomerName() : "-" %></td>
                        <td><%= dto.getReason() != null ? dto.getReason() : "-" %></td>
                        <td><%= dto.getActionContent() != null ? dto.getActionContent() : "-" %></td>
                        <td><%= dto.getNote() != null ? dto.getNote() : "-" %></td>
                        <td>
                            <div class="action-buttons">
                                <button type="button" class="btn-icon btn-edit"
                                    onclick="openEditModal(<%= dto.getId() %>, '<%= sdf.format(dto.getResponseDate()) %>', '<%= dto.getCustomerName() %>', '<%= dto.getReason() %>', '<%= dto.getActionContent() != null ? dto.getActionContent().replace("'", "\\'").replace("\n", "\\n") : "" %>', '<%= dto.getNote() != null ? dto.getNote().replace("'", "\\'").replace("\n", "\\n") : "" %>')">
                                    <i class="fas fa-edit"></i>
                                </button>
                                <button type="button" class="btn-icon btn-delete"
                                    onclick="deleteResponse(<%= dto.getId() %>)">
                                    <i class="fas fa-trash"></i>
                                </button>
                            </div>
                        </td>
                    </tr>
                    <% } %>
                </tbody>
            </table>
        </div>
    </div>
    <% } %>

    <!-- 데이터가 없을 때 -->
    <% if (monthlyResponses == null || monthlyResponses.isEmpty()) { %>
    <div class="results-card">
        <div class="results-header">
            <h3>
                <i class="fas fa-calendar-check"></i>
                <%= selectedYear %>년 <%= selectedMonth %>월 고객 응대 현황
            </h3>
            <button type="button" class="btn-min primary" onclick="openAddModal()">
                <i class="fas fa-plus"></i> 추가
            </button>
        </div>
        <div class="empty-state">
            <i class="fas fa-inbox"></i>
            <p><%= selectedYear %>년 <%= selectedMonth %>월에는 고객 응대 기록이 없습니다.</p>
            <button type="button" class="add-button" onclick="openAddModal()">
                <i class="fas fa-plus"></i> 첫 응대 기록 추가
            </button>
        </div>
    </div>
    <% } %>
</div>

<!-- 추가/수정 모달 -->
<div id="responseModal" class="modal">
    <div class="modal-content">
        <div class="modal-header">
            <h2 id="modalTitle">고객 응대 추가</h2>
            <button class="close" onclick="closeModal()">&times;</button>
        </div>
        <form id="responseForm" method="POST" action="${pageContext.request.contextPath}/mypage">
            <input type="hidden" name="formAction" id="formAction" value="addResponse">
            <input type="hidden" name="responseId" id="responseId">
            <input type="hidden" name="year" value="<%= selectedYear != null ? selectedYear : currentYear %>">
            <input type="hidden" name="month" value="<%= selectedMonth != null ? selectedMonth : currentMonth %>">

            <div class="modal-body">
                <div class="modal-form-group">
                    <label for="responseDate">날짜 <span class="required">*</span></label>
                    <input type="date" id="responseDate" name="responseDate" required>
                </div>

                <div class="modal-form-group">
                    <label for="customerName">고객명 <span class="required">*</span></label>
                    <input type="text" id="customerName" name="customerName" required>
                </div>

                <div class="modal-form-group">
                    <label for="reason">사유 <span class="required">*</span></label>
                    <input type="text" id="reason" name="reason" required>
                </div>

                <div class="modal-form-group">
                    <label for="actionContent">조치 내용</label>
                    <textarea id="actionContent" name="actionContent"></textarea>
                </div>

                <div class="modal-form-group">
                    <label for="note">비고</label>
                    <textarea id="note" name="note"></textarea>
                </div>
            </div>

            <div class="modal-footer">
                <button type="button" class="btn btn-cancel" onclick="closeModal()">취소</button>
                <button type="submit" class="btn btn-primary" id="submitBtn">저장</button>
            </div>
        </form>
    </div>
</div>

<script>
const selectedYear = <%= selectedYear != null ? selectedYear : currentYear %>;
const selectedMonth = <%= selectedMonth != null ? selectedMonth : currentMonth %>;

// 자동 조회 함수
function autoSubmitForm() {
    document.getElementById('filterForm').submit();
}

function getDefaultResponseDate() {
    const today = new Date();
    const day = (today.getFullYear() === selectedYear && today.getMonth() + 1 === selectedMonth)
        ? today.getDate()
        : 1;
    return selectedYear + '-' + String(selectedMonth).padStart(2, '0') + '-' + String(day).padStart(2, '0');
}

// 모달 열기 (추가)
function openAddModal() {
    document.getElementById('modalTitle').textContent = '고객 응대 추가';
    document.getElementById('responseForm').reset();
    document.getElementById('formAction').value = 'addResponse';
    document.getElementById('responseId').value = '';
    document.getElementById('responseDate').value = getDefaultResponseDate();

    document.getElementById('responseModal').style.display = 'block';
}

// 모달 열기 (수정)
function openEditModal(id, date, customerName, reason, actionContent, note) {
    document.getElementById('modalTitle').textContent = '고객 응대 수정';
    document.getElementById('formAction').value = 'updateResponse';
    document.getElementById('responseId').value = id;
    document.getElementById('responseDate').value = date;
    document.getElementById('customerName').value = customerName;
    document.getElementById('reason').value = reason;
    document.getElementById('actionContent').value = actionContent.replace(/\\n/g, '\n');
    document.getElementById('note').value = note.replace(/\\n/g, '\n');

    document.getElementById('responseModal').style.display = 'block';
}

// 모달 닫기
function closeModal() {
    document.getElementById('responseModal').style.display = 'none';
}

// 삭제
function deleteResponse(id) {
    if (confirm('정말로 이 응대 기록을 삭제하시겠습니까?')) {
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = '${pageContext.request.contextPath}/mypage';

        const formActionInput = document.createElement('input');
        formActionInput.type = 'hidden';
        formActionInput.name = 'formAction';
        formActionInput.value = 'deleteResponse';
        form.appendChild(formActionInput);

        const idInput = document.createElement('input');
        idInput.type = 'hidden';
        idInput.name = 'responseId';
        idInput.value = id;
        form.appendChild(idInput);

        const yearInput = document.createElement('input');
        yearInput.type = 'hidden';
        yearInput.name = 'year';
        yearInput.value = '<%= selectedYear != null ? selectedYear : currentYear %>';
        form.appendChild(yearInput);

        const monthInput = document.createElement('input');
        monthInput.type = 'hidden';
        monthInput.name = 'month';
        monthInput.value = '<%= selectedMonth != null ? selectedMonth : currentMonth %>';
        form.appendChild(monthInput);

        document.body.appendChild(form);
        form.submit();
    }
}

// 모달 외부 클릭 시 닫기
window.onclick = function(event) {
    const modal = document.getElementById('responseModal');
    if (event.target == modal) {
        closeModal();
    }
}
</script>

<%@ include file="/WEB-INF/includes/footer_content.jspf" %>

</body>
</html>
