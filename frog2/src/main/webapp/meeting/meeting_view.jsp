<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="pageTitle" value="${meeting.title}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-meeting page-meeting-view" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/meeting_view.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/meeting_view.js" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>

<c:url var="meetingListReturnUrl" value="/meeting">
    <c:param name="view" value="list" />
    <c:if test="${not empty param.returnPage}"><c:param name="page" value="${param.returnPage}" /></c:if>
</c:url>
<c:url var="meetingEditUrl" value="/meeting">
    <c:param name="view" value="edit" />
    <c:param name="id" value="${meeting.meetingId}" />
    <c:if test="${not empty param.returnPage}"><c:param name="returnPage" value="${param.returnPage}" /></c:if>
</c:url>

<div class="meeting-view content-management content-shell" data-context-path="<c:out value='${pageContext.request.contextPath}' />" data-meeting-id="<c:out value='${meeting.meetingId}' />">
    <nav class="back-navigation" aria-label="회의록 상세 이동">
        <a href="<c:out value='${meetingListReturnUrl}' />" class="back-link">
            <i class="fas fa-arrow-left" aria-hidden="true"></i>
            회의록 목록
        </a>
    </nav>
    <t:pageHeader>
        <jsp:attribute name="title"><i class="fas fa-file-alt"></i> <c:out value="${meeting.title}" /></jsp:attribute>
        <jsp:attribute name="subtitle">
            <span class="meta-item"><i class="fas fa-tag"></i> <span class="type-badge ui-badge ui-badge--neutral"><c:out value="${meeting.meetingTypeLabel}" /></span></span>
            <span class="meta-item"><i class="fas fa-calendar"></i> <fmt:formatDate value="${meeting.meetingDatetime}" pattern="yyyy-MM-dd HH:mm"/></span>
            <span class="meta-item"><i class="fas fa-user"></i> <c:out value="${meeting.authorName}" /></span>
        </jsp:attribute>
        <jsp:attribute name="actions">
            <c:if test="${meeting.authorId == user.userId}">
                <a href="<c:out value='${meetingEditUrl}' />"
                   class="ui-button button--secondary button--md"><i class="fas fa-edit"></i> 수정하기</a>
            </c:if>
        </jsp:attribute>
    </t:pageHeader>

    <t:flashMessages />

    <!-- 회의록 내용 -->
    <section class="meeting-content ui-work-surface" aria-labelledby="meetingContentTitle">
        <t:sectionHeader className="content-header">
            <jsp:attribute name="title">
                <h2 class="content-title ui-section-title" id="meetingContentTitle">
                    <i class="fas fa-file-alt"></i>
                    회의 내용
                </h2>
            </jsp:attribute>
        </t:sectionHeader>
        <div class="content-body ui-section-body">
            <div class="meeting-text" data-meeting-text><c:out value="${meeting.content}" /></div>
        </div>
    </section>

    <!-- 댓글 섹션 -->
    <div class="comments-section ui-work-surface" id="comments">
        <t:sectionHeader className="comments-header">
            <jsp:attribute name="title">
                <h2 class="comments-title ui-section-title">
                    <i class="fas fa-comments"></i>
                    댓글
                </h2>
            </jsp:attribute>
            <jsp:attribute name="actions">
                <span class="comment-count">최근 <c:out value="${comments.size()}" />개</span>
            </jsp:attribute>
        </t:sectionHeader>

        <!-- 댓글 작성 폼 -->
        <div class="comment-form ui-section-body">
            <form id="commentForm" class="ui-form">
                <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
                <label for="commentContent" class="sr-only">댓글 내용</label>
                <textarea id="commentContent" class="comment-textarea"
                          rows="3"
                          aria-describedby="commentHelp"
                          placeholder="회의 결과에 대한 의견이나 후속 내용을 남겨주세요." required></textarea>
                <div class="comment-form-actions">
                    <span class="comment-help" id="commentHelp">Enter로 줄바꿈할 수 있습니다.</span>
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
                                                id="edit-comment-<c:out value='${comment.commentId}' />"
                                                class="comment-btn edit ui-button button--secondary button--sm"
                                                aria-controls="edit-form-<c:out value='${comment.commentId}' />"
                                                aria-expanded="false">
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

                            <div class="comment-edit-form"
                                 id="edit-form-<c:out value='${comment.commentId}' />"
                                 role="region"
                                 aria-labelledby="edit-comment-<c:out value='${comment.commentId}' />"
                                 hidden>
                                <textarea class="comment-edit-textarea"
                                          id="edit-content-<c:out value='${comment.commentId}' />"
                                          aria-label="댓글 수정 내용"><c:out value="${comment.content}" /></textarea>
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
                        <p>첫 댓글을 남겨보세요.</p>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

        <c:if test="${commentPage.hasOlder or not empty param.commentBefore}">
            <nav class="comment-pagination" aria-label="댓글 페이지 이동">
                <c:if test="${not empty param.commentBefore}">
                    <c:url var="latestCommentsUrl" value="/meeting">
                        <c:param name="view" value="view" />
                        <c:param name="id" value="${meeting.meetingId}" />
                        <c:if test="${not empty param.returnPage}"><c:param name="returnPage" value="${param.returnPage}" /></c:if>
                    </c:url>
                    <a class="ui-button button--secondary button--sm"
                       href="<c:out value='${latestCommentsUrl}' />#comments">최신 댓글</a>
                </c:if>
                <c:if test="${commentPage.hasOlder}">
                    <c:url var="olderCommentsUrl" value="/meeting">
                        <c:param name="view" value="view" />
                        <c:param name="id" value="${meeting.meetingId}" />
                        <c:param name="commentBefore" value="${commentPage.nextBeforeCommentId}" />
                        <c:if test="${not empty param.returnPage}"><c:param name="returnPage" value="${param.returnPage}" /></c:if>
                    </c:url>
                    <a class="ui-button button--secondary button--sm"
                       href="<c:out value='${olderCommentsUrl}' />#comments">이전 댓글 <c:out value="${commentPage.pageSize}" />개</a>
                </c:if>
            </nav>
        </c:if>
    </div>
</div>


<%@ include file="/includes/footer.jsp" %>
