/* new branch: 'midi_refactoring' - midi-connections should be handled analoguesly to OSC-handling - within a MVC-logic. Moreover, there shouldn't be a new CCResponder created for each new connection, rather this should be handled within the CCResponder's function (to be investigated in depth. */

CVWidgetKnob : CVWidget {

	var thisCV, <cc, <midisrc, <midichan, <midinum /* keep this for now but that might change ... */;
	var <window, <knob, <numVal, <specBut, <midiHead, <midiLearn, <midiSrc, <midiChan, <midiCtrl;
	var <oscEditBut, <calibBut, <editor;
	var prSpec, prOSCMapping = \linlin, prCalibConstraints, oscResponder;
	var mapConstrainterLo, mapConstrainterHi;

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
		var flow, thisname, thisXY, thisWidth, thisHeight, knobsize, widgetSpecsActions, cvString;
		var msrc = "source", mchan = "chan", mctrl = "ctrl", margs;
		var nextY, knobX, knobY;
		var tmpSetup, tmpMapping;
		var makeCCResponder, ccResponder;
		var ctrlString, meanVal;
		
		if(name.isNil, { thisname = "knob" }, { thisname = name });
		
		if(cv.isNil, {
			thisCV = CV.new;
		}, {
			thisCV = cv;
		});
				
		setUpArgs.isKindOf(Array).not.if { setUpArgs = [setUpArgs] };
		
		setUpArgs[0] !? { this.midimode_(setUpArgs[0]) };
		setUpArgs[1] !? { this.midiresolution_(setUpArgs[1]) };
		setUpArgs[2] !? { this.midimean_(setUpArgs[2]) };
		setUpArgs[3] !? { this.midistring_(setUpArgs[3].asString) };
		setUpArgs[4] !? { this.ctrlButtonBank_(setUpArgs[4]) };
		setUpArgs[5] !? { this.softWithin_(setUpArgs[5]) };
		setUpArgs[6] !? { this.calibrate_(setUpArgs[6]) };
		
		action !? { thisCV.action_(action) };
		
		if(wdgtControllersAndModels.notNil, {
			wdgtControllersAndModels = controllersAndModels;
		}, {
			wdgtControllersAndModels = ();
		});
		
		wdgtControllersAndModels.calibration ?? {
			wdgtControllersAndModels.calibration = ();
		};
		wdgtControllersAndModels.calibration.model ?? {
			wdgtControllersAndModels.calibration.model = Ref(prCalibrate);
		};
		wdgtControllersAndModels.cvSpec ?? {
			wdgtControllersAndModels.cvSpec = ();
		};
		wdgtControllersAndModels.cvSpec.model ?? { 
			wdgtControllersAndModels.cvSpec.model = Ref(this.spec);
		};
		wdgtControllersAndModels.oscInputRange ?? {
			wdgtControllersAndModels.oscInputRange = ();
		};
		wdgtControllersAndModels.oscInputRange.model ?? {
			wdgtControllersAndModels.oscInputRange.model = Ref([0.0001, 0.0001]);
		};
		wdgtControllersAndModels.oscConnection ?? {
			wdgtControllersAndModels.oscConnection = ();
		};
		wdgtControllersAndModels.oscConnection.model ?? {
			wdgtControllersAndModels.oscConnection.model = Ref(false);
		};
		wdgtControllersAndModels.midiConnection ?? {
			wdgtControllersAndModels.midiConnection = ();
		};
		wdgtControllersAndModels.midiConnection.model ?? {
			wdgtControllersAndModels.midiConnection.model = Ref(nil);
		};
		wdgtControllersAndModels.midiDisplay ?? {
			wdgtControllersAndModels.midiDisplay = ();
		};
		wdgtControllersAndModels.midiDisplay.model ?? {
			wdgtControllersAndModels.midiDisplay.model = Ref((src: "source", chan: "chan", ctrl: "ctrl", learn: "L"));
		};	

		mapConstrainterLo ?? { 
			mapConstrainterLo = CV([-inf, inf].asSpec, wdgtControllersAndModels.oscInputRange.model.value[0]);
		};
		mapConstrainterHi ?? { 
			mapConstrainterHi = CV([-inf, inf].asSpec, wdgtControllersAndModels.oscInputRange.model.value[1]);
		};
		
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
			window = Window(thisname, Rect(50, 50, thisWidth+14, thisHeight+7), server: server);
		}, {
			window = parentView;
		});
				
		cvcGui ?? { 
			window.onClose_({
				if(editor.notNil, {
					if(editor.isClosed.not, {
						editor.close;
					}, {
						if(CVWidgetEditor.allEditors.notNil and:{
							CVWidgetEditor.allEditors[thisname.asSymbol].notNil;
						}, {
							CVWidgetEditor.allEditors.removeAt(thisname.asSymbol)
						})
					})
				});
				oscResponder !? { oscResponder.remove };
				wdgtControllersAndModels.do({ |mc| mc.controller.remove });
			})
		};
						
		widgetBg = UserView(window, Rect(thisXY.x, thisXY.y, thisWidth, thisHeight))
			.focusColor_(Color(alpha: 1.0))
			.background_(Color.white)
		;
		label = Button(window, Rect(thisXY.x+1, thisXY.y+1, thisWidth-2, 15))
			.states_([
				[""+thisname.asString, Color.white, Color.blue],
				[""+thisname.asString, Color.black, Color.yellow],
			])
			.font_(Font("Helvetica", 9))
			.action_({ |b|
				this.toggleComment(b.value);
			})
		;
		nameField = TextField(window, Rect(label.bounds.left, label.bounds.top+label.bounds.height, thisWidth-2, thisHeight-label.bounds.height-2))
			.background_(Color.white)
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.value_(thisname.asString)
			.action_({ |nf| nf.value_(nf.value) })
			.visible_(false)
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
				if(thisCV.spec == symbol.asSpec, { break.value(knob.centered_(true)) });
			})
		};
		nextY = thisXY.y+thisHeight-117;
		numVal = NumberBox(window, Rect(thisXY.x+1, nextY, thisWidth-2, 15))
			.value_(thisCV.value)
		;
		nextY = nextY+numVal.bounds.height;
		specBut = Button(window, Rect(thisXY.x+1, nextY, thisWidth-2, 15))
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.states_([["edit Spec", Color.black, Color(241/255, 209/255, 0)]])
			.action_({ |btn|
				if(editor.isNil or:{ editor.isClosed }, {
					editor = CVWidgetEditor(this, thisname, 0);
				}, {
					editor.front(0)
				});
				wdgtControllersAndModels.oscConnection.model.value_(
					wdgtControllersAndModels.oscConnection.model.value
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
					editor = CVWidgetEditor(this, thisname, 1);
				}, {
					editor.front(1)
				});
				wdgtControllersAndModels.oscConnection.model.value_(
					wdgtControllersAndModels.oscConnection.model.value;
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
			.background_(Color(alpha: 0))
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
		;
		nextY = nextY+midiSrc.bounds.height;
		midiChan = TextField(window, Rect(thisXY.x+1, nextY, thisWidth-2/2, 12))
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.string_(mchan)
			.background_(Color(alpha: 0))
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
		;
		midiCtrl = TextField(window, Rect(thisXY.x+(thisWidth-2/2)+1, nextY, thisWidth-2/2, 12))
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.string_(mctrl)
			.background_(Color(alpha: 0))
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
					editor = CVWidgetEditor(this, thisname, 2);
				}, {
					editor.front(2)
				});
				editor.calibNumBoxes !? {
					mapConstrainterLo.connect(editor.calibNumBoxes.lo);
					mapConstrainterHi.connect(editor.calibNumBoxes.hi);
				};
				wdgtControllersAndModels.oscConnection.model.value_(
					wdgtControllersAndModels.oscConnection.model.value;
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
		;
		
		wdgtControllersAndModels.calibration.controller ?? { 
			wdgtControllersAndModels.calibration.controller = SimpleController(wdgtControllersAndModels.calibration.model);
		};

		wdgtControllersAndModels.calibration.controller.put(\value, { |theChanger, what, moreArgs|
			prCalibrate = (theChanger.value);
			theChanger.value.switch(
				true, { 
					calibBut.value_(0);
					if(editor.notNil and:{ editor.isClosed.not }, {
						editor.calibBut.value_(0);
						mapConstrainterLo ?? { 
							mapConstrainterLo = CV([-inf, inf].asSpec, 0.00001);
							mapConstrainterLo.connect(editor.calibNumBoxes.lo);
						};
						mapConstrainterHi ?? { 
							mapConstrainterHi = CV([-inf, inf].asSpec, 0.00001);
							mapConstrainterHi.connect(editor.calibNumBoxes.hi);
						};
						[editor.calibNumBoxes.lo, editor.calibNumBoxes.hi].do({ |nb| 
							nb.enabled_(false);
							nb.action_(nil);
						})
					})
				},
				false, { 
					calibBut.value_(1);
					if(editor.notNil and:{ editor.isClosed.not }, {
						editor.calibBut.value_(1);
						[mapConstrainterLo, mapConstrainterHi].do({ |cv| cv = nil; });
						[editor.calibNumBoxes.lo, editor.calibNumBoxes.hi].do({ |nb| 
							nb.enabled_(true);
							nb.action_({ |b| 
								this.oscInputConstraints_(
									editor.calibNumBoxes.lo.value@editor.calibNumBoxes.hi.value;
								) 
							})
						})
					})
				}
			)
		});

		calibBut.action_({ |cb|
			cb.value.switch(
				0, { wdgtControllersAndModels.calibration.model.value_(true).changed(\value) },
				1, { wdgtControllersAndModels.calibration.model.value_(false).changed(\value) }
			)
		});
		
		wdgtControllersAndModels.cvSpec.controller ?? {
			wdgtControllersAndModels.cvSpec.controller = SimpleController(wdgtControllersAndModels.cvSpec.model);
		};
		
		wdgtControllersAndModels.cvSpec.controller.put(\value, { |theChanger, what, moreArgs|
			if(theChanger.value.minval <= 0.0 or:{
				theChanger.value.maxval <= 0.0
			}, {
				if(prOSCMapping === \linexp or:{
					prOSCMapping === \expexp
				}, {
					prOSCMapping = \linlin;
					if(editor.notNil and:{
						editor.isClosed.not
					}, {
						editor.mappingSelect.value_(0);
					})
				})
			}, {
				if(editor.notNil and:{
					editor.isClosed.not	
				}, {
					tmpMapping = editor.mappingSelect.item;
					editor.mappingSelect.items.do({ |item, i|
						if(item == tmpMapping, {
							editor.mappingSelect.value_(i)
						})
					})
				})
			});
			thisCV.spec_(theChanger.value);
			block { |break|
				#[\pan, \boostcut, \bipolar, \detune].do({ |symbol| 
					if(thisCV.spec == symbol.asSpec, { 
						break.value(knob.centered_(true));
					}, {
						knob.centered_(false);
					})			
				})
			}
		});
		
		wdgtControllersAndModels.oscInputRange.controller ?? {
			wdgtControllersAndModels.oscInputRange.controller = SimpleController(wdgtControllersAndModels.oscInputRange.model);
		};

		wdgtControllersAndModels.oscInputRange.controller.put(\value, { |theChanger, what, moreArgs|
			if(theChanger.value[0] <= 0 or:{
				theChanger.value[1] <= 0
			}, {
				if(prOSCMapping === \explin or:{
					prOSCMapping === \expexp
				}, {
					prOSCMapping = \linlin;
				});
				if(editor.notNil and:{
					editor.isClosed.not
				}, {
					{	
						oscEditBut.states_([[
							oscEditBut.states[0][0].split($\n)[0]++"\n"++prOSCMapping.asString,
							oscEditBut.states[0][1],
							oscEditBut.states[0][2]
						]]);
						oscEditBut.refresh;
						editor.mappingSelect.items.do({ |item, i|
							if(item.asSymbol === prOSCMapping, {
								editor.mappingSelect.value_(i);
							})
						})
					}.defer
				})		
			}, {
				if(editor.notNil and:{
					editor.isClosed.not	
				}, {
					{
						editor.mappingSelect.items.do({ |item, i|
							if(item.asSymbol === prOSCMapping, {
								editor.mappingSelect.value_(i)
							})
						});
					}.defer;
					if(oscEditBut.states[0][0].split($\n)[0] != "edit OSC", {
						{
							oscEditBut.states_([[
								oscEditBut.states[0][0].split($\n)[0]++"\n"++prOSCMapping.asString,
								oscEditBut.states[0][1],
								oscEditBut.states[0][2]
							]]);
							oscEditBut.refresh;
						}.defer
					})
				})
			})
		});
		
		wdgtControllersAndModels.midiConnection.controller ?? {
			wdgtControllersAndModels.midiConnection.controller = SimpleController(wdgtControllersAndModels.midiConnection.model);
		};
		
		wdgtControllersAndModels.midiConnection.controller.put(\value, { |theChanger, what, moreArgs|
			if(theChanger.value.isKindOf(Event), {
				makeCCResponder = { |argSrc, argChan, argNum|
					CCResponder({ |src, chan, num, val|
						ctrlString ? ctrlString = num+1;
						if(this.ctrlButtonBank.notNil, {
							if(ctrlString%this.ctrlButtonBank == 0, {
								ctrlString = this.ctrlButtonBank.asString;
							}, {
								ctrlString = (ctrlString%this.ctrlButtonBank).asString;
							});
							ctrlString = ((num+1/this.ctrlButtonBank).ceil).asString++":"++ctrlString;
						}, {
							ctrlString = num+1;
						});
	
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
						src !? { midisrc = src };
						chan !? { midichan = chan };
						num !? { midinum = ctrlString };
					}, argSrc, argChan, argNum, nil);
				};
				
				fork {
					block { |break|
						loop {
							0.01.wait;
							if(midisrc.notNil and:{
								midichan.notNil and:{
									midinum.notNil;
								}
							}, {
								break.value(
									wdgtControllersAndModels.midiDisplay.model.value_(
										(src: midisrc, chan: midichan, ctrl: midinum, learn: "X")
									).changed(\value)
								)
							})
						}
					}
				};

				if(theChanger.value.isEmpty, {
					cc = makeCCResponder.().learn;
				}, {
					cc = makeCCResponder.(theChanger.value.src, theChanger.value.chan, theChanger.value.num);
				})
			}, {
				cc.remove;
				cc = nil;
				wdgtControllersAndModels.midiDisplay.model.value_(
					(src: "source", chan: "chan", ctrl: "ctrl", learn: "L")
				).changed(\value);
				midisrc = nil; midichan = nil; midinum = nil;
			})
		});
		
		wdgtControllersAndModels.midiDisplay.controller ?? {
			wdgtControllersAndModels.midiDisplay.controller = SimpleController(wdgtControllersAndModels.midiDisplay.model);
		};
		
		wdgtControllersAndModels.midiDisplay.controller.put(\value, { |theChanger, what, moreArgs|
			theChanger.value.learn.switch(
				"X", {
					defer {
						midiSrc.string_(theChanger.value.src.asString)
							.background_(Color.red)
							.stringColor_(Color.white)
							.canFocus_(false)
						;
						midiChan.string_((theChanger.value.chan+1).asString)
							.background_(Color.red)
							.stringColor_(Color.white)
							.canFocus_(false)
						;
						midiCtrl.string_(theChanger.value.ctrl)
							.background_(Color.red)
							.stringColor_(Color.white)
							.canFocus_(false)
						;
						midiLearn.value_(1)
					}
				},
				"C", {
					midiLearn.states_([
						["C", Color.white, Color(0.11468057974842, 0.38146154367376, 0.19677815686724)],
						["X", Color.white, Color.red]
					]).refresh;
				},
				"L", {
					defer {
						midiSrc.string_(theChanger.value.src)
							.background_(Color.white)
							.stringColor_(Color.black)
							.canFocus_(true)
						;
						midiChan.string_(theChanger.value.chan)
							.background_(Color.white)
							.stringColor_(Color.black)
							.canFocus_(true)
						;
						midiCtrl.string_(theChanger.value.ctrl)
							.background_(Color.white)
							.stringColor_(Color.black)
							.canFocus_(true)
						;
						midiLearn.states_([
							["L", Color.white, Color.blue],
							["X", Color.white, Color.red]
						])
						.value_(0).refresh;
					}
				}
			)
		});

		wdgtControllersAndModels.oscConnection.controller ?? {
			wdgtControllersAndModels.oscConnection.controller = SimpleController(wdgtControllersAndModels.oscConnection.model);
		};

		wdgtControllersAndModels.oscConnection.controller.put(\value, { |theChanger, what, moreArgs|
			if(theChanger.value.size == 2, {
				oscResponder = OSCresponderNode(nil, theChanger.value[0].asSymbol, { |t, r, msg|
					if(prCalibrate, { 
						if(prCalibConstraints.isNil, {
							prCalibConstraints = (lo: msg[theChanger.value[1]], hi: msg[theChanger.value[1]]);
						}, {
							if(msg[theChanger.value[1]] < prCalibConstraints.lo, { 
								prCalibConstraints.lo = msg[theChanger.value[1]];
								wdgtControllersAndModels.oscInputRange.model.value_([
									msg[theChanger.value[1]], 
									wdgtControllersAndModels.oscInputRange.model.value[1]
								]).changed(\value);
							});
							if(msg[theChanger.value[1]] > prCalibConstraints.hi, {
								prCalibConstraints.hi = msg[theChanger.value[1]];
								wdgtControllersAndModels.oscInputRange.model.value_([
									wdgtControllersAndModels.oscInputRange.model.value[0], 
									msg[theChanger.value[1]]
								]).changed(\value);
							});
						});
						mapConstrainterLo.value_(prCalibConstraints.lo);
						mapConstrainterHi.value_(prCalibConstraints.hi);
					}, {
						if(prCalibConstraints.isNil, {
							prCalibConstraints = (lo: wdgtControllersAndModels.oscInputRange.model.value[0], hi: wdgtControllersAndModels.oscInputRange.model.value[1]);
						})
					});
					thisCV.value_(
						msg[theChanger.value[1]].perform(
							prOSCMapping,
							prCalibConstraints.lo, prCalibConstraints.hi,
							this.spec.minval, this.spec.maxval,
							\minmax
						)
					)
				}).add;
				oscEditBut.states_([
					[theChanger.value[0].asString++"["++theChanger.value[1].asString++"]"++"\n"++prOSCMapping.asString, Color.white, Color.cyan(0.5)]
				]);
				if(editor.notNil and:{
					editor.isClosed.not
				}, {
					editor.connectorBut.value_(0);
					editor.nameField.enabled_(false).string_(theChanger.value[0].asString);
					if(prCalibrate, {
						[editor.inputConstraintLoField, editor.inputConstraintHiField].do(_.enabled_(false));
					});
					editor.indexField.value_(theChanger.value[1]).enabled_(false);
					editor.connectorBut.value_(1);
				});
				oscEditBut.refresh;
			});
			if(theChanger.value == false, {
				oscResponder.remove;
				oscEditBut.states_([
					["edit OSC", Color.black, Color.clear]
				]);
				wdgtControllersAndModels.oscInputRange.model.value_([0.0001, 0.0001]).changed(\value);
				prCalibConstraints = nil;
				if(editor.notNil and:{
					editor.isClosed.not
				}, {
					editor.connectorBut.value_(0);
					editor.nameField.enabled_(true);
					editor.inputConstraintLoField.value_(
						wdgtControllersAndModels.oscInputRange.model.value[0];
					);
					editor.inputConstraintHiField.value_(
						wdgtControllersAndModels.oscInputRange.model.value[1];
					);
					if(prCalibrate.not, {
						[editor.inputConstraintLoField, editor.inputConstraintHiField].do(_.enabled_(true));
					});
					editor.indexField.enabled_(true);
					editor.connectorBut.value_(0);
				});
				oscEditBut.refresh;
			})
		});
		
//		this.prCCResponderAdd(thisCV, midiLearn, midiSrc, midiChan, midiCtrl, midiHead);
		
		[knob, numVal].do({ |view| thisCV.connect(view) });
		visibleGuiEls = [knob, numVal, specBut, midiHead, midiLearn, midiSrc, midiChan, midiCtrl, oscEditBut, calibBut];
		allGuiEls = [widgetBg, label, nameField, knob, numVal, specBut, midiHead, midiLearn, midiSrc, midiChan, midiCtrl, oscEditBut, calibBut]
	}
	
	calibrate_ { |bool|
		if(bool.isKindOf(Boolean).not, {
			Error("calibration can only be set to true or false!").throw;
		});
		wdgtControllersAndModels.calibration.model.value_(bool).changed(\value);
	}
	
	calibrate {
		^prCalibrate;
	}
	
	spec_ { |spec|
		if(prSpec.isKindOf(ControlSpec).not, {
			Error("Please provide a valid spec! (its class must inherit from ControlSpec)").throw;
		});
		wdgtControllersAndModels.cvSpec.model.value_(prSpec).changed(\value);
	}
	
	spec {
		^thisCV.spec;
	}
	
	oscMapping_ { |mapping|
		if(mapping.asSymbol !== \linlin and:{
			mapping.asSymbol !== \linexp and:{
				mapping.asSymbol !== \explin and:{
					mapping.asSymbol !== \expexp
				}
			}
		}, {
			Error("A valid mapping can either be \\linlin, \\linexp, \\explin or \\expexp").throw;
		}, {
			prOSCMapping = mapping.asSymbol;
			wdgtControllersAndModels.oscInputRange.model.value_(
				wdgtControllersAndModels.oscInputRange.model.value;
			).changed(\value);
			wdgtControllersAndModels.cvSpec.model.value_(
				wdgtControllersAndModels.cvSpec.model.value;
			).changed(\value);
		})
	}
	
	oscMapping {
		^prOSCMapping;
	}
	
	oscInputConstraints_ { |constraintsHiLo|
		if(constraintsHiLo.isKindOf(Point).not, {
			Error("setOSCInputConstraints expects a Point in the form of lo@hi").throw;
		}, {
			this.calibrate_(false);
			prCalibConstraints = (lo: constraintsHiLo.x, hi: constraintsHiLo.y);
			if(editor.notNil and:{ editor.isClosed.not }, {
				mapConstrainterLo.value_(constraintsHiLo.x);
				mapConstrainterHi.value_(constraintsHiLo.y);
			})
		})
	}
	
	oscInputConstraints {
		^[prCalibConstraints.lo, prCalibConstraints.hi];
	}
	
	oscConnect { |name, oscMsgIndex|
		if("^\/".matchRegexp(name.asString).not, {
			Error("You have to supply a valid OSC-typetag, beginning with an \"/\" as second argument to oscConnect").throw;
		});
		if(oscMsgIndex.isKindOf(Integer).not, {
			Error("You have to supply an integer as third argument to oscConnect").throw;
		});
		wdgtControllersAndModels.oscConnection.model.value_([name, oscMsgIndex]).changed(\value);
		CmdPeriod.add({ this.oscDisconnect });
	}
	
	oscDisconnect {
		wdgtControllersAndModels.oscConnection.model.value_(false).changed(\value);
		wdgtControllersAndModels.oscInputRange.model.value_([0.00001, 0.00001]).changed(\value);
	}
	
	// if all arguments are nil .learn should be triggered
	midiConnect { |uid, chan, num|
		var args;
		if(this.cc.isNil, {
			args = [uid, chan, num].select({ |param| param.notNil }).collect({ |param| param.asInt });
			wdgtControllersAndModels.midiConnection.model.value_(
				(src: uid, chan: chan, num: num)
			).changed(\value);
		}, {
			"Already connected!".warn;	
		})
	}
	
	midiDisconnect { 
		this.cc !? wdgtControllersAndModels.midiConnection.model.value_(nil).changed(\value);
	}
	
	front {
		window.front;
	}
	
}
