CVCenterLoadDialog {

	classvar <window;

	*new {
		var staticTextFont, staticTextFontBold, staticTextColor, textFieldFont, textFieldFontColor, textFieldBg;
		var buildCheckbox;
		var flow, midiBg, oscBg;
		var midiFlow, oscFlow;
		var loadMidiCC, loadMidiSrc, loadMidiChan, loadMidiCtrl;
		var textMidiSrc, textMidiChan, textMidiCtrl;
		var loadOscResponders, loadOscIP, loadOscPort, activateCalibration, resetCalibration;
		var textOscIP, textOscPort, textActivateCalibration, textResetCalibration;

		staticTextFont = Font("Arial", 9.4);
		staticTextFontBold = Font("Arial", 9.4, true);
		staticTextColor = Color(0.2, 0.2, 0.2);
		textFieldFont = Font("Andale Mono", 9);
		textFieldFontColor = Color.black;
		textFieldBg = Color.white;

		buildCheckbox = { |active, view, props, font|
			var cBox;
			if(GUI.id === \cocoa, {
				cBox = Button(view, props)
					.states_([
						["", Color.white, Color.white],
						["X", Color.black, Color.white],
					])
					.font_(font)
				;
				if(active, { cBox.value_(1) }, { cBox.value_(0) });
			}, {
				cBox = \CheckBox.asClass.new(view, props).value_(active);
			});
			cBox;
		};

		if(window.isNil or:{ window.isClosed }, {
			window = Window("load a new setup from disk", Rect(
				(Window.screenBounds.width-500).div(2),
				(Window.screenBounds.height-300).div(2),
				500, 300
			));

			window.view.decorator = flow = FlowLayout(window.view.bounds, 7@7, 3@3);

			midiBg = CompositeView(window.view, flow.bounds.width.div(2)-8@flow.indentedRemaining.height);
			oscBg = CompositeView(window.view, flow.indentedRemaining.width@flow.indentedRemaining.height);
			[midiBg, oscBg].do({ |el| el.background_(Color(0.95, 0.95, 0.95)) });

			midiBg.decorator = midiFlow = FlowLayout(midiBg.bounds, 7@7, 3@3);
			oscBg.decorator = oscFlow = FlowLayout(oscBg.bounds, 7@7, 3@3);

			// midi

			StaticText(midiBg, midiFlow.indentedRemaining.width@20)
				.font_(staticTextFontBold)
				.string_("MIDI options")
			;

			loadMidiCC = buildCheckbox.(true, midiBg, 15@15, staticTextFontBold)
				.action_({ |cb|
					switch(cb.value.asBoolean,
						true, {
							[loadMidiSrc, loadMidiChan, loadMidiCtrl].do(_.enabled_(true));
							[textMidiSrc, textMidiChan, textMidiCtrl].do(_.stringColor_(staticTextColor));
						},
						false, {
							[loadMidiSrc, loadMidiChan, loadMidiCtrl].do(_.enabled_(false));
							[textMidiSrc, textMidiChan, textMidiCtrl].do(_.stringColor_(Color(0.7, 0.7, 0.7)));
						}
					)
				})
			;

			StaticText(midiBg, midiFlow.indentedRemaining.width@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("load CCResponders")
			;

			midiFlow.nextLine.shift(15, 0);

			loadMidiCtrl = buildCheckbox.(true, midiBg, 15@15, staticTextFontBold);

			textMidiCtrl = StaticText(midiBg, midiFlow.indentedRemaining.width@25)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("initialize CCResponders with ctrl-nr. stored in the setup")
			;

			midiFlow.nextLine.shift(15, 0);

			loadMidiChan = buildCheckbox.(true, midiBg, 15@15, staticTextFontBold);

			textMidiChan = StaticText(midiBg, midiFlow.indentedRemaining.width@25)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("initialize CCResponders with channel-nr. stored in the setup")
			;

			midiFlow.nextLine.shift(15, 0);

			loadMidiSrc = buildCheckbox.(false, midiBg, 15@15, staticTextFontBold);

			textMidiSrc = StaticText(midiBg, midiFlow.indentedRemaining.width@25)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("initialize CCResponders with source-ID stored in the setup")
			;

			// osc

			StaticText(oscBg, oscFlow.indentedRemaining.width@20)
				.font_(staticTextFontBold)
				.string_("OSC options")
			;

			loadOscResponders = buildCheckbox.(true, oscBg, 15@15, staticTextFontBold)
				.action_({ |cb|
					switch(cb.value.asBoolean,
						true, {
							[loadOscIP, activateCalibration].do(_.enabled_(true));
							if(loadOscIP.value.asBoolean, {
								loadOscPort.enabled_(true);
								textOscPort.stringColor_(staticTextColor);
							});
							if(activateCalibration.value.asBoolean, {
								resetCalibration.enabled_(true);
								textResetCalibration.stringColor_(staticTextColor);
							});
							[textOscIP, textActivateCalibration].do(_.stringColor_(staticTextColor));
						},
						false, {
							[loadOscIP, loadOscPort, activateCalibration, resetCalibration].do(
								_.enabled_(false)
							);
							[textOscIP, textOscPort, textActivateCalibration, textResetCalibration].do(
								_.stringColor_(Color(0.7, 0.7, 0.7))
							);
							if(GUI.id == \cocoa, {
								loadOscPort.value_(0)
							}, { loadOscPort.value_(false) });
							if(GUI.id == \cocoa, {
								resetCalibration.value_(0)
							}, { resetCalibration.value_(false) });
						}
					)
				})
			;

			StaticText(oscBg, oscFlow.indentedRemaining.width@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("load OSCresponders")
			;

			oscFlow.nextLine.shift(15, 0);

			loadOscIP = buildCheckbox.(true, oscBg, 15@15, staticTextFontBold)
				.action_({ |cb|
					switch(cb.value.asBoolean,
						true, {
							loadOscPort.enabled_(true);
							textOscPort.stringColor_(staticTextColor);
						},
						false, {
							loadOscPort.enabled_(false);
							textOscPort.stringColor_(Color(0.7, 0.7, 0.7));
							if(GUI.id == \cocoa, {
								loadOscPort.value_(0)
							}, { loadOscPort.value_(false) });
						}
					)
				})
			;

			textOscIP = StaticText(oscBg, oscFlow.indentedRemaining.width@25)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("initialize OSCresponders with IP-addresses stored in the setup")
			;

			oscFlow.nextLine.shift(15, 0);

			loadOscPort = buildCheckbox.(false, oscBg, 15@15, staticTextFontBold);

			textOscPort = StaticText(oscBg, oscFlow.indentedRemaining.width@25)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("initialize OSCresponders with the port stored in the setup")
			;

			oscFlow.nextLine.shift(15, 0);

			activateCalibration = buildCheckbox.(false, oscBg, 15@15, staticTextFontBold)
				.action_({ |cb|
					switch(cb.value.asBoolean,
						true, {
							resetCalibration.enabled_(true);
							textResetCalibration.stringColor_(staticTextColor);
						},
						false, {
							resetCalibration.enabled_(false);
							textResetCalibration.stringColor_(Color(0.7, 0.7, 0.7));
							if(GUI.id == \cocoa, {
								resetCalibration.value_(0)
							}, { resetCalibration.value_(false) });
						}
					)
				})
			;

			textActivateCalibration = StaticText(oscBg, oscFlow.indentedRemaining.width@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("activate calibration")
			;

			oscFlow.nextLine.shift(15, 0);

			resetCalibration = buildCheckbox.(false, oscBg, 15@15, staticTextFontBold).enabled_(false);

			textResetCalibration = StaticText(oscBg, oscFlow.indentedRemaining.width@15)
				.font_(staticTextFont)
				.stringColor_(Color(0.7, 0.7, 0.7))
				.string_("reset calibration")
			;

			window.front;
		})
	}

}