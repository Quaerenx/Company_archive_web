<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:set var="pageTitle" value="업무 인박스" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-work-inbox" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/work_inbox.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/work_inbox.js" scope="request" />
<%@ include file="/includes/header.jsp" %>

<div class="work-inbox-page content-shell"
     data-work-inbox
     data-user-id="<c:out value='${workInboxUserId}' />">
    <t:pageHeader>
        <jsp:attribute name="title">업무 인박스</jsp:attribute>
        <jsp:attribute name="subtitle">담당 고객사의 위험과 누락을 원본 데이터 기준으로 확인합니다.</jsp:attribute>
        <jsp:attribute name="actions">
            <a class="ui-button button--secondary button--sm"
               href="${pageContext.request.contextPath}/mypage">마이페이지로</a>
        </jsp:attribute>
    </t:pageHeader>

    <section class="work-inbox-page__surface ui-work-surface">
        <form class="work-inbox-filter ui-table-toolbar ui-form"
              data-work-inbox-filter>
            <div class="ui-form-field">
                <label for="workInboxSeverity">위험도</label>
                <select id="workInboxSeverity" name="severity">
                    <option value="all">전체 위험도</option>
                    <option value="danger">위험</option>
                    <option value="warning">확인 필요</option>
                    <option value="neutral">정보 누락</option>
                </select>
            </div>
            <div class="ui-form-field">
                <label for="workInboxType">항목 유형</label>
                <select id="workInboxType" name="type">
                    <option value="all">전체 유형</option>
                    <option value="maintenance">정기점검 미진행</option>
                    <option value="license">라이선스 위험</option>
                    <option value="missing-info">정보 누락</option>
                </select>
            </div>
            <div class="ui-form-field">
                <label for="workInboxStatus">상태</label>
                <select id="workInboxStatus" name="status">
                    <option value="active">확인 필요</option>
                    <option value="deferred">보류</option>
                    <option value="all">전체</option>
                </select>
            </div>
            <div class="ui-form-field work-inbox-filter__search">
                <label for="workInboxCustomer">고객사</label>
                <input id="workInboxCustomer" name="customer" type="search"
                       placeholder="고객사명 검색" autocomplete="off" />
            </div>
            <button class="ui-button button--secondary button--md"
                    type="reset">초기화</button>
        </form>

        <div class="work-inbox-page__summary" aria-live="polite">
            <strong data-work-inbox-visible-count><c:out value="${workInbox.totalCount}" /></strong>건 표시
            <span>· 원본 정보가 보완되면 항목은 자동으로 사라집니다.</span>
            <span>· 보류 설정은 현재 브라우저에만 저장됩니다.</span>
        </div>

        <c:choose>
            <c:when test="${not empty workInbox.items}">
                <ul class="work-inbox-page__list" data-work-inbox-list>
                    <c:forEach var="item" items="${workInbox.items}">
                        <c:url var="itemUrl" value="${item.path}" />
                        <li class="work-inbox-card"
                            data-work-inbox-item
                            data-item-key="<c:out value='${item.itemKey}' />"
                            data-severity="<c:out value='${item.tone}' />"
                            data-type="<c:out value='${item.typeCode}' />"
                            data-customer="<c:out value='${item.customerName}' />">
                            <div class="work-inbox-card__main">
                                <span class="ui-badge ui-badge--<c:out value='${item.tone}' />">
                                    <c:out value="${item.severityLabel}" />
                                </span>
                                <div class="work-inbox-card__content">
                                    <div class="work-inbox-card__meta">
                                        <strong><c:out value="${item.customerName}" /></strong>
                                        <span><c:out value="${item.typeLabel}" /></span>
                                        <span><c:out value="${item.timelineLabel}" /></span>
                                    </div>
                                    <h2><c:out value="${item.title}" /></h2>
                                    <p><c:out value="${item.detail}" /></p>
                                    <p class="work-inbox-card__dates">
                                        <c:if test="${not empty item.referenceDate}">발생·기준일 <c:out value="${item.referenceDate}" /></c:if>
                                        <c:if test="${not empty item.dueDate}"> · 기한 <c:out value="${item.dueDate}" /></c:if>
                                    </p>
                                    <p class="work-inbox-card__deferred" data-deferred-copy hidden></p>
                                </div>
                            </div>
                            <div class="work-inbox-card__actions">
                                <a class="ui-button button--primary button--sm"
                                   href="<c:out value='${itemUrl}' />"><c:out value="${item.actionLabel}" /></a>
                                <details class="work-inbox-card__defer" data-defer-panel>
                                    <summary class="ui-button button--secondary button--sm">보류</summary>
                                    <form data-defer-form>
                                        <label>보류 사유
                                            <input name="reason" maxlength="200" required />
                                        </label>
                                        <label>보류 기한
                                            <input name="until" type="date" required />
                                        </label>
                                        <button class="ui-button button--secondary button--sm" type="submit">보류 저장</button>
                                    </form>
                                </details>
                                <button class="ui-button button--secondary button--sm"
                                        type="button" data-resume hidden>보류 해제</button>
                            </div>
                        </li>
                    </c:forEach>
                </ul>
                <div class="work-inbox-page__empty ui-empty-state" data-filter-empty hidden>
                    <strong>조건에 맞는 항목이 없습니다.</strong>
                    <span>필터를 바꾸거나 보류 상태를 확인해 주세요.</span>
                </div>
            </c:when>
            <c:otherwise>
                <div class="work-inbox-page__empty ui-empty-state">
                    <strong>현재 확인할 담당 고객사 항목이 없습니다.</strong>
                    <span>원본 데이터에서 새 위험이나 누락이 생기면 여기에 표시됩니다.</span>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</div>

<%@ include file="/includes/footer.jsp" %>
