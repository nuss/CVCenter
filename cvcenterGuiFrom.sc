
/* automatic GUI-creation from SynthDefs, Ndefs ... */
/* read input */

+Synth {
	
	cvcGui { |pairs2D, environment|
		var sDef, def, cDict = (), thisPairs2D, metadata;
		sDef = SynthDescLib.global[this.defName.asSymbol];
		metadata = sDef.metadata.specs;
		sDef.controlDict.pairsDo({ |n, c| cDict.put(n, c.defaultValue) });
		CVWidgetSpecsEditor(this, this.defName.asSymbol, cDict, pairs2D, metadata, environment);
	}
		
}