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
    <c:if test="${not empty param.returnPage}"><c:param name="page" value="${param.returnPage}" /></c:if>
    <c:if test="${not empty param.returnPageSize}"><c:param name="pageSize" value="${param.returnPageSize}" /></c:if>
</c:url>
<c:url var="troubleshootingEditUrl" value="/troubleshooting">
    <c:param name="view" value="edit" />
    <c:param name="id" value="${troubleshooting.id}" />
    <c:if test="${not empty param.returnQ}"><c:param name="returnQ" value="${param.returnQ}" /></c:if>
    <c:if test="${not empty param.returnPage}"><c:param name="returnPage" value="${param.returnPage}" /></c:if>
    <c:if test="${not empty param.returnPageSize}"><c:param name="returnPageSize" value="${param.returnPageSize}" /></c:if>
</c:url>

<div class="troubleshooting-detail content-shell">
    <t:pageHeader>
        <jsp:attribute name="title"><i class="fas fa-tools" aria-hidden="true"></i> <c:out value="${troubleshooting.title}" /></jsp:attribute>
        <jsp:attribute name="subtitle">트러블 슈팅 상세정보</jsp:attribute>
        <jsp:attribute name="actions">
            <a href="<c:out value='${troubleshootingListReturnUrl}' />"
               class="btn btn-secondary ui-button button--secondary button--md">
                <i class="fas fa-arrow-left" aria-hidden="true"></i>
                목록으로
            </a>
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
    
    <div class="detail-container ui-detail">
        <!-- 기본작성 항목 섹션 -->
        <div class="detail-section">
            <div class="detail-section-title">
                <i class="fas fa-info-circle"></i>
                기본작성 항목
            </div>
            <div class="detail-grid">
                <div class="detail-item">
                    <span class="detail-label">제목</span>
                    <span class="detail-value"><c:out value="${troubleshooting.title}" /></span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">고객사</span>
                    <span class="detail-value"><c:out value="${troubleshooting.customerName}" /></span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">고객사 담당자</span>
                    <span class="detail-value"><c:out value="${not empty troubleshooting.customerManager ? troubleshooting.customerManager : '-'}" /></span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">발생일자</span>
                    <span class="detail-value">
                        <c:choose>
                            <c:when test="${not empty troubleshooting.occurrenceDate}">
                                <fmt:formatDate value="${troubleshooting.occurrenceDate}" pattern="yyyy-MM-dd" />
                            </c:when>
                            <c:otherwise>-</c:otherwise>
                        </c:choose>
                    </span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">작업인원</span>
                    <span class="detail-value"><c:out value="${not empty troubleshooting.workPersonnel ? troubleshooting.workPersonnel : '-'}" /></span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">작업기간</span>
                    <span class="detail-value"><c:out value="${not empty troubleshooting.workPeriod ? troubleshooting.workPeriod : '-'}" /></span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">작성자</span>
                    <span class="detail-value"><c:out value="${troubleshooting.creator}" /></span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">작성일자</span>
                    <span class="detail-value">
                        <fmt:formatDate value="${troubleshooting.createDate}" pattern="yyyy-MM-dd HH:mm" />
                    </span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">지원형태</span>
                    <span class="detail-value"><c:out value="${not empty troubleshooting.supportType ? troubleshooting.supportType : '-'}" /></span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">케이스오픈 여부</span>
                    <span class="detail-value">
                        <c:choose>
                            <c:when test="${troubleshooting.caseOpenYn == 'Y'}">예</c:when>
                            <c:when test="${troubleshooting.caseOpenYn == 'N'}">아니오</c:when>
                            <c:otherwise>-</c:otherwise>
                        </c:choose>
                    </span>
                </div>
            </div>
        </div>
        
        <!-- 세부작성 항목 섹션 -->
        <div class="detail-section">
            <div class="detail-section-title">
                <i class="fas fa-list-alt"></i>
                세부작성 항목
            </div>
            <div class="detail-grid">
                <div class="detail-item full-width">
                    <span class="detail-label">개요</span>
                    <div class="detail-value">
                        <c:choose>
                            <c:when test="${not empty troubleshooting.overview}">
                                <c:out value="${troubleshooting.overview}" />
                            </c:when>
                            <c:otherwise>
                                <span class="empty-value">작성된 내용이 없습니다.</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
                
                <div class="detail-item full-width">
                    <span class="detail-label">원인</span>
                    <div class="detail-value">
                        <c:choose>
                            <c:when test="${not empty troubleshooting.causeAnalysis}">
                                <c:out value="${troubleshooting.causeAnalysis}" />
                            </c:when>
                            <c:otherwise>
                                <span class="empty-value">작성된 내용이 없습니다.</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
                
                <div class="detail-item full-width">
                    <span class="detail-label">에러내용</span>
                    <div class="detail-value">
                        <c:choose>
                            <c:when test="${not empty troubleshooting.errorContent}">
                                <c:out value="${troubleshooting.errorContent}" />
                            </c:when>
                            <c:otherwise>
                                <span class="empty-value">작성된 내용이 없습니다.</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
                
                <div class="detail-item full-width">
                    <span class="detail-label">조치내용</span>
                    <div class="detail-value">
                        <c:choose>
                            <c:when test="${not empty troubleshooting.actionTaken}">
                                <c:out value="${troubleshooting.actionTaken}" />
                            </c:when>
                            <c:otherwise>
                                <span class="empty-value">작성된 내용이 없습니다.</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
                
                <div class="detail-item full-width">
                    <span class="detail-label">스크립트</span>
                    <div class="detail-value">
                        <c:choose>
                            <c:when test="${not empty troubleshooting.scriptContent}">
                                <c:out value="${troubleshooting.scriptContent}" />
                            </c:when>
                            <c:otherwise>
                                <span class="empty-value">작성된 내용이 없습니다.</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
                
                <div class="detail-item full-width">
                    <span class="detail-label">비고</span>
                    <div class="detail-value">
                        <c:choose>
                            <c:when test="${not empty troubleshooting.note}">
                                <c:out value="${troubleshooting.note}" />
                            </c:when>
                            <c:otherwise>
                                <span class="empty-value">작성된 내용이 없습니다.</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>
            
            <c:if test="${not empty troubleshooting.updatedDate}">
                <div class="mt-4 pt-3 border-top text-right text-muted troubleshooting-updated-at">
                    최종 수정: <fmt:formatDate value="${troubleshooting.updatedDate}" pattern="yyyy-MM-dd HH:mm" />
                </div>
            </c:if>
            <c:if test="${canManageTroubleshooting}">
                <div class="section-actions">
                    <a href="<c:out value='${troubleshootingEditUrl}' />"
                       class="btn btn-ghost ui-button button--secondary button--sm">수정하기</a>
                    <button type="submit"
                            id="deleteTroubleshootingButton"
                            class="btn btn-ghost btn-ghost-danger ui-button button--danger button--sm"
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
        </div>
    </div>
</div>


<%@ include file="/includes/footer.jsp" %>
