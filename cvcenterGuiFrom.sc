
/* automatic GUI-creation from SynthDefs, Ndefs ... */
/* read input */

+Synth {
	
	cvcGui { |pairs2D, environment|
		var sDef, def, cDict = (), metadata;
		sDef = SynthDescLib.global[this.defName.asSymbol];
		metadata = sDef.metadata.specs;
		sDef.controlDict.pairsDo({ |n, c| cDict.put(n, c.defaultValue) });
		CVWidgetSpecsEditor(this, this.defName.asSymbol, cDict, pairs2D, metadata, environment);
	}
		
}

+NodeProxy {
	
	cvcGui { |pairs2D|
		var cDict = (), name;
		this.getKeysValues.do({ |pair| cDict.put(pair[0], pair[1]) });
		if(this.class === Ndef, {
			name = this.key;
		}, {
			name = nil;
		});
		CVWidgetSpecsEditor(this, name, cDict, pairs2D);
	}

}