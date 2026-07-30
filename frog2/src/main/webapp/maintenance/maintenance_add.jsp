<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="pageTitle" value="정기점검 이력 추가" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-maintenance" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/customers.css,/resources/css/pages/maintenance.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/maintenance_form.js" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>

<c:url var="customerHistoryUrl" value="/maintenance">
    <c:param name="view" value="history" />
    <c:param name="customerName" value="${customerName}" />
</c:url>

<!-- 전체를 maintenance-add-page 클래스로 감싸기 -->
<div class="maintenance-add-page"
     data-maintenance-form-mode="add"
     data-context-path="<c:out value='${pageContext.request.contextPath}' />">
    <div class="container content-shell">
        <t:pageHeader>
            <jsp:attribute name="title"><i class="fas fa-plus-circle"></i> 새 정기점검 이력 등록</jsp:attribute>
            <jsp:attribute name="subtitle">
                <c:if test="${not empty customerName}"><strong><c:out value="${customerName}" /></strong>의 정기점검 이력을 입력해주세요.</c:if>
            </jsp:attribute>
            <jsp:attribute name="actions">
                <c:choose>
                    <c:when test="${not empty customerName}">
                        <a href="<c:out value='${customerHistoryUrl}' />"
                           class="add-button secondary ui-button button--secondary button--md"><i class="fas fa-history"></i> 이력으로</a>
                    </c:when>
                    <c:otherwise>
                        <a href="${pageContext.request.contextPath}/maintenance?view=cards"
                           class="add-button secondary ui-button button--secondary button--md"><i class="fas fa-list"></i> 카드로</a>
                    </c:otherwise>
                </c:choose>
            </jsp:attribute>
        </t:pageHeader>

        <!-- 오류 메시지 -->
        <c:if test="${not empty error}">
            <div class="alert alert-danger ui-alert ui-alert--danger"
                 role="alert"
                 aria-atomic="true">
                <i class="fas fa-exclamation-circle"></i> <c:out value="${error}" />
            </div>
        </c:if>

        <!-- 등록 폼 -->
        <div class="form-container ui-form-card">
            <form id="maintenanceForm"
                  class="ui-form ui-form-layout ui-form-layout--actions-end"
                  method="post"
                  action="${pageContext.request.contextPath}/maintenance"
                  data-ui-submit-lock="auto">
                <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
                <input type="hidden" name="action" value="add">

                <!-- 기본 정보 -->
                <div class="section-title">기본 정보</div>

                <div class="form-row">
                    <div class="form-group">
                        <label for="customer_name">고객사명 <span class="required">*</span></label>
                        <c:choose>
                            <c:when test="${not empty customerName}">
                                <input type="text" id="customer_name" name="customer_name"
                                       value="<c:out value='${customerName}' />" readonly class="readonly-field">
                            </c:when>
                            <c:otherwise>
                                <select id="customer_name" name="customer_name" required>
                                    <option value="">고객사를 선택하세요</option>
                                </select>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <div class="form-group">
                        <label for="inspector_name">점검자 <span class="required">*</span></label>
                        <select id="inspector_name" name="inspector_name" required>
                            <option value="">점검자를 선택하세요</option>
                        </select>
                    </div>
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label for="inspection_date">점검일자 <span class="required">*</span></label>
                        <input type="date" id="inspection_date" name="inspection_date" required>
                    </div>
                    <div class="form-group">
                        <label for="vertica_version">Vertica 버전</label>
                        <input type="text" id="vertica_version" name="vertica_version" placeholder="예: 12.0.4">
                    </div>
                </div>

                <!-- 라이선스 정보 -->
                <div class="section-title">라이선스 정보 (선택)</div>
                <div class="form-row">
                    <div class="form-group">
                        <label for="license_size_gb">라이선스 크기 (TB)</label>
                        <!-- varchar(50)로 변경됨: 자유 입력, 최대 50자 -->
                        <input type="text" id="license_size_gb" name="license_size_gb" maxlength="50" placeholder="예: 4 또는 4TB">
                    </div>
                    <div class="form-group">
                        <label for="license_usage_size">라이선스 사용량 (TB)</label>
                        <input type="text" id="license_usage_size" name="license_usage_size" maxlength="50" placeholder="예: 3.5 또는 3.5TB">
                    </div>
                    <div class="form-group">
                        <label for="license_usage_pct">라이선스 사용률 (%)</label>
                        <input type="text" id="license_usage_pct" name="license_usage_pct" maxlength="50" placeholder="예: 75 또는 75%">
                    </div>
                </div>

                <!-- 점검 내용 -->
                <div class="section-title">점검 내용</div>

                <div class="form-row">
                    <div class="form-group full-width">
                        <label for="note">비고 및 점검 내용</label>
                        <textarea id="note" name="note" rows="8" placeholder="점검 내용, 발견된 이슈, 조치사항 등을 입력해주세요."></textarea>
                    </div>
                </div>

                <!-- 버튼 -->
                <div class="button-group">
                    <c:choose>
                        <c:when test="${not empty customerName}">
                            <a href="<c:out value='${customerHistoryUrl}' />"
                               class="btn btn-cancel ui-button button--secondary button--md">취소</a>
                        </c:when>
                        <c:otherwise>
                            <a href="${pageContext.request.contextPath}/maintenance?view=cards"
                               class="btn btn-cancel ui-button button--secondary button--md">취소</a>
                        </c:otherwise>
                    </c:choose>
                    <button type="submit"
                            class="btn btn-primary ui-button button--primary button--md"
                            data-busy-label="등록 중">등록하기</button>
                </div>
            </form>
        </div>
    </div>
</div>



<%@ include file="/includes/footer.jsp" %>
