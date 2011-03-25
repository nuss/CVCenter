
CVWidget {

	var <>midimode = 0, <>midimean = 64, <>midistring = "", <>ctrlButtonBank, <>midiresolution = 1, <>softWithin = 0.1;
	var prCalibrate = true; // OSC-calibration enabled/disabled - private
	var visibleGuiEls, allGuiEls;
	var <widgetBg, <label, <nameField; // elements contained in any kind of CVWidget
	var <visible, widgetXY, widgetProps;
	var <wdgtControllersAndModels;

	setup {
		^[this.midimode, this.midiresolution, this.midimean, this.midistring, this.ctrlButtonBank, this.softwithin, prCalibrate];
	}
	
	visible_ { |visible|
		if(visible.isKindOf(Boolean).not, {
			^nil;
		}, {
			if(visible, {
				allGuiEls.do({ |el| 
					if(el === nameField, {
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
					nameField.visible_(false);
				})
			},
			1, {
				visibleGuiEls.do({ |el|
					el.visible_(false);
					nameField.visible_(true);
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
		^widgetBg.bounds.left@widgetBg.bounds.top;
	}
	
	widgetProps {
		^widgetBg.bounds.width@widgetBg.bounds.height;
	}
	
	bounds {
		^Rect(this.widgetXY.x, this.widgetXY.y, this.widgetProps.x, this.widgetProps.y);
	}
	
	remove {
		allGuiEls.do(_.remove);
	}
	
	initControllersAndModels { |controllersAndModels, key|
		var wcm;
		if(wdgtControllersAndModels.notNil, {
			wdgtControllersAndModels = controllersAndModels;
		}, {
			wdgtControllersAndModels = ();
		});
		
		key !? {
			wdgtControllersAndModels.put(key, ());
		};
		
		wcm = wdgtControllersAndModels;
				
		wcm.calibration ?? {
			wcm.calibration = ();
		};
		wcm.calibration.model ?? {
			wcm.calibration.model = Ref(prCalibrate);
		};
		wcm.cvSpec ?? {
			wcm.cvSpec = ();
		};
		wcm.cvSpec.model ?? { 
			wcm.cvSpec.model = Ref(this.spec);
		};
		wcm.oscInputRange ?? {
			wcm.oscInputRange = ();
		};
		wcm.oscInputRange.model ?? {
			wcm.oscInputRange.model = Ref([0.0001, 0.0001]);
		};
		wcm.oscConnection ?? {
			wcm.oscConnection = ();
		};
		wcm.oscConnection.model ?? {
			wdgtControllersAndModels.oscConnection.model = Ref(false);
		};
		wcm.midiConnection ?? {
			wcm.midiConnection = ();
		};
		wcm.midiConnection.model ?? {
			wcm.midiConnection.model = Ref(nil);
		};
		wcm.midiDisplay ?? {
			wcm.midiDisplay = ();
		};
		wcm.midiDisplay.model ?? {
			wcm.midiDisplay.model = Ref((src: "source", chan: "chan", ctrl: "ctrl", learn: "L"));
		}
	}
	
//	initControllerActions { 
//		|
//			controllersAndModels, 
//			key, 
//			argCalibBut, 
//			argEditor, 
//			arpMapConstrainterLo, 
//			argMapConstrainterHi, 
//			argOSCMapping,
//			argKnob,
//			argOSCEditBut,
//			argCV,
//			argMidiSrc, argMidiChan, argMidiCtrl, argMidiLearn,
//			argCalibConstraints
//		|
//		var wcm, tmpMapping;
//		var tmpSetup, tmpMapping;
//		var makeCCResponder, ccResponder;
//		var ctrlString, meanVal;
//		
//		if(key.notNil, {
//			wcm = controllersAndModels[key];
//		}, {
//			wcm = controllersAndModels;
//		});
//			
//		wcm.calibration.controller ?? { 
//			wcm.calibration.controller = SimpleController(wcm.calibration.model);
//		};
//
//		wcm.calibration.controller.put(\value, { |theChanger, what, moreArgs|
//			prCalibrate = (theChanger.value);
//			theChanger.value.switch(
//				true, { 
//					calibBut.value_(0);
//					if(editor.notNil and:{ editor.isClosed.not }, {
//						editor.calibBut.value_(0);
//						mapConstrainterLo ?? { 
//							mapConstrainterLo = CV([-inf, inf].asSpec, 0.00001);
//							mapConstrainterLo.connect(editor.calibNumBoxes.lo);
//						};
//						mapConstrainterHi ?? { 
//							mapConstrainterHi = CV([-inf, inf].asSpec, 0.00001);
//							mapConstrainterHi.connect(editor.calibNumBoxes.hi);
//						};
//						[editor.calibNumBoxes.lo, editor.calibNumBoxes.hi].do({ |nb| 
//							nb.enabled_(false);
//							nb.action_(nil);
//						})
//					})
//				},
//				false, { 
//					calibBut.value_(1);
//					if(editor.notNil and:{ editor.isClosed.not }, {
//						editor.calibBut.value_(1);
//						[mapConstrainterLo, mapConstrainterHi].do({ |cv| cv = nil; });
//						[editor.calibNumBoxes.lo, editor.calibNumBoxes.hi].do({ |nb| 
//							nb.enabled_(true);
//							nb.action_({ |b| 
//								this.oscInputConstraints_(
//									editor.calibNumBoxes.lo.value@editor.calibNumBoxes.hi.value;
//								) 
//							})
//						})
//					})
//				}
//			)
//		});
//
//		calibBut.action_({ |cb|
//			cb.value.switch(
//				0, { wcm.calibration.model.value_(true).changed(\value) },
//				1, { wcm.calibration.model.value_(false).changed(\value) }
//			)
//		});
//		
//		wcm.cvSpec.controller ?? {
//			wcm.cvSpec.controller = SimpleController(wcm.cvSpec.model);
//		};
//		
//		wcm.cvSpec.controller.put(\value, { |theChanger, what, moreArgs|
//			if(theChanger.value.minval <= 0.0 or:{
//				theChanger.value.maxval <= 0.0
//			}, {
//				if(prOSCMapping === \linexp or:{
//					prOSCMapping === \expexp
//				}, {
//					prOSCMapping = \linlin;
//					if(editor.notNil and:{
//						editor.isClosed.not
//					}, {
//						editor.mappingSelect.value_(0);
//					})
//				})
//			}, {
//				if(editor.notNil and:{
//					editor.isClosed.not	
//				}, {
//					tmpMapping = editor.mappingSelect.item;
//					editor.mappingSelect.items.do({ |item, i|
//						if(item == tmpMapping, {
//							editor.mappingSelect.value_(i)
//						})
//					})
//				})
//			});
//			thisCV.spec_(theChanger.value);
//			block { |break|
//				#[\pan, \boostcut, \bipolar, \detune].do({ |symbol| 
//					if(thisCV.spec == symbol.asSpec, { 
//						break.value(knob.centered_(true));
//					}, {
//						knob.centered_(false);
//					})			
//				})
//			}
//		});
//		
//		wcm.oscInputRange.controller ?? {
//			wcm.oscInputRange.controller = SimpleController(wcm.oscInputRange.model);
//		};
//
//		wcm.oscInputRange.controller.put(\value, { |theChanger, what, moreArgs|
//			if(theChanger.value[0] <= 0 or:{
//				theChanger.value[1] <= 0
//			}, {
//				if(prOSCMapping === \explin or:{
//					prOSCMapping === \expexp
//				}, {
//					prOSCMapping = \linlin;
//				});
//				if(editor.notNil and:{
//					editor.isClosed.not
//				}, {
//					{	
//						oscEditBut.states_([[
//							oscEditBut.states[0][0].split($\n)[0]++"\n"++prOSCMapping.asString,
//							oscEditBut.states[0][1],
//							oscEditBut.states[0][2]
//						]]);
//						oscEditBut.refresh;
//						editor.mappingSelect.items.do({ |item, i|
//							if(item.asSymbol === prOSCMapping, {
//								editor.mappingSelect.value_(i);
//							})
//						})
//					}.defer
//				})		
//			}, {
//				if(editor.notNil and:{
//					editor.isClosed.not	
//				}, {
//					{
//						editor.mappingSelect.items.do({ |item, i|
//							if(item.asSymbol === prOSCMapping, {
//								editor.mappingSelect.value_(i)
//							})
//						});
//					}.defer;
//					if(oscEditBut.states[0][0].split($\n)[0] != "edit OSC", {
//						{
//							oscEditBut.states_([[
//								oscEditBut.states[0][0].split($\n)[0]++"\n"++prOSCMapping.asString,
//								oscEditBut.states[0][1],
//								oscEditBut.states[0][2]
//							]]);
//							oscEditBut.refresh;
//						}.defer
//					})
//				})
//			})
//		});
//		
//		wcm.midiConnection.controller ?? {
//			wcm.midiConnection.controller = SimpleController(wcm.midiConnection.model);
//		};
//		
//		wcm.midiConnection.controller.put(\value, { |theChanger, what, moreArgs|
//			if(theChanger.value.isKindOf(Event), {
//				makeCCResponder = { |argSrc, argChan, argNum|
//					CCResponder({ |src, chan, num, val|
//						ctrlString ? ctrlString = num+1;
//						if(this.ctrlButtonBank.notNil, {
//							if(ctrlString%this.ctrlButtonBank == 0, {
//								ctrlString = this.ctrlButtonBank.asString;
//							}, {
//								ctrlString = (ctrlString%this.ctrlButtonBank).asString;
//							});
//							ctrlString = ((num+1/this.ctrlButtonBank).ceil).asString++":"++ctrlString;
//						}, {
//							ctrlString = num+1;
//						});
//	
//						this.midimode.switch(
//							0, { 
//								if(val/127 < (cv.input+(softWithin/2)) and: {
//									val/127 > (cv.input-(softWithin/2));
//								}, { 
//									cv.input_(val/127);
//								})
//							},
//							1, { 
//								meanVal = this.midimean;
//								cv.input_(cv.input+((val-meanVal)/127*this.midiresolution)) 
//							}
//						);
//						src !? { midisrc = src };
//						chan !? { midichan = chan };
//						num !? { midinum = ctrlString };
//					}, argSrc, argChan, argNum, nil);
//				};
//				
//				fork {
//					block { |break|
//						loop {
//							0.01.wait;
//							if(midisrc.notNil and:{
//								midichan.notNil and:{
//									midinum.notNil;
//								}
//							}, {
//								break.value(
//									wcm.midiDisplay.model.value_(
//										(src: midisrc, chan: midichan, ctrl: midinum, learn: "X")
//									).changed(\value)
//								)
//							})
//						}
//					}
//				};
//
//				if(theChanger.value.isEmpty, {
//					cc = makeCCResponder.().learn;
//				}, {
//					cc = makeCCResponder.(theChanger.value.src, theChanger.value.chan, theChanger.value.num);
//				})
//			}, {
//				cc.remove;
//				cc = nil;
//				wcm.midiDisplay.model.value_(
//					(src: "source", chan: "chan", ctrl: "ctrl", learn: "L")
//				).changed(\value);
//				midisrc = nil; midichan = nil; midinum = nil;
//			})
//		});
//		
//		wcm.midiDisplay.controller ?? {
//			wcm.midiDisplay.controller = SimpleController(wcm.midiDisplay.model);
//		};
//		
//		wcm.midiDisplay.controller.put(\value, { |theChanger, what, moreArgs|
//			theChanger.value.learn.switch(
//				"X", {
//					defer {
//						midiSrc.string_(theChanger.value.src.asString)
//							.background_(Color.red)
//							.stringColor_(Color.white)
//							.canFocus_(false)
//						;
//						midiChan.string_((theChanger.value.chan+1).asString)
//							.background_(Color.red)
//							.stringColor_(Color.white)
//							.canFocus_(false)
//						;
//						midiCtrl.string_(theChanger.value.ctrl)
//							.background_(Color.red)
//							.stringColor_(Color.white)
//							.canFocus_(false)
//						;
//						midiLearn.value_(1)
//					}
//				},
//				"C", {
//					midiLearn.states_([
//						["C", Color.white, Color(0.11468057974842, 0.38146154367376, 0.19677815686724)],
//						["X", Color.white, Color.red]
//					]).refresh;
//				},
//				"L", {
//					defer {
//						midiSrc.string_(theChanger.value.src)
//							.background_(Color.white)
//							.stringColor_(Color.black)
//							.canFocus_(true)
//						;
//						midiChan.string_(theChanger.value.chan)
//							.background_(Color.white)
//							.stringColor_(Color.black)
//							.canFocus_(true)
//						;
//						midiCtrl.string_(theChanger.value.ctrl)
//							.background_(Color.white)
//							.stringColor_(Color.black)
//							.canFocus_(true)
//						;
//						midiLearn.states_([
//							["L", Color.white, Color.blue],
//							["X", Color.white, Color.red]
//						])
//						.value_(0).refresh;
//					}
//				}
//			)
//		});
//
//		wcm.oscConnection.controller ?? {
//			wcm.oscConnection.controller = SimpleController(wcm.oscConnection.model);
//		};
//
//		wcm.oscConnection.controller.put(\value, { |theChanger, what, moreArgs|
//			if(theChanger.value.size == 2, {
//				oscResponder = OSCresponderNode(nil, theChanger.value[0].asSymbol, { |t, r, msg|
//					if(prCalibrate, { 
//						if(prCalibConstraints.isNil, {
//							prCalibConstraints = (lo: msg[theChanger.value[1]], hi: msg[theChanger.value[1]]);
//						}, {
//							if(msg[theChanger.value[1]] < prCalibConstraints.lo, { 
//								prCalibConstraints.lo = msg[theChanger.value[1]];
//								wcm.oscInputRange.model.value_([
//									msg[theChanger.value[1]], 
//									wcm.oscInputRange.model.value[1]
//								]).changed(\value);
//							});
//							if(msg[theChanger.value[1]] > prCalibConstraints.hi, {
//								prCalibConstraints.hi = msg[theChanger.value[1]];
//								wcm.oscInputRange.model.value_([
//									wcm.oscInputRange.model.value[0], 
//									msg[theChanger.value[1]]
//								]).changed(\value);
//							});
//						});
//						mapConstrainterLo.value_(prCalibConstraints.lo);
//						mapConstrainterHi.value_(prCalibConstraints.hi);
//					}, {
//						if(prCalibConstraints.isNil, {
//							prCalibConstraints = (lo: wcm.oscInputRange.model.value[0], hi: wcm.oscInputRange.model.value[1]);
//						})
//					});
//					thisCV.value_(
//						msg[theChanger.value[1]].perform(
//							prOSCMapping,
//							prCalibConstraints.lo, prCalibConstraints.hi,
//							this.spec.minval, this.spec.maxval,
//							\minmax
//						)
//					)
//				}).add;
//				oscEditBut.states_([
//					[theChanger.value[0].asString++"["++theChanger.value[1].asString++"]"++"\n"++prOSCMapping.asString, Color.white, Color.cyan(0.5)]
//				]);
//				if(editor.notNil and:{
//					editor.isClosed.not
//				}, {
//					editor.connectorBut.value_(0);
//					editor.nameField.enabled_(false).string_(theChanger.value[0].asString);
//					if(prCalibrate, {
//						[editor.inputConstraintLoField, editor.inputConstraintHiField].do(_.enabled_(false));
//					});
//					editor.indexField.value_(theChanger.value[1]).enabled_(false);
//					editor.connectorBut.value_(1);
//				});
//				oscEditBut.refresh;
//			});
//			if(theChanger.value == false, {
//				oscResponder.remove;
//				oscEditBut.states_([
//					["edit OSC", Color.black, Color.clear]
//				]);
//				wcm.oscInputRange.model.value_([0.0001, 0.0001]).changed(\value);
//				prCalibConstraints = nil;
//				if(editor.notNil and:{
//					editor.isClosed.not
//				}, {
//					editor.connectorBut.value_(0);
//					editor.nameField.enabled_(true);
//					editor.inputConstraintLoField.value_(
//						wcm.oscInputRange.model.value[0];
//					);
//					editor.inputConstraintHiField.value_(
//						wcm.oscInputRange.model.value[1];
//					);
//					if(prCalibrate.not, {
//						[editor.inputConstraintLoField, editor.inputConstraintHiField].do(_.enabled_(true));
//					});
//					editor.indexField.enabled_(true);
//					editor.connectorBut.value_(0);
//				});
//				oscEditBut.refresh;
//			})
//		});
//	}

}