<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<jcr:session>
<div class="dialog">
<h3>Session - Show tokens</h3>
<hr height="1"/>	
<form action="response.txt" id="dialogForm">
<table class="dialog">
<c:forEach var="token" items="${jcrsession.lockTokens}">
<tr>
	<td><c:out value="${token}"/></td>
</tr>
</c:forEach>
<tr>
	<td align="center">
		<hr height="1"/>	
		<input type="button" value="Cancel" onClick="hideDialog();"/>
	</td>
</tr>
</table>
</form>
</div>
</jcr:session>