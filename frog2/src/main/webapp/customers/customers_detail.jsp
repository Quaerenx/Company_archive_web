<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="pageTitle" value="고객사 상세정보" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/customers.css,/resources/css/pages/customer_detail.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/customer_detail.js" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>



<c:set var="currentCustomerName" value="${not empty customerDetail.customerName ? customerDetail.customerName : (not empty customer.customerName ? customer.customerName : '')}" />
<c:url var="currentCustomerEditUrl" value="/customers">
    <c:param name="view" value="editDetail" />
    <c:param name="customerName" value="${currentCustomerName}" />
    <c:if test="${not empty param.returnFilter}"><c:param name="returnFilter" value="${param.returnFilter}" /></c:if>
    <c:if test="${not empty param.returnSortField}"><c:param name="returnSortField" value="${param.returnSortField}" /></c:if>
    <c:if test="${not empty param.returnSortDirection}"><c:param name="returnSortDirection" value="${param.returnSortDirection}" /></c:if>
    <c:if test="${not empty param.returnQ}"><c:param name="returnQ" value="${param.returnQ}" /></c:if>
    <c:if test="${not empty param.returnPage}"><c:param name="returnPage" value="${param.returnPage}" /></c:if>
    <c:if test="${not empty param.returnPageSize}"><c:param name="returnPageSize" value="${param.returnPageSize}" /></c:if>
</c:url>
<c:url var="customerListReturnUrl" value="/customers">
    <c:param name="view" value="list" />
    <c:if test="${not empty param.returnFilter}"><c:param name="filter" value="${param.returnFilter}" /></c:if>
    <c:if test="${not empty param.returnSortField}"><c:param name="sortField" value="${param.returnSortField}" /></c:if>
    <c:if test="${not empty param.returnSortDirection}"><c:param name="sortDirection" value="${param.returnSortDirection}" /></c:if>
    <c:if test="${not empty param.returnQ}"><c:param name="q" value="${param.returnQ}" /></c:if>
    <c:if test="${not empty param.returnPage}"><c:param name="page" value="${param.returnPage}" /></c:if>
    <c:if test="${not empty param.returnPageSize}"><c:param name="pageSize" value="${param.returnPageSize}" /></c:if>
</c:url>
<c:url var="currentCustomerDetailUrl" value="/customers">
    <c:param name="view" value="detail" />
    <c:param name="customerName" value="${currentCustomerName}" />
</c:url>


<div class="customer-detail customer-detail--view customer-management content-management content-shell"
     data-context-path="<c:out value='${pageContext.request.contextPath}' />"
     data-customer-edit-url="<c:out value='${currentCustomerEditUrl}' />"
     data-quick-nav-recent-customer="<c:out value='${currentCustomerName}' />"
     data-quick-nav-recent-customer-url="<c:out value='${currentCustomerDetailUrl}' />">
    <t:pageHeader>
        <jsp:attribute name="title">
            <i class="fas fa-building"></i>
            <c:choose>
                <c:when test="${not empty customerDetail.customerName}">
                    <c:out value="${customerDetail.customerName}" />
                </c:when>
                <c:when test="${not empty customer.customerName}">
                    <c:out value="${customer.customerName}" />
                </c:when>
                <c:otherwise>
                    고객사
                </c:otherwise>
            </c:choose>
            상세정보
        </jsp:attribute>
        <jsp:attribute name="subtitle">
            고객사 정보 및 시스템 세부사항
        </jsp:attribute>
		<jsp:attribute name="actions">
			<div class="header-actions">
				<button type="button"
                        class="ui-button button--secondary button--sm customer-favorite-button"
                        data-customer-favorite
                        data-customer-name="<c:out value='${currentCustomerName}' />"
                        data-customer-url="<c:out value='${currentCustomerDetailUrl}' />"
                        aria-label="<c:out value='${currentCustomerName}' /> 즐겨찾기"
                        aria-pressed="false">
                    <i class="far fa-star" aria-hidden="true"></i>
                    <span>즐겨찾기</span>
                </button>
					<a href="<c:out value='${customerListReturnUrl}' />"
                   class="ui-button button--secondary button--sm">
					<i class="fas fa-arrow-left"></i> 목록으로
				</a>
			</div>
        </jsp:attribute>
        <jsp:attribute name="extra">
            <c:if test="${not empty customerDetail.updatedAt or not empty customerDetail.updatedBy}">
                <div class="customer-audit-meta text-muted">
                    <i class="fas fa-clock" aria-hidden="true"></i>
                    마지막 수정
                    <c:if test="${not empty customerDetail.updatedBy}">
                        · <c:out value="${customerDetail.updatedBy}" />
                    </c:if>
                    <c:if test="${not empty customerDetail.updatedAt}">
                        · <fmt:formatDate value="${customerDetail.updatedAt}"
                                          pattern="yyyy-MM-dd HH:mm" />
                    </c:if>
                </div>
            </c:if>
            <c:if test="${not empty verticaEosDate}">
                <div class="customer-eos-alert ui-alert ui-alert--${verticaEosNotice.tone}"
                     role="status"
                     aria-live="polite"
                     aria-atomic="true">
                    <i class="fas fa-exclamation-triangle"></i>
                    <span><c:out value="${verticaEosNotice.message}" />
                    (<strong><fmt:formatDate value="${verticaEosDate}" pattern="yyyy-MM-dd"/></strong>)</span>
                </div>
            </c:if>
        </jsp:attribute>
    </t:pageHeader>

    <t:flashMessages />

    <c:set var="hasAnyDetail" value="${not empty customerDetail or not empty customerDetailStg or not empty customerDetailDev}" />
    <c:if test="${hasAnyDetail}">
        <div class="detail-container env-tabs ui-work-surface">
            <div class="tab-nav" role="tablist" aria-label="고객사 환경">
                <button type="button" id="env-prod-tab" class="tab-btn ui-touch-target active"
                        role="tab" aria-selected="true" aria-controls="env-prod" tabindex="0"
                        data-target="env-prod">운영</button>
                <button type="button" id="env-stg-tab" class="tab-btn ui-touch-target"
                        role="tab" aria-selected="false" aria-controls="env-stg" tabindex="-1"
                        data-target="env-stg">스테이징</button>
                <button type="button" id="env-dev-tab" class="tab-btn ui-touch-target"
                        role="tab" aria-selected="false" aria-controls="env-dev" tabindex="-1"
                        data-target="env-dev">개발</button>
                <span class="tab-indicator" aria-hidden="true"></span>
            </div>
            <div class="tab-panel active" id="env-prod" role="tabpanel" aria-labelledby="env-prod-tab">
                <c:if test="${empty customerDetail}">
                    <div class="ui-alert ui-alert--neutral">운영 환경 데이터가 없습니다.</div>
                </c:if>
                <c:if test="${not empty customerDetail}">
                    <c:set var="detail" value="${customerDetail}" />
                    <%@ include file="/customers/_detail_sections.jspf" %>
                </c:if>
            </div>
            <div class="tab-panel" id="env-stg" role="tabpanel" aria-labelledby="env-stg-tab" hidden>
        <c:if test="${empty customerDetailStg}">
            <div class="ui-alert ui-alert--neutral">스테이징 환경 데이터가 없습니다.</div>
        </c:if>
        <c:if test="${not empty customerDetailStg}">
            <c:set var="detail" value="${customerDetailStg}" />
            <%@ include file="/customers/_detail_sections.jspf" %>
        </c:if>
    </div>
    <div class="tab-panel" id="env-dev" role="tabpanel" aria-labelledby="env-dev-tab" hidden>
        <c:if test="${empty customerDetailDev}">
            <div class="ui-alert ui-alert--neutral">개발 환경 데이터가 없습니다.</div>
        </c:if>
        <c:if test="${not empty customerDetailDev}">
            <c:set var="detail" value="${customerDetailDev}" />
            <%@ include file="/customers/_detail_sections.jspf" %>
        </c:if>
    </div>
    </div>
    </c:if>

    <c:if test="${empty customerDetail and not empty customer}">
        <div class="detail-container ui-work-surface">
            <div class="detail-section text-center p-5">
                <i class="fas fa-info-circle text-warning customer-empty-icon"></i>
                <h3 class="text-dark mb-3">상세정보가 등록되지 않았습니다</h3>
                <p class="text-muted mb-4">
                    <c:out value="${customer.customerName}" />의 기본 정보는 있지만 상세정보가 등록되지 않았습니다.<br>
                    상세정보를 등록하려면 수정 페이지에서 추가해 주세요.
                </p>
                <div class="d-flex gap-3 justify-content-center">
                    <a href="<c:out value='${customerListReturnUrl}' />"
                       class="ui-button button--secondary button--md">
                        <i class="fas fa-arrow-left"></i>
                        목록으로 돌아가기
                    </a>
				    <a href="<c:out value='${currentCustomerEditUrl}' />"
                       class="ui-button button--secondary button--md">
				        <i class="fas fa-edit"></i>
				        정보 수정하기
				    </a>
                </div>
            </div>
        </div>
    </c:if>

    <c:if test="${empty customer and not hasAnyDetail}">
        <div class="detail-container ui-work-surface">
            <div class="detail-section text-center p-5">
                <i class="fas fa-exclamation-triangle text-danger customer-empty-icon"></i>
                <h3 class="text-dark mb-3">고객사 정보를 찾을 수 없습니다</h3>
                <p class="text-muted mb-4">요청하신 고객사 정보가 존재하지 않거나 삭제되었을 수 있습니다.</p>
                <a href="<c:out value='${customerListReturnUrl}' />"
                   class="ui-button button--secondary button--md">
                    <i class="fas fa-arrow-left"></i>
                    목록으로 돌아가기
                </a>
            </div>
        </div>
    </c:if>

    <c:if test="${not empty customer}">
        <c:url var="maintenanceHistoryUrl" value="/maintenance">
            <c:param name="view" value="history" />
            <c:param name="customerName" value="${currentCustomerName}" />
        </c:url>
        <c:url var="maintenanceAddUrl" value="/maintenance">
            <c:param name="view" value="add" />
            <c:param name="customerName" value="${currentCustomerName}" />
        </c:url>
        <c:url var="customerHistoryUrl" value="/customer-history">
            <c:param name="customerName" value="${currentCustomerName}" />
        </c:url>
        <c:url var="customerHistoryAddUrl" value="/customer-history">
            <c:param name="view" value="add" />
            <c:param name="customerName" value="${currentCustomerName}" />
        </c:url>
        <c:url var="troubleshootingUrl" value="/troubleshooting">
            <c:param name="view" value="list" />
            <c:param name="q" value="${currentCustomerName}" />
        </c:url>
        <c:url var="troubleshootingAddUrl" value="/troubleshooting">
            <c:param name="view" value="add" />
            <c:param name="customerName" value="${currentCustomerName}" />
        </c:url>

        <section class="customer-activity ui-work-surface"
                 aria-labelledby="customerActivityTitle">
            <div class="customer-activity__header">
                <div>
                    <h2 id="customerActivityTitle">최근 업무</h2>
                    <p>점검, 작업 이력, 트러블슈팅을 고객사 기준으로 모아봅니다.</p>
                </div>
                <div class="customer-activity__actions" aria-label="업무 등록">
                    <a class="ui-button button--secondary button--sm"
                       href="<c:out value='${maintenanceAddUrl}' />">점검 등록</a>
                    <a class="ui-button button--secondary button--sm"
                       href="<c:out value='${customerHistoryAddUrl}' />">히스토리 등록</a>
                    <a class="ui-button button--secondary button--sm"
                       href="<c:out value='${troubleshootingAddUrl}' />">트러블슈팅 작성</a>
                </div>
            </div>
            <div class="customer-activity__grid">
                <section class="customer-activity__group">
                    <div class="customer-activity__group-header">
                        <h3>정기점검</h3>
                        <a href="<c:out value='${maintenanceHistoryUrl}' />">전체 보기</a>
                    </div>
                    <c:choose>
                        <c:when test="${not empty customerActivity.maintenanceRecords}">
                            <ul class="customer-activity__list">
                                <c:forEach var="record" items="${customerActivity.maintenanceRecords}">
                                    <li>
                                        <a href="<c:out value='${maintenanceHistoryUrl}' />">
                                            <strong><fmt:formatDate value="${record.inspectionDate}" pattern="yyyy-MM-dd" /></strong>
                                            <span><c:out value="${not empty record.verticaVersion ? record.verticaVersion : '버전 미등록'}" /></span>
                                        </a>
                                    </li>
                                </c:forEach>
                            </ul>
                        </c:when>
                        <c:otherwise><p class="customer-activity__empty">최근 점검이 없습니다.</p></c:otherwise>
                    </c:choose>
                </section>

                <section class="customer-activity__group">
                    <div class="customer-activity__group-header">
                        <h3>고객사 히스토리</h3>
                        <a href="<c:out value='${customerHistoryUrl}' />">전체 보기</a>
                    </div>
                    <c:choose>
                        <c:when test="${not empty customerActivity.historyRecords}">
                            <ul class="customer-activity__list">
                                <c:forEach var="record" items="${customerActivity.historyRecords}">
                                    <c:url var="historyDetailUrl" value="/customer-history">
                                        <c:param name="view" value="detail" />
                                        <c:param name="id" value="${record.id}" />
                                    </c:url>
                                    <li>
                                        <a href="<c:out value='${historyDetailUrl}' />">
                                            <strong><c:out value="${record.workDate}" /></strong>
                                            <span><c:out value="${record.title}" /></span>
                                        </a>
                                    </li>
                                </c:forEach>
                            </ul>
                        </c:when>
                        <c:otherwise><p class="customer-activity__empty">최근 히스토리가 없습니다.</p></c:otherwise>
                    </c:choose>
                </section>

                <section class="customer-activity__group">
                    <div class="customer-activity__group-header">
                        <h3>트러블슈팅</h3>
                        <a href="<c:out value='${troubleshootingUrl}' />">전체 보기</a>
                    </div>
                    <c:choose>
                        <c:when test="${not empty customerActivity.troubleshootingRecords}">
                            <ul class="customer-activity__list">
                                <c:forEach var="record" items="${customerActivity.troubleshootingRecords}">
                                    <c:url var="troubleDetailUrl" value="/troubleshooting">
                                        <c:param name="view" value="view" />
                                        <c:param name="id" value="${record.id}" />
                                    </c:url>
                                    <li>
                                        <a href="<c:out value='${troubleDetailUrl}' />">
                                            <strong><fmt:formatDate value="${record.occurrenceDate}" pattern="yyyy-MM-dd" /></strong>
                                            <span><c:out value="${record.title}" /></span>
                                        </a>
                                    </li>
                                </c:forEach>
                            </ul>
                        </c:when>
                        <c:otherwise><p class="customer-activity__empty">최근 트러블슈팅이 없습니다.</p></c:otherwise>
                    </c:choose>
                </section>
            </div>
        </section>
    </c:if>

    <c:if test="${not empty customer}">
        <div class="detail-actions">
            <a href="<c:out value='${currentCustomerEditUrl}' />"
               id="editCustomerButton" data-customer-name="<c:out value='${currentCustomerName}' />"
               class="ui-button button--secondary button--sm">
                <i class="fas fa-edit"></i> 정보수정
            </a>
            <button type="button"
                    id="deleteCustomerButton"
                    data-customer-name="<c:out value='${currentCustomerName}' />"
                    class="ui-button button--danger button--sm"
                    data-busy-label="삭제 중">
                <i class="fas fa-trash"></i> 고객사 삭제
            </button>
        </div>
    </c:if>
</div>

<%@ include file="/includes/footer.jsp" %>
