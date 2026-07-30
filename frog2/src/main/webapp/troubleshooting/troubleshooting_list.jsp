<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="pageTitle" value="트러블 슈팅" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-troubleshooting" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/troubleshooting_list.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/troubleshooting_list.js" scope="request" />
<%@ include file="/includes/header.jsp" %>


<div class="troubleshooting-management content-shell">
    <div class="page-header">
        <div class="d-flex justify-content-between align-items-center">
            <div>
                <h1><i class="fas fa-tools"></i> 트러블 슈팅</h1>
                <p class="lead">기술지원 및 문제 해결 이력: <strong>${troubleshootingList.size()}</strong>건</p>
            </div>
            <div>
                <a href="${pageContext.request.contextPath}/troubleshooting?view=add"
                   class="add-button ui-button button--primary button--md">
                    <i class="fas fa-plus"></i>
                    새 트러블 슈팅 등록
                </a>
            </div>
        </div>
    </div>

    <!-- 성공/에러 메시지 표시 -->
    <c:if test="${not empty sessionScope.message}">
        <div class="alert alert-success ui-alert ui-alert--success"
             role="status"
             aria-live="polite"
             aria-atomic="true">
            <i class="fas fa-check-circle"></i>
            <c:out value="${sessionScope.message}" />
        </div>
        <c:remove var="message" scope="session" />
    </c:if>

    <c:if test="${not empty sessionScope.error}">
        <div class="alert alert-danger ui-alert ui-alert--danger"
             role="alert"
             aria-atomic="true">
            <i class="fas fa-exclamation-circle"></i>
            <c:out value="${sessionScope.error}" />
        </div>
        <c:remove var="error" scope="session" />
    </c:if>

    <div class="table-container">
    <div class="ts-search-bar">
        <form class="ts-search-form ui-form"
              method="get"
              action="${pageContext.request.contextPath}/troubleshooting">
            <input type="hidden" name="view" value="list" />
            <input type="text" name="q" value="<c:out value="${q}" />" class="ts-search-input" placeholder="제목, 고객사, 작성자, 본문 전체에서 검색" autocomplete="off" />
            <button type="submit"
                    class="btn-search-simple ui-button button--primary button--md">검색</button>
        </form>
    </div>
    <div class="table-wrapper ui-table-wrap">
        <table class="troubleshooting-table ui-table">
            <thead>
                <tr>
                    <%-- 1. '고객사'와 '제목' 헤더의 순서를 변경합니다. --%>
                    <th width="200">고객사</th>
                    <th>제목</th>
                    <th width="150">발생일자</th>
                    <th width="120">작성자</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="ts" items="${troubleshootingList}" varStatus="status">
                    <c:url var="troubleshootingViewUrl" value="/troubleshooting">
                        <c:param name="view" value="view" />
                        <c:param name="id" value="${ts.id}" />
                    </c:url>
                    <tr data-detail-url="<c:out value='${troubleshootingViewUrl}' />">
                        <%-- 2. '고객사'와 '제목' 데이터 셀(td)의 순서를 헤더와 동일하게 변경합니다. --%>
                        <td class="text-center"><c:out value="${ts.customerName}" /></td>
                        <td>
                            <a href="<c:out value='${troubleshootingViewUrl}' />"
                               class="title-link">
                                <c:out value="${ts.title}" />
                            </a>
                        </td>
                        <td class="text-center">
                            <c:choose>
                                <c:when test="${not empty ts.occurrenceDate}">
                                    <fmt:formatDate value="${ts.occurrenceDate}" pattern="yyyy-MM-dd" />
                                </c:when>
                                <c:otherwise>-</c:otherwise>
                            </c:choose>
                        </td>
                        <td class="text-center"><c:out value="${ts.creator}" /></td>
                    </tr>
                </c:forEach>

                <c:if test="${empty troubleshootingList}">
                    <tr>
                        <td colspan="4" class="empty-state">
                            <i class="fas fa-tools"></i>
                            <div>등록된 트러블 슈팅이 없습니다.</div>
                        </td>
                    </tr>
                </c:if>
            </tbody>
        </table>
    </div>
</div>



<%@ include file="/includes/footer.jsp" %>
