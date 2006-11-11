<div class="dialog">
<h3>Session - Import xml</h3>
<hr height="1"/>	
<form action="response.txt" id="dialogForm">
<table class="dialog">
<tr>
	<th height="25" width="100">UUID Behavior</th>
	<td>
		<select name="uuidBehavior">
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
		<input type="button" value="Submit" onClick="submitDialog();"/>
		<input type="button" value="Cancel" onClick="hideDialog();"/>
	</td>
</tr>
</table>
</form>
</div>
