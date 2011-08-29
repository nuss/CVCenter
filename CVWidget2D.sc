CVWidget2D : CVWidget {
	var <window, <guiEnv, <editorEnv;
	var <slider2d, <rangeSlider, <numVal, <specBut;
	var <midiHead, <midiLearn, <midiSrc, <midiChan, <midiCtrl, <oscEditBut, <calibBut;

	*new { |parent, cvs, name, bounds, actions, setup, controllersAndModels, cvcGui, server|
		^super.new.init(
			parent, 
			cvs, 
			name, 
			bounds,
			actions, 
			setup,
			controllersAndModels,
			cvcGui,
			server
		)
	}
	
	init { |parentView, cvs, name, bounds, actions, setupArgs, controllersAndModels, cvcGui, server|
		var thisName, thisXY, thisWidth, thisHeight, knobsize, widgetSpecsActions;
		var msrc = "source", mchan = "chan", mctrl = "ctrl", margs;
		var nextY, rightBarX, oscEditButHeight, right, left;
		var actionLo, actionHi;
		
		setupArgs !? {
			(setupArgs.class !== Event).if{ 
				Error( "a setup for a CVWidget2D has to be provided as an Event: (lo: [args], hi[args])!").throw;
			}
		};
		
		prCalibrate = (lo: true, hi: true);
		prMidiMode = (lo: 0, hi: 0);
		prMidiMean = (lo: 64, hi:64);
		prMidiResolution = (lo: 1, hi: 1);
		prSoftWithin = (lo: 0.1, hi: 0.1);
		prCtrlButtonBank = ();
				
		guiEnv = (lo: (), hi: ());
		editorEnv = ();

		cvcGui !? { isCVCWidget = true };
		
		if(cvcGui.class == Event and:{ cvcGui.midiOscEnv.notNil }, { midiOscEnv = cvcGui.midiOscEnv }, { midiOscEnv = () });
		[\lo, \hi].do({ |hilo|
			midiOscEnv[hilo] ?? { midiOscEnv.put(hilo, ()) };
			midiOscEnv[hilo].oscMapping ?? { midiOscEnv[hilo].oscMapping = \linlin };
		});
		
		if(name.isNil, { thisName = "2-dimensional" }, { thisName = name });
		wdgtInfo = thisName.asString;
		
		widgetCV = ();
		
		if(cvs.isNil, {
			widgetCV.lo = CV.new; widgetCV.hi = CV.new;
		}, {
			if(cvs.class !== Array, {
				Error("Please provide CVs in an array: [cv1, cv2]").throw;
			}, {
				if(cvs[0].isNil, { widgetCV.lo = CV.new }, { widgetCV.lo = cvs[0] });
				if(cvs[1].isNil, { widgetCV.hi = CV.new }, { widgetCV.hi = cvs[1] });
			})
		});
				
		numVal = (); specBut = ();
		midiHead = (); midiLearn = (); midiSrc = (); midiChan = (); midiCtrl = ();
		oscEditBut = (); calibBut = ();
		editor = ();
		
		[\lo, \hi].do({ |key| this.initControllersAndModels(controllersAndModels, key) });
						
		[\lo, \hi].do({ |key|
			setupArgs[key] !? { setupArgs[key][\midiMode] !? { this.setMidiMode(setupArgs[key][\midiMode], key) }};
			setupArgs[key] !? { setupArgs[key][\midiResolution] !? { this.setMidiResolution(setupArgs[key][\midiResolution], key) }};
			setupArgs[key] !? { setupArgs[key][\midiMean] !? { this.setMidiMean(setupArgs[key][\midiMean], key) }};
			setupArgs[key] !? { setupArgs[key][\ctrlButtonBank] !? { this.setCtrlButtonBank(setupArgs[key][\ctrlButtonBank], key) }};
			setupArgs[key] !? { setupArgs[key][\softWithin] !? { this.setSoftWithin(setupArgs[key][\softWithin], key) }};
			setupArgs[key] !? { setupArgs[key][\calibrate] !? { this.setCalibrate(setupArgs[key][\calibrate], key) }};
		});
					
		actions !? {
			if(actions.class !== Array, {
				Error("Please provide actions in an array: [action1, action2]").throw;
			}, {
				actions[0] !? { widgetCV.lo.action_(actions[0]) };
				actions[1] !? { widgetCV.hi.action_(actions[1]) };
			})
		};

		if(bounds.isNil, {		
			thisXY = 7@0;
			thisWidth = 122;
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
				[\lo, \hi].do({ |hilo|
					if(editor[hilo].notNil, {
						if(editor[hilo].isClosed.not, {
							editor[hilo].close(hilo);
						}, {
							if(CVWidgetEditor.allEditors.notNil and:{
								CVWidgetEditor.allEditors[thisName.asSymbol].notNil
							}, {
								CVWidgetEditor.allEditors[thisName.asSymbol].removeAt(hilo);
								if(CVWidgetEditor.allEditors[thisName.asSymbol].isEmpty, {
									CVWidgetEditor.allEditors.removeAt(thisName.asSymbol);
								})
							})
						})
					});
					midiOscEnv[hilo].oscResponder !? { midiOscEnv[hilo].oscResponder.remove };
					midiOscEnv[hilo].cc !? { midiOscEnv[hilo].cc.remove };
					wdgtControllersAndModels[hilo].do({ |mc| mc.isKindOf(SimpleController).if{ mc.controller.remove } });
				})
			})
		};
						
		widgetBg = UserView(window, Rect(thisXY.x, thisXY.y, thisWidth, thisHeight))
			.focusColor_(Color(alpha: 1.0))
			.background_(Color.white)
		;
		label = Button(window, Rect(thisXY.x+1, thisXY.y+1, thisWidth-2, 15))
			.states_([
				[thisName.asString, Color.white, Color.blue],
				[thisName.asString, Color.black, Color.yellow],
			])
			.font_(Font("Helvetica", 9))
			.action_({ |b|
				this.toggleComment(b.value);
			})
		;
		nameField = TextView(window, Rect(label.bounds.left, label.bounds.top+label.bounds.height, thisWidth-2, thisHeight-label.bounds.height-2))
			.background_(Color.white)
			.font_(Font("Helvetica", 9))
			.string_(thisName.asString)
			.visible_(false)
			.keyUpAction_({ wdgtInfo = nameField.string })
		;
		
		nextY = thisXY.y+1+label.bounds.height;
		
		slider2d = Slider2D(window, Rect(thisXY.x+1, nextY, thisWidth-42, thisWidth-47))
			.canFocus_(false)
			.background_(Color.white)
			.knobColor_(Color.red)
		;

		nextY = nextY+slider2d.bounds.height;

		rangeSlider = RangeSlider(window, Rect(
			thisXY.x+1,
			nextY,
			thisWidth-42,
			15
		))
		.canFocus_(false)
		.background_(Color.white);
		nextY = nextY+this.rangeSlider.bounds.height;

		numVal.lo = NumberBox(window);
		numVal.hi = NumberBox(window);
		
		[numVal.lo, [thisXY.x+1, widgetCV.lo], numVal.hi, [thisXY.x+(thisWidth-42/2), widgetCV.hi]].pairsDo({ |k, v|
			k.bounds_(Rect(
				v[0], 
				nextY,
				this.rangeSlider.bounds.width/2,
				15
			));
			k.value_(v[1].value);
		});
		
		specBut.lo = Button(window)
			.action_({ |btn|
				if(editor.lo.isNil or:{ editor.lo.isClosed }, {
					editor.lo = CVWidgetEditor(this, thisName, 0, \lo);
					guiEnv.lo.editor = editor.lo;
				}, {
					editor.lo.front(0)
				});
				wdgtControllersAndModels.lo.oscConnection.model.value_(
					wdgtControllersAndModels.lo.oscConnection.model.value;
				).changed(\value);
				wdgtControllersAndModels.lo.midiConnection.model.value_(
					wdgtControllersAndModels.lo.midiConnection.model.value
				).changed(\value);
			})
		;
		specBut.hi = Button(window)
			.action_({ |btn|
				if(editor.hi.isNil or:{ editor.hi.isClosed }, {
					editor.hi = CVWidgetEditor(this, thisName, 0, \hi);
					guiEnv.hi.editor = editor.hi;
				}, {
					editor.hi.front(0)
				});
				wdgtControllersAndModels.hi.oscConnection.model.value_(
					wdgtControllersAndModels.hi.oscConnection.model.value;
				).changed(\value);
				wdgtControllersAndModels.hi.midiConnection.model.value_(
					wdgtControllersAndModels.hi.midiConnection.model.value
				).changed(\value);
			})
		;
		midiHead.lo = Button(window)
			.action_({ |btn|
				if(editor.lo.isNil or:{ editor.lo.isClosed }, {
					editor.hi = CVWidgetEditor(this, thisName, 1, \lo);
					guiEnv.lo.editor = editor.lo;
				}, {
					editor.lo.front(1)
				});
				wdgtControllersAndModels.lo.oscConnection.model.value_(
					wdgtControllersAndModels.lo.oscConnection.model.value;
				).changed(\value);
				wdgtControllersAndModels.lo.midiConnection.model.value_(
					wdgtControllersAndModels.lo.midiConnection.model.value
				).changed(\value);
			})
		;
		midiHead.hi = Button(window)
			.action_({ |btn|
				if(editor.hi.isNil or:{ editor.hi.isClosed }, {
					editor.hi = CVWidgetEditor(this, thisName, 1, \hi);
					guiEnv.hi.editor = editor.hi;
				}, {
					editor.hi.front(1)
				});
				wdgtControllersAndModels.hi.oscConnection.model.value_(
					wdgtControllersAndModels.hi.oscConnection.model.value;
				).changed(\value);
				wdgtControllersAndModels.hi.midiConnection.model.value_(
					wdgtControllersAndModels.hi.midiConnection.model.value
				).changed(\value);
			})
		;
		midiLearn.lo = Button(window);
		midiLearn.hi = Button(window);
		midiSrc.lo = TextField(window);
		midiSrc.hi = TextField(window);
		midiChan.lo = TextField(window);
		midiChan.hi = TextField(window);
		midiCtrl.lo = TextField(window);
		midiCtrl.hi = TextField(window);
		
		nextY = thisXY.y+1+label.bounds.height;
		rightBarX = thisXY.x+slider2d.bounds.width+1;

		[specBut.hi, [nextY, \hi], specBut.lo, [nextY+52, \lo]].pairsDo({ |k, v|
			k.bounds_(Rect(rightBarX, v[0], 40, 13))
			.font_(Font("Helvetica", 8))
			.focusColor_(Color(alpha: 0))
			.states_([["edit Spec", Color.black, Color(241/255, 209/255, 0)]])
		});
		
		nextY = nextY+14;
				
		[midiHead.hi, nextY, midiHead.lo, nextY+52].pairsDo({ |k, v|
			k.bounds_(Rect(rightBarX, v, 28, 13))
			.font_(Font("Helvetica", 7))
			.focusColor_(Color(alpha: 0))
			.states_([["MIDI", Color.black, Color(alpha: 0)]])
		});
		
		
		[midiLearn.hi, [\hi, nextY], midiLearn.lo, [\lo, nextY+52]].pairsDo({ |k, v|
			k.bounds_(Rect(rightBarX+midiHead.lo.bounds.width, v[1], 12, 13))
			.font_(Font("Helvetica", 7))
			.focusColor_(Color(alpha: 0))
			.states_([
				["L", Color.white, Color.blue],
				["X", Color.white, Color.red]
			])
			.action_({ |ml|
				ml.value.switch(
					1, {
						margs = [
							[midiSrc[v[0]].string, msrc], 
							[midiChan[v[0]].string, mchan], 
							[midiCtrl[v[0]].string, mctrl]
						].collect({ |pair| if(pair[0] != pair[1], { pair[0].asInt }, { nil }) });
						if(margs.select({ |i| i.notNil }).size > 0, {
							this.midiConnect(uid: margs[0], chan: margs[1], num: margs[2], key: v[0]);
						}, {
							this.midiConnect(key: v[0]);
						})
					},
					0, { this.midiDisconnect(v[0]) }
				)
			})
		});
		
		nextY = nextY+13;
		
		[midiSrc.hi, [\hi, nextY], midiSrc.lo, [\lo, nextY+52]].pairsDo({ |k, v|
			k.bounds_(Rect(rightBarX, v[1], 40, 13))
			.font_(Font("Helvetica", 8.5))
			.focusColor_(Color(alpha: 0))
			.string_("source")
			.background_(Color(alpha: 0))
			.stringColor_(Color.black)
			.action_({ |tf|
				if(tf.string != msrc, {
					wdgtControllersAndModels[v[0]].midiDisplay.model.value_((
						learn: "C",
						src: tf.string,
						chan: wdgtControllersAndModels[v[0]].midiDisplay.model.value.chan,
						ctrl: wdgtControllersAndModels[v[0]].midiDisplay.model.value.ctrl
					)).changed(\value)
				})
			})
			.mouseDownAction_({ |tf|
				tf.stringColor_(Color.red)
			})
			.keyUpAction_({ |tf, char, modifiers, unicode, keycode|
				if(unicode == 13, {
					tf.stringColor_(Color.black);
				})
			}) 
		});

		nextY = nextY+13;

		[midiChan.hi, [\hi, nextY], midiChan.lo, [\lo, nextY+52]].pairsDo({ |k, v|
			k.bounds_(Rect(rightBarX, v[1], 15, 13))
			.font_(Font("Helvetica", 8.5))
			.focusColor_(Color(alpha: 0))
			.string_("chan")
			.background_(Color(alpha: 0))
			.stringColor_(Color.black)
			.action_({ |tf|
				if(tf.string != msrc, {
					wdgtControllersAndModels[v[0]].midiDisplay.model.value_((
						learn: "C",
						src: wdgtControllersAndModels[v[0]].midiDisplay.model.value.src,
						chan: tf.string,
						ctrl: wdgtControllersAndModels[v[0]].midiDisplay.model.value.ctrl
					)).changed(\value)
				})
			})
			.mouseDownAction_({ |tf|
				tf.stringColor_(Color.red)
			})
			.keyUpAction_({ |tf, char, modifiers, unicode, keycode|
				if(unicode == 13, {
					tf.stringColor_(Color.black);
				})
			}) 
		});

		[midiCtrl.hi, [\hi, nextY], midiCtrl.lo, [\lo, nextY+52]].pairsDo({ |k, v|
			k.bounds_(Rect(rightBarX+15, v[1], 25, 13))
			.font_(Font("Helvetica", 8.5))
			.focusColor_(Color(alpha: 0))
			.string_("ctrl")
			.background_(Color(alpha: 0))
			.stringColor_(Color.black)
			.action_({ |tf|
				if(tf.string != msrc, {
					wdgtControllersAndModels[v[0]].midiDisplay.model.value_((
						learn: "C",
						src: wdgtControllersAndModels[v[0]].midiDisplay.model.value.src,
						chan: wdgtControllersAndModels[v[0]].midiDisplay.model.value.chan,
						ctrl: tf.string
					)).changed(\value)
				})
			})
			.mouseDownAction_({ |tf|
				tf.stringColor_(Color.red)
			})
			.keyUpAction_({ |tf, char, modifiers, unicode, keycode|
				if(unicode == 13, {
					tf.stringColor_(Color.black);
				})
			}) 
		});
		
		if(
			(left = slider2d.bounds.height
			+rangeSlider.bounds.height
			+numVal.lo.bounds.height)
			>= 
			(right = specBut.lo.bounds.height
			+midiLearn.lo.bounds.height
			+midiSrc.lo.bounds.height
			+midiChan.lo.bounds.height
			*2), 
		{
			nextY = 
			label.bounds.top
			+label.bounds.height
			+left;
			oscEditButHeight = thisHeight-left-32;
		}, {
			nextY = 
			label.bounds.top
			+label.bounds.height
			+right;
			oscEditButHeight = thisHeight-right-32;
		});
		
		oscEditBut.lo = Button(window);
		oscEditBut.hi = Button(window);
		calibBut.lo = Button(window);
		calibBut.hi = Button(window);
		
		[oscEditBut.lo, [\lo, thisXY.x+1], oscEditBut.hi, [\hi, thisXY.x+(thisWidth/2)]].pairsDo({ |k, v|
			k.bounds_(Rect(v[1], nextY, thisWidth/2-1, oscEditButHeight))
			.font_(Font("Helvetica", 8.5))
			.focusColor_(Color(alpha: 0))
			.states_([
				["edit OSC", Color.black, Color.clear]
			])
			.action_({ |oscb|
				if(editor[v[0]].isNil or:{ editor[v[0]].isClosed }, {
					editor.put(v[0], CVWidgetEditor(this, thisName, 2, v[0]));
					guiEnv[v[0]].editor = editor[v[0]];
				}, {
					editor[v[0]].front(2)
				});
				editor[v[0]].calibNumBoxes !? {
					wdgtControllersAndModels[v[0]].mapConstrainterLo.connect(editor[v[0]].calibNumBoxes.lo);
					wdgtControllersAndModels[v[0]].mapConstrainterHi.connect(editor[v[0]].calibNumBoxes.hi);
				};
				wdgtControllersAndModels[v[0]].oscConnection.model.value_(
					wdgtControllersAndModels[v[0]].oscConnection.model.value;
				).changed(\value);
				wdgtControllersAndModels[v[0]].midiConnection.model.value_(
					wdgtControllersAndModels[v[0]].midiConnection.model.value
				).changed(\value);
			})
		});
		
		nextY = nextY+oscEditBut.lo.bounds.height;
		
		[calibBut.lo, [\lo, thisXY.x+1], calibBut.hi, [\hi, thisXY.x+(thisWidth/2)]].pairsDo({ |k, v|
			k.bounds_(Rect(v[1], nextY, thisWidth/2-1, 15))
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.states_([
				["calibrating", Color.white, Color.red],
				["calibrate", Color.black, Color.green]
			])
			.action_({ |cb|
				switch(cb.value,
					0, { this.setCalibrate(true, v[0]) },
					1, { this.setCalibrate(false, v[0]) }
				)
			})
		});
				
		[slider2d, rangeSlider].do({ |view| /*[widgetCV.lo.spec, widgetCV.hi.spec].postln; */[widgetCV.lo, widgetCV.hi].connect(view) });
		widgetCV.lo.connect(numVal.lo);
		widgetCV.hi.connect(numVal.hi);

		visibleGuiEls = [
			slider2d, 
			rangeSlider, 
			numVal.lo, numVal.hi, 
			specBut.lo, specBut.hi,
			midiHead.lo, midiHead.hi, 
			midiLearn.lo, midiHead.hi, 
			midiSrc.lo, midiSrc.hi, 
			midiChan.lo, midiHead.hi, 
			midiCtrl.lo, midiCtrl.hi, 
			oscEditBut.lo, oscEditBut.hi, 
			calibBut.lo, calibBut.hi,
		];

		allGuiEls = [
			widgetBg, 
			label, 
			nameField, 
			slider2d, 
			rangeSlider, 
			numVal.lo, numVal.hi, 
			specBut.lo, specBut.hi, 
			midiHead.lo, midiHead.hi, 
			midiLearn.lo, midiLearn.hi, 
			midiSrc.lo, midiSrc.hi, 
			midiChan.lo, midiChan.hi, 
			midiCtrl.lo, midiCtrl.hi, 
			oscEditBut.lo, oscEditBut.hi, 
			calibBut.lo, calibBut.hi,
		];
		
		[\lo, \hi].do({ |hilo|
			guiEnv[hilo] = (
				editor: editor[hilo],
				calibBut: calibBut[hilo],
				slider2d: slider2d,
				specBut: specBut[hilo],
				oscEditBut: oscEditBut[hilo],
				calibBut: calibBut[hilo],
				midiSrc: midiSrc[hilo],
				midiChan: midiChan[hilo],
				midiCtrl: midiCtrl[hilo],
				midiLearn: midiLearn[hilo]
			)
		});

		this.initControllerActions(\lo);
		this.initControllerActions(\hi);
	}
	
}