
CVWidget {

//	classvar <responderMsgIndices;
	var <>midimode = 0, <>midimean = 64, <>midistring = "", <>ctrlButtonBank, <>midiresolution = 1, <>softWithin = 0.1;
	var <>prCalibrate = true; // OSC-settings
	var visibleGuiEls, allGuiEls;
	var <>widgetBg, <>label, <>nameField; // elements contained in any kind of CVWidget
	var <visible, widgetXY, widgetProps;
//	var <>calibModel, <>specModel, <>oscInputRangeModel, <>oscConnectionModel;
	var <>controllersAndModels;

	setup {
		^[this.midimode, this.midiresolution, this.midimean, this.midistring, this.ctrlButtonBank, this.softwithin, this.prCalibrate];
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
//						"no CCResponder yet or just removing the existing one".postln;
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

}

CVWidgetKnob : CVWidget {

	var thisCV;
	var <>knob, <>numVal, <>specBut, <>midiHead, <>midiLearn, <>midiSrc, <>midiChan, <>midiCtrl;
	var <>cc, spec;
	var <>oscEditBut, <>calibBut, <>editor;
	var <>prOSCMapping = \linlin, <>calibConstraints, <>oscResponder;
	var <>mapConstrainterLo, <>mapConstrainterHi;
//	var <>oscInputRangeController;

	*new { |parent, cv, name, bounds, setUpArgs, controllersAndModels, cvcGui|
		^super.new.init(parent, cv, name, bounds.left@bounds.top, bounds.width, bounds.height, setUpArgs, controllersAndModels, cvcGui)
	}
	
	init { |parentView, cv, name, xy, widgetwidth=52, widgetheight=166, setUpArgs, controllersAndModels, cvcGui|
		var knobsize, meanVal, widgetSpecsActions, editor, cvString;
		var tmpSetup, tmpMapping;
		
		thisCV = cv;
		setUpArgs.isKindOf(Array).not.if { setUpArgs = [setUpArgs] };
		
		setUpArgs[0] !? { this.midimode_(setUpArgs[0]) };
		setUpArgs[1] !? { this.midiresolution_(setUpArgs[1]) };
		setUpArgs[2] !? { this.midimean_(setUpArgs[2]) };
		setUpArgs[3] !? { this.midistring_(setUpArgs[3].asString) };
		setUpArgs[4] !? { this.ctrlButtonBank_(setUpArgs[4]) };
		setUpArgs[5] !? { this.softWithin_(setUpArgs[5]) };
		setUpArgs[6] !? { this.calibrate_(setUpArgs[6]) };
		
		if(controllersAndModels.notNil, {
			this.controllersAndModels = controllersAndModels
		}, {
			this.controllersAndModels = ();
		});
		
		this.controllersAndModels.calibModelController ?? {
			this.controllersAndModels.calibModelController = ();
		};
		this.controllersAndModels.calibModelController.model ?? {
			this.controllersAndModels.calibModelController.model = Ref(this.prCalibrate);
		};
		this.controllersAndModels.specModelController ?? {
			this.controllersAndModels.specModelController = ();
		};
		this.controllersAndModels.specModelController.model ?? { 
			this.controllersAndModels.specModelController.model = Ref(this.spec);
		};
		this.controllersAndModels.oscInputRangeModelController ?? {
			this.controllersAndModels.oscInputRangeModelController = ();
		};
		this.controllersAndModels.oscInputRangeModelController.model ?? {
			this.controllersAndModels.oscInputRangeModelController.model = Ref([0.00001, 0.00001]);
		};
		this.controllersAndModels.oscConnectionModelController ?? {
			this.controllersAndModels.oscConnectionModelController = ();
		};
		this.controllersAndModels.oscConnectionModelController.model ?? {
			this.controllersAndModels.oscConnectionModelController.model = Ref(false);
		};
		
		cvcGui ?? { 
			parentView.onClose_({
				if(this.editor.notNil and:{
					this.editor.isClosed.not
				}, {
					this.editor.close;
				});
				this.controllersAndModels.do({ |mc| mc.controller.remove });
			})
		};
				
		this.mapConstrainterLo ?? { this.mapConstrainterLo_(CV([-inf, inf].asSpec, 0.0)) };
		this.mapConstrainterHi ?? { this.mapConstrainterHi_(CV([-inf, inf].asSpec, 0.0)) };
		
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
				if(this.editor.isNil or:{ this.editor.isClosed }, {
					this.editor_(CVWidgetEditor(this, name, 0))
				}, {
					this.editor.front(0)
				});
				this.controllersAndModels.oscConnectionModelController.model.value_(
					this.controllersAndModels.oscConnectionModelController.model.value
				).changed(\value);
			})
		;
		this.midiHead = Button(parentView, Rect(xy.x+1, xy.y+knobsize+43, widgetwidth-17, 15))
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.states_([["MIDI", Color.black, Color(alpha: 0)]])
			.action_({ |ms|
				if(this.editor.isNil or:{ this.editor.isClosed }, {
					this.editor_(CVWidgetEditor(this, name, 1))
				}, {
					this.editor.front(1)
				});
				this.controllersAndModels.oscConnectionModelController.model.value_(
					this.controllersAndModels.oscConnectionModelController.model.value;
				).changed(\value);
			})
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
		this.oscEditBut = Button(parentView, Rect(xy.x+1, xy.y+knobsize+82, widgetwidth-2, 30))
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.states_([
				["edit OSC", Color.black, Color.clear]
			])
			.action_({ |oscb|
				if(this.editor.isNil or:{ this.editor.isClosed }, {
					this.editor_(CVWidgetEditor(this, name, 2))
				}, {
					this.editor.front(2)
				});
				this.editor.calibNumBoxes !? {
					this.mapConstrainterLo.connect(this.editor.calibNumBoxes.lo);
					this.mapConstrainterHi.connect(this.editor.calibNumBoxes.hi);
				};
				this.controllersAndModels.oscConnectionModelController.model.value_(
					this.controllersAndModels.oscConnectionModelController.model.value;
				).changed(\value);
			})
		;
		this.calibBut = Button(parentView, Rect(xy.x+1, xy.y+knobsize+112, widgetwidth-2, 15))
			.font_(Font("Helvetica", 9))
			.focusColor_(Color(alpha: 0))
			.states_([
				["calibrating", Color.white, Color.red],
				["calibrate", Color.black, Color.green]
			])
		;
		
		this.controllersAndModels.calibModelController.controller ?? { 
			this.controllersAndModels.calibModelController.controller = SimpleController(this.controllersAndModels.calibModelController.model);
		};

		this.controllersAndModels.calibModelController.controller.put(\value, { |theChanger, what, moreArgs|
//			[theChanger.value, what, moreArgs, this.editor.calibBut, this.calibBut].postln;
			this.prCalibrate_(theChanger.value);
			theChanger.value.switch(
				true, { 
					this.calibBut.value_(0);
					if(this.editor.notNil and:{ this.editor.isClosed.not }, {
						this.editor.calibBut.value_(0);
						this.mapConstrainterLo ?? { 
							this.mapConstrainterLo_(CV([-inf, inf].asSpec, 0.0));
							this.mapConstrainterLo.connect(this.editor.calibNumBoxes.lo);
						};
						this.mapConstrainterHi ?? { 
							this.mapConstrainterHi_(CV([-inf, inf].asSpec, 0.0));
							this.mapConstrainterHi.connect(this.editor.calibNumBoxes.hi);
						};
						[this.editor.calibNumBoxes.lo, this.editor.calibNumBoxes.hi].do(_.enabled_(false))
					})
				},
				false, { 
					this.calibBut.value_(1);
					if(this.editor.notNil and:{ this.editor.isClosed.not }, {
						this.editor.calibBut.value_(1);
						[this.mapConstrainterLo, this.mapConstrainterHi].do({ |cv| cv = nil; });
						if(this.controllersAndModels.oscConnectionModelController.model.value == false, {
							[this.editor.calibNumBoxes.lo, this.editor.calibNumBoxes.hi].do(_.enabled_(true));
						})
					})
				}
			)
		});

		this.calibBut.action_({ |cb|
			cb.value.switch(
				0, { this.controllersAndModels.calibModelController.model.value_(true).changed(\value) },
				1, { this.controllersAndModels.calibModelController.model.value_(false).changed(\value) }
			)
		});
		
//		if(this.editor.notNil and:{
//			this.editor.isClosed.not	
//		}, {
//			this.editor.calibBut.action_({ |but|
//				but.value.switch(
//					0, { calibModel.value_(true).changed(\value) },
//					1, { calibModel.value_(false).changed(\value) }
//				)
//			})
//		});
				
		this.controllersAndModels.specModelController.controller ?? {
			this.controllersAndModels.specModelController.controller = SimpleController(this.controllersAndModels.specModelController.model);
		};
		
		this.controllersAndModels.specModelController.controller.put(\value, { |theChanger, what, moreArgs|
			if(theChanger.value.minval <= 0.0 or:{
				theChanger.value.maxval <= 0.0
			}, {
				if(this.prOSCMapping == \linexp or:{
					this.prOSCMapping == \expexp
				}, {
					this.prOSCMapping_(\linlin);
					if(this.editor.notNil and:{
						this.editor.isClosed.not
					}, {
						this.editor.mappingSelect.value_(0);
					})
				})
			}, {
				if(this.editor.notNil and:{
					this.editor.isClosed.not	
				}, {
					tmpMapping = this.editor.mappingSelect.item;
					this.editor.mappingSelect.items.do({ |item, i|
						if(item == tmpMapping, {
							this.editor.mappingSelect.value_(i)
						})
					})
				})
			});
			thisCV.spec_(theChanger.value);
			block { |break|
				#[\pan, \boostcut, \bipolar, \detune].do({ |symbol| 
					if(thisCV.spec == symbol.asSpec, { 
						break.value(this.knob.centered_(true));
					}, {
						this.knob.centered_(false);
					})			
				})
			}
		});
		
		this.controllersAndModels.oscInputRangeModelController.controller ?? {
			this.controllersAndModels.oscInputRangeModelController.controller = SimpleController(this.controllersAndModels.oscInputRangeModelController.model);
		};

		this.controllersAndModels.oscInputRangeModelController.controller.put(\value, { |theChanger, what, moreArgs|
			if(theChanger.value[0] <= 0 or:{
				theChanger.value[1] <= 0
			}, {
				if(this.prOSCMapping == \explin or:{
					this.prOSCMapping == \expexp
				}, {
					this.prOSCMapping_(\linlin)
				});
				if(this.editor.notNil and:{
					this.editor.isClosed.not
				}, {
					{	
						this.oscEditBut.states_([[
							this.oscEditBut.states[0][0].split($\n)[0]++"\n"++this.prOSCMapping.asString,
							this.oscEditBut.states[0][1],
							this.oscEditBut.states[0][2]
						]]);
						this.oscEditBut.refresh;
						this.editor.mappingSelect.value_(0);
					}.defer
				})		
			}, {
				if(this.editor.notNil and:{
					this.editor.isClosed.not	
				}, {
					{
						this.editor.mappingSelect.items.do({ |item, i|
							if(item.asSymbol === this.prOSCMapping, {
								this.editor.mappingSelect.value_(i)
							})
						});
					}.defer;
					if(this.oscEditBut.states[0][0].split($\n)[0] != "edit OSC", {
						{
							this.oscEditBut.states_([[
								this.oscEditBut.states[0][0].split($\n)[0]++"\n"++this.prOSCMapping.asString,
								this.oscEditBut.states[0][1],
								this.oscEditBut.states[0][2]
							]]);
							this.oscEditBut.refresh;
						}.defer
					})
				})
			})
		});

		this.controllersAndModels.oscConnectionModelController.controller ?? {
			this.controllersAndModels.oscConnectionModelController.controller = SimpleController(this.controllersAndModels.oscConnectionModelController.model);
		};

		this.controllersAndModels.oscConnectionModelController.controller.put(\value, { |theChanger, what, moreArgs|
			if(theChanger.value.size == 2, {
				this.oscResponder = OSCresponderNode(nil, theChanger.value[0].asSymbol, { |t, r, msg|
//					[t, r, msg, theChanger.value, this.controllersAndModels.oscConnectionModelController.model.value].postln;
					if(this.prCalibrate, { 
						if(calibConstraints.isNil, {
							calibConstraints = (lo: msg[theChanger.value[1]], hi: msg[theChanger.value[1]]);
						}, {
							if(msg[theChanger.value[1]] < calibConstraints.lo, { 
								calibConstraints.lo = msg[theChanger.value[1]];
								this.controllersAndModels.oscInputRangeModelController.model.value_([
									msg[theChanger.value[1]], 
									this.controllersAndModels.oscInputRangeModelController.model.value[1]
								]).changed(\value);
							});
							if(msg[theChanger.value[1]] > calibConstraints.hi, {
								calibConstraints.hi = msg[theChanger.value[1]];
								this.controllersAndModels.oscInputRangeModelController.model.value_([
									this.controllersAndModels.oscInputRangeModelController.model.value[0], 
									msg[theChanger.value[1]]
								]).changed(\value);
							});
						});
						this.mapConstrainterLo.value_(calibConstraints.lo);
						this.mapConstrainterHi.value_(calibConstraints.hi);
					}, {
						if(calibConstraints.isNil, {
		//					calibConstraints = (lo: 0, hi: 0);
							calibConstraints = (lo: this.controllersAndModels.oscInputRangeModelController.model.value[0], hi: this.controllersAndModels.oscInputRangeModelController.model.value[1]);
						}/*, {
							this.calibConstraints.lo = this.editor.calibNumBoxes.lo.value;
							this.calibConstraints.hi = this.editor.calibNumBoxes.hi.value;
						}*/)	
					});
					thisCV.value_(
						msg[theChanger.value[1]].perform(
							this.prOSCMapping,
							calibConstraints.lo, calibConstraints.hi,
							this.spec.minval, this.spec.maxval,
							\minmax
						)
					)
				}).add;
				this.oscEditBut.states_([
					[theChanger.value[0].asString++"["++theChanger.value[1].asString++"]"++"\n"++this.prOSCMapping.asString, Color.white, Color.cyan(0.5)]
				]);
				if(this.editor.notNil and:{
					this.editor.isClosed.not
				}, {
					this.editor.nameField.enabled_(false).string_(theChanger.value[0].asString);
					if(this.prCalibrate, {
						[this.editor.inputConstraintLoField, this.editor.inputConstraintHiField].do(_.enabled_(false));
					});
					this.editor.indexField.value_(theChanger.value[1]).enabled_(false);
					this.editor.connectorBut.value_(1);
				});
				this.oscEditBut.refresh;
			});
			if(theChanger.value == false, {
				this.oscResponder.remove;
				this.oscEditBut.states_([
					["edit OSC", Color.black, Color.clear]
				]);
				this.controllersAndModels.oscInputRangeModelController.model.value_([0.00001, 0.00001]).changed(\value);
				this.calibConstraints_(nil);
				if(this.editor.notNil and:{
					this.editor.isClosed.not
				}, {
//					this.editor.nameField.enabled_(true).string_("/my/typetag");
					this.editor.nameField.enabled_(true);
					this.editor.inputConstraintLoField.value_(
						this.controllersAndModels.oscInputRangeModelController.model.value[0];
					);
					this.editor.inputConstraintHiField.value_(
						this.controllersAndModels.oscInputRangeModelController.model.value[1];
					);
					if(this.prCalibrate.not, {
						[this.editor.inputConstraintLoField, this.editor.inputConstraintHiField].do(_.enabled_(true));
					});
//					this.editor.indexField.value_(1).enabled_(true);
					this.editor.indexField.enabled_(true);
					this.editor.connectorBut.value_(0);
				});
				this.oscEditBut.refresh;
//				("removing the responder done:"+this.controllersAndModels.oscConnectionModelController.model).postln;
			});
		});
		
//		this.oscEditBut.onClose_({ 
////			if(this.oscResponder.value == false and:{
////				OSCresponder.all.asArray.select({ |resp| resp.cmdName == this.oscResponder.cmdName }).size < 2
////			}, {
//				this.controllersAndModels.controllersAndModels.oscConnectionModelController.modelController.controller.remove;
//				this.controllersAndModels.oscConnectionModelController.controller.remove;
////			})
//		});
////		if(this.oscResponder.value == false and:{
////			OSCresponder.all.asArray.select({ |resp| resp.cmdName == this.oscResponder.cmdName }).size < 2
////		}, {
//			this.specBut.onClose_({ this.controllersAndModels.specModelController.controller.remove });
//			this.calibBut.onClose_({ this.controllersAndModels.calibModelController.controller.remove });
////		});
		
		this.prCCResponderAdd(cv, this.midiLearn, this.midiSrc, this.midiChan, this.midiCtrl, this.midiHead);
		
		[this.knob, this.numVal].do({ |view| cv.connect(view) });
		visibleGuiEls = [this.knob, this.numVal, this.specBut, this.midiHead, this.midiLearn, this.midiSrc, this.midiChan, this.midiCtrl, this.oscEditBut, this.calibBut];
		allGuiEls = [this.widgetBg, this.label, this.nameField, this.knob, this.numVal, this.specBut, this.midiHead, this.midiLearn, this.midiSrc, this.midiChan, this.midiCtrl, this.oscEditBut, this.calibBut]
	}
	
	calibrate_ { |bool|
		if(bool.isKindOf(Boolean).not, {
			Error("calibration can only be set to true or false!").throw;
		});
		this.controllersAndModels.calibModelController.model.value_(bool).changed(\value);
	}
	
	calibrate {
		^this.prCalibrate;
	}
	
	spec_ { |spec|
		if(spec.isKindOf(ControlSpec).not, {
			Error("Please provide a valid ControlSpec!").throw;
		});
		this.controllersAndModels.specModelController.model.value_(spec).changed(\value);
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
			this.prOSCMapping_(mapping.asSymbol);
			this.controllersAndModels.oscInputRangeModelController.model.value_(
				this.controllersAndModels.oscInputRangeModelController.model.value;
			).changed(\value);
			this.controllersAndModels.specModelController.model.value_(
				this.controllersAndModels.specModelController.model.value;
			).changed(\value);
		})
	}
	
	oscMapping {
		^this.prOSCMapping;	
	}
	
	oscConnect { |name, oscMsgIndex|
//		var netAddr;
//		[addr.class, port, name, oscMsgIndex].postln;
//		if(addr.size > 0 and:{ addr != "nil" }, {
//			if("^([0-9]{1,3})\.([0-9]{1,3})\.([0-9]{1,3})\.([0-9]{1,3})$".matchRegexp(addr.asString).not and:{
//				port.isKindOf(Integer).not
//			}, {
//				Error("You have to supply a valid IP-address and an integer as port-number if the address is not set to nil").throw;
//			}, {
//				netAddr = NetAddr(addr.asString, port);
//			})
//		});
		if("^\/".matchRegexp(name.asString).not, {
			Error("You have to supply a valid OSC-typetag, beginning with an \"/\" as second argument to oscConnect").throw;
		});
		if(oscMsgIndex.isKindOf(Integer).not, {
			Error("You have to supply an integer as third argument to oscConnect").throw;
		});
		this.controllersAndModels.oscConnectionModelController.model.value_([name, oscMsgIndex]).changed(\value);
	}
	
	oscResponderRemove {
		this.controllersAndModels.oscConnectionModelController.model.value_(false).changed(\value);
		this.controllersAndModels.oscInputRangeModelController.model.value_([0.00001, 0.00001]).changed(\value);
	}
	
}

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
		setUpArgs[6] !? { this.prCalibrate_(setUpArgs[6]) };

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
	
	oscConnect { |addr=nil, name, oscMsgIndex, hilo|
		hilo ?? { Error("Please provide the CV's key \('hi' or 'lo')!").throw };
		if(hilo.asSymbol === \lo, {
			this.oscResponderLo = OSCresponderNode(addr, name.asSymbol, { |t, r, msg|
				if(this.prCalibrate, { 
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
				if(this.prCalibrate, { 
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
	
	oscResponderRemove { |hilo|
		hilo ?? { Error("Please provide the CV's key \(\hi or \lo\)!").throw };
		if(hilo.asSymbol === \hi, {
			this.oscResponderHi.remove;
//			this.oscInputRangeModelHi_(`[0.0, 0.0]);
			this.calibConstraintsHi_(nil);	
		});
		if(hilo.asSymbol === \lo, {
			this.oscResponderLo.remove;
//			this.oscInputRangeModelLo_(`[0.0, 0.0]);
			this.calibConstraintsLo_(nil);	
		})
	}
	
}