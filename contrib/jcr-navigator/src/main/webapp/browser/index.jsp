<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<head>
<title>JCR Browser</title>

<script type="text/javascript">
	var djConfig = {isDebug: true};
	//djConfig.debugAtAllCosts = true;
</script>

<script type="text/javascript" src="<c:url value="/dojo/dojo.js"/>"></script>
<script type="text/javascript" src="<c:url value="/browser/custom.js"/>"></script>

<script language="JavaScript" type="text/javascript">
	dojo.require("dojo.io.*");
	dojo.require("dojo.event.*");
	dojo.require("dojo.io.ScriptSrcIO");
	dojo.require("dojo.io.IframeIO");	

	dojo.require("dojo.lfx.rounded");

	dojo.require("dojo.widget.TreeV3");
	dojo.require("dojo.widget.TreeNodeV3");
	dojo.require("dojo.widget.TreeBasicControllerV3");
	dojo.require("dojo.widget.TreeLoadingControllerV3");
	dojo.require("dojo.widget.TreeEmphaseOnSelect");	
	dojo.require("dojo.widget.TreeSelectorV3");	
	dojo.require("dojo.widget.TreeDeselectOnDblselect");	
	dojo.require("dojo.widget.TreeContextMenuV3");
	
	dojo.require("dojo.widget.DropdownDatePicker");	
	dojo.require("dojo.widget.Dialog");
	dojo.require("dojo.widget.FilteringTable");
	dojo.require("dojo.widget.Menu2");
	dojo.require("dojo.widget.LayoutContainer");
	dojo.require("dojo.widget.ContentPane");
	dojo.require("dojo.widget.LinkPane");
	dojo.require("dojo.widget.SplitContainer");
	dojo.require("dojo.widget.ColorPalette");
	dojo.require("dojo.widget.TabContainer");
	dojo.require("dojo.widget.Toaster");	

	dojo.hostenv.writeIncludes();

    var reporter = function(reporter) {
            this.go = function(message) {
			var node = message.node ;
            		onTreeNodeSelected(node.widgetId);
            }
    }

	var myDojoTreeListener = {
	    nodeCollapsed: function(message){
		var node = message.source ;
		node.destroyChildren();
		node.state = node.loadStates.UNCHECKED;
		node.setFolder();
	    }
	};

    dojo.addOnLoad(function(){
    
        var selector = dojo.widget.manager.getWidgetById('selector');

        dojo.event.topic.subscribe(
        	selector.eventNames['select'],new reporter('selector'),'go');


		var firstTree = dojo.widget.manager.getWidgetById('firstTree');
		dojo.event.topic.subscribe(
			firstTree.eventNames['afterCollapse'],myDojoTreeListener,"nodeCollapsed");
	
		// Load item list
		refreshDescription('/');
		refreshList('/');
		
    });	
	
</script>

<link href="<c:url value="/style.css"/>" type="text/css" rel="stylesheet" />
  <style>
    html, body{	
		width: 100%;	/* make the body expand to fill the visible window */
		height: 100%;
		overflow: hidden;	/* erase window level scrollbars */
		padding: 0 0 0 0;
		margin: 0 0 0 0;
		font: 12px "Trebuchet MS", Verdana, Arial, Helvetica, sans-serif;
    }

    #roundMePreview{
		margin:20px;
		padding:10px;
		border:2px solid green;
		width:90%;
		background-color:#fff;
	}

   </style>
</head>
<body>

<a dojoType="dialog" id="dialog" toggle="wipe" toggleDuration="250" executeScripts="true" scriptSeparation="false"></a>

<!-- T R E E    C O N T R O L L ER -->
<div dojoType="TreeLoadingControllerV3" widgetId="treeController" RpcUrl="tree.jsp"></div>
<div dojoType="TreeSelectorV3" widgetId="selector"></div>	
<div dojoType="TreeDeselectOnDblselect" selector="selector"></div>
<div dojoType="TreeEmphaseOnSelect" selector="selector"></div>
<div dojoType="TreeContextMenuV3" toggle="explode" contextMenuForWindow="false" widgetId="treeContextMenu">
	<div dojoType="TreeMenuItemV3" widgetId="treeContextMenuAddNode" caption="Add node" onClick="showDialog('node/addnode');"></div>
	<div dojoType="TreeMenuItemV3" widgetId="treeContextMenuRemove" caption="Remove" onClick="showDialog('node/remove');" iconSrc="<c:url value="/images/x.gif"/>"></div>
	<div dojoType="TreeMenuItemV3" widgetId="treeContextMenuRename" caption="Rename" onClick="showDialog('session/renameitem');"></div>
	<div dojoType="TreeMenuItemV3" widgetId="treeContextMenuSave" caption="Save" onClick="showDialog('node/save');"></div>
	<div dojoType="TreeMenuItemV3" widgetId="treeContextMenuSetProperty" caption="Set property" submenuId="setPropertyMenu"></div>
</div>
<!-- E N D    T R E E    C O N T R O L L ER -->

<div dojoType="LayoutContainer"
	layoutChildPriority='top-bottom'
	style="width: 100%; height: 100%;">

	<div dojoType="ContentPane" layoutAlign="top" style="background-color: #274383; color: white;">

		<div dojoType="PopupMenu2" widgetId="setPropertyMenu">
			<div dojoType="MenuItem2" caption="Binary" onClick="showDialog('node/setproperty/binary');"></div>
			<div dojoType="MenuItem2" caption="Boolean" onClick="showDialog('node/setproperty/boolean');"></div>
			<div dojoType="MenuItem2" caption="Date" onClick="showDialog('node/setproperty/date');"></div>
			<div dojoType="MenuItem2" caption="Double" onClick="showDialog('node/setproperty/double');"></div>
			<div dojoType="MenuItem2" caption="Long" onClick="showDialog('node/setproperty/long');"></div>
			<div dojoType="MenuItem2" caption="Name" onClick="showDialog('node/setproperty/name');"></div>
			<div dojoType="MenuItem2" caption="Path" onClick="showDialog('node/setproperty/path');"></div>
			<div dojoType="MenuItem2" caption="Reference" onClick="showDialog('node/setproperty/reference');"></div>
			<div dojoType="MenuItem2" caption="String" onClick="showDialog('node/setproperty/string');"></div>
		</div>

		<div dojoType="PopupMenu2" widgetId="versioningMenu">
			<div dojoType="MenuItem2" caption="Checkin" onClick="showDialog('node/versioning/checkin');"></div>
			<div dojoType="MenuItem2" caption="Checkout" onClick="showDialog('node/versioning/checkout');"></div>
			<div dojoType="MenuItem2" caption="Restore" onClick="showDialog('node/versioning/restore');"></div>
			<div dojoType="MenuSeparator2"></div>
			<div dojoType="MenuItem2" caption="Merge" onClick="showDialog('node/versioning/merge');"></div>
			<div dojoType="MenuItem2" caption="Done Merge" onClick="showDialog('node/versioning/donemerge');"></div>
			<div dojoType="MenuItem2" caption="Cancel Merge" onClick="showDialog('node/versioning/cancelmerge');"></div>
			<div dojoType="MenuSeparator2"></div>
			<div dojoType="MenuItem2" caption="Add version label" onClick="showDialog('node/versioning/addversionlabel');"></div>
			<div dojoType="MenuItem2" caption="Remove version label" onClick="showDialog('node/versioning/removeversionlabel');"></div>
			<div dojoType="MenuSeparator2"></div>
			<div dojoType="MenuItem2" caption="Version history" onClick="showDialog('node/versioning/versionhistory');"></div>
		</div>

		<div dojoType="PopupMenu2" widgetId="nodeLockingMenu">
			<div dojoType="MenuItem2" caption="Lock" onClick="showDialog('node/locking/lock');"></div>
			<div dojoType="MenuItem2" caption="Unlock" onClick="showDialog('node/locking/unlock');"></div>
			<div dojoType="MenuItem2" caption="Refresh lock" onClick="alert('showDialog('node/locking/refreshlock');')"></div>
		</div>

		<div dojoType="PopupMenu2" widgetId="sessionLockingMenu">
			<div dojoType="MenuItem2" caption="Add token" onClick="showDialog('session/locking/addtoken');"></div>
			<div dojoType="MenuItem2" caption="Remove token" onClick="showDialog('session/locking/removetoken');"></div>
			<div dojoType="MenuItem2" caption="Show tokens" onClick="showDialog('session/locking/showtokens');"></div>
		</div>

		<div dojoType="PopupMenu2" widgetId="repositoryMenu">
			<div dojoType="MenuItem2" caption="Descriptors" onClick="showDialog('repository/descriptors');"></div>
		</div>

		<div dojoType="PopupMenu2" widgetId="workspaceMenu">
			<div dojoType="MenuItem2" caption="Accessible workspaces" onClick="showDialog('workspace/accessibleworkspaces');"></div>
			<div dojoType="MenuSeparator2"></div>
			<div dojoType="MenuItem2" caption="Copy Item" onClick="showDialog('workspace/copyitem');"></div>
			<div dojoType="MenuItem2" caption="Move Item" onClick="showDialog('workspace/moveitem');"></div>
			<div dojoType="MenuItem2" caption="Rename Item" onClick="showDialog('workspace/renameitem');"></div>
			<div dojoType="MenuSeparator2"></div>
			<div dojoType="MenuItem2" caption="Import XML" onClick="showDialog('workspace/importxml');"></div>
			<div dojoType="MenuSeparator2"></div>
			<div dojoType="MenuItem2" caption="Namespaces" onClick="showDialog('workspace/namespaces');"></div>
			<div dojoType="MenuItem2" caption="Register Namespace" onClick="showDialog('workspace/registernamespace');"></div>
			<div dojoType="MenuItem2" caption="Unregister Namespace" onClick="showDialog('workspace/unregisternamespace');"></div>
		</div>

		<div dojoType="PopupMenu2" widgetId="sessionMenu">
			<div dojoType="MenuItem2" caption="Move Item" onClick="showDialog('session/moveitem');"></div>
			<div dojoType="MenuItem2" caption="Refresh" onClick="showDialog('session/refresh');"></div>
			<div dojoType="MenuItem2" caption="Rename Item" onClick="showDialog('session/renameitem');"></div>
			<div dojoType="MenuItem2" caption="Save" onClick="showDialog('session/save');"></div>
			<div dojoType="MenuSeparator2"></div>
			<div dojoType="MenuItem2" caption="Export Document View" onClick="showDialog('session/exportdocumentview');"></div>
			<div dojoType="MenuItem2" caption="Export System View" onClick="showDialog('session/exportsystemview');"></div>
			<div dojoType="MenuItem2" caption="Import XML" onClick="showDialog('session/importxml');"></div>
			<div dojoType="MenuSeparator2"></div>
			<div dojoType="MenuItem2" caption="Locking" submenuId="sessionLockingMenu"></div>
			<div dojoType="MenuSeparator2"></div>
			<div dojoType="MenuItem2" caption="Namespaces" onClick="showDialog('session/namespaces');"></div>
			<div dojoType="MenuItem2" caption="Set namespace prefix" onClick="showDialog('session/setnamespaceprefix');"></div>
			<div dojoType="MenuSeparator2"></div>
			<div dojoType="MenuItem2" caption="Attributes" onClick="showDialog('session/attributes');"></div>
			<div dojoType="MenuItem2" caption="Logout" onClick="location.replace('<c:url value="/logout.jsp"/>');"></div>
		</div>
		
		<div dojoType="PopupMenu2" widgetId="nodeMenu">
			<div dojoType="MenuItem2" caption="Add node" onClick="showDialog('node/addnode');"></div>
			<div dojoType="MenuItem2" caption="Refresh" onClick="showDialog('node/refresh');"></div>
			<div dojoType="MenuItem2" caption="Remove" onClick="showDialog('node/remove');" iconSrc="<c:url value="/images/x.gif"/>"></div>
			<div dojoType="MenuItem2" caption="Rename" onClick="showDialog('session/renameitem');"></div>
			<div dojoType="MenuItem2" caption="Save" onClick="showDialog('node/save');"></div>
			<div dojoType="MenuItem2" caption="Set property" submenuId="setPropertyMenu"></div>
			<div dojoType="MenuItem2" caption="Set mandatory properties" onClick="showDialog('node/setmandatoryproperties');"></div>
			<div dojoType="MenuSeparator2"></div>
			<div dojoType="MenuItem2" caption="Order before" onClick="showDialog('node/orderbefore');"></div>
			<div dojoType="MenuSeparator2"></div>
			<div dojoType="MenuItem2" caption="Add mixin" onClick="showDialog('node/addmixin');"></div>
			<div dojoType="MenuItem2" caption="Remove mixin" onClick="showDialog('node/removemixin');"></div>
			<div dojoType="MenuSeparator2"></div>
			<div dojoType="MenuItem2" caption="Versioning" submenuId="versioningMenu"></div>
			<div dojoType="MenuItem2" caption="Locking" submenuId="nodeLockingMenu"></div>
			<div dojoType="MenuSeparator2"></div>
			<div dojoType="MenuItem2" caption="Update" onClick="showDialog('node/update');"></div>
		</div>
		
		<div dojoType="PopupMenu2" widgetId="propertyMenu">
			<div dojoType="MenuItem2" caption="Remove" onClick="showDialog('property/remove');" iconSrc="<c:url value="/images/x.gif"/>" ></div>
			<div dojoType="MenuItem2" caption="Rename" onClick="showDialog('property/rename');"></div>
			<div dojoType="MenuItem2" caption="Save" onClick="showDialog('property/save');"></div>
			<div dojoType="MenuItem2" caption="Refresh" onClick="showDialog('property/refresh');"></div>
			<div dojoType="MenuItem2" caption="Set Value" onClick="showDialog('property/setvalue');"></div>
		</div>		

		<div dojoType="PopupMenu2" widgetId="searchMenu">
			<div dojoType="MenuItem2" caption="XPath" onClick="showDialog('search/xpath');"></div>
			<div dojoType="MenuItem2" caption="SQL" onClick="showDialog('search/sql');"></div>
		</div>		
	
		<div dojoType="MenuBar2">
			<div dojoType="MenuBarItem2" caption="Repository" submenuId="repositoryMenu"></div>
			<div dojoType="MenuBarItem2" caption="Workspace" submenuId="workspaceMenu"></div>
			<div dojoType="MenuBarItem2" caption="Session" submenuId="sessionMenu"></div>
			<div dojoType="MenuBarItem2" caption="Node" submenuId="nodeMenu"></div>
			<div dojoType="MenuBarItem2" caption="Property" submenuId="propertyMenu"></div>
			<div dojoType="MenuBarItem2" caption="Search" submenuId="searchMenu"></div>
		</div>
		
	</div>
	
	<div dojoType="SplitContainer"
		orientation="horizontal"
		sizerWidth="5"
		activeSizing="0"
		layoutAlign="client">
	
		<div dojoType="ContentPane" class="treeDiv" style="padding-right: 0px; z-index: 1;">

			<div dojoType="TreeV3" widgetId="firstTree" listeners="treeController;selector;treeContextMenu">
			    <div dojoType="TreeNodeV3" title="Root" isFolder="true" widgetId="/">
			    </div>
			</div>	
			
		</div>		

		<div dojoType="SplitContainer"
			id="rightPane"
			orientation="vertical"
			sizerWidth="5"
			activeSizing="0"
			sizeMin="50" 
			sizeShare="80">
			
			<div dojoType="ContentPane" sizeMin="20" sizeShare="30" class="itemList">
				<table 
					class="itemList" 
					dojoType="filteringTable"
					tbodyClass="scrollContent"
					alternateRows="true" 
					maxSortable="2"
					border="0"
					cellpadding="0"
					cellspacing="0"
					widgetId="itemList"
					id="itemList">
				<thead>
					<tr>
						<th field="Index" dataType="Number" sort="asc" width="10px">Index</th>
						<th field="Node" dataType="String" width="10px">Node</th>
						<th field="Name" dataType="String">Name</th>
						<th field="Value" dataType="String">Value</th>
						<th field="Type" dataType="String" width="50px">Type</th>
						<th field="New" dataType="String" width="10px">New</th>
						<th field="Modified" dataType="String" width="10px">Modified</th>
					</tr>
				</thead>
				<tbody>
				</tbody>
				</table>
			</div>
			
			<div dojoType="ContentPane" sizeMin="20" sizeShare="30" class="infoPane">
				<div id="mainTabContainer" dojoType="TabContainer" style="overflow: none; width: 100%; height: 100%;" class="infoPane">
					<div id="itemTab" dojoType="ContentPane" label="Item" class="infoPane" cacheContent="false" preload="false"></div>
					<div id="typeTab" dojoType="ContentPane" label="Type" class="typePane" cacheContent="false" preload="false"></div>
					<div id="definitionTab" dojoType="ContentPane" label="Definition" class="infoPane" cacheContent="false" preload="false"></div>
				</div>
			</div>
		</div>
		
	</div>
	
	<div dojoType="LayoutContainer" layoutAlign="bottom" class="statusBar" style="height: 28px" widgetId="statusBar">

		<div dojoType="ContentPane" layoutAlign="left" class="logoPanel">
			<img src="<c:url value="/images/jackrabbit-small.gif"/>" width="35" height="27" border="0"/>
		</div>
		
		<div dojoType="ContentPane" layoutAlign="left" class="statusPanel">
			<%= request.getRemoteUser() %>@default
		</div>
		
		<div dojoType="ContentPane" layoutAlign="client" class="statusPanel" style="padding-right: 0px; z-index: 1;">
			<div id="path">/</div> 
		</div>
		
	</div>
	
</div>
<div dojoType="toaster" id="successToast" positionDirection="bl-up" showDelay="4000" messageTopic="successMessageTopic"></div>
<div dojoType="toaster" id="errorToast" positionDirection="bl-up" showDelay="4000" messageTopic="errorMessageTopic" width="250"></div>
</body>
</html>