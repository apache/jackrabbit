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
<h3>Node - Set date property</h3>
<hr height="1"/>	
<form action="<c:url value="/command/node/setdateproperty"/>" id="dialogForm" 
method="POST" onsubmit="return false;">
<table class="dialog">
<tr>
	<th width="100">Parent</th>
	<td>
	<input type="hidden" name="parentPath" value="<c:out value="${node.path}"/>"/>
	<input type="hidden" name="type" value="Date"/>	
	<c:out value="${node.path}"/>
	</td>
</tr>
<tr>
	<th>Name</th>
	<td><input type="text" name="name"/></td>
</tr>
<tr>
	<th>Date</th>
	<td>
	<input 
		dojoType="dropdowndatepicker" 
		containerToggle="wipe" 
		containerToggleDuration="300"
		displayFormat="yyyy/MM/dd"
		name="date"/>
</tr>
<tr>
	<th>Time</th>
	<td>
	<input type="hidden" name="time" id="formTime"/>
	<input 
		dojoType="dropdowntimepicker" 
		lang="en-us" 
		containertoggle="wipe" 
		containerToggleDuration="300"
		widgetId="timeWidget" />
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
	// workaround because the input name is lost for the timepicker widget
	var timeWidget = dojo.widget.manager.getWidgetById('timeWidget');
	document.getElementById('formTime').value= timeWidget.inputNode.value ;

	// nodes to refresh 
	var parent = dojo.widget.manager.getWidgetById(currentItem);
	var nodes = new Array(parent);
	submitDialog(nodes);
}
</script>

