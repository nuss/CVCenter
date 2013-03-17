/* (c) 2010-2013 Stefan Nussbaumer */
/*
 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
*/

CVWidgetMSEditor : AbstractCVWidgetEditor {

	var msEditorEnv;
	var <extMidiCtrlArrayField;
	var <extOscCtrlArrayField, <intStartIndexField;
	var <oscEditBtns, <oscCalibBtns;
	var <midiEditGroups;
	var <oscTabs, <midiTabs;
	var oscFlow0, oscFlow1, midiFlow0, midiFlow1;

	*new { |widget, widgetName, tab|
		^super.new.init(widget, widgetName, tab);
	}

	init { |widget, widgetName, tab|
		var cvString;
		// var oscTabs, midiTabs;
		var staticTextFont, staticTextFontBold, staticTextColor, textFieldFont, textFieldFontColor, textFieldBg;
		var maxNum, msrc = "source", mchan = "channel", mctrl = "ctrl", margs;
		var addr, wcmMS, thisGuiEnv, labelColors;
		var midiUid, midiChan;
		var oscLabelColor, midiLabelColor, oscLabelStringColor, midiLabelStringColor;
		var oscConnectCondition = 0;
		var oscConnectWarning = "Couldn't connect OSC-controllers:";
		var connectIP, connectPort, connectName, connectOscMsgIndex, connectIndexStart;
		var deviceListMenu, cmdListMenu, addDeviceBut, thisCmdNames;
		var midiModes, nextMidiX, nextMidiY;
		var thisMidiMode, thisMidiMean, thisMidiResolution, thisSoftWithin, thisCtrlButtonBank;
		var mappingSelectItems, mappingDiffers;
		var wdgtActions;
		var cmdNames, orderedCmds, orderedCmdSlots;
		var tmp, gapNextX, gapNextY;

		widget ?? {
			Error("CVWidgetEditor is a utility-GUI-class that can only be used in connection with an existing CVWidget").throw;
		};

		#oscEditBtns, oscCalibBtns, midiEditGroups = []!3;

		name = (widgetName.asString++"MS").asSymbol;
		nextX ?? { nextX = 0 }; nextY ?? { nextY = 0 };
		xySlots ?? { xySlots = [] };
		msEditorEnv = ();

//		"widget: %\n".postf(widget.msSize);

		#thisMidiMode, thisMidiMean, thisMidiResolution, thisSoftWithin, thisCtrlButtonBank = Array.newClear(widget.msSize)!5;

		cmdNames ?? { cmdNames = OSCCommands.deviceCmds };
		thisCmdNames ?? { thisCmdNames = [nil] };

		actionsList ?? { actionsList = () };

		thisGuiEnv = widget.guiEnv;

		widget.wdgtControllersAndModels !? {
			wcmMS = widget.wdgtControllersAndModels;
		};
		widget.msSize.do({ |i|
			thisMidiMode[i] = widget.getMidiMode(i);
			thisMidiMean[i] = widget.getMidiMean(i);
			thisMidiResolution[i] = widget.getMidiResolution(i);
			thisSoftWithin[i] = widget.getSoftWithin(i);
			thisCtrlButtonBank[i] = widget.getCtrlButtonBank(i);
		});

		staticTextFont = Font("Arial", 9.4);
		staticTextFontBold = Font("Arial", 9.4, true);
		staticTextColor = Color(0.2, 0.2, 0.2);
		textFieldFont = Font("Andale Mono", 9);
		textFieldFontColor = Color.black;
		textFieldBg = Color.white;

		// allEditors ?? { allEditors = IdentityDictionary() };

		if(thisEditor.isNil or:{ thisEditor.window.isClosed }, {

			// any seats left empty?
			block { |break|
				xySlots.do({ |p, i|
					if(p[1] == 0, {
						break.value(
							#gapNextX, gapNextY = p[0].asArray;
							xySlots[i][1] = name;
						);
					})
				})
			};

			window = Window("Widget Editor:"+widgetName, Rect(
				gapNextX ?? { nextX }, gapNextY ?? { nextY }, 400, 265
			));

			xySlots = xySlots.add([nextX@nextY, name]);
			if(nextX+275 > Window.screenBounds.width, {
				nextX = shiftXY ?? { 0 }; nextY = xySlots.last[0].y+295;
			}, {
				nextX = xySlots.last[0].x+405; nextY = xySlots.last[0].y;
			});

			allEditors.put(name, (editor: this, window: window, name: widgetName));
			thisEditor = allEditors[name];

			if(Quarks.isInstalled("wslib"), { window.background_(Color.white) });
		});

		tabs = TabbedView(window, Rect(0, 1, window.bounds.width, window.bounds.height), ["Specs", "MIDI", "OSC", "Actions"], scroll: true);
		tabs.view.resize_(5);
		tabs.tabCurve_(4);
		tabs.labelColors_(Color.white!4);
		labelColors = [
			Color(1.0, 0.3), //specs
			Color.red, //midi
			Color(0.0, 0.5, 0.5), //osc
			Color(0.32, 0.67, 0.76), //actions
		];
		labelStringColors = labelColors.collect({ |c| Color(c.red * 0.8, c.green * 0.8, c.blue * 0.8) });
		tabs.unfocusedColors_(labelColors);
		tabs.stringColor_(Color.white);
		tabs.views[0].decorator = flow0 = FlowLayout(window.view.bounds, 7@7, 3@3);
		tabs.views[1].decorator = flow1 = FlowLayout(window.view.bounds, 0@0, 0@0);
		tabs.views[2].decorator = flow2 = FlowLayout(window.view.bounds, 0@0, 0@0);
		tabs.views[3].decorator = flow3 = FlowLayout(window.view.bounds, 7@7, 3@3);
		tabs.views.do({ |v| v.background_(Color(0.8, 0.8, 0.8, 1.0)) });
		tabs.focusActions_((0..tabs.views.size-1).collect({ |t|
			{
				tabs.stringFocusedColor_(labelStringColors[t]);
				{ tabs.views[t].background_(Color(0.8, 0.8, 0.8, 1.0)) }.defer(0.01);
			}
		}));
		tabs.stringFocusedColor_(labelStringColors[tab]);

		oscTabs = TabbedView(tabs.views[2], Rect(0, 1, tabs.views[2].bounds.width-4, tabs.views[2].bounds.height-4), ["Batch Connection", "Individual Sliders"], scroll: true);
		oscTabs.view.resize_(tabs.view.resize);
		oscLabelColor = labelColors[2];
		oscLabelStringColor = labelStringColors[2];
		oscTabs.tabCurve_(4)
			.labelColors_(Color.white!2)
			.unfocusedColors_([oscLabelColor])
			.stringColor_(Color.white)
			.stringFocusedColor_(oscLabelStringColor)
		;
		oscTabs.views[0].decorator = oscFlow0 = FlowLayout(window.view.bounds, 7@7, 3@3);
		oscTabs.views[1].decorator = oscFlow1 = FlowLayout(window.view.bounds, 7@7, 3@3);
		oscTabs.views.do({ |v| v.background_(Color(0.8, 0.8, 0.8, 1.0)) });
		oscTabs.focusActions_((0..oscTabs.views.size-1).collect({ |t|
			{
				{ oscTabs.views[t].background_(Color(0.8, 0.8, 0.8, 1.0)) }.defer(0.01);
			}
		}));

		midiTabs = TabbedView(tabs.views[1], Rect(0, 1, tabs.views[1].bounds.width-4, tabs.views[1].bounds.height-4), ["Batch Connection", "Individual Sliders"], scroll: true);
		midiTabs.view.resize_(tabs.view.resize);
		midiLabelColor = labelColors[1];
		midiLabelStringColor = labelStringColors[1];
		midiTabs.tabCurve_(4)
			.labelColors_(Color.white!2)
			.unfocusedColors_([midiLabelColor])
			.stringColor_(Color.white)
			.stringFocusedColor_(midiLabelStringColor)
		;
		midiTabs.views[0].decorator = midiFlow0 = FlowLayout(window.view.bounds, 7@7, 3@3);
		midiTabs.views[1].decorator = midiFlow1 = FlowLayout(window.view.bounds, 7@7, 3@3);
		midiTabs.views.do({ |v| v.background_(Color(0.8, 0.8, 0.8, 1.0)) });
		midiTabs.focusActions_((0..midiTabs.views.size-1).collect({ |t|
			{
				{ midiTabs.views[t].background_(Color(0.8, 0.8, 0.8, 1.0)) }.defer(0.01);
			}
		}));

		thisEditor.tabs = tabs;
		thisEditor.oscTabs = oscTabs;
		thisEditor.midiTabs = midiTabs;

		thisEditor.tabs.view.keyDownAction_({ |view, char, modifiers, unicode, keycode|
//				[view, char, modifiers, unicode, keycode].postln;
			switch(unicode,
				111, { thisEditor.tabs.focus(2) }, // "o" -> osc
				109, { thisEditor.tabs.focus(1) }, // "m" -> midi
				97, { thisEditor.tabs.focus(3) }, // "a" -> actions
				115, { thisEditor.tabs.focus(0) }, // "s" -> specs
				120, { this.close }, // "x" -> close editor
				99, { OSCCommands.makeWindow } // "c" -> collect OSC-commands resp. open the collector's GUI
			)
		});

		maxNum = [
			widget.getSpec.minval.size,
			widget.getSpec.maxval.size,
			widget.getSpec.default.size
		].maxItem;

		StaticText(thisEditor.tabs.views[0], flow0.bounds.width-20@50)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("Enter a ControlSpec in the textfield:\ne.g. ControlSpec(20, 20000, \\exp, 0.0, 440, \"Hz\") or \\freq or [[20, 20, 20, 20, 20], [20000, 20000, 20000, 20000, 20000], \\exp].asSpec. Or select a suitable ControlSpec from the List below. If you don't know what this all means have a look at the ControlSpec-helpfile.")
		;

		flow0.shift(0, 5);

		StaticText(thisEditor.tabs.views[0], flow0.bounds.width-20@45)
			.font_(staticTextFontBold)
			.stringColor_(staticTextColor)
			.string_("NOTE: CVWidgetMS expects a Spec whose minvals, maxvals, step-sizes and/or default-values are arrays of the size of the number of sliders in the multislider. However, you may provide a spec like 'freq' and its parameters will internally expanded to arrays of the required size.")
		;

		flow0.shift(0, 5);

		StaticText(thisEditor.tabs.views[0], flow0.bounds.width-20@10)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("Enter the desired Spec and execute it by hitting shift+return.")
		;

		flow0.shift(0, 5);

		cvString = widget.getSpec.asCompileString;
		specField = TextView(thisEditor.tabs.views[0], flow0.bounds.width-20@70)
			.font_(staticTextFont)
			.string_(cvString)
			.keyDownAction_({ |tf, char, modifiers, unicode, keycode|
//				[tf, char, modifiers, unicode, keycode].postcs;
				if(char == $\r and:{ modifiers == 131072 }, {
					widget.setSpec(tf.string.interpret);
					widget.mSlider.refresh;
				})
			})
		;

		flow0.shift(0, 5);

		specsList = PopUpMenu(thisEditor.tabs.views[0], flow0.bounds.width-20@20)
			.action_({ |sl|
				widget.setSpec(specsListSpecs[sl.value])
			})
		;

		if(msEditorEnv.specsListSpecs.isNil, {
			specsListSpecs = List()
		}, {
			specsListSpecs = msEditorEnv.specsListSpecs;
		});

		// "Spec.specs: %\n".postf(Spec.specs);

		// Spec.specs.pairsDo({ |name, spec| [name, spec].postln });

		if(msEditorEnv.specsListItems.notNil, {
			specsList.items_(msEditorEnv.specsListItems);
		}, {
			Spec.specs.asSortedArray.do({ |spec|
				if(spec[1].isKindOf(ControlSpec), {
					if((tmp = [spec[1].minval, spec[1].maxval, spec[1].step, spec[1].default].select(_.isArray)).size > 0, {
						// "array: %\n".postf(spec[1]);
						if(tmp.collect(_.size).includes(widget.msSize), {
							specsList.items_(specsList.items.add(spec[0]++":"+spec[1]));
							specsListSpecs.add(spec[1]);
						});
					}, {
						// "no array: %\n".postf(spec[1]);
						specsList.items_(specsList.items.add(spec[0]++":"+spec[1]));
						specsListSpecs.add(spec[1]);
					})
				})
			})
		});

		tmp = specsListSpecs.detectIndex({ |spec, i| spec == widget.getSpec });
		if(tmp.notNil, {
			specsList.value_(tmp);
		}, {
			specsListSpecs.array_([widget.getSpec]++specsListSpecs.array);
			specsList.items = List["custom:"+widget.getSpec.asString]++specsList.items;
		});

		StaticText(thisEditor.midiTabs.views[0], midiFlow0.bounds.width-20@20)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("The following options will apply to all MIDI-responders that get connected within this widget. However you may override these settings within the MIDI-options for any particular slider.")
		;

		midiFlow0.shift(0, 7);

		midiModes = ["0-127", "+/-"];

		midiModeSelect = PopUpMenu(thisEditor.midiTabs.views[0], midiFlow0.bounds.width/5-7@15)
			.font_(staticTextFont)
			.items_(midiModes)
			.action_({ |ms|
				widget.msSize.do({ |sl|
					if(ms.value != 2, {
						widget.setMidiMode(ms.value, sl);
					})
				})
			})
		;

		if(thisMidiMode.minItem == thisMidiMode.maxItem, {
			midiModeSelect.value_(thisMidiMode[0])
		}, {
			midiModeSelect.items = midiModeSelect.items.add("--");
			midiModeSelect.value_(midiModeSelect.items.last);
		});

		if(GUI.id !== \cocoa, {
			midiModeSelect.toolTip_("Set the mode according to the output\nof your MIDI-device: 0-127 if it outputs\nabsolute values or +/- for in- resp.\ndecremental values")
		});

		midiMeanNB = TextField(thisEditor.midiTabs.views[0], midiFlow0.bounds.width/5-7@15)
			.font_(staticTextFont)
			.action_({ |mb|
				// string for possible compilation to an int
				if("^[-+]?[0-9]*$".matchRegexp(mb.string), {
					widget.msSize.do({ |sl|
						widget.setMidiMean(mb.string.asInt, sl);
					})
				}, {
					Error("MIDI-mean must be an integer value!").throw;
				})
			})
		;

		if(GUI.id !== \cocoa, {
			midiMeanNB.toolTip_("If your device outputs in-/decremental\nvalues often a slider's output in neutral\nposition will not be 0. E.g. it could be 64")
		});

		if(thisMidiMean.select(_.isInteger).size == widget.msSize and:{
			thisMidiMean.minItem == thisMidiMean.maxItem;
		}, {
			midiMeanNB.string_(thisMidiMean[0].asString);
		}, {
			midiMeanNB.string_("--");
		});

		softWithinNB = TextField(thisEditor.midiTabs.views[0], midiFlow0.bounds.width/5-7@15)
			.font_(staticTextFont)
			.action_({ |mb|
				// string must be a valid float or integer
				if("^[0-9]*\.?[0-9]*$".matchRegexp(mb.string), {
					widget.msSize.do({ |sl|
						widget.setSoftWithin(mb.string.asFloat, sl);
					})
				})
			})
		;

		if(GUI.id !== \cocoa, {
			softWithinNB.toolTip_("If your device outputs absolute values\nyou can set here a threshold to the\ncurrent CV-value within which a slider\nwill react and set a new value. This avoids\njumps if a new value set by a slider\nis far away from the previous value")
		});

		if(thisSoftWithin.select(_.isNumber).size == widget.msSize and:{
			thisSoftWithin.minItem == thisSoftWithin.maxItem;
		}, {
			softWithinNB.string_(thisSoftWithin[0].asString);
		}, {
			softWithinNB.string_("--");
		});

		midiResolutionNB = TextField(thisEditor.midiTabs.views[0], midiFlow0.bounds.width/5-7@15)
			.font_(staticTextFont)
			.action_({ |mb|
				if("^[0-9]*\.?[0-9]*$".matchRegexp(mb.string), {
					widget.msSize.do({ |sl|
						widget.setMidiResolution(mb.string.asFloat, sl);
					})
				})
			})
		;

		if(GUI.id !== \cocoa, {
			midiResolutionNB.toolTip_("Higher values mean lower\nresolution and vice versa.")
		});

		if(thisMidiResolution.select(_.isNumber).size == widget.msSize and:{
			thisMidiResolution.minItem == thisMidiResolution.maxItem;
		}, {
			midiResolutionNB.string_(thisMidiResolution[0].asString);
		}, {
			midiResolutionNB.string_("--");
		});

		ctrlButtonBankField = TextField(thisEditor.midiTabs.views[0], midiFlow0.bounds.width/5-7@15)
			.font_(staticTextFont)
			.action_({ |mb|
				if("^[0-9]*$".matchRegexp(mb.string) or:{ mb.string == "nil" }, {
					if(mb.string != "nil", {
						widget.msSize.do({ |sl|
							widget.setCtrlButtonBank(mb.string.asInt, sl);
						})
					}, {
						widget.msSize.do({ |sl|
							widget.setCtrlButtonBank(nil);
						})
					})
				})
			})
		;

		if(GUI.id !== \cocoa, {
			ctrlButtonBankField.toolTip_("Set the number of sliders on in one bank of your MIDI-device.\nSetting this number will display the selected slider in a widget not as\na single number but rather as combination of the selected bank and\nthe slider number (e.g.: 4:3 means bank nr. 4, slider nr. 3)")
		});

		if(thisCtrlButtonBank.select(_.isNil).size == widget.msSize, {
			ctrlButtonBankField.string_("nil");
		}, {
			if(thisCtrlButtonBank.select(_.isInteger).size == widget.msSize and:{
				thisCtrlButtonBank.minItem == thisCtrlButtonBank.maxItem;
			}, {
				ctrlButtonBankField.string_(thisMidiResolution[0].asString);
			}, {
				ctrlButtonBankField.string_("--");
			})
		});

		StaticText(thisEditor.midiTabs.views[0], midiFlow0.bounds.width/5-7@32)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("MIDI-mode: 0-127 or in-/decremental")
		// .background_(Color.white)
		;

		StaticText(thisEditor.midiTabs.views[0], midiFlow0.bounds.width/5-7@32)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("MIDI-mean (in-/decremental mode only)")
		// .background_(Color.white)
		;

		StaticText(thisEditor.midiTabs.views[0], midiFlow0.bounds.width/5-7@32)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("minimum snap-distance for sli-ders (0-127 only)")
		// .background_(Color.white)
		;

		StaticText(thisEditor.midiTabs.views[0], midiFlow0.bounds.width/5-7@32)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("MIDI-resolution (in-/decremental mode only)")
		// .background_(Color.white)
		;

		midiFlow0.shift(0, -5);

		StaticText(thisEditor.midiTabs.views[0], midiFlow0.bounds.width/5-7@32)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("number of sliders per bank")
		// .background_(Color.white)
		;

		midiFlow0.shift(0, 10);

		StaticText(thisEditor.midiTabs.views[0], midiFlow0.bounds.width-20@15)
			.font_(staticTextFontBold)
			.string_("batch-connect a range of MIDI-sliders")
		;

		// midiFlow0.shift(0, 7);

		midiInitBut = Button(thisEditor.midiTabs.views[0], 60@25)
			.font_(staticTextFont)
			.action_({ |mb|
				if(MIDIClient.initialized, {
					MIDIClient.restart; MIDIIn.connectAll
				}, { MIDIClient.init; MIDIIn.connectAll });
				wcmMS.slots[0].midiDisplay.model.value_(
					wcmMS.slots[0].midiDisplay.model.value
				).changedKeys(widget.synchKeys);
			})
		;

		if(GUI.id !== \cocoa, {
			midiInitBut.toolTip_(
				"MIDI must only be inititialized before\nconnecting if you want the responders\nto listen to a specific source only (e.g.\nif you have more than one interface\nconnected to your computer)."
			)
		});

		if(MIDIClient.initialized, {
			midiInitBut.states_([["restart MIDI", Color.black, Color.green]]);
		}, {
			midiInitBut.states_([["init MIDI", Color.white, Color.red]]);
		});

		midiSourceSelect = PopUpMenu(thisEditor.midiTabs.views[0], midiFlow0.indentedRemaining.width-10@25)
			.items_(["select device port..."])
			.font_(staticTextFont)
			.action_({ |ms|
				if(ms.value != 0, {

				})
			})
		;

		if(GUI.id !== \cocoa, {
			midiSourceSelect.toolTip_(
				"Select a source from the list of possible\nsources (MIDI must be initialized). The uid\nof the selected source will be inserted into\nthe source-field below and responders will only\nlisten to messages coming from that source."
			)
		});

		midiSrcField = TextField(thisEditor.midiTabs.views[0], 120@15)
			.font_(textFieldFont)
			.stringColor_(textFieldFontColor)
			.background_(textFieldBg)
			.string_(msrc)
		;

		if(GUI.id !== \cocoa, {
			midiSrcField.toolTip_(
				"Enter a numeric source-uid or select from the\ndrop-down menu above (MIDI must be initialized.\nIf this field is left empty resulting responders\nwill listen to any source."
			)
		});

		midiChanField = TextField(thisEditor.midiTabs.views[0], 45@15)
			.font_(textFieldFont)
			.stringColor_(textFieldFontColor)
			.background_(textFieldBg)
			.string_(mchan)
		;

		if(GUI.id !== \cocoa, {
			midiChanField.toolTip_(
				"Enter the channel-number to which the resulting\nresponders shall listen. If this field is left\nresulting responders will listen to any channel."
			)
		});

		extMidiCtrlArrayField = TextField(thisEditor.midiTabs.views[0], midiFlow0.indentedRemaining.width-10@15)
			.font_(textFieldFont)
			.stringColor_(textFieldFontColor)
			.background_(textFieldBg)
			.string_("(0.."++(maxNum-1)++")")
		;

		if(GUI.id !== \cocoa, {
			extMidiCtrlArrayField.toolTip_(
				"Enter an array, representing the range of sliders\nyou want to connect. 0 corresponds to slider nr. 1.\nBy default the field displays an array that connects\nall slots of the multi-slider to midi-fader 1 to the\nnumber of slots of the multi-slider."
			)
		});

		connectorBut = Button(thisEditor.midiTabs.views[0], midiFlow0.bounds.width-21@25)
			.font_(staticTextFont)
			.states_([
				["connect MIDI-sliders", Color.white, Color.blue],
				["disconnect MIDI-sliders", Color.white, Color.red],
			])
			.action_({ |cb|
				switch(cb.value,
					1, {
						if("^[-+]?[0-9]*$".matchRegexp(midiSrcField.string), {
							midiUid = midiSrcField.string.interpret
						});
						if("^[0-9]*$".matchRegexp(midiChanField.string), {
							midiChan = midiChanField.string.interpret
						});
						extMidiCtrlArrayField.string.interpret.do({ |ctrlNum, sl|
							widget.midiConnect(midiUid, midiChan, ctrlNum, sl)
						})
					},
					0, { widget.msSize.do(widget.midiDisconnect(_)) }
				)
			})
		;

		widget.msSize.do({ |sl|
			CVMidiEditGroup(thisEditor.midiTabs.views[1], midiFlow1.bounds.width/5-10@39, widget, sl)
		});

		StaticText(thisEditor.oscTabs.views[0], oscFlow0.bounds.width-154@15)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("IP-address (optional)")
		;

		oscFlow0.shift(0, 0);

		StaticText(thisEditor.oscTabs.views[0], 130@15)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("port (usually not necessary)")
		;

		ipField = TextField(thisEditor.oscTabs.views[0], oscFlow0.bounds.width-154@15)
			.font_(textFieldFont)
			.stringColor_(textFieldFontColor)
			.background_(textFieldBg)
			.string_("")
		;

		oscFlow0.shift(0, 0);

		portField = TextField(thisEditor.oscTabs.views[0], 130@15)
			.font_(textFieldFont)
			.stringColor_(textFieldFontColor)
			.background_(textFieldBg)
			.string_("")
		;

		oscFlow0.shift(0, 0);

		deviceListMenu = PopUpMenu(thisEditor.oscTabs.views[0], oscFlow0.bounds.width/2-46@15)
			.items_(["select device..."])
			.font_(Font("Arial", 10))
			.action_({ |m|
				cmdListMenu.items_(["command-names..."]);
				thisCmdNames = [nil];
				if(m.value != 0, {
					orderedCmds = cmdNames[m.items[m.value].asSymbol].order;
					orderedCmdSlots = cmdNames[m.items[m.value].asSymbol].atAll(orderedCmds);
					orderedCmds.do({ |cmd, i|
						cmdListMenu.items_(cmdListMenu.items.add(cmd.asString+"("++orderedCmdSlots[i]++")"));
						thisCmdNames = thisCmdNames.add(cmd.asString);
					})
				})
			})
			.mouseDownAction_({ |m|
				cmdNames = OSCCommands.deviceCmds;
				deviceListMenu.items_(["select device..."]);
				cmdNames.pairsDo({ |dev, cmds|
					deviceListMenu.items_(deviceListMenu.items ++ dev);
				})
			})
		;

		oscFlow0.shift(0, 0);

		cmdListMenu = PopUpMenu(thisEditor.oscTabs.views[0], oscFlow0.bounds.width/2-11@15)
			.items_(["command-names..."])
			.font_(Font("Arial", 10))
			.action_({ |m|
				if(nameField.enabled, {
					nameField.string_(thisCmdNames[m.value]);
				})
			})
		;

		cmdNames.pairsDo({ |dev, cmds|
			deviceListMenu.items = deviceListMenu.items ++ dev;
		});

		oscFlow0.shift(0, 0);

		addDeviceBut = Button(thisEditor.oscTabs.views[0], 29@15)
			.states_([
				["new", Color.white, Color(0.15, 0.5, 0.15)]
			])
			.font_(staticTextFont)
			.action_({ OSCCommands.makeWindow })
		;

		StaticText(thisEditor.oscTabs.views[0], 65@40)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("placeholder values (numeric array)")
		;

		oscFlow0.shift(0, 10);

		StaticText(thisEditor.oscTabs.views[0], 185@40)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("OSC-command (use % as placeholder)")
		;

		oscFlow0.shift(0, -5);

		StaticText(thisEditor.oscTabs.views[0], 60@40)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("Multislider start-index")
		;

		oscFlow0.shift(0, -5);

		StaticText(thisEditor.oscTabs.views[0], 60@40)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("msg.-slot (use % as placeholder)")
		;

		oscFlow0.shift(0, 0);

		extOscCtrlArrayField = TextField(thisEditor.oscTabs.views[0], 65@15)
			.font_(textFieldFont)
			.stringColor_(textFieldFontColor)
			.background_(textFieldBg)
			.string_("(1.."++maxNum++")")
		;

		nameField = TextField(thisEditor.oscTabs.views[0], 185@15)
			.font_(textFieldFont)
			.stringColor_(textFieldFontColor)
			.background_(textFieldBg)
			.string_("/my/cmd/name/%")
		;

		intStartIndexField = NumberBox(thisEditor.oscTabs.views[0], 60@15)
			.font_(textFieldFont)
			.normalColor_(textFieldFontColor)
			.clipLo_(0)
			.clipHi_(widget.msSize-1)
			.shift_scale_(1)
			.ctrl_scale_(1)
			.alt_scale_(1)
			.value_(0)
		;

		indexField = TextField(thisEditor.oscTabs.views[0], 60@15)
			.font_(textFieldFont)
			.string_("int or %")
		;

		oscFlow0.shift(0, 0);

		StaticText(thisEditor.oscTabs.views[0], oscFlow0.bounds.width/2-10@15)
			.font_(staticTextFont)
			.string_("Input to Output mapping")
		;

		StaticText(thisEditor.oscTabs.views[0], oscFlow0.bounds.width/2-10@15)
			.font_(staticTextFont)
			.string_("Global Calibration")
		;

		mappingSelectItems = ["set global mapping...", "linlin", "linexp", "explin", "expexp"];

		mappingSelect = PopUpMenu(thisEditor.oscTabs.views[0], oscFlow0.bounds.width/2-12@20)
			.font_(Font("Arial", 12))
			.items_(mappingSelectItems)
			.action_({ |ms|
				if(ms.value != 0, {
					widget.msSize.do({ |i|
						if(i == 0, { tmp = ms.item });
						widget.setOscMapping(tmp, i);
					})
				})
			})
		;

		tmp = widget.msSize.collect({ |sl| widget.getOscMapping(sl) });
		block { |break|
			(1..widget.msSize-1).do({ |sl|
				if(tmp[0] != tmp[sl], { break.value(mappingDiffers = true) }, { mappingDiffers = false });
			})
		};

		if(mappingDiffers, {
			mappingSelect.value_(0);
		}, {
			mappingSelectItems.do({ |item, i|
				if(item.asSymbol === widget.getOscMapping(0), {
					mappingSelect.value_(i);
				})
			})
		});

		calibBut = Button(thisEditor.oscTabs.views[0],  oscFlow0.bounds.width/2-12@20)
			.font_(staticTextFont)
			.states_([
				["calibrating all", Color.black, Color.green],
				["calibrate all", Color.white, Color.red]
			])
			.action_({ |cb|
				cb.value.switch(
					0, {
						widget.msSize.do({ |i|
							widget.setCalibrate(true, i);
							wcmMS.slots[i].calibration.model.value_(true).changedKeys(widget.synchKeys);
						})
					},
					1, {
						widget.msSize.do({ |i|
							widget.setCalibrate(false, i);
							wcmMS.slots[i].calibration.model.value_(false).changedKeys(widget.synchKeys);
						})
					}
				)
			})
		;

		oscFlow0.shift(0, 0);

		connectorBut = Button(thisEditor.oscTabs.views[0], oscFlow0.bounds.width-21@25)
			.font_(staticTextFont)
			.states_([
				["connect OSC-controllers", Color.white, Color.blue],
				["disconnect OSC-controllers", Color.white, Color.red]
			])
			.action_({ |cb|
				// user-input:
				// 	extOscCtrlArrayField -> external controllers
				//	ipField -> IP-address
				//	portField -> port
				//	nameField -> osc-cmds, ext. controllers as placeholders
				//	intStartIndexField -> multislider-index to start connecting at
				//	indexField -> msg-slot, an integer or a placeholder, starting at 1
				cb.value.switch(
					1, {
						if(extOscCtrlArrayField.string.interpret.isArray and:{
							extOscCtrlArrayField.string.interpret.collect(_.isInteger).size ==
							extOscCtrlArrayField.string.interpret.size
						}, {
							oscConnectCondition = oscConnectCondition+1;
						}, {
							oscConnectWarning = oscConnectWarning++"\n\t- 'ext. sliders' must be an array of integers";
						});
						if(indexField.string != "%" and:{ indexField.string.interpret.isInteger.not }, {
							oscConnectWarning = oscConnectWarning++"\n\t- msg. slot must either contain '%' (a placeholder) or an integer";
						}, {
							oscConnectCondition = oscConnectCondition+1;
						});
						if(oscConnectCondition >= 2, {
						// "ok, we're ready to rock: %".postf(extOscCtrlArrayField.string.interpret);
							extOscCtrlArrayField.string.interpret.do({ |ext, i|
								if(ipField.string.size > 0, { connectIP = ipField.string }, { connectIP = "nil" });
								if(portField.string.size > 0, {
									connectPort = portField.string;
								}, {
									connectPort = "nil";
								});
								if(nameField.string.includes($%), {
									connectName = nameField.string.format(ext);
								}, {
									connectName = nameField.string;
								});
								if(indexField.string.includes($%), {
									connectOscMsgIndex = indexField.string.format(ext).asInt;
								}, {
									connectOscMsgIndex = indexField.string.asInt;
								});
								connectIndexStart = intStartIndexField.value+(ext-1);
								if(connectIndexStart >= 0 and:{ connectIndexStart < widget.msSize }, {
									widget.oscConnect(
										ip: connectIP,
										port: connectPort,
										name: connectName,
										oscMsgIndex: connectOscMsgIndex,
										slot: connectIndexStart
									)
								})
							})
						})
					},
					0, { widget.msSize.do(widget.oscDisconnect(_)) }
				)
			})
		;

		widget.msSize.do({ |sindex|
			oscEditBtns = oscEditBtns.add(
				Button(thisEditor.oscTabs.views[1], oscFlow1.bounds.width/5-10@25)
					.states_([
						[sindex.asString++": edit OSC", Color.black, Color.white(0.2)]
					])
					.font_(staticTextFont)
					.action_({ |bt|
						// "widget, widget.label.states[0][0], sindex: %, %, %\n".postf(widget, widget.label.states[0][0], sindex);
						if(widget.editor.editors[sindex].isNil or:{ widget.editor.editors[sindex].isClosed }, {
							widget.editor.editors[sindex] = CVWidgetEditor(
								widget, widget.label.states[0][0], 1, sindex
							);
						// if(widget.guiEnv.editor.notNil, {
						// 	"widget.guiEnv.editor: %\n".postf(widget.guiEnv.editor);
						// 	}, {
						// 		"no widget.guiEnv.editor yet - create it first".postln;
						// });
						// widget.guiEnv.editor[sindex] = widget.editor.editors[sindex];
						}, {
							widget.editor.editors[sindex].front(1)
						});
						widget.editor.editors[sindex].calibNumBoxes !? {
							wcmMS.slots[sindex].mapConstrainterLo.connect(
								widget.editor.editors[sindex].calibNumBoxes.lo;
							);
							widget.editor.editors[sindex].calibNumBoxes.lo.value_(
								wcmMS.slots[sindex].oscInputRange.model.value[0];
							);
							wcmMS.slots[sindex].mapConstrainterHi.connect(
								widget.editor.editors[sindex].calibNumBoxes.hi;
							);
							widget.editor.editors[sindex].calibNumBoxes.hi.value_(
								wcmMS.slots[sindex].oscInputRange.model.value[1];
							)
						};
						wcmMS.slots[sindex].oscDisplay.model.value_(
							wcmMS.slots[sindex].oscDisplay.model.value;
						).changedKeys(widget.synchKeys);
						wcmMS.slots[sindex].midiDisplay.model.value_(
							wcmMS.slots[sindex].midiDisplay.model.value
						).changedKeys(widget.synchKeys);
					})
				;
			);

			oscFlow1.shift(-13, oscEditBtns[sindex].bounds.height-10);

			oscCalibBtns = oscCalibBtns.add(
				Button(thisEditor.oscTabs.views[1], 10@10)
					.states_([
						["", Color.black, Color.green],
						["", Color.white, Color.red]
					])
					.action_({ |cb|
						cb.value.switch(
							0, {
								widget.setCalibrate(true, sindex);
								wcmMS.slots[sindex].calibration.model.value_(true).changedKeys(widget.synchKeys);
							},
							1, {
								widget.setCalibrate(false, sindex);
								wcmMS.slots[sindex].calibration.model.value_(false).changedKeys(widget.synchKeys);
							}
						)
					})
				;
			);

			widget.getCalibrate(sindex).switch(
				true, { oscCalibBtns[sindex].value_(0) },
				false, { oscCalibBtns[sindex].value_(1) }
			);

			oscFlow1.shift(0, (oscEditBtns[sindex].bounds.height-10).neg);
		});


		window.onClose_({
			msEditorEnv.specsListSpecs = specsListSpecs;
			msEditorEnv.specsListItems = specsList.items;
		});

		tab !? {
			thisEditor.tabs.focus(tab);
		};
		thisEditor.window.front;
	}

	close {
		thisEditor.window.close;
		allEditors.removeAt(name);
	}

}