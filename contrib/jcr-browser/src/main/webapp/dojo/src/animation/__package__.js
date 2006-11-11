dojo.kwCompoundRequire({
	common: [
		"dojo.animation.AnimationEvent",
		"dojo.animation.Animation",
		"dojo.animation.AnimationSequence"
	]
});
dojo.provide("dojo.animation.*");

dojo.deprecated("dojo.Animation.* is slated for removal in 0.5; use dojo.lfx.* instead.", "0.5");
