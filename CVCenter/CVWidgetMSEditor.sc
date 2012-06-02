/* (c) 2010-2012 Stefan Nussbaumer */
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
CVWidgetMSEditor {
	
	classvar <allMSEditors;
	var thisEditor, <window, <tabs, msEditorEnv, labelStringColors;
	var <specField, <specsList, <specsListSpecs;
	var <ipField, <portField, <extCtrlArrayField, <nameField, <intStartIndexField, <indexField;
	var <calibBut, <calibNumBoxes;
	var <oscEditBtns, <oscCalibBtns;
	var deviceListMenu, cmdListMenu, addDeviceBut, thisCmdNames;
	var inputConstraintLoField, inputConstraintHiField, <alwaysPosField;
	var <mappingSelect, <connectorBut;
	var actionName, enterAction, enterActionBut, <actionsList;
	var name;
	var flow0, flow1, flow2, flow3;
	var oscFlow0, oscFlow1;

	*new { |widget, widgetName, tab|
		^super.new.init(widget, widgetName, tab);
	}
	
	init { |widget, widgetName, tab|
		var tabs, cvString;
		var oscTabs, midiTabs;
		var staticTextFont, staticTextFontBold, staticTextColor, textFieldFont, textFieldFontColor, textFieldBg;
		var msrc = "source", mchan = "chan", mctrl = "ctrl", margs;
		var addr, wcmMS, thisGuiEnv, labelColors;
		var oscLabelColor, midiLabelColor, oscLabelStringColor, midiLabelStringColor;
		var oscConnectCondition = 0;
		var oscConnectWarning = "Couldn't connect OSC-controllers:";
		var connectIP, connectPort, connectName, connectOscMsgIndex, connectIndexStart;
		var deviceListMenu, cmdListMenu, addDeviceBut, thisCmdNames;
		var midiModes;
		var thisMidiMode, thisMidiMean, thisMidiResolution, thisSoftWithin, thisCtrlButtonBank;
		var mappingSelectItems;
		var wdgtActions;
		var cmdNames, orderedCmds, orderedCmdSlots;
		var tmp; // multipurpose short-term var
		
		widget ?? {
			Error("CVWidgetEditor is a utility-GUI-class that should only be used in connection with an existing CVWidget").throw;
		};
		
		#oscEditBtns, oscCalibBtns = []!2;

		name = widgetName.asSymbol;
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
		
		staticTextFont = Font(Font.defaultSansFace, 10);
		staticTextFontBold = Font(Font.defaultSansFace, 10, true);
		staticTextColor = Color(0.2, 0.2, 0.2);
		textFieldFont = Font(Font.defaultMonoFace, 9);
		textFieldFontColor = Color.black;
		textFieldBg = Color.white;
		
		allMSEditors ?? { allMSEditors = IdentityDictionary() };

		if(thisEditor.isNil or:{ thisEditor.window.isClosed }, {
			window = Window("Widget Editor:"+widgetName, Rect(Window.screenBounds.width/2-200, Window.screenBounds.height/2-150, 400, 300));
			
			allMSEditors.put(name, (window: window, name: widgetName));
			thisEditor = allMSEditors[name];
			
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
		tabs.views[1].decorator = flow1 = FlowLayout(window.view.bounds, 7@7, 3@3);
		tabs.views[2].decorator = flow2 = FlowLayout(window.view.bounds, 0@0, 0@0);
		tabs.views[3].decorator = flow3 = FlowLayout(window.view.bounds, 7@7, 3@3);
		(0..3).do({ |t| tabs.focusActions[t] = { tabs.stringFocusedColor_(labelStringColors[t]) } });
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
		
		StaticText(thisEditor.tabs.views[0], flow0.bounds.width-20@50)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("Enter a ControlSpec in the textfield:\ne.g. ControlSpec(20, 20000, \\exp, 0.0, 440, \"Hz\")\nor \\freq \nor [[20, 20, 20, 20, 20], [20000, 20000, 20000, 20000, 20000], \\exp].asSpec.\nOr select a suitable ControlSpec from the List below.\nIf you don't know what this all means have a look\nat the ControlSpec-helpfile.")
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
		specField = TextView(thisEditor.tabs.views[0], flow0.bounds.width-20@105)
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
		
		if(msEditorEnv.specsListItems.notNil, {
			specsList.items_(msEditorEnv.specsListItems);
		}, {
			Spec.specs.pairsDo({ |k, v|
				if(v.isKindOf(ControlSpec), {
					specsList.items_(specsList.items.add(k++":"+v));
					specsListSpecs.add(v);
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
			.font_(Font("Helvetica", 10))
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
			.font_(Font("Helvetica", 10))
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
			.string_("ext. sliders (numeric array)")
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
		
		extCtrlArrayField = TextField(thisEditor.oscTabs.views[0], 65@15)
			.font_(textFieldFont)
			.stringColor_(textFieldFontColor)
			.background_(textFieldBg)
			.string_("(from..to)")
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
			.clipHi_(widget.msSize)
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
				
		mappingSelectItems = ["linlin", "linexp", "explin", "expexp"];
		
		mappingSelect = PopUpMenu(thisEditor.oscTabs.views[0], oscFlow0.bounds.width/2-12@20)
			.font_(Font("Helvetica", 12))
			.items_(mappingSelectItems)
//			.action_({ |ms|
//				widget.setOscMapping(ms.item, slot);
//			})
		;
		
//		if(widget.getOscMapping(slot).notNil, {
//			mappingSelectItems.do({ |item, i|
//				if(item.asSymbol === widget.getOscMapping(slot), {
//					mappingSelect.value_(i);
//				});
//			}, {
//				mappingSelect.value_(0);
//			})
//		});
		
		calibBut = Button(thisEditor.oscTabs.views[0],  oscFlow0.bounds.width/2-12@20)
			.font_(staticTextFont)
			.states_([
				["calibrating all", Color.white, Color.red],
				["partially calibrating", Color.white, Color.red(0.7)],
				["calibrate all", Color.black, Color.green]
			])
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
				// 	extCtrlArrayField -> external controllers
				//	ipField -> IP-address
				//	portField -> port
				//	nameField -> osc-cmds, ext. controllers as placeholders
				//	intStartIndexField -> multislider-index to start connecting at
				//	indexField -> msg-slot, an integer or a placeholder, starting at 1
				cb.value.switch(
					1, { 
						if(extCtrlArrayField.string.interpret.isArray and:{
							extCtrlArrayField.string.interpret.collect(_.isInteger).size == 
							extCtrlArrayField.string.interpret.size
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
							"ok, we're ready to rock".postln;
							extCtrlArrayField.string.interpret.do({ |ext, i|
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
								connectIndexStart = intStartIndexField.value+i;
								widget.oscConnect(
									name: connectIP, 
									port: connectPort, 
									name: connectName, 
									oscMsgIndex: connectOscMsgIndex,
									slot: connectIndexStart
								)
							})
						})
					},
					0, {/* widget.oscDisconnect(slot) */}
				)
			})
		;

//		calibNumBoxes = (lo: inputConstraintLoField, hi: inputConstraintHiField);
//		
//		calibBut.action_({ |but|
//			but.value.switch(
//				0, { 
//					widget.setCalibrate(true, slot);
//					wcmHiLo.calibration.model.value_(true).changed(\value);
//				},
//				1, { 
//					widget.setCalibrate(false, slot);
//					wcmHiLo.calibration.model.value_(false).changed(\value);
//				}
//			)
//		});
//
//		widget.getCalibrate(slot).switch(
//			true, { calibBut.value_(0) },
//			false, { calibBut.value_(1) }
//		);

		widget.msSize.do({ |sindex|
			oscEditBtns = oscEditBtns.add(
				Button(thisEditor.oscTabs.views[1], oscFlow0.bounds.width/5-10@25)
					.states_([
						[sindex.asString++": edit OSC", Color.black, Color.white(0.2)]
					])
					.font_(staticTextFont)
					.action_({ |bt|
						if(widget.editor[sindex].isNil or:{ widget.editor[sindex].isClosed }, {
							widget.editor[sindex] = CVWidgetEditor(widget, widget.label.states[0][0], 1, sindex);
							widget.guiEnv.editor[sindex] = widget.editor[sindex];
						}, {
							widget.editor[sindex].front(1)
						});
//						wdgtControllersAndModels.oscDisplay.model.value_(
//							wdgtControllersAndModels.oscDisplay.model.value;
//						).changed(\value);
//						wdgtControllersAndModels.midiDisplay.model.value_(
//							wdgtControllersAndModels.midiDisplay.model.value
//						).changed(\value);
					})
				;
			);
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
	
	front { |tab|
		thisEditor.window.front;
		tab !? { 
			thisEditor.tabs.stringFocusedColor_(labelStringColors[tab]);
			thisEditor.tabs.focus(tab);
		}
	}
	
	close { |slot|
		thisEditor.window.close;
		allMSEditors.removeAt(name);
	}
	
	isClosed { 
		var ret;
		thisEditor.window !? {
			ret = defer { thisEditor.window.isClosed };
			^ret.value;
		}
	}
	
	
}