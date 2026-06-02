<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="pageTitle" value="트러블 슈팅 상세보기" scope="request" />
<%@ include file="/includes/header.jsp" %>

<style>
    .troubleshooting-detail {
        max-width: 1000px;
        margin: 0 auto;
        padding: 20px;
        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    }
    
    /* 액션 바: 제목과 구분되는 별도 행 */
    .action-bar {
        display: flex;
        gap: 0.75rem;
        justify-content: flex-end;
        margin-top: 12px;
        padding-top: 12px;
        border-top: 1px solid #e5e7eb;
    }
    /* 액션 버튼 고정 크기/톤 통일 */
    .action-bar .btn {
        padding: 0.625rem 1rem;
        font-size: 0.875rem;
        border-radius: 6px;
        min-width: 120px;
        justify-content: center;
    }
    .action-bar .btn i {
        font-size: 0.875rem;
    }
    /* 상세 섹션 하단 액션 영역 */
    .section-actions {
        display: flex;
        gap: 0.75rem;
        justify-content: flex-end;
        margin-top: 16px;
        padding-top: 12px;
        border-top: 1px solid #e5e7eb;
    }
    .section-actions .btn {
        padding: 0.625rem 1rem;
        font-size: 0.875rem;
        border-radius: 6px;
        min-width: 120px;
        justify-content: center;
    }
    /* 더 미니멀한 텍스트형 버튼 */
    .section-actions .btn-ghost {
        background: transparent;
        border: none;
        color: #374151;
        padding: 0.25rem 0.5rem;
        min-width: auto;
        border-radius: 0;
    }
    .section-actions .btn-ghost:hover {
        color: var(--primary);
        text-decoration: underline;
        background: transparent;
    }
    .section-actions .btn-ghost-danger {
        color: #b91c1c;
    }
    .section-actions .btn-ghost-danger:hover {
        color: #991b1b;
        text-decoration: underline;
    }
    @media (max-width: 768px) {
        .section-actions { flex-direction: column; align-items: stretch; }
        .section-actions .btn { width: 100%; }
    }
    @media (max-width: 768px) {
        .action-bar {
            flex-direction: column;
            align-items: stretch;
            gap: 0.5rem;
        }
        .action-bar .btn { width: 100%; }
    }
    
    .alert {
        padding: 1rem 1.25rem;
        margin-bottom: 1.5rem;
        border-radius: 8px;
        border: none;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
    }
    
    .alert-success {
        background: #f0fdf4;
        color: #166534;
        border-left: 4px solid #22c55e;
    }
    
    .alert-danger {
        background: #fef2f2;
        color: #991b1b;
        border-left: 4px solid #ef4444;
    }
    
    .detail-container {
        background: white;
        border-radius: 8px;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        overflow: hidden;
        border: 1px solid #e5e7eb;
    }
    
    .detail-section {
        margin-bottom: 0;
        padding: 20px;
        border-bottom: 1px solid #f3f4f6;
    }
    
    .detail-section:last-child {
        margin-bottom: 0;
        border-bottom: none;
    }
    
    .detail-section-title {
        font-size: 1.1rem;
        font-weight: 700;
        color: #1f2937;
        margin-bottom: 15px;
        padding-bottom: 8px;
        border-bottom: 2px solid #e5e7eb;
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }
    
.detail-grid {
    display: grid;
    grid-template-columns: 1fr 1fr; /* 2열 고정 */
    gap: 1rem;
}
    
    .detail-item {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        padding: 8px 0;
        border-bottom: 1px solid #f3f4f6;
    }
    
	.detail-item:nth-last-child(1),
	.detail-item:nth-last-child(2) {
	    border-bottom: none;
	}
    
    .detail-label {
        font-weight: 600;
        color: #6b7280;
        font-size: 14px;
        min-width: 110px;
        flex-shrink: 0;
    }
    
    .detail-value {
        font-weight: 500;
        color: #1f2937;
        text-align: right;
        word-break: break-word;
        flex: 1;
        margin-left: 1rem;
        font-size: 14px;
    }
    
    .detail-item.full-width {
        flex-direction: column;
        align-items: flex-start;
        grid-column: 1 / -1;
    }
    
    .detail-item.full-width .detail-label {
        margin-bottom: 8px;
    }
    
	.detail-item.full-width .detail-value {
	    text-align: left;
	    width: 100%;
	    margin-left: 0;
	    background: #f9fafb;
	    padding: 12px;
	    border-radius: 6px;
	    border: 1px solid #e5e7eb;
	    min-height: 60px;
	    white-space: pre-line;  /* pre-wrap을 pre-line으로 변경 */
	    font-family: 'Courier New', monospace;
	    font-size: 13px;
	    line-height: 1.4;
	}
    
    .empty-value {
        color: #9ca3af;
        font-style: italic;
    }
    
    /* 반응형 디자인 */
    @media (max-width: 768px) {
        .troubleshooting-detail {
            max-width: 100%;
            padding: 15px;
        }
        
        .header-actions {
            flex-direction: column;
            align-items: stretch;
        }
        
        .detail-grid {
            grid-template-columns: 1fr;
        }
        
        .detail-section {
            padding: 15px;
        }
        
        .detail-section-title {
            font-size: 1rem;
        }
        
        .detail-item {
            flex-direction: column;
            align-items: flex-start;
            gap: 0.5rem;
        }
        
        .detail-label {
            min-width: auto;
        }
        
        .detail-value {
            text-align: left;
            margin-left: 0;
        }
    }
</style>

<div class="troubleshooting-detail">
    <div class="page-header">
        <div class="d-flex justify-content-between align-items-center">
            <div>
                <h1><i class="fas fa-tools"></i> ${troubleshooting.title}</h1>
                <p class="lead">트러블 슈팅 상세정보</p>
            </div>
        </div>
        <div class="action-bar">
            <a href="${pageContext.request.contextPath}/troubleshooting?view=list" class="btn btn-secondary">
                <i class="fas fa-arrow-left"></i>
                목록으로
            </a>
        </div>
    </div>
    
    <!-- 성공/에러 메시지 표시 -->
    <c:if test="${not empty sessionScope.message}">
        <div class="alert alert-success">
            <i class="fas fa-check-circle"></i>
            ${sessionScope.message}
        </div>
        <c:remove var="message" scope="session" />
    </c:if>
    
    <c:if test="${not empty sessionScope.error}">
        <div class="alert alert-danger">
            <i class="fas fa-exclamation-circle"></i>
            ${sessionScope.error}
        </div>
        <c:remove var="error" scope="session" />
    </c:if>
    
    <div class="detail-container">
        <!-- 기본작성 항목 섹션 -->
        <div class="detail-section">
            <div class="detail-section-title">
                <i class="fas fa-info-circle"></i>
                기본작성 항목
            </div>
            <div class="detail-grid">
                <div class="detail-item">
                    <span class="detail-label">제목</span>
                    <span class="detail-value">${troubleshooting.title}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">고객사</span>
                    <span class="detail-value">${troubleshooting.customerName}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">고객사 담당자</span>
                    <span class="detail-value">${not empty troubleshooting.customerManager ? troubleshooting.customerManager : '-'}</span>
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
                    <span class="detail-value">${not empty troubleshooting.workPersonnel ? troubleshooting.workPersonnel : '-'}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">작업기간</span>
                    <span class="detail-value">${not empty troubleshooting.workPeriod ? troubleshooting.workPeriod : '-'}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">작성자</span>
                    <span class="detail-value">${troubleshooting.creator}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">작성일자</span>
                    <span class="detail-value">
                        <fmt:formatDate value="${troubleshooting.createDate}" pattern="yyyy-MM-dd HH:mm" />
                    </span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">지원형태</span>
                    <span class="detail-value">${not empty troubleshooting.supportType ? troubleshooting.supportType : '-'}</span>
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
                                ${troubleshooting.overview}
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
                                ${troubleshooting.causeAnalysis}
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
                                ${troubleshooting.errorContent}
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
                                ${troubleshooting.actionTaken}
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
                                ${troubleshooting.scriptContent}
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
                                ${troubleshooting.note}
                            </c:when>
                            <c:otherwise>
                                <span class="empty-value">작성된 내용이 없습니다.</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>
            
            <c:if test="${not empty troubleshooting.updatedDate}">
                <div class="mt-4 pt-3 border-top text-right text-muted" style="font-size: 13px;">
                    최종 수정: <fmt:formatDate value="${troubleshooting.updatedDate}" pattern="yyyy-MM-dd HH:mm" />
                </div>
            </c:if>
            <div class="section-actions">
                <a href="${pageContext.request.contextPath}/troubleshooting?view=edit&id=${troubleshooting.id}" class="btn btn-ghost">수정하기</a>
                <button class="btn btn-ghost btn-ghost-danger" onclick="deleteTroubleshooting('${troubleshooting.id}')">삭제하기</button>
            </div>
        </div>
    </div>
</div>

<script>
    // 트러블 슈팅 삭제 함수
    function deleteTroubleshooting(id) {
        if (confirm('정말로 이 트러블 슈팅을 삭제하시겠습니까?\n\n삭제된 데이터는 복구할 수 없습니다.')) {
            // 삭제 폼 생성 및 전송
            var form = document.createElement('form');
            form.method = 'POST';
            form.action = '${pageContext.request.contextPath}/troubleshooting';
            
            var actionInput = document.createElement('input');
            actionInput.type = 'hidden';
            actionInput.name = 'action';
            actionInput.value = 'delete';
            
            var idInput = document.createElement('input');
            idInput.type = 'hidden';
            idInput.name = 'id';
            idInput.value = id;
            
            form.appendChild(actionInput);
            form.appendChild(idInput);
            
            document.body.appendChild(form);
            form.submit();
        }
    }
</script>

<%@ include file="/includes/footer.jsp" %>
