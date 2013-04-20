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

CVWidgetEditor : AbstractCVWidgetEditor {

	var <specConstraintsText, <calibNumBoxes;
	var editorSlot;

	*new { |widget, widgetName, tab, slot|
		^super.new.init(widget, widgetName, tab, slot)
	}

	init { |widget, widgetName, tab, slot|
		var cvString, slotHiLo;
		var staticTextFont, staticTextColor, textFieldFont, textFieldFontColor, textFieldBg;
		var msrc = "source", mchan = "channel", mctrl = "ctrl", margs;
		var addr, wcm, labelColors;
		var midiModes;
		var thisMidiMode, thisMidiMean, thisMidiResolution, thisSoftWithin, thisCtrlButtonBank;
		var mappingSelectItems;
		var wdgtActions;
		var cmdNames, orderedCmds, orderedCmdSlots;
		var tmp, gapNextX, gapNextY;
		var buildCheckbox, ddIPsItems, cmdPairs, dropDownIPs;
		var connectIP, connectPort;

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

		name = widgetName.asSymbol;
		nextX ?? { nextX = 0 }; nextY ?? { nextY = 0 };
		xySlots ?? { xySlots = [] };
		editorEnv = ();
		slot !? { editorSlot = slot };

		cmdNames ?? { cmdNames = OSCCommands.deviceCmds };
		thisCmdNames ?? { thisCmdNames = [nil] };

		actionsList ?? { actionsList = () };

		if(slot.notNil, {
			switch(widget.class,
				CVWidget2D, {
//					widget.wdgtControllersAndModels[slot] !? {
						wcm = widget.wdgtControllersAndModels[slot];
//					}
				},
				CVWidgetMS, {
//					widget.wdgtControllersAndModels.slots[slot] !? {
						wcm = widget.wdgtControllersAndModels.slots[slot];
//					}
				}
			);
			thisMidiMode = widget.getMidiMode(slot);
			thisMidiMean = widget.getMidiMean(slot);
			thisMidiResolution = widget.getMidiResolution(slot);
			thisSoftWithin = widget.getSoftWithin(slot);
			thisCtrlButtonBank = widget.getCtrlButtonBank(slot);
		}, {
			widget.wdgtControllersAndModels !? {
				wcm = widget.wdgtControllersAndModels;
			};
			thisMidiMode = widget.getMidiMode;
			thisMidiMean = widget.getMidiMean;
			thisMidiResolution = widget.getMidiResolution;
			thisSoftWithin = widget.getSoftWithin;
			thisCtrlButtonBank = widget.getCtrlButtonBank;
		});


		staticTextFont = Font("Arial", 9.4);
		staticTextColor = Color(0.2, 0.2, 0.2);
		textFieldFont = Font("Andale Mono", 9);
		textFieldFontColor = Color.black;
		textFieldBg = Color.white;

		if(slot.notNil, {
			slotHiLo = "["++slot.asString++"]";
		}, {
			slotHiLo = "";
		});

		// allEditors ?? { allEditors = IdentityDictionary() };

		if(thisEditor.isNil or:{ thisEditor.window.isClosed }, {

			// any seats left empty?
			block { |break|
				xySlots.do({ |p, i|
					if(p[1] == 0, {
						break.value(
							#gapNextX, gapNextY = p[0].asArray;
							xySlots[i][1] = name++slotHiLo;
						);
					})
				})
			};

			window = Window("Widget Editor:"+widgetName++slotHiLo, Rect(
				gapNextX ?? { nextX }, gapNextY ?? { nextY }, 270, 253
			));

			xySlots = xySlots.add([nextX@nextY, name++slotHiLo]);
			// [xySlots, nextX, nextY].postln;
			if(nextX+275 > Window.screenBounds.width, {
				nextX = shiftXY ?? { 0 }; nextY = xySlots.last[0].y+280;
			}, {
				nextX = xySlots.last[0].x+275; nextY = xySlots.last[0].y;
			});

			if(slot.isNil, {
				allEditors.put(name, (editor: this, window: window, name: widgetName))
			}, {
				tmp = (); tmp.put(slot, (editor: this, window: window, name: widgetName));
				if(allEditors[name].isNil, {
					allEditors.put(name, tmp);
				}, {
					allEditors[name].put(slot, (editor: this, window: window, name: widgetName));
				});
			});

			if(slot.notNil, { thisEditor = allEditors[name][slot] }, { thisEditor = allEditors[name] });

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
			tabs.views[1].decorator = flow1 = FlowLayout(window.view.bounds, 7@7, 3@3);
			tabs.views[2].decorator = flow2 = FlowLayout(window.view.bounds, 7@7, 3@3);
			tabs.views[3].decorator = flow3 = FlowLayout(window.view.bounds, 7@7, 3@3);
			tabs.views.do({ |v| v.background_(Color(0.8, 0.8, 0.8, 1.0)) });

			thisEditor[\tabs] = tabs;

			thisEditor[\tabs].view.keyDownAction_({ |view, char, modifiers, unicode, keycode|
//				[view, char, modifiers, unicode, keycode].postln;
				switch(unicode,
					111, { // "o" -> osc
						switch(widget.class,
							CVWidgetMS, { thisEditor[\tabs].focus(1) },
							{ thisEditor[\tabs].focus(2) }
						)
					},
					109, { // "m" -> midi
						switch(widget.class,
							CVWidgetMS, { thisEditor[\tabs].focus(0) },
							{ thisEditor[\tabs].focus(1) }
						)
					},
					97, { if(widget.class != CVWidgetMS, { thisEditor[\tabs].focus(3) }) }, // "a" -> actions
					115, { if(widget.class != CVWidgetMS, { thisEditor[\tabs].focus(0) }) }, // "s" -> specs
					120, { this.close(slot) }, // "x" -> close editor
					99, { OSCCommands.makeWindow } // "c" -> collect OSC-commands resp. open the collector's GUI
				)
			});

			if(widget.class != CVWidgetMS, {
				StaticText(thisEditor[\tabs].views[0], flow0.bounds.width-20@95)
					.font_(staticTextFont)
					.stringColor_(staticTextColor)
					.string_("Enter a ControlSpec in the textfield:\ne.g. ControlSpec(20, 20000, \\exp, 0.0, 440, \"Hz\")\nor \\freq.asSpec \nor [20, 20000, \\exp].asSpec.\nOr select a suitable ControlSpec from the List below.\nIf you don't know what this all means have a look\nat the ControlSpec-helpfile.")
				;

				cvString = widget.getSpec(slot).asString.split($ );

				cvString = cvString[1..cvString.size-1].join(" ");

				specField = TextField(thisEditor[\tabs].views[0], flow0.bounds.width-20@15)
					.font_(staticTextFont)
					.string_(cvString)
					.action_({ |tf|
						widget.setSpec(tf.string.interpret, slot)
					})
				;

				flow0.shift(0, 5);

				specsList = PopUpMenu(thisEditor[\tabs].views[0], flow0.bounds.width-20@20)
					.action_({ |sl|
						widget.setSpec(specsListSpecs[sl.value], slot);
					})
				;

				if(editorEnv.specsListSpecs.isNil, {
					specsListSpecs = List()
				}, {
					specsListSpecs = editorEnv.specsListSpecs;
				});

				if(editorEnv.specsListItems.notNil, {
					specsList.items_(editorEnv.specsListItems);
				}, {
					Spec.specs.asSortedArray.do({ |spec|
						if(spec[1].isKindOf(ControlSpec) and:{
							[spec[1].minval, spec[1].maxval, spec[1].step, spec[1].default].select(_.isArray).size == 0
						}, {
							specsList.items_(specsList.items.add(spec[0]++":"+spec[1]));
							specsListSpecs.add(spec[1]);
						})
					})
				});

				tmp = specsListSpecs.detectIndex({ |spec, i| spec == widget.getSpec(slot) });
				if(tmp.notNil, {
					specsList.value_(tmp);
				}, {
					specsListSpecs.array_([widget.getSpec(slot)]++specsListSpecs.array);
					specsList.items = List["custom:"+widget.getSpec(slot).asString]++specsList.items;
				});

				window.onClose_({
					editorEnv.specsListSpecs = specsListSpecs;
					editorEnv.specsListItems = specsList.items;
					tmp = xySlots.detectIndex({ |n| n[1] == (name.asString++slotHiLo) });
					xySlots[tmp][1] = 0;
					OSCCommands.tempIPsAndCmds.keysDo(OSCCommands.tempIPsAndCmds[_] = nil);
					if(allEditors.collect(_.isClosed).size == 0, { OSCCommands.collectTempIPsAndCmds(false) });
				})
			});

			// MIDI editing

			StaticText(thisEditor[\tabs].views[1], flow1.bounds.width/2+40@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("MIDI-mode: 0-127 or in/decremental")
			;

			flow1.shift(5, 0);

			midiModes = ["0-127", "+/-"];

			midiModeSelect = PopUpMenu(thisEditor[\tabs].views[1], flow1.bounds.width/2-70@15)
				.font_(staticTextFont)
				.items_(midiModes)
				.value_(thisMidiMode)
				.action_({ |ms|
					widget.setMidiMode(ms.value, slot);
				})
			;

			if(GUI.id !== \cocoa, {
				midiModeSelect.toolTip_("Set the mode according to the output\nof your MIDI-device: 0-127 if it outputs\nabsolute values or +/- for in- resp.\ndecremental values")
			});

			StaticText(thisEditor[\tabs].views[1], flow1.bounds.width/2+60@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("MIDI-mean (in/decremental mode only)")
			;

			flow1.shift(5, 0);

			midiMeanNB = NumberBox(thisEditor[\tabs].views[1], flow1.bounds.width/2-90@15)
				.font_(staticTextFont)
				.value_(thisMidiMean)
				.action_({ |mb|
					widget.setMidiMean(mb.value, slot);
				})
				.step_(1.0)
				.clipLo_(0.0)
			;

			if(GUI.id !== \cocoa, {
				midiMeanNB.toolTip_("If your device outputs in-/decremental\nvalues often a slider's output in neutral\nposition will not be 0. E.g. it could be 64")
			});

			StaticText(thisEditor[\tabs].views[1], flow1.bounds.width/2+60@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("min. snap-distance for the slider (0-127 only)")
			;

			flow1.shift(5, 0);

			softWithinNB = NumberBox(thisEditor[\tabs].views[1], flow1.bounds.width/2-90@15)
				.font_(staticTextFont)
				.value_(thisSoftWithin)
				.action_({ |mb|
					widget.setSoftWithin(mb.value, slot);
				})
				.step_(0.005)
				.clipLo_(0.01)
				.clipHi_(0.5)
			;

			if(GUI.id !== \cocoa, {
				softWithinNB.toolTip_("If your device outputs absolute values\nyou can set here a threshold to the\ncurrent CV-value within which a slider\nwill react and set a new value. This avoids\njumps if a new value set by a slider\nis far away from the previous value")
			});

			StaticText(thisEditor[\tabs].views[1], flow1.bounds.width/2+60@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("MIDI-resolution (+/- only)")
			;

			flow1.shift(5, 0);

			midiResolutionNB = NumberBox(thisEditor[\tabs].views[1], flow1.bounds.width/2-90@15)
				.font_(staticTextFont)
				.value_(thisMidiResolution)
				.action_({ |mb|
					widget.setMidiResolution(mb.value, slot);
				})
				.step_(0.05)
				.clipLo_(0.001)
				.clipHi_(10.0)
			;

			if(GUI.id !== \cocoa, {
				midiResolutionNB.toolTip_("Higher values mean lower\nresolution and vice versa.")
			});

			StaticText(thisEditor[\tabs].views[1], flow1.bounds.width/2+60@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("number of sliders per bank")
			;

			flow1.shift(5, 0);

			ctrlButtonBankField = TextField(thisEditor[\tabs].views[1], flow1.bounds.width/2-90@15)
				.font_(staticTextFont)
				.string_(thisCtrlButtonBank)
				.action_({ |mb|
					if(mb.string != "nil", {
						if("^[0-9]*$".matchRegexp(mb.string), {
							widget.setCtrlButtonBank(mb.string.asInt, slot);
						})
					}, {
						widget.setCtrlButtonBank(nil, slot);
					})
				})
			;

			if(GUI.id !== \cocoa, {
				ctrlButtonBankField.toolTip_("Set the number of sliders on in one bank of your MIDI-device.\nSetting this number will display the selected slider in a widget not as\na single number but rather as combination of the selected bank and\nthe slider number (e.g.: 4:3 means bank nr. 4, slider nr. 3)")
			});

			flow1.shift(0, 10);

			midiInitBut = Button(thisEditor[\tabs].views[1], 60@15)
				.font_(staticTextFont)
				.action_({ |mb|
					if(MIDIClient.initialized, {
						MIDIClient.restart; MIDIIn.connectAll
					}, { MIDIClient.init; MIDIIn.connectAll });
					wcm.midiDisplay.model.value_(
						wcm.midiDisplay.model.value
					).changedKeys(widget.synchKeys);
				})
			;

			if(MIDIClient.initialized, {
				midiInitBut.states_([["restart MIDI", Color.black, Color.green]]);
			}, {
				midiInitBut.states_([["init MIDI", Color.white, Color.red]]);
			});

			midiSourceSelect = PopUpMenu(thisEditor[\tabs].views[1], flow1.indentedRemaining.width-10@15)
				.items_(["select device port..."])
				.font_(staticTextFont)
				.action_({ |ms|
					if(ms.value != 0, {
						wcm.midiDisplay.model.value_((
							learn: "C",
							src: CVWidget.midiSources[ms.items[ms.value].asSymbol],
							chan: wcm.midiDisplay.model.value.chan,
							ctrl: wcm.midiDisplay.model.value.ctrl
						)).changedKeys(widget.synchKeys);
					});
				})
			;

			StaticText(thisEditor[\tabs].views[1], flow1.bounds.width-20@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("(learn | connect) / source-ID (device) / chan / ctrl-nr.")
			;

			midiLearnBut = Button(thisEditor[\tabs].views[1], 15@15)
				.font_(staticTextFont)
				.states_([
					["L", Color.white, Color.blue],
					["X", Color.white, Color.red]
				])
				.action_({ |ml|
					ml.value.switch(
					// wcm.midiDisplay.model.value_((
					// 	learn: ml.states[ml.value][0],
					// 	src: wcm.midiDisplay.model.value.src,
					// 	chan: wcm.midiDisplay.model.value.chan,
					// 	ctrl: wcm.midiDisplay.model.value.ctrl
					// )).changedKeys(widget.synchKeys);
						1, {
							margs = [
								[midiSrcField.string, msrc],
								[midiChanField.string, mchan],
								[midiCtrlField.string, mctrl]
							].collect({ |pair| if(pair[0] != pair[1], { pair[0].asInt }, { nil }) });
							if(margs.select({ |i| i.notNil }).size > 0, {
								widget.midiConnect(uid: margs[0], chan: margs[1], num: margs[2], slot: slot)
							}, {
								widget.midiConnect(slot: slot)
							})
						},
						0, { widget.midiDisconnect(slot) };
					)
				})
			;

			flow1.shift(0, 0);

			midiSrcField = TextField(thisEditor[\tabs].views[1], flow1.bounds.width-165@15)
				.font_(staticTextFont)
				.string_(msrc)
				.background_(Color.white)
				.action_({ |tf|
					if("^[-+]?[0-9]*$".matchRegexp(tf.string), {
						wcm.midiDisplay.model.value_((
							learn: "C",
							src: tf.string.asInt,
							chan: wcm.midiDisplay.model.value.chan,
							ctrl: wcm.midiDisplay.model.value.ctrl
						)).changedKeys(widget.synchKeys)
					})
				})
				.mouseDownAction_({ |tf|
					tf.stringColor_(Color.red)
				})
				.keyUpAction_({ |tf, char, modifiers, unicode, keycode|
					if(unicode == 13, {
						tf.stringColor_(Color.black);
					})
				})
			;

			flow1.shift(0, 0);

			midiChanField = TextField(thisEditor[\tabs].views[1], 60@15)
				.font_(staticTextFont)
				.string_(mchan)
				.background_(Color.white)
				.action_({ |tf|
					if("^[0-9]*$".matchRegexp(tf.string), {
						wcm.midiDisplay.model.value_((
							learn: "C",
							src: wcm.midiDisplay.model.value.src,
							chan: tf.string.asInt,
							ctrl: wcm.midiDisplay.model.value.ctrl
						)).changedKeys(widget.synchKeys)
					})
				})
				.mouseDownAction_({ |tf|
					tf.stringColor_(Color.red)
				})
				.keyUpAction_({ |tf, char, modifiers, unicode, keycode|
					if(unicode == 13, {
						tf.stringColor_(Color.black);
					})
				})
			;

			flow1.shift(0, 0);

			midiCtrlField = TextField(thisEditor[\tabs].views[1], 60@15)
				.font_(staticTextFont)
				.string_(mctrl)
				.background_(Color.white)
				.action_({ |tf|
					if("^[0-9]*$".matchRegexp(tf.string), {
						wcm.midiDisplay.model.value_((
							learn: "C",
							src: wcm.midiDisplay.model.value.src,
							chan: wcm.midiDisplay.model.value.chan,
							ctrl: tf.string.asInt
						)).changedKeys(widget.synchKeys)
					})
				})
				.mouseDownAction_({ |tf|
					tf.stringColor_(Color.red)
				})
				.keyUpAction_({ |tf, char, modifiers, unicode, keycode|
					if(unicode == 13, {
						tf.stringColor_(Color.black);
					})
				})
			;

			// OSC editting

			// StaticText(thisEditor[\tabs].views[2], flow2.bounds.width-20@12)
			// .font_(staticTextFont)
			// .stringColor_(staticTextColor)
			// .string_("device-IP/port")
			// ;

			deviceDropDown = PopUpMenu(thisEditor[\tabs].views[2], flow2.bounds.width-95@15)
				.items_(["select IP-address... (optional)"])
				.font_(Font("Arial", 10))
			;

			StaticText(thisEditor[\tabs].views[2], 60@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("restrict to port ")
				.align_(\right)
			;

			if(GUI.id !== \cocoa, {
				deviceDropDown.toolTip_(
					"Select from a list of IP-addresses which\nare currently sending OSC messages to\ncreate responders that will listen to\nthe selected address only."
				)
			});

			portRestrictor = buildCheckbox.(false, thisEditor[\tabs].views[2], 15@15, Font("Arial", 10, true));
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

			StaticText(thisEditor[\tabs].views[2], flow2.bounds.width-20@40)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("OSC command-name, e.g.: /my/cmd/name / OSC\nmessage slot: Either choose from a list of command-names\n(as set by the selected device) or add your custom one ")
			;

			flow2.shift(0, 0);

			deviceListMenu = PopUpMenu(thisEditor[\tabs].views[2], flow2.bounds.width/2-40@15)
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
					}, {
						cmdListMenu.items_(["command-names..."]);
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

			flow2.shift(0, 0);

			cmdListMenu = PopUpMenu(thisEditor[\tabs].views[2], flow2.bounds.width/2-11@15)
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
						indexField.clipHi_(m.items[m.value].asString.split($ )[1].interpret);
					})
				})
			;

			cmdNames.pairsDo({ |dev, cmds|
				deviceListMenu.items = deviceListMenu.items ++ dev;
			});

			flow2.shift(0, 0);

			addDeviceBut = Button(thisEditor[\tabs].views[2], 29@15)
				.states_([
					["new", Color.white, Color(0.15, 0.5, 0.15)]
				])
				.font_(staticTextFont)
				.action_({ OSCCommands.makeWindow })
			;

			if(GUI.id !== \cocoa, {
				addDeviceBut.toolTip_("Scan for incoming OSC-messages\nresp. their commandnames. These\ncan be saved to disk together with a\ndevice-name. You may then quickly\nselect from devices + commandnames\nfrom the dropdowns on the left.")
			});

			nameField = TextField(thisEditor[\tabs].views[2], flow2.bounds.width-60@15)
				.font_(textFieldFont)
				.stringColor_(textFieldFontColor)
				.background_(textFieldBg)
				.string_("/my/cmd/name")
			;

			if(GUI.id !== \cocoa, {
				nameField.toolTip_("Enter a commandname manualy or first\nselect the device in the dropdown-\nmenu above and then a commandname.\nThe commandname will automatically\nbe filled in here");
			});

			flow2.shift(5, 0);

			indexField = NumberBox(thisEditor[\tabs].views[2], 36@15)
				.font_(textFieldFont)
				.normalColor_(textFieldFontColor)
				.clipLo_(1)
				.clipHi_(inf)
				.shift_scale_(1)
				.ctrl_scale_(1)
				.alt_scale_(1)
				.value_(1)
			;

			if(GUI.id !== \cocoa, {
				indexField.toolTip_("A CVWidget expects values coming in in the\nsucceeding slots of the OSC-message (an array)\nbehind the comandname. An OSC-message\nmay have one or n slots. Select a valid slot\nby moving the mouse clicked up and down.")
			});

			flow2.shift(0, 0);

			StaticText(thisEditor[\tabs].views[2], flow2.bounds.width-15@15)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("OSC-input constraints + compensation")
			;

			inputConstraintLoField = NumberBox(thisEditor[\tabs].views[2], flow2.bounds.width/2-66@15)
				.font_(textFieldFont)
				.normalColor_(textFieldFontColor)
				.value_(wcm.oscInputRange.model.value[0])
				.enabled_(false)
			;

			flow2.shift(5, 0);

			inputConstraintHiField = NumberBox(thisEditor[\tabs].views[2], flow2.bounds.width/2-66@15)
				.font_(textFieldFont)
				.normalColor_(textFieldFontColor)
				.value_(wcm.oscInputRange.model.value[1])
				.enabled_(false)
			;

			if(GUI.id !== \cocoa, {
				[inputConstraintLoField, inputConstraintHiField].do(_.toolTip_("The constraints for incoming values - either\ndetermined automatically if calibration is on\nor set them manualy if calibration is off"))
			});

			flow2.shift(5, 0);

			alwaysPosField = StaticText(thisEditor[\tabs].views[2], 32@15)
				.font_(staticTextFont)
				.string_(" +"++widget.alwaysPositive)
				.stringColor_(Color(0.5))
				.background_(Color(0.95, 0.95, 0.95))
			;

			if(GUI.id !== \cocoa, {
				alwaysPosField.toolTip_("Make all input same-signed.\nAvoid NaN-results in calculations.")
			});

			flow2.shift(5, 0);

			calibBut = Button(thisEditor[\tabs].views[2], 60@15)
				.font_(staticTextFont)
				.states_([
					["calibrating", Color.black, Color.green],
					["calibrate", Color.white, Color.red]
				])
			;

			flow2.shift(0, 0);

			StaticText(thisEditor[\tabs].views[2], flow2.bounds.width-15@15)
				.font_(staticTextFont)
				.string_("Input to Output mapping")
			;

			flow2.shift(0, 0);

			specConstraintsText = StaticText(thisEditor[\tabs].views[2], flow2.bounds.width-15@15)
				.font_(staticTextFont)
				.background_(Color.white)
			;

			if(widget.class == CVWidgetMS, {
				specConstraintsText.string_(" current widget-spec constraints lo / hi:"+widget.getSpec.minval.wrapAt(slot)+"/"+widget.getSpec.maxval.wrapAt(slot))
			}, {
				specConstraintsText.string_(" current widget-spec constraints lo / hi:"+widget.getSpec(slot).minval+"/"+widget.getSpec(slot).maxval)
			});

			flow2.shift(5, 0);

			mappingSelectItems = ["linlin", "linexp", "explin", "expexp"];

			mappingSelect = PopUpMenu(thisEditor[\tabs].views[2], flow2.bounds.width-15@20)
				.font_(Font("Arial", 12))
				.items_(mappingSelectItems)
				.action_({ |ms|
					widget.setOscMapping(ms.item, slot);
				})
			;

			if(widget.getOscMapping(slot).notNil, {
				mappingSelectItems.do({ |item, i|
					if(item.asSymbol === widget.getOscMapping(slot), {
						mappingSelect.value_(i);
					});
				}, {
					mappingSelect.value_(0);
				})
			});

			flow2.shift(0, 0);

			connectorBut = Button(thisEditor[\tabs].views[2], flow2.bounds.width-15@25)
				.font_(staticTextFont)
				.states_([
					["connect OSC-controller", Color.white, Color.blue],
					["disconnect OSC-controller", Color.white, Color.red]
				])
				.action_({ |cb|
					cb.value.switch(
						1, {
							if(deviceDropDown.value > 0, {
								connectIP = deviceDropDown.items[deviceDropDown.value].asString.split($:)[0];
								if(portRestrictor.value.asBoolean, {
									connectPort = deviceDropDown.items[deviceDropDown.value].asString.split($:)[1];
								})
							});
							widget.oscConnect(
								connectIP,
								connectPort,
								nameField.string,
								indexField.value.asInt,
								slot
							);
						},
						0, { widget.oscDisconnect(slot) }
					)
				})
			;

			calibNumBoxes = (lo: inputConstraintLoField, hi: inputConstraintHiField);

			calibBut.action_({ |but|
				but.value.switch(
					0, {
						widget.setCalibrate(true, slot);
						wcm.calibration.model.value_(true).changedKeys(widget.synchKeys);
					},
					1, {
						widget.setCalibrate(false, slot);
						wcm.calibration.model.value_(false).changedKeys(widget.synchKeys);
					}
				)
			});

			widget.getCalibrate(slot).switch(
				true, { calibBut.value_(0) },
				false, { calibBut.value_(1) }
			);

			if(widget.class != CVWidgetMS, {
				actionName = TextField(thisEditor[\tabs].views[3], flow3.bounds.width-100@20)
					.string_("action-name")
					.font_(textFieldFont)
				;

				if(GUI.id !== \cocoa, {
					actionName.toolTip_("Mandatory: each action must\nbe saved under a unique name")
				});

				flow3.shift(5, 0);

				enterActionBut = Button(thisEditor[\tabs].views[3], 57@20)
					.font_(staticTextFont)
					.states_([
						["add Action", Color.white, Color.blue],
					])
					.action_({ |ab|
						if(actionName.string != "action-name" and:{
							enterAction.string != "{ |cv| /* do something */ }"
						}, {
							widget.addAction(actionName.string.asSymbol, enterAction.string.replace("\t", "    "), slot.asSymbol);
						})
					})
				;

				flow3.shift(0, 0);

				enterAction = TextView(thisEditor[\tabs].views[3], flow3.bounds.width-35@50)
					.background_(Color.white)
					.font_(textFieldFont)
					.string_("{ |cv| /* do something */ }")
					.syntaxColorize
				;

				if(GUI.id !== \cocoa, {
					enterAction.tabWidth_("    ".bounds.width);
					enterAction.toolTip_("The variable 'cv' holds the widget's CV resp.\n'cv.value' its current value. You may enter an\narbitrary function using this variable (or not).")
				});

				if(slot.notNil, {
					wdgtActions = widget.wdgtActions[slot];
				}, {
					wdgtActions = widget.wdgtActions;
				});

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
								0, { widget.activateAction(name, false, slot) },
								1, { widget.activateAction(name, true, slot) }
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
							widget.removeAction(name.asSymbol, slot.asSymbol);
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
				})
			});

			if(widget.class == CVWidgetMS, {
				[3, 0].do({ |i|
					tabs.removeAt(i);
					labelColors.removeAt(i);
					labelStringColors.removeAt(i);
				})
			});

			tabs.focusActions_((0..tabs.views.size-1).collect({ |t|
				{
					tabs.stringFocusedColor_(labelStringColors[t]);
					{ tabs.views[t].background_(Color(0.8, 0.8, 0.8, 1.0)) }.defer(0.01);
				}
			}));

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
		});

		tab !? {
			thisEditor[\tabs].focus(tab);
			if(widget.class == CVWidgetMS, {
				tabs.stringFocusedColor_(labelStringColors[tab]);
				tabs.views[tab].background_(Color(0.8, 0.8, 0.8, 1.0));
			})
		};
		thisEditor.window.front;
	}

	// not to be used directly!

	amendActionsList { |widget, addRemove, name, action, slot, active|

		var staticTextFont = Font("Arial", 9.4);
		var textFieldFont = Font("Andale Mono", 9);

		if(widget.class != CVWidgetMS, {
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
					[
						actionsList[name].nameField,
						actionsList[name].activate,
						actionsList[name].removeBut,
						actionsList[name].actionView
					].do(_.remove);
					flow3.reFlow(thisEditor[\tabs].views[3]);
				}
			)
		})

	}

	close {
		thisEditor.window.close;
		switch(allEditors[name].class,
			Event, {
				allEditors[name].removeAt(editorSlot);
				if(allEditors[name].isEmpty, { allEditors.removeAt(name) });
			},
			{ allEditors.removeAt(name) };
		);
		// if(allEditors.size == 0, { OSCCommands.collectTempIPsAndCmds(false) });
	}

}