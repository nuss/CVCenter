CVCenterPreferences {

	classvar <window;
	// classvar guiProps, saveClassvars;
	// classvar setMidiMode, setMidiResolution, setCtrlButtonBank, setMidiMean, setSoftWithin;

	*dialog {
		var labelColors, labelStringColors, flow;
		var staticTextFont, staticTextColor, textFieldFont, textFieldFontColor, textFieldBg;
		var saveGuiPosition, leftText, left, topText, top, widthText, width, heightText, height;
		var saveClassVars;
		var saveMidiMode, saveMidiResolution, saveCtrlButtonBank, saveMidiMean, saveSoftWithin;
		var textMidiMode, textMidiResolution, textCtrlButtonBank, textMidiMean, textSoftWithin;
		var guiVal, buildCheckbox, buildNumTextBox, uView;
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

		buildNumTextBox = { |val, kind, width, height, clip|
			var ntBox;
			case
				{ kind === \text } {
					ntBox = TextField(window.view, (width ?? { 30 }) @ (height ?? { 20 })).string_(val);
				}
				{ kind === \num } {
					ntBox = NumberBox(window.view, (width ?? { 30 }) @ ( height ?? { 20 }))
						.value_(val)
						.clipLo_(clip[0] ?? { -100 })
						.clipHi_(clip[1] ?? { 100 })
					;
				}
			;
			ntBox.font_(textFieldFont);
		};

		if(window.isNil or:{ window.isClosed }, {
			window = Window("CVCenter: preferences", Rect(
				Window.screenBounds.width/2-250,
				Window.screenBounds.height/2-162,
				500, 324
			)).front;

			window.view.decorator = flow = FlowLayout(window.view.bounds, 7@7, 3@3);

			uView = UserView(window.view, flow.bounds.width-20@50)
				.background_(Color(0.95, 0.95, 0.95))
				.enabled_(false)
			;

			// "flow.left before shift: %\n".postf(flow.left);
			flow.nextLine.shift(5, -50);
			// "flow.left after shift: %\n".postf(flow.left);

			saveGuiPosition = PopUpMenu(window.view, flow.bounds.width-30@20)
				.items_([
					"No specific settings for GUI-properties",
					"Remember GUI-properties on shutdown / window-close",
					"Remember GUI-properties as given below"
				])
				.value_(guiVal ?? { 0 })
				.font_(textFieldFont)
			;

			leftText = StaticText(window.view, flow.bounds.width/10@20)
				.string_("left: ")
				.font_(staticTextFont)
				.align_(\right)
			;

			flow.shift(0, 0);

			left = buildNumTextBox.(kind: \text, width: 60);

			flow.shift(0, 0);

			topText = StaticText(window.view, flow.bounds.width/10@20)
				.string_("top: ")
				.font_(staticTextFont)
				.align_(\right)
			;

			top = buildNumTextBox.(kind: \text, width: 60);

			flow.shift(0, 0);

			widthText = StaticText(window.view, flow.bounds.width/10@20)
				.string_("width: ")
				.font_(staticTextFont)
				.align_(\right)
			;

			width = buildNumTextBox.(kind: \text, width: 60);

			flow.shift(0, 0);

			heightText = StaticText(window.view, flow.bounds.width/10@20)
				.string_("height: ")
				.font_(staticTextFont)
				.align_(\right)
			;

			height = buildNumTextBox.(kind: \text, width: 60);

			if(saveGuiPosition.value == 0 or:{ saveGuiPosition.value == 1 }, {
				[leftText, topText, widthText, heightText].do(_.stringColor_(Color(0.7, 0.7, 0.7, 0.7)));
				[left, top, width, height].do(_.enabled_(false));
			}, {
				[leftText, topText, widthText, heightText].do(_.stringColor_(Color.black));
				[left, top, width, height].do(_.enabled_(true));
			});

			saveGuiPosition.action_({ |dd|
				if(dd.value == 0 or:{ dd.value == 1 }, {
					[leftText, topText, widthText, heightText].do(_.stringColor_(Color(0.7, 0.7, 0.7, 0.7)));
					[left, top, width, height].do(_.enabled_(false));
				}, {
					[leftText, topText, widthText, heightText].do(_.stringColor_(Color.black));											[left, top, width, height].do(_.enabled_(true));
				})
			});

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
				classVarsText.toolTip_(
					"Selecting this option will make CVCenter remember the current\nvalues for midiMode, midiResolution, midiMean, softWithin and\nctrlButtonBank. These can also be set by entering appropriate\nvalues in the following text-boxes. If this option is not selected\nany of the following values will only be remembered until the\nnext library recompilation.\nNote also that these values get overridden by the corresponding\nsettings in a CVWidget.\nFor more information please have a look at the regarding\nsections in CVCenter's helpfile."
				);
				saveClassVars.toolTip_("Select this option to make CVCenter remember the current values\nof its classvars midiMode, midiResolution, midiMean, softWithin,\nctrlButtonBank on shutdown resp. startup.")
			});

			flow.nextLine.shift(28, 0);

			saveMidiMode = buildNumTextBox.(CVCenter.midiMode, \text);

			flow.shift(5, 2);

			textMidiMode = StaticText(window.view, flow.bounds.width-100@20)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("Set CVCenter's midi-mode (0 or 1).")
			;

			if(GUI.id !== \cocoa, {
				saveMidiMode.toolTip_(
					"Either 0 (hardware-slider output 0-127)\nor 1 (in-/decremental output)."
				)
			});

			flow.nextLine.shift(28, 0);

			saveMidiResolution = buildNumTextBox.(CVCenter.midiResolution, \text);

			flow.shift(5, 2);

			textMidiResolution = StaticText(window.view, flow.bounds.width-100@20)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("Set CVCenter's midi-resolution. Applies only if midi-mode is 1.")
			;

			if(GUI.id !== \cocoa, {
				saveMidiResolution.toolTip_(
					"A floating point value representing the slider's resolution.\n0.1 has proven to be a sensible default. Smaller values mean\na higher resolution. Applies only if midiMode is set to 1."
				)
			});

			flow.nextLine.shift(28, 0);

			saveMidiMean = buildNumTextBox.(CVCenter.midiMean, \text);

			flow.shift(5, 2);

			textMidiMean = StaticText(window.view, flow.bounds.width-100@30)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("Set CVCenter's midi-mean: the default-output of your MIDI-device's\nsliders in neutral position. Applies only if midi-mode is 1.")
			;

			if(GUI.id !== \cocoa, {
				saveMidiMean.toolTip_(
					"The output of your device's sliders in neutral position.\nOnly needed if output != 0 and midiMode is set to 1."
				)
			});

			flow.nextLine.shift(28, 0);

			saveSoftWithin = buildNumTextBox.(CVCenter.softWithin, \text);

			flow.shift(5, 2);

			textSoftWithin = StaticText(window.view, flow.bounds.width-100@42)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("Set the soft-within threshold: the widget will only respond if the\ncurrent MIDI-output is within the widget's current value +/- threshold.\nApplies only if midi-mode is 0.");

			if(GUI.id !== \cocoa, {
				saveSoftWithin.toolTip_(
					"Set an arbitrary floating point value. Recomended: 0.1.\nApplies only if midiMode is set to 0."
				)
			});

			flow.nextLine.shift(28, 0);

			saveCtrlButtonBank = buildNumTextBox.(CVCenter.ctrlButtonBank, \text);

			flow.shift(5, 2);

			if(GUI.id === \cocoa, { specialHeight = 60 }, { specialHeight = 54 });

			textCtrlButtonBank = StaticText(window.view, flow.bounds.width-100@specialHeight)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("Set the number of sliders on in one bank of your MIDI-device.\nSetting this number will display the selected slider in a widget not as\na single number but rather as combination of the selected bank and\nthe slider number (e.g.: 4:3 means bank nr. 4, slider nr. 3)")
			;

			if(GUI.id !== \cocoa, {
				saveCtrlButtonBank.toolTip_(
					"Set an arbitrary integer number, corresponding\nto the number of sliders on your device."
				)
			});

			flow.nextLine.shift(0, 8);

			Button(window.view, flow.bounds.width/2-10@25)
				.states_([["Cancel", Color.black, Color.white]])
				.font_(Font(Font.defaultSansFace, 14, true))
				.action_({ window.close })
			;

			flow.shift(-2, 0);

			Button(window.view, flow.bounds.width/2-10@25)
				.states_([["Save", Color.white, Color.red]])
				.font_(Font(Font.defaultSansFace, 14, true))
				.action_({
					if(saveGuiPosition.value == 2 and:{
						[left, top, width, height].select({ |field|
							field.string.interpret.isInteger
						}).size < 4
					}, {
						[leftText, topText, widthText, heightText].do(_.stringColor_(Color.red));
						"Please supply valid values (integer numbers) for 'left', 'top', 'width', 'height'".warn;
					}, {
						this.writePreferences(
							saveGuiPosition.value,
							Rect(
								left.value.interpret,
								top.value.interpret,
								width.value.interpret,
								height.value.interpret
							),
							saveClassVars.value,
							saveMidiMode.string.interpret,
							saveMidiResolution.string.interpret,
							saveMidiMean.string.interpret,
							saveSoftWithin.string.interpret,
							saveCtrlButtonBank.string.interpret
						);
						window.close;
					})
				})
			;
		});
		window.front;
	}

	*writePreferences { |saveGuiProperties, guiProperties, saveClassVars, midiMode, midiResolution, midiMean, softWithin, ctrlButtonBank|
		var prefsPath, prefs, thisGuiProperties;

		guiProperties !? {
			if(guiProperties.isArray, {
				thisGuiProperties = guiProperties.asRect;
			}, {
				thisGuiProperties = guiProperties;
			})
		};

		if(saveGuiProperties == 2 and:{
			guiProperties.isNil
		}, {
			Error("Please provide either a Rect or an Array for your desired GUI-properties").throw;
		});

		prefsPath = this.filenameSymbol.asString.dirname +/+ "CVCenterPreferences";
		if(File.exists(prefsPath), {
			prefs = Object.readArchive(prefsPath);
		}, {
			prefs = ();
		});
		prefs.put(\saveGuiProperties, saveGuiProperties);
		if(saveGuiProperties == 2, { prefs.put(\guiProperties, thisGuiProperties) });
		if(saveClassVars, {
			prefs.put(\midiMode, midiMode);
			prefs.put(\midiResolution, midiResolution);
			prefs.put(\midiMean, midiMean);
			prefs.put(\softWithin, softWithin);
			prefs.put(\ctrlButtonBank, ctrlButtonBank);
		}, {
			#[midiMode, midiResolution, midiMean, softWithin, ctrlButtonBank].do(prefs.removeAt(_));
		});
		CVCenter.midiMode_(midiMode);
		CVCenter.midiResolution_(midiResolution);
		CVCenter.midiMean_(midiMean);
		CVCenter.softWithin_(softWithin);
		CVCenter.ctrlButtonBank_(ctrlButtonBank);
		prefs.writeArchive(prefsPath);
	}

	*readPreferences { |...args|
		var prefsPath, prefs, res;
		prefsPath = this.filenameSymbol.asString.dirname +/+ "CVCenterPreferences";
		if(File.exists(prefsPath), {
			prefs = Object.readArchive(prefsPath);
			if(args.size > 0, {
				res = ();
				args.do({ |val| res.put(val.asSymbol, prefs[val.asSymbol]) });
				^res;
			}, {
				^prefs;
			})
		})
		^nil;
	}
}