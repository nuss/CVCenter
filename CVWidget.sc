
CVWidget {

	classvar <editorWindow, <window;

	var <>midiSetUp, thisCV;
	var <>widgetBg, <>label, <>nameField, <>knob, <>numVal, <>specBut, <>midiHead, <>midiLearn, <>midiSrc, <>midiChan, <>midiCtrl, <>editor;
	var <>cc, <>softWithin = 0.1, spec;
	var <visible;
	var widgetXY, widgetProps;

	*new { |parent, cv, name, xy, widgetwidth=52, widgetheight=137, setup|
		^super.new.init(parent, cv, name, xy, widgetwidth, widgetheight, setup)
	}
	
	init { |parentView, cv, name, xy, widgetwidth, widgetheight, setup|
		var knobsize, meanVal, widgetSpecsActions, editor, cvString;
		
		thisCV = cv;
		
		setup ?? { this.midiSetUp = [0] };
		setup !? { this.midiSetUp = [setup].flat };
		
		knobsize = widgetwidth-10;
		
		this.widgetBg = UserView(parentView, Rect(xy.x, xy.y, widgetwidth, widgetheight))
			.focusColor_(Color(alpha: 0))
			.background_(Color.white)
		;
		this.label = StaticText(parentView, Rect(xy.x+1, xy.y+1, widgetwidth-2, 12))
			.background_(Color(green:(255.div(4)/255)))
			.stringColor_(Color.white)
			.string_(""+name.asString)
			.font_(Font("Helvetica", 9))
		;
		this.nameField = TextField(parentView, Rect(xy.x+1, xy.y+13, widgetwidth-2, 12))
			.background_(Color.white)
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.value_(name.asString)
			.action_({ |nf| nf.value_(nf.value) })
		;
		this.knob = Knob(parentView, Rect(xy.x+(widgetwidth/2-(knobsize/2)), xy.y+27, knobsize, knobsize))
			.canFocus_(false)
		;
		block { |break|
			[\pan, \boostcut, \bipolar, \detune].do({ |symbol| 
				if(cv.spec == symbol.asSpec, { break.value(this.knob.centered_(true)) });
			})
		};
		this.numVal = NumberBox(parentView, Rect(xy.x+1, xy.y+knobsize+23, widgetwidth-2, 15))
			.value_(cv.value)
			.action_({ |nv| cv.value_(nv.value) })
		;
		this.specBut = Button(parentView, Rect(xy.x+1, xy.y+knobsize+38, widgetwidth-2, 15))
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.states_([["edit Spec", Color.black, Color(241/255, 209/255, 0)]])
			.action_({ |btn|
				CVWidget.editorWindow_(this, name);
			})
		;
		this.midiHead = Button(parentView, Rect(xy.x+1, xy.y+knobsize+54, widgetwidth-17, 15))
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.states_([["MIDI", Color.black, Color(alpha: 0)]])
			.action_({ |ms| })
		;
		this.midiLearn = Button(parentView, Rect(xy.x+widgetwidth-16, xy.y+knobsize+54, 15, 15))
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
								this.midiSetUp.isArray.not.if{this.midiSetUp_(this.midiSetUp.asArray.flat)};
								this.midiSetUp[0].switch(
									0, { 
										if(val/127 < (cv.input+(softWithin/2)) and: { 
											val/127 > (cv.input-(softWithin/2));										}, {
											cv.input_(val/127);
										})
									},
									1, { 
										if(this.midiSetUp[1].isNil, { 
											meanVal = 64;
										}, { 
											meanVal = this.midiSetUp[1];
										});
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
										this.midiCtrl.string_((ctrl+1).asString)
											.background_(Color.red)
											.stringColor_(Color.white)
										;
										this.midiHead.enabled_(false);
									}
								}.defer;
							});
							this.cc.learn;
							nil.yield;
						}, {
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
		this.midiSrc = TextField(parentView, Rect(xy.x+1, xy.y+knobsize+69, widgetwidth-2, 12))
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.string_("source")
			.canFocus_(false)
			.background_(Color(alpha: 0))
			.stringColor_(Color.black)
		;
		this.midiChan = TextField(parentView, Rect(xy.x+1, xy.y+knobsize+81, widgetwidth-2/2, 12))
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.string_("chan")
			.canFocus_(false)
			.background_(Color(alpha: 0))
			.stringColor_(Color.black)
		;
		this.midiCtrl = TextField(parentView, Rect(xy.x+(widgetwidth-2/2)+1, xy.y+knobsize+81, widgetwidth-2/2, 12))
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.string_("ctrl")
			.canFocus_(false)
			.background_(Color(alpha: 0))
			.stringColor_(Color.black)
		;
		
		[this.knob, this.numVal].do({ |view| cv.connect(view) });
	}
	
	visible_ { |visible|
		if(visible.isKindOf(Boolean).not, {
			^nil;
		}, {
			if(visible, {
				[this.widgetBg, this.label, this.nameField, this.knob, this.numVal, this.specBut, this.midiHead, this.midiLearn, this.midiSrc, this.midiChan, this.midiCtrl].do(_.visible_(true));
			}, {
				[this.widgetBg, this.label, this.nameField, this.knob, this.numVal, this.specBut, this.midiHead, this.midiLearn, this.midiSrc, this.midiChan, this.midiCtrl].do(_.visible_(false));
			})
		})
	}
	
	spec_ { |spec|
		if(spec.isKindOf(ControlSpec), {
			thisCV.spec_(spec);
			block { |break|
				[\pan, \boostcut, \bipolar, \detune].do({ |symbol| 
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
	
	widgetXY_ { |point|
		var originXZero, originYZero;
		originXZero = [this.widgetBg, this.label, this.nameField, this.knob, this.numVal, this.specBut, this.midiHead, this.midiLearn, this.midiSrc, this.midiChan, this.midiCtrl].collect({ |view| view.bounds.left });
		originXZero = originXZero-originXZero.minItem;
		originYZero = [this.widgetBg, this.label, this.nameField, this.knob, this.numVal, this.specBut, this.midiHead, this.midiLearn, this.midiSrc, this.midiChan, this.midiCtrl].collect({ |view| view.bounds.top });
		originYZero = originYZero-originYZero.minItem;
		
		[this.widgetBg, this.label, this.nameField, this.knob, this.numVal, this.specBut, this.midiHead, this.midiLearn, this.midiSrc, this.midiChan, this.midiCtrl].do({ |view, i|
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
		[this.widgetBg, this.label, this.nameField, this.knob, this.numVal, this.specBut, this.midiHead, this.midiLearn, this.midiSrc, this.midiChan, this.midiCtrl].do(_.remove);
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
					[\pan, \boostcut, \bipolar, \detune].do({ |symbol| 
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
		editor.string_("// edit and hit <enter>\n// the string must represent a valid ControlSpec\n// e.g. \\freq.asSpec\n// or [20, 400].asSpec\n// or ControlSpec(23, 653, \\exp, 1.0, 312, \" hz\")\n// have a look at the ControlSpec-helpfile ...\n\n"++cvString)
			.visible_(false)
			.syntaxColorize
			.enterInterpretsSelection_(false)
			.keyDownAction_({ |view, char, modifiers, unicode, keycode|
				if(unicode == 3, {
					("result:"+view.string.interpret).postln;
					widget.spec_(view.string.interpret);
					block { |break|
						[\pan, \boostcut, \bipolar, \detune].do({ |symbol| 
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