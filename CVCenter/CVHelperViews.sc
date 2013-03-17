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

CVMidiEditGroup {

	var <midiLearn, <midiHead, <midiSrc, <midiChan, <midiCtrl;

	*new { |parent, bounds, widget, slot|
		^super.new.init(parent, bounds, widget, slot)
	}

	init { |parentView, bounds, widget, slot|
		var flow, thisSlot;
		var margs, msrc, mchan, mctrl;
		var wcm, editor, tabIndex;

		switch(widget.class,
			CVWidget2D, {
				thisSlot = slot.asSymbol;
				wcm = widget.wdgtControllersAndModels[thisSlot];
				if(widget.editor[thisSlot].notNil and:{ widget.editor[thisSlot].isClosed.not }, {
					editor = widget.editor[thisSlot];
				})
			},
			CVWidgetMS, {
				thisSlot = slot.asInt;
				wcm = widget.wdgtControllersAndModels.slots[thisSlot];
				if(widget.editor[thisSlot].notNil and:{ widget.editor[thisSlot].isClosed.not }, {
					editor = widget.editor[thisSlot];
				})
			},
			{
				wcm = widget.wdgtControllersAndModels;
				if(widget.editor.notNil and:{ widget.editor.isClosed.not }, { editor = widget.editor })
			}
		);

		if(widget.isNil or:{ widget.isKindOf(CVWidget).not }, {
			Error("CVMidiEditGroup is a utility-class to be used with CVWidgets only.").throw
		});

		flow = parentView.addFlowLayout(0, 0);

		midiHead = Button(parentView, bounds.width-(bounds.height.div(3))@bounds.height.div(3))
			.font_("Arial", 8)
			.action_({ |mh|
				if(editor.isNil, {
				if(widget.class = CVWidgetMS, { tabIndex = 0 }, { tabIndex = 1 });
					editor = CVWidgetEditor(this, widget.label.states[0][0], tabIndex, thisSlot);
					switch(widget.class,
						CVWidget2D, {
							widget.editor.put(thisSlot, editor);
							widget.guiEnv[thisSlot].editor = editor;
						},
						CVWidgetMS, {
							widget.editor[\editors][thisSlot] = editor;
							widget.guiEnv[\editor][thisSlot] = editor;
						},
						{
							widget.editor = editor;
							widget.guiEnv.editor = editor;
						}
					)
				}, {
					editor.front(1)
				});
				wcm.oscDisplay.model.value_(
					wcm.oscDisplay.model.value;
				).changedKeys(widget.synchKeys);
				wcm.midiDisplay.model.value_(
					wcm.midiDisplay.model.value
				).changedKeys(widget.synchKeys);
			})
		;

		if(slot.notNil, {
			midiHead.states_([[thisSlot.asString++": MIDI", Color.black, Color.white]]);
		}, {
			midiHead.states_([["MIDI", Color.black, Color.white]]);
		});

		if(GUI.id === \qt, {
			if(slot.notNil, {
				midiHead.mouseEnterAction_({ |mh|
					mh.states_([[thisSlot.asString++": MIDI", Color.white, Color.red]])
				}).mouseLeaveAction_({ |mh|
					mh.states_([[thisSlot.asString++": MIDI", Color.black, Color.white]])
				})
			}, {
				midiHead.mouseEnterAction_({ |mh|
					mh.states_([["MIDI", Color.white, Color.red]])
				}).mouseLeaveAction_({ |mh|
					mh.states_([["MIDI", Color.black, Color.white]])
				})
			})
		});

		midiLearn = Button(parentView, bounds.height.div(3)@bounds.height.div(3))
			.font_("Arial", 8)
			.states_([
				["L", Color.white, Color.blue],
				["X", Color.white, Color.red]
			])
			.action_({ |ml|
				ml.value.switch(
					1, {
						margs = [
							[midiSrc.string, msrc],
							[midiChan.string, mchan],
							[midiCtrl.string, mctrl]
						].collect({ |pair| if(pair[0] != pair[1], { pair[0].asInt }, { nil }) });
						if(margs.select({ |i| i.notNil }).size > 0, {
							widget.midiConnect(uid: margs[0], chan: margs[1], num: margs[2], slot: thisSlot);
						}, {
							widget.midiConnect(slot: thisSlot);
						})
					},
					0, { widget.midiDisconnect(thisSlot) }
				)
			})
		;

		midiSrc = TextField(parentView, bounds.width@bounds.height.div(3))
			.font_("Andale Mono", 8)
			.string_(msrc)
			.background_(Color.white)
			.stringColor_(Color.black)
			.action_({ |ms|
				if("^[-+]?[0-9]*$".matchRegexp(ms.string), {
					wcm.midiDisplay.model.value_((
						learn: "C",
						src: ms.string,
						chan: wcm.midiDisplay.model.value.chan,
						ctrl: wcm.midiDisplay.model.value.ctrl
					)).changedKeys(widget.synchKeys)
				})
			})
			.mouseDownAction_({ |ms|
				ms.stringColor_(Color.red)
			})
			.keyUpAction_({ |ms, char, modifiers, unicode, keycode|
				if(unicode == 13, {
					ms.stringColor_(Color.black)
				})
			})
		;
	}

}