
dojo.provide("dojo.widget.TreeEmphaseOnSelect");

dojo.require("dojo.widget.HtmlWidget");
dojo.require("dojo.widget.TreeSelectorV3");
dojo.require("dojo.html.selection");

// selector extension to emphase node

dojo.widget.defineWidget(
	"dojo.widget.TreeEmphaseOnSelect",
	dojo.widget.HtmlWidget,
{
	selector: "",
	
	initialize: function() {
		this.selector = dojo.widget.byId(this.selector);
		
		dojo.event.topic.subscribe(this.selector.eventNames.select, this, "onSelect");
		dojo.event.topic.subscribe(this.selector.eventNames.deselect, this, "onDeselect");	
	},

	
	onSelect: function(message) {
		message.node.viewEmphase()		
	},
	
	onDeselect: function(message) {
		message.node.viewUnemphase()
	}
	

});
