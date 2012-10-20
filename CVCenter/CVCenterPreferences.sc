CVCenterPreferences {

	classvar <window;
	// classvar guiProps, saveClassvars;
	// classvar setMidiMode, setMidiResolution, setCtrlButtonBank, setMidiMean, setSoftWithin;

	*makeWindow {
		var labelColors, labelStringColors, flow;
		var staticTextFont, staticTextColor, textFieldFont, textFieldFontColor, textFieldBg;
		var saveGuiPosition;
		var saveClassVars;
		var saveMidiMode, saveMidiResolution, saveCtrlButtonBank, saveMidiMean, saveSoftWithin;
		var textMidiMode, textMidiResolution, textCtrlButtonBank, textMidiMean, textSoftWithin;
		var buildCheckbox, buildNumTextBox, uView;
		var cvcenterBounds, propsText, classVarsText;
		var fFact, specialHeight;

		if(GUI.id === \cocoa, { fFact = 0.9 }, { fFact = 1 });

		staticTextFont = Font(Font.defaultSansFace, 13 * fFact);
		staticTextColor = Color(0.2, 0.2, 0.2);
		textFieldFont = Font(Font.defaultMonoFace, 12);
		textFieldFontColor = Color.black;
		textFieldBg = Color.white;

		buildCheckbox = { |active|
			var cBox;
			if(GUI.id === \cocoa, {
				cBox = Button(window.view, 15@15)
					.states_([
						["X", Color.black, Color.white],
						["", Color.white, Color.white]
					])
					.font_(Font(Font.defaultSansFace, 10, true))
				;
				if(active, { cBox.value_(0) }, { cBox.value_(1) });
			}, {
				cBox = CheckBox(window.view, 15@15).value_(active);
			});
			// cBox.bounds.postln;
			cBox;
		};

		buildNumTextBox = { |val, kind, clip|
			var ntBox;
			case
				{ kind === \text } {
					ntBox = TextField(window.view, 30@20).string_(val);
				}
				{ kind === \num } {
					ntBox = NumberBox(window.view, 30@20)
						.value_(val)
						.clipLo_(clip[0])
						.clipHi_(clip[1])
					;
				}
			;
			ntBox.font_(textFieldFont);
		};

		if(window.isNil or:{ window.isClosed }, {
			window = Window("CVCenter: preferences", Rect(
				Window.screenBounds.width/2-250,
				Window.screenBounds.height/2-140,
				500, 280
			)).front;

			window.view.decorator = flow = FlowLayout(window.view.bounds, 7@7, 3@3);

			uView = UserView(window.view, flow.bounds.width-20@40)
				.background_(Color(0.95, 0.95, 0.95))
				.enabled_(false)
			;

			// "flow.left before shift: %\n".postf(flow.left);
			flow.nextLine.shift(5, -40);
			// "flow.left after shift: %\n".postf(flow.left);

			saveGuiPosition = buildCheckbox.(false);

			flow.shift(5, 1);

			cvcenterBounds = CVCenter.window !? { CVCenter.bounds };

			propsText = StaticText(window.view, flow.bounds.width-100@30)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("Remember CVCenter's screen-properties on shutdown.\nThe current properties are: x: %, y: %, width: %, height: %".format(
					cvcenterBounds !? { cvcenterBounds.left },
					cvcenterBounds !? { cvcenterBounds.top },
					cvcenterBounds !? { cvcenterBounds.width },
					cvcenterBounds !? { cvcenterBounds.height }
				))
			;

			flow.nextLine.shift(0, 10);

			uView = UserView(window.view, flow.bounds.width-20@220)
				.background_(Color(0.95, 0.95, 0.95))
				.enabled_(false)
			;

			// "flow.left before shift: %\n".postf(flow.left);
			flow.nextLine.shift(5, -220);
			// "flow.left after shift: %\n".postf(flow.left);

			saveClassVars = buildCheckbox.(false);

			flow.shift(5, 1);

			classVarsText = StaticText(window.view, flow.bounds.width-100@20)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("Remember CVCenter's classvar-values on shutdown.")
			;

			if(GUI.id !== \cocoa, {
				[saveClassVars, classVarsText].do(_.toolTip_(
					"Selecting this option will make CVCenter remember\nthe currently set values for midiMode, midiResolution,\nmidiMean, softWithin and ctrlButtonBank.\nFor more information please have a look at the regarding\nsections in CVCenter's helpfile."
				))
			});

			flow.nextLine.shift(28, 0);

			saveMidiMode = buildNumTextBox.(CVCenter.midiMode, \text);

			flow.shift(5, 2);

			textMidiMode = StaticText(window.view, flow.bounds.width-100@20)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("Set CVCenter's midi-mode (0 or 1).")
			;

			flow.nextLine.shift(28, 0);

			saveMidiResolution = buildNumTextBox.(CVCenter.midiResolution, \text);

			flow.shift(5, 2);

			textMidiResolution = StaticText(window.view, flow.bounds.width-100@20)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("Set CVCenter's midi-resolution. Applies only if midi-mode is 1.")
			;

			flow.nextLine.shift(28, 0);

			saveMidiMean = buildNumTextBox.(CVCenter.midiMean, \text);

			flow.shift(5, 2);

			textMidiMean = StaticText(window.view, flow.bounds.width-100@30)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("Set CVCenter's midi-mean: the default-output of your MIDI-device's\nsliders in neutral position. Applies only if midi-mode is 1.")
			;

			flow.nextLine.shift(28, 0);

			saveSoftWithin = buildNumTextBox.(CVCenter.softWithin, \text);

			flow.shift(5, 2);

			textSoftWithin = StaticText(window.view, flow.bounds.width-100@42)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("Set the soft-within threshold: the widget will only respond if the\ncurrent MIDI-output is within the widget's current value +/- threshold.\nApplies only if midi-mode is 0.");

			flow.nextLine.shift(28, 0);

			saveSoftWithin = buildNumTextBox.(CVCenter.ctrlButtonBank, \text);

			flow.shift(5, 2);

			if(GUI.id === \cocoa, { specialHeight = 60 }, { specialHeight = 54 });

			textSoftWithin = StaticText(window.view, flow.bounds.width-100@specialHeight)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
			.string_("Set the numbers of sliders on in one bank of your MIDI-device.\nSetting this number will display the selected slider in a widget not as\na single number but rather as combination of the selected bank and the\nslider number (e.g.: 4:3 means bank nr. 4, slider nr. 3)")
			;

		});
		window.front;
	}

}