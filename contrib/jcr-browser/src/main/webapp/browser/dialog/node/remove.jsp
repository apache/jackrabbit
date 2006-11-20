<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<% 
pageContext.setAttribute("path", request.getParameter("path")); 
pageContext.setAttribute("jcrsession",session.getAttribute("jcr.session"));
%>
<jcr:set var="node" item="${path}"/>
<c:if test="${!node.node}">
	<jcr:set var="node" item="${node.parent}"/>
</c:if>
<div class="dialog">
<h3>Node - Remove</h3>
<hr height="1"/>	
<form action="<c:url value="/command/node/remove" />" id="dialogForm" 
method="POST" onsubmit="return false;">
<table class="dialog">
<tr>
	<th>Node</th>
	<td>
	<input type="hidden" name="path" value="<c:out value="${node.path}"/>"/>
	<c:out value="${node.path}"/>
	</td>
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
	var parent = dojo.widget.manager.getWidgetById(currentItem).parent ;
	var nodes = new Array(parent);
	submitDialog(nodes);
}
</script>