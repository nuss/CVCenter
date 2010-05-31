
CVCenter {

	classvar <cvsList, <nextCVKey, <cvWidgets, <window, <tabs, <switchBoard;
	classvar <>midimode = 0, <>midimean = 64, <>midistring = "";
	classvar <>ctrlButtonMode, controlButtonKeys;
	classvar widgetwidth = 52, widgetheight = 136, colwidth, rowheight, <widgetStates;
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
			
			this.prSetup(*setUpArgs);
			
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
		var updateRoutine, lastUpdate, lastUpdateWidth, lastSetUp, removedKeys, skipJacks;
		var thisNextPos, tabLabels, labelColors, unfocusedColors;
			
		cvs !? { this.put(*cvs) };
		
		if(Window.allWindows.select({ |w| "^CVCenter".matchRegexp(w.name) == true }).size < 1, {

			window = Window("CVCenter"+this.midistring, Rect(0, 0, 335, 200));
			window.view.background_(Color.black);
			flow = FlowLayout(window.bounds.insetBy(4));
			window.view.decorator = flow;
			flow.gap_(0@0);
			flow.shift(0, 0);

			
			if(tabProperties.size < 1, {
				tabProperties = tabProperties.add(());
				if(tab.isNil, { tabProperties[0].tabLabel = "default" }, { tabProperties[0].tabLabel = tab.asString });
				tabProperties[0].tabColor = nextColor.next;
				tabProperties.postln;
			});
			
			tabLabels = tabProperties.collect(_.tabLabel);
			labelColors = tabProperties.collect(_.tabColor);
			unfocusedColors = tabProperties.collect({ |t| t.tabColor.copy.alpha_(0.8) });
			tabProperties.postln;
			
			tabs = TabbedView(
				window, 
				Rect(0, 0, flow.bounds.width, flow.bounds.height-50), 
				labels: tabLabels, 
				scroll: true
			);
			tabs.view.resize_(5);
			tabs.labelColors_(labelColors);
			tabs.labelPadding_(5);
			tabs.unfocusedColors_(unfocusedColors);
			tabs.font_(GUI.font.new("Helvetica", 9));
			tabs.tabCurve_(3);
			tabs.stringColor_(Color.black);
			tabs.stringFocusedColor_(Color.white);
			tabs.focusActions_(Array.fill(tabs.views.size, {{ this.prRegroupWidgets(tabs.activeTab) }}));
			
			flow.shift(0, 0);
			
			switchBoard = ScrollView(window, Rect(0, 0, flow.bounds.width, 50));
			switchBoard.resize_(8);

			window.onClose_({
				cvWidgets.keysValuesDo({ |k, w|
					widgetStates[k].nameField = w.nameField.string;
					widgetStates[k].midiEdit = w.midiHead.enabled;
					widgetStates[k].midiLearn = w.midiLearn.value;
					widgetStates[k].midiBg = w.midiSrc.background;
					widgetStates[k].midiStrColor = w.midiSrc.stringColor;
					widgetStates[k].midiSrc = w.midiSrc.string;
					widgetStates[k].midiChan = w.midiChan.string;
					widgetStates[k].midiCtrl = w.midiCtrl.string;
				});
				tabProperties.do(_.nextPos_(0@0));
			});

			thisNextPos = 0@0;
			colwidth = widgetwidth+1; // add a small gap between widgets
			rowheight = widgetheight+1;
			rowwidth = window.bounds.width-10;
			colcount = rowwidth.div(colwidth);
			rowwidth = colcount * colwidth;
			
			order = cvsList.order;
			orderedCVs = cvsList.atAll(order);
			order.postln;
			
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
					
				cvWidgets[k] = CVWidget(
					tabs.views[cvTabIndex], orderedCVs[i], k, Rect(thisNextPos.x, thisNextPos.y, widgetwidth, widgetheight), this.setup
				);
								
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
				};
				widgetStates.put(k, (tabIndex: cvTabIndex));
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
						this.prSetup;
					})
				};	
				if(cvsList.size != lastUpdate, {
					if(cvsList.size > lastUpdate and:{ cvWidgets.size <= lastUpdate }, {
						this.prAddToGui;
						this.prUpdateSwitchboard;
					});
					if(cvsList.size < lastUpdate, {
						removedKeys = cvWidgets.keys.difference(cvsList.keys);
						removedKeys.do({ |k|
							cvWidgets[k].remove;
							cvWidgets.removeAt(k);
						});
						this.prRegroupWidgets(tabs.activeTab);
						this.prUpdateSwitchboard;
					});
					lastUpdate = cvsList.size;
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
			this.gui(tab.asSymbol);
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
		^[this.midimode, this.midimean, this.midistring];
	}
	
	// private Methods - not to be used directly
	
	*prSetup { |argMode, argMean, argString|
		argMode !? { this.midimode_(argMode) };
		argMean !? { this.midimean_(argMean) };
		argString !? { this.midistring_(argString.asString) };
		if(Window.allWindows.select({ |w| "^CVCenter".matchRegexp(w.name) == true }).size > 0, {
			window.name = "CVCenter"+midistring.asString;
			cvWidgets.keysValuesDo({ |k, cv| 
				cv.midimode_(this.midimode); 
				cv.midimean_(this.midimean);
				cv.midistring_(this.midistring.asString);
			})
		})
	}
			
	*prAddToGui { |tab|
		var allCVKeys, widgetKeys, thisKeys;
		var rowwidth, colcount;
		var cvTabIndex, tabLabels;
		var thisNextPos;
				
		tabLabels = tabProperties.collect({ |tab| tab.tabLabel.asSymbol });
		
		if(tab.notNil, {
			if(tabLabels.includes(tab.asSymbol), {
				cvTabIndex = tabLabels.indexOf(tab.asSymbol);
			}, {
				tabs.add(tab);
				cvTabIndex = tabLabels.size;
				tabProperties = tabProperties.add((tabLabel: tab, tabColor: nextColor.next));
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
						
		
		colwidth = widgetwidth+1; // add a small gap between widgets
		rowheight = widgetheight+1;
		rowwidth = window.bounds.width-10;
		colcount = rowwidth.div(colwidth);
		rowwidth = colcount * colwidth;
		
		allCVKeys = cvsList.keys;
		widgetKeys = cvWidgets.keys;
		thisKeys = allCVKeys.difference(widgetKeys);
		thisKeys.do({ |k|
			cvWidgets[k] = CVWidget(tabs.views[cvTabIndex], cvsList[k], k, Rect(thisNextPos.x, thisNextPos.y, widgetwidth, widgetheight), this.setup);
			cvWidgets[k].widgetBg.background_(tabProperties[cvTabIndex].tabColor);
			if(thisNextPos.x+colwidth >= rowwidth, {
				// jump to next row
				tabProperties[cvTabIndex].nextPos = thisNextPos = 0@(thisNextPos.y+rowheight);
			}, {
				// add next widget to the right
				tabProperties[cvTabIndex].nextPos = thisNextPos = thisNextPos.x+colwidth@(thisNextPos.y);
			});
			[cvTabIndex, thisNextPos].postln;
			widgetStates.put(k, (tabIndex: cvTabIndex));
			tabs.focusActions_(Array.fill(tabs.views.size, {{ this.prRegroupWidgets(tabs.activeTab) }}));
			tabs.focus(cvTabIndex);
		});
		this.prRegroupWidgets(cvTabIndex);
		window.front;
	}
	
	*prRegroupWidgets { |tabIndex|
		var rowwidth, rowheight, colcount, colwidth, widgetwidth, widgetheight, thisNextPos, order, orderedWidgets;
				
		widgetwidth = 52;
		colwidth = widgetwidth+1; // add a small gap between widgets
		widgetheight = 136;
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
	
	*prUpdateSwitchboard {
//		("controlButtons:"+controlButtons).postln;
		controlButtonKeys ? controlButtonKeys = IdentityDictionary();
		cvWidgets.pairsDo({ |k, wdgt|
//			wdgt.midiCtrl.value.postln;
			if(wdgt.midiCtrl.value != "ctrl", {
				controlButtonKeys.put(k, (
					uid: wdgt.midiSrc.value, 
					chan: wdgt.midiChan.value, 
					ctrl: wdgt.midiCtrl.value
				))
			})
		});
	}
			
}
