CVWidgetMS : CVWidget {
	var <msSize, <mSlider, <calibViews, <numVal, <midiBut, <oscBut, <specBut, <actionsBut;
	var numOscResponders, numMidiResponders;
	var <msEditor;
	// persistent widgets
	var isPersistent, oldBounds, oldName;

	*new { |parent, cv, name, bounds, defaultAction, setup, controllersAndModels, cvcGui, persistent, numSliders=5, server|
		^super.new.init(
			parent,
			cv,
			name,
			bounds,
			defaultAction,
			setup,
			controllersAndModels,
			cvcGui,
			persistent,
			numSliders,
			server // swing compatibility. well, ...
		)
	}

	init { |parentView, cv, name, bounds, action, setupArgs, controllersAndModels, cvcGui, persistent, numSliders, server|
		var thisName, thisXY, thisX, thisY, thisWidth, thisHeight, knobsize, widgetSpecsActions;
		// hmmm...
		var msrc = "source", mchan = "chan", mctrl = "ctrl", margs;
		var nextX, nextY, knobX, knobY;
		var calibViewsWidth, calibViewsNextX;
		var text;

		this.bgColor ?? { this.bgColor = Color.white };
		synchKeys ?? { synchKeys = [\default] };
		#numOscResponders, numMidiResponders = 0!2;

		calibViews = [];

		if(cv.isNil, {
			widgetCV = CV([0 ! numSliders, 1 ! numSliders]);
		}, {
			widgetCV = cv;
		});

		if(widgetCV.spec.minval.isArray.not and:{
			widgetCV.spec.maxval.isArray.not and:{
				widgetCV.spec.step.isArray.not and:{
					widgetCV.spec.default.isArray.not
				}
			}
		}, {
			widgetCV.spec_(ControlSpec(
				widgetCV.spec.minval ! numSliders,
				widgetCV.spec.maxval ! numSliders,
				widgetCV.spec.warp,
				widgetCV.spec.step ! numSliders,
				widgetCV.spec.default ! numSliders,
				widgetCV.spec.units
			))
		});

		msSize = [
			widgetCV.spec.minval.size,
			widgetCV.spec.maxval.size,
			widgetCV.spec.step.size,
			widgetCV.spec.default.size
		].maxItem;

//		"msSize: %\n".postf(msSize);

		prCalibrate = true ! msSize;
		prMidiMode = 0 ! msSize;
		prMidiMean = 64 ! msSize;
		prMidiResolution = 1 ! msSize;
		prSoftWithin = 0.1 ! msSize;
		prCtrlButtonBank = nil ! msSize;

//		"setup: %\n".postf([prCalibrate, prMidiMode, prMidiMean, prMidiResolution, prSoftWithin]);

		guiEnv = ();
		cvcGui !? { isCVCWidget = true };

		if(cvcGui.class == Event and:{ cvcGui.midiOscEnv.notNil }, {
			midiOscEnv = cvcGui.midiOscEnv
		}, {
			midiOscEnv = ()!msSize
		});

		msSize.do({ |i|
			midiOscEnv[i].oscMapping ?? { midiOscEnv[i].oscMapping = \linlin };
		});

		if(name.isNil, { thisName = "multislider" }, { thisName = name });
		wdgtInfo = thisName.asString;

//		what's the editor gonna look like?
		editor = (editors: Array.newClear(msSize));

//		TO DO
		// "wdgtControllersAndModels before init: %\n".postf(wdgtControllersAndModels);
		msSize.do(this.initControllersAndModels(controllersAndModels, _));
		// "wdgtControllersAndModels after init: %\n".postf(wdgtControllersAndModels);

		setupArgs !? {
			msSize.do({ |slot|
				setupArgs[slot] !? { setupArgs[slot][\midiMode] !? { this.setMidiMode(setupArgs[slot][\midiMode], slot) }};
				setupArgs[slot] !? { setupArgs[slot][\midiResolution] !? { this.setMidiResolution(setupArgs[slot][\midiResolution], slot) }};
				setupArgs[slot] !? { setupArgs[slot][\midiMean] !? { this.setMidiMean(setupArgs[slot][\midiMean], slot) }};
				setupArgs[slot] !? { setupArgs[slot][\ctrlButtonBank] !? { this.setCtrlButtonBank(setupArgs[slot][\ctrlButtonBank], slot) }};
				setupArgs[slot] !? { setupArgs[slot][\softWithin] !? { this.setSoftWithin(setupArgs[slot][\softWithin], slot) }};
				setupArgs[slot] !? { setupArgs[slot][\calibrate] !? { this.setCalibrate(setupArgs[slot][\calibrate], slot) }};
			})
		};

		action !? { this.addAction(\default, action) };

		if(bounds.isNil, {
			thisXY = 7@0;
			thisX = 50; thisY = 50;
			thisWidth = 122;
			thisHeight = 196;
		}, {
			if(parentView.isNil, { thisXY = 7@0 }, { thisXY = bounds.left@bounds.top });
			thisX = bounds.left; thisY = bounds.top;
			thisWidth = bounds.width;
			thisHeight = bounds.height;
		});

		if(parentView.isNil, {
			window = Window(thisName, Rect(thisX, thisY, thisWidth+14, thisHeight+7), server: server);
		}, {
			window = parentView;
		});

		cvcGui ?? {
			window.onClose_({
				msSize.do({ |slot|
					if(editor.editors[slot].notNil, {
						if(editor.editors[slot].isClosed.not, {
							editor.editors[slot].close(slot);
						}, {
							if(CVWidgetEditor.allEditors.notNil and:{
								CVWidgetEditor.allEditors[thisName.asSymbol].notNil
							}, {
								CVWidgetEditor.allEditors[thisName.asSymbol].removeAt(slot);
								if(CVWidgetEditor.allEditors[thisName.asSymbol].isEmpty, {
									CVWidgetEditor.allEditors.removeAt(thisName.asSymbol);
								})
							})
						})
					})
				});
				editor.msEditor !? {
					if(editor.msEditor.isClosed.not, {
						editor.msEditor.close;
					}, {
						if(CVWidgetMSEditor.allEditors.notNil and:{
							CVWidgetMSEditor.allEditors[(thisName.asString++"MS").asSymbol].notNil
						}, {
							if(CVWidgetMSEditor.allEditors[(thisName.asString++"MS").asSymbol].isEmpty, {
								CVWidgetMSEditor.allEditors.removeAt((thisName.asString++"MS").asSymbol);
							})
						})
					})
				}
			})
		};

		cvcGui ?? {
			if(persistent == false or:{ persistent.isNil }, {
				window.onClose_(window.onClose.addFunc({
					msSize.do({ |slot|
						midiOscEnv[slot].oscResponder !? { midiOscEnv[slot].oscResponder.remove };
						midiOscEnv[slot].cc !? { midiOscEnv[slot].cc.remove };
						wdgtControllersAndModels[slot].do({ |mc| mc.isKindOf(SimpleController).if{ mc.controller.remove } });
					})
				}))
			}, {
				isPersistent = true;
			})
		};

		persistent !? { if(persistent, { isPersistent = true }) };

		widgetBg = UserView(window, Rect(thisXY.x, thisXY.y, thisWidth, thisHeight))
//			.focusColor_(Color(alpha: 1.0))
			.background_(this.bgColor)
		;
		label = Button(window, Rect(thisXY.x+1, thisXY.y+1, thisWidth-2, 15))
			.states_([
				[""+thisName.asString, Color.white, Color.blue],
				[""+thisName.asString, Color.black, Color.yellow],
			])
			.font_(Font("Arial", 9))
			.action_({ |b|
				this.toggleComment(b.value.asBoolean);
			})
		;
		nameField = TextView(window, Rect(label.bounds.left, label.bounds.top+label.bounds.height, thisWidth-2, thisHeight-label.bounds.height-2))
			.background_(Color.white)
			.font_(Font("Arial", 9))
			.string_(wdgtInfo)
			.visible_(false)
			.keyUpAction_({ wdgtInfo = nameField.string })
		;

		mSlider = MultiSliderView(window, Rect(thisXY.x+1, thisXY.y+16, thisWidth-2, thisHeight-2-79-1))
			.drawRects_(true)
			.isFilled_(true)
			.colors_(Color.clear, Color.blue)
			.elasticMode_(1)
		;

		nextY = thisXY.y+mSlider.bounds.height+label.bounds.height+1;

		calibViewsWidth = (thisWidth-2).div(msSize);
		calibViewsNextX = thisXY.x+1;
		msSize.do({ |sl|
			calibViews = calibViews.add(
				UserView(window, Rect(calibViewsNextX, nextY, calibViewsWidth, 2)).background_(Color.green);
			);
			calibViewsNextX = calibViewsNextX+calibViewsWidth;
			if(GUI.id !== \cocoa, {
				if(this.getCalibrate(sl), {
					text = "Calibration for slot % is active.".format(sl);
				}, {
					text = "Calibration for slot % is inactive.".format(sl);
				});
				calibViews[sl].toolTip_(text);
			})
		});

		nextY = nextY+2+1;

		numVal = TextView(window, Rect(thisXY.x+1, nextY, thisWidth-2, 30))
			.string_(widgetCV.value.asCompileString).font_(Font("Arial", 9.5))
			.keyDownAction_({ |nv, char, modifiers, unicode, keycode|
				if(char == $\r and:{ modifiers == 131072 }, {
					if(nv.string.interpret.class == Array and:{
						nv.string.interpret.select(_.isNumber).size == mSlider.size
					}, { nv.doAction })
				})
			})
		;

		nextY = thisXY.y+numVal.bounds.top+numVal.bounds.height+1;

		midiBut = Button(window, Rect(thisXY.x+1, nextY, thisWidth-2/2, 15))
			.states_([
				["MIDI"+"("++numMidiResponders++"/"++msSize++")", Color.black, this.bgColor]
			])
			.font_(Font("Arial", 9))
			.action_({ |mb|
				if(editor.msEditor.isNil or:{ editor.msEditor.isClosed }, {
					editor.msEditor = CVWidgetMSEditor(this, thisName, 1);
					guiEnv.msEditor = editor.msEditor;
				}, {
					editor.msEditor.front(1)
				});
				if(midiOscEnv.select({ |slot| slot.cc.notNil }).size > 0, {
					editor.msEditor.midiTabs.focus(1)
				}, {
					editor.msEditor.midiTabs.focus(0)
				});
				msSize.do({ |i|
					wdgtControllersAndModels.slots[i].oscDisplay.model.value_(
						wdgtControllersAndModels.slots[i].oscDisplay.model.value;
					).changedKeys(synchKeys);
					wdgtControllersAndModels.slots[i].midiDisplay.model.value_(
						wdgtControllersAndModels.slots[i].midiDisplay.model.value
					).changedKeys(synchKeys);
				})
			})
		;

		if(GUI.id === \qt, {
			midiBut.mouseEnterAction_({ |mb|
				if(wdgtControllersAndModels.slots.select({ |slot| slot.midiConnection.model.value.isNil }).size == msSize, {
					mb.states_([[mb.states[0][0], Color.white, Color.red]])
				})
			}).mouseLeaveAction_({ |mb|
				if(wdgtControllersAndModels.slots.select({ |slot| slot.midiConnection.model.value.isNil }).size == msSize, {
					mb.states_([[mb.states[0][0], Color.black, this.bgColor]])
				})
			})
		});

		nextX = thisXY.x+1+midiBut.bounds.width;
		oscBut = Button(window, Rect(nextX, nextY, thisWidth-2/2, 15))
			.states_([
				["OSC"+"("++numOscResponders++"/"++msSize++")", Color.black, this.bgColor]
			])
			.font_(Font("Arial", 9))
			.action_({ |oscb|
				if(editor.msEditor.isNil or:{ editor.msEditor.isClosed }, {
					editor.msEditor = CVWidgetMSEditor(this, thisName, 2);
					guiEnv.msEditor = editor.msEditor;
				}, {
					editor.msEditor.front(2)
				});
				if(midiOscEnv.select({ |slot| slot.oscResponder.notNil }).size > 0, {
					editor.msEditor.oscTabs.focus(1)
				}, {
					editor.msEditor.oscTabs.focus(0)
				});
				msSize.do({ |i|
					// [i, wdgtControllersAndModels.slots[i]].postln;
					wdgtControllersAndModels.slots[i].oscDisplay.model.value_(
						wdgtControllersAndModels.slots[i].oscDisplay.model.value;
					).changedKeys(synchKeys);
					wdgtControllersAndModels.slots[i].midiDisplay.model.value_(
						wdgtControllersAndModels.slots[i].midiDisplay.model.value
					).changedKeys(synchKeys);
				})
			})
		;

		if(GUI.id === \qt, {
			oscBut.mouseEnterAction_({ |oscb|
				// "oscb.states[0][0] on enter: %\n".postf(oscb.states[0][0]);
				if(wdgtControllersAndModels.slots.select({ |slot| slot.oscConnection.model.value == false }).size == msSize, {
					oscb.states_([[oscb.states[0][0], Color.white, Color.cyan(0.5)]])
				})
			}).mouseLeaveAction_({ |oscb|
				// "oscb.states[0][0] on leave: %\n".postf(oscb.states[0][0]);
				if(wdgtControllersAndModels.slots.select({ |slot| slot.oscConnection.model.value == false }).size == msSize, {
					oscb.states_([[oscb.states[0][0], Color.black, this.bgColor]])
				})
			})
		});

		nextY = nextY+oscBut.bounds.height;
		specBut = Button(window, Rect(thisXY.x+1, nextY, thisWidth-2/2, 15))
			.states_([
				["Spec", Color.white, Color(1.0, 0.3)]
			])
			.font_(Font("Arial", 9))
			.action_({ |spb|
				if(editor.msEditor.isNil or:{ editor.msEditor.isClosed }, {
					editor.msEditor = CVWidgetMSEditor(this, thisName, 0);
					guiEnv.msEditor = editor.msEditor;
				}, {
					editor.msEditor.front(0)
				})
			})
		;
		nextX = nextX+specBut.bounds.width;
		actionsBut = Button(window, Rect(thisXY.x+1+specBut.bounds.width, nextY, thisWidth-2/2, 15))
			.states_([
				["Actions ("++this.wdgtActions.select({ |v| v.asArray[0][1] == true }).size++"/"++this.wdgtActions.size++")", Color(0.08, 0.09, 0.14), Color(0.32, 0.67, 0.76)]
			])
			.font_(Font("Arial", 9))
			.action_({ |spb|
				if(editor.msEditor.isNil or:{ editor.msEditor.isClosed }, {
					editor.msEditor = CVWidgetMSEditor(this, thisName, 3);
					guiEnv.msEditor = editor.msEditor;
				}, {
					editor.msEditor.front(3)
				})
			})
		;

		visibleGuiEls = [
			mSlider,
			calibViews,
			numVal,
			midiBut,
			specBut,
			oscBut,
			actionsBut
		];

		allGuiEls = [
			widgetBg,
			label,
			nameField,
			mSlider,
			calibViews,
			numVal,
			midiBut,
			oscBut,
			specBut,
			actionsBut
		];

//		msSize.do({ |slot|
			guiEnv = (
			// msEditor: editor.msEditor,
				editor: editor.editors,
				mSlider: mSlider,
				calibViews: calibViews,
				numVal: numVal,
				midiBut: midiBut,
				oscBut: oscBut,
				specBut: specBut,
				actionsBut: actionsBut
			);
//			this.initControllerActions(slot);
//		});

		msSize.do({ |slot| this.initControllerActions(slot) });

		widgetCV.connect(mSlider);
		widgetCV.connect(numVal);

		oldBounds = window.bounds;
		if(window.respondsTo(\name), { oldName = window.name });
	}
}