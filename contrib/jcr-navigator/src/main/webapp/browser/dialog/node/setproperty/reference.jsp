<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<c:set var="type" scope="request">reference</c:set>
<c:set var="editor" scope="request">
<input type="hidden" name="type" value="Reference"/>
<input type="hidden" name="formValue" value=""/>
<table>
	<tr><td height="200" width="300">
<div dojoType="TreeLoadingControllerV3" widgetId="targetTreeController" RpcUrl="tree.jsp?prefix=target"></div>
<div dojoType="TreeSelectorV3" widgetId="targetSelector"></div>	
<div dojoType="TreeDeselectOnDblselect" selector="targetSelector"></div>
<div class="targetTreeContainer" >
	<div dojoType="TreeV3" widgetId="targetTree" listeners="targetTreeController;targetSelector">
	    <div dojoType="TreeNodeV3" title="Root" isFolder="true" widgetId="target/" objectId="/">
	    </div>
	</div>
</div>	
	</td></tr>
</table>
</c:set>
<c:import url="setproperty.jsp"></c:import>
<script language="JavaScript" type="text/javascript">

</script>
