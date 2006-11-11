dojo.provide("dojo.widget.DebugConsole");
dojo.require("dojo.widget.Widget");
dojo.require("dojo.widget.*");
dojo.require("dojo.widget.FloatingPane");

dojo.widget.defineWidget(
	"dojo.widget.DebugConsole",
	dojo.widget.FloatingPane,
{
	fillInTemplate: function() {
		dojo.widget.DebugConsole.superclass.fillInTemplate.apply(this, arguments);
		this.containerNode.id = "debugConsoleClientPane";
		djConfig.isDebug = true;
		djConfig.debugContainerId = this.containerNode.id;
	}
});
