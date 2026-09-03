<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

</main>

<c:if test="${not empty vendorScript}">
    <script src="${vendorScript}"></script>
</c:if>

<div id="ui-status-region"
     class="ui-status-region"
     role="status"
     aria-live="polite"
     aria-atomic="true"></div>
<div id="ui-toast-region-polite"
     class="ui-toast-region"
     aria-live="polite"
     aria-atomic="false"
     aria-relevant="additions"></div>
<div id="ui-toast-region-assertive"
     class="ui-toast-region"
     aria-live="assertive"
     aria-atomic="false"
     aria-relevant="additions"></div>

<script src="${pageContext.request.contextPath}/resources/js/ui-system.js?v=${frog2AssetVersion}"></script>
<script src="${pageContext.request.contextPath}/resources/js/ui-table.js?v=${frog2AssetVersion}"></script>
<script src="${pageContext.request.contextPath}/resources/js/session-expiry.js?v=${frog2AssetVersion}"
        data-context-path="<c:out value='${pageContext.request.contextPath}' />"></script>

<c:if test="${not empty pageScript}">
    <c:forTokens items="${pageScript}" delims="," var="script">
        <script src="${pageContext.request.contextPath}${script}?v=${frog2AssetVersion}"></script>
    </c:forTokens>
</c:if>

<%@ include file="/WEB-INF/includes/footer_content.jspf" %>
</body>
</html>
