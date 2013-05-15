CVCenterLoadDialog {

	classvar <window, midiSources;

	*new {
		var staticTextFont, staticTextFontBold, staticTextColor, textFieldFont, textFieldFontColor, textFieldBg;
		var buildCheckbox;
		var flow, replaceBg, midiBg, oscBg, actionsBg;
		var replaceFlow, midiFlow, oscFlow, actionsFlow;
		var replaceExisting, textReplaceExisting;
		var loadMidiCC, loadMidiSrc, loadMidiChan, loadMidiCtrl;
		var textMidiSrc, textMidiChan, textMidiCtrl;
		var loadOscResponders, loadOscIP, loadOscPort, activateCalibration, resetCalibration;
		var textOscIP, textOscPort, textActivateCalibration, textResetCalibration;
		var activateActions, textActivateActions;
		var cancelBut, loadBut;
		var lineheight, linebreak, fFact;
		var initCCSrc, initCCChan, initCCCtrl;
		var initOscIP, initOscPort, initCalib, initCalibReset;
		var textOscSelect, textRestrictToPort, textMidiSelect;
		var midiInitBut, midiSourceSelect, sourceNames;
		var oscIPSelect, restrictToPort, oscAddrList;
		var midiSrcID, oscIPAddress;

		OSCCommands.collectTempIPsAndCmds;

		if(GUI.id === \cocoa, { fFact = 0.9 }, { fFact = 1 });

		staticTextFont = Font("Arial", 12 * fFact);
		staticTextFontBold = Font("Arial", 12 * fFact, true);
		staticTextColor = Color(0.2, 0.2, 0.2);
		textFieldFont = Font("Andale Mono", 12);
		textFieldFontColor = Color.black;
		textFieldBg = Color.white;

		if(GUI.id == \cocoa, {
			lineheight = { |lines| if(lines > 1, { (18 * lines)-3 }, { 18 }) };
			linebreak = "\n";
		}, {
			lineheight = { |lines| if(lines > 1, { (18 * lines)-5 }, { 18 }) };
			linebreak = " ";
		});

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
				(Window.screenBounds.height-328).div(2),
				500, 328
			), false);

			window.view.decorator = flow = FlowLayout(window.view.bounds, 7@7, 3@3);

			replaceBg = CompositeView(window.view, flow.indentedRemaining.width@29);
			flow.nextLine;
			midiBg = CompositeView(window.view, flow.bounds.width.div(2)-8@218);
			oscBg = CompositeView(window.view, flow.indentedRemaining.width@218);
			flow.nextLine;
			actionsBg = CompositeView(window.view, flow.indentedRemaining.width@29);
			[replaceBg, midiBg, oscBg, actionsBg].do({ |el| el.background_(Color(0.95, 0.95, 0.95)) });

			replaceBg.decorator = replaceFlow = FlowLayout(replaceBg.bounds, 7@7, 3@3);
			midiBg.decorator = midiFlow = FlowLayout(midiBg.bounds, 7@7, 3@3);
			oscBg.decorator = oscFlow = FlowLayout(oscBg.bounds, 7@7, 3@3);
			actionsBg.decorator = actionsFlow = FlowLayout(actionsBg.bounds, 7@7, 3@3);

			// replace existing widgets in CVCenter or not

			replaceExisting = buildCheckbox.(true, replaceBg, 15@15, staticTextFontBold);

			textReplaceExisting = StaticText(replaceBg, replaceFlow.indentedRemaining.width@15)
				.font_(staticTextFont)
				.string_("replace the existing setup in CVCenter")
			;

			// midi

			StaticText(midiBg, midiFlow.indentedRemaining.width@20)
				.font_(staticTextFontBold)
				.string_("MIDI options")
			;

			loadMidiCC = buildCheckbox.(true, midiBg, 15@15, staticTextFontBold)
				.action_({ |cb|
					switch(cb.value.asBoolean,
						true, {
							[loadMidiSrc, loadMidiChan, loadMidiCtrl, midiInitBut, midiSourceSelect].do(_.enabled_(true));
							[textMidiSrc, textMidiChan, textMidiCtrl, textMidiSelect].do(_.stringColor_(staticTextColor));
						},
						false, {
							[loadMidiSrc, loadMidiChan, loadMidiCtrl, midiInitBut, midiSourceSelect].do(_.enabled_(false));
							[textMidiSrc, textMidiChan, textMidiCtrl, textMidiSelect].do(_.stringColor_(Color(0.7, 0.7, 0.7)));
						}
					)
				})
			;

			StaticText(midiBg, midiFlow.indentedRemaining.width@lineheight.(1))
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("load CCResponders")
			;

			midiFlow.nextLine.shift(15, 0);

			loadMidiCtrl = buildCheckbox.(true, midiBg, 15@15, staticTextFontBold);

			textMidiCtrl = StaticText(midiBg, midiFlow.indentedRemaining.width@lineheight.(2))
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("initialize CCResponders with ctrl-nr.%stored in the setup".format(linebreak))
			;

			midiFlow.nextLine.shift(15, 0);

			loadMidiChan = buildCheckbox.(true, midiBg, 15@15, staticTextFontBold);

			textMidiChan = StaticText(midiBg, midiFlow.indentedRemaining.width@lineheight.(2))
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("initialize CCResponders with channel-nr.%stored in the setup".format(linebreak))
			;

			midiFlow.nextLine.shift(15, 0);

			loadMidiSrc = buildCheckbox.(false, midiBg, 15@15, staticTextFontBold);

			textMidiSrc = StaticText(midiBg, midiFlow.indentedRemaining.width@lineheight.(2))
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("initialize CCResponders with source-ID%stored in the setup".format(linebreak))
			;

			midiFlow.nextLine.shift(15, 0);

			textMidiSelect = StaticText(midiBg, midiFlow.indentedRemaining.width@lineheight.(2))
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("... or select from a list of currently%available sources".format(linebreak))
			;

			midiFlow.nextLine.shift(15, 0);

			midiInitBut = Button(midiBg, 60@15).font_(Font("Arial", 9, true));

			if(MIDIClient.initialized, {
				midiInitBut.states_([
					["restart MIDI", Color.black, Color.green]
				])
			}, {
				midiInitBut.states_([
					["init MIDI", Color.white, Color.red]
				]).action_({ |b|
					midiSources = ();
					if(MIDIClient.initialized, {
						MIDIClient.restart; MIDIIn.connectAll;
					}, { MIDIClient.init; MIDIIn.connectAll });
					if(MIDIClient.initialized, {
						b.states_([
							["restart MIDI", Color.black, Color.green]
						]);
						MIDIClient.sources.do({ |source|
							if(midiSources.values.includes(source.uid.asInt).not, {
								// OSX/Linux specific tweek
								if(source.name == source.device, {
									midiSources.put(source.name.asSymbol, source.uid.asInt)
								}, {
									midiSources.put(
										(source.device++":"+source.name).asSymbol, source.uid.asInt
									)
								})
							})
						});
						sourceNames = midiSources.keys.asArray.sort;
						midiSourceSelect.items_(
							[midiSourceSelect.items[0]]++sourceNames;
						);
					})
				})
			});

			midiSourceSelect = PopUpMenu(midiBg, midiFlow.indentedRemaining.width@15)
				.font_(Font("Arial", 9))
				.items_(["select device port..."])
				.action_({ |dd|
					if(dd.value != 0, {
						loadMidiSrc.enabled_(false); textMidiSrc.stringColor_(Color(0.7, 0.7, 0.7));
					}, {
						loadMidiSrc.enabled_(true); textMidiSrc.stringColor_(staticTextColor);
					})
				})
			;

			if(MIDIClient.initialized, {
				MIDIClient.sources.do({ |source|
					if(midiSources.values.includes(source.uid.asInt).not, {
						// OSX/Linux specific tweek
						if(source.name == source.device, {
							midiSources.put(source.name.asSymbol, source.uid.asInt)
						}, {
							midiSources.put(
								(source.device++":"+source.name).asSymbol, source.uid.asInt
							)
						})
					})
				});
				sourceNames = midiSources.keys.asArray.sort;
				midiSourceSelect.items_(
					[midiSourceSelect.items[0]]++sourceNames
				);
			});

			// osc

			StaticText(oscBg, oscFlow.indentedRemaining.width@20)
				.font_(staticTextFontBold)
				.string_("OSC options")
			;

			loadOscResponders = buildCheckbox.(true, oscBg, 15@15, staticTextFontBold)
				.action_({ |cb|
					switch(cb.value.asBoolean,
						true, {
							[loadOscIP, activateCalibration, oscIPSelect, restrictToPort].do(_.enabled_(true));
							if(loadOscIP.value.asBoolean, {
								loadOscPort.enabled_(true);
								textOscPort.stringColor_(staticTextColor);
							});
							if(activateCalibration.value.asBoolean, {
								resetCalibration.enabled_(true);
								textResetCalibration.stringColor_(staticTextColor);
							});
							[textOscIP, textActivateCalibration, textOscSelect, textRestrictToPort].do(_.stringColor_(staticTextColor));
						},
						false, {
							[loadOscIP, loadOscPort, activateCalibration, resetCalibration, oscIPSelect, restrictToPort].do(
								_.enabled_(false)
							);
							[textOscIP, textOscPort, textActivateCalibration, textResetCalibration, textOscSelect, textRestrictToPort].do(
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

			StaticText(oscBg, oscFlow.indentedRemaining.width@18)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("load OSCresponders")
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

			textOscIP = StaticText(oscBg, oscFlow.indentedRemaining.width@lineheight.(2))
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("initialize OSCresponders with IP-%addresses stored in the setup".format(linebreak))
			;

			oscFlow.nextLine.shift(15, 0);

			loadOscPort = buildCheckbox.(false, oscBg, 15@15, staticTextFontBold);

			textOscPort = StaticText(oscBg, oscFlow.indentedRemaining.width@lineheight.(2))
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("initialize OSCresponders with the port%stored in the setup".format(linebreak))
			;

			oscFlow.nextLine.shift(15, 0);

			textOscSelect = StaticText(oscBg, oscFlow.indentedRemaining.width@lineheight.(2))
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("... or select from a list of currently%available addresses".format(linebreak))
			;

			oscFlow.nextLine.shift(15, 0);

			oscIPSelect = PopUpMenu(oscBg, 130@15)
				.font_(Font("Arial", 9))
				.items_(["select IP-address..."])
				.mouseDownAction_({ |m|
					if(restrictToPort.value.asBoolean, {
						oscAddrList = OSCCommands.tempIPsAndCmds.keys.asArray.sort;
					}, {
						oscAddrList = OSCCommands.tempIPsAndCmds.keys.collect({ |it| it.asString.split($:)[0] }).asBag.contents.keys.asArray.sort;
					});
					m.items_(
						[m.items[0]]++oscAddrList;
					);
					[loadOscIP, loadOscPort].do(_.enabled_(true));
					[textOscIP, textOscPort].do(_.stringColor_(staticTextColor));
				})
				.action_({ |dd|
					if(dd.value != 0, {
						[loadOscIP, loadOscPort].do(_.enabled_(false));
						[textOscIP, textOscPort].do(_.stringColor_(Color(0.7, 0.7, 0.7)));
					})
				})
			;

			textRestrictToPort = StaticText(oscBg, 60@15)
				.string_("restrict to port")
				.font_(Font("Arial", 9))
				.align_(\right)
			;

			restrictToPort = buildCheckbox.(false, oscBg, 15@15, staticTextFontBold)
				.action_({ |cb|
					switch(cb.value.asBoolean,
						true, {
							oscIPSelect.items_(
								["select IP-address:port..."]++OSCCommands.tempIPsAndCmds.keys.collect({ |it|
									it.asString.split($:)[0]
								}).asBag.contents.keys.asArray.sort
							)
						},
						false, {
							oscIPSelect.items_(
								["select IP-address..."]++OSCCommands.tempIPsAndCmds.keys.asArray.sort
							)
						}
					);
					[loadOscIP, loadOscPort].do(_.enabled_(true));
					[textOscIP, textOscPort].do(_.stringColor_(staticTextColor));
				})
			;

			// { OSCCommands.tempIPsAndCmds.postcs }.defer(0.2);

			// actions

			activateActions = buildCheckbox.(true, actionsBg, 15@15, staticTextFontBold);

			textActivateActions = StaticText(actionsBg, actionsFlow.indentedRemaining.width@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("load all CVWidget-actions stored in the setup")
			;

			flow.nextLine;

			// cancel or load a setup;

			cancelBut = Button(window.view, flow.bounds.width.div(2)-8@flow.indentedRemaining.height)
				.states_([["Cancel", Color.black, Color.white]])
				.font_(Font("Arial", 14, true))
				.action_({ |b| window.close })
			;

			loadBut = Button(window.view, flow.indentedRemaining.width@flow.indentedRemaining.height)
				.states_([["Load Setup", Color.white, Color.red]])
				.font_(Font("Arial", 14, true))
				.action_({ |b|
					if(loadOscResponders.value.asBoolean, {
						if(oscIPSelect.value == 0, {
							initOscIP = loadOscIP.value.asBoolean;
							if(initOscIP, { initOscPort = loadOscPort.value.asBoolean });
						}, {
							oscIPAddress = oscIPSelect.item;
						});
						initCalib = activateCalibration.value.asBoolean;
						if(initCalib, { initCalibReset = resetCalibration.value.asBoolean });
					});
					if(loadMidiCC.value.asBoolean, {
						if(midiSourceSelect.value == 0, {
							initCCSrc = loadMidiSrc.value.asBoolean;
						}, {
							midiSrcID = midiSources[midiSourceSelect.item.asSymbol];
						});
						initCCChan = loadMidiChan.value.asBoolean;
						initCCCtrl = loadMidiCtrl.value.asBoolean;
					});
					CVCenter.loadSetup(
						addToExisting: replaceExisting.value.asBoolean,
						autoConnectOSC: loadOscResponders.value.asBoolean,
						oscConnectToIP: initOscIP,
						oscRestrictToPort: initOscPort,
						activateCalibration: initCalib,
						resetCalibration: initCalibReset,
						autoConnectMIDI: loadMidiCC.value.asBoolean,
						loadActions: activateActions.value.asBoolean,
						midiSrcID: midiSrcID,
						oscIPAddress: oscIPAddress
					)
				})
			;
		});
		window.front;
	}

}