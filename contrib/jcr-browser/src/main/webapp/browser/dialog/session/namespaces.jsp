<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<%
pageContext.setAttribute("jcrsession",session.getAttribute("jcr.session"));
%>
<%@page import="javax.jcr.Session"%>
<div class="dialog">
<h3>Registered namespaces</h3>
<hr height="1"/>	
<table class="dialog">
<tr>
	<th>prefix</th>
	<th>uri</th>
</tr>
<c:forEach var="prefix" items="${jcrsession.namespacePrefixes}">
<tr>
	<td><c:out value="${prefix}"/></td>
	<td><%= ((Session) pageContext.getAttribute("jcrsession")).getNamespaceURI(pageContext.getAttribute("prefix").toString()) %></td>
</tr>
</c:forEach>
<tr>
	<td colspan="2" align="center">
		<hr height="1"/>	
		<input type="button" value="Close" onClick="hideDialog();"/>
	</td>
</tr>
</table>
</div>