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
		var buildCheckbox;

		buildCheckbox = { |active|
			var cBox;
			"window.view: %\n".postf(window.view);
			if(GUI.id === \cocoa, {
				cBox = Button(window, 15@15)
					.states_([
						["X", Color.red, Color.white],
						["", Color.white, Color.white]
					])
					.font_(Font(Font.defaultMonoFace, 15))
				;
				if(active, { cBox.value_(0) }, { cBox.value_(1) });
			}, {
				cBox = CheckBox(window.view, 15@15).value_(active);
			});
			cBox;
		};

		if(window.isNil or:{ window.isClosed }, {
			window = Window("CVCenter: preferences", Rect(
				Window.screenBounds.width/2-250,
				Window.screenBounds.height/2-175,
				500, 350
			)).front;

			staticTextFont = Font(Font.defaultSansFace, 15);
			staticTextColor = Color(0.2, 0.2, 0.2);
			textFieldFont = Font(Font.defaultMonoFace, 12);
			textFieldFontColor = Color.black;
			textFieldBg = Color.white;

			window.view.decorator = flow = FlowLayout(window.view.bounds, 7@7, 3@3);

			saveGuiPosition = buildCheckbox.(true);

			flow.shift(0, -15);

			StaticText(window.view, flow.bounds.width-20@80)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("Remember CVCenter's screen-properties on shutdown")
			;
		});
		window.front;
	}

}