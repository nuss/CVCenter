CVCenterKeyboard {
	classvar <all;
	var <synthDefName, <keyboardArg, <velocArg, <bendArg, widgetsPrefix;
	var on, off, bend, namesCVs;
	var <>debug = false;

	*new { |synthDefName, keyboardArg = \freq, velocArg = \veloc, bendArg = \bend, widgetsPrefix = \kb|
		^super.newCopyArgs(synthDefName, keyboardArg, velocArg, bendArg, widgetsPrefix).init;
	}

	init {
		synthDefName = synthDefName.asSymbol;

		all ?? {
			all = ();
		};

		all.put(synthDefName, this);

		if (CVCenter.scv[synthDefName].isNil) {
			CVCenter.scv.put(synthDefName, Array.newClear(128));
		} {
			Error("Sorry, the given SynthDef name cannot be used.").throw;
		};

		MIDIClient.init;
		// doesn't seem to work properly on Ubuntustudio 16
		// rather do it manually in QJackQtl...
		// MIDIClient.connectAll;
	}

	// keyboardArg is the arg that will be set through playing the keyboard
	// bendArg will be the arg that's set through the pitch bend wheel
	setUpControls { |tab, prefix, keyboardControl, velocControl, bendControl, server, deactivateDefaultWidgetActions = true|
		var testSynth, notesEnv;
		var args = [];

		server ?? { server = Server.default };

		prefix !? { widgetsPrefix = prefix };
		keyboardControl !? { keyboardArg = keyboardControl };
		velocControl !? { velocArg = velocControl };
		bendControl !? { bendArg = bendControl };

		tab ?? { tab = synthDefName };

		server ?? { server = Server.default };
		SynthDescLib.at(synthDefName) ?? {
			Error(
				"The synthDefName '%' does not exist".format(synthDefName)
			).throw;
		};

		if (SynthDescLib.at(synthDefName).hasGate.not) {
			Error(
				"The given SynthDef does not provide a 'gate' argument and can not be used."
			).throw;
		};

		server.waitForBoot {
			// SynthDef *should* have an \amp arg, otherwise it will sound for moment
			testSynth = Synth(synthDefName);
			// \gate will be set internally
			testSynth.cvcGui(prefix: widgetsPrefix, excemptArgs: [keyboardArg, velocArg, \gate], tab: tab, completionFunc: {
				this.addWidgetActionsForKeyboard(deactivateDefaultWidgetActions);
			});
			testSynth.release;
		}
	}

	addWidgetActionsForKeyboard { |deactivateDefaultActions = true|
		var args = SynthDescLib.at(synthDefName).controlDict.keys.asArray;
		var wdgtNames, wdgtName, nameString;

		wdgtNames = this.initCVs(args);

		args.do { |name, i|
			wdgtName = wdgtNames[i];
			CVCenter.cvWidgets[wdgtName] !? {
				if (CVCenter.cvWidgets[wdgtName].class == CVWidget2D) {
					#[lo, hi].do { |slot|
						CVCenter.addActionAt(
							wdgtName, 'keyboard set arg',
							"{ |cv| CVCenter.scv['%'].do { |synth| synth !? { synth.set('%', cv.value) }}; }"
							.format(synthDefName, name), slot);
						CVCenter.activateActionAt(wdgtName, \default, deactivateDefaultActions.not, slot);
					}
				} {
					CVCenter.addActionAt(wdgtName, 'keyboard set arg',
						"{ |cv| CVCenter.scv['%'].do { |synth| synth !? { synth.set('%', cv.value) }}; }"
						.format(synthDefName, name));
					CVCenter.activateActionAt(wdgtName, \default, deactivateDefaultActions.not);
				};
			}
		};

		this.initKeyboard;
	}

	reInit {
		var args = SynthDescLib.at(synthDefName).controlDict.keys.asArray;
		CVCenter.scv[synthDefName] !? {
			"re-initializing!".postln;
			this.free;
			CVCenter.scv.put(synthDefName, Array.newClear(128));
			this.initCVs(args);
			this.initKeyboard;
		}
	}

	initCVs { |args|
		var nameString, wdgtName;
		var wdgtNames = [];

		namesCVs = [];

		args.do { |name|
			nameString = name.asString;
			widgetsPrefix.notNil !? {
				nameString = nameString[0].toUpper ++ nameString[1..nameString.size-1];
			};
			wdgtName = (widgetsPrefix ++ nameString).asSymbol;
			CVCenter.cvWidgets[wdgtName] !? {
				if (CVCenter.cvWidgets[wdgtName].class == CVWidget2D) {
					if (namesCVs.includes(name).not) {
						namesCVs = namesCVs.add(name).add(CVCenter.at(wdgtName).asArray);
					}
				} {
					if (namesCVs.includes(name).not) {
						namesCVs = namesCVs.add(name).add(CVCenter.at(wdgtName));
					}
				}
			};
			wdgtNames = wdgtNames.add(wdgtName);
		};

		^wdgtNames;
	}

	initKeyboard {
		on = MIDIFunc.noteOn({ |veloc, num, chan, src|
			var argsValues = [keyboardArg, num.midicps, velocArg, veloc * 0.005] ++ namesCVs.deepCollect(2, _.value);
			if (this.debug) { "on: %\n".postf(argsValues) };
			CVCenter.scv[synthDefName][num] = Synth(synthDefName, argsValues);
		});

		off = MIDIFunc.noteOff({ |veloc, num, chan, src|
			if (this.debug) { "off".postln };
			CVCenter.scv[synthDefName][num].release;
		});

		bend = MIDIFunc.bend({ |bendVal, chan, src|
			if (this.debug) { "bend: %\n".postf(bendVal) };
		});
	}

	free {
		on !? { on.free };
		off !? { off.free };
		bend !? { bend.free };
		CVCenter.scv[synthDefName].do(_.release);
		CVCenter.scv.removeAt(synthDefName);
	}
}