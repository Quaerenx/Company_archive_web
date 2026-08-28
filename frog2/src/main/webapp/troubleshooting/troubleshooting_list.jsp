<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:set var="pageTitle" value="트러블 슈팅" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-troubleshooting" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/troubleshooting_list.css" scope="request" />
<%@ include file="/includes/header.jsp" %>


<div class="troubleshooting-management content-shell">
    <t:pageHeader>
        <jsp:attribute name="title"><i class="fas fa-tools" aria-hidden="true"></i> 트러블 슈팅</jsp:attribute>
        <jsp:attribute name="subtitle">기술지원 및 문제 해결 이력 <strong><c:out value="${totalCount}" /></strong>건</jsp:attribute>
        <jsp:attribute name="actions">
            <a href="${pageContext.request.contextPath}/troubleshooting?view=add"
               class="ui-button button--primary button--md">
                <i class="fas fa-plus" aria-hidden="true"></i>
                새 트러블 슈팅 등록
            </a>
        </jsp:attribute>
    </t:pageHeader>

    <t:flashMessages />

    <div class="ui-work-surface">
        <div class="ts-search-bar ui-table-toolbar">
            <form class="ts-search-form ui-form"
                  method="get"
                  action="${pageContext.request.contextPath}/troubleshooting"
                  data-ui-submit-lock="auto">
                <input type="hidden" name="view" value="list" />
                <input type="hidden" name="pageSize" value="<c:out value='${pageSize}' />" />
                <label class="sr-only" for="troubleshooting-search">트러블 슈팅 검색</label>
                <input type="text"
                       id="troubleshooting-search"
                       name="q"
                       value="<c:out value='${q}' />"
                       class="ts-search-input"
                       placeholder="제목, 고객사, 작성자 검색"
                       minlength="2"
                       maxlength="100"
                       autocomplete="off" />
                <label class="ts-search-scope">
                    <input type="checkbox"
                           name="scope"
                           value="content"
                           ${searchScope eq 'content' ? 'checked' : ''} />
                    본문 포함
                </label>
                <button type="submit"
                        class="btn-search-simple ui-button button--secondary button--md"
                        data-busy-label="검색 중">검색</button>
            </form>
        </div>
        <c:choose>
            <c:when test="${not empty troubleshootingList}">
        <div class="ui-table-wrap"
             data-ui-scroll-region
             data-ui-scroll-label="트러블슈팅 표">
        <table class="troubleshooting-table ui-table ui-data-table">
            <caption class="sr-only">트러블슈팅 목록</caption>
            <thead>
                <tr>
                    <th scope="col" class="col--customer">고객사</th>
                    <th scope="col" class="col--title">제목</th>
                    <th scope="col" class="col--date">발생일자</th>
                    <th scope="col" class="col--author">작성자</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="ts" items="${troubleshootingList}" varStatus="status">
                    <c:url var="troubleshootingViewUrl" value="/troubleshooting">
                        <c:param name="view" value="view" />
                        <c:param name="id" value="${ts.id}" />
                        <c:param name="returnQ" value="${q}" />
                        <c:if test="${searchScope eq 'content'}"><c:param name="returnScope" value="content" /></c:if>
                        <c:param name="returnPage" value="${currentPage}" />
                        <c:param name="returnPageSize" value="${pageSize}" />
                    </c:url>
                    <tr class="ui-data-row" data-detail-url="<c:out value='${troubleshootingViewUrl}' />">
                        <td class="col--customer"><c:out value="${ts.customerName}" /></td>
                        <td class="col--title">
                            <a href="<c:out value='${troubleshootingViewUrl}' />"
                               class="title-link">
                                <c:out value="${ts.title}" />
                            </a>
                            <div class="troubleshooting-row-meta">
                                <strong><c:out value="${ts.customerName}" /></strong>
                                <span>
                                    <c:choose>
                                        <c:when test="${not empty ts.occurrenceDate}">
                                            <fmt:formatDate value="${ts.occurrenceDate}" pattern="yyyy-MM-dd" />
                                        </c:when>
                                        <c:otherwise>-</c:otherwise>
                                    </c:choose>
                                </span>
                                <span><c:out value="${ts.creator}" /></span>
                            </div>
                        </td>
                        <td class="col--date">
                            <c:choose>
                                <c:when test="${not empty ts.occurrenceDate}">
                                    <fmt:formatDate value="${ts.occurrenceDate}" pattern="yyyy-MM-dd" />
                                </c:when>
                                <c:otherwise>-</c:otherwise>
                            </c:choose>
                        </td>
                        <td class="col--author"><c:out value="${ts.creator}" /></td>
                    </tr>
                </c:forEach>

            </tbody>
        </table>
        </div>
        <c:if test="${currentPage > 1}">
        <c:url var="troubleshootingPreviousPageUrl" value="/troubleshooting">
            <c:param name="view" value="list" />
            <c:param name="page" value="${currentPage - 1}" />
            <c:param name="pageSize" value="${pageSize}" />
            <c:if test="${not empty q}"><c:param name="q" value="${q}" /></c:if>
            <c:if test="${searchScope eq 'content'}"><c:param name="scope" value="content" /></c:if>
        </c:url>
        </c:if>
        <c:if test="${currentPage < totalPages}">
        <c:url var="troubleshootingNextPageUrl" value="/troubleshooting">
            <c:param name="view" value="list" />
            <c:param name="page" value="${currentPage + 1}" />
            <c:param name="pageSize" value="${pageSize}" />
            <c:if test="${not empty q}"><c:param name="q" value="${q}" /></c:if>
            <c:if test="${searchScope eq 'content'}"><c:param name="scope" value="content" /></c:if>
        </c:url>
        </c:if>
        <t:tableFooter itemLabel="트러블슈팅"
                       currentPage="${currentPage}"
                       totalPages="${totalPages}"
                       previousUrl="${troubleshootingPreviousPageUrl}"
                       nextUrl="${troubleshootingNextPageUrl}"
                       paginationLabel="트러블슈팅 페이지" />
            </c:when>
            <c:otherwise>
                <div class="troubleshooting-empty ui-empty-state">
                    <i class="fas fa-tools" aria-hidden="true"></i>
                    <strong>
                        <c:choose>
                            <c:when test="${not empty q}">검색 결과가 없습니다.</c:when>
                            <c:otherwise>등록된 트러블 슈팅이 없습니다.</c:otherwise>
                        </c:choose>
                    </strong>
                </div>
            </c:otherwise>
        </c:choose>
    </div>



<%@ include file="/includes/footer.jsp" %>
