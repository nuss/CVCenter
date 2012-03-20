/* (c) 2010-2012 2010-2012 Stefan Nussbaumer */
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
					)).changed(\value);
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
					)).changed(\value);
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
					)).changed(\value);
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
					)).changed(\value);
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
			)).changed(\value);
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
					).changed(\value);
				}
			},
			{
				prMidiMode[slot.asSymbol] = mode;
				wdgtControllersAndModels[slot.asSymbol] !? {
					wdgtControllersAndModels[slot.asSymbol].midiOptions.model.value_(
						(
							midiMode: prMidiMode[slot.asSymbol],
							midiMean: prMidiMean[slot.asSymbol],
							ctrlButtonBank: prCtrlButtonBank[slot.asSymbol],
							midiResolution: prMidiResolution[slot.asSymbol],
							softWithin: prSoftWithin[slot.asSymbol]
						)
					).changed(\value);
				}
			}
		);
	}
	
	getMidiMode { |slot|
		switch(this.class,
			CVWidgetKnob, {
				^prMidiMode;
			},
			{ ^prMidiMode[slot.asSymbol] }
		)
	}
	
	setMidiMean { |meanval, slot|
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
					).changed(\value);
				}
			},
			{
				prMidiMean[slot.asSymbol] = meanval;
				wdgtControllersAndModels[slot.asSymbol] !? {
					wdgtControllersAndModels[slot.asSymbol].midiOptions.model.value_(
						(
							midiMode: prMidiMode[slot.asSymbol],
							midiMean: prMidiMean[slot.asSymbol],
							ctrlButtonBank: prCtrlButtonBank[slot.asSymbol],
							midiResolution: prMidiResolution[slot.asSymbol],
							softWithin: prSoftWithin[slot.asSymbol]
						)
					).changed(\value);
				}
			}			
		)
	}
	
	getMidiMean { |slot|
		switch(this.class,
			CVWidgetKnob, {
				^prMidiMean;
			},
			{ ^prMidiMean[slot.asSymbol] }
		)
	}
	
	setSoftWithin { |threshold, slot|
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
					).changed(\value);
				}
			},
			{
				prSoftWithin[slot] = threshold;
				wdgtControllersAndModels[slot.asSymbol] !? {
					wdgtControllersAndModels[slot].midiOptions.model.value_(
						(
							midiMode: prMidiMode[slot.asSymbol],
							midiMean: prMidiMean[slot.asSymbol],
							ctrlButtonBank: prCtrlButtonBank[slot.asSymbol],
							midiResolution: prMidiResolution[slot.asSymbol],
							softWithin: prSoftWithin[slot.asSymbol]
						)
					).changed(\value);
				}
			}
		)	
	}
	
	getSoftWithin { |slot|
		switch(this.class,
			CVWidgetKnob, {
				^prSoftWithin;
			},
			{ ^prSoftWithin[slot.asSymbol] }
		)
	}
	
	setCtrlButtonBank { |numSliders, slot|
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
					).changed(\value);
				}
			},
			{
				prCtrlButtonBank.put(slot.asSymbol, numSliders);
				wdgtControllersAndModels[slot.asSymbol] !? {
					wdgtControllersAndModels[slot.asSymbol].midiOptions.model.value_(
						(
							midiMode: prMidiMode[slot.asSymbol],
							midiMean: prMidiMean[slot.asSymbol],
							ctrlButtonBank: prCtrlButtonBank[slot.asSymbol],
							midiResolution: prMidiResolution[slot.asSymbol],
							softWithin: prSoftWithin[slot.asSymbol]
						)
					).changed(\value);
				}
			}
		)
	}
	
	getCtrlButtonBank { |slot|
		switch(this.class,
			CVWidgetKnob, {
				^prCtrlButtonBank;
			},
			{ ^prCtrlButtonBank[slot.asSymbol] }
		)
	}
	
	setMidiResolution { |resolution, slot|
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
					).changed(\value);
				}
			},
			{
				prMidiResolution[slot.asSymbol] = resolution;
				wdgtControllersAndModels[slot.asSymbol] !? {
					wdgtControllersAndModels[slot.asSymbol].midiOptions.model.value_(
						(
							midiMode: prMidiMode[slot.asSymbol],
							midiMean: prMidiMean[slot.asSymbol],
							ctrlButtonBank: prCtrlButtonBank[slot.asSymbol],
							midiResolution: prMidiResolution[slot.asSymbol],
							softWithin: prSoftWithin[slot.asSymbol]
						)
					).changed(\value);
				}
			}
		)
	}
	
	getMidiResolution { |slot|
		switch(this.class,
			CVWidgetKnob, {
				^prMidiResolution;
			},
			{ ^prMidiResolution[slot.asSymbol] }
		)
	}
	
	setCalibrate { |bool, slot|
		if(bool.isKindOf(Boolean).not, {
			Error("calibration can only be set to true or false!").throw;
		});
		switch(this.class, 
			CVWidgetKnob, {
				prCalibrate = bool;
				wdgtControllersAndModels.oscConnection.model.value_(
					wdgtControllersAndModels.oscConnection.model.value
				).changed(\value);
//				wdgtControllersAndModels.oscDisplay.model.value_(
//					wdgtControllersAndModels.oscDisplay.model.value
//				).changed(\value);
				wdgtControllersAndModels.calibration.model.value_(bool).changed(\value);
			},
			{
				prCalibrate[slot.asSymbol] = bool;
				wdgtControllersAndModels[slot.asSymbol].oscConnection.model.value_(
					wdgtControllersAndModels[slot.asSymbol].oscConnection.model.value
				).changed(\value);
//				wdgtControllersAndModels[slot.asSymbol].oscDisplay.model.value_(
//					wdgtControllersAndModels[slot.asSymbol].oscDisplay.model.value
//				).changed(\value);
				wdgtControllersAndModels[slot.asSymbol].calibration.model.value_(bool).changed(\value);
			}
		)
	}
	
	getCalibrate { |slot|
		switch(this.class,
			CVWidgetKnob, {
				^prCalibrate;
			},
			{ ^prCalibrate[slot.asSymbol] }
		)
	}
	
	setSpec { |spec, slot|
		var thisSpec;
		if(spec.class == String, { thisSpec = spec.asSymbol }, { thisSpec = spec });
		switch(this.class,
			CVWidgetKnob, {
				if(thisSpec.asSpec.isKindOf(ControlSpec).not, {
					Error("Please provide a valid spec! (its class must inherit from ControlSpec)").throw;
				});
				wdgtControllersAndModels.cvSpec.model.value_(thisSpec.asSpec).changed(\value);
			},
			{
				if(thisSpec.asSpec.isKindOf(ControlSpec), {
					wdgtControllersAndModels[slot.asSymbol].cvSpec.model.value_(thisSpec.asSpec).changed(\value);
				}, {
					Error("Please provide a valid ControlSpec!").throw;
				});
			}
		)
	}
	
	getSpec { |slot|
		switch(this.class,
			CVWidgetKnob, {
				^widgetCV.spec;
			},
			{
				^widgetCV[slot.asSymbol].spec;
			}
		)
	}
	
	setOscMapping { |mapping, slot|
		if(mapping.asSymbol !== \linlin and:{
			mapping.asSymbol !== \linexp and:{
				mapping.asSymbol !== \explin and:{
					mapping.asSymbol !== \expexp
				}
			}
		}, {
			Error("A valid mapping can either be \\linlin, \\linexp, \\explin or \\expexp").throw;
		});
		switch(this.class,
			CVWidgetKnob, {
				midiOscEnv.oscMapping = mapping.asSymbol;
				wdgtControllersAndModels.oscInputRange.model.value_(
					wdgtControllersAndModels.oscInputRange.model.value;
				).changed(\value);
				wdgtControllersAndModels.cvSpec.model.value_(
					wdgtControllersAndModels.cvSpec.model.value;
				).changed(\value);
			},
			{	
				midiOscEnv[slot.asSymbol].oscMapping = mapping.asSymbol;
				wdgtControllersAndModels[slot.asSymbol].oscInputRange.model.value_(
					wdgtControllersAndModels[slot.asSymbol].oscInputRange.model.value;
				).changed(\value);
				wdgtControllersAndModels[slot.asSymbol].cvSpec.model.value_(
					wdgtControllersAndModels[slot.asSymbol].cvSpec.model.value;
				).changed(\value);
			}
		)
	}
		
	getOscMapping { |slot|
		switch(this.class, 
			CVWidgetKnob, {
				^midiOscEnv.oscMapping;
			},
			{
				^midiOscEnv[slot.asSymbol].oscMapping
			}
		)
	}
	
	oscConnect { |ip, port, name, oscMsgIndex=1, slot|
		var thisIP, intPort;
		
		if(ip.size > 0 and:{
			"^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$".matchRegexp(ip).not and:{
				ip != "nil"
			}
		}, {
			Error("Please provide a valid IP-address or leave the IP-field empty").throw;
		});
		
		if(port.size > 0, {
			if("^[0-9]{1,5}$".matchRegexp(port).not and:{ port != "nil" }, {
				Error("Please provide a valid port or leave this field empty").throw;
			}, {
				intPort = port.asInt;
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
				wdgtControllersAndModels.oscConnection.model.value_([thisIP, intPort, name.asSymbol, oscMsgIndex]).changed(\value);
				CmdPeriod.add({ this.oscDisconnect });
			},
			{
				wdgtControllersAndModels[slot.asSymbol].oscConnection.model.value_([thisIP, intPort, name.asSymbol, oscMsgIndex]).changed(\value);
				CmdPeriod.add({ this.oscDisconnect(slot.asSymbol) });
			}
		)
	}
	
	oscDisconnect { |slot|
		switch(this.class, 
			CVWidgetKnob, {
				wdgtControllersAndModels.oscConnection.model.value_(false).changed(\value);
				wdgtControllersAndModels.oscInputRange.model.value_([0.00001, 0.00001]).changed(\value);
				CmdPeriod.remove({ this.oscDisconnect });
			},
			{
				wdgtControllersAndModels[slot.asSymbol].oscConnection.model.value_(false).changed(\value);
				wdgtControllersAndModels[slot.asSymbol].oscInputRange.model.value_([0.00001, 0.00001]).changed(\value);
				CmdPeriod.remove({ this.oscDisconnect(slot) });
			}
		)
	}
	
	// if all arguments besides 'slot' are nil .learn should be triggered
	midiConnect { |uid, chan, num, slot|
		switch(this.class,
			CVWidgetKnob, {
				if(midiOscEnv.cc.isNil, {
					wdgtControllersAndModels.midiConnection.model.value_(
						(src: uid, chan: chan, num: num)
					).changed(\value);
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
					wdgtControllersAndModels[slot.asSymbol].midiConnection.model.value_(
						(src: uid, chan: chan, num: num)
					).changed(\value);
					CmdPeriod.add({ this !? { this.midiDisconnect(slot) } });
				}, {
					"Already connected!".warn;	
				})
			}
		)
	}
	
	midiDisconnect { |slot|
		switch(this.class,
			CVWidgetKnob, {
				wdgtControllersAndModels.midiConnection.model.value_(nil).changed(\value);
				CmdPeriod.remove({ this.midiDisconnect });
			}, 
			{
				wdgtControllersAndModels[slot.asSymbol].midiConnection.model.value_(nil).changed(\value);
				CmdPeriod.remove({ this.midiDisconnect(slot) });
			}
		)		
	}
	
	setOscInputConstraints { |constraintsHiLo, slot|
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
					midiOscEnv[slot.asSymbol].calibConstraints = (lo: constraintsHiLo.x, hi: constraintsHiLo.y);
					if(editor[slot.asSymbol].notNil and:{ editor[slot.asSymbol].isClosed.not }, {
						wdgtControllersAndModels[slot.asSymbol].mapConstrainterLo.value_(constraintsHiLo.x);
						wdgtControllersAndModels[slot.asSymbol].mapConstrainterHi.value_(constraintsHiLo.y);
					})
				}
			)
		})
	}
	
	getOscInputConstraints { |slot|
		switch(this.class,
			CVWidgetKnob, {
				^midiOscEnv.calibConstraints;
			},
			{
				^midiOscEnv[slot.asSymbol].calibConstraints;
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
			^this.window.isClosed;
		})
	}
	
	initControllersAndModels { |controllersAndModels, slot|
		var wcm;
						
		if(controllersAndModels.notNil, {
			wdgtControllersAndModels = controllersAndModels;
		}, {
			wdgtControllersAndModels ?? { wdgtControllersAndModels = () };
		});
				
		slot !? {
			if(wdgtControllersAndModels[slot].isNil, {
				wdgtControllersAndModels.put(slot, ());
			})
		};
		
		if(slot.notNil, {
			wcm = wdgtControllersAndModels[slot];
		}, {
			wcm = wdgtControllersAndModels;
		});
								
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
			wcm.cvSpec = ();
		};
		wcm.cvSpec.model ?? { 
			wcm.cvSpec.model = Ref(this.getSpec(slot));
		};
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
			wcm.oscDisplay.model = Ref((
				but: ["edit OSC", Color.black, Color.clear],
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
			wcm.midiOptions.model = Ref(
				(
					midiMode: prMidiMode, 
					midiMean: prMidiMean, 
					ctrlButtonBank: prCtrlButtonBank, 
					midiResolution: prMidiResolution, 
					softWithin: prSoftWithin
				)
			)
		};
		wcm.mapConstrainterLo ?? { 
			wcm.mapConstrainterLo = CV([-inf, inf].asSpec, wcm.oscInputRange.model.value[0]);
		};
		wcm.mapConstrainterHi ?? { 
			wcm.mapConstrainterHi = CV([-inf, inf].asSpec, wcm.oscInputRange.model.value[1]);
		};
		wcm.actions ?? {
			wcm.actions = ();
		};
		wcm.actions.model ?? {
			wcm.actions.model = Ref((numActions: 0, activeActions: 0))
		};
		
	}
		
	initControllerActions { |slot|
		var wcm, thisGuiEnv, midiOscEnv, tmpSetup, thisWidgetCV;
		var thisCalib;
						
		if(slot.notNil, {
			wcm = wdgtControllersAndModels[slot];
			thisGuiEnv = this.guiEnv[slot];
			midiOscEnv = this.midiOscEnv[slot];
			thisWidgetCV = this.widgetCV[slot];
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
		
		wcm.calibration.controller ?? { 
			wcm.calibration.controller = SimpleController(wcm.calibration.model);
		};

		wcm.calibration.controller.put(\value, { |theChanger, what, moreArgs|
			theChanger.value.switch(
				true, { 
					this.window.isClosed.not.if { thisGuiEnv.calibBut.value_(0) };
					if(thisGuiEnv.editor.notNil and:{ thisGuiEnv.editor.isClosed.not }, {
						thisGuiEnv.editor.calibBut.value_(0);
						wcm.mapConstrainterLo ?? { 
							wcm.mapConstrainterLo = CV([-inf, inf].asSpec, 0.00001);
							wcm.mapConstrainterLo.connect(thisGuiEnv.editor.calibNumBoxes.lo);
						};
						wcm.mapConstrainterHi ?? { 
							wcm.mapConstrainterHi = CV([-inf, inf].asSpec, 0.00001);
							wcm.mapConstrainterHi.connect(thisGuiEnv.editor.calibNumBoxes.hi);
						};
						[thisGuiEnv.editor.calibNumBoxes.lo, thisGuiEnv.editor.calibNumBoxes.hi].do({ |nb| 
							nb.enabled_(false);
							nb.action_(nil);
						})
					})
				},
				false, { 
					this.window.isClosed.not.if { thisGuiEnv.calibBut.value_(1) };
					if(thisGuiEnv.editor.notNil and:{ thisGuiEnv.editor.isClosed.not }, {
						thisGuiEnv.editor.calibBut.value_(1);
						[wcm.mapConstrainterLo, wcm.mapConstrainterHi].do({ |cv| cv = nil; });
						[thisGuiEnv.editor.calibNumBoxes.lo, thisGuiEnv.editor.calibNumBoxes.hi].do({ |nb| 
							nb.enabled_(true);
							nb.action_({ |b| 
								this.setOscInputConstraints(
									thisGuiEnv.editor.calibNumBoxes.lo.value @ thisGuiEnv.editor.calibNumBoxes.hi.value, slot;
								) 
							})
						})
					})
				}
			)
		})
	}
	
	prInitSpecControl { |wcm, thisGuiEnv, midiOscEnv, argWidgetCV, thisCalib, slot|
		var tmp, tmpMapping;
		
		wcm.cvSpec.controller ?? {
			wcm.cvSpec.controller = SimpleController(wcm.cvSpec.model);
		};
			
		wcm.cvSpec.controller.put(\value, { |theChanger, what, moreArgs|
			if(theChanger.value.minval <= 0.0 or:{
				theChanger.value.maxval <= 0.0
			}, {
				if(midiOscEnv.oscMapping === \linexp or:{
					midiOscEnv.oscMapping === \expexp
				}, {
					midiOscEnv.oscMapping = \linlin;
					if(thisGuiEnv.editor.notNil and:{
						thisGuiEnv.editor.isClosed.not
					}, {
						thisGuiEnv.editor.mappingSelect.value_(0);
					})
				})
			}, {
				if(thisGuiEnv.editor.notNil and:{
					thisGuiEnv.editor.isClosed.not	
				}, {
					tmpMapping = thisGuiEnv.editor.mappingSelect.item;
					thisGuiEnv.editor.mappingSelect.items.do({ |item, i|
						if(item == tmpMapping, {
							thisGuiEnv.editor.mappingSelect.value_(i)
						})
					});
				})
			});
			
			if(thisGuiEnv.editor.notNil and:{
				thisGuiEnv.editor.isClosed.not	
			}, {
				thisGuiEnv.editor.specField.string_(theChanger.value.asCompileString);
				tmp = thisGuiEnv.editor.specsListSpecs.detectIndex({ |item, i| item == theChanger.value });
				if(tmp.notNil, {
					thisGuiEnv.editor.specsList.value_(tmp);
				}, {
					thisGuiEnv.editor.specsList.items = List["custom:"+(theChanger.value.asString)]++thisGuiEnv.editor.specsList.items;
					thisGuiEnv.editor.specsListSpecs.array_([theChanger.value]++thisGuiEnv.editor.specsListSpecs.array);
					thisGuiEnv.editor.specsList.value_(0);
					thisGuiEnv.editor.specsList.refresh;
				})
			});
			
			argWidgetCV.spec_(theChanger.value);
			
			if(this.class === CVWidgetKnob, {
				block { |break|
					#[pan, boostcut, bipolar, detune].do({ |symbol| 
						if(argWidgetCV.spec == symbol.asSpec, { 
							break.value(thisGuiEnv.knob.centered_(true));
						}, {
							thisGuiEnv.knob.centered_(false);
						})			
					})
				}
			});
		})
	}
	
	prInitMidiConnect { |wcm, thisGuiEnv, midiOscEnv, argWidgetCV, thisCalib, slot|
		var ctrlString, meanVal, ccResponderAction, makeCCResponder;
		
		wcm.midiConnection.controller ?? {
			wcm.midiConnection.controller = SimpleController(wcm.midiConnection.model);
		};
		
		wcm.midiConnection.controller.put(\value, { |theChanger, what, moreArgs|
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
									).changed(\value)
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
				).changed(\value);
				midiOscEnv.midisrc = nil; midiOscEnv.midichan = nil; midiOscEnv.midinum = nil; midiOscEnv.midiRawNum = nil;
			})
		})
	}
	
	prInitMidiDisplay { |wcm, thisGuiEnv, midiOscEnv, argWidgetCV, thisCalib, slot|
		
		wcm.midiDisplay.controller ?? {
			wcm.midiDisplay.controller = SimpleController(wcm.midiDisplay.model);
		};
		
		wcm.midiDisplay.controller.put(\value, { |theChanger, what, moreArgs|
			theChanger.value.learn.switch(
				"X", {
					defer {
						if(this.window.isClosed.not, {
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
							thisGuiEnv.midiLearn.value_(1);
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
					thisGuiEnv.midiLearn.states_([
						["C", Color.white, Color(0.11, 0.38, 0.2)],
						["X", Color.white, Color.red]
					]).refresh;
					if(thisGuiEnv.editor.notNil and:{
						thisGuiEnv.editor.isClosed.not
					}, {
						thisGuiEnv.editor.midiLearnBut.states_([
							["C", Color.white, Color(0.11, 0.38, 0.2)],
							["X", Color.white, Color.red]
						]).refresh;
					})
				},
				"L", {
					defer {
						if(this.window.isClosed.not, {
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
	}
	
	prInitMidiOptions { |wcm, thisGuiEnv, midiOscEnv, argWidgetCV, thisCalib, slot|

		wcm.midiOptions.controller ?? {
			wcm.midiOptions.controller = SimpleController(wcm.midiOptions.model);
		};
		
		wcm.midiOptions.controller.put(\value, { |theChanger, what, moreArgs|
			if(thisGuiEnv.editor.notNil and:{
				thisGuiEnv.editor.isClosed.not
			}, {
				thisGuiEnv.editor.midiModeSelect.value_(theChanger.value.midiMode);
				thisGuiEnv.editor.midiMeanNB.value_(theChanger.value.midiMean);
				thisGuiEnv.editor.softWithinNB.value_(theChanger.value.softWithin);
				thisGuiEnv.editor.midiResolutionNB.value_(theChanger.value.midiResolution);
				thisGuiEnv.editor.ctrlButtonBankField.string_(theChanger.value.ctrlButtonBank);
			})
		})
	}
	
	prInitOscConnect { |wcm, thisGuiEnv, midiOscEnv, argWidgetCV, thisCalib, slot|
		var oscResponderAction;
				
		wcm.oscConnection.controller ?? {
			wcm.oscConnection.controller = SimpleController(wcm.oscConnection.model);
		};

		wcm.oscConnection.controller.put(\value, { |theChanger, what, moreArgs|
			switch(prCalibrate.class, 
				Event, { thisCalib = prCalibrate[slot] },
				{ thisCalib = prCalibrate }
			);
						
			if(theChanger.value.size == 4, {
// 				OSCresponderNode: t, r, msg
// 				OSCfunc: msg, time, addr // for the future
				oscResponderAction = { |t, r, msg|
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
								]).changed(\value);
							});
							if(msg[theChanger.value[3]] > midiOscEnv.calibConstraints.hi, {
								midiOscEnv.calibConstraints.hi = msg[theChanger.value[3]];
								wcm.oscInputRange.model.value_([
									wcm.oscInputRange.model.value[0], 
									msg[theChanger.value[3]]
								]).changed(\value);
							});
						});
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
					argWidgetCV.value_(
						(msg[theChanger.value[3]]+alwaysPositive).perform(
							midiOscEnv.oscMapping,
							midiOscEnv.calibConstraints.lo+alwaysPositive, 
							midiOscEnv.calibConstraints.hi+alwaysPositive,
							this.getSpec(slot).minval, this.getSpec(slot).maxval,
							\minmax
						)
					)
				};
								
				if(theChanger.value[0].size > 0, { netAddr = NetAddr(theChanger.value[0], theChanger.value[1]) });
												
				if(midiOscEnv.oscResponder.isNil, { 
					midiOscEnv.oscResponder = OSCresponderNode(netAddr, theChanger.value[2].asSymbol, oscResponderAction).add;
//					midiOscEnv.oscResponder = OSCFunc(oscResponderAction, theChanger.value[2].asSymbol, netAddr);
					midiOscEnv.oscMsgIndex = theChanger.value[3];
				}, {
					midiOscEnv.oscResponder.action_(oscResponderAction);
				});
				
				wcm.oscDisplay.model.value_(
					(
						but: [theChanger.value[2].asString++"["++theChanger.value[3].asString++"]"++"\n"++midiOscEnv.oscMapping.asString, Color.white, Color.cyan(0.5)],
						ipField: theChanger.value[0].asString,
						portField: theChanger.value[1].asString,
						nameField: theChanger.value[2].asString,
						index: theChanger.value[3],
						connectorButVal: 1, 
						editEnabled: false
					)
				).changed(\value);
			});
			if(theChanger.value == false, {
				midiOscEnv.oscResponder.remove;
				midiOscEnv.oscResponder = nil;
				midiOscEnv.msgIndex = nil;
				wcm.oscInputRange.model.value_([0.0001, 0.0001]).changed(\value);
				midiOscEnv.calibConstraints = nil;
				
				wcm.oscDisplay.model.value_(
					(
						but: ["edit OSC", Color.black, Color.clear],
						ipField: wcm.oscDisplay.model.value.ipField,
						portField: wcm.oscDisplay.model.value.portField,
						nameField: wcm.oscDisplay.model.value.nameField,
						index: wcm.oscDisplay.model.value.index,
						connectorButVal: 0, 
						editEnabled: true
					)
				).changed(\value);
			})
		})
	}
	
	prInitOscDisplay { |wcm, thisGuiEnv, midiOscEnv, argWidgetCV, thisCalib, slot|
		
		wcm.oscDisplay.controller ?? {
			wcm.oscDisplay.controller = SimpleController(wcm.oscDisplay.model);
		};
		
		wcm.oscDisplay.controller.put(\value, { |theChanger, what, moreArgs|
			switch(prCalibrate.class, 
				Event, { thisCalib = prCalibrate[slot] },
				{ thisCalib = prCalibrate }
			);
			if(this.window.isClosed.not, {
				thisGuiEnv.oscEditBut.states_([theChanger.value.but]);
				thisGuiEnv.oscEditBut.refresh;
			});
			defer {
				if(thisGuiEnv.editor.notNil and:{
					thisGuiEnv.editor.isClosed.not
				}, {
					thisGuiEnv.editor.connectorBut.value_(theChanger.value.connectorButVal);
					thisGuiEnv.editor.ipField.string_(theChanger.value.ipField);
					thisGuiEnv.editor.portField.string_(theChanger.value.portField);
					thisGuiEnv.editor.nameField.string_(theChanger.value.nameField);
					if(thisCalib, {
						[
//							thisGuiEnv.editor.inputConstraintLoField, 
//							thisGuiEnv.editor.inputConstraintHiField
							thisGuiEnv.editor.calibNumBoxes.lo, 
							thisGuiEnv.editor.calibNumBoxes.hi
						].do(_.enabled_(theChanger.value.editEnabled));
					});
					thisGuiEnv.editor.indexField.value_(theChanger.value.index);
					thisGuiEnv.editor.connectorBut.value_(theChanger.value.connectorButVal);
					[
						thisGuiEnv.editor.ipField,
						thisGuiEnv.editor.portField,
						thisGuiEnv.editor.nameField,
						thisGuiEnv.editor.indexField
					].do(_.enabled_(theChanger.value.editEnabled))
				})
			}
		})
	}
	
	prInitOscInputRange { |wcm, thisGuiEnv, midiOscEnv, argWidgetCV, thisCalib, slot|
		
		wcm.oscInputRange.controller ?? {
			wcm.oscInputRange.controller = SimpleController(wcm.oscInputRange.model);
		};

		wcm.oscInputRange.controller.put(\value, { |theChanger, what, moreArgs|
			{
				if(thisGuiEnv.editor.notNil and:{
					thisGuiEnv.editor.isClosed.not
				}, {
					thisGuiEnv.editor.mappingSelect.items.do({ |item, i|
						if(item.asSymbol === midiOscEnv.oscMapping, {
							thisGuiEnv.editor.mappingSelect.value_(i)
						})
					});
					thisGuiEnv.editor.alwaysPosField.string_(" +"++(alwaysPositive.trunc(0.1)));
				});
				if(this.window.isClosed.not, {
					if(thisGuiEnv.oscEditBut.states[0][0].split($\n)[0] != "edit OSC", {
						thisGuiEnv.oscEditBut.states_([[
							thisGuiEnv.oscEditBut.states[0][0].split($\n)[0]++"\n"++midiOscEnv.oscMapping.asString,
							thisGuiEnv.oscEditBut.states[0][1],
							thisGuiEnv.oscEditBut.states[0][2]
						]]);
						thisGuiEnv.oscEditBut.refresh;
					})
				})
			}.defer;
		})
	}
	
	prInitActionsControl { |wcm, thisGuiEnv, midiOscEnv, argWidgetCV, thisCalib, slot|
		
		wcm.actions.controller ?? {
			wcm.actions.controller = SimpleController(wcm.actions.model);
		};
		
		wcm.actions.controller.put(\value, { |theChanger, what, moreArgs|
			if(this.window.isClosed.not, {
				thisGuiEnv.actionsBut.states_([[
					"actions ("++theChanger.value.activeActions++"/"++theChanger.value.numActions++")",
					Color(0.08, 0.09, 0.14),
					Color(0.32, 0.67, 0.76),
				]])
			})
		})
	}

}