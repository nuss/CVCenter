/* (c) Stefan Nussbaumer */
/* 
 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 3 of the License, or
 (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
*/

/* automatic GUI-creation from SynthDefs, Ndefs ... */
/* read input */

+Synth {
	
	cvcGui { |displayDialog=true, prefix, pairs2D, environment|
		var sDef, def, cDict = (), metadata;
		sDef = SynthDescLib.global[this.defName.asSymbol];
		sDef.metadata !? { sDef.metadata.specs !? { metadata = sDef.metadata.specs }};
		sDef.controlDict.pairsDo({ |n, c| cDict.put(n, c.defaultValue) });
		CVWidgetSpecsEditor(displayDialog, this, this.defName.asSymbol, cDict, prefix, pairs2D, metadata, environment);
	}
		
}

+NodeProxy {
	
	cvcGui { |displayDialog=true, prefix, pairs2D|
		var cDict = (), name;
		this.getKeysValues.do({ |pair| cDict.put(pair[0], pair[1]) });
		if(this.class === Ndef, {
			name = this.key;
		}, {
			name = nil;
		});
		CVWidgetSpecsEditor(displayDialog, this, name, cDict, prefix, pairs2D);
	}

}