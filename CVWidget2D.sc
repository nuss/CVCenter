CVWidget2D : CVWidget {
	var <widgetCV;
	var <window, <guiEnv, <midiOscEnv, <editorEnv;
	var <slider2d, <rangeSlider, <numVal, <specBut;
	var <midiHead, <midiLearn, <midiSrc, <midiChan, <midiCtrl, <oscEditBut, <calibBut, <editor;
	var prSpec;
	var calibConstraintsHi, calibConstraintsLo;

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
	
	init { |window, cvs, name, bounds, actions, setUpArgs, controllersAndModels, cvcGui, server|
		var thisName, thisXY, thisWidth, thisHeight, knobsize, widgetSpecsActions;
		var msrc = "source", mchan = "chan", mctrl = "ctrl", margs;
		var nextY, rightBarX;
		var actionLo, actionHi;
				
		guiEnv = ();
		editorEnv = ();
		cvcGui !? { isCVCWidget = true };
		
		if(cvcGui.class == Event and:{ cvcGui.midiOscEnv.notNil }, { midiOscEnv = cvcGui.midiOscEnv }, { midiOscEnv = () });
		midiOscEnv.oscMapping ?? { midiOscEnv.oscMapping = \linlin };
				
		if(name.isNil, { thisName = "knob" }, { thisName = name });
		wdgtInfo = thisName.asString;
		
		widgetCV = ();
		cvs !? {
			if(cvs.class !== Array, {
				Error("Please provide CVs in an array: [cv1, cv2]").throw;
			}, {
				if(cvs[0].isNil, { widgetCV.lo = CV.new }, { widgetCV.lo = widgetCV.lo });
				if(cvs[1].isNil, { widgetCV.hi = CV.new }, { widgetCV.hi = widgetCV.hi });
			})
		};
		
		numVal = (); specBut = ();
		midiHead = (); midiLearn = (); midiSrc = (); midiChan = (); midiCtrl = ();
		oscEditBut = (); calibBut = ();
		editor = ();
		
		setUpArgs.isKindOf(Array).not.if { setUpArgs = [setUpArgs] };
		
		setUpArgs[0] !? { this.midiMode_(setUpArgs[0]) };
		setUpArgs[1] !? { this.midiResolution_(setUpArgs[1]) };
		setUpArgs[2] !? { this.midiMean_(setUpArgs[2]) };
		setUpArgs[4] !? { this.ctrlButtonBank_(setUpArgs[4]) };
		setUpArgs[5] !? { this.softWithin_(setUpArgs[5]) };
		setUpArgs[6] !? { prCalibrate = (setUpArgs[6]) };
		
		actions !? {
			if(actions.class !== Array, {
				Error("Please provide actions in an array: [action1, action2]").throw;
			}, {
				actions[0] !? { widgetCV.lo.action_(actionLo) };
				actions[1] !? { widgetCV.hi.action_(actionHi) };
			})
		};
		
		[\lo, \hi].do(this.initControllersAndModels(controllersAndModels, _));

		if(bounds.isNil, {		
			thisXY = 7@0;
			thisWidth = 122;
			thisHeight = 166;
		}, {
			if(window.isNil, { thisXY = 7@0 }, { thisXY = bounds.left@bounds.top });
			thisWidth = bounds.width;
			thisHeight = bounds.height;
		});
		
		if(window.isNil, {
			window = Window(thisName, Rect(50, 50, thisWidth+14, thisHeight+7), server: server);
		}, {
			window = window;
		});
						
		cvcGui ?? { 
			window.onClose_({
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
				[\lo, \hi].do({ |key|
					midiOscEnv.oscResponder[key] !? { midiOscEnv.oscResponder[key].remove.postln };
					midiOscEnv.cc[key] !? { midiOscEnv.cc[key].remove };
					wdgtControllersAndModels[key].do({ |mc| mc.isKindOf(SimpleController).if{ mc.controller.remove } });
				})
			})
		};
						
		widgetBg = UserView(window, Rect(thisXY.x, thisXY.y, thisWidth, thisHeight))
			.focusColor_(Color(alpha: 1.0))
			.background_(Color.white)
		;
		label = Button(window, Rect(thisXY.x+1, thisXY.y+1, thisWidth-2, 15))
			.states_([
				[""++name.asString, Color.white, Color.blue],
				[""++name.asString, Color.black, Color.yellow],
			])
			.font_(Font("Helvetica", 9))
			.action_({ |b|
				this.toggleComment(b.value);
			})
		;
		nameField = TextView(window, Rect(label.bounds.left, label.bounds.top+label.bounds.height, thisWidth-2, thisHeight-label.bounds.height-2))
			.background_(Color.white)
			.font_(Font("Helvetica", 9))
			.string_(name.asString)
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
			[k, v].postln;
			k.bounds_(Rect(
				v[0], 
				nextY,
				this.rangeSlider.bounds.width/2,
				15
			));
			k.value_(v[1].value);
//			k.canFocus_(false)
		});
		
		specBut.lo = Button(window);
		specBut.hi = Button(window);
		midiHead.lo = Button(window);
		midiHead.hi = Button(window);
		midiLearn.lo = Button(window);
		midiLearn.hi = Button(window);
		midiSrc.lo = TextField(window);
		midiSrc.hi = TextField(window);
		midiChan.lo = TextField(window);
		midiChan.hi = TextField(window);
		midiCtrl.lo = TextField(window);
		midiCtrl.hi = TextField(window);
		
		nextY = thisXY.y+1+label.bounds.height;

		[specBut.hi, [nextY, \hi], specBut.lo, [nextY+52, \lo]].pairsDo({ |k, v|
			k.bounds_(Rect(thisXY.x+rightBarX, v[0], 40, 13))
			.font_(Font("Helvetica", 8))
			.focusColor_(Color(alpha: 0))
			.states_([["edit Spec", Color.black, Color(241/255, 209/255, 0)]])
			.action_({ |btn|
				CVWidget.editorWindow_(this, name, v[1]);
			})
		});
		
		nextY = nextY+14;
				
		[midiHead.hi, nextY, midiHead.lo, nextY+52].pairsDo({ |k, v|
			k.bounds_(Rect(thisXY.x+rightBarX, v, 28, 13))
			.font_(Font("Helvetica", 7))
			.focusColor_(Color(alpha: 0))
			.states_([["MIDI", Color.black, Color(alpha: 0)]])
			.action_({ |ms|  ms.postln })
		});
		
		
		[midiLearn.hi, nextY, midiLearn.lo, nextY+52].pairsDo({ |k, v|
			k.bounds_(Rect(thisXY.x+rightBarX+midiHead.lo.bounds.width, v, 12, 13))
			.font_(Font("Helvetica", 7))
			.focusColor_(Color(alpha: 0))
			.states_([
				["L", Color.white, Color.blue],
				["X", Color.white, Color.red]
			])
		});
		
		nextY = nextY+13;
		
		[midiSrc.hi, nextY, midiSrc.lo, nextY+52].pairsDo({ |k, v|
			k.bounds_(Rect(thisXY.x+rightBarX, v, 40, 13))
			.font_(Font("Helvetica", 8.5))
			.focusColor_(Color(alpha: 0))
			.string_("source")
			.canFocus_(false)
			.background_(Color(alpha: 0))
			.stringColor_(Color.black)
		});

		nextY = nextY+13;

		[midiChan.hi, nextY, midiChan.lo, nextY+52].pairsDo({ |k, v|
			k.bounds_(Rect(thisXY.x+rightBarX, v, 15, 13))
			.font_(Font("Helvetica", 8.5))
			.focusColor_(Color(alpha: 0))
			.string_("chan")
			.canFocus_(false)
			.background_(Color(alpha: 0))
			.stringColor_(Color.black)
		});

//		nextY = nextY+12;

		[midiCtrl.hi, nextY, midiCtrl.lo, nextY+52].pairsDo({ |k, v|
			k.bounds_(Rect(thisXY.x+rightBarX+15, v, 25, 13))
			.font_(Font("Helvetica", 8.5))
			.focusColor_(Color(alpha: 0))
			.string_("ctrl")
			.canFocus_(false)
			.background_(Color(alpha: 0))
			.stringColor_(Color.black)
		});
		
//		prCCResponderAdd(widgetCV.hi, midiLearn.hi, midiSrc.hi, midiChan.hi, midiCtrl.hi, midiHead.hi, \hi);
//		prCCResponderAdd(widgetCV.lo, midiLearn.lo, midiSrc.lo, midiChan.lo, this.midiCtrl.lo, midiHead.lo, \lo);
		
		[slider2d, rangeSlider].do({ |view| [widgetCV.lo, widgetCV.hi].connect(view) });
		widgetCV.lo.connect(numVal.lo);
		widgetCV.hi.connect(numVal.hi);

		visibleGuiEls = [slider2d, rangeSlider, numVal.hi, numVal.lo, specBut.hi, specBut.lo, midiHead.hi, midiHead.lo, midiLearn.hi, midiLearn.lo, midiSrc.hi, midiSrc.lo, midiChan.hi, midiChan.lo, midiCtrl.hi, midiCtrl.lo];

		allGuiEls = [widgetBg, label, nameField, slider2d, rangeSlider, numVal.hi, numVal.lo, specBut.hi, specBut.lo, midiHead.hi, midiHead.lo, midiLearn.hi, midiLearn.lo, midiSrc.hi, midiSrc.lo, midiChan.hi, midiChan.lo, midiCtrl.hi, midiCtrl.lo]
	}
	
	spec_ { |spec, hilo|
		if(hilo.isNil or:{ [\hi, \lo].includes(hilo).not }, {
			Error("In order to set the inbuilt spec you must provide either \lo or \hi, indicating which spec shall be set").throw;
		});
		if(spec.isKindOf(ControlSpec), {
			widgetCV[hilo].spec_(spec);
		}, {
			Error("Please provide a valid ControlSpec!").throw;
		})
	}
	
	spec { |hilo|
		^widgetCV[hilo].spec;
	}
	
	oscConnect { |addr=nil, name, oscMsgIndex, hilo|
		hilo ?? { Error("Please provide the CV's key \('hi' or 'lo')!").throw };
		if(hilo.asSymbol === \lo, {
			this.oscResponderLo = OSCresponderNode(addr, name.asSymbol, { |t, r, msg|
				if(prCalibrate, { 
					if(calibConstraintsLo.isNil, {
						calibConstraintsLo = (lo: msg[oscMsgIndex], hi: msg[oscMsgIndex]);
					}, {
						if(msg[oscMsgIndex] < calibConstraintsLo.lo, { calibConstraintsLo.lo = msg[oscMsgIndex] });
						if(msg[oscMsgIndex] > calibConstraintsLo.hi, { calibConstraintsLo.hi = msg[oscMsgIndex] });
					})
				}, {
					if(calibConstraintsLo.isNil, {
						calibConstraintsLo = (lo: 0, hi: 0);
					})	
				});
				widgetCV[\lo].value_(
					msg[oscMsgIndex].perform(
						this.prOSCMappingLo,
						this.calibConstraintsLo.lo, this.calibConstraintsLo.hi,
						widgetCV[hilo].spec.minval, widgetCV[hilo].spec.maxval,
						\minmax
					)
				)
			}).add
		});
		if(hilo.asSymbol === \hi, {
			this.oscResponderHi = OSCresponderNode(addr, name.asSymbol, { |t, r, msg|
				if(prCalibrate, { 
					if(calibConstraintsHi.isNil, {
						calibConstraintsHi = (lo: msg[oscMsgIndex], hi: msg[oscMsgIndex]);
					}, {
						if(msg[oscMsgIndex] < calibConstraintsHi.lo, { calibConstraintsHi.lo = msg[oscMsgIndex] });
						if(msg[oscMsgIndex] > calibConstraintsHi.hi, { calibConstraintsHi.hi = msg[oscMsgIndex] });
					})
				}, {
					if(calibConstraintsHi.isNil, {
						calibConstraintsHi = (lo: 0, hi: 0);
					})	
				});
				widgetCV[\hi].value_(
					msg[oscMsgIndex].perform(
						this.prOSCMappingHi,
						this.calibConstraintsHi.lo, this.calibConstraintsHi.hi,
						widgetCV[hilo].spec.minval, widgetCV[hilo].spec.maxval,
						\minmax
					)
				)
			}).add
		})
	}
	
	oscDisconnect { |hilo|
		hilo ?? { Error("Please provide the CV's key \(\hi or \lo\)!").throw };
		if(hilo.asSymbol === \hi, {
			this.oscResponderHi.remove;
//			this.oscInputRangeRangeModelHi_(`[0.0, 0.0]);
			this.calibConstraintsHi_(nil);	
		});
		if(hilo.asSymbol === \lo, {
			this.oscResponderLo.remove;
//			this.oscInputRangeRangeModelLo_(`[0.0, 0.0]);
			this.calibConstraintsLo_(nil);	
		})
	}
	
}