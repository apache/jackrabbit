dojo.provide("dojo.widget.TitlePane");
dojo.require("dojo.widget.*");
dojo.require("dojo.widget.ContentPane");
dojo.require("dojo.html.style");
dojo.require("dojo.lfx.*");

dojo.widget.defineWidget(
	"dojo.widget.TitlePane",
	dojo.widget.ContentPane,
{
	labelNode: "",
	labelNodeClass: "",
	containerNodeClass: "",
	label: "",
	
	open: true,
	templatePath: dojo.uri.dojoUri("src/widget/templates/TitlePane.html"),

	postCreate: function() {
		if (this.label) {
			this.labelNode.appendChild(document.createTextNode(this.label));
		}

		if (this.labelNodeClass) {
			dojo.html.addClass(this.labelNode, this.labelNodeClass);
		}	

		if (this.containerNodeClass) {
			dojo.html.addClass(this.containerNode, this.containerNodeClass);
		}	

		if (!this.open) {
			dojo.html.hide(this.containerNode);
		}
		dojo.widget.TitlePane.superclass.postCreate.apply(this, arguments);
	},

	onLabelClick: function() {
		if (this.open) {
			dojo.lfx.wipeOut(this.containerNode, 250).play();
			this.open=false;
		} else {
			dojo.lfx.wipeIn(this.containerNode, 250).play();
			this.open=true;
		}
	},

	setLabel: function(label) {
		this.labelNode.innerHTML=label;
	}
});
