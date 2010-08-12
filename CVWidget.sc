
CVWidget {

	classvar <editorWindow, <window;
	var <>midimode = 0, <>midimean = 64, <>midistring = "", <>ctrlButtonBank, <>softWithin = 0.1;
	var visibleGuiEls, allGuiEls;
	var <>nameField, <>widgetBg; // elements contained in any kind CVWidget
	var <visible, widgetXY, widgetProps;

	setup {
		^[this.midimode, this.midimean, this.midistring, this.ctrlButtonBank];
	}
	
	visible_ { |visible|
		if(visible.isKindOf(Boolean).not, {
			^nil;
		}, {
			if(visible, {
				allGuiEls.do(_.visible_(true));
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
	
	remove {
		allGuiEls.do(_.remove);
	}
	
	*editorWindow_ { |widget, widgetName|
		var specsList, specsActions, editor, cvString;
		
		if(Window.allWindows.select({ |w| "^Widget-Spec Editor:".matchRegexp(w.name) == true }).size < 1, {			window = Window("Widget-Spec Editor:"+widgetName, Rect(Window.screenBounds.width/2-150, Window.screenBounds.height/2-100, 330, 200));
		}, {
			window.name_("Widget-Spec Editor:"+widgetName);
		});
		specsList = ListView(window, Rect(0, 0, window.bounds.width, window.bounds.height))
			.resize_(5)
			.background_(Color.white)
			.hiliteColor_(Color.blue)
			.selectedStringColor_(Color.white)
			.visible_(true)
			.enterKeyAction_({ |ws|
				specsActions[ws.value].value;
				block { |break|
					#[\pan, \boostcut, \bipolar, \detune].do({ |symbol| 
						if(widget.spec == symbol.asSpec, { 
							break.value(widget.knob.centered_(true));
						}, {
							widget.knob.centered_(false);
						})			
					})
				};
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
				specsActions = specsActions.add({ 
					widget.spec_(v);
					window.close;
				})
			})
		});
		editor = TextView(window, Rect(0, 0, window.bounds.width, window.bounds.height))
			.resize_(5)
		;
		cvString = widget.spec.asString.split($ );
		cvString = cvString[1..cvString.size-1].join(" ");
		editor
			.string_("// edit and hit <enter>\n// the string must represent a valid ControlSpec\n// e.g. \\freq.asSpec\n// or [20, 400].asSpec\n// or ControlSpec(23, 653, \\exp, 1.0, 312, \" hz\")\n// have a look at the ControlSpec-helpfile ...\n\n"++cvString)
			.visible_(false)
			.syntaxColorize
			.enterInterpretsSelection_(false)
			.keyDownAction_({ |view, char, modifiers, unicode, keycode|
				if(unicode == 3, {
					("result:"+view.string.interpret).postln;
					widget.spec_(view.string.interpret);
					block { |break|
						#[\pan, \boostcut, \bipolar, \detune].do({ |symbol| 
							if(widget.spec == symbol.asSpec, { 
								break.value(this.knob.centered_(true));
							}, {
								widget.knob.centered_(false);
							})			
						})
					};
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
	var <>label, <>knob, <>numVal, <>specBut, <>midiHead, <>midiLearn, <>midiSrc, <>midiChan, <>midiCtrl, <>editor;
	var <>cc, spec;

	*new { |parent, cv, name, bounds, setUpArgs|
		^super.new.init(parent, cv, name, bounds.left@bounds.top, bounds.width, bounds.height, setUpArgs)
	}
	
	init { |parentView, cv, name, xy, widgetwidth=52, widgetheight=120, setUpArgs|
		var knobsize, flow, meanVal, widgetSpecsActions, editor, cvString;
		var tmpSetup, thisToggleColor/*, oneShot*/;
		
		thisCV = cv;
//		("widget"+name.asString+"initialized with setup:"+[this.setup, setUpArgs]).postln;
		setUpArgs.isKindOf(Array).not.if { setUpArgs = [setUpArgs] };
		
		setUpArgs[0] !? { this.midimode_(setUpArgs[0]) };
		setUpArgs[1] !? { this.midimean_(setUpArgs[1]) };
		setUpArgs[2] !? { this.midistring_(setUpArgs[2].asString) };
		setUpArgs[3] !? { this.ctrlButtonBank_(setUpArgs[3]) };
				
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
			.action_({ |nv| cv.value_(nv.value) })
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
			.action_({ |ml|
				{
					loop {
						0.01.wait;
						if(ml.value == 1, {
							this.cc = CCResponder({ |src, chan, ctrl, val|
								var ctrlString;
//								[src, chan, ctrl, val].postln;
																				ctrlString ? ctrlString = ctrl+1;

								if(this.ctrlButtonBank.notNil, {
									if(ctrlString%ctrlButtonBank == 0, {
										ctrlString = ctrlButtonBank.asString;
									}, {
										ctrlString = (ctrlString%ctrlButtonBank).asString;
									});
									ctrlString = ((ctrl+1/ctrlButtonBank).ceil).asString++":"++ctrlString;
								}, {
									ctrlString = ctrl+1;								});								
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
										cv.input_(cv.input+((val-meanVal)/127)) 
									}
								);
								{
									try {
										this.midiSrc.string_(src.asString)
											.background_(Color.red)
											.stringColor_(Color.white)
										;
										this.midiChan.string_((chan+1).asString)
											.background_(Color.red)
											.stringColor_(Color.white)
										;
										this.midiCtrl.string_(ctrlString.asString)
											.background_(Color.red)
											.stringColor_(Color.white)
										;
										this.midiHead.enabled_(false);
									}
								}.defer;
							});
							this.cc.learn;
//							oneShot = this.cc.oneShotLearn;
							nil.yield;
						}, {
//							oneShot !? { oneShot.remove };
							this.cc.remove;
							this.cc = nil;
							this.midiSrc.string_("source")
								.background_(Color(alpha: 0))
								.stringColor_(Color.black)
							;
							this.midiChan.string_("chan")
								.background_(Color(alpha: 0))
								.stringColor_(Color.black)
							;
							this.midiCtrl.string_("ctrl")
								.background_(Color(alpha: 0))
								.stringColor_(Color.black)
							;
							this.midiHead.enabled_(true);
							nil.yield;
						})
					}
				}.fork(AppClock)
			})
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