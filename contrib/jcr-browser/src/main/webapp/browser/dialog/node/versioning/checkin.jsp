<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<% 
pageContext.setAttribute("path", request.getParameter("path")); 
%>
<jcr:session>
<jcr:set var="node" item="${path}"/>
<c:if test="${!node.node}">
	<jcr:set var="node" item="${node.parent}"/>
</c:if>
<div class="dialog">
<h3>Node - Checkin</h3>
<hr height="1"/>	
<form action="response.txt" id="dialogForm">
<table class="dialog">
<tr>
	<th>Node</th>
	<td><c:out value="${node.path}"/></td>
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