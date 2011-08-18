/* new branch: 'midi_refactoring' - midi-connections should be handled analoguesly to OSC-handling - within a MVC-logic. Moreover, there shouldn't be a new CCResponder created for each new connection, rather this should be handled within the CCResponder's function (to be investigated in depth. */

CVWidgetKnob : CVWidget {
	
	var <window, <guiEnv, <editorEnv;
	var <knob, <numVal, <specBut, <midiHead, <midiLearn, <midiSrc, <midiChan, <midiCtrl, <oscEditBut, <calibBut;

	*new { |parent, cv, name, bounds, action, setup, controllersAndModels, cvcGui, server|
		^super.new.init(
			parent, 
			cv, 
			name, 
			bounds, 
			action,
			setup,
			controllersAndModels, 
			cvcGui, 
			server // swing compatibility. well, ...
		)
	}
	
	init { |parentView, cv, name, bounds, action, setUpArgs, controllersAndModels, cvcGui, server|
		var thisName, thisXY, thisWidth, thisHeight, knobsize, widgetSpecsActions;
		var msrc = "source", mchan = "chan", mctrl = "ctrl", margs;
		var nextY, knobX, knobY;
		
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

		setUpArgs.isKindOf(Array).not.if { setUpArgs = [setUpArgs] };
		
		setUpArgs[0] !? { this.setMidiMode(setUpArgs[0]) };
		setUpArgs[1] !? { this.setMidiResolution(setUpArgs[1]) };
		setUpArgs[2] !? { this.setMidiMean(setUpArgs[2]) };
		setUpArgs[3] !? { this.setCtrlButtonBank(setUpArgs[3]) };
		setUpArgs[4] !? { this.setSoftWithin(setUpArgs[4]) };
		setUpArgs[5] !? { this.setCalibrate(setUpArgs[5]) };
						
		action !? { widgetCV.action_(action) };
		
		if(bounds.isNil, {
			thisXY = 7@0;
			thisWidth = 52;
			thisHeight = 166;
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
				"CVWidgetKnob: %\n".postf(editor);
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
			})
		};
						
		widgetBg = UserView(window, Rect(thisXY.x, thisXY.y, thisWidth, thisHeight))
			.focusColor_(Color(alpha: 1.0))
			.background_(Color.white)
		;
		label = Button(window, Rect(thisXY.x+1, thisXY.y+1, thisWidth-2, 15))
			.states_([
				[""+thisName.asString, Color.white, Color.blue],
				[""+thisName.asString, Color.black, Color.yellow],
			])
			.font_(Font("Helvetica", 9))
			.action_({ |b|
				this.toggleComment(b.value);
			})
		;
		nameField = TextView(window, Rect(label.bounds.left, label.bounds.top+label.bounds.height, thisWidth-2, thisHeight-label.bounds.height-2))
			.background_(Color.white)
			.font_(Font("Helvetica", 9))
			.string_(wdgtInfo)
			.visible_(false)
			.keyUpAction_({ wdgtInfo = nameField.string })
		;
		knobsize = thisHeight-2-130;
		if(knobsize >= thisWidth, {
			knobsize = thisWidth;
			knobY = thisXY.y+16+(thisHeight-128-knobsize/2);
			knobX = thisXY.x;
		}, {
			knobsize = thisHeight-128;
			knobX = thisWidth-knobsize/2+thisXY.x;
			knobY = thisXY.y+16;
		});						
		knob = Knob(window, Rect(knobX, knobY, knobsize, knobsize))
			.canFocus_(false)
		;
		block { |break|
			#[\pan, \boostcut, \bipolar, \detune].do({ |symbol| 
				if(widgetCV.spec == symbol.asSpec, { break.value(knob.centered_(true)) });
			})
		};
		nextY = thisXY.y+thisHeight-117;
		numVal = NumberBox(window, Rect(thisXY.x+1, nextY, thisWidth-2, 15))
			.value_(widgetCV.value)
		;
		nextY = nextY+numVal.bounds.height;
		specBut = Button(window, Rect(thisXY.x+1, nextY, thisWidth-2, 15))
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.states_([["edit Spec", Color.black, Color(241/255, 209/255, 0)]])
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
			.focusColor_(Color(alpha: 0))
			.states_([["MIDI", Color.black, Color(alpha: 0)]])
			.action_({ |ms|
				if(editor.isNil or:{ editor.isClosed }, {
					editor = CVWidgetEditor(this, thisName, 1);
					guiEnv.editor = editor;
				}, {
					editor.front(1)
				});
				wdgtControllersAndModels.oscConnection.model.value_(
					wdgtControllersAndModels.oscConnection.model.value;
				).changed(\value);
				wdgtControllersAndModels.midiConnection.model.value_(
					wdgtControllersAndModels.midiConnection.model.value
				).changed(\value);
			})
		;
		
		midiLearn = Button(window, Rect(thisXY.x+thisWidth-16, nextY, 15, 15))
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
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
			.focusColor_(Color(alpha: 0))
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
			.focusColor_(Color(alpha: 0))
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
			.focusColor_(Color(alpha: 0))
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
			.focusColor_(Color(alpha: 0))
			.states_([
				["edit OSC", Color.black, Color.clear]
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
					wdgtControllersAndModels.mapConstrainterHi.connect(editor.calibNumBoxes.hi);
				};
				wdgtControllersAndModels.oscConnection.model.value_(
					wdgtControllersAndModels.oscConnection.model.value;
				).changed(\value);
				wdgtControllersAndModels.midiConnection.model.value_(
					wdgtControllersAndModels.midiConnection.model.value
				).changed(\value);
			})
		;
		nextY = nextY+oscEditBut.bounds.height;
		calibBut = Button(window, Rect(thisXY.x+1, nextY, thisWidth-2, 15))
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
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
			calibBut
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
			calibBut
		];		
		guiEnv = (
			editor: editor,
			calibBut: calibBut,
			knob: knob,
			oscEditBut: oscEditBut,
			midiSrc: midiSrc,
			midiChan: midiChan,
			midiCtrl: midiCtrl,
			midiLearn: midiLearn
		);

		this.initControllerActions;
	}
		
	cvAction_ { |func|
		widgetCV.action_(func);
	}
		
}
