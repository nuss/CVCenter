/* (c) Stefan Nussbaumer */
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

CVCenter {

	classvar <all, nextCVKey, <cvWidgets, <window, <tabs, prefPane, removeButs;
	classvar <>midiMode, <>midiResolution, <>ctrlButtonBank, <>midiMean, <>softWithin;
	classvar <>guix, <>guiy, <>guiwidth, <>guiheight;
	classvar widgetStates;
	classvar tabProperties, colors, nextColor;
	classvar widgetwidth, widgetheight=181, colwidth, rowheight;
	classvar nDefWin, pDefWin, pDefnWin, tDefWin, allWin, historyWin;

	*new { |cvs...setUpArgs|
		var r, g, b;

		if(all.isNil, {
			all = IdentityDictionary.new;
			cvWidgets = IdentityDictionary.new;
			widgetStates ?? { widgetStates = IdentityDictionary.new };
			removeButs = IdentityDictionary.new;
			r = g = b = (0.6, 0.65 .. 0.75);
			colors = List();
			tabProperties = [];

			if(setUpArgs.size > 0, {
				this.prSetup(setUpArgs);
			});

			r.do({ |red|
				g.do({ |green|
					b.do({ |blue|
						colors.add(Color(red, green, blue));
					})
				})
			});

			nextColor = Pxrand(colors, inf).asStream;

			if(cvs.isNil, {
				nextCVKey = 1;
			}, {
				if(cvs.isKindOf(Dictionary).not and:{
					cvs.isKindOf(IdentityDictionary).not and:{
						cvs.isKindOf(Event).not
					}
				}, {
					Error("Arguments for CVCenter have to be either a Dictionary, an IdentityDictionary or an Event.").throw;
				}, {
					cvs.keysValuesDo({ |k, v|
						if("^cv[0-9]".matchRegexp(k.asString).not, {
							all.put(k.asSymbol, v.asSpec);
						}, {
							"Your given key-name matches the reserved names for new keys in the CVCenter. Please choose a different name.".warn;
						})
					});
				})
			})
		})
	}

	*makeWindow { |tab...cvs|
		var flow, rowwidth, colcount;
		var cvTabIndex, order, orderedCVs;
		var updateRoutine, lastUpdate, lastUpdateWidth, lastSetUp, lastCtrlBtnBank, removedKeys, skipJacks;
		var lastCtrlBtnsMode, swFlow;
		var thisNextPos, tabLabels, labelColors, unfocusedColors;
		var funcToAdd;
		var cvcArgs, btnColor;
		var prefBut, saveBut, loadBut, autoConnectOSCRadio, autoConnectMIDIRadio, loadActionsRadio;
		var midiFlag, oscFlag, loadFlag, tmp, wdgtActions;
		var nDefGui, pDefGui, pDefnGui, tDefGui, allGui, historyGui;

		cvs !? { this.put(*cvs) };

		this.guix ?? { this.guix_(0) };
		this.guiy ?? { this.guiy_(0) };
		this.guiwidth ?? { this.guiwidth_(500) };
		this.guiheight ?? { this.guiheight_(265) };

		if(window.isNil or:{ window.isClosed }, {
			window = Window("CVCenter", Rect(this.guix, this.guiy, this.guiwidth, this.guiheight));
			if(Quarks.isInstalled("wslib") and:{ GUI.id !== \swing }, { window.background_(Color.black) });
			window.view.background_(Color.black);
			flow = FlowLayout(window.bounds.insetBy(4));
			window.view.decorator = flow;
			flow.margin_(4@0);
			flow.gap_(0@4);
			flow.shift(0, 0);

			if(tabProperties.size < 1, {
				tabProperties = tabProperties.add(());
				if(tab.isNil, { tabProperties[0].tabLabel = "default" }, { tabProperties[0].tabLabel = tab.asString });
				tabProperties[0].tabColor = nextColor.next;
			}, {
				if(tabProperties.size == 1 and:{
					tabProperties[0].tabLabel == "default" and:{
						tabProperties[0].nextPos == (0@0)
					}
				}, {
					tab !? {
						tabProperties[0].tabLabel = tab.asString;
						tabProperties[0].tabColor = nextColor.next;
					};
				})
			});

			tabLabels = tabProperties.collect(_.tabLabel);
			labelColors = tabProperties.collect(_.tabColor);
			unfocusedColors = tabProperties.collect({ |t| t.tabColor.copy.alpha_(0.8) });

			tabs = TabbedView(
				window,
				Rect(0, 0, flow.bounds.width, flow.bounds.height-40),
				labels: tabLabels,
				scroll: true
			);
			tabs.backgrounds_(Color(0.1, 0.1, 0.1)!tabs.views.size);
			tabs.view.resize_(5);

			tabs.labelColors_(labelColors);
			tabs.labelPadding_(7);
			tabs.tabHeight_(15);
			tabs.unfocusedColors_(unfocusedColors);
			tabs.font_(GUI.font.new("Helvetica", 10, true));
			tabs.tabCurve_(3);
			tabs.stringColor_(Color.black);
			tabs.stringFocusedColor_(Color(0.0, 0.0, 0.5, 1.0));
			tabs.focusActions_(Array.fill(tabs.views.size, {{ this.prRegroupWidgets(tabs.activeTab) }}));

			flow.shift(0, 0);

			prefPane = ScrollView(window, Rect(0, 0, flow.bounds.width, 40));
			prefPane.decorator = swFlow = FlowLayout(prefPane.bounds, 0@0, 0@0);
			prefPane.resize_(8).background_(Color.black);

			[tabs.view, tabs.views, prefPane].flat.do({ |v|
				v.keyDownAction_({ |view, char, modifiers, unicode, keycode|
//					[view, char, modifiers, unicode, keycode].postcs;
					switch(keycode,
						16r1000014, { tabs.focus((tabs.activeTab+1).wrap(0, tabs.views.size-1)) },
						16r1000012, { tabs.focus((tabs.activeTab-1).wrap(0, tabs.views.size-1)) },
						// when and why have the keycodes been changed??
						124, { tabs.focus((tabs.activeTab+1).wrap(0, tabs.views.size-1)) },
						123, { tabs.focus((tabs.activeTab-1).wrap(0, tabs.views.size-1)) }
					);
					switch(unicode,
						99, { OSCCommands.gui }, // "c" -> collect OSC-commands resp. open the collector's GUI
						111, { CVCenterControllersMonitor(1) }, // key "o" -> osc
						109, { CVCenterControllersMonitor(0) }, // key "m" -> midi
						120, { // key "x" -> close window
							if(CVCenterControllersMonitor.window.notNil and:{
								CVCenterControllersMonitor.window.isClosed.not;
							}, {
								CVCenterControllersMonitor.window.close;
							})
						},
						104, { // key "h" -> start History and open History window
							if(History.started === false, { History.start });
							if(historyWin.isNil or:{ historyWin.isClosed }, {
								historyGui = History.makeWin(
									Window.screenBounds.width-300 @ Window.screenBounds.height
								);
								historyWin = historyGui.w;
							});
							if(historyWin.notNil and:{ historyWin.isClosed.not }, { historyWin.front })
						},
						110, {
							if(nDefWin.isNil or:{ nDefWin.isClosed }, {
								nDefGui = NdefMixer(Server.default); nDefWin = nDefGui.parent;
							});
							if(nDefWin.notNil and:{ nDefWin.isClosed.not }, { nDefWin.front });
						}, // key "n" -> the NdefMixer for the default server
						112, {
							if(pDefWin.isNil or: { pDefWin.isClosed }, {
								pDefGui = PdefAllGui(); pDefWin = pDefGui.parent;
							});
							if(pDefWin.notNil and:{ pDefWin.isClosed.not }, { pDefWin.front });
						}, // key "p"
						80, {
							if(pDefnWin.isNil or: { pDefnWin.isClosed }, {
								pDefnGui = PdefnAllGui(); pDefnWin = pDefnGui.parent;
							});
							if(pDefnWin.notNil and:{ pDefnWin.isClosed.not }, { pDefnWin.front });
						}, // key shift+"p"
						116, {
							if(tDefWin.isNil or:{ tDefWin.isClosed }, {
								tDefGui = TdefAllGui(); tDefWin = tDefGui.parent;
							});
							if(tDefWin.notNil and:{ tDefWin.isClosed.not }, { tDefWin.front });
						}, // key "t"
						97, { if(\AllGui.asClass.notNil, {
								if(allWin.isNil or:{ allWin.isClosed }, {
									allGui = \AllGui.asClass.new; allWin = allGui.parent;
								});
								if(allWin.notNil and:{ allWin.isClosed.not }, { allWin.front })
							})
						} // key "a"
					);
					if((48..57).includes(unicode), { tabs.views[unicode-48] !? { tabs.focus(unicode-48) }});
					if(modifiers == 131072 and:{ unicode == 72 and:{ History.started }}, {
						// keys <shift> + "h" -> end History and open History in a new document
						History.end;
						if(Platform.ideName != "scqt", { History.document });
					})
				})
			});

//			prefBut = Button(prefPane, Rect(0, 0, 70, 20))
//				.font_(Font("Helvetica", 10))
//				.states_([["preferences", Color.red, Color.yellow]])
//				.action_({ |pb| })
//			;
//
//			swFlow.shift(1, 0);

			saveBut = Button(prefPane, Rect(0, 0, 70, 20))
				.font_(Font("Helvetica", 10))
				.states_([["save setup", Color.white, Color(0.15, 0.15, 0.15)]])
				.action_({ |sb| this.saveSetup })
			;

			swFlow.shift(1, 0);

			loadBut = Button(prefPane, Rect(0, 0, 70, 20))
				.font_(Font("Helvetica", 10))
				.states_([["load setup", Color.white, Color(0.15, 0.15, 0.15)]])
				.action_({ |pb|
					if(loadActionsRadio.value.asBoolean, { loadFlag = true }, { loadFlag = false });
					if(autoConnectMIDIRadio.value.asBoolean, { midiFlag = true }, { midiFlag = false });
					if(autoConnectOSCRadio.value.asBoolean, { oscFlag = true }, { oscFlag = false });
					this.loadSetup(autoConnectMIDI: midiFlag, autoConnectOSC: oscFlag, loadActions: loadFlag);
				})
			;

			swFlow.shift(8, 0);

			StaticText(prefPane, Rect(0, 0, 60, 20))
				.font_(Font("Helvetica", 10))
				.stringColor_(Color.white)
				.string_("load actions")
				.align_(\right)
			;

			swFlow.shift(5, 2);

			if(GUI.id === \cocoa, {
				loadActionsRadio = Button(prefPane, Rect(0, 0, 15, 15))
					.font_(Font("Helvetica", 10))
					.states_([
						["", Color.red, Color.white],
						["X", Color.red, Color.white]
					])
					.value_(1)
				;
			}, {
				loadActionsRadio = \CheckBox.asClass.new(prefPane, Rect(0, 0, 15, 15)).value_(true)
			});

			swFlow.shift(5, -2);

			StaticText(prefPane, Rect(0, 0, 90, 20))
				.font_(Font("Helvetica", 10))
				.stringColor_(Color.white)
				.string_("auto-connect MIDI")
				.align_(\right)
			;

			swFlow.shift(5, 2);

			if(GUI.id === \cocoa, {
				autoConnectMIDIRadio = Button(prefPane, Rect(0, 0, 15, 15))
					.font_(Font("Helvetica", 10))
					.states_([
						["", Color.red, Color.white],
						["X", Color.red, Color.white]
					])
					.value_(1)
				;
			}, {
				autoConnectMIDIRadio = \CheckBox.asClass.new(prefPane, Rect(0, 0, 15, 15)).value_(true)
			});

			swFlow.shift(5, -2);

			StaticText(prefPane, Rect(5, 0, 90, 20))
				.font_(Font("Helvetica", 10))
				.stringColor_(Color.white)
				.string_("auto-connect OSC")
				.align_(\right)
			;

			swFlow.shift(5, 2);

			if(GUI.id === \cocoa, {
				autoConnectOSCRadio = Button(prefPane, Rect(0, 0, 15, 15))
					.font_(Font("Helvetica", 10))
					.states_([
						["", Color.red, Color.white],
						["X", Color.red, Color.white]
					])
					.value_(1)
				;
			}, {
				autoConnectOSCRadio = CheckBox(prefPane, Rect(0, 0, 15, 15)).value_(true)
			});

			window.onClose_({
				CVWidgetEditor.allEditors.pairsDo({ |editor, val|
					switch(cvWidgets[editor].class,
						CVWidgetKnob, {
							val.window.close;
						},
						CVWidget2D, {
							#[lo, hi].do({ |hilo|
								val[hilo] !? { val[hilo].window.close };
							})
						}
					)
				});
				tabProperties.do(_.nextPos_(0@0));
			});

			thisNextPos = 0@0;
			rowheight = widgetheight+1+15; // add a small gap between rows

			order = all.order;
			orderedCVs = all.atAll(order);

			order.do({ |k, i|
				if(cvWidgets[k].notNil and:{ cvWidgets[k].midiOscEnv.notNil }, {
					cvcArgs = ();
					cvcArgs.midiOscEnv = cvWidgets[k].midiOscEnv;
				}, {
					cvcArgs = true;
				});

				if(widgetStates.size < 1, { cvTabIndex = 0 }, {
					if(widgetStates[k].isNil, {
						cvTabIndex = 0;
					}, {
						cvTabIndex = widgetStates[k].tabIndex ?? { cvTabIndex = 0 };
					})
				});

				if(tabProperties[cvTabIndex].nextPos.isNil, {
					thisNextPos = 0@0;
				}, {
					thisNextPos = tabProperties[cvTabIndex].nextPos;
				});

				if(orderedCVs[i].class === Event and:{
					orderedCVs[i].keys.includesAny(#[lo, hi])
				}, {
					tmp = (
						lo: this.setup.calibrate = cvWidgets[k] !? {
							cvWidgets[k].wdgtControllersAndModels.lo.calibration.model.value
						},
						hi: this.setup.calibrate = cvWidgets[k] !? {
							cvWidgets[k].wdgtControllersAndModels.hi.calibration.model.value
						}
					);
					cvWidgets[k] !? { cvWidgets[k].wdgtActions !? { wdgtActions = cvWidgets[k].wdgtActions }};
					cvWidgets[k] = CVWidget2D(
						tabs.views[cvTabIndex],
						[orderedCVs[i].lo, orderedCVs[i].hi],
						k,
						Rect(thisNextPos.x, thisNextPos.y, widgetwidth = 105, widgetheight),
						setup: tmp,
						controllersAndModels: cvWidgets[k] !? {
							(lo: cvWidgets[k].wdgtControllersAndModels.lo, hi: cvWidgets[k].wdgtControllersAndModels.hi)
						},
						cvcGui: cvcArgs
					);
					removeButs.put(k,
						Button(tabs.views[cvTabIndex], Rect(thisNextPos.x, thisNextPos.y+widgetheight, widgetwidth, 15))
							.states_([["remove", Color.white, Color(0, 0.15)]])
							.action_({ |b| this.removeAt(k) })
							.font_(Font("Helvetica", 9))
						;
					);
					cvWidgets[k].bgColor_(tabProperties[cvTabIndex].tabColor);
					#[lo, hi].do({ |sl|
						[cvWidgets[k].midiHead[sl], cvWidgets[k].oscEditBut[sl]].do({ |b|
							{
								if(cvWidgets[k].wdgtControllersAndModels[sl].notNil and:{
									cvWidgets[k].wdgtControllersAndModels[sl].oscConnection.model.value !== false and:{
										b === cvWidgets[k].oscEditBut[sl]
									}
								}, {
									btnColor = Color.cyan(0.5);
								}, {
									btnColor = tabProperties[cvTabIndex].tabColor;
								});
								b.states_([
									[b.states[0][0], b.states[0][1], btnColor]
								])
							}.defer(0.1);
						})
					});
					wdgtActions !? { cvWidgets[k].wdgtActions = wdgtActions };
				}, {
					tmp = this.setup.calibrate = cvWidgets[k] !? {
						cvWidgets[k].wdgtControllersAndModels.calibration.model.value;
					};
					cvWidgets[k] !? { cvWidgets[k].wdgtActions !? { wdgtActions = cvWidgets[k].wdgtActions }};
					cvWidgets[k] = CVWidgetKnob(
						tabs.views[cvTabIndex],
						orderedCVs[i],
						k,
						Rect(thisNextPos.x, thisNextPos.y, widgetwidth = 52, widgetheight),
						setup: tmp,
						controllersAndModels: cvWidgets[k] !? { cvWidgets[k].wdgtControllersAndModels },
						cvcGui: cvcArgs
					);
					removeButs.put(k,
						Button(tabs.views[cvTabIndex], Rect(thisNextPos.x, thisNextPos.y+widgetheight, widgetwidth, 15))
							.states_([["remove", Color.white, Color(0.0, 0.15)]])
							.action_({ |b| this.removeAt(k) })
							.font_(Font("Helvetica", 9))
						;
					);
					widgetStates[k] !? { widgetStates[k].actions !? { cvWidgets[k].wdgtActions = widgetStates[k].actions }};
					cvWidgets[k].bgColor_(tabProperties[cvTabIndex].tabColor);
					[cvWidgets[k].midiHead, cvWidgets[k].oscEditBut].do({ |b|
						{
							if(cvWidgets[k].wdgtControllersAndModels.oscConnection.model.value !== false and:{
								b === cvWidgets[k].oscEditBut;
							}, {
								btnColor = Color.cyan(0.5);
							}, {
								btnColor = tabProperties[cvTabIndex].tabColor;
							});
							b.states_([
								[b.states[0][0], b.states[0][1], btnColor]
							])
						}.defer(0.1);
					});
					wdgtActions !? { cvWidgets[k].wdgtActions = wdgtActions };
				});

				cvWidgets[k].widgetBg.background_(tabProperties[cvTabIndex].tabColor);

				switch(cvWidgets[k].class,
					CVWidgetKnob, {
						cvWidgets[k].wdgtControllersAndModels.midiDisplay.model.value_(
							cvWidgets[k].wdgtControllersAndModels.midiDisplay.model.value
						).changedKeys(cvWidgets[k].synchKeys);
						cvWidgets[k].wdgtControllersAndModels.oscDisplay.model.value_(
							cvWidgets[k].wdgtControllersAndModels.oscDisplay.model.value
						).changedKeys(cvWidgets[k].synchKeys);
						cvWidgets[k].wdgtControllersAndModels.actions.model.value_((
							numActions: cvWidgets[k].wdgtActions.size,
							activeActions: cvWidgets[k].wdgtActions.select({ |v| v.asArray[0][1] == true }).size
						)).changedKeys(cvWidgets[k].synchKeys);
					},
					CVWidget2D, {
						#[lo, hi].do({ |hilo|
							cvWidgets[k].wdgtControllersAndModels[hilo].midiDisplay.model.value_(
								cvWidgets[k].wdgtControllersAndModels[hilo].midiDisplay.model.value
							).changedKeys(cvWidgets[k].synchKeys);
							cvWidgets[k].wdgtControllersAndModels[hilo].oscDisplay.model.value_(
								cvWidgets[k].wdgtControllersAndModels[hilo].oscDisplay.model.value
							).changedKeys(cvWidgets[k].synchKeys);
							cvWidgets[k].wdgtControllersAndModels[hilo].actions.model.value_((
								numActions: cvWidgets[k].wdgtActions[hilo].size,
								activeActions: cvWidgets[k].wdgtActions[hilo].select({ |v| v.asArray[0][1] == true }).size
							)).changedKeys(cvWidgets[k].synchKeys);
						})
					}
				);

				if(widgetStates[k].isNil, {
					widgetStates.put(k, (tabIndex: cvTabIndex));
				}, {
					widgetStates[k].tabIndex = cvTabIndex;
				});
				rowwidth = tabs.views[cvTabIndex].bounds.width-15;
				colwidth = widgetwidth+1; // add a small gap between widgets

				if(thisNextPos.x+colwidth > rowwidth, {
					// jump to next row
					tabProperties[cvTabIndex].nextPos = 0@(thisNextPos.y+rowheight);
				}, {
					// add next widget to the right
					tabProperties[cvTabIndex].nextPos = thisNextPos.x+colwidth@(thisNextPos.y);
				});
			});
			window.front;
		});
		window.front;

		skipJacks = SkipJack.all.collect({ |r| r === updateRoutine });
		if(skipJacks.includes(true).not, {
			updateRoutine = SkipJack({
				lastUpdate ?? { lastUpdate = all.size };
				lastSetUp !? {
					if(this.setup != lastSetUp, {
						this.prSetup(this.setup);
					})
				};
				if(all.size != lastUpdate, {
					if(all.size > lastUpdate and:{ cvWidgets.size <= lastUpdate }, {
						this.prAddToGui;
					});
					if(all.size < lastUpdate, {
						removedKeys = cvWidgets.keys.difference(all.keys);
						removedKeys.do({ |k|
							this.removeAt(k);
						});
						this.prRegroupWidgets(tabs.activeTab);
						tmp = tabs.getLabelAt(0);
					});
					lastUpdate = all.size;
				});
				try {
					if(window.bounds.width != lastUpdateWidth, {
						this.prRegroupWidgets(tabs.activeTab);
					})
				};
				lastUpdateWidth = window.bounds.width;
				lastSetUp = this.setup;
			}, 0.5, { window.isClosed }, "CVCenter-Updater");
		});
	}

	*put { |...args|
		var inputArgs, overwrite=false, tmp;
		inputArgs = args;
		if(inputArgs.size.odd, {
			overwrite = inputArgs.pop;
			if(overwrite.isKindOf(Boolean).not, {
				overwrite = nil;
			});
		});
		this.new;
		inputArgs.pairsDo({ |key, cv|
			if(cv.isKindOf(CV).not and:{ cv.isKindOf(Array).not }, {
				Error("CVCenter expects a single CV or an array of CVs as input!").throw;			});
			[cv].flat.do({ |cv|
				if(cv.isKindOf(CV).not, {
					Error("The value provided for key '"++key.asString++"' doesn't appear to be a CV.\nPlease choose a valid input!").throw;
				})
			});
			if(cv.isKindOf(Array) and:{ cv.size == 2 }, {
				tmp = cv.copy;
				#[lo, hi].do({ |key, i|
					cv.isKindOf(Event).not.if { cv = () };
					cv.put(key, tmp[i]);
				})
			});

			if(overwrite, {
				all.put(key.asSymbol, cv);
			}, {
				if(all.matchAt(key.asSymbol).isNil, {
					all.put(key.asSymbol, cv);
				}, {
					("There is already a CV stored under the name '"++key.asString++"'. \nPlease choose a different key-name!").warn;
				})
			})
		})
	}

	*removeAt { |key|
		var lastVal, thisKey, tabIndex;
		thisKey = key.asSymbol;
		all.removeAt(thisKey);
		cvWidgets[thisKey].class.switch(
			CVWidgetKnob, {
				if(cvWidgets[thisKey].editor.notNil and:{ cvWidgets[thisKey].editor.isClosed.not }, {
					cvWidgets[thisKey].editor.close;
				});
				cvWidgets[thisKey].midiOscEnv.cc !? {
					cvWidgets[thisKey].midiOscEnv.cc.remove;
					cvWidgets[thisKey].midiOscEnv.cc = nil;
				};
				cvWidgets[thisKey].midiOscEnv.oscResponder !? {
					cvWidgets[thisKey].midiOscEnv.oscResponder.remove;
					cvWidgets[thisKey].midiOscEnv.oscResponder = nil;
				};
			},
			CVWidget2D, {
				#[lo, hi].do({ |hilo|
					if(cvWidgets[thisKey].editor[hilo].notNil and:{ cvWidgets[thisKey].editor[hilo].isClosed.not }, {
						cvWidgets[thisKey].editor[hilo].close;
					});
					cvWidgets[thisKey].midiOscEnv[hilo].cc !? {
						cvWidgets[thisKey].midiOscEnv[hilo].cc.remove;
						cvWidgets[thisKey].midiOscEnv[hilo].cc = nil;
					};
					cvWidgets[thisKey].midiOscEnv[hilo].oscResponder !? {
						cvWidgets[thisKey].midiOscEnv[hilo].oscResponder.remove;
						cvWidgets[thisKey].midiOscEnv[hilo].oscResponder = nil;
					}
				})
			}
		);
		cvWidgets[thisKey].remove;
		cvWidgets.removeAt(key);
		removeButs[thisKey].remove;
		removeButs.removeAt(thisKey);
		if(window.notNil, {
			if(window.isClosed.not, {
				{ tabs.views.do({ |v, i| v.refresh; if(v.children.size == 0, { this.prRemoveTab(i) }) }) }.defer(0.1);
			}, {
				if(this.widgetsAtTab(tabs.getLabelAt(widgetStates[key].tabIndex)).size == 0, {
					if(tabProperties.size == 1, {
						tabProperties = [(tabLabel: "default", tabColor: tabProperties[0].tabColor)];
					}, {
						tabProperties.removeAt(widgetStates[key].tabIndex);
						widgetStates.do({ |w|
							if(w.tabIndex > widgetStates[key].tabIndex, { w.tabIndex = w.tabIndex-1 });
						})
					})
				})
			})
		});
		widgetStates.removeAt(thisKey);
	}

	*removeAll { |...keys|
		if(keys.size < 1, {
			all.keys.do(this.removeAt(_));
		}, {
			keys.do(this.removeAt(_));
		});
	}

	*removeAtTab { |label|
		var wdgts;
		wdgts = this.widgetsAtTab(label);
		this.removeAll(*wdgts);
	}

	*at { |key|
		^all.at(key.asSymbol);
	}

	*use { |key, spec, value, tab, slot|
		var thisKey, thisSpec, thisVal, thisSlot, thisTab, widget2DKey;

		key ?? { Error("You cannot use a CV in CVCenter without providing key").throw };
		thisKey = key.asSymbol;

		// if a 2D-widget under the given key exists force the given slot to become
		// the other slot of the already existing widget
		// also prevents misbehaviour in case of bogus slots
		if(cvWidgets.notNil and:{
			cvWidgets[thisKey].notNil and:{
				cvWidgets[thisKey].class == CVWidget2D
			}
		}, {
			block { |break|
				#[lo, hi].do({ |hilo|
					if(widgetStates[thisKey].notNil and:{
						widgetStates[thisKey][hilo].isNil
					}, { break.value(thisSlot = hilo) })
				})
			}
		});

		// above test didn't apply. so we can assume no widget exists under the given key
		if(slot.notNil and:{ thisSlot.isNil }, {
			thisSlot = slot.asString.toLower.asSymbol;
			if(#[lo, hi].detect({ |sbl| sbl === thisSlot }).class !== Symbol, {
				Error("Looks like you wanted to create a multi-dimensional widget. However, the given slot-value '%' is not valid!".format(slot)).throw;
			})
		});

		tab !? {
			if(widgetStates.notNil and:{
				widgetStates[thisKey].notNil and:{
					tabs.getLabelAt(widgetStates[thisKey].tabIndex) != tab.asString
				}
			}, {
				// force widget2D-slot to be created under the same tab as the already existing slot
				thisTab = tabs.getLabelAt(widgetStates[thisKey].tabIndex);
			}, {
				thisTab = tab;
			})
		};

		all ? this.new;

		thisSlot !? {
			widgetStates[thisKey] ?? { widgetStates.put(thisKey, ()) };
			widgetStates[thisKey][thisSlot] ?? { widgetStates[thisKey].put(thisSlot, ()) };
		};

		thisSpec = spec.asSpec;

		if(value.notNil and:{ value.isNumber }, {
			thisVal = value;
		}, {
			thisVal = thisSpec.default;
		});

		if(thisSlot.notNil and:{ widgetStates[thisKey][thisSlot][\made] != true }, {
			widgetStates[thisKey][thisSlot].made = true;
			if(thisSlot === \lo or: { thisSlot === \hi }, {
				all[thisKey] ?? { all.put(thisKey, (lo: CV.new, hi: CV.new)) };
				all[thisKey][thisSlot].spec_(thisSpec, thisVal);
				widget2DKey = (key: thisKey, slot: thisSlot, spec: thisSpec);
			})
		}, {
			all[thisKey] ?? { all.put(thisKey, CV.new(thisSpec, thisVal)) };
		});

		if(window.isNil or:{ window.isClosed }, {
			this.makeWindow(tab);
		}, {
			this.prAddToGui(thisTab, widget2DKey);
		});

		if(slot.notNil, {
			^all[thisKey][thisSlot];
		}, {
			^all[thisKey];
		})
	}

	*setup {
		^(
			midiMode: this.midiMode,
			midiResolution: this.midiResolution,
			midiMean: this.midiMean,
			ctrlButtonBank: this.ctrlButtonBank,
			softWithin: this.softWithin
		)
	}

	*guiMoveTo { |point|
		if(point.isKindOf(Point).not, {
			Error("guiMoveTo expects a Point in the form of e.g. 0@0").throw;
		});
		this.guix_(point.x);
		this.guiy_(point.y);
		window.bounds_(Rect(this.guix, this.guiy, this.guiwidth, this.guiheight));
	}

	*guiChangeDimensions { |point|
		if(point.isKindOf(Point).not, {
			Error("guiMoveTo expects a Point in the form of e.g. 0@0").throw;
		});
		this.guiwidth_(point.x);
		this.guiheight_(point.y);
		window.bounds_(Rect(this.guix, this.guiy, this.guiwidth, this.guiheight));
	}

	*bounds {
		^window.bounds;
	}

	*bounds_ { |rect|
		this.guix_(rect.left);
		this.guiy_(rect.top);
		this.guiwidth_(rect.width);
		this.guiheight_(rect.height);
		window.bounds_(rect);
	}

	*renameTab { |oldName, newName|
		var index;
		index = tabs.views.detectIndex({ |view, i| tabs.getLabelAt(i) == oldName.asString; });
		tabs.setLabelAt(index, newName.asString);
		tabProperties[index].tabLabel = newName.asString;
	}

	*addActionAt { |key, name, action, slot, active=true|
		key ?? { Error("You have to provide the CV's key in order to add an action!").throw };
		cvWidgets[key.asSymbol].addAction(name, action, slot, active);
	}

	*removeActionAt { |key, name, slot|
		key ?? { Error("You have to provide the CV's key in order to remove an action!").throw };
		cvWidgets[key.asSymbol].removeAction(name, slot);
	}

	*activateActionAt { |key, name, activate, slot|
		key ?? { Error("You have to provide the CV's key in order to activate or deactivate an action!").throw };
		cvWidgets[key.asSymbol].activateAction(name, activate, slot);
	}

	*widgetsAtTab { |label|
		var index, wdgts = [];
		index = tabProperties.detectIndex({ |t, i| t.tabLabel.asSymbol === label.asSymbol });
		all.keys.do({ |key|
			if(widgetStates[key].tabIndex == index, { wdgts = wdgts.add(key) });
		});
		^wdgts;
	}

	*saveSetup { |path|
		var lib, midiOscEnvs = (), successFunc;
		successFunc = { |f|
			lib = Library();
			lib.put( \all, ());
			all.pairsDo({ |k, cv|
				lib[\all].put(k, ());
				switch(cvWidgets[k].class,
					CVWidget2D, {
						lib[\all][k].wdgtClass = CVWidget2D;
						#[lo, hi].do({ |hilo|
							lib[\all][k][hilo] = (
								spec: cvWidgets[k].widgetCV[hilo].spec,
								val: cvWidgets[k].widgetCV[hilo].value,
								actions: cvWidgets[k].wdgtActions !? { cvWidgets[k].wdgtActions[hilo] },
								osc: (
									addr: cvWidgets[k].midiOscEnv[hilo].oscResponder !? {
										cvWidgets[k].midiOscEnv[hilo].oscResponder.addr
									},
									cmdName: cvWidgets[k].midiOscEnv[hilo].oscResponder !? {
										cvWidgets[k].midiOscEnv[hilo].oscResponder.cmdName
									},
									msgIndex: cvWidgets[k].midiOscEnv[hilo].oscMsgIndex,
									calibConstraints: cvWidgets[k].getOscInputConstraints(hilo),
									oscMapping: cvWidgets[k].getOscMapping(hilo)
								),
								midi: (
									src: cvWidgets[k].midiOscEnv[hilo].midisrc,
									chan: cvWidgets[k].midiOscEnv[hilo].midichan,
									num: cvWidgets[k].midiOscEnv[hilo].midiRawNum,
									midiMode: cvWidgets[k].getMidiMode(hilo),
									midiMean: cvWidgets[k].getMidiMean(hilo),
									softWithin: cvWidgets[k].getSoftWithin(hilo),
									midiResolution: cvWidgets[k].getMidiResolution(hilo),
									ctrlButtonBank: cvWidgets[k].getCtrlButtonBank(hilo)
								)
							)
						})
					},
					CVWidgetKnob, {
						lib[\all][k] = (
							spec: cvWidgets[k].widgetCV.spec,
							val: cvWidgets[k].widgetCV.value,
							actions: cvWidgets[k].wdgtActions,
							osc: (
								addr: cvWidgets[k].midiOscEnv.oscResponder !? {
									cvWidgets[k].midiOscEnv.oscResponder.addr
								},
								cmdName: cvWidgets[k].midiOscEnv.oscResponder !? {
									cvWidgets[k].midiOscEnv.oscResponder.cmdName
								},
								msgIndex: cvWidgets[k].midiOscEnv.oscMsgIndex,
								calibConstraints: cvWidgets[k].getOscInputConstraints,
								oscMapping: cvWidgets[k].getOscMapping
							),
							midi: (
								src: cvWidgets[k].midiOscEnv.midisrc,
								chan: cvWidgets[k].midiOscEnv.midichan,
								num: cvWidgets[k].midiOscEnv.midiRawNum,
								midiMode: cvWidgets[k].getMidiMode,
								midiMean: cvWidgets[k].getMidiMean,
								softWithin: cvWidgets[k].getSoftWithin,
								midiResolution: cvWidgets[k].getMidiResolution,
								ctrlButtonBank: cvWidgets[k].getCtrlButtonBank
							),
							wdgtClass: CVWidgetKnob
						)
					}
				);
				lib[\all][k].tabLabel = tabProperties[widgetStates[k].tabIndex].tabLabel;
			});

			if(GUI.id === \cocoa, {
				lib.writeTextArchive(*f);
			}, {
				lib.writeTextArchive(f);
			});
			lib = nil;
		};
		if(path.isNil, {
			if(GUI.id !== \qt, {
				File.saveDialog(
					prompt: "Save your current setup to a file",
					defaultName: "Setup",
					successFunc: successFunc
				)
			}, { QDialog.savePanel(successFunc) })
		}, {
			successFunc.(path);
		});
	}

	*loadSetup { |path, addToExisting=false, autoConnectOSC=true, autoConnectMIDI=true, loadActions=true|
		var lib, midiOscEnvs, successFunc;

		successFunc = { |f|
			if(GUI.id === \qt, {
				lib = Library.readTextArchive(*f);
			}, {
				lib = Library.readTextArchive(f);
			});
			all !? {
				if(addToExisting === false, {
					this.removeAll;
				})
			};
			lib[\all].pairsDo({ |key, v|
				switch(v.wdgtClass,
					CVWidget2D, {
						#[lo, hi].do({ |hilo|
							this.use(key, v[hilo].spec, v[hilo].val, v.tabLabel, hilo);
							cvWidgets[key].setMidiMode(v[hilo].midi.midiMode, hilo)
								.setMidiMean(v[hilo].midi.midiMean, hilo)
								.setSoftWithin(v[hilo].midi.softWithin, hilo)
								.setMidiResolution(v[hilo].midi.midiResolution, hilo)
								.setCtrlButtonBank(v[hilo].midi.ctrlButtonBank, hilo)
							;
							if(loadActions, {
								v[hilo].actions !? {
									v[hilo].actions.pairsDo({ |ak, av|
										this.addActionAt(key, ak, av.asArray[0][0], hilo, av.asArray[0][1]);
									})
								}
							});
							if(autoConnectOSC, {
								if(v[hilo].osc.notNil and:{ v[hilo].osc.cmdName.notNil }, {
									cvWidgets[key].oscConnect(
										v[hilo].osc.addr !? { v[hilo].osc.addr.ip },
										v[hilo].osc.addr !? { v[hilo].osc.addr.port },
										v[hilo].osc.cmdName,
										v[hilo].osc.msgIndex,
										hilo
									);
									cvWidgets[key].setOscInputConstraints(
										v[hilo].osc.calibConstraints.lo @ v[hilo].osc.calibConstraints.hi, hilo
									);
									cvWidgets[key].wdgtControllersAndModels[hilo].oscInputRange.model.value_(
										[v[hilo].osc.calibConstraints.lo, v[hilo].osc.calibConstraints.hi]
									).changedKeys(cvWidgets[key].synchKeys);
									cvWidgets[key].setOscMapping(v[hilo].osc.oscMapping, hilo)
								})
							});
							if(autoConnectMIDI, {
								if(v[hilo].midi.notNil and:{ v[hilo].midi.num.notNil }, {
									try {
										cvWidgets[key].midiConnect(
											// v[hilo].midi.src,
											nil,
											v[hilo].midi.chan,
											v[hilo].midi.num,
											hilo
										)
									}
								})
							})
						})
					},
					CVWidgetKnob, {
						this.use(key, v.spec, v.val, v.tabLabel);
						cvWidgets[key].setMidiMode(v.midi.midiMode)
							.setMidiMean(v.midi.midiMean)
							.setSoftWithin(v.midi.softWithin)
							.setMidiResolution(v.midi.midiResolution)
							.setCtrlButtonBank(v.midi.ctrlButtonBank)
						;
						if(loadActions, {
							v.actions !? {
								v.actions.pairsDo({ |ak, av|
									this.addActionAt(key, ak, av.asArray[0][0], active: av.asArray[0][1]);
								})
							}
						});
						if(autoConnectOSC, {
							v.osc.cmdName !? {
								cvWidgets[key].oscConnect(
									v.osc.addr !? { v.osc.addr.ip },
									v.osc.addr !? { v.osc.addr.port },
									v.osc.cmdName,
									v.osc.msgIndex
								);
								cvWidgets[key].setOscInputConstraints(
									v.osc.calibConstraints.lo @ v.osc.calibConstraints.hi
								);
								cvWidgets[key].wdgtControllersAndModels.oscInputRange.model.value_(
									[v.osc.calibConstraints.lo, v.osc.calibConstraints.hi]
								).changedKeys(cvWidgets[key].synchKeys);
								cvWidgets[key].setOscMapping(v.osc.oscMapping)
							}
						});
						if(autoConnectMIDI, {
							v.midi.num !? {
								try {
									cvWidgets[key].midiConnect(
										// v.midi.src,
										nil,
										v.midi.chan,
										v.midi.num
									)
								}
							}
						})
					}
				)
			});
		};

		if(path.isNil, {
			if(GUI.id === \qt, {
				QDialog.getPaths(successFunc, allowsMultiple: false);
			}, {
				File.openDialog(
					prompt: "Please choose a setup",
					successFunc: successFunc
				)
			})
		}, {
			successFunc.(path);
		})
	}

	// private Methods - not to be used directly

	*prSetup { |setupDict|
		setupDict[\midiMode] !? { this.midiMode_(setupDict[\midiMode]) };
		setupDict[\midiResolution] !? { this.midiResolution_(setupDict[\midiResolution]) };
		setupDict[\midiMean] !? { this.midiMean_(setupDict[\midiMean]) };
		this.ctrlButtonBank_(setupDict[\ctrlButtonBank]);
//		setupDict[\ctrlButtonBank] !? { this.ctrlButtonBank_(setupDict[\ctrlButtonBank]) };
		setupDict[\softWithin] !? { this.softWithin_(setupDict[\softWithin]) };
		if(window.notNil and:{ window.notClosed }, {
			cvWidgets.pairsDo({ |k, wdgt|
				switch(wdgt.class,
					CVWidgetKnob, {
						this.midiMode !? { wdgt.setMidiMode(this.midiMode) };
						this.midiResolution !? { wdgt.setMidiResolution(this.midiResolution) };
						this.midiMean !? { wdgt.setMidiMean(this.midiMean) };
						this.ctrlButtonBank !? { wdgt.setCtrlButtonBank(this.ctrlButtonBank) };
						this.softWithin !? { wdgt.setSoftWithin(this.softWithin) };
					},
					CVWidget2D, {
						#[lo, hi].do({ |hilo|
							this.midiMode !? { wdgt.setMidiMode(this.midiMode, hilo) };
							this.midiResolution !? { wdgt.setMidiResolution(this.midiResolution, hilo) };
							this.midiMean !? { wdgt.setMidiMean(this.midiMean, hilo) };
							this.ctrlButtonBank !? { wdgt.setCtrlButtonBank(this.ctrlButtonBank, hilo) };
							this.softWithin !? { wdgt.setSoftWithin(this.softWithin, hilo) };
						});
					}
				);
			})
		})
	}

	*prAddToGui { |tab, widget2DKey|
		var allCVKeys, widgetKeys, thisKeys;
		var rowwidth, colcount;
		var cvTabIndex, tabLabels;
		var thisNextPos;
		var cvcArgs, btnColor;
		var tmp;

		tabLabels = tabProperties.collect({ |tab| tab.tabLabel.asSymbol });

		if(tab.notNil, {
			if(tabLabels.includes(tab.asSymbol), {
				cvTabIndex = tabLabels.indexOf(tab.asSymbol);
			}, {
				if(tabs.views.size == 1 and: { tabs.views[0].children.size == 0 }, {
					cvTabIndex = 0;
					this.renameTab(tabs.getLabelAt(0), tab.asString);
				}, {
					tabs.add(tab).keyDownAction_({ |view, char, modifiers, unicode, keycode|
//						[view, char, modifiers, unicode, keycode].postcs;
						switch(keycode,
							16r1000014, { tabs.focus((tabs.activeTab+1).wrap(0, tabs.views.size-1)) },
							16r1000012, { tabs.focus((tabs.activeTab-1).wrap(0, tabs.views.size-1)) },
							// when and why have the keycodes been changed??
							124, { tabs.focus((tabs.activeTab+1).wrap(0, tabs.views.size-1)) },
							123, { tabs.focus((tabs.activeTab-1).wrap(0, tabs.views.size-1)) }
						);
						switch(unicode,
							111, { CVCenterControllersMonitor(1) }, // key "o" -> osc
							109, { CVCenterControllersMonitor(0) }, // key "m" -> midi
							120, { CVCenterControllersMonitor.window.close } // key "x" -> close window
						);
						if((48..57).includes(unicode), { tabs.views[unicode-48] !? { tabs.focus(unicode-48) }})
					});
					cvTabIndex = tabLabels.size;
					tabProperties = tabProperties.add((tabLabel: tab.asString, tabColor: nextColor.next));
				})
			})
		}, {
			cvTabIndex = tabs.activeTab;
		});

		tabs.labelColors_(tabProperties.collect(_.tabColor));
		tabs.unfocusedColors_(tabProperties.collect({ |t| t.tabColor.copy.alpha_(0.8) }));

		if(tabProperties[cvTabIndex].nextPos.notNil, {
			thisNextPos = tabProperties[cvTabIndex].nextPos;
		}, {
			thisNextPos = 0@0;
		});

		rowheight = widgetheight+1+15; // add a small gap between rows

		allCVKeys = all.keys;
		widgetKeys = cvWidgets.keys;
		thisKeys = allCVKeys.difference(widgetKeys);

		thisKeys.do({ |k|
			if(widgetStates[k].notNil and:{ widgetStates[k].midiOscEnv.notNil }, {
				cvcArgs = ();
				cvcArgs.midiOscEnv = widgetStates[k].midiOscEnv;
			}, {
				cvcArgs = true;
			});
			if(all[k].class === Event and:{
				all[k].keys.includesAny(#[hi, lo])
			}, {
				tmp = (
					lo: this.setup.calibrate = cvWidgets[k] !? {
						cvWidgets[k].wdgtControllersAndModels.lo.calibration.model.value
					},
					hi: this.setup.calibrate = cvWidgets[k] !? {
						cvWidgets[k].wdgtControllersAndModels.hi.calibration.model.value
					},
					wdgtActions: cvWidgets[k] !? { cvWidgets[k].wdgtActions !? { cvWidgets[k].wdgtActions }};
				);
				cvWidgets[k] = CVWidget2D(
					tabs.views[cvTabIndex],
					[all[k].lo, all[k].hi],
					k,
					Rect(thisNextPos.x, thisNextPos.y, widgetwidth = 105, widgetheight),
					setup: tmp,
					controllersAndModels: cvWidgets[k] !? {
						(lo: cvWidgets[k].wdgtControllersAndModels.lo, hi: cvWidgets[k].wdgtControllersAndModels.hi)
					},
					cvcGui: cvcArgs
				);
				removeButs.put(k,
					Button(tabs.views[cvTabIndex], Rect(thisNextPos.x, thisNextPos.y+widgetheight, widgetwidth, 15))
						.states_([["remove", Color.white, Color(0.0, 0.15)]])
						.action_({ |b| this.removeAt(k) })
						.font_(Font("Helvetica", 9))
					;
				);
				if(widgetStates[k].isNil, {
					widgetStates.put(k, (tabIndex: cvTabIndex));
				}, {
					widgetStates[k].tabIndex = cvTabIndex;
				});
				cvWidgets[k].bgColor_(tabProperties[cvTabIndex].tabColor);
				#[lo, hi].do({ |sl|
					[cvWidgets[k].midiHead[sl], cvWidgets[k].oscEditBut[sl]].do({ |b|
						{
							if(cvWidgets[k].wdgtControllersAndModels[sl].notNil and:{
								cvWidgets[k].wdgtControllersAndModels[sl].oscConnection.model.value !== false and:{
									b === cvWidgets[k].oscEditBut[sl]
								}
							}, {
								btnColor = Color.cyan(0.5);
							}, {
								btnColor = tabProperties[cvTabIndex].tabColor;
							});
							b.states_([
								[b.states[0][0], b.states[0][1], btnColor]
							])
						}.defer(0.1);
					})
				});
				tmp.wdgtActions !? { cvWidgets[k].wdgtActions = tmp.wdgtActions };
			}, {
				tmp = this.setup.calibrate = cvWidgets[k] !? {
					cvWidgets[k].wdgtControllersAndModels.calibration.model.value
				};
				cvWidgets[k] = CVWidgetKnob(
					tabs.views[cvTabIndex],
					all[k],
					k,
					Rect(thisNextPos.x, thisNextPos.y, widgetwidth = 52, widgetheight),
					setup: tmp,
					controllersAndModels: cvWidgets[k] !? { cvWidgets[k].wdgtControllersAndModels },
					cvcGui: cvcArgs
				);
				removeButs.put(k,
					Button(tabs.views[cvTabIndex], Rect(thisNextPos.x, thisNextPos.y+widgetheight, widgetwidth, 15))
						.states_([["remove", Color.white, Color(0.0, 0.15)]])
						.action_({ |b| this.removeAt(k) })
						.font_(Font("Helvetica", 9))
					;
				);
				if(widgetStates[k].isNil, {
					widgetStates.put(k, (tabIndex: cvTabIndex));
				}, {
					widgetStates[k].tabIndex = cvTabIndex;
				});
				cvWidgets[k].widgetCV !? { cvWidgets[k].widgetCV.value_(cvWidgets[k].widgetCV.value) };
				widgetStates[k] !? { widgetStates[k].actions !? { cvWidgets[k].wdgtActions = widgetStates[k].actions }};
				cvWidgets[k].bgColor_(tabProperties[cvTabIndex].tabColor);
				[cvWidgets[k].midiHead, cvWidgets[k].oscEditBut].do({ |b|
					{
						if(cvWidgets[k].wdgtControllersAndModels.oscConnection.model.value !== false and:{
							b === cvWidgets[k].oscEditBut;
						}, {
							btnColor = Color.cyan(0.5);
						}, {
							btnColor = tabProperties[cvTabIndex].tabColor;
						});
						b.states_([
							[b.states[0][0], b.states[0][1], btnColor]
						])
					}.defer(0.1);
				})
			});
			cvWidgets[k].widgetBg.background_(tabProperties[cvTabIndex].tabColor);
			colwidth = widgetwidth+1; // add a small gap between widgets
			rowwidth = tabs.views[cvTabIndex].bounds.width-15;
			if(thisNextPos.x+colwidth >= (rowwidth-colwidth-15), {
				// jump to next row
				tabProperties[cvTabIndex].nextPos = thisNextPos = 0@(thisNextPos.y+rowheight);
			}, {
				// add next widget to the right
				tabProperties[cvTabIndex].nextPos = thisNextPos = thisNextPos.x+colwidth@(thisNextPos.y);
			});
			tabs.focusActions_(Array.fill(tabs.views.size, {{ this.prRegroupWidgets(tabs.activeTab) }}));
			tabs.focus(cvTabIndex);
		});

		widget2DKey !? {
			cvWidgets[widget2DKey.key].setSpec(widget2DKey.spec, widget2DKey.slot);
		};
		this.prRegroupWidgets(cvTabIndex);
		window.front;
	}

	*prRegroupWidgets { |tabIndex|
		var rowwidth, rowheight, colcount, colwidth, thisNextPos, order, orderedWidgets, orderedRemoveButs;
		var widgetwidth, widgetheight=181;

		rowheight = widgetheight+1+15;
		thisNextPos = 0@0;

		tabIndex !? {
			order = cvWidgets.order;
			orderedWidgets = cvWidgets.atAll(order);
			orderedRemoveButs = removeButs.atAll(order);
			order.do({ |k, i|
				if(widgetStates[k].notNil and:{ tabIndex == widgetStates[k].tabIndex }, {
					if(thisNextPos != (0@0), {
						thisNextPos = tabProperties[widgetStates[k].tabIndex].nextPos;
					});
					orderedWidgets[i].widgetXY_(thisNextPos);
					orderedRemoveButs[i].bounds_(Rect(
						thisNextPos.x,
						thisNextPos.y+widgetheight,
						orderedRemoveButs[i].bounds.width,
						orderedRemoveButs[i].bounds.height
					));
					colwidth = orderedWidgets[i].widgetProps.x+1; // add a small gap to the right
					rowwidth = tabs.views[widgetStates[k].tabIndex].bounds.width/*-15*/;
					if(thisNextPos.x+colwidth >= (rowwidth-colwidth/*-15*/), {
						// jump to next row
						tabProperties[widgetStates[k].tabIndex].nextPos = thisNextPos = 0@(thisNextPos.y+rowheight);
					}, {
						// add next widget to the right
						tabProperties[widgetStates[k].tabIndex].nextPos = thisNextPos = thisNextPos.x+colwidth@(thisNextPos.y);
					})
				})
			})
		}
	}

	*prRemoveTab { |index|
		if(tabs.views.size > 1, {
			if(window.isClosed.not, { tabs.removeAt(index) });
			tabProperties.removeAt(index);
			widgetStates.do({ |w| if(w.tabIndex > index, { w.tabIndex = w.tabIndex-1 }) });
		}, {
			if(window.isClosed.not and:{ tabs.getLabelAt(index) != "default" }, { tabs.setLabelAt(index, "default") });
			tabProperties = [(tabLabel: "default", tabColor: tabProperties[index].tabColor)];
		})
	}

	/* utilities */

	*finishGui { |obj, ctrlName, environment, more|
		var interpreterVars, varNames = [], envs = [], thisSpec;
		var pSpaces = [], proxySpace;
		var activate = true;
		var actionName = "default";
		var wms;

//		[obj, ctrlName, environment, more].postln;

		interpreterVars = #[a,b,c,d,e,f,g,h,i,j,k,l,m,n,p,q,r,s,t,u,v,w,x,y,z];

		varNames = varNames ++ interpreterVars.select({ |n|
			thisProcess.interpreter.perform(n) === obj;
		});
		if(currentEnvironment.class !== ProxySpace, {
			currentEnvironment.pairsDo({ |k, v|
				if(v === obj, { varNames = varNames.add("~"++(k.asString)) });
			})
		});

		switch(obj.class,
			Synth, {
				environment !? {
					envs = interpreterVars.select({ |n|
						thisProcess.interpreter.perform(n) === environment;
					});
					currentEnvironment.pairsDo({ |k, v|
						if(v === environment, { envs = envs.add("~"++(k.asString)) });
					});
					environment.pairsDo({ |k, v|
						if(v === obj, {
							envs = envs.collect({ |ev| ev = ev++"['"++k++"']" });
						})
					})
				};
				varNames = varNames++envs;
			},
			NodeProxy, {
				// the NodeProxy passed in could be part of a ProxySpace
				if(varNames.size < 1, {
					pSpaces = pSpaces ++ interpreterVars.select({ |n|
						thisProcess.interpreter.perform(n).class === ProxySpace;
					});
					if(currentEnvironment.class !== ProxySpace, {
						currentEnvironment.pairsDo({ |k, v|
							if(v.class === ProxySpace, { pSpaces = pSpaces.add("~"++k) });
						})
					});
					pSpaces.do({ |p|
						if(p.class === Symbol, {
							proxySpace = thisProcess.interpreter.perform(p);
						});
						if(p.class === String, {
							proxySpace = p.interpret;
						});
						if(proxySpace.respondsTo(\envir), {
							proxySpace.envir.pairsDo({ |k, v|
								if(v === obj, {
									varNames = varNames.add(p.asString++"['"++k++"']");
								})
							})
						})
					})
				})

			},
			Ndef, {
				varNames = varNames.add(obj.asString);
			}
		);

		if(more.specEnterText.notNil and:{
			more.specEnterText.interpret.asSpec.isKindOf(ControlSpec)
		}, {
			thisSpec = more.specEnterText.interpret.asSpec;
		}, {
			thisSpec = more.specSelect;
		});

//		"pSpaces: %\n".postf(pSpaces);
//		"varNames: %\n".postf(varNames);
//		"more: %\n".postf(more);

		if(more.type.notNil, {
			if(more.type === \w2d or:{ more.type === \w2dc }, {
				#[lo, hi].do({ |slot, i|
					this.use(more.cName, thisSpec, more.slots[i], more.enterTab, slot);
					varNames.do({ |v, j|
						actionName = "default"++(j+1);
						if(j == 0, { activate = true }, { activate = false });
						switch(more.type,
							\w2d, {
								if(slot === \lo, {
									wms = "cv.value, CVCenter.at('"++more.cName++"').hi.value";
								}, {
									wms = "CVCenter.at('"++more.cName++"').lo.value, cv.value";
								});
								this.addActionAt(more.cName, actionName, "{ |cv|"+v+"!? {"+v++".setn('"++ctrlName++"', ["++wms++"]) }}", slot, activate);
							},
							\w2dc, {
								this.addActionAt(more.cName, actionName, "{ |cv|"+v+"!? {"+v++".set('"++more.controls[i]++"', cv.value) }}", slot, activate);
							}
						)
					})
				})
			}, {
				if(more.type === \wms, {
					more.slots.do({ |sl, i|
						this.use(more.cName.asString++(i+1), thisSpec, sl, more.enterTab);
						wms = [];
						more.slots.size.do({ |j|
							if(this.at((more.cName.asString++(j+1)).asSymbol) === this.at((more.cName.asString++(i+1)).asSymbol), {
								wms = wms.add("cv.value");
							}, {
								wms = wms.add("CVCenter.at('"++more.cName.asString++(j+1)++"').value")
							})
						});
						varNames.do({ |v, j|
							actionName = "default"++(j+1);
							if(j == 0, { activate = true }, { activate = false });
							this.addActionAt(more.cName.asString++(i+1), actionName, "{ |cv|"+v+"!? {"+v++".setn('"++ctrlName++"', ["++(wms.join(", "))++"]) }}", active: activate);
						})
					});
				})
			})
		}, {
			this.use(more.cName, thisSpec, more.slots[0], more.enterTab);
			varNames.do({ |v, j|
				actionName = "default"++(j+1);
				if(j == 0, { activate = true }, { activate = false });
				this.addActionAt(more.cName, actionName, "{ |cv|"+v+"!? {"+v++".set('"++ctrlName++"', cv.value) }}", active: activate);
			})
		});

		^obj;
	}

}