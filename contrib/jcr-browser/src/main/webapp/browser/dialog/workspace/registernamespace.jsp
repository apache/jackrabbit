<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<%
pageContext.setAttribute("jcrsession",session.getAttribute("jcr.session"));
%>
<div class="dialog">
<h3>Register namespace</h3>
<hr height="1"/>	
<form action="<c:url value="/command/workspace/registernamespace"/>" id="dialogForm" 
method="POST" onsubmit="return false;">
<table class="dialog">
<tr>
	<th height="25" width="50">prefix</th>
	<td><input type="text" name="prefix" value=""/></td>
</tr>
<tr>
	<th>Uri</th>
	<td><input type="text" name="uri" value=""/></td>
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
