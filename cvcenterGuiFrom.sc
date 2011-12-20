
/* automatic GUI-creation from SynthDefs, Ndefs ... */

+Synth {
	
	cvcGui { |environment|
		var sDef, cNames, cVals, cDict, def;
		sDef = SynthDescLib.global[this.defName.asSymbol];
		def = sDef.def.asCompileString;
		cNames = sDef.controlNames;
		cVals = sDef.def.controls;
		cDict = [cNames, cVals].flop.flat.as(Event);
		
		CVWidgetSpecsEditor(this.defName.asSymbol, cDict, environment);
	}
	
}