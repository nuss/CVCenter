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
	var <extMidiCtrlArrayField, <midiConnectorBut, <midiDisconnectorBut;
	var <oscDisconnectorBut;
	var <extOscCtrlArrayField, <intStartIndexField;
	var <oscEditBtns, <oscCalibBtns;
	var <midiEditGroups;
	var <oscTabs, <midiTabs;
	var oscFlow0, <oscFlow1, midiFlow0, <midiFlow1;

	*new { |widget, widgetName, tab|
		^super.new.init(widget, widgetName, tab);
	}

	init { |widget, widgetName, tab|
		var cvString;
		// var oscTabs, midiTabs;
		var staticTextFont, staticTextFontBold, staticTextColor, textFieldFont, textFieldFontColor, textFieldBg;
		var maxNum, msrc = "source", mchan = "channel", mctrl = "ctrl", margs;
		var addr, wcmMS, labelColors;
		var midiUid, midiChan;
		var oscLabelColor, midiLabelColor, oscLabelStringColor, midiLabelStringColor;
		var oscConnectCondition = 0;
		var oscConnectWarning = "Couldn't connect OSC-controllers:";
		var connectIP, connectPort, connectName, connectOscMsgIndex, connectIndexStart;
		var addDeviceBut, thisCmdNames;
		var midiModes, nextMidiX, nextMidiY;
		var thisMidiMode, thisMidiMean, thisMidiResolution, thisSoftWithin, thisCtrlButtonBank;
		var mappingSelectItems, mappingDiffers;
		var wdgtActions;
		var cmdNames, orderedCmds, orderedCmdSlots;
		var tmp, tmpIP, tmpPortRestrictor, gapNextX, gapNextY;
		var buildCheckbox, ddIPsItems, cmdPairs, dropDownIPs;
		var connectWarning;

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

		widget ?? {
			Error("CVWidgetEditor is a utility-GUI-class that can only be used in connection with an existing CVWidget").throw;
		};

		#oscEditBtns, oscCalibBtns, midiEditGroups = List()!3;

		name = (widgetName.asString++"MS").asSymbol;
		nextX ?? { nextX = 0 }; nextY ?? { nextY = 0 };
		xySlots ?? { xySlots = [] };
		msEditorEnv = ();

//		"widget: %\n".postf(widget.msSize);

		#thisMidiMode, thisMidiMean, thisMidiResolution, thisSoftWithin, thisCtrlButtonBank = Array.newClear(widget.msSize)!5;

		cmdNames ?? { cmdNames = OSCCommands.deviceCmds };
		thisCmdNames ?? { thisCmdNames = [nil] };

		actionsList ?? { actionsList = () };

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
				gapNextX ?? { nextX }, gapNextY ?? { nextY }, 400, 253
			));

			xySlots = xySlots.add([nextX@nextY, name]);
			if(nextX+275 > Window.screenBounds.width, {
				nextX = shiftXY ?? { 0 }; nextY = xySlots.last[0].y+280;
			}, {
				nextX = xySlots.last[0].x+405; nextY = xySlots.last[0].y;
			});

			allEditors.put(name, (editor: this, window: window, name: widgetName));
			thisEditor = allEditors[name];

			if(Quarks.isInstalled("wslib"), { window.background_(Color.white) });

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

			StaticText(thisEditor.tabs.views[0], flow0.bounds.width-20@43)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
//				.background_(Color.white)
				.string_("Enter a ControlSpec in the textfield: e.g. ControlSpec(20, 20000, \\exp, 0.0, 440, \"Hz\") or\n\\freq or[[20, 20, 20, 20, 20], [20000, 20000, 20000, 20000, 20000], \\exp].asSpec. Or select\na suitable ControlSpec from the List below.")
			;

			// flow0.shift(0, 2);

			if(GUI.id == \cocoa, { tmp = "\n" }, { tmp = " " });

			StaticText(thisEditor.tabs.views[0], flow0.bounds.width-20@85/*60*/)
				.font_(staticTextFontBold)
				.stringColor_(staticTextColor)
//				.background_(Color.white)
				.string_("NOTE: You may enter a Spec whose minvals, maxvals, step-sizes and/or default values%are arrays of the size of the number of sliders in the multislider. However, a spec%may also be provided by its name, e.g. 'freq' and its parameters will internally%expanded to arrays of the required size. If you enter a Spec whose minvals, maxvals,%step-sizes and/or default values are arrays of a different size than in the current%spec the widget will get redimensioned to the size of the largest of these arrays.".format(tmp, tmp, tmp, tmp, tmp))
			;

			// flow0.shift(0, 2);

			StaticText(thisEditor.tabs.views[0], flow0.bounds.width-20@14)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("Enter the desired Spec and execute it by hitting shift+return.")
//				.background_(Color.white)
			;

			// flow0.shift(0, 5);

			cvString = widget.getSpec.asCompileString;
			specField = TextView(thisEditor.tabs.views[0], flow0.bounds.width-20@48)
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

			// flow0.shift(0, 5);

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
					tmp = ms.value;
					widget.msSize.do({ |sl|
						if(tmp != 2, {
							widget.setMidiMode(tmp, sl);
						})
					})
				})
			;

			if(thisMidiMode.minItem == thisMidiMode.maxItem, {
				midiModeSelect.value_(thisMidiMode[0])
			}, {
				midiModeSelect.items = midiModeSelect.items.add("--");
				midiModeSelect.value_(
					midiModeSelect.items.indexOf(midiModeSelect.items.last)
				)
			});

			if(GUI.id !== \cocoa, {
				midiModeSelect.toolTip_("Set the mode according to the output\nof your MIDI-device: 0-127 if it outputs\nabsolute values or +/- for in- resp.\ndecremental values")
			});

			midiMeanNB = TextField(thisEditor.midiTabs.views[0], midiFlow0.bounds.width/5-7@15)
				.font_(staticTextFont)
				.action_({ |mb|
					tmp = mb.string;
					// string for possible compilation to an int
					if("^[-+]?[0-9]*$".matchRegexp(tmp), {
						widget.msSize.do({ |sl|
							widget.setMidiMean(tmp.asInt, sl);
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
					tmp = mb.string;
					// string must be a valid float or integer
					if("^[0-9]*\.?[0-9]*$".matchRegexp(tmp), {
						widget.msSize.do({ |sl|
							widget.setSoftWithin(tmp.asFloat, sl);
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
					tmp = mb.string;
					if("^[0-9]*\.?[0-9]*$".matchRegexp(tmp), {
						widget.msSize.do({ |sl|
							widget.setMidiResolution(tmp.asFloat, sl);
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
					tmp = mb.string;
					if(mb.string != "nil", {
						tmp = mb.string;
						if("^[0-9]*$".matchRegexp(mb.string), {
							widget.msSize.do({ |sl|
								widget.setCtrlButtonBank(tmp.asInt, sl);
							})
						})
					}, {
						widget.msSize.do({ |sl|
							widget.setCtrlButtonBank(slot: sl);
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

			StaticText(thisEditor.midiTabs.views[0], midiFlow0.bounds.width/5-7@42)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("MIDI-mode:\n0-127 or in-/\ndecremental")
//				.background_(Color.white)
			;

			StaticText(thisEditor.midiTabs.views[0], midiFlow0.bounds.width/5-7@42)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("MIDI-mean (in-/\ndecremental\nmode only)")
//				.background_(Color.white)
			;

			StaticText(thisEditor.midiTabs.views[0], midiFlow0.bounds.width/5-7@42)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("minimum snap-\ndistance for sli-\nders (0-127 only)")
//				.background_(Color.white)
			;

			StaticText(thisEditor.midiTabs.views[0], midiFlow0.bounds.width/5-7@42)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("MIDI-resolution\n(in-/decremental\nmode only)")
//				.background_(Color.white)
			;

			//		midiFlow0.shift(0, 0);

			StaticText(thisEditor.midiTabs.views[0], midiFlow0.bounds.width/5-7@30)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("number of sliders\nper bank")
//				.background_(Color.white)
			;

			midiFlow0.shift(0, 3);

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
					"MIDI only needs to be inititialized before\nconnecting if you want the responders\nto listen to a specific source only (e.g.\nif you have more than one interface\nconnected to your computer)."
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
					if(ms.value != 0 and:{ MIDIClient.initialized }, {
						midiSrcField.string_(CVWidget.midiSources[ms.items[ms.value]]);
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
					"Enter the channel-number to which the resulting\nresponders shall listen. If this field is left empty\nresulting responders will listen to any channel."
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

			midiConnectorBut = Button(thisEditor.midiTabs.views[0], midiFlow0.bounds.width-24/2@25)
				.font_(staticTextFont)
				.states_([
					["connect MIDI-sliders", Color.white, Color.red],
				])
				.action_({ |cb|
					if("^[-+]?[0-9]*$".matchRegexp(midiSrcField.string), {
						midiUid = midiSrcField.string.interpret
					});
					if("^[0-9]*$".matchRegexp(midiChanField.string), {
						midiChan = midiChanField.string.interpret
					});
					if(extMidiCtrlArrayField.string.interpret.class == Array and:{
						extMidiCtrlArrayField.string.interpret.select(_.isNumber).size ==
						extMidiCtrlArrayField.string.interpret.size
					}, {
						extMidiCtrlArrayField.string.interpret.do({ |ctrlNum, sl|
							widget.midiConnect(midiUid, midiChan, ctrlNum.asInt, sl)
						})
					})
				})
			;

			if(widget.midiOscEnv.collect(_.cc).takeThese(_.isNil).size < widget.msSize, {
				midiConnectorBut.enabled_(true).states_([
					[midiConnectorBut.states[0][0], midiConnectorBut.states[0][1], Color.red]
				])
			}, { midiConnectorBut.enabled_(false).states_([
				[midiConnectorBut.states[0][0], midiConnectorBut.states[0][1], Color.red(alpha: 0.5)]
			]) });

			midiDisconnectorBut = Button(thisEditor.midiTabs.views[0], midiFlow0.bounds.width-29/2@25)
				.font_(staticTextFont)
				.states_([
					["disconnect all Midi-sliders", Color.white, Color.blue]
				])
				.action_({ |dcb| widget.msSize.do(widget.midiDisconnect(_)) })
			;

			if(widget.midiOscEnv.collect(_.cc).takeThese(_.isNil).size > 0, {
				midiDisconnectorBut.enabled_(true).states_([
					[midiDisconnectorBut.states[0][0], midiDisconnectorBut.states[0][1], Color.blue(alpha: 0.5)]
				])
			}, { midiDisconnectorBut.enabled_(false).states_([
				[midiDisconnectorBut.states[0][0], midiDisconnectorBut.states[0][1], Color.blue]
			]) });

			widget.msSize.do({ |sl|
				midiEditGroups.add(
					CVMidiEditGroup(thisEditor.midiTabs.views[1], midiFlow1.bounds.width/5-10@39, widget, sl);
				)
			});

			deviceDropDown = PopUpMenu(thisEditor.oscTabs.views[0], oscFlow0.bounds.width-110@15)
				.items_(["select IP-address... (optional)"])
				.font_(Font("Arial", 10))
			;

			if(GUI.id !== \cocoa, {
				deviceDropDown.toolTip_("Selecting one of the addresses will restrict listening within\nthe responder to messages coming from that address only.\nHowever, an IP-address will only be listed if the program is\nalready receiving OSC messages from that address.");
			});

			StaticText(thisEditor.oscTabs.views[0], 70@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("restrict to port ")
				.align_(\right)
			;

			portRestrictor = buildCheckbox.(false, thisEditor.oscTabs.views[0], 15@15, Font("Arial", 10, true));
			portRestrictor.action_({ |bt|
				switch(bt.value.asBoolean,
					true, {
						deviceDropDown.items_(
							["select IP-address:port... (optional)"] ++ deviceDropDown.items[1..];
						)
					},
					false, {
						deviceDropDown.items_(
							["select IP-address... (optional)"] ++  deviceDropDown.items[1..];
						)
					}
				)
			});

			if(GUI.id !== \cocoa, {
				portRestrictor.toolTip_("If clicked listening within in responders that get\ncreated after selecting an IP-address from the\ndrop-down on the left will also be restricted to\nthe port from which messages are sent.")
			});

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
							cmdListMenu.items_(
								cmdListMenu.items.add(cmd.asString+"("++orderedCmdSlots[i]++")")
							);
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

			if(GUI.id !== \cocoa, {
				deviceListMenu.toolTip_(
					"Select from the list of stored devices (e.g. a mobile controller).\nAfterwards you may select a command-name from the drop-\ndown on the right. If there are no devices listed you may add\nnew ones by clicking the green 'new' button on the right."
				)
			});

			oscFlow0.shift(0, 0);

			cmdListMenu = PopUpMenu(thisEditor.oscTabs.views[0], oscFlow0.bounds.width/2-11@15)
				.items_(["command-names..."])
				.font_(Font("Arial", 10))
				.mouseDownAction_({ |m|
					if(deviceDropDown.value > 0 and:{ deviceListMenu.value == 0 }, {
						cmdPairs = [];
						if(portRestrictor.value.asBoolean, {
							OSCCommands.tempIPsAndCmds[deviceDropDown.items[deviceDropDown.value]].pairsDo({ |cmd, size|
								cmdPairs = cmdPairs.add(cmd.asString+"("++size++")");
							})
						}, {
							OSCCommands.tempIPsAndCmds.pairsDo({ |k, v|
								if(k.asString.contains(deviceDropDown.items[deviceDropDown.value].asString), {
									v.pairsDo({ |cmd, size|
										cmdPairs = cmdPairs.add(cmd.asString+"("++size++")");
									})
								})
							})
						});
						m.items_(
							[m.items[0]] ++ cmdPairs.sort;
						)
					})
				})
				.action_({ |m|
					if(nameField.enabled, {
						nameField.string_(m.items[m.value].asString.split($ )[0]);
					})
				})
			;

			cmdNames.pairsDo({ |dev, cmds|
				deviceListMenu.items = deviceListMenu.items ++ dev;
			});

			if(GUI.id !== \cocoa, {
				cmdListMenu.toolTip_("If an IP-address has been selected in the drop-down above\nthis menu will list command-names coming in from that address.\nOtherwise select a device from the drop-down to the right and\nthis menu will list commands available for the selected device."
				)
			});

			oscFlow0.shift(0, 0);

			addDeviceBut = Button(thisEditor.oscTabs.views[0], 29@15)
				.states_([
					["new", Color.white, Color(0.15, 0.5, 0.15)]
				])
				.font_(staticTextFont)
				.action_({ OSCCommands.makeWindow })
			;

			if(GUI.id !== \cocoa, {
				addDeviceBut.toolTip_("Scan for incoming OSC-messages\nresp. their command-names. These\ncan be saved to disk together with a\ndevice-name. You may then quickly\nselect devices + command-names\nfrom the dropdowns on the left.")
			});

			oscFlow0.shift(0, 0);

			extOscCtrlArrayField = TextField(thisEditor.oscTabs.views[0], 65@15)
				.font_(textFieldFont)
				.stringColor_(textFieldFontColor)
				.background_(textFieldBg)
				.string_("(1.."++maxNum++")")
			;

			if(GUI.id !== \cocoa, {
				extOscCtrlArrayField.toolTip_(
					"An array of placeholder-values, supposed to replace occurrencies\nof % in the textfields to the right. When batch-connecting with\nexternal controllers the program will iterate over these values\nand insert them in appropriate places within oscConnect(). Please\nhave a look at the CVWidgetMSEditor-help for more information\nabout batch-connecting within a CVWidgetMS."
				)
			});

			nameField = TextField(thisEditor.oscTabs.views[0], 185@15)
				.font_(textFieldFont)
				.stringColor_(textFieldFontColor)
				.background_(textFieldBg)
				.string_("/my/cmd/name/%")
			;

			if(GUI.id !== \cocoa, {
				nameField.toolTip_(
					"An OSC command-name. If you want to connect with several command-\nnames which differ only within a number (e.g. \"/my/cmd/name/1\" to\n\"/my/cmd/name/10) that number may be replaced by a placeholder %\nand the program will replace them with the values specified in the\ntextfield on the left. Please have a look at the CVWidgetMSEditor-help\nfor more information about batch-connecting within a CVWidgetMS."
				)
			});

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

			if(GUI.id !== \cocoa, {
				intStartIndexField.toolTip_(
					"The slider-index at which the connection shall begin.\n0 means the connection will begin at the first slider\nof the MultiSlider within a CVWidgetMS."
				)
			});

			indexField = TextField(thisEditor.oscTabs.views[0], 60@15)
				.font_(textFieldFont)
				.string_("int or %")
			;

			if(GUI.id !== \cocoa, {
				indexField.toolTip_(
					"The index of the OSC message that shall be connected. OSC messages may contain\none or more message slots. E.g. [\"/cmd/name/\", <slot 1>, <slot 2>,..<slot N>]. These\nslots may be addressed by a placeholder-value % for which the values are defined\nin the textfield on the right. The first message-slot will always be 1 (as 0 refers to the\ncommand-name itself). If only one message-slot exists for the given command-name\nsimply enter 1. Please have a look at the CVWidgetMSEditor-help for more infor-\nmation about batch-connecting within a CVWidgetMS."
				)
			});

			StaticText(thisEditor.oscTabs.views[0], 65@42)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("placeholder\nvalues (numeric\narray)")
//				.background_(Color.white)
			;

			StaticText(thisEditor.oscTabs.views[0], 185@20)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("OSC-command (use % as placeholder)")
//				.background_(Color.white)
			;

			StaticText(thisEditor.oscTabs.views[0], 60@30)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("Multislider\nstart-index")
//				.background_(Color.white)
			;

			StaticText(thisEditor.oscTabs.views[0], 60@42)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("msg.-slot (use\n% as\nplaceholder)")
//				.background_(Color.white)
			;

			StaticText(thisEditor.oscTabs.views[0], oscFlow0.bounds.width/2-10@15)
				.font_(staticTextFont)
				.string_("Input to Output mapping")
//				.background_(Color.white)
			;

			StaticText(thisEditor.oscTabs.views[0], oscFlow0.bounds.width/2-10@15)
				.font_(staticTextFont)
				.string_("Global Calibration")
//				.background_(Color.white)
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
					if(tmp[0] != tmp[sl], {
						break.value(mappingDiffers = true)
					}, { mappingDiffers = false });
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

			if(GUI.id !== \cocoa, {
				calibBut.toolTip_("As the range of incoming values may be unknown the\ncalibration provides a way to detect the constraints\nof incoming values. It may be useful in some cases\nto deactivate the mechanismand restrict the input to\na limited range.")
			});

			oscFlow0.shift(0, 0);

			connectorBut = Button(thisEditor.oscTabs.views[0], oscFlow0.bounds.width-24/2@25)
				.font_(staticTextFont)
				.states_([
					["connect OSC-controllers", Color.white, Color.red],
				])
				.action_({ |cb|
					tmpIP = deviceDropDown.items[deviceDropDown.value];
					tmpPortRestrictor = portRestrictor.value.asBoolean;
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
							if(deviceDropDown.value > 0, {
								connectIP = tmpIP.asString.split($:)[0];
								if(tmpPortRestrictor, {
									connectPort = tmpIP.asString.split($:)[1];
								})
							}, { #connectIP, connectPort = nil!2 });
							if(indexField.string.includes($%), { indexField.string = "%" });
							if(nameField.string.includes($%) and:{ indexField.string.includes($%) }, {
								connectWarning = "There can only be one placeholder '%', either in the OSC-command or the msg-slot!";
							});
							if(nameField.string.includes($%).not and:{ indexField.string.includes($%).not }, {
								connectWarning = "There has to be at least one placeholder '%', either in the OSC-command or the msg-slot";
							});
							if(connectWarning.notNil, {
								connectWarning.warn;
							}, {
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
								// [connectIP, connectPort, connectName, connectOscMsgIndex, connectIndexStart].postln;
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
					})
				})
			;

			if(widget.midiOscEnv.collect(_.oscResponder).takeThese(_.isNil).size < widget.msSize, {
				connectorBut.enabled_(true).states_([
					[connectorBut.states[0][0], connectorBut.states[0][1], Color.red]
				])
			}, { connectorBut.enabled_(false).states_([
				[connectorBut.states[0][0], connectorBut.states[0][1], Color.red(alpha: 0.5)]
			]) });

			oscDisconnectorBut = Button(thisEditor.oscTabs.views[0], oscFlow0.bounds.width-24/2@25)
				.font_(staticTextFont)
				.states_([
					["disconnect all OSC-controllers", Color.white, Color.blue],
				])
				.action_({ |dcb| widget.msSize.do(widget.oscDisconnect(_)) })
			;

			if(widget.midiOscEnv.collect(_.oscResponder).takeThese(_.isNil).size > 0, {
				oscDisconnectorBut.enabled_(true).states_([
					[oscDisconnectorBut.states[0][0], oscDisconnectorBut.states[0][1], Color.blue]
				])
			}, { oscDisconnectorBut.enabled_(false).states_([
				[oscDisconnectorBut.states[0][0], oscDisconnectorBut.states[0][1], Color.blue(alpha: 0.5)]
			]) });

			widget.msSize.do({ |sindex|
				oscEditBtns.add(
					Button(thisEditor.oscTabs.views[1], oscFlow1.bounds.width/5-10@25)
						.states_([
							[sindex.asString++": edit OSC", Color.black, Color.white(0.2)]
						])
						.font_(staticTextFont)
						.action_({ |bt|
							if(widget.editor.editors[sindex].isNil or:{
								widget.editor.editors[sindex].isClosed
							}, {
								widget.editor.editors[sindex] = CVWidgetEditor(
									widget, widget.label.states[0][0], 1, sindex
								);
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

				oscCalibBtns.add(
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

			actionName = TextField(thisEditor.tabs.views[3], flow3.bounds.width-100@20)
				.string_("action-name")
				.font_(textFieldFont)
			;

			if(GUI.id !== \cocoa, {
				actionName.toolTip_("Mandatory: each action must\nbe saved under a unique name")
			});

			flow3.shift(5, 0);

			enterActionBut = Button(thisEditor.tabs.views[3], 57@20)
				.font_(staticTextFont)
				.states_([
					["add Action", Color.white, Color.blue],
				])
				.action_({ |ab|
					if(actionName.string != "action-name" and:{
						enterAction.string != "{ |cv| /* do something */ }"
					}, {
						widget.addAction(actionName.string.asSymbol, enterAction.string.replace("\t", "    "));
					})
				})
			;

			enterAction = TextView(thisEditor.tabs.views[3], flow3.bounds.width-35@50)
				.background_(Color.white)
				.font_(textFieldFont)
				.string_("{ |cv| /* do something */ }")
				.syntaxColorize
			;

			if(GUI.id !== \cocoa, {
				enterAction.tabWidth_("    ".bounds.width);
				enterAction.toolTip_("The variable 'cv' holds the widget's CV resp.\n'cv.value' its current value. You may enter an\narbitrary function using this variable (or not).")
			});

			wdgtActions = widget.wdgtActions;

			wdgtActions.pairsDo({ |name, action|

				actionsList = actionsList.put(name, ());

				flow3.shift(0, 5);

				actionsList[name].nameField = StaticText(thisEditor[\tabs].views[3], flow3.bounds.width-173@15)
					.font_(staticTextFont)
					.background_(Color(1.0, 1.0, 1.0, 0.5))
					.string_(""+name.asString)
				;

				flow3.shift(5, 0);

				actionsList[name].activate = Button(thisEditor[\tabs].views[3], 60@15)
					.font_(staticTextFont)
					.states_([
						["activate", Color(0.1, 0.3, 0.15), Color(0.99, 0.77, 0.11)],
						["deactivate", Color.white, Color(0.1, 0.30, 0.15)],
					])
					.action_({ |rb|
						switch(rb.value,
							0, { widget.activateAction(name, false) },
							1, { widget.activateAction(name, true) }
						)
					})
				;

				switch(action.asArray[0][1],
					true, {
						actionsList[name].activate.value_(1);
					},
					false, {
						actionsList[name].activate.value_(0);
					}
				);

				flow3.shift(5, 0);

				actionsList[name].removeBut = Button(thisEditor[\tabs].views[3], 60@15)
					.font_(staticTextFont)
					.states_([
						["remove", Color.white, Color.red],
					])
					.action_({ |rb|
						widget.removeAction(name.asSymbol)
					})
				;

				flow3.shift(0, 0);

				actionsList[name].actionView = TextView(thisEditor[\tabs].views[3], flow3.bounds.width-35@50)
					.background_(Color(1.0, 1.0, 1.0, 0.5))
					.font_(textFieldFont)
					.string_(action.asArray[0][0].replace("\t", "    "))
					.syntaxColorize
					.editable_(false)
				;
			});

			window.onClose_({
				msEditorEnv.specsListSpecs = specsListSpecs;
				msEditorEnv.specsListItems = specsList.items;
			})
		});

		OSCCommands.collectTempIPsAndCmds;
		deviceDropDown
			.mouseDownAction_({ |dd|
				dropDownIPs = OSCCommands.tempIPsAndCmds.keys.asArray;
				if(portRestrictor.value.asBoolean, {
					ddIPsItems = dropDownIPs;
				}, {
					ddIPsItems = dropDownIPs.collect({ |addr| addr.asString.split($:)[0].asSymbol });
				});
				dd.items_([dd.items[0]]);
				ddIPsItems.do({ |it|
					if(dd.items.includesEqual(it).not, {
						dd.items_(dd.items.add(it));
					})
				})
			})
			.action_({ |dd|
				if(dd.value != 0, { deviceListMenu.value_(0) });
				cmdPairs = [];
				if(portRestrictor.value.asBoolean, {
					OSCCommands.tempIPsAndCmds[dd.items[dd.value]].pairsDo({ |cmd, size|
						cmdPairs = cmdPairs.add(cmd.asString+"("++size++")");
					})
				}, {
					OSCCommands.tempIPsAndCmds.pairsDo({ |k, v|
						if(k.asString.contains(dd.items[dd.value].asString), {
							v.pairsDo({ |cmd, size|
								cmdPairs = cmdPairs.add(cmd.asString+"("++size++")");
							})
						})
					})
				});
				cmdListMenu.items_(
					[cmdListMenu.items[0]] ++ cmdPairs.sort;
				)
			})
		;

		tab !? {
			thisEditor.tabs.focus(tab);
			tabs.views[tab].background_(Color(0.8, 0.8, 0.8, 1.0));
		};
		thisEditor.window.front;
	}

	// not to be used directly!

	amendActionsList { |widget, addRemove, name, action, slot, active|

		var staticTextFont = Font("Arial", 9.4);
		var textFieldFont = Font("Andale Mono", 9);
		var actTop;

		switch(addRemove,
			\add, {
				actionsList.put(name, ());
				flow3.shift(0, 5);

				actionsList[name].nameField = StaticText(thisEditor[\tabs].views[3], flow3.bounds.width-173@15)
					.font_(staticTextFont)
					.background_(Color(1.0, 1.0, 1.0, 0.5))
					.string_(""+name.asString)
				;

				flow3.shift(5, 0);

				actionsList[name].activate = Button(thisEditor[\tabs].views[3], 60@15)
					.font_(staticTextFont)
					.states_([
						["activate", Color(0.1, 0.3, 0.15), Color(0.99, 0.77, 0.11)],
						["deactivate", Color.white, Color(0.1, 0.30, 0.15)],
					])
					.action_({ |rb|
						switch(rb.value,
							0, { widget.activateAction(name, false, slot) },
							1, { widget.activateAction(name, true, slot) }
						)
					})
				;

				switch(active,
					true, {
						actionsList[name].activate.value_(1);
					},
					false, {
						actionsList[name].activate.value_(0);
					}
				);

				flow3.shift(5, 0);

				actionsList[name].removeBut = Button(thisEditor[\tabs].views[3], 60@15)
					.font_(staticTextFont)
					.states_([
						["remove", Color.white, Color.red],
					])
					.action_({ |ab|
						widget.removeAction(name.asSymbol, slot.asSymbol);
					})
				;

				flow3.shift(0, 0);

				actionsList[name].actionView = TextView(thisEditor[\tabs].views[3], flow3.bounds.width-35@50)
					.background_(Color(1.0, 1.0, 1.0, 0.5))
					.font_(textFieldFont)
					.string_(action.asArray[0][0])
					.syntaxColorize
					.editable_(false)
				;

			},
			\remove, {
				actTop = actionsList[name].nameField.bounds.top;
				[
					actionsList[name].nameField,
					actionsList[name].activate,
					actionsList[name].removeBut,
					actionsList[name].actionView
				].do(_.remove);
				actionsList.removeAt(name);
				actionsList.pairsDo({ |actName, it|
					if(it.nameField.bounds.top > actTop, {
						#[nameField, activate, removeBut, actionView].do({ |name|
							it[name].bounds_(Rect(
								it[name].bounds.left,
								it[name].bounds.top-76,
								it[name].bounds.width,
								it[name].bounds.height
							))
						})
					})
				});
				flow3.top_(flow3.top-76);
			}
		)

	}

	close {
		thisEditor.window.close;
		allEditors.removeAt(name);
	}

}