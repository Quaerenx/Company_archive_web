<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="월별 고객 응대 현황" scope="request" />
<c:set var="pageDocumentTitle" value="${pageTitle}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers monthly-response-page" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/monthly_customer_response.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/monthly_customer_response.js" scope="request" />

<%@ include file="/includes/header.jsp" %>

<div class="monthly-response-container content-shell">
    <a href="${pageContext.request.contextPath}/mypage" class="back-link ui-touch-target">
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
            <button type="button"
                    class="add-button ui-button button--primary button--md"
                    data-monthly-action="add">
                <i class="fas fa-plus"></i> 응대 추가
            </button>
        </jsp:attribute>
    </t:pageHeader>

    <!-- 알림 메시지 -->
    <c:if test="${not empty message}">
        <c:set var="safeMessageTone" value="${messageType == 'success' ? 'success' : 'danger'}" />
        <c:set var="safeMessageIcon" value="${messageType == 'success' ? 'check-circle' : 'exclamation-circle'}" />
        <div class="ui-alert ui-alert--<c:out value='${safeMessageTone}' />"
             role="${messageType == 'success' ? 'status' : 'alert'}"
             aria-live="${messageType == 'success' ? 'polite' : 'assertive'}"
             aria-atomic="true">
            <i class="fas fa-<c:out value='${safeMessageIcon}' />"></i>
            <c:out value="${message}" />
        </div>
    </c:if>

    <!-- 필터 옵션 -->
    <div class="filter-card">
        <div class="filter-header">
            <i class="fas fa-filter"></i>
            <h3>검색 조건</h3>
        </div>
        <form class="filter-form ui-form"
              method="GET"
              action="${pageContext.request.contextPath}/mypage"
              id="filterForm"
              data-ui-submit-lock="auto">
            <input type="hidden" name="action" value="monthlyResponse">
            <div class="filter-inputs">
                <div class="form-group">
                    <label for="year">연도</label>
                    <select id="year" name="year" required data-monthly-auto-submit>
                        <c:forEach var="offset" begin="0" end="5">
                            <c:set var="yearOption" value="${currentYear - offset}" />
                            <option value="<c:out value='${yearOption}' />" <c:if test="${selectedYear == yearOption}">selected</c:if>>
                                <c:out value="${yearOption}" />년
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <div class="form-group">
                    <label for="month">월</label>
                    <select id="month" name="month" required data-monthly-auto-submit>
                        <c:forEach var="monthOption" begin="1" end="12">
                            <option value="<c:out value='${monthOption}' />" <c:if test="${selectedMonth == monthOption}">selected</c:if>>
                                <c:out value="${monthOption}" />월
                            </option>
                        </c:forEach>
                    </select>
                </div>
            </div>
        </form>
    </div>

    <!-- 월별 응대 데이터 -->
    <c:if test="${not empty monthlyResponses}">
    <div class="results-card">
        <div class="results-header">
            <h3>
                <i class="fas fa-calendar-check"></i>
                <c:out value="${selectedYear}" />년 <c:out value="${selectedMonth}" />월 고객 응대 현황 (<c:out value="${monthlyResponses.size()}" />건)
            </h3>
            <button type="button"
                    class="btn-min ui-button button--secondary button--sm"
                    data-monthly-action="add">
                <i class="fas fa-plus"></i> 추가
            </button>
        </div>
        <div class="table-responsive ui-table-wrap"
             data-ui-scroll-region
             data-ui-scroll-label="월별 고객 응대 표">
            <table class="data-table ui-table">
                <caption class="sr-only">월별 고객 응대 기록</caption>
                <thead>
                    <tr>
                        <th scope="col">날짜</th>
                        <th scope="col">고객명</th>
                        <th scope="col">사유</th>
                        <th scope="col">조치 내용</th>
                        <th scope="col">비고</th>
                        <th scope="col" class="monthly-response-actions-column">액션</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="responseEntry" items="${monthlyResponses}">
                    <fmt:formatDate var="responseDateValue" value="${responseEntry.responseDate}" pattern="yyyy-MM-dd" />
                    <tr data-response-id="<c:out value='${responseEntry.id}' />">
                        <td><c:out value="${not empty responseDateValue ? responseDateValue : '-'}" /></td>
                        <td><c:out value="${not empty responseEntry.customerName ? responseEntry.customerName : '-'}" /></td>
                        <td><c:out value="${not empty responseEntry.reason ? responseEntry.reason : '-'}" /></td>
                        <td><c:out value="${not empty responseEntry.actionContent ? responseEntry.actionContent : '-'}" /></td>
                        <td><c:out value="${not empty responseEntry.note ? responseEntry.note : '-'}" /></td>
                        <td>
                            <div class="action-buttons">
                                <input type="hidden" class="response-date" value="<c:out value='${responseDateValue}' />">
                                <input type="hidden" class="response-customer-name" value="<c:out value='${responseEntry.customerName}' />">
                                <input type="hidden" class="response-reason" value="<c:out value='${responseEntry.reason}' />">
                                <textarea class="response-action-content" hidden><c:out value="${responseEntry.actionContent}" /></textarea>
                                <textarea class="response-note" hidden><c:out value="${responseEntry.note}" /></textarea>
                                <button type="button"
                                        class="btn-icon btn-edit ui-button button--secondary button--sm ui-touch-target"
                                        aria-label="응대 기록 수정">
                                    <i class="fas fa-edit" aria-hidden="true"></i>
                                </button>
                                <button type="button"
                                        class="btn-icon btn-delete ui-button button--danger button--sm ui-touch-target"
                                        aria-label="응대 기록 삭제">
                                    <i class="fas fa-trash" aria-hidden="true"></i>
                                </button>
                            </div>
                        </td>
                    </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
        <t:tableFooter itemLabel="응대 기록"
                       currentPage="1"
                       totalPages="1"
                       paginationLabel="월별 고객 응대 페이지" />
    </div>
    </c:if>

    <!-- 데이터가 없을 때 -->
    <c:if test="${empty monthlyResponses}">
    <div class="results-card">
        <div class="results-header">
            <h3>
                <i class="fas fa-calendar-check"></i>
                <c:out value="${selectedYear}" />년 <c:out value="${selectedMonth}" />월 고객 응대 현황
            </h3>
            <button type="button"
                    class="btn-min ui-button button--secondary button--sm"
                    data-monthly-action="add">
                <i class="fas fa-plus"></i> 추가
            </button>
        </div>
        <div class="empty-state">
            <i class="fas fa-inbox"></i>
            <p><c:out value="${selectedYear}" />년 <c:out value="${selectedMonth}" />월에는 고객 응대 기록이 없습니다.</p>
            <button type="button"
                    class="add-button secondary ui-button button--secondary button--md"
                    data-monthly-action="add">
                <i class="fas fa-plus"></i> 첫 응대 기록 추가
            </button>
        </div>
    </div>
    </c:if>
</div>

<!-- 추가/수정 모달 -->
<div id="responseModal"
     class="modal"
     role="dialog"
     aria-modal="true"
     aria-labelledby="modalTitle"
     aria-hidden="true"
     tabindex="-1">
    <div class="modal-content">
        <div class="modal-header">
            <h2 id="modalTitle">고객 응대 추가</h2>
            <button type="button"
                    class="close ui-touch-target"
                    data-monthly-action="close"
                    aria-label="고객 응대 창 닫기">&times;</button>
        </div>
        <form id="responseForm"
              class="ui-form"
              method="POST"
              action="${pageContext.request.contextPath}/mypage"
              data-ui-submit-lock="auto">
            <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
            <input type="hidden" name="formAction" id="formAction" value="addResponse">
            <input type="hidden" name="responseId" id="responseId">
            <input type="hidden" name="year" value="<c:out value='${not empty selectedYear ? selectedYear : currentYear}' />">
            <input type="hidden" name="month" value="<c:out value='${not empty selectedMonth ? selectedMonth : currentMonth}' />">

            <div class="modal-body">
                <div class="modal-form-group">
                    <label for="responseDate">날짜 <span class="required">*</span></label>
                    <input type="date"
                           id="responseDate"
                           name="responseDate"
                           data-dialog-initial-focus
                           required>
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

            <div class="modal-footer ui-form-actions">
                <button type="button"
                        class="btn btn-cancel ui-button button--secondary button--md"
                        data-monthly-action="close">취소</button>
                <button type="submit"
                        class="btn btn-primary ui-button button--primary button--md"
                        id="submitBtn"
                        data-busy-label="저장 중">저장</button>
            </div>
        </form>
    </div>
</div>
<div id="monthlyResponseConfig" hidden data-year="<c:out value='${not empty selectedYear ? selectedYear : currentYear}' />" data-month="<c:out value='${not empty selectedMonth ? selectedMonth : currentMonth}' />"></div>

<%@ include file="/includes/footer.jsp" %>
