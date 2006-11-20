<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<%
pageContext.setAttribute("jcrsession",session.getAttribute("jcr.session"));
%>
<div class="dialog">
<h3>Session - Remove Lock Token</h3>
<hr height="1"/>	
<form action="<c:url value="/command/session/locking/removetoken" />" id="dialogForm" 
method="POST" onsubmit="return false;">
<table class="dialog">
<tr>
	<th height="25" width="100">Token</th>
	<td>
	<c:if test="${not empty jcrsession.lockTokens}">
	<select type="select" name="token">
		<c:forEach var="token" items="${jcrsession.lockTokens}">
			<option><c:out value="${token}"/></option>
		</c:forEach>	
	</select>
	</c:if>
	</td>
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
