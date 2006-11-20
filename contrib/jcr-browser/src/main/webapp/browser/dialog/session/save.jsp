<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<div class="dialog">
<h3>Session - Save</h3>
<hr height="1"/>	
<form action="<c:url value="/command/session/save"/>" id="dialogForm" 
method="POST" onsubmit="return false;">
<table class="dialog">
<tr>
	<td colspan="2" align="center">
		<input type="button" value="Submit" onClick="submitDialog();"/>
		<input type="button" value="Cancel" onClick="hideDialog();"/>
	</td>
</tr>
</table>
</form>
</div>
