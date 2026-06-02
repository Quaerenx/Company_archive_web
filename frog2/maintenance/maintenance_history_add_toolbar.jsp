<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
        <%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

            <c:set var="pageTitle" value="정기점검 이력 - ${customerName}" scope="request" />
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

                        .detail-item i {
                            color: #6c757d;
                            width: 16px;
                        }

                        .action-buttons {
                            display: flex;
                            gap: 8px;
                            flex-wrap: wrap;
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

                        /* 상단 툴바 - 정돈된 레이아웃 */
                        .mh-toolbar {
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                            gap: 12px;
                            flex-wrap: wrap;
                            padding: 8px 0 12px;
                            border-bottom: 1px solid #e5e7eb;
                        }

                        .mh-toolbar__title {
                            display: flex;
                            align-items: baseline;
                            gap: 8px;
                        }

                        .mh-toolbar__title h3 {
                            margin: 0;
                            font-size: 18px;
                            font-weight: 600;
                            color: #111827;
                        }

                        .mh-toolbar__meta {
                            font-size: 13px;
                            color: #6b7280;
                        }

                        .mh-toolbar__actions {
                            display: flex;
                            align-items: center;
                            gap: 10px;
                            flex-wrap: wrap;
                        }

                        .mh-toolbar__filters {
                            display: flex;
                            align-items: center;
                            gap: 8px;
                        }

                        .mh-toolbar__buttons {
                            display: flex;
                            align-items: center;
                            gap: 8px;
                        }

                        .mh-toolbar input.form-control,
                        .mh-toolbar select.form-control,
                        .mh-toolbar .btn-min {
                            height: 34px;
                            line-height: 32px;
                            padding: 0 10px;
                        }

                        @media (max-width: 768px) {
                            .mh-toolbar {
                                align-items: flex-start;
                            }

                            .mh-toolbar__actions {
                                width: 100%;
                                justify-content: flex-start;
                            }

                            .mh-toolbar__filters {
                                width: 100%;
                                flex-wrap: wrap;
                            }
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
                            align-items: center;
                            /* 수직 중앙 정렬로 변경하여 안정감을 줍니다. */
                            margin-bottom: 1rem;
                            flex-wrap: wrap;
                            gap: 1rem;
                            /* 간격을 조금 더 확보합니다. */
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
                            [cite: 133] color: #6b7280;
                            [cite: 133] font-size: 0.875rem;
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
                    </style>

                    <div class="maintenance-history">
                        <t:pageHeader>
                            <jsp:attribute name="title"><i class="fas fa-building"></i> ${customerName}</jsp:attribute>
                            <jsp:attribute name="subtitle">
                                <c:if test="${not empty customer}">
                                    <span class="detail-item"><i class="fas fa-calendar"></i> 도입년도:
                                        ${customer.firstIntroductionYear}</span>
                                    <span class="detail-item"><i class="fas fa-database"></i> DB:
                                        ${customer.dbName}</span>
                                    <span class="detail-item"><i class="fas fa-code-branch"></i> 버전:
                                        ${customer.verticaVersion}</span>
                                    <span class="detail-item"><i class="fas fa-user"></i> 담당자:
                                        ${customer.managerName}</span>
                                </c:if>
                            </jsp:attribute>
                            <jsp:attribute name="actions">
                                <a href="${pageContext.request.contextPath}/maintenance?view=add&customerName=${customerName}"
                                    class="btn-min"><i class="fas fa-plus"></i> 새 점검 이력 추가</a>
                                <a href="${pageContext.request.contextPath}/maintenance?view=cards" class="btn-min"><i
                                        class="fas fa-arrow-left"></i> 목록으로</a>
                            </jsp:attribute>
                        </t:pageHeader>

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

                        <!-- 상단 툴바 -->
                        <div class="mh-toolbar">
                            <div class="mh-toolbar__title">
                                <h3>정기점검 이력</h3>
                                <span class="mh-toolbar__meta">총 ${records != null ? records.size() : 0}건</span>
                            </div>
                            <div class="mh-toolbar__actions">
                                <div class="mh-toolbar__filters">
                                    <input type="text" class="form-control" name="q" placeholder="검색어">
                                    <select class="form-control" name="status">
                                        <option value="">전체</option>
                                        <option value="completed">완료</option>
                                        <option value="pending">대기</option>
                                    </select>
                                    <input type="date" class="form-control" name="from">
                                    <input type="date" class="form-control" name="to">
                                </div>
                                <div class="mh-toolbar__buttons">
                                    <a href="${pageContext.request.contextPath}/maintenance?view=add&customerName=${customerName}"
                                        class="btn-min">
                                        <i class="fas fa-plus"></i> 새 점검 이력 추가
                                    </a>
                                </div>
                            </div>
                        </div>

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
                                            <div class="history-item"
                                                onclick="location.href='${pageContext.request.contextPath}/maintenance?view=edit&id=${record.maintenanceId}'"
                                                style="cursor:pointer;">
                                                <div class="history-meta">
                                                    <div class="inspection-date">
                                                        <i class="fas fa-calendar-check"></i>
                                                        <fmt:formatDate value="${record.inspectionDate}"
                                                            pattern="yyyy년 MM월 dd일" />
                                                    </div>
                                                    <div class="inspector-info">
                                                        <div class="inspector-name">${record.inspectorName}</div>
                                                        <div class="timestamp-info">
                                                            등록:
                                                            <fmt:formatDate value="${record.createdAt}"
                                                                pattern="MM/dd HH:mm" />
                                                        </div>
                                                    </div>
                                                </div>

                                                <div class="history-details">
                                                    <div class="detail-group">
                                                        <span class="detail-label">Vertica 버전</span>
                                                        <span class="detail-value">
                                                            <c:choose>
                                                                <c:when test="${not empty record.verticaVersion}">
                                                                    <span
                                                                        class="version-tag">${record.verticaVersion}</span>
                                                                </c:when>
                                                                <c:otherwise>-</c:otherwise>
                                                            </c:choose>
                                                        </span>
                                                    </div>
                                                    <div class="detail-group">
                                                        <span class="detail-label">라이선스</span>
                                                        <span class="detail-value">
                                                            <c:choose>
                                                                <c:when
                                                                    test="${not empty record.licenseSizeGb || not empty record.licenseUsageSize || not empty record.licenseUsagePct}">
                                                                    <c:if test="${not empty record.licenseSizeGb}">
                                                                        ${record.licenseSizeGb}GB</c:if>
                                                                    <c:if test="${not empty record.licenseUsageSize}">
                                                                        <c:if test="${not empty record.licenseSizeGb}">,
                                                                        </c:if>${record.licenseUsageSize}GB 사용
                                                                    </c:if>
                                                                    <c:if test="${not empty record.licenseUsagePct}">
                                                                        <c:if
                                                                            test="${not empty record.licenseSizeGb || not empty record.licenseUsageSize}">
                                                                            , </c:if>${record.licenseUsagePct}%
                                                                    </c:if>
                                                                </c:when>
                                                                <c:otherwise>-</c:otherwise>
                                                            </c:choose>
                                                        </span>
                                                    </div>
                                                </div>
                                                <c:if test="${not empty record.note}">
                                                    <div class="history-note">
                                                        <div class="note-label"><i class="fas fa-sticky-note"> 점검 내용 및
                                                                비고</i>
                                                        </div>
                                                        <c:out value="${record.note}" />
                                                    </div>
                                                </c:if>
                                                <div class="history-actions"></div>
                                            </div>
                                        </c:forEach>
                                    </c:when>
                                    <c:otherwise>
                                        <div class="empty-history">
                                            <i class="fas fa-clipboard"></i>
                                            <h3>정기점검 이력이 없습니다</h3>
                                            <p>${customerName}의 정기점검 이력이 아직 등록되지 않았습니다.</p>
                                            <a href="${pageContext.request.contextPath}/maintenance?view=add&customerName=${customerName}"
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

                    <script>
                        document.addEventListener('DOMContentLoaded', function () {
                            // 히스토리 아이템 애니메이션
                            const historyItems = document.querySelectorAll('.history-item');

                            historyItems.forEach((item, index) => {
                                item.style.opacity = '0';
                                item.style.transform = 'translateY(20px)';

                                setTimeout(() => {
                                    item.style.transition = 'all 0.5s ease';
                                    item.style.opacity = '1';
                                    item.style.transform = 'translateY(0)';
                                }, index * 100);
                            });

                            // 삭제 확인 강화
                            const deleteForms = document.querySelectorAll('form[onsubmit*="confirm"]');
                            deleteForms.forEach(form => {
                                form.addEventListener('submit', function (e) {
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
