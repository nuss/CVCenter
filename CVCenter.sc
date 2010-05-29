
CVCenter {

	classvar <cvsList, <nextCVKey, <cvWidgets, <window, <tabs, <switchBoard;
	classvar <>midimode = 0, <>midimean = 64, <>midistring = "";  
	classvar 	widgetwidth = 52, widgetheight = 136, colwidth, rowheight, widgetStates/*, tabLabels*/;
	classvar tabProperties;
	
	*new { |cvs...setUpArgs|
		
		if(cvsList.isNil, {
			cvsList = Dictionary.new;
			cvWidgets = Dictionary.new;
			widgetStates = Dictionary.new;
			tabProperties = [];
			this.prSetup(*setUpArgs);
			
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
		var rowwidth, colcount;
		var cvTabIndex;
		var updateRoutine, lastUpdate, lastUpdateWidth, lastSetUp, removedKeys, skipJacks;
		var thisNextPos, tabLabels;
			
		cvs !? { this.put(*cvs) };
		
//		this.setup.postln;

		if(Window.allWindows.select({ |w| "^CVCenter".matchRegexp(w.name) == true }).size < 1, {

			window = Window("CVCenter"+this.midistring, Rect(0, 0, 335, 200));
						
			tabLabels ?? { if(tab.isNil, { tabLabels = ["default"] }, { tabLabels = [tab.asString] }) };
			
			if(tabProperties.size < 1, {
				tabProperties = tabProperties.add(());
				if(tab.isNil, { tabProperties[0].tabLabel = "default" }, { tabProperties[0].tabLabel = tab.asString });
			});
			
			tabLabels = tabProperties.collect(_.tabLabel);
//			("tab-properties:"+tabProperties).postln;
			
			tabs = TabbedView(
				window, 
				Rect(0, 0, window.bounds.width, window.bounds.height-30), 
				labels: tabLabels, 
				scroll: true
			);
			tabs.view.resize_(5);
			tabs.labelColors_([Color.white]);
			tabs.font_(GUI.font.new("Helvetica", 9));
			tabs.tabCurve_(3);
			tabs.stringColor_(Color.white);
			tabs.stringFocusedColor_(Color.black);
			tabs.focusActions_(Array.fill(tabs.views.size, {{ this.prRegroupWidgets(tabs.activeTab) }}));

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
//				tabLabels = tabs.labelNames;
//				("widgetStates at gui-close:"+widgetStates).postcs;
			});

			thisNextPos = 0@0;
			colwidth = widgetwidth+1; // add a small gap between widgets
			rowheight = widgetheight+1;
			rowwidth = window.bounds.width-10;
			colcount = rowwidth.div(colwidth);
			rowwidth = colcount * colwidth;
			
			cvsList.keysValuesDo({ |k, v|
				if(widgetStates.size < 1, { cvTabIndex = 0 }, {
					if(widgetStates[k].isNil, {
						cvTabIndex = 0;
					}, {
						cvTabIndex = widgetStates[k].tabIndex ? cvTabIndex = 0;
					})
				});
				
				["widget-state now:"+widgetStates[k], "tab-properties:"+tabProperties[cvTabIndex]].postln;
				if(tabProperties[cvTabIndex].nextPos.isNil, { 
					thisNextPos = 0@0;
				}, { 
					thisNextPos = tabProperties[cvTabIndex].nextPos;
				});
					
				cvWidgets[k] = CVWidget(tabs.views[cvTabIndex], v, k, Rect(thisNextPos.x, thisNextPos.y, widgetwidth, widgetheight), this.setup);
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
//				("widgetStates at widget-creation:"+widgetStates).postln;
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
					});
					if(cvsList.size < lastUpdate, {
						removedKeys = cvWidgets.keys.difference(cvsList.keys);
						("removed from CVCenter:"+removedKeys).postln;
						removedKeys.do({ |k|
							cvWidgets[k].remove;
							cvWidgets.removeAt(k);
						});
						this.prRegroupWidgets(tabs.activeTab);
					});
					lastUpdate = cvsList.size;
				});
				try {
					if(window.bounds.width != lastUpdateWidth, {
//						"window-size changed".postln;
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
//		[key, spec, value].postln;		
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
			this.gui(tab.asString);
		}, {
			("now adding:"+tab.asString).postln;
			this.prAddToGui(tab.asString);
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
		
//		("prAddToGui called with:"+tab).postln;
		
		tabLabels = tabProperties.collect({ |tab| tab.tabLabel.asSymbol });
		
		if(tab.notNil, {
			if(tabLabels.includes(tab.asSymbol), {
				cvTabIndex = tabLabels.indexOf(tab.asSymbol);
				("tab exists at tab-index"+cvTabIndex).postln;
			}, {
				tabs.add(tab);
				cvTabIndex = tabLabels.size;
				("tab doesn't exist. new index:"+cvTabIndex).postln;
				tabProperties = tabProperties.add((tabLabel: tab));
			})
		}, {
			cvTabIndex = tabs.activeTab;
		});
		
		if(tabProperties[cvTabIndex].nextPos.notNil, {
			thisNextPos = tabProperties[cvTabIndex].nextPos;
			("nextPos not nil:"+thisNextPos).postln;
		}, {
			thisNextPos = 0@0;
			("nextPos is nil:"+thisNextPos).postln;
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
			["update from thisKeys", k, thisNextPos, tabProperties[cvTabIndex].nextPos].postln;
			cvWidgets[k] = CVWidget(tabs.views[cvTabIndex], cvsList[k], k, Rect(thisNextPos.x, thisNextPos.y, widgetwidth, widgetheight), this.setup);
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
//			("tab-properties:"+tabProperties).postln;
		});
		window.front;
	}
	
	*prRegroupWidgets { |tabIndex|
		var rowwidth, rowheight, colcount, colwidth, widgetwidth, widgetheight, thisNextPos;
				
		widgetwidth = 52;
		colwidth = widgetwidth+1; // add a small gap between widgets
		widgetheight = 136;
		rowheight = widgetheight+1;
		rowwidth = window.bounds.width-15;
		colcount = rowwidth.div(colwidth);
		rowwidth = colcount * colwidth;
		thisNextPos = 0@0;
		
		tabIndex !? {						
			cvWidgets.keysValuesDo({ |k, v|
				if(tabIndex == widgetStates[k].tabIndex, {
//					[k, v, widgetStates[k].tabIndex, thisNextPos, tabProperties[widgetStates[k].tabIndex].nextPos].postln;
					if(thisNextPos != (0@0), { 
//						["false", thisNextPos].postln; 
						thisNextPos = tabProperties[widgetStates[k].tabIndex].nextPos;
					});
//					["next Position:"+tabProperties[widgetStates[k].tabIndex].nextPos, "next tab-index:"+widgetStates[k].tabIndex].postln;
					v.widgetXY_(thisNextPos);
//					[thisNextPos.x+colwidth, rowwidth].postln;
					if(thisNextPos.x+colwidth >= rowwidth, {
						// jump to next row
						thisNextPos = tabProperties[widgetStates[k].tabIndex].nextPos = 0@(thisNextPos.y+rowheight);
					}, {
						// add next widget to the right
						thisNextPos = tabProperties[widgetStates[k].tabIndex].nextPos = thisNextPos.x+colwidth@(thisNextPos.y);
					})
				})
			})
		}
	}
			
}
