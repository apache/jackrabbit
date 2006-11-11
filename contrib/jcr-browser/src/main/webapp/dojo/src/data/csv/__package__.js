dojo.require("dojo.experimental");

dojo.experimental("dojo.data.csv.*");
dojo.kwCompoundRequire({
	common: [
		"dojo.data.csv.CsvStore",
		"dojo.data.csv.Result"
	]
});
dojo.provide("dojo.data.csv.*");

