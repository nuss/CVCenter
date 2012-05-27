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
		var staticTextFont, staticTextColor, textFieldFont, textFieldFontColor, textFieldBg;
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
//		widget.msSize.do({ |i|
//			thisMidiMode[i] = widget.getMidiMode(i);
//			thisMidiMean[i] = widget.getMidiMean(i);
//			thisMidiResolution[i] = widget.getMidiResolution(i);
//			thisSoftWithin[i] = widget.getSoftWithin(i);
//			thisCtrlButtonBank[i] = widget.getCtrlButtonBank(i);
//		});
		
		staticTextFont = Font(Font.defaultSansFace, 10);
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