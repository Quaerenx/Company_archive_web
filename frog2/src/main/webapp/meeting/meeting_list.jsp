<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="pageTitle" value="회의록 관리" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-meeting page-meeting-list" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/meeting_list.css" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>

<c:url var="meetingWriteUrl" value="/meeting">
    <c:param name="view" value="write" />
    <c:param name="returnPage" value="${currentPage}" />
    <c:if test="${not empty q}"><c:param name="returnQ" value="${q}" /></c:if>
    <c:if test="${not empty meetingType}"><c:param name="returnType" value="${meetingType}" /></c:if>
    <c:if test="${not empty author}"><c:param name="returnAuthor" value="${author}" /></c:if>
    <c:if test="${not empty startDate}"><c:param name="returnStartDate" value="${startDate}" /></c:if>
    <c:if test="${not empty endDate}"><c:param name="returnEndDate" value="${endDate}" /></c:if>
</c:url>

<div class="meeting-management content-management content-shell">
    <t:pageHeader>
        <jsp:attribute name="title">
        	<i class="fas fa-clipboard-list"></i> 회의록 관리
        </jsp:attribute>
        <jsp:attribute name="subtitle">회의 결과와 후속 내용을 한곳에서 확인합니다.</jsp:attribute>
        <jsp:attribute name="actions">
            <a href="<c:out value='${meetingWriteUrl}' />"
               class="ui-button button--primary button--md"><i class="fas fa-pen"></i> 새 회의록 작성</a>
        </jsp:attribute>
    </t:pageHeader>
    
    <t:flashMessages />
    
    <div class="ui-work-surface">
        <form class="meeting-filter ui-table-toolbar ui-form"
              method="get"
              action="${pageContext.request.contextPath}/meeting">
            <input type="hidden" name="view" value="list" />
            <div class="ui-form-field meeting-filter__query">
                <label for="meetingQuery">제목·본문</label>
                <input id="meetingQuery" name="q" type="search"
                       value="<c:out value='${q}' />"
                       placeholder="검색어 입력" maxlength="100" />
            </div>
            <div class="ui-form-field">
                <label for="meetingType">회의 유형</label>
                <select id="meetingType" name="type">
                    <option value="">전체 유형</option>
                    <option value="daily" ${meetingType == 'daily' ? 'selected' : ''}>일일 회의</option>
                    <option value="weekly" ${meetingType == 'weekly' ? 'selected' : ''}>주간 회의</option>
                    <option value="monthly" ${meetingType == 'monthly' ? 'selected' : ''}>월간 회의</option>
                    <option value="project" ${meetingType == 'project' ? 'selected' : ''}>프로젝트 회의</option>
                    <option value="emergency" ${meetingType == 'emergency' ? 'selected' : ''}>긴급 회의</option>
                    <option value="other" ${meetingType == 'other' ? 'selected' : ''}>기타</option>
                </select>
            </div>
            <div class="ui-form-field">
                <label for="meetingAuthor">작성자</label>
                <input id="meetingAuthor" name="author"
                       value="<c:out value='${author}' />"
                       placeholder="작성자명" maxlength="100" />
            </div>
            <div class="ui-form-field">
                <label for="meetingStartDate">시작일</label>
                <input id="meetingStartDate" name="startDate" type="date"
                       value="<c:out value='${startDate}' />" />
            </div>
            <div class="ui-form-field">
                <label for="meetingEndDate">종료일</label>
                <input id="meetingEndDate" name="endDate" type="date"
                       value="<c:out value='${endDate}' />" />
            </div>
            <div class="meeting-filter__actions">
                <c:if test="${meetingFilterActive}">
                    <a class="ui-button button--secondary button--md"
                       href="${pageContext.request.contextPath}/meeting?view=list">초기화</a>
                </c:if>
                <button class="ui-button button--secondary button--md" type="submit">검색</button>
            </div>
        </form>
        <c:choose>
            <c:when test="${not empty meetingList}">
                <div class="ui-table-wrap"
                     data-ui-return-list
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
                                    <c:if test="${not empty q}"><c:param name="returnQ" value="${q}" /></c:if>
                                    <c:if test="${not empty meetingType}"><c:param name="returnType" value="${meetingType}" /></c:if>
                                    <c:if test="${not empty author}"><c:param name="returnAuthor" value="${author}" /></c:if>
                                    <c:if test="${not empty startDate}"><c:param name="returnStartDate" value="${startDate}" /></c:if>
                                    <c:if test="${not empty endDate}"><c:param name="returnEndDate" value="${endDate}" /></c:if>
                                </c:url>
                                <fmt:formatDate var="meetingDateIso" value="${meeting.meetingDatetime}" pattern="yyyy-MM-dd'T'HH:mm" />
                                <fmt:formatDate var="meetingDateLabel" value="${meeting.meetingDatetime}" pattern="yyyy-MM-dd" />
                                <fmt:formatDate var="meetingTimeLabel" value="${meeting.meetingDatetime}" pattern="HH:mm" />
                                <tr class="ui-data-row"
                                    data-ui-return-row
                                    data-ui-return-key="<c:out value='${meeting.meetingId}' />"
                                    data-detail-url="<c:out value='${meetingViewUrl}' />">
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
                        <c:if test="${not empty q}"><c:param name="q" value="${q}" /></c:if>
                        <c:if test="${not empty meetingType}"><c:param name="type" value="${meetingType}" /></c:if>
                        <c:if test="${not empty author}"><c:param name="author" value="${author}" /></c:if>
                        <c:if test="${not empty startDate}"><c:param name="startDate" value="${startDate}" /></c:if>
                        <c:if test="${not empty endDate}"><c:param name="endDate" value="${endDate}" /></c:if>
                    </c:url>
                </c:if>
                <c:if test="${currentPage < totalPages}">
                    <c:url var="meetingNextPageUrl" value="/meeting">
                        <c:param name="view" value="list" />
                        <c:param name="page" value="${currentPage + 1}" />
                        <c:if test="${not empty q}"><c:param name="q" value="${q}" /></c:if>
                        <c:if test="${not empty meetingType}"><c:param name="type" value="${meetingType}" /></c:if>
                        <c:if test="${not empty author}"><c:param name="author" value="${author}" /></c:if>
                        <c:if test="${not empty startDate}"><c:param name="startDate" value="${startDate}" /></c:if>
                        <c:if test="${not empty endDate}"><c:param name="endDate" value="${endDate}" /></c:if>
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
                <div class="meeting-list-empty ui-empty-state">
                    <i class="fas fa-clipboard" aria-hidden="true"></i>
                    <c:choose>
                        <c:when test="${meetingFilterActive}">
                            <strong>검색 결과가 없습니다</strong>
                            <span>검색 조건을 바꿔 다시 확인해 주세요.</span>
                        </c:when>
                        <c:otherwise>
                            <strong>등록된 회의록이 없습니다</strong>
                            <span>첫 번째 회의록을 작성해보세요.</span>
                        </c:otherwise>
                    </c:choose>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>



<%@ include file="/includes/footer.jsp" %>
