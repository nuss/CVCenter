
CVCenter {

	classvar <all, <nextCVKey, <cvWidgets, <window, <tabs, <switchBoard;
	classvar <>midiMode, <>midiResolution, <>ctrlButtonBank, <>midiMean, <>softWithin, <numccs;
	classvar <>guix, <>guiy, <>guiwidth, <>guiheight; 
	classvar <controlButtons, <nextButtonPos;
	classvar currentButtonStates, guiClosed = false/*, buttonProps*/;
	classvar <widgetStates;
	classvar <tabProperties, colors, nextColor;
	classvar controllersAndModels;
	
	*new { |cvs...setUpArgs|
		var r, g, b;

		if(all.isNil, {
			all = IdentityDictionary.new;
			cvWidgets = IdentityDictionary.new;
			widgetStates = IdentityDictionary.new;
			r = g = b = (0.5, 0.55 .. 0.7);
			colors = List();
			tabProperties = [];
			
			if(setUpArgs.size > 0, { 
				this.prSetup(*setUpArgs) 
			});
			
			r.do({ |red|
				g.do({ |green|
					b.do({ |blue|
						colors.add(Color(red, green, blue, 1.0));
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
							all.put(k.asSymbol, v);
						}, {
							"Your given key-name matches the reserved names for new keys in the CVCenter. Please choose a different name.".warn;
						})
					});
//					("CVs added so far:"+all).postln;
				})
			})
		})
	}
		
	*gui { |tab...cvs|
		var flow, rowwidth, colcount;
		var cvTabIndex, order, orderedCVs;
		var updateRoutine, lastUpdate, lastUpdateWidth, lastSetUp, lastCtrlBtnBank, removedKeys, skipJacks;
		var lastCtrlBtnsMode, swFlow;
		var thisNextPos, tabLabels, labelColors, unfocusedColors;
		var widgetwidth, widgetheight=166, colwidth, rowheight;
		var funcToAdd;
		var widgetControllersAndModels, cvcArgs;
		var tmp;
		
//		"should add to tab %\n".postf(tab);
			
		cvs !? { this.put(*cvs) };
		
		this.guix ?? { this.guix_(0) };
		this.guiy ?? { this.guiy_(0) };
		this.guiwidth ?? { this.guiwidth_(500) };
		this.guiheight ?? { this.guiheight_(250) };
		
		if(window.isNil or:{ window.isClosed }, {
			window = Window("CVCenter", Rect(this.guix, this.guiy, this.guiwidth, this.guiheight));
			if(Quarks.isInstalled("wslib"), { window.background_(Color.black) });
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
			});
			
			tabLabels = tabProperties.collect(_.tabLabel);
			labelColors = tabProperties.collect(_.tabColor);
			unfocusedColors = tabProperties.collect({ |t| t.tabColor.copy.alpha_(0.8) });
			
			tabs = TabbedView(
				window,
				Rect(0, 0, flow.bounds.width, flow.bounds.height-60), 
				labels: tabLabels, 
				scroll: true
			);
			tabs.view.resize_(5);
			tabs.labelColors_(labelColors);
			tabs.labelPadding_(5);
			tabs.unfocusedColors_(unfocusedColors);
			tabs.font_(GUI.font.new("Helvetica", 10));
			tabs.tabCurve_(3);
			tabs.stringColor_(Color.black);
			tabs.stringFocusedColor_(Color(0.0, 0.0, 0.5, 1.0));
			tabs.focusActions_(Array.fill(tabs.views.size, {{ this.prRegroupWidgets(tabs.activeTab) }}));
			
			flow.shift(0, 0);
			
			switchBoard = ScrollView(window, Rect(0, 0, flow.bounds.width, 50));
			switchBoard.decorator = swFlow = FlowLayout(switchBoard.bounds, 0@0, 0@0);
			switchBoard.resize_(8);

			window.onClose_({
				CVWidgetEditor.allEditors.pairsDo({ |editor, val| 
					switch(cvWidgets[editor].class, 
						CVWidgetKnob, {
							val.window.close;
						},
						CVWidget2D, {
							[\lo, \hi].do({ |hilo|
								val[hilo] !? { val[hilo].window.close };
							})
						}
					)
				});
				tabProperties.do(_.nextPos_(0@0));
				controlButtons = nil;
				nextButtonPos = 0@0;
				guiClosed = true;
			});

			thisNextPos = 0@0;
			rowheight = widgetheight+1; // add a small gap between rows
			
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
					orderedCVs[i].keys.includesAny([\lo, \hi])
				}, {
					cvWidgets[k] = CVWidget2D(
						tabs.views[cvTabIndex], 
						[orderedCVs[i].lo, orderedCVs[i].hi], 
						k, 
						Rect(thisNextPos.x, thisNextPos.y, widgetwidth = 105, widgetheight), 
						setup: (lo: this.setup, hi: this.setup), 
						controllersAndModels: cvWidgets[k] !? { 
							(lo: cvWidgets[k].wdgtControllersAndModels.lo, hi: cvWidgets[k].wdgtControllersAndModels.hi) 
						},
						cvcGui: cvcArgs
					)
				}, {
					cvWidgets[k] = CVWidgetKnob(
						tabs.views[cvTabIndex], 
						orderedCVs[i], 
						k, 
						Rect(thisNextPos.x, thisNextPos.y, widgetwidth = 52, widgetheight),
						setup: this.setup,
						controllersAndModels: cvWidgets[k] !? { cvWidgets[k].wdgtControllersAndModels },
						cvcGui: cvcArgs
					);
					
				});
				
				cvWidgets[k].widgetBg.background_(tabProperties[cvTabIndex].tabColor);

				switch(cvWidgets[k].class,
					CVWidgetKnob, {
						cvWidgets[k].wdgtControllersAndModels.midiDisplay.model.value_(
							cvWidgets[k].wdgtControllersAndModels.midiDisplay.model.value
						).changed(\value);
						cvWidgets[k].wdgtControllersAndModels.oscDisplay.model.value_(
							cvWidgets[k].wdgtControllersAndModels.oscDisplay.model.value
						).changed(\value);
					},
					CVWidget2D, {
						[\lo, \hi].do({ |hilo|
							cvWidgets[k].wdgtControllersAndModels[hilo].midiDisplay.model.value_(
								cvWidgets[k].wdgtControllersAndModels[hilo].midiDisplay.model.value
							).changed(\value);
							cvWidgets[k].wdgtControllersAndModels[hilo].oscDisplay.model.value_(
								cvWidgets[k].wdgtControllersAndModels[hilo].oscDisplay.model.value
							).changed(\value);
						})
					}
				);

				widgetStates.put(k, (tabIndex: cvTabIndex));
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

		skipJacks = SkipJack.all.collect({ |r| r.name == "CVCenter-Updater" });
		if(skipJacks.includes(true).not, {
			updateRoutine = SkipJack({
				lastUpdate ?? { lastUpdate = all.size };
				lastSetUp !? {
					if(this.setup != lastSetUp, {
//						("now updating widget-setups:"+[this.setup, lastSetUp]).postln;
						this.prSetup(*this.setup);
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
				[\lo, \hi].do({ |key, i|
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
		var lastVal, tabIndex;
		lastVal = all.at(key.asSymbol).value;
		all.removeAt(key.asSymbol);
		cvWidgets[key].class.switch(
			CVWidgetKnob, { 
				if(cvWidgets[key].editor.notNil and:{ cvWidgets[key].editor.isClosed.not }, {
					cvWidgets[key].editor.close;
				});
				cvWidgets[key].midiOscEnv.cc !? { 
					cvWidgets[key].midiOscEnv.cc.remove; 
					cvWidgets[key].midiOscEnv.cc = nil;
				};
				cvWidgets[key].midiOscEnv.oscResponder !? { 
					cvWidgets[key].midiOscEnv.oscResponder.remove;
					cvWidgets[key].midiOscEnv.oscResponder = nil;
				};
			},
			CVWidget2D, {
				[\lo, \hi].do({ |hilo|
					if(cvWidgets[key].editor[hilo].notNil and:{ cvWidgets[key].editor.isClosed.not }, {
						cvWidgets[key].editor[hilo].close;
					});
					cvWidgets[key].midiOscEnv[hilo].cc !? { 
						cvWidgets[key].midiOscEnv[hilo].cc.remove; 
						cvWidgets[key].midiOscEnv[hilo].cc = nil;
					};
					cvWidgets[key].midiOscEnv[hilo].oscResponder !? { 
						cvWidgets[key].midiOscEnv[hilo].oscResponder.remove;
						cvWidgets[key].midiOscEnv[hilo].oscResponder = nil;	
					}
				})
			}
		);
		cvWidgets[key].remove;
		cvWidgets.removeAt(key);
		widgetStates.removeAt(key);
		tabs.views.do({ |v, i| if(v.children.size == 0, { this.prRemoveTab(i) }) });
		^lastVal;
	}
	
	*removeAll { |...keys|
		if(keys.size < 1, { 
			all.keys.do(this.removeAt(_)); 
		}, { 
			keys.do(this.removeAt(_)); 
		});
	}
	
	*at { |key|
		^all.at(key.asSymbol);
	}
		
	*use { |key, spec, value, tab, slot|
		var thisKey, thisSpec, thisVal, thisSlot;
		key ?? { Error("You cannot use a CV in CVCenter without providing key").throw };
		slot !? {
			thisSlot = slot.asString.toLower.asSymbol;
			if([\lo, \hi].detect({ |sbl| sbl === thisSlot }).class !== Symbol, {
				Error("Looks like you wanted to create a multi-dimensional widget. However, the given slot-value"+slot+"is not valid!").throw;
			});
		};
				
		thisKey = key.asSymbol;
		all ?? { this.new };
				
		thisSpec = spec ?? { thisSpec = ControlSpec.new };
		thisVal = value ?? { thisVal = thisSpec.default };

		if(thisSlot.notNil, {
			/* special case: CVWidget2D needs 2 CVs */
			if(thisSlot === \lo or: { thisSlot === \hi }, {
				all[thisKey] ?? { all.put(thisKey, ()) };
				all[thisKey].put(thisSlot, CV.new(thisSpec, thisVal));
			})
		}, {
			all.put(thisKey, CV.new(thisSpec, thisVal)) 
		});
				
		if(window.isNil or:{ window.isClosed }, {
			this.gui(tab);
		}, {
			this.prAddToGui(tab);
		});
		
		if(slot.notNil, {
			^all[thisKey][thisSlot];
		}, {
			^all[thisKey];
		})
	}
	
	*setup {
		^[this.midiMode, this.midiResolution, this.midiMean, this.ctrlButtonBank, this.softWithin];
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
	
	*renameTab { |oldName, newName|
		var index;
		index = tabs.views.detectIndex({ |view, i| tabs.getLabelAt(i) == oldName.asString; });
		tabs.setLabelAt(index, newName.asString);
		tabProperties[index].tabLabel = newName.asString;
	}
	
	*setActionAt { |key, action, slot|
		if(all[key].notNil and:{ cvWidgets[key].notNil }, {
			if(action.class === String, { action = action.interpret });
			switch(cvWidgets[key].class,
				CVWidget2D, {
					if(slot.isNil, { Error("Please provide the key (\hi or \lo) for which the action shall be set").throw });
					all[key][slot].action_(action);
					widgetStates[key].action ?? { widgetStates[key].action = () };
					widgetStates[key].action.put(slot, action.asCompileString);
				},
				{
					all[key].action_(action);
					widgetStates[key].put(\action, action.asCompileString);
				}
			)
		})
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
						[\lo, \hi].do({ |hilo|
							lib[\all][k][hilo] = (
								spec: all[k][hilo].spec,
								val: all[k][hilo].value,
								action: widgetStates[k].action !? { widgetStates[k].action[hilo] },
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
									num: cvWidgets[k].midiOscEnv[hilo].midiRawNum
								)
							)
						})
					},
					CVWidgetKnob, {
						lib[\all][k] = (
							spec: all[k].spec,
							val: all[k].value,
							action: widgetStates[k].action,
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
								num: cvWidgets[k].midiOscEnv.midiRawNum
							),
							wdgtClass: CVWidgetKnob
						)
					}
				);
				lib[\all][k].tabLabel = tabProperties[widgetStates[k].tabIndex].tabLabel;
//				lib[\all][k].postcs;
			});

			if(GUI.current.asString == "QtGUI", {
				lib.writeTextArchive(*f);
			}, {
				lib.writeTextArchive(f);
			});
			lib = nil;
		};
		if(path.isNil, {
			if(GUI.current.asString != "QtGUI", {
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
			if(GUI.current.asString == "QtGUI", {
				lib = Library.readTextArchive(*f);
			}, {
				lib = Library.readTextArchive(f);
			});
			if(all.notNil, {
				if(addToExisting.not, { 
					this.removeAll;
				});
			});
			lib[\all].pairsDo({ |key, v|
				switch(v.wdgtClass,
					CVWidget2D, {
						[\lo, \hi].do({ |hilo|
							this.use(key, v[hilo].spec, v[hilo].val, v.tabLabel, hilo);
							if(loadActions, {
								v[hilo].action !? {
									this.setActionAt(key, v[hilo].action.interpret, hilo);
								}
							});
							if(autoConnectOSC, {
								v[hilo].osc.cmdName !? {
									v[hilo].osc.addr.class;
									cvWidgets[key].oscConnect(
										v[hilo].osc.addr.ip, 
										v[hilo].osc.addr.port, 
										v[hilo].osc.cmdName, 
										v[hilo].osc.msgIndex, 
										hilo
									);
									cvWidgets[key].setOscInputConstraints(
										v[hilo].osc.calibConstraints.lo @ v[hilo].osc.calibConstraints.hi, hilo
									);
									cvWidgets[key].setOscMapping(v[hilo].osc.oscMapping, hilo)
								}
							});
							if(autoConnectMIDI, {
								v[hilo].midi.num !? {
									cvWidgets[key].midiConnect(
										v[hilo].midi.src,
										v[hilo].midi.chan,
										v[hilo].midi.num,
										hilo
									)
								}
							})
						})
					},
					CVWidgetKnob, { 
						this.use(key, v.spec, v.val, v.tabLabel);
						if(loadActions, {
							v.action !? {
								this.setActionAt(key, v.action.interpret);
							}
						});
						if(autoConnectOSC, {
							v.osc.cmdName !? {
								cvWidgets[key].oscConnect(
									v.osc.addr.ip, 
									v.osc.addr.port, 
									v.osc.cmdName, 
									v.osc.msgIndex 
								);
								cvWidgets[key].setOscInputConstraints(
									v.osc.calibConstraints.lo @ v.osc.calibConstraints.hi
								);
								cvWidgets[key].setOscMapping(v.osc.oscMapping)
							}
						});
						if(autoConnectMIDI, {
							v.midi.num !? {
								cvWidgets[key].midiConnect(
									v.midi.src,
									v.midi.chan,
									v.midi.num
								)
							}
						})
					}
				)
			});
		};

		if(path.isNil, {
			if(GUI.current.asString == "QtGUI", {
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
	
	*prSetup { |argMode, argResolution, argMean, argCButtonBank, argSoftWithin|
		argMode !? { this.midiMode_(argMode) };
		argResolution !? { this.midiResolution_(argResolution) };
		argMean !? { this.midiMean_(argMean) };
		argCButtonBank !? { this.ctrlButtonBank_(argCButtonBank) };
		argSoftWithin !? { this.softWithin_(argSoftWithin) };
		if(window.notNil and:{ window.notClosed }, {
//			("setup-args:"+[argMode, argResolution, argMean, argCButtonBank, argSoftWithin]).postln;
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
						[\lo, \hi].do({ |hilo|
							this.midiMode !? { wdgt.setMidiMode(this.midiMode, hilo) };
							this.midiResolution !? { wdgt.setMidiResolution(this.midiResolution, hilo) };
							this.midiMean !? { wdgt.setMidiMean(this.midiMean, hilo) };
							this.ctrlButtonBank !? { wdgt.setCtrlButtonBank(this.ctrlButtonBank, hilo) };
							this.softWithin !? { wdgt.setSoftWithin(this.softWithin, hilo) };
						});
					}
				)
			})
		})
	}
			
	*prAddToGui { |tab|
		var allCVKeys, widgetKeys, thisKeys;
		var rowwidth, colcount;
		var cvTabIndex, tabLabels;
		var thisNextPos;
		var widgetwidth, widgetheight=166, colwidth, rowheight;
		var widgetControllersAndModels, cvcArgs;
		
//		("tab passed to prAddToGui:"+[tab, this.setup]).postln;
		
		tabLabels = tabProperties.collect({ |tab| tab.tabLabel.asSymbol });
		
		if(tab.notNil, {
			if(tabLabels.includes(tab.asSymbol), {
				cvTabIndex = tabLabels.indexOf(tab.asSymbol);
//				("tab is not nil and included in the list of current tabs:"+cvTabIndex).postln;
			}, {
				if(tabs.views.size == 1 and: { tabs.views[0].children.size == 0 }, {
					cvTabIndex = 0;
					this.renameTab(tabs.getLabelAt(0), tab.asString);
				}, { 
					tabs.add(tab);
					cvTabIndex = tabLabels.size;
					tabProperties = tabProperties.add((tabLabel: tab, tabColor: nextColor.next));
				})
//				("tab is not nil and not included in the list of current tabs:"+cvTabIndex).postln;
			})
		}, {
			cvTabIndex = tabs.activeTab;
//			("tab is nil:"+cvTabIndex).postln;
		});
		
		tabs.labelColors_(tabProperties.collect(_.tabColor));
		tabs.unfocusedColors_(tabProperties.collect({ |t| t.tabColor.copy.alpha_(0.8) }));
				
		if(tabProperties[cvTabIndex].nextPos.notNil, {
			thisNextPos = tabProperties[cvTabIndex].nextPos;
		}, {
			thisNextPos = 0@0;
		});
		
		rowheight = widgetheight+1; // add a small gap between rows
		
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
				all[k].keys.includesAny([\hi, \lo])
			}, {
				cvWidgets[k] = CVWidget2D(
					tabs.views[cvTabIndex], 
					[all[k].lo, all[k].hi], 
					k, 
					Rect(thisNextPos.x, thisNextPos.y, widgetwidth = 105, widgetheight), 
					setup: (lo: this.setup, hi: this.setup),
					controllersAndModels: cvWidgets[k] !? { 
						(lo: cvWidgets[k].wdgtControllersAndModels.lo, hi: cvWidgets[k].wdgtControllersAndModels.hi) 
					},
					cvcGui: cvcArgs
				);
				widgetStates.put(k, (tabIndex: cvTabIndex));
			}, {	
//				"invoked a CVWidgetKnob at prAddToGui".postln;
				cvWidgets[k] = CVWidgetKnob(
					tabs.views[cvTabIndex], 
					all[k], 
					k, 
					Rect(thisNextPos.x, thisNextPos.y, widgetwidth = 52, widgetheight),
					setup: this.setup,
					controllersAndModels: cvWidgets[k] !? { cvWidgets[k].wdgtControllersAndModels },
					cvcGui: cvcArgs
				);
				if(widgetStates[k].isNil, {
					widgetStates.put(k, (tabIndex: cvTabIndex));
				}, {
					widgetStates[k].tabIndex = cvTabIndex;
				});
				cvWidgets[k].widgetCV !? { cvWidgets[k].widgetCV.value_(cvWidgets[k].widgetCV.value) };
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
//			[cvTabIndex, thisNextPos].postln;
			tabs.focusActions_(Array.fill(tabs.views.size, {{ this.prRegroupWidgets(tabs.activeTab) }}));
			tabs.focus(cvTabIndex);
		});
		this.prRegroupWidgets(cvTabIndex);
		window.front;
	}
	
	*prRegroupWidgets { |tabIndex|
		var rowwidth, rowheight, colcount, colwidth, thisNextPos, order, orderedWidgets;
		var widgetwidth, widgetheight=166;
				
//		"prRegroupWidgets invoked".postln;
				
		rowheight = widgetheight+1;
		thisNextPos = 0@0;
		
		tabIndex !? {	
			order = cvWidgets.order;
			orderedWidgets = cvWidgets.atAll(order);
			order.do({ |k, i|
//				widgetStates[k].postln;
				if(widgetStates[k].notNil and:{ tabIndex == widgetStates[k].tabIndex }, {
					if(thisNextPos != (0@0), { 
						thisNextPos = tabProperties[widgetStates[k].tabIndex].nextPos;
					});
					orderedWidgets[i].widgetXY_(thisNextPos);
					colwidth = orderedWidgets[i].widgetProps.x+1; // add a small gap to the right
					rowwidth = tabs.views[widgetStates[k].tabIndex].bounds.width-15;
					if(thisNextPos.x+colwidth >= (rowwidth-colwidth-15), {
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
			tabs.removeAt(index);
			tabProperties.removeAt(index);
		}, {
			if(tabs.getLabelAt(index) != "default", { tabs.setLabelAt(index, "default") });
			tabProperties = [(tabLabel: "default", tabColor: tabProperties[index].tabColor)];
		})
	}
	
}