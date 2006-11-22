<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<% 
pageContext.setAttribute("path", request.getParameter("path")); 
pageContext.setAttribute("jcrsession",session.getAttribute("jcr.session"));
%>
<div class="dialog">
<jcr:set var="node" item="${path}"/>
<h3>Workspace - Rename node</h3>
<hr height="1"/>	
<form action="<c:url value="/command/workspace/rename"/>" id="dialogForm" 
method="POST" onsubmit="return false;">
<table class="dialog">
<tr>
	<th height="25" width="50">From</th>
	<td>
	<input type="hidden" name="persistent" value="true">	
	<input type="hidden" name="srcPath" value="<%= request.getParameter("path")%>">
	<c:out value="${node.name}"/>
	</td>
</tr>
<tr>
	<th>To</th>
	<td><input type="text" name="destPath" value="<c:out value="${node.name}"/>"/></td>
</tr>
<tr>
	<td colspan="2" align="center">
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
    var node = dojo.widget.manager.getWidgetById(currentItem);
    var nodes = new Array(node.parent);
	submitDialog(nodes); 
}

</script>