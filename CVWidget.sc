
CVWidget {

	var prMidiMode = 0, prMidiMean = 64, prCtrlButtonBank, prMidiResolution = 1, prSoftWithin = 0.1;
	var prCalibrate = true; // OSC-calibration enabled/disabled - private
	var visibleGuiEls, allGuiEls;
	var <widgetBg, <label, <nameField; // elements contained in any kind of CVWidget
	var <visible, widgetXY, widgetProps;
	var <wdgtControllersAndModels;

	setup {
		^[this.midiMode, this.midiResolution, this.midiMean, this.ctrlButtonBank, this.softWithin, prCalibrate];
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
	
	midiMode_ { |mode|
		prMidiMode = mode;
		wdgtControllersAndModels.midiOptions.model.value_(
			(
				midiMode: prMidiMode,
				midiMean: prMidiMean,
				ctrlButtonBank: prCtrlButtonBank,
				midiResolution: prMidiResolution,
				softWithin: prSoftWithin
			)
		).changed(\value);
	}
	
	midiMode {
		^prMidiMode;	
	}
	
	midiMean_ { |meanval|
		prMidiMean = meanval;
		wdgtControllersAndModels.midiOptions.model.value_(
			(
				midiMode: prMidiMode,
				midiMean: prMidiMean,
				ctrlButtonBank: prCtrlButtonBank,
				midiResolution: prMidiResolution,
				softWithin: prSoftWithin
			)
		).changed(\value);
	}
	
	midiMean	{
		^prMidiMean;	
	}
	
	softWithin_ { |threshold|
		prSoftWithin = threshold;
		wdgtControllersAndModels.midiOptions.model.value_(
			(
				midiMode: prMidiMode,
				midiMean: prMidiMean,
				ctrlButtonBank: prCtrlButtonBank,
				midiResolution: prMidiResolution,
				softWithin: prSoftWithin
			)
		).changed(\value);
	}
	
	softWithin {
		^prSoftWithin;
	}
	
	ctrlButtonBank_ { |numSliders|
		prCtrlButtonBank = numSliders;
		wdgtControllersAndModels.midiOptions.model.value_(
			(
				midiMode: prMidiMode,
				midiMean: prMidiMean,
				ctrlButtonBank: prCtrlButtonBank,
				midiResolution: prMidiResolution,
				softWithin: prSoftWithin
			)
		).changed(\value);
	}
	
	ctrlButtonBank {
		^prCtrlButtonBank;
	}
	
	midiResolution_ { |resolution|
		if(resolution.isNumber.not, {
			Error("The value passed in for midiResolution must be a float or an integer - lower values result in a higher resolution and vice versa. midiResolution only applies for in-/decremental midiMode").throw;
		}, {
			prMidiResolution = resolution;
			wdgtControllersAndModels.midiOptions.model.value_(
				(
					midiMode: prMidiMode,
					midiMean: prMidiMean,
					ctrlButtonBank: prCtrlButtonBank,
					midiResolution: prMidiResolution,
					softWithin: prSoftWithin
				)
			).changed(\value);
		})
	}
	
	midiResolution {
		^prMidiResolution;
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
		};
		wcm.midiOptions ?? {
			wcm.midiOptions = ();
		};
		wcm.midiOptions.model ?? {
			wcm.midiOptions.model = Ref(
				(
					midiMode: prMidiMode, 
					midiMean: prMidiMean, 
					ctrlButtonBank: prCtrlButtonBank, 
					midiResolution: prMidiResolution, 
					softWithin: prSoftWithin
				)
			)
		};
		wcm.mapConstrainterLo ?? { 
			wcm.mapConstrainterLo = CV([-inf, inf].asSpec, wdgtControllersAndModels.oscInputRange.model.value[0]);
		};
		wcm.mapConstrainterHi ?? { 
			wcm.mapConstrainterHi = CV([-inf, inf].asSpec, wdgtControllersAndModels.oscInputRange.model.value[1]);
		};

	}
	
	initControllerActions { |controllersAndModels, guiEnv, midiOscSpecsEnv, cv, key|
		var wcm, thisGuiEls, midiOscSpecs, tmpMapping, tmpSetup, tmpCV;
		var makeCCResponder, ccResponder;
		var ctrlString, meanVal;
		
		if(key.notNil, {
			wcm = controllersAndModels[key];
			thisGuiEls = guiEnv[key];
			midiOscSpecs = midiOscSpecsEnv[key];
			tmpCV = cv[key];
		}, {
			wcm = controllersAndModels;
			thisGuiEls = guiEnv;
			midiOscSpecs = midiOscSpecsEnv;
			tmpCV = cv;
		});
			
		wcm.calibration.controller ?? { 
			wcm.calibration.controller = SimpleController(wcm.calibration.model);
		};

		wcm.calibration.controller.put(\value, { |theChanger, what, moreArgs|
			prCalibrate = (theChanger.value);
			theChanger.value.switch(
				true, { 
					thisGuiEls.calibBut.value_(0);
					if(thisGuiEls.editor.notNil and:{ thisGuiEls.editor.isClosed.not }, {
						thisGuiEls.editor.calibBut.value_(0);
						wcm.mapConstrainterLo ?? { 
							wcm.mapConstrainterLo = CV([-inf, inf].asSpec, 0.00001);
							wcm.mapConstrainterLo.connect(thisGuiEls.editor.calibNumBoxes.lo);
						};
						wcm.mapConstrainterHi ?? { 
							wcm.mapConstrainterHi = CV([-inf, inf].asSpec, 0.00001);
							wcm.mapConstrainterHi.connect(thisGuiEls.editor.calibNumBoxes.hi);
						};
						[thisGuiEls.editor.calibNumBoxes.lo, thisGuiEls.editor.calibNumBoxes.hi].do({ |nb| 
							nb.enabled_(false);
							nb.action_(nil);
						})
					})
				},
				false, { 
					thisGuiEls.calibBut.value_(1);
					if(thisGuiEls.editor.notNil and:{ thisGuiEls.editor.isClosed.not }, {
						thisGuiEls.editor.calibBut.value_(1);
						[wcm.mapConstrainterLo, wcm.mapConstrainterHi].do({ |cv| cv = nil; });
						[thisGuiEls.editor.calibNumBoxes.lo, thisGuiEls.editor.calibNumBoxes.hi].do({ |nb| 
							nb.enabled_(true);
							nb.action_({ |b| 
								this.oscInputConstraints_(
									thisGuiEls.editor.calibNumBoxes.lo.value@thisGuiEls.editor.calibNumBoxes.hi.value;
								) 
							})
						})
					})
				}
			)
		});

		thisGuiEls.calibBut.action_({ |cb|
			cb.value.switch(
				0, { wcm.calibration.model.value_(true).changed(\value) },
				1, { wcm.calibration.model.value_(false).changed(\value) }
			)
		});
		
		wcm.cvSpec.controller ?? {
			wcm.cvSpec.controller = SimpleController(wcm.cvSpec.model);
		};
		
		wcm.cvSpec.controller.put(\value, { |theChanger, what, moreArgs|
			if(theChanger.value.minval <= 0.0 or:{
				theChanger.value.maxval <= 0.0
			}, {
				if(midiOscSpecs.oscMapping === \linexp or:{
					midiOscSpecs.oscMapping === \expexp
				}, {
					midiOscSpecs.oscMapping = \linlin;
					if(thisGuiEls.editor.notNil and:{
						thisGuiEls.editor.isClosed.not
					}, {
						thisGuiEls.editor.mappingSelect.value_(0);
					})
				})
			}, {
				if(thisGuiEls.editor.notNil and:{
					thisGuiEls.editor.isClosed.not	
				}, {
					tmpMapping = thisGuiEls.editor.mappingSelect.item;
					thisGuiEls.editor.mappingSelect.items.do({ |item, i|
						if(item == tmpMapping, {
							thisGuiEls.editor.mappingSelect.value_(i)
						})
					})
				})
			});
			tmpCV.spec_(theChanger.value);
			block { |break|
				#[\pan, \boostcut, \bipolar, \detune].do({ |symbol| 
					if(tmpCV.spec == symbol.asSpec, { 
						break.value(thisGuiEls.knob.centered_(true));
					}, {
						thisGuiEls.knob.centered_(false);
					})			
				})
			}
		});
		
		wcm.oscInputRange.controller ?? {
			wcm.oscInputRange.controller = SimpleController(wcm.oscInputRange.model);
		};

		wcm.oscInputRange.controller.put(\value, { |theChanger, what, moreArgs|
			if(theChanger.value[0] <= 0 or:{
				theChanger.value[1] <= 0
			}, {
				if(midiOscSpecs.oscMapping === \explin or:{
					midiOscSpecs.oscMapping === \expexp
				}, {
					midiOscSpecs.oscMapping = \linlin;
				});
				if(thisGuiEls.editor.notNil and:{
					thisGuiEls.editor.isClosed.not
				}, {
					{	
						thisGuiEls.oscEditBut.states_([[
							thisGuiEls.oscEditBut.states[0][0].split($\n)[0]++"\n"++midiOscSpecs.oscMapping.asString,
							thisGuiEls.oscEditBut.states[0][1],
							thisGuiEls.oscEditBut.states[0][2]
						]]);
						thisGuiEls.oscEditBut.refresh;
						thisGuiEls.editor.mappingSelect.items.do({ |item, i|
							if(item.asSymbol === midiOscSpecs.oscMapping, {
								thisGuiEls.editor.mappingSelect.value_(i);
							})
						})
					}.defer
				})		
			}, {
				if(thisGuiEls.editor.notNil and:{
					thisGuiEls.editor.isClosed.not	
				}, {
					{
						thisGuiEls.editor.mappingSelect.items.do({ |item, i|
							if(item.asSymbol === midiOscSpecs.oscMapping, {
								thisGuiEls.editor.mappingSelect.value_(i)
							})
						});
					}.defer;
					if(thisGuiEls.oscEditBut.states[0][0].split($\n)[0] != "edit OSC", {
						{
							thisGuiEls.oscEditBut.states_([[
								thisGuiEls.oscEditBut.states[0][0].split($\n)[0]++"\n"++midiOscSpecs.oscMapping.asString,
								thisGuiEls.oscEditBut.states[0][1],
								thisGuiEls.oscEditBut.states[0][2]
							]]);
							thisGuiEls.oscEditBut.refresh;
						}.defer
					})
				})
			})
		});
		
		wcm.midiConnection.controller ?? {
			wcm.midiConnection.controller = SimpleController(wcm.midiConnection.model);
		};
		
		wcm.midiConnection.controller.put(\value, { |theChanger, what, moreArgs|
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
	
						this.midiMode.switch(
							0, { 
								if(val/127 < (cv.input+(prSoftWithin/2)) and: {
									val/127 > (cv.input-(prSoftWithin/2));
								}, { 
									cv.input_(val/127);
								})
							},
							1, { 
								meanVal = this.midiMean;
								cv.input_(cv.input+((val-meanVal)/127*this.midiResolution)) 
							}
						);
						src !? { midiOscSpecs.midisrc = src };
						chan !? { midiOscSpecs.midichan = chan };
						num !? { midiOscSpecs.midinum = ctrlString };
					}, argSrc, argChan, argNum, nil);
				};
				
				fork {
					block { |break|
						loop {
							0.01.wait;
							if(midiOscSpecs.midisrc.notNil and:{
								midiOscSpecs.midichan.notNil and:{
									midiOscSpecs.midinum.notNil;
								}
							}, {
								break.value(
									wcm.midiDisplay.model.value_(
										(src: midiOscSpecs.midisrc, chan: midiOscSpecs.midichan, ctrl: midiOscSpecs.midinum, learn: "X")
									).changed(\value)
								)
							})
						}
					}
				};

				if(theChanger.value.isEmpty, {
					midiOscSpecs.cc = makeCCResponder.().learn;
				}, {
					midiOscSpecs.cc = makeCCResponder.(theChanger.value.src, theChanger.value.chan, theChanger.value.num);
				})
			}, {
				midiOscSpecs.cc.remove;
				midiOscSpecs.cc = nil;
				wcm.midiDisplay.model.value_(
					(src: "source", chan: "chan", ctrl: "ctrl", learn: "L")
				).changed(\value);
				midiOscSpecs.midisrc = nil; midiOscSpecs.midichan = nil; midiOscSpecs.midinum = nil;
			})
		});
		
		wcm.midiDisplay.controller ?? {
			wcm.midiDisplay.controller = SimpleController(wcm.midiDisplay.model);
		};
		
		wcm.midiDisplay.controller.put(\value, { |theChanger, what, moreArgs|
			theChanger.value.learn.switch(
				"X", {
					defer {
						thisGuiEls.midiSrc.string_(theChanger.value.src.asString)
							.background_(Color.red)
							.stringColor_(Color.white)
							.canFocus_(false)
						;
						thisGuiEls.midiChan.string_((theChanger.value.chan+1).asString)
							.background_(Color.red)
							.stringColor_(Color.white)
							.canFocus_(false)
						;
						thisGuiEls.midiCtrl.string_(theChanger.value.ctrl)
							.background_(Color.red)
							.stringColor_(Color.white)
							.canFocus_(false)
						;
						thisGuiEls.midiLearn.value_(1)
					};
					if(thisGuiEls.editor.notNil and:{
						thisGuiEls.editor.isClosed.not
					}, {
						defer {
							thisGuiEls.editor.midiSrcField.string_(theChanger.value.src.asString)
								.background_(Color.red)
								.stringColor_(Color.white)
								.canFocus_(false)
							;
							thisGuiEls.editor.midiChanField.string_((theChanger.value.chan+1).asString)
								.background_(Color.red)
								.stringColor_(Color.white)
								.canFocus_(false)
							;
							thisGuiEls.editor.midiCtrlField.string_(theChanger.value.ctrl)
								.background_(Color.red)
								.stringColor_(Color.white)
								.canFocus_(false)
							;
							thisGuiEls.editor.midiLearnBut.value_(1)
						}
					})
				},
				"C", {
					thisGuiEls.midiLearn.states_([
						["C", Color.white, Color(0.11468057974842, 0.38146154367376, 0.19677815686724)],
						["X", Color.white, Color.red]
					]).refresh;
					if(thisGuiEls.editor.notNil and:{
						thisGuiEls.editor.isClosed.not
					}, {
						thisGuiEls.editor.midiLearnBut.states_([
							["C", Color.white, Color(0.11468057974842, 0.38146154367376, 0.19677815686724)],
							["X", Color.white, Color.red]
						]).refresh;
					})
				},
				"L", {
					defer {
						thisGuiEls.midiSrc.string_(theChanger.value.src)
							.background_(Color.white)
							.stringColor_(Color.black)
							.canFocus_(true)
						;
						thisGuiEls.midiChan.string_(theChanger.value.chan)
							.background_(Color.white)
							.stringColor_(Color.black)
							.canFocus_(true)
						;
						thisGuiEls.midiCtrl.string_(theChanger.value.ctrl)
							.background_(Color.white)
							.stringColor_(Color.black)
							.canFocus_(true)
						;
						thisGuiEls.midiLearn.states_([
							["L", Color.white, Color.blue],
							["X", Color.white, Color.red]
						])
						.value_(0).refresh;
					};
					if(thisGuiEls.editor.notNil and:{
						thisGuiEls.editor.isClosed.not
					}, {
						defer {
							thisGuiEls.editor.midiSrcField.string_(theChanger.value.src)
								.background_(Color.white)
								.stringColor_(Color.black)
								.canFocus_(true)
							;
							thisGuiEls.editor.midiChanField.string_(theChanger.value.chan)
								.background_(Color.white)
								.stringColor_(Color.black)
								.canFocus_(true)
							;
							thisGuiEls.editor.midiCtrlField.string_(theChanger.value.ctrl)
								.background_(Color.white)
								.stringColor_(Color.black)
								.canFocus_(true)
							;
							thisGuiEls.editor.midiLearnBut.value_(0)
						}
					})
				}
			)
		});

		wcm.midiOptions.controller ?? {
			wcm.midiOptions.controller = SimpleController(wcm.midiOptions.model);
		};
		
		wcm.midiOptions.controller.put(\value, { |theChanger, what, moreArgs|
			if(thisGuiEls.editor.notNil and:{
				thisGuiEls.editor.isClosed.not
			}, {
				thisGuiEls.editor.midiModeSelect.value_(theChanger.value.midiMode);
				thisGuiEls.editor.midiMeanNB.value_(theChanger.value.midiMean);
				thisGuiEls.editor.softWithinNB.value_(theChanger.value.softWithin);
				thisGuiEls.editor.midiResolutionNB.value_(theChanger.value.midiResolution);
				thisGuiEls.editor.ctrlButtonBankField.value_(theChanger.value.ctrlButtonBank);
			})
		});

		wcm.oscConnection.controller ?? {
			wcm.oscConnection.controller = SimpleController(wcm.oscConnection.model);
		};

		wcm.oscConnection.controller.put(\value, { |theChanger, what, moreArgs|
			if(theChanger.value.size == 2, {
				midiOscSpecs.oscResponder = OSCresponderNode(nil, theChanger.value[0].asSymbol, { |t, r, msg|
					if(prCalibrate, { 
						if(midiOscSpecs.calibConstraints.isNil, {
							midiOscSpecs.calibConstraints = (lo: msg[theChanger.value[1]], hi: msg[theChanger.value[1]]);
						}, {
							if(msg[theChanger.value[1]] < midiOscSpecs.calibConstraints.lo, { 
								midiOscSpecs.calibConstraints.lo = msg[theChanger.value[1]];
								wcm.oscInputRange.model.value_([
									msg[theChanger.value[1]], 
									wcm.oscInputRange.model.value[1]
								]).changed(\value);
							});
							if(msg[theChanger.value[1]] > midiOscSpecs.calibConstraints.hi, {
								midiOscSpecs.calibConstraints.hi = msg[theChanger.value[1]];
								wcm.oscInputRange.model.value_([
									wcm.oscInputRange.model.value[0], 
									msg[theChanger.value[1]]
								]).changed(\value);
							});
						});
						wcm.mapConstrainterLo.value_(midiOscSpecs.calibConstraints.lo);
						wcm.mapConstrainterHi.value_(midiOscSpecs.calibConstraints.hi);
					}, {
						if(midiOscSpecs.calibConstraints.isNil, {
							midiOscSpecs.calibConstraints = (lo: wcm.oscInputRange.model.value[0], hi: wcm.oscInputRange.model.value[1]);
						})
					});
					tmpCV.value_(
						msg[theChanger.value[1]].perform(
							midiOscSpecs.oscMapping,
							midiOscSpecs.calibConstraints.lo, midiOscSpecs.calibConstraints.hi,
							this.spec.minval, this.spec.maxval,
							\minmax
						)
					)
				}).add;
				thisGuiEls.oscEditBut.states_([
					[theChanger.value[0].asString++"["++theChanger.value[1].asString++"]"++"\n"++midiOscSpecs.oscMapping.asString, Color.white, Color.cyan(0.5)]
				]);
				if(thisGuiEls.editor.notNil and:{
					thisGuiEls.editor.isClosed.not
				}, {
					thisGuiEls.editor.connectorBut.value_(0);
					thisGuiEls.editor.nameField.enabled_(false).string_(theChanger.value[0].asString);
					if(prCalibrate, {
						[thisGuiEls.editor.inputConstraintLoField, thisGuiEls.editor.inputConstraintHiField].do(_.enabled_(false));
					});
					thisGuiEls.editor.indexField.value_(theChanger.value[1]).enabled_(false);
					thisGuiEls.editor.connectorBut.value_(1);
				});
				thisGuiEls.oscEditBut.refresh;
			});
			if(theChanger.value == false, {
				midiOscSpecs.oscResponder.remove;
				thisGuiEls.oscEditBut.states_([
					["edit OSC", Color.black, Color.clear]
				]);
				wcm.oscInputRange.model.value_([0.0001, 0.0001]).changed(\value);
				midiOscSpecs.calibConstraints = nil;
				if(thisGuiEls.editor.notNil and:{
					thisGuiEls.editor.isClosed.not
				}, {
					thisGuiEls.editor.connectorBut.value_(0);
					thisGuiEls.editor.nameField.enabled_(true);
					thisGuiEls.editor.inputConstraintLoField.value_(
						wcm.oscInputRange.model.value[0];
					);
					thisGuiEls.editor.inputConstraintHiField.value_(
						wcm.oscInputRange.model.value[1];
					);
					if(prCalibrate.not, {
						[thisGuiEls.editor.inputConstraintLoField, thisGuiEls.editor.inputConstraintHiField].do(_.enabled_(true));
					});
					thisGuiEls.editor.indexField.enabled_(true);
					thisGuiEls.editor.connectorBut.value_(0);
				});
				thisGuiEls.oscEditBut.refresh;
			})
		});
	}

}