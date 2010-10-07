
CVWidget {

	classvar <editorWindow, <window;
	var <>midimode = 0, <>midimean = 64, <>midistring = "", <>ctrlButtonBank, <>midiresolution=1, <>softWithin = 0.1;
	var visibleGuiEls, allGuiEls;
	var <>widgetBg, <>label, <>nameField; // elements contained in any kind of CVWidget
	var <visible, widgetXY, widgetProps;

	setup {
		^[this.midimode, this.midiresolution, this.midimean, this.midistring, this.ctrlButtonBank, this.softwithin];
	}
	
	visible_ { |visible|
		if(visible.isKindOf(Boolean).not, {
			^nil;
		}, {
			if(visible, {
				allGuiEls.do({ |el| 
					if(el === this.nameField, {
						el.visible_(false);
					}, {
						el.visible_(true);
					})	
				});
			}, {
				allGuiEls.do(_.visible_(false));
			})
		})
	}
	
	toggleComment { |visible|
		visible.switch(
			0, { 
				visibleGuiEls.do({ |el| 
					el.visible_(true);
					this.nameField.visible_(false);
				})
			},
			1, {
				visibleGuiEls.do({ |el|
					el.visible_(false);
					this.nameField.visible_(true);
				})
			}
		)
	}
	
	widgetXY_ { |point|
		var originXZero, originYZero;
		originXZero = allGuiEls.collect({ |view| view.bounds.left });
		originXZero = originXZero-originXZero.minItem;
		originYZero = allGuiEls.collect({ |view| view.bounds.top });
		originYZero = originYZero-originYZero.minItem;
		
		allGuiEls.do({ |view, i|
			view.bounds_(Rect(originXZero[i]+point.x, originYZero[i]+point.y, view.bounds.width, view.bounds.height));
		})
	}
	
	widgetXY {
		^this.widgetBg.bounds.left@this.widgetBg.bounds.top;
	}
	
	widgetProps {
		^this.widgetBg.bounds.width@this.widgetBg.bounds.height;
	}
	
	bounds {
		^Rect(this.widgetXY.x, this.widgetXY.y, this.widgetProps.x, this.widgetProps.y);
	}
	
	remove {
		allGuiEls.do(_.remove);
	}

	// private
	prCCResponderAdd { |cv, learnBut, srcField, chanField, ctrlField, headBut, key| 
		var cc;
		learnBut.action_({ |but|
			{
				loop {
					0.01.wait;
					if(but.value == 1, {
//						"adding a new CCResponder".postln;
						cc = CCResponder({ |src, chan, ctrl, val|
							var ctrlString, meanVal;
//							[src, chan, ctrl, val].postln;
							ctrlString ? ctrlString = ctrl+1;
	
							if(this.ctrlButtonBank.notNil, {
								if(ctrlString%this.ctrlButtonBank == 0, {
									ctrlString = this.ctrlButtonBank.asString;
								}, {
									ctrlString = (ctrlString%this.ctrlButtonBank).asString;
								});
								ctrlString = ((ctrl+1/this.ctrlButtonBank).ceil).asString++":"++ctrlString;
							}, {
								ctrlString = ctrl+1;
							});
	
	//						this.setup.postln;
							this.midimode.switch(
								0, { 
									if(val/127 < (cv.input+(softWithin/2)) and: {
										val/127 > (cv.input-(softWithin/2));
									}, { 
										cv.input_(val/127);
									})
								},
								1, { 
									meanVal = this.midimean;
									cv.input_(cv.input+((val-meanVal)/127*this.midiresolution)) 
								}
							);
							{
								try {
									srcField.string_(src.asString)
										.background_(Color.red)
										.stringColor_(Color.white)
									;
									chanField.string_((chan+1).asString)
										.background_(Color.red)
										.stringColor_(Color.white)
									;
									ctrlField.string_(ctrlString.asString)
										.background_(Color.red)
										.stringColor_(Color.white)
									;
									headBut.enabled_(false);
								}
							}.defer;
						});
						cc.learn;
						key.switch(
							\hi, { this.ccHi = cc },
							\lo, { this.ccLo = cc },
							{ this.cc = cc }
						);
//						oneShot = this.cc.oneShotLearn;
						nil.yield;
					}, {
						"no CCResponder yet or just removing the existing one".postln;
//						oneShot !? { oneShot.remove };
						key.switch(
							\hi, { 
								this.ccHi.remove; 
								this.ccHi = nil;
							},
							\lo, {
								this.ccLo.remove;
								this.ccLo = nil;
							}, {
								this.cc.remove; 
								this.cc = nil;
							}
						);
						srcField.string_("source")
							.background_(Color(alpha: 0))
							.stringColor_(Color.black)
						;
						chanField.string_("chan")
							.background_(Color(alpha: 0))
							.stringColor_(Color.black)
						;
						ctrlField.string_("ctrl")
							.background_(Color(alpha: 0))
							.stringColor_(Color.black)
						;
						headBut.enabled_(true);
						nil.yield;
					})
				}
			}.fork(AppClock);
		})
		^cc;
	}
	
	*editorWindow_ { |widget, widgetName, hilo|
		var specsList, specsActions, editor, cvString, slot;
		
		if(hilo.notNil, {
			slot = "["++hilo.asString++"]";
		}, {
			slot = "";
		});
		
		if(Window.allWindows.select({ |w| "^Widget-Spec Editor:".matchRegexp(w.name) == true }).size < 1, {			window = Window("Widget-Spec Editor:"+widgetName+slot, Rect(Window.screenBounds.width/2-150, Window.screenBounds.height/2-100, 330, 200));
		}, {
			window.name_("Widget-Spec Editor:"+widgetName+slot);
		});
		specsList = ListView(window, Rect(0, 0, window.bounds.width, window.bounds.height))
			.resize_(5)
			.background_(Color.white)
			.hiliteColor_(Color.blue)
			.selectedStringColor_(Color.white)
			.visible_(true)
			.enterKeyAction_({ |ws|
				specsActions[ws.value].value;
				if(widget.class === CVWidgetKnob, {
					block { |break|
						#[\pan, \boostcut, \bipolar, \detune].do({ |symbol| 
							if(widget.spec == symbol.asSpec, { 
								break.value(widget.knob.centered_(true));
							}, {
								widget.knob.centered_(false);
							})			
						})
					}
				});
				[ws.value, ws.items[ws.value]].postln;
			})
		;
		specsActions = [
			{ specsList.visible_(false); editor.visible_(true) },
		];
		specsList.items_(specsList.items.add("customize ..."));
		Spec.specs.keysValuesDo({ |k, v|
			if(v.isKindOf(ControlSpec), {
				specsList.items_(specsList.items.add(k++":"+v));
				if(hilo.notNil, {
					specsActions = specsActions.add({ 
						widget.spec_(v, hilo);
						window.close;
					})
				}, {
					specsActions = specsActions.add({ 
						widget.spec_(v);
						window.close;
					})
				})
			})
		});
		editor = TextView(window, Rect(0, 0, window.bounds.width, window.bounds.height))
			.resize_(5)
		;
		if(hilo.notNil, {
			cvString = widget.spec(hilo).asString.split($ );
		}, {
			cvString = widget.spec.asString.split($ );
		});
		cvString = cvString[1..cvString.size-1].join(" ");
		editor
			.string_("// edit and hit <enter>\n// the string must represent a valid ControlSpec\n// e.g. \\freq.asSpec\n// or [20, 400].asSpec\n// or ControlSpec(23, 653, \\exp, 1.0, 312, \" hz\")\n// have a look at the ControlSpec-helpfile ...\n\n"++cvString)
			.visible_(false)
			.syntaxColorize
			.enterInterpretsSelection_(false)
			.keyDownAction_({ |view, char, modifiers, unicode, keycode|
				if(unicode == 3, {
					("result:"+view.string.interpret).postln;
					if(hilo.isNil, {
						widget.spec_(view.string.interpret);
						block { |break|
							#[\pan, \boostcut, \bipolar, \detune].do({ |symbol| 
								if(widget.spec == symbol.asSpec, { 
									break.value(this.knob.centered_(true));
								}, {
									widget.knob.centered_(false);
								})			
							})
						}
					}, {
						widget.spec_(view.string.interpret, hilo);
					});
					window.close;
				});
				if(unicode == 13 or: { 
					unicode == 32 or: {
						unicode == 3 or: {
							unicode == 46
						}
					}
				}, {
					view.syntaxColorize;
				})
			})
		;
		window.front;
	}
	
}

CVWidgetKnob : CVWidget {

	var thisCV;
	var <>knob, <>numVal, <>specBut, <>midiHead, <>midiLearn, <>midiSrc, <>midiChan, <>midiCtrl;
	var <>cc, spec;

	*new { |parent, cv, name, bounds, setUpArgs|
		^super.new.init(parent, cv, name, bounds.left@bounds.top, bounds.width, bounds.height, setUpArgs)
	}
	
	init { |parentView, cv, name, xy, widgetwidth=52, widgetheight=120, setUpArgs|
		var knobsize, meanVal, widgetSpecsActions, editor, cvString;
		var tmpSetup, thisToggleColor/*, oneShot*/;
		
		thisCV = cv;
//		("widget"+name.asString+"initialized with setup:"+[this.setup, setUpArgs]).postln;
		setUpArgs.isKindOf(Array).not.if { setUpArgs = [setUpArgs] };
		
		setUpArgs[0] !? { this.midimode_(setUpArgs[0]) };
		setUpArgs[1] !? { this.midiresolution_(setUpArgs[1]) };
		setUpArgs[2] !? { this.midimean_(setUpArgs[2]) };
		setUpArgs[3] !? { this.midistring_(setUpArgs[3].asString) };
		setUpArgs[4] !? { this.ctrlButtonBank_(setUpArgs[4]) };
		setUpArgs[5] !? { this.softWithin_(setUpArgs[5]) };
				
		knobsize = widgetwidth-14;
		
		this.widgetBg = UserView(parentView, Rect(xy.x, xy.y, widgetwidth, widgetheight))
			.focusColor_(Color(alpha: 1.0))
			.background_(Color.white)
		;
		this.label = Button(parentView, Rect(xy.x+1, xy.y+1, widgetwidth-2, 15))
			.states_([
				[""+name.asString, Color.white, Color.blue],
				[""+name.asString, Color.black, Color.yellow],
			])
			.font_(Font("Helvetica", 9))
			.action_({ |b|
				this.toggleComment(b.value);
			})
		;
		this.nameField = TextField(parentView, Rect(this.label.bounds.left, this.label.bounds.top+this.label.bounds.height, widgetwidth-2, widgetheight-this.label.bounds.height-2))
			.background_(Color.white)
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.value_(name.asString)
			.action_({ |nf| nf.value_(nf.value) })
			.visible_(false)
		;
		this.knob = Knob(parentView, Rect(xy.x+(widgetwidth/2-(knobsize/2)), xy.y+16, knobsize, knobsize))
			.canFocus_(false)
		;
		block { |break|
			#[\pan, \boostcut, \bipolar, \detune].do({ |symbol| 
				if(cv.spec == symbol.asSpec, { break.value(this.knob.centered_(true)) });
			})
		};
		this.numVal = NumberBox(parentView, Rect(xy.x+1, xy.y+knobsize+12, widgetwidth-2, 15))
			.value_(cv.value)
		;
		this.specBut = Button(parentView, Rect(xy.x+1, xy.y+knobsize+27, widgetwidth-2, 15))
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.states_([["edit Spec", Color.black, Color(241/255, 209/255, 0)]])
			.action_({ |btn|
				CVWidget.editorWindow_(this, name);
			})
		;
		this.midiHead = Button(parentView, Rect(xy.x+1, xy.y+knobsize+43, widgetwidth-17, 15))
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.states_([["MIDI", Color.black, Color(alpha: 0)]])
			.action_({ |ms| })
		;
		this.midiLearn = Button(parentView, Rect(xy.x+widgetwidth-16, xy.y+knobsize+43, 15, 15))
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.states_([
				["L", Color.white, Color.blue],
				["X", Color.white, Color.red]
			])
		;
		this.midiSrc = TextField(parentView, Rect(xy.x+1, xy.y+knobsize+58, widgetwidth-2, 12))
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.string_("source")
			.canFocus_(false)
			.background_(Color(alpha: 0))
			.stringColor_(Color.black)
		;
		this.midiChan = TextField(parentView, Rect(xy.x+1, xy.y+knobsize+70, widgetwidth-2/2, 12))
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.string_("chan")
			.canFocus_(false)
			.background_(Color(alpha: 0))
			.stringColor_(Color.black)
		;
		this.midiCtrl = TextField(parentView, Rect(xy.x+(widgetwidth-2/2)+1, xy.y+knobsize+70, widgetwidth-2/2, 12))
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.string_("ctrl")
			.canFocus_(false)
			.background_(Color(alpha: 0))
			.stringColor_(Color.black)
		;

		this.prCCResponderAdd(cv, this.midiLearn, this.midiSrc, this.midiChan, this.midiCtrl, this.midiHead);
		
		[this.knob, this.numVal].do({ |view| cv.connect(view) });
		visibleGuiEls = [this.knob, this.numVal, this.specBut, this.midiHead, this.midiLearn, this.midiSrc, this.midiChan, this.midiCtrl];
		allGuiEls = [this.widgetBg, this.label, this.nameField, this.knob, this.numVal, this.specBut, this.midiHead, this.midiLearn, this.midiSrc, this.midiChan, this.midiCtrl]
	}

	spec_ { |spec|
		if(spec.isKindOf(ControlSpec), {
			thisCV.spec_(spec);
			block { |break|
				#[\pan, \boostcut, \bipolar, \detune].do({ |symbol| 
					if(thisCV.spec == symbol.asSpec, { 
						break.value(this.knob.centered_(true));
					}, {
						this.knob.centered_(false);
					})			
				})
			};
		}, {
			Error("Please provide a valid ControlSpec!").throw;
		})
	}
	
	spec {
		^thisCV.spec;
	}
	
}

CVWidget2D : CVWidget {
	var thisCV, midiLearnActions;
	var <>slider2d, <>rangeSlider;
	var <>numValHi, <>numValLo, <>specButHi, <>specButLo;
	var <>midiHeadLo, <>midiLearnLo, <>midiSrcLo, <>midiChanLo, <>midiCtrlLo;
	var <>midiHeadHi, <>midiLearnHi, <>midiSrcHi, <>midiChanHi, <>midiCtrlHi;
	var <>ccLo, <>ccHi, specLo, specHi;

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

		this.widgetBg = UserView(parentView, Rect(xy.x, xy.y, widgetwidth, widgetheight))
			.focusColor_(Color(alpha: 1.0))
			.background_(Color.white)
		;
		this.label= Button(parentView, Rect(xy.x+1, xy.y+1, widgetwidth-2, 15))
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
		nextY = this.label.bounds.top+this.label.bounds.height;
		this.nameField = TextField(parentView, Rect(xy.x+1, nextY, widgetwidth-2, widgetheight-this.label.bounds.height-2))
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
		
		nextY = xy.y+1+this.label.bounds.height;

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

		allGuiEls = [this.widgetBg, this.label, this.nameField, this.slider2d, this.rangeSlider, this.numValHi, this.numValLo, this.specButHi, this.specButLo, this.midiHeadHi, this.midiHeadLo, this.midiLearnHi, this.midiLearnLo, this.midiSrcHi, this.midiSrcLo, this.midiChanHi, this.midiChanLo, this.midiCtrlHi, this.midiCtrlLo]
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
	
}