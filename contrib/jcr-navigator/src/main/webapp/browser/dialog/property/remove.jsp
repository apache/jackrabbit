<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<% 
pageContext.setAttribute("path", request.getParameter("path")); 
pageContext.setAttribute("jcrsession",session.getAttribute("jcr.session"));
%>
<jcr:set var="item" item="${path}"/>
<div class="dialog">
<h3>Property - Remove</h3>
<hr height="1"/>	
<form action="<c:url value="/command/property/remove" />" id="dialogForm" 
method="POST" onsubmit="return false;">
<table class="dialog">

<c:if test="${item.node}">
	The given item is not a property	
</c:if>

<c:if test="${!item.node}">

<tr>
	<th>Property</th>
	<td>
	<input type="hidden" name="path" value="<c:out value="${item.path}"/>"/>	
	<c:out value="${item.path}"/></td>
</tr>

</c:if>

<tr>
	<td colspan="2" align="center">
		<hr height="1"/>
<c:if test="${!item.node}">
<input type="button" value="Submit" onClick="internalSubmitDialog();"/>
</c:if>
		<input type="button" value="Cancel" onClick="hideDialog();"/>
	</td>
</tr>
</table>
</form>
</div>
<script language="JavaScript" type="text/javascript">
function internalSubmitDialog() {
	// nodes to refresh 
	var parent = dojo.widget.manager.getWidgetById(currentItem);
	var nodes = new Array(parent);
	submitDialog(nodes);
}
</script>