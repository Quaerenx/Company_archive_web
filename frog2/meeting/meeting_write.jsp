<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="pageTitle" value="회의록 작성" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>

<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/meeting.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/customers.css">

<div class="meeting-page-container customer-management">
    <t:pageHeader>
        <jsp:attribute name="title"><i class="fas fa-pen"></i> 새 회의록 작성</jsp:attribute>
        <jsp:attribute name="subtitle">회의 내용을 정리하여 등록해주세요.</jsp:attribute>
        <jsp:attribute name="actions">
            <a href="${pageContext.request.contextPath}/meeting?view=list" class="add-button secondary"><i class="fas fa-list"></i> 목록으로</a>
        </jsp:attribute>
    </t:pageHeader>
    
    <!-- 오류 메시지 -->
    <c:if test="${not empty error}">
        <div class="alert alert-danger">
            <i class="fas fa-exclamation-circle"></i> ${error}
        </div>
    </c:if>
    
    <!-- 작성 폼 -->
    <div class="form-container">
        <form method="post" action="${pageContext.request.contextPath}/meeting" id="meetingForm">
            <input type="hidden" name="action" value="write">
            
            <!-- 기본 정보 -->
            <div class="section-title">회의 기본 정보</div>
            
            <div class="form-row">
                <div class="form-group">
                    <label for="title">회의 제목 <span class="required">*</span></label>
                    <input type="text" id="title" name="title" required maxlength="200" 
                           placeholder="회의 제목을 입력하세요">
                </div>
                <div class="form-group">
                    <label for="meeting_type">회의 유형 <span class="required">*</span></label>
                    <select id="meeting_type" name="meeting_type" required>
                        <option value="">회의 유형을 선택하세요</option>
                        <option value="daily">일일 회의</option>
                        <option value="weekly">주간 회의</option>
                        <option value="monthly">월간 회의</option>
                        <option value="project">프로젝트 회의</option>
                        <option value="emergency">긴급 회의</option>
                        <option value="other">기타</option>
                    </select>
                </div>
            </div>
            
            <div class="form-row">
                <div class="form-group">
                    <label for="meeting_datetime">회의 일시 <span class="required">*</span></label>
                    <input type="datetime-local" id="meeting_datetime" name="meeting_datetime" required>
                </div>
                <div class="form-group">
                    <!-- 공간 확보용 -->
                </div>
            </div>
            
            <!-- 회의 내용 -->
            <div class="section-title">회의 내용</div>
            
            <div class="form-row">
                <div class="form-group full-width">
                    <label for="content">회의록 내용 <span class="required">*</span></label>
                    <textarea id="content" name="content" rows="15" required 
                              placeholder="회의 내용을 상세히 작성해주세요.&#10;&#10;• 참석자:&#10;• 주요 논의사항:&#10;• 결정사항:&#10;• 액션 아이템:&#10;• 다음 회의 일정:"></textarea>
                </div>
            </div>
            
            <!-- 버튼 -->
            <div class="button-group">
                <a href="${pageContext.request.contextPath}/meeting?view=list" class="btn btn-cancel">취소</a>
                <button type="button" class="btn btn-secondary" onclick="previewContent()">미리보기</button>
                <button type="submit" class="btn btn-primary">등록하기</button>
            </div>
        </form>
    </div>
</div>

<!-- 미리보기 모달 -->
<div id="previewModal" class="modal modal-wide">
    <div class="modal-content">
        <div class="modal-header">
            <h3><i class="fas fa-eye"></i> 미리보기</h3>
            <button type="button" class="modal-close" onclick="closePreview()">&times;</button>
        </div>
        <div class="modal-body">
            <div class="preview-content">
                <div class="preview-meta">
                    <h2 id="preview-title"></h2>
                    <div class="preview-info">
                        <span class="preview-type"></span>
                        <span class="preview-datetime"></span>
                        <span class="preview-author">작성자: ${user.userName}</span>
                    </div>
                </div>
                <div class="preview-text" id="preview-content"></div>
            </div>
        </div>
        <div class="modal-footer">
            <button type="button" class="btn btn-secondary" onclick="closePreview()">닫기</button>
        </div>
    </div>
</div>

<script>
// 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', function() {
    // 현재 날짜시간을 기본값으로 설정
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    
    const currentDateTime = `${year}-${month}-${day}T${hours}:${minutes}`;
    document.getElementById('meeting_datetime').value = currentDateTime;
});

// 미리보기 기능
function previewContent() {
    const title = document.getElementById('title').value.trim();
    const meetingType = document.getElementById('meeting_type').value;
    const meetingDateTime = document.getElementById('meeting_datetime').value;
    const content = document.getElementById('content').value.trim();
    
    if (!title) {
        alert('회의 제목을 입력해주세요.');
        document.getElementById('title').focus();
        return;
    }
    
    if (!meetingType) {
        alert('회의 유형을 선택해주세요.');
        document.getElementById('meeting_type').focus();
        return;
    }
    
    if (!meetingDateTime) {
        alert('회의 일시를 선택해주세요.');
        document.getElementById('meeting_datetime').focus();
        return;
    }
    
    if (!content) {
        alert('회의 내용을 입력해주세요.');
        document.getElementById('content').focus();
        return;
    }
    
    // 미리보기 데이터 설정
    document.getElementById('preview-title').textContent = title;
    document.querySelector('.preview-type').textContent = getTypeLabel(meetingType);
    document.querySelector('.preview-datetime').textContent = formatDateTime(meetingDateTime);
    document.getElementById('preview-content').textContent = content;
    
    // 모달 표시
    document.getElementById('previewModal').classList.add('show');
}

// 미리보기 닫기
function closePreview() {
    document.getElementById('previewModal').classList.remove('show');
}

// 회의 유형 라벨 변환
function getTypeLabel(type) {
    const typeLabels = {
        'daily': '일일 회의',
        'weekly': '주간 회의',
        'monthly': '월간 회의',
        'project': '프로젝트 회의',
        'emergency': '긴급 회의',
        'other': '기타'
    };
    return typeLabels[type] || type;
}

// 날짜시간 포맷팅
function formatDateTime(dateTimeString) {
    const date = new Date(dateTimeString);
    return date.toLocaleString('ko-KR', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// 모달 외부 클릭 시 닫기
window.onclick = function(event) {
    const modal = document.getElementById('previewModal');
    if (event.target === modal) {
        closePreview();
    }
}

// ESC 키로 모달 닫기
document.addEventListener('keydown', function(event) {
    if (event.key === 'Escape') {
        closePreview();
    }
});

// 폼 유효성 검사
document.getElementById('meetingForm').addEventListener('submit', function(e) {
    const title = document.getElementById('title').value.trim();
    const meetingType = document.getElementById('meeting_type').value;
    const meetingDateTime = document.getElementById('meeting_datetime').value;
    const content = document.getElementById('content').value.trim();
    
    if (!title) {
        e.preventDefault();
        alert('회의 제목을 입력해주세요.');
        document.getElementById('title').focus();
        return false;
    }
    
    if (!meetingType) {
        e.preventDefault();
        alert('회의 유형을 선택해주세요.');
        document.getElementById('meeting_type').focus();
        return false;
    }
    
    if (!meetingDateTime) {
        e.preventDefault();
        alert('회의 일시를 선택해주세요.');
        document.getElementById('meeting_datetime').focus();
        return false;
    }
    
    if (!content) {
        e.preventDefault();
        alert('회의 내용을 입력해주세요.');
        document.getElementById('content').focus();
        return false;
    }
    
    // 제출 확인
    return confirm('회의록을 등록하시겠습니까?');
});

// 페이지 이탈 시 경고 (내용이 있는 경우)
window.addEventListener('beforeunload', function(e) {
    const title = document.getElementById('title').value.trim();
    const content = document.getElementById('content').value.trim();
    
    if (title || content) {
        e.preventDefault();
        e.returnValue = '';
        return '';
    }
});
</script>

<%@ include file="/includes/footer.jsp" %>
