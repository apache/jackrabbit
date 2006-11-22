/*
 * Current item
 */
var currentItem = "/";

/*
* refresh the item list and the description list
*/
function onTreeNodeSelected(path) {
	// alert("on tree node selected. path = " + path);
	refreshDescription(path);
	refreshList(path);
}

/*
* Refresh the description panel
*/
function onListItemSelected(path) {
	dojo.debug("on list item selected. path = " + path);
	refreshDesctription(path);
}

/*
* Refresh all panels
*/
function refreshAll() {
	dojo.debug("refresh all");

}

/*
* Refresh the list panel
*/
function refreshList(path) {
	dojo.debug("refresh list. path = " + path);
	// alert("refreshList");
	// load items through ajax
	populateItemList();
}

/*
* Refresh the description panel.
* Sets the current working item to the described item.
*/
function refreshDescription(path) {
	// Sets the current working item
	currentItem = path ;
	refreshPath();
	// load description through ajax
	populateInfo();
}

/*
* Refresh the path div
*/
function refreshPath() {
	var pathDiv = document.getElementById('path');
	pathDiv.innerHTML= currentItem ;
}
	
/*
 * Populate the item list thorugh ajax
 */
function populateItemList(){
	var w=dojo.widget.byId("itemList");
	w.store.setData([]);

	var kw = {
		url: "itemlist.jsp?path=" + currentItem,
		mimetype: "text/javascript",
		load: function(type, data, http) {
			var w=dojo.widget.byId("itemList");
			w.store.setData(data);
			// Connect to on click event
			var row = dojo.byId("itemList").getElementsByTagName('tr');
			for (var i=0; i<row.length; i++) {
			    dojo.event.connect(row[i], "onclick", "onClickItemList");
			}
		}
	};
	dojo.io.bind(kw);
}

function onClickItemList() {
	refreshDescription(dojo.widget.byId("itemList").getSelectedData().Id) ;
}


/*
 * Populate the description panel thorugh ajax
 */
function populateInfo(){

	var itemTab = dojo.widget.byId("itemTab");
	itemTab.setUrl("info/item.jsp?path=" + currentItem); 
	
	var typeTab = dojo.widget.byId("typeTab");
	typeTab.setUrl("info/type.jsp?path=" + currentItem); 

	var definitionTab = dojo.widget.byId("definitionTab");
	definitionTab.setUrl("info/definition.jsp?path=" + currentItem); 

}

/*
* Shows the given dialog
*   @param object id
*/
function showDialog(path) {
	var dialog = dojo.widget.byId("dialog");
	dialog.cacheContent= false ;
	dialog.setUrl('dialog/' + path + '.jsp?path=' + currentItem); 
	dialog.show();
} 

/*
* Hides the dialog
*/
function hideDialog() {
	dojo.widget.byId("dialog").hide() ;
}

/*
* Submits the dialog
*/
function submitDialog() {
	try{
		dojoForm(document.getElementById('dialogForm'), new Array());
	} catch(E) {
		hideDialog();
	};
	hideDialog();
}

/*
* Submits the dialog and reloads the array of tree nodes.
*/
function submitDialog(nodes) {
	try{
		dojoForm(document.getElementById('dialogForm'), nodes);
	} catch(E) {
		hideDialog();
	};
	hideDialog();
}

/*
* Helper function to submit a form through ajax
*/
function dojoForm(form, nodes) {
	var kw = {
		formNode: form,
		mimetype: "text/plain",
		load: function(t, txt, e) {
			var treeController = dojo.widget.manager.getWidgetById('treeController');
			var treeSelector = dojo.widget.manager.getWidgetById('selector');
			for (key in nodes) {
				var node = nodes[key];
				if (node==null) {
					node = dojo.widget.manager.getWidgetById('/');
				}
				treeSelector.deselectAll() ;
				treeController.collapse(node) ;
				treeController.expand(node) ;
				treeSelector.select(node) ;
			}
			dojo.event.topic.publish("successMessageTopic", txt);
		},		
		error: function(t, e) {
			dojo.event.topic.publish("errorMessageTopic", {message: e.message, type: "ERROR"});
		}
	};
	dojo.io.bind(kw);
	return false;
}

