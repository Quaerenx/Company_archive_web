<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="pageTitle" value="회의록 관리" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-meeting page-meeting-list" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/meeting_list.css" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>


<div class="meeting-management content-management content-shell">
    <t:pageHeader>
        <jsp:attribute name="title">
        	<i class="fas fa-clipboard-list"></i> 회의록 관리
        </jsp:attribute>
        <jsp:attribute name="subtitle">회의 결과와 후속 내용을 한곳에서 확인합니다.</jsp:attribute>
        <jsp:attribute name="actions">
            <a href="${pageContext.request.contextPath}/meeting?view=write"
               class="ui-button button--primary button--md"><i class="fas fa-pen"></i> 새 회의록 작성</a>
        </jsp:attribute>
    </t:pageHeader>
    
    <t:flashMessages />
    
    <div class="table-container ui-work-surface">
        <c:choose>
            <c:when test="${not empty meetingList}">
                <div class="table-wrapper ui-table-wrap"
                     data-ui-scroll-region
                     data-ui-scroll-label="회의록 표">
                    <table class="meeting-list-table ui-table ui-data-table">
                        <caption class="sr-only">회의록 목록</caption>
                        <thead>
                            <tr>
                                <th scope="col" class="meeting-col-datetime col--date">회의 일시</th>
                                <th scope="col" class="meeting-col-type col--type">유형</th>
                                <th scope="col" class="col--title">제목</th>
                                <th scope="col" class="meeting-col-author col--author">작성자</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="meeting" items="${meetingList}">
                                <c:url var="meetingViewUrl" value="/meeting">
                                    <c:param name="view" value="view" />
                                    <c:param name="id" value="${meeting.meetingId}" />
                                    <c:param name="returnPage" value="${currentPage}" />
                                </c:url>
                                <fmt:formatDate var="meetingDateIso" value="${meeting.meetingDatetime}" pattern="yyyy-MM-dd'T'HH:mm" />
                                <fmt:formatDate var="meetingDateLabel" value="${meeting.meetingDatetime}" pattern="yyyy.MM.dd" />
                                <fmt:formatDate var="meetingTimeLabel" value="${meeting.meetingDatetime}" pattern="HH:mm" />
                                <tr class="ui-data-row" data-detail-url="<c:out value='${meetingViewUrl}' />">
                                    <td class="meeting-datetime-cell col--date">
                                        <time datetime="<c:out value='${meetingDateIso}' />">
                                            <span class="meeting-date"><c:out value="${meetingDateLabel}" /></span>
                                            <span class="meeting-time"><c:out value="${meetingTimeLabel}" /></span>
                                        </time>
                                    </td>
                                    <td class="meeting-type-cell col--type">
                                        <span class="meeting-type-badge ui-badge ui-badge--neutral"><c:out value="${meeting.meetingTypeLabel}" /></span>
                                    </td>
                                    <td class="meeting-title-cell col--title">
                                        <a href="<c:out value='${meetingViewUrl}' />"
                                           class="title-link">
                                            <c:out value="${meeting.title}" />
                                        </a>
                                        <span class="meeting-row-meta" aria-hidden="true">
                                            <span><c:out value="${meeting.meetingTypeLabel}" /></span>
                                            <span><c:out value="${meetingDateLabel}" /> <c:out value="${meetingTimeLabel}" /></span>
                                            <span><c:out value="${meeting.authorName}" /></span>
                                        </span>
                                    </td>
                                    <td class="meeting-author-cell col--author"><c:out value="${meeting.authorName}" /></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>

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
                       class="ui-button button--secondary button--md">
                        <i class="fas fa-pen"></i>
                        회의록 작성하기
                    </a>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>



<%@ include file="/includes/footer.jsp" %>
