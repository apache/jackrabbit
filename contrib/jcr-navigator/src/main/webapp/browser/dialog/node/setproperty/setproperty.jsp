<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<% 
pageContext.setAttribute("path", request.getParameter("path")); 
pageContext.setAttribute("jcrsession",session.getAttribute("jcr.session"));
%>
<div class="dialog">
<jcr:set var="node" item="${path}"/>
<c:if test="${!node.node}">
	<jcr:set var="node" item="${node.parent}"/>
</c:if>
<h3>Node - Set <c:out value="${type}"/> property</h3>
<hr height="1"/>	
<form action="<c:url value="/command/node/setproperty"/>" id="dialogForm" 
method="POST" onsubmit="return false;">
<table class="dialog">
<tr>
	<th width="100">Parent</th>
	<td>
	<input type="hidden" name="parentPath" value="<c:out value="${node.path}"/>"/>
	<c:out value="${node.path}"/>
	</td>
</tr>
<tr>
	<th>Name</th>
	<td><input type="text" name="name"/></td>
</tr>
<tr>
	<th>Value</th>
	<td><c:out value="${editor}" escapeXml="false"/></td>
</tr>
<tr>
	<td colspan="2" align="center">
		<hr height="1"/>
		<input type="button" value="Submit" onClick="internalSubmitDialog();"/>
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