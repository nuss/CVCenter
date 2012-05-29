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
	var <ipField, <portField, <nameField, <indexField;
	var <calibBut, <calibNumBoxes;
	var deviceListMenu, cmdListMenu, addDeviceBut, thisCmdNames;
	var inputConstraintLoField, inputConstraintHiField, <alwaysPosField;
	var <mappingSelect, <connectorBut;
	var actionName, enterAction, enterActionBut, <actionsList;
	var name;
	var flow0, flow1, flow2, flow3;

	*new { |widget, widgetName, tab|
		^super.new.init(widget, widgetName, tab);
	}
	
	init { |widget, widgetName, tab|
		var tabs, cvString;
		var staticTextFont, staticTextFontBold, staticTextColor, textFieldFont, textFieldFontColor, textFieldBg;
		var msrc = "source", mchan = "chan", mctrl = "ctrl", margs;
		var addr, wcmMS, thisGuiEnv, labelColors;
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
		tabs.views[2].decorator = flow2 = FlowLayout(window.view.bounds, 7@7, 3@3);
		tabs.views[3].decorator = flow3 = FlowLayout(window.view.bounds, 7@7, 3@3);
		(0..3).do({ |t| tabs.focusActions[t] = { tabs.stringFocusedColor_(labelStringColors[t]) } });
		tabs.stringFocusedColor_(labelStringColors[tab]);

		thisEditor.tabs = tabs;
		
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
		
		StaticText(thisEditor.tabs.views[2], flow2.bounds.width-20@40)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("Sliders within a CVWidgetMS can be conected to external OSC-controllers one by one or you can batch-connect them using the following mask. Basically this happens by running a do-loop over an array of controller-numbers. For example (your widget is stored in the variable 'm', the command-name is '/mfader/(1-10)':")
		;

		StaticText(thisEditor.tabs.views[2], flow2.bounds.width-20@20)
			.font_(staticTextFont)
			.stringColor_(textFieldFont)
			.string_(" (1..10).do({ |n, i| format(\"m.oscConnect(nil, nil, '/mfader/%, 1, %)\", n, i).interpret })")
			.background_(Color(1.0, 1.0, 1.0, 0.5))
		;
		
		StaticText(thisEditor.tabs.views[2], flow2.bounds.width-20@50)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("In the above example 2 placeholders are used (%). the first is the external controller-number, the second the slider-index within the multislider. You may specifiy one placeholder either within the command-name or the slot (depending on the layout of the OSC-command your widget shall listen to). For further information check the String-helpfile resp. CVWidget:oscConnect")
		;

		flow2.shift(0, 10);
		
		StaticText(thisEditor.tabs.views[2], flow2.bounds.width-155@15)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("IP-address (optional)")
		;
		
		flow2.shift(0, 0);
		
		StaticText(thisEditor.tabs.views[2], 130@15)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("port (usually not necessary)")
		;

		ipField = TextField(thisEditor.tabs.views[2], flow2.bounds.width-155@15)
			.font_(textFieldFont)
			.stringColor_(textFieldFontColor)
			.background_(textFieldBg)
			.string_("")
		;
		
		flow2.shift(0, 0);

		portField = TextField(thisEditor.tabs.views[2], 130@15)
			.font_(textFieldFont)
			.stringColor_(textFieldFontColor)
			.background_(textFieldBg)
			.string_("")
		;

		StaticText(thisEditor.tabs.views[2], flow2.bounds.width-20@40)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("OSC command-name, e.g.: /my/cmd/name / OSC message slot: Either choose from a list of command-names (as set by the selected device) or add your custom one ")
		;

		flow2.shift(0, 0);
		
		deviceListMenu = PopUpMenu(thisEditor.tabs.views[2], flow2.bounds.width/2-40@15)
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
		
		flow2.shift(0, 0);
		
		cmdListMenu = PopUpMenu(thisEditor.tabs.views[2], flow2.bounds.width/2-11@15)
			.items_(["command-names..."])
			.font_(Font("Helvetica", 10))
			.action_({ |m|
				if(nameField.enabled, {
					nameField.string_(thisCmdNames[m.value]);
					indexField.clipHi_(orderedCmdSlots[m.value-1]);
				})
			})
		;
		
		cmdNames.pairsDo({ |dev, cmds|
			deviceListMenu.items = deviceListMenu.items ++ dev;
		});
		
		flow2.shift(0, 0);
		
		addDeviceBut = Button(thisEditor.tabs.views[2], 29@15)
			.states_([
				["new", Color.white, Color(0.15, 0.5, 0.15)]
			])
			.font_(staticTextFont)
			.action_({ OSCCommands.makeWindow })
		;

		nameField = TextField(thisEditor.tabs.views[2], flow2.bounds.width-60@15)
			.font_(textFieldFont)
			.stringColor_(textFieldFontColor)
			.background_(textFieldBg)
			.string_("/my/cmd/name")
		;
					
		flow2.shift(5, 0);
		
		indexField = NumberBox(thisEditor.tabs.views[2], 36@15)
			.font_(textFieldFont)
			.normalColor_(textFieldFontColor)
			.clipLo_(1)
			.clipHi_(inf)
			.shift_scale_(1)
			.ctrl_scale_(1)
			.alt_scale_(1)
			.value_(1)
		;
		
		flow2.shift(0, 0);

		StaticText(thisEditor.tabs.views[2], flow2.bounds.width-15@15)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("OSC-input constraints + compensation")
		;
								
		inputConstraintLoField = NumberBox(thisEditor.tabs.views[2], flow2.bounds.width/2-66@15)
			.font_(textFieldFont)
			.normalColor_(textFieldFontColor)
//			.value_(wcmHiLo.oscInputRange.model.value[0])
			.enabled_(false)
		;
		
		flow2.shift(5, 0);
		
		inputConstraintHiField = NumberBox(thisEditor.tabs.views[2], flow2.bounds.width/2-66@15)
			.font_(textFieldFont)
			.normalColor_(textFieldFontColor)
//			.value_(wcmHiLo.oscInputRange.model.value[1])
			.enabled_(false)
		;
					
		flow2.shift(5, 0);
		
		alwaysPosField = StaticText(thisEditor.tabs.views[2], 32@15)
			.font_(staticTextFont)
			.string_(" +"++widget.alwaysPositive)
			.stringColor_(Color(0.5))
			.background_(Color(0.95, 0.95, 0.95))
		;
					
		flow2.shift(5, 0);

		calibBut = Button(thisEditor.tabs.views[2], 60@15)
			.font_(staticTextFont)
			.states_([
				["calibrating", Color.white, Color.red],
				["calibrate", Color.black, Color.green]
			])
		;

		flow2.shift(0, 0);

		StaticText(thisEditor.tabs.views[2], flow2.bounds.width-15@15)
			.font_(staticTextFont)
			.string_("Input to Output mapping")
		;
		
		flow2.shift(0, 0);

		StaticText(thisEditor.tabs.views[2], flow2.bounds.width-15@15)
			.font_(staticTextFont)
			.background_(Color.white)
//			.string_(" current widget-spec constraints lo / hi:"+widget.getSpec(slot).minval+"/"+widget.getSpec(slot).maxval)
		;

		flow2.shift(5, 0);
		
		mappingSelectItems = ["linlin", "linexp", "explin", "expexp"];
		
		mappingSelect = PopUpMenu(thisEditor.tabs.views[2], flow2.bounds.width-15@20)
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
					
		flow2.shift(0, 0);

		connectorBut = Button(thisEditor.tabs.views[2], flow2.bounds.width-15@25)
			.font_(staticTextFont)
			.states_([
				["connect OSC-controller", Color.white, Color.blue],
				["disconnect OSC-controller", Color.white, Color.red]
			])
//			.action_({ |cb|
//				cb.value.switch(
//					1, { 
//						widget.oscConnect(
//							ipField.string,
//							portField.value,
//							nameField.string, 
//							indexField.value.asInt,
//							slot
//						);
//					},
//					0, { widget.oscDisconnect(slot) }
//				)
//			})
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