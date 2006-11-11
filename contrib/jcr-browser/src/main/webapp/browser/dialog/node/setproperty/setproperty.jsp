<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<%@page import="javax.jcr.PropertyType"%>
<% 
pageContext.setAttribute("path", request.getParameter("path")); 
%>
<jcr:session>
<jcr:set var="node" item="${path}"/>
<c:if test="${!node.node}">
	<jcr:set var="node" item="${node.parent}"/>
</c:if>
<div class="dialog">
<h3>Node - Set <c:out value="${type}"/> property</h3>
<hr height="1"/>	
<form action="response.txt" id="dialogForm">
<input type="hidden" name="type" value="<%= PropertyType.STRING %>"/>
<table class="dialog">
<tr>
	<th width="100">Parent</th>
	<td><c:out value="${node.path}"/></td>
</tr>
<tr>
	<th>Namespace</th>
	<td>
	<select name="namespace">
	<c:forEach var="prefix" items="${jcrsession.namespacePrefixes}">
<c:if test="${empty prefix}"><option selected="selected"><c:out value="${prefix}"/></option></c:if>
<c:if test="${!empty prefix}"><option><c:out value="${prefix}"/></option></c:if>		
	</c:forEach>
	</select>
	</td>
</tr>
<tr>
	<th>Name</th>
	<td><input type="text" name="name" value=""/></td>
</tr>
<tr>
	<th>Value</th>
	<td><c:out value="${editor}" escapeXml="false"/></td>
</tr>
<tr>
	<td colspan="2" align="center">
		<hr height="1"/>
		<input type="button" value="Submit" onClick="submitDialog();"/>
		<input type="button" value="Cancel" onClick="hideDialog();"/>
	</td>
</tr>
</table>
</form>
</div>
</jcr:session>