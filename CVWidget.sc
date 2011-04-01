
CVWidget {

	var prMidiMode = 0, prMidiMean = 64, prCtrlButtonBank, prMidiResolution = 1, prSoftWithin = 0.1;
	var prCalibrate = true; // OSC-calibration enabled/disabled - private
	var visibleGuiEls, allGuiEls;
	var <widgetBg, <label, <nameField; // elements contained in any kind of CVWidget
	var <visible, widgetXY, widgetProps;
	var <wdgtControllersAndModels;

	setup {
		^[prMidiMode, prMidiResolution, prMidiMean, prCtrlButtonBank, prSoftWithin, prCalibrate];
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
		wdgtControllersAndModels !? {
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
	}
	
	midiMode {
		^prMidiMode;	
	}
	
	midiMean_ { |meanval|
		prMidiMean = meanval;
		wdgtControllersAndModels !? {
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
	}
	
	midiMean	{
		^prMidiMean;	
	}
	
	softWithin_ { |threshold|
		prSoftWithin = threshold;
		wdgtControllersAndModels !? {
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
	}
	
	softWithin {
		^prSoftWithin;
	}
	
	ctrlButtonBank_ { |numSliders|
		prCtrlButtonBank = numSliders;
		wdgtControllersAndModels !? {
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
	}
	
	ctrlButtonBank {
		^prCtrlButtonBank;
	}
	
	midiResolution_ { |resolution|
		prMidiResolution = resolution;
		wdgtControllersAndModels !? {
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
		var wcm, thisGuiEnv, midiOscSpecs, tmpMapping, tmpSetup, tmpCV, tmp;
		var makeCCResponder, ccResponder;
		var ctrlString, meanVal;
		
		if(key.notNil, {
			wcm = controllersAndModels[key];
			thisGuiEnv = guiEnv[key];
			midiOscSpecs = midiOscSpecsEnv[key];
			tmpCV = cv[key];
		}, {
			wcm = controllersAndModels;
			thisGuiEnv = guiEnv;
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
					thisGuiEnv.calibBut.value_(0);
					if(thisGuiEnv.editor.notNil and:{ thisGuiEnv.editor.isClosed.not }, {
						thisGuiEnv.editor.calibBut.value_(0);
						wcm.mapConstrainterLo ?? { 
							wcm.mapConstrainterLo = CV([-inf, inf].asSpec, 0.00001);
							wcm.mapConstrainterLo.connect(thisGuiEnv.editor.calibNumBoxes.lo);
						};
						wcm.mapConstrainterHi ?? { 
							wcm.mapConstrainterHi = CV([-inf, inf].asSpec, 0.00001);
							wcm.mapConstrainterHi.connect(thisGuiEnv.editor.calibNumBoxes.hi);
						};
						[thisGuiEnv.editor.calibNumBoxes.lo, thisGuiEnv.editor.calibNumBoxes.hi].do({ |nb| 
							nb.enabled_(false);
							nb.action_(nil);
						})
					})
				},
				false, { 
					thisGuiEnv.calibBut.value_(1);
					if(thisGuiEnv.editor.notNil and:{ thisGuiEnv.editor.isClosed.not }, {
						thisGuiEnv.editor.calibBut.value_(1);
						[wcm.mapConstrainterLo, wcm.mapConstrainterHi].do({ |cv| cv = nil; });
						[thisGuiEnv.editor.calibNumBoxes.lo, thisGuiEnv.editor.calibNumBoxes.hi].do({ |nb| 
							nb.enabled_(true);
							nb.action_({ |b| 
								this.oscInputConstraints_(
									thisGuiEnv.editor.calibNumBoxes.lo.value@thisGuiEnv.editor.calibNumBoxes.hi.value;
								) 
							})
						})
					})
				}
			)
		});

		thisGuiEnv.calibBut.action_({ |cb|
			cb.value.switch(
				0, { wcm.calibration.model.value_(true).changed(\value) },
				1, { wcm.calibration.model.value_(false).changed(\value) }
			)
		});
		
		wcm.cvSpec.controller ?? {
			wcm.cvSpec.controller = SimpleController(wcm.cvSpec.model);
		};
		
		wcm.cvSpec.controller.put(\value, { |theChanger, what, moreArgs|
			[theChanger.value, theChanger.value.class].postln;
			if(theChanger.value.minval <= 0.0 or:{
				theChanger.value.maxval <= 0.0
			}, {
				if(midiOscSpecs.oscMapping === \linexp or:{
					midiOscSpecs.oscMapping === \expexp
				}, {
					midiOscSpecs.oscMapping = \linlin;
					if(thisGuiEnv.editor.notNil and:{
						thisGuiEnv.editor.isClosed.not
					}, {
						thisGuiEnv.editor.mappingSelect.value_(0);
					})
				})
			}, {
				if(thisGuiEnv.editor.notNil and:{
					thisGuiEnv.editor.isClosed.not	
				}, {
					tmpMapping = thisGuiEnv.editor.mappingSelect.item;
					thisGuiEnv.editor.mappingSelect.items.do({ |item, i|
						if(item == tmpMapping, {
							thisGuiEnv.editor.mappingSelect.value_(i)
						})
					});
				})
			});
			
			if(thisGuiEnv.editor.notNil and:{
				thisGuiEnv.editor.isClosed.not	
			}, {
				thisGuiEnv.editor.specField.string_(theChanger.value.asCompileString);
				tmp = thisGuiEnv.editor.specsListSpecs.detectIndex({ |item, i| item == theChanger.value });
				if(tmp.notNil, {
					thisGuiEnv.editor.specsList.value_(tmp);
				}, {
					thisGuiEnv.editor.specsList.items = List["custom:"+(theChanger.value.asString)]++thisGuiEnv.editor.specsList.items;
					thisGuiEnv.editor.specsListSpecs.array_([theChanger.value]++thisGuiEnv.editor.specsListSpecs.array);
					thisGuiEnv.editor.specsList.value_(0);
					thisGuiEnv.editor.specsList.refresh;
				})
			});
			
			tmpCV.spec_(theChanger.value);
			block { |break|
				#[\pan, \boostcut, \bipolar, \detune].do({ |symbol| 
					if(tmpCV.spec == symbol.asSpec, { 
						break.value(thisGuiEnv.knob.centered_(true));
					}, {
						thisGuiEnv.knob.centered_(false);
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
				if(thisGuiEnv.editor.notNil and:{
					thisGuiEnv.editor.isClosed.not
				}, {
					{	
						thisGuiEnv.oscEditBut.states_([[
							thisGuiEnv.oscEditBut.states[0][0].split($\n)[0]++"\n"++midiOscSpecs.oscMapping.asString,
							thisGuiEnv.oscEditBut.states[0][1],
							thisGuiEnv.oscEditBut.states[0][2]
						]]);
						thisGuiEnv.oscEditBut.refresh;
						thisGuiEnv.editor.mappingSelect.items.do({ |item, i|
							if(item.asSymbol === midiOscSpecs.oscMapping, {
								thisGuiEnv.editor.mappingSelect.value_(i);
							})
						})
					}.defer
				})		
			}, {
				if(thisGuiEnv.editor.notNil and:{
					thisGuiEnv.editor.isClosed.not	
				}, {
					{
						thisGuiEnv.editor.mappingSelect.items.do({ |item, i|
							if(item.asSymbol === midiOscSpecs.oscMapping, {
								thisGuiEnv.editor.mappingSelect.value_(i)
							})
						});
					}.defer;
					if(thisGuiEnv.oscEditBut.states[0][0].split($\n)[0] != "edit OSC", {
						{
							thisGuiEnv.oscEditBut.states_([[
								thisGuiEnv.oscEditBut.states[0][0].split($\n)[0]++"\n"++midiOscSpecs.oscMapping.asString,
								thisGuiEnv.oscEditBut.states[0][1],
								thisGuiEnv.oscEditBut.states[0][2]
							]]);
							thisGuiEnv.oscEditBut.refresh;
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
						thisGuiEnv.midiSrc.string_(theChanger.value.src.asString)
							.background_(Color.red)
							.stringColor_(Color.white)
							.canFocus_(false)
						;
						thisGuiEnv.midiChan.string_((theChanger.value.chan+1).asString)
							.background_(Color.red)
							.stringColor_(Color.white)
							.canFocus_(false)
						;
						thisGuiEnv.midiCtrl.string_(theChanger.value.ctrl)
							.background_(Color.red)
							.stringColor_(Color.white)
							.canFocus_(false)
						;
						thisGuiEnv.midiLearn.value_(1)
					};
					if(thisGuiEnv.editor.notNil and:{
						thisGuiEnv.editor.isClosed.not
					}, {
						defer {
							thisGuiEnv.editor.midiSrcField.string_(theChanger.value.src.asString)
								.background_(Color.red)
								.stringColor_(Color.white)
								.canFocus_(false)
							;
							thisGuiEnv.editor.midiChanField.string_((theChanger.value.chan+1).asString)
								.background_(Color.red)
								.stringColor_(Color.white)
								.canFocus_(false)
							;
							thisGuiEnv.editor.midiCtrlField.string_(theChanger.value.ctrl)
								.background_(Color.red)
								.stringColor_(Color.white)
								.canFocus_(false)
							;
							thisGuiEnv.editor.midiLearnBut.value_(1)
						}
					})
				},
				"C", {
					thisGuiEnv.midiLearn.states_([
						["C", Color.white, Color(0.11468057974842, 0.38146154367376, 0.19677815686724)],
						["X", Color.white, Color.red]
					]).refresh;
					if(thisGuiEnv.editor.notNil and:{
						thisGuiEnv.editor.isClosed.not
					}, {
						thisGuiEnv.editor.midiLearnBut.states_([
							["C", Color.white, Color(0.11468057974842, 0.38146154367376, 0.19677815686724)],
							["X", Color.white, Color.red]
						]).refresh;
					})
				},
				"L", {
					defer {
						thisGuiEnv.midiSrc.string_(theChanger.value.src)
							.background_(Color.white)
							.stringColor_(Color.black)
							.canFocus_(true)
						;
						thisGuiEnv.midiChan.string_(theChanger.value.chan)
							.background_(Color.white)
							.stringColor_(Color.black)
							.canFocus_(true)
						;
						thisGuiEnv.midiCtrl.string_(theChanger.value.ctrl)
							.background_(Color.white)
							.stringColor_(Color.black)
							.canFocus_(true)
						;
						thisGuiEnv.midiLearn.states_([
							["L", Color.white, Color.blue],
							["X", Color.white, Color.red]
						])
						.value_(0).refresh;
					};
					if(thisGuiEnv.editor.notNil and:{
						thisGuiEnv.editor.isClosed.not
					}, {
						defer {
							thisGuiEnv.editor.midiSrcField.string_(theChanger.value.src)
								.background_(Color.white)
								.stringColor_(Color.black)
								.canFocus_(true)
							;
							thisGuiEnv.editor.midiChanField.string_(theChanger.value.chan)
								.background_(Color.white)
								.stringColor_(Color.black)
								.canFocus_(true)
							;
							thisGuiEnv.editor.midiCtrlField.string_(theChanger.value.ctrl)
								.background_(Color.white)
								.stringColor_(Color.black)
								.canFocus_(true)
							;
							thisGuiEnv.editor.midiLearnBut.states_([
								["L", Color.white, Color.blue],
								["X", Color.white, Color.red]
							])
							.value_(0)
						}
					})
				}
			)
		});

		wcm.midiOptions.controller ?? {
			wcm.midiOptions.controller = SimpleController(wcm.midiOptions.model);
		};
		
		wcm.midiOptions.controller.put(\value, { |theChanger, what, moreArgs|
			if(thisGuiEnv.editor.notNil and:{
				thisGuiEnv.editor.isClosed.not
			}, {
				thisGuiEnv.editor.midiModeSelect.value_(theChanger.value.midiMode);
				thisGuiEnv.editor.midiMeanNB.value_(theChanger.value.midiMean);
				thisGuiEnv.editor.softWithinNB.value_(theChanger.value.softWithin);
				thisGuiEnv.editor.midiResolutionNB.value_(theChanger.value.midiResolution);
				thisGuiEnv.editor.ctrlButtonBankField.value_(theChanger.value.ctrlButtonBank);
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
				thisGuiEnv.oscEditBut.states_([
					[theChanger.value[0].asString++"["++theChanger.value[1].asString++"]"++"\n"++midiOscSpecs.oscMapping.asString, Color.white, Color.cyan(0.5)]
				]);
				if(thisGuiEnv.editor.notNil and:{
					thisGuiEnv.editor.isClosed.not
				}, {
					thisGuiEnv.editor.connectorBut.value_(0);
					thisGuiEnv.editor.nameField.enabled_(false).string_(theChanger.value[0].asString);
					if(prCalibrate, {
						[thisGuiEnv.editor.inputConstraintLoField, thisGuiEnv.editor.inputConstraintHiField].do(_.enabled_(false));
					});
					thisGuiEnv.editor.indexField.value_(theChanger.value[1]).enabled_(false);
					thisGuiEnv.editor.connectorBut.value_(1);
				});
				thisGuiEnv.oscEditBut.refresh;
			});
			if(theChanger.value == false, {
				midiOscSpecs.oscResponder.remove;
				thisGuiEnv.oscEditBut.states_([
					["edit OSC", Color.black, Color.clear]
				]);
				wcm.oscInputRange.model.value_([0.0001, 0.0001]).changed(\value);
				midiOscSpecs.calibConstraints = nil;
				if(thisGuiEnv.editor.notNil and:{
					thisGuiEnv.editor.isClosed.not
				}, {
					thisGuiEnv.editor.connectorBut.value_(0);
					thisGuiEnv.editor.nameField.enabled_(true);
					thisGuiEnv.editor.inputConstraintLoField.value_(
						wcm.oscInputRange.model.value[0];
					);
					thisGuiEnv.editor.inputConstraintHiField.value_(
						wcm.oscInputRange.model.value[1];
					);
					if(prCalibrate.not, {
						[thisGuiEnv.editor.inputConstraintLoField, thisGuiEnv.editor.inputConstraintHiField].do(_.enabled_(true));
					});
					thisGuiEnv.editor.indexField.enabled_(true);
					thisGuiEnv.editor.connectorBut.value_(0);
				});
				thisGuiEnv.oscEditBut.refresh;
			})
		});
	}

}