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
			.string_("Enter a ControlSpec in the textfield:\ne.g. ControlSpec(20!5, 20000!5, \\exp, 0.0, [440, 330, 220, 110, 55], \"Hz\")\nor \\freq \nor [[20, 20, 20, 20, 20], [20000, 20000, 20000, 20000, 20000], \\exp].asSpec.\nOr select a suitable ControlSpec from the List below.\nIf you don't know what this all means have a look\nat the ControlSpec-helpfile.")
		;
		
		flow0.shift(0, 5);
		
		StaticText(thisEditor.tabs.views[0], flow0.bounds.width-20@45)
			.font_(staticTextFontBold)
			.stringColor_(staticTextColor)
			.string_("NOTE: CVWidgetMS it expects a Spec whose minvals, maxvals, step-sizes and/or default-values are arrays of the size of the number of sliders in the multislider. However, you may provide a spec like 'freq' and its parameters will internally expanded to arrays of the required size.")
		;
		
		flow0.shift(0, 5);
		
		StaticText(thisEditor.tabs.views[0], flow0.bounds.width-20@10)
			.font_(staticTextFont)
			.stringColor_(staticTextColor)
			.string_("Enter the desired Spec and execute it shift+return.")
		;

		flow0.shift(0, 5);
		
		cvString = widget.getSpec.asCompileString;
//		cvString = widget.getSpec.asCompileString.split($ );
//		cvString.postcs;
//		cvString = cvString[1..cvString.size-1].join(" ");
		
		flow0.shift(0, 5);
		
		specField = TextView(thisEditor.tabs.views[0], flow0.bounds.width-20@105)
			.font_(staticTextFont)
			.enterInterpretsSelection_(true)
			.string_(cvString)
			.keyDownAction_({ |tf, char, modifiers, unicode, keycode|
//				[tf, char, modifiers, unicode, keycode].postcs;
				if(char == $\r and:{ modifiers == 131072 }, {
					widget.setSpec(tf.string.interpret)
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