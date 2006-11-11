<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<% 
pageContext.setAttribute("path", request.getParameter("path")); 
%>
<jcr:session>
<jcr:set var="item" item="${path}"/>

<div class="dialog">
<h3>Property - Remove</h3>
<hr height="1"/>	
<form action="response.txt" id="dialogForm">
<table class="dialog">

<c:if test="${item.node}">
	The given item is not a property	
</c:if>

<c:if test="${!item.node}">

<tr>
	<th>Property</th>
	<td><c:out value="${item.path}"/></td>
</tr>

</c:if>

<tr>
	<td colspan="2" align="center">
		<hr height="1"/>
<c:if test="${!item.node}"><input type="button" value="Submit" onClick="submitDialog();"/></c:if>
		<input type="button" value="Cancel" onClick="hideDialog();"/>
	</td>
</tr>
</table>
</form>
</div>


</jcr:session>