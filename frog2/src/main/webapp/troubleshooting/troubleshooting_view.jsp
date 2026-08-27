<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:set var="pageTitle" value="트러블 슈팅 상세보기" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-troubleshooting" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/troubleshooting_view.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/troubleshooting_view.js" scope="request" />
<%@ include file="/includes/header.jsp" %>

<c:url var="troubleshootingListReturnUrl" value="/troubleshooting">
    <c:param name="view" value="list" />
    <c:if test="${not empty param.returnQ}"><c:param name="q" value="${param.returnQ}" /></c:if>
    <c:if test="${param.returnScope eq 'content'}"><c:param name="scope" value="content" /></c:if>
    <c:if test="${not empty param.returnPage}"><c:param name="page" value="${param.returnPage}" /></c:if>
    <c:if test="${not empty param.returnPageSize}"><c:param name="pageSize" value="${param.returnPageSize}" /></c:if>
</c:url>
<c:url var="troubleshootingEditUrl" value="/troubleshooting">
    <c:param name="view" value="edit" />
    <c:param name="id" value="${troubleshooting.id}" />
    <c:if test="${not empty param.returnQ}"><c:param name="returnQ" value="${param.returnQ}" /></c:if>
    <c:if test="${param.returnScope eq 'content'}"><c:param name="returnScope" value="content" /></c:if>
    <c:if test="${not empty param.returnPage}"><c:param name="returnPage" value="${param.returnPage}" /></c:if>
    <c:if test="${not empty param.returnPageSize}"><c:param name="returnPageSize" value="${param.returnPageSize}" /></c:if>
</c:url>
<c:set var="troubleshootingOccurrenceDateLabel" value="발생일 미등록" />
<c:if test="${not empty troubleshooting.occurrenceDate}">
    <fmt:formatDate var="troubleshootingOccurrenceDateLabel"
                    value="${troubleshooting.occurrenceDate}"
                    pattern="yyyy-MM-dd" />
</c:if>
<c:set var="troubleshootingCustomerLabel"
       value="${not empty troubleshooting.customerName ? troubleshooting.customerName : '고객사 미등록'}" />
<c:set var="troubleshootingSupportLabel"
       value="${not empty troubleshooting.supportType ? troubleshooting.supportType : '지원 형태 미등록'}" />

<div class="troubleshooting-detail content-shell">
    <t:pageHeader>
        <jsp:attribute name="title"><i class="fas fa-tools" aria-hidden="true"></i> <c:out value="${troubleshooting.title}" /></jsp:attribute>
        <jsp:attribute name="subtitle">
            <span><c:out value="${troubleshootingCustomerLabel}" /></span>
            <span aria-hidden="true">·</span>
            <span><c:out value="${troubleshootingOccurrenceDateLabel}" /></span>
            <span aria-hidden="true">·</span>
            <span><c:out value="${troubleshootingSupportLabel}" /></span>
        </jsp:attribute>
        <jsp:attribute name="actions">
            <a href="<c:out value='${troubleshootingListReturnUrl}' />"
               class="ui-button button--secondary button--md">
                <i class="fas fa-arrow-left" aria-hidden="true"></i>
                목록으로
            </a>
        </jsp:attribute>
    </t:pageHeader>
    
    <t:flashMessages />
    
    <article class="detail-container ui-detail troubleshooting-report ui-work-surface">
        <section class="detail-section troubleshooting-meta-section"
                 aria-labelledby="troubleshooting-meta-title">
            <h2 id="troubleshooting-meta-title" class="detail-section-title">
                <i class="fas fa-info-circle" aria-hidden="true"></i>
                기본 정보
            </h2>
            <dl class="troubleshooting-meta-grid">
                <div class="troubleshooting-meta-item">
                    <dt>고객사</dt>
                    <dd><c:out value="${troubleshootingCustomerLabel}" /></dd>
                </div>
                <div class="troubleshooting-meta-item">
                    <dt>고객사 담당자</dt>
                    <dd><c:out value="${not empty troubleshooting.customerManager ? troubleshooting.customerManager : '-'}" /></dd>
                </div>
                <div class="troubleshooting-meta-item">
                    <dt>발생일</dt>
                    <dd><c:out value="${troubleshootingOccurrenceDateLabel}" /></dd>
                </div>
                <div class="troubleshooting-meta-item">
                    <dt>지원 형태</dt>
                    <dd><c:out value="${troubleshootingSupportLabel}" /></dd>
                </div>
                <div class="troubleshooting-meta-item">
                    <dt>케이스 오픈</dt>
                    <dd>
                        <c:choose>
                            <c:when test="${troubleshooting.caseOpenYn == 'Y'}">예</c:when>
                            <c:when test="${troubleshooting.caseOpenYn == 'N'}">아니오</c:when>
                            <c:otherwise>-</c:otherwise>
                        </c:choose>
                    </dd>
                </div>
                <div class="troubleshooting-meta-item">
                    <dt>작업 인원</dt>
                    <dd><c:out value="${not empty troubleshooting.workPersonnel ? troubleshooting.workPersonnel : '-'}" /></dd>
                </div>
                <div class="troubleshooting-meta-item">
                    <dt>작업 기간</dt>
                    <dd><c:out value="${not empty troubleshooting.workPeriod ? troubleshooting.workPeriod : '-'}" /></dd>
                </div>
                <div class="troubleshooting-meta-item">
                    <dt>작성자</dt>
                    <dd><c:out value="${troubleshooting.creator}" default="-" /></dd>
                </div>
                <div class="troubleshooting-meta-item">
                    <dt>작성 일시</dt>
                    <dd><fmt:formatDate value="${troubleshooting.createDate}" pattern="yyyy-MM-dd HH:mm" /></dd>
                </div>
            </dl>
        </section>

        <section class="detail-section troubleshooting-report-body"
                 aria-labelledby="troubleshooting-report-title">
            <h2 id="troubleshooting-report-title" class="detail-section-title">
                <i class="fas fa-list-alt" aria-hidden="true"></i>
                문제 해결 기록
            </h2>
            <div class="troubleshooting-report-sections">
                <section class="troubleshooting-report-section" aria-labelledby="troubleshooting-overview-title">
                    <h3 id="troubleshooting-overview-title">개요</h3>
                    <c:choose>
                        <c:when test="${not empty troubleshooting.overview}">
                            <div class="troubleshooting-report-text"><c:out value="${troubleshooting.overview}" /></div>
                        </c:when>
                        <c:otherwise><p class="troubleshooting-report-empty">기록 없음</p></c:otherwise>
                    </c:choose>
                </section>

                <section class="troubleshooting-report-section" aria-labelledby="troubleshooting-error-title">
                    <h3 id="troubleshooting-error-title">에러 및 증상</h3>
                    <c:choose>
                        <c:when test="${not empty troubleshooting.errorContent}">
                            <div class="troubleshooting-report-text"><c:out value="${troubleshooting.errorContent}" /></div>
                        </c:when>
                        <c:otherwise><p class="troubleshooting-report-empty">기록 없음</p></c:otherwise>
                    </c:choose>
                </section>

                <section class="troubleshooting-report-section" aria-labelledby="troubleshooting-cause-title">
                    <h3 id="troubleshooting-cause-title">원인</h3>
                    <c:choose>
                        <c:when test="${not empty troubleshooting.causeAnalysis}">
                            <div class="troubleshooting-report-text"><c:out value="${troubleshooting.causeAnalysis}" /></div>
                        </c:when>
                        <c:otherwise><p class="troubleshooting-report-empty">기록 없음</p></c:otherwise>
                    </c:choose>
                </section>

                <section class="troubleshooting-report-section" aria-labelledby="troubleshooting-action-title">
                    <h3 id="troubleshooting-action-title">조치 내용</h3>
                    <c:choose>
                        <c:when test="${not empty troubleshooting.actionTaken}">
                            <div class="troubleshooting-report-text"><c:out value="${troubleshooting.actionTaken}" /></div>
                        </c:when>
                        <c:otherwise><p class="troubleshooting-report-empty">기록 없음</p></c:otherwise>
                    </c:choose>
                </section>

                <section class="troubleshooting-report-section troubleshooting-report-section--script"
                         aria-labelledby="troubleshooting-script-title">
                    <div class="troubleshooting-report-section-header">
                        <h3 id="troubleshooting-script-title">스크립트</h3>
                        <c:if test="${not empty troubleshooting.scriptContent}">
                            <button type="button"
                                    class="ui-button button--secondary button--sm troubleshooting-copy-button"
                                    data-copy-target="troubleshooting-script-content">
                                <i class="far fa-copy" aria-hidden="true"></i>
                                <span data-copy-label>복사</span>
                            </button>
                        </c:if>
                    </div>
                    <c:choose>
                        <c:when test="${not empty troubleshooting.scriptContent}">
                            <pre class="troubleshooting-code-block" tabindex="0" aria-labelledby="troubleshooting-script-title"><code id="troubleshooting-script-content"><c:out value="${troubleshooting.scriptContent}" /></code></pre>
                        </c:when>
                        <c:otherwise><p class="troubleshooting-report-empty">기록 없음</p></c:otherwise>
                    </c:choose>
                </section>

                <section class="troubleshooting-report-section" aria-labelledby="troubleshooting-note-title">
                    <h3 id="troubleshooting-note-title">비고</h3>
                    <c:choose>
                        <c:when test="${not empty troubleshooting.note}">
                            <div class="troubleshooting-report-text"><c:out value="${troubleshooting.note}" /></div>
                        </c:when>
                        <c:otherwise><p class="troubleshooting-report-empty">기록 없음</p></c:otherwise>
                    </c:choose>
                </section>
            </div>
            
            <c:if test="${not empty troubleshooting.updatedDate}">
                <div class="troubleshooting-updated-at">
                    최종 수정 <fmt:formatDate value="${troubleshooting.updatedDate}" pattern="yyyy-MM-dd HH:mm" />
                </div>
            </c:if>
            <c:if test="${canManageTroubleshooting}">
                <div class="section-actions">
                    <a href="<c:out value='${troubleshootingEditUrl}' />"
                       class="ui-button button--ghost button--sm">수정하기</a>
                    <button type="submit"
                            id="deleteTroubleshootingButton"
                            class="ui-button button--ghost button--ghost-danger button--sm"
                            form="deleteTroubleshootingForm"
                            data-busy-label="삭제 중">삭제하기</button>
                </div>
                <form id="deleteTroubleshootingForm"
                      class="ui-form"
                      method="post"
                      action="${pageContext.request.contextPath}/troubleshooting"
                      data-ui-submit-lock="auto"
                      hidden>
                    <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
                    <input type="hidden" name="action" value="delete">
                    <input type="hidden" name="id" value="<c:out value='${troubleshooting.id}' />">
                </form>
            </c:if>
        </section>
    </article>
</div>


<%@ include file="/includes/footer.jsp" %>
