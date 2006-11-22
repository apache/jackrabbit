<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<%@page import="javax.jcr.NamespaceRegistry"%>
<%
pageContext.setAttribute("jcrsession",session.getAttribute("jcr.session"));
%>
<div class="dialog">
<h3>Unregister namespace</h3>
<hr height="1"/>	
<form action="<c:url value="/command/workspace/unregisternamespace"/>" id="dialogForm" 
method="POST" onsubmit="return false;">
<table class="dialog">
<tr>
	<th>Prefix: Uri</th>
</tr>
<tr>
	<td>
		<select name="prefix">
			<c:set value="${jcrsession.workspace.namespaceRegistry}" var="namespaceRegistry"/>
			<c:forEach var="prefix" items="${namespaceRegistry.prefixes}">
<%String uri = ((NamespaceRegistry) pageContext.getAttribute("namespaceRegistry")).getURI(pageContext.getAttribute("prefix").toString()) ;%>			
				<option value="<c:out value="${prefix}"/>"><c:out value="${prefix}"/>: <%= uri %></option>
			</c:forEach>			
		</select>
	</td>
</tr>

<tr>
	<td align="center">
		<hr height="1"/>	
		<input type="button" value="Submit" onClick="submitDialog();"/>
		<input type="button" value="Cancel" onClick="hideDialog();"/>
	</td>
</tr>
</table>
</form>
</div>
