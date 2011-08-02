CVWidgetEditor {
	classvar <allEditors;
	var <window, <tabs;
	var <specField, <specsList, <specsListSpecs;
	var <midiModeSelect, <midiMeanNB, <softWithinNB, <ctrlButtonBankField, <midiResolutionNB;
	var <midiLearnBut, <midiSrcField, <midiChanField, <midiCtrlField;
	var <calibBut, <calibNumBoxes;
	var <ipField, <portField, <nameField, <indexField;
	var <inputConstraintLoField, <inputConstraintHiField;
	var <mappingSelect;
	var <connectorBut;
	var <name;
	
	*new { |widget, widgetName, tab, hilo|
		^super.new.init(widget, widgetName, tab, hilo)
	}

	init { |widget, widgetName, tab, hilo|
		var flow0, flow1, flow2;
		var tabs, /*specsActions, editor, */cvString, slotHiLo;
		var staticTextFont, staticTextColor, textFieldFont, textFieldFontColor, textFieldBg;
		var msrc = "source", mchan = "chan", mctrl = "ctrl", margs;
		var addr;
		var midiModes;
		var mappingSelectItems/*, mappingModes*/;
		var tmp; // multipurpose, short-term var
		
		name = widgetName.asSymbol;
		
		widget ?? {
			Error("CVWidgetEditor is a utility-GUI-class that should only be used in connection with an existing CVWidget").throw;
		};

		staticTextFont = Font(Font.defaultSansFace, 10);
		staticTextColor = Color(0.2, 0.2, 0.2);
		textFieldFont = Font(Font.defaultMonoFace, 9);
		textFieldFontColor = Color.black;
		textFieldBg = Color.white;
		
		if(hilo.notNil, {
			slotHiLo = "["++hilo.asString++"]";
		}, {
			slotHiLo = "";
		});
		
		allEditors ?? { allEditors = IdentityDictionary() };
		
		if(allEditors[name].isNil or:{ allEditors[name].window.isClosed }, {
			window = Window("Widget Editor:"+widgetName, Rect(Window.screenBounds.width/2-150, Window.screenBounds.height/2-100, 270, 225));

			allEditors.put(name, (window: window, name: widgetName));

			if(Quarks.isInstalled("wslib"), { window.background_(Color.white) });
			tabs = TabbedView(window, Rect(0, 0, window.bounds.width, window.bounds.height), ["Specs", "MIDI", "OSC"], scroll: true);
			allEditors[name].tabs = tabs;
//			allEditors[name].tabs.postln;
			allEditors[name].tabs.view.resize_(5);
			allEditors[name].tabs.stringFocusedColor_(Color.blue);
			
			allEditors[name].tabs.views[0].decorator = flow0 = FlowLayout(window.view.bounds, 7@7, 3@3);
			allEditors[name].tabs.views[1].decorator = flow1 = FlowLayout(window.view.bounds, 7@7, 3@3);
			allEditors[name].tabs.views[2].decorator = flow2 = FlowLayout(window.view.bounds, 7@7, 3@3);
			
			StaticText(allEditors[name].tabs.views[0], flow0.bounds.width-20@95)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("Enter a ControlSpec in the textfield:\ne.g. ControlSpec(20, 20000, \\exp, 0.0, 440, \"Hz\")\nor \\freq.asSpec \nor [20, 20000, \\exp].asSpec.\nOr select a suitable ControlSpec from the List below.\nIf you don't know what this all means have a look\nat the ControlSpec-helpfile.")
			;

			if(hilo.notNil, {
				cvString = widget.getSpec(hilo).asString.split($ );
			}, {
				cvString = widget.getSpec.asString.split($ );
			});

			cvString = cvString[1..cvString.size-1].join(" ");
			
			specField = TextField(allEditors[name].tabs.views[0], flow0.bounds.width-20@15)
				.font_(staticTextFont)
				.string_(cvString)
				.action_({ |tf|
					widget.setSpec(tf.string.interpret, hilo)
				})
			;
			
			flow0.shift(0, 5);

			specsList = PopUpMenu(allEditors[name].tabs.views[0], flow0.bounds.width-20@20)
				.action_({ |sl|
					widget.setSpec(specsListSpecs[sl.value], hilo);
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
			
//			[widget.spec, specsListSpecs.size, specsList.items.size].postln;
			
			tmp = specsListSpecs.detectIndex({ |spec, i| spec == widget.getSpec(hilo) });
			if(tmp.notNil, {
				specsList.value_(tmp);
			}, {
				specsListSpecs.array_([widget.getSpec(hilo)]++specsListSpecs.array);
				specsList.items = List["custom:"+widget.getSpec(hilo).asString]++specsList.items;
			});
			
			window.onClose_({
				widget.editorEnv.specsListSpecs = specsListSpecs;
				widget.editorEnv.specsListItems = specsList.items;
			});
						
			// MIDI editing
			
			StaticText(allEditors[name].tabs.views[1], flow1.bounds.width/2+40@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("MIDI-mode: 0-127 or in/decremental")
			;
			
			flow1.shift(5, 0);
			
			midiModes = ["0-127", "+/-"];
			
			midiModeSelect = PopUpMenu(allEditors[name].tabs.views[1], flow1.bounds.width/2-70@15)
				.font_(staticTextFont)
				.items_(midiModes)
				.value_(widget.midiMode)
				.action_({ |ms|
					widget.midiMode_(ms.value);
				})
			;
			
			StaticText(allEditors[name].tabs.views[1], flow1.bounds.width/2+60@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("MIDI-mean (in/decremental mode only)")
			;
			
			flow1.shift(5, 0);
			
			midiMeanNB = NumberBox(allEditors[name].tabs.views[1], flow1.bounds.width/2-90@15)
				.font_(staticTextFont)
				.value_(widget.midiMean)
				.action_({ |mb|
					widget.midiMean_(mb.value)
				})
				.step_(1.0)
				.clipLo_(0.0)
			;
			
			StaticText(allEditors[name].tabs.views[1], flow1.bounds.width/2+60@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("minimum distance for the slider (0-127 only)")
			;
			
			flow1.shift(5, 0);
			
			softWithinNB = NumberBox(allEditors[name].tabs.views[1], flow1.bounds.width/2-90@15)
				.font_(staticTextFont)
				.value_(widget.softWithin)
				.action_({ |mb|
					widget.softWithin_(mb.value)
				})
				.step_(0.005)
				.clipLo_(0.01)
				.clipHi_(0.5)
			;
			
			StaticText(allEditors[name].tabs.views[1], flow1.bounds.width/2+60@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("MIDI-resolution (+/- only)")
			;
			
			flow1.shift(5, 0);

			midiResolutionNB = NumberBox(allEditors[name].tabs.views[1], flow1.bounds.width/2-90@15)
				.font_(staticTextFont)
				.value_(widget.midiResolution)
				.action_({ |mb|
					widget.midiResolution_(mb.value)
				})
				.step_(0.05)
				.clipLo_(0.001)
				.clipHi_(10.0)
			;
			
			StaticText(allEditors[name].tabs.views[1], flow1.bounds.width/2+60@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("number of sliders per bank")
			;

			flow1.shift(5, 0);
			
			ctrlButtonBankField = TextField(allEditors[name].tabs.views[1], flow1.bounds.width/2-90@15)
				.font_(staticTextFont)
				.string_(widget.ctrlButtonBank)
				.action_({ |mb|
					widget.ctrlButtonBank_(mb.string.asInt)
				})
			;
			
			flow1.shift(0, 10);
						
			midiLearnBut = Button(allEditors[name].tabs.views[1], 15@15)
				.font_(staticTextFont)
				.states_([
					["L", Color.white, Color.blue],
					["X", Color.white, Color.red]
				])
				.action_({ |ml|
					ml.value.switch(
						1, {
							margs = [
								[widget.guiEnv.midiSrc.string, msrc], 
								[widget.guiEnv.midiChan.string, mchan], 
								[widget.guiEnv.midiCtrl.string, mctrl]
							].collect({ |pair| if(pair[0] != pair[1], { pair[0].asInt }, { nil }) });
							if(margs.select({ |i| i.notNil }).size > 0, {
								widget.midiConnect(*margs);
							}, {
								widget.midiConnect;
							})
						},
						0, { widget.midiDisconnect }
					)
				})
			;
			
			flow1.shift(0, 0);
			
			midiSrcField = TextField(allEditors[name].tabs.views[1], flow1.bounds.width-165@15)
				.font_(staticTextFont)
				.string_(msrc)
				.background_(Color.white)
				.action_({ |tf|
					if(tf.string != mctrl, {
						widget.wdgtControllersAndModels.midiDisplay.model.value_((
							learn: "C",
							src: widget.wdgtControllersAndModels.midiDisplay.model.value.src,
							chan: widget.wdgtControllersAndModels.midiDisplay.model.value.chan,
							ctrl: tf.string
						)).changed(\value)
					})
				})
				.mouseDownAction_({ |tf|
					tf.stringColor_(Color.red)
				})
				.keyDownAction_({ |tf, char, modifiers, unicode, keycode|
//					[tf, char, modifiers, unicode, keycode].postln;
					if(unicode == 13, {
						tf.stringColor_(Color.black);
					})
				}) 
			;
			
			flow1.shift(0, 0);
			
			midiChanField = TextField(allEditors[name].tabs.views[1], 60@15)
				.font_(staticTextFont)
				.string_(mchan)
				.background_(Color.white)
				.action_({ |tf|
					if(tf.string != mctrl, {
						widget.wdgtControllersAndModels.midiDisplay.model.value_((
							learn: "C",
							src: widget.wdgtControllersAndModels.midiDisplay.model.value.src,
							chan: widget.wdgtControllersAndModels.midiDisplay.model.value.chan,
							ctrl: tf.string
						)).changed(\value)
					})
				})
				.mouseDownAction_({ |tf|
					tf.stringColor_(Color.red)
				})
				.keyDownAction_({ |tf, char, modifiers, unicode, keycode|
//					[tf, char, modifiers, unicode, keycode].postln;
					if(unicode == 13, {
						tf.stringColor_(Color.black);
					})
				}) 
			;
			
			flow1.shift(0, 0);
			
			midiCtrlField = TextField(allEditors[name].tabs.views[1], 60@15)
				.font_(staticTextFont)
				.string_(mctrl)
				.background_(Color.white)
				.action_({ |tf|
					if(tf.string != mctrl, {
						widget.wdgtControllersAndModels.midiDisplay.model.value_((
							learn: "C",
							src: widget.wdgtControllersAndModels.midiDisplay.model.value.src,
							chan: widget.wdgtControllersAndModels.midiDisplay.model.value.chan,
							ctrl: tf.string
						)).changed(\value)
					})
				})
				.mouseDownAction_({ |tf|
					tf.stringColor_(Color.red)
				})
				.keyDownAction_({ |tf, char, modifiers, unicode, keycode|
//					[tf, char, modifiers, unicode, keycode].postln;
					if(unicode == 13, {
						tf.stringColor_(Color.black);
					})
				}) 
			;
			
			// OSC editting
			
			StaticText(allEditors[name].tabs.views[2], flow2.bounds.width-20@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("device-IP/port: leave empty for listening to any IP/port")
			;
			
			ipField = TextField(allEditors[name].tabs.views[2], flow2.bounds.width-60@15)
				.font_(textFieldFont)
				.stringColor_(textFieldFontColor)
				.background_(textFieldBg)
				.string_("")
			;
			
			flow2.shift(5, 0);

			portField = TextField(allEditors[name].tabs.views[2], 36@15)
				.font_(textFieldFont)
				.stringColor_(textFieldFontColor)
				.background_(textFieldBg)
				.string_("")
			;
				
			flow2.shift(0, 0);

			StaticText(allEditors[name].tabs.views[2], flow2.bounds.width-20@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("OSC-typetag, e.g.: /my/typetag / OSC message index")
			;
	
			nameField = TextField(allEditors[name].tabs.views[2], flow2.bounds.width-60@15)
				.font_(textFieldFont)
				.stringColor_(textFieldFontColor)
				.background_(textFieldBg)
				.string_("/my/typetag")
			;
						
			flow2.shift(5, 0);
			
			indexField = NumberBox(allEditors[name].tabs.views[2], 36@15)
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
	
			StaticText(allEditors[name].tabs.views[2], flow2.bounds.width-15@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("lower and upper constraints of the OSC-input.")
			;
			
			inputConstraintLoField = NumberBox(allEditors[name].tabs.views[2], flow2.bounds.width/2-56@15)
				.font_(textFieldFont)
				.normalColor_(textFieldFontColor)
				.value_(widget.wdgtControllersAndModels.oscInputRange.model.value[0])
				.enabled_(false)
			;
			
			flow2.shift(5, 0);
			
			inputConstraintHiField = NumberBox(allEditors[name].tabs.views[2], flow2.bounds.width/2-56@15)
				.font_(textFieldFont)
				.normalColor_(textFieldFontColor)
				.value_(widget.wdgtControllersAndModels.oscInputRange.model.value[0])
				.enabled_(false)
			;
			
			flow2.shift(5, 0);
			
			calibBut = Button(allEditors[name].tabs.views[2], 80@15)
				.font_(staticTextFont)
				.states_([
					["calibrating", Color.white, Color.red],
					["calibrate", Color.black, Color.green]
				])
			;
	
			flow2.shift(0, 0);
	
			StaticText(allEditors[name].tabs.views[2], flow2.bounds.width-15@15)
				.font_(staticTextFont)
				.string_("Input to Output mapping")
			;
			
			flow2.shift(0, 0);
	
			StaticText(allEditors[name].tabs.views[2], flow2.bounds.width-15@15)
				.font_(staticTextFont)
				.background_(Color.white)
				.string_(" current widget-spec constraints lo / hi:"+widget.getSpec(hilo).minval+"/"+widget.getSpec(hilo).maxval)
			;
	
			flow2.shift(5, 0);
			
			mappingSelectItems = ["linlin", "linexp", "explin", "expexp"];
			
			mappingSelect = PopUpMenu(allEditors[name].tabs.views[2], flow2.bounds.width-15@20)
				.font_(Font("Helvetica", 14))
				.items_(mappingSelectItems)
				.action_({ |ms|
					widget.oscMapping_(ms.item);
				})
			;
			
			if(widget.oscMapping.notNil, {
				mappingSelectItems.do({ |item, i|
					if(item.asSymbol === widget.oscMapping, {
						mappingSelect.value_(i);
					})
				}, {
					mappingSelect.value_(0);
				})
			});
						
			flow2.shift(0, 0);
	
			connectorBut = Button(allEditors[name].tabs.views[2], flow2.bounds.width-15@25)
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
								indexField.value.asInt
							);
						},
						0, { widget.oscDisconnect }
					)
				})
			;

			calibNumBoxes = (lo: inputConstraintLoField, hi: inputConstraintHiField);
			
			calibBut.action_({ |but|
				but.value.switch(
					0, { widget.wdgtControllersAndModels.calibration.model.value_(true).changed(\value) },
					1, { widget.wdgtControllersAndModels.calibration.model.value_(false).changed(\value) }
				)
			});
	
			widget.calibrate.switch(
				true, { calibBut.value_(0) },
				false, { calibBut.value_(1) }
			);
	
		});
		
		tab !? { 
			allEditors[name].tabs.focus(tab);
		};
		allEditors[name].window.front;
	}
	
	front { |tab|
		allEditors[name].window.front;
		tab !? allEditors[name].tabs.focus(tab);
	}
	
	close {
		allEditors[name].window.close;
		allEditors.removeAt(name);
	}
	
	isClosed {
		var ret;
//		defer {
			allEditors[name].window !? {
				ret = defer { allEditors[name].window.isClosed };
				^ret.value;
			}
//		}
	}
			
}