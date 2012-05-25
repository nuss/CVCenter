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

	var <mSlider, <numVal, <midiBut, <oscBut, <specBut, <actionsBut;
	// persistent widgets
	var isPersistent, oldBounds, oldName;

	*new { |parent, cv, name, bounds, defaultAction, setup, controllersAndModels, cvcGui, persistent, server|
		^super.new.init(
			parent, 
			cv, 
			name, 
			bounds, 
			defaultAction,
			setup,
			controllersAndModels, 
			cvcGui, 
			persistent, 
			server // swing compatibility. well, ...
		)
	}
	
	init { |parentView, cv, name, bounds, action, setupArgs, controllersAndModels, cvcGui, persistent, server|
		var thisSize, thisName, thisXY, thisX, thisY, thisWidth, thisHeight, knobsize, widgetSpecsActions;
		// hmmm...
		var msrc = "source", mchan = "chan", mctrl = "ctrl", margs;
		var nextX, nextY, knobX, knobY;
		
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
						
		guiEnv = Array.newClear(thisSize);
		cvcGui !? { isCVCWidget = true };

		if(cvcGui.class == Event and:{ cvcGui.midiOscEnv.notNil }, { 
			midiOscEnv = cvcGui.midiOscEnv
		}, {
			midiOscEnv = ()!thisSize
		});
		
		thisSize.do({ |i|
			midiOscEnv[i].oscMapping ?? { midiOscEnv[i].oscMapping = \linlin };
		});
						
		if(name.isNil, { thisName = "multislider" }, { thisName = name });
		wdgtInfo = thisName.asString;
		
//		hmm...
//		numVal = (); specBut = ();
//		midiHead = (); midiLearn = (); midiSrc = (); midiChan = (); midiCtrl = ();
//		oscEditBut = (); calibBut = (); actionsBut = ();

//		what's the editor gonna look like?
//		editor = ();

//		TO DO
		thisSize.do(this.initControllersAndModels(controllersAndModels, _));
		
		setupArgs !? {
			thisSize.do({ |slot|
				setupArgs[slot] !? { setupArgs[slot][\midiMode] !? { this.setMidiMode(setupArgs[slot][\midiMode], slot) }};
				setupArgs[slot] !? { setupArgs[slot][\midiResolution] !? { this.setMidiResolution(setupArgs[slot][\midiResolution], slot) }};
				setupArgs[slot] !? { setupArgs[slot][\midiMean] !? { this.setMidiMean(setupArgs[slot][\midiMean], slot) }};
				setupArgs[slot] !? { setupArgs[slot][\ctrlButtonBank] !? { this.setCtrlButtonBank(setupArgs[slot][\ctrlButtonBank], slot) }};
				setupArgs[slot] !? { setupArgs[slot][\softWithin] !? { this.setSoftWithin(setupArgs[slot][\softWithin], slot) }};
				setupArgs[slot] !? { setupArgs[slot][\calibrate] !? { this.setCalibrate(setupArgs[slot][\calibrate], slot) }};
			})
		};

		action !? { this.addAction(\default, action) };

		if(bounds.isNil, {		
			thisXY = 7@0;
			thisX = 50; thisY = 50;
			thisWidth = 122;
			thisHeight = 196;
		}, {
			if(parentView.isNil, { thisXY = 7@0 }, { thisXY = bounds.left@bounds.top });
			thisX = bounds.left; thisY = bounds.top;
			thisWidth = bounds.width;
			thisHeight = bounds.height;
		});
				
		if(parentView.isNil, {
			window = Window(thisName, Rect(thisX, thisY, thisWidth+14, thisHeight+7), server: server);
		}, {
			window = parentView;
		});
												
		cvcGui ?? { 
			window.onClose_({
				thisSize.do({ |slot|
					if(editor[slot].notNil, {
						if(editor[slot].isClosed.not, {
							editor[slot].close(slot);
						}, {
							if(CVWidgetEditor.allEditors.notNil and:{
								CVWidgetEditor.allEditors[thisName.asSymbol].notNil
							}, {
								CVWidgetEditor.allEditors[thisName.asSymbol].removeAt(slot);
								if(CVWidgetEditor.allEditors[thisName.asSymbol].isEmpty, {
									CVWidgetEditor.allEditors.removeAt(thisName.asSymbol);
								})
							})
						})
					})
				})
			})
		};

		cvcGui ?? {
			if(persistent == false or:{ persistent.isNil }, {
				window.onClose_(window.onClose.addFunc({
					thisSize.do({ |slot|
						midiOscEnv[slot].oscResponder !? { midiOscEnv[slot].oscResponder.remove };
						midiOscEnv[slot].cc !? { midiOscEnv[slot].cc.remove };
						wdgtControllersAndModels[slot].do({ |mc| mc.isKindOf(SimpleController).if{ mc.controller.remove } });
					})
				}))
			}, {
				isPersistent = true;
			})
		};

		persistent !? { if(persistent, { isPersistent = true }) };
		
		widgetBg = UserView(window, Rect(thisXY.x, thisXY.y, thisWidth, thisHeight))
//			.focusColor_(Color(alpha: 1.0))
			.background_(this.bgColor)
		;
		label = Button(window, Rect(thisXY.x+1, thisXY.y+1, thisWidth-2, 15))
			.states_([
				[""+thisName.asString, Color.white, Color.blue],
				[""+thisName.asString, Color.black, Color.yellow],
			])
			.font_(Font("Helvetica", 9))
			.action_({ |b|
				this.toggleComment(b.value.asBoolean);
			})
		;
		nameField = TextView(window, Rect(label.bounds.left, label.bounds.top+label.bounds.height, thisWidth-2, thisHeight-label.bounds.height-2))
			.background_(Color.white)
			.font_(Font("Helvetica", 9))
			.string_(wdgtInfo)
			.visible_(false)
			.keyUpAction_({ wdgtInfo = nameField.string })
		;
		mSlider = MultiSliderView(window, Rect(thisXY.x+1, thisXY.y+16, thisWidth-2, thisHeight-2-45-1))
			.drawRects_(true)
			.isFilled_(true)
			.colors_(Color.clear, Color.blue)
			.elasticMode_(1)
		;
		nextY = thisXY.y+mSlider.bounds.height+label.bounds.height+1;
		numVal = TextField(window, Rect(thisXY.x+1, nextY, thisWidth-2, 15))
			.string_(widgetCV.value.asString).font_(Font("Helvetica", 9.5))
		;
		nextY = thisXY.y+numVal.bounds.top+numVal.bounds.height+1;
		midiBut = Button(window, Rect(thisXY.x+1, nextY, thisWidth-2/4, 15))
			.states_([
				["MIDI", Color.black, this.bgColor]
			])
			.font_(Font("Helvetica", 9))
			.action_({ |mb| })
		;
		nextX = thisXY.x+1+midiBut.bounds.width;
		oscBut = Button(window, Rect(nextX, nextY, thisWidth-2/4, 15))
			.states_([
				["OSC", Color.black, this.bgColor]
			])
			.font_(Font("Helvetica", 9))
			.action_({ |oscb| })
		;
		nextX = nextX+oscBut.bounds.width;
		specBut = Button(window, Rect(nextX, nextY, thisWidth-2/4, 15))
			.states_([
				["Spec", Color.white, Color(1.0, 0.3)]
			])
			.font_(Font("Helvetica", 9))
			.action_({ |spb| })
		;
		nextX = nextX+specBut.bounds.width;
		actionsBut = Button(window, Rect(nextX, nextY, thisWidth-2/4, 15))
			.states_([
				["Actions", Color(0.08, 0.09, 0.14), Color(0.32, 0.67, 0.76)]
			])
			.font_(Font("Helvetica", 9))
			.action_({ |spb| })
		;
		
		visibleGuiEls = [
			mSlider, 
			numVal, 
			midiBut,
			specBut,
			oscBut, 
			actionsBut
		];

		allGuiEls = [
			widgetBg, 
			label, 
			nameField, 
			mSlider, 
			numVal, 
			midiBut, 
			oscBut,
			specBut,
			actionsBut
		];
		
		thisSize.do({ |slot|
			guiEnv[slot] = (
				editor: editor[slot],
				mSlider: mSlider,
				numVal: numVal,
				midiBut: midiBut[slot],
				oscBut: oscBut[slot]
				specBut: specBut[slot],
				actionsBut: actionsBut[slot]
			)
		});
		
		widgetCV.connect(mSlider);
		oldBounds = window.bounds;
		if(window.respondsTo(\name), { oldName = window.name });
	}
}