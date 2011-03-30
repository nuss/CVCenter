
CVCenter {

	classvar <all, <nextCVKey, <cvWidgets, <window, <tabs, <switchBoard;
	classvar <>midiMode, <>midiResolution, <>midiMean, <>softWithin, <numccs;
	classvar <>guix, <>guiy, <>guiwidth, <>guiheight; 
	classvar /*<controlButtonKeys, */<controlButtons, <nextButtonPos;
	classvar <>ctrlButtonBank, currentButtonStates, guiClosed = false/*, buttonProps*/;
	classvar /*widgetwidth = 52, widgetheight = 122, colwidth, rowheight, */<widgetStates;
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
			
//			("setUpArgs:"+setUpArgs).postln;
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
			
//			colors.postln;
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
					("CVs added so far:"+all).postln;
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
		var widgetControllersAndModels;
			
		cvs !? { this.put(*cvs) };
		
		this.guix ?? { this.guix_(0) };
		this.guiy ?? { this.guiy_(0) };
		this.guiwidth ?? { this.guiwidth_(500) };
		this.guiheight ?? { this.guiheight_(250) };
		
		if(window.isNil or:{ window.isClosed }, {
//			Window.allWindows.select({ |w| "^CVCenter".matchRegexp(w.name) == true }).size < 1
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
				cvWidgets.pairsDo({ |k, w|
					widgetStates[k].nameField = w.nameField.string;
					w.class.switch(
						CVWidgetKnob, {
							widgetStates[k].midiEdit = w.guiEls.midiHead.enabled;
							widgetStates[k].midiLearn = w.guiEls.midiLearn.value;
							widgetStates[k].midiBg = w.guiEls.midiSrc.background; 
							widgetStates[k].midiStrColor = w.guiEls.midiSrc.stringColor;
							widgetStates[k].midiSrc = w.guiEls.midiSrc.string;
							widgetStates[k].midiChan = w.guiEls.midiChan.string;
							widgetStates[k].midiCtrl = w.guiEls.midiCtrl.string;
							w.cc !? { widgetStates[k].cc = w.midiOscSpecss.cc };
						},
						CVWidget2D, {
							widgetStates[k].midiEditHi = w.midiHeadHi.enabled;
							widgetStates[k].midiEditLo = w.midiHeadLo.enabled;
							widgetStates[k].midiLearnHi = w.midiLearnHi.value;
							widgetStates[k].midiLearnLo = w.midiLearnLo.value;
							widgetStates[k].midiBgHi = w.midiSrcHi.background; 
							widgetStates[k].midiStrColorHi = w.midiSrcHi.stringColor;
							widgetStates[k].midiSrcHi = w.midiSrcHi.string;
							widgetStates[k].midiBgLo = w.midiSrcLo.background; 
							widgetStates[k].midiStrColorLo = w.midiSrcLo.stringColor;
							widgetStates[k].midiSrcLo = w.midiSrcLo.string;
							widgetStates[k].midiChanHi = w.midiChanHi.string;
							widgetStates[k].midiChanLo = w.midiChanLo.string;
							widgetStates[k].midiCtrlHi = w.midiCtrlHi.string;
							widgetStates[k].midiCtrlLo = w.midiCtrlLo.string;
							w.ccHi !? { widgetStates[k].ccHi = w.ccHi };
							w.ccLo !? { widgetStates[k].ccLo = w.ccLo };
						}
					)
				});
				tabProperties.do(_.nextPos_(0@0));
				controlButtons = nil;
				cvWidgets = IdentityDictionary();
				nextButtonPos = 0@0;
				guiClosed = true;
			});

			thisNextPos = 0@0;
			rowheight = widgetheight+1; // add a small gap between rows
			
			order = all.order;
			orderedCVs = all.atAll(order);

//			[orderedCVs, order].postln;
			
			order.do({ |k, i|
				if(widgetStates.size < 1, { cvTabIndex = 0 }, {
					if(widgetStates[k].isNil, {
						cvTabIndex = 0;
					}, {
						cvTabIndex = widgetStates[k].tabIndex ? cvTabIndex = 0;
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
//				"and now a 2D widget".postln;
					cvWidgets[k] = CVWidget2D(
						tabs.views[cvTabIndex], [orderedCVs[i].lo, orderedCVs[i].hi], k, Rect(thisNextPos.x, thisNextPos.y, widgetwidth = 122, widgetheight), this.setup
					);
					// to be tested in depth ...
					if(cvWidgets[k].midiLearnLo.action.class != FunctionList, {
						cvWidgets[k].midiLearnLo.action_(
							cvWidgets[k].midiLearnLo.action.addFunc({ this.prAddControlButton(k, \ccLo) })
						)
					});
					if(cvWidgets[k].midiLearnHi.action.class != FunctionList, {
						cvWidgets[k].midiLearnHi.action_(
							cvWidgets[k].midiLearnHi.action.addFunc({ this.prAddControlButton(k, \ccHi) })
						)
					})
				}, {
//					this.setup.postln;
//					"and now a knob widget".postln;
					cvWidgets[k] = CVWidgetKnob(
						tabs.views[cvTabIndex], 
						orderedCVs[i], 
						k, 
						Rect(thisNextPos.x, thisNextPos.y, widgetwidth = 52, widgetheight), 
						setup: this.setup,
						controllersAndModels: widgetControllersAndModels,
						cvcGui: true
					);
					// to be tested in depth ...
					if(cvWidgets[k].guiEls.midiLearn.action.class != FunctionList, {
						cvWidgets[k].guiEls.midiLearn.action_(
							cvWidgets[k].guiEls.midiLearn.action.addFunc({ this.prAddControlButton(k) })
						)
					})
				});

				cvWidgets[k].widgetBg.background_(tabProperties[cvTabIndex].tabColor);
				
				widgetStates[k] !? {
					cvWidgets[k].nameField.string_(widgetStates[k].nameField);
					widgetStates[k].midiEdit !? { cvWidgets[k].guiEls.midiHead.enabled_(widgetStates[k].midiEdit) };
					widgetStates[k].midiEditHi !? { cvWidgets[k].midiHeadHi.enabled_(widgetStates[k].midiEditHi) };
					widgetStates[k].midiEditLo !? { cvWidgets[k].midiHeadLo.enabled_(widgetStates[k].midiEditLo) };
					widgetStates[k].midiLearn !? { cvWidgets[k].guiEls.midiLearn.value_(widgetStates[k].midiLearn) };
					widgetStates[k].midiLearnHi !? { cvWidgets[k].midiLearnHi.value_(widgetStates[k].midiLearnHi) };
					widgetStates[k].midiLearnLo !? { cvWidgets[k].midiLearnLo.value_(widgetStates[k].midiLearnLo) };
					widgetStates[k].midiBg !? {
						[cvWidgets[k].guiEls.midiSrc, cvWidgets[k].guiEls.midiChan, cvWidgets[k].guiEls.midiCtrl].do({ |view|
							view.background_(widgetStates[k].midiBg);
							view.stringColor_(widgetStates[k].midiStrColor);
						})
					};
					widgetStates[k].midiBgHi !? {
						[cvWidgets[k].midiSrcHi, cvWidgets[k].midiChanHi, cvWidgets[k].midiCtrlHi].do({ |view|
							view.background_(widgetStates[k].midiBgHi);
							view.stringColor_(widgetStates[k].midiStrColorHi);
						})
					};
					widgetStates[k].midiBgLo !? {
						[cvWidgets[k].midiSrcLo, cvWidgets[k].midiChanLo, cvWidgets[k].midiCtrlLo].do({ |view|
							view.background_(widgetStates[k].midiBgLo);
							view.stringColor_(widgetStates[k].midiStrColorLo);
						})
					};
					widgetStates[k].midiSrc !? { cvWidgets[k].guiEls.midiSrc.string_(widgetStates[k].midiSrc) };
					widgetStates[k].midiSrcHi !? { cvWidgets[k].midiSrcHi.string_(widgetStates[k].midiSrcHi) };
					widgetStates[k].midiSrcLo !? { cvWidgets[k].midiSrcLo.string_(widgetStates[k].midiSrcLo) };
					widgetStates[k].midiChan !? { cvWidgets[k].guiEls.midiChan.string_(widgetStates[k].midiChan) };
					widgetStates[k].midiChanHi !? { cvWidgets[k].midiChanHi.string_(widgetStates[k].midiChanHi) };
					widgetStates[k].midiChanLo !? { cvWidgets[k].midiChanLo.string_(widgetStates[k].midiChanLo) };
					widgetStates[k].midiCtrl !? { cvWidgets[k].guiEls.midiCtrl.string_(widgetStates[k].midiCtrl) };
					widgetStates[k].midiCtrlHi !? { cvWidgets[k].midiCtrlHi.string_(widgetStates[k].midiCtrlHi) };
					widgetStates[k].midiCtrlLo !? { cvWidgets[k].midiCtrlLo.string_(widgetStates[k].midiCtrlLo) };
					widgetStates[k].cc !? { 
						cvWidgets[k].midiOscSpecs.cc_(widgetStates[k].cc);
						this.prAddControlButton(k);
					};
					widgetStates[k].ccHi !? { 
						cvWidgets[k].ccHi_(widgetStates[k].ccHi);
						this.prAddControlButton(k, \ccHi);
					};
					widgetStates[k].ccLo !? { 
						cvWidgets[k].ccLo_(widgetStates[k].ccLo);
						this.prAddControlButton(k, \ccLo);
					};
				};
				if(all[k].class === Event and:{ 
					all[k].keys.includesAny([\lo, \hi])
				}, {
					widgetStates.put(k, (tabIndex: cvTabIndex, addedFunc: (\lo: false, \hi: false)))
				}, {
					widgetStates.put(k, (tabIndex: cvTabIndex, addedFunc: false))
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

		skipJacks = SkipJack.all.collect({ |r| r.name == "CVCenter-Updater" });
		if(skipJacks.includes(true).not, {
			updateRoutine = SkipJack({
//				this.prUpdateSwitchboardSetup;
				lastUpdate ?? { lastUpdate = all.size };
				lastSetUp !? {
					if(this.setup != lastSetUp, {
//						("now updating widget-setups:"+[this.setup, lastSetUp]).postln;
						this.prSetup(*this.setup);
						this.prUpdateSwitchboard(true);
					})
				};	
				if(all.size != lastUpdate, {
					if(all.size > lastUpdate and:{ cvWidgets.size <= lastUpdate }, {
						this.prAddToGui;
					});
					if(all.size < lastUpdate, {
						removedKeys = cvWidgets.keys.difference(all.keys);
						removedKeys.do({ |k|
							cvWidgets[k].remove;
							cvWidgets.removeAt(k);
							controlButtons !? {
								controlButtons[k] !? {
									controlButtons[k].remove;
									controlButtons.removeAt(k)
								}
							};
						});
						this.prRegroupWidgets(tabs.activeTab);
//						"it happens at 1".postln;
//						this.prUpdateSwitchboardSetup;
					});
					lastUpdate = all.size;
				});
				try {
					if(window.bounds.width != lastUpdateWidth, {
						this.prRegroupWidgets(tabs.activeTab);
						this.prUpdateSwitchboard;
//						this.prUpdateSwitchboardSetup;
					})
				};
				lastUpdateWidth = window.bounds.width;
				lastSetUp = this.setup;
//				lastSetUp.postln;
				this.prAddFuncToCC;
				controlButtons !? {
//					controlButtons.postln;
					if(lastCtrlBtnBank != ctrlButtonBank, {
//						"should be triggered even if gui has been closed and re-oppened again".postln;
//						if(guiClosed.not, { this.prUpdateSwitchboardSetup });
						guiClosed = false;
					});
					controlButtons.pairsDo({ |k, btn|
						if(btn.class === Event and:{
							btn.keys.includesAny([\ccHi, \ccLo])
						}, {
							[\ccHi, \ccLo].do({ |hilo|
								btn[hilo] !? {
									if(hilo == \ccHi, {
										btn[hilo].states_([[
											cvWidgets[k].midiCtrlHi.string, 
											Color.black,
											tabProperties[widgetStates[k].tabIndex].tabColor
										]])
									});
									if(hilo == \ccLo, {
										btn[hilo].states_([[
											cvWidgets[k].midiCtrlLo.string, 
											Color.black,
											tabProperties[widgetStates[k].tabIndex].tabColor
										]])
									});
									window.refresh;
								}
							})
						}, {
//							("false func triggered:"+[k, btn]).postln;
							btn.states_([[
								cvWidgets[k].midiOscSpecs.midiCtrl.string, 
								Color.black, 
								tabProperties[widgetStates[k].tabIndex].tabColor
							]]);
							window.refresh;
						})
					});
					lastCtrlBtnBank = ctrlButtonBank;
				}
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
//			[key, cv].postln;
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
		var lastVal;
		lastVal = all.at(key.asSymbol).value;
		all.removeAt(key.asSymbol);
		cvWidgets[key].class.switch(
			CVWidgetKnob, { 
				cvWidgets[key].midiOscSpecs.cc !? { cvWidgets[key].midiOscSpecs.cc.remove };
				controlButtons !? {
					controlButtons[key] !? { 
						controlButtons[key].postln;
						controlButtons[key].remove;
						controlButtons.removeAt(key);
					}
				}
			},
			CVWidget2D, {
				cvWidgets[key].ccHi !? { cvWidgets[key].ccHi.remove };
				cvWidgets[key].ccLo !? { cvWidgets[key].ccLo.remove };
				controlButtons !? {
					controlButtons[key] !? {
						controlButtons[key][\ccLo] !? {
							controlButtons[key][\ccLo].remove;
						};
						controlButtons[key][\ccHi] !? {
							controlButtons[key][\ccHi].remove;
						};
						controlButtons.removeAt(key);
					}
				}
			}
		);
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
		var thiskey, thisspec, thisval, thisslot;
		key ?? { Error("You cannot use a CV in CVCenter without providing key").throw };
		slot !? {
			thisslot = slot.asString.toLower.asSymbol;
			if([\lo, \hi].detect({ |sbl| sbl === thisslot }).class !== Symbol, {
				Error("Looks like you wanted to create a multi-dimensional widget. However, the given slot-value"+slot+"is not valid!").throw;
			});
		};
				
		thiskey = key.asSymbol;
		all ?? { this.new };
				
		if(all.keys.asArray.indexOfEqual(thiskey).isNil, {
			thisspec = spec ?? { thisspec = ControlSpec.new };
			thisval = value ?? { thisval = thisspec.default };
			thisslot.class.switch(
				Symbol, {
					if(thisslot === \lo or: { thisslot === \hi }, {
						[\lo, \hi].do({ |slot|
							all[key.asSymbol] ?? { all.put(key.asSymbol, ()) };
							all[key.asSymbol][slot] = CV.new(thisspec.copy, thisval.copy);
						})
					})
				}, 
				{ all.put(key.asSymbol, CV.new(thisspec, thisval)) };
			)
		}, {
			if(all[key.asSymbol].isKindOf(Event) and:{ slot.notNil }, {
				spec !? { all[key.asSymbol][thisslot].spec_(spec) };
				value !? { all[key.asSymbol][thisslot].value_(value) };
			}) 
		});
		
		if(window.isNil or:{ window.isClosed }, {
//			Window.allWindows.select({ |w| "^CVCenter".matchRegexp(w.name) == true }).size < 1
//			("this.gui:"+this.setup).postln;
			this.gui(tab);
		}, {
//			("now adding:"+tab.asString).postln;
//			("this.prAddToGui:"+this.setup).postln;
			this.prAddToGui(tab);
		});
		
		if(slot.notNil, {
			^all.at(key.asSymbol)[slot.asSymbol];
		}, {
			^all.at(key.asSymbol);
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
	
	// private Methods - not to be used directly
	
	*prSetup { |argMode, argResolution, argMean, argCButtonBank, argSoftWithin|
		argMode !? { this.midiMode_(argMode) };
		argResolution !? { this.midiResolution_(argResolution) };
		argMean !? { this.midiMean_(argMean) };
		argCButtonBank !? { this.ctrlButtonBank_(argCButtonBank) };
		argSoftWithin !? { this.softWithin_(argSoftWithin) };
		if(Window.allWindows.select({ |w| "^CVCenter".matchRegexp(w.name) == true }).size > 0, {
			("setup-args:"+[argMode, argResolution, argMean, argCButtonBank, argSoftWithin]).postln;
			cvWidgets.pairsDo({ |k, wdgt| 
				this.midiMode !? { wdgt.midiMode_(this.midiMode) };
				this.midiResolution !? { wdgt.midiResolution_(this.midiResolution) };
				this.midiMean !? { wdgt.midiMean_(this.midiMean) };
				wdgt.ctrlButtonBank_(this.ctrlButtonBank);
				this.softWithin !? { wdgt.softWithin_(this.softWithin) };
			})
		})
	}
			
	*prAddToGui { |tab|
		var allCVKeys, widgetKeys, thisKeys;
		var rowwidth, colcount;
		var cvTabIndex, tabLabels;
		var thisNextPos;
		var widgetwidth, widgetheight=166, colwidth, rowheight;
		var widgetControllersAndModels;
		
		("tab passed to prAddToGui:"+[tab, this.setup]).postln;
		
		tabLabels = tabProperties.collect({ |tab| tab.tabLabel.asSymbol });
		
		if(tab.notNil, {
			if(tabLabels.includes(tab.asSymbol), {
				cvTabIndex = tabLabels.indexOf(tab.asSymbol);
//				("tab is not nil and included in the list of current tabs:"+cvTabIndex).postln;
			}, {
				tabs.add(tab);
				cvTabIndex = tabLabels.size;
				tabProperties = tabProperties.add((tabLabel: tab, tabColor: nextColor.next));
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
			if(all[k].class === Event and:{
				all[k].keys.includesAny([\hi, \lo])
			}, {
				cvWidgets[k] = CVWidget2D(tabs.views[cvTabIndex], [all[k].lo, all[k].hi], k, Rect(thisNextPos.x, thisNextPos.y, widgetwidth = 122, widgetheight), this.setup);
				// to be tested in depth ...
				if(cvWidgets[k].midiLearnLo.action.class != FunctionList, {
					cvWidgets[k].midiLearnLo.action_(
						cvWidgets[k].midiLearnLo.action.addFunc({ this.prAddControlButton(k, \ccLo) })
					)
				});
				if(cvWidgets[k].midiLearnHi.action.class != FunctionList, {
					cvWidgets[k].midiLearnHi.action_(
						cvWidgets[k].midiLearnHi.action.addFunc({ this.prAddControlButton(k, \ccHi) })
					)
				});
				widgetStates.put(k, (tabIndex: cvTabIndex, addedFunc: (hi: false, lo: false)));
			}, {	
				cvWidgets[k] = CVWidgetKnob(
					tabs.views[cvTabIndex], 
					all[k], 
					k, 
					Rect(thisNextPos.x, thisNextPos.y, widgetwidth = 52, widgetheight), 
					setup: this.setup,
					controllersAndModels: widgetControllersAndModels,
					cvcGui: true
				);
				if(cvWidgets[k].guiEls.midiLearn.action != FunctionList, {
					cvWidgets[k].guiEls.midiLearn.action_(
						cvWidgets[k].guiEls.midiLearn.action.addFunc({ this.prAddControlButton(k) })
					)
				});
				widgetStates.put(k, (tabIndex: cvTabIndex, addedFunc: false));
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
				
		rowheight = widgetheight+1;
		thisNextPos = 0@0;
		
		tabIndex !? {	
			order = cvWidgets.order;
			orderedWidgets = cvWidgets.atAll(order);
			order.do({ |k, i|
				if(tabIndex == widgetStates[k].tabIndex, {
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
	
	*prAddControlButton { |key, slot|
		var buttonWidth;
		
		if(ctrlButtonBank.notNil and:{
			ctrlButtonBank.isInteger and:{
				ctrlButtonBank > 1
			}
		}, {
			buttonWidth = 30
		}, {
			buttonWidth = 18
		});

		controlButtons ?? { controlButtons = IdentityDictionary() };
		
		if(slot.isNil, {
			this.prAddCtrlBtn(key, buttonWidth);
		}, {
			this.prAdd2DCtrlBtn(key, slot, buttonWidth);
		});
		switchBoard.reflow;
//		switchBoard.reflowAll;
	}
	
	*prAddCtrlBtn { |key, buttonWidth|
		var val;
		
		if(controlButtons[key].isNil, {
			val = cvWidgets[key].guiEls.midiCtrl.string;
			controlButtons.put(
				key,
				Button(
					switchBoard,
					buttonWidth@15
				)
				.states_([[
					val, 
					Color.black, 
					tabProperties[widgetStates[key].tabIndex].tabColor
				]])
				.font_(Font("Helvetica", 9))
				.action_({ 
					tabs.focus(widgetStates[key].tabIndex);
					cvWidgets[key].widgetBg.focusColor_(Color.green).focus(true);
				})
			)
		}, {
			controlButtons[key].remove;
			controlButtons.removeAt(key);
		});
	}
	
	*prAdd2DCtrlBtn { |key, slot, buttonWidth|
		var val;
		
		if(controlButtons[key].isNil, {
			controlButtons.put(key, ());
		});
		
		if(controlButtons[key][slot].isNil, {
			if(slot == "ccHi", {
				val = cvWidgets[key].midiCtrlHi.string;
			}, {
				val = cvWidgets[key].midiCtrlLo.string;
			});			
			controlButtons[key].put(
				slot,
				Button(
					switchBoard,
					buttonWidth@15
				)
				.states_([[
					val, 
					Color.black, 
					tabProperties[widgetStates[key].tabIndex].tabColor
				]])
				.font_(Font("Helvetica", 9))
				.action_({ 
					tabs.focus(widgetStates[key].tabIndex);
					cvWidgets[key].widgetBg.focusColor_(Color.green).focus(true);
				})
			)
		}, {
			controlButtons[key][slot].remove;
			controlButtons[key].removeAt(slot);
			if(controlButtons[key].size == 0, { controlButtons.removeAt(key) });
		})
	}
	
	*prUpdateSwitchboard { |updateButtons=false|
		var buttonWidth;
		
		if(updateButtons, {
			if(ctrlButtonBank.notNil and:{
				ctrlButtonBank.isInteger and:{
					ctrlButtonBank > 1
				}
			}, {
				buttonWidth = 30
			}, {
				buttonWidth = 18
			});
			
			controlButtons.pairsDo({ |key, btn|
				if(btn.class == Event, {
					btn.do({ |b| b.bounds_(Rect(b.bounds.left, b.bounds.top, buttonWidth, b.bounds.height)) });
				}, {
					btn.bounds_(Rect(btn.bounds.left, btn.bounds.top, buttonWidth, btn.bounds.height));				});
			})
		});
		
		switchBoard.decorator.bounds_(switchBoard.bounds);
		switchBoard.reflow;
//		switchBoard.reflowAll;
	}
	
	*prAddFuncToCC {
		var funcToAdd;
		cvWidgets.pairsDo({ |k, wdgt|
//			widgetStates[k].postln;
			funcToAdd = { 
				{ 
					tabs.focus(widgetStates[k].tabIndex);
					cvWidgets[k].widgetBg.focusColor_(Color.green).focus(true);
				}.defer
			};
			if(wdgt.respondsTo(\cc), {
				wdgt.cc !? {
					if(widgetStates[k].addedFunc == false and:{
						wdgt.cc.function.class != FunctionList
					}, {
						wdgt.cc.function_(
//							"adding another function".postln;
							wdgt.cc.function.addFunc(funcToAdd)
						);
						widgetStates[k].addedFunc = true;
					});
				};
				wdgt.cc ?? {
					if(widgetStates[k].addedFunc == true, {
						widgetStates[k].addedFunc = false
					});
				};
			});
			if(wdgt.respondsTo(\ccLo) and:{ wdgt.respondsTo(\ccHi) }, {
				wdgt.ccLo !? {
					if(widgetStates[k].addedFunc.lo == false and:{
						wdgt.ccLo.function.class != FunctionList
					}, {
						wdgt.ccLo.function_(
							wdgt.ccLo.function.addFunc(funcToAdd)
						);
						widgetStates[k].addedFunc.lo = true;
					});
				};
				wdgt.ccHi !? {
					if(widgetStates[k].addedFunc.hi == false and:{
						wdgt.ccHi.function.class != FunctionList
					}, {
						wdgt.ccHi.function_(
							wdgt.ccHi.function.addFunc(funcToAdd)
						);
						widgetStates[k].addedFunc.hi = true;
					});
				};
				wdgt.ccLo ?? {
					if(widgetStates[k].addedFunc.lo == true, {
						widgetStates[k].addedFunc.lo = false
					});
				};
				wdgt.ccHi ?? {
					if(widgetStates[k].addedFunc.hi == true, {
						widgetStates[k].addedFunc.hi = false
					});
				};
			})
		})
	}
	
}