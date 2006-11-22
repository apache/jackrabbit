<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<%
pageContext.setAttribute("jcrsession",session.getAttribute("jcr.session"));
%>
<div class="dialog">
<h3>Workspace - Accessible workspaces</h3>
<hr height="1"/>
<table class="dialog">
<c:forEach var="w" items="${jcrsession.workspace.accessibleWorkspaceNames}">
<tr><td><c:out value="${w}"/></td></tr>
</c:forEach>
</table>
<hr height="1"/>
<input type="button" value="Close" onClick="hideDialog();"/>
</div>
