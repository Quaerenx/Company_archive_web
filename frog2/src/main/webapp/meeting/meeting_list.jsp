<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="pageTitle" value="회의록 관리" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers page-meeting" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/meeting.css,/resources/css/pages/meeting_list_layout.css,/resources/css/pages/meeting_list.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/meeting_list.js" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>


<div class="meeting-management content-management content-shell">
    <t:pageHeader>
        <jsp:attribute name="title">
        	<i class="fas fa-clipboard-list"></i> 회의록 관리
        </jsp:attribute>
        <jsp:attribute name="subtitle">총 ${totalCount}개의 회의록이 등록되어 있습니다</jsp:attribute>
        <jsp:attribute name="actions">
            <a href="${pageContext.request.contextPath}/meeting?view=write"
               class="add-button ui-button button--primary button--md"><i class="fas fa-pen"></i> 새 회의록 작성</a>
        </jsp:attribute>
    </t:pageHeader>
    
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
    
    <!-- 회의록 목록 (troubleshooting_list 테이블 디자인 적용) -->

    <div class="table-container">
        <div class="table-wrapper ui-table-wrap">
            <c:choose>
                <c:when test="${not empty meetingList}">
                    <table class="meeting-list-table ui-table">
                        <thead>
                            <tr>
                                <th>제목</th>
                                <th width="120">글쓴이</th>
                                <th width="160">회의 일시</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="meeting" items="${meetingList}">
                                <c:url var="meetingViewUrl" value="/meeting">
                                    <c:param name="view" value="view" />
                                    <c:param name="id" value="${meeting.meetingId}" />
                                    <c:param name="returnPage" value="${currentPage}" />
                                </c:url>
                                <tr data-detail-url="<c:out value='${meetingViewUrl}' />">
                                    <td>
                                        <a href="<c:out value='${meetingViewUrl}' />"
                                           class="title-link">
                                            <c:out value="${meeting.title}" />
                                        </a>
                                    </td>
                                    <td class="text-center"><c:out value="${meeting.authorName}" /></td>
                                    <td class="text-center">
                                        <fmt:formatDate value="${meeting.meetingDatetime}" pattern="yyyy-MM-dd HH:mm"/>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>

                    <!-- 페이징 -->
                    <div class="pagination-container">
                        <div class="page-info">
                            ${currentPage} / ${totalPages} 페이지 (총 ${totalCount}건)
                        </div>
                        <div class="pagination">
                            <c:if test="${currentPage > 1}">
                                <a href="?view=list&page=1" class="page-link ui-touch-target" aria-label="첫 페이지"><i class="fas fa-angle-double-left" aria-hidden="true"></i></a>
                                <a href="?view=list&page=${currentPage - 1}" class="page-link ui-touch-target" aria-label="이전 페이지"><i class="fas fa-angle-left" aria-hidden="true"></i></a>
                            </c:if>
                            <c:set var="startPage" value="${currentPage - 2}" />
                            <c:set var="endPage" value="${currentPage + 2}" />
                            <c:if test="${startPage < 1}"><c:set var="startPage" value="1" /></c:if>
                            <c:if test="${endPage > totalPages}"><c:set var="endPage" value="${totalPages}" /></c:if>
                            <c:forEach var="i" begin="${startPage}" end="${endPage}">
                                <c:choose>
                                    <c:when test="${i == currentPage}"><span class="page-link active">${i}</span></c:when>
                                    <c:otherwise><a href="?view=list&page=${i}" class="page-link">${i}</a></c:otherwise>
                                </c:choose>
                            </c:forEach>
                            <c:if test="${currentPage < totalPages}">
                                <a href="?view=list&page=${currentPage + 1}" class="page-link ui-touch-target" aria-label="다음 페이지"><i class="fas fa-angle-right" aria-hidden="true"></i></a>
                                <a href="?view=list&page=${totalPages}" class="page-link ui-touch-target" aria-label="마지막 페이지"><i class="fas fa-angle-double-right" aria-hidden="true"></i></a>
                            </c:if>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="empty-state">
                        <i class="fas fa-clipboard"></i>
                        <h3>등록된 회의록이 없습니다</h3>
                        <p>첫 번째 회의록을 작성해보세요.</p>
                        <a href="${pageContext.request.contextPath}/meeting?view=write"
                           class="add-button secondary ui-button button--secondary button--md">
                            <i class="fas fa-pen"></i>
                            회의록 작성하기
                        </a>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>



<%@ include file="/includes/footer.jsp" %>
