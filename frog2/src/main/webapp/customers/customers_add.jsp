<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="pageTitle" value="새 고객사 추가" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/customers.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/customers_add.js" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>

<!-- 전체를 customer-add-page 클래스로 감싸기 -->
<div class="customer-add-page customer-form-page customer-management content-shell">
        <t:pageHeader>
            <jsp:attribute name="title">
                <i class="fas fa-plus-circle"></i> 새 고객사 등록
            </jsp:attribute>
            <jsp:attribute name="subtitle">
                새로운 고객사의 기본 정보를 입력해주세요.
            </jsp:attribute>
            <jsp:attribute name="actions">
                <a href="${pageContext.request.contextPath}/customers?view=list"
                   class="add-button ui-button button--secondary button--md">
                    <i class="fas fa-list"></i> 목록으로
                </a>
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
            <form class="ui-form ui-form-layout"
                  method="post"
                  action="${pageContext.request.contextPath}/customers"
                  data-ui-submit-lock="auto">
                <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
                <input type="hidden" name="action" value="add">
                
                <!-- 기본 정보 -->
                <div class="section-title">기본 정보</div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="customer_name">고객사명 <span class="required">*</span></label>
                        <input type="text" id="customer_name" name="customer_name" required>
                    </div>
                    <div class="form-group">
                        <label for="first_introduction_year">도입년도</label>
                        <input type="number" id="first_introduction_year" name="first_introduction_year" min="2000" max="2030">
                    </div>
                </div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="db_name">DB명</label>
                        <input type="text" id="db_name" name="db_name">
                    </div>
                    <div class="form-group">
                        <label for="vertica_version">Vertica 버전</label>
                        <input type="text" id="vertica_version" name="vertica_version">
                    </div>
                </div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="vertica_eos">EOS 일자</label>
                        <input type="date" id="vertica_eos" name="vertica_eos">
                    </div>
                    <div class="form-group">
                        <label for="mode">모드</label>
                        <select id="mode" name="mode">
                            <option value="">선택하세요</option>
                            <option value="ENT">ENT</option>
                            <option value="EON">EON</option>
                        </select>
                    </div>
                </div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="os">운영체제</label>
                        <input type="text" id="os" name="os">
                    </div>
                    <div class="form-group">
                        <label for="nodes">노드 수</label>
                        <input type="number" id="nodes" name="nodes" min="1">
                    </div>
                </div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="license_size">라이선스 크기</label>
                        <input type="text" id="license_size" name="license_size">
                    </div>
                    <div class="form-group">
                        <label for="said">SAID</label>
                        <input type="text" id="said" name="said">
                    </div>
                </div>
                
                <!-- 담당자 정보 -->
                <div class="section-title">담당자 정보</div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="manager_name">담당자</label>
                        <input type="text" id="manager_name" name="manager_name">
                    </div>
                    <div class="form-group">
                        <label for="sub_manager_name">부담당자</label>
                        <input type="text" id="sub_manager_name" name="sub_manager_name">
                    </div>
                </div>
                
                <!-- 시스템 구성 -->
                <div class="section-title">시스템 구성</div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="os_storage_config">스토리지 구성</label>
                        <textarea id="os_storage_config" name="os_storage_config" rows="3"></textarea>
                    </div>
                    <div class="form-group">
                        <label for="backup_config">백업 구성</label>
                        <textarea id="backup_config" name="backup_config" rows="3"></textarea>
                    </div>
                </div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="customer_type">고객 유형</label>
                        <select class="form-control" id="customer_type" name="customer_type">
                            <option value="">선택하세요</option>
                            <option value="정기점검 계약 고객사">정기점검 계약 고객사</option>
                            <option value="납품 계약 고객사">납품 계약 고객사</option>
                            <option value="유지보수 종료 고객사">유지보수 종료 고객사</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <!-- 빈 칸으로 두어 균형 맞춤 -->
                    </div>
                </div>
                
                <!-- 외부 솔루션 -->
                <div class="section-title">외부 솔루션</div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="etl_tool">ETL Tool</label>
                        <input type="text" id="etl_tool" name="etl_tool">
                    </div>
                    <div class="form-group">
                        <label for="bi_tool">BI Tool</label>
                        <input type="text" id="bi_tool" name="bi_tool">
                    </div>
                </div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="db_encryption">DB 암호화</label>
                        <input type="text" id="db_encryption" name="db_encryption">
                    </div>
                    <div class="form-group">
                        <label for="cdc_tool">CDC Tool</label>
                        <input type="text" id="cdc_tool" name="cdc_tool">
                    </div>
                </div>
                
                <!-- 비고 -->
                <div class="form-row">
                    <div class="form-group full-width">
                        <label for="note">비고</label>
                        <textarea id="note" name="note" rows="3"></textarea>
                    </div>
                </div>
                
                <!-- 버튼 -->
                <div class="button-group">
                    <a href="${pageContext.request.contextPath}/customers?view=list"
                       class="btn btn-cancel ui-button button--secondary button--md">취소</a>
                    <button type="submit"
                            class="btn btn-primary ui-button button--primary button--md"
                            data-busy-label="등록 중">등록하기</button>
                </div>
            </form>
        </div>
</div>


<%@ include file="/includes/footer.jsp" %>
