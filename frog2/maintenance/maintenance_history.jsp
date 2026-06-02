<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="pageTitle" value="정기점검 이력 - ${fn:escapeXml(customerName)}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-maintenance" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>

<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/customers.css">
<style>
.maintenance-history {
        width: 100%;
        max-width: 1000px;
        margin: 0 auto;
        padding: var(--space-32) var(--space-16);
        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    }
    
    /* 유지보수 페이지: detail-item 아래 얇은 선 제거(하단 보더만 숨김) - 특이성 강화 및 폭 명시 */
    body.page-maintenance .page-header { border-bottom: 0 !important; border-width: 1px 1px 0 1px !important; }
    
    .header-content {
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-wrap: wrap;
        gap: 1rem;
    }
    
    .customer-info {
        flex: 1;
    }
    
    .customer-name {
        font-size: 1.75rem;
        font-weight: 700;
        color: #2c3e50;
        margin-bottom: 0.5rem;
        display: flex;
        align-items: center;
        gap: 0.75rem;
    }
    
    .customer-name i {
        color: #3b82f6;
        font-size: 1.5rem;
    }
    
    .customer-details {
        display: flex;
        gap: 2rem;
        flex-wrap: wrap;
        color: #6c757d;
        font-size: 0.95rem;
    }
    
    .detail-item {
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }
    
    /* detail-item 아래의 불필요한 선을 제거합니다. */
	.detail-item {
    	border-bottom: none !important;
	}
    
    .detail-item i {
        color: #6c757d;
        width: 16px;
    }
    
    .action-buttons { display: flex; gap: 8px; flex-wrap: wrap; }
    
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
    
    /* 정기점검 이력 카드 */
    .history-container {
        background: white;
        border-radius: 12px;
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
        border: 1px solid #e5e7eb;
        overflow: hidden;
    }
    
    .history-header {
        background: #f8fafc;
        padding: 1.5rem;
        border-bottom: 1px solid #e5e7eb;
        display: flex;
        justify-content: space-between;
        align-items: center;
    }
    
    .history-title {
        font-size: 1.25rem;
        font-weight: 600;
        color: #374151;
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }
    
    .record-count {
        background: var(--primary-light);
        color: var(--primary);
        padding: 0.25rem 0.75rem;
        border-radius: 12px;
        font-size: 0.875rem;
        font-weight: 500;
    }
    
    .history-grid {
        padding: 1.5rem;
        display: grid;
        gap: 1rem;
    }
    
    .history-item {
        background: #fafbfc;
        border: 1px solid #e5e7eb;
        border-radius: 10px;
        padding: 1.5rem;
        transition: all 0.2s ease;
        position: relative;
    }
    
    .history-item:hover {
        background: #f0f9ff;
        border-color: #3b82f6;
        transform: translateY(-2px);
        box-shadow: 0 8px 25px rgba(59, 130, 246, 0.15);
    }
    
	.history-meta {
	    display: flex; 
	    align-items: center; /* 수직 중앙 정렬로 변경하여 안정감을 줍니다. */
	    margin-bottom: 1rem;
	    flex-wrap: wrap;
	    gap: 1rem; /* 간격을 조금 더 확보합니다. */
	}

    
    .inspection-date {
        font-size: 1.1rem;
        font-weight: 600;
        color: #1f2937;
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }
    
    .inspection-date i {
        color: #3b82f6;
    }
    
	.inspector-info {
	    margin-left: auto;
	    text-align: right;
	    color: #6b7280;
	    font-size: 0.875rem;
	}
    
    .inspector-name {
        font-weight: 500;
        color: #374151;
    }
    
    .history-details {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 1rem;
        margin-bottom: 1rem;
    }
    
    .detail-group {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0.5rem 0;
        border-bottom: 1px solid #f3f4f6;
    }
    
    .detail-label {
        font-weight: 500;
        color: #6b7280;
        font-size: 0.875rem;
    }
    
    .detail-value {
        color: #374151;
        font-weight: 500;
    }
    
    .version-tag {
        background: #dbeafe;
        color: #1e40af;
        padding: 0.125rem 0.5rem;
        border-radius: 6px;
        font-size: 0.75rem;
        font-weight: 500;
    }
    
    .history-note {
        background: white;
        border: 1px solid #e5e7eb;
        border-radius: 8px;
        padding: 1rem;
        margin-top: 1rem;
        color: #374151;
        line-height: 1.6;
        font-size: 0.875rem;
        white-space: pre-wrap;	
    }
    
	.note-label {
	    font-weight: 600;
	    color: #6b7280;
	    margin-bottom: 0.25rem;
	    display: flex;
	    align-items: center;
	    gap: 0.375rem;
	    white-space: pre-wrap;
	}
    
    .history-actions {
        display: flex;
        gap: 0.5rem;
        justify-content: flex-end;
        margin-top: 1rem;
        flex-wrap: wrap;
    }
    
    .btn-edit {
        background: #f59e0b;
        color: white;
        padding: 0.5rem 1rem;
        border-radius: 6px;
        text-decoration: none;
        font-size: 0.8rem;
        font-weight: 500;
    }
    
    .btn-edit:hover {
        background: #d97706;
        color: white;
        text-decoration: none;
    }
    
    .btn-delete {
        background: #ef4444;
        color: white;
        padding: 0.5rem 1rem;
        border-radius: 6px;
        font-size: 0.8rem;
        font-weight: 500;
        border: none;
        cursor: pointer;
    }
    
    .btn-delete:hover {
        background: #dc2626;
    }
    
    .empty-history {
        text-align: center;
        padding: 4rem 2rem;
        color: #9ca3af;
    }
    
    .empty-history i {
        font-size: 4rem;
        margin-bottom: 1rem;
        opacity: 0.5;
        color: #d1d5db;
    }
    
    .empty-history h3 {
        font-size: 1.25rem;
        font-weight: 600;
        color: #6b7280;
        margin-bottom: 0.5rem;
    }
    
    .empty-history p {
        margin-bottom: 1.5rem;
    }
    
    .timestamp-info {
        font-size: 0.75rem;
        color: #9ca3af;
        margin-top: 0.5rem;
    }
    
  /* 반응형 디자인 */
    @media (max-width: 768px) {
        .maintenance-history {
            max-width: 100%;
            padding: var(--space-24) var(--space-16);
        }
        
        .header-content {
            flex-direction: column;
            align-items: flex-start;
        }
        
        .customer-name {
            font-size: 1.5rem;
        }
        
        .customer-details {
            flex-direction: column;
            gap: 0.5rem;
        }
        
        .action-buttons {
            width: 100%;
            justify-content: stretch;
        }
        
        .empty-history .btn {
            flex: 1;
            justify-content: center;
        }
        
        .history-grid {
            padding: 1rem;
        }
        
        .history-item {
            padding: 1rem;
        }
        
        .history-meta {
            flex-direction: column;
            align-items: flex-start;
        }
        
        .inspector-info {
            text-align: left;
        }
        
        .history-details {
            grid-template-columns: 1fr;
        }
        
        .history-actions {
            justify-content: center;
        }
    }

    /* 차트 카드 여백 보정 */
.usage-chart-wrap { padding: 1rem 1.25rem 1.25rem; }
.chart-note { color:#6b7280; font-size: 0.82rem; margin: 0.25rem 0 0.5rem; }
</style>

<div class="maintenance-history">
    <c:url value="/maintenance" var="addHistoryUrl">
        <c:param name="view" value="add"/>
        <c:param name="customerName" value="${customerName}"/>
    </c:url>
    <t:pageHeader>
        <jsp:attribute name="title"><i class="fas fa-building"></i> <c:out value="${customerName}" /></jsp:attribute>
        <jsp:attribute name="subtitle">
            <c:if test="${not empty customer}">
                <!-- <span class="detail-item"><i class="fas fa-calendar"></i> 도입년도: ${customer.firstIntroductionYear}</span>  -->
                <span class="detail-item"><i class="fas fa-database"></i> DB: <c:out value="${customer.dbName}" /></span>
                <span class="detail-item"><i class="fas fa-code-branch"></i> 버전: <c:out value="${customer.verticaVersion}" /></span>
                <span class="detail-item"><i class="fas fa-user"></i> 담당자: <c:out value="${customer.managerName}" /></span>
            </c:if>
        </jsp:attribute>
        <jsp:attribute name="actions">
            <a href="${addHistoryUrl}" class="btn-min"><i class="fas fa-plus"></i> 새 점검 이력 추가</a>
            <a href="${pageContext.request.contextPath}/maintenance?view=cards" class="btn-min"><i class="fas fa-arrow-left"></i> 목록으로</a>
        </jsp:attribute>
    </t:pageHeader>

    <!-- 라이선스 사용률 추이 차트 -->
    <div class="history-container" style="margin-bottom: 1rem;">
        <div class="history-header">
            <div class="history-title"><i class="fas fa-chart-line"></i> 라이선스 사용률 추이</div>
            <div class="record-count">
                <c:choose>
                    <c:when test="${not empty usageSeries}">데이터: ${fn:length(usageSeries)}건</c:when>
                    <c:otherwise>데이터 없음</c:otherwise>
                </c:choose>
            </div>
        </div>
        <div class="usage-chart-wrap">
            <c:choose>
                <c:when test="${not empty usageSeries}">
                    <canvas id="licenseUsageChart" height="120"></canvas>
                </c:when>
                <c:otherwise>
                    <div class="empty-history" style="padding: 2rem 1rem;">
                        <i class="fas fa-chart-area"></i>
                        <h3>표시할 사용률 추이 데이터가 없습니다</h3>
                        <p>정기점검 이력을 추가하면 사용률 추이를 확인할 수 있습니다.</p>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <!-- 성공/에러 메시지 표시 -->
    <c:if test="${not empty sessionScope.message}">
        <div class="alert alert-success">
            <i class="fas fa-check-circle"></i>
            <c:out value="${sessionScope.message}" />
        </div>
        <c:remove var="message" scope="session" />
    </c:if>

    <c:if test="${not empty sessionScope.error}">
        <div class="alert alert-danger">
            <i class="fas fa-exclamation-circle"></i>
            <c:out value="${sessionScope.error}" />
        </div>
        <c:remove var="error" scope="session" />
    </c:if>
    
    <!-- 정기점검 이력 목록 -->
    <div class="history-container">
        <div class="history-header">
            <div class="history-title">
                <i class="fas fa-clipboard-list"></i>
                정기점검 이력
            </div>
            <div class="record-count">
                총 ${records.size()}건의 점검 이력
            </div>
        </div>
        
        <div class="history-grid">
            <c:choose>
                <c:when test="${not empty records}">
                    <c:forEach var="record" items="${records}">
                        <div class="history-item" onclick="location.href='${pageContext.request.contextPath}/maintenance?view=edit&id=${record.maintenanceId}'" style="cursor:pointer;">
                            <div class="history-meta">
                                <div class="inspection-date">
                                    <i class="fas fa-calendar-check"></i>
                                    <fmt:formatDate value="${record.inspectionDate}" pattern="yyyy년 MM월 dd일"/>
                                </div>
                                <div class="inspector-info">
                                    <div class="inspector-name"><c:out value="${record.inspectorName}" /></div>
                                    <div class="timestamp-info">
                                        등록: <fmt:formatDate value="${record.createdAt}" pattern="MM/dd HH:mm"/>
                                    </div>
                                </div>
                            </div>
                            
                            <div class="history-details">
                                <div class="detail-group">
                                    <span class="detail-label">Vertica 버전</span>
                                    <span class="detail-value">
                                        <c:choose>
                                            <c:when test="${not empty record.verticaVersion}">
                                                <span class="version-tag"><c:out value="${record.verticaVersion}" /></span>
                                            </c:when>
                                            <c:otherwise>-</c:otherwise>
                                        </c:choose>
                                    </span>
                                </div>
                                <div class="detail-group">
                                    <span class="detail-label">라이선스</span>
                                    <span class="detail-value">
                                        <c:choose>
                                            <c:when test="${not empty record.licenseSummary}">
                                                <c:out value="${record.licenseSummary}" />
                                            </c:when>
                                            <c:otherwise>-</c:otherwise>
                                        </c:choose>
                                    </span>
                                </div>
                            </div>
                            <c:if test="${not empty record.note}">
                                <div class="history-note"><div class="note-label"><i class="fas fa-sticky-note"> 점검 내용 및 비고</i>
                                </div><c:out value="${record.note}" /></div>
                            </c:if> 
                            <div class="history-actions"></div>
                        </div>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                    <div class="empty-history">
                        <i class="fas fa-clipboard"></i>
                        <h3>정기점검 이력이 없습니다</h3>
                        <p><c:out value="${customerName}" />의 정기점검 이력이 아직 등록되지 않았습니다.</p>
                        <a href="${addHistoryUrl}"
                           class="btn btn-primary">
                            <i class="fas fa-plus"></i>
                            첫 번째 점검 이력 추가하기
                        </a>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<!-- Chart.js -->
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.4"></script>
<script>
(function() {
    // 데이터 준비: 서버에서 날짜별 pct/usedTb/sizeTb를 모두 포함한 단일 usageSeries를 전달
    const usageSeries = [
        <c:forEach var="pt" items="${usageSeries}" varStatus="st">
            { 
                date: "<c:out value='${pt.date}'/>",
                // 기존 호환 키
                value: <c:choose><c:when test="${pt.value != null}"><c:out value='${pt.value}'/></c:when><c:otherwise>null</c:otherwise></c:choose>,
                // 명시 키들(숫자 그대로 출력)
                pct: <c:choose><c:when test="${pt.pct != null}"><c:out value='${pt.pct}'/></c:when><c:otherwise>null</c:otherwise></c:choose>,
                usedTb: <c:choose><c:when test="${pt.usedTb != null}"><c:out value='${pt.usedTb}'/></c:when><c:otherwise>null</c:otherwise></c:choose>,
                sizeTb: <c:choose><c:when test="${pt.sizeTb != null}"><c:out value='${pt.sizeTb}'/></c:when><c:otherwise>null</c:otherwise></c:choose>
            }<c:if test="${!st.last}">,</c:if>
        </c:forEach>
    ];

    if (!usageSeries || usageSeries.length === 0) return;

    // 퍼센트 문자열/숫자 파싱 (예: "75", "75%", "75.2 %") - 레거시 value 대비용
    function parsePercent(x) {
        if (x == null) return null;
        const raw = String(x).trim();
        if (!raw) return null;
        const cleaned = raw.replace(/,/g, '').replace(/[^0-9.\-]/g, '');
        if (!cleaned || cleaned === '-' || cleaned === '.') return null;
        const v = Number(cleaned);
        return Number.isFinite(v) ? v : null;
    }

    // 차트 X축 라벨과 데이터 구성 (오름차순 가정)
    const labels = usageSeries.map(p => p.date);
    const usageData = usageSeries.map(p => Number.isFinite(p.pct) ? p.pct : parsePercent(p.value));
    const usedSizeData = usageSeries.map(p => Number.isFinite(p.usedTb) ? p.usedTb : null);
    const capacitySizeData = usageSeries.map(p => Number.isFinite(p.sizeTb) ? p.sizeTb : null);

    const ctx = document.getElementById('licenseUsageChart');
    if (!ctx) return;

    const chart = new Chart(ctx, {
        type: 'line',
        data: {
            labels,
            datasets: [
                {
                    label: '사용률(%)',
                    data: usageData,
                    yAxisID: 'y',
                    borderColor: '#3b82f6',
                    backgroundColor: 'rgba(59,130,246,0.15)',
                    tension: 0.25,
                    spanGaps: true,
                    pointRadius: 3,
                    pointHoverRadius: 5,
                    fill: false
                },
                {
                    label: '라이선스 사용량(TB)',
                    data: usedSizeData,
                    yAxisID: 'y1',
                    borderColor: '#10b981',
                    backgroundColor: 'rgba(16,185,129,0.15)',
                    tension: 0,
                    stepped: true,
                    spanGaps: true,
                    pointRadius: 2,
                    pointHoverRadius: 4,
                    fill: false
                },
                {
                    label: '라이선스 크기(TB)',
                    data: capacitySizeData,
                    yAxisID: 'y1',
                    borderColor: '#ef4444',
                    backgroundColor: 'rgba(239,68,68,0.15)',
                    tension: 0,
                    stepped: true,
                    pointRadius: 2,
                    pointHoverRadius: 4,
                    fill: false
                }
            ]
        },
        options: {
            responsive: true,
            interaction: { mode: 'index', intersect: false },
            plugins: {
                legend: { position: 'top' },
                tooltip: {
                    // 숫자 값이 있는 항목만 노출
                    filter: function(context) {
                        const y = context && context.parsed ? context.parsed.y : null;
                        return y != null && Number.isFinite(y);
                    },
                    callbacks: {
                        // 점검일(라벨) 표시
                        title: function(items) {
                            if (!items || !items.length) return '';
                            const idx = items[0].dataIndex;
                            return '점검일: ' + (Array.isArray(labels) ? labels[idx] : '');
                        },
                        // 값 + 단위 정확 표기
                        label: function(ctx) {
                            let y = ctx && ctx.parsed ? ctx.parsed.y : null;
                            if (y == null || !Number.isFinite(y)) {
                                // 보강: raw에서도 숫자 시도
                                const raw = ctx.raw;
                                const cand = raw && typeof raw === 'object' ? (raw.y ?? raw.value) : raw;
                                if (cand != null) {
                                    const n = (typeof cand === 'number') ? cand : Number(String(cand).replace(/,/g,'').replace(/[^0-9.\-]/g,''));
                                    if (Number.isFinite(n)) y = n;
                                }
                            }
                            if (y == null || !Number.isFinite(y)) return null;
                            const name = ctx.dataset && ctx.dataset.label ? ctx.dataset.label : '';
                            if (ctx.dataset && ctx.dataset.yAxisID === 'y') {
                                return (name ? name + ': ' : '') + y.toFixed(0) + '%';
                            }
                            return (name ? name + ': ' : '') + y.toFixed(2) + ' TB';
                        }
                    }
                }
            },
            scales: {
                y: {
                    type: 'linear',
                    position: 'left',
                    suggestedMin: 0,
                    suggestedMax: 100,
                    ticks: {
                        callback: function(v) {
                            const n = Number(v);
                            return Number.isFinite(n) ? n.toFixed(0) : String(v);
                        }
                    },
                    title: { display: true, text: '사용률(%)' }
                },
                y1: {
                    type: 'linear',
                    position: 'right',
                    min: 0,
                    grid: { drawOnChartArea: false },
                    title: { display: true, text: '용량(TB)' }
                },
                x: { title: { display: true, text: '점검일' } }
            }
        }
    });
})();
</script>

<script>
document.addEventListener('DOMContentLoaded', function() {
    // 히스토리 아이템 애니메이션
    const historyItems = document.querySelectorAll('.history-item');
    const staggerStep = historyItems.length > 20 ? 15 : 40;
    const maxDelay = 240;
    
    historyItems.forEach((item, index) => {
        item.style.opacity = '0';
        item.style.transform = 'translateY(20px)';
        
        setTimeout(() => {
            item.style.transition = 'all 0.5s ease';
            item.style.opacity = '1';
            item.style.transform = 'translateY(0)';
        }, Math.min(index * staggerStep, maxDelay));
    });
    
    // 삭제 확인 강화
    const deleteForms = document.querySelectorAll('form[onsubmit*="confirm"]');
    deleteForms.forEach(form => {
        form.addEventListener('submit', function(e) {
            e.preventDefault();
            
            const customerName = '${customerName}';
            const confirmMessage = `정말로 이 정기점검 이력을 삭제하시겠습니까?\n\n고객사: ${customerName}\n\n삭제된 데이터는 복구할 수 없습니다.`;
            
            if (confirm(confirmMessage)) {
                this.submit();
            }
        });
    });
});
</script>

<%@ include file="/includes/footer.jsp" %>
