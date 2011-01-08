CVWidgetEditor {
	classvar allEditors;
	var <>window, <>tabs;
	var <>calibBut, <>calibNumBoxes;
	var <>addrField, <>nameField, <>indexField;
	var <>inputConstraintLoField, <>inputConstraintHiField;
	var <>mappingSelect;
	var <>connectorBut;
	
	*new { |widget, widgetName, tab, hilo|
		^super.new.init(widget, widgetName, tab, hilo)
	}

	init { |widget, widgetName, tab, hilo|
		var flow1, flow2;
		var tabs, specsList, specsActions, editor, cvString, slotHiLo;
		var staticTextFont, staticTextColor, textFieldFont, textFieldFontColor, textFieldBg;
		var addr;
//		var /*addrField, */addr, indexField/*, connectorBut*/;
		var mappingSelectItems/*, mappingModes*/;
		
		widget ?? {
			Error("CVWidgetEditor is a utility-class that should only be used in connection with an existing CVWidget").throw;
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
		
		if(allEditors[widgetName.asSymbol].isNil or:{ allEditors[widgetName.asSymbol].window.isClosed }, {
			this.window = Window("Widget Editor:"+widgetName, Rect(Window.screenBounds.width/2-150, Window.screenBounds.height/2-100, 330, 260));

			allEditors.put(widgetName.asSymbol, (window: this.window));

			if(Quarks.isInstalled("wslib"), { this.window.background_(Color.white) });
			this.tabs = TabbedView(window, Rect(0, 0, this.window.bounds.width, this.window.bounds.height), ["Specs", "MIDI", "OSC"], scroll: true);
			allEditors[widgetName.asSymbol].tabs = this.tabs;
			allEditors[widgetName.asSymbol].tabs.postln;
			allEditors[widgetName.asSymbol].tabs.view.resize_(5);
			allEditors[widgetName.asSymbol].tabs.stringFocusedColor_(Color.blue);
			
			specsList = ListView(allEditors[widgetName.asSymbol].tabs.views[0], Rect(0, 0, Window.screenBounds.width, Window.screenBounds.height))
				.resize_(5)
				.background_(Color.white)
				.hiliteColor_(Color.blue)
				.selectedStringColor_(Color.white)
				.visible_(true)
				.enterKeyAction_({ |ws|
					specsActions[ws.value].value;
					if(widget.class === CVWidgetKnob, {
						block { |break|
							#[\pan, \boostcut, \bipolar, \detune].do({ |symbol| 
								if(widget.spec == symbol.asSpec, { 
									break.value(widget.knob.centered_(true));
								}, {
									widget.knob.centered_(false);
								})
							})
						}
					});
					[ws.value, ws.items[ws.value]].postln;
				})
			;
			
			specsActions = [
				{ specsList.visible_(false); editor.visible_(true) },
			];
			
			specsList.items_(specsList.items.add(widget.spec.asString));

			Spec.specs.pairsDo({ |k, v|
				if(v.isKindOf(ControlSpec), {
					specsList.items_(specsList.items.add(k++":"+v));
					if(hilo.notNil, {
						specsActions = specsActions.add({ 
							widget.spec_(v, hilo);
							specsList.items_(
								[v.asString] ++ specsList.items[1..(specsList.items.size-1)]
							);
							specsList.visible_(true);
							editor.visible_(false);
						})
					}, {
						specsActions = specsActions.add({ 
							widget.spec_(v);
							specsList.items_(
								[v.asString] ++ specsList.items[1..(specsList.items.size-1)]
							);
							specsList.visible_(true);
							editor.visible_(false);
						})
					})
				})
			});

			editor = TextView(window, Rect(0, 0, this.window.bounds.width, this.window.bounds.height))
				.resize_(5)
			;

			if(hilo.notNil, {
				cvString = widget.spec(hilo).asString.split($ );
			}, {
				cvString = widget.spec.asString.split($ );
			});

			cvString = cvString[1..cvString.size-1].join(" ");

			editor
				.string_("// edit and hit <enter>\n// the string must represent a valid ControlSpec\n// e.g. \\freq.asSpec\n// or [20, 400].asSpec\n// or ControlSpec(23, 653, \\exp, 1.0, 312, \" hz\")\n// have a look at the ControlSpec-helpfile ...\n\n"++cvString)
				.visible_(false)
				.syntaxColorize
				.enterInterpretsSelection_(false)
				.keyDownAction_({ |view, char, modifiers, unicode, keycode|
					if(unicode == 3, {
						("result:"+view.string.interpret).postln;
						if(hilo.isNil, {
							widget.spec_(view.string.interpret);
							block { |break|
								#[\pan, \boostcut, \bipolar, \detune].do({ |symbol|
									if(widget.spec == symbol.asSpec, { 
										break.value(widget.knob.centered_(true));
									}, {
										widget.knob.centered_(false);
									})			
								})
							}
						}, {
							widget.spec_(view.string.interpret, hilo);
						});
						specsList.visible_(true);
						specsList.items_([view.string.interpret.asString] ++ specsList.items[1..(specsList.items.size-1)]);
						editor.visible_(false)
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

			allEditors[widgetName.asSymbol].tabs.views[1].decorator = flow1 = FlowLayout(window.view.bounds, 7@7, 3@3);
			allEditors[widgetName.asSymbol].tabs.views[2].decorator = flow2 = FlowLayout(window.view.bounds, 7@7, 3@3);
	
			StaticText(allEditors[widgetName.asSymbol].tabs.views[2], flow2.bounds.width-30@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("Listening address address of the OSCresponder.")
			;
			
			this.addrField = TextField(allEditors[widgetName.asSymbol].tabs.views[2], flow2.bounds.width-30@15)
				.font_(textFieldFont)
				.stringColor_(textFieldFontColor)
				.background_(textFieldBg)
			;
			
			flow2.shift(0, 0);
			
			StaticText(allEditors[widgetName.asSymbol].tabs.views[2], flow2.bounds.width-30@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("OSC-typetag, beginning with a slash: e.g. /my/typetag.")
			;
	
			this.nameField = TextField(allEditors[widgetName.asSymbol].tabs.views[2], flow2.bounds.width-30@15)
				.font_(textFieldFont)
				.stringColor_(textFieldFontColor)
				.background_(textFieldBg)
			;
						
			flow2.shift(0, 0);
			
			StaticText(allEditors[widgetName.asSymbol].tabs.views[2], flow2.bounds.width-30@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("OSC message-index.")
			;
			
			
			this.indexField = NumberBox(allEditors[widgetName.asSymbol].tabs.views[2], 36@15)
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
	
			StaticText(allEditors[widgetName.asSymbol].tabs.views[2], flow2.bounds.width-30@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("lower and upper constraints of the OSC-input.")
			;
			
			this.inputConstraintLoField = NumberBox(allEditors[widgetName.asSymbol].tabs.views[2], 50@15)
				.font_(textFieldFont)
				.normalColor_(textFieldFontColor)
				.value_(widget.controllersAndModels.oscInputRangeModelController.model.value[0])
				.enabled_(false)
			;
			
			flow2.shift(5, 0);
			
			this.inputConstraintHiField = NumberBox(allEditors[widgetName.asSymbol].tabs.views[2], 50@15)
				.font_(textFieldFont)
				.normalColor_(textFieldFontColor)
				.value_(widget.controllersAndModels.oscInputRangeModelController.model.value[0])
				.enabled_(false)
			;
			
			flow2.shift(5, 0);
			
			this.calibBut = Button(allEditors[widgetName.asSymbol].tabs.views[2], 115@15)
				.font_(staticTextFont)
				.states_([
					["calibrating", Color.white, Color.red],
					["calibrate", Color.black, Color.green]
				])
			;
	
			flow2.shift(0, 0);
	
			StaticText(allEditors[widgetName.asSymbol].tabs.views[2], flow2.bounds.width-30@15)
				.font_(staticTextFont)
				.string_("Input to Output mapping")
			;
			
			flow2.shift(0, 0);
	
			StaticText(allEditors[widgetName.asSymbol].tabs.views[2], flow2.bounds.width-30@15)
				.font_(staticTextFont)
				.background_(Color.white)
				.string_(" The current widget-spec constraints low / high:"+widget.spec.minval+"/"+widget.spec.maxval)
			;
	
			flow2.shift(5, 0);
			
			mappingSelectItems = ["linlin", "linexp", "explin", "expexp"];
			
			this.mappingSelect = PopUpMenu(allEditors[widgetName.asSymbol].tabs.views[2], flow2.bounds.width-30@20)
				.font_(Font("Helvetica", 14))
				.items_(mappingSelectItems)
				.action_({ |ms|
					widget.oscMapping_(ms.item);
				})
			;
						
			flow2.shift(0, 0);
	
			this.connectorBut = Button(allEditors[widgetName.asSymbol].tabs.views[2], flow2.bounds.width-30@25)
				.font_(staticTextFont)
				.states_([
					["connect OSC-controller", Color.white, Color.blue],
					["disconnect OSC-controller", Color.white, Color.red]
				])
				.action_({ |cb|
					cb.value.switch(
						1, { 
							if(addrField.value.size > 6, { addr = addrField.value });
							widget.oscConnect(addr, nameField.value, indexField.value.asInt);
						},
						0, { widget.oscResponderRemove }
					)
				})
			;

			this.calibNumBoxes = (lo: inputConstraintLoField, hi: inputConstraintHiField);
			
			this.calibBut.action_({ |but|
				but.value.switch(
					0, { widget.controllersAndModels.calibModelController.model.value_(true).changed(\value) },
					1, { widget.controllersAndModels.calibModelController.model.value_(false).changed(\value) }
				)
			});
	
			widget.calibrate.switch(
				true, { this.calibBut.value_(0) },
				false, { this.calibBut.value_(1) }
			);
	
		});
		
		tab !? { 
			allEditors[widgetName.asSymbol].tabs.focus(tab);		};
		allEditors[widgetName.asSymbol].window.front;
	}
	
	front { |widgetName, tab|
		allEditors[widgetName.asSymbol].window.front;
		tab !? allEditors[widgetName.asSymbol].tabs.focus(tab);
	}
	
	isClosed { |widgetName|
		allEditors[widgetName.asSymbol].window !? {
			^allEditors[widgetName.asSymbol].window.isClosed;
		}
	}
}