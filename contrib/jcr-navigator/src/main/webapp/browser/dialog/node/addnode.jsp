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
<h3>Node - Add node</h3>
<hr height="1"/>
<form action="<c:url value="/command/node/addnode" />" id="dialogForm" 
method="POST" onsubmit="return false;">
<table class="dialog">
<tr>
	<th>Parent</th>
	<td><c:out value="${node.path}"/></td>
</tr>
<tr>
	<th>Primary node type</th>
	<td>
	<select name="type">
	<c:forEach var="nt" items="${jcrsession.workspace.nodeTypeManager.primaryNodeTypes}">
<c:if test="${nt.name eq 'nt:unstructured'}">
<option value="<c:out value="${nt.name}"/>" selected="selected"><c:out value="${nt.name}"/></option>
</c:if>
<c:if test="${nt.name ne 'nt:unstructured'}">
<option value="<c:out value="${nt.name}"/>"><c:out value="${nt.name}"/></option>
</c:if>
	</c:forEach>
	</select>
	</td>
</tr>
<tr>
	<th>Name</th>
	<td><input type="text" name="relPath" id="relPath"/></td>
</tr>
<tr>
	<td colspan="2">
<hr height="1"/>
<input type="button" value="Submit" onClick="internalSubmitDialog();"/>
<input type="button" value="Close" onClick="hideDialog();"/>
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
	
	if (parent.widgetId == '/') {
		document.getElementById('relPath').value =  '/' + document.getElementById('relPath').value;
	} else {
		document.getElementById('relPath').value =  '<c:out value="${node.path}"/>/' + document.getElementById('relPath').value;
	}
	
	submitDialog(nodes);
}
</script>
