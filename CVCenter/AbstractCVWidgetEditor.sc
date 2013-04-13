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

AbstractCVWidgetEditor {

	classvar <allEditors, xySlots, nextX, nextY, shiftXY;
	var thisEditor, <window, <tabs, editorEnv, labelStringColors;
	var <specField, <specsList, <specsListSpecs;
	var <midiModeSelect, <midiMeanNB, <softWithinNB, <ctrlButtonBankField, <midiResolutionNB;
	var <midiInitBut, <midiSourceSelect, <midiLearnBut, <midiSrcField, <midiChanField, <midiCtrlField;
	var <calibBut;
	var deviceListMenu, cmdListMenu, addDeviceBut, thisCmdNames;
	var <deviceDropDown, <ipField, <portField, <nameField, <indexField;
	var inputConstraintLoField, inputConstraintHiField, <alwaysPosField;
	var <mappingSelect, <connectorBut;
	var actionName, enterAction, enterActionBut, <actionsList;
	var name;
	var flow0, flow1, flow2, flow3;

	*initClass {
		var localOscFunc;

		allEditors = IdentityDictionary.new;
	}

	front { |tab|
		thisEditor.window.front;
		tab !? {
			thisEditor[\tabs].stringFocusedColor_(labelStringColors[tab]);
			thisEditor[\tabs].focus(tab);
		}
	}

	isClosed {
		var ret;
		thisEditor.window !? {
			ret = defer { thisEditor.window.isClosed };
			^ret.value;
		}
	}

}