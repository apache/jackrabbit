
dojo.provide("dojo.widget.TreeDeselectOnDblselect");

dojo.require("dojo.widget.HtmlWidget");
dojo.require("dojo.widget.TreeSelectorV3");

// selector extension to emphase node

dojo.widget.defineWidget(
	"dojo.widget.TreeDeselectOnDblselect",
	[dojo.widget.HtmlWidget],
{
	selector: "",
	
	initialize: function() {
		this.selector = dojo.widget.byId(this.selector);
		//dojo.debug("OK "+this.selector);
		dojo.event.topic.subscribe(this.selector.eventNames.dblselect, this, "onDblselect");		
	},

	onDblselect: function(message) {
		//dojo.debug("happen "+this.selector);
		//dojo.debug(message.node);
		this.selector.deselect(message.node);
	}
});
