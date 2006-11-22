<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<div class="dialog">
<h3>Session - Refresh</h3>
<hr height="1"/>	
<form action="<c:url value="/command/session/refresh"/>" id="dialogForm" 
method="POST" onsubmit="return false;">
<table class="dialog">
<tr>
	<th height="25" width="100">Keep changes</th>
	<td><input type="checkbox" name="keepChanges" value="true"/></td>
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
	var root = dojo.widget.manager.getWidgetById('/');
	var nodes = new Array(root);
	submitDialog(nodes);
}
</script>