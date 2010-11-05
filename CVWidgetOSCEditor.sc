
CVWidgetOSCEditor {
	
	classvar <window, <>tabs;
	classvar <calibBut, <calibButAdv;
	
	*new { |widget, widgetName, hilo|
		var slot;
		var flow1, flow2;
		var inputConstraintLoField, inputConstraintHiField;
		var inputConstraintLoFieldAdv, inputConstraintHiFieldAdv;
		var addrField, nameField, indexField, connectorBut, connectorButAdv;
		var mappingSelect;
		var staticTextFont, staticTextColor;
		var textFieldFont, textFieldBg, textFieldFontColor;
		
		staticTextFont = Font(Font.defaultSansFace, 10);
		staticTextColor = Color(0.2, 0.2, 0.2);
		textFieldFont = Font(Font.defaultMonoFace, 9);
		textFieldFontColor = Color.black;
		textFieldBg = Color.white;
//		mapConstraintLo = CV([-inf, inf].asSpec, 0.0);
//		mapConstraintHi = CV([-inf, inf].asSpec, 0.0);
		
		if(hilo.notNil, {
			slot = "["++hilo.asString++"]";
		}, {
			slot = "";
		});
		
		if(window.isNil or:{ window.isClosed }, {
			window = Window("OSC-Editor:"+widgetName+slot, Rect(Window.screenBounds.width/2-150, Window.screenBounds.height/2-100, 330, 395));
		}, {
//			window.view.children.postln;
			window.view.children.do(_.remove);
			window.name_("OSC-Editor:"+widgetName+slot);
		});
		
		window.view.background_(Color.white);
		if(Quarks.isInstalled("wslib"), { window.background_(Color.white) });
		
		this.tabs = TabbedView(window, Rect(0, 0, window.bounds.width, window.bounds.height), ["basic", "advanced"], scroll: true);
		this.tabs.view.resize_(5);
		this.tabs.stringFocusedColor_(Color.blue);
		this.tabs.views[0].decorator = flow1 = FlowLayout(window.view.bounds, 7@7, 3@3);

//		[flow1.bounds, flow1.margin, flow1.gap, flow1.left, flow1.top, flow1.maxHeight, flow1.maxRight].postln;
	
		StaticText(this.tabs.views[0], flow1.bounds.width-14@50)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("Set the address the OSCresponder is supposed to listen to.\nUsually you will leave this blank (address will be set to 'nil' -> listening to any address)")
		;
		
//		flow1.nextLine;
//		[flow1.bounds, flow1.margin, flow1.gap, flow1.left, flow1.top, flow1.maxHeight, flow1.maxRight].postln;
		
		addrField = TextField(this.tabs.views[0], flow1.bounds.width-14@15)
			.font_(textFieldFont)
			.stringColor_(textFieldFontColor)
			.background_(textFieldBg)
		;
		
		flow1.shift(0, -20);
		
		StaticText(this.tabs.views[0], flow1.bounds.width-14@75)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("Set the OSC-typetag, beginning with a slash: e.g. /my/typetag.\nDepending on the OSC-client your using this may either be predefined or you'll have to set a name in the client which has then to be entered here again.");
			
//		[flow1.bounds, flow1.margin, flow1.gap, flow1.left, flow1.top, flow1.maxHeight, flow1.maxRight].postln;

		nameField = TextField(this.tabs.views[0], flow1.bounds.width-14@15)
			.font_(textFieldFont)
			.stringColor_(textFieldFontColor)
			.background_(textFieldBg)
			.string_("/my/typetag")
		;
		
		flow1.shift(0, -35);
//		[flow1.bounds, flow1.margin, flow1.gap, flow1.left, flow1.top, flow1.maxHeight, flow1.maxRight].postln;

		StaticText(this.tabs.views[0], flow1.bounds.width-14@90)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
//			.background_(Color.green)
			.string_("Set the OSC message-index: An OSC-message will usually be an Array, containing the type-tag at index 0 and values returned by the controller in the subsequent slots: e.g. a 2D-slider will perhaps return the x-axis at index 1 and the y-axis at index 2")
		;
		
//		[flow1.bounds, flow1.margin, flow1.gap, flow1.left, flow1.top, flow1.maxHeight, flow1.maxRight].postln;
		
		indexField = NumberBox(this.tabs.views[0], 36@15)
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
		flow1.shift(0, -55);

		StaticText(this.tabs.views[0], flow1.bounds.width-14@140)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("Set the lower and upper constraints of the OSC-input. Different to MIDI this will probably not be 0 / 127. By default these values will be set by the built-in calibration mechanism, so, the following 2 textfields are deactived as long as the calibration-mechanism is active. Hit the calibration button to deactivate it and set the values manually though it's recommended to use the calibration-mechanism")
		;
		
		inputConstraintLoField = NumberBox(this.tabs.views[0], 50@15)
			.font_(textFieldFont)
			.normalColor_(textFieldFontColor)
			.value_(0)
		;
		
		flow1.shift(5, 0);
		
		inputConstraintHiField = NumberBox(this.tabs.views[0], 50@15)
			.font_(textFieldFont)
			.normalColor_(textFieldFontColor)
			.value_(0)
		;
		
		flow1.shift(5, 0);
		
		calibBut = Button(this.tabs.views[0], 115@15)
			.font_(staticTextFont)
			.states_([
				["calibrating", Color.white, Color.red],
				["calibrate", Color.black, Color.green]
			])
		;
		
		flow1.shift(0, 10);
		
		connectorBut = Button(this.tabs.views[0], flow1.bounds.width-14@25)
			.font_(staticTextFont)
			.states_([
				["connect OSC-controller", Color.white, Color.blue],
				["disconnect OSC-controller", Color.white, Color.red]
			])
		;
		
		this.tabs.views[1].decorator = flow2 = FlowLayout(window.view.bounds, 7@7, 3@3);
		
		flow2.shift(0, -5);
		StaticText(this.tabs.views[1], flow2.bounds.width-14@45)
			.font_(staticTextFont)
			.string_("You may set different kinds of mapping for input to the output. This may result in a finer grained control but be warned:")
		;
		flow2.shift(0, -20);
		StaticText(this.tabs.views[1], flow2.bounds.width-14@65)
			.font_(Font("Helvetica", 18))
			.string_("Inappropriate mappings will inevitably crash the application!!")
//			.align_(\center)
		;
		flow2.shift(0, -45);
		StaticText(this.tabs.views[1], flow2.bounds.width-14@115)
			.font_(staticTextFont)
			.string_("The reason for this lies in the math behind the mapping-methods: e.g. you cannot map a range whose constraints contain a negative value or zero to exponantial as this leads to NaN output-values. If you're unsure about the applicabable settings please read the helpfile (again)!")
		;
		
		StaticText(this.tabs.views[1], flow2.bounds.width-14@15)
			.font_(staticTextFont)
			.background_(Color.white)
			.string_(" The current widget-spec constraints low / high:"+widget.spec.minval+"/"+widget.spec.maxval)
		;
		
		flow2.shift(0, -10);
		
		StaticText(this.tabs.views[1], flow2.bounds.width-14@35)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
//			.background_(Color.green)
			.string_("The current input-constraints. You may set them manually if calibration is dectivated.")
		;
		
		inputConstraintLoFieldAdv = NumberBox(this.tabs.views[1], 50@15)
			.font_(textFieldFont)
			.normalColor_(textFieldFontColor)
		;
		
		flow2.shift(5, 0);
		
		inputConstraintHiFieldAdv = NumberBox(this.tabs.views[1], 50@15)
			.font_(textFieldFont)
			.normalColor_(textFieldFontColor)
		;
		
		flow2.shift(5, 0);
		
		calibButAdv = Button(this.tabs.views[1], 115@15)
			.font_(staticTextFont)
			.states_([
				["calibrating", Color.white, Color.red],
				["calibrate", Color.black, Color.green]
			])
		;
		
		flow2.shift(0, 10);
		
		mappingSelect = PopUpMenu(this.tabs.views[1], flow2.bounds.width-14@20)
			.font_(Font("Helvetica", 14))
			.items_([
				"linear -> linear",
				"linear -> exponential",
				"exponential -> linear",
				"exponential -> exponential"
			])
		;
		
		flow2.shift(0, 10);
		
		connectorButAdv = Button(this.tabs.views[1], flow2.bounds.width-14@25)
			.font_(staticTextFont)
			.states_([
				["connect OSC-controller", Color.white, Color.blue],
				["disconnect OSC-controller", Color.white, Color.red]
			])
		;
		
		[widget.mapConstrainterLo, widget.mapConstrainterHi].postln;
		[inputConstraintLoField, inputConstraintLoFieldAdv].do(widget.mapConstrainterLo.connect(_));
		[inputConstraintHiField, inputConstraintHiFieldAdv].do(widget.mapConstrainterHi.connect(_));
		
		window.front;
	}
	
}