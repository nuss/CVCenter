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

CVWidget {

	classvar <>removeResponders;
	var <window, <guiEnv;
	var <widgetCV, prDefaultAction, <>wdgtActions, <>bgColor, <alwaysPositive = 0.1;
	var prMidiMode, prMidiMean, prCtrlButtonBank, prMidiResolution, prSoftWithin;
	var prCalibrate, netAddr; // OSC-calibration enabled/disabled, NetAddr if not nil at instantiation
	var visibleGuiEls, allGuiEls, isCVCWidget = false;
	var <widgetBg, <label, <nameField, wdgtInfo; // elements contained in any kind of CVWidget
	var widgetXY, widgetProps, <editor;
	var <wdgtControllersAndModels, <midiOscEnv;
	// persistent widgets
	var isPersistent, oldBounds, oldName;
	// extended API
	var <synchKeys, synchedActions;
	// special bookkeeping for CVWidgetMS
	var msCmds, msSlots;
	var slotCmdName, lastIntSlots, msSlotsChecked = false;
	var lastMsgIndex, msMsgIndexDiffers = false, count = 0;

	*initClass {
		StartUp.add({
			if(Quarks.isInstalled("cruciallib"), {
				Spec.add(\in, StaticIntegerSpec(0, Server.default.options.firstPrivateBus-1, 0));
			})
		})
	}

	setup {
		^(
			midiMode: prMidiMode,
			midiResolution: prMidiResolution,
			midiMean: prMidiMean,
			ctrlButtonBank: prCtrlButtonBank,
			softWithin: prSoftWithin,
			calibrate: prCalibrate
		);
	}

	toggleComment { |visible|
		visible.switch(
			false, {
				visibleGuiEls.do({ |el|
					el.visible_(true);
					nameField.visible_(false);
				})
			},
			true, {
				visibleGuiEls.do({ |el|
					el.visible_(false);
					nameField.visible_(true);
				})
			}
		)
	}

	widgetXY_ { |point|
		var originXZero, originYZero;
		originXZero = allGuiEls.collect({ |view| view.bounds.left });
		originXZero = originXZero-originXZero.minItem;
		originYZero = allGuiEls.collect({ |view| view.bounds.top });
		originYZero = originYZero-originYZero.minItem;

		allGuiEls.do({ |view, i|
			view.bounds_(Rect(originXZero[i]+point.x, originYZero[i]+point.y, view.bounds.width, view.bounds.height));
		})
	}

	widgetXY {
		^widgetBg.bounds.left@widgetBg.bounds.top;
	}

	widgetProps {
		^widgetBg.bounds.width@widgetBg.bounds.height;
	}

	bounds {
		^Rect(this.widgetXY.x, this.widgetXY.y, this.widgetProps.x, this.widgetProps.y);
	}

	remove {
		allGuiEls.do(_.remove);
	}

	close {
		if(isCVCWidget and:{ isPersistent == false or:{ isPersistent == nil }}, { this.remove }, { this.window.close });
	}

	addAction { |name, action, slot, active=true|
		var act, controller, thisGuiEnv;
		name ?? { Error("Please provide a name under which the action will be added to the widget").throw };
		action ?? { Error("Please provide an action!").throw };
		if(action.isFunction.not and:{
			action.interpret.isFunction.not
		}, {
			Error("'action' must be a function or a string that compiles to one").throw;
		});
		this.wdgtActions ?? { this.wdgtActions = () };
		if(action.class === String, { act = action.interpret }, { act = action });
		switch(this.class,
			CVWidget2D, {
				slot ?? { Error("Please provide either 'lo' or 'hi' as third argument to addAction!").throw };
				this.wdgtActions[slot.asSymbol] ?? { this.wdgtActions.put(slot.asSymbol, ()) };
				// avoid duplicates
				this.wdgtActions[slot.asSymbol][name.asSymbol] ?? { this.wdgtActions[slot.asSymbol].put(name.asSymbol, ()) };
				if(this.wdgtActions[slot.asSymbol][name.asSymbol].size < 1, {
					if(active == true, {
						controller = widgetCV[slot.asSymbol].action_(act);
						this.wdgtActions[slot.asSymbol][name.asSymbol].put(controller, [act.asCompileString, true]);
					}, {
						controller = \dummy;
						this.wdgtActions[slot.asSymbol][name.asSymbol].put(controller, [act.asCompileString, false]);
					});
					wdgtControllersAndModels[slot.asSymbol].actions.model.value_((
						numActions: this.wdgtActions[slot.asSymbol].size,
						activeActions: this.wdgtActions[slot.asSymbol].select({ |v| v.asArray[0][1] == true }).size
					)).changedKeys(synchKeys);
					thisGuiEnv = this.guiEnv[slot.asSymbol];
					if(thisGuiEnv.editor.notNil and: {
						thisGuiEnv.editor.isClosed.not;
					}, {
						thisGuiEnv.editor.amendActionsList(
							this, \add, name.asSymbol, this.wdgtActions[slot.asSymbol][name.asSymbol], slot.asSymbol, active;
						)
					})
				})
			},
			{
				this.wdgtActions[name.asSymbol] ?? {
					this.wdgtActions.put(name.asSymbol, ());
					if(active == true, {
						controller = widgetCV.action_(act);
						this.wdgtActions[name.asSymbol].put(controller, [act.asCompileString, true]);
					}, {
						controller = \dummy;
						this.wdgtActions[name.asSymbol].put(controller, [act.asCompileString, false]);
					});
					wdgtControllersAndModels.actions.model.value_((
						numActions: this.wdgtActions.size,
						activeActions: this.wdgtActions.select({ |v| v.asArray[0][1] == true }).size
					)).changedKeys(synchKeys);
				};
				thisGuiEnv = this.guiEnv;
				if(thisGuiEnv.editor.notNil and: {
					thisGuiEnv.editor.isClosed.not;
				}, {
					thisGuiEnv.editor.amendActionsList(
						this, \add, name.asSymbol, this.wdgtActions[name.asSymbol], active: active;
					)
				})
			}
		)
	}

	removeAction { |name, slot|
		var controller, thisGuiEnv;
		name ?? { Error("Please provide the action's name!").throw };
		switch(this.class,
			CVWidget2D, {
				slot ?? { Error("Please provide either 'lo' or 'hi' as second argument to removeAction!").throw };
				thisGuiEnv = this.guiEnv[slot.asSymbol];
				this.wdgtActions[slot.asSymbol][name.asSymbol] !? {
					this.wdgtActions[slot.asSymbol][name.asSymbol].keys.do({ |c|
						if(c.class === SimpleController, { c.remove });
					});
					this.wdgtActions[slot.asSymbol].removeAt(name.asSymbol);
					this.wdgtActions[slot.asSymbol].isEmpty.if { this.wdgtActions.removeAt(slot.asSymbol) };
					wdgtControllersAndModels[slot.asSymbol].actions.model.value_((
						numActions: this.wdgtActions[slot.asSymbol].size,
						activeActions: this.wdgtActions[slot.asSymbol].select({ |v| v.asArray[0][1] == true }).size
					)).changedKeys(synchKeys);
					if(thisGuiEnv.editor.notNil and: {
						thisGuiEnv.editor.isClosed.not;
					}, {
						thisGuiEnv.editor.amendActionsList(
							this, \remove, name.asSymbol;
						)
					})
				}
			},
			{
				thisGuiEnv = this.guiEnv;
				this.wdgtActions[name.asSymbol] !? {
					this.wdgtActions[name.asSymbol].keys.do({ |c|
						if(c.class === SimpleController, { c.remove });
					});
					this.wdgtActions.removeAt(name.asSymbol);
					wdgtControllersAndModels.actions.model.value_((
						numActions: this.wdgtActions.size,
						activeActions: this.wdgtActions.select({ |v| v.asArray[0][1] == true }).size
					)).changedKeys(synchKeys);
					if(thisGuiEnv.editor.notNil and: {
						thisGuiEnv.editor.isClosed.not;
					}, {
						thisGuiEnv.editor.amendActionsList(
							this, \remove, name.asSymbol;
						)
					})
				}
			}
		);
		controller.do({ |c| c = nil });
	}

	activateAction { |name, activate=true, slot|
		var action, actions, cv, thisGuiEnv, wcm, controller, thisAction;

		if(slot.notNil, {
			cv = widgetCV[slot.asSymbol];
			actions = this.wdgtActions[slot.asSymbol];
			action = this.wdgtActions[slot.asSymbol][name.asSymbol];
			thisGuiEnv = this.guiEnv[slot.asSymbol];
			wcm = wdgtControllersAndModels[slot.asSymbol];
		}, {
			cv = widgetCV;
			actions = this.wdgtActions;
			action = this.wdgtActions[name.asSymbol];
			thisGuiEnv = this.guiEnv;
			wcm = wdgtControllersAndModels;
		});

		if(action.notNil, {
			switch(activate,
				true, {
					if(action.keys.asArray[0].class != SimpleController, {
						if(action.asArray[0][0].class === String, {
							thisAction = action.asArray[0][0].interpret;
						}, {
							thisAction = action.asArray[0][0];
						});
						controller = cv.action_(thisAction);
						action.put(controller, [thisAction.asCompileString, true]);
						action.removeAt(\dummy);
					})
				},
				false, {
					if(action.keys.asArray[0].class == SimpleController, {
						controller = action.keys.asArray[0];
						controller.remove;
						action.put(\dummy, [action.asArray[0][0], false]);
						action[controller] = nil;
					})
				}
			);
			wcm.actions.model.value_((
				numActions: actions.size,
				activeActions: actions.select({ |v| v.asArray[0][1] == true }).size
			)).changedKeys(synchKeys);
			if(thisGuiEnv.editor.notNil and: {
				thisGuiEnv.editor.isClosed.not;
			}, {
				switch(activate,
					true, {
						thisGuiEnv.editor.actionsList[name.asSymbol].activate.value_(1);
					},
					false, {
						thisGuiEnv.editor.actionsList[name.asSymbol].activate.value_(0);
					}
				)
			})
		})
	}

	setMidiMode { |mode, slot|
		var thisSlot;
		switch(this.class,
			CVWidget2D, { thisSlot = slot.asSymbol },
			CVWidgetMS, { thisSlot = slot.asInt }
		);

		switch(this.class,
			CVWidgetKnob, {
				prMidiMode = mode;
				wdgtControllersAndModels !? {
					wdgtControllersAndModels.midiOptions.model.value_(
						(
							midiMode: prMidiMode,
							midiMean: prMidiMean,
							ctrlButtonBank: prCtrlButtonBank,
							midiResolution: prMidiResolution,
							softWithin: prSoftWithin
						)
					).changedKeys(synchKeys);
				}
			},
			{
				prMidiMode[thisSlot] = mode;
				wdgtControllersAndModels[thisSlot] !? {
					wdgtControllersAndModels[thisSlot].midiOptions.model.value_(
						(
							midiMode: prMidiMode[thisSlot],
							midiMean: prMidiMean[thisSlot],
							ctrlButtonBank: prCtrlButtonBank[thisSlot],
							midiResolution: prMidiResolution[thisSlot],
							softWithin: prSoftWithin[thisSlot]
						)
					).changedKeys(synchKeys);
				}
			}
		);
	}

	getMidiMode { |slot|
		var thisSlot;
		switch(this.class,
			CVWidget2D, { thisSlot = slot.asSymbol },
			CVWidgetMS, { thisSlot = slot.asInt }
		);
		switch(this.class,
			CVWidgetKnob, {
				^prMidiMode;
			},
			{ ^prMidiMode[thisSlot] }
		)
	}

	setMidiMean { |meanval, slot|
		var thisSlot;
		switch(this.class,
			CVWidget2D, { thisSlot = slot.asSymbol },
			CVWidgetMS, { thisSlot = slot.asInt }
		);
		switch(this.class,
			CVWidgetKnob, {
				prMidiMean = meanval;
				wdgtControllersAndModels !? {
					wdgtControllersAndModels.midiOptions.model.value_(
						(
							midiMode: prMidiMode,
							midiMean: prMidiMean,
							ctrlButtonBank: prCtrlButtonBank,
							midiResolution: prMidiResolution,
							softWithin: prSoftWithin
						)
					).changedKeys(synchKeys);
				}
			},
			{
				prMidiMean[thisSlot] = meanval;
				wdgtControllersAndModels[thisSlot] !? {
					wdgtControllersAndModels[thisSlot].midiOptions.model.value_(
						(
							midiMode: prMidiMode[thisSlot],
							midiMean: prMidiMean[thisSlot],
							ctrlButtonBank: prCtrlButtonBank[thisSlot],
							midiResolution: prMidiResolution[thisSlot],
							softWithin: prSoftWithin[thisSlot]
						)
					).changedKeys(synchKeys);
				}
			}
		)
	}

	getMidiMean { |slot|
		var thisSlot;
		switch(this.class,
			CVWidget2D, { thisSlot = slot.asSymbol },
			CVWidgetMS, { thisSlot = slot.asInt }
		);
		switch(this.class,
			CVWidgetKnob, {
				^prMidiMean;
			},
			{ ^prMidiMean[thisSlot] }
		)
	}

	setSoftWithin { |threshold, slot|
		var thisSlot;
		switch(this.class,
			CVWidget2D, { thisSlot = slot.asSymbol },
			CVWidgetMS, { thisSlot = slot.asInt }
		);
		switch(this.class,
			CVWidgetKnob, {
				prSoftWithin = threshold;
				wdgtControllersAndModels !? {
					wdgtControllersAndModels.midiOptions.model.value_(
						(
							midiMode: prMidiMode,
							midiMean: prMidiMean,
							ctrlButtonBank: prCtrlButtonBank,
							midiResolution: prMidiResolution,
							softWithin: prSoftWithin
						)
					).changedKeys(synchKeys);
				}
			},
			{
				prSoftWithin[thisSlot] = threshold;
				wdgtControllersAndModels[thisSlot] !? {
					wdgtControllersAndModels[thisSlot].midiOptions.model.value_(
						(
							midiMode: prMidiMode[thisSlot],
							midiMean: prMidiMean[thisSlot],
							ctrlButtonBank: prCtrlButtonBank[thisSlot],
							midiResolution: prMidiResolution[thisSlot],
							softWithin: prSoftWithin[thisSlot]
						)
					).changedKeys(synchKeys);
				}
			}
		)
	}

	getSoftWithin { |slot|
		var thisSlot;
		switch(this.class,
			CVWidget2D, { thisSlot = slot.asSymbol },
			CVWidgetMS, { thisSlot = slot.asInt }
		);
		switch(this.class,
			CVWidgetKnob, {
				^prSoftWithin;
			},
			{ ^prSoftWithin[thisSlot] }
		)
	}

	setCtrlButtonBank { |numSliders, slot|
		var thisSlot;
		switch(this.class,
			CVWidget2D, { thisSlot = slot.asSymbol },
			CVWidgetMS, { thisSlot = slot.asInt }
		);
		switch(this.class,
			CVWidgetKnob, {
				if(numSliders.asString == "nil" or:{ numSliders.asInt === 0 }, {
					prCtrlButtonBank = nil;
				}, {
					prCtrlButtonBank = numSliders.asInt;
				});
				wdgtControllersAndModels !? {
					wdgtControllersAndModels.midiOptions.model.value_(
						(
							midiMode: prMidiMode,
							midiMean: prMidiMean,
							ctrlButtonBank: prCtrlButtonBank,
							midiResolution: prMidiResolution,
							softWithin: prSoftWithin
						)
					).changedKeys(synchKeys);
				}
			},
			{
				prCtrlButtonBank.put(thisSlot, numSliders);
				wdgtControllersAndModels[thisSlot] !? {
					wdgtControllersAndModels[thisSlot].midiOptions.model.value_(
						(
							midiMode: prMidiMode[thisSlot],
							midiMean: prMidiMean[thisSlot],
							ctrlButtonBank: prCtrlButtonBank[thisSlot],
							midiResolution: prMidiResolution[thisSlot],
							softWithin: prSoftWithin[thisSlot]
						)
					).changedKeys(synchKeys);
				}
			}
		)
	}

	getCtrlButtonBank { |slot|
		var thisSlot;
		switch(this.class,
			CVWidget2D, { thisSlot = slot.asSymbol },
			CVWidgetMS, { thisSlot = slot.asInt }
		);
		switch(this.class,
			CVWidgetKnob, {
				^prCtrlButtonBank;
			},
			{ ^prCtrlButtonBank[thisSlot] }
		)
	}

	setMidiResolution { |resolution, slot|
		var thisSlot;
		switch(this.class,
			CVWidget2D, { thisSlot = slot.asSymbol },
			CVWidgetMS, { thisSlot = slot.asInt }
		);
		switch(this.class,
			CVWidgetKnob, {
				prMidiResolution = resolution;
				wdgtControllersAndModels !? {
					wdgtControllersAndModels.midiOptions.model.value_(
						(
							midiMode: prMidiMode,
							midiMean: prMidiMean,
							ctrlButtonBank: prCtrlButtonBank,
							midiResolution: prMidiResolution,
							softWithin: prSoftWithin
						)
					).changedKeys(synchKeys);
				}
			},
			{
				prMidiResolution[thisSlot] = resolution;
				wdgtControllersAndModels[thisSlot] !? {
					wdgtControllersAndModels[thisSlot].midiOptions.model.value_(
						(
							midiMode: prMidiMode[thisSlot],
							midiMean: prMidiMean[thisSlot],
							ctrlButtonBank: prCtrlButtonBank[thisSlot],
							midiResolution: prMidiResolution[thisSlot],
							softWithin: prSoftWithin[thisSlot]
						)
					).changedKeys(synchKeys);
				}
			}
		)
	}

	getMidiResolution { |slot|
		var thisSlot;
		switch(this.class,
			CVWidget2D, { thisSlot = slot.asSymbol },
			CVWidgetMS, { thisSlot = slot.asInt }
		);
		switch(this.class,
			CVWidgetKnob, {
				^prMidiResolution;
			},
			{ ^prMidiResolution[thisSlot] }
		)
	}

	setCalibrate { |bool, slot|
		var thisSlot;
		switch(this.class,
			CVWidget2D, { thisSlot = slot.asSymbol },
			CVWidgetMS, { thisSlot = slot.asInt }
		);
		if(bool.isKindOf(Boolean).not, {
			Error("calibration can only be set to true or false!").throw;
		});
		switch(this.class,
			CVWidgetKnob, {
				prCalibrate = bool;
				wdgtControllersAndModels.oscConnection.model.value_(
					wdgtControllersAndModels.oscConnection.model.value
				).changedKeys(synchKeys);
				wdgtControllersAndModels.calibration.model.value_(bool).changedKeys(synchKeys);
			},
			CVWidgetMS, {
				prCalibrate[thisSlot] = bool;
				wdgtControllersAndModels.slots[thisSlot].oscConnection.model.value_(
					wdgtControllersAndModels.slots[thisSlot].oscConnection.model.value
				).changedKeys(synchKeys);
				wdgtControllersAndModels.slots[thisSlot].calibration.model.value_(bool).changedKeys(synchKeys);
			},
			{
				prCalibrate[thisSlot] = bool;
				wdgtControllersAndModels[thisSlot].oscConnection.model.value_(
					wdgtControllersAndModels[thisSlot].oscConnection.model.value
				).changedKeys(synchKeys);
				wdgtControllersAndModels[thisSlot].calibration.model.value_(bool).changedKeys(synchKeys);
			}
		)
	}

	getCalibrate { |slot|
		var thisSlot;
		switch(this.class,
			CVWidget2D, { thisSlot = slot.asSymbol },
			CVWidgetMS, { thisSlot = slot.asInt }
		);
		switch(this.class,
			CVWidgetKnob, {
				^prCalibrate;
			},
			{ ^prCalibrate[thisSlot] }
		)
	}

	setSpec { |spec, slot|
		var thisSpec;
		if(spec.class == String, { thisSpec = spec.asSymbol }, { thisSpec = spec });
		if(thisSpec.asSpec.isKindOf(ControlSpec).not, {
			Error("Please provide a valid spec! (its class must inherit from ControlSpec)").throw;
		});
		thisSpec = thisSpec.asSpec;
		switch(this.class,
			CVWidget2D, {
				wdgtControllersAndModels[slot.asSymbol].cvSpec.model.value_(thisSpec).changedKeys(synchKeys);
			},
			{
				if(this.class == CVWidgetMS, {
					if([thisSpec.minval, thisSpec.maxval, thisSpec.warp, thisSpec.step, thisSpec.default].select(_.isArray).size == 0, {
						thisSpec = ControlSpec(
							thisSpec.minval!this.msSize,
							thisSpec.maxval!this.msSize,
							thisSpec.warp,
							thisSpec.step!this.msSize,
							thisSpec.default!this.msSize,
							thisSpec.units,
						)
					})
				});
				wdgtControllersAndModels.cvSpec.model.value_(thisSpec).changedKeys(synchKeys);
			}
		)
	}

	getSpec { |slot|
		switch(this.class,
			CVWidget2D, {
				^widgetCV[slot.asSymbol].spec;
			},
			{
				^widgetCV.spec;
			}
		)
	}

	setOscMapping { |mapping, slot|
		var thisSlot, wcm;
		switch(this.class,
			CVWidget2D, {
				thisSlot = slot.asSymbol;
				wcm = wdgtControllersAndModels[thisSlot];
			},
			CVWidgetMS, {
				thisSlot = slot.asInt;
				wcm = wdgtControllersAndModels.slots[thisSlot];
			}
		);

		// "mapping: %\n".postf(mapping);

		if(mapping.asSymbol !== \linlin and:{
			mapping.asSymbol !== \linexp and:{
				mapping.asSymbol !== \explin and:{
					mapping.asSymbol !== \expexp and:{
						mapping.asSymbol !== 'set global mapping...'
					}
				}
			}
		}, {
			Error("A valid mapping can either be \\linlin, \\linexp, \\explin or \\expexp").throw;
		});

		// if(mapping.asSymbol !== 'set global mapping...', {
			switch(this.class,
				CVWidgetKnob, {
					midiOscEnv.oscMapping = mapping.asSymbol;
					wdgtControllersAndModels.oscInputRange.model.value_(
						wdgtControllersAndModels.oscInputRange.model.value;
					).changedKeys(synchKeys);
					wdgtControllersAndModels.cvSpec.model.value_(
						wdgtControllersAndModels.cvSpec.model.value;
					).changedKeys(synchKeys);
				},
				{
					midiOscEnv[thisSlot].oscMapping = mapping.asSymbol;
					wcm.oscInputRange.model.value_(
						wcm.oscInputRange.model.value;
					).changedKeys(synchKeys);
				// "inputRange model updated".postln;
					switch(this.class,
						CVWidget2D, {
							wdgtControllersAndModels[thisSlot].cvSpec.model.value_(
								wdgtControllersAndModels[thisSlot].cvSpec.model.value;
							).changedKeys(synchKeys);
						},
						CVWidgetMS, {
							wdgtControllersAndModels.cvSpec.model.value_(
								wdgtControllersAndModels.cvSpec.model.value;
							).changedKeys(synchKeys);
						// "spec model updated".postln;
						}
					)
				}
			)
		// })
	}

	getOscMapping { |slot|
		var thisSlot;
		switch(this.class,
			CVWidget2D, { thisSlot = slot.asSymbol },
			CVWidgetMS, { thisSlot = slot.asInt }
		);
		switch(this.class,
			CVWidgetKnob, {
				^midiOscEnv.oscMapping;
			},
			{
				^midiOscEnv[thisSlot].oscMapping
			}
		)
	}

	oscConnect { |ip, port, name, oscMsgIndex=1, slot|
		var thisSlot;
		var thisIP, intPort;

		switch(this.class,
			CVWidget2D, { thisSlot = slot.asSymbol },
			CVWidgetMS, { thisSlot = slot.asInt }
		);

		if(ip.size > 0 and:{
			"^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$".matchRegexp(ip).not and:{
				ip != "nil"
			}
		}, {
			Error("Please provide a valid IP-address or leave the IP-field empty").throw;
		});

		intPort = port.asString;

		if(intPort.size > 0, {
			if("^[0-9]{1,5}$".matchRegexp(intPort).not and:{ intPort != "nil" }, {
				Error("Please provide a valid port or leave this field empty").throw;
			}, {
				intPort = intPort.asInt;
			})
		});

		if(ip == "nil", { thisIP = "" }, { thisIP = ip });
		if(port == "nil", { intPort = nil });

		if("^\/".matchRegexp(name.asString).not, {
			Error("You have to supply a valid OSC-typetag (command-name), beginning with an \"/\" as third argument to oscConnect").throw;
		});

		if(oscMsgIndex.isKindOf(Integer).not, {
			Error("You have to supply an integer as forth argument to oscConnect").throw;
		});

		switch(this.class,
			CVWidgetKnob, {
				wdgtControllersAndModels.oscConnection.model.value_([thisIP, intPort, name.asSymbol, oscMsgIndex]).changedKeys(synchKeys);
				CmdPeriod.add({ if(this.class.removeResponders, { this.oscDisconnect }) });
			},
			CVWidget2D, {
				wdgtControllersAndModels[thisSlot].oscConnection.model.value_([thisIP, intPort, name.asSymbol, oscMsgIndex]).changedKeys(synchKeys);
				CmdPeriod.add({ if(this.class.removeResponders, { this.oscDisconnect(thisSlot) }) });
			},
			CVWidgetMS, {
				wdgtControllersAndModels.slots[thisSlot].oscConnection.model.value_([thisIP, intPort, name.asSymbol, oscMsgIndex]).changedKeys(synchKeys);
				CmdPeriod.add({ if(this.class.removeResponders, { this.oscDisconnect(thisSlot) }) });
			}
		)
	}

	oscDisconnect { |slot|
		var thisSlot;
		switch(this.class,
			CVWidget2D, { thisSlot = slot.asSymbol },
			CVWidgetMS, { thisSlot = slot.asInt }
		);
		switch(this.class,
			CVWidgetKnob, {
				wdgtControllersAndModels.oscConnection.model.value_(false).changedKeys(synchKeys);
				wdgtControllersAndModels.oscInputRange.model.value_([0.00001, 0.00001]).changedKeys(synchKeys);
				CmdPeriod.remove({ this.oscDisconnect });
			},
			CVWidget2D, {
				wdgtControllersAndModels[thisSlot].oscConnection.model.value_(false).changedKeys(synchKeys);
				wdgtControllersAndModels[thisSlot].oscInputRange.model.value_([0.00001, 0.00001]).changedKeys(synchKeys);
				CmdPeriod.remove({ this.oscDisconnect(slot) });
			},
			CVWidgetMS, {
				wdgtControllersAndModels.slots[thisSlot].oscConnection.model.value_(false).changedKeys(synchKeys);
				wdgtControllersAndModels.slots[thisSlot].oscInputRange.model.value_([0.00001, 0.00001]).changedKeys(synchKeys);
				CmdPeriod.remove({ this.oscDisconnect(slot) });
			}
		)
	}

	// if all arguments besides 'slot' are nil .learn should be triggered
	midiConnect { |uid, chan, num, slot|
		var thisSlot;
		switch(this.class,
			CVWidget2D, { thisSlot = slot.asSymbol },
			CVWidgetMS, { thisSlot = slot.asInt }
		);
		switch(this.class,
			CVWidgetKnob, {
				if(midiOscEnv.cc.isNil, {
					wdgtControllersAndModels.midiConnection.model.value_(
						(src: uid, chan: chan, num: num)
					).changedKeys(synchKeys);
					CmdPeriod.add({ this !? { this.midiDisconnect } });
				}, {
					"Already connected!".warn;
				})
			},
			{
				slot ?? {
					Error("Missing 'slot'-argument. Maybe you forgot to explicitely provide the slot: e.g. <wdgt>.midiConnect(slot: \lo)").throw;
				};
				if(midiOscEnv[slot].cc.isNil, {
					wdgtControllersAndModels[thisSlot].midiConnection.model.value_(
						(src: uid, chan: chan, num: num)
					).changedKeys(synchKeys);
					CmdPeriod.add({ this !? { this.midiDisconnect(slot) } });
				}, {
					"Already connected!".warn;
				})
			}
		)
	}

	midiDisconnect { |slot|
		var thisSlot;
		switch(this.class,
			CVWidget2D, { thisSlot = slot.asSymbol },
			CVWidgetMS, { thisSlot = slot.asInt }
		);
		switch(this.class,
			CVWidgetKnob, {
				wdgtControllersAndModels.midiConnection.model.value_(nil).changedKeys(synchKeys);
				CmdPeriod.remove({ this.midiDisconnect });
			},
			{
				wdgtControllersAndModels[thisSlot].midiConnection.model.value_(nil).changedKeys(synchKeys);
				CmdPeriod.remove({ this.midiDisconnect(slot) });
			}
		)
	}

	setOscInputConstraints { |constraintsHiLo, slot|
		var thisSlot;
		switch(this.class,
			CVWidget2D, { thisSlot = slot.asSymbol },
			CVWidgetMS, { thisSlot = slot.asInt }
		);
		if(constraintsHiLo.isKindOf(Point).not, {
			Error("setOSCInputConstraints expects a Point in the form of lo@hi").throw;
		}, {
			this.setCalibrate(false, slot);
			switch(this.class,
				CVWidgetKnob, {
					midiOscEnv.calibConstraints = (lo: constraintsHiLo.x, hi: constraintsHiLo.y);
					if(editor.notNil and:{ editor.isClosed.not }, {
						wdgtControllersAndModels.mapConstrainterLo.value_(constraintsHiLo.x);
						wdgtControllersAndModels.mapConstrainterHi.value_(constraintsHiLo.y);
					})
				},
				{
					midiOscEnv[thisSlot].calibConstraints = (lo: constraintsHiLo.x, hi: constraintsHiLo.y);
					if(editor[thisSlot].notNil and:{ editor[thisSlot].isClosed.not }, {
						wdgtControllersAndModels[thisSlot].mapConstrainterLo.value_(constraintsHiLo.x);
						wdgtControllersAndModels[thisSlot].mapConstrainterHi.value_(constraintsHiLo.y);
					})
				}
			)
		})
	}

	getOscInputConstraints { |slot|
		var thisSlot;
		switch(this.class,
			CVWidget2D, { thisSlot = slot.asSymbol },
			CVWidgetMS, { thisSlot = slot.asInt }
		);
		switch(this.class,
			CVWidgetKnob, {
				^midiOscEnv.calibConstraints;
			},
			{
				^midiOscEnv[thisSlot].calibConstraints;
			}
		)
	}

	front {
		this.window.front;
	}

	isClosed {
		if(isCVCWidget, {
			// we're within a CVCenter-gui or some other gui
			// -> a widget 'is closed' if its elements have been removed
			if(allGuiEls.select({ |el| el.isClosed.not }).size == 0, { ^true }, { ^false });
		}, {
			// we just want to check for a single widget resp. its parent window
			^window.isClosed;
		})
	}

	// controllers, controllers, controllers...

	initControllersAndModels { |controllersAndModels, slot|
		var wcm, tmp;

		if(controllersAndModels.notNil, {
			wdgtControllersAndModels = controllersAndModels;
		}, {
			wdgtControllersAndModels ?? {
				switch(this.class,
					CVWidgetMS, { wdgtControllersAndModels = (slots: Array.newClear(this.msSize)) },
					{ wdgtControllersAndModels = () }
				)
			}
		});

		slot !? {
			if(wdgtControllersAndModels[slot].isNil, {
				switch(this.class,
					CVWidget2D, { wdgtControllersAndModels.put(slot, ()) },
					CVWidgetMS, { wdgtControllersAndModels.slots[slot] = () }
				)
			})
		};

		if(slot.notNil, {
			switch(this.class,
				CVWidget2D, { wcm = wdgtControllersAndModels[slot] },
				CVWidgetMS, { wcm = wdgtControllersAndModels.slots[slot] }
			)
		}, {
			wcm = wdgtControllersAndModels;
		});

//		"wcm: %\n".postf(wcm);

		wcm.calibration ?? {
			wcm.calibration = ();
		};
		wcm.calibration.model ?? {
			if(slot.notNil, {
				wcm.calibration.model = Ref(prCalibrate[slot]);
			}, {
				wcm.calibration.model = Ref(prCalibrate);
			})
		};
		wcm.cvSpec ?? {
			switch(this.class,
				CVWidgetMS, {
					wdgtControllersAndModels.cvSpec ?? {
						wdgtControllersAndModels.cvSpec = ();
					}
				},
				{ wcm.cvSpec = () }
			)
		};
		switch(this.class,
			CVWidgetMS, {
				wdgtControllersAndModels.cvSpec.model ?? {
					wdgtControllersAndModels.cvSpec.model = Ref(this.getSpec);
				}
			},
			{ wcm.cvSpec.model ?? {
				wcm.cvSpec.model = Ref(this.getSpec(slot));
			}}
		);
		wcm.oscInputRange ?? {
			wcm.oscInputRange = ();
		};
		wcm.oscInputRange.model ?? {
			wcm.oscInputRange.model = Ref([0.0001, 0.0001]);
		};
		wcm.oscConnection ?? {
			wcm.oscConnection = ();
		};
		wcm.oscConnection.model ?? {
			wcm.oscConnection.model = Ref(false);
		};
		wcm.oscDisplay ?? {
			wcm.oscDisplay = ();
		};
		wcm.oscDisplay.model ?? {
			if(this.class == CVWidgetMS, { tmp = slot.asString++": edit OSC" }, { tmp = "edit OSC" });
			wcm.oscDisplay.model = Ref((
				but: [tmp, Color.black, this.bgColor],
				ipField: "",
				portField: "",
				nameField: "/my/cmd/name",
				index: 1,
				connectorButVal: 0,
				editEnabled: true
			))
		};
		wcm.midiConnection ?? {
			wcm.midiConnection = ();
		};
		wcm.midiConnection.model ?? {
			wcm.midiConnection.model = Ref(nil);
		};
		wcm.midiDisplay ?? {
			wcm.midiDisplay = ();
		};
		wcm.midiDisplay.model ?? {
			wcm.midiDisplay.model = Ref((src: "source", chan: "chan", ctrl: "ctrl", learn: "L"));
		};
		wcm.midiOptions ?? {
			wcm.midiOptions = ();
		};
		wcm.midiOptions.model ?? {
			switch(this.class,
				CVWidgetMS, {
					wcm.midiOptions.model = Ref(
						(
							midiMode: prMidiMode[slot],
							midiMean: prMidiMean[slot],
							ctrlButtonBank: prCtrlButtonBank[slot],
							midiResolution: prMidiResolution[slot],
							softWithin: prSoftWithin[slot]
						)
					)
				},
				{ wcm.midiOptions.model = Ref(
					(
						midiMode: prMidiMode,
						midiMean: prMidiMean,
						ctrlButtonBank: prCtrlButtonBank,
						midiResolution: prMidiResolution,
						softWithin: prSoftWithin
					)
				)}
			)
		};
		wcm.mapConstrainterLo ?? {
			wcm.mapConstrainterLo = CV([-inf, inf].asSpec, wcm.oscInputRange.model.value[0]);
		};
		wcm.mapConstrainterHi ?? {
			wcm.mapConstrainterHi = CV([-inf, inf].asSpec, wcm.oscInputRange.model.value[1]);
		};
		switch(this.class,
			CVWidgetMS, {
				wdgtControllersAndModels.actions ?? {
					wdgtControllersAndModels.actions = ()
				}
			},
			{ wcm.actions ?? {
				wcm.actions = ();
			}}
		);
		switch(this.class,
			CVWidgetMS, {
				wdgtControllersAndModels.actions.model ?? {
					wdgtControllersAndModels.actions.model = Ref((numActions: 0, activeActions: 0))
				}
			},
			{ wcm.actions.model ?? {
				wcm.actions.model = Ref((numActions: 0, activeActions: 0))
			}}
		)

	}

	initControllerActions { |slot|
		var wcm, thisGuiEnv, midiOscEnv, tmpSetup, thisWidgetCV;
		var thisCalib;

//		(
//			slot: slot,
//			wdgtControllersAndModels: wdgtControllersAndModels[slot],
//			midiOscEnv: this.midiOscEnv[slot],
//			widgetCV: this.widgetCV,
//			guiEnv: this.guiEnv,
//			prCalibrate: prCalibrate[slot]
//		).pairsDo({ |k, v| [k, v].postcs });

		if(slot.notNil, {
			switch(this.class,
				CVWidget2D, { wcm = wdgtControllersAndModels[slot] },
				CVWidgetMS, {
					wcm = wdgtControllersAndModels.slots[slot];
					wcm.cvSpec = wdgtControllersAndModels.cvSpec;
					wcm.actions = wdgtControllersAndModels.actions;
				};
			);
			midiOscEnv = this.midiOscEnv[slot];
			switch(this.class,
				CVWidget2D, {
					thisWidgetCV = this.widgetCV[slot];
					thisGuiEnv = this.guiEnv[slot];
				},
				CVWidgetMS, {
					thisWidgetCV = this.widgetCV;
					thisGuiEnv = this.guiEnv;
				}
			);
			thisCalib = prCalibrate[slot];
		}, {
			wcm = wdgtControllersAndModels;
			thisGuiEnv = this.guiEnv;
			midiOscEnv = this.midiOscEnv;
			thisWidgetCV = this.widgetCV;
			thisCalib = prCalibrate;
		});

		#[
			prInitCalibration,
			prInitSpecControl,
			prInitMidiConnect,
			prInitMidiDisplay,
			prInitMidiOptions,
			prInitOscConnect,
			prInitOscDisplay,
			prInitOscInputRange,
			prInitActionsControl
		].do({ |method| this.perform(method, wcm, thisGuiEnv, midiOscEnv, thisWidgetCV, thisCalib, slot) });
	}

	prInitCalibration { |wcm, thisGuiEnv, midiOscEnv, argWidgetCV, thisCalib, slot|
		var thisEditor, numCalib;

		wcm.calibration.controller ?? {
			wcm.calibration.controller = SimpleController(wcm.calibration.model);
		};

		wcm.calibration.controller.put(\default, { |theChanger, what, moreArgs|
			// "prInitCalibration: %\n".postf(theChanger.value);
			if(this.class == CVWidgetMS, {
				thisEditor = thisGuiEnv[\editor][slot];
			}, {
				thisEditor = thisGuiEnv[\editor];
			});

			// [slot, thisEditor.calibNumBoxes.lo, thisEditor.calibNumBoxes.hi].postln;

			theChanger.value.switch(
				true, {
					if(this.class != CVWidgetMS, {
						window.isClosed.not.if { thisGuiEnv.calibBut.value_(0) };
					});
					if(thisEditor.notNil and:{ thisEditor.isClosed.not }, {
						thisEditor.calibBut.value_(0);
						wcm.mapConstrainterLo ?? {
							wcm.mapConstrainterLo = CV([-inf, inf].asSpec, 0.00001);
							wcm.mapConstrainterLo.connect(thisEditor.calibNumBoxes.lo);
						};
						wcm.mapConstrainterHi ?? {
							wcm.mapConstrainterHi = CV([-inf, inf].asSpec, 0.00001);
							wcm.mapConstrainterHi.connect(thisEditor.calibNumBoxes.hi);
						};
						[thisEditor.calibNumBoxes.lo, thisEditor.calibNumBoxes.hi].do({ |nb|
							nb.enabled_(false);
							nb.action_(nil);
						})
					});
					if(this.class == CVWidgetMS, {
						numCalib = this.msSize.collect(this.getCalibrate(_)).select(_ == true).size;
						// "numCalib on true: %\n".postf(numCalib);
						if(numCalib == this.msSize, {
							if(thisGuiEnv.msEditor.notNil and:{
								thisGuiEnv.msEditor.isClosed.not
							}, {
								thisGuiEnv.msEditor.calibBut.states_([
									["calibrating all", Color.white, Color.red],
									["calibrate all", Color.black, Color.green]
								]).value_(0);
								thisGuiEnv.msEditor.oscCalibBtns[slot].value_(0);
							})
						}, {
							if(thisGuiEnv.msEditor.notNil and:{
								thisGuiEnv.msEditor.isClosed.not
							}, {
								thisGuiEnv.msEditor.calibBut.states_([
									["partially calibrating", Color.white, Color.red],
									["calibrate all", Color.black, Color.green]
								]).value_(0);
								thisGuiEnv.msEditor.oscCalibBtns[slot].value_(0);
							})
						})
					})
				},
				false, {
					if(this.class != CVWidgetMS, {
						window.isClosed.not.if { thisGuiEnv.calibBut.value_(1) };
					});
					if(thisEditor.notNil and:{ thisEditor.isClosed.not }, {
						thisEditor.calibBut.value_(1);
						[wcm.mapConstrainterLo, wcm.mapConstrainterHi].do({ |cv| cv = nil; });
						[thisEditor.calibNumBoxes.lo, thisEditor.calibNumBoxes.hi].do({ |nb|
							nb.enabled_(true);
							nb.action_({ |b|
								this.setOscInputConstraints(
									thisEditor.calibNumBoxes.lo.value @ thisEditor.calibNumBoxes.hi.value, slot;
								)
							})
						})
					});
					if(this.class == CVWidgetMS, {
						numCalib = this.msSize.collect(this.getCalibrate(_)).select(_ == true).size;
						if(numCalib == 0, {
							if(thisGuiEnv.msEditor.notNil and:{
								thisGuiEnv.msEditor.isClosed.not
							}, {
								thisGuiEnv.msEditor.calibBut.states_([
									["calibrating all", Color.white, Color.red],
									["calibrate all", Color.black, Color.green]
								]).value_(1);
								thisGuiEnv.msEditor.oscCalibBtns[slot].value_(1);
							})
						}, {
							if(thisGuiEnv.msEditor.notNil and:{
								thisGuiEnv.msEditor.isClosed.not
							}, {
								thisGuiEnv.msEditor.calibBut.states_([
									["partially calibrating", Color.white, Color.red],
									["calibrate all", Color.black, Color.green]
								]).value_(0);
								thisGuiEnv.msEditor.oscCalibBtns[slot].value_(1);
							})
						})
					})
				}
			)
		})
	}

	prInitSpecControl { |wcm, thisGuiEnv, midiOscEnv, argWidgetCV, thisCalib, slot|
		var tmp, tmpMapping;
		var specEditor, msEditors;
		var thisSpec, customName;

		wcm.cvSpec.controller ?? {
			wcm.cvSpec.controller = SimpleController(wcm.cvSpec.model);
		};

		wcm.cvSpec.controller.put(\default, { |theChanger, what, moreArgs|
			// "prInitSpecControl: %\n".postf(theChanger.value);

			switch(this.class,
				CVWidgetMS, {
					specEditor = thisGuiEnv.msEditor;
					msEditors = thisGuiEnv.editor;
				},
				{ specEditor = thisGuiEnv.editor }
			);

			if(theChanger.value.hasZeroCrossing, {
				if(midiOscEnv.oscMapping === \linexp or:{
					midiOscEnv.oscMapping === \expexp
				}, {
					if(this.class == CVWidgetMS, {
						this.msSize.do({ |sl| this.midiOscEnv[sl].oscMapping = \linlin });
					}, {
						midiOscEnv.oscMapping = \linlin;
					});
					if(specEditor.notNil and:{
						specEditor.isClosed.not
					}, {
						if(this.class == CVWidgetMS, {
							specEditor.mappingSelect.value_(1);
							msEditors.do({ |it|
								if(it.notNil and:{ it.isClosed.not }, {
									it.mappingSelect.value_(0)
								})
							})
						}, {
							specEditor.mappingSelect.value_(0);
						})
					})
				})
			}, {
				if(specEditor.notNil and:{
					specEditor.isClosed.not
				}, {
					tmpMapping = specEditor.mappingSelect.item;
					specEditor.mappingSelect.items.do({ |item, i|
						if(item == tmpMapping, {
							specEditor.mappingSelect.value_(i)
						})
					});
				})
			});

			if(specEditor.notNil and:{
				specEditor.isClosed.not
			}, {
				if(this.class == CVWidgetMS, {
					if([
						theChanger.value.minval,
						theChanger.value.maxval,
						theChanger.value.warp,
						theChanger.value.step,
						theChanger.value.default
					].select(_.isArray).size == 0, {
						thisSpec = ControlSpec(
							theChanger.value.minval!this.msSize,
							theChanger.value.maxval!this.msSize,
							theChanger.value.warp,
							theChanger.value.step!this.msSize,
							theChanger.value.default!this.msSize,
							theChanger.value.units,
						)
					}, {
						thisSpec = theChanger.value;
					});

					tmp = [
						thisSpec.minval.size,
						thisSpec.maxval.size,
						thisSpec.step.size,
						thisSpec.default.size
					].maxItem;

					if(tmp < this.msSize, { this.mSlider.indexThumbSize_(this.mSlider.bounds.width/tmp) });

					if(Spec.findKeyForSpec(theChanger.value).notNil, {
						customName = Spec.findKeyForSpec(theChanger.value).asString++tmp;
					}, {
						customName = "custom"++tmp;
					});
				}, {
					thisSpec = theChanger.value;
				});

				specEditor.specField.string_(thisSpec.asCompileString);
				tmp = specEditor.specsListSpecs.detectIndex({ |item, i| item == thisSpec });
				if(tmp.notNil, {
					specEditor.specsList.value_(tmp);
				}, {
					customName ?? { customName = "custom" };
					specEditor.specsList.items = List[customName++":"+(thisSpec.asString)]++specEditor.specsList.items;
					Spec.add(customName, thisSpec);
					specEditor.specsListSpecs.array_([thisSpec]++specEditor.specsListSpecs.array);
					specEditor.specsList.value_(0);
					specEditor.specsList.refresh;
				})
			});

			argWidgetCV.spec_(thisSpec);

			if(this.specBut.class == Event, {
				this.specBut[slot].toolTip_(
					"Edit the CV's ControlSpec in '"++slot++"':\n"++(this.getSpec(slot).asCompileString)
				)
			}, {
				this.specBut.toolTip_(
					"Edit the CV's ControlSpec:\n"++(this.getSpec.asCompileString)
				)
			});

			if(this.class === CVWidgetKnob, {
				if(argWidgetCV.spec.excludingZeroCrossing, {
					thisGuiEnv.knob.centered_(true);
				}, {
					thisGuiEnv.knob.centered_(false);
				})
			});

			msEditors !? {
				msEditors.do({ |ed|
					if(ed.notNil and:{ ed.isClosed.not }, {
						ed.specConstraintsText.string_(
							" current widget-spec constraints lo / hi:"+this.getSpec.minval.wrapAt(slot)+"/"+this.getSpec.maxval.wrapAt(slot)
						)
					})
				})
			};
			if(this.class != CVWidgetMS, {
				if(specEditor.notNil and:{ specEditor.isClosed.not }, {
					specEditor.specConstraintsText.string_(
						" current widget-spec constraints lo / hi:"+this.getSpec(slot).minval+"/"+this.getSpec(slot).maxval
					)
				})
			})
		})
	}

	prInitMidiConnect { |wcm, thisGuiEnv, midiOscEnv, argWidgetCV, thisCalib, slot|
		var ctrlString, meanVal, ccResponderAction, makeCCResponder;

		wcm.midiConnection.controller ?? {
			wcm.midiConnection.controller = SimpleController(wcm.midiConnection.model);
		};

		wcm.midiConnection.controller.put(\default, { |theChanger, what, moreArgs|
			// "prInitMidiConnect: %\n".postf(theChanger.value);

			if(theChanger.value.isKindOf(Event), {
				ccResponderAction = { |src, chan, num, val|
					ctrlString ? ctrlString = num+1;
					if(this.getCtrlButtonBank(slot).notNil, {
						if(ctrlString % this.getCtrlButtonBank(slot) == 0, {
							ctrlString = this.getCtrlButtonBank(slot).asString;
						}, {
							ctrlString = (ctrlString % this.getCtrlButtonBank(slot)).asString;
						});
						ctrlString = ((num+1/this.getCtrlButtonBank(slot)).ceil).asString++":"++ctrlString;
					}, {
						ctrlString = num+1;
					});

					this.getMidiMode(slot).switch(
						0, {
							if(val/127 < (argWidgetCV.input+(this.getSoftWithin(slot)/2)) and: {
								val/127 > (argWidgetCV.input-(this.getSoftWithin(slot)/2));
							}, {
								argWidgetCV.input_(val/127);
							})
						},
						1, {
							meanVal = this.getMidiMean(slot);
							argWidgetCV.input_(argWidgetCV.input+((val-meanVal)/127*this.getMidiResolution(slot)))
						}
					);
					src !? { midiOscEnv.midisrc = src };
					chan !? { midiOscEnv.midichan = chan };
					num !? { midiOscEnv.midinum = ctrlString; midiOscEnv.midiRawNum = num };
				};
				makeCCResponder = { |argSrc, argChan, argNum|
					if(midiOscEnv.cc.isNil, {
						CCResponder(ccResponderAction, argSrc, argChan, argNum, nil);
					}, {
						midiOscEnv.cc.function_(ccResponderAction);
					})
				};

				{
					block { |break|
						loop {
							0.01.wait;
							if(midiOscEnv.midisrc.notNil and:{
								midiOscEnv.midichan.notNil and:{
									midiOscEnv.midinum.notNil;
								}
							}, {
								break.value(
									wcm.midiDisplay.model.value_(
										(
											src: midiOscEnv.midisrc,
											chan: midiOscEnv.midichan,
											ctrl: midiOscEnv.midinum,
											learn: "X"
										)
									).changedKeys(synchKeys)
								)
							})
						}
					}
				}.fork(AppClock);

				if(theChanger.value.isEmpty, {
					midiOscEnv.cc = makeCCResponder.().learn;
				}, {
					midiOscEnv.cc = makeCCResponder.(theChanger.value.src, theChanger.value.chan, theChanger.value.num);
				});
			}, {
				midiOscEnv.cc.remove;
				midiOscEnv.cc = nil;
				wcm.midiDisplay.model.value_(
					(src: "source", chan: "chan", ctrl: "ctrl", learn: "L")
				).changedKeys(synchKeys);
				midiOscEnv.midisrc = nil; midiOscEnv.midichan = nil; midiOscEnv.midinum = nil; midiOscEnv.midiRawNum = nil;
			})
		})
	}

	prInitMidiDisplay { |wcm, thisGuiEnv, midiOscEnv, argWidgetCV, thisCalib, slot|
		var ctrlToolTip,typeText, r, p;

		wcm.midiDisplay.controller ?? {
			wcm.midiDisplay.controller = SimpleController(wcm.midiDisplay.model);
		};

		wcm.midiDisplay.controller.put(\default, { |theChanger, what, moreArgs|
			// "prInitMidiDisplay: %\n".postf(theChanger.value);

			if(this.class != CVWidgetMS, {
				theChanger.value.learn.switch(
					"X", {
						defer {
							if(window.isClosed.not, {
								thisGuiEnv.midiSrc.string_(theChanger.value.src.asString)
									.background_(Color.red)
									.stringColor_(Color.white)
									.canFocus_(false)
								;
								thisGuiEnv.midiChan.string_((theChanger.value.chan+1).asString)
									.background_(Color.red)
									.stringColor_(Color.white)
									.canFocus_(false)
								;
								thisGuiEnv.midiCtrl.string_(theChanger.value.ctrl)
									.background_(Color.red)
									.stringColor_(Color.white)
									.canFocus_(false)
								;
								if(slot.notNil, { typeText = " at '"++slot++"'" }, { typeText = "" });
								thisGuiEnv.midiLearn.value_(1);
								if(GUI.id !== \cocoa, {
									thisGuiEnv.midiLearn.toolTip_("Click to remove the current\nMIDI-responder in this widget %.".format(typeText));
									[thisGuiEnv.midiSrc, thisGuiEnv.midiChan, thisGuiEnv.midiCtrl].do({ |elem|
										if(theChanger.value.ctrl.class == String and:{ theChanger.value.ctrl.includes($:) }, {
											ctrlToolTip = theChanger.value.ctrl.split($:);
											ctrlToolTip = ctrlToolTip[1]++" in bank "++ctrlToolTip[0];
										}, { ctrlToolTip = theChanger.value.ctrl });
										elem.toolTip_(
											"currently connected to\ndevice-ID %,\non channel %,\ncontroller %".format(theChanger.value.src.asString, (theChanger.value.chan+1).asString, ctrlToolTip)
										)
									})
								})
							});

							if(thisGuiEnv.editor.notNil and:{
								thisGuiEnv.editor.isClosed.not
							}, {
								thisGuiEnv.editor.midiSrcField.string_(theChanger.value.src.asString)
									.background_(Color.red)
									.stringColor_(Color.white)
									.canFocus_(false)
								;
								thisGuiEnv.editor.midiChanField.string_((theChanger.value.chan+1).asString)
									.background_(Color.red)
									.stringColor_(Color.white)
									.canFocus_(false)
								;
								thisGuiEnv.editor.midiCtrlField.string_(theChanger.value.ctrl)
									.background_(Color.red)
									.stringColor_(Color.white)
									.canFocus_(false)
								;
								thisGuiEnv.editor.midiLearnBut.value_(1)
							})
						}
					},
					"C", {
						if(window.isClosed.not, {
							thisGuiEnv.midiLearn.states_([
								["C", Color.white, Color(0.11, 0.38, 0.2)],
								["X", Color.white, Color.red]
							]).refresh;
							if(slot.notNil, { typeText = " at '"++slot++"' " }, { typeText = " " });
							thisGuiEnv.midiLearn.value_(0);
							thisGuiEnv.midiLearn.toolTip_("Click to connect the widget% to\nthe slider(s) as given in the fields below.".format(typeText));
							r = [
								thisGuiEnv.midiSrc.string != "source" and:{
									try{ thisGuiEnv.midiSrc.string.interpret.isInteger }
								},
								thisGuiEnv.midiChan.string != "chan" and:{
									try{ thisGuiEnv.midiChan.string.interpret.isInteger }
								},
								thisGuiEnv.midiCtrl.string != "ctrl"
							].collect({ |r| r });

							if(GUI.id !== \cocoa, {
								p = "Use ";
								if(r[0], { p = p++" MIDI-device ID "++theChanger.value.src++",\n" });
								if(r[1], { p = p++"channel nr. "++theChanger.value.chan++",\n" });
								if(r[2], { p = p++"controller nr. "++theChanger.value.ctrl });
								p = p++"\nto connect widget%to MIDI";

								[thisGuiEnv.midiSrc, thisGuiEnv.midiChan, thisGuiEnv.midiCtrl].do(
									_.toolTip_(p.format(slot !? { " at '"++slot++"' " } ?? { " " }))
								)
							})
						});
						if(thisGuiEnv.editor.notNil and:{
							thisGuiEnv.editor.isClosed.not
						}, {
							thisGuiEnv.editor.midiLearnBut
								.states_([
									["C", Color.white, Color(0.11, 0.38, 0.2)],
									["X", Color.white, Color.red]
								])
								.value_(0)
							;
						})
					},
					"L", {
						defer {
							if(window.isClosed.not, {
								thisGuiEnv.midiSrc.string_(theChanger.value.src)
									.background_(Color.white)
									.stringColor_(Color.black)
									.canFocus_(true)
								;
								thisGuiEnv.midiChan.string_(theChanger.value.chan)
									.background_(Color.white)
									.stringColor_(Color.black)
									.canFocus_(true)
								;
								thisGuiEnv.midiCtrl.string_(theChanger.value.ctrl)
									.background_(Color.white)
									.stringColor_(Color.black)
									.canFocus_(true)
								;
								thisGuiEnv.midiLearn.states_([
									["L", Color.white, Color.blue],
									["X", Color.white, Color.red]
								])
								.value_(0).refresh;
								if(GUI.id !== \cocoa, {
									if(slot.notNil, { typeText = " at '"++slot++"' " }, { typeText = " " });
									thisGuiEnv.midiLearn.toolTip_("Click and and move an arbitrary\nslider on your MIDI-device to\nconnect the widget%to that slider.".format(typeText));
									thisGuiEnv.midiSrc.toolTip_("Enter your MIDI-device's ID,\nhit 'return' and click 'C' to\nconnect all sliders of your\ndevice to this widget%".format(typeText));
									thisGuiEnv.midiChan.toolTip_("Enter a MIDI-channel, hit 'return'\nand click 'C' to connect all sliders\nin that channel to this widget%".format(typeText));
									thisGuiEnv.midiCtrl.toolTip_("Enter a MIDI-ctrl-nr., hit 'return'\nand click 'C' to connect the slider\nwith that number to this widget%".format(typeText));
								})
							});

							if(thisGuiEnv.editor.notNil and:{
								thisGuiEnv.editor.isClosed.not
							}, {
								thisGuiEnv.editor.midiSrcField.string_(theChanger.value.src)
									.background_(Color.white)
									.stringColor_(Color.black)
									.canFocus_(true)
								;
								thisGuiEnv.editor.midiChanField.string_(theChanger.value.chan)
									.background_(Color.white)
									.stringColor_(Color.black)
									.canFocus_(true)
								;
								thisGuiEnv.editor.midiCtrlField.string_(theChanger.value.ctrl)
									.background_(Color.white)
									.stringColor_(Color.black)
									.canFocus_(true)
								;
								thisGuiEnv.editor.midiLearnBut.states_([
									["L", Color.white, Color.blue],
									["X", Color.white, Color.red]
								])
								.value_(0);
							})
						}
					}
				)
			})
		})
	}

	prInitMidiOptions { |wcm, thisGuiEnv, midiOscEnv, argWidgetCV, thisCalib, slot|
		var typeText;

		wcm.midiOptions.controller ?? {
			wcm.midiOptions.controller = SimpleController(wcm.midiOptions.model);
		};

		wcm.midiOptions.controller.put(\default, { |theChanger, what, moreArgs|
			// "prInitMidiOptions: %\n".postf(theChanger.value);
			if(thisGuiEnv.editor.notNil and:{
				thisGuiEnv.editor.isClosed.not
			}, {
				thisGuiEnv.editor.midiModeSelect.value_(theChanger.value.midiMode);
				thisGuiEnv.editor.midiMeanNB.value_(theChanger.value.midiMean);
				thisGuiEnv.editor.softWithinNB.value_(theChanger.value.softWithin);
				thisGuiEnv.editor.midiResolutionNB.value_(theChanger.value.midiResolution);
				thisGuiEnv.editor.ctrlButtonBankField.string_(theChanger.value.ctrlButtonBank);
			});

			// thisGuiEnv.postln;
			if(window.notNil and:{ window.isClosed.not }, {
				if(slot.notNil, { typeText = "'s '"++slot++"' slot" }, { typeText = "" });
				thisGuiEnv.midiHead.toolTip_(("Edit all MIDI-options\nof this widget%.\nmidiMode:"+theChanger.value.midiMode++"\nmidiMean:"+theChanger.value.midiMean++"\nmidiResolution:"+theChanger.value.midiResolution++"\nsoftWithin:"+theChanger.value.softWithin++"\nctrlButtonBank:"+theChanger.value.ctrlButtonBank).format(typeText));
			})
		})
	}

	prInitOscConnect { |wcm, thisGuiEnv, midiOscEnv, argWidgetCV, thisCalib, slot|
		var oscResponderAction, tmp;
		var intSlots;

		wcm.oscConnection.controller ?? {
			wcm.oscConnection.controller = SimpleController(wcm.oscConnection.model);
		};

		wcm.oscConnection.controller.put(\default, { |theChanger, what, moreArgs|
			// "prInitOscConnect: %\n".postf(theChanger.value);

			// "oscConnect: %\n".postf([theChanger, what, moreArgs]);

			switch(prCalibrate.class,
				Event, { thisCalib = prCalibrate[slot] },
				Array, { thisCalib = prCalibrate[slot] },
				{ thisCalib = prCalibrate }
			);

			if(theChanger.value.size == 4, {
// 				OSCresponderNode: t, r, msg
// 				OSCfunc: msg, time, addr // for the future
				oscResponderAction = { |t, r, msg|
//					"msg[theChanger[3]]: %\n".postf(msg[theChanger.value[3]]);
					if(thisCalib, {
						if(midiOscEnv.calibConstraints.isNil, {
							midiOscEnv.calibConstraints = (lo: msg[theChanger.value[3]], hi: msg[theChanger.value[3]]);
						}, {
							if(msg[theChanger.value[3]] <= 0 and:{
								msg[theChanger.value[3]].abs > alwaysPositive;
							}, { alwaysPositive = msg[theChanger.value[3]].abs+0.1 });
							if(msg[theChanger.value[3]] < midiOscEnv.calibConstraints.lo, {
								midiOscEnv.calibConstraints.lo = msg[theChanger.value[3]];
								wcm.oscInputRange.model.value_([
									msg[theChanger.value[3]],
									wcm.oscInputRange.model.value[1]
								]).changedKeys(synchKeys);
							});
							if(msg[theChanger.value[3]] > midiOscEnv.calibConstraints.hi, {
								midiOscEnv.calibConstraints.hi = msg[theChanger.value[3]];
								wcm.oscInputRange.model.value_([
									wcm.oscInputRange.model.value[0],
									msg[theChanger.value[3]]
								]).changedKeys(synchKeys);
							});
						});
						// [slot, midiOscEnv.calibConstraints.lo, midiOscEnv.calibConstraints.hi].postln;
						wcm.mapConstrainterLo.value_(midiOscEnv.calibConstraints.lo);
						wcm.mapConstrainterHi.value_(midiOscEnv.calibConstraints.hi);
					}, {
						if(midiOscEnv.calibConstraints.isNil, {
							midiOscEnv.calibConstraints = (
								lo: wcm.oscInputRange.model.value[0],
								hi: wcm.oscInputRange.model.value[1]
							)
						})
					});
					if(this.class == CVWidgetKnob or:{ this.class == CVWidget2D }, {
						argWidgetCV.value_(
							(msg[theChanger.value[3]]+alwaysPositive).perform(
								midiOscEnv.oscMapping,
								midiOscEnv.calibConstraints.lo+alwaysPositive,
								midiOscEnv.calibConstraints.hi+alwaysPositive,
								this.getSpec(slot).minval, this.getSpec(slot).maxval,
								\minmax
							)
						)
					}, {
						argWidgetCV.value_([
							argWidgetCV.value[..(slot-1)],
							(msg[theChanger.value[3]]+alwaysPositive).perform(
								midiOscEnv.oscMapping,
								midiOscEnv.calibConstraints.lo+alwaysPositive,
								midiOscEnv.calibConstraints.hi+alwaysPositive,
								[this.getSpec(slot).minval].flat.wrapAt(slot),
								[this.getSpec(slot).maxval].flat.wrapAt(slot),
								\minmax
							),
							argWidgetCV.value[(slot+1)..]
						].flat);
					})
				};

				if(theChanger.value[0].size > 0, { netAddr = NetAddr(theChanger.value[0], theChanger.value[1]) });

				if(midiOscEnv.oscResponder.isNil, {
					midiOscEnv.oscResponder = OSCresponderNode(netAddr, theChanger.value[2].asSymbol, oscResponderAction).add;
//					midiOscEnv.oscResponder = OSCFunc(oscResponderAction, theChanger.value[2].asSymbol, netAddr);
					midiOscEnv.oscMsgIndex = theChanger.value[3];
				}, {
					midiOscEnv.oscResponder.action_(oscResponderAction);
				});

				tmp = theChanger.value[2].asString++"["++theChanger.value[3].asString++"]"++"\n"++midiOscEnv.oscMapping.asString;
				if(this.class == CVWidgetMS, {
					tmp = slot.asString++":"+tmp;
				});

				wcm.oscDisplay.model.value_(
					(
						but: [tmp, Color.white, Color.cyan(0.5)],
						ipField: theChanger.value[0].asString,
						portField: theChanger.value[1].asString,
						nameField: theChanger.value[2].asString,
						index: theChanger.value[3],
						connectorButVal: 1,
						editEnabled: false
					)
				).changedKeys(synchKeys);
			});

			if(theChanger.value == false, {
				midiOscEnv.oscResponder.remove;
				midiOscEnv.oscResponder = nil;
				midiOscEnv.msgIndex = nil;
				wcm.oscInputRange.model.value_([0.0001, 0.0001]).changedKeys(synchKeys);
				midiOscEnv.calibConstraints = nil;
				// if(this.class == CVWidgetMS, { msSlots[slot] = nil; msCmds[slot] = nil });

				tmp = "edit OSC";
				if(this.class == CVWidgetMS, { tmp = slot.asString++":"+tmp });
				wcm.oscDisplay.model.value_(
					(
						but: [tmp, Color.black, this.bgColor],
						ipField: wcm.oscDisplay.model.value.ipField,
						portField: wcm.oscDisplay.model.value.portField,
						nameField: wcm.oscDisplay.model.value.nameField,
						index: wcm.oscDisplay.model.value.index,
						connectorButVal: 0,
						editEnabled: true
					)
				).changedKeys(synchKeys);
			})
		})
	}

	prInitOscDisplay { |wcm, thisGuiEnv, midiOscEnv, argWidgetCV, thisCalib, slot|
		var thisEditor, thisOscEditBut, p, tmp;
		var numOscString, numOscResponders, oscButBg, oscButTextColor;

		wcm.oscDisplay.controller ?? {
			wcm.oscDisplay.controller = SimpleController(wcm.oscDisplay.model);
		};

		wcm.oscDisplay.controller.put(\default, { |theChanger, what, moreArgs|
			// "prInitOscDisplay: %\n".postf(theChanger.value);

			switch(prCalibrate.class,
				Event, { thisCalib = prCalibrate[slot] },
				Array, { thisCalib = prCalibrate[slot] },
				{ thisCalib = prCalibrate }
			);

			if(this.class == CVWidgetMS, {
				thisEditor = thisGuiEnv.editor[slot];
				thisGuiEnv[\msEditor] !? {
					thisOscEditBut = thisGuiEnv.msEditor.oscEditBtns[slot];
				};
			}, {
				thisEditor = thisGuiEnv.editor;
				if(GUI.id !== \cocoa, {
					if(theChanger.value.but[0] == "edit OSC", {
						if(slot.notNil, { p =  " in '"++slot++"'" }, { p = "" });
						thisGuiEnv.oscEditBut.toolTip_("no OSC-responder present%.\nClick to edit.".format(p));
					}, {
						thisGuiEnv.oscEditBut.toolTip_("Connected, listening to\n%, msg-slot %,\nusing '%' in-output mapping".format(theChanger.value.nameField, theChanger.value.index, midiOscEnv.oscMapping));
					})
				});
				thisOscEditBut = thisGuiEnv.oscEditBut;
			});

			if(window.isClosed.not, {
				if(this.class != CVWidgetMS, {
					thisGuiEnv.oscEditBut.states_([theChanger.value.but]);
					thisGuiEnv.oscEditBut.refresh;
				}, {
					numOscResponders = this.midiOscEnv.select({ |it| it.oscResponder.notNil }).size;
					numOscString = "OSC ("++numOscResponders++"/"++this.msSize++")";
					if(numOscResponders > 0, {
						oscButBg = Color.cyan(0.5);
						oscButTextColor = Color.white;
					}, {
						oscButBg = this.bgColor;
						oscButTextColor = Color.black;
					});
					this.guiEnv[\oscBut].states_([[
						numOscString,
						oscButTextColor, // text
						oscButBg // background
					]]);
				})
			});

			if(this.class == CVWidgetMS, {
				defer {
					if(thisGuiEnv.msEditor.notNil and:{
						thisGuiEnv.msEditor.isClosed.not
					}, {
						thisGuiEnv.msEditor.connectorBut.value_(theChanger.value.connectorButVal);
						if(theChanger.value.ipField.isNil or:{ theChanger.value.ipField == "nil" }, {
							thisGuiEnv.msEditor.ipField.string_("");
						}, {
							thisGuiEnv.msEditor.ipField.string_(theChanger.value.ipField);
						});
						if(theChanger.value.portField.isNil or:{
							theChanger.value.portField == "nil" or:{
								theChanger.value.portField == "0"
							}
						}, {
							thisGuiEnv.msEditor.portField.string_("");
						}, {
							thisGuiEnv.msEditor.portField.string_(theChanger.value.portField);
						});
						thisOscEditBut.states_([theChanger.value.but]);
							if(this.midiOscEnv.select({ |sl| sl[\oscResponder].notNil }).size > 0, {
							thisGuiEnv.msEditor.connectorBut.value_(1);
						}, {
							thisGuiEnv.msEditor.connectorBut.value_(0);
						});
						[
							thisGuiEnv.msEditor.extCtrlArrayField,
							thisGuiEnv.msEditor.intStartIndexField,
							thisGuiEnv.msEditor.ipField,
							thisGuiEnv.msEditor.portField,
							thisGuiEnv.msEditor.nameField,
							thisGuiEnv.msEditor.indexField
						].do(_.enabled_(theChanger.value.editEnabled));
					})
				}
			});

			if(thisEditor.notNil and:{
				thisEditor.isClosed.not
			}, {
				defer {
					thisEditor.connectorBut.value_(theChanger.value.connectorButVal);
					if(theChanger.value.ipField.isNil or:{ theChanger.value.ipField == "nil" }, {
						thisEditor.ipField.string_("");
					}, {
						thisEditor.ipField.string_(theChanger.value.ipField);
					});
						// "theChanger.value.portField (Editor): %\n".postf(theChanger.value.portField);
					if(theChanger.value.portField.isNil or:{
						theChanger.value.portField == "nil" or:{
							theChanger.value.portField == "0"
						}
					}, {
						thisEditor.portField.string_("");
					}, {
						thisEditor.portField.string_(theChanger.value.portField);
					});
					thisEditor.nameField.string_(theChanger.value.nameField);
					if(thisCalib, {
						[
							// thisEditor.inputConstraintLoField,
							// thisEditor.inputConstraintHiField,
							thisEditor.calibNumBoxes.lo,
							thisEditor.calibNumBoxes.hi
						].do(_.enabled_(theChanger.value.editEnabled));
					});
					thisEditor.indexField.value_(theChanger.value.index);
					[
						thisEditor.ipField,
						thisEditor.portField,
						thisEditor.nameField,
						thisEditor.indexField
					].do(_.enabled_(theChanger.value.editEnabled))
				}
			})
		})
	}

	prInitOscInputRange { |wcm, thisGuiEnv, midiOscEnv, argWidgetCV, thisCalib, slot|
		var thisEditor, thisOscEditBut, p, tmp;
		var mappingsDiffer;

		wcm.oscInputRange.controller ?? {
			wcm.oscInputRange.controller = SimpleController(wcm.oscInputRange.model);
		};

		wcm.oscInputRange.controller.put(\default, { |theChanger, what, moreArgs|
			// "prInitOscInputRange: %\n".postf(theChanger.value);
			// "thisGuiEnv: %, slot: %\n".postf(thisGuiEnv.asCompileString, slot);

			if(this.class == CVWidgetMS, {
				thisEditor = thisGuiEnv.editor[slot];
				// "slot: %\n".postf(slot);
				// "thisGuiEnv: %\n".postf(thisGuiEnv);
				thisGuiEnv.msEditor !? {
					thisOscEditBut = thisGuiEnv.msEditor.oscEditBtns[slot];
				}
//				"thisOscEditBut: %\n".postf(thisOscEditBut);
//				thisMidiOscEnv = midiOscEnv[slot]; // hmmm...
			}, {
				thisEditor = thisGuiEnv.editor;
					// "thisGuiEnv.oscEditBut: %\n".postf(thisGuiEnv.oscEditBut);
				thisOscEditBut = thisGuiEnv.oscEditBut;
			});
//			"thisEditor: %\n".postf(thisEditor);

			{
				if(thisEditor.notNil and:{
					thisEditor.isClosed.not
				}, {
					thisEditor.mappingSelect.items.do({ |item, i|
						if(item.asSymbol === midiOscEnv.oscMapping, {
							thisEditor.mappingSelect.value_(i)
						})
					});
					thisEditor.alwaysPosField.string_(" +"++(alwaysPositive.trunc(0.1)));
				});

				if(this.class == CVWidgetMS, {
					if(thisGuiEnv.msEditor.notNil and:{
						thisGuiEnv.msEditor.isClosed.not
					}, {
						tmp = this.msSize.collect({ |sl| this.getOscMapping(sl) });
						block { |break|
							(1..this.msSize-1).do({ |sl|
								if(tmp[0] != tmp[sl], { break.value(mappingsDiffer = true) }, { mappingsDiffer = false });
							})
						};

						midiOscEnv.oscResponder !? {
							thisOscEditBut.states_([[
								thisOscEditBut.states[0][0].split($\n)[0]++"\n"++midiOscEnv.oscMapping.asString,
								thisOscEditBut.states[0][1],
								thisOscEditBut.states[0][2]
							]])
						};

							// "mappings differ: %, %\n".postf(tmp, slot);

						if(mappingsDiffer, {
							thisGuiEnv.msEditor.mappingSelect.value_(0);
						}, {
							thisGuiEnv.msEditor.mappingSelect.items.do({ |item, i|
								if(item.asSymbol === midiOscEnv.oscMapping, {
									thisGuiEnv.msEditor.mappingSelect.value_(i);
								})
							})
						})
					})
				});

				if(window.isClosed.not, {
					if(this.class != CVWidgetMS, {
						// "thisGuiEnv.oscEditBut.states[0][0]: %\n".postf(thisGuiEnv.oscEditBut.states[0][0]);
						if(thisGuiEnv.oscEditBut.states[0][0].split($\n)[0] != "edit OSC", {
							thisGuiEnv.oscEditBut.states_([[
								thisGuiEnv.oscEditBut.states[0][0].split($\n)[0]++"\n"++midiOscEnv.oscMapping.asString,
								thisGuiEnv.oscEditBut.states[0][1],
								thisGuiEnv.oscEditBut.states[0][2]
							]]);
							if(GUI.id !== \cocoa, {
								p = thisGuiEnv.oscEditBut.toolTip.split($\n);
								p[2] = "using '"++midiOscEnv.oscMapping.asString++"' in-output mapping";
								p = p.join("\n");
								thisGuiEnv.oscEditBut.toolTip_(p);
							});
							thisGuiEnv.oscEditBut.refresh;
						})
					})
				})
			}.defer;
		})
	}

	prInitActionsControl { |wcm, thisGuiEnv, midiOscEnv, argWidgetCV, thisCalib, slot|

		wcm.actions.controller ?? {
			wcm.actions.controller = SimpleController(wcm.actions.model);
		};

		wcm.actions.controller.put(\default, { |theChanger, what, moreArgs|
			// "prInitActionsControl: %\n".postf(theChanger.value);
			if(window.isClosed.not, {
				thisGuiEnv.actionsBut.states_([[
					"actions ("++theChanger.value.activeActions++"/"++theChanger.value.numActions++")",
					Color(0.08, 0.09, 0.14),
					Color(0.32, 0.67, 0.76),
				]]);
				if(GUI.id !== \cocoa, {
					thisGuiEnv.actionsBut.toolTip_(""++theChanger.value.activeActions++" of "++theChanger.value.numActions++" active.\nClick to edit")
				})
			})
		})
	}

	// EXPERIMENTAL: extended API
	extend { |key, func ... controllers|
		var thisKey, thisControllers;

		thisKey = key.asSymbol;
		thisControllers = controllers.collect({ |c| c.asSymbol });
		synchedActions ?? { synchedActions = IdentityDictionary.new };

		synchKeys = synchKeys.add(thisKey);
		synchedActions.put(thisKey, func);

		if(thisKey != \default, {
			if(controllers.size == 0, {
				wdgtControllersAndModels.pairsDo({ |k, v|
					if(k != \mapConstrainterHi and:{
						k != \mapConstrainterLo
					}, {
						v.controller.put(thisKey, synchedActions[thisKey])
					})
				})
			}, {
				thisControllers.do({ |c|
					if(wdgtControllersAndModels[c].notNil and:{
						c != \mapConstrainterHi and:{
							c != \mapConstrainterLo
						}
					}, {
						wdgtControllersAndModels[c].controller.put(thisKey, synchedActions[thisKey]);
					})
				})
			})
		}, { Error("'default' is a reserved key and can not be used to extend a controller-action.").throw })
	}

	reduce { |key|
		var thisKey;

		thisKey = key.asSymbol;
		if(key.notNil and:{ thisKey !== \default and:{ synchKeys.includes(thisKey) }}, {
			synchedActions[thisKey] = nil;
			synchKeys.remove(thisKey);
		}, {
			synchKeys.do({ |k|
				if(k != \default, {
					synchedActions[k] = nil;
					synchKeys.remove[k];
				})
			})
		})
	}

}