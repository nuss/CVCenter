CVWidgetEditor {
	classvar allWindows;
	var <>window, <>tabs;
	var <>calibBut, <>calibNumBoxes;
	
	*new { |widget, widgetName, tab, calibModel, hilo|
		^super.new.init(widget, widgetName, tab, calibModel, hilo)
	}

	init { |widget, widgetName, tab, calibModel, hilo|
		var flow1, flow2;
		var tabs, specsList, specsActions, editor, cvString, slotHiLo;
		var staticTextFont, staticTextColor, textFieldFont, textFieldFontColor, textFieldBg;
		var inputConstraintLoField, inputConstraintHiField;
		var inputConstraintLoFieldAdv, inputConstraintHiFieldAdv;
		var addrField, nameField, indexField, connectorBut, connectorButAdv;
		var mappingSelect;
		
		staticTextFont = Font(Font.defaultSansFace, 10);
		staticTextColor = Color(0.2, 0.2, 0.2);
		textFieldFont = Font(Font.defaultMonoFace, 9);
		textFieldFontColor = Color.black;
		textFieldBg = Color.white;
		
		widget ?? { Error("You have to specify the widget you want to edit!").throw };
		
		if(hilo.notNil, {
			slotHiLo = "["++hilo.asString++"]";
		}, {
			slotHiLo = "";
		});
		
		allWindows ?? { allWindows = IdentityDictionary() };
		
		if(allWindows[widgetName.asSymbol].isNil or:{ allWindows[widgetName.asSymbol].window.isClosed }, {
			this.window = Window("Widget Editor:"+widgetName, Rect(Window.screenBounds.width/2-150, Window.screenBounds.height/2-100, 330, 260));
			allWindows.put(widgetName.asSymbol, (window: this.window));
			if(Quarks.isInstalled("wslib"), { this.window.background_(Color.white) });
			this.tabs = TabbedView(window, Rect(0, 0, this.window.bounds.width, this.window.bounds.height), ["Specs", "MIDI", "OSC"], scroll: true);
			this.tabs.view.resize_(5);
			this.tabs.stringFocusedColor_(Color.blue);
			allWindows[widgetName.asSymbol].tabs = this.tabs;
			
			specsList = ListView(this.tabs.views[0], Rect(0, 0, Window.screenBounds.width, Window.screenBounds.height))
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
										("it happens here:"+[widget.spec, symbol.asSpec]).postln;
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

		});
		
		this.tabs.views[1].decorator = flow1 = FlowLayout(window.view.bounds, 7@7, 3@3);

		this.tabs.views[2].decorator = flow2 = FlowLayout(window.view.bounds, 7@7, 3@3);

		StaticText(this.tabs.views[2], flow2.bounds.width-30@15)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("Listening address address of the OSCresponder.")
		;
		
		addrField = TextField(this.tabs.views[2], flow2.bounds.width-30@15)
			.font_(textFieldFont)
			.stringColor_(textFieldFontColor)
			.background_(textFieldBg)
		;
		
		flow2.shift(0, 0);
		
		StaticText(this.tabs.views[2], flow2.bounds.width-30@15)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("OSC-typetag, beginning with a slash: e.g. /my/typetag.");
			
//		[flow1.bounds, flow1.margin, flow1.gap, flow1.left, flow1.top, flow1.maxHeight, flow1.maxRight].postln;

		nameField = TextField(this.tabs.views[2], flow2.bounds.width-30@15)
			.font_(textFieldFont)
			.stringColor_(textFieldFontColor)
			.background_(textFieldBg)
			.string_("/my/typetag")
		;
		
		flow2.shift(0, 0);
		
		StaticText(this.tabs.views[2], flow2.bounds.width-30@15)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
//			.background_(Color.green)
			.string_("OSC message-index.")
		;
		
//		[flow1.bounds, flow1.margin, flow1.gap, flow1.left, flow1.top, flow1.maxHeight, flow1.maxRight].postln;
		
		indexField = NumberBox(this.tabs.views[2], 36@15)
			.font_(textFieldFont)
			.normalColor_(textFieldFontColor)
//			.typingColor_(Color.red)
			.clipLo_(1)
			.clipHi_(inf)
			.shift_scale_(1)
			.ctrl_scale_(1)
			.alt_scale_(1)
			.value_(1)
		;
		
//		[indexField.bounds.left, indexField.bounds.top].postln;
		flow2.shift(0, 0);

		StaticText(this.tabs.views[2], flow2.bounds.width-30@15)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("lower and upper constraints of the OSC-input.")
		;
		
		inputConstraintLoField = NumberBox(this.tabs.views[2], 50@15)
			.font_(textFieldFont)
			.normalColor_(textFieldFontColor)
			.value_(0)
		;
		
		flow2.shift(5, 0);
		
		inputConstraintHiField = NumberBox(this.tabs.views[2], 50@15)
			.font_(textFieldFont)
			.normalColor_(textFieldFontColor)
			.value_(0)
		;
		
		flow2.shift(5, 0);
		
		this.calibBut = Button(this.tabs.views[2], 115@15)
			.font_(staticTextFont)
			.states_([
				["calibrating", Color.white, Color.red],
				["calibrate", Color.black, Color.green]
			])
		;

		flow2.shift(0, 0);

		StaticText(this.tabs.views[2], flow2.bounds.width-30@15)
			.font_(staticTextFont)
			.string_("Input to Output mapping")
		;
		
		flow2.shift(0, 0);

		StaticText(this.tabs.views[2], flow2.bounds.width-30@15)
			.font_(staticTextFont)
			.background_(Color.white)
			.string_(" The current widget-spec constraints low / high:"+widget.spec.minval+"/"+widget.spec.maxval)
		;

		flow2.shift(5, 0);
		
		mappingSelect = PopUpMenu(this.tabs.views[2], flow2.bounds.width-30@20)
			.font_(Font("Helvetica", 14))
			.items_([
				"linear -> linear",
				"linear -> exponential",
				"exponential -> linear",
				"exponential -> exponential"
			])
		;
		
		flow2.shift(0, 0);

		connectorBut = Button(this.tabs.views[2], flow2.bounds.width-30@25)
			.font_(staticTextFont)
			.states_([
				["connect OSC-controller", Color.white, Color.blue],
				["disconnect OSC-controller", Color.white, Color.red]
			])
		;

		this.calibNumBoxes = (lo: inputConstraintLoField, hi: inputConstraintHiField);

		this.calibBut.action_({ |but|
			but.value.switch(
				0, { calibModel.value_(true).changed(\value) },
				1, { calibModel.value_(false).changed(\value) }
			)
		});

		widget.calibrate.switch(
			true, { this.calibBut.value_(0) },
			false, { this.calibBut.value_(1) }
		);

		if(widget.calibrate, {
			inputConstraintHiField.enabled_(true);
			inputConstraintLoField.enabled_(true);
		}, {
			inputConstraintHiField.enabled_(false);
			inputConstraintLoField.enabled_(false);
		});

		tab !? { 
//			("tab-index:"+[tab, allWindows[widgetName.asSymbol].tabs.views[tab]]).postln; 
			allWindows[widgetName.asSymbol].tabs.focus(tab);		};
		allWindows[widgetName.asSymbol].window.front;
	}
}