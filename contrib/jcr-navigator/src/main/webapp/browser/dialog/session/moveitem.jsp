<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<%
pageContext.setAttribute("jcrsession",session.getAttribute("jcr.session"));
%>
<div class="dialog">
<h3>Session - Move node</h3>
<hr height="1"/>	
<form action="<c:url value="/command/session/move" />" id="dialogForm" 
method="POST" onsubmit="return false;">
<table class="dialog">
<tr>
	<th height="25" width="50">From</th>
	<td>
	<input type="hidden" name="persistent" value="false">
	<input type="hidden" name="srcAbsPath" value="<%= request.getParameter("path")%>">
	<%= request.getParameter("path")%>
	</td>
</tr>
<tr>
	<th>To</th>
	<td height="300">
		<input type="hidden" name="destAbsPath" id="destAbsPath" value="">
		<div class="targetTreeContainer">
		<div id="targetTreeDiv"></div>
		</div>
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
var targetController = dojo.widget.createWidget("TreeLoadingControllerV3");
var targetSelector = dojo.widget.createWidget("TreeSelectorV3",{widgetId: "targetSelector"});
var treeEmphaseOnSelect = dojo.widget.createWidget("TreeEmphasizeOnSelect",{selector:targetSelector.widgetId});
targetController.RpcUrl="tree.jsp?prefix=target";
var treeNodes = [{title: "Root", widgetId: "target/", isFolder: true}] ;
var targetTree = dojo.widget.createWidget("TreeV3", {listeners: [targetSelector.widgetId, targetController.widgetId]},dojo.byId("targetTreeDiv"));
targetTree.setChildren(treeNodes);

function internalSubmitDialog() {

	var targetPath = dojo.widget.manager.getWidgetById('targetSelector').selectedNodes[0].objectId ;

	// nodes to refresh 
	var parentFrom = dojo.widget.manager.getWidgetById(currentItem).parent ;
	var parentTo =  dojo.widget.manager.getWidgetById(targetPath) ;
	var nodes = new Array(parentFrom, parentTo);

	if (targetPath.charAt(targetPath.length-1) != '/') {
		targetPath = targetPath + '/' ;
	} 
	document.getElementById('destAbsPath').value = targetPath;	

	submitDialog(nodes);
}

</script>