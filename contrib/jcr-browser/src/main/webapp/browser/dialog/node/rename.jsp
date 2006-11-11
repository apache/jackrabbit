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
<h3>Node - Rename</h3>
<hr height="1"/>
<form action="response.txt" id="dialogForm">
<table class="dialog">
<tr>
	<th>Node</th>
	<td><c:out value="${node.path}"/></td>
</tr>
<tr>
	<th>Name</th>
	<td><input type="text" name="name" value="<c:out value="${node.name}"/>"/></td>
</tr>
<tr>
	<td colspan="2">
<input type="button" value="Submit" onClick="submitDialog();"/>
<input type="button" value="Close" onClick="hideDialog();"/>
	</td>
</tr>

</table>
</form>
</div>
</jcr:session>