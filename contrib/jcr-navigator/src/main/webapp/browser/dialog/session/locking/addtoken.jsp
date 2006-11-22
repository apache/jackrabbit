<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<div class="dialog">
<h3>Session - Add Lock Token</h3>
<hr height="1"/>	
<form action="<c:url value="/command/session/locking/addtoken" />" id="dialogForm" 
method="POST" onsubmit="return false;">
<table class="dialog">
<tr>
	<th height="25" width="100">Token</th>
	<td><input type="text" name="token" value=""/></td>
</tr>
<tr>
	<td colspan="2" align="center">
		<hr height="1"/>	
		<input type="button" value="Submit" onClick="submitDialog();"/>
		<input type="button" value="Cancel" onClick="hideDialog();"/>
	</td>
</tr>
</table>
</form>
</div>
