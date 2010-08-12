
CVCenter {

	classvar <cvsList, <nextCVKey, <cvWidgets, <window, <tabs, <switchBoard;
	classvar <>midimode, <>midimean, <>midistring, <numccs;
	classvar <controlButtonKeys, <controlButtons, <nextButtonPos;
	classvar <>ctrlButtonBank, currentButtonStates, guiClosed = false/*, buttonProps*/;
	classvar widgetwidth = 52, widgetheight = 120, colwidth, rowheight, <widgetStates;
	classvar <tabProperties, colors, nextColor;
	
	*new { |cvs...setUpArgs|
		var r, g, b;

		if(cvsList.isNil, {
			cvsList = IdentityDictionary.new;
			cvWidgets = IdentityDictionary.new;
			widgetStates = IdentityDictionary.new;
			r = g = b = (0.5, 0.55 .. 0.7);
			colors = List();
			tabProperties = [];
			
//			("setUpArgs:"+setUpArgs).postln;
			if(setUpArgs.size > 0, { "prSetup called in *new".postln; this.prSetup(*setUpArgs) });
			
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
							cvsList.put(k.asSymbol, v);
						}, {
							"Your given key-name matches the reserved names for new keys in the CVCenter. Please choose a different name.".warn;
						})
					});
					("CVs added so far:"+cvsList).postln;
				})
			})
		})
	}
		
	*gui { |tab...cvs|
		var flow, rowwidth, colcount;
		var cvTabIndex, order, orderedCVs;
		var updateRoutine, lastUpdate, lastUpdateWidth, lastSetUp, lastCtrlBtnBank, removedKeys, skipJacks;
		var lastCtrlBtnsMode;
		var thisNextPos, tabLabels, labelColors, unfocusedColors;
			
		cvs !? { this.put(*cvs) };
		
		if(Window.allWindows.select({ |w| "^CVCenter".matchRegexp(w.name) == true }).size < 1, {

			window = Window("CVCenter", Rect(0, 0, 400, 210));
			window.view.background_(Color.black);
			flow = FlowLayout(window.bounds.insetBy(4));
			window.view.decorator = flow;
			flow.margin_(4@0);
			flow.gap_(0@4);
			flow.shift(0, 0);

			controlButtonKeys ?? { controlButtonKeys = IdentityDictionary() };
			
			if(tabProperties.size < 1, {
				tabProperties = tabProperties.add(());
				if(tab.isNil, { tabProperties[0].tabLabel = "default" }, { tabProperties[0].tabLabel = tab.asString });
				tabProperties[0].tabColor = nextColor.next;
//				tabProperties.postln;
			});
			
			tabLabels = tabProperties.collect(_.tabLabel);
			labelColors = tabProperties.collect(_.tabColor);
			unfocusedColors = tabProperties.collect({ |t| t.tabColor.copy.alpha_(0.8) });
//			tabProperties.postln;
			
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
//			buttonProps !? { this.prAddCtrlButton }
			switchBoard ?? { switchBoard = ScrollView(window, Rect(0, 0, flow.bounds.width, 50)) };
			switchBoard.resize_(8);

			window.onClose_({
				cvWidgets.pairsDo({ |k, w|
					widgetStates[k].nameField = w.nameField.string;
					widgetStates[k].midiEdit = w.midiHead.enabled;
					widgetStates[k].midiLearn = w.midiLearn.value;
					widgetStates[k].midiBg = w.midiSrc.background;
					widgetStates[k].midiStrColor = w.midiSrc.stringColor;
					widgetStates[k].midiSrc = w.midiSrc.string;
					widgetStates[k].midiChan = w.midiChan.string;
					widgetStates[k].midiCtrl = w.midiCtrl.string;
					w.cc !? { widgetStates[k].cc = w.cc };
				});
				tabProperties.do(_.nextPos_(0@0));
				controlButtons = nil;
				cvWidgets = IdentityDictionary();
				nextButtonPos = 0@0;
				guiClosed = true;
			});

			thisNextPos = 0@0;
			colwidth = widgetwidth+1; // add a small gap between widgets
			rowheight = widgetheight+1;
			rowwidth = window.bounds.width-10;
			colcount = rowwidth.div(colwidth);
			rowwidth = colcount * colwidth;
			
			order = cvsList.order;
			orderedCVs = cvsList.atAll(order);
//			order.postln;
			
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
				
				cvWidgets[k] = CVWidgetKnob(
					tabs.views[cvTabIndex], orderedCVs[i], k, Rect(thisNextPos.x, thisNextPos.y, widgetwidth, widgetheight), this.setup
				);
				
				controlButtons !? { this.prAddCtrlButton };
								
				cvWidgets[k].widgetBg.background_(tabProperties[cvTabIndex].tabColor);
				
				widgetStates[k] !? {
					cvWidgets[k].nameField.string_(widgetStates[k].nameField);
					cvWidgets[k].midiHead.enabled_(widgetStates[k].midiEdit);
					cvWidgets[k].midiLearn.value_(widgetStates[k].midiLearn);
					[cvWidgets[k].midiSrc, cvWidgets[k].midiChan, cvWidgets[k].midiCtrl].do({ |view|
						view.background_(widgetStates[k].midiBg);
						view.stringColor_(widgetStates[k].midiStrColor);					});
					cvWidgets[k].midiSrc.string_(widgetStates[k].midiSrc);
					cvWidgets[k].midiChan.string_(widgetStates[k].midiChan);
					cvWidgets[k].midiCtrl.string_(widgetStates[k].midiCtrl);
					widgetStates[k].cc !? { cvWidgets[k].cc_(widgetStates[k].cc) };
				};
				widgetStates.put(k, (tabIndex: cvTabIndex, addedFunc: false));
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
				lastUpdate ?? { lastUpdate = cvsList.size };
				lastSetUp !? {
					if(this.setup != lastSetUp, {
						("now updating widget-setups:"+this.setup).postln;
						this.prSetup(*this.setup);
					})
				};	
				if(cvsList.size != lastUpdate, {
					if(cvsList.size > lastUpdate and:{ cvWidgets.size <= lastUpdate }, {
						this.prAddToGui;
					});
					if(cvsList.size < lastUpdate, {
						removedKeys = cvWidgets.keys.difference(cvsList.keys);
						removedKeys.do({ |k|
							cvWidgets[k].remove;
							cvWidgets.removeAt(k);
							controlButtons[k].postln;
							controlButtons[k].remove;
							controlButtons.removeAt(k);
						});
						this.prRegroupWidgets(tabs.activeTab);
//						"it happens at 1".postln;
						this.prUpdateSwitchboardSetup;
					});
					lastUpdate = cvsList.size;
				});
				try {
					if(window.bounds.width != lastUpdateWidth, {
						this.prRegroupWidgets(tabs.activeTab);
						this.prUpdateSwitchboardSetup;
					})
				};
				lastUpdateWidth = window.bounds.width;
				lastSetUp = this.setup;
				cvWidgets.pairsDo({ |k, wdgt|
					wdgt.cc !? {
						if(widgetStates[k].addedFunc == false, {
							wdgt.cc.function_(
								wdgt.cc.function.addFunc({ 
									{ 
//										("CCResponder \\"++k+"focuses tab"+widgetStates[k].tabIndex+"now").postln;
										tabs.focus(widgetStates[k].tabIndex);
										cvWidgets.do({ |w|
											if(w === wdgt, {
												wdgt.widgetBg.focusColor_(Color.green).focus(true);
											})
										}) 
									}.defer
								})
							);
							widgetStates[k].addedFunc = true;
						});
					};
					wdgt.cc ?? {
						if(widgetStates[k].addedFunc == true, {
							widgetStates[k].addedFunc = false
						});
					};
					wdgt.midiLearn.action_(
						wdgt.midiLearn.action.addFunc( this.prAddCtrlButton )
					)
				});
				controlButtons !? {
					if(lastCtrlBtnBank != ctrlButtonBank, {
						"should be triggered even if gui has been closed and re-oppened again".postln;
						if(guiClosed.not, { this.prUpdateSwitchboardSetup });
						guiClosed = false;
					});
					controlButtons.pairsDo({ |k, btn|
						currentButtonStates[k] = [
							cvWidgets[k].midiCtrl.string, 
							Color.black, 
							tabProperties[widgetStates[k].tabIndex].tabColor
						];
						btn.states_([currentButtonStates[k]]);
						if(btn.states.flatten(1)[0] == "ctrl", {
							btn.remove;
							controlButtons.removeAt(k);
//							"it happens at 3".postln;
							this.prUpdateSwitchboardSetup;
						});
						window.refresh;
					});
					lastCtrlBtnBank = ctrlButtonBank;
				}
			}, 0.5, { window.isClosed }, "CVCenter-Updater");
		});
	}	
	
	*put { |...args|
		var inputArgs, overwrite=false;
		inputArgs = args;
		if(inputArgs.size.odd, {
			overwrite = inputArgs.pop;
			if(overwrite.isKindOf(Boolean).not, {
				overwrite = nil;
			});
		});
		this.new;
		inputArgs.pairsDo({ |key, cv|
			if(cv.isKindOf(CV).not, {
				Error("The value provided for key '"++key.asString++"' doesn't appear to be a CV.\nPlease choose a valid input!").throw;
			});
			if(overwrite, {
				cvsList.put(key.asSymbol, cv);
			}, {
				if(cvsList.matchAt(key.asSymbol).isNil, {
					cvsList.put(key.asSymbol, cv);
				}, {
					("There is already a CV stored under the name '"++key.asString++"'. \nPlease choose a different key-name!").warn;
				})
			})	
		})
	}
		
	*removeAt { |key|
		var lastVal;
		lastVal = cvsList.at(key.asSymbol).value;
		cvsList.removeAt(key.asSymbol);
		cvWidgets[key].cc !? { cvWidgets[key].cc.remove };
		^lastVal;
	}
	
	*removeAll { |...keys|
		if(keys.size < 1, { 
			cvsList.keys.do(cvsList.removeAt(_)); 
		}, { 
			keys.do(cvsList.removeAt(_)); 
		});
	}
	
	*at { |key|
		^cvsList.at(key.asSymbol);
	}
		
	*use { |key, spec, value, tab|
		var thiskey, thisspec, thisval;
		key ?? { Error("You cannot use a CV in CVCenter without providing key").throw };
		
		thiskey = key.asSymbol;
		cvsList ?? { this.new };
				
		if(cvsList.keys.asArray.indexOfEqual(thiskey).isNil, {
			thisspec = spec ?? { thisspec = ControlSpec.new };
			thisval = value ?? { thisval = thisspec.default };
			cvsList.put(key.asSymbol, CV.new(thisspec, thisval));
		}, {
			// sanity check - condition shouldn't return true anyway
			if(this.at(thiskey).isNil or: { this.at(thiskey).isKindOf(CV).not }, {
				cvsList.put(key.asSymbol, CV.new(thisspec, thisval));
			})
		});
		
		if(Window.allWindows.select({ |w| "^CVCenter".matchRegexp(w.name) == true }).size < 1, {
			this.gui(tab);
		}, {
//			("now adding:"+tab.asString).postln;
			this.prAddToGui(tab);
		});
		^cvsList.at(key.asSymbol);
	}
	
	*softWithin_ { |val|
		cvWidgets !? {
			cvWidgets.do({ |wdgt|
				wdgt.softWithin_(val);
			})
		}
	}
	
	*softWithin {
		var softWithins;
		cvWidgets !? {
			softWithins = [];
			cvWidgets.do({ |wdgt|
				softWithins = softWithins.add(wdgt.softWhithin)
			});
			^softWithins;
		}
	}
	
	*setup {
		^[this.midimode, this.midimean, this.midistring, this.ctrlButtonBank];
	}
	
	// private Methods - not to be used directly
	
	*prSetup { |argMode, argMean, argString, argCButtonBank|
//		("setup-args:"+[argMode, argMean, argString, argCButtonBank]).postln;
		argMode !? { this.midimode_(argMode) };
		argMean !? { this.midimean_(argMean) };
		argString !? { this.midistring_(argString.asString) };
		argCButtonBank !? { this.ctrlButtonBank_(argCButtonBank) };
		if(Window.allWindows.select({ |w| "^CVCenter".matchRegexp(w.name) == true }).size > 0, {
//			window.name = "CVCenter";
			cvWidgets.pairsDo({ |k, cv| 
				this.midimode !? { cv.midimode_(this.midimode) }; 
				this.midimean !? { cv.midimean_(this.midimean) };
				this.midistring !? { cv.midistring_(this.midistring.asString) };
				cv.ctrlButtonBank_(this.ctrlButtonBank); 
			})
		})
	}
			
	*prAddToGui { |tab|
		var allCVKeys, widgetKeys, thisKeys;
		var rowwidth, colcount;
		var cvTabIndex, tabLabels;
		var thisNextPos;
		
//		("tab passed to prAddToGui:"+tab).postln;
		
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
		
//		("next widget added now at:"+thisNextPos).postln;				
		
		colwidth = widgetwidth+1; // add a small gap between widgets
		rowheight = widgetheight+1;
		rowwidth = window.bounds.width-10;
		colcount = rowwidth.div(colwidth);
		rowwidth = colcount * colwidth;
		
		allCVKeys = cvsList.keys;
		widgetKeys = cvWidgets.keys;
		thisKeys = allCVKeys.difference(widgetKeys);
		thisKeys.do({ |k|
			cvWidgets[k] = CVWidgetKnob(tabs.views[cvTabIndex], cvsList[k], k, Rect(thisNextPos.x, thisNextPos.y, widgetwidth, widgetheight), this.setup);
			cvWidgets[k].widgetBg.background_(tabProperties[cvTabIndex].tabColor);
			if(thisNextPos.x+colwidth >= rowwidth, {
				// jump to next row
				tabProperties[cvTabIndex].nextPos = thisNextPos = 0@(thisNextPos.y+rowheight);
			}, {
				// add next widget to the right
				tabProperties[cvTabIndex].nextPos = thisNextPos = thisNextPos.x+colwidth@(thisNextPos.y);
			});
//			[cvTabIndex, thisNextPos].postln;
			widgetStates.put(k, (tabIndex: cvTabIndex, addedFunc: false));
			tabs.focusActions_(Array.fill(tabs.views.size, {{ this.prRegroupWidgets(tabs.activeTab) }}));
			tabs.focus(cvTabIndex);
		});
		this.prRegroupWidgets(cvTabIndex);
		window.front;
	}
	
	*prRegroupWidgets { |tabIndex|
		var rowwidth, rowheight, colcount, colwidth, thisNextPos, order, orderedWidgets;
				
		colwidth = widgetwidth+1; // add a small gap between widgets
		rowheight = widgetheight+1;
		rowwidth = window.bounds.width-15;
		colcount = rowwidth.div(colwidth);
		rowwidth = colcount * colwidth;
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
					if(thisNextPos.x+colwidth >= rowwidth, {
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
	
	*prAddCtrlButton { 
		var buttonWidth, btnTabKeys;
		
		currentButtonStates ?? { currentButtonStates = IdentityDictionary() };
		
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
		btnTabKeys = cvWidgets.select({ |wdgt| wdgt.cc.notNil and:{ wdgt.midiCtrl.string != "ctrl" }}).keys.asArray;
		
//		("btnTabKeys:"+btnTabKeys).postln;

		nextButtonPos ?? { nextButtonPos = 0@0 };
		
		btnTabKeys.do({ |key|
			controlButtons[key] ?? {
				currentButtonStates.put(
					key,
					[
						cvWidgets[key].midiCtrl.string, 
						Color.black, 
						tabProperties[widgetStates[key].tabIndex].tabColor
					]
				);
				
				controlButtons.put(
					key,
					Button(
						switchBoard,
						Rect(nextButtonPos.x, nextButtonPos.y, buttonWidth, 15)
					)
					.states_([currentButtonStates[key]])
					.font_(Font("Helvetica", 9))
					.action_({ 
						tabs.focus(widgetStates[key].tabIndex);
						cvWidgets.pairsDo({ |k, wdgt|
							if(k == key, {
								wdgt.widgetBg.focusColor_(Color.green).focus(true);
							})
						})
					})
				);
								
				if(nextButtonPos.x+buttonWidth >= (switchBoard.bounds.width-80), {
					nextButtonPos = 0@(nextButtonPos.y+15);
				}, {
					nextButtonPos = nextButtonPos.x+buttonWidth@nextButtonPos.y;
				});
			}
		})
	}
	
	*prUpdateSwitchboardSetup {
		var buttonWidth;
		
		"prUpdateSwitchboardSetup triggered".postln;

		if(ctrlButtonBank.notNil and:{
			ctrlButtonBank.isInteger and:{
				ctrlButtonBank > 1
			}
		}, {
			"now should update to new width".postln;
			buttonWidth = 30
		}, {
			buttonWidth = 18
		});

		nextButtonPos = 0@0;
		
		controlButtons.pairsDo({ |key, btn|
			btn.bounds_(Rect(nextButtonPos.x, nextButtonPos.y, buttonWidth, btn.bounds.height));
			
			if(nextButtonPos.x+buttonWidth >= (switchBoard.bounds.width-80), {
				nextButtonPos = 0@(nextButtonPos.y+15);
			}, {
				nextButtonPos = nextButtonPos.x+buttonWidth@nextButtonPos.y;
			})
		})	
	}
	
}