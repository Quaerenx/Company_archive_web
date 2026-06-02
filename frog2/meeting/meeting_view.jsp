<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="pageTitle" value="${meeting.title}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>

<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/meeting.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/customers.css">

<div class="meeting-view customer-management">
    <t:pageHeader>
        <jsp:attribute name="title"><i class="fas fa-file-alt"></i> ${meeting.title}</jsp:attribute>
        <jsp:attribute name="subtitle">
            <span class="meta-item"><i class="fas fa-tag"></i> <span class="type-badge type-${meeting.meetingType.toLowerCase()}">${meeting.meetingType}</span></span>
            <span class="meta-item"><i class="fas fa-calendar"></i> <fmt:formatDate value="${meeting.meetingDatetime}" pattern="yyyy년 MM월 dd일 HH:mm"/></span>
            <span class="meta-item"><i class="fas fa-user"></i> ${meeting.authorName}</span>
        </jsp:attribute>
        <jsp:attribute name="actions">
            <c:if test="${meeting.authorId == user.userId}">
                <a href="${pageContext.request.contextPath}/meeting?view=edit&id=${meeting.meetingId}" class="add-button"><i class="fas fa-edit"></i> 수정하기</a>
            </c:if>
            <a href="${pageContext.request.contextPath}/meeting?view=list" class="add-button secondary"><i class="fas fa-list"></i> 목록</a>
        </jsp:attribute>
    </t:pageHeader>
    <!-- 뒤로 가기 -->
    <div class="back-navigation">
        <a href="${pageContext.request.contextPath}/meeting?view=list" class="back-link">
            <i class="fas fa-arrow-left"></i>
            회의록 목록으로 돌아가기
        </a>
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
    
    <!-- 회의록 내용 -->
    <div class="meeting-content">
        <div class="content-header">
            <h2 class="content-title">
                <i class="fas fa-file-alt"></i>
                회의 내용
            </h2>
        </div>
        <div class="content-body">
            <div class="meeting-text">${meeting.content}</div>
        </div>
    </div>
    
    <!-- 댓글 섹션 -->
    <div class="comments-section">
        <div class="comments-header">
            <h2 class="comments-title">
                <i class="fas fa-comments"></i>
                댓글
            </h2>
            <span class="comment-count">${comments.size()}개</span>
        </div>
        
        <!-- 댓글 작성 폼 -->
        <div class="comment-form">
            <form id="commentForm">
                <textarea id="commentContent" class="comment-textarea" 
                          placeholder="댓글을 작성해주세요..." required></textarea>
                <div class="comment-form-actions">
                    <button type="submit" class="btn-comment">
                        <i class="fas fa-paper-plane"></i>
                        댓글 등록
                    </button>
                </div>
            </form>
        </div>
        
        <!-- 댓글 목록 -->
        <div class="comments-list">
            <c:choose>
                <c:when test="${not empty comments}">
                    <c:forEach var="comment" items="${comments}">
                        <div class="comment-item" data-comment-id="${comment.commentId}">
                            <div class="comment-header">
                                <div>
                                    <div class="comment-author">${comment.authorName}</div>
                                    <div class="comment-date">
                                        <fmt:formatDate value="${comment.createdAt}" pattern="yyyy-MM-dd HH:mm"/>
                                        <c:if test="${comment.updatedAt != comment.createdAt}">
                                            (수정됨)
                                        </c:if>
                                    </div>
                                </div>
                                <c:if test="${comment.authorId == user.userId}">
                                    <div class="comment-actions">
                                        <button type="button" class="comment-btn edit" onclick="editComment(${comment.commentId})">
                                            <i class="fas fa-edit"></i> 수정
                                        </button>
                                        <button type="button" class="comment-btn delete" onclick="deleteComment(${comment.commentId})">
                                            <i class="fas fa-trash"></i> 삭제
                                        </button>
                                    </div>
                                </c:if>
                            </div>
                            
                            <div class="comment-content" id="content-${comment.commentId}">${comment.content}</div>
                            
                            <div class="comment-edit-form" id="edit-form-${comment.commentId}">
                                <textarea class="comment-edit-textarea" id="edit-content-${comment.commentId}">${comment.content}</textarea>
                                <div class="comment-edit-actions">
                                    <button type="button" class="btn-save" onclick="saveComment(${comment.commentId})">저장</button>
                                    <button type="button" class="btn-cancel-edit" onclick="cancelEdit(${comment.commentId})">취소</button>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                    <div class="empty-comments">
                        <i class="fas fa-comment-slash"></i>
                        <p>등록된 댓글이 없습니다.</p>
                        <p>첫 번째 댓글을 작성해보세요!</p>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<!-- 삭제 확인 폼 (숨김) -->
<form id="deleteForm" method="post" action="${pageContext.request.contextPath}/meeting" class="d-none">
    <input type="hidden" name="action" value="delete">
    <input type="hidden" name="meeting_id" value="${meeting.meetingId}">
</form>

<script>
// 회의록 삭제 확인
function confirmDelete() {
    if (confirm('정말로 이 회의록을 삭제하시겠습니까?\n삭제된 데이터는 복구할 수 없습니다.')) {
        document.getElementById('deleteForm').submit();
    }
}

// 댓글 등록
document.getElementById('commentForm').addEventListener('submit', function(e) {
    e.preventDefault();
    
    const content = document.getElementById('commentContent').value.trim();
    if (!content) {
        alert('댓글 내용을 입력해주세요.');
        return;
    }
    
    const submitButton = this.querySelector('.btn-comment');
    submitButton.disabled = true;
    submitButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 등록 중...';
    
    // AJAX 요청으로 댓글 등록
    fetch('${pageContext.request.contextPath}/comment', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: 'action=add&meeting_id=${meeting.meetingId}&content=' + encodeURIComponent(content)
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            location.reload(); // 페이지 새로고침으로 댓글 목록 갱신
        } else {
            alert(data.message || '댓글 등록 중 오류가 발생했습니다.');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('댓글 등록 중 오류가 발생했습니다.');
    })
    .finally(() => {
        submitButton.disabled = false;
        submitButton.innerHTML = '<i class="fas fa-paper-plane"></i> 댓글 등록';
    });
});

// 댓글 수정 모드 전환
function editComment(commentId) {
    const contentDiv = document.getElementById('content-' + commentId);
    const editForm = document.getElementById('edit-form-' + commentId);
    
    contentDiv.classList.add('d-none');
    editForm.classList.remove('d-none');
    
    // 텍스트 영역에 포커스
    document.getElementById('edit-content-' + commentId).focus();
}

// 댓글 수정 취소
function cancelEdit(commentId) {
    const contentDiv = document.getElementById('content-' + commentId);
    const editForm = document.getElementById('edit-form-' + commentId);
    
    contentDiv.classList.remove('d-none');
    editForm.classList.add('d-none');
}

// 댓글 수정 저장
function saveComment(commentId) {
    const newContent = document.getElementById('edit-content-' + commentId).value.trim();
    if (!newContent) {
        alert('댓글 내용을 입력해주세요.');
        return;
    }
    
    // AJAX 요청으로 댓글 수정
    fetch('${pageContext.request.contextPath}/comment', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: 'action=update&comment_id=' + commentId + '&content=' + encodeURIComponent(newContent)
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            location.reload(); // 페이지 새로고침으로 댓글 목록 갱신
        } else {
            alert(data.message || '댓글 수정 중 오류가 발생했습니다.');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('댓글 수정 중 오류가 발생했습니다.');
    });
}

// 댓글 삭제
function deleteComment(commentId) {
    if (!confirm('정말로 이 댓글을 삭제하시겠습니까?')) {
        return;
    }
    
    // AJAX 요청으로 댓글 삭제
    fetch('${pageContext.request.contextPath}/comment', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: 'action=delete&comment_id=' + commentId
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            location.reload(); // 페이지 새로고침으로 댓글 목록 갱신
        } else {
            alert(data.message || '댓글 삭제 중 오류가 발생했습니다.');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('댓글 삭제 중 오류가 발생했습니다.');
    });
}
</script>

<%@ include file="/includes/footer.jsp" %>
