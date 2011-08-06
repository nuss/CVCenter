CVWidget2D : CVWidget {
	var <widgetCV;
	var <window, <guiEnv, <midiOscEnv, <editorEnv;
	var <slider2d, <rangeSlider, <numVal, <specBut;
	var <midiHead, <midiLearn, <midiSrc, <midiChan, <midiCtrl, <oscEditBut, <calibBut, <editor;
	var prSpec;
	var calibConstraintsHi, calibConstraintsLo;

	*new { |parent, cvs, name, bounds, actions, setup, controllersAndModels, cvcGui, server|
		^super.newCopyArgs.init(
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
	
	init { |parentView, cvs, name, bounds, actions, setUpArgs, controllersAndModels, cvcGui, server|
		var thisName, thisXY, thisWidth, thisHeight, knobsize, widgetSpecsActions;
		var msrc = "source", mchan = "chan", mctrl = "ctrl", margs;
		var nextY, rightBarX;
		var actionLo, actionHi;
		
		prCalibrate ? prCalibrate = (lo: true, hi: true);
				
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

		setUpArgs.isKindOf(Array).not.if { setUpArgs = [setUpArgs] };
		
		setUpArgs[6] ? prCalibrate = (lo: true, hi: true);
				
		setUpArgs[0] !? { this.midiMode_(setUpArgs[0]) };
		setUpArgs[1] !? { this.midiResolution_(setUpArgs[1]) };
		setUpArgs[2] !? { this.midiMean_(setUpArgs[2]) };
		setUpArgs[4] !? { this.ctrlButtonBank_(setUpArgs[4]) };
		setUpArgs[5] !? { this.softWithin_(setUpArgs[5]) };
		setUpArgs[6] !? { prCalibrate = (lo: setUpArgs[6], hi: setUpArgs[6]) };
						
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
			if(window.isNil, { thisXY = 7@0 }, { thisXY = bounds.left@bounds.top });
			thisWidth = bounds.width;
			thisHeight = bounds.height;
		});
				
		if(window.isNil, {
			window = Window(thisName, Rect(50, 50, thisWidth+14, thisHeight+7), server: server);
		}, {
			window = parentView;
		});
						
		cvcGui ?? { 
			window.onClose_({
				[\lo, \lo].do({ |hilo|
					if(editor[hilo].notNil, {
						if(editor[hilo].isClosed.not, {
							editor[hilo].close;
						}, {
							if(CVWidgetEditor.allEditors.notNil and:{
								CVWidgetEditor.allEditors[thisName.asSymbol].notNil;
							}, {
								CVWidgetEditor.allEditors.removeAt(thisName.asSymbol)
							})
						})
					});
					midiOscEnv[hilo].oscResponder !? { midiOscEnv[hilo].oscResponder.remove.postln };
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
		rightBarX = slider2d.bounds.width+1;

		[specBut.hi, [nextY, \hi], specBut.lo, [nextY+52, \lo]].pairsDo({ |k, v|
			k.bounds_(Rect(thisXY.x+rightBarX, v[0], 40, 13))
			.font_(Font("Helvetica", 8))
			.focusColor_(Color(alpha: 0))
			.states_([["edit Spec", Color.black, Color(241/255, 209/255, 0)]])
		});
		
		nextY = nextY+14;
				
		[midiHead.hi, nextY, midiHead.lo, nextY+52].pairsDo({ |k, v|
			k.bounds_(Rect(thisXY.x+rightBarX, v, 28, 13))
			.font_(Font("Helvetica", 7))
			.focusColor_(Color(alpha: 0))
			.states_([["MIDI", Color.black, Color(alpha: 0)]])
		});
		
		
		[midiLearn.hi, [\hi, nextY], midiLearn.lo, [\lo, nextY+52]].pairsDo({ |k, v|
			k.bounds_(Rect(thisXY.x+rightBarX+midiHead.lo.bounds.width, v[1], 12, 13))
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
							this.midiConnect(*margs, hilo: v[0]);
						}, {
							this.midiConnect(hilo: v[0]);
						})
					},
					0, { this.midiDisconnect(v[0]) }
				)
			})
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
		
		nextY = label.bounds.top+label.bounds.height+slider2d.bounds.height+rangeSlider.bounds.height+numVal.lo.bounds.height+1;
		
		oscEditBut.lo = Button(window);
		oscEditBut.hi = Button(window);
		calibBut.lo = Button(window);
		calibBut.hi = Button(window);
		
		[oscEditBut.lo, [\lo, thisXY.x+1], oscEditBut.hi, [\hi, thisXY.x+(thisWidth/2)]].pairsDo({ |k, v|
			k.bounds_(Rect(v[1], nextY, thisWidth/2-1, thisHeight-(label.bounds.top+label.bounds.height+slider2d.bounds.height+rangeSlider.bounds.height+numVal.lo.bounds.height+15)))
			.font_(Font("Helvetica", 8.5))
			.focusColor_(Color(alpha: 0))
			.states_([
				["edit OSC", Color.black, Color.clear]
			])
			.action_({ |oscb|
				("opening editor:"+thisName).postln;
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
				("calibrate button action triggered:"+cb.value).postln;
				switch(cb.value,
					0, { cb.value.postln; this.setCalibrate(true, v[0]) },
					1, { cb.value.postln; this.setCalibrate(false, v[0]) }
				)
			})
		});
		
//		prCCResponderAdd(widgetCV.hi, midiLearn.hi, midiSrc.hi, midiChan.hi, midiCtrl.hi, midiHead.hi, \hi);
//		prCCResponderAdd(widgetCV.lo, midiLearn.lo, midiSrc.lo, midiChan.lo, this.midiCtrl.lo, midiHead.lo, \lo);
		
		[slider2d, rangeSlider].do({ |view| [widgetCV.lo, widgetCV.hi].connect(view) });
		widgetCV.lo.connect(numVal.lo);
		widgetCV.hi.connect(numVal.hi);

		visibleGuiEls = [
			slider2d, 
			rangeSlider, 
			numVal.lo, numVal.hi, 
			midiHead.lo, midiHead.hi, 
			midiLearn.lo, midiHead.lo, 
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
			specBut.lo, specBut.lo, 
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
				oscEditBut: oscEditBut[hilo],
				midiSrc: midiSrc[hilo],
				midiChan: midiChan[hilo],
				midiCtrl: midiCtrl[hilo],
				midiLearn: midiLearn[hilo]
			)
		});

		this.initControllerActions(\lo);
		this.initControllerActions(\hi);
	}
	
	setCalibrate { |bool, hilo|
		hilo ?? {
			Error("CVWidget2D: no key for calibration provided!").throw;
		};
		if(bool.isKindOf(Boolean).not, {
			Error("calibration can only be set to true or false!").throw;
		});
		prCalibrate[hilo] = bool;
		wdgtControllersAndModels[hilo].oscConnection.model.value_(wdgtControllersAndModels[hilo].oscConnection.model.value).changed(\value);
		wdgtControllersAndModels[hilo].calibration.model.value_(bool).changed(\value);
	}
	
	getCalibrate { |hilo|
		^prCalibrate[hilo];
	}
	
	setSpec { |spec, hilo|
		if(hilo.isNil or:{ [\hi, \lo].includes(hilo).not }, {
			Error("In order to set the inbuilt spec you must provide either \lo or \hi, indicating which spec shall be set").throw;
		});
		if(spec.isKindOf(ControlSpec), {
			widgetCV[hilo].spec_(spec);
		}, {
			Error("Please provide a valid ControlSpec!").throw;
		})
	}
	
	getSpec { |hilo|
		^widgetCV[hilo].spec;
	}
	
	setOscMapping { |mapping, hilo|
		if(hilo.isNil or:{ [\hi, \lo].includes(hilo).not }, {
			Error("In order to set the OSC=mapping you must provide either \lo or \hi, indicating which OSC-mapping shall be set").throw;
		});
		if(mapping.asSymbol !== \linlin and:{
			mapping.asSymbol !== \linexp and:{
				mapping.asSymbol !== \explin and:{
					mapping.asSymbol !== \expexp
				}
			}
		}, {
			Error("A valid mapping can either be \\linlin, \\linexp, \\explin or \\expexp").throw;
		}, {
			midiOscEnv[hilo].oscMapping = mapping.asSymbol;
			wdgtControllersAndModels[hilo].oscInputRange.model.value_(
				wdgtControllersAndModels[hilo].oscInputRange.model.value;
			).changed(\value);
			wdgtControllersAndModels[hilo].cvSpec.model.value_(
				wdgtControllersAndModels[hilo].cvSpec.model.value;
			).changed(\value);
		})
	}
	
	getOscMapping { |hilo|
		^midiOscEnv[hilo].oscMapping;
	}
		
	oscConnect { |ip, port, name, oscMsgIndex, hilo|
		var intPort;
		hilo ?? { Error("Please provide the CV's key \('hi' or 'lo'\)!").throw };
		if(ip.size > 0 and:{ "^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$".matchRegexp(ip).not }, {
			Error("Please provide a valid IP-address or leave the IP-field empty").throw;
		});
		
		if(port.size > 0, {
			if("^[0-9]{1,5}$".matchRegexp(port).not and:{ port != "nil" }, {
				Error("Please provide a valid port or leave this field empty").throw;
			}, {
				intPort = port.asInt;
			})
		});
		
		if(port == "nil", { intPort = nil });
		
		if("^\/".matchRegexp(name.asString).not, {
			Error("You have to supply a valid OSC-typetag, beginning with an \"/\" as first argument to oscConnect").throw;
		});
		
		if(oscMsgIndex.isKindOf(Integer).not, {
			Error("You have to supply an integer as second argument to oscConnect").throw;
		});

		wdgtControllersAndModels[hilo].oscConnection.model.value_([ip, intPort, name, oscMsgIndex]).changed(\value);
		CmdPeriod.add({ this.oscDisconnect(hilo) });
	}
	
	oscDisconnect { |hilo|
		hilo ?? { Error("Please provide the CV's key \(\hi or \lo\)!").throw };
		if(this.isClosed.not, {
			wdgtControllersAndModels[hilo].oscConnection.model.value_(false).changed(\value);
			wdgtControllersAndModels[hilo].oscInputRange.model.value_([0.00001, 0.00001]).changed(\value);
		}, {
			midiOscEnv[hilo].oscResponder.remove;
		});
		CmdPeriod.remove({ this.oscDisconnect(hilo) });
	}
	
	// if all arguments are nil .learn should be triggered
	midiConnect { |uid, chan, num, hilo|
		var args;
		("hilo:"+hilo).postln;
		if(midiOscEnv[hilo].cc.isNil, {
			args = [uid, chan, num].select({ |param| param.notNil }).collect({ |param| param.asInt });
			wdgtControllersAndModels[hilo].midiConnection.model.value_(
				(src: uid, chan: chan, num: num)
			).changed(\value);
			CmdPeriod.add({ this !? { this.midiDisconnect(hilo) } });
		}, {
			"Already connected!".warn;	
		})
	}
	
	midiDisconnect { |hilo|
		midiOscEnv.cc.notNil !? {
			if(this.isClosed.not, {
				wdgtControllersAndModels[hilo].midiConnection.model.value_(nil).changed(\value);
			}, {
				midiOscEnv[hilo].cc.remove;
			})		
		};
		CmdPeriod.remove({ this.midiDisconnect(hilo) });
	}
		
}