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

CVWidgetKnob : CVWidget {
	
	var <window, <guiEnv, <editorEnv;
	var <knob, <numVal, <specBut, <midiHead, <midiLearn, <midiSrc, <midiChan, <midiCtrl, <oscEditBut, <calibBut, <actionsBut;

	*new { |parent, cv, name, bounds, defaultAction, setup, controllersAndModels, cvcGui, server|
		^super.new.init(
			parent, 
			cv, 
			name, 
			bounds, 
			defaultAction,
			setup,
			controllersAndModels, 
			cvcGui, 
			server // swing compatibility. well, ...
		)
	}
	
	init { |parentView, cv, name, bounds, action, setupArgs, controllersAndModels, cvcGui, server|
		var thisName, thisXY, thisWidth, thisHeight, knobsize, widgetSpecsActions;
		var msrc = "source", mchan = "chan", mctrl = "ctrl", margs;
		var nextY, knobX, knobY;
		
		this.bgColor ?? { this.bgColor = Color.white };
		
		prCalibrate = true;
		prMidiMode = 0;
		prMidiMean = 64;
		prMidiResolution = 1;
		prSoftWithin = 0.1;
						
		guiEnv = ();
		editorEnv = ();
		cvcGui !? { isCVCWidget = true };

		if(cvcGui.class == Event and:{ cvcGui.midiOscEnv.notNil }, { midiOscEnv = cvcGui.midiOscEnv }, { midiOscEnv = () });
		midiOscEnv.oscMapping ?? { midiOscEnv.oscMapping = \linlin };
				
		if(name.isNil, { thisName = "knob" }, { thisName = name });
		wdgtInfo = thisName.asString;
		
		if(cv.isNil, {
			widgetCV = CV.new;
		}, {
			widgetCV = cv;
		});
				
		this.initControllersAndModels(controllersAndModels);

		setupArgs !? {
			setupArgs.isKindOf(Event).not.if { Error("a setup has to be provided as a Dictionary or an Event").throw };
			setupArgs[\midiMode] !? { this.setMidiMode(setupArgs[\midiMode]) };
			setupArgs[\midiResolution] !? { this.setMidiResolution(setupArgs[\midiResolution]) };
			setupArgs[\midiMean] !? { this.setMidiMean(setupArgs[\midiMean]) };
			setupArgs[\ctrlButtonBank] !? { this.setCtrlButtonBank(setupArgs[\ctrlButtonBank]) };
			setupArgs[\softWithin] !? { this.setSoftWithin(setupArgs[\softWithin]) };
			setupArgs[\calibrate] !? { this.setCalibrate(setupArgs[\calibrate]) };
		};
								
		action !? { this.addAction(\default, action) };
		
		if(bounds.isNil, {
			thisXY = 7@0;
			thisWidth = 52;
			thisHeight = 181;
		}, {
			if(parentView.isNil, { thisXY = 7@0 }, { thisXY = bounds.left@bounds.top });
			thisWidth = bounds.width;
			thisHeight = bounds.height;
		});
		
		if(parentView.isNil, {
			window = Window(thisName, Rect(50, 50, thisWidth+14, thisHeight+7), server: server);
		}, {
			window = parentView;
		});
				
		cvcGui ?? { 
			window.onClose_({
				[window, thisName, editor].postln;
				if(editor.notNil, {
					if(editor.isClosed.not, {
						editor.close;
					}, {
						if(CVWidgetEditor.allEditors.notNil and:{
							CVWidgetEditor.allEditors[thisName.asSymbol].notNil;
						}, {
							CVWidgetEditor.allEditors.removeAt(thisName.asSymbol)
						})
					})
				});
				midiOscEnv.oscResponder !? { midiOscEnv.oscResponder.remove };
				midiOscEnv.cc !? { midiOscEnv.cc.remove };
				wdgtControllersAndModels.do({ |mc| mc.isKindOf(SimpleController).if{ mc.controller.remove } });
			});
		};
//		cvcGui ?? {
//			window.onClose.addFunc({
//				midiOscEnv.oscResponder !? { midiOscEnv.oscResponder.remove };
//				midiOscEnv.cc !? { midiOscEnv.cc.remove };
//				wdgtControllersAndModels.do({ |mc| mc.isKindOf(SimpleController).if{ mc.controller.remove } });
//			})
//		};
						
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
		knobsize = thisHeight-2-145;
		if(knobsize >= thisWidth, {
			knobsize = thisWidth;
			knobY = thisXY.y+16+(thisHeight-143-knobsize/2);
			knobX = thisXY.x;
		}, {
			knobsize = thisHeight-143;
			knobX = thisWidth-knobsize/2+thisXY.x;
			knobY = thisXY.y+16;
		});						
		knob = Knob(window, Rect(knobX, knobY, knobsize, knobsize))
			.canFocus_(false)
		;
		block { |break|
			#[pan, boostcut, bipolar, detune].do({ |symbol| 
				if(widgetCV.spec == symbol.asSpec, { break.value(knob.centered_(true)) });
			})
		};
		nextY = thisXY.y+thisHeight-132;
		numVal = NumberBox(window, Rect(thisXY.x+1, nextY, thisWidth-2, 15))
			.value_(widgetCV.value).font_(Font("Helvetica", 9.5))
		;
		nextY = nextY+numVal.bounds.height;
		specBut = Button(window, Rect(thisXY.x+1, nextY, thisWidth-2, 15))
			.font_(Font("Helvetica", 9))
//			.focusColor_(Color(alpha: 0))
			.states_([["edit Spec", Color.white, Color(1.0, 0.3)]])
			.action_({ |btn|
				if(editor.isNil or:{ editor.isClosed }, {
					editor = CVWidgetEditor(this, thisName, 0);
					guiEnv.editor = editor;
				}, {
					editor.front(0)
				});
				wdgtControllersAndModels.oscConnection.model.value_(
					wdgtControllersAndModels.oscConnection.model.value;
				).changed(\value);
				wdgtControllersAndModels.midiConnection.model.value_(
					wdgtControllersAndModels.midiConnection.model.value
				).changed(\value);
			})
		;
		nextY = nextY+specBut.bounds.height+1;
		midiHead = Button(window, Rect(thisXY.x+1, nextY, thisWidth-17, 15))
			.font_(Font("Helvetica", 9))
//			.focusColor_(Color(alpha: 0))
			.states_([["MIDI", Color.black, this.bgColor]])
			.action_({ |ms|
				if(editor.isNil or:{ editor.isClosed }, {
					editor = CVWidgetEditor(this, thisName, 1);
					guiEnv.editor = editor;
				}, {
					editor.front(1)
				});
				wdgtControllersAndModels.oscDisplay.model.value_(
					wdgtControllersAndModels.oscDisplay.model.value;
				).changed(\value);
				wdgtControllersAndModels.midiDisplay.model.value_(
					wdgtControllersAndModels.midiDisplay.model.value
				).changed(\value);
			})
		;
		
		if(GUI.current.name === \QtGUI, {
			midiHead.mouseEnterAction_({ |mb|
				mb.states_([["MIDI", Color.white, Color.red]])
			}).mouseLeaveAction_({ |mb|
				mb.states_([["MIDI", Color.black, this.bgColor]])
			})
		});
		
		midiLearn = Button(window, Rect(thisXY.x+thisWidth-16, nextY, 15, 15))
			.font_(Font("Helvetica", 9))
//			.focusColor_(Color(alpha: 0))
			.states_([
				["L", Color.white, Color.blue],
				["X", Color.white, Color.red]
			])
			.action_({ |ml|
				ml.value.switch(
					1, {
						margs = [
							[midiSrc.string, msrc], 
							[midiChan.string, mchan], 
							[midiCtrl.string, mctrl]
						].collect({ |pair| if(pair[0] != pair[1], { pair[0].asInt }, { nil }) });
						if(margs.select({ |i| i.notNil }).size > 0, {
							this.midiConnect(*margs);
						}, {
							this.midiConnect;
						})
					},
					0, { this.midiDisconnect }
				)
			})
		;
		nextY = nextY+midiLearn.bounds.height;
		midiSrc = TextField(window, Rect(thisXY.x+1, nextY, thisWidth-2, 12))
			.font_(Font("Helvetica", 9))
//			.focusColor_(Color(alpha: 0))
			.string_(msrc)
			.background_(Color.white)
			.stringColor_(Color.black)
			.action_({ |tf|
				if(tf.string != msrc, {
					wdgtControllersAndModels.midiDisplay.model.value_((
						learn: "C",
						src: tf.string,
						chan: wdgtControllersAndModels.midiDisplay.model.value.chan,
						ctrl: wdgtControllersAndModels.midiDisplay.model.value.ctrl
					)).changed(\value)
				})
			})
			.mouseDownAction_({ |tf|
				tf.stringColor_(Color.red)
			})
			.keyDownAction_({ |tf, char, modifiers, unicode, keycode|
				if(unicode == 13, {
					tf.stringColor_(Color.black);
				})
			}) 
		;
		nextY = nextY+midiSrc.bounds.height;
		midiChan = TextField(window, Rect(thisXY.x+1, nextY, thisWidth-2/2, 12))
			.font_(Font("Helvetica", 9))
//			.focusColor_(Color(alpha: 0))
			.string_(mchan)
			.background_(Color.white)
			.stringColor_(Color.black)
			.action_({ |tf|
				if(tf.string != mchan, {
					wdgtControllersAndModels.midiDisplay.model.value_((
						learn: "C",
						src: wdgtControllersAndModels.midiDisplay.model.value.src,
						chan: tf.string,
						ctrl: wdgtControllersAndModels.midiDisplay.model.value.ctrl
					)).changed(\value)
				})
			})
			.mouseDownAction_({ |tf|
				tf.stringColor_(Color.red)
			})
			.keyDownAction_({ |tf, char, modifiers, unicode, keycode|
				if(unicode == 13, {
					tf.stringColor_(Color.black);
				})
			}) 
		;
		midiCtrl = TextField(window, Rect(thisXY.x+(thisWidth-2/2)+1, nextY, thisWidth-2/2, 12))
			.font_(Font("Helvetica", 9))
//			.focusColor_(Color(alpha: 0))
			.string_(mctrl)
			.background_(Color.white)
			.stringColor_(Color.black)
			.action_({ |tf|
				if(tf.string != mctrl, {
					wdgtControllersAndModels.midiDisplay.model.value_((
						learn: "C",
						src: wdgtControllersAndModels.midiDisplay.model.value.src,
						chan: wdgtControllersAndModels.midiDisplay.model.value.chan,
						ctrl: tf.string
					)).changed(\value)
				})
			})
			.mouseDownAction_({ |tf|
				tf.stringColor_(Color.red)
			})
			.keyDownAction_({ |tf, char, modifiers, unicode, keycode|
				if(unicode == 13, {
					tf.stringColor_(Color.black);
				})
			}) 
		;
		nextY = nextY+midiCtrl.bounds.height+1;
				
		oscEditBut = Button(window, Rect(thisXY.x+1, nextY, thisWidth-2, 30))
			.font_(Font("Helvetica", 9))
//			.focusColor_(Color(alpha: 0))
			.states_([
				["edit OSC", Color.black, this.bgColor]
			])
			.action_({ |oscb|
				if(editor.isNil or:{ editor.isClosed }, {
					editor = CVWidgetEditor(this, thisName, 2);
					guiEnv.editor = editor;
				}, {
					editor.front(2)
				});
				editor.calibNumBoxes !? {
					wdgtControllersAndModels.mapConstrainterLo.connect(editor.calibNumBoxes.lo);
					editor.calibNumBoxes.lo.value_(wdgtControllersAndModels.oscInputRange.model.value[0]);
					wdgtControllersAndModels.mapConstrainterHi.connect(editor.calibNumBoxes.hi);
					editor.calibNumBoxes.hi.value_(wdgtControllersAndModels.oscInputRange.model.value[1]);
				};
				wdgtControllersAndModels.oscDisplay.model.value_(
					wdgtControllersAndModels.oscDisplay.model.value;
				).changed(\value);
				wdgtControllersAndModels.midiDisplay.model.value_(
					wdgtControllersAndModels.midiDisplay.model.value
				).changed(\value);
			})
		;
		
		if(GUI.current.name === \QtGUI, {
			oscEditBut.mouseEnterAction_({ |oscb|
				if(wdgtControllersAndModels.oscConnection.model.value === false, {
					oscb.states_([["edit OSC", Color.white, Color.cyan(0.5)]]);
				})
			}).mouseLeaveAction_({ |oscb|
				if(wdgtControllersAndModels.oscConnection.model.value === false, {
					oscb.states_([["edit OSC", Color.black, this.bgColor]])
				})
			})
		});
		
		nextY = nextY+oscEditBut.bounds.height;
		calibBut = Button(window, Rect(thisXY.x+1, nextY, thisWidth-2, 15))
			.font_(Font("Helvetica", 9))
//			.focusColor_(Color(alpha: 0))
			.states_([
				["calibrating", Color.white, Color.red],
				["calibrate", Color.black, Color.green]
			])
			.action_({ |cb|
				switch(cb.value,
					0, { this.setCalibrate(true) },
					1, { this.setCalibrate(false) }
				)
			})
		;
		nextY = nextY+calibBut.bounds.height;
		actionsBut = Button(window, Rect(thisXY.x+1, nextY, thisWidth-2, 15))
			.font_(Font("Helvetica", 9))
//			.focusColor_(Color(alpha: 0))
			.states_([
				["actions ("++this.wdgtActions.select({ |v| v.asArray[0][1] == true }).size++"/"++this.wdgtActions.size++")", Color(0.08, 0.09, 0.14), Color(0.32, 0.67, 0.76)],
			])
			.action_({ |ab|
				if(editor.isNil or:{ editor.isClosed }, {
					editor = CVWidgetEditor(this, thisName, 3);
					guiEnv.editor = editor;
				}, {
					editor.front(3)
				});
			})
		;
		if(prCalibrate, { calibBut.value_(0) }, { calibBut.value_(1) });
		
				
		[knob, numVal].do({ |view| widgetCV.connect(view) });
		visibleGuiEls = [
			knob, 
			numVal, 
			specBut, 
			midiHead, 
			midiLearn, 
			midiSrc, 
			midiChan, 
			midiCtrl, 
			oscEditBut, 
			calibBut,
			actionsBut
		];
		allGuiEls = [
			widgetBg, 
			label, 
			nameField, 
			knob, 
			numVal, 
			specBut, 
			midiHead, 
			midiLearn, 
			midiSrc, 
			midiChan, 
			midiCtrl, 
			oscEditBut, 
			calibBut,
			actionsBut
		];		
		guiEnv = (
			editor: editor,
			calibBut: calibBut,
			actionsBut: actionsBut,
			knob: knob,
			oscEditBut: oscEditBut,
			midiSrc: midiSrc,
			midiChan: midiChan,
			midiCtrl: midiCtrl,
			midiLearn: midiLearn
		);

		this.initControllerActions;
	}
		
}
