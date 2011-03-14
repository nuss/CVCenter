
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

	// private
	prCCResponderAdd { |cv, learnBut, srcField, chanField, ctrlField, headBut, key| 
		var tmpcc;
		learnBut.action_({ |but|
			{
				loop {
					0.01.wait;
					if(but.value == 1, {
//						"adding a new CCResponder".postln;
						tmpcc = CCResponder({ |src, chan, ctrl, val|
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
						tmpcc.learn;
						key.switch(
							\hi, { this.ccHi = tmpcc },
							\lo, { this.ccLo = tmpcc },
							{ this.cc = tmpcc }
						);
//						oneShot = cc.oneShotLearn;
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
		^tmpcc;
	}

}