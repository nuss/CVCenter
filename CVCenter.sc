
CVCenter {

	classvar <cvsList, <nextCVKey, <cvWidgets, <window, nextPos;
	classvar <>midimode = 0, <>midimean = 64, <>midistring = "";  
	classvar 	widgetwidth = 52, widgetheight = 136, colwidth, rowheight, widgetStates;
	
	*new { |cvs...setUpArgs|
		
		if(cvsList.isNil, {
			cvsList = Dictionary.new;
			cvWidgets = Dictionary.new;
			widgetStates = Dictionary.new;
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
		
	*gui { |...args|
		var rowwidth, colcount;
//		var midiString, mode, mean;
		var updateRoutine, lastUpdate, lastUpdateWidth, lastSetUp, removedKeys, skipJacks;
			
		args !? { this.put(*args) };
		
		this.setup.postln;

		if(Window.allWindows.select({ |w| "^CVCenter".matchRegexp(w.name) == true }).size < 1, {

			window = Window("CVCenter"+this.midistring, Rect(0, 0, 335, 200), scroll: true);
			
			window.onClose_({
				cvWidgets.keysValuesDo({ |k, w|
					widgetStates.put(k, (
						nameField: w.nameField.string,
						midiEdit: w.midiHead.enabled,
						midiLearn: w.midiLearn.value,
						midiBg: w.midiSrc.background,
						midiStrColor: w.midiSrc.stringColor,
						midiSrc: w.midiSrc.string,
						midiChan: w.midiChan.string,
						midiCtrl: w.midiCtrl.string
					))
				})
			});

			nextPos = 5@0;
			colwidth = widgetwidth+1; // add a small gap between widgets
			rowheight = widgetheight+1;
			rowwidth = window.bounds.width-10;
			colcount = rowwidth.div(colwidth);
			rowwidth = colcount * colwidth;
			
			cvsList.keysValuesDo({ |k, v|
				cvWidgets[k] = CVWidget(window, v, k, nextPos, widgetwidth, widgetheight, this.setup);
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
				if(nextPos.x+colwidth > rowwidth, {
					// jump to next row
					nextPos = 5@(nextPos.y+rowheight);
				}, {
					// add next widget to the right
					nextPos = nextPos.x+colwidth@(nextPos.y);
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
					if(cvsList.size > lastUpdate, {
						this.prAddToGui;
					});
					if(cvsList.size < lastUpdate, {
						removedKeys = cvWidgets.keys.difference(cvsList.keys);
						("removed from CVCenter:"+removedKeys).postln;
						removedKeys.do({ |k|
							cvWidgets[k].remove;
							cvWidgets.removeAt(k);
						});
						this.prRegroupWidgets;
					});
					lastUpdate = cvsList.size;
				});
				try {
					if(window.bounds.width != lastUpdateWidth, {
						this.prRegroupWidgets;
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
		[key, spec, value].postln;		
		key ?? { 
			Error("You cannot use a CV in CVCenter without providing a key-name").throw;
		};
		
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
			this.gui;
		}, {
			this.prAddToGui;
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
			
	*prAddToGui {
		var allCVKeys, widgetKeys, thisKeys;
		var rowwidth, colcount;
		
		colwidth = widgetwidth+1; // add a small gap between widgets
		rowheight = widgetheight+1;
		rowwidth = window.bounds.width-10;
		colcount = rowwidth.div(colwidth);
		rowwidth = colcount * colwidth;
		
		allCVKeys = cvsList.keys;
		widgetKeys = cvWidgets.keys;
		thisKeys = allCVKeys.difference(widgetKeys);
		thisKeys.do({ |k| 
			cvWidgets[k] = CVWidget(window, cvsList[k], k, nextPos, widgetwidth, widgetheight, this.setup);
			if(nextPos.x+colwidth > rowwidth, {
				// jump to next row
				nextPos = 5@(nextPos.y+rowheight);
			}, {
				// add next widget to the right
				nextPos = nextPos.x+colwidth@(nextPos.y);
			});
		});
		window.front;
	}
	
	*prRegroupWidgets {
		var rowwidth, rowheight, colcount, colwidth, widgetwidth, widgetheight;
				
		widgetwidth = 52;
		colwidth = widgetwidth+1; // add a small gap between widgets
		widgetheight = 136;
		rowheight = widgetheight+1;
		rowwidth = window.bounds.width-10;
		colcount = rowwidth.div(colwidth);
		rowwidth = colcount * colwidth;
		nextPos = 5@0;
								
		cvWidgets.keysValuesDo({ |k, v|
			v.widgetXY_(nextPos);
			if(nextPos.x+colwidth > rowwidth, {
				// jump to next row
				nextPos = 5@(nextPos.y+rowheight);
			}, {
				// add next widget to the right
				nextPos = nextPos.x+colwidth@(nextPos.y);
			});
		})
	}
			
}
