<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<% 
pageContext.setAttribute("path", request.getParameter("path")); 
%>
<div class="dialog">
<jcr:session>
<jcr:set var="node" item="${path}"/>
<h3>Workspace - Rename node</h3>
<hr height="1"/>	
<form action="response.txt" id="dialogForm">
<table class="dialog">
<tr>
	<th height="25" width="50">From</th>
	<td>
	<input type="hidden" name="srcAbsPath" value="<%= request.getParameter("path")%>">
	<c:out value="${node.name}"/>
	</td>
</tr>
<tr>
	<th>To</th>
	<td><input type="text" name="newName" value=""/></td>
</tr>
<tr>
	<td colspan="2" align="center">
		<hr height="1"/>	
		<input type="button" value="Submit" onClick="submitDialog();"/>
		<input type="button" value="Close" onClick="hideDialog();"/>
	</td>
</tr>
</table>
</form>
</jcr:session>
</div>
