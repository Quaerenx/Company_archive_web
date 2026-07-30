<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="pageTitle" value="${meeting.title}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers page-meeting" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/meeting.css,/resources/css/pages/meeting_view.css,/resources/css/pages/customers.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/meeting_view.js" scope="request" />
<c:set var="meetingTypeValue" value="${empty meeting.meetingType ? 'other' : fn:toLowerCase(meeting.meetingType)}" />
<c:set var="meetingTypeLabel" value="${empty meeting.meetingType ? '기타' : meeting.meetingType}" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>


<div class="meeting-view customer-management content-shell" data-context-path="<c:out value='${pageContext.request.contextPath}' />" data-meeting-id="<c:out value='${meeting.meetingId}' />">
    <t:pageHeader>
        <jsp:attribute name="title"><i class="fas fa-file-alt"></i> <c:out value="${meeting.title}" /></jsp:attribute>
        <jsp:attribute name="subtitle">
            <span class="meta-item"><i class="fas fa-tag"></i> <span class="type-badge ui-badge type-<c:out value="${meetingTypeValue}" />"><c:out value="${meetingTypeLabel}" /></span></span>
            <span class="meta-item"><i class="fas fa-calendar"></i> <fmt:formatDate value="${meeting.meetingDatetime}" pattern="yyyy년 MM월 dd일 HH:mm"/></span>
            <span class="meta-item"><i class="fas fa-user"></i> <c:out value="${meeting.authorName}" /></span>
        </jsp:attribute>
        <jsp:attribute name="actions">
            <c:if test="${meeting.authorId == user.userId}">
                <a href="${pageContext.request.contextPath}/meeting?view=edit&id=${meeting.meetingId}"
                   class="add-button ui-button button--primary button--md"><i class="fas fa-edit"></i> 수정하기</a>
            </c:if>
            <a href="${pageContext.request.contextPath}/meeting?view=list"
               class="add-button secondary ui-button button--secondary button--md"><i class="fas fa-list"></i> 목록</a>
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

    <!-- 회의록 내용 -->
    <div class="meeting-content">
        <div class="content-header">
            <h2 class="content-title">
                <i class="fas fa-file-alt"></i>
                회의 내용
            </h2>
        </div>
        <div class="content-body">
            <div class="meeting-text"><c:out value="${meeting.content}" /></div>
        </div>
    </div>

    <!-- 댓글 섹션 -->
    <div class="comments-section">
        <div class="comments-header">
            <h2 class="comments-title">
                <i class="fas fa-comments"></i>
                댓글
            </h2>
            <span class="comment-count"><c:out value="${comments.size()}" />개</span>
        </div>

        <!-- 댓글 작성 폼 -->
        <div class="comment-form">
            <form id="commentForm" class="ui-form">
                <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
                <textarea id="commentContent" class="comment-textarea"
                          placeholder="댓글을 작성해주세요..." required></textarea>
                <div class="comment-form-actions">
                    <button type="submit"
                            class="btn-comment ui-button button--primary button--md">
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
                        <div class="comment-item" data-comment-id="<c:out value='${comment.commentId}' />">
                            <div class="comment-header">
                                <div>
                                    <div class="comment-author"><c:out value="${comment.authorName}" /></div>
                                    <div class="comment-date">
                                        <fmt:formatDate value="${comment.createdAt}" pattern="yyyy-MM-dd HH:mm"/>
                                        <c:if test="${comment.updatedAt != comment.createdAt}">
                                            (수정됨)
                                        </c:if>
                                    </div>
                                </div>
                                <c:if test="${comment.authorId == user.userId}">
                                    <div class="comment-actions">
                                        <button type="button"
                                                class="comment-btn edit ui-button button--secondary button--sm">
                                            <i class="fas fa-edit"></i> 수정
                                        </button>
                                        <button type="button"
                                                class="comment-btn delete ui-button button--danger button--sm">
                                            <i class="fas fa-trash"></i> 삭제
                                        </button>
                                    </div>
                                </c:if>
                            </div>

                            <div class="comment-content" id="content-<c:out value='${comment.commentId}' />"><c:out value="${comment.content}" /></div>

                            <div class="comment-edit-form" id="edit-form-<c:out value='${comment.commentId}' />">
                                <textarea class="comment-edit-textarea" id="edit-content-<c:out value='${comment.commentId}' />"><c:out value="${comment.content}" /></textarea>
                                <div class="comment-edit-actions">
                                    <button type="button"
                                            class="btn-save ui-button button--primary button--sm">저장</button>
                                    <button type="button"
                                            class="btn-cancel-edit ui-button button--secondary button--sm">취소</button>
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


<%@ include file="/includes/footer.jsp" %>
