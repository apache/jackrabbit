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
<h3>Node - Add mixin</h3>
<hr height="1"/>
<form action="<c:url value="/command/node/addmixin"/>" id="dialogForm" 
method="POST" onsubmit="return false;">
<table class="dialog">
<tr>
	<th>Node</th>
	<td>
	<input type="hidden" name="path" value="<c:out value="${node.path}"/>" />
	<c:out value="${node.path}"/>
	</td>
</tr>
<tr>
	<th>Mixin</th>
	<td>
	<select name="mixin">
<c:forEach var="mixin" items="${jcrsession.nodeTypeManager.mixinNodeTypes}">
	<option selected="selected"><c:out value="${mixin.name}"/></option>
</c:forEach>
	</select>
	</td>
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
	var node = dojo.widget.manager.getWidgetById(currentItem);
	var nodes = new Array(node);
	submitDialog(nodes);
}
</script>
