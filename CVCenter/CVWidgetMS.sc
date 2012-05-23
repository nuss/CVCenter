/* (c) 2010-2012 Stefan Nussbaumer */
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
CVWidgetMS : CVWidget {

	var <knob, <numVal, <specBut, <midiHead, <midiLearn, <midiSrc, <midiChan, <midiCtrl, <oscEditBut, <calibBut, <actionsBut;
	// persistent widgets
	var isPersistent, oldBounds, oldName;

	*new { |parent, cv, name, bounds, defaultActions, setup, controllersAndModels, cvcGui, persistent, server|
		^super.new.init(
			parent, 
			cv, 
			name, 
			bounds, 
			defaultActions,
			setup,
			controllersAndModels, 
			cvcGui, 
			persistent, 
			server // swing compatibility. well, ...
		)
	}
	
	init { |parentView, cv, name, bounds, actions, setupArgs, controllersAndModels, cvcGui, persistent, server|
		var thisSize, thisName, thisXY, thisX, thisY, thisWidth, thisHeight, knobsize, widgetSpecsActions;
		// hmmm...
		var msrc = "source", mchan = "chan", mctrl = "ctrl", margs;
		var nextY, knobX, knobY;
		
		this.bgColor ?? { this.bgColor = Color.white };
		
		if(cv.isNil, {
			widgetCV = CV([0!3, 1!3]);
		}, {
			widgetCV = cv;
		});

		if(widgetCV.spec.minval.isArray.not and:{
			widgetCV.spec.maxval.isArray.not and:{
				widgetCV.spec.step.isArray.not and:{
					widgetCV.spec.default.isArray.not
				}
			}
		}, {
			Error("CVWidgetMS expects a multidimensional ControlSpec within its CV. Otherwise use CVWidgetKnob").throw;
		});
		
		thisSize = [
			widgetCV.spec.minval.size, 
			widgetCV.spec.maxval.size, 
			widgetCV.spec.step.size, 
			widgetCV.spec.default.size
		].maxItem;
		
		prCalibrate = true ! thisSize;
		prMidiMode = 0 ! thisSize;
		prMidiMean = 64 ! thisSize;
		prMidiResolution = 1 ! thisSize;
		prSoftWithin = 0.1 ! thisSize;
						
		guiEnv = ();
		cvcGui !? { isCVCWidget = true };

		if(cvcGui.class == Event and:{ cvcGui.midiOscEnv.notNil }, { midiOscEnv = cvcGui.midiOscEnv }, { midiOscEnv = () });
		midiOscEnv.oscMapping ?? { midiOscEnv.oscMapping = \linlin };
						
		if(name.isNil, { thisName = "knob" }, { thisName = name });
		wdgtInfo = thisName.asString;
		
	}
}