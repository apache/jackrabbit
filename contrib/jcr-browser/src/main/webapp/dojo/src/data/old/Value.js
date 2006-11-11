dojo.provide("dojo.data.old.Value");
dojo.require("dojo.lang.assert");

// -------------------------------------------------------------------
// Constructor
// -------------------------------------------------------------------
dojo.data.old.Value = function(/* anything */ value) {
	/**
	 * summary:
	 * A Value represents a simple literal value (like "foo" or 334),
	 * or a reference value (a pointer to an Item).
	 */
	this._value = value;
	this._type = null;
};

// -------------------------------------------------------------------
// Public instance methods
// -------------------------------------------------------------------
dojo.data.old.Value.prototype.toString = function() {
	return this._value.toString(); // string
};

dojo.data.old.Value.prototype.getValue = function() {
	/**
	 * summary: Returns the value itself.
	 */ 
	return this._value; // anything
};

dojo.data.old.Value.prototype.getType = function() {
	/**
	 * summary: Returns the data type of the value.
	 */ 
	dojo.unimplemented('dojo.data.old.Value.prototype.getType');
	return this._type; // dojo.data.old.Type
};

dojo.data.old.Value.prototype.compare = function() {
	dojo.unimplemented('dojo.data.old.Value.prototype.compare');
};

dojo.data.old.Value.prototype.isEqual = function() {
	dojo.unimplemented('dojo.data.old.Value.prototype.isEqual');
};
