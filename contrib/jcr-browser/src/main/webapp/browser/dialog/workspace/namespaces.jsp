<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<%@page import="javax.jcr.NamespaceRegistry"%>
<div class="dialog">
<jcr:session>
<h3>Registered namespaces</h3>
<hr height="1"/>	
<table class="dialog">
<tr>
	<th>prefix</th>
	<th>uri</th>
</tr>
<c:set value="${jcrsession.workspace.namespaceRegistry}" var="namespaceRegistry"/>
<c:forEach var="prefix" items="${namespaceRegistry.prefixes}">

<tr>
	<td><c:out value="${prefix}"/></td>
	<td><%= ((NamespaceRegistry) pageContext.getAttribute("namespaceRegistry")).getURI(pageContext.getAttribute("prefix").toString()) %></td>
</tr>
</c:forEach>
<tr>
	<td colspan="2" align="center">
		<hr height="1"/>	
		<input type="button" value="Close" onClick="hideDialog();"/>
	</td>
</tr>
</table>

</jcr:session>
</div>