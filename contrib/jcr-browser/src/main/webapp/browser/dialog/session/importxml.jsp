<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<div class="dialog">
<h3>Session - Import xml</h3>
<hr height="1"/>	
<form action="<c:url value="/command/workspace/importxml.iframe?flavor=text/html" />" 
	id="dialogForm"
	method="POST" 
	enctype="multipart/form-data" 
	onsubmit="return false;">
<table class="dialog">
<tr>
	<th height="25" width="100">UUID Behavior</th>
	<td>
		<input type="hidden" name="persistent" value="false"/>
		<input type="hidden" name="destJcrPath" value="<%= request.getParameter("path")%>"/>
		<select name="uuidBehaviour">
		<option value="0">Create new</option>
		<option value="1">Remove existing</option>
		<option value="2">Replace existing</option>
		<option value="3">Collision throw</option>
		</select>	
	</td>
</tr>
<tr>
	<th height="25" width="100">XML file</th>
	<td>
		<input type="file" name="file"/>
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
    var node = dojo.widget.manager.getWidgetById(currentItem);
    var nodes = new Array(node);
	submitDialog(nodes); 
}

</script>
