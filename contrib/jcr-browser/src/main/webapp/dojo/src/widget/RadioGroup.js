dojo.provide("dojo.widget.RadioGroup");

dojo.require("dojo.lang.common");
dojo.require("dojo.event.browser");
dojo.require("dojo.html.selection");
dojo.require("dojo.widget.*");
dojo.require("dojo.widget.HtmlWidget");

// Widget that provides useful/common functionality that may be desirable
// when interacting with ul/ol html lists.
//
// This widget was mostly developed under supervision/guidance from Tom Trenka.
dojo.widget.defineWidget(
	"dojo.widget.RadioGroup", 
	dojo.widget.HtmlWidget,
	function(){
		this.selectedItem=null; //currently selected li, if any
		this.listNode=null; //the containing ul or ol
		this.items=[]; //the individual li's
		this.selected=[];
		
		//	CSS classes as attributes.
		this.groupCssClass="radioGroup";
		this.selectedCssClass="selected";
		this.itemContentCssClass="itemContent";
	},
	{
		isContainer:false,
		templatePath: null,
		templateCssPath: null,
		
		//	overridden from HtmlWidget
		postCreate:function(){
			this.parseStructure();
			dojo.html.addClass(this.listNode, this.groupCssClass);
			this.setupChildren();
			dojo.event.browser.addListener(this.listNode, "onclick", dojo.lang.hitch(this, "onSelect"));
			if (this.selectedItem){
				this.selectItem(this.selectedItem);
			}
		},
		
		/* 
		 * Sets local radioGroup and items properties, also validates
		 * that domNode contains an expected list.
		 *
		 * exception: If a ul or ol node can't be found in this widgets domNode
		 */
		parseStructure:function() {
			var listNode = dojo.dom.firstElement(this.domNode, "ul");
			if (!listNode) { listNode = dojo.dom.firstElement(this.domNode, "ol");}
			
			if (listNode) {
				this.listNode = listNode;
				this.items=[];	//	reset the items.
				var nl=listNode.getElementsByTagName("li");
				for (var i=0; i<nl.length; i++){
					if(nl[i].parentNode==listNode){
						this.items.push(nl[i]);
					}
				}
				return;
			}
			dojo.raise("RadioGroup: Expected ul or ol content.");
		},
		
		/*
		 * Allows the app to add a node on the fly, finishing up
		 * the setup so that we don't need to deal with it on a
		 * widget-wide basis.
		 */
		add:function(node){
			if(node.parentNode!=this.listNode){
				this.listNode.appendChild(node);
			}
			this.items.push(node);
			this.setup(node);
		},
		
		// removes the specified node from this group, if it exists
		remove:function(node){
			var idx=-1;
			for(var i=0; i<this.items.length; i++){
				if(this.items[i]==node){
					idx=i;
					break;
				}
			}
			if(idx<0) {return;}
			this.items.splice(idx,1);
			node.parentNode.removeChild(node);
		},
		
		// removes all items in this list
		clear:function(){
			for(var i=0; i<this.items.length; i++){
				this.listNode.removeChild(this.items[i]);
			}
			this.items=[];
		},
		
		// clears any selected items from being selected
		clearSelections:function(){
			for(var i=0; i<this.items.length; i++){
				dojo.html.removeClass(this.items[i], this.selectedCssClass);
			}
			this.selectedItem=null;
		},
		
		setup:function(node){
			var span = document.createElement("span");
			dojo.html.disableSelection(span);
			dojo.html.addClass(span, this.itemContentCssClass);
			dojo.dom.moveChildren(node, span);
			node.appendChild(span);
			
			if (this.selected.length > 0) {
				var uid = dojo.html.getAttribute(node, "id");
				if (uid && uid == this.selected){
					this.selectedItem = node;
				}
			}
			dojo.event.browser.addListener(node, "onclick", dojo.lang.hitch(this, "onItemSelect"));
			if (dojo.html.hasAttribute(node, "onitemselect")) {
				var tn = dojo.lang.nameAnonFunc(new Function(dojo.html.getAttribute(node, "onitemselect")), 
												this);
				dojo.event.browser.addListener(node, "onclick", dojo.lang.hitch(this, tn));
			}
		},
		
		/*
		 * Iterates over the items li nodes and manipulates their
		 * dom structures to handle things like span tag insertion
		 * and selecting a default item.
		 */
		setupChildren:function(){
			for (var i=0; i<this.items.length; i++){
				this.setup(this.items[i]);
			}
		},
		
		// Sets the selectedItem to passed in node, applies
		// css selection class on new item
		selectItem:function(node, event, nofire){
			if(this.selectedItem){
				dojo.html.removeClass(this.selectedItem, this.selectedCssClass);
			}
			
			this.selectedItem = node;
			dojo.html.addClass(this.selectedItem, this.selectedCssClass);
			
			// if this is the result of an event, stop here.
			if (!dj_undef("currentTarget", event)){
				return;
			}
			
			//	if there's no nofire flag, passed when this is nailed internally.
			if(!nofire){
				if(dojo.render.html.ie){
					this.selectedItem.fireEvent("onclick");
				}else{
					var e = document.createEvent("MouseEvents");
					e.initEvent("click", true, false);
					this.selectedItem.dispatchEvent(e);
				}
			}
		},
		
		// gets the currently selected item
		getValue:function() {
			return this.selectedItem;
		},
		
		// when the ul or ol contained by this widget is selected
		onSelect:function(e) { },
		
		// when an individual li is selected
		onItemSelect:function(e) {
			if (!dj_undef("currentTarget", e)){
				this.selectItem(e.currentTarget, e);
			}
		}
	}
);
