CVWidget2D : CVWidget {
	var thisCV, midiLearnActions;
	var <>slider2d, <>rangeSlider;
	var <>numValHi, <>numValLo, <>specButHi, <>specButLo;
	var <>midiHeadLo, <>midiLearnLo, <>midiSrcLo, <>midiChanLo, <>midiCtrlLo;
	var <>midiHeadHi, <>midiLearnHi, <>midiSrcHi, <>midiChanHi, <>midiCtrlHi;
	var <>ccLo, <>ccHi, specLo, specHi;
	var <>prOSCMappingLo = \linlin, <>prOSCMappingHi = \linlin;
	var <>calibConstraintsLo, <>oscResponderLo;
	var <>calibConstraintsHi, <>oscResponderHi;

	*new { |parent, cvs, name, bounds, setUpArgs|
		^super.new.init(parent, cvs[0], cvs[1], name, bounds.left@bounds.top, bounds.width, bounds.height, setUpArgs)
	}
	
	init { |parentView, cvLo, cvHi, name, xy, widgetwidth=122, widgetheight=122, setUpArgs|
		var meanVal, widgetSpecsAction, editor, cvString;
		var tmpSetup, thisToggleColor, nextY, rightBarX=widgetwidth-41;
		
		thisCV = (lo: cvLo, hi: cvHi);
		
		setUpArgs.isKindOf(Array).not.if { setUpArgs = [setUpArgs] };
		
		setUpArgs[0] !? { this.midimode_(setUpArgs[0]) };
		setUpArgs[1] !? { this.midiresolution_(setUpArgs[1]) };
		setUpArgs[2] !? { this.midimean_(setUpArgs[2]) };
		setUpArgs[3] !? { this.midistring_(setUpArgs[3].asString) };
		setUpArgs[4] !? { this.ctrlButtonBank_(setUpArgs[4]) };
		setUpArgs[5] !? { this.softWithin_(setUpArgs[5]) };
		setUpArgs[6] !? { prCalibrate = (setUpArgs[6]) };

		widgetBg = UserView(parentView, Rect(xy.x, xy.y, widgetwidth, widgetheight))
			.focusColor_(Color(alpha: 1.0))
			.background_(Color.white)
		;
		label= Button(parentView, Rect(xy.x+1, xy.y+1, widgetwidth-2, 15))
			.states_([
				[""++name.asString, Color.white, Color.blue],
				[""++name.asString, Color.black, Color.yellow],
			])
			.font_(Font("Helvetica", 9))
			.action_({ |b|
				this.toggleComment(b.value);
			})
			.canFocus_(false)
		;
		nextY = label.bounds.top+label.bounds.height;
		nameField = TextField(parentView, Rect(xy.x+1, nextY, widgetwidth-2, widgetheight-label.bounds.height-2))
			.background_(Color.white)
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.value_(name.asString)
			.action_({ |nf| nf.value_(nf.value) })
			.visible_(false)
		;
		this.slider2d = Slider2D(parentView, Rect(xy.x+1, nextY, widgetwidth-42, widgetwidth-47))
			.canFocus_(false)
			.background_(Color.white)
			.knobColor_(Color.red)
		;
		nextY = nextY+this.slider2d.bounds.height;
		this.rangeSlider = RangeSlider(parentView, Rect(
			xy.x+1,
			nextY,
			widgetwidth-42,
			15
		))
		.canFocus_(false)
		.background_(Color.white);
		nextY = nextY+this.rangeSlider.bounds.height;
		this.numValLo = NumberBox(parentView);
		this.numValHi = NumberBox(parentView);
		
		[this.numValLo, [xy.x+1, cvLo], this.numValHi, [xy.x+(widgetwidth-42/2), cvHi]].pairsDo({ |k, v|
			k.bounds_(Rect(
				v[0], 
				nextY,
				this.rangeSlider.bounds.width/2,
				15
			));
			k.value_(v[1].value);
//			k.canFocus_(false)
		});
		
		this.specButLo = Button(parentView);
		this.specButHi = Button(parentView);
		this.midiHeadLo = Button(parentView);
		this.midiHeadHi = Button(parentView);
		this.midiLearnLo = Button(parentView);
		this.midiLearnHi = Button(parentView);
		this.midiSrcLo = TextField(parentView);
		this.midiSrcHi = TextField(parentView);
		this.midiChanLo = TextField(parentView);
		this.midiChanHi = TextField(parentView);
		this.midiCtrlLo = TextField(parentView);
		this.midiCtrlHi = TextField(parentView);
		
		nextY = xy.y+1+label.bounds.height;

		[this.specButHi, [nextY, \hi], this.specButLo, [nextY+52, \lo]].pairsDo({ |k, v|
			k.bounds_(Rect(xy.x+rightBarX, v[0], 40, 13))
			.font_(Font("Helvetica", 8))
			.focusColor_(Color(alpha: 0))
			.states_([["edit Spec", Color.black, Color(241/255, 209/255, 0)]])
			.action_({ |btn|
				CVWidget.editorWindow_(this, name, v[1]);
			})
		});
		
		nextY = nextY+14;
				
		[this.midiHeadHi, nextY, this.midiHeadLo, nextY+52].pairsDo({ |k, v|
			k.bounds_(Rect(xy.x+rightBarX, v, 28, 13))
			.font_(Font("Helvetica", 7))
			.focusColor_(Color(alpha: 0))
			.states_([["MIDI", Color.black, Color(alpha: 0)]])
			.action_({ |ms|  ms.postln })
		});
		
		
		[this.midiLearnHi, nextY, this.midiLearnLo, nextY+52].pairsDo({ |k, v|
			k.bounds_(Rect(xy.x+rightBarX+midiHeadLo.bounds.width, v, 12, 13))
			.font_(Font("Helvetica", 7))
			.focusColor_(Color(alpha: 0))
			.states_([
				["L", Color.white, Color.blue],
				["X", Color.white, Color.red]
			])
		});
		
		nextY = nextY+13;
		
		[this.midiSrcHi, nextY, this.midiSrcLo, nextY+52].pairsDo({ |k, v|
			k.bounds_(Rect(xy.x+rightBarX, v, 40, 13))
			.font_(Font("Helvetica", 8.5))
			.focusColor_(Color(alpha: 0))
			.string_("source")
			.canFocus_(false)
			.background_(Color(alpha: 0))
			.stringColor_(Color.black)
		});

		nextY = nextY+13;

		[this.midiChanHi, nextY, this.midiChanLo, nextY+52].pairsDo({ |k, v|
			k.bounds_(Rect(xy.x+rightBarX, v, 15, 13))
			.font_(Font("Helvetica", 8.5))
			.focusColor_(Color(alpha: 0))
			.string_("chan")
			.canFocus_(false)
			.background_(Color(alpha: 0))
			.stringColor_(Color.black)
		});

//		nextY = nextY+12;

		[this.midiCtrlHi, nextY, this.midiCtrlLo, nextY+52].pairsDo({ |k, v|
			k.bounds_(Rect(xy.x+rightBarX+15, v, 25, 13))
			.font_(Font("Helvetica", 8.5))
			.focusColor_(Color(alpha: 0))
			.string_("ctrl")
			.canFocus_(false)
			.background_(Color(alpha: 0))
			.stringColor_(Color.black)
		});
		
		this.prCCResponderAdd(cvHi, this.midiLearnHi, this.midiSrcHi, this.midiChanHi, this.midiCtrlHi, this.midiHeadHi, \hi);
		this.prCCResponderAdd(cvLo, this.midiLearnLo, this.midiSrcLo, this.midiChanLo, this.midiCtrlLo, this.midiHeadLo, \lo);
		
		[this.slider2d, this.rangeSlider].do({ |view| [cvLo, cvHi].connect(view) });
		cvLo.connect(this.numValLo);
		cvHi.connect(this.numValHi);

		visibleGuiEls = [this.slider2d, this.rangeSlider, this.numValHi, this.numValLo, this.specButHi, this.specButLo, this.midiHeadHi, this.midiHeadLo, this.midiLearnHi, this.midiLearnLo, this.midiSrcHi, this.midiSrcLo, this.midiChanHi, this.midiChanLo, this.midiCtrlHi, this.midiCtrlLo];

		allGuiEls = [widgetBg, label, nameField, this.slider2d, this.rangeSlider, this.numValHi, this.numValLo, this.specButHi, this.specButLo, this.midiHeadHi, this.midiHeadLo, this.midiLearnHi, this.midiLearnLo, this.midiSrcHi, this.midiSrcLo, this.midiChanHi, this.midiChanLo, this.midiCtrlHi, this.midiCtrlLo]
	}
	
	spec_ { |spec, hilo|
		if(hilo.isNil or:{ [\hi, \lo].includes(hilo).not }, {
			Error("In order to set the inbuilt spec you must provide either \lo or \hi, indicating which spec shall be set").throw;
		});
		if(spec.isKindOf(ControlSpec), {
			thisCV[hilo].spec_(spec);
		}, {
			Error("Please provide a valid ControlSpec!").throw;
		})
	}
	
	spec { |hilo|
		^thisCV[hilo].spec;
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
				thisCV[\lo].value_(
					msg[oscMsgIndex].perform(
						this.prOSCMappingLo,
						this.calibConstraintsLo.lo, this.calibConstraintsLo.hi,
						thisCV[hilo].spec.minval, thisCV[hilo].spec.maxval,
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
				thisCV[\hi].value_(
					msg[oscMsgIndex].perform(
						this.prOSCMappingHi,
						this.calibConstraintsHi.lo, this.calibConstraintsHi.hi,
						thisCV[hilo].spec.minval, thisCV[hilo].spec.maxval,
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