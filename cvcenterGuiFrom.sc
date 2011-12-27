
/* automatic GUI-creation from SynthDefs, Ndefs ... */

+Synth {
	
	cvcGui { |pairs2D, environment|
		var sDef, def, cDict = (), thisPairs2D;
		sDef = SynthDescLib.global[this.defName.asSymbol];
		sDef.controlDict.pairsDo({ |n, c| cDict.put(n, c.defaultValue) });
		CVWidgetSpecsEditor(this.class, this.defName.asSymbol, cDict, pairs2D, environment);
	}
		
}