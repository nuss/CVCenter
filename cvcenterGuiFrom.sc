
/* automatic GUI-creation from SynthDefs, Ndefs ... */

+Synth {
	
	cvcGui { |pairs2D, environment|
		var sDef, def, cDict = (), thisPairs2D;
		sDef = SynthDescLib.global[this.defName.asSymbol];
		sDef.controlDict.pairsDo({ |n, c| cDict.put(n, c.defaultValue) });
		pairs2D !? {
			pairs2D.do({ |pair|
				pair.do({ |c| 
					
				})
			})
		};
		CVWidgetSpecsEditor(this.class, this.defName.asSymbol, cDict, environment);
	}
		
}