CVWidgetEditor {
	classvar <allEditors;
	var thisEditor, <window, <tabs;
	var <specField, <specsList, <specsListSpecs;
	var <midiModeSelect, <midiMeanNB, <softWithinNB, <ctrlButtonBankField, <midiResolutionNB;
	var <midiLearnBut, <midiSrcField, <midiChanField, <midiCtrlField;
	var <calibBut, <calibNumBoxes;
	var <ipField, <portField, <nameField, <indexField;
	var <inputConstraintLoField, <inputConstraintHiField;
	var <mappingSelect;
	var <connectorBut;
	var <name;
	
	*new { |widget, widgetName, tab, key|
		^super.new.init(widget, widgetName, tab, key)
	}

	init { |widget, widgetName, tab, key|
		var flow0, flow1, flow2;
		var tabs, /*specsActions, editor, */cvString, slotHiLo;
		var staticTextFont, staticTextColor, textFieldFont, textFieldFontColor, textFieldBg;
		var msrc = "source", mchan = "chan", mctrl = "ctrl", margs;
		var addr, wcmHiLo, thisGuiEnv; 
		var midiModes;
		var thisMidiMode, thisMidiMean, thisMidiResolution, thisSoftWithin, thisCtrlButtonBank;
		var mappingSelectItems/*, mappingModes*/;
		var tmp; // multipurpose, short-term var
						
		name = widgetName.asSymbol;
		
		if(key.isNil, { thisGuiEnv = widget.guiEnv }, { thisGuiEnv = widget.guiEnv[key] });

		widget ?? {
			Error("CVWidgetEditor is a utility-GUI-class that should only be used in connection with an existing CVWidget").throw;
		};

		if(key.notNil, {
			if(widget.wdgtControllersAndModels[key].notNil, { 
				wcmHiLo = widget.wdgtControllersAndModels[key];
			});
			thisMidiMode = widget.getMidiMode(key);
			thisMidiMean = widget.getMidiMean(key);
			thisMidiResolution = widget.getMidiResolution(key);
			thisSoftWithin = widget.getSoftWithin(key);
			thisCtrlButtonBank = widget.getCtrlButtonBank(key);
		}, { 
			if(widget.wdgtControllersAndModels.notNil, { 
				wcmHiLo = widget.wdgtControllersAndModels;
			});
			thisMidiMode = widget.getMidiMode;
			thisMidiMean = widget.getMidiMean;
			thisMidiResolution = widget.getMidiResolution;
			thisSoftWithin = widget.getSoftWithin;
			thisCtrlButtonBank = widget.getCtrlButtonBank;
		});
						

		staticTextFont = Font(Font.defaultSansFace, 10);
		staticTextColor = Color(0.2, 0.2, 0.2);
		textFieldFont = Font(Font.defaultMonoFace, 9);
		textFieldFontColor = Color.black;
		textFieldBg = Color.white;
		
		if(key.notNil, {
			slotHiLo = "["++key.asString++"]";
		}, {
			slotHiLo = "";
		});
		
		allEditors ?? { allEditors = IdentityDictionary() };
		
		if(thisEditor.isNil or:{ thisEditor.window.isClosed }, {
			window = Window("Widget Editor:"+widgetName++slotHiLo, Rect(Window.screenBounds.width/2-150, Window.screenBounds.height/2-100, 270, 225));

			if(key.isNil, { 
				allEditors.put(name, (window: window, name: widgetName)) 
			}, {
				tmp = (); tmp.put(key, (window: window, name: widgetName));
				if(allEditors[name].isNil, { 
					allEditors.put(name, tmp);
				}, { 
					allEditors[name].put(key, (window: window, name: widgetName));
				});
			});
			
			if(key.notNil, { thisEditor = allEditors[name][key] }, { thisEditor = allEditors[name] });

			if(Quarks.isInstalled("wslib"), { window.background_(Color.white) });
			tabs = TabbedView(window, Rect(0, 0, window.bounds.width, window.bounds.height), ["Specs", "MIDI", "OSC"], scroll: true);
			thisEditor.tabs = tabs;
			thisEditor.tabs.view.resize_(5);
			thisEditor.tabs.stringFocusedColor_(Color.blue);
			
			thisEditor.tabs.views[0].decorator = flow0 = FlowLayout(window.view.bounds, 7@7, 3@3);
			thisEditor.tabs.views[1].decorator = flow1 = FlowLayout(window.view.bounds, 7@7, 3@3);
			thisEditor.tabs.views[2].decorator = flow2 = FlowLayout(window.view.bounds, 7@7, 3@3);
						
			StaticText(thisEditor.tabs.views[0], flow0.bounds.width-20@95)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("Enter a ControlSpec in the textfield:\ne.g. ControlSpec(20, 20000, \\exp, 0.0, 440, \"Hz\")\nor \\freq.asSpec \nor [20, 20000, \\exp].asSpec.\nOr select a suitable ControlSpec from the List below.\nIf you don't know what this all means have a look\nat the ControlSpec-helpfile.")
			;

			cvString = widget.getSpec(key).asString.split($ );

			cvString = cvString[1..cvString.size-1].join(" ");
			
			specField = TextField(thisEditor.tabs.views[0], flow0.bounds.width-20@15)
				.font_(staticTextFont)
				.string_(cvString)
				.action_({ |tf|
					widget.setSpec(tf.string.interpret, key)
				})
			;
			
			flow0.shift(0, 5);

			specsList = PopUpMenu(thisEditor.tabs.views[0], flow0.bounds.width-20@20)
				.action_({ |sl|
					widget.setSpec(specsListSpecs[sl.value], key);
				})
			;
			
			if(widget.editorEnv.specsListSpecs.isNil, { 
				specsListSpecs = List() 
			}, {
				specsListSpecs = widget.editorEnv.specsListSpecs;
			});
			
			if(widget.editorEnv.specsListItems.notNil, {
				specsList.items_(widget.editorEnv.specsListItems);
			}, {
				Spec.specs.pairsDo({ |k, v|
					if(v.isKindOf(ControlSpec), {
						specsList.items_(specsList.items.add(k++":"+v));
						specsListSpecs.add(v);
					})
				})
			});
						
			tmp = specsListSpecs.detectIndex({ |spec, i| spec == widget.getSpec(key) });
			if(tmp.notNil, {
				specsList.value_(tmp);
			}, {
				specsListSpecs.array_([widget.getSpec(key)]++specsListSpecs.array);
				specsList.items = List["custom:"+widget.getSpec(key).asString]++specsList.items;
			});
			
			window.onClose_({
				widget.editorEnv.specsListSpecs = specsListSpecs;
				widget.editorEnv.specsListItems = specsList.items;
			});
						
			// MIDI editing
						
			StaticText(thisEditor.tabs.views[1], flow1.bounds.width/2+40@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("MIDI-mode: 0-127 or in/decremental")
			;
			
			flow1.shift(5, 0);
			
			midiModes = ["0-127", "+/-"];
						
			midiModeSelect = PopUpMenu(thisEditor.tabs.views[1], flow1.bounds.width/2-70@15)
				.font_(staticTextFont)
				.items_(midiModes)
				.value_(thisMidiMode)
				.action_({ |ms|
					widget.setMidiMode(ms.value, key);
				})
			;
			
			StaticText(thisEditor.tabs.views[1], flow1.bounds.width/2+60@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("MIDI-mean (in/decremental mode only)")
			;
			
			flow1.shift(5, 0);
			
			midiMeanNB = NumberBox(thisEditor.tabs.views[1], flow1.bounds.width/2-90@15)
				.font_(staticTextFont)
				.value_(thisMidiMean)
				.action_({ |mb|
					widget.setMidiMean(mb.value, key);
				})
				.step_(1.0)
				.clipLo_(0.0)
			;
			
			StaticText(thisEditor.tabs.views[1], flow1.bounds.width/2+60@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("minimum distance for the slider (0-127 only)")
			;
			
			flow1.shift(5, 0);
			
			softWithinNB = NumberBox(thisEditor.tabs.views[1], flow1.bounds.width/2-90@15)
				.font_(staticTextFont)
				.value_(thisSoftWithin)
				.action_({ |mb|
					widget.setSoftWithin(mb.value, key);
				})
				.step_(0.005)
				.clipLo_(0.01)
				.clipHi_(0.5)
			;
			
			StaticText(thisEditor.tabs.views[1], flow1.bounds.width/2+60@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("MIDI-resolution (+/- only)")
			;
			
			flow1.shift(5, 0);

			midiResolutionNB = NumberBox(thisEditor.tabs.views[1], flow1.bounds.width/2-90@15)
				.font_(staticTextFont)
				.value_(thisMidiResolution)
				.action_({ |mb|
					widget.setMidiResolution(mb.value, key);
				})
				.step_(0.05)
				.clipLo_(0.001)
				.clipHi_(10.0)
			;
			
			StaticText(thisEditor.tabs.views[1], flow1.bounds.width/2+60@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("number of sliders per bank")
			;

			flow1.shift(5, 0);
			
			ctrlButtonBankField = TextField(thisEditor.tabs.views[1], flow1.bounds.width/2-90@15)
				.font_(staticTextFont)
				.string_(thisCtrlButtonBank)
				.action_({ |mb|
					if(mb.string != "nil", {
						widget.setCtrlButtonBank(mb.string.asInt, key);
					}, {
						widget.setCtrlButtonBank(nil);
					})
				})
			;
			
			flow1.shift(0, 10);
			
			StaticText(thisEditor.tabs.views[1], flow1.bounds.width-20@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("(learn | connect) / source-ID (device) / chan / ctrl-nr.")
			;

			midiLearnBut = Button(thisEditor.tabs.views[1], 15@15)
				.font_(staticTextFont)
				.states_([
					["L", Color.white, Color.blue],
					["X", Color.white, Color.red]
				])
				.action_({ |ml|
					ml.value.switch(
						1, {
							margs = [
								[thisGuiEnv.midiSrc.string, msrc], 
								[thisGuiEnv.midiChan.string, mchan], 
								[thisGuiEnv.midiCtrl.string, mctrl]
							].collect({ |pair| if(pair[0] != pair[1], { pair[0].asInt }, { nil }) });
							if(margs.select({ |i| i.notNil }).size > 0, {
								widget.midiConnect(uid: margs[0], chan: margs[1], num: margs[2], key: key)
							}, {
								widget.midiConnect(key: key)
							})
						},
						0, { widget.midiDisconnect(key) }
					)
				})
			;
			
			flow1.shift(0, 0);
			
			midiSrcField = TextField(thisEditor.tabs.views[1], flow1.bounds.width-165@15)
				.font_(staticTextFont)
				.string_(msrc)
				.background_(Color.white)
				.action_({ |tf|
					if(tf.string != msrc, {
						wcmHiLo.midiDisplay.model.value_((
							learn: "C",
							src: tf.string,
							chan: wcmHiLo.midiDisplay.model.value.chan,
							ctrl: wcmHiLo.midiDisplay.model.value.ctrl
						)).changed(\value)
					})
				})
				.mouseDownAction_({ |tf|
					tf.stringColor_(Color.red)
				})
				.keyUpAction_({ |tf, char, modifiers, unicode, keycode|
					if(unicode == 13, {
						tf.stringColor_(Color.black);
					})
				}) 
			;
						
			flow1.shift(0, 0);
			
			midiChanField = TextField(thisEditor.tabs.views[1], 60@15)
				.font_(staticTextFont)
				.string_(mchan)
				.background_(Color.white)
				.action_({ |tf|
					if(tf.string != mchan, {
						wcmHiLo.midiDisplay.model.value_((
							learn: "C",
							src: wcmHiLo.midiDisplay.model.value.src,
							chan: tf.string,
							ctrl: wcmHiLo.midiDisplay.model.value.ctrl
						)).changed(\value)
					})
				})
				.mouseDownAction_({ |tf|
					tf.stringColor_(Color.red)
				})
				.keyUpAction_({ |tf, char, modifiers, unicode, keycode|
					if(unicode == 13, {
						tf.stringColor_(Color.black);
					})
				}) 
			;
			
			flow1.shift(0, 0);
			
			midiCtrlField = TextField(thisEditor.tabs.views[1], 60@15)
				.font_(staticTextFont)
				.string_(mctrl)
				.background_(Color.white)
				.action_({ |tf|
					if(tf.string != mctrl, {
						wcmHiLo.midiDisplay.model.value_((
							learn: "C",
							src: wcmHiLo.midiDisplay.model.value.src,
							chan: wcmHiLo.midiDisplay.model.value.chan,
							ctrl: tf.string
						)).changed(\value)
					})
				})
				.mouseDownAction_({ |tf|
					tf.stringColor_(Color.red)
				})
				.keyUpAction_({ |tf, char, modifiers, unicode, keycode|
					if(unicode == 13, {
						tf.stringColor_(Color.black);
					})
				}) 
			;
			
			// OSC editting
			
			StaticText(thisEditor.tabs.views[2], flow2.bounds.width-20@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("device-IP/port: leave empty for listening to any IP/port")
			;
			
			ipField = TextField(thisEditor.tabs.views[2], flow2.bounds.width-60@15)
				.font_(textFieldFont)
				.stringColor_(textFieldFontColor)
				.background_(textFieldBg)
				.string_("")
			;
			
			flow2.shift(5, 0);

			portField = TextField(thisEditor.tabs.views[2], 36@15)
				.font_(textFieldFont)
				.stringColor_(textFieldFontColor)
				.background_(textFieldBg)
				.string_("")
			;
				
			flow2.shift(0, 0);

			StaticText(thisEditor.tabs.views[2], flow2.bounds.width-20@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("OSC-typetag, e.g.: /my/typetag / OSC message index")
			;
	
			nameField = TextField(thisEditor.tabs.views[2], flow2.bounds.width-60@15)
				.font_(textFieldFont)
				.stringColor_(textFieldFontColor)
				.background_(textFieldBg)
				.string_("/my/typetag")
			;
						
			flow2.shift(5, 0);
			
			indexField = NumberBox(thisEditor.tabs.views[2], 36@15)
				.font_(textFieldFont)
				.normalColor_(textFieldFontColor)
				.clipLo_(1)
				.clipHi_(inf)
				.shift_scale_(1)
				.ctrl_scale_(1)
				.alt_scale_(1)
				.value_(1)
			;
			
			flow2.shift(0, 0);
	
			StaticText(thisEditor.tabs.views[2], flow2.bounds.width-15@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("lower and upper constraints of the OSC-input.")
			;
			
			inputConstraintLoField = NumberBox(thisEditor.tabs.views[2], flow2.bounds.width/2-56@15)
				.font_(textFieldFont)
				.normalColor_(textFieldFontColor)
				.value_(wcmHiLo.oscInputRange.model.value[0])
				.enabled_(false)
			;
			
			flow2.shift(5, 0);
			
			inputConstraintHiField = NumberBox(thisEditor.tabs.views[2], flow2.bounds.width/2-56@15)
				.font_(textFieldFont)
				.normalColor_(textFieldFontColor)
				.value_(wcmHiLo.oscInputRange.model.value[0])
				.enabled_(false)
			;
			
			flow2.shift(5, 0);
						
			calibBut = Button(thisEditor.tabs.views[2], 80@15)
				.font_(staticTextFont)
				.states_([
					["calibrating", Color.white, Color.red],
					["calibrate", Color.black, Color.green]
				])
			;
	
			flow2.shift(0, 0);
	
			StaticText(thisEditor.tabs.views[2], flow2.bounds.width-15@15)
				.font_(staticTextFont)
				.string_("Input to Output mapping")
			;
			
			flow2.shift(0, 0);
	
			StaticText(thisEditor.tabs.views[2], flow2.bounds.width-15@15)
				.font_(staticTextFont)
				.background_(Color.white)
				.string_(" current widget-spec constraints lo / hi:"+widget.getSpec(key).minval+"/"+widget.getSpec(key).maxval)
			;
	
			flow2.shift(5, 0);
			
			mappingSelectItems = ["linlin", "linexp", "explin", "expexp"];
			
			mappingSelect = PopUpMenu(thisEditor.tabs.views[2], flow2.bounds.width-15@20)
				.font_(Font("Helvetica", 14))
				.items_(mappingSelectItems)
				.action_({ |ms|
					widget.setOscMapping(ms.item, key);
				})
			;
			
			if(widget.getOscMapping(key).notNil, {
				mappingSelectItems.do({ |item, i|
					if(item.asSymbol === widget.getOscMapping(key), {
						mappingSelect.value_(i);
					});
				}, {
					mappingSelect.value_(0);
				})
			});
						
			flow2.shift(0, 0);
	
			connectorBut = Button(thisEditor.tabs.views[2], flow2.bounds.width-15@25)
				.font_(staticTextFont)
				.states_([
					["connect OSC-controller", Color.white, Color.blue],
					["disconnect OSC-controller", Color.white, Color.red]
				])
				.action_({ |cb|
					cb.value.switch(
						1, { 
							widget.oscConnect(
								ipField.string,
								portField.value,
								nameField.string, 
								indexField.value.asInt,
								key
							);
						},
						0, { widget.oscDisconnect(key) }
					)
				})
			;

			calibNumBoxes = (lo: inputConstraintLoField, hi: inputConstraintHiField);
			
			calibBut.action_({ |but|
				but.value.switch(
					0, { 
						widget.setCalibrate(true, key);
						wcmHiLo.calibration.model.value_(true).changed(\value);
					},
					1, { 
						widget.setCalibrate(false, key);
						wcmHiLo.calibration.model.value_(false).changed(\value);
					}
				)
			});
	
			widget.getCalibrate(key).switch(
				true, { calibBut.value_(0) },
				false, { calibBut.value_(1) }
			);
	
		});
		
		tab !? { 
			thisEditor.tabs.focus(tab);
		};
		thisEditor.window.front;
	}
	
	front { |tab|
		thisEditor.window.front;
		tab !? thisEditor.tabs.focus(tab);
	}
	
	close { |key|
		thisEditor.window.close;
		switch(allEditors[name].class,
			Event, { 
				allEditors[name].removeAt(key);
				if(allEditors[name].isEmpty, { allEditors.removeAt(name) });
			},
			{ allEditors.removeAt(name) };
		)
	}
	
	isClosed { 
		var ret;
		thisEditor.window !? {
			ret = defer { thisEditor.window.isClosed };
			^ret.value;
		}
	}
			
}