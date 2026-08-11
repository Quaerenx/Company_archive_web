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
        <div class="table-wrapper ui-table-wrap"
             data-ui-scroll-region
             data-ui-scroll-label="회의록 표">
            <c:choose>
                <c:when test="${not empty meetingList}">
                    <table class="meeting-list-table ui-table">
                        <caption class="sr-only">회의록 목록</caption>
                        <thead>
                            <tr>
                                <th scope="col">제목</th>
                                <th scope="col" width="120">글쓴이</th>
                                <th scope="col" width="160">회의 일시</th>
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

                    <c:if test="${currentPage > 1}">
                        <c:url var="meetingPreviousPageUrl" value="/meeting">
                            <c:param name="view" value="list" />
                            <c:param name="page" value="${currentPage - 1}" />
                        </c:url>
                    </c:if>
                    <c:if test="${currentPage < totalPages}">
                        <c:url var="meetingNextPageUrl" value="/meeting">
                            <c:param name="view" value="list" />
                            <c:param name="page" value="${currentPage + 1}" />
                        </c:url>
                    </c:if>
                    <t:tableFooter itemLabel="회의록"
                                   currentPage="${currentPage}"
                                   totalPages="${totalPages}"
                                   previousUrl="${meetingPreviousPageUrl}"
                                   nextUrl="${meetingNextPageUrl}"
                                   paginationLabel="회의록 페이지" />
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
