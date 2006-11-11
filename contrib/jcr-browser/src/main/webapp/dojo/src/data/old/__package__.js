dojo.require("dojo.experimental");

dojo.experimental("dojo.data.old.*");
dojo.kwCompoundRequire({
	common: [
		"dojo.data.old.Item",
		"dojo.data.old.ResultSet",
		"dojo.data.old.provider.FlatFile"
	]
});
dojo.provide("dojo.data.old.*");

