<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="pageTitle" value="고객사 상세정보 수정" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/customers.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/customer_detail_edit.js" scope="request" />
<c:set var="currentCustomerName" value="${not empty customerDetail.customerName ? customerDetail.customerName : customer.customerName}" />
<c:url var="customerDetailUrl" value="/customers">
    <c:param name="view" value="detail" />
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

<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>


<div class="customer-detail customer-management content-management content-shell">
    <t:pageHeader>
        <jsp:attribute name="title">
            <i class="fas fa-edit"></i>
            <c:choose>
                <c:when test="${not empty customerDetail.customerName}"><c:out value="${customerDetail.customerName}" /></c:when>
                <c:when test="${not empty customer.customerName}"><c:out value="${customer.customerName}" /></c:when>
                <c:otherwise>고객사</c:otherwise>
            </c:choose>
            상세정보 수정
        </jsp:attribute>
        <jsp:attribute name="subtitle">고객사 상세정보를 수정하세요</jsp:attribute>
        <jsp:attribute name="actions">
            <a href="<c:out value='${customerDetailUrl}' />"
               class="add-button secondary ui-button button--secondary button--md">
                <i class="fas fa-info-circle"></i> 상세보기
            </a>
            <a href="<c:out value='${customerListReturnUrl}' />"
               class="add-button ui-button button--secondary button--md">
                <i class="fas fa-list"></i> 목록으로
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

    <form id="customerDetailForm"
          class="ui-form"
          method="post"
          action="${pageContext.request.contextPath}/customers"
          data-ui-submit-lock="auto">
        <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
        <input type="hidden" name="action" value="saveDetail">
        <input type="hidden" name="env" value="<c:out value="${env != null ? env : 'prod'}" />">

        <div class="detail-container ui-detail">
            <!-- 메타정보 섹션 -->
            <div class="detail-section">
                <div class="detail-section-title">
                    <i class="fas fa-info-circle"></i>
                    메타정보
                </div>
                <div class="detail-grid">
                    <div class="detail-item">
                        <span class="detail-label">고객사</span>
                        <div class="detail-value">
                            <input type="text" class="form-control readonly" name="customerName"
                                   value="<c:out value="${not empty customerDetail.customerName ? customerDetail.customerName : customer.customerName}" />"
                                   readonly>
                        </div>
                    </div>
                    <div class="detail-item">
					    <span class="detail-label">시스템명</span>
					    <div class="detail-value">
					        <input type="text" class="form-control" name="systemName"
					               value="<c:out value="${not empty customerDetail.systemName ? customerDetail.systemName : ''}" />"
					               placeholder="시스템명을 입력하세요">
					    </div>
					</div>
                    <div class="detail-item">
                        <span class="detail-label">고객사 담당자</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="customerManager"
                                   value="<c:out value="${not empty customerDetail.customerManager ? customerDetail.customerManager : ''}" />"
                                   placeholder="고객사 담당자를 입력하세요">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">담당 SI</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="siCompany"
                                   value="<c:out value="${not empty customerDetail.siCompany ? customerDetail.siCompany : ''}" />"
                                   placeholder="SI 회사명을 입력하세요">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">SI 담당자</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="siManager"
                                   value="<c:out value="${not empty customerDetail.siManager ? customerDetail.siManager : ''}" />"
                                   placeholder="SI 담당자를 입력하세요">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">작성자</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="creator"
                                   value="<c:out value="${not empty customerDetail.creator ? customerDetail.creator : ''}" />"
                                   placeholder="작성자를 입력하세요">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">작성일자</span>
                        <div class="detail-value">
                            <input type="date" class="form-control" name="createDate"
                                   value="<c:if test='${not empty customerDetail.createDate}'><fmt:formatDate value='${customerDetail.createDate}' pattern='yyyy-MM-dd' /></c:if>">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">담당자 정</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="mainManager"
                                   value="<c:out value="${not empty customerDetail.mainManager ? customerDetail.mainManager : customer.managerName}" />"
                                   placeholder="주 담당자를 입력하세요">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">담당자 부</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="subManager"
                                   value="<c:out value="${not empty customerDetail.subManager ? customerDetail.subManager : customer.subManagerName}" />"
                                   placeholder="부 담당자를 입력하세요">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">설치일자</span>
                        <div class="detail-value">
                            <input type="date" class="form-control" name="installDate"
                                   value="<c:if test='${not empty customerDetail.installDate}'><fmt:formatDate value='${customerDetail.installDate}' pattern='yyyy-MM-dd' /></c:if>">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">도입년도</span>
                        <div class="detail-value">
                            <input type="number" class="form-control" name="introductionYear"
                                   value="<c:out value="${not empty customerDetail.introductionYear ? customerDetail.introductionYear : customer.firstIntroductionYear}" />"
                                   min="2000" max="2030" placeholder="YYYY">
                        </div>
                    </div>
                </div>
            </div>

            <!-- Vertica 정보 섹션 -->
            <div class="detail-section">
                <div class="detail-section-title">
                    <i class="fas fa-database"></i>
                    Vertica 정보
                </div>
                <div class="detail-grid">
                    <div class="detail-item">
                        <span class="detail-label">DB명</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="dbName"
                                   value="<c:out value="${not empty customerDetail.dbName ? customerDetail.dbName : customer.dbName}" />"
                                   placeholder="데이터베이스명을 입력하세요">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">DB mode</span>
                        <div class="detail-value">
                            <select class="form-control" name="dbMode">
                                <option value="">선택하세요</option>
                                <option value="ENT" ${(not empty customerDetail.dbMode and customerDetail.dbMode == 'ENT') or (empty customerDetail.dbMode and customer.mode == 'ENT') ? 'selected' : ''}>ENT</option>
                                <option value="EON" ${(not empty customerDetail.dbMode and customerDetail.dbMode == 'EON') or (empty customerDetail.dbMode and customer.mode == 'EON') ? 'selected' : ''}>EON</option>
                            </select>
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Version</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="verticaVersion"
                                   value="<c:out value="${not empty customerDetail.verticaVersion ? customerDetail.verticaVersion : customer.verticaVersion}" />"
                                   placeholder="예: 12.0.4">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">라이센스</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="licenseInfo"
                                   value="<c:out value="${not empty customerDetail.licenseInfo ? customerDetail.licenseInfo : customer.licenseSize}" />"
                                   placeholder="라이센스 정보를 입력하세요">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">SAID</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="said"
                                   value="<c:out value="${not empty customerDetail.said ? customerDetail.said : customer.said}" />"
                                   placeholder="Service Agreement ID">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">노드 수</span>
                        <div class="detail-value">
                            <input type="number" class="form-control" name="nodeCount"
                                   value="<c:out value="${not empty customerDetail.nodeCount ? customerDetail.nodeCount : customer.nodes}" />"
                                   min="1" placeholder="노드 수">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Vertica admin</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="verticaAdmin"
                                   value="<c:out value="${not empty customerDetail.verticaAdmin ? customerDetail.verticaAdmin : ''}" />"
                                   placeholder="Vertica 관리자 계정">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Subcluster 유무</span>
                        <div class="detail-value">
                            <select class="form-control" name="subclusterYn">
                                <option value="">선택하세요</option>
                                <option value="Y" ${customerDetail.subclusterYn == 'Y' ? 'selected' : ''}>있음</option>
                                <option value="N" ${customerDetail.subclusterYn == 'N' ? 'selected' : ''}>없음</option>
                            </select>
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">MC 여부</span>
                        <div class="detail-value">
                            <select class="form-control" name="mcYn">
                                <option value="">선택하세요</option>
                                <option value="Y" ${customerDetail.mcYn == 'Y' ? 'selected' : ''}>사용</option>
                                <option value="N" ${customerDetail.mcYn == 'N' ? 'selected' : ''}>미사용</option>
                            </select>
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">MC host</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="mcHost"
                                   value="<c:out value="${not empty customerDetail.mcHost ? customerDetail.mcHost : ''}" />"
                                   placeholder="MC 호스트 정보">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">MC version</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="mcVersion"
                                   value="<c:out value="${not empty customerDetail.mcVersion ? customerDetail.mcVersion : ''}" />"
                                   placeholder="MC 버전">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">MC admin</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="mcAdmin"
                                   value="<c:out value="${not empty customerDetail.mcAdmin ? customerDetail.mcAdmin : ''}" />"
                                   placeholder="MC 관리자 계정">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">백업 여부</span>
                        <div class="detail-value">
                            <select class="form-control" name="backupYn">
                                <option value="">선택하세요</option>
                                <option value="Y" ${customerDetail.backupYn == 'Y' ? 'selected' : ''}>사용</option>
                                <option value="N" ${customerDetail.backupYn == 'N' ? 'selected' : ''}>미사용</option>
                            </select>
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">사용자 정의 리소스풀 여부</span>
                        <div class="detail-value">
                            <select class="form-control" name="customResourcePoolYn">
                                <option value="">선택하세요</option>
                                <option value="Y" ${customerDetail.customResourcePoolYn == 'Y' ? 'selected' : ''}>사용</option>
                                <option value="N" ${customerDetail.customResourcePoolYn == 'N' ? 'selected' : ''}>미사용</option>
                            </select>
                        </div>
                    </div>
                </div>
                <div class="detail-item full-width mt-4">
                    <span class="detail-label">백업비고</span>
                    <div class="detail-value">
                        <textarea class="form-control" name="backupNote" placeholder="백업 관련 상세 정보를 입력하세요"><c:out value="${not empty customerDetail.backupNote ? customerDetail.backupNote : customer.backupConfig}" /></textarea>
                    </div>
                </div>
            </div>

            <!-- 환경 정보 섹션 -->
            <div class="detail-section">
                <div class="detail-section-title">
                    <i class="fas fa-server"></i>
                    환경 정보
                </div>
                <div class="detail-grid">
                    <div class="detail-item">
                        <span class="detail-label">OS</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="osInfo"
                                   value="<c:out value="${not empty customerDetail.osInfo ? customerDetail.osInfo : customer.os}" />"
                                   placeholder="예: RHEL 8.6">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">메모리</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="memoryInfo"
                                   value="<c:out value="${not empty customerDetail.memoryInfo ? customerDetail.memoryInfo : ''}" />"
                                   placeholder="예: 64GB">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">인프라 구분</span>
                        <div class="detail-value">
                            <select class="form-control" name="infraType">
                                <option value="">선택하세요</option>
                                <option value="온프레미스" ${customerDetail.infraType == '온프레미스' ? 'selected' : ''}>온프레미스</option>
                                <option value="클라우드" ${customerDetail.infraType == '클라우드' ? 'selected' : ''}>클라우드</option>
                                <option value="하이브리드" ${customerDetail.infraType == '하이브리드' ? 'selected' : ''}>하이브리드</option>
                            </select>
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">CPU 소켓</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="cpuSocket"
                                   value="<c:out value="${not empty customerDetail.cpuSocket ? customerDetail.cpuSocket : ''}" />"
                                   placeholder="예: 2">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">HyperThreading</span>
                        <div class="detail-value">
                            <select class="form-control" name="hyperThreading">
                                <option value="">선택하세요</option>
                                <option value="Y" ${customerDetail.hyperThreading == 'Y' ? 'selected' : ''}>사용</option>
                                <option value="N" ${customerDetail.hyperThreading == 'N' ? 'selected' : ''}>미사용</option>
                            </select>
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">CPU 코어</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="cpuCore"
                                   value="<c:out value="${not empty customerDetail.cpuCore ? customerDetail.cpuCore : ''}" />"
                                   placeholder="예: 16">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">/data 영역</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="dataArea"
                                   value="<c:out value="${not empty customerDetail.dataArea ? customerDetail.dataArea : ''}" />"
                                   placeholder="예: 1TB">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Depot 영역</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="depotArea"
                                   value="<c:out value="${not empty customerDetail.depotArea ? customerDetail.depotArea : ''}" />"
                                   placeholder="예: 500GB">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">/catalog 영역</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="catalogArea"
                                   value="<c:out value="${not empty customerDetail.catalogArea ? customerDetail.catalogArea : ''}" />"
                                   placeholder="예: 50GB">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">object 영역</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="objectArea"
                                   value="<c:out value="${not empty customerDetail.objectArea ? customerDetail.objectArea : ''}" />"
                                   placeholder="예: 100GB">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Public 여부</span>
                        <div class="detail-value">
                            <select class="form-control" name="publicYn">
                                <option value="">선택하세요</option>
                                <option value="Y" ${customerDetail.publicYn == 'Y' ? 'selected' : ''}>사용</option>
                                <option value="N" ${customerDetail.publicYn == 'N' ? 'selected' : ''}>미사용</option>
                            </select>
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Public 대역</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="publicNetwork"
                                   value="<c:out value="${not empty customerDetail.publicNetwork ? customerDetail.publicNetwork : ''}" />"
                                   placeholder="예: 192.168.1.0/24">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Private 여부</span>
                        <div class="detail-value">
                            <select class="form-control" name="privateYn">
                                <option value="">선택하세요</option>
                                <option value="Y" ${customerDetail.privateYn == 'Y' ? 'selected' : ''}>사용</option>
                                <option value="N" ${customerDetail.privateYn == 'N' ? 'selected' : ''}>미사용</option>
                            </select>
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Private 대역</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="privateNetwork"
                                   value="<c:out value="${not empty customerDetail.privateNetwork ? customerDetail.privateNetwork : ''}" />"
                                   placeholder="예: 10.0.0.0/24">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">storage 여부</span>
                        <div class="detail-value">
                            <select class="form-control" name="storageYn">
                                <option value="">선택하세요</option>
                                <option value="Y" ${customerDetail.storageYn == 'Y' ? 'selected' : ''}>사용</option>
                                <option value="N" ${customerDetail.storageYn == 'N' ? 'selected' : ''}>미사용</option>
                            </select>
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Storage 대역</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="storageNetwork"
                                   value="<c:out value="${not empty customerDetail.storageNetwork ? customerDetail.storageNetwork : ''}" />"
                                   placeholder="예: 172.16.0.0/24">
                        </div>
                    </div>
                </div>
            </div>

            <!-- 외부 솔루션 섹션 -->
            <div class="detail-section">
                <div class="detail-section-title">
                    <i class="fas fa-puzzle-piece"></i>
                    외부 솔루션
                </div>
                <div class="detail-grid">
                    <div class="detail-item">
                        <span class="detail-label">ETL</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="etlTool"
                                   value="<c:out value="${not empty customerDetail.etlTool ? customerDetail.etlTool : customer.etlTool}" />"
                                   placeholder="예: Informatica, DataStage">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">BI</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="biTool"
                                   value="<c:out value="${not empty customerDetail.biTool ? customerDetail.biTool : customer.biTool}" />"
                                   placeholder="예: Tableau, PowerBI">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">DB암호화</span>
                        <div class="detail-value">
                            <select class="form-control" name="dbEncryption">
                                <option value="">선택하세요</option>
                                <option value="적용" ${(not empty customerDetail.dbEncryption and customerDetail.dbEncryption == '적용') or (empty customerDetail.dbEncryption and customer.dbEncryption == '적용') ? 'selected' : ''}>적용</option>
                                <option value="미적용" ${(not empty customerDetail.dbEncryption and customerDetail.dbEncryption == '미적용') or (empty customerDetail.dbEncryption and customer.dbEncryption == '미적용') ? 'selected' : ''}>미적용</option>
                                <option value="부분적용" ${(not empty customerDetail.dbEncryption and customerDetail.dbEncryption == '부분적용') or (empty customerDetail.dbEncryption and customer.dbEncryption == '부분적용') ? 'selected' : ''}>부분적용</option>
                            </select>
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">CDC</span>
                        <div class="detail-value">
                            <input type="text" class="form-control" name="cdcTool"
                                   value="<c:out value="${not empty customerDetail.cdcTool ? customerDetail.cdcTool : customer.cdcTool}" />"
                                   placeholder="예: Oracle GoldenGate">
                        </div>
                    </div>
                </div>
            </div>

            <!-- 기타 정보 섹션 -->
            <div class="detail-section">
                <div class="detail-section-title">
                    <i class="fas fa-sticky-note"></i>
                    기타 정보
                </div>
                <div class="detail-grid">
                    <div class="detail-item">
                        <span class="detail-label">EOS 일자</span>
                        <div class="detail-value">
                            <c:set var="eosInputValue" value="${customer.verticaEos}" />
                            <c:if test="${not empty customerDetail.eosDate}">
                                <fmt:formatDate var="eosInputValue" value="${customerDetail.eosDate}" pattern="yyyy-MM-dd" />
                            </c:if>
                            <input type="date" class="form-control" name="eosDate" value="<c:out value='${eosInputValue}' />">
                        </div>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">고객 유형</span>
                        <div class="detail-value">
                        	<select class="form-control" name="customerType">
                        	  <option value="">선택하세요</option>
                                <option value="정기점검 계약 고객사" ${(not empty customerDetail.customerType and customerDetail.customerType == '정기점검 계약 고객사') or (empty customerDetail.customerType and customer.customerType == '정기점검 계약 고객사') ? 'selected' : ''}>정기점검 계약 고객사</option>
                                <option value="납품 계약 고객사" ${(not empty customerDetail.customerType and customerDetail.customerType == '납품 계약 고객사') or (empty customerDetail.customerType and customer.customerType == '납품 계약 고객사') ? 'selected' : ''}>납품 계약 고객사</option>
                                <option value="유지보수 종료 고객사" ${(not empty customerDetail.customerType and customerDetail.customerType == '유지보수 종료 고객사') or (empty customerDetail.customerType and customer.customerType == '유지보수 종료 고객사') ? 'selected' : ''}>유지보수 종료 고객사</option>
                            </select>
                        </div>
                    </div>



                </div>
                <div class="detail-item full-width mt-4">
                    <span class="detail-label">비고</span>
                    <div class="detail-value">
                        <textarea class="form-control note-textarea" name="note" placeholder="기타 참고사항을 입력하세요"><c:out value="${not empty customerDetail.note ? customerDetail.note : customer.note}" /></textarea>
                    </div>
                </div>
            </div>
        </div>

        <!-- 버튼 그룹 -->
        <div class="form-actions">
            <button type="submit"
                    class="btn btn-success ui-button button--primary button--md"
                    data-busy-label="저장 중">
                <i class="fas fa-save"></i>
                저장하기
            </button>
            <a href="<c:out value='${customerDetailUrl}' />"
               class="btn btn-secondary ui-button button--secondary button--md">
                <i class="fas fa-times"></i>
                취소
            </a>
            <a href="<c:out value='${customerListReturnUrl}' />"
               class="btn btn-secondary ui-button button--secondary button--md">
                <i class="fas fa-list"></i>
                목록으로
            </a>
        </div>
    </form>
</div>

<%@ include file="/includes/footer.jsp" %>
