
/* automatic GUI-creation from SynthDefs, Ndefs ... */

+SynthDef {
	
	cvcGui {
		var cNames, cVals, def;
		cNames = this.def.controlNames;
		cVals = this.controls;
		def = this.asCompileString;
	}
	
}